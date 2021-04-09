package com.think123.code.log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

@Slf4j
public class RestHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body,
            ClientHttpRequestExecution execution) throws IOException {

        request.getHeaders().add("X_USER_REQ_ID", MDC.get("requestId"));
        this.traceRequest(request, body);
        ClientHttpResponse response = execution.execute(request, body);
        this.traceResponse(response);
        return response;
    }

    private void traceRequest(HttpRequest request, byte[] body) {

        StringBuilder message = (new StringBuilder()).append("\n");
        message.append("=========================== request begin ===========================")
                .append("\n");
        message.append("URI         : " + request.getURI()).append("\n");
        message.append("Method      : " + request.getMethod()).append("\n");
        message.append("Headers     : " + request.getHeaders()).append("\n");
        message.append("Request body: " + new String(body, StandardCharsets.UTF_8)).append("\n");
        message.append("===========================  request end  ===========================");
        log.info(message.toString());
    }

    private void traceResponse(ClientHttpResponse response) throws IOException {

        StringBuilder message = (new StringBuilder()).append("\n");
        message.append("=========================== response begin ===========================")
                .append("\n");
        message.append("Status code  : " + response.getStatusCode()).append("\n");
        message.append("Status text  : " + response.getStatusText()).append("\n");
        message.append("Headers      : " + response.getHeaders()).append("\n");
        message.append("Response body: " + StreamUtils
                .copyToString(response.getBody(), StandardCharsets.UTF_8)).append("\n");
        message.append(
                "===========================  response end  ===========================");
        log.info(message.toString());
    }
}
