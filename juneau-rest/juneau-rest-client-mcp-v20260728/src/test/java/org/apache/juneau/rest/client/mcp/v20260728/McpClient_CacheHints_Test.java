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

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.mcp.*;
import org.junit.jupiter.api.*;

class McpClient_CacheHints_Test {

	static final class RecordingCache implements McpResponseCache {
		String lastScope;
		String lastKey;
		Object lastValue;
		long lastTtl;

		@Override
		public Optional<Object> get(String scope, String key) {
			return Optional.empty();
		}

		@Override
		public void put(String scope, String key, Object value, long ttlMs) {
			lastScope = scope;
			lastKey = key;
			lastValue = value;
			lastTtl = ttlMs;
		}

		@Override
		public void clear() {
			// Intentional no-op: this test double only records put() calls; clear() is never exercised here.
		}
	}

	@Test
	void a01_privateScope_usesPerInstancePartitionPrefix() throws Exception {
		var cache = new RecordingCache();
		try (
			var c1 = McpClient.builder().endpoint("http://x").responseCache(cache).build();
			var c2 = McpClient.builder().endpoint("http://x").responseCache(cache).build();
		) {
			assertNotEquals(c1.privateScopePartitionPrefix(), c2.privateScopePartitionPrefix());
		}
	}

	@Test
	void b01_privateScopeResult_isServedFromCacheOnSecondCall() throws Exception {
		var calls = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			var wire = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"ttlMs\":60000,\"cacheScope\":\"private\",\"tools\":[{\"name\":\"echo\"}]}}";
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var c = McpClient.builder()
			.endpoint("http://x/mcp")
			.transport(transport)
			.responseCache(new InMemoryMcpResponseCache())
			.build()) {
			var a = c.listTools();
			var b = c.listTools();
			assertEquals(1, calls.get(), "second identical call must be served from the private-scope cache partition");
			assertSame(a, b);
		}
	}
}
