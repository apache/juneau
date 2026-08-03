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
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * End-to-end coverage for the {@code 2026-07-28} Multi-Round-Trip Request (SEP-2322 {@code input_required}) loop,
 * driven through the first-party {@link McpClient} against a live embedded-Jetty v2
 * {@link org.apache.juneau.rest.server.mcp.v20260728.McpRestServlet} fixture &mdash; proving PAUSE&rarr;RESUME&rarr;
 * complete works across a real client/server wire boundary, not just the in-process dispatcher assertions the
 * module-local {@code McpMrtrDispatch_Test} already covers.
 *
 * <p>
 * Placed in the {@code rest.client.mcp.v20260728} package (beside {@link McpClientV20260728_Integration_Test},
 * the canonical real-client/real-server fixture pattern) rather than the server package, because Task 15 mandates
 * driving the loop through the real client stack; the server-package integration tests use raw {@code MockRestClient}
 * bodies instead.
 *
 * <p>
 * <b>Client resume path:</b> both the pausing and the resuming calls go through {@link McpClient#callRaw} &mdash;
 * the typed {@code callTool} overload can neither represent an {@code input_required} result nor set
 * {@code requestState}/{@code inputResponses} on the follow-up request. Ergonomic client-side MRTR-resume support
 * (auto-detect + re-issue) is intentionally out of scope and remains a separate follow-up; here the test drives
 * the loop explicitly, exactly as the plan's Task 15 scopes it.
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class McpMrtrIntegration_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Fixture server: a v2 MCP servlet whose tools pause via McpInputRequiredSignal and resume via the injected
	// McpMrtrResumeContext. Holds the exact AeadRequestStateCodec instance the dispatcher seals with, so a02 can
	// unseal the emitted requestState without re-deriving a key (the ephemeral AES key lives inside this instance).
	// -----------------------------------------------------------------------------------------------------------------

	static final AeadRequestStateCodec CODEC = new AeadRequestStateCodec();

	private static String aad(String method) {
		return method + '\u0000' + McpProtocol.VERSION_2026_07_28;
	}

	private static McpToolHandler ask() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("ask").setDescription("Pauses once, then completes"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var resume = ctx.getBean(McpMrtrResumeContext.class);
				if (resume.isEmpty())
					throw new McpInputRequiredSignal(Map.of("q1", Map.of("type", "elicitation")), "cont-1");
				return McpToolOutcome.text("resumed:" + resume.get().inputResponses().get("q1"));
			}
		};
	}

	private static McpToolHandler askTwice() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("askTwice").setDescription("Pauses twice"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) {
				var resume = ctx.getBean(McpMrtrResumeContext.class);
				if (resume.isEmpty())
					throw new McpInputRequiredSignal(Map.of("q1", Map.of("type", "elicitation")), "cont-1");
				if ("cont-1".equals(resume.get().continuation()))
					throw new McpInputRequiredSignal(Map.of("q2", Map.of("type", "elicitation")), "cont-2");
				return McpToolOutcome.text("resumed2:" + resume.get().inputResponses().get("q2"));
			}
		};
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Fixture extends org.apache.juneau.rest.server.mcp.v20260728.McpRestServlet {
		private static final long serialVersionUID = 1L;

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("it-mrtr-fixture").setVersion("1.0.0").addTool(ask()).addTool(askTwice());
		}

		@Override
		protected McpMrtrConfig createMrtrConfig() {
			return new McpMrtrConfig().setCodec(CODEC);
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
		return McpClient.builder()
			.endpoint(fixture.getRootUrl() + "/")
			.clientCapabilities(caps);
	}

	// =================================================================================================================
	// A: full PAUSE -> RESUME -> complete loop across the real wire.
	// =================================================================================================================

	@Test void a01_pauseResumeComplete_fullLoop() throws Exception {
		try (var client = clientBuilder(true).build()) {
			// First call pauses: expect an input_required result carrying a requestState and one input request.
			var paused = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("ask").setArguments(Map.of()));
			assertEquals("input_required", paused.get("resultType"));
			var token = (String) paused.get("requestState");
			assertNotNull(token);
			assertTrue(((Map<?,?>) paused.get("inputRequests")).containsKey("q1"));

			// Resume with the echoed requestState + the client's answers: expect completion echoing the answer.
			var completed = client.callRaw(McpMethods.TOOLS_CALL,
				new CallToolRequest().setName("ask").setRequestState(token).setInputResponses(Map.of("q1", "answer")));
			assertEquals("complete", completed.get("resultType"));
			var result = Json.to(Json.of(completed), CallToolResult.class);
			assertEquals("resumed:answer", ((TextContent) result.getContent().get(0)).getText());
		}
	}

	@Test void a02_pauseResumeToPauseAgain_secondRoundIncrementsCount() throws Exception {
		try (var client = clientBuilder(true).build()) {
			var paused1 = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("askTwice").setArguments(Map.of()));
			assertEquals("input_required", paused1.get("resultType"));
			var token1 = (String) paused1.get("requestState");

			// Resume once; the handler pauses again -> a second input_required with an incremented round counter.
			var paused2 = client.callRaw(McpMethods.TOOLS_CALL,
				new CallToolRequest().setName("askTwice").setRequestState(token1).setInputResponses(Map.of("q1", "a1")));
			assertEquals("input_required", paused2.get("resultType"));
			var token2 = (String) paused2.get("requestState");

			// Unseal via the same codec instance the server used (round counter lives inside the sealed token).
			var state = CODEC.unseal(token2, aad(McpMethods.TOOLS_CALL)).orElseThrow();
			assertEquals(2, state.round());
			assertEquals("cont-2", state.continuation());
		}
	}

	// =================================================================================================================
	// B: capability gate + negative path, enforced at the real HTTP boundary.
	// =================================================================================================================

	@Test void b01_unsupportedCapability_rejectedOverTheWire() throws Exception {
		try (var client = clientBuilder(false).build()) {
			var e = assertThrows(McpException.class,
				() -> client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("ask").setArguments(Map.of())));
			assertEquals(org.apache.juneau.rest.server.mcp.v20260728.McpRevision.CODE_MISSING_REQUIRED_CLIENT_CAPABILITY, e.getCode());
		}
	}

	@Test void b02_tamperedRequestState_rejectedOverTheWire() throws Exception {
		try (var client = clientBuilder(true).build()) {
			var paused = client.callRaw(McpMethods.TOOLS_CALL, new CallToolRequest().setName("ask").setArguments(Map.of()));
			var token = (String) paused.get("requestState");
			// Flip one ciphertext byte to corrupt the AEAD tag; a final-character flip can land on an unpadded
			// base64url "don't-care" low bit and decode to identical bytes, leaving the tag (and test) flaky.
			var parts = token.split("\\.", 2);
			var ciphertext = Base64.getUrlDecoder().decode(parts[1]);
			ciphertext[0] ^= 1;
			var tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
			var e = assertThrows(McpException.class,
				() -> client.callRaw(McpMethods.TOOLS_CALL,
					new CallToolRequest().setName("ask").setRequestState(tampered).setInputResponses(Map.of("q1", "answer"))));
			assertEquals(org.apache.juneau.rest.server.mcp.v20260728.McpRevision.CODE_INVALID_PARAMS, e.getCode());
		}
	}
}
