package com.example.elibrary.api.controller;

import com.example.elibrary.application.PagedResult;
import com.example.elibrary.application.dto.BookDetail;
import com.example.elibrary.application.dto.BookSummary;
import com.example.elibrary.application.service.CatalogService;
import com.example.elibrary.domain.model.BookCategory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 目录浏览。这几个接口是公开的：书目这块展示内容，不该因为认证服务挂了就看不了。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/books")
public class BookController {

    private final CatalogService catalogService;

    public BookController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @Operation(summary = "浏览/检索书目",
            description = "对书名、作者、ISBN 做文本检索；结果有短时缓存。")
    @GetMapping
    public PagedResult<BookSummary> browse(
            @RequestParam(required = false) @Parameter(description = "free text over title/author/isbn") String q,
            @RequestParam(required = false) BookCategory category,
            @RequestParam(required = false) String author,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int cappedSize = Math.min(Math.max(size, 1), 100);
        return catalogService.search(q, category, author, page, cappedSize);
    }

    @Operation(summary = "书籍详情（含实时可借数）")
    @GetMapping("/{id}")
    public BookDetail detail(@PathVariable Long id) {
        return catalogService.detail(id);
    }
}
