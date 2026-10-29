package com.example.elibrary.api;

import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.application.port.out.CopyRepositoryPort;
import com.example.elibrary.application.port.out.MemberRepositoryPort;
import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCategory;
import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.Member;
import com.example.elibrary.domain.model.MemberRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HTTP 契约的端到端测试：认证、幂等借书、幂等还书。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LoanApiIntegrationTest {

    private static final String PASSWORD = "secret";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MemberRepositoryPort members;
    @Autowired
    private BookRepositoryPort books;
    @Autowired
    private CopyRepositoryPort copies;
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 每个用例都新建自己的会员和书：整套件共用一个库和一个 Spring 上下文，
     * 复用 email、isbn 这种唯一列会把用例绑死在执行顺序上。
     */
    private String email;
    private String bearer;
    private Long bookId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        email = "member-" + suffix + "@elibrary.test";
        members.save(new Member("Test Member", email, passwordEncoder.encode(PASSWORD), MemberRole.MEMBER));
        Book book = books.save(new Book("978-opq" + suffix, "Idempotent Book", "Api Author",
                BookCategory.TECHNOLOGY, 2026));
        copies.save(new BookCopy(book.getId(), "BC-" + suffix));
        book.registerCopy();
        books.save(book);
        bookId = book.getId();
        bearer = "Bearer " + login();
    }

    private String login() throws Exception {
        String body = objectMapper.writeValueAsString(new LoginBody(email, PASSWORD));
        String response = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(response, "$.accessToken");
    }

    @Test
    @DisplayName("同一个 Idempotency-Key 重试借书，重放的是第一次那条借阅记录")
    void idempotentBorrow() throws Exception {
        String first = borrow("key-1", bookId, org.springframework.http.HttpStatus.CREATED);
        String second = borrow("key-1", bookId, org.springframework.http.HttpStatus.OK);

        long firstLoanId = loanIdOf(first);
        long secondLoanId = loanIdOf(second);
        assertThat(secondLoanId).isEqualTo(firstLoanId);
    }

    /** 小数字的 JSON 会被 Jackson 转成 Integer，所以按 Number 读，别直接按 Long 取。 */
    private static long loanIdOf(String responseBody) {
        return ((Number) com.jayway.jsonpath.JsonPath.read(responseBody, "$.loanId")).longValue();
    }

    @Test
    @DisplayName("同一个键换请求体是冲突，不是默默成功")
    void idempotencyKeyConflict() throws Exception {
        Book another = books.save(new Book("978-second" + UUID.randomUUID().toString().substring(0, 8),
                "Second Book", "Api Author",
                BookCategory.TECHNOLOGY, 2026));
        copies.save(new BookCopy(another.getId(), "BC-API-2"));
        another.registerCopy();
        books.save(another);

        borrow("key-2", bookId, org.springframework.http.HttpStatus.CREATED);
        mockMvc.perform(post("/api/v1/loans")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .header("Idempotency-Key", "key-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": " + another.getId() + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"))
                .andExpect(jsonPath("$.traceId").exists());
    }

    @Test
    @DisplayName("重复还书返回同一条已关闭的借阅，而不是报错")
    void idempotentReturn() throws Exception {
        String borrowResponse = borrow("key-3", bookId, org.springframework.http.HttpStatus.CREATED);
        long loanId = loanIdOf(borrowResponse);

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyReturned").value(false));

        mockMvc.perform(post("/api/v1/loans/" + loanId + "/return").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alreadyReturned").value(true))
                .andExpect(jsonPath("$.fineAmount").doesNotExist());
    }

    @Test
    @DisplayName("没带令牌借书会被拒绝，返回的是 Problem Details")
    void unauthenticatedBorrowRejected() throws Exception {
        mockMvc.perform(post("/api/v1/loans")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": " + bookId + "}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").exists());
    }

    @Test
    @DisplayName("目录可以匿名浏览")
    void catalogueIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/books").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalPages").exists());
    }

    private String borrow(String key, Long requestedBookId, org.springframework.http.HttpStatus expected)
            throws Exception {
        return mockMvc.perform(post("/api/v1/loans")
                        .header(HttpHeaders.AUTHORIZATION, bearer)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookId\": " + requestedBookId + "}"))
                .andExpect(status().is(expected.value()))
                .andExpect(jsonPath("$.loanId").exists())
                .andReturn().getResponse().getContentAsString();
    }

    record LoginBody(String email, String password) {
    }
}
