package com.bike.webapp.weather;

/**
 * Converts WGS84 latitude/longitude to the KMA (기상청) forecast grid (nx, ny) using the official
 * Lambert conformal conic parameters of the 동네예보 grid.
 */
public final class KmaGridConverter {

	public static final class Grid {
		public final int nx;
		public final int ny;

		Grid(int nx, int ny) {
			this.nx = nx;
			this.ny = ny;
		}

		@Override
		public String toString() {
			return nx + "," + ny;
		}
	}

	private static final double RE = 6371.00877; // earth radius (km)
	private static final double GRID = 5.0; // grid spacing (km)
	private static final double SLAT1 = 30.0;
	private static final double SLAT2 = 60.0;
	private static final double OLON = 126.0;
	private static final double OLAT = 38.0;
	private static final double XO = 43;
	private static final double YO = 136;

	public static final int MAX_NX = 149;
	public static final int MAX_NY = 253;

	private KmaGridConverter() {
	}

	/** @return the grid cell, or null when the coordinates are invalid or outside the KMA grid. */
	public static Grid toGrid(Double latitude, Double longitude) {
		if (latitude == null || longitude == null
				|| latitude.isNaN() || longitude.isNaN()
				|| latitude.isInfinite() || longitude.isInfinite()
				|| latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
			return null;
		}
		double degrad = Math.PI / 180.0;
		double re = RE / GRID;
		double slat1 = SLAT1 * degrad;
		double slat2 = SLAT2 * degrad;
		double olon = OLON * degrad;
		double olat = OLAT * degrad;

		double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
		sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
		double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
		sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
		double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
		ro = re * sf / Math.pow(ro, sn);

		double ra = Math.tan(Math.PI * 0.25 + latitude * degrad * 0.5);
		ra = re * sf / Math.pow(ra, sn);
		double theta = longitude * degrad - olon;
		if (theta > Math.PI) {
			theta -= 2.0 * Math.PI;
		}
		if (theta < -Math.PI) {
			theta += 2.0 * Math.PI;
		}
		theta *= sn;

		int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
		int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
		if (nx < 1 || nx > MAX_NX || ny < 1 || ny > MAX_NY) {
			return null;
		}
		return new Grid(nx, ny);
	}
}
