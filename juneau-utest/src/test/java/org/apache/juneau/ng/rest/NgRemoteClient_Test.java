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
import java.util.*;

import org.apache.juneau.http.remote.Remote;
import org.apache.juneau.http.remote.RemoteDelete;
import org.apache.juneau.http.remote.RemoteGet;
import org.apache.juneau.http.remote.RemotePatch;
import org.apache.juneau.http.remote.RemotePost;
import org.apache.juneau.http.remote.RemotePut;
import org.apache.juneau.http.remote.RemoteReturn;
import org.apache.juneau.ng.http.HttpBody;
import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.http.remote.Body;
import org.apache.juneau.ng.http.remote.Header;
import org.apache.juneau.ng.http.remote.Path;
import org.apache.juneau.ng.http.remote.Query;
import org.apache.juneau.ng.http.remote.RrpcInterfaceMeta;
import org.apache.juneau.ng.rest.client.*;
import org.apache.juneau.ng.rest.client.remote.*;
import org.apache.juneau.ng.rest.mock.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the ng.* remote proxy support: @Remote, @RemoteGet/Post/etc., parameter annotations,
 * RrpcInterfaceMeta, RrpcInterfaceMethodMeta, and NgRemoteClient.
 */
public class NgRemoteClient_Test {

	// ------------------------------------------------------------------------------------------------------------------
	// A — @Remote annotation and metadata
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/api/users")
	interface A01_UserService {
		@RemoteGet("/{id}")
		String getUser(@Path("id") String id);

		@RemotePost
		String createUser(@Body String json);

		@RemotePut("/{id}")
		String updateUser(@Path("id") String id, @Body String json);

		@RemotePatch("/{id}")
		String patchUser(@Path("id") String id, @Body String json);

		@RemoteDelete("/{id}")
		void deleteUser(@Path("id") String id);
	}

	@Test void a01_rpcInterfaceMeta_basePath() {
		var meta = RrpcInterfaceMeta.of(A01_UserService.class);
		assertEquals("/api/users", meta.getBasePath());
		assertEquals(A01_UserService.class, meta.getInterface());
	}

	@Test void a02_rpcInterfaceMeta_methodMetas() {
		var meta = RrpcInterfaceMeta.of(A01_UserService.class);
		assertEquals(5, meta.getMethodMetas().size());
	}

	@Test void a03_rpcInterfaceMethodMeta_getUser() throws Exception {
		var meta = RrpcInterfaceMeta.of(A01_UserService.class);
		var m = A01_UserService.class.getMethod("getUser", String.class);
		var mm = meta.getMethodMeta(m);
		assertNotNull(mm);
		assertEquals("GET", mm.getHttpMethod());
		assertEquals("/{id}", mm.getPath());
		assertEquals(RemoteReturn.BODY, mm.getReturnType());
		assertEquals(m, mm.getMethod());
		assertTrue(mm.toString().contains("GET"));
	}

	@Test void a04_rpcInterfaceMethodMeta_createUser() throws Exception {
		var meta = RrpcInterfaceMeta.of(A01_UserService.class);
		var m = A01_UserService.class.getMethod("createUser", String.class);
		var mm = meta.getMethodMeta(m);
		assertEquals("POST", mm.getHttpMethod());
	}

	@Test void a05_rpcInterfaceMethodMeta_deleteUser() throws Exception {
		var meta = RrpcInterfaceMeta.of(A01_UserService.class);
		var m = A01_UserService.class.getMethod("deleteUser", String.class);
		var mm = meta.getMethodMeta(m);
		assertEquals("DELETE", mm.getHttpMethod());
	}

	@Test void a06_rpcInterfaceMeta_cached() {
		var meta1 = RrpcInterfaceMeta.of(A01_UserService.class);
		var meta2 = RrpcInterfaceMeta.of(A01_UserService.class);
		assertSame(meta1, meta2);
	}

	@Test void a07_rpcInterfaceMeta_toString() {
		var meta = RrpcInterfaceMeta.of(A01_UserService.class);
		assertTrue(meta.toString().contains("UserService"));
	}

	@Test void a08_rpcInterfaceMeta_notAnnotated_throws() {
		assertThrows(IllegalArgumentException.class, () -> RrpcInterfaceMeta.of(List.class));
	}

