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

import java.util.Map;

import org.apache.juneau.bean.jsonrpc.JsonRpcRequest;
import org.apache.juneau.bean.mcp.v20260728.McpProtocol;
import org.apache.juneau.bean.mcp.v20260728.RequestMeta;
import org.apache.juneau.commons.concurrent.InMemoryReplayCache;
import org.apache.juneau.commons.inject.BasicBeanStore;
import org.apache.juneau.marshall.collections.JsonMap;
import org.apache.juneau.rest.server.mcp.McpExchange;
import org.apache.juneau.rest.server.mcp.McpResponseResult;
import org.apache.juneau.rest.server.mcp.McpServerConfig;
import org.junit.jupiter.api.Test;

/**
 * Coverage for {@link McpMrtrConfig} and the {@link McpRevision} four-arg constructor that carries it.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class McpMrtrConfig_Test {

	@Test void a01_defaultsAreAeadCodecFiveMinuteTtlTenMaxRounds() {
		var a = new McpMrtrConfig();
		assertInstanceOf(AeadRequestStateCodec.class, a.getCodec());
		assertEquals(McpMrtrConfig.DEFAULT_TTL_MS, a.getTtlMs());
		assertEquals(5 * 60 * 1000L, a.getTtlMs());
		assertEquals(McpMrtrConfig.DEFAULT_MAX_ROUNDS, a.getMaxRounds());
		assertEquals(10, a.getMaxRounds());
	}

	@Test void a02_setCodecNullThrows() {
		var config = new McpMrtrConfig();
		var e = assertThrows(IllegalArgumentException.class, () -> config.setCodec(null));
		assertEquals("codec must not be null", e.getMessage());
	}

	@Test void a03_setTtlMsZeroThrows() {
		var config = new McpMrtrConfig();
		var e = assertThrows(IllegalArgumentException.class, () -> config.setTtlMs(0));
		assertEquals("ttlMs 0 must be > 0", e.getMessage());
	}

	@Test void a04_setTtlMsNegativeThrows() {
		var config = new McpMrtrConfig();
		var e = assertThrows(IllegalArgumentException.class, () -> config.setTtlMs(-1));
		assertEquals("ttlMs -1 must be > 0", e.getMessage());
	}

	@Test void a05_setMaxRoundsZeroThrows() {
		var config = new McpMrtrConfig();
		var e = assertThrows(IllegalArgumentException.class, () -> config.setMaxRounds(0));
		assertEquals("maxRounds 0 must be >= 1", e.getMessage());
	}

	@Test void a06_validChainRoundTripsThroughGetters() {
		var codec = new AeadRequestStateCodec();
		var a = new McpMrtrConfig().setCodec(codec).setTtlMs(60_000L).setMaxRounds(3);
		assertSame(codec, a.getCodec());
		assertEquals(60_000L, a.getTtlMs());
		assertEquals(3, a.getMaxRounds());
	}

	@Test void a07_setKeyProviderWiresAeadCodecSharingTheProvider() {
		// "behaves": prove the KeyProvider is actually threaded through, not just type-checked -- two
		// independently-constructed McpMrtrConfig instances sharing one StaticKeyProvider unseal each other's
		// tokens, the config-level analog of the dispatch-level d04 test added in Task 11.
		var provider = StaticKeyProvider.of("k1", StaticKeyProvider.aesKey(new byte[32]));
		var a = new McpMrtrConfig().setKeyProvider(provider);
		assertInstanceOf(AeadRequestStateCodec.class, a.getCodec());
		var b = new McpMrtrConfig().setKeyProvider(provider);
		var state = new McpRequestState("cont-1", "tools/call", 1, System.currentTimeMillis() + 60_000L, "jti-1", "args-hash-1");
		var token = a.getCodec().seal(state, "aad");
		var unsealed = b.getCodec().unseal(token, "aad").orElseThrow();
		assertEquals("cont-1", unsealed.continuation());
	}

	@Test void a08_setKeyProviderAndSetCodecAreLastWins() {
		var provider = StaticKeyProvider.of("k1", StaticKeyProvider.aesKey(new byte[32]));
		var explicitCodec = new AeadRequestStateCodec();
		// setCodec(...) called after setKeyProvider(...): the explicit codec wins.
		var a = new McpMrtrConfig().setKeyProvider(provider).setCodec(explicitCodec);
		assertSame(explicitCodec, a.getCodec());
		// setKeyProvider(...) called after setCodec(...): the provider-wrapping codec wins.
		var b = new McpMrtrConfig().setCodec(explicitCodec).setKeyProvider(provider);
		assertNotSame(explicitCodec, b.getCodec());
		assertInstanceOf(AeadRequestStateCodec.class, b.getCodec());
	}

	@Test void a09_setKeyProviderNullThrows() {
		var config = new McpMrtrConfig();
		var e = assertThrows(IllegalArgumentException.class, () -> config.setKeyProvider(null));
		assertEquals("keyProvider must not be null", e.getMessage());
	}

	@Test void a10_defaultReplayCacheIsNull() {
		// D1 (opt-in): replay rejection must be disabled by default -- no ReplayCache is auto-wired.
		assertNull(new McpMrtrConfig().getReplayCache());
	}

	@Test void a11_setReplayCacheRoundTripsThroughGetter() {
		var cache = new InMemoryReplayCache();
		var a = new McpMrtrConfig().setReplayCache(cache);
		assertSame(cache, a.getReplayCache());
	}

	@Test void a12_setReplayCacheNullExplicitlyDisablesIt() {
		// Unlike setCodec/setKeyProvider, null is a legal (and the default) value here -- it explicitly
		// disables replay rejection rather than being a programming error.
		var a = new McpMrtrConfig().setReplayCache(new InMemoryReplayCache()).setReplayCache(null);
		assertNull(a.getReplayCache());
	}

	// -------- McpRevision four-arg constructor wiring ---------

	private static Object validMeta() {
		return JsonMap.of(
			RequestMeta.KEY_PROTOCOL_VERSION, "2026-07-28",
			RequestMeta.KEY_CLIENT_CAPABILITIES, JsonMap.of());
	}

	private static Object withMeta(Object meta) {
		var p = new JsonMap();
		p.put("_meta", meta);
		return p;
	}

	private static JsonRpcRequest discoverRequest() {
		return new JsonRpcRequest().setJsonrpc(McpProtocol.JSON_RPC_2_0).setId(1)
			.setMethod("server/discover").setParams(withMeta(validMeta()));
	}

	@Test void b01_fourArgConstructorAcceptsMrtrConfigAndDispatchesNormally() {
		var rev = new McpRevision(null, new McpCacheConfig(), "instructions", new McpMrtrConfig());
		var headers = Map.of("Mcp-Method", "server/discover", "Mcp-Name", "");
		var result = rev.dispatch(new McpExchange(discoverRequest(), headers::get), new McpServerConfig(), new BasicBeanStore());
		assertInstanceOf(McpResponseResult.class, result);
		assertNull(((McpResponseResult) result).response().getError());
	}

	@Test void b02_threeArgConstructorStillCompilesAndDispatchesNormally() {
		var rev = new McpRevision(null, new McpCacheConfig(), "instructions");
		var headers = Map.of("Mcp-Method", "server/discover", "Mcp-Name", "");
		var result = rev.dispatch(new McpExchange(discoverRequest(), headers::get), new McpServerConfig(), new BasicBeanStore());
		assertInstanceOf(McpResponseResult.class, result);
		assertNull(((McpResponseResult) result).response().getError());
	}

	@Test void b03_fourArgConstructorNullMrtrConfigThrowsNpe() {
		var cacheConfig = new McpCacheConfig();
		assertThrows(NullPointerException.class,
			() -> new McpRevision(null, cacheConfig, "instructions", null));
	}
}
