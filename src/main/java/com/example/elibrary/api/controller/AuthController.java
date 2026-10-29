package com.example.elibrary.api.controller;

import com.example.elibrary.api.dto.request.LoginRequest;
import com.example.elibrary.api.dto.response.TokenResponse;
import com.example.elibrary.domain.exception.AuthenticationFailedException;
import com.example.elibrary.infrastructure.security.AuthenticatedMember;
import com.example.elibrary.infrastructure.security.AuthenticationService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 令牌接口。这里不做业务逻辑，只负责拿凭证换一个能标识调用方的签名令牌。
 *
 * <p>换失败时抛的是领域异常，所以走的是和业务拒绝同一处理器，不会变成带技术细节的 500。
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    public AuthController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/token")
    public ResponseEntity<TokenResponse> issueToken(@Valid @RequestBody LoginRequest request) {
        AuthenticationService.Session session = authenticationService
                .authenticate(request.email(), request.password())
                .orElseThrow(AuthenticationFailedException::invalidCredentials);
        AuthenticatedMember member = session.member();
        return ResponseEntity.ok(TokenResponse.of(session.token(), session.expiresInSeconds(),
                member.memberId(), member.role()));
    }
}
