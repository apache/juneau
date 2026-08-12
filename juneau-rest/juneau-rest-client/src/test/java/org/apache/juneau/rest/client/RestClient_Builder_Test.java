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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.http.part.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RestClient.Builder}'s default-header / default-query-data accumulators
 * ({@link RestClient.Builder#headers(HttpHeader...)}, {@link RestClient.Builder#queryData(HttpPart...)},
 * {@link RestClient.Builder#queryData(String, String)}), which are applied to every {@link RestRequest} built
 * from the client.
 */
@SuppressWarnings("resource") // 'tReq' is inspected synchronously by the fake HttpTransport lambda; not owned by the test.
class RestClient_Builder_Test extends TestBase {

	@Test
	void a01_headersVarargs_appliedToEveryRequest() throws Exception {
		TransportRequest[] captured = new TransportRequest[1];
		HttpTransport transport = tReq -> {
			captured[0] = tReq;
			return TransportResponse.builder().statusCode(200).build();
		};
		try (var client = RestClient.builder().transport(transport)
				.headers(HttpHeaderBean.of("X-A", "1"), HttpHeaderBean.of("X-B", "2"))
				.build()) {
			try (var res = client.get("http://x/").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		var names = captured[0].getHeaders().stream().map(TransportHeader::name).toList();
		assertTrue(names.contains("X-A"));
		assertTrue(names.contains("X-B"));
	}

	@Test
	void a02_headerNameValue_eagerlyEvaluated_appliedToEveryRequest() throws Exception {
		TransportRequest[] captured = new TransportRequest[1];
		HttpTransport transport = tReq -> {
			captured[0] = tReq;
			return TransportResponse.builder().statusCode(200).build();
		};
		try (var client = RestClient.builder().transport(transport).header("X-Eager", "v1").build()) {
			try (var res = client.get("http://x/").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		var h = captured[0].getHeaders().stream().filter(x -> "X-Eager".equals(x.name())).findFirst().orElseThrow();
		assertEquals("v1", h.value());
	}

	@Test
	void a03_headerNameSupplier_evaluatedPerRequest() throws Exception {
		TransportRequest[] captured = new TransportRequest[1];
		HttpTransport transport = tReq -> {
			captured[0] = tReq;
			return TransportResponse.builder().statusCode(200).build();
		};
		var counter = new int[1];
		try (var client = RestClient.builder().transport(transport).header("X-Dyn", () -> "call-" + (++counter[0])).build()) {
			try (var res1 = client.get("http://x/").run()) {
				assertEquals(200, res1.getStatusCode());
			}
			var h1 = captured[0].getHeaders().stream().filter(x -> "X-Dyn".equals(x.name())).findFirst().orElseThrow();
			assertEquals("call-1", h1.value());

			try (var res2 = client.get("http://x/").run()) {
				assertEquals(200, res2.getStatusCode());
			}
			var h2 = captured[0].getHeaders().stream().filter(x -> "X-Dyn".equals(x.name())).findFirst().orElseThrow();
			assertEquals("call-2", h2.value());
		}
	}

	@Test
	void b01_queryDataVarargs_appliedToEveryRequest() throws Exception {
		TransportRequest[] captured = new TransportRequest[1];
		HttpTransport transport = tReq -> {
			captured[0] = tReq;
			return TransportResponse.builder().statusCode(200).build();
		};
		try (var client = RestClient.builder().transport(transport)
				.queryData(HttpPartBean.of("a", "1"), HttpPartBean.of("b", "2"))
				.build()) {
			try (var res = client.get("http://x/").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		var query = captured[0].getUri().getRawQuery();
		assertTrue(query.contains("a=1"), "Expected a=1 in: " + query);
		assertTrue(query.contains("b=2"), "Expected b=2 in: " + query);
	}

	@Test
	void b02_queryDataNameValue_appliedToEveryRequest() throws Exception {
		TransportRequest[] captured = new TransportRequest[1];
		HttpTransport transport = tReq -> {
			captured[0] = tReq;
			return TransportResponse.builder().statusCode(200).build();
		};
		try (var client = RestClient.builder().transport(transport).queryData("c", "3").build()) {
			try (var res = client.get("http://x/").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		var query = captured[0].getUri().getRawQuery();
		assertTrue(query.contains("c=3"), "Expected c=3 in: " + query);
	}
}
