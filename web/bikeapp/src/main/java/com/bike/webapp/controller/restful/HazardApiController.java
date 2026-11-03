package com.bike.webapp.controller.restful;

import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bike.webapp.auth.AppIdentity;
import com.bike.webapp.dto.HazardRequest;
import com.bike.webapp.dto.HazardView;
import com.bike.webapp.hazard.HazardException;
import com.bike.webapp.service.HazardAppService;
import com.bike.webapp.weather.RequestRateLimiter;

/**
 * Hazard zones for the app. Reading is open to guests; reporting, editing and withdrawing need a signed-in
 * rider (bearer token checked by AppAuthFilter), and the reporter is always that rider, never a body field.
 */
@RestController
@RequestMapping("/app/hazards")
public class HazardApiController {

	private final Logger log = LoggerFactory.getLogger(getClass());

	static final long HOUR_MS = 60L * 60L * 1000L;
	static final long READ_WINDOW_MS = 10L * 60L * 1000L;
	static final int REPORTS_PER_HOUR = 10;
	static final int READS_PER_WINDOW = 120;

	private final HazardAppService service;
	private final RequestRateLimiter reportLimiter = new RequestRateLimiter(REPORTS_PER_HOUR, HOUR_MS);
	private final RequestRateLimiter readLimiter = new RequestRateLimiter(READS_PER_WINDOW, READ_WINDOW_MS);

	//프록시 뒤에서만 true: X-Forwarded-For의 첫 주소를 호출자 IP로 본다
	@Value("${trailgo.weather.trust-forwarded-for:false}")
	private boolean trustForwardedFor;

	@Autowired
	public HazardApiController(HazardAppService service) {
		this.service = service;
	}

	//주변 위험지역 (게스트 가능). 반경은 위험구역 경계까지의 거리로 판단한다.
	@GetMapping("nearby")
	public ResponseEntity<Map<String, Object>> nearby(HttpServletRequest request,
			@RequestParam(value = "lat", required = false) Double lat,
			@RequestParam(value = "lon", required = false) Double lon,
			@RequestParam(value = "radius", required = false) Integer radius) {
		if (!readLimiter.tryAcquire("ip:" + clientAddress(request), System.currentTimeMillis())) {
			return tooMany();
		}
		HazardAppService.Nearby result = service.nearby(lat, lon, radius, AppIdentity.riderOf(request));
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("items", result.getItems());
		body.put("count", result.getItems().size());
		body.put("radiusM", result.getRadiusM());
		body.put("truncated", result.isTruncated());
		body.put("removedIds", result.getRemovedIds());
		body.put("serverTime", System.currentTimeMillis());
		return ResponseEntity.ok(body);
	}

	@GetMapping("{id}")
	public HazardView detail(HttpServletRequest request, @PathVariable("id") int id) {
		return service.get(id, AppIdentity.riderOf(request));
	}

	@PostMapping
	public ResponseEntity<?> report(HttpServletRequest request, @RequestBody HazardRequest body) {
		String rider = requireRider(request);
		if (!reportLimiter.tryAcquire("rider:" + rider, System.currentTimeMillis())) {
			return tooMany();
		}
		return ResponseEntity.status(HttpStatus.CREATED).body(service.report(rider, body));
	}

	@PutMapping("{id}")
	public HazardView update(HttpServletRequest request, @PathVariable("id") int id, @RequestBody HazardRequest body) {
		return service.update(requireRider(request), id, body);
	}

	@DeleteMapping("{id}")
	public Map<String, Object> withdraw(HttpServletRequest request, @PathVariable("id") int id) {
		service.delete(requireRider(request), id);
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("deleted", true);
		return body;
	}

	private static String requireRider(HttpServletRequest request) {
		String rider = AppIdentity.riderOf(request);
		if (rider == null) {
			throw new HazardException(HttpStatus.UNAUTHORIZED, "LOGIN_REQUIRED", "로그인이 필요합니다.");
		}
		return rider;
	}

	private static ResponseEntity<Map<String, Object>> tooMany() {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("code", "TOO_MANY_REQUESTS");
		body.put("message", "요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.");
		return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "600").body(body);
	}

	private String clientAddress(HttpServletRequest request) {
		if (trustForwardedFor) {
			String forwarded = request.getHeader("X-Forwarded-For");
			if (forwarded != null && !forwarded.trim().isEmpty()) {
				return forwarded.split(",")[0].trim();
			}
		}
		return request.getRemoteAddr();
	}
}
