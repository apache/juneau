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
package org.apache.juneau.rest.server.remote;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Integration tests for {@link RrpcServlet}.
 */
@SuppressWarnings({
	"serial"  // serialVersionUID not required for test classes.
})
class RrpcServlet_Test extends TestBase {

	// =========================================================================
	// Test interface and implementation
	// =========================================================================

	public interface TestInterface {
		String echo(String input);
		int add(int a, int b);
		void doNothing();
		String throwException() throws Exception;
	}

	public static class TestInterfaceImpl implements TestInterface {
		@Override public String echo(String input) { return input; }
		@Override public int add(int a, int b) { return a + b; }
		@Override public void doNothing() { /* Intentionally empty: exercises RPC invocation of a void no-op interface method. */ }
		@Override public String throwException() throws Exception { throw new RuntimeException("test error"); }
	}

	// =========================================================================
	// Test servlet
	// =========================================================================

	@Rest
	public static class TestRrpcServlet extends RrpcServlet {
		private final Map<Class<?>,Object> services = Map.of(TestInterface.class, new TestInterfaceImpl());

		@Override
		protected Map<Class<?>,Object> getServiceMap() {
			return services;
		}
	}

	// =========================================================================
	// A — Servlet initialization and interface listing
	// =========================================================================

	@Test
	void a01_getInterfaces_returnsRegisteredInterface() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			try (var response = client.get("/").run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("TestInterface"), "Expected 'TestInterface' in response: " + body);
			}
		}
	}

	// =========================================================================
	// B — Route-not-found (404) paths
	// =========================================================================

	@Test
	void b01_interfaceNotFound_returns404() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			try (var response = client.get("/com.nonexistent.FakeInterface").run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}

	@Test
	void b02_methodNotFound_returns404() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			var iface = TestInterface.class.getName();
			try (var response = client.get("/" + iface + "/nonExistentMethod").run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}

	@Test
	void b03_invokeMethodNotFound_returns404() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			var iface = TestInterface.class.getName();
			try (var response = client.post("/" + iface + "/nonExistentMethod")
					.header("Content-Type", "application/json")
					.bodyString("[]")
					.run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}

	@Test
	void b04_invokeInterfaceNotFound_returns404() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			try (var response = client.post("/com.nonexistent.FakeInterface/someMethod")
					.header("Content-Type", "application/json")
					.bodyString("[]")
					.run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}

	// =========================================================================
	// C — Valid interface invocation
	// =========================================================================

	@Test
	void c01_invokeMethod_noArgs() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			var iface = TestInterface.class.getName();
			try (var response = client.post("/" + iface + "/" + urlEncode("doNothing/"))
					.header("Content-Type", "application/json")
					.bodyString("[]")
					.run()) {
				assertEquals(200, response.getStatusCode());
			}
		}
	}

	@Test
	void c02_invokeMethod_withArgs_returnsResult() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			var iface = TestInterface.class.getName();
			try (var response = client.post("/" + iface + "/" + urlEncode("add/(int,int)"))
					.header("Content-Type", "application/json")
					.bodyString("[3,4]")
					.run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("7"), "Expected '7' in response: " + body);
			}
		}
	}

	@Test
	void c03_listMethods_returnsMethodList() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			var iface = TestInterface.class.getName();
			try (var response = client.get("/" + iface).run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("echo"), "Expected 'echo' in response: " + body);
				assertTrue(body.contains("add"), "Expected 'add' in response: " + body);
			}
		}
	}

	// =========================================================================
	// D — Error propagation (exception → HTTP 500)
	// =========================================================================

	@Test
	void d01_invokeMethod_throwsException_returns500() throws Exception {
		try (var client = MockRestClient.create(TestRrpcServlet.class)) {
			var iface = TestInterface.class.getName();
			try (var response = client.post("/" + iface + "/" + urlEncode("throwException/"))
					.header("Content-Type", "application/json")
					.bodyString("[]")
					.run()) {
				assertEquals(500, response.getStatusCode());
			}
		}
	}
}
