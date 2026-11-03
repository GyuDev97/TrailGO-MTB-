package com.bike.webapp.weather;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class KmaGridConverterTest {

	@Test
	void seoulCityHallIsGrid60_127() {
		KmaGridConverter.Grid grid = KmaGridConverter.toGrid(37.5665, 126.9780);
		assertNotNull(grid);
		assertEquals(60, grid.nx);
		assertEquals(127, grid.ny);
	}

	@Test
	void busanAndJejuMatchTheOfficialKmaGrid() {
		KmaGridConverter.Grid busan = KmaGridConverter.toGrid(35.1796, 129.0756);
		assertEquals(98, busan.nx);
		assertEquals(76, busan.ny);
		KmaGridConverter.Grid jeju = KmaGridConverter.toGrid(33.4996, 126.5312);
		assertEquals(53, jeju.nx);
		assertEquals(38, jeju.ny);
	}

	@Test
	void daeguGridCells() {
		KmaGridConverter.Grid city = KmaGridConverter.toGrid(35.8714, 128.6014);
		assertEquals(89, city.nx);
		assertEquals(91, city.ny);
		KmaGridConverter.Grid apsan = KmaGridConverter.toGrid(35.8329, 128.5650);
		assertEquals(88, apsan.nx);
		assertEquals(90, apsan.ny);
	}

	@Test
	void invalidOrOutOfGridCoordinatesGiveNoGrid() {
		assertNull(KmaGridConverter.toGrid(null, 128.0));
		assertNull(KmaGridConverter.toGrid(35.0, null));
		assertNull(KmaGridConverter.toGrid(Double.NaN, 128.0));
		assertNull(KmaGridConverter.toGrid(95.0, 128.0));
		// New York is far outside the KMA grid.
		assertNull(KmaGridConverter.toGrid(40.7128, -74.0060));
	}
}
