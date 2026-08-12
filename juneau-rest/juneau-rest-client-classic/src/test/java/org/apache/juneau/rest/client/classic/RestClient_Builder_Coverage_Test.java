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
package org.apache.juneau.rest.client.classic;

import static org.apache.juneau.rest.RestSharedConstants.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.jcs.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.commons.httppart.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Exercises {@link RestClient.Builder} shortcut/config methods that aren't reached by the
 * {@code juneau-integration-tests} module's {@code RestClient_*_Test} suites -- mostly one-line delegate methods
 * (append/setDefault on the header, form-data, path-data, and query-data lists; media-type shortcuts; OpenAPI
 * format shortcuts) plus a couple of small branchy methods ({@code interceptors(...)}, {@code rootUrl(Object)}).
 * A local {@link HttpServer} fixture captures the request actually sent so these can be verified with real
 * assertions instead of just "didn't throw".
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class RestClient_Builder_Coverage_Test {

	private static HttpServer server;
	private static ExecutorService executor;
	private static int port;
	private static final AtomicReference<Headers> lastHeaders = new AtomicReference<>();
	private static final AtomicReference<String> lastQuery = new AtomicReference<>();

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();
		// Without an explicit executor, exchanges run on HttpServer's single internal dispatch thread, which
		// starves under -T1C reactor-level parallel test load and can fail with "server failed to respond".
		executor = Executors.newCachedThreadPool();
		server.setExecutor(executor);
		server.createContext("/echo", exchange -> {
			lastHeaders.set(exchange.getRequestHeaders());
			lastQuery.set(exchange.getRequestURI().getQuery());
			exchange.getRequestBody().readAllBytes();
			exchange.sendResponseHeaders(200, -1);
			exchange.close();
		});
		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
		if (executor != null)
			executor.shutdownNow();
	}

	private static String url() {
		return "http://localhost:" + port + "/echo";
	}

	@Test void a01_addInterceptorFirst() throws Exception {
		var itcp = (HttpResponseInterceptor)(response, context) -> {};
		try (var client = RestClient.create().addInterceptorFirst(itcp).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
	}

	@Test void a02_builderCopy_notImplemented() {
		var b = RestClient.create();
		assertThrows(NoSuchMethodError.class, b::copy);
	}

	@Test void a03_debugOutputLines() throws Exception {
		try (var client = RestClient.create().json().debugOutputLines(50).build()) {
			assertNotNull(client);
		}
	}

	@Test void a04_formData_supplier() {
		var b = RestClient.create();
		b.formData("foo", () -> "bar");
		assertEquals("bar", b.formData().get("foo").orElseThrow().getValue());
	}

	@Test void a05_formDataDefault() {
		var b = RestClient.create();
		b.formDataDefault(org.apache.juneau.http.classic.part.BasicPart.of("foo", "bar"));
		assertEquals("bar", b.formData().get("foo").orElseThrow().getValue());
	}

	@Test void a06_getRootUri_setAndUnset() {
		var withRoot = RestClient.create().rootUrl("http://example.com");
		assertEquals("http://example.com", withRoot.getRootUri());

		var withoutRoot = RestClient.create();
		assertNull(withoutRoot.getRootUri());
	}

	@Test void a07_header_supplier() throws Exception {
		try (var client = RestClient.create().header("X-Dyn", () -> "v1").build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("v1", lastHeaders.get().getFirst("X-Dyn"));
	}

	@Test void a08_serializerSessionOptionsHeader_string() throws Exception {
		try (var client = RestClient.create().serializerSessionOptionsHeader("{foo:'bar'}\"").build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNotNull(lastHeaders.get().getFirst(HEADER_JuneauSerializerOptions), "header not found; actual keys: " + lastHeaders.get().keySet());
	}

	@Test void a09_serializerSessionOptionsHeader_string_null_isNoOp() throws Exception {
		try (var client = RestClient.create().serializerSessionOptionsHeader((String)null).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastHeaders.get().getFirst(HEADER_JuneauSerializerOptions));
	}

	@Test void a10_parserSessionOptionsHeader_string() throws Exception {
		try (var client = RestClient.create().parserSessionOptionsHeader("{foo:'bar'}\"").build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNotNull(lastHeaders.get().getFirst(HEADER_JuneauParserOptions));
	}

	@Test void a11_parserSessionOptionsHeader_string_null_isNoOp() throws Exception {
		try (var client = RestClient.create().parserSessionOptionsHeader((String)null).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastHeaders.get().getFirst(HEADER_JuneauParserOptions));
	}

	@Test void a12_serializerSessionOptionsHeader_map_nonEmpty() throws Exception {
		try (var client = RestClient.create().serializerSessionOptionsHeader(Map.of("foo", "bar")).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNotNull(lastHeaders.get().getFirst(HEADER_JuneauSerializerOptions));
	}

	@Test void a13_serializerSessionOptionsHeader_map_empty_isNoOp() throws Exception {
		try (var client = RestClient.create().serializerSessionOptionsHeader(Map.of()).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastHeaders.get().getFirst(HEADER_JuneauSerializerOptions));
	}

	@Test void a14_parserSessionOptionsHeader_map_nonEmpty() throws Exception {
		try (var client = RestClient.create().parserSessionOptionsHeader(Map.of("foo", "bar")).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNotNull(lastHeaders.get().getFirst(HEADER_JuneauParserOptions));
	}

	@Test void a15_parserSessionOptionsHeader_map_empty_isNoOp() throws Exception {
		try (var client = RestClient.create().parserSessionOptionsHeader(Map.of()).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastHeaders.get().getFirst(HEADER_JuneauParserOptions));
	}

	@Test void a16_httpClientBuilder_custom() throws Exception {
		var hcb = org.apache.http.impl.client.HttpClientBuilder.create();
		try (var client = RestClient.create().httpClientBuilder(hcb).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
	}

	@Test void a17_interceptors_classVarargs_nullsSkippedAndValidClassApplied() throws Exception {
		var b = RestClient.create();
		b.interceptors((Class<?>)null, RecordingInterceptor.class);
		try (var client = b.build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertTrue(RecordingInterceptor.initCalled, "Interceptor class was not instantiated/registered");
	}

	@Test void a18_interceptors_classVarargs_invalidClass_throwsConfigException() {
		var b = RestClient.create();
		assertThrows(org.apache.juneau.marshall.ConfigException.class, () -> b.interceptors(String.class));
	}

	@Test void a19_interceptors_objectVarargs_nullsSkipped_andHttpRequestAndResponseAndRestCallInterceptors() throws Exception {
		var b = RestClient.create();
		var restCallItcp = new BasicRestCallInterceptor() {};
		b.interceptors(
			(Object)null,
			(HttpRequestInterceptor)(request, context) -> {},
			(HttpResponseInterceptor)(response, context) -> {},
			restCallItcp
		);
		// A second call exercises the "interceptors already non-null" branch (addAll vs assign).
		b.interceptors(new BasicRestCallInterceptor() {});
		try (var client = b.build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
	}

	@Test void a20_interceptors_objectVarargs_invalidObject_throwsConfigException() {
		var b = RestClient.create();
		assertThrows(org.apache.juneau.marshall.ConfigException.class, () -> b.interceptors("not an interceptor"));
	}

	@Test void a21_jcs_setsSerializerAndParser() throws Exception {
		try (var client = RestClient.create().jcs().build()) {
			assertEquals(JcsSerializer.class, client.getMatchingSerializer(null).getClass());
			assertEquals(JsonParser.class, client.getMatchingParser(null).getClass());
		}
	}

	@Test void a22_bson_setsSerializerAndParser() throws Exception {
		try (var client = RestClient.create().bson().build()) {
			assertEquals(BsonSerializer.class, client.getMatchingSerializer(null).getClass());
			assertEquals(BsonParser.class, client.getMatchingParser(null).getClass());
		}
	}

	@Test void a23_mediaType_setsAcceptAndContentType() throws Exception {
		try (var client = RestClient.create().json().mediaType("application/json").build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertEquals("application/json", lastHeaders.get().getFirst("Accept"));
	}

	@Test void a24_oapiCollectionFormat() throws Exception {
		try (var client = RestClient.create().openApi().oapiCollectionFormat(HttpPartCollectionFormat.PIPES).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
	}

	@Test void a25_oapiFormat() throws Exception {
		try (var client = RestClient.create().openApi().oapiFormat(HttpPartFormat.UON).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
	}

	@Test void a26_pathData_varargsAndNameValueAndSupplierAndDefault() {
		var b = RestClient.create();
		b.pathData(org.apache.juneau.http.classic.part.BasicPart.of("a", "1"));
		b.pathData("b", "2");
		b.pathData("c", () -> "3");
		b.pathDataDefault(org.apache.juneau.http.classic.part.BasicPart.of("d", "4"));
		assertEquals("1", b.pathData().get("a").orElseThrow().getValue());
		assertEquals("2", b.pathData().get("b").orElseThrow().getValue());
		assertEquals("3", b.pathData().get("c").orElseThrow().getValue());
		assertEquals("4", b.pathData().get("d").orElseThrow().getValue());
	}

	@Test void a27_queryData_supplierAndDefault() throws Exception {
		try (var client = RestClient.create()
				.queryData("dyn", () -> "v")
				.queryDataDefault(org.apache.juneau.http.classic.part.BasicPart.of("def", "d"))
				.build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		var query = lastQuery.get();
		assertTrue(query.contains("dyn=v"), "Expected dyn=v in: " + query);
		assertTrue(query.contains("def=d"), "Expected def=d in: " + query);
	}

	@Test void a28_serializerSessionOptionsQueryDefault_string() throws Exception {
		try (var client = RestClient.create().serializerSessionOptionsQueryDefault("(foo=bar)").build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertTrue(lastQuery.get().contains(QUERY_juneauSerializerOptions), "Actual query: " + lastQuery.get());
	}

	@Test void a29_serializerSessionOptionsQueryDefault_string_null_isNoOp() throws Exception {
		try (var client = RestClient.create().serializerSessionOptionsQueryDefault((String)null).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastQuery.get());
	}

	@Test void a30_parserSessionOptionsQueryDefault_string() throws Exception {
		try (var client = RestClient.create().parserSessionOptionsQueryDefault("(foo=bar)").build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertTrue(lastQuery.get().contains(QUERY_juneauParserOptions), "Actual query: " + lastQuery.get());
	}

	@Test void a31_parserSessionOptionsQueryDefault_string_null_isNoOp() throws Exception {
		try (var client = RestClient.create().parserSessionOptionsQueryDefault((String)null).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastQuery.get());
	}

	@Test void a32_serializerSessionOptionsQueryDefault_map_nonEmptyAndEmpty() throws Exception {
		try (var client = RestClient.create().serializerSessionOptionsQueryDefault(Map.of("foo", "bar")).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertTrue(lastQuery.get().contains(QUERY_juneauSerializerOptions), "Actual query: " + lastQuery.get());

		try (var client = RestClient.create().serializerSessionOptionsQueryDefault(Map.of()).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastQuery.get());
	}

	@Test void a33_parserSessionOptionsQueryDefault_map_nonEmptyAndEmpty() throws Exception {
		try (var client = RestClient.create().parserSessionOptionsQueryDefault(Map.of("foo", "bar")).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertTrue(lastQuery.get().contains(QUERY_juneauParserOptions), "Actual query: " + lastQuery.get());

		try (var client = RestClient.create().parserSessionOptionsQueryDefault(Map.of()).build();
				var req = client.get(url());
				var res = req.run()) {
			assertEquals(200, res.getStatusCode());
		}
		assertNull(lastQuery.get());
	}

	@Test void a34_rootUrl_valid_empty_andInvalid() {
		var b1 = RestClient.create().rootUrl("http://example.com/foo/");
		assertEquals("http://example.com/foo", b1.getRootUri());

		var b2 = RestClient.create().rootUrl("");
		assertNull(b2.getRootUri());

		var b3 = RestClient.create();
		var e = assertThrows(RuntimeException.class, () -> b3.rootUrl("not-a-url"));
		assertTrue(e.getMessage().contains("Invalid rootUrl"), "Unexpected message: " + e.getMessage());
	}

	@Test void a35_urlEncodingSerializer_lazyInitThenCached() {
		var b = RestClient.create();
		var s1 = b.urlEncodingSerializer();
		var s2 = b.urlEncodingSerializer();
		assertSame(s1, s2, "Second call should return the cached instance, not re-create it");
	}

	/**
	 * Fixed: {@code skipEmptyFormData(boolean)}, {@code skipEmptyHeaderData(boolean)}, and
	 * {@code skipEmptyQueryData(boolean)} now honor their {@code value} parameter instead of hardcoding the field
	 * to {@code true}, so e.g. {@code skipEmptyFormData(false)} correctly disables the setting.
	 */
	@Test void a36_skipEmptyXxxData_booleanArg_honored() throws Exception {
		try (var c1 = RestClient.create().skipEmptyFormData(true).build()) {
			assertTrue(isSkipEmptyFormData(c1));
		}
		try (var c2 = RestClient.create().skipEmptyFormData(false).build()) {
			assertFalse(isSkipEmptyFormData(c2));
		}
		try (var c3 = RestClient.create().skipEmptyHeaderData(true).build()) {
			assertTrue(isSkipEmptyHeaderData(c3));
		}
		try (var c4 = RestClient.create().skipEmptyHeaderData(false).build()) {
			assertFalse(isSkipEmptyHeaderData(c4));
		}
		try (var c5 = RestClient.create().skipEmptyQueryData(true).build()) {
			assertTrue(isSkipEmptyQueryData(c5));
		}
		try (var c6 = RestClient.create().skipEmptyQueryData(false).build()) {
			assertFalse(isSkipEmptyQueryData(c6));
		}
	}

	// Package-private accessors on RestClient (isSkipEmptyFormData() etc.) are `protected`, so they're already
	// callable directly from this same-package test without any visibility change.
	private static boolean isSkipEmptyFormData(RestClient c) { return callProtected(c, "isSkipEmptyFormData"); }
	private static boolean isSkipEmptyHeaderData(RestClient c) { return callProtected(c, "isSkipEmptyHeaderData"); }
	private static boolean isSkipEmptyQueryData(RestClient c) { return callProtected(c, "isSkipEmptyQueryData"); }

	private static boolean callProtected(RestClient c, String method) {
		try {
			var m = RestClient.class.getDeclaredMethod(method);
			m.setAccessible(true);
			return (boolean)m.invoke(c);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/** Records whether it was instantiated by {@code interceptors(Class...)}. */
	public static class RecordingInterceptor extends BasicRestCallInterceptor {
		static boolean initCalled = false;
		@Override
		public void onInit(RestRequest req) {
			initCalled = true;
		}
	}
}
