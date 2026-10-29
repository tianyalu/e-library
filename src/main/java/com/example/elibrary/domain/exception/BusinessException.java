package com.example.elibrary.domain.exception;

/**
 * 所有"这是业务结果、不是 bug"的异常的父类。
 *
 * <p>基础设施层不抛这些；它们一律由领域层或应用层抛出，
 * 再被那个唯一的全局处理器翻译成 RFC 7807 响应。
 */
public abstract class BusinessException extends RuntimeException {

    private final BusinessErrorCode code;

    protected BusinessException(BusinessErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessErrorCode code() {
        return code;
    }
}
