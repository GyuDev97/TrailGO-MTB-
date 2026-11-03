package com.bike.webapp.controller.restful;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.bike.webapp.hazard.HazardException;

/** One error shape for /app/hazards: {@code {"code": "...", "message": "..."}}. Internals are logged, never sent. */
@RestControllerAdvice(assignableTypes = HazardApiController.class)
public class HazardApiAdvice {

	private final Logger log = LoggerFactory.getLogger(getClass());

	@ExceptionHandler(HazardException.class)
	public ResponseEntity<Map<String, Object>> refused(HazardException e) {
		return ResponseEntity.status(e.getStatus()).body(e.body());
	}

	@ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
	public ResponseEntity<Map<String, Object>> malformed(Exception e) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "요청 형식이 올바르지 않습니다.");
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> failed(Exception e) {
		log.error("Hazard API failed", e);
		return error(HttpStatus.INTERNAL_SERVER_ERROR, "SERVER_ERROR", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
	}

	private static ResponseEntity<Map<String, Object>> error(HttpStatus status, String code, String message) {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("code", code);
		body.put("message", message);
		return ResponseEntity.status(status).body(body);
	}
}
