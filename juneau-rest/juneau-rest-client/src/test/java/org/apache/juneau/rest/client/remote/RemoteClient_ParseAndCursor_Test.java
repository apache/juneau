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
package org.apache.juneau.rest.client.remote;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.entity.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.stream.*;
import org.apache.juneau.rest.client.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for {@code RemoteClient$RemoteInvocationHandler}'s bean-body parsing ({@code parseBody}/{@code selectParser})
 * and cursor return mode ({@code processCursor}), plus a couple of remaining {@code @Part}/{@code toPartBody}
 * branches ({@link HttpBody}-typed part, bean part with no default serializer, and a non-{@code @Part} parameter
 * mixed into a {@code @Multipart} method).
 */
@SuppressWarnings({
	"resource" // RestClient/RestResponse instances used inline; closed via try-with-resources where needed.
})
class RemoteClient_ParseAndCursor_Test extends TestBase {

	private static HttpServer server;
	private static int port;

	public static final class Widget {
		private String name;
		public String getName() { return name; }
		public void setName(String value) { name = value; }
	}

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(0), 0);
		port = server.getAddress().getPort();

		server.createContext("/widget", exchange -> {
			var body = "{\"name\":\"gizmo\"}".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/empty-widget", exchange -> exchange.sendResponseHeaders(200, -1));

		server.createContext("/malformed-json", exchange -> {
			var body = "{not valid json".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/json-unlabeled", exchange -> {
			// No Content-Type header at all: the response Content-Type never matches a registered parser, so
			// selectParser must fall through to the method-level "accept" fallback.
			var body = "{\"name\":\"fromFallback\"}".getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/not-found", exchange -> {
			var body = "missing".getBytes(StandardCharsets.UTF_8);
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(404, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.createContext("/echo-multipart", exchange -> {
			var body = exchange.getRequestBody().readAllBytes();
			exchange.getResponseHeaders().add("Content-Type", "text/plain");
			exchange.sendResponseHeaders(200, body.length);
			exchange.getResponseBody().write(body);
			exchange.close();
		});

		server.start();
	}

	@AfterAll
	static void stopServer() {
		if (server != null)
			server.stop(0);
	}

	private String rootUrl() {
		return "http://localhost:" + port;
	}

	// -----------------------------------------------------------------------
	// a — parseBody: null body, ParseException leniency (Object.class) vs. rethrow (declared type)
	// -----------------------------------------------------------------------

	@Remote
	interface ParseBodyService {
		@RemoteGet("/empty-widget")
		Widget getWidgetFromEmptyBody();

		@RemoteGet("/malformed-json")
		Widget getWidgetFromMalformedJson();

		@RemoteGet("/malformed-json")
		Object getObjectFromMalformedJson_lenientlyReturnsRawString();

		@RemoteGet(path = "/json-unlabeled", accept = "application/json")
		Widget getWidgetViaAcceptFallback();
	}

	@Test void a01_parseBody_emptyBody_emptyStringFailsJsonParsing() throws Exception {
		// parseBody's "body == null" short-circuit is HTT-excluded (see RemoteClient.java): every bundled Transport
		// yields a non-null (possibly empty) body stream, so getBodyAsString() returns "" here, not null, and that
		// empty string is handed to the parser like any other body.
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultParser(org.apache.juneau.marshall.json.JsonParser.DEFAULT).build()) {
			assertThrows(ParseException.class, () -> client.remote(ParseBodyService.class).getWidgetFromEmptyBody());
		}
	}

	@Test void a02_parseBody_malformedJson_declaredType_rethrowsParseException() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultParser(org.apache.juneau.marshall.json.JsonParser.DEFAULT).build()) {
			assertThrows(ParseException.class, () -> client.remote(ParseBodyService.class).getWidgetFromMalformedJson());
		}
	}

	@Test void a03_parseBody_malformedJson_objectReturnType_lenientlyReturnsRawBody() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultParser(org.apache.juneau.marshall.json.JsonParser.DEFAULT).build()) {
			var result = client.remote(ParseBodyService.class).getObjectFromMalformedJson_lenientlyReturnsRawString();
			assertEquals("{not valid json", result);
		}
	}

	@Test void a04_selectParser_responseContentTypeUnmatched_fallsBackToAcceptMediaType() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultParser(org.apache.juneau.marshall.json.JsonParser.DEFAULT).build()) {
			var widget = client.remote(ParseBodyService.class).getWidgetViaAcceptFallback();
			assertEquals("fromFallback", widget.getName());
		}
	}

	@Remote
	interface WidgetService {
		@RemoteGet("/widget")
		Widget getWidget();

		@RemoteGet(path = "/json-unlabeled", accept = "application/json")
		Widget getWidgetViaAcceptFallback();
	}

	@Test void a05_selectParser_responseContentTypeMatchesRegisteredParser_usedDirectly() throws Exception {
		// Registering via .parsers(...) (rather than the singular .defaultParser(...) used by a01-a04) populates
		// RestClient's "parsers" set, so getParserForMediaType("application/json") actually matches -- exercising
		// selectParser's first ("response Content-Type matched") branch, which a01-a04 never reach (they leave
		// "parsers" null, so getParserForMediaType always misses and every call falls through to getMatchingParser).
		try (var client = RestClient.builder().rootUrl(rootUrl())
				.parsers(ParserSet.create().add(org.apache.juneau.marshall.json.JsonParser.class).build()).build()) {
			var widget = client.remote(WidgetService.class).getWidget();
			assertEquals("gizmo", widget.getName());
		}
	}

	@Test void a06b_selectParser_noMatchAnywhereAndNoDefaultParser_throwsUnsupportedMediaType() throws Exception {
		// No .parsers(...), no .defaultParser(...), and an unlabeled response with no accept fallback: every
		// selectParser candidate misses, so getMatchingParser(...) falls all the way through to its own
		// no-default-parser Optional.empty(), and orElseThrow's UnsupportedMediaType-construction lambda finally runs.
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var ex = assertThrows(org.apache.juneau.http.response.UnsupportedMediaType.class,
				() -> client.remote(WidgetService.class).getWidget());
			assertTrue(ex.getMessage().contains("No parser matched"), "Unexpected message: " + ex.getMessage());
		}
	}

	@Test void a06_selectParser_acceptFallbackMatchesRegisteredParser_usedDirectly() throws Exception {
		// Same "parsers" (not "defaultParser") registration as a05, but the response is unlabeled: the response
		// Content-Type match misses, so this instead exercises the "accept fallback matched a registered parser"
		// branch -- distinct from a04, which relies on the client's implicit defaultParser fallback (getMatchingParser),
		// not a registered-parser match on the accept media type.
		try (var client = RestClient.builder().rootUrl(rootUrl())
				.parsers(ParserSet.create().add(org.apache.juneau.marshall.json.JsonParser.class).build()).build()) {
			var widget = client.remote(WidgetService.class).getWidgetViaAcceptFallback();
			assertEquals("fromFallback", widget.getName());
		}
	}

	// -----------------------------------------------------------------------
	// b — processCursor: throwIfError fires before the cursor is created ("!ok" close branch)
	// -----------------------------------------------------------------------

	@Remote
	interface CursorService {
		@RemoteGet("/widget")
		TokenReader getCursor();

		@RemoteGet(path = "/not-found", throwOnError = true)
		TokenReader getCursorThrowsBeforeHandingBack();
	}

	@Test void b01_cursor_liveBody_readable() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultParser(org.apache.juneau.marshall.json.JsonParser.DEFAULT).build()) {
			try (var cursor = client.remote(CursorService.class).getCursor()) {
				assertNotEquals(TokenType.END_OF_STREAM, cursor.next());
			}
		}
	}

	@Test void b02_cursor_throwOnErrorBeforeHandingBackCursor_closesResponse() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultParser(org.apache.juneau.marshall.json.JsonParser.DEFAULT).build()) {
			assertThrows(org.apache.juneau.http.response.BasicHttpException.class,
				() -> client.remote(CursorService.class).getCursorThrowsBeforeHandingBack());
		}
	}

	// -----------------------------------------------------------------------
	// c — toPartBody: HttpBody-typed part, bean part with no default serializer; bindMultipartBody: skipped
	//     non-@Part parameter
	// -----------------------------------------------------------------------

	@Remote
	interface MultipartExtraService {
		@RemotePost("/echo-multipart")
		@Multipart
		String uploadHttpBodyPart(@Part("raw") HttpBody body);

		@RemotePost("/echo-multipart")
		@Multipart
		String uploadBeanPart(@Part("bean") RemoteClient_ReturnModesAndRetry_Test.MultipartBean bean);

		// 'label' has no @Part annotation and must be silently skipped by bindMultipartBody.
		@RemotePost("/echo-multipart")
		@Multipart
		String uploadWithUnannotatedParam(@Part("title") String title, String label);

		// An explicit fileName() on a non-File part -- exercises bindMultipartBody's "part.fileName() non-empty" branch.
		@RemotePost("/echo-multipart")
		@Multipart
		String uploadWithExplicitFileName(@Part(name = "data", fileName = "custom.bin") byte[] data);

		// A bean part with an explicit contentType() -- exercises toPartBody's "contentType != null" bean branch.
		@RemotePost("/echo-multipart")
		@Multipart
		String uploadBeanPartWithExplicitContentType(
			@Part(name = "bean", contentType = "application/json") RemoteClient_ReturnModesAndRetry_Test.MultipartBean bean);
	}

	@Test void c01_toPartBody_httpBodyArgument_usedDirectly() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var result = client.remote(MultipartExtraService.class).uploadHttpBodyPart(
				StringBody.of("raw-body-content", "text/plain"));
			assertTrue(result.contains("raw-body-content"), "Expected raw part content in: " + result);
		}
	}

	@Test void c02_toPartBody_beanPart_noDefaultSerializerConfigured_throwsIllegalState() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var ex = assertThrows(IllegalStateException.class,
				() -> client.remote(MultipartExtraService.class).uploadBeanPart(new RemoteClient_ReturnModesAndRetry_Test.MultipartBean()));
			assertTrue(ex.getMessage().contains("No default serializer"), "Unexpected message: " + ex.getMessage());
		}
	}

	@Test void c03_bindMultipartBody_nonPartParameter_isSkipped() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var result = client.remote(MultipartExtraService.class).uploadWithUnannotatedParam("my-title", "ignored-label");
			assertTrue(result.contains("my-title"), "Expected the title part in: " + result);
			assertFalse(result.contains("ignored-label"), "The unannotated parameter must not be sent as a part: " + result);
		}
	}

	@Test void c04_bindMultipartBody_explicitFileName_usedVerbatim() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).build()) {
			var result = client.remote(MultipartExtraService.class).uploadWithExplicitFileName(new byte[] {1, 2, 3});
			assertTrue(result.contains("custom.bin"), "Expected the explicit fileName in: " + result);
		}
	}

	@Test void c05_toPartBody_beanPart_explicitContentType_usedInsteadOfSerializerDefault() throws Exception {
		try (var client = RestClient.builder().rootUrl(rootUrl()).defaultSerializer(org.apache.juneau.marshall.json.JsonSerializer.DEFAULT).build()) {
			var result = client.remote(MultipartExtraService.class)
				.uploadBeanPartWithExplicitContentType(new RemoteClient_ReturnModesAndRetry_Test.MultipartBean());
			assertTrue(result.contains("application/json"), "Expected the explicit contentType in: " + result);
		}
	}
}
