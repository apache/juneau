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
package org.apache.juneau.rest.server.mcp.v20260728;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.mcp.McpCompletionResult;
import org.apache.juneau.rest.server.mcp.McpContentBlock;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpPromptArgument;
import org.apache.juneau.rest.server.mcp.McpPromptHandler;
import org.apache.juneau.rest.server.mcp.McpPromptOutcome;
import org.apache.juneau.rest.server.mcp.McpPromptSpec;
import org.apache.juneau.rest.server.mcp.McpResourceHandler;
import org.apache.juneau.rest.server.mcp.McpResourceOutcome;
import org.apache.juneau.rest.server.mcp.McpResourceSpec;
import org.apache.juneau.rest.server.mcp.McpResourceTemplateHandler;
import org.apache.juneau.rest.server.mcp.McpResourceTemplateSpec;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.junit.jupiter.api.*;

/**
 * Consolidated dispatcher coverage for the {@code 2026-07-28} MRTR (Multi-Round-Trip Request) PAUSE/RESUME flow
 * (spec &sect;2/&sect;4; plan Phase 3, Tasks 9-11): sentinel&rarr;seal&rarr;emit across all three in-scope methods,
 * RESUME&rarr;unseal&rarr;re-invoke happy path and loop, capability-gate, tamper/expiry/max-rounds/method-mismatch
 * rejection with exact codes, cache-hint suppression on PAUSE, exact-resource-only scoping, per-instance codec
 * injection, and a scoped-methods-only proof that no other method engages MRTR.
 */
class McpMrtrDispatch_Test {

	private final BeanStore ctx = new BasicBeanStore();

	// -------- request/harness helpers ---------

