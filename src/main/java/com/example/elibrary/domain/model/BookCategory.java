package com.example.elibrary.domain.model;

/**
 * 图书分类。既是检索过滤条件，也是缓存键的一部分。
 *
 * <p>不带任何框架注解：Spring 自带的转换器能按名字映射枚举，
 * domain 不需要知道自己是怎么被序列化的。
 */
public enum BookCategory {
    TECHNOLOGY,
    BUSINESS,
    SCIENCE,
    LITERATURE,
    HISTORY,
    MEDICINE
}
