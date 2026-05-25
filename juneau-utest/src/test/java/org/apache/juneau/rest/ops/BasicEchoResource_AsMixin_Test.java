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
package org.apache.juneau.rest.ops;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BasicEchoResource} mounted as a mixin via {@code @Rest(mixins=...)} on a vanilla
 * {@link RestServlet}.
 *
 * <p>
 * Cases covered:
 * <ul>
 * 	<li>Default deny &mdash; no {@code @Rest(debug)} on the host returns {@code 404} from
 * 		{@code /echo/*} so the endpoint's existence isn't disclosed.
 * 	<li>{@code @Rest(debug=@Debug("always"))} unlocks the endpoint and returns the full echo payload.
 * 	<li>{@code @Rest(debug=@Debug("conditional"))} requires the {@code Debug: true} request header.
 * 	<li>Sensitive headers ({@code Authorization}, {@code Cookie}) are redacted by default.
 * 	<li>Importer's {@code @Bean BasicEchoResource} factory drives the body cap and redact list.
 * 	<li>Body capture truncates correctly when the inbound body exceeds the configured cap.
 * 	<li>Path remainder, query string, and query params are surfaced.
 * 	<li>The handler dispatches on {@code @RestOp(method="*")} &mdash; POST and PUT both work.
 * 	<li>The host's own endpoints remain reachable.
 * </ul>
 *
 * @since 9.5.0
 */
class BasicEchoResource_AsMixin_Test extends TestBase {

	/** Default-host mounting the mixin without {@code @Rest(debug)}. */
	@Rest(mixins=BasicEchoResource.class)
	public static class A extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient ca = MockRestClient.buildLax(A.class);

	@Test void a01_echoReturns404WhenDebugDisabled() throws Exception {
		ca.get("/echo/anything").run().assertStatus(404);
	}

	@Test void a02_debugEchoReturns404WhenDebugDisabled() throws Exception {
		ca.get("/debug/echo/something").run().assertStatus(404);
	}

	@Test void a03_hostEndpointStillReachable() throws Exception {
		ca.get("/items").run().assertStatus(200).assertContent().asString().isContains("items");
	}

	/** Host with debug always-on so the echo endpoint serves. */
	@Rest(mixins=BasicEchoResource.class, debug=@Debug("always"))
	public static class B extends RestServlet {
		private static final long serialVersionUID = 1L;
		@RestGet(path="/items") public String items() { return "items"; }
	}

	private static final MockRestClient cb = MockRestClient.buildLax(B.class);

	@Test void b01_echoServesFullPayload() throws Exception {
		var body = cb.get("/echo/foo/bar?x=1&y=hello")
			.run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("application/json")
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertEquals("GET", parsed.get("method"));
		Assertions.assertTrue(((String) parsed.get("path")).endsWith("/echo/foo/bar"),
			"path should end with /echo/foo/bar; got: " + parsed.get("path"));
		Assertions.assertEquals("x=1&y=hello", parsed.get("queryString"));
		Assertions.assertEquals("foo/bar", parsed.get("pathRemainder"));
		Assertions.assertEquals(Boolean.FALSE, parsed.get("truncated"));
		var qp = (Map<?,?>) parsed.get("queryParams");
		Assertions.assertEquals("1", qp.get("x"));
		Assertions.assertEquals("hello", qp.get("y"));
	}

	@Test void b02_debugEchoAlsoServesAtAlternateMount() throws Exception {
		cb.get("/debug/echo/abc")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"pathRemainder\": \"abc\"");
	}

	@Test void b03_authorizationHeaderRedacted() throws Exception {
		var body = cb.get("/echo/")
			.header("Authorization", "Bearer secret-token")
			.run()
			.assertStatus(200)
			.getContent().asString();
		Assertions.assertFalse(body.contains("Bearer secret-token"),
			"Authorization value must NEVER be reflected back; body was: " + body);
		Assertions.assertTrue(body.contains(BasicEchoResource.REDACTED),
			"Redaction sentinel should appear; body was: " + body);
	}

	@Test void b04_cookieHeaderRedacted() throws Exception {
		var body = cb.get("/echo/")
			.header("Cookie", "JSESSIONID=DEADBEEF")
			.run()
			.assertStatus(200)
			.getContent().asString();
		Assertions.assertFalse(body.contains("DEADBEEF"),
			"Cookie value must NEVER be reflected back; body was: " + body);
	}

	@Test void b05_postEchoesBody() throws Exception {
		var body = cb.post("/echo/posting", "hello-world")
			.run()
			.assertStatus(200)
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertEquals("POST", parsed.get("method"));
		Assertions.assertEquals("hello-world", parsed.get("content"));
		Assertions.assertEquals(11L, ((Number) parsed.get("contentLength")).longValue());
		Assertions.assertEquals(Boolean.FALSE, parsed.get("truncated"));
	}

	@Test void b06_putAlsoDispatchesViaWildcardMethod() throws Exception {
		cb.put("/echo/x", "payload")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"method\": \"PUT\"");
	}

	/** Host with conditional debug — requires {@code Debug: true} request header to unlock echo. */
	@Rest(mixins=BasicEchoResource.class, debug=@Debug("conditional"))
	public static class C extends RestServlet {
		private static final long serialVersionUID = 1L;
	}

	private static final MockRestClient cc = MockRestClient.buildLax(C.class);

	@Test void c01_conditional_withoutDebugHeaderReturns404() throws Exception {
		cc.get("/echo/anything").run().assertStatus(404);
	}

	@Test void c02_conditional_withDebugHeaderUnlocks() throws Exception {
		cc.get("/echo/anything")
			.header("Debug", "true")
			.run()
			.assertStatus(200)
			.assertContent().asString().isContains("\"pathRemainder\": \"anything\"");
	}

	/** Host with a custom redact list and a tight body cap via @Bean factory. */
	@Rest(mixins=BasicEchoResource.class, debug=@Debug("always"))
	public static class D extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public BasicEchoResource echo() {
			return BasicEchoResource.create()
				.bodyLimit(8L)
				.redactHeader("X-Internal-Trace")
				.build();
		}
	}

	private static final MockRestClient cd = MockRestClient.buildLax(D.class);

	@Test void d01_customRedactHeaderHonored() throws Exception {
		var body = cd.get("/echo/")
			.header("X-Internal-Trace", "abc-trace-id")
			.run()
			.assertStatus(200)
			.getContent().asString();
		Assertions.assertFalse(body.contains("abc-trace-id"),
			"Custom-redacted header value must NOT be reflected back; body was: " + body);
		Assertions.assertTrue(body.contains(BasicEchoResource.REDACTED));
	}

	@Test void d02_authorizationStillRedactedAfterAddingCustom() throws Exception {
		var body = cd.get("/echo/")
			.header("Authorization", "Bearer secret-token")
			.run()
			.assertStatus(200)
			.getContent().asString();
		Assertions.assertFalse(body.contains("secret-token"),
			"Built-in default redactions must remain in place when redactHeader(...) is called; body was: "
				+ body);
	}

	@Test void d03_bodyCapTruncates() throws Exception {
		var body = cd.post("/echo/", "0123456789ABCDEF")
			.run()
			.assertStatus(200)
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertEquals(Boolean.TRUE, parsed.get("truncated"),
			"Body must be flagged truncated when it exceeds the cap; body was: " + body);
		Assertions.assertEquals(8L, ((Number) parsed.get("contentLength")).longValue());
		Assertions.assertEquals("01234567", parsed.get("content"));
	}

	/** Host with a zero body cap — every non-empty body truncates immediately. */
	@Rest(mixins=BasicEchoResource.class, debug=@Debug("always"))
	public static class G extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public BasicEchoResource echo() {
			return BasicEchoResource.create().bodyLimit(0L).build();
		}
	}

	private static final MockRestClient cg = MockRestClient.buildLax(G.class);

	@Test void g01_zeroBodyLimitTruncatesNonEmptyBody() throws Exception {
		var body = cg.post("/echo/", "ANY")
			.run()
			.assertStatus(200)
			.getContent().asString();
		var parsed = JsonParser.DEFAULT.parse(body, Map.class);
		Assertions.assertEquals(Boolean.TRUE, parsed.get("truncated"),
			"zero cap → truncated; body was: " + body);
		Assertions.assertEquals(0L, ((Number) parsed.get("contentLength")).longValue());
	}

	/** Host with a redactedHeaders(...) replace-list that disables built-in defaults. */
	@Rest(mixins=BasicEchoResource.class, debug=@Debug("always"))
	public static class E extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public BasicEchoResource echo() {
			return BasicEchoResource.create()
				.redactedHeaders("X-Custom-Only")
				.build();
		}
	}

	private static final MockRestClient ce = MockRestClient.buildLax(E.class);

	@Test void e01_replaceListDropsBuiltInRedactions() throws Exception {
		var body = ce.get("/echo/")
			.header("Authorization", "Bearer not-redacted-here")
			.run()
			.assertStatus(200)
			.getContent().asString();
		Assertions.assertTrue(body.contains("Bearer not-redacted-here"),
			"redactedHeaders(...) replace-list should drop built-in defaults; body was: " + body);
	}

	@Test void e02_replaceListRedactsCustomHeader() throws Exception {
		var body = ce.get("/echo/")
			.header("X-Custom-Only", "secret-custom")
			.run()
			.assertStatus(200)
			.getContent().asString();
		Assertions.assertFalse(body.contains("secret-custom"),
			"X-Custom-Only must be redacted; body was: " + body);
	}

	// -----------------------------------------------------------------------------------------
	// Builder-only tests (no MockRest involvement).
	// -----------------------------------------------------------------------------------------

	@Test void f01_builderRejectsNegativeBodyLimit() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BasicEchoResource.create().bodyLimit(-1L));
	}

	@Test void f02_builderRejectsBlankRedactHeader() {
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BasicEchoResource.create().redactHeader(""));
		Assertions.assertThrows(IllegalArgumentException.class,
			() -> BasicEchoResource.create().redactHeader(null));
	}

	@Test void f03_redactedHeadersLowerIsImmutable() {
		var r = BasicEchoResource.create().build();
		Assertions.assertThrows(UnsupportedOperationException.class,
			() -> r.getRedactedHeadersLower().add("X-Test"));
	}

	@Test void f04_defaultsExposedAsConstants() {
		Assertions.assertEquals(1_048_576L, BasicEchoResource.DEFAULT_BODY_LIMIT);
		Assertions.assertTrue(BasicEchoResource.DEFAULT_REDACTED_HEADERS.contains("Authorization"));
		Assertions.assertTrue(BasicEchoResource.DEFAULT_REDACTED_HEADERS.contains("Cookie"));
	}

	@Test void f05_noArgConstructorMatchesDefaultBuilder() {
		var r = new BasicEchoResource();
		Assertions.assertEquals(BasicEchoResource.DEFAULT_BODY_LIMIT, r.getBodyLimit());
		Assertions.assertTrue(r.getRedactedHeadersLower().contains("authorization"));
	}

	@Test void f06_redactedHeadersFiltersNullAndBlank() {
		var r = BasicEchoResource.create()
			.redactedHeaders("X-Keep", null, "", "  ", "X-Also")
			.build();
		var s = r.getRedactedHeadersLower();
		Assertions.assertEquals(2, s.size(), "expected 2 entries; got: " + s);
		Assertions.assertTrue(s.contains("x-keep"));
		Assertions.assertTrue(s.contains("x-also"));
	}

	@Test void f07_redactedHeadersNullVarargsClears() {
		var r = BasicEchoResource.create().redactedHeaders((String[]) null).build();
		Assertions.assertTrue(r.getRedactedHeadersLower().isEmpty(),
			"null varargs should clear the redact list");
	}

	@Test void f08_zeroBodyLimitTruncatesEverything() throws Exception {
		// Builder accepts 0; the handler then truncates any non-empty body.
		var b = BasicEchoResource.create().bodyLimit(0L).build();
		Assertions.assertEquals(0L, b.getBodyLimit());
	}
}
