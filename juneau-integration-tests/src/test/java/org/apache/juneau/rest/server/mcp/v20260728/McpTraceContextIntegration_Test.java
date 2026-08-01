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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.apache.juneau.rest.server.mcp.McpToolHandler;
import org.apache.juneau.rest.server.mcp.McpToolOutcome;
import org.apache.juneau.rest.server.mcp.McpToolSpec;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.rest.server.tracing.*;
import org.junit.jupiter.api.*;

/**
 * Cross-module coverage for the {@code 2026-07-28} v2 trace-context binding, exercised from
 * {@code juneau-integration-tests} exactly as an external consumer would: through the published
 * public API of {@code juneau-rest-server-mcp-v20260728} and the neutral {@code juneau-rest-server}
 * tracing seam ({@link TracerHook}, {@link Scope}, {@link TraceContextCarrier}, {@link TraceOperation})
 * &mdash; no OpenTelemetry import anywhere in this module.
 *
 * <p>
 * Mirrors the acceptance surface of the module-local {@code McpTraceContext_Test}: {@code params._meta}
 * over HTTP-header precedence, an active handler-scoped tracer, the {@code result._meta} echo, no-tracer
 * omission, and per-error {@link Scope#recordRpcError(int, String)} observation, through consumed
 * artifacts rather than module-internal test helpers.
 *
 * <p>
 * Each traced resource class below publishes its recording tracer as a <b>static</b> {@code @Bean}
 * return value and is exercised via {@code MockRestClient.create(Class)} (not a specific instance): the
 * mock REST harness builds/caches one {@code RestContext} per resource <i>class</i>, so a fresh
 * {@code new Resource()} instance passed directly to {@code MockRestClient.create(Object)} is not
 * guaranteed to be the instance that actually serves a later request against that same class.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class McpTraceContextIntegration_Test {

	// -----------------------------------------------------------------------------------------------------------------
	// Neutral test doubles.
	// -----------------------------------------------------------------------------------------------------------------

	static final class RecordingScope implements Scope {
		final List<String> events = Collections.synchronizedList(new ArrayList<>());
		volatile Integer lastErrorCode;
		volatile String lastErrorMessage;

		@Override /* Scope */ public void setStatusCode(int statusCode) { events.add("status:" + statusCode); }
		@Override /* Scope */ public void setError(Throwable error) { events.add("error:" + error.getClass().getSimpleName()); }
		@Override /* Scope */ public void close() { events.add("close"); }

		@Override /* Scope */
		public void recordRpcError(int code, String message) {
			lastErrorCode = code;
			lastErrorMessage = message;
			events.add("rpcError:" + code);
		}
	}

	/**
	 * Records the carrier/operation an active {@link TraceContextExtractor} handed to
	 * {@link #startSpan(RestRequest, TraceContextCarrier, TraceOperation)}, and stashes rendered W3C
	 * fields as request attributes exactly the way a real bridge (e.g. {@code OtelTracerHook}) does at
	 * span-start time &mdash; honoring an explicitly recognized parent from the carrier, or falling back
	 * to a fixed "freshly started root span" value when none was recognized.
	 */
	static final class RecordingTracer implements TracerHook {
		static final String ROOT_TRACEPARENT = "00-0000000000000000000000000000000b-000000000000000b-01";

		volatile TraceContextCarrier lastCarrier;
		volatile TraceOperation lastOperation;
		volatile RecordingScope lastScope;

		void reset() {
			lastCarrier = null;
			lastOperation = null;
			lastScope = null;
		}

		@Override /* TracerHook */
		public Scope startSpan(RestRequest request) {
			return startSpan(request, null, TraceOperation.DEFAULT);
		}

		@Override /* TracerHook */
		public Scope startSpan(RestRequest request, TraceContextCarrier carrier, TraceOperation operation) {
			lastCarrier = carrier;
			lastOperation = operation;
			var scope = new RecordingScope();
			lastScope = scope;
			var traceparent = carrier == null ? null : carrier.get(RequestMeta.KEY_TRACEPARENT);
			request.setAttribute(TraceContextResponseProcessor.ATTR_TRACEPARENT, traceparent != null ? traceparent : ROOT_TRACEPARENT);
			var tracestate = carrier == null ? null : carrier.get(RequestMeta.KEY_TRACESTATE);
			if (tracestate != null)
				request.setAttribute(TraceContextResponseProcessor.ATTR_TRACESTATE, tracestate);
			var baggage = carrier == null ? null : carrier.get(RequestMeta.KEY_BAGGAGE);
			if (baggage != null)
				request.setAttribute(TraceContextResponseProcessor.ATTR_BAGGAGE, baggage);
			return scope;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Fixtures.
	// -----------------------------------------------------------------------------------------------------------------

	private static JsonMap validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static Object withMeta(Object baseParams, Map<String,Object> extraMeta) {
		var p = baseParams instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		var meta = new JsonMap(validMeta());
		if (extraMeta != null)
			meta.putAll(extraMeta);
		p.put("_meta", meta);
		return p;
	}

	private static String body(Object id, String method, Object params, Map<String,Object> extraMeta) {
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(withMeta(params, extraMeta)));
	}

	private static McpToolHandler echo() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo").setDescription("Echoes back"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text(String.valueOf(arguments.get("text"))); }
		};
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Traced servlet resource (registers the recording tracer as a class-shared @Bean).
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Traced extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		static final RecordingTracer TRACER = new RecordingTracer();

		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}

		@Bean
		public TracerHook tracer() {
			return TRACER;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Traced endpoint-mixin resource — parity with the servlet path above.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class TracedMixin extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		static final RecordingTracer TRACER = new RecordingTracer();

		@Override
		public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}

		@Bean
		public TracerHook tracer() {
			return TRACER;
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Untraced servlet resource — no @Bean TracerHook registered at all.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class Untraced extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
	}

	private static MockRestClient client(Class<?> resourceClass) {
		return MockRestClient.create(resourceClass).json().contentType("application/json").accept("application/json").build();
	}

	@BeforeEach
	void resetTracers() {
		Traced.TRACER.reset();
		TracedMixin.TRACER.reset();
	}

	// =====================================================================================================================
	// A: params._meta / HTTP-header extraction precedence (metadata-only parent, header fallback, meta wins).
	// =====================================================================================================================

	@Test void a01_metaOnlyParent_usedAsCarrierTraceparent() throws Exception {
		var extra = Map.<String,Object>of(RequestMeta.KEY_TRACEPARENT, "00-metaonly000000000000000000000-0000000000000011-01");
		client(Traced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), extra))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200);
		assertEquals("00-metaonly000000000000000000000-0000000000000011-01", Traced.TRACER.lastCarrier.get(RequestMeta.KEY_TRACEPARENT));
	}

	@Test void a02_noMetaTraceparent_fallsBackToHttpHeader() throws Exception {
		client(Traced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), null))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.header(RequestMeta.KEY_TRACEPARENT, "00-fromheader0000000000000000-0000000000000012-01")
			.run().assertStatus(200);
		assertEquals("00-fromheader0000000000000000-0000000000000012-01", Traced.TRACER.lastCarrier.get(RequestMeta.KEY_TRACEPARENT));
	}

	@Test void a03_metaTraceparent_winsOverConflictingHttpHeader() throws Exception {
		var extra = Map.<String,Object>of(RequestMeta.KEY_TRACEPARENT, "00-metawins00000000000000000000-0000000000000013-01");
		client(Traced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), extra))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.header(RequestMeta.KEY_TRACEPARENT, "00-headerloses0000000000000000-0000000000000014-01")
			.run().assertStatus(200);
		assertEquals("00-metawins00000000000000000000-0000000000000013-01", Traced.TRACER.lastCarrier.get(RequestMeta.KEY_TRACEPARENT));
	}

	// =====================================================================================================================
	// B: an active handler-scoped tracer runs through real dispatch.
	// =====================================================================================================================

	@Test void b01_activeTracer_startsAndClosesScopeAroundHandlerExecution() throws Exception {
		client(Traced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), null))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200);
		var scope = Traced.TRACER.lastScope;
		assertNotNull(scope);
		assertTrue(scope.events.contains("close"), scope.events.toString());
	}

	// =====================================================================================================================
	// C: result._meta / HTTP echo round-trip.
	// =====================================================================================================================

	@Test void c01_successResult_echoesTraceparentIntoResultMeta() throws Exception {
		var extra = Map.<String,Object>of(RequestMeta.KEY_TRACEPARENT, "00-c01parent00000000000000000-0000000000000015-01");
		var resp = client(Traced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), extra))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"traceparent\":\"00-c01parent00000000000000000-0000000000000015-01\"", resp);
	}

	@Test void c02_successResult_echoesTracestateAndBaggage() throws Exception {
		var extra = Map.<String,Object>of(
			RequestMeta.KEY_TRACEPARENT, "00-c02parent00000000000000000-0000000000000016-01",
			RequestMeta.KEY_TRACESTATE, "vendor=abc",
			RequestMeta.KEY_BAGGAGE, "userId=42");
		var resp = client(Traced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), extra))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"tracestate\":\"vendor=abc\"", resp);
		assertContains("\"baggage\":\"userId=42\"", resp);
	}

	@Test void c03_noRemoteParent_echoesTracerRenderedRootTraceparent() throws Exception {
		var resp = client(Traced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), null))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"traceparent\":\"" + RecordingTracer.ROOT_TRACEPARENT + "\"", resp);
	}

	@Test void c04_noTracerRegistered_resultMetaHasNoTraceKeys() throws Exception {
		var resp = client(Untraced.class).post("/").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), null))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"" + ResultMeta.KEY_SERVER_INFO + "\"", resp);
		assertFalse(resp.contains("\"traceparent\""), resp);
		assertFalse(resp.contains("\"tracestate\""), resp);
		assertFalse(resp.contains("\"baggage\""), resp);
	}

	@Test void c05_endpointMixin_echoesTraceparentIntoResultMeta_parityWithServlet() throws Exception {
		var extra = Map.<String,Object>of(RequestMeta.KEY_TRACEPARENT, "00-c05parent00000000000000000-0000000000000017-01");
		var resp = client(TracedMixin.class).post("/mcp").contentString(body(1, "tools/call", JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hi")), extra))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"traceparent\":\"00-c05parent00000000000000000-0000000000000017-01\"", resp);
		assertEquals("00-c05parent00000000000000000-0000000000000017-01", TracedMixin.TRACER.lastCarrier.get(RequestMeta.KEY_TRACEPARENT));
	}

	// =====================================================================================================================
	// D: per-error Scope#recordRpcError observation.
	// =====================================================================================================================

	@Test void d01_unknownMethod_recordsMethodNotFoundCode() throws Exception {
		client(Traced.class).post("/").contentString(body(1, "mystery/method", null, null))
			.header("Mcp-Method", "mystery/method").header("Mcp-Name", "")
			.run().assertStatus(200);
		var scope = Traced.TRACER.lastScope;
		assertNotNull(scope);
		assertEquals(McpRevision.CODE_METHOD_NOT_FOUND, scope.lastErrorCode);
		assertTrue(scope.events.contains("rpcError:" + McpRevision.CODE_METHOD_NOT_FOUND), scope.events.toString());
	}

	@Test void d02_errorResponse_hasNoResultMetaTraceKeys() throws Exception {
		var resp = client(Traced.class).post("/").contentString(body(1, "mystery/method", null, null))
			.header("Mcp-Method", "mystery/method").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"error\"", resp);
		assertFalse(resp.contains("\"result\""), resp);
	}
}
