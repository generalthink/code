package com.think123.code.log;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoggingEntity {
	private String userNo;
	private String tenantId;
	private Long responseTimeMs;
	private LoggingRequest request;
	private LoggingResponse response;
}
