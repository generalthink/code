package com.think123.code.log;

import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoggingResponse {
	private int status;
	private Map<String, String> headers;
	private String body;
}