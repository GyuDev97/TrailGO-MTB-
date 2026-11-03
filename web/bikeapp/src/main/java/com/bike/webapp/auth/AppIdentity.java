package com.bike.webapp.auth;

import javax.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Who is calling, as established by {@link AppAuthFilter} from a verified session token. A rider id
 * written into a URL or a form field is only a claim; this is the proof.
 */
public final class AppIdentity {

	public static final String ATTRIBUTE = "trailgo.auth.rider";

	private AppIdentity() {
	}

	/** The signed-in rider, or null for a guest / anonymous call. */
	public static String riderOf(HttpServletRequest request) {
		Object rider = request.getAttribute(ATTRIBUTE);
		return rider instanceof String ? (String) rider : null;
	}

	/**
	 * Lets the call through only when the signed-in rider is the rider the call is about.
	 *
	 * @throws ResponseStatusException 401 without a valid session, 403 for somebody else's data
	 */
	public static String requireOwner(HttpServletRequest request, String claimedRiderId) {
		String rider = riderOf(request);
		if (rider == null) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		}
		if (claimedRiderId == null || !rider.equals(claimedRiderId.trim())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN);
		}
		return rider;
	}
}
