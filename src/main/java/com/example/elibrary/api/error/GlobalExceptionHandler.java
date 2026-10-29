package com.example.elibrary.api.error;

import com.example.elibrary.domain.exception.BusinessException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * 异常到 HTTP 响应的唯一出口，统一返回 RFC 7807 Problem Details。
 *
 * <p>三条规则：
 * <ul>
 *   <li>业务失败带上自己的错误码和状态码，不笼统地甩 400 / 500；</li>
 *   <li>校验失败要列出是哪个字段错了；</li>
 *   <li>意料之外的异常记堆栈，但回给调用方的信息要克制：SQL、类名、IP 一个都不许漏出去。</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemBody> handleBusiness(BusinessException exception) {
        HttpStatus status = HttpStatus.valueOf(exception.code().statusCode());
        log.info("Business rejection {} - {}", exception.code(), exception.getMessage());
        return ResponseEntity.status(status)
                .body(new ProblemBody("urn:elibrary:" + exception.code().name().toLowerCase(),
                        exception.code().title(), status.value(), exception.getMessage(),
                        exception.code().name(), Instant.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemBody> handleInvalidBody(MethodArgumentNotValidException exception) {
        String detail = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return badRequest("Request validation failed: " + detail);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemBody> handleConstraint(ConstraintViolationException exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemBody> handleUnreadable(HttpMessageNotReadableException exception) {
        return badRequest("Malformed JSON request body");
    }

    /**
     * 路径或查询参数类型对不上（比如 {@code /loans/abc}）是调用方的错，不是服务端故障。
     * 不接住的话 Spring 会报 500——既撒谎，又把错误大盘搞脏。
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemBody> handleMismatchedArgument(MethodArgumentTypeMismatchException exception) {
        return badRequest("Request parameter '" + exception.getName() + "' must be of type "
                + requiredTypeName(exception) + ", got '" + exception.getValue() + "'");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemBody> handleIllegalArgument(IllegalArgumentException exception) {
        return badRequest(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemBody> handleUnexpected(Exception exception) {
        log.error("Unexpected failure while serving request traceId={}", TraceIdFilter.currentTraceId(), exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ProblemBody("urn:elibrary:internal", "Internal server error", 500,
                        "The request could not be completed, please retry with the same Idempotency-Key",
                        "INTERNAL", Instant.now()));
    }

    private ResponseEntity<ProblemBody> badRequest(String detail) {
        return ResponseEntity.badRequest()
                .body(new ProblemBody("urn:elibrary:bad-request", "Bad request", 400, detail, "VALIDATION",
                        Instant.now()));
    }

    /** 只留简单类名，带着 java.lang 前缀对调用方没意义。 */
    private static String requiredTypeName(MethodArgumentTypeMismatchException exception) {
        Class<?> required = exception.getRequiredType();
        return required == null ? "unknown" : required.getSimpleName();
    }
}
