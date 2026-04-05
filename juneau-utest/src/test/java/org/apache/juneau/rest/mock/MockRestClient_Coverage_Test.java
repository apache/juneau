// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                               *
// ***************************************************************************************************************************
package org.apache.juneau.rest.mock;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.RestDelete;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for MockRestClient.
 */
class MockRestClient_Coverage_Test extends TestBase {

	@Rest
	public static class A {
		@RestGet
		public String get() {
			return "ok";
		}

		@RestPost
		public String post() {
			return "posted";
		}

		@RestPut
		public String put() {
			return "put";
		}

		@RestDelete
		public String delete() {
			return "deleted";
		}

		@RestOp(method="OPTIONS")
		public String options() {
			return "options";
		}
	}

	@Rest
	public static class B {
		@RestGet
		public String get() {
			return "ok-b";
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// MockRestClient.create() with an instance (not a class)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a00_createWithInstance() throws Exception {
		// Create MockRestClient with a pre-instantiated REST resource (not a class)
		// This covers the `restBean instanceof Class` false branch
		var instance = new B();
		MockRestClient
			.create(instance)
			.build()
			.get("/")
			.run()
			.assertStatus(200)
			.assertContent("ok-b");
	}

	//------------------------------------------------------------------------------------------------------------------
	// getCurrentXxx() methods - returns values during request execution
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_getCurrentClientRequest() throws Exception {
		var client = MockRestClient.create(A.class).build();
		// Before a request, the value is null
		assertNull(client.getCurrentClientRequest());
	}

	@Test void a02_getCurrentClientResponse() throws Exception {
		var client = MockRestClient.create(A.class).build();
		assertNull(client.getCurrentClientResponse());
	}

	@Test void a03_getCurrentServerRequest() throws Exception {
		var client = MockRestClient.create(A.class).build();
		assertNull(client.getCurrentServerRequest());
	}

	@Test void a04_getCurrentServerResponse() throws Exception {
		var client = MockRestClient.create(A.class).build();
		assertNull(client.getCurrentServerResponse());
	}

	@Test void a05_getMetrics() throws Exception {
		var client = MockRestClient.create(A.class).build();
		assertNull(client.getMetrics());
	}

	@Test void a06_getSocketTimeout() throws Exception {
		var client = MockRestClient.create(A.class).build();
		assertEquals(Integer.MAX_VALUE, client.getSocketTimeout());
	}

	@Test void a07_isOpen() throws Exception {
		var client = MockRestClient.create(A.class).build();
		assertTrue(client.isOpen());
	}

	@Test void a08_isResponseAvailable() throws Exception {
		var client = MockRestClient.create(A.class).build();
		assertTrue(client.isResponseAvailable(100));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Various request methods
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_get_basic() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.get("/")
			.run()
			.assertStatus(200)
			.assertContent("ok");
	}

	@Test void b02_post_withBody() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.post("/")
			.run()
			.assertStatus(200)
			.assertContent("posted");
	}

	@Test void b03_request_method() throws Exception {
		// Test request() method with explicit method string
		MockRestClient
			.create(A.class)
			.build()
			.request("GET", "/")
			.run()
			.assertStatus(200);
	}

	@Test void b04_delete_request() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.delete("/")
			.run()
			.assertStatus(200)
			.assertContent("deleted");
	}

	@Test void b05_put_request() throws Exception {
		MockRestClient
			.create(A.class)
			.build()
			.put("/")
			.run()
			.assertStatus(200)
			.assertContent("put");
	}

	//------------------------------------------------------------------------------------------------------------------
	// getCurrentXxx() values during request - capture via interceptors
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_currentServerRequest_duringExecution() throws Exception {
		var client = MockRestClient.create(A.class).build();
		client.get("/").run();
		// After execution, ThreadLocal should be set
		assertNotNull(client.getCurrentServerRequest());
	}

	@Test void c02_currentServerResponse_duringExecution() throws Exception {
		var client = MockRestClient.create(A.class).build();
		client.get("/").run();
		// After execution, ThreadLocal should be set
		assertNotNull(client.getCurrentServerResponse());
	}

	//------------------------------------------------------------------------------------------------------------------
	// MockHttpClientConnectionManager
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_connectionManager_equals() {
		var cm1 = new MockHttpClientConnectionManager();
		var cm2 = new MockHttpClientConnectionManager();
		assertEquals(cm1, cm2); // All are equal
		assertEquals(cm1, cm1); // Same instance
		assertNotEquals(cm1, null); // Not equal to null
		assertNotEquals(cm1, "string"); // Not equal to different type
	}

	@Test void d02_connectionManager_hashCode() {
		var cm1 = new MockHttpClientConnectionManager();
		var cm2 = new MockHttpClientConnectionManager();
		assertEquals(cm1.hashCode(), cm2.hashCode()); // Same hash
	}

	@Test void d03_connectionManager_noOpMethods() throws Exception {
		var cm = new MockHttpClientConnectionManager();
		// These should be no-ops
		assertDoesNotThrow(() -> cm.closeExpiredConnections());
		assertDoesNotThrow(() -> cm.closeIdleConnections(10, java.util.concurrent.TimeUnit.SECONDS));
		assertDoesNotThrow(() -> cm.connect(null, null, 0, null));
		assertDoesNotThrow(() -> cm.releaseConnection(null, null, 0, java.util.concurrent.TimeUnit.SECONDS));
		assertDoesNotThrow(() -> cm.routeComplete(null, null, null));
		assertDoesNotThrow(() -> cm.shutdown());
		assertDoesNotThrow(() -> cm.upgrade(null, null, null));
	}

	@Test void d04_connectionManager_requestConnection() throws Exception {
		var client = MockRestClient.create(A.class).build();
		// Verify the connection manager works through normal request execution
		client.get("/").run().assertStatus(200);
	}
}
