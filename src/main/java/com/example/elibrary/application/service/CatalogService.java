package com.example.elibrary.application.service;

import com.example.elibrary.application.PagedResult;
import com.example.elibrary.application.dto.BookDetail;
import com.example.elibrary.application.dto.BookSummary;
import com.example.elibrary.application.port.out.BookRepositoryPort;
import com.example.elibrary.domain.exception.ResourceNotFoundException;
import com.example.elibrary.domain.model.BookCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * 目录读侧。
 *
 * <p><b>缓存策略。</b>目录数据读多写少且变化慢，所以用 Redis 缓存。
 * 但<em>库存</em>本身绝不缓存：热门库存值直接查库，跟核心系统里的账户余额一个道理。
 * 这个读模型里的 {@code availableCopies} 可能略旧（受 TTL 限制）；
 * 真正权威的判断发生在借书事务内部。
 */
@Slf4j
@Service
@CacheConfig(cacheNames = "catalogue")
public class CatalogService {

    private final BookRepositoryPort books;

    public CatalogService(BookRepositoryPort books) {
        this.books = books;
    }

    @Cacheable(key = "'detail:' + #id")
    public BookDetail detail(Long id) {
        return BookDetail.from(books.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Book", id)));
    }

    @Cacheable(key = "'search:' + #keyword + ':' + #category + ':' + #author + ':' + #page + ':' + #size")
    public PagedResult<BookSummary> search(String keyword, BookCategory category, String author, int page, int size) {
        PagedResult<com.example.elibrary.domain.model.Book> result = books.search(normalise(keyword), category,
                author, page, size);
        List<BookSummary> summaries = result.content().stream().map(BookSummary::from).toList();
        return PagedResult.of(summaries, result.page(), result.size(), result.totalElements());
    }

    private String normalise(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return keyword.trim().toLowerCase(Locale.ROOT);
    }
}
