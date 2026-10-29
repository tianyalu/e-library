package com.example.elibrary.infrastructure.persistence;

import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.application.port.out.CopyRepositoryPort;
import com.example.elibrary.application.port.out.MemberRepositoryPort;
import com.example.elibrary.domain.model.Book;
import com.example.elibrary.domain.model.BookCategory;
import com.example.elibrary.domain.model.BookCopy;
import com.example.elibrary.domain.model.Member;
import com.example.elibrary.domain.model.MemberRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 库里没数据时灌一份演示数据。
 *
 * <p>幂等：先查有没有已有账号，所以重启不会把目录灌两遍。
 * 真实部署应该走管理后台接口录入，这里只是为了 checkout 之后一条命令就能用。
 */
@Slf4j
@Component
@Profile("!test")
public class DataInitializer implements ApplicationRunner {

    private static final String DEFAULT_PASSWORD = "password123";

    private final MemberRepositoryPort members;
    private final BookRepositoryPort books;
    private final CopyRepositoryPort copies;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(MemberRepositoryPort members, BookRepositoryPort books, CopyRepositoryPort copies,
                           PasswordEncoder passwordEncoder) {
        this.members = members;
        this.books = books;
        this.copies = copies;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (members.findByEmail("librarian@elibrary.test").isPresent()) {
            log.info("Demo data already present - skipping seeding");
            return;
        }
        String password = passwordEncoder.encode(DEFAULT_PASSWORD);

        members.save(new Member("Ada Lovelace", "librarian@elibrary.test", password, MemberRole.LIBRARIAN));
        members.save(new Member("Grace Hopper", "grace@elibrary.test", password, MemberRole.MEMBER));
        members.save(new Member("Alan Turing", "alan@elibrary.test", password, MemberRole.MEMBER));
        members.save(new Member("Barbara Liskov", "barbara@elibrary.test", password, MemberRole.MEMBER));

        seed("978-0132350884", "Clean Code", "Robert C. Martin", BookCategory.TECHNOLOGY, 2008, 3);
        seed("978-0201633610", "Design Patterns", "Erich Gamma", BookCategory.TECHNOLOGY, 1994, 2);
        seed("978-0321125217", "Domain-Driven Design", "Eric Evans", BookCategory.TECHNOLOGY, 2003, 2);
        seed("978-1119449278", "Analysing Requirements", "Elizabeth Hull", BookCategory.TECHNOLOGY, 2017, 1);
        seed("978-0134494166", "The Pragmatic Programmer", "David Thomas", BookCategory.TECHNOLOGY, 2019, 1);
        seed("978-0198528119", "An Introduction to Financial Markets", "Michael Choudhary",
                BookCategory.BUSINESS, 2019, 4);
        seed("978-1292214076", "Principles of Banking", "Jane Moore", BookCategory.BUSINESS, 2020, 2);
        seed("978-0071807789", "Options, Futures and Other Derivatives", "John Hull", BookCategory.BUSINESS,
                2021, 1);
        seed("978-0141036144", "A Brief History of Time", "Stephen Hawking", BookCategory.SCIENCE, 1988, 2);
        seed("978-0061120084", "Thinking, Fast and Slow", "Daniel Kahneman", BookCategory.SCIENCE, 2011, 1);
        seed("978-0307476463", "The Innovators", "Walter Isaacson", BookCategory.HISTORY, 2014, 1);

        log.info("Seeded catalogue and demo accounts (password for all: {})", DEFAULT_PASSWORD);
    }

    private void seed(String isbn, String title, String author, BookCategory category, int year, int copyCount) {
        Book book = books.save(new Book(isbn, title, author, category, year));
        for (int i = 1; i <= copyCount; i++) {
            copies.save(new BookCopy(book.getId(), barcode(book.getId(), i)));
            book.registerCopy();
        }
        books.save(book);
    }

    private String barcode(Long bookId, int index) {
        return String.format("BC-%04d-%02d", bookId, index);
    }
}
