package com.bike.webapp.weather;

import java.time.LocalDateTime;

/** Source of raw 초단기실황 responses. An interface so tests never touch the network. */
public interface KmaWeatherClient {

	/** What came back from the service. {@code status} is 0 when no HTTP response was received at all. */
	final class RawResponse {
		public final int status;
		public final String body;

		public RawResponse(int status, String body) {
			this.status = status;
			this.body = body;
		}
	}

	/**
	 * Calls getUltraSrtNcst for the base time (a whole hour).
	 *
	 * @return the response, or null when the service could not be reached (timeout, I/O error)
	 */
	RawResponse fetchObservation(int nx, int ny, LocalDateTime baseDateTime);
}
