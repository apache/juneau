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
package org.apache.juneau.bean.jsonrpc;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the JSON-RPC 2.0 envelope beans and {@link JsonRpcResponse}'s static helpers.
 */
class JsonRpcBeans_Test {

	private static void assertRoundTrip(Object bean, Class<?> type) {
		var a = JsonSerializer.DEFAULT.write(bean);
		var b = JsonSerializer.DEFAULT.write(JsonParser.DEFAULT.read(a, type));
		assertEquals(a, b, () -> "Round-trip JSON mismatch for " + type.getName() + ": " + a);
	}

	@Test
	void a01_request_roundTrip() {
		var a = new JsonRpcRequest().setJsonrpc("2.0").setId("abc").setMethod("tools/list").setParams(JsonMap.of("cursor", "1"));
		assertRoundTrip(a, JsonRpcRequest.class);
	}

	@Test
	void a02_response_resultRoundTrip() {
		var a = new JsonRpcResponse().setJsonrpc("2.0").setId(42).setResult(JsonMap.of("x", 1));
		assertRoundTrip(a, JsonRpcResponse.class);
	}

	@Test
	void a03_response_errorRoundTrip() {
		var a = new JsonRpcResponse().setJsonrpc("2.0").setId(7)
			.setError(new JsonRpcError().setCode(-32600).setMessage("Invalid Request").setData(JsonMap.of("detail", "x")));
		assertRoundTrip(a, JsonRpcResponse.class);
	}

	@Test
	void a04_error_roundTrip() {
		assertRoundTrip(new JsonRpcError().setCode(-32603).setMessage("boom").setData(JsonMap.of("type", "X")), JsonRpcError.class);
	}

	@Test
	void b01_notification_isNullId() {
		assertTrue(JsonRpcResponse.notification(null));
		assertFalse(JsonRpcResponse.notification(1));
		assertFalse(JsonRpcResponse.notification("abc"));
	}

	@Test
	void b02_ok_setsVersionIdAndResult() {
		var a = JsonRpcResponse.ok(1, JsonMap.of("k", "v"));
		assertEquals("2.0", a.getJsonrpc());
		assertEquals(1, a.getId());
		assertNotNull(a.getResult());
		assertNull(a.getError());
	}

	@Test
	void b03_errorResponse_fourArg_carriesData() {
		var a = JsonRpcResponse.errorResponse(1, -32603, "boom", JsonMap.of("type", "X"));
		assertEquals("2.0", a.getJsonrpc());
		assertEquals(1, a.getId());
		assertNull(a.getResult());
		assertEquals(-32603, a.getError().getCode());
		assertEquals("boom", a.getError().getMessage());
		assertNotNull(a.getError().getData());
	}

	@Test
	void b04_errorResponse_threeArg_leavesDataNull() {
		var a = JsonRpcResponse.errorResponse(null, -32600, "Request envelope is null");
		assertNull(a.getId());
		assertEquals(-32600, a.getError().getCode());
		assertNull(a.getError().getData());
	}

	@Test
	void c01_requestMeta_roundTripsEveryJsonCategory() {
		for (var value : List.of(JsonMap.of("p", "v"), JsonList.of(1, 2), "x", 7, true)) {
			var json = JsonSerializer.DEFAULT.write(new JsonRpcRequest().setMeta(value));
			var bean = JsonParser.DEFAULT.read(json, JsonRpcRequest.class);
			assertEquals(JsonSerializer.DEFAULT.write(value), JsonSerializer.DEFAULT.write(bean.getMeta()));
			assertTrue(json.contains("\"_meta\""));
		}
	}

	@Test
	void c02_responseMeta_roundTripsAndNullIsOmitted() {
		var json = JsonSerializer.DEFAULT.write(new JsonRpcResponse().setMeta(JsonMap.of("x", 1)));
		assertTrue(json.contains("\"_meta\""));
		assertNotNull(JsonParser.DEFAULT.read(json, JsonRpcResponse.class).getMeta());
		assertFalse(JsonSerializer.DEFAULT.write(new JsonRpcResponse().setMeta(null)).contains("_meta"));
	}

	@Test
	void c03_envelopesRemainMcpNeutral() {
		for (var type : List.of(JsonRpcRequest.class, JsonRpcResponse.class)) {
			var names = Arrays.stream(type.getMethods()).map(Method::getName).toList();
			assertFalse(names.contains("getProtocolVersion"));
			assertFalse(names.contains("getClientCapabilities"));
			assertEquals(List.of(Object.class), Arrays.stream(type.getDeclaredFields())
				.filter(x -> x.getName().equals("meta")).map(Field::getType).toList());
		}
	}
}
