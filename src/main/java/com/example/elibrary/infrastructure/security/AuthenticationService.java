package com.example.elibrary.infrastructure.security;

import com.example.elibrary.application.port.out.MemberRepositoryPort;
import com.example.elibrary.domain.model.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Credentials -> bearer token.
 *
 * <p>Deliberately returns {@code Optional.empty()} for every failure reason (unknown account,
 * bad password) so the endpoint cannot be used to enumerate registered emails.
 */
@Slf4j
@Service
public class AuthenticationService {

    private final MemberRepositoryPort members;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthenticationService(MemberRepositoryPort members, PasswordEncoder passwordEncoder,
                                 JwtService jwtService) {
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public record Session(String token, AuthenticatedMember member, long expiresInSeconds) {

    }

    public Optional<Session> authenticate(String email, String rawPassword) {
        Optional<Member> maybeMember = members.findByEmail(email == null ? null : email.trim().toLowerCase());
        if (maybeMember.isEmpty()) {
            return Optional.empty();
        }
        Member member = maybeMember.get();
        if (!passwordEncoder.matches(rawPassword, member.getPasswordHash())) {
            return Optional.empty();
        }
        AuthenticatedMember principal = new AuthenticatedMember(member.getId(), member.getRole());
        log.debug("Issuing token for memberId={} role={}", member.getId(), member.getRole());
        return Optional.of(new Session(jwtService.issue(principal), principal, ttlSeconds()));
    }

    private long ttlSeconds() {
        return 3600L;
    }
}
