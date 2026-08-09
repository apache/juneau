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
import org.apache.juneau.bean.mcp.v20260728.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.client.mcp.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Mock HttpTransport lambdas are short-lived test fixtures whose clients are already closed via try-with-resources.
})
class McpClient_TraceContext_Test {

	@Test
	void a01_traceFieldsSupplier_stampsRequestMeta() throws Exception {
		var seenBody = new AtomicReference<String>();
		HttpTransport transport = tReq -> {
			try {
				var out = new ByteArrayOutputStream();
				tReq.getBody().writeTo(out);
				seenBody.set(out.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new TransportException(e.getMessage(), e);
			}
			var json = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"resultType\":\"complete\",\"_meta\":{\"io.modelcontextprotocol/serverInfo\":{\"name\":\"s\",\"version\":\"1\"}},\"contents\":[]}}";
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var c = McpClient.builder()
			.endpoint("http://x/mcp")
			.transport(transport)
			.traceFieldsSupplier(() -> Map.of(
				RequestMeta.KEY_TRACEPARENT, "00-parent-01",
				RequestMeta.KEY_TRACESTATE, "v=x",
				RequestMeta.KEY_BAGGAGE, "u=42"))
			.build()) {
			c.readResource("file:///a");
		}
		assertTrue(seenBody.get().contains("\"traceparent\":\"00-parent-01\""));
		assertTrue(seenBody.get().contains("\"tracestate\":\"v=x\""));
		assertTrue(seenBody.get().contains("\"baggage\":\"u=42\""));
	}

	@Test
	void a02_perCallVaryingTraceparent_doesNotDefeatCache() throws Exception {
		var calls = new AtomicInteger();
		var traceparentSeq = new AtomicInteger();
		HttpTransport transport = tReq -> {
			calls.incrementAndGet();
			var json = "{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"result\":{\"ttlMs\":60000,\"cacheScope\":\"public\",\"resultType\":\"complete\",\"_meta\":{\"io.modelcontextprotocol/serverInfo\":{\"name\":\"s\",\"version\":\"1\"}},\"contents\":[]}}";
			return TransportResponse.builder()
				.statusCode(200)
				.header("Content-Type", "application/json")
				.body(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
				.build();
		};
		try (var c = McpClient.builder()
			.endpoint("http://x/mcp")
			.transport(transport)
			.responseCache(new InMemoryMcpResponseCache())
			.traceFieldsSupplier(() -> Map.of(RequestMeta.KEY_TRACEPARENT, "00-parent-" + traceparentSeq.incrementAndGet()))
			.build()) {
			var a = c.readResource("file:///a");
			var b = c.readResource("file:///a");
			assertEquals(1, calls.get(), "a per-call traceparent supplier must not defeat the response cache");
			assertSame(a, b);
		}
	}
}
