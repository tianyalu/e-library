package com.example.elibrary.domain.exception;

/**
 * 凭证没通过。
 *
 * <p>抛的是领域异常，而不是 Spring 的 {@code ResponseStatusException}：这样它走的处理器
 * 和其它业务拒绝完全一致，返回文档里那套 Problem Details，不会漏成 500。
 */
public class AuthenticationFailedException extends BusinessException {

    public AuthenticationFailedException(String message) {
        super(BusinessErrorCode.INVALID_CREDENTIALS, message);
    }

    public static AuthenticationFailedException invalidCredentials() {
        return new AuthenticationFailedException("Email or password is incorrect");
    }
}
