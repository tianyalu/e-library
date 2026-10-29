package com.example.elibrary.api.controller;

import com.example.elibrary.api.dto.request.BorrowRequest;
import com.example.elibrary.api.dto.response.BorrowResponse;
import com.example.elibrary.api.dto.response.RenewResponse;
import com.example.elibrary.api.dto.response.ReturnResponse;
import com.example.elibrary.application.PagedResult;
import com.example.elibrary.application.dto.LoanView;
import com.example.elibrary.application.event.CatalogueChanged;
import com.example.elibrary.application.service.IdempotencyService;
import com.example.elibrary.application.service.LoanQueryService;
import com.example.elibrary.application.usecase.BorrowBookUseCase;
import com.example.elibrary.application.usecase.RenewLoanUseCase;
import com.example.elibrary.application.usecase.ReturnBookUseCase;
import com.example.elibrary.domain.model.LoanStatus;
import com.example.elibrary.infrastructure.security.AuthenticatedMember;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * 借阅相关接口。
 *
 * <p>下面两件事放在这一层而不是更里面，因为它们属于传输层的事：
 * <ul>
 *   <li><b>Idempotency-Key</b>：重试请求返回第一次的响应，而不是再开一笔借阅；</li>
 *   <li><b>缓存失效</b>：等业务事务提交之后再发事件，绝不放在事务里。</li>
 * </ul>
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/loans")
public class LoanController {

    private static final String ENDPOINT_BORROW = "POST /api/v1/loans";
    private static final String ENDPOINT_RENEW = "POST /api/v1/loans/{loanId}/renew";

    private final BorrowBookUseCase borrowBookUseCase;
    private final ReturnBookUseCase returnBookUseCase;
    private final RenewLoanUseCase renewLoanUseCase;
    private final LoanQueryService loanQueryService;
    private final IdempotencyService idempotencyService;
    private final ApplicationEventPublisher publisher;

    public LoanController(BorrowBookUseCase borrowBookUseCase,
                          ReturnBookUseCase returnBookUseCase,
                          RenewLoanUseCase renewLoanUseCase,
                          LoanQueryService loanQueryService,
                          IdempotencyService idempotencyService,
                          ApplicationEventPublisher publisher) {
        this.borrowBookUseCase = borrowBookUseCase;
        this.returnBookUseCase = returnBookUseCase;
        this.renewLoanUseCase = renewLoanUseCase;
        this.loanQueryService = loanQueryService;
        this.idempotencyService = idempotencyService;
        this.publisher = publisher;
    }

    @Operation(summary = "借书",
            description = "可选 Idempotency-Key；重放时返回首次响应，状态码为 200。")
    @PostMapping
    public ResponseEntity<BorrowResponse> borrow(
            @AuthenticationPrincipal AuthenticatedMember currentMember,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody BorrowRequest request) {
        IdempotencyService.Outcome<BorrowResponse> outcome = idempotencyService.execute(
                currentMember.memberId(), ENDPOINT_BORROW, idempotencyKey, request, BorrowResponse.class,
                () -> {
                    BorrowBookUseCase.Result result = borrowBookUseCase.execute(currentMember.memberId(),
                            request.bookId());
                    publisher.publishEvent(new CatalogueChanged(request.bookId()));
                    return new BorrowResponse(result.loanId(), result.bookId(), result.bookTitle(),
                            result.copyBarcode(), result.borrowedAt(), result.dueAt());
                });
        BorrowResponse body = outcome.body();
        log.info("Borrow for memberId={} produced loanId={} replayed={}", currentMember.memberId(),
                body.loanId(), outcome.replayed());
        return ResponseEntity.status(outcome.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .location(URI.create("/api/v1/loans/" + body.loanId()))
                .body(body);
    }

    @Operation(summary = "还书", description = "天然幂等，重复归还返回同样的结果。")
    @PostMapping("/{loanId}/return")
    public ReturnResponse returnLoan(@AuthenticationPrincipal AuthenticatedMember currentMember,
                                     @PathVariable Long loanId) {
        ReturnBookUseCase.Result result = returnBookUseCase.execute(currentMember.memberId(), loanId);
        publisher.publishEvent(new CatalogueChanged(result.bookId()));
        return new ReturnResponse(result.loanId(), result.bookId(), result.returnedAt(), result.daysOverdue(),
                result.fineAmount(), result.alreadyReturned());
    }

    @Operation(summary = "续借", description = "延长到期日，受续借次数上限限制。")
    @PostMapping("/{loanId}/renew")
    public RenewResponse renew(@AuthenticationPrincipal AuthenticatedMember currentMember,
                               @PathVariable Long loanId,
                               @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        IdempotencyService.Outcome<RenewResponse> outcome = idempotencyService.execute(
                currentMember.memberId(), ENDPOINT_RENEW, idempotencyKey, loanId, RenewResponse.class,
                () -> {
                    RenewLoanUseCase.Result result = renewLoanUseCase.execute(currentMember.memberId(), loanId);
                    return new RenewResponse(result.loanId(), result.previousDueAt(), result.newDueAt(),
                            result.renewCount());
                });
        return outcome.body();
    }

    @Operation(summary = "我的借阅记录", description = "可按状态过滤；在借=ACTIVE,OVERDUE")
    @GetMapping
    public PagedResult<LoanView> myLoans(@AuthenticationPrincipal AuthenticatedMember currentMember,
                                         @RequestParam(required = false) LoanStatus status,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size) {
        return loanQueryService.myLoans(currentMember.memberId(), status, page, cap(size));
    }

    @Operation(summary = "我的单条借阅")
    @GetMapping("/{loanId}")
    public LoanView myLoan(@AuthenticationPrincipal AuthenticatedMember currentMember,
                           @PathVariable Long loanId) {
        return loanQueryService.findById(currentMember.memberId(), loanId, currentMember.isLibrarian());
    }

    private int cap(int size) {
        return Math.min(Math.max(size, 1), 100);
    }
}
