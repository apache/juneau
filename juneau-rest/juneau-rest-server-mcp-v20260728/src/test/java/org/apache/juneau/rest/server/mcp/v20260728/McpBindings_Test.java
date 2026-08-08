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
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
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
import org.junit.jupiter.api.*;

/**
 * End-to-end HTTP coverage for the {@code 2026-07-28} {@link McpRestServlet} and {@link McpEndpoint}
 * bindings and their typed {@code server/discover} capability hooks.
 */
@SuppressWarnings({
	"resource" // Test helpers return Closeables; Eclipse JDT @Owning warning is by design.
})
class McpBindings_Test extends TestBase {

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_INFO, JsonMap.of("name", "fixture-client", "version", "1.0"),
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static Object withMeta(Object baseParams) {
		var p = baseParams instanceof Map<?,?> m ? new JsonMap(m) : new JsonMap();
		p.put("_meta", validMeta());
		return p;
	}

	private static String body(Object id, String method, Object params) {
		return org.apache.juneau.marshall.marshaller.Json.of(new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(id).setMethod(method).setParams(withMeta(params)));
	}

	private static McpToolHandler echo() {
		return new McpToolHandler() {
			@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("echo").setDescription("Echoes back"); }
			@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text(String.valueOf(arguments.get("text"))); }
		};
	}

	// -------- servlet path (POST /) ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class A extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("test").setVersion("1.0.0").addTool(echo());
		}
	}

	private MockRestClient clientA() {
		return MockRestClient.create(A.class).json().contentType("application/json").accept("application/json").build();
	}

	@Test void a01_servlet_serverDiscover_autoDerivesToolCapability() throws Exception {
		var resp = clientA().post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"" + ResultMeta.KEY_SERVER_INFO + "\"", resp);
		assertContains("\"test\"", resp);
		assertContains("\"tools\"", resp);
	}

	@Test void a02_servlet_toolsCall_dispatches() throws Exception {
		var params = JsonMap.of("name", "echo", "arguments", JsonMap.of("text", "hello"));
		var resp = clientA().post("/").contentString(body(1, "tools/call", params))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "echo")
			.run().assertStatus(200).getContent().asString();
		assertContains("hello", resp);
	}

	// -------- servlet with explicit capability override (via McpOptions) ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class D extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().setName("override").setVersion("1.0.0").addTool(echo());
		}
		@Override
		protected McpOptions createMcpOptions() {
			return new McpOptions().setCapabilities(new ServerCapabilities().setPrompts(new PromptCapability()));
		}
	}

	@Test void a03_servlet_overrideCapabilities_advertisedAsIs() throws Exception {
		var c = MockRestClient.create(D.class).json().contentType("application/json").accept("application/json").build();
		var resp = c.post("/").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"prompts\"", resp);
		assertFalse(resp.contains("\"tools\""), resp);  // a registered tool does NOT leak past an explicit override
	}

	// -------- endpoint mixin path (POST /mcp) ---------

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class B extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override
		public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(new McpToolHandler() {
				@Override public McpToolSpec descriptor() { return new McpToolSpec().setName("ping"); }
				@Override public McpToolOutcome call(Map<String,Object> arguments, BeanStore ctx) { return McpToolOutcome.text("pong"); }
			});
		}
	}

	private MockRestClient clientB() {
		return MockRestClient.create(B.class).json().contentType("application/json").accept("application/json").build();
	}

	@Test void b01_endpointMixin_serverDiscover_dispatches() throws Exception {
		var resp = clientB().post("/mcp").contentString(body(1, "server/discover", null))
			.header("Mcp-Method", "server/discover").header("Mcp-Name", "")
			.run().assertStatus(200).getContent().asString();
		assertContains("\"" + ResultMeta.KEY_SERVER_INFO + "\"", resp);
		assertContains("\"tools\"", resp);
	}

	@Test void b02_endpointMixin_toolsCall_dispatches() throws Exception {
		var resp = clientB().post("/mcp").contentString(body(1, "tools/call", JsonMap.of("name", "ping")))
			.header("Mcp-Method", "tools/call").header("Mcp-Name", "ping")
			.run().assertStatus(200).getContent().asString();
		assertContains("pong", resp);
	}

	// -------- default null capability hooks (now read through McpOptions) ---------

	@Test void c01_servletCapabilityHook_defaultsToNull() {
		assertNull(new A().createMcpOptions().getCapabilities());
	}

	@Test void c02_endpointCapabilityHook_defaultsToNull() {
		assertNull(new B().getMcpOptions().getCapabilities());
	}

	// -------- cache-config lifecycle, now folded into McpOptions ---------

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class E extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		static final AtomicInteger calls = new AtomicInteger();
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override
		protected McpOptions createMcpOptions() {
			calls.incrementAndGet();
			return new McpOptions().cache(c -> c.setToolsList(new McpCacheHint().setTtlMs(21)));
		}
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class F extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig();
		}
		@Override
		protected McpOptions createMcpOptions() {
			return null;
		}
	}

	@Rest(path = "/api", serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class G extends BasicRestServlet implements McpEndpoint {
		private static final long serialVersionUID = 1L;
		@Override
		public McpServerConfig getMcpConfig() {
			return new McpServerConfig().addTool(echo());
		}
		@Override
		public McpOptions getMcpOptions() {
			return new McpOptions().cache(c -> c.setToolsList(new McpCacheHint().setCacheScope(McpCacheScope.PRIVATE)));
		}
	}

	private static MockRestClient client(Class<?> c) {
		return MockRestClient.create(c).json().contentType("application/json").accept("application/json").build();
	}

	private MockRestClient clientBWithCache() {
		return MockRestClient.create(G.class).json().contentType("application/json").accept("application/json").build();
	}

	@Test void d01_servletOptions_isLazilyCachedAndInjected() throws Exception {
		var servlet = new E();
		assertSame(servlet.getMcpOptions(), servlet.getMcpOptions());
		assertEquals(1, E.calls.get());
		var body = client(E.class).post("/").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "").run().getContent().asString();
		assertContains("\"ttlMs\":21", body);
	}

	@Test void d02_servletNullOptionsFactoryFailsFast() {
		var f = new F();
		var e = assertThrows(IllegalStateException.class, f::getMcpOptions);
		assertEquals("createMcpOptions() returned null", e.getMessage());
	}

	@Test void d03_endpointDefaultIsEmptyAndOverrideIsInjected() throws Exception {
		assertNotNull(new B().getMcpOptions().getCache());
		var body = clientBWithCache().post("/mcp").contentString(body(1, "tools/list", null))
			.header("Mcp-Method", "tools/list").header("Mcp-Name", "").run().getContent().asString();
		assertContains("\"cacheScope\":\"private\"", body);
	}

	// -------- MRTR lifecycle, now folded into McpOptions ---------

	@Test void e01_servletOptions_isLazilyCachedAcrossCalls() {
		var servlet = new A();
		assertSame(servlet.getMcpOptions(), servlet.getMcpOptions());
	}

	@Rest(serializers = JsonSerializer.class, parsers = JsonParser.class, defaultAccept = "application/json")
	public static class I extends McpRestServlet {
		private static final long serialVersionUID = 1L;
		final AtomicInteger createCalls = new AtomicInteger();
		@Override
		protected McpServerConfig createMcpConfig() {
			return new McpServerConfig();
		}
		@Override
		protected McpOptions createMcpOptions() {
			createCalls.incrementAndGet();
			return new McpOptions();
		}
	}

	/**
	 * H2 regression: {@link McpRestServlet#getMcpOptions()} must publish exactly one {@link McpOptions} (and
	 * thus one MRTR AES key) even under a concurrent cold-start race, even though (mirroring
	 * {@code AbstractMcpRestServlet#getMcpConfig()}'s lock-free {@link java.util.concurrent.atomic.AtomicReference}
	 * pattern) {@link McpRestServlet#createMcpOptions()} itself MAY run more than once under that race: a
	 * losing thread's locally-computed instance (and its distinct, never-otherwise-used MRTR key) is simply
	 * discarded, never published, and never returned to any caller. Every concurrent first-access caller must
	 * observe the SAME published options instance regardless of how many times the factory itself ran.
	 */
	@Test void e02_servletOptions_concurrentFirstAccessPublishesExactlyOneInstance() throws Exception {
		var servlet = new I();
		var threads = 16;
		var pool = Executors.newFixedThreadPool(threads);
		try {
			var start = new CountDownLatch(1);
			var results = new ArrayList<Future<McpOptions>>();
			for (var i = 0; i < threads; i++)
				results.add(pool.submit(() -> { start.await(); return servlet.getMcpOptions(); }));
			start.countDown();
			var first = results.get(0).get();
			for (var f : results)
				assertSame(first, f.get(), "every concurrent first-access caller must observe the same published options");
			assertTrue(servlet.createCalls.get() >= 1, "createMcpOptions() must run at least once");
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void e03_endpointOptionsHook_defaultsToNonNull() {
		assertNotNull(new B().getMcpOptions().getMrtr());
	}

	/**
	 * CRITICAL regression: two {@link McpRevision} instances built from the SAME servlet binding &mdash;
	 * exactly as {@link McpRestServlet#revision()} constructs a fresh one per dispatched request &mdash;
	 * must share the same binding-owned {@link McpMrtrConfig}/{@link RequestStateCodec}, so a
	 * {@code requestState} sealed on a PAUSE request can be unsealed on a later RESUME request. A
	 * single-{@link McpRevision}-instance test would mask this: the bug only manifests across the
	 * per-request re-construction {@link McpRestServlet#revision()} performs on every dispatch.
	 */
	@Test void e04_mrtrConfigIsStableAcrossSeparateRevisionInstancesFromSameBinding() {
		var servlet = new A();
		var rev1 = (McpRevision)servlet.revision();
		var rev2 = (McpRevision)servlet.revision();
		assertNotSame(rev1, rev2, "revision() must construct a fresh McpRevision per call (per-request)");
		assertSame(rev1.mrtrConfig(), rev2.mrtrConfig(), "the MRTR config must be memoized at the binding level");

		var codec1 = rev1.mrtrConfig().getCodec();
		var codec2 = rev2.mrtrConfig().getCodec();
		assertSame(codec1, codec2, "the codec (and its AES key) must be memoized at the binding level");

		var state = new McpRequestState("resume-here", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", "args-hash-1");
		var token = codec1.seal(state, "tools/call" + '\u0000' + "2026-07-28");
		var unsealed = codec2.unseal(token, "tools/call" + '\u0000' + "2026-07-28");
		assertTrue(unsealed.isPresent(), "a requestState sealed on one request must unseal on the next");
		assertEquals(state, unsealed.get());
	}

	/**
	 * Mixin-path analogue of {@link #e04_mrtrConfigIsStableAcrossSeparateRevisionInstancesFromSameBinding}: two
	 * {@link McpRevision}s built through the SAME {@link McpEndpoint} instance's mixin default share the
	 * per-binding {@link McpMrtrConfig}/{@link RequestStateCodec} (memoized by {@link McpEndpointOptionsCache}),
	 * so a {@code requestState} sealed on a PAUSE request unseals on a later RESUME request &mdash; without ever
	 * standing up a full {@code RestContext} (bare construction).
	 */
	@Test void e05_mixinMrtrConfigIsStableAcrossSeparateRevisionInstancesFromSameEndpoint() {
		var b = new B();
		var rev1 = (McpRevision)b.revision();
		var rev2 = (McpRevision)b.revision();
		assertNotSame(rev1, rev2, "revision() must construct a fresh McpRevision per call (per-request)");
		assertSame(rev1.mrtrConfig(), rev2.mrtrConfig(), "the mixin MRTR config must be memoized at the binding level");

		var codec1 = rev1.mrtrConfig().getCodec();
		var codec2 = rev2.mrtrConfig().getCodec();
		assertSame(codec1, codec2, "the codec (and its AES key) must be memoized at the binding level");

		var state = new McpRequestState("resume-here", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", "args-hash-1");
		var token = codec1.seal(state, "tools/call" + '\u0000' + "2026-07-28");
		var unsealed = codec2.unseal(token, "tools/call" + '\u0000' + "2026-07-28");
		assertTrue(unsealed.isPresent(), "a requestState sealed on one request must unseal on the next");
		assertEquals(state, unsealed.get());
	}

	/**
	 * Regression: the pre-consolidation mixin default accidentally shared its MRTR key/broker
	 * JVM-wide ({@code SharedMrtrConfig}/{@code SharedSubscriptionBroker}) across every distinct endpoint
	 * instance. Post-consolidation, {@link McpEndpoint#getMcpOptions()}'s default is per-binding: two distinct
	 * endpoint instances must resolve to two distinct {@link McpOptions} (and therefore distinct MRTR keys),
	 * proving the accidental JVM-wide sharing is gone.
	 */
	@Test void e06_mixinMrtrConfig_distinctAcrossDistinctEndpointInstances() {
		var rev1 = (McpRevision)new B().revision();
		var rev2 = (McpRevision)new B().revision();
		assertNotSame(rev1.mrtrConfig(), rev2.mrtrConfig(),
			"two distinct endpoint instances must NOT share the same MRTR config (no JVM-wide sharing)");
		assertNotSame(rev1.mrtrConfig().getCodec(), rev2.mrtrConfig().getCodec(),
			"two distinct endpoint instances must NOT share the same AES key (no JVM-wide sharing)");
	}
}
