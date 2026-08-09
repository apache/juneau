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

import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.concurrent.*;
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
import org.apache.juneau.rest.server.mcp.McpResponseResult;
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
		var result = rev.dispatch(new McpExchange(r, headers::get), config, ctx);
		return result instanceof McpResponseResult mrr ? mrr.response() : null;
	}

	private static McpMrtrConfig mrtr(RequestStateCodec codec) {
		return new McpMrtrConfig().setCodec(codec);
	}

	private static McpRevision revision(McpMrtrConfig mrtrConfig) {
		return new McpRevision(null, new McpCacheConfig(), null, mrtrConfig);
	}

	// Mirror of McpRevision#aad(method, target): the sealed AAD now binds the operation target (tool/prompt
	// name or resource uri) in addition to method+version, so every hand-sealed fixture below must seal under
	// the same target it will be resumed against or the GCM tag check fails on unseal.
	private static String aad(String method, String target) {
		return method + '\u0000' + "2026-07-28" + '\u0000' + (target == null ? "" : target);
	}

	// The empty-arguments sentinel hash (see McpRevision#argumentsHash): every fixture below that seals a
	// McpRequestState directly (bypassing pause(...)) and then resumes with no "arguments" member in its params
	// must seal this same sentinel, or the always-on argument-hash check added alongside replay-cache support
	// would newly reject it as a mismatch.
	private static final String NO_ARGS_HASH = McpRevision.argumentsHash(Map.of());

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
		var state = codec.unseal(result.getRequestState(), aad("tools/call", "ask")).orElseThrow();
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

		@Override public String seal(McpRequestState state, String aad, Principal principal) { sealCalls++; return delegate.seal(state, aad, principal); }
		@Override public Optional<McpRequestState> unseal(String token, String aad, Principal principal) { return delegate.unseal(token, aad, principal); }
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
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
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
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var result = (InputRequiredResult) send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask")).getResult();
		var state = codec.unseal(result.getRequestState(), aad("tools/call", "ask")).orElseThrow();
		assertEquals(2, state.round());
		assertEquals("cont-2", state.continuation());
	}

	@Test void c03_tamperedRequestState_invalidParamsAndHandlerNotReInvoked() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var parts = token.split("\\.", 4);
		var ciphertext = Base64.getUrlDecoder().decode(parts[3]);
		ciphertext[0] ^= 1;
		var tampered = parts[0] + "." + parts[1] + "." + parts[2] + "."
			+ Base64.getUrlEncoder().withoutPadding().encodeToString(ciphertext);
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
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() - 1000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
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
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 10, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
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
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "requestState", token);
		var resp = send(rev, config, req(1, "prompts/get", params, true), hdrs("prompts/get", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	// A codec that ignores AAD, so a sealed method survives unseal and the dispatcher's own sealed.method()
	// equality check (isolated from the codec's AAD binding) is exercised directly.
	static final class C07_NoAadCodec implements RequestStateCodec {
		@Override public String seal(McpRequestState state, String aad, Principal principal) { return Json.of(state); }
		@Override public Optional<McpRequestState> unseal(String token, String aad, Principal principal) {
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
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), "ignored");
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
		var token = codec.seal(new McpRequestState(continuation, "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
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

	@Test void c09_resumeWithAlteredArgumentsIsRejectedAsArgumentsMismatch() {
		// The sealed McpRequestState carries an argumentsHash computed from the original round's arguments; a
		// resume whose arguments differ from that sealed hash is hard-rejected before the handler is re-invoked.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "arguments", JsonMap.of("altered", "value"),
			"requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32026, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	@Test void c10_promptsGet_resumeToComplete_reInvokesHandlerWithResumeContext() {
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var seen = new AtomicReference<McpMrtrResumeContext>();
		var config = new McpServerConfig().addPrompt(prompt("ask", (args, c) -> {
			seen.set(c.getBean(McpMrtrResumeContext.class).orElse(null));
			return new McpPromptOutcome().setDescription("done");
		}));
		var token = codec.seal(new McpRequestState("cont-1", "prompts/get", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("prompts/get", "ask"));
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
		var token = codec.seal(new McpRequestState("cont-1", "resources/read", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("resources/read", "file:///a"));
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
		var token = codecA.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "requestState", token);
		var resp = send(revB, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	// -------- F4 (READY-312f): principal exposure to the codec seam ---------

	// Records the principal (and whether it was called at all) the dispatcher threads into seal/unseal.
	static final class E_PrincipalCapturingCodec implements RequestStateCodec {
		final RequestStateCodec delegate = new AeadRequestStateCodec();
		boolean sealCalled;
		boolean unsealCalled;
		Principal sealPrincipal;
		Principal unsealPrincipal;

		@Override public String seal(McpRequestState state, String aad, Principal principal) {
			sealCalled = true;
			sealPrincipal = principal;
			return delegate.seal(state, aad, principal);
		}

		@Override public Optional<McpRequestState> unseal(String token, String aad, Principal principal) {
			unsealCalled = true;
			unsealPrincipal = principal;
			return delegate.unseal(token, aad, principal);
		}
	}

	@Test void e01_noBoundRestRequest_principalIsNullAtSealAndUnsealAndStillRoundTrips() {
		// The direct-dispatch harness binds no RestRequest, so the dispatcher resolves NO principal (anonymous /
		// RS-auth-disabled path).  seal and unseal must both be invoked with a null principal and the token must
		// still round-trip cleanly (no NPE) -- the contract the F4 seam must preserve.
		var codec = new E_PrincipalCapturingCodec();
		var rev = revision(mrtr(codec));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			if (c.getBean(McpMrtrResumeContext.class).isPresent())
				return text("done");
			throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1");
		}));
		var paused = (InputRequiredResult) send(rev, config, req(1, "tools/call", JsonMap.of("name", "ask"), true), hdrs("tools/call", "ask")).getResult();
		assertTrue(codec.sealCalled);
		assertNull(codec.sealPrincipal);
		assertNotNull(paused.getRequestState());

		var resumeParams = JsonMap.of("name", "ask", "requestState", paused.getRequestState(), "inputResponses", JsonMap.of("q1", "answer"));
		var result = (CallToolResult) send(rev, config, req(1, "tools/call", resumeParams, true), hdrs("tools/call", "ask")).getResult();
		assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
		assertTrue(codec.unsealCalled);
		assertNull(codec.unsealPrincipal);
	}

	@Test void d04_sharedStaticKeyProviderResumeSucceedsAcrossIndependentRevisions() {
		// Direct contrast to d03: two independently-constructed McpRevision instances (via two independently-
		// constructed McpMrtrConfig -> AeadRequestStateCodec instances) wired to the SAME StaticKeyProvider DO
		// resume each other's tokens -- the horizontal-scaling proof design.md §2/§7 exists to enable.
		var sharedKeyProvider = StaticKeyProvider.of("2026-08-d04", StaticKeyProvider.aesKey(new byte[32]));
		var revA = revision(new McpMrtrConfig().setKeyProvider(sharedKeyProvider));
		var revB = revision(new McpMrtrConfig().setKeyProvider(sharedKeyProvider));

		// Pause dispatched through revision A: mints a requestState sealed by A's own, independently-constructed
		// codec instance.
		var pauseConfig = new McpServerConfig().addTool(tool("ask",
			(args, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var paused = (InputRequiredResult) send(revA, pauseConfig, req(1, "tools/call", JsonMap.of("name", "ask"), true), hdrs("tools/call", "ask")).getResult();
		var token = paused.getRequestState();
		assertNotNull(token);

		// Resume dispatched through revision B (a DIFFERENT McpRevision, DIFFERENT McpMrtrConfig, DIFFERENT
		// AeadRequestStateCodec instance -- sharing only sharedKeyProvider) succeeds.
		var resumeConfig = new McpServerConfig().addTool(tool("ask", (args, c) -> text("done")));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(revB, resumeConfig, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertNull(resp.getError());
		var result = (CallToolResult) resp.getResult();
		assertEquals("done", ((TextContent) result.getContent().get(0)).getText());
	}

	// -------- replay cache + argument-hash sealing ---------

	@Test void f01_replayOfConsumedToken_rejectedWithWiredReplayCache() {
		// A wired ReplayCache narrows the documented default multi-use tolerance to single-use: resubmitting a
		// token already consumed by an earlier resume is rejected, and the handler is not re-invoked a second time.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec).setReplayCache(new InMemoryReplayCache()));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));

		var first = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertNull(first.getError());
		assertEquals(1, calls.get());

		var second = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32025, second.getError().getCode());
		assertEquals(1, calls.get());  // not re-invoked a second time
	}

	@Test void f02_sameTokenDoubleSubmit_secondRejectedEvenAfterAdvancingToNextRound() {
		// The same wired ReplayCache also catches a stale round-N token being resubmitted after the client has
		// already legitimately advanced past it to a later round's token -- replay detection is per-jti, not
		// merely "is this the current round's token".
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec).setReplayCache(new InMemoryReplayCache()));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			if (c.getBean(McpMrtrResumeContext.class).isPresent())
				throw new McpInputRequiredSignal(Map.of("q2", reqEntry("elicitation")), "cont-2");
			throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1");
		}));
		var token1 = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params1 = JsonMap.of("name", "ask", "requestState", token1, "inputResponses", JsonMap.of("q1", "answer"));

		// Legitimate first use of token1 consumes it and advances to round 2.
		var pausedAgain = (InputRequiredResult) send(rev, config, req(1, "tools/call", params1, true), hdrs("tools/call", "ask")).getResult();
		assertNotNull(pausedAgain.getRequestState());

		// A second submission of the SAME (now-stale) round-1 token is a replay.
		var resp = send(rev, config, req(1, "tools/call", params1, true), hdrs("tools/call", "ask"));
		assertEquals(-32025, resp.getError().getCode());
	}

	@Test void f03_defaultConfigWithoutReplayCache_doesNotRejectReplay() {
		// Pins D1 (opt-in): with no ReplayCache wired (the default), a requestState token remains the documented
		// multi-use bearer credential -- the exact same token may be resubmitted repeatedly within its TTL.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));  // no ReplayCache wired
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));

		for (var i = 0; i < 3; i++)
			assertNull(send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask")).getError());
		assertEquals(3, calls.get());
	}

	// A ReplayCache whose checkAndRecord always throws (after counting the call), proving the dispatcher's
	// fail-open contract AND that the cache was actually consulted (so f04 cannot pass vacuously).
	static final class F04_ThrowingReplayCache implements ReplayCache {
		final AtomicInteger calls = new AtomicInteger();
		@Override public boolean checkAndRecord(String jti, long expiresAtMs) {
			calls.incrementAndGet();
			throw new RuntimeException("simulated replay-store outage");
		}
	}

	@Test void f04_replayCacheThatThrows_failsOpenAndResumeProceeds() {
		// A ReplayCache that throws is fail-open by contract: the dispatcher catches the exception, logs it, and
		// treats the token as first-seen -- degrading to the same behavior as if no ReplayCache were wired at all,
		// rather than rejecting the resume. An operator who wants fail-closed-on-outage must return false instead
		// of throwing (see ReplayCache's javadoc); the framework applies no such policy itself.
		var codec = new AeadRequestStateCodec();
		var replayCache = new F04_ThrowingReplayCache();
		var rev = revision(mrtr(codec).setReplayCache(replayCache));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertNull(resp.getError());
		assertEquals(1, calls.get());
		assertEquals(1, replayCache.calls.get());  // the throwing cache WAS consulted -- fail-open is not vacuous
	}

	@Test void f05_argumentsMutatedBetweenRounds_rejectedAsArgumentsMismatch() {
		// End-to-end (via pause(...) itself, not a hand-built fixture): round 1 is paused and resumed with
		// faithful arguments, advancing to round 2 -- whose sealed argumentsHash is computed from THAT resume
		// request's arguments. Resuming round 2 with DIFFERENT arguments than that round actually saw is rejected.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			if (c.getBean(McpMrtrResumeContext.class).isPresent())
				throw new McpInputRequiredSignal(Map.of("q2", reqEntry("elicitation")), "cont-2");
			throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1");
		}));

		var initialParams = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", 1));
		var paused = (InputRequiredResult) send(rev, config, req(1, "tools/call", initialParams, true), hdrs("tools/call", "ask")).getResult();

		// Faithful round-1 resume (same arguments as the original request) advances to round 2; round 2's
		// argumentsHash is sealed from THIS request's arguments.
		var resume1Params = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", 1),
			"requestState", paused.getRequestState(), "inputResponses", JsonMap.of("q1", "answer"));
		var pausedAgain = (InputRequiredResult) send(rev, config, req(1, "tools/call", resume1Params, true), hdrs("tools/call", "ask")).getResult();
		assertNotNull(pausedAgain.getRequestState());

		// Round-2 resume with arguments that differ from what round 2 actually saw is rejected before the
		// handler is re-invoked.
		var resume2Params = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", 2),
			"requestState", pausedAgain.getRequestState(), "inputResponses", JsonMap.of("q2", "answer"));
		var resp = send(rev, config, req(1, "tools/call", resume2Params, true), hdrs("tools/call", "ask"));
		assertEquals(-32026, resp.getError().getCode());
	}

	@Test void f06_happyPath_faithfulResendUnaffectedByEitherCheck() {
		// A faithful client that never reuses a token and always resends identical arguments sees zero
		// behavioral change with a ReplayCache AND the always-on argument-hash check both engaged at once.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec).setReplayCache(new InMemoryReplayCache()));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> {
			if (c.getBean(McpMrtrResumeContext.class).isPresent())
				return text("done:" + args.get("x"));
			throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1");
		}));

		var initialParams = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", 1));
		var paused = (InputRequiredResult) send(rev, config, req(1, "tools/call", initialParams, true), hdrs("tools/call", "ask")).getResult();

		var resumeParams = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", 1),
			"requestState", paused.getRequestState(), "inputResponses", JsonMap.of("q1", "answer"));
		var result = (CallToolResult) send(rev, config, req(1, "tools/call", resumeParams, true), hdrs("tools/call", "ask")).getResult();
		assertEquals("done:1", ((TextContent) result.getContent().get(0)).getText());
	}

	@Test void f07_sharedReplayCache_detectsReplayAcrossIndependentRevisions() {
		// Direct analog to d04 (shared KeyProvider), but for the ReplayCache SPI: a store shared across every
		// node catches a token replayed against a DIFFERENT McpRevision instance than the one that consumed it
		// first -- the cross-node single-use enforcement design.md documents as needing a shared impl.
		var sharedKeyProvider = StaticKeyProvider.of("2026-08-f07", StaticKeyProvider.aesKey(new byte[32]));
		var sharedReplayCache = new InMemoryReplayCache();
		var revA = revision(new McpMrtrConfig().setKeyProvider(sharedKeyProvider).setReplayCache(sharedReplayCache));
		var revB = revision(new McpMrtrConfig().setKeyProvider(sharedKeyProvider).setReplayCache(sharedReplayCache));

		var pauseConfig = new McpServerConfig().addTool(tool("ask",
			(args, c) -> { throw new McpInputRequiredSignal(Map.of("q1", reqEntry("elicitation")), "cont-1"); }));
		var paused = (InputRequiredResult) send(revA, pauseConfig, req(1, "tools/call", JsonMap.of("name", "ask"), true), hdrs("tools/call", "ask")).getResult();
		var token = paused.getRequestState();
		assertNotNull(token);

		var resumeConfig = new McpServerConfig().addTool(tool("ask", (args, c) -> text("done")));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));

		// First resume, dispatched through the OTHER revision instance, consumes the token via the shared cache.
		var first = send(revB, resumeConfig, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertNull(first.getError());

		// Replaying the same token back through revision A (the one that minted it) is still caught: the
		// ReplayCache -- shared across both instances -- is what recorded the jti as seen, not either revision's
		// own in-process state.
		var second = send(revA, resumeConfig, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32025, second.getError().getCode());
	}

	@Test void f08_argumentsHashIsPinnedJcsSha256Base64Url() {
		// Algorithm-pinning: the hash is base64url(SHA-256(JCS-canonical UTF-8 bytes)). These literals are the
		// externally-computed digests of the canonical bytes {"a":1,"b":2} and {} respectively -- passing keys
		// out of order proves JCS key-sorting, and the exact bytes pin JCS + SHA-256 + base64url as a unit. This
		// MUST fail if someone swaps the JCS canonicalizer for a plain (insertion-order) serializer.
		assertEquals("QyWM_3g_5wNtikMDP4MK38YOwDc4JHNUisdCuIgpJ3c", McpRevision.argumentsHash(JsonMap.of("b", 2, "a", 1)));
		assertEquals("RBNvo1WzZ4oRRq0W9-hknpT7T8If536DEMBg9hyq_4o", McpRevision.argumentsHash(Map.of()));
		// The empty-object literal is exactly the sentinel every no-argument fixture seals.
		assertEquals("RBNvo1WzZ4oRRq0W9-hknpT7T8If536DEMBg9hyq_4o", NO_ARGS_HASH);
	}

	@Test void f09_resumeReorderingArgumentKeysSucceeds() {
		// JCS sorts keys, so a resume that reorders argument keys hashes identically to the sealed round and is
		// NOT a mismatch -- the handler is re-invoked normally.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> text("done")));
		var sealedHash = McpRevision.argumentsHash(JsonMap.of("a", 1, "b", 2));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", sealedHash), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "arguments", JsonMap.of("b", 2, "a", 1),
			"requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertNull(resp.getError());
		assertEquals("done", ((TextContent) ((CallToolResult) resp.getResult()).getContent().get(0)).getText());
	}

	@Test void f10_typeCoercionArgumentsRejected() {
		// Canonicalization is type-faithful: a number-vs-string coercion (1 vs "1") and a scalar-vs-array
		// coercion (1 vs [1]) both hash differently from the sealed {"x":1} and are rejected as -32026.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var sealedHash = McpRevision.argumentsHash(JsonMap.of("x", 1));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", sealedHash), aad("tools/call", "ask"));

		var stringParams = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", "1"),
			"requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		assertEquals(-32026, send(rev, config, req(1, "tools/call", stringParams, true), hdrs("tools/call", "ask")).getError().getCode());

		var arrayParams = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", List.of(1)),
			"requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		assertEquals(-32026, send(rev, config, req(1, "tools/call", arrayParams, true), hdrs("tools/call", "ask")).getError().getCode());

		assertEquals(0, calls.get());  // neither coercion re-invoked the handler
	}

	@Test void f11_absentNullEmptyArgumentsAreInterchangeable() {
		// absent "arguments", an explicit null "arguments", and an empty {} all resolve to Map.of() and hash to
		// the same sentinel, so all three faithfully resume a token sealed with the sentinel hash. No replay
		// cache is wired, so the single token stays usable across all three submissions.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> text("done")));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "ask"));

		var absent = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var explicitNull = JsonMap.of("name", "ask", "arguments", null, "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var empty = JsonMap.of("name", "ask", "arguments", JsonMap.of(), "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		assertNull(send(rev, config, req(1, "tools/call", absent, true), hdrs("tools/call", "ask")).getError());
		assertNull(send(rev, config, req(1, "tools/call", explicitNull, true), hdrs("tools/call", "ask")).getError());
		assertNull(send(rev, config, req(1, "tools/call", empty, true), hdrs("tools/call", "ask")).getError());
	}

	@Test void f12_tokenPausedForToolAResumedAgainstToolBRejected() {
		// H2: the operation target (tool name) is bound into the AAD, so a token paused for tool A cannot be
		// resumed against tool B -- the differing target fails the GCM tag check, surfacing as -32602. This also
		// proves the no-argument sentinel no longer lets a token cross tools (both seal NO_ARGS_HASH).
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig()
			.addTool(tool("toolA", (args, c) -> { calls.incrementAndGet(); return text("a"); }))
			.addTool(tool("toolB", (args, c) -> { calls.incrementAndGet(); return text("b"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("tools/call", "toolA"));
		var params = JsonMap.of("name", "toolB", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "toolB"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	@Test void f13_tokenPausedForResourceUriAResumedAgainstUriBRejected() {
		// H2, resource variant: the resource uri is the bound target for resources/read, so a token paused for
		// uri A cannot be resumed against uri B.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec));
		var calls = new AtomicInteger();
		var config = new McpServerConfig()
			.addResource(resource("file:///a", (u, c) -> { calls.incrementAndGet(); return new McpResourceOutcome(); }))
			.addResource(resource("file:///b", (u, c) -> { calls.incrementAndGet(); return new McpResourceOutcome(); }));
		var token = codec.seal(new McpRequestState("cont-1", "resources/read", 1, System.currentTimeMillis() + 60_000L, "jti-1", NO_ARGS_HASH), aad("resources/read", "file:///a"));
		var params = JsonMap.of("uri", "file:///b", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(rev, config, req(1, "resources/read", params, true), hdrs("resources/read", "file:///b"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	@Test void f14_mismatchedArgumentsLeavesTokenUsableForLaterFaithfulResume() {
		// H3: the stateless argument-hash check runs BEFORE the stateful replay checkAndRecord, so a submission
		// with wrong arguments is rejected WITHOUT consuming the token's jti. A subsequent faithful resume of the
		// same token therefore still succeeds -- an attacker with only a leaked token cannot burn a victim's
		// in-flight resume, and an honest client's typo does not destroy its own token.
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec).setReplayCache(new InMemoryReplayCache()));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var sealedHash = McpRevision.argumentsHash(JsonMap.of("x", 1));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", sealedHash), aad("tools/call", "ask"));

		var wrongArgs = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", 2),
			"requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		assertEquals(-32026, send(rev, config, req(1, "tools/call", wrongArgs, true), hdrs("tools/call", "ask")).getError().getCode());
		assertEquals(0, calls.get());  // rejected before the handler, and before the jti was recorded

		var faithful = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", 1),
			"requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		assertNull(send(rev, config, req(1, "tools/call", faithful, true), hdrs("tools/call", "ask")).getError());
		assertEquals(1, calls.get());  // token was NOT burned by the earlier mismatch
	}

	@Test void f15_nonFiniteNumberArgumentsRejectedAsInvalidParamsNotInternalError() {
		// M3: a hostile-but-syntactically-legal value (a non-finite number, here 1e999 -> +Infinity) makes the
		// JCS canonicalizer throw; that is mapped to -32602 (invalid params) rather than surfacing as a generic
		// -32603 internal error. The hash is computed before the handler runs, so it is never invoked.
		var rev = revision(mrtr(new AeadRequestStateCodec()));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var params = JsonMap.of("name", "ask", "arguments", JsonMap.of("x", Double.POSITIVE_INFINITY));
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	@Test void f16_nullJtiFailsClosedWithWiredReplayCache() {
		// M4: a null jti reaching a wired ReplayCache is a codec/contract violation, not a store outage -- it
		// fails CLOSED with -32602 rather than inheriting checkReplay's fail-open policy (which a downstream NPE
		// would otherwise trigger, silently disabling replay protection).
		var codec = new AeadRequestStateCodec();
		var rev = revision(mrtr(codec).setReplayCache(new InMemoryReplayCache()));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var token = codec.seal(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, null, NO_ARGS_HASH), aad("tools/call", "ask"));
		var params = JsonMap.of("name", "ask", "requestState", token, "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}

	// A custom RequestStateCodec whose unseal() always succeeds with a sealed argumentsHash that is not valid
	// base64url -- a codec/contract violation distinct from f16's null-jti case, exercised the same way (a
	// hand-built codec double, not a real AEAD token).
	static final class F17_MalformedArgumentsHashCodec implements RequestStateCodec {
		@Override public String seal(McpRequestState state, String aad, Principal principal) {
			throw new UnsupportedOperationException("not exercised by f17 -- the fixture token is opaque to this codec");
		}
		@Override public Optional<McpRequestState> unseal(String token, String aad, Principal principal) {
			return Optional.of(new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", "not!valid-base64url"));
		}
	}

	@Test void f17_malformedSealedArgumentsHashFailsClosedWithInvalidParamsNotInternalError() {
		// Low3: a sealed argumentsHash that isn't valid base64url (a custom codec's contract violation, mirroring
		// f16's null-jti case) must fail CLOSED with -32602, not let B64URL_DECODER.decode(...)'s raw
		// IllegalArgumentException surface as a generic -32603 internal error.
		var rev = revision(mrtr(new F17_MalformedArgumentsHashCodec()));
		var calls = new AtomicInteger();
		var config = new McpServerConfig().addTool(tool("ask", (args, c) -> { calls.incrementAndGet(); return text("done"); }));
		var params = JsonMap.of("name", "ask", "requestState", "opaque-token", "inputResponses", JsonMap.of("q1", "answer"));
		var resp = send(rev, config, req(1, "tools/call", params, true), hdrs("tools/call", "ask"));
		assertEquals(-32602, resp.getError().getCode());
		assertEquals(0, calls.get());
	}
}
