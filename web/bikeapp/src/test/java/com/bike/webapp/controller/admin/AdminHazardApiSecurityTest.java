package com.bike.webapp.controller.admin;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.bike.webapp.config.SecurityConfig;
import com.bike.webapp.dto.AdminHazardRequest;
import com.bike.webapp.dto.HazardMergeRequest;
import com.bike.webapp.dto.HazardReview;
import com.bike.webapp.hazard.HazardException;
import com.bike.webapp.service.HazardAdminService;

/** The real SecurityConfig in front of the admin hazard API: who may call it, and the error shape. */
@SpringJUnitWebConfig(AdminHazardApiSecurityTest.Config.class)
class AdminHazardApiSecurityTest {

	@Configuration
	@EnableWebMvc
	@Import(SecurityConfig.class)
	static class Config {
		@Bean
		UserDetailsService userDetailsService() {
			return mock(UserDetailsService.class);
		}

		@Bean
		HazardAdminService hazardAdminService() {
			return mock(HazardAdminService.class);
		}

		@Bean
		AdminHazardApiController adminHazardApiController(HazardAdminService service) {
			return new AdminHazardApiController(service);
		}
	}

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private HazardAdminService service;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		reset(service);
		mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
		when(service.findById(1)).thenReturn(new HazardReview());
	}

	@Test
	void nobodyWhoIsNotSignedInCanChangeAHazard() throws Exception {
		mvc.perform(post("/admin/api/hazards/1/approve").with(csrf())).andExpect(status().is3xxRedirection());
		mvc.perform(post("/admin/api/hazards").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().is3xxRedirection());
		mvc.perform(post("/admin/api/hazards/merge").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().is3xxRedirection());
		verifyNoInteractions(service);
	}

	@Test
	void aSignedInNonAdminIsForbidden() throws Exception {
		mvc.perform(post("/admin/api/hazards/1/approve").with(user("rider").roles("USER")).with(csrf()))
				.andExpect(status().isForbidden());
		mvc.perform(post("/admin/api/hazards").with(user("rider").roles("USER")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
		mvc.perform(get("/admin/api/hazards/merges").with(user("rider").roles("USER"))).andExpect(status().isForbidden());
		verifyNoInteractions(service);
	}

	@Test
	void anAdminWithoutACsrfTokenIsRefused() throws Exception {
		mvc.perform(post("/admin/api/hazards/1/approve").with(user("root").roles("ADMIN")))
				.andExpect(status().isForbidden());
		verify(service, never()).approve(1, "root");
	}

	@Test
	void anAdminActsUnderTheirOwnLoginName() throws Exception {
		mvc.perform(post("/admin/api/hazards/1/approve").with(user("root").roles("ADMIN")).with(csrf()))
				.andExpect(status().isOk());
		verify(service).approve(1, "root");
	}

	@Test
	void anAdminCanRegisterAHazard() throws Exception {
		HazardReview created = new HazardReview();
		created.setId(30);
		when(service.createByAdmin(any(AdminHazardRequest.class), eq("root"))).thenReturn(created);
		mvc.perform(post("/admin/api/hazards").with(user("root").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"latitude\":37.5,\"longitude\":127.0,\"type\":\"BAD_ROAD\",\"title\":\"t\",\"radiusM\":30}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.hazard.id").value(30));
	}

	@Test
	void refusalsAndBadInputUseTheStandardErrorShape() throws Exception {
		doThrow(new HazardException(HttpStatus.CONFLICT, "ALREADY_IN_STATUS", "이미 '승인됨' 상태입니다."))
				.when(service).approve(1, "root");
		mvc.perform(post("/admin/api/hazards/1/approve").with(user("root").roles("ADMIN")).with(csrf()))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ALREADY_IN_STATUS"));

		when(service.merge(any(HazardMergeRequest.class), eq("root"))).thenThrow(new IllegalArgumentException("대표 위험지역을 선택해 주세요."));
		mvc.perform(post("/admin/api/hazards/merge").with(user("root").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

		mvc.perform(patch("/admin/api/hazards/1").with(user("root").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{broken"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void aRaceOnTheUniqueKeyIsAConflictNotAServerError() throws Exception {
		when(service.merge(any(HazardMergeRequest.class), eq("root")))
				.thenThrow(new org.springframework.dao.DuplicateKeyException("uk_hazard_merge_merged"));
		mvc.perform(post("/admin/api/hazards/merge").with(user("root").roles("ADMIN")).with(csrf())
				.contentType(MediaType.APPLICATION_JSON).content("{\"canonicalId\":1,\"sourceIds\":[2]}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("CONFLICT"));
	}
}
