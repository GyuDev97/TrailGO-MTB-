package com.bike.webapp.hazard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bike.webapp.dto.HazardRow;

/**
 * Finds zones that may be the same hazard as a given one. It only ranks and explains candidates; it never
 * decides anything: whether to merge is the operator's call.
 *
 * <p>A zone is a candidate when the two circles overlap, or come within {@link #NEAR_BUFFER_M} of each other.
 * The score (higher = more likely the same hazard) adds up: overlapping circles 50, merely near 25, one circle
 * inside the other +10, same type +25, similar title/description up to +25.</p>
 */
public final class HazardDuplicateFinder {

	/** Circles whose edges are this close count as "near" even when they do not overlap. */
	public static final double NEAR_BUFFER_M = 30.0;

	/** What to look for. {@code excludeId} is the zone itself (0 when it is not stored yet). */
	public static final class Probe {
		public final double latitude;
		public final double longitude;
		public final int radiusM;
		public final String type;
		public final String text;
		public final int excludeId;

		public Probe(double latitude, double longitude, int radiusM, String type, String title, String description,
				int excludeId) {
			this.latitude = latitude;
			this.longitude = longitude;
			this.radiusM = radiusM;
			this.type = type;
			this.text = (title == null ? "" : title) + " " + (description == null ? "" : description);
			this.excludeId = excludeId;
		}
	}

	public static final class Candidate {
		public final HazardRow row;
		public final double distanceM;
		public final boolean overlap;
		public final boolean sameType;
		public final double similarity;
		public final int score;
		public final List<String> reasons;

		Candidate(HazardRow row, double distanceM, boolean overlap, boolean sameType, double similarity, int score,
				List<String> reasons) {
			this.row = row;
			this.distanceM = distanceM;
			this.overlap = overlap;
			this.sameType = sameType;
			this.similarity = similarity;
			this.score = score;
			this.reasons = reasons;
		}
	}

	private HazardDuplicateFinder() {
	}

	/** @param zones active zones to compare with (the caller already left out hidden, rejected and merged ones) */
	public static List<Candidate> find(Probe probe, List<HazardRow> zones) {
		List<Candidate> found = new ArrayList<Candidate>();
		for (HazardRow zone : zones) {
			if (zone.getId() == probe.excludeId || zone.getLatitude() == null || zone.getLongitude() == null) continue;
			double distance = HazardGeo.distanceMeters(probe.latitude, probe.longitude,
					zone.getLatitude(), zone.getLongitude());
			int otherRadius = zone.getRadiusM() > 0 ? zone.getRadiusM() : 30;
			boolean overlap = distance <= probe.radiusM + otherRadius;
			if (!overlap && distance > probe.radiusM + otherRadius + NEAR_BUFFER_M) continue;

			List<String> reasons = new ArrayList<String>();
			int score = 0;
			if (overlap) {
				score += 50;
				reasons.add("위험 반경이 겹칩니다 (중심 간 " + Math.round(distance) + "m)");
			} else {
				score += 25;
				reasons.add("바로 인접해 있습니다 (반경 가장자리 간 "
						+ Math.round(distance - probe.radiusM - otherRadius) + "m)");
			}
			if (distance <= Math.abs(probe.radiusM - otherRadius)) {
				score += 10;
				reasons.add("한쪽 반경이 다른 쪽을 완전히 포함합니다");
			}
			boolean sameType = sameType(probe.type, zone.getHazardType());
			if (sameType) {
				score += 25;
				reasons.add("같은 위험 유형입니다");
			} else {
				reasons.add("위험 유형이 다릅니다 (" + HazardLabels.type(zone.getHazardType()) + ")");
			}
			double similarity = similarity(probe.text,
					(zone.getTitle() == null ? "" : zone.getTitle()) + " " + (zone.getDescription() == null ? "" : zone.getDescription()));
			if (similarity >= 0.2) {
				score += (int) Math.round(similarity * 25);
				reasons.add("제목·설명이 비슷합니다 (" + Math.round(similarity * 100) + "%)");
			}
			found.add(new Candidate(zone, distance, overlap, sameType, similarity, score, reasons));
		}
		Collections.sort(found, new Comparator<Candidate>() {
			@Override
			public int compare(Candidate a, Candidate b) {
				if (a.score != b.score) return b.score - a.score;
				return Double.compare(a.distanceM, b.distanceM);
			}
		});
		return found;
	}

	static boolean sameType(String a, String b) {
		return a != null && a.equals(b == null ? "OTHER" : b);
	}

	/** Jaccard similarity of character bigrams (works for Korean without word splitting); 0..1. */
	static double similarity(String a, String b) {
		Set<String> x = bigrams(a);
		Set<String> y = bigrams(b);
		if (x.isEmpty() || y.isEmpty()) return 0.0;
		Set<String> common = new HashSet<String>(x);
		common.retainAll(y);
		Set<String> all = new HashSet<String>(x);
		all.addAll(y);
		return (double) common.size() / all.size();
	}

	private static Set<String> bigrams(String text) {
		StringBuilder clean = new StringBuilder();
		if (text != null) {
			for (char c : text.toLowerCase().toCharArray()) {
				if (Character.isLetterOrDigit(c)) clean.append(c);
			}
		}
		Set<String> grams = new HashSet<String>();
		for (int i = 0; i + 1 < clean.length(); i++) grams.add(clean.substring(i, i + 2));
		return grams;
	}
}
