package com.bike.webapp.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class HazardGeoTest {

	@Test
	void oneDegreeOfLatitudeIsAbout111Kilometres() {
		assertEquals(111_195.0, HazardGeo.distanceMeters(37.0, 127.0, 38.0, 127.0), 5.0);
	}

	@Test
	void distanceIsZeroForTheSamePointAndSymmetric() {
		assertEquals(0.0, HazardGeo.distanceMeters(35.8, 128.5, 35.8, 128.5), 1e-9);
		assertEquals(HazardGeo.distanceMeters(37.5, 127.0, 35.1, 129.0),
				HazardGeo.distanceMeters(35.1, 129.0, 37.5, 127.0), 1e-6);
	}

	@Test
	void longitudeDegreesShrinkWithLatitude() {
		double atEquator = HazardGeo.distanceMeters(0.0, 127.0, 0.0, 128.0);
		double atSeoul = HazardGeo.distanceMeters(37.5, 127.0, 37.5, 128.0);
		assertEquals(111_195.0, atEquator, 5.0);
		assertEquals(atEquator * Math.cos(Math.toRadians(37.5)), atSeoul, 100.0);
	}

	@Test
	void boundingBoxContainsEveryPointWithinTheDistance() {
		double lat = 37.5;
		double lon = 127.0;
		HazardGeo.Box box = HazardGeo.boundingBox(lat, lon, 1000);
		double[][] bearings = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 }, { 0.7, 0.7 }, { -0.7, 0.7 } };
		for (double[] direction : bearings) {
			// walk 999 m in that direction (in local metres) and check the box still holds the point
			double dLat = direction[0] * 999 / 111_195.0;
			double dLon = direction[1] * 999 / (111_195.0 * Math.cos(Math.toRadians(lat)));
			double pLat = lat + dLat;
			double pLon = lon + dLon;
			assertTrue(HazardGeo.distanceMeters(lat, lon, pLat, pLon) <= 1000.0);
			assertTrue(pLat >= box.minLat && pLat <= box.maxLat && pLon >= box.minLon && pLon <= box.maxLon);
		}
	}

	@Test
	void boundingBoxStaysWithinTheGlobe() {
		HazardGeo.Box box = HazardGeo.boundingBox(89.9999, 179.9999, 5000);
		assertTrue(box.maxLat <= 90.0 && box.maxLon <= 180.0 && box.minLon >= -180.0);
	}

	@Test
	void coordinateValidation() {
		assertTrue(HazardGeo.isValidCoordinate(37.5, 127.0));
		assertTrue(HazardGeo.isValidCoordinate(-90.0, 180.0));
		assertFalse(HazardGeo.isValidCoordinate(90.0001, 0.0));
		assertFalse(HazardGeo.isValidCoordinate(0.0, -180.1));
		assertFalse(HazardGeo.isValidCoordinate(null, 127.0));
		assertFalse(HazardGeo.isValidCoordinate(Double.NaN, 127.0));
	}

	@Test
	void typeParsingAcceptsOnlyTheSixCodes() {
		assertEquals(6, HazardType.values().length);
		assertEquals(HazardType.BAD_ROAD, HazardType.parse("BAD_ROAD"));
		assertEquals(HazardType.BAD_ROAD, HazardType.parse(" BAD_ROAD "));
		org.junit.jupiter.api.Assertions.assertNull(HazardType.parse("bad_road"));
		org.junit.jupiter.api.Assertions.assertNull(HazardType.parse("DROP TABLE"));
		org.junit.jupiter.api.Assertions.assertNull(HazardType.parse(null));
		assertEquals(HazardType.OTHER, HazardType.parseOrOther("LEGACY"));
	}
}
