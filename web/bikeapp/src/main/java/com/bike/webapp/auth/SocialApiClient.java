package com.bike.webapp.auth;

/** GET with a bearer token. A seam so the verifiers can be tested without the network. */
public interface SocialApiClient {

	/** @throws SocialUnavailableException when no answer could be obtained at all */
	Response get(String url, String bearerToken);

	class Response {
		public final int status;
		public final String body;

		public Response(int status, String body) {
			this.status = status;
			this.body = body;
		}
	}
}
