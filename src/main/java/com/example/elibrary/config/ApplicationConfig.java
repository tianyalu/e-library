package com.example.elibrary.config;

import com.example.elibrary.application.policy.LibraryLoanPolicy;
import com.example.elibrary.domain.policy.LoanPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;

import java.time.Clock;

/**
 * 把几个不依赖框架的基础件装配进来。两个 bean 值得说一句：
 * <ul>
 *   <li>注入 {@link Clock} 而不是直接 {@code now()}，到期日、罚金这些跟时间有关的行为
 *       才好在测试里钉死；</li>
 *   <li>{@link LoanPolicy} 把配置项适配成领域抽象，规则本身不用起 Spring 就能测。</li>
 * </ul>
 */
@Configuration
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties(LibraryProperties.class)
public class ApplicationConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public LoanPolicy loanPolicy(LibraryProperties properties) {
        LibraryProperties.Policy policy = properties.policy();
        return new LibraryLoanPolicy(policy.loanPeriodDays(), policy.renewalDays(), policy.maxRenewals(),
                policy.maxActiveLoans(), policy.finePerDay(), policy.currency());
    }
}
