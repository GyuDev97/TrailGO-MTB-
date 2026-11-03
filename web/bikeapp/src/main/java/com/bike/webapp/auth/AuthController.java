package com.bike.webapp.auth;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bike.webapp.weather.RequestRateLimiter;

/**
 * Social sign-in for the app. The app sends the provider's access token; this server asks the
 * provider who it belongs to and answers with its own session. Tokens are never logged.
 */
@RestController
@RequestMapping("/app/auth/")
public class AuthController {

	private static final Logger log = LoggerFactory.getLogger(AuthController.class);
	private static final long WINDOW_MS = 10L * 60L * 1000L;

	private final Map<String, SocialVerifier> verifiers = new HashMap<String, SocialVerifier>();
	private final MemberAccounts accounts;
	private final AuthTokenService tokens;
	private final RequestRateLimiter signInLimiter = new RequestRateLimiter(30, WINDOW_MS);
	private final RequestRateLimiter refreshLimiter = new RequestRateLimiter(120, WINDOW_MS);

	@Value("${trailgo.weather.trust-forwarded-for:false}")
	private boolean trustForwardedFor;

	@Autowired
	public AuthController(List<SocialVerifier> verifierList, MemberAccounts accounts, AuthTokenService tokens) {
		for (SocialVerifier verifier : verifierList) {
			verifiers.put(verifier.provider(), verifier);
		}
		this.accounts = accounts;
		this.tokens = tokens;
	}

	@RequestMapping(value = "{provider}", method = RequestMethod.POST)
	public ResponseEntity<Map<String, Object>> signIn(HttpServletRequest request,
			@PathVariable("provider") String provider, @RequestParam("access_token") String accessToken) {
		SocialVerifier verifier = verifiers.get(provider);
		if (verifier == null) {
			return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
		}
		if (!signInLimiter.tryAcquire("signin:" + clientAddress(request), System.currentTimeMillis())) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "600").build();
		}
		if (accessToken.trim().isEmpty() || accessToken.length() > 2000) {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
		}
		try {
			Optional<SocialProfile> profile = verifier.verify(accessToken.trim());
			if (!profile.isPresent()) {
				return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
			}
			MemberAccounts.Result member = accounts.signIn(profile.get());
			AuthTokenService.Session session = tokens.issue(member.riderId);
			Map<String, Object> body = sessionBody(session);
			body.put("riderId", member.riderId);
			body.put("newMember", member.created);
			body.put("nickname", profile.get().getNickname());
			body.put("imageUrl", profile.get().getImageUrl());
			return ResponseEntity.ok(body);
		} catch (SocialUnavailableException e) {
			log.warn("{} sign-in unavailable: {}", provider, e.getMessage());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
	}

	@RequestMapping(value = "refresh", method = RequestMethod.POST)
	public ResponseEntity<Map<String, Object>> refresh(HttpServletRequest request,
			@RequestParam("refresh_token") String refreshToken) {
		if (!refreshLimiter.tryAcquire("refresh:" + clientAddress(request), System.currentTimeMillis())) {
			return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "600").build();
		}
		Optional<AuthTokenService.Session> session = tokens.refresh(refreshToken.trim());
		if (!session.isPresent()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
		}
		Map<String, Object> body = sessionBody(session.get());
		body.put("riderId", session.get().riderId);
		return ResponseEntity.ok(body);
	}

	/** Ends this sign-in on the server (the app also forgets its tokens). Always succeeds. */
	@RequestMapping(value = "logout", method = RequestMethod.POST)
	public ResponseEntity<Void> logout(@RequestParam(value = "refresh_token", required = false) String refreshToken,
			HttpServletRequest request) {
		tokens.revoke(refreshToken);
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith("Bearer ")) {
			tokens.revoke(header.substring(7).trim());
		}
		return ResponseEntity.noContent().build();
	}

	private static Map<String, Object> sessionBody(AuthTokenService.Session session) {
		Map<String, Object> body = new LinkedHashMap<String, Object>();
		body.put("accessToken", session.accessToken);
		body.put("accessTokenExpiresAt", session.accessTokenExpiresAtMillis);
		body.put("refreshToken", session.refreshToken);
		return body;
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
