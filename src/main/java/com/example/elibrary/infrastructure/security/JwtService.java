package com.example.elibrary.infrastructure.security;

import com.example.elibrary.domain.model.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

/**
 * JWT 令牌的签发与校验（无状态认证，服务端不存会话）。
 *
 * <p>令牌里只有身份标识，不放额度、权限这类授权数据——那些每次都回库重读，
 * 这样停用某个会员立刻生效，不用等令牌过期。
 */
@Slf4j
@Component
public class JwtService {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long ttlSeconds;
    private final String issuer;

    public JwtService(@Value("${security.jwt.secret}") String secret,
                      @Value("${security.jwt.access-token-ttl-seconds}") long ttlSeconds,
                      @Value("${security.jwt.issuer}") String issuer) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlSeconds = ttlSeconds;
        this.issuer = issuer;
    }

    public String issue(AuthenticatedMember member) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(member.memberId()))
                .claim(CLAIM_ROLE, member.role().name())
                .issuer(issuer)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key)
                .compact();
    }

    public Optional<AuthenticatedMember> parse(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Long memberId = Long.valueOf(claims.getSubject());
            MemberRole role = MemberRole.valueOf(claims.get(CLAIM_ROLE, String.class));
            return Optional.of(new AuthenticatedMember(memberId, role));
        } catch (JwtException | NumberFormatException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return Optional.empty();
        }
    }
}
