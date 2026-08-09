/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.rest.client.mcp.v20260728;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.*;
import org.apache.juneau.rest.server.mcp.v20260728.*;
// Single-type import needed to disambiguate from org.apache.juneau.rest.server.mcp.McpRevision (the
// revision-neutral interface), which the wildcard import above collides with.
import org.apache.juneau.rest.server.mcp.v20260728.McpRevision;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * End-to-end coverage for MCP {@code 2026-07-28} SEP-2322 elicitation (C6), driven through the real
 * {@link McpClient} and its client-side {@link ElicitationRequests}/{@link ElicitationResponses} typed helpers
 * against a live embedded-Jetty server whose tools pause/resume via the server-side (same-named)
 * {@code ElicitationRequests}/{@code ElicitationResponses} &mdash; proving the full typed path works across a
 * real wire boundary, complementing {@link McpMrtrIntegration_Test}'s generic (untyped) PAUSE/RESUME coverage
 * and Phase 7's {@code Characterization_Test} in-process replay fixtures.
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class McpElicitationIntegration_Test extends TestBase {

	private static ElicitRequest confirmQuestion() {
		return new ElicitRequest()
			.setMessage("Proceed with deletion?")
			.setRequestedSchema(ElicitSchema.create().booleanField("confirm").title("Confirm").build());
	}

	// The server-side org.apache.juneau.rest.server.mcp.v20260728.ElicitationRequests/ElicitationResponses
	// (wildcard-imported above) share their simple names with this test's own client-side package
	// (org.apache.juneau.rest.client.mcp.v20260728.ElicitationRequests/ElicitationResponses), where same-package
	// visibility wins over a wildcard import - so every server-side call below is qualified with its full
	// package to disambiguate, while every client-side call in the test bodies below stays unqualified.
	private static McpToolHandler confirm() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("confirm").setDescription("Pauses for one confirmation"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var resume = ctx.getBean(McpMrtrResumeContext.class);
				if (resume.isEmpty())
					throw org.apache.juneau.rest.server.mcp.v20260728.ElicitationRequests.of("confirm", confirmQuestion(), "cont-1");
				var answer = org.apache.juneau.rest.server.mcp.v20260728.ElicitationResponses.get(resume.get(), "confirm");
				// Echoes the answer's content (not just its action) and the continuation this tool paused
				// with, so the assertions below actually exercise ElicitResult.content marshalling and prove
				// the continuation round-trips through McpMrtrResumeContext, instead of only the action enum.
				var confirmValue = answer.getContent() == null ? null : answer.getContent().get("confirm");
				return McpToolOutcome.text("action:" + answer.getAction() + ",confirm:" + confirmValue
					+ ",cont:" + resume.get().continuation());
			}
		};
	}

	private static McpToolHandler confirmTwo() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("confirmTwo").setDescription("Pauses for two confirmations in one round"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var resume = ctx.getBean(McpMrtrResumeContext.class);
				if (resume.isEmpty()) {
					var requests = new LinkedHashMap<String,ElicitRequest>();
					requests.put("confirm", confirmQuestion());
					requests.put("reason", new ElicitRequest().setMessage("Why?")
						.setRequestedSchema(ElicitSchema.create().stringField("reason").build()));
					throw org.apache.juneau.rest.server.mcp.v20260728.ElicitationRequests.of(requests, "cont-multi");
				}
				var answers = org.apache.juneau.rest.server.mcp.v20260728.ElicitationResponses.all(resume.get());
				var confirmAnswer = answers.get("confirm");
				var reasonAnswer = answers.get("reason");
				var confirmValue = confirmAnswer.getContent() == null ? null : confirmAnswer.getContent().get("confirm");
				var reasonValue = reasonAnswer.getContent() == null ? null : reasonAnswer.getContent().get("reason");
				return McpToolOutcome.text("confirm:" + confirmAnswer.getAction() + "=" + confirmValue
					+ ",reason:" + reasonAnswer.getAction() + "=" + reasonValue
					+ ",cont:" + resume.get().continuation());
			}
		};
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Fixture extends org.apache.juneau.rest.server.mcp.v20260728.McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("it-elicit-fixture").setVersion("1.0.0")
				.addTool(confirm()).addTool(confirmTwo());
		}
	}

	@Configuration
	public static class FixtureConfig {
		@Bean public Servlet mcpServlet() { return new Fixture(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(FixtureConfig.class);

	private static McpClient.Builder clientBuilder(boolean withElicitation) {
		var caps = new ClientCapabilities();
		if (withElicitation)
			caps.setElicitation(new ElicitationCapability());
		return McpClient.builder().endpoint(fixture.getRootUrl() + "/").clientCapabilities(caps);
	}

	/**
	 * Decodes {@code raw} (a {@link McpClient#callRaw} "complete" result) into a {@link CallToolResult} and
	 * returns its first content block's text, replacing the raw {@code List}/{@code Map} cast chains every test
	 * below previously repeated inline.
	 */
	private static String firstText(Map<String,Object> raw) {
		var result = Json.to(Json.of(raw), CallToolResult.class);
		return ((TextContent) result.getContent().get(0)).getText();
	}

	// =================================================================================================================
	// A: single-question pause -> resume -> complete, driven entirely through the client-side ElicitationRequests/ElicitationResponses.
	// =================================================================================================================

	@Test void a01_singleQuestion_pauseResumeAccept_fullLoop() throws Exception {
		try (var client = clientBuilder(true).build()) {
			var paused = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirm").setArguments(Map.of()));
			assertTrue(ElicitationRequests.isInputRequired(paused));
			var requests = ElicitationRequests.requests(paused);
			assertEquals("Proceed with deletion?", requests.get("confirm").getMessage());
			// The requestedSchema (built via ElicitSchema.booleanField("confirm").title("Confirm")) must survive
			// the wire intact, not just the message - otherwise a real client could not render the right input
			// control for the end user.
			var confirmProperty = (Map<?,?>) ((Map<?,?>) requests.get("confirm").getRequestedSchema().get("properties")).get("confirm");
			assertBean(confirmProperty, "type,title", "boolean,Confirm");

			var answer = new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("confirm", true);
			var completed = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirm")
				.setRequestState(ElicitationRequests.requestState(paused))
				.setInputResponses(ElicitationResponses.toInputResponse("confirm", answer)));
			assertFalse(ElicitationRequests.isInputRequired(completed));
			assertEquals("complete", completed.get("resultType"));
			assertEquals("action:accept,confirm:true,cont:cont-1", firstText(completed));
		}
	}

	// =================================================================================================================
	// B: multi-question pause -> resume with both answers in one round.
	// =================================================================================================================

	@Test void b01_multiQuestion_pauseResumeBoth_singleRoundTrip() throws Exception {
		try (var client = clientBuilder(true).build()) {
			var paused = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirmTwo").setArguments(Map.of()));
			var requests = ElicitationRequests.requests(paused);
			assertEquals(2, requests.size());
			assertTrue(requests.containsKey("confirm"));
			assertTrue(requests.containsKey("reason"));

			var answers = new LinkedHashMap<String,ElicitResult>();
			answers.put("confirm", new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("confirm", true));
			answers.put("reason", new ElicitResult().setAction(ElicitAction.ACCEPT).putContent("reason", "cleanup"));
			var completed = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirmTwo")
				.setRequestState(ElicitationRequests.requestState(paused))
				.setInputResponses(ElicitationResponses.toInputResponses(answers)));
			assertEquals("complete", completed.get("resultType"));
			assertEquals("confirm:accept=true,reason:accept=cleanup,cont:cont-multi", firstText(completed));
		}
	}

	// =================================================================================================================
	// C: decline and cancel resumes both complete, carrying the correct ElicitAction to the handler.
	// =================================================================================================================

	@Test void c01_decline_resumeCarriesDeclineAction() throws Exception {
		try (var client = clientBuilder(true).build()) {
			var paused = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirm").setArguments(Map.of()));
			assertTrue(ElicitationRequests.isInputRequired(paused));
			assertNotNull(ElicitationRequests.requestState(paused));
			var answer = new ElicitResult().setAction(ElicitAction.DECLINE);
			var completed = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirm")
				.setRequestState(ElicitationRequests.requestState(paused))
				.setInputResponses(ElicitationResponses.toInputResponse("confirm", answer)));
			assertEquals("complete", completed.get("resultType"));
			assertEquals("action:decline,confirm:null,cont:cont-1", firstText(completed));
		}
	}

	@Test void c02_cancel_resumeCarriesCancelAction() throws Exception {
		try (var client = clientBuilder(true).build()) {
			var paused = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirm").setArguments(Map.of()));
			assertTrue(ElicitationRequests.isInputRequired(paused));
			assertNotNull(ElicitationRequests.requestState(paused));
			var answer = new ElicitResult().setAction(ElicitAction.CANCEL);
			var completed = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("confirm")
				.setRequestState(ElicitationRequests.requestState(paused))
				.setInputResponses(ElicitationResponses.toInputResponse("confirm", answer)));
			assertEquals("complete", completed.get("resultType"));
			assertEquals("action:cancel,confirm:null,cont:cont-1", firstText(completed));
		}
	}

	// =================================================================================================================
	// D: capability gate still fires unmodified when a typed-helper pause is thrown by an unsupported client.
	// =================================================================================================================

	/**
	 * Delta vs {@link McpMrtrIntegration_Test#b01_unsupportedCapability_rejectedOverTheWire()}: proves the same
	 * capability gate fires for a signal built via the typed server-side {@code ElicitationRequests.of(...)} helper (not a
	 * hand-built {@code McpInputRequiredSignal}) — i.e. the helper yields a gate-recognized signal.
	 */
	@Test void d01_unsupportedCapability_typedHelperPauseStillRejectedOverTheWire() throws Exception {
		try (var client = clientBuilder(false).build()) {
			var request = new CallToolRequest().setName("confirm").setArguments(Map.of());
			var e = assertThrows(McpException.class, () -> client.callRaw(McpMethods.TOOLS_CALL, request));
			assertEquals(McpRevision.CODE_MISSING_REQUIRED_CLIENT_CAPABILITY, e.getCode());
		}
	}
}
