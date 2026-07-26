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
package org.apache.juneau.rest.server.rrpc;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * End-to-end coverage for the {@code @RestOp(method="RRPC")} dispatch path
 * ({@link RrpcRestOpContext} / {@link RrpcRestOpSession}).
 *
 * <p>
 * Before this file, the only tests referencing these two classes were {@code RrpcRestOpSession_Test}'s
 * reflection-only method-signature checks -- no test actually exercised the feature end-to-end, leaving both
 * classes at 25-36% coverage per the TEST-04 findings.
 *
 * <p>
 * Covers both halves of the dispatch path: the {@code GET} method-path listing and the {@code POST}
 * method-invocation round trip, plus the {@link RrpcRestOpContext} constructor's zero-remote-method guard.
 * The {@code POST} side previously 404'd unconditionally at dispatch (a genuine framework defect in
 * {@code RrpcRestOpSession.run()}'s method-signature-path extraction, since fixed).
 */
class RrpcRestOpContext_Test extends TestBase {

	@Remote
	public interface Calculator {
		int add(int a, int b);
		String greet(String name);
	}

	public static class CalculatorImpl implements Calculator {
		@Override public int add(int a, int b) { return a + b; }
		@Override public String greet(String name) { return "Hello, " + name; }
	}

	@Rest(parsers={JsonParser.class}, serializers={JsonSerializer.class})
	public static class RrpcResource {
		@RestOp(method="RRPC", path="/calc/*")
		public Calculator getCalculator() {
			return new CalculatorImpl();
		}
	}

	@Test void a01_get_listsMethodPaths() throws Exception {
		try (var client = MockRestClient.create(RrpcResource.class)) {
			try (var response = client.get("/calc").run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("add"), "Expected 'add' in response: " + body);
				assertTrue(body.contains("greet"), "Expected 'greet' in response: " + body);
			}
		}
	}

	@Test void a02_get_listsMethodPaths_ignoresPathSuffix() throws Exception {
		// The RRPC GET handler always returns the full method list regardless of any sub-path -- verify this
		// holds even when a (structurally valid) method-signature suffix is present in the request path.
		try (var client = MockRestClient.create(RrpcResource.class)) {
			try (var response = client.get("/calc/add/(int,int)").run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("add"));
				assertTrue(body.contains("greet"));
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Interface with no remote methods -- RrpcRestOpContext's InternalServerError guard
	//------------------------------------------------------------------------------------------------------------------

	@Remote
	public interface EmptyInterface { /* No methods -- exercises the "no remote methods" guard. */ }

	@Rest
	public static class EmptyRrpcResource {
		@RestOp(method="RRPC", path="/empty/*")
		public EmptyInterface getEmpty() {
			return new EmptyInterface() { /* Anonymous no-op implementation. */ };
		}
	}

	@Test void b01_emptyInterface_failsOnFirstRequest() throws Exception {
		// RrpcRestOpContext's constructor throws InternalServerError when the RRPC method's return-type
		// interface declares zero remote methods. Per-op contexts are built lazily (on first matching
		// request), so MockRestClient.create() itself succeeds; the failure surfaces as a 500 on dispatch.
		try (var client = MockRestClient.create(EmptyRrpcResource.class)) {
			try (var response = client.get("/empty").run()) {
				assertEquals(500, response.getStatusCode());
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// POST -- method invocation round trip
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_post_invokesMethod_withArgs_returnsResult() throws Exception {
		try (var client = MockRestClient.create(RrpcResource.class)) {
			try (var response = client.post("/calc/" + urlEncode("add/(int,int)"))
					.header("Content-Type", "application/json")
					.bodyString("[3,4]")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("7", response.getBodyAsString());
			}
		}
	}

	@Test void c02_post_invokesMethod_withStringArg_returnsResult() throws Exception {
		try (var client = MockRestClient.create(RrpcResource.class)) {
			try (var response = client.post("/calc/" + urlEncode("greet/(java.lang.String)"))
					.header("Content-Type", "application/json")
					.bodyString("[\"world\"]")
					.run()) {
				assertEquals(200, response.getStatusCode());
				assertEquals("\"Hello, world\"", response.getBodyAsString());
			}
		}
	}

	@Test void c03_post_unknownMethodSignature_returns404() throws Exception {
		// A structurally-plausible but unregistered method signature must still 404 -- confirms the fix
		// doesn't over-match (e.g. by falling back to the base path or ignoring the signature entirely).
		try (var client = MockRestClient.create(RrpcResource.class)) {
			try (var response = client.post("/calc/" + urlEncode("subtract/(int,int)"))
					.header("Content-Type", "application/json")
					.bodyString("[3,4]")
					.run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}
}
