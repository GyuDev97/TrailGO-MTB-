package com.bike.webapp.auth;

import java.util.Optional;

/** Checks a provider access token with the provider itself. */
public interface SocialVerifier {

	/** "kakao" or "naver". */
	String provider();

	/**
	 * @return the verified profile, or empty when the token is not valid for this service. Never
	 *         throws for a bad token; a provider outage is reported as {@link SocialUnavailableException}.
	 */
	Optional<SocialProfile> verify(String accessToken);
}
