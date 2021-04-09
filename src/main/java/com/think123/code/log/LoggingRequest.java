package com.think123.code.log;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoggingRequest {
	private String clientIp;
	private String method;
	private String path;
	private Map<String, String> params;
	private Map<String, String> headers;
	private String body;
}