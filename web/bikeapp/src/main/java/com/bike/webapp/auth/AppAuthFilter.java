package com.bike.webapp.auth;

import java.io.IOException;
import java.util.Optional;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads {@code Authorization: Bearer <token>} on /app/**. No header: the call goes on as a guest
 * (many app endpoints are public). A header that does not verify (expired, revoked, unknown) is
 * answered with 401 right here, so the app knows to refresh the token instead of silently being
 * treated as somebody else.
 */
@Component
public class AppAuthFilter extends OncePerRequestFilter {

	private static final String BEARER = "Bearer ";

	private final AuthTokenService tokens;

	@Autowired
	public AppAuthFilter(AuthTokenService tokens) {
		this.tokens = tokens;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return path == null || !path.startsWith("/app/") || path.startsWith("/app/auth/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader("Authorization");
		if (header != null && header.startsWith(BEARER)) {
			Optional<String> rider = tokens.resolveAccess(header.substring(BEARER.length()).trim());
			if (!rider.isPresent()) {
				response.setHeader("WWW-Authenticate", "Bearer error=\"invalid_token\"");
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
				return;
			}
			request.setAttribute(AppIdentity.ATTRIBUTE, rider.get());
		}
		chain.doFilter(request, response);
	}
}
