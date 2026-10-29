package com.example.elibrary.application;

import java.util.List;

/**
 * 不带框架味道的分页抽象。
 *
 * <p>端口不许往上暴露框架类型（比如 {@code org.springframework.data.domain.Page}）：
 * 换掉 JPA 不能波及应用层。
 *
 * @param content       当前页的数据
 * @param page          页码，从 0 开始
 * @param size          请求的页大小
 * @param totalElements 符合条件的总行数
 * @param totalPages    总页数
 */
public record PagedResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PagedResult<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size == 0 ? 0 : (int) Math.ceil((double) totalElements / size);
        return new PagedResult<>(content, page, size, totalElements, totalPages);
    }
}
