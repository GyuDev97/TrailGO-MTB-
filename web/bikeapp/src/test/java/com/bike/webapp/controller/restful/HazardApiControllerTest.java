package com.bike.webapp.controller.restful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Timestamp;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bike.webapp.auth.AppIdentity;
import com.bike.webapp.dto.HazardRow;
import com.bike.webapp.mapper.HazardAdminMapper;
import com.bike.webapp.mapper.HazardAppMapper;
import com.bike.webapp.service.HazardAppService;

/** HTTP contract of /app/hazards: status codes, the error shape, who may do what. */
class HazardApiControllerTest {

	private static final String BODY = "{\"latitude\":37.5,\"longitude\":127.0,\"radiusM\":40,\"type\":\"BAD_ROAD\","
			+ "\"title\":\"움푹 파인 길\",\"description\":\"위험\"}";

	private HazardAppMapper mapper;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		mapper = mock(HazardAppMapper.class);
		when(mapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Collections.<HazardRow>emptyList());
		HazardApiController controller = new HazardApiController(new HazardAppService(mapper, mock(HazardAdminMapper.class)));
		mvc = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new HazardApiAdvice()).build();
	}

	private static RequestPostProcessor rider(final String id) {
		return request -> {
			request.setAttribute(AppIdentity.ATTRIBUTE, id);
			return request;
		};
	}

	private static HazardRow stored(int id, String reporter, String status) {
		HazardRow row = new HazardRow();
		row.setId(id);
		row.setReporter(reporter);
		row.setLatitude(37.5);
		row.setLongitude(127.0);
		row.setRadiusM(40);
		row.setHazardType("BAD_ROAD");
		row.setStatus(status);
		row.setTitle("제목");
		row.setCreatedAt(new Timestamp(1000L));
		row.setUpdatedAt(new Timestamp(1000L));
		return row;
	}

	// ---- reading is open to guests -------------------------------------------------------------------

	@Test
	void guestsCanReadNearbyHazards() throws Exception {
		when(mapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Collections.singletonList(stored(3, "someone", "PENDING")));
		mvc.perform(get("/app/hazards/nearby").param("lat", "37.5").param("lon", "127.0").param("radius", "500"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.count").value(1))
				.andExpect(jsonPath("$.items[0].id").value(3))
				.andExpect(jsonPath("$.items[0].verified").value(false))
				.andExpect(jsonPath("$.items[0].mine").value(false))
				.andExpect(jsonPath("$.items[0].reporter").doesNotExist())
				.andExpect(jsonPath("$.truncated").value(false))
				.andExpect(jsonPath("$.removedIds").isArray());
	}

	@Test
	void nearbyErrorsUseTheStandardShape() throws Exception {
		mvc.perform(get("/app/hazards/nearby").param("lat", "37.5"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_COORDINATE"));
		mvc.perform(get("/app/hazards/nearby").param("lat", "37.5").param("lon", "127").param("radius", "999999"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_RADIUS"));
		mvc.perform(get("/app/hazards/nearby").param("lat", "abc").param("lon", "127"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@Test
	void detailOfAHiddenHazardIsNotFound() throws Exception {
		when(mapper.findById(8)).thenReturn(stored(8, "x", "INACTIVE"));
		mvc.perform(get("/app/hazards/8")).andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("NOT_FOUND"));
	}

	@Test
	void nearbyIsRateLimitedPerCaller() throws Exception {
		for (int i = 0; i < HazardApiController.READS_PER_WINDOW; i++) {
			mvc.perform(get("/app/hazards/nearby").param("lat", "37.5").param("lon", "127.0")).andExpect(status().isOk());
		}
		mvc.perform(get("/app/hazards/nearby").param("lat", "37.5").param("lon", "127.0"))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.code").value("TOO_MANY_REQUESTS"));
	}

	// ---- writing needs a signed-in rider -------------------------------------------------------------

	@Test
	void guestsCannotReportEditOrDelete() throws Exception {
		mvc.perform(post("/app/hazards").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("LOGIN_REQUIRED"));
		mvc.perform(put("/app/hazards/1").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized());
		mvc.perform(delete("/app/hazards/1")).andExpect(status().isUnauthorized());
		verify(mapper, never()).insertDanger(any(HazardRow.class), anyString(), anyString());
	}

	@Test
	void aSignedInRiderCanReport() throws Exception {
		org.mockito.Mockito.doAnswer(invocation -> {
			((HazardRow) invocation.getArgument(0)).setId(12);
			return 1;
		}).when(mapper).insertDanger(any(HazardRow.class), anyString(), anyString());
		when(mapper.findById(12)).thenReturn(stored(12, "rider1", "PENDING"));
		mvc.perform(post("/app/hazards").with(rider("rider1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.id").value(12))
				.andExpect(jsonPath("$.verified").value(false))
				.andExpect(jsonPath("$.mine").value(true));
	}

	@Test
	void theReporterInTheBodyIsIgnored() throws Exception {
		org.mockito.Mockito.doAnswer(invocation -> {
			((HazardRow) invocation.getArgument(0)).setId(12);
			return 1;
		}).when(mapper).insertDanger(any(HazardRow.class), anyString(), anyString());
		when(mapper.findById(12)).thenReturn(stored(12, "rider1", "PENDING"));
		String spoofed = BODY.replace("{", "{\"reporter\":\"admin\",\"da_rider\":\"admin\",");
		mvc.perform(post("/app/hazards").with(rider("rider1")).contentType(MediaType.APPLICATION_JSON).content(spoofed))
				.andExpect(status().isCreated());
		org.mockito.ArgumentCaptor<HazardRow> saved = org.mockito.ArgumentCaptor.forClass(HazardRow.class);
		verify(mapper).insertDanger(saved.capture(), anyString(), anyString());
		org.junit.jupiter.api.Assertions.assertEquals("rider1", saved.getValue().getReporter());
	}

	@Test
	void malformedBodiesAreBadRequests() throws Exception {
		mvc.perform(post("/app/hazards").with(rider("rider1")).contentType(MediaType.APPLICATION_JSON).content("{nope"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
		mvc.perform(post("/app/hazards").with(rider("rider1")).contentType(MediaType.APPLICATION_JSON)
				.content(BODY.replace("BAD_ROAD", "VOLCANO")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TYPE"));
	}

	@Test
	void duplicatesAreConflictsThatNameTheExistingHazard() throws Exception {
		when(mapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Collections.singletonList(stored(4, "x", "PENDING")));
		mvc.perform(post("/app/hazards").with(rider("rider1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("DUPLICATE_HAZARD"))
				.andExpect(jsonPath("$.hazardId").value(4));
	}

	@Test
	void reportingIsRateLimitedPerRider() throws Exception {
		when(mapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenReturn(Collections.singletonList(stored(4, "x", "PENDING")));
		for (int i = 0; i < HazardApiController.REPORTS_PER_HOUR; i++) {
			mvc.perform(post("/app/hazards").with(rider("rider1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
					.andExpect(status().isConflict());
		}
		mvc.perform(post("/app/hazards").with(rider("rider1")).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isTooManyRequests());
		// another rider has their own allowance
		mvc.perform(post("/app/hazards").with(rider("rider2")).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isConflict());
	}

	@Test
	void onlyTheOwnerMayEditOrWithdraw() throws Exception {
		when(mapper.findById(5)).thenReturn(stored(5, "owner", "PENDING"));
		mvc.perform(put("/app/hazards/5").with(rider("intruder")).contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.code").value("NOT_OWNER"));
		mvc.perform(delete("/app/hazards/5").with(rider("intruder"))).andExpect(status().isForbidden());
		verify(mapper, never()).deactivateReview(5);

		mvc.perform(delete("/app/hazards/5").with(rider("owner")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deleted").value(true));
		verify(mapper).deactivateReview(5);
	}

	@Test
	void unexpectedFailuresAreReportedWithoutInternals() throws Exception {
		when(mapper.findVisibleInBox(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
				.thenThrow(new IllegalStateException("jdbc:mariadb://secret-host/db password=hunter2"));
		mvc.perform(get("/app/hazards/nearby").param("lat", "37.5").param("lon", "127.0"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.code").value("SERVER_ERROR"))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("hunter2"))));
	}
}
