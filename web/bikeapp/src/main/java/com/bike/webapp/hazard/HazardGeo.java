package com.bike.webapp.hazard;

/** WGS84 helpers: distances are metres on a spherical earth, which is plenty for hazard zones. */
public final class HazardGeo {

	public static final double EARTH_RADIUS_M = 6_371_000.0d;
	private static final double METERS_PER_DEGREE_LAT = Math.PI * EARTH_RADIUS_M / 180.0d;

	private HazardGeo() {
	}

	public static boolean isValidCoordinate(Double latitude, Double longitude) {
		return latitude != null && longitude != null
				&& !latitude.isNaN() && !longitude.isNaN()
				&& latitude >= -90.0d && latitude <= 90.0d
				&& longitude >= -180.0d && longitude <= 180.0d;
	}

	/** Haversine distance in metres. */
	public static double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lon2 - lon1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
				* Math.sin(dLon / 2) * Math.sin(dLon / 2);
		return EARTH_RADIUS_M * 2.0d * Math.atan2(Math.sqrt(a), Math.sqrt(1.0d - a));
	}

	/** A latitude/longitude rectangle that contains every point within {@code meters} of the centre. */
	public static final class Box {
		public final double minLat;
		public final double maxLat;
		public final double minLon;
		public final double maxLon;

		Box(double minLat, double maxLat, double minLon, double maxLon) {
			this.minLat = minLat;
			this.maxLat = maxLat;
			this.minLon = minLon;
			this.maxLon = maxLon;
		}
	}

	/**
	 * The search rectangle for a circle. Longitude degrees shrink towards the poles, so the longitude span
	 * is widened by 1/cos(lat) (capped, so near the poles it simply covers all longitudes). A box that
	 * crosses the antimeridian is clamped to +-180, which is irrelevant for Korea but never wrong
	 * for the points it does cover.
	 */
	public static Box boundingBox(double latitude, double longitude, double meters) {
		double dLat = meters / METERS_PER_DEGREE_LAT;
		double cos = Math.cos(Math.toRadians(latitude));
		double dLon = cos < 0.01d ? 180.0d : Math.min(180.0d, meters / (METERS_PER_DEGREE_LAT * cos));
		return new Box(Math.max(-90.0d, latitude - dLat), Math.min(90.0d, latitude + dLat),
				Math.max(-180.0d, longitude - dLon), Math.min(180.0d, longitude + dLon));
	}
}
