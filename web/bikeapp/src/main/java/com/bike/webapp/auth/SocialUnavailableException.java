package com.bike.webapp.auth;

/** The provider could not be asked (network error, 5xx) or this server is not configured for it. */
public class SocialUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public SocialUnavailableException(String message) {
		super(message);
	}
}
