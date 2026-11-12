package com.Support.Ticket.Management.System;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.http.HttpHeaders;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ApplicationTests {
	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void allowsCorsPreflightFromVercelFrontend() throws Exception {
		mockMvc.perform(options("/api/auth/register")
				.header(HttpHeaders.ORIGIN, "https://support-ticket-management-system-drab.vercel.app")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
				.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
			.andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header()
				.string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
						"https://support-ticket-management-system-drab.vercel.app"));
	}

	@Test
	void usersCanOnlyViewTheirOwnTicketsAndCannotAssignAgents() throws Exception {
		String ownerToken = register("owner@example.com");
		String otherToken = register("other@example.com");
		mockMvc.perform(post("/api/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"owner@example.com\",\"password\":\"wrong-password\"}"))
			.andExpect(status().isUnauthorized());
		MvcResult created = mockMvc.perform(post("/api/tickets")
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"title\":\"Cannot sign in\",\"description\":\"Login page fails\",\"category\":\"Account access\",\"priority\":\"HIGH\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("OPEN"))
			.andReturn();
		Long ticketId = ((Number) JsonPath.read(created.getResponse().getContentAsString(), "$.id")).longValue();

		mockMvc.perform(get("/api/tickets/{id}", ticketId).header("Authorization", "Bearer " + ownerToken))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/tickets/{id}", ticketId).header("Authorization", "Bearer " + otherToken))
				.andExpect(status().isNotFound());
		mockMvc.perform(put("/api/tickets/{id}/assignment", ticketId)
				.header("Authorization", "Bearer " + ownerToken)
				.contentType(MediaType.APPLICATION_JSON).content("{\"agentId\":1}"))
			.andExpect(status().isForbidden());
	}

	private String register(String email) throws Exception {
		MvcResult result = mockMvc.perform(post("/api/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"name\":\"Test User\",\"email\":\"" + email + "\",\"password\":\"test-password-123\"}"))
			.andExpect(status().isOk())
			.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
	}

}
