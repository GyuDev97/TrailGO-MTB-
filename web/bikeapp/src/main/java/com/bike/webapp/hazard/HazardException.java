package com.bike.webapp.hazard;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;

/** A refused hazard call. Becomes {@code {"code": ..., "message": ...}} with the given HTTP status. */
public class HazardException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private final HttpStatus status;
	private final String code;
	private final transient Map<String, Object> extra = new LinkedHashMap<String, Object>();

	public HazardException(HttpStatus status, String code, String message) {
		super(message);
		this.status = status;
		this.code = code;
	}

	public HazardException with(String key, Object value) {
		extra.put(key, value);
		return this;
	}

	public HttpStatus getStatus() {
		return status;
	}

	public String getCode() {
		return code;
	}

	public Map<String, Object> body() {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("code", code);
		body.put("message", getMessage());
		body.putAll(extra);
		return body;
	}
}
