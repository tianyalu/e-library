package com.example.elibrary.api.error;

import java.time.Instant;

/**
 * 对外的错误契约（RFC 7807 Problem Details）。
 *
 * @param type    稳定的 URN，给程序判断用
 * @param title   一句话给人看的概括
 * @param status  HTTP 状态码
 * @param detail  具体说明，可以直接给终端用户看
 * @param code    {@code BusinessErrorCode} 里的业务码，用于分支和展示映射
 * @param traceId 这次请求的关联 ID，排查问题时用
 */
public record ProblemBody(String type,
                          String title,
                          int status,
                          String detail,
                          String code,
                          String traceId,
                          Instant timestamp) {

    public ProblemBody(String type, String title, int status, String detail, String code, Instant timestamp) {
        this(type, title, status, detail, code, TraceIdFilter.currentTraceId(), timestamp);
    }
}