	private static Object validMeta(boolean elicitation) {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, elicitation ? JsonMap.of("elicitation", JsonMap.of()) : JsonMap.of());
	}

	private static JsonRpcRequest req(Object id, String method, Map<String,Object> params, boolean elicitation) {
		var p = params == null ? new JsonMap() : new JsonMap(params);
		p.put("_meta", validMeta(elicitation));
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(p);
	}

	private static Map<String,String> hdrs(String method, String name) {
		var m = new LinkedHashMap<String,String>();
		m.put("Mcp-Method", method);
		m.put("Mcp-Name", name);
		return m;
	}

	private JsonRpcResponse send(McpRevision rev, McpServerConfig config, JsonRpcRequest r, Map<String,String> headers) {
		return rev.dispatch(new McpExchange(r, headers::get), config, ctx);
	}

	private static McpMrtrConfig mrtr(RequestStateCodec codec) {
		return new McpMrtrConfig().setCodec(codec);
	}

	private static McpRevision revision(McpMrtrConfig mrtrConfig) {
		return new McpRevision(null, new McpCacheConfig(), null, mrtrConfig);
	}

	private static String aad(String method) {
		return method + '\u0000' + "2026-07-28";
	}

	// -------- handler fixtures ---------

	private static McpToolOutcome text(String value) {
		return new McpToolOutcome().setContent(List.of(McpContentBlock.text(value)));
	}

	private static McpToolHandler tool(String name, BiFunction<Map<String,Object>,BeanStore,McpToolOutcome> fn) {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName(name); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore c) { return fn.apply(arguments, c); }
		};
	}

	private static McpPromptHandler prompt(String name, BiFunction<Map<String,Object>,BeanStore,McpPromptOutcome> fn) {
		return new McpPromptHandler() {
			@Override public McpPromptSpec descriptor() { return new McpPromptSpec().setName(name); }
			@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore c) { return fn.apply(arguments, c); }
		};
	}

	private static McpResourceHandler resource(String uri, BiFunction<String,BeanStore,McpResourceOutcome> fn) {
		return new McpResourceHandler() {
			@Override public McpResourceSpec descriptor() { return new McpResourceSpec().setUri(uri).setName("res"); }
			@Override public McpResourceOutcome read(String u, BeanStore c) { return fn.apply(u, c); }
		};
	}

	private static Map<String,Object> reqEntry(String type) {
		return Map.of("type", type);
	}

	// -------- PAUSE (Task 9) ---------

	@Test void a01_toolsCall_pauseEmitsInputRequired() {
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addTool(tool("ask",
			(args, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var result = (InputRequiredResult) send(rev, config, req(1, "tools/call", JsonMap.of("name", "ask"), true), hdrs("tools/call", "ask")).getResult();
		assertEquals("input_required", result.getResultType());
		assertNotNull(result.getRequestState());
		assertFalse(result.getRequestState().isEmpty());
		assertTrue(result.getInputRequests().containsKey("q1"));
		assertEquals("elicitation", result.getInputRequests().get("q1").get("type"));
	}

	@Test void a02_promptsGet_pauseEmitsInputRequired() {
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addPrompt(prompt("ask",
			(args, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var result = (InputRequiredResult) send(rev, config, req(1, "prompts/get", JsonMap.of("name", "ask"), true), hdrs("prompts/get", "ask")).getResult();
		assertEquals("input_required", result.getResultType());
		assertTrue(result.getInputRequests().containsKey("q1"));
	}

	@Test void a03_resourcesRead_exactPath_pauseEmitsInputRequired() {
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addResource(resource("file:///a",
			(u, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var result = (InputRequiredResult) send(rev, config, req(1, "resources/read", JsonMap.of("uri", "file:///a"), true), hdrs("resources/read", "file:///a")).getResult();
		assertEquals("input_required", result.getResultType());
		assertTrue(result.getInputRequests().containsKey("q1"));
	}

	@Test void a04_pauseRequestStateUnsealsToRoundOneExactContinuationAndMethod() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var config = new McpServerConfig().addTool(tool("ask",
			(args, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var result = (InputRequiredResult) send(rev, config, req(1, "tools/call", JsonMap.of("name", "ask"), true), hdrs("tools/call", "ask")).getResult();
		var state = codec.unseal(result.getRequestState(), aad("tools/call")).orElseThrow();
		assertEquals(1, state.round());
		assertEquals("cont-1", state.continuation());
		assertEquals("tools/call", state.method());
	}

	@Test void a05_resourcesRead_templatePath_notMrtrWired() {
		// A resource-*template* handler is dispatched on the exact-resource miss and is NOT wrapped in the MRTR
		// try/catch. A signal thrown from it propagates to dispatch's generic branch as -32603, never input_required
		// -- proving the exact-resource-only scoping.
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addResourceTemplate(new McpResourceTemplateHandler() {
			@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate("file:///t/{id}").setName("t"); }
			@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore c) { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }
		});
		var resp = send(rev, config, req(1, "resources/read", JsonMap.of("uri", "file:///t/42"), true), hdrs("resources/read", "file:///t/42"));
		assertEquals(-32603, resp.getError().getCode());
		assertNull(resp.getResult());
	}

	@Test void a06_pauseCarriesNoCacheHintEvenWhenResourceHasConfiguredHint() {
		var cache = new McpCacheConfig().setResourcesRead(new McpCacheHint().setTtlMs(99).setCacheScope(McpCacheScope.PRIVATE));
		var rev = new McpRevision(null, cache, null, mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addResource(resource("file:///a",
			(u, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var result = send(rev, config, req(1, "resources/read", JsonMap.of("uri", "file:///a"), true), hdrs("resources/read", "file:///a")).getResult();
		// InputRequiredResult is not a CacheableResult, so it structurally cannot carry ttl/cacheScope: applyCache(...)
		// is never reached on the PAUSE branch.
		assertInstanceOf(InputRequiredResult.class, result);
		assertFalse(Json.of(result).contains("ttlMs"));
		assertFalse(Json.of(result).contains("cacheScope"));
	}

	// -------- capability gate (Task 10) ---------

	static final class B_CountingCodec implements RequestStateCodec {
		final RequestStateCodec delegate = new AeadRequestStateCodec();
		int sealCalls;

		@Override public String seal(McpRequestState state, String aad) { sealCalls++; return delegate.seal(state, aad); }
		@Override public Optional<McpRequestState> unseal(String token, String aad) { return delegate.unseal(token, aad); }
	}

	@Test void b01_pauseWithoutElicitationCapability_rejectedAndNothingSealed() {
		var codec = new B_CountingCodec();
		var rev = revision(mrtr(codec));
		var config = new McpServerConfig().addTool(tool("ask",
			(args, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var resp = send(rev, config, req(1, "tools/call", JsonMap.of("name", "ask"), false), hdrs("tools/call", "ask"));
		assertEquals(-32021, resp.getError().getCode());
		assertEquals(0, codec.sealCalls);
	}

	@Test void b02_pauseWithElicitationCapability_reachesInputRequired() {
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addTool(tool("ask",
			(args, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var result = (InputRequiredResult) send(rev, config, req(1, "tools/call", JsonMap.of("name", "ask"), true), hdrs("tools/call", "ask")).getResult();
		assertEquals("input_required", result.getResultType());
	}

	@Test void b03_handlerCanPreCheckExposedCapabilityAndDegrade() {
		// The always-present McpMrtrCapabilityContext lets a handler read the client's capability and degrade
		// (return a normal result) instead of pausing into a rejection.
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			var supported = c.getBean(McpMrtrCapabilityContext.class).map(McpMrtrCapabilityContext::elicitationSupported).orElse(false);
			if (! supported)
				return text("degraded");
			throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1");
		}));
		var result = (CallToolResult) send(rev, config, req(1, "tools/call", JsonMap.of("name", "ask"), false), hdrs("tools/call", "ask")).getResult();
		assertEquals("degraded", ((TextContent) result.getContent().get(0)).getText());
	}

	// -------- RESUME (Task 10) ---------

	@Test void c01_resumeToComplete_reInvokesHandlerWithResumeContext() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var seen = new AtomicReference<McpMrtrResumeContext>();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			seen.set(c.getBean(McpMrtrResumeContext.class).orElse(null));
			return text("done");
		}));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var result = (CallToolResult) send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask")).getResult();
		assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		assertNotNull(seen.get());
		assertEquals("cont-1", seen.get().continuation());
		assertEquals("answer", seen.get().inputResponses().get("q1"));
	}

	@Test void c02_resumeToPauseAgain_incrementsRoundToTwo() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			// On resume, pause a second time.
			if (c.getBean(McpMrtrResumeContext.class).isPresent())
				throw new McpInputRequiredSignal(Map.of("q2", reqEntry("elicitation")), "cont-2");
			throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1");
		}));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var result = (InputRequiredResult) send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask")).getResult();
		var state = codec.unseal(result.getRequestState(), aad("tools/call")).orElseThrow();
		assertEquals(2, state.round());
		assertEquals("cont-2", state.continuation());
	}

	@Test void c03_tamperedRequestState_invalidParamsAndHandlerNotReInvoked() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var parts = token.split("\\.", 2);
		var ciphertext = Base64.getUrlDecoder().decode(parts[1]);
		ciphertext[0] ^= 1;
		var tampered = parts[0] + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
		var params = JsonMap.of("name", "ask", "requestState", tampered);
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	@Test void c04_expiredRequestState_distinctCodeAndHandlerNotReInvoked() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() - 1000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "requestState", token);
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32022, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	@Test void c05_maxRoundsExceeded_distinctCodeAndHandlerNotReInvoked() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec).setMaxRounds(10));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 10, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "requestState", token);
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32023, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	@Test void c06_aadBoundMethodMismatch_unsealFailsAsInvalidParams() {
		// AES-GCM binds method into the AAD, so a tools/call-sealed token cannot be replayed on a prompts/get
		// follow-up: unseal itself fails under the different AAD, surfacing as -32602.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addPrompt(prompt("ask", (args, c) -> { calls.incrementAndGet(); return new McpPromptOutcome(); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "requestState", token);
		var resp = send(rev, config, req(1, "prompts/get", params, true), hdrs("prompts/get", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	// A codec that ignores AAD, so a sealed method survives unseal and the dispatcher's own sealed.method()
	// equality check (isolated from the codec's AAD binding) is exercised directly.
	static final class C07_NoAadCodec implements RequestStateCodec {
		@Override public String seal(McpRequestState state, String aad) { return Json.of(state); }
		@Override public Optional<McpRequestState> unseal(String token, String aad) {
			try {
				return Optional.of(Json.to(token, McpRequestState.class));
			} catch (@SuppressWarnings("unused") Exception e) {
				return Optional.empty();
			}
		}
	}

	@Test void c07_dispatcherSealedMethodEqualityCheck_isolatedFromAad() {
		var codec = new C07_NoAadCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addPrompt(prompt("ask", (args, c) -> { calls.incrementAndGet(); return new McpPromptOutcome(); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L), "ignored");
		var params = JsonMap.of("name", "ask", "requestState", token);
		var resp = send(rev, config, req(1, "prompts/get", params, true), hdrs("prompts/get", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals("requestState method mismatch", resp.getError().getMessage());
		assertEquals(0, calls.get());
	}

	@Test void c08_resumeContinuationTypedConvenienceRoundTrip() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var seen = new AtomicReference<C08_Continuation>();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			var rc = c.getBean(McpMrtrResumeContext.class).orElseThrow();
			seen.set(rc.continuationAs(C08_Continuation.class));
			return text("done");
		}));
		var continuation = new C08_Continuation().setStep(3).setNote("resume-me");
		var token = codec.seal(new McpRequestState(continuation, "tools/call", 1, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(3, seen.get().getStep());
		assertEquals("resume-me", seen.get().getNote());
	}

	public static class C08_Continuation {
		private int step;
		private String note;

		public int getStep() { return step; }
		public C08_Continuation setStep(int value) { step = value; return this; }
		public String getNote() { return note; }
		public C08_Continuation setNote(String value) { note = value; return this; }
	}

	@Test void c09_resumeWithAlteredArgumentsIsAcceptedAtMrtrLayer() {
		// MRTR seals nothing about the per-round arguments, so a resume whose arguments differ from the original
		// round is accepted at the MRTR layer; the handler sees the client-supplied arguments while the sealed
		// continuation stays intact. Pins the trust contract documented on McpMrtrResumeContext / McpRequestState
		// (an args-hash binding is deliberately NOT applied).
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var seenArgs = new AtomicReference<Map<String,Object>>();
		var seen = new AtomicReference<McpMrtrResumeContext>();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			seenArgs.set(args);
			seen.set(c.getBean(McpMrtrResumeContext.class).orElse(null));
			return text("done");
		}));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "arguments", JsonMap.of("altered", "value"),
			"requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var result = (CallToolResult) send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask")).getResult();
		assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		assertEquals("value", seenArgs.get().get("altered"));  // altered per-round args reach the handler unchanged
		assertEquals("cont-1", seen.get().continuation());     // the sealed continuation is intact
	}

	@Test void c10_promptsGet_resumeToComplete_reInvokesHandlerWithResumeContext() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var seen = new AtomicReference<McpMrtrResumeContext>();
		var config = new McpServerConfig().addPrompt(prompt("ask", (args, c) -> {
			seen.set(c.getBean(McpMrtrResumeContext.class).orElse(null));
			return new McpPromptOutcome().setDescription("done");
		}));
		var token = codec.seal(new McpRequestState("cont-1", "prompts/get", 1, System.currentTimeMillis() + 60_000L), aad("prompts/get"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var result = (GetPromptResult) send(rev, config, req(1, "prompts/get", params, true), hdrs("prompts/get", "ask")).getResult();
		assertEquals("done", result.getDescription());
		assertNotNull(seen.get());
		assertEquals("cont-1", seen.get().continuation());
		assertEquals("answer", seen.get().inputResponses().get("q1"));
	}

	@Test void c11_resourcesRead_resumeToComplete_reInvokesAndAppliesCacheHint() {
		// Mirror of a06 (a PAUSE carries no hint): a COMPLETED resume on the exact-resource path DOES reach
		// applyCache(...), so the configured resources/read hint is applied to the final ReadResourceResult.
		var codec = new AeadRequestStateCodec();
		var cache = new McpCacheConfig().setResourcesRead(new McpCacheHint().setTtlMs(99).setCacheScope(McpCacheScope.PRIVATE));
		var rev = new McpRevision(null, cache, null, mrtr(codec));
		var seen = new AtomicReference<McpMrtrResumeContext>();
		var config = new McpServerConfig().addResource(resource("file:///a", (u, c) -> {
			seen.set(c.getBean(McpMrtrResumeContext.class).orElse(null));
			return new McpResourceOutcome();
		}));
		var token = codec.seal(new McpRequestState("cont-1", "resources/read", 1, System.currentTimeMillis() + 60_000L), aad("resources/read"));
		var params = JsonMap.of("uri", "file:///a", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var result = (ReadResourceResult) send(rev, config, req(1, "resources/read", params, true), hdrs("resources/read", "file:///a")).getResult();
		assertNotNull(seen.get());
		assertEquals("cont-1", seen.get().continuation());
		assertTrue(Json.of(result).contains("\"ttlMs\":99"));  // a COMPLETED resume gets its cache hint, unlike a PAUSE
	}

	// -------- Task 11: scoped-methods-only + per-instance codec injection ---------

	@Test void d01_nonScopedMethodsAreUnaffectedByMrtr() {
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig()
			.addTool(tool("t", (args, c) -> new McpToolOutcome()))
			.addPrompt(new McpPromptHandler() {
				@Override public McpPromptSpec descriptor() {
					return new McpPromptSpec().setName("p").setArguments(List.of(new McpPromptArgument().setName("x")
						.setCompleter((request, c) -> McpCompletionResult.empty())));
				}
				@Override public McpPromptOutcome get(Map<String,Object> arguments, BeanStore c) { return new McpPromptOutcome(); }
			})
			.addResource(resource("file:///r", (u, c) -> new McpResourceOutcome()))
			.addResourceTemplate(new McpResourceTemplateHandler() {
				@Override public McpResourceTemplateSpec descriptor() { return new McpResourceTemplateSpec().setUriTemplate("file:///tpl/{id}").setName("tpl"); }
				@Override public McpResourceOutcome read(String uri, Map<String,String> variables, BeanStore c) { return new McpResourceOutcome(); }
			});
		assertInstanceOf(ServerDiscoverResult.class, send(rev, config, req(1, "server/discover", null, true), hdrs("server/discover", "")).getResult());
		assertInstanceOf(PingResult.class, send(rev, config, req(1, "ping", null, true), hdrs("ping", "")).getResult());
		assertInstanceOf(ListToolsResult.class, send(rev, config, req(1, "tools/list", null, true), hdrs("tools/list", "")).getResult());
		assertInstanceOf(ListPromptsResult.class, send(rev, config, req(1, "prompts/list", null, true), hdrs("prompts/list", "")).getResult());
		assertInstanceOf(ListResourcesResult.class, send(rev, config, req(1, "resources/list", null, true), hdrs("resources/list", "")).getResult());
		assertInstanceOf(ListResourceTemplatesResult.class, send(rev, config, req(1, "resources/templates/list", null, true), hdrs("resources/templates/list", "")).getResult());
		var completeParams = JsonMap.of("ref", JsonMap.of("type", "ref/prompt", "name", "p"), "argument", JsonMap.of("name", "x", "value", "y"));
		assertInstanceOf(CompleteResult.class, send(rev, config, req(1, "completion/complete", completeParams, true), hdrs("completion/complete", "")).getResult());
	}

	@Test void d02_nonScopedMethodIgnoresRequestStateParam() {
		// A stray requestState on a non-scoped method must be inert -- it is never unsealed or validated.
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var config = new McpServerConfig().addTool(tool("t", (args, c) -> new McpToolOutcome()));
		var resp = send(rev, config, req(1, "tools/list", JsonMap.of("requestState", "garbage-token"), true), hdrs("tools/list", ""));
		assertNull(resp.getError());
		assertInstanceOf(ListToolsResult.class, resp.getResult());
	}

	@Test void d03_codecIsInjectablePerRevisionInstance() {
		// Two revisions with two independently-keyed AEAD codecs: a token sealed for one cannot resume through the
		// other (proves the McpMrtrConfig injection seam, not just the codec unit test).
		var codecA = new AeadRequestStateCodec();
		var codecB = new AeadRequestStateCodec();
		var revB = revision(mrtr(codecB));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codecA.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L), aad("tools/call"));
		var params = JsonMap.of("name", "ask", "requestState", token);
		var resp = send(revB, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}
}
