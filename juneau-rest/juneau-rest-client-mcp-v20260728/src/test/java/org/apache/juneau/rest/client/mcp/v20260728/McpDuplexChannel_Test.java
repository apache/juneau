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
import java.util.concurrent.atomic.*;

import org.apache.juneau.bean.jsonrpc.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

class McpDuplexChannel_Test {

	@Test
	void a01_openDuplexChannel_dispatchesOpaqueRequest_andPostsRawResponse() throws Exception {
		var inbound = "data: {\"jsonrpc\":\"2.0\",\"id\":\"42\",\"method\":\"sampling/createMessage\",\"params\":{\"name\":\"opaque\",\"experimental\":{\"x\":1}}}\n\n";
		var posted = new AtomicReference<String>();
		var first = new AtomicBoolean(true);
		HttpTransport transport = tReq -> {
			if (first.getAndSet(false))
				return TransportResponse.builder().statusCode(200).header("Content-Type", "text/event-stream")
					.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();
			try {
				var out = new ByteArrayOutputStream();
				tReq.getBody().writeTo(out);
				posted.set(out.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new TransportException(e.getMessage(), e);
			}
			return TransportResponse.builder().statusCode(200).header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"42\",\"result\":{\"ok\":true}}".getBytes(StandardCharsets.UTF_8))).build();
		};

		var c = McpClient.builder().endpoint("http://x/mcp").transport(transport).build();
		var seenParamType = new AtomicReference<Class<?>>();
		c.setServerRequestHandler((request, ctx) -> {
			seenParamType.set(request.getParams().getClass());
			return JsonMap.of("status", "handled");
		});

		c.pumpNextServerMessage();

		assertEquals(JsonMap.class, seenParamType.get(), "duplex path must keep params untyped");
		assertTrue(posted.get().contains("\"status\":\"handled\""), posted.get());
		assertTrue(posted.get().contains("\"id\":\"42\""), posted.get());
	}

	@Test
	void a02_handlerFailure_postsJsonRpcErrorEnvelope() throws Exception {
		var inbound = "data: {\"jsonrpc\":\"2.0\",\"id\":\"77\",\"method\":\"sampling/createMessage\",\"params\":{\"name\":\"opaque\"}}\n\n";
		var posted = new AtomicReference<String>();
		var first = new AtomicBoolean(true);
		HttpTransport transport = tReq -> {
			if (first.getAndSet(false))
				return TransportResponse.builder().statusCode(200).header("Content-Type", "text/event-stream")
					.body(new ByteArrayInputStream(inbound.getBytes(StandardCharsets.UTF_8))).build();
			try {
				var out = new ByteArrayOutputStream();
				tReq.getBody().writeTo(out);
				posted.set(out.toString(StandardCharsets.UTF_8));
			} catch (IOException e) {
				throw new TransportException(e.getMessage(), e);
			}
			return TransportResponse.builder().statusCode(200).header("Content-Type", "application/json")
				.body(new ByteArrayInputStream("{\"jsonrpc\":\"2.0\",\"id\":\"77\",\"result\":{}}".getBytes(StandardCharsets.UTF_8))).build();
		};

		var c = McpClient.builder().endpoint("http://x/mcp").transport(transport).build();
		c.setServerRequestHandler((request, ctx) -> {
			throw new McpException(-32603, "handler failed");
		});
		c.pumpNextServerMessage();

		assertTrue(posted.get().contains("\"error\""), posted.get());
		assertTrue(posted.get().contains("\"code\":-32603"), posted.get());
	}
}
