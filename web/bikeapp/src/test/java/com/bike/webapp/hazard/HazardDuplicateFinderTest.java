package com.bike.webapp.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.bike.webapp.dto.HazardRow;

class HazardDuplicateFinderTest {

	private static final double LAT = 37.5;
	private static final double LON = 127.0;
	private static final double METERS_PER_DEGREE_LAT = 111_195.0;

	private static HazardRow zone(int id, double northMeters, int radius, String type, String title) {
		HazardRow row = new HazardRow();
		row.setId(id);
		row.setLatitude(LAT + northMeters / METERS_PER_DEGREE_LAT);
		row.setLongitude(LON);
		row.setRadiusM(radius);
		row.setHazardType(type);
		row.setTitle(title);
		row.setDescription("");
		row.setStatus("PENDING");
		return row;
	}

	private static HazardDuplicateFinder.Probe probe(int radius, String type, String title) {
		return new HazardDuplicateFinder.Probe(LAT, LON, radius, type, title, "", 0);
	}

	@Test
	void overlappingCirclesAreCandidates() {
		// centres 50 m apart, radii 30 + 30: they overlap
		List<HazardDuplicateFinder.Candidate> found = HazardDuplicateFinder.find(probe(30, "BAD_ROAD", "x"),
				Collections.singletonList(zone(1, 50, 30, "BAD_ROAD", "y")));
		assertEquals(1, found.size());
		assertTrue(found.get(0).overlap);
		assertEquals(50.0, found.get(0).distanceM, 0.5);
	}

	@Test
	void nearButNotOverlappingWithinTheBufferIsACandidateButRankedLower() {
		HazardRow overlapping = zone(1, 50, 30, "BAD_ROAD", "a");
		HazardRow near = zone(2, 80, 30, "BAD_ROAD", "b");        // 20 m gap
		List<HazardDuplicateFinder.Candidate> found = HazardDuplicateFinder.find(probe(30, "BAD_ROAD", "z"),
				Arrays.asList(near, overlapping));
		assertEquals(2, found.size());
		assertEquals(1, found.get(0).row.getId());
		assertFalse(found.get(1).overlap);
	}

	@Test
	void farZonesAreNotCandidates() {
		assertTrue(HazardDuplicateFinder.find(probe(30, "BAD_ROAD", "x"),
				Collections.singletonList(zone(1, 130, 30, "BAD_ROAD", "x"))).isEmpty()); // 70 m gap > 30 m buffer
	}

	@Test
	void theZoneItselfIsLeftOut() {
		HazardDuplicateFinder.Probe self = new HazardDuplicateFinder.Probe(LAT, LON, 30, "BAD_ROAD", "x", "", 1);
		assertTrue(HazardDuplicateFinder.find(self, Collections.singletonList(zone(1, 0, 30, "BAD_ROAD", "x"))).isEmpty());
	}

	@Test
	void sameTypeAndSimilarTextRankHigher() {
		HazardRow same = zone(1, 40, 30, "BAD_ROAD", "대구 수성교 아래 움푹 파인 길");
		HazardRow otherType = zone(2, 40, 30, "TRAFFIC", "전혀 다른 내용입니다");
		List<HazardDuplicateFinder.Candidate> found = HazardDuplicateFinder.find(
				probe(30, "BAD_ROAD", "수성교 아래 움푹 파인 길"), Arrays.asList(otherType, same));
		assertEquals(1, found.get(0).row.getId());
		assertTrue(found.get(0).sameType);
		assertTrue(found.get(0).similarity > 0.3);
		assertFalse(found.get(1).sameType);
		assertTrue(found.get(0).score > found.get(1).score);
	}

	@Test
	void aDifferentTypeIsStillShownAndSaysSo() {
		List<HazardDuplicateFinder.Candidate> found = HazardDuplicateFinder.find(probe(30, "BAD_ROAD", "x"),
				Collections.singletonList(zone(1, 20, 30, "SLIPPERY", "y")));
		assertEquals(1, found.size());
		boolean mentionsType = false;
		for (String reason : found.get(0).reasons) mentionsType |= reason.contains("유형이 다릅니다");
		assertTrue(mentionsType);
	}

	@Test
	void aCircleInsideAnotherIsRecognised() {
		List<HazardDuplicateFinder.Candidate> found = HazardDuplicateFinder.find(probe(100, "OTHER", "x"),
				Collections.singletonList(zone(1, 20, 30, "OTHER", "y")));
		boolean contained = false;
		for (String reason : found.get(0).reasons) contained |= reason.contains("포함");
		assertTrue(contained);
	}

	@Test
	void zonesWithoutCoordinatesAreSkipped() {
		HazardRow broken = zone(1, 0, 30, "OTHER", "x");
		broken.setLatitude(null);
		List<HazardRow> zones = new ArrayList<HazardRow>(Collections.singletonList(broken));
		assertTrue(HazardDuplicateFinder.find(probe(30, "OTHER", "x"), zones).isEmpty());
	}

	@Test
	void textSimilarityBehaves() {
		assertEquals(1.0, HazardDuplicateFinder.similarity("움푹 파인 길", "움푹파인길"), 1e-9);
		assertEquals(0.0, HazardDuplicateFinder.similarity("", "움푹"), 1e-9);
		assertTrue(HazardDuplicateFinder.similarity("급경사 내리막", "차량 많음") < 0.2);
	}
}
