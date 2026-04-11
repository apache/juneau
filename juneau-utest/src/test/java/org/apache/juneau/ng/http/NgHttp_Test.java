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
package org.apache.juneau.ng.http;

import static org.apache.juneau.ng.http.HttpBodies.*;
import static org.apache.juneau.ng.http.HttpHeaders.*;
import static org.apache.juneau.ng.http.HttpParts.*;
import static org.apache.juneau.ng.http.HttpResponses.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

import org.apache.juneau.ng.http.entity.*;
import org.apache.juneau.ng.http.header.*;
import org.apache.juneau.ng.http.part.*;
import org.apache.juneau.ng.http.resource.*;
import org.apache.juneau.ng.http.response.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for all ng.http classes: headers, parts, bodies, response, and static factory helpers.
 */
public class NgHttp_Test {

	// ------------------------------------------------------------------------------------------------------------------
	// A — Named header classes
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class A_namedHeaders {

		@Test void a01_accept_eagerValue() {
			var h = Accept.of("application/json");
			assertEquals("Accept", h.getName());
			assertEquals("application/json", h.getValue());
			assertEquals("Accept: application/json", h.toString());
		}

		@Test void a02_accept_lazyValue() {
			var value = new String[]{"text/plain"};
			var h = Accept.ofLazyWire(() -> value[0]);
			assertEquals("text/plain", h.getValue());
			value[0] = "application/xml";
			assertEquals("application/xml", h.getValue());
		}

		@Test void a03_contentType() {
			var h = ContentType.of("application/json; charset=UTF-8");
			assertEquals("Content-Type", h.getName());
			assertEquals("application/json; charset=UTF-8", h.getValue());
		}

		@Test void a04_authorization() {
			var h = Authorization.of("Bearer abc123");
			assertEquals("Authorization", h.getName());
			assertEquals("Bearer abc123", h.getValue());
		}

		@Test void a05_cacheControl() {
			var h = CacheControl.of("no-cache");
			assertEquals("Cache-Control", h.getName());
			assertEquals("no-cache", h.getValue());
		}

		@Test void a06_location() {
			var h = Location.of("https://example.com/resource");
			assertEquals("Location", h.getName());
		}

		@Test void a07_etag() {
			var h = ETag.of("\"abc123\"");
			assertEquals("ETag", h.getName());
			assertEquals("\"abc123\"", h.getValue());
		}

		@Test void a08_noTrace_defaultValue() {
			var h = noTrace();
			assertEquals("No-Trace", h.getName());
			assertEquals("true", h.getValue());
		}

		@Test void a09_debug_defaultValue() {
			var h = debug();
			assertEquals("Debug", h.getName());
			assertEquals("true", h.getValue());
		}

		@Test void a10_headerEqualsAndHashCode() {
			var h1 = Accept.of("application/json");
			var h2 = Accept.of("application/json");
			var h3 = Accept.of("text/plain");
			assertEquals(h1, h2);
			assertNotEquals(h1, h3);
			assertEquals(h1.hashCode(), h2.hashCode());
		}

		@Test void a11_nullValue_isAllowed() {
			var h = ContentType.of((String) null);
			assertNull(h.getValue());
		}

		@Test void a12_allStandardHeaders_nameCheck() {
			assertEquals("Accept-Charset", AcceptCharset.of("utf-8").getName());
			assertEquals("Accept-Encoding", AcceptEncoding.of("gzip").getName());
			assertEquals("Accept-Language", AcceptLanguage.of("en-US").getName());
			assertEquals("Accept-Ranges", AcceptRanges.of("bytes").getName());
			assertEquals("Age", Age.of("3600").getName());
			assertEquals("Allow", Allow.of("GET, POST").getName());
			assertEquals("Connection", Connection.of("keep-alive").getName());
			assertEquals("Content-Disposition", ContentDisposition.of("attachment").getName());
			assertEquals("Content-Encoding", ContentEncoding.of("gzip").getName());
			assertEquals("Content-Language", ContentLanguage.of("en").getName());
			assertEquals("Content-Length", ContentLength.of("1024").getName());
			assertEquals("Content-Location", ContentLocation.of("/resource").getName());
			assertEquals("Content-Range", ContentRange.of("bytes 0-999/1000").getName());
			assertEquals("Date", Date.of("Thu, 01 Jan 2026 00:00:00 GMT").getName());
			assertEquals("Expect", Expect.of("100-continue").getName());
			assertEquals("Expires", Expires.of("Tue, 21 Oct 2025 07:28:00 GMT").getName());
			assertEquals("Forwarded", Forwarded.of("for=192.0.2.60").getName());
			assertEquals("From", From.of("user@example.com").getName());
			assertEquals("Host", Host.of("example.com").getName());
			assertEquals("If-Match", IfMatch.of("\"abc\"").getName());
			assertEquals("If-Modified-Since", IfModifiedSince.of("Sat, 29 Oct 1994 19:43:31 GMT").getName());
			assertEquals("If-None-Match", IfNoneMatch.of("\"abc\"").getName());
			assertEquals("If-Range", IfRange.of("\"abc\"").getName());
			assertEquals("If-Unmodified-Since", IfUnmodifiedSince.of("Sat, 29 Oct 1994 19:43:31 GMT").getName());
			assertEquals("Last-Modified", LastModified.of("Wed, 21 Oct 2015 07:28:00 GMT").getName());
			assertEquals("Max-Forwards", MaxForwards.of("10").getName());
			assertEquals("Origin", Origin.of("https://example.com").getName());
			assertEquals("Pragma", Pragma.of("no-cache").getName());
			assertEquals("Proxy-Authenticate", ProxyAuthenticate.of("Basic").getName());
			assertEquals("Proxy-Authorization", ProxyAuthorization.of("Basic abc").getName());
			assertEquals("Range", Range.of("bytes=0-499").getName());
			assertEquals("Referer", Referer.of("https://example.com").getName());
			assertEquals("Retry-After", RetryAfter.of("120").getName());
			assertEquals("Server", Server.of("Apache").getName());
			assertEquals("TE", TE.of("trailers").getName());
			assertEquals("Trailer", Trailer.of("Expires").getName());
			assertEquals("Transfer-Encoding", TransferEncoding.of("chunked").getName());
			assertEquals("Upgrade", Upgrade.of("h2c").getName());
			assertEquals("User-Agent", UserAgent.of("MyClient/1.0").getName());
			assertEquals("Vary", Vary.of("Accept-Encoding").getName());
			assertEquals("Via", Via.of("1.1 proxy.example.com").getName());
			assertEquals("Warning", Warning.of("199 misc").getName());
			assertEquals("WWW-Authenticate", WwwAuthenticate.of("Basic realm=\"test\"").getName());
			assertEquals("Client-Version", ClientVersion.of("1.0").getName());
			assertEquals("Thrown", Thrown.of("RuntimeException").getName());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// B — HttpHeaders static factory
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class B_httpHeaders {

		@Test void b01_accept() {
			var h = accept("application/json");
			assertEquals("Accept", h.getName());
			assertEquals("application/json", h.getValue());
		}

		@Test void b02_authorization_lazy() {
			var token = new String[]{"tok1"};
			var h = authorization(() -> token[0]);
			assertEquals("tok1", h.getValue());
			token[0] = "tok2";
			assertEquals("tok2", h.getValue());
		}

		@Test void b03_contentType() {
			assertEquals("Content-Type", contentType("text/html").getName());
		}

		@Test void b04_contentLength_long() {
			var h = contentLength(1024L);
			assertEquals("Content-Length", h.getName());
			assertEquals("1024", h.getValue());
		}

		@Test void b05_header_generic() {
			var h = header("X-Custom", "value");
			assertEquals("X-Custom", h.getName());
			assertEquals("value", h.getValue());
		}

		@Test void b06_header_generic_lazy() {
			var h = header("X-Custom", () -> "lazy-value");
			assertEquals("lazy-value", h.getValue());
		}

		@Test void b07_noTrace_withValue() {
			assertEquals("true", noTrace("true").getValue());
			assertEquals("false", noTrace("false").getValue());
		}

		@Test void b08_debug_withValue() {
			assertEquals("true", debug("true").getValue());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// C — HttpPart and PartList
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class C_parts {

		@Test void c01_part_eagerValue() {
			var p = part("key", "value");
			assertEquals("key", p.getName());
			assertEquals("value", p.getValue());
			assertEquals("key=value", p.toString());
		}

		@Test void c02_part_lazyValue() {
			var val = new String[]{"v1"};
			var p = part("key", () -> val[0]);
			assertEquals("v1", p.getValue());
			val[0] = "v2";
			assertEquals("v2", p.getValue());
		}

		@Test void c03_part_nullValue() {
			var p = part("key", (String) null);
			assertNull(p.getValue());
		}

		@Test void c04_partList_of() {
			var list = PartList.of(part("a", "1"), part("b", "2"));
			assertEquals(2, list.size());
			assertFalse(list.isEmpty());
		}

		@Test void c05_partList_ofPairs() {
			var list = PartList.ofPairs("user", "alice", "pass", "secret");
			assertEquals(2, list.size());
			assertEquals("alice", list.getFirst("user").getValue());
			assertEquals("secret", list.getFirst("pass").getValue());
		}

		@Test void c06_partList_ofPairs_odd_throws() {
			assertThrows(IllegalArgumentException.class, () -> PartList.ofPairs("a", "1", "b"));
		}

		@Test void c07_partList_empty() {
			var list = PartList.empty();
			assertEquals(0, list.size());
			assertTrue(list.isEmpty());
		}

		@Test void c08_partList_getFirst_notFound() {
			var list = PartList.of(part("a", "1"));
			assertNull(list.getFirst("z"));
		}

		@Test void c09_partList_contentType() {
			var list = PartList.ofPairs("a", "1");
			assertEquals("application/x-www-form-urlencoded", list.getContentType());
		}

		@Test void c10_partList_writeTo_urlEncoded() throws IOException {
			var list = PartList.ofPairs("hello world", "foo bar");
			var baos = new ByteArrayOutputStream();
			list.writeTo(baos);
			assertEquals("hello+world=foo+bar", baos.toString());
		}

		@Test void c11_partList_writeTo_skipsNullValues() throws IOException {
			var list = PartList.of(HttpPartBean.of("a", "1"), HttpPartBean.of("b", (String) null), HttpPartBean.of("c", "3"));
			var baos = new ByteArrayOutputStream();
			list.writeTo(baos);
			assertEquals("a=1&c=3", baos.toString());
		}

		@Test void c12_partList_toString() {
			var list = PartList.ofPairs("a", "1", "b", "2");
			assertEquals("a=1&b=2", list.toString());
		}

		@Test void c13_partList_isRepeatable() {
			assertTrue(PartList.ofPairs("a", "1").isRepeatable());
		}

		@Test void c14_partList_contentLength() {
			assertEquals(-1, PartList.ofPairs("a", "1").getContentLength());
		}

		@Test void c15_partList_iterator() {
			var list = PartList.of(part("a", "1"), part("b", "2"));
			var names = new ArrayList<String>();
			for (var p : list)
				names.add(p.getName());
			assertEquals(List.of("a", "b"), names);
		}

		@Test void c16_httpParts_factory() {
			var list = partList(part("x", "1"), part("y", "2"));
			assertEquals(2, list.size());
		}

		@Test void c17_httpParts_ofPairs() {
			var list = partListOfPairs("a", "1");
			assertEquals(1, list.size());
		}

		@Test void c18_partList_writeTo_nullCheck() {
			var list = PartList.ofPairs("a", "1");
			assertThrows(IllegalArgumentException.class, () -> list.writeTo(null));
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// D — Body classes
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class D_bodies {

		@Test void d01_httpBodyBean_wrapsDelegate() throws IOException {
			var inner = StringBody.of("hello");
			var wrapped = HttpBodyBean.of(inner, "application/json");
			assertEquals("application/json", wrapped.getContentType());
			assertEquals(5, wrapped.getContentLength());
			assertTrue(wrapped.isRepeatable());
			var baos = new ByteArrayOutputStream();
			wrapped.writeTo(baos);
			assertEquals("hello", baos.toString());
			assertEquals("hello", wrapped.toString());
		}

		@Test void d02_httpBodyBean_retainsOriginalContentType() {
			var inner = StringBody.of("hi", "text/xml");
			var wrapped = HttpBodyBean.of(inner);
			assertEquals("text/xml", wrapped.getContentType());
		}

		@Test void d03_httpBodyBean_overridesContentType() {
			var inner = StringBody.of("hi", "text/xml");
			var wrapped = HttpBodyBean.of(inner, "application/json");
			assertEquals("application/json", wrapped.getContentType());
		}

		@Test void d04_httpBodyBean_nullContentType() {
			var inner = StringBody.of("hi");
			var wrapped = HttpBodyBean.of(inner, null);
			assertNull(wrapped.getContentType());
		}

		@Test void d05_stringBody() {
			var body = stringBody("test");
			assertEquals("text/plain; charset=UTF-8", body.getContentType());
			assertEquals(4, body.getContentLength());
		}

		@Test void d06_stringBody_withContentType() {
			var body = stringBody("test", "application/json");
			assertEquals("application/json", body.getContentType());
		}

		@Test void d07_byteArrayBody() {
			var body = byteArrayBody(new byte[]{1, 2, 3});
			assertEquals(3, body.getContentLength());
			assertTrue(body.isRepeatable());
		}

		@Test void d08_streamBody() {
			var body = streamBody(InputStream.nullInputStream());
			assertFalse(body.isRepeatable());
			assertEquals(-1, body.getContentLength());
		}

		@Test void d09_formBody() throws IOException {
			var body = formBody(part("a", "1"), part("b", "2"));
			assertEquals("application/x-www-form-urlencoded", body.getContentType());
			var baos = new ByteArrayOutputStream();
			body.writeTo(baos);
			assertEquals("a=1&b=2", baos.toString());
		}

		@Test void d10_formBodyOfPairs() {
			var body = formBodyOfPairs("a", "1", "b", "2");
			assertEquals(2, body.size());
		}

		@Test void d11_fileBody() {
			var f = new File("/tmp/test.txt");
			var body = fileBody(f);
			assertNotNull(body); // content type may vary by implementation
		}

		@Test void d12_fileBody_withContentType() {
			var f = new File("/tmp/test.pdf");
			var body = fileBody(f, "application/pdf");
			assertEquals("application/pdf", body.getContentType());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// E — HttpResource
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class E_httpResource {

		@Test void e01_of_noHeaders() {
			var body = StringBody.of("hello");
			var resource = HttpResource.of(body);
			assertSame(body, resource.getBody());
			assertTrue(resource.getHeaders().isEmpty());
			assertEquals("text/plain; charset=UTF-8", resource.getContentType());
		}

		@Test void e02_withHeader() {
			var resource = HttpResource.of(StringBody.of("hello"))
				.withHeader("X-Custom", "value");
			assertEquals("value", resource.getFirstHeader("X-Custom").getValue());
		}

		@Test void e03_withHeader_HttpHeader() {
			var resource = HttpResource.of(StringBody.of("hello"))
				.withHeader(accept("application/json"));
			assertEquals("application/json", resource.getFirstHeader("Accept").getValue());
		}

		@Test void e04_contentType_fromHeader_overridesBody() {
			var body = StringBody.of("hello", "text/plain");
			var resource = HttpResource.of(body, List.of(ContentType.of("application/json")));
			assertEquals("application/json", resource.getContentType());
		}

		@Test void e05_contentLength_fromDelegate() {
			var resource = HttpResource.of(StringBody.of("hello"));
			assertEquals(5, resource.getContentLength());
		}

		@Test void e06_isRepeatable() {
			assertTrue(HttpResource.of(StringBody.of("hello")).isRepeatable());
		}

		@Test void e07_writeTo() throws IOException {
			var resource = HttpResource.of(StringBody.of("world"));
			var baos = new ByteArrayOutputStream();
			resource.writeTo(baos);
			assertEquals("world", baos.toString());
		}

		@Test void e08_toString() {
			var resource = HttpResource.of(StringBody.of("hi"));
			assertEquals("hi", resource.toString());
		}

		@Test void e09_getFirstHeader_caseInsensitive() {
			var resource = HttpResource.of(StringBody.of("")).withHeader("content-type", "application/json");
			assertNotNull(resource.getFirstHeader("Content-Type"));
			assertNotNull(resource.getFirstHeader("CONTENT-TYPE"));
		}

		@Test void e10_getFirstHeader_absent() {
			var resource = HttpResource.of(StringBody.of(""));
			assertNull(resource.getFirstHeader("X-Missing"));
		}

		@Test void e11_of_nullBodyThrows() {
			assertThrows(IllegalArgumentException.class, () -> HttpResource.of(null));
		}

		@Test void e12_withHeader_nullThrows() {
			var resource = HttpResource.of(StringBody.of(""));
			assertThrows(IllegalArgumentException.class, () -> resource.withHeader((HttpHeader) null));
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// F — BasicHttpResponse and status response classes
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class F_httpResponses {

		@Test void f01_ok_noBody() {
			var resp = ok();
			assertEquals(200, resp.getStatusCode());
			assertNull(resp.getBody());
			assertEquals("HTTP/1.1 200 OK", resp.getStatusLine().toString());
			assertEquals("HTTP/1.1 200 OK", resp.toString());
		}

		@Test void f02_ok_withStringBody() {
			var resp = ok("hello");
			assertEquals(200, resp.getStatusCode());
			assertNotNull(resp.getBody());
			assertEquals("text/plain; charset=UTF-8", resp.getBody().getContentType());
		}

		@Test void f03_created() {
			var resp = created();
			assertEquals(201, resp.getStatusCode());
		}

		@Test void f04_noContent() {
			assertEquals(204, noContent().getStatusCode());
		}

		@Test void f05_notModified() {
			assertEquals(304, notModified().getStatusCode());
		}

		@Test void f06_redirect_withLocation() {
			var resp = movedPermanently("https://new.example.com");
			assertEquals(301, resp.getStatusCode());
			assertNotNull(resp.getFirstHeader("Location"));
			assertEquals("https://new.example.com", resp.getFirstHeader("Location").getValue());
		}

		@Test void f07_withHeader_fluent() {
			var resp = ok().withHeader("X-Custom", "value");
			assertEquals("value", resp.getFirstHeader("X-Custom").getValue());
		}

		@Test void f08_withHeader_httpHeader() {
			var resp = ok().withHeader(accept("text/html"));
			assertEquals("text/html", resp.getFirstHeader("Accept").getValue());
		}

		@Test void f09_withBody_httpBody() {
			var resp = ok().withBody(StringBody.of("new body"));
			assertNotNull(resp.getBody());
		}

		@Test void f10_withBody_string() {
			var resp = ok().withBody("new body");
			assertNotNull(resp.getBody());
		}

		@Test void f11_withBody_null_clearsBody() {
			var resp = ok("body").withBody((String) null);
			assertNull(resp.getBody());
		}

		@Test void f12_headers_unmodifiable() {
			var resp = ok();
			assertThrows(UnsupportedOperationException.class, () -> resp.getHeaders().add(null));
		}

		@Test void f13_getHeaders_filterByName() {
			var resp = ok().withHeader("X-A", "1").withHeader("X-B", "2").withHeader("X-A", "3");
			var xas = resp.getHeaders("X-A");
			assertEquals(2, xas.size());
		}

		@Test void f14_statusClasses() {
			assertEquals(100, new Continue().getStatusCode());
			assertEquals(101, new SwitchingProtocols().getStatusCode());
			assertEquals(202, new Accepted().getStatusCode());
			assertEquals(206, new PartialContent().getStatusCode());
			assertEquals(207, new MultiStatus().getStatusCode());
			assertEquals(208, new AlreadyReported().getStatusCode());
			assertEquals(226, new IMUsed().getStatusCode());
			assertEquals(300, new MultipleChoices().getStatusCode());
			assertEquals(302, new Found().getStatusCode());
			assertEquals(303, new SeeOther().getStatusCode());
			assertEquals(305, new UseProxy().getStatusCode());
			assertEquals(307, new TemporaryRedirect().getStatusCode());
			assertEquals(308, new PermanentRedirect().getStatusCode());
		}

		@Test void f15_instance_singleton() {
			assertSame(Ok.INSTANCE, Ok.INSTANCE);
			assertEquals(200, Ok.INSTANCE.getStatusCode());
		}

		@Test void f16_copyConstructor() {
			var original = ok("body");
			var copy = new Ok(original);
			assertEquals(200, copy.getStatusCode());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// G — BasicHttpException and error status classes
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class G_httpExceptions {

		@Test void g01_notFound_noMessage() {
			var ex = notFound();
			assertEquals(404, ex.getStatusCode());
			assertEquals("Not Found", ex.getStatusLine().getReasonPhrase());
			assertNull(ex.getMessage()); // no-arg constructor does not set a message
			assertNull(ex.getBody());    // no body when no message
		}

		@Test void g02_notFound_withMessage() {
			var ex = notFound("Item not found: 42");
			assertEquals(404, ex.getStatusCode());
			assertEquals("Item not found: 42", ex.getMessage());
			assertNotNull(ex.getBody());
		}

		@Test void g03_internalServerError_withCause() {
			var cause = new RuntimeException("root cause");
			var ex = internalServerError(cause);
			assertEquals(500, ex.getStatusCode());
			assertSame(cause, ex.getCause());
		}

		@Test void g04_badRequest() {
			var ex = badRequest("invalid input");
			assertEquals(400, ex.getStatusCode());
			assertEquals("invalid input", ex.getMessage());
		}

		@Test void g05_isRuntimeException() {
			assertInstanceOf(RuntimeException.class, notFound());
			assertInstanceOf(HttpResponseMessage.class, notFound());
		}

		@Test void g06_headers_unmodifiable() {
			assertThrows(UnsupportedOperationException.class, () -> notFound().getHeaders().add(null));
		}

		@Test void g07_toString() {
			assertEquals("HTTP/1.1 404 Not Found", notFound().toString());
		}

		@Test void g08_allErrorClasses() {
			assertEquals(400, new BadRequest().getStatusCode());
			assertEquals(401, new Unauthorized().getStatusCode());
			assertEquals(403, new Forbidden().getStatusCode());
			assertEquals(405, new MethodNotAllowed().getStatusCode());
			assertEquals(406, new NotAcceptable().getStatusCode());
			assertEquals(409, new Conflict().getStatusCode());
			assertEquals(410, new Gone().getStatusCode());
			assertEquals(411, new LengthRequired().getStatusCode());
			assertEquals(412, new PreconditionFailed().getStatusCode());
			assertEquals(413, new PayloadTooLarge().getStatusCode());
			assertEquals(414, new UriTooLong().getStatusCode());
			assertEquals(415, new UnsupportedMediaType().getStatusCode());
			assertEquals(416, new RangeNotSatisfiable().getStatusCode());
			assertEquals(417, new ExpectationFailed().getStatusCode());
			assertEquals(421, new MisdirectedRequest().getStatusCode());
			assertEquals(422, new UnprocessableEntity().getStatusCode());
			assertEquals(423, new Locked().getStatusCode());
			assertEquals(424, new FailedDependency().getStatusCode());
			assertEquals(426, new UpgradeRequired().getStatusCode());
			assertEquals(428, new PreconditionRequired().getStatusCode());
			assertEquals(429, new TooManyRequests().getStatusCode());
			assertEquals(431, new RequestHeaderFieldsTooLarge().getStatusCode());
			assertEquals(451, new UnavailableForLegalReasons().getStatusCode());
			assertEquals(500, new InternalServerError().getStatusCode());
			assertEquals(501, new NotImplemented().getStatusCode());
			assertEquals(502, new BadGateway().getStatusCode());
			assertEquals(503, new ServiceUnavailable().getStatusCode());
			assertEquals(504, new GatewayTimeout().getStatusCode());
			assertEquals(505, new HttpVersionNotSupported().getStatusCode());
			assertEquals(506, new VariantAlsoNegotiates().getStatusCode());
			assertEquals(507, new InsufficientStorage().getStatusCode());
			assertEquals(508, new LoopDetected().getStatusCode());
			assertEquals(510, new NotExtended().getStatusCode());
			assertEquals(511, new NetworkAuthenticationRequired().getStatusCode());
		}

		@Test void g09_copyConstructor() {
			var original = new NotFound("original message");
			var copy = new NotFound(original);
			assertEquals(404, copy.getStatusCode());
		}

		@Test void g10_messageAndCause() {
			var cause = new RuntimeException("cause");
			var ex = new BadRequest("bad message", cause);
			assertEquals("bad message", ex.getMessage());
			assertSame(cause, ex.getCause());
		}
	}

	// ------------------------------------------------------------------------------------------------------------------
	// H — HttpResponses factory
	// ------------------------------------------------------------------------------------------------------------------

	@Nested class H_httpResponsesFactory {

		@Test void h01_ok() {
			assertEquals(200, ok().getStatusCode());
		}

		@Test void h02_ok_withBody() {
			assertNotNull(ok("body").getBody());
		}

		@Test void h03_created() {
			assertEquals(201, created().getStatusCode());
		}

		@Test void h04_accepted() {
			assertEquals(202, accepted().getStatusCode());
		}

		@Test void h05_noContent() {
			assertEquals(204, noContent().getStatusCode());
		}

		@Test void h06_partialContent() {
			assertEquals(206, partialContent().getStatusCode());
		}

		@Test void h07_notModified() {
			assertEquals(304, notModified().getStatusCode());
		}

		@Test void h08_redirects() {
			assertEquals(301, movedPermanently("https://new.com").getStatusCode());
			assertEquals(302, found("https://new.com").getStatusCode());
			assertEquals(303, seeOther("https://new.com").getStatusCode());
			assertEquals(307, temporaryRedirect("https://new.com").getStatusCode());
			assertEquals(308, permanentRedirect("https://new.com").getStatusCode());
		}

		@Test void h09_errors() {
			assertEquals(400, badRequest().getStatusCode());
			assertEquals(401, unauthorized().getStatusCode());
			assertEquals(403, forbidden().getStatusCode());
			assertEquals(405, methodNotAllowed().getStatusCode());
			assertEquals(409, conflict().getStatusCode());
			assertEquals(410, gone().getStatusCode());
			assertEquals(412, preconditionFailed().getStatusCode());
			assertEquals(415, unsupportedMediaType().getStatusCode());
			assertEquals(422, unprocessableEntity().getStatusCode());
			assertEquals(429, tooManyRequests().getStatusCode());
			assertEquals(500, internalServerError().getStatusCode());
			assertEquals(501, notImplemented().getStatusCode());
			assertEquals(503, serviceUnavailable().getStatusCode());
		}

		@Test void h10_errors_withMessage() {
			assertEquals("oops", badRequest("oops").getMessage());
			assertEquals("denied", forbidden("denied").getMessage());
			assertEquals("missing", notFound("missing").getMessage());
			assertEquals("conflict", conflict("conflict").getMessage());
			assertEquals("unsupported", unsupportedMediaType("unsupported").getMessage());
			assertEquals("invalid", unprocessableEntity("invalid").getMessage());
			assertEquals("down", serviceUnavailable("down").getMessage());
			assertEquals("crash", internalServerError("crash").getMessage());
		}

		@Test void h11_internalServerError_withCause() {
			var cause = new RuntimeException("root");
			var ex = internalServerError(cause);
			assertSame(cause, ex.getCause());
		}
	}
}