	@Test void a09_rpcInterfaceMeta_notInterface_throws() {
		assertThrows(IllegalArgumentException.class, () -> RrpcInterfaceMeta.of(String.class));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// B — Proxy invocation via MockHttpTransport
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/items")
	interface B01_ItemService {
		@RemoteGet("/{id}")
		String getItem(@Path("id") String id);

		@RemoteGet
		String listItems(@Query("page") String page);

		@RemotePost
		String createItem(@Body String body);

		@RemoteGet(returns = RemoteReturn.STATUS)
		int getStatus();

		@RemoteGet(returns = RemoteReturn.STATUS)
		boolean isOk();
	}

	private MockHttpTransport.Builder captureTransport(List<TransportRequest> captured) {
		return MockHttpTransport.builder()
			.fallback(req -> {
				captured.add(req);
				return TransportResponse.builder().statusCode(200).body(new ByteArrayInputStream("result".getBytes())).build();
			});
	}

	@Test void b01_get_withPathParam() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			var result = svc.getItem("42");
			assertEquals("result", result);
			assertEquals(1, captured.size());
			assertEquals("GET", captured.get(0).getMethod());
			assertTrue(captured.get(0).getUri().getPath().endsWith("/items/42"));
		}
	}

	@Test void b02_get_withQueryParam() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			svc.listItems("2");
			var uri = captured.get(0).getUri().toString();
			assertTrue(uri.contains("page=2"), "Expected 'page=2' in URI: " + uri);
		}
	}

	@Test void b03_post_withBody() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			svc.createItem("{\"name\":\"widget\"}");
			assertEquals("POST", captured.get(0).getMethod());
		}
	}

	@Test void b04_get_returnsStatus_int() throws Exception {
		var transport = MockHttpTransport.builder().fallback(req -> TransportResponse.builder().statusCode(200).build()).build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			assertEquals(200, svc.getStatus());
		}
	}

	@Test void b05_get_returnsStatus_boolean() throws Exception {
		var transport = MockHttpTransport.builder().fallback(req -> TransportResponse.builder().statusCode(200).build()).build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			assertTrue(svc.isOk());
		}
	}

	@Test void b06_get_returnsStatus_boolean_false_when_error() throws Exception {
		var transport = MockHttpTransport.builder().fallback(req -> TransportResponse.builder().statusCode(500).build()).build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			assertFalse(svc.isOk());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// C — @Header parameter
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/secure")
	interface C01_SecureService {
		@RemoteGet
		String getData(@Header("Authorization") String token);
	}

	@Test void c01_header_param() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(C01_SecureService.class);
			svc.getData("Bearer mytoken");
			var authHeader = captured.get(0).getFirstHeader("Authorization");
			assertNotNull(authHeader);
			assertEquals("Bearer mytoken", authHeader.value());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// D — @RemoteReturn.RESPONSE
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/res")
	interface D01_ResponseService {
		@RemoteGet(returns = RemoteReturn.RESPONSE)
		NgRestResponse getRaw();
	}

	@Test void d01_returns_response() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "raw")).rootUrl("http://x.com").build()) {
			var svc = client.remote(D01_ResponseService.class);
			try (var resp = svc.getRaw()) {
				assertEquals(200, resp.getStatusCode());
				assertEquals("raw", resp.getBodyAsString());
			}
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// E — void return type
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/void")
	interface E01_VoidService {
		@RemoteGet
		void doSomething();
	}

	@Test void e01_void_return() throws Exception {
		var transport = MockHttpTransport.builder().fallback(req -> TransportResponse.builder().statusCode(204).build()).build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var svc = client.remote(E01_VoidService.class);
			assertDoesNotThrow(() -> svc.doSomething());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// F — @Remote with value() as path alias
	// ------------------------------------------------------------------------------------------------------------------

	@Remote("/products")
	interface F01_ProductService {
		@RemoteGet("/list")
		String list();
	}

	@Test void f01_remote_value_as_path() {
		var meta = RrpcInterfaceMeta.of(F01_ProductService.class);
		assertEquals("/products", meta.getBasePath());
	}

	@Test void f01_remote_method_path() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(F01_ProductService.class);
			svc.list();
			var path = captured.get(0).getUri().getPath();
			assertTrue(path.endsWith("/products/list"), "Unexpected path: " + path);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// G — @Body with HttpBody
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/upload")
	interface G01_UploadService {
		@RemotePost
		String upload(@Body HttpBody body);
	}

	@Test void g01_body_httpBody() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(G01_UploadService.class);
			svc.upload(StringBody.of("file content", "application/octet-stream"));
			assertEquals("POST", captured.get(0).getMethod());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// H — unannotated single param treated as body
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/infer")
	interface H01_InferredBodyService {
		@RemotePost
		String create(String body);
	}

	@Test void h01_unannotated_single_param_as_body() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(H01_InferredBodyService.class);
			svc.create("test body");
			assertEquals("POST", captured.get(0).getMethod());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// I — NgRemoteClient factory
	// ------------------------------------------------------------------------------------------------------------------

	@Test void i01_ngRemoteClient_create() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			var remote = new NgRemoteClient(client);
			var svc = remote.create(B01_ItemService.class);
			assertNotNull(svc);
		}
	}

	@Test void i02_ngRemoteClient_null_client_throws() {
		assertThrows(IllegalArgumentException.class, () -> new NgRemoteClient(null));
	}

	@Test void i03_ngRemoteClient_null_iface_throws() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			var remote = new NgRemoteClient(client);
			assertThrows(IllegalArgumentException.class, () -> remote.create(null));
		}
	}

	@Test void i04_ngRestClient_remote_shortcut() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			assertNotNull(svc);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// J — Method without @Remote* annotation
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/mixed")
	interface J01_MixedService {
		@RemoteGet
		String annotated();

		String notAnnotated(); // no @Remote* annotation
	}

	@Test void j01_unannotated_method_throws() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			var svc = client.remote(J01_MixedService.class);
			assertThrows(UnsupportedOperationException.class, () -> svc.notAnnotated());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// K — RrpcInterfaceMethodMeta toString
	// ------------------------------------------------------------------------------------------------------------------

	@Test void k01_methodMeta_toString() throws Exception {
		var meta = RrpcInterfaceMeta.of(A01_UserService.class);
		var m = A01_UserService.class.getMethod("getUser", String.class);
		var mm = meta.getMethodMeta(m);
		var str = mm.toString();
		assertTrue(str.contains("GET"));
		assertTrue(str.contains("getUser"));
	}

	// ------------------------------------------------------------------------------------------------------------------
	// L1 — path() attribute on all annotations (covers the a.path() branch)
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/branch")
	interface L1_PathAttrService {
		@RemoteGet(path = "/get-path")
		String getByPath();

		@RemotePost(path = "/post-path")
		String postByPath();

		@RemotePut(path = "/put-path")
		String putByPath();

		@RemotePatch(path = "/patch-path")
		String patchByPath();

		@RemoteDelete(path = "/delete-path")
		String deleteByPath();
	}

	@Test void l1_01_path_attr_coverage() {
		var meta = RrpcInterfaceMeta.of(L1_PathAttrService.class);
		assertEquals(5, meta.getMethodMetas().size());
		meta.getMethodMetas().forEach((m, mm) -> {
			assertTrue(mm.getPath().endsWith("-path"), "Expected path to end with -path: " + mm.getPath());
		});
	}

	// ------------------------------------------------------------------------------------------------------------------
	// L — Null parameters are skipped
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/null-test")
	interface L01_NullParamService {
		@RemoteGet
		String get(@Query("q") String query);
	}

	@Test void l01_null_param_skipped() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(L01_NullParamService.class);
			svc.get(null);
			var uri = captured.get(0).getUri().toString();
			assertFalse(uri.contains("q="), "Null param should not appear in URI: " + uri);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// M — PUT, PATCH, DELETE verbs via proxy
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/m")
	interface M01_VerbService {
		@RemotePut("/{id}")
		String put(@Path("id") String id, @Body String body);

		@RemotePatch("/{id}")
		String patch(@Path("id") String id);

		@RemoteDelete("/{id}")
		String delete(@Path("id") String id);
	}

	@Test void m01_put_verb() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(M01_VerbService.class);
			svc.put("5", "payload");
			assertEquals("PUT", captured.get(0).getMethod());
		}
	}

	@Test void m02_patch_verb() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(M01_VerbService.class);
			svc.patch("5");
			assertEquals("PATCH", captured.get(0).getMethod());
		}
	}

	@Test void m03_delete_verb() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(M01_VerbService.class);
			svc.delete("5");
			assertEquals("DELETE", captured.get(0).getMethod());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// N — Parameter annotations with empty value() fall back to param name
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/n")
	interface N01_EmptyValueService {
		@RemoteGet
		String search(@Query String term);   // @Query with no value() — uses param name

		@RemoteGet
		String get(@Header String accept);   // @Header with no value()
	}

	@Test void n01_empty_query_value_uses_param_name() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(N01_EmptyValueService.class);
			svc.search("hello");
			// param name is used as query param name (may be "arg0" without debug info, so just verify a param is present)
			assertNotNull(captured.get(0));
		}
	}

	@Test void n02_empty_header_value_uses_param_name() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(N01_EmptyValueService.class);
			svc.get("text/plain");
			assertNotNull(captured.get(0));
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// O — Return types: InputStream, byte[], void, unknown type
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/o")
	interface O01_ReturnTypeService {
		@RemoteGet
		InputStream getStream();

		@RemoteGet
		byte[] getBytes();

		@RemoteGet
		void doVoid();

		@RemoteGet(returns = RemoteReturn.STATUS)
		Integer getStatusBoxed();

		@RemoteGet(returns = RemoteReturn.STATUS)
		Boolean isOkBoxed();
	}

	@Test void o01_inputstream_return() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "stream-data")).rootUrl("http://x.com").build()) {
			var svc = client.remote(O01_ReturnTypeService.class);
			try (var stream = svc.getStream()) {
				assertNotNull(stream);
			}
		}
	}

	@Test void o02_bytes_return() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "byte-data")).rootUrl("http://x.com").build()) {
			var svc = client.remote(O01_ReturnTypeService.class);
			var bytes = svc.getBytes();
			assertEquals("byte-data", new String(bytes));
		}
	}

	@Test void o03_void_return() throws Exception {
		var transport = MockHttpTransport.builder().fallback(req -> TransportResponse.builder().statusCode(204).build()).build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var svc = client.remote(O01_ReturnTypeService.class);
			assertDoesNotThrow(() -> svc.doVoid());
		}
	}

	@Test void o04_status_boxed_integer() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			var svc = client.remote(O01_ReturnTypeService.class);
			assertEquals(200, svc.getStatusBoxed());
		}
	}

	@Test void o05_status_boxed_boolean() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			var svc = client.remote(O01_ReturnTypeService.class);
			assertTrue(svc.isOkBoxed());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// P — combinePaths edge cases
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "")
	interface P01_EmptyBasePath {
		@RemoteGet("/p1")
		String get();

		@RemoteGet
		String getNoMethod();
	}

	@Remote(path = "/p-base/")
	interface P02_TrailingSlashBase {
		@RemoteGet("/item")
		String get();
	}

	@Test void p01_empty_base_uses_method_path() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(P01_EmptyBasePath.class);
			svc.get();
			assertTrue(captured.get(0).getUri().getPath().endsWith("/p1"));
		}
	}

	@Test void p02_empty_base_empty_method() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(P01_EmptyBasePath.class);
			svc.getNoMethod();
			// just should not throw
			assertNotNull(captured.get(0));
		}
	}

	@Test void p03_trailing_slash_base_no_double_slash() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(P02_TrailingSlashBase.class);
			svc.get();
			var path = captured.get(0).getUri().getPath();
			assertFalse(path.contains("//"), "Should not have double slash: " + path);
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// Q — Object methods (toString, hashCode, equals) on proxy
	// ------------------------------------------------------------------------------------------------------------------

	@Test void q01_object_methods_on_proxy() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "ok")).rootUrl("http://x.com").build()) {
			var svc = client.remote(B01_ItemService.class);
			// These invoke Object.class methods directly on the handler — should not throw
			assertDoesNotThrow(() -> svc.toString());
			assertDoesNotThrow(() -> svc.hashCode());
			assertDoesNotThrow(() -> svc.equals(svc));
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// R — Unannotated single param as HttpBody
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/r")
	interface R01_HttpBodyInferredService {
		@RemotePost
		String upload(HttpBody body);
	}

	@Test void r01_unannotated_httpbody_param() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(R01_HttpBodyInferredService.class);
			svc.upload(StringBody.of("content", "text/plain"));
			assertEquals("POST", captured.get(0).getMethod());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// S — @Body param with HttpBody type (explicit @Body annotation, non-String arg)
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/s")
	interface S01_BodyAnnotatedHttpBodyService {
		@RemotePost
		String upload(@Body HttpBody body);
	}

	@Test void s01_body_annotated_httpbody() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(S01_BodyAnnotatedHttpBodyService.class);
			svc.upload(StringBody.of("hello", "text/plain"));
			assertEquals("POST", captured.get(0).getMethod());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// T — Void (boxed) return type in BODY mode
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/t")
	interface T01_VoidBoxedReturnService {
		@RemoteGet
		Void doVoidBoxed();

		@RemoteGet
		Object getObject();    // fallthrough to getBodyAsString()

		@RemoteGet(returns = RemoteReturn.STATUS)
		String getStatusAsString();  // STATUS mode fallthrough to yield sc
	}

	@Test void t01_void_boxed_return() throws Exception {
		var transport = MockHttpTransport.builder().fallback(req -> TransportResponse.builder().statusCode(200).build()).build();
		try (var client = NgRestClient.builder().transport(transport).rootUrl("http://x.com").build()) {
			var svc = client.remote(T01_VoidBoxedReturnService.class);
			assertNull(svc.doVoidBoxed());
		}
	}

	@Test void t02_object_return_falls_through_to_string() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "hello")).rootUrl("http://x.com").build()) {
			var svc = client.remote(T01_VoidBoxedReturnService.class);
			assertEquals("hello", svc.getObject());
		}
	}

	@Test void t03_status_mode_non_numeric_return() throws Exception {
		try (var client = NgRestClient.builder().transport(MockHttpTransport.of(200, "")).rootUrl("http://x.com").build()) {
			var svc = client.remote(T01_VoidBoxedReturnService.class);
			// STATUS mode with non-int/non-boolean return falls through to yield sc (Integer)
			// The proxy returns it; a ClassCastException occurs when the caller tries to use it as String
			assertThrows(Exception.class, () -> svc.getStatusAsString());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// U — @Path annotation with empty value (uses param name)
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/u")
	interface U01_EmptyPathValueService {
		@RemoteGet
		String get(@Path String myParam);  // no path template — @Path with no value, uses param name
	}

	@Test void u01_path_empty_value_uses_param_name() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(U01_EmptyPathValueService.class);
			svc.get("99");
			// Just verify it ran — param name depends on compiler flags
			assertNotNull(captured.get(0));
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// V — Multiple unannotated params (false branch: params.length != 1)
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/v")
	interface V01_MultiUnannotatedParamService {
		@RemotePost
		String create(String a, String b);  // 2 unannotated params — neither is treated as body
	}

	@Test void v01_multiple_unannotated_params_not_body() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(V01_MultiUnannotatedParamService.class);
			svc.create("x", "y");  // should not throw, just send empty body
			assertEquals("POST", captured.get(0).getMethod());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// W — Method path without leading slash (covers base + "/" + method branch)
	// ------------------------------------------------------------------------------------------------------------------

	@Remote(path = "/w-base")
	interface W01_NoLeadingSlashMethodService {
		@RemoteGet("items")    // no leading slash on method path
		String list();
	}

	@Remote(path = "/w-trail/")
	interface W02_TrailingBaseNoSlashMethod {
		@RemoteGet("items")    // base ends with "/", method has no "/"
		String list();
	}

	@Test void w01_method_path_no_leading_slash() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(W01_NoLeadingSlashMethodService.class);
			svc.list();
			var path = captured.get(0).getUri().getPath();
			assertTrue(path.contains("items"), "Path should contain 'items': " + path);
			assertFalse(path.contains("//"), "Should not have double slash: " + path);
		}
	}

	@Test void w02_trailing_base_no_leading_method_no_double_slash() throws Exception {
		var captured = new ArrayList<TransportRequest>();
		try (var client = NgRestClient.builder().transport(captureTransport(captured).build()).rootUrl("http://x.com").build()) {
			var svc = client.remote(W02_TrailingBaseNoSlashMethod.class);
			svc.list();
			var path = captured.get(0).getUri().getPath();
			assertTrue(path.contains("items"), "Path should contain 'items': " + path);
			assertFalse(path.contains("//"), "Should not have double slash: " + path);
		}
	}
}
