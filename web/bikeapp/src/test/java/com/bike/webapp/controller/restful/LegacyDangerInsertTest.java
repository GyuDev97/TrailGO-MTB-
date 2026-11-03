package com.bike.webapp.controller.restful;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.bike.webapp.auth.AppIdentity;
import com.bike.webapp.dto.DangerousArea;
import com.bike.webapp.service.AndroidService;

/** The old /app/insertDanger no longer trusts the rider named in the body. */
class LegacyDangerInsertTest {

	private static final String BODY = "{\"da_rider\":\"admin\",\"da_latitude\":\"37.5\",\"da_longitude\":\"127.0\",\"da_content\":\"x\"}";

	private AndroidService androidService;
	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		androidService = mock(AndroidService.class);
		AndroidController controller = new AndroidController();
		ReflectionTestUtils.setField(controller, "androidService", androidService);
		mvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void guestsAreRefused() throws Exception {
		mvc.perform(post("/app/insertDanger").contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isUnauthorized());
		verify(androidService, never()).insertDanger(any(DangerousArea.class));
	}

	@Test
	void theReporterIsTheSignedInRiderNotTheBodyField() throws Exception {
		mvc.perform(post("/app/insertDanger").requestAttr(AppIdentity.ATTRIBUTE, "rider1")
				.contentType(MediaType.APPLICATION_JSON).content(BODY))
				.andExpect(status().isOk());
		ArgumentCaptor<DangerousArea> saved = ArgumentCaptor.forClass(DangerousArea.class);
		verify(androidService).insertDanger(saved.capture());
		org.junit.jupiter.api.Assertions.assertEquals("rider1", saved.getValue().getDa_rider());
	}
}
