package com.example.elibrary;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 电子图书馆服务。
 *
 * <p>分层，依赖一律指向内层：
 * <pre>
 *   api  ->  application  ->  domain
 *             ^        （端口由 infrastructure 实现）
 * </pre>
 * domain 不带任何框架依赖，所以业务规则可以脱离 Spring、JPA、HTTP 单独跑和测试。
 */
@SpringBootApplication
public class ELibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ELibraryApplication.class, args);
    }
}
