package com.think123.code.log;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;

@Slf4j
@Order
@Component
public class LoggingFilter extends OncePerRequestFilter {

    public static final String UNKNOWN = "unknown";
    private static final String IGNORE_CONTENT_TYPE = "multipart/form-data";
    private static final String NEED_TRACE_PATH_PREFIX = "/api";
    private final static String X_USER_REQ_ID = "X_USER_REQ_ID";
    private final static String REQUEST_ID = "requestId";
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!isRequestValid(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!needLog(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 对request和response进行封装，这样可以让我们多次消费请求和响应
        if (!(request instanceof ContentCachingRequestWrapper)) {
            request = new ContentCachingRequestWrapper(request);
        }
        if (!(response instanceof ContentCachingResponseWrapper)) {
            response = new ContentCachingResponseWrapper(response);
        }

        // 从header中获取X_USER_REQ_ID的值，如果不存在则生成
        String requestId = Optional
                .ofNullable(request.getHeader(X_USER_REQ_ID)).orElse(UUID.randomUUID().toString());


        // 通过SLF4J的MDC工具类将requestId放到threadLocal中
        MDC.put(REQUEST_ID, requestId);
        response.setHeader(REQUEST_ID, requestId);

        long startTime = System.currentTimeMillis();

        filterChain.doFilter(request, response);


        byte[] body = ((ContentCachingResponseWrapper) response).getContentAsByteArray();
        copyBodyToResponse(response);

        long responseTimeMs = System.currentTimeMillis() - startTime;

        LoggingEntity loggingEntity = LoggingEntity.builder()
                .userNo("")
                .tenantId("")
                .responseTimeMs(responseTimeMs)
                .request(getLoggingRequest(request))
                .response(getLoggingResponse(response, body))
                .build();
        //记录日志，包含了请求体以及响应
        log.info(objectMapper.writeValueAsString(loggingEntity));

        // 相当于threadLocal.remove
        MDC.clear();



    }

    private boolean needLog(HttpServletRequest request) {
        return request.getRequestURI().startsWith(NEED_TRACE_PATH_PREFIX) && !Objects
                .equals(IGNORE_CONTENT_TYPE, request.getContentType());
    }

    private boolean isRequestValid(HttpServletRequest request) {
        try {
            new URI(request.getRequestURL().toString());
            return true;
        } catch (URISyntaxException ex) {
            return false;
        }
    }

    private LoggingRequest getLoggingRequest(HttpServletRequest request)
            throws UnsupportedEncodingException {

        String clientIp = getClientIp(request);
        Map<String, String> headers = getHeaders(request);

        ContentCachingRequestWrapper wrapper = WebUtils
                .getNativeRequest(request, ContentCachingRequestWrapper.class);

        String requestBody = new String(wrapper.getContentAsByteArray(),
                wrapper.getCharacterEncoding());

        LoggingRequest loggingRequest = new LoggingRequest();
        loggingRequest.setClientIp(clientIp);
        loggingRequest.setMethod(wrapper.getMethod());
        loggingRequest.setPath(wrapper.getRequestURI());
        loggingRequest
                .setParams(getParameters(wrapper.getParameterMap()));
        loggingRequest.setHeaders(headers);
        loggingRequest.setBody(requestBody);

        return loggingRequest;
    }

    private LoggingResponse getLoggingResponse(HttpServletResponse response, byte[] body) throws IOException {

        Map<String, String> headers = getHeaders(response);

        ContentCachingResponseWrapper wrapper = WebUtils
                .getNativeResponse(response, ContentCachingResponseWrapper.class);

        String responseBody = new String(body,
                wrapper.getCharacterEncoding());

        LoggingResponse loggingResponse = new LoggingResponse();
        loggingResponse.setStatus(wrapper.getStatus());
        loggingResponse.setHeaders(headers);

        loggingResponse.setBody(responseBody);

        return loggingResponse;
    }

    private void copyBodyToResponse(HttpServletResponse response) throws IOException {
        ContentCachingResponseWrapper responseWrapper = WebUtils
                .getNativeResponse(response, ContentCachingResponseWrapper.class);
        Objects.requireNonNull(responseWrapper).copyBodyToResponse();
    }

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(UNKNOWN)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(UNKNOWN)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(UNKNOWN)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(UNKNOWN)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.length() == 0 || ip.equalsIgnoreCase(UNKNOWN)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    public Map<String, String> getParameters(Map<String, String[]> parameterMap) {
        return parameterMap.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> {
            String[] values = e.getValue();
            return values.length > 0 ? String.join(",", values) : "[EMPTY]";
        }));
    }

    public Map<String, String> getHeaders(HttpServletRequest request) {
        Map<String, String> headers = new HashMap<>(0);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (headerName != null) {
                headers.put(headerName, request.getHeader(headerName));
            }
        }
        return headers;
    }

    public Map<String, String> getHeaders(HttpServletResponse response) {
        Map<String, String> headers = new HashMap<>(0);
        for (String headerName : response.getHeaderNames()) {
            headers.put(headerName, response.getHeader(headerName));
        }
        return headers;
    }
}
