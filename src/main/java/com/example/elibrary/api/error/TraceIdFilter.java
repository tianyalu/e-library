package com.example.elibrary.api.error;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 给一次请求的所有日志和错误响应打上同一个关联 ID。
 *
 * <p>分布式部署下这个值由上游网关传入；这里接受它，意味着一次请求能被端到端串起来，
 * 这是排查故障的基本要求。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(TRACE_ID_HEADER);
        String traceId = (incoming == null || incoming.isBlank()) ? UUID.randomUUID().toString() : incoming;
        MDC.put(TRACE_ID, traceId);
        response.setHeader(TRACE_ID_HEADER, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(TRACE_ID);
        }
    }

    public static String currentTraceId() {
        String value = MDC.get(TRACE_ID);
        return value == null ? "unknown" : value;
    }
}
