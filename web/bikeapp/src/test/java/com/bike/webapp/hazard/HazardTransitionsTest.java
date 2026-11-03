package com.bike.webapp.hazard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.bike.webapp.dto.HazardReview;

/** The status policy and the buttons derived from it (the approve-button bug: buttons ignored the status). */
class HazardTransitionsTest {

	private static HazardReview zone(String status, Integer mergedInto) {
		HazardReview zone = new HazardReview();
		zone.setId(1);
		zone.setStatus(status);
		zone.setMergedInto(mergedInto);
		return zone;
	}

	@Test
	void pendingAndReviewingOfferApproveRejectAndHide() {
		for (String status : new String[] { "PENDING", "REVIEWING" }) {
			HazardReview zone = zone(status, null);
			assertTrue(zone.isCanApprove(), status);
			assertTrue(zone.isCanReject(), status);
			assertTrue(zone.isCanHide(), status);
			assertFalse(zone.isCanRestore(), status);
		}
	}

	@Test
	void anApprovedZoneOffersNeitherApproveNorReject() {
		HazardReview zone = zone("APPROVED", null);
		assertFalse(zone.isCanApprove());
		assertFalse(zone.isCanReject());
		assertTrue(zone.isCanHide());
		assertEquals("승인됨", zone.getStatusLabel());
	}

	@Test
	void aRejectedZoneIsLabelledAndCanOnlyBeReopenedOrHidden() {
		HazardReview zone = zone("REJECTED", null);
		assertFalse(zone.isCanApprove());
		assertFalse(zone.isCanReject());
		assertTrue(zone.isCanRestore());
		assertEquals("거절됨", zone.getStatusLabel());
	}

	@Test
	void aHiddenZoneIsLabelledAndCanOnlyBeShownAgain() {
		HazardReview zone = zone("INACTIVE", null);
		assertFalse(zone.isCanApprove());
		assertFalse(zone.isCanHide());
		assertTrue(zone.isCanRestore());
		assertEquals("숨김", zone.getStatusLabel());
	}

	@Test
	void aMergedZoneOffersNothingAndCannotBeEdited() {
		for (HazardReview zone : new HazardReview[] { zone("MERGED", 7), zone("INACTIVE", 7), zone("APPROVED", 7) }) {
			assertFalse(zone.isCanApprove());
			assertFalse(zone.isCanReject());
			assertFalse(zone.isCanHide());
			assertFalse(zone.isCanRestore());
			assertFalse(zone.isCanEdit());
			assertEquals("병합됨", zone.getStatusLabel());
		}
	}

	@Test
	void theTableForbidsRepeatingOrSkipping() {
		assertFalse(HazardTransitions.canMove("APPROVED", "APPROVED"));
		assertFalse(HazardTransitions.canMove("APPROVED", "REJECTED"));
		assertFalse(HazardTransitions.canMove("REJECTED", "APPROVED"));
		assertFalse(HazardTransitions.canMove("MERGED", "PENDING"));
		assertFalse(HazardTransitions.canMove("NOPE", "PENDING"));
		assertTrue(HazardTransitions.canMove("PENDING", "APPROVED"));
		assertTrue(HazardTransitions.canMove("APPROVED", "INACTIVE"));
		assertTrue(HazardTransitions.canMove("INACTIVE", "PENDING"));
	}

	@Test
	void ridersSeeOnlyPendingReviewingAndApproved() {
		assertTrue(HazardTransitions.isVisibleToRiders("PENDING"));
		assertTrue(HazardTransitions.isVisibleToRiders("REVIEWING"));
		assertTrue(HazardTransitions.isVisibleToRiders("APPROVED"));
		for (String status : new String[] { "REJECTED", "INACTIVE", "EXPIRED", "MERGED" }) {
			assertFalse(HazardTransitions.isVisibleToRiders(status), status);
		}
	}

	@Test
	void labelsCoverTheAppTypesAndTheOlderOnes() {
		assertEquals("노면 불량", HazardLabels.type("BAD_ROAD"));
		assertEquals("급커브", HazardLabels.type("SHARP_CURVE"));
		assertEquals("낙석", HazardLabels.type("ROCKFALL"));
		assertEquals("기타 위험", HazardLabels.type(null));
	}
}
