package com.example.elibrary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * {@code elibrary.*} 配置的类型化入口。用 record 是为了让参数集中在一处、不可变，
 * 而不是把魔法数字散落在代码里。
 *
 * <p>这里没有加 {@code @Validated}：缺项时 Spring 会绑成 null / 0 而不报错，
 * 需要严格校验的话要另外补约束注解。
 *
 * @param policy  借阅规则（借期、续借次数、罚金）
 * @param outbox  事务性发件箱的中转设置
 * @param catalog 目录读侧设置（分页上限这类护栏）
 */
@ConfigurationProperties(prefix = "elibrary")
public record LibraryProperties(Policy policy, Outbox outbox, Catalog catalog) {

    public record Policy(int loanPeriodDays,
                         int renewalDays,
                         int maxRenewals,
                         int maxActiveLoans,
                         BigDecimal finePerDay,
                         String currency) {
    }

    public record Outbox(boolean enabled, int batchSize, int delayMillis) {
    }

    public record Catalog(int maxPageSize) {
    }
}
