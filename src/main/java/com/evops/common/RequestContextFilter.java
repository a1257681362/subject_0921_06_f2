package com.evops.common;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 每次请求生成（或透传 X-Request-No）请求号，并从请求头解析操作人，写入 RequestContext。
 * 请求头：X-Request-No / X-Operator-Id / X-Operator-Name
 */
@Component("auditRequestContextFilter")
@Order(1)
public class RequestContextFilter extends OncePerRequestFilter {

    public static final String H_REQUEST_NO = "X-Request-No";
    public static final String H_OPERATOR_ID = "X-Operator-Id";
    public static final String H_OPERATOR_NAME = "X-Operator-Name";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {
        String requestNo = request.getHeader(H_REQUEST_NO);
        if (!StringUtils.hasText(requestNo)) {
            requestNo = "REQ-" + UUID.randomUUID().toString().replace("-", "");
        }

        Long operatorId = null;
        String idHeader = request.getHeader(H_OPERATOR_ID);
        if (StringUtils.hasText(idHeader)) {
            try {
                operatorId = Long.parseLong(idHeader.trim());
            } catch (NumberFormatException ignore) {
                // 非数字操作人ID忽略，按匿名处理
            }
        }
        String operatorName = decodeHeader(request.getHeader(H_OPERATOR_NAME));

        RequestContext.set(new RequestContext.Ctx(requestNo, operatorId, operatorName));
        try {
            chain.doFilter(request, response);
        } finally {
            RequestContext.clear();
        }
    }

    /**
     * Tomcat 默认按 ISO-8859-1 解码请求头，客户端按 UTF-8 发送的中文操作人名会乱码，
     * 这里按 UTF-8 还原。
     */
    private static String decodeHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return value;
        }
        return new String(value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                java.nio.charset.StandardCharsets.UTF_8);
    }
}
