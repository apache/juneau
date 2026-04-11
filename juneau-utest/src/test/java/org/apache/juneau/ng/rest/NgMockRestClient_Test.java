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
package org.apache.juneau.ng.rest;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.rest.mock.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.junit.jupiter.api.*;

/**
 * Integration tests for {@link NgMockRestClient}.
 */
public class NgMockRestClient_Test {

	// =================================================================================================================
	// Test REST resources
	// =================================================================================================================

	@Rest(path="/api")
	public static class EchoResource {
		@RestGet
		public String get() {
			return "got";
		}

		@RestPost
		public String post(@Content String body) {
			return "posted:" + body;
		}

		@RestPut
		public String put(@Content String body) {
			return "put:" + body;
		}

		@RestDelete
		public String delete() {
			return "deleted";
		}

		@RestOp(method="PATCH")
		public String patch(@Content String body) {
			return "patched:" + body;
		}

		@RestGet(path="/echo-header")
		public String echoHeader(RestRequest req) {
			return req.getHeader("X-Custom");
		}

		@RestGet(path="/not-found-path-that-will-404")
		public String notFound() {
			return "unreachable";
		}
	}

	@Rest
	public static class RootResource {
		@RestGet
		public String get() {
			return "root-ok";
		}
	}

	// Used only with instances (not by class) to cover the isClass=false branch in NgMockRestClient.Builder.build()
	@Rest
	public static class InstanceOnlyResource {
		@RestGet
		public String get() {
			return "instance-ok";
		}
	}

	// Resource with 204 No Content to test empty-body response handling
	@Rest
	public static class NoContentResource {
		@RestDelete
		public void delete(RestResponse res) {
			res.setStatus(204);
		}
	}

	// =================================================================================================================
	// A — Basic HTTP methods
	// =================================================================================================================

	@Test
	void a01_get_basic() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.get("/").run()) {
				assertEquals(200, response.getStatusCode());
				assertNotNull(response.getBodyAsString());
			}
		}
	}

	@Test
	void a02_post_withBody() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.post("/").body(StringBody.of("world", "text/plain")).run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("posted"), "Expected 'posted' in response: " + body);
			}
		}
	}

	@Test
	void a03_put_withBody() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.put("/").body(StringBody.of("data", "text/plain")).run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("put"), "Expected 'put' in response: " + body);
			}
		}
	}

	@Test
	void a04_delete() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.delete("/").run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("deleted"), "Expected 'deleted' in response: " + body);
			}
		}
	}

	@Test
	void a05_patch_withBody() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.patch("/").body(StringBody.of("fix", "text/plain")).run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("patched"), "Expected 'patched' in response: " + body);
			}
		}
	}

	@Test
	void a06_request_explicitMethod() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.request("GET", "/").run()) {
				assertEquals(200, response.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// B — Request headers and responses
	// =================================================================================================================

	@Test
	void b01_customRequestHeader() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.get("/echo-header").header("X-Custom", "my-value").run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("my-value"), "Expected 'my-value' in response: " + body);
			}
		}
	}

	@Test
	void b02_statusCode404_unknownPath() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			try (var response = client.get("/does-not-exist-at-all").run()) {
				assertEquals(404, response.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// C — Factory variants and builder
	// =================================================================================================================

	@Test
	void c01_createWithInstance() throws Exception {
		var instance = new RootResource();
		try (var client = NgMockRestClient.create(instance)) {
			try (var response = client.get("/").run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("root-ok"), "Expected 'root-ok' in response: " + body);
			}
		}
	}

	@Test
	void c02_createWithClass() throws Exception {
		try (var client = NgMockRestClient.create(RootResource.class)) {
			try (var response = client.get("/").run()) {
				assertEquals(200, response.getStatusCode());
			}
		}
	}

	@Test
	void c03_builder() throws Exception {
		try (var client = NgMockRestClient.builder(RootResource.class).build()) {
			try (var response = client.get("/").run()) {
				assertEquals(200, response.getStatusCode());
			}
		}
	}

	@Test
	void c04_getClient_returnsNgRestClient() {
		try (var client = NgMockRestClient.create(RootResource.class)) {
			assertNotNull(client.getClient());
		} catch (IOException e) {
			fail("Unexpected exception: " + e.getMessage());
		}
	}

	// =================================================================================================================
	// D — Body content types
	// =================================================================================================================

	@Test
	void d01_postWithByteArrayBody() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			var bodyBytes = "hello".getBytes();
			try (var response = client.post("/").body(ByteArrayBody.of(bodyBytes, "text/plain")).run()) {
				assertEquals(200, response.getStatusCode());
			}
		}
	}

	@Test
	void d02_postWithInputStreamBody() throws Exception {
		try (var client = NgMockRestClient.create(EchoResource.class)) {
			var is = new ByteArrayInputStream("hello".getBytes());
			try (var response = client.post("/").body(StreamBody.of(is, "text/plain")).run()) {
				assertEquals(200, response.getStatusCode());
			}
		}
	}

	// =================================================================================================================
	// E — Additional coverage tests
	// =================================================================================================================

	@Test
	void e01_head_method() throws Exception {
		// HEAD is dispatched; the resource returns 405 because it only has @RestGet, not HEAD
		// This validates that head() dispatches the request correctly (not 500)
		try (var client = NgMockRestClient.create(RootResource.class)) {
			try (var response = client.head("/").run()) {
				assertTrue(response.getStatusCode() != 500, "Unexpected server error on HEAD");
			}
		}
	}

	@Test
	void e02_contextPath_builder() throws Exception {
		try (var client = NgMockRestClient.builder(RootResource.class).contextPath("").build()) {
			try (var response = client.get("/").run()) {
				assertEquals(200, response.getStatusCode());
			}
		}
	}

	@Test
	void e03_createWithInstance_isClassFalse() throws Exception {
		// Use InstanceOnlyResource (never passed as a Class) to exercise the isClass=false branch
		var instance = new InstanceOnlyResource();
		try (var client = NgMockRestClient.create(instance)) {
			try (var response = client.get("/").run()) {
				assertEquals(200, response.getStatusCode());
				var body = response.getBodyAsString();
				assertTrue(body.contains("instance-ok"), "Expected 'instance-ok' in response: " + body);
			}
		}
	}

	@Test
	void e04_emptyBodyResponse_noContent() throws Exception {
		try (var client = NgMockRestClient.create(NoContentResource.class)) {
			try (var response = client.delete("/").run()) {
				assertEquals(204, response.getStatusCode());
				// Body is null for 204 No Content
				assertNull(response.getBodyAsString());
			}
		}
	}
}
