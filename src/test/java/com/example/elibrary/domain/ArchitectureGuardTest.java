package com.example.elibrary.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 架构守护：把依赖方向钉住，别让它慢慢烂掉。
 *
 * <p>没有引 ArchUnit，就自己扫一遍源码。规则只有一条：domain 不许出现 Spring、JPA、
 * Hibernate、Jackson、Micrometer 的 import。
 */
class ArchitectureGuardTest {

    private static final Path DOMAIN_ROOT = Paths.get("src", "main", "java", "com", "example", "elibrary", "domain");
    private static final List<String> FORBIDDEN = List.of(
            "org.springframework",
            "jakarta.persistence",
            "org.hibernate",
            "com.fasterxml.jackson",
            "io.micrometer");

    @Test
    @DisplayName("domain 包不出现任何框架 import")
    void domainHasNoFrameworkImports() throws IOException {
        assertThat(Files.exists(DOMAIN_ROOT))
                .as("test expects to run from the project root: %s", DOMAIN_ROOT.toAbsolutePath())
                .isTrue();

        try (Stream<Path> files = Files.walk(DOMAIN_ROOT)) {
            List<String> offenders = files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            return Stream.of(Files.readString(path));
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .filter(source -> FORBIDDEN.stream().anyMatch(source::contains))
                    .filter(source -> !source.contains("// @TestOnly"))
                    .toList();
            assertThat(offenders).as("no domain class may import %s", FORBIDDEN).isEmpty();
        }
    }
}
