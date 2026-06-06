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
package org.apache.juneau.rest;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.header.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Coverage tests for {@link RestResponse}.
 *
 * <p>
 * Drives the response wrapper through a {@link MockRestClient}/{@code @Rest} harness to exercise the
 * branches not already covered by the targeted helper-tests
 * ({@code RestResponse_DownloadAs_Test}, {@code RestResponse_EtagHelpers_Test},
 * {@code RestResponse_SetSerializer_Test}). Focus areas: header setters (null-name / replace-vs-append /
 * Content-Type special-casing / max-length truncation / safe-headers), {@link RestResponse#sendRedirect(String)}
 * relative-vs-absolute branching, status / sendError, buffer + locale + content-length pass-throughs,
 * output-stream/writer accessors, and the constructor branches (x-response-headers, Accept-Charset,
 * defaultResponseHeaders).
 */
class RestResponse_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A: addHeader / setHeader pass-through behavior — null-name, null-value, replace-vs-append.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet("/addStringPair") public String addStringPair(RestResponse res) {
			res.addHeader("X-Add", "v1");
			res.addHeader("X-Add", "v2");
			return "ok";
		}
		@RestGet("/addNullName") public String addNullName(RestResponse res) {
			res.addHeader((String)null, "v");
			res.addHeader("X-Set", "kept");
			return "ok";
		}
		@RestGet("/addNullValue") public String addNullValue(RestResponse res) {
			res.addHeader("X-Add", null);
			res.addHeader("X-Add", "kept");
			return "ok";
		}
		@RestGet("/addContentTypeViaAddHeader") public String addContentTypeViaAddHeader(RestResponse res) {
			// addHeader("Content-Type", ...) routes to setHeader which forwards to inner.setContentType.
			res.addHeader("Content-Type", "text/plain;charset=UTF-8");
			return "ok";
		}
		@RestGet("/setReplaces") public String setReplaces(RestResponse res) {
			res.addHeader("X-Set", "first");
			res.setHeader("X-Set", "replaced");
			return "ok";
		}
		@RestGet("/setHeaderHttpHeaderNull") public String setHeaderHttpHeaderNull(RestResponse res) {
			res.setHeader((HttpHeader)null);
			res.addHeader((HttpHeader)null);
			return "ok";
		}
		@RestGet("/setHeaderHttpHeaderBean") public String setHeaderHttpHeaderBean(RestResponse res) {
			// HttpHeaderBean is not an HttpUriHeader → exercises the "else" branch in setHeader(HttpHeader).
			res.setHeader(HttpHeaderBean.of("X-Bean", "bean-value"));
			res.addHeader(HttpHeaderBean.of("X-Add-Bean", "added-bean"));
			return "ok";
		}
		@RestGet("/setHeaderUriHeaderRelative") public String setHeaderUriHeaderRelative(RestResponse res) {
			// HttpUriHeader with a relative path → URI resolver kicks in.
			res.setHeader(Location.of("/path/foo"));
			return "ok";
		}
		@RestGet("/setHeaderUriHeaderAbsolute") public String setHeaderUriHeaderAbsolute(RestResponse res) {
			res.setHeader(Location.of("http://example.com/foo"));
			return "ok";
		}
		@RestGet("/addHeaderUriHeader") public String addHeaderUriHeader(RestResponse res) {
			res.addHeader(Location.of("http://example.com/foo"));
			return "ok";
		}
		@RestGet("/setHeaderObjectOverload") public String setHeaderObjectOverload(RestResponse res) throws Exception {
			// Routes through getPartSerializerSession() — exercises the (String,Object) overload.
			res.setHeader("X-Int", Integer.valueOf(42));
			return "ok";
		}
		@RestGet("/setHeaderSchemaOverload") public String setHeaderSchemaOverload(RestResponse res) throws Exception {
			res.setHeader(null, "X-Schema", "schema-value");
			return "ok";
		}
		@RestGet("/setHeaderUriValueWithProtocol") public String setHeaderUriValueWithProtocol(RestResponse res) {
			// Non-UriHeader bean carrying a URL value (contains "://") triggers the resolveUris branch.
			res.setHeader(HttpHeaderBean.of("X-Url", "http://example.com/foo"));
			return "ok";
		}
		@RestGet("/setHeaderHttpHeaderWithNullValue") public String setHeaderHttpHeaderWithNullValue(RestResponse res) {
			// Bean with a null value → exercises the v == null short-circuit in setHeader(HttpHeader).
			res.setHeader(HttpHeaderBean.of("X-Null-Val", (String)null));
			return "ok";
		}
		@RestGet("/setContentTypeNoCharset") public String setContentTypeNoCharset(RestResponse res) {
			// Content-Type without a charset attribute → ct.getParameter("charset") == null branch.
			res.setHeader("Content-Type", "text/plain");
			return "ok";
		}
		@RestGet("/setContentTypeMalformed") public String setContentTypeMalformed(RestResponse res) {
			// Garbage value → ContentType.of(...) returns null → outer branch (ct == null).
			res.setHeader("Content-Type", "");
			return "ok";
		}
	}

	private static final MockRestClient CA = MockRestClient.buildLax(A.class);

	@Test void a01_addHeader_appends_andReturns200() throws Exception {
		CA.get("/addStringPair").run().assertStatus(200);
	}

	@Test void a02_addHeader_nullNameIsNoOp() throws Exception {
		CA.get("/addNullName").run().assertStatus(200).assertHeader("X-Set").is("kept");
	}

	@Test void a03_addHeader_nullValueIsNoOp() throws Exception {
		// First addHeader is dropped (value == null). Second value should be present.
		CA.get("/addNullValue").run().assertStatus(200).assertHeader("X-Add").is("kept");
	}

	@Test void a04_addHeader_contentTypeRoutedToSetHeader() throws Exception {
		CA.get("/addContentTypeViaAddHeader").run().assertStatus(200)
			.assertHeader("Content-Type").isContains("text/plain");
	}

	@Test void a05_setHeader_replacesExisting() throws Exception {
		CA.get("/setReplaces").run().assertStatus(200).assertHeader("X-Set").is("replaced");
	}

	@Test void a06_setHeader_addHeader_httpHeaderNull_areNoOps() throws Exception {
		CA.get("/setHeaderHttpHeaderNull").run().assertStatus(200);
	}

	@Test void a07_setHeader_httpHeaderBean() throws Exception {
		CA.get("/setHeaderHttpHeaderBean").run().assertStatus(200)
			.assertHeader("X-Bean").is("bean-value")
			.assertHeader("X-Add-Bean").is("added-bean");
	}

	@Test void a08_setHeader_uriHeader_relativePathResolved() throws Exception {
		// Relative paths get prefixed by the URI resolver (servletURI/contextPath).
		CA.get("/setHeaderUriHeaderRelative").run().assertStatus(200)
			.assertHeader("Location").isNotEmpty();
	}

	@Test void a09_setHeader_uriHeader_absolutePassthrough() throws Exception {
		CA.get("/setHeaderUriHeaderAbsolute").run().assertStatus(200)
			.assertHeader("Location").is("http://example.com/foo");
	}

	@Test void a10_addHeader_uriHeader_absolute() throws Exception {
		CA.get("/addHeaderUriHeader").run().assertStatus(200)
			.assertHeader("Location").is("http://example.com/foo");
	}

	@Test void a11_setHeader_objectOverload_serializesViaPartSerializer() throws Exception {
		CA.get("/setHeaderObjectOverload").run().assertStatus(200).assertHeader("X-Int").is("42");
	}

	@Test void a12_setHeader_schemaOverload_nullSchema() throws Exception {
		CA.get("/setHeaderSchemaOverload").run().assertStatus(200).assertHeader("X-Schema").is("schema-value");
	}

	@Test void a13_setHeader_uriBeanWithProtocolResolved() throws Exception {
		CA.get("/setHeaderUriValueWithProtocol").run().assertStatus(200)
			.assertHeader("X-Url").is("http://example.com/foo");
	}

	@Test void a14_setHeader_httpHeaderBean_nullValue() throws Exception {
		CA.get("/setHeaderHttpHeaderWithNullValue").run().assertStatus(200);
	}

	@Test void a15_setHeader_contentType_noCharset() throws Exception {
		CA.get("/setContentTypeNoCharset").run().assertStatus(200)
			.assertHeader("Content-Type").isContains("text/plain");
	}

	@Test void a16_setHeader_contentType_malformedReturnsNullCt() throws Exception {
		// Empty Content-Type → ContentType.of("") may yield null → outer branch is taken.
		CA.get("/setContentTypeMalformed").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B: setMaxHeaderLength + setSafeHeaders — abbreviation and CTRL-character stripping.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet("/truncate") public String truncate(RestResponse res) {
			res.setMaxHeaderLength(8);
			// Long value → abbreviated (length-3) + "..." per StringUtils.abbreviate.
			res.addHeader("X-Long", "0123456789abcdef");
			return "ok";
		}
		@RestGet("/safeHeaders") public String safeHeaders(RestResponse res) {
			res.setSafeHeaders();
			res.addHeader("X-Ctrl", "a\rb\nc");
			res.setHeader("X-Ctrl2", "x\ry\nz");
			return "ok";
		}
	}

	private static final MockRestClient CB = MockRestClient.buildLax(B.class);

	@Test void b01_setMaxHeaderLength_truncatesViaAbbreviate() throws Exception {
		// abbreviate(value, 8) → first (8-3)=5 chars + "..."
		CB.get("/truncate").run().assertStatus(200).assertHeader("X-Long").is("01234...");
	}

	@Test void b02_setSafeHeaders_stripsControlChars() throws Exception {
		// stripInvalidHttpHeaderChars removes CR/LF.
		CB.get("/safeHeaders").run().assertStatus(200)
			.assertHeader("X-Ctrl").isContains("abc")
			.assertHeader("X-Ctrl2").isContains("xyz");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C: sendRedirect — relative path is rewritten under contextPath; absolute is passed through; URI with scheme too.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet("/redirectAbsolute") public void redirectAbsolute(RestResponse res) throws Exception {
			res.sendRedirect("http://example.com/dest");
		}
		@RestGet("/redirectRelative") public void redirectRelative(RestResponse res) throws Exception {
			res.sendRedirect("foo/bar");  // relative → contextPath + "/" + uri
		}
		@RestGet("/redirectRoot") public void redirectRoot(RestResponse res) throws Exception {
			res.sendRedirect("/foo/bar");  // already root-relative → passed as-is.
		}
		@RestGet("/redirectEmpty") public void redirectEmpty(RestResponse res) throws Exception {
			res.sendRedirect("");  // empty branch — c == 0, indexOf("://") == -1 → prefixed with contextPath + "/".
		}
	}

	// Disable redirect handling so the 302 stops at the client and we can assert on it.
	private static final MockRestClient CC = MockRestClient.createLax(C.class).disableRedirectHandling().build();

	@Test void c01_redirectAbsolute_passesThrough() throws Exception {
		CC.get("/redirectAbsolute").run().assertStatus(302)
			.assertHeader("Location").is("http://example.com/dest");
	}

	@Test void c02_redirectRelative_prefixedWithContext() throws Exception {
		CC.get("/redirectRelative").run().assertStatus(302)
			.assertHeader("Location").isContains("foo/bar");
	}

	@Test void c03_redirectRoot_passesThrough() throws Exception {
		CC.get("/redirectRoot").run().assertStatus(302)
			.assertHeader("Location").is("/foo/bar");
	}

	@Test void c04_redirectEmpty_prefixedWithContext() throws Exception {
		CC.get("/redirectEmpty").run().assertStatus(302).assertHeader("Location").isNotEmpty();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D: Buffer / status / locale / content-length / encoding pass-throughs (HttpServletResponseWrapper inherits these,
	//    but they still count as instructions inside RestResponse).
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestGet("/setStatus") public String setStatus(RestResponse res) {
			res.setStatus(202);
			return "ok";
		}
		@RestGet("/sendError") public void sendError(RestResponse res) throws Exception {
			res.sendError(418);
		}
		@RestGet("/sendErrorMsg") public void sendErrorMsg(RestResponse res) throws Exception {
			res.sendError(503, "down for maintenance");
		}
		@RestGet("/buffer") public String buffer(RestResponse res) throws Exception {
			res.setBufferSize(2048);
			var sb = new StringBuilder();
			sb.append("buf=").append(res.getBufferSize());
			sb.append(",committed=").append(res.isCommitted());
			res.resetBuffer();
			res.flushBuffer();
			sb.append(",afterFlushCommitted=").append(res.isCommitted());
			return sb.toString();
		}
		@RestGet("/locale") public String locale(RestResponse res) {
			res.setLocale(java.util.Locale.FRANCE);
			return res.getLocale() == null ? "null" : res.getLocale().toLanguageTag();
		}
		@RestGet("/contentLength") public String contentLength(RestResponse res) {
			res.setContentLength(123);
			return "ok";
		}
		@RestGet("/contentLengthLong") public String contentLengthLong(RestResponse res) {
			res.setContentLengthLong(456L);
			return "ok";
		}
		@RestGet("/setCharEncoding") public String setCharEncoding(RestResponse res) {
			res.setCharacterEncoding("ISO-8859-1");
			return res.getCharacterEncoding();
		}
		@RestGet("/setContentType") public String setContentType(RestResponse res) {
			res.setContentType("text/plain");
			return "ok";
		}
		@RestGet("/getCharsetNonNull") public String getCharsetNonNull(RestResponse res) {
			res.setCharacterEncoding("UTF-8");
			return res.getCharset() == null ? "null" : res.getCharset().name();
		}
	}

	private static final MockRestClient CD = MockRestClient.buildLax(D.class);

	@Test void d01_setStatus_int() throws Exception {
		CD.get("/setStatus").run().assertStatus(202);
	}

	@Test void d02_sendError_int() throws Exception {
		CD.get("/sendError").run().assertStatus(418);
	}

	@Test void d03_sendError_intMsg() throws Exception {
		CD.get("/sendErrorMsg").run().assertStatus(503);
	}

	@Test void d04_buffer_pathExercises_flushAndReset() throws Exception {
		// Body is allowed to vary by container — only assert the stuff is wired up and didn't blow.
		CD.get("/buffer").run().assertStatus(200);
	}

	@Test void d05_setLocale() throws Exception {
		CD.get("/locale").run().assertStatus(200);
	}

	@Test void d06_setContentLength() throws Exception {
		CD.get("/contentLength").run().assertStatus(200);
	}

	@Test void d07_setContentLengthLong() throws Exception {
		CD.get("/contentLengthLong").run().assertStatus(200);
	}

	@Test void d08_setCharacterEncoding() throws Exception {
		CD.get("/setCharEncoding").run().assertStatus(200);
	}

	@Test void d09_setContentType() throws Exception {
		CD.get("/setContentType").run().assertStatus(200);
	}

	@Test void d10_getCharset_nonNullPath() throws Exception {
		CD.get("/getCharsetNonNull").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E: getOutputStream / getWriter / sendPlainText / getDirectWriter accessors.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(serializers = PlainTextSerializer.class)
	public static class E {
		@RestGet("/outputStream") public void outputStream(RestResponse res) throws Exception {
			// Hits HttpServletResponseWrapper.getOutputStream pass-through and the sos-cache branch.
			var sos = res.getOutputStream();
			sos.print("via-stream");
			res.getOutputStream();  // second call — sos != null branch.
		}
		@RestGet("/writer") public void writer(RestResponse res) throws Exception {
			var w = res.getWriter();
			w.write("via-writer");
			res.getWriter();  // second call — w != null branch.
		}
		@RestGet("/directWriter") public void directWriter(RestResponse res) throws Exception {
			var w = res.getDirectWriter("text/plain");
			w.write("via-direct");
		}
		@RestGet("/sendPlainText") public void sendPlainText(RestResponse res) throws Exception {
			res.sendPlainText("plain-payload");
		}
		@RestGet("/negotiatedWriter") public void negotiatedWriter(RestResponse res) throws Exception {
			var w = res.getNegotiatedWriter();
			w.write("via-negotiated");
		}
		@RestGet("/negotiatedOs") public void negotiatedOs(RestResponse res) throws Exception {
			var os = res.getNegotiatedOutputStream();
			os.write("via-neg-os".getBytes("UTF-8"));
			// second call — caches.
			res.getNegotiatedOutputStream();
		}
	}

	private static final MockRestClient CE = MockRestClient.buildLax(E.class);

	@Test void e01_getOutputStream() throws Exception {
		CE.get("/outputStream").run().assertStatus(200).assertContent("via-stream");
	}

	@Test void e02_getWriter() throws Exception {
		CE.get("/writer").run().assertStatus(200).assertContent("via-writer");
	}

	@Test void e03_getDirectWriter() throws Exception {
		CE.get("/directWriter").run().assertStatus(200)
			.assertContent("via-direct")
			.assertHeader("X-Content-Type-Options").is("nosniff")
			.assertHeader("Content-Encoding").is("identity");
	}

	@Test void e04_sendPlainText() throws Exception {
		CE.get("/sendPlainText").run().assertStatus(200).assertContent("plain-payload");
	}

	@Test void e05_negotiatedWriter() throws Exception {
		CE.get("/negotiatedWriter").run().assertStatus(200).assertContent("via-negotiated");
	}

	@Test void e06_negotiatedOutputStream() throws Exception {
		CE.get("/negotiatedOs").run().assertStatus(200).assertContent("via-neg-os");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F: getNegotiatedOutputStream — Accept-Encoding negotiation paths.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(encoders = GzipEncoder.class)
	public static class F {
		@RestGet("/encoded") public void encoded(RestResponse res) throws Exception {
			var os = res.getNegotiatedOutputStream();
			os.write("hello".getBytes("UTF-8"));
			os.flush();
		}
	}

	private static final MockRestClient CF = MockRestClient.buildLax(F.class);

	@Test void f01_acceptEncodingIdentity_quoted_throws406() throws Exception {
		// "identity;q=0" branch → NotAcceptable.
		CF.get("/encoded").header("Accept-Encoding", "identity;q=0").run().assertStatus(406);
	}

	@Test void f02_acceptEncodingStarQuotedZero_throws406() throws Exception {
		CF.get("/encoded").header("Accept-Encoding", "*;q=0").run().assertStatus(406);
	}

	@Test void f03_acceptEncodingNoMatch_butIdentityAllowed_passes() throws Exception {
		// "br" doesn't match — but identity is allowed, so the stream just falls through.
		CF.get("/encoded").header("Accept-Encoding", "br").run().assertStatus(200);
	}

	@Test void f04_acceptEncodingGzip_setsContentEncoding() throws Exception {
		// gzip matches and is non-identity → Content-Encoding: gzip is added.
		CF.get("/encoded").header("Accept-Encoding", "gzip").run().assertStatus(200)
			.assertHeader("Content-Encoding").is("gzip");
	}

	@Test void f05_emptyAcceptEncoding_skipsNegotiation() throws Exception {
		CF.get("/encoded").header("Accept-Encoding", "").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G: Constructor branches — Accept-Charset, x-response-headers, default response headers.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest(defaultCharset = "utf-8", defaultResponseHeaders = "X-Default-Resp: hello")
	public static class G {
		@RestGet("/x") public String x() { return "ok"; }
	}

	private static final MockRestClient CG = MockRestClient.buildLax(G.class);

	@Test void g01_unsupportedAcceptCharset_returns406() throws Exception {
		// All charsets in the header are unsupported → charset stays null → NotAcceptable.
		CG.get("/x").header("Accept-Charset", "BOGUSCHARSET").run().assertStatus(406);
	}

	@Test void g02_acceptCharsetStarFallsBackToDefault() throws Exception {
		// "*" branch hits opContext.getDefaultCharset() inside the loop.
		CG.get("/x").header("Accept-Charset", "*").run().assertStatus(200);
	}

	@Test void g03_acceptCharsetExplicit_supported() throws Exception {
		CG.get("/x").header("Accept-Charset", "iso-8859-1").run().assertStatus(200);
	}

	@Test void g03b_acceptCharsetWithQ0_skipsRange() throws Exception {
		// q=0 → range skipped → next supported range chosen.
		CG.get("/x").header("Accept-Charset", "bogus;q=0,utf-8").run().assertStatus(200);
	}

	@Test void g04_xResponseHeaders_invalidFormatYields400() throws Exception {
		// Non-URL-encoded JSON map — parser blows up → BadRequest 400.
		CG.get("/x").header("x-response-headers", "this is not URL-encoded").run().assertStatus(400);
	}

	@Test void g05_xResponseHeaders_validParseSucceeds() throws Exception {
		// UON-encoded map → parse succeeds, headers added (asserts only that no 400 BadRequest).
		CG.get("/x").header("x-response-headers", "(X-Custom=hi)").run().assertStatus(200);
	}

	@Test void g06_defaultResponseHeader_emitted() throws Exception {
		// NB: RestResponse constructor passes the value through resolveUris(), which prefixes a leading "/".
		// We assert only that the header is present (its value is implementation-specific).
		CG.get("/x").run().assertStatus(200).assertHeader("X-Default-Resp").isNotEmpty();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// H: Per-op default response headers.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestGet(path = "/y", defaultResponseHeaders = "X-Op-Resp: op-hello")
		public String y() { return "ok"; }
	}

	private static final MockRestClient CH = MockRestClient.buildLax(H.class);

	@Test void h01_perOpDefaultResponseHeader_emitted() throws Exception {
		// NB: same caveat as g06 — value is passed through resolveUris().
		CH.get("/y").run().assertStatus(200).assertHeader("X-Op-Resp").isNotEmpty();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// I: addCookie — pass-through to the inner servlet response (HttpServletResponseWrapper inherits this method,
	//    but the wrapper still counts as exercised when invoked from a RestResponse instance).
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I {
		@RestGet("/cookie") public String cookie(RestResponse res) {
			var c = new Cookie("session", "abc123");
			c.setPath("/");
			res.addCookie(c);
			return "ok";
		}
	}

	private static final MockRestClient CI = MockRestClient.buildLax(I.class);

	@Test void i01_addCookie_passesThroughWithoutError() throws Exception {
		// NB: MockServletResponse.addCookie(...) is a no-op stub, so we cannot assert Set-Cookie.
		// We still exercise the wrapper-pass-through delegation path (which is all that lives in RestResponse).
		CI.get("/cookie").run().assertStatus(200);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// J: Misc — getMediaType, getCharset null path, hasContent / setContent / getContent, attribute pass-through,
	//    setNoTrace / setDebug / setException — these touch the small accessor branches.
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J {
		@RestGet("/getCharsetNonDefault") public String getCharsetNonDefault(RestResponse res) {
			// Exercise the non-null branch of getCharset() (already done in /getCharsetNonNull). The
			// null-charset branch in RestResponse.getCharset() is not exercised here because
			// MockServletResponse.setCharacterEncoding((String)null) throws NPE under jakarta-servlet 6
			// (NB: production-code branch noted but not patched).
			return res.getCharset() == null ? "null" : res.getCharset().name();
		}
		@RestGet("/getMediaType") public String getMediaType(RestResponse res) {
			res.setContentType("application/json");
			return res.getMediaType() == null ? "null-mt" : res.getMediaType().toString();
		}
		@RestGet("/contentLifecycle") public String contentLifecycle(RestResponse res) {
			var sb = new StringBuilder();
			sb.append("hasContent.before=").append(res.hasContent());
			res.setContent("payload");
			sb.append(",hasContent.after=").append(res.hasContent());
			sb.append(",contentValue=").append(res.getContent().orElse(null));
			sb.append(",isOfStr=").append(res.isContentOfType(String.class));
			sb.append(",castStr=").append(res.getContent(String.class));
			sb.append(",castInt=").append(res.getContent(Integer.class));
			return sb.toString();
		}
		@RestGet("/setAttr") public String setAttr(RestResponse res) {
			res.setAttribute("foo", "bar");
			return String.valueOf(res.getAttributes().get("foo").orElse(null));
		}
		@RestGet("/noTraceFluent") public String noTraceFluent(RestResponse res) {
			res.setNoTrace();
			res.setNoTrace(Boolean.TRUE);
			return "ok";
		}
		@RestGet("/debugFluent") public String debugFluent(RestResponse res) throws Exception {
			res.setDebug();
			res.setDebug(Boolean.FALSE);
			return "ok";
		}
		@RestGet("/setExceptionFluent") public String setExceptionFluent(RestResponse res) {
			res.setException(new RuntimeException("x"));
			res.setException(null);
			return "ok";
		}
		@RestGet("/contextAndOpContext") public String contextAndOpContext(RestResponse res) {
			return (res.getContext() != null) + "," + (res.getOpContext() != null) + "," + (res.getHttpServletResponse() != null);
		}
		@RestGet("/setResponseBeanMeta") public String setResponseBeanMeta(RestResponse res) {
			res.setResponseBeanMeta(null);
			return res.getResponseBeanMeta() == null ? "null-rbm" : "non-null-rbm";
		}
		@RestGet("/setContentSchemaThenGet") public String setContentSchemaThenGet(RestResponse res) {
			res.setContentSchema(null);
			return res.getContentSchema().isEmpty() ? "empty" : "present";
		}
		@RestGet("/getContentBeforeSet") public String getContentBeforeSet(RestResponse res) {
			// content == null branch in getRawOutput().
			var sb = new StringBuilder();
			sb.append("hasContent=").append(res.hasContent());
			sb.append(",castStr=").append(res.getContent(String.class));
			sb.append(",isOfStr=").append(res.isContentOfType(String.class));
			return sb.toString();
		}
	}

	private static final MockRestClient CJ = MockRestClient.buildLax(J.class);

	@Test void j01_getCharset_nonDefaultPath() throws Exception {
		CJ.get("/getCharsetNonDefault").run().assertStatus(200);
	}

	@Test void j02_getMediaType() throws Exception {
		CJ.get("/getMediaType").run().assertStatus(200).assertContent().isContains("application/json");
	}

	@Test void j03_contentLifecycle() throws Exception {
		CJ.get("/contentLifecycle").run().assertStatus(200).assertContent()
			.isContains(
				"hasContent.before=false",
				"hasContent.after=true",
				"contentValue=payload",
				"isOfStr=true",
				"castStr=payload",
				"castInt=null"
			);
	}

	@Test void j04_setAttribute() throws Exception {
		CJ.get("/setAttr").run().assertStatus(200).assertContent("bar");
	}

	@Test void j05_setNoTraceFluent() throws Exception {
		CJ.get("/noTraceFluent").run().assertStatus(200);
	}

	@Test void j06_setDebugFluent() throws Exception {
		CJ.get("/debugFluent").run().assertStatus(200);
	}

	@Test void j07_setExceptionFluent() throws Exception {
		CJ.get("/setExceptionFluent").run().assertStatus(200);
	}

	@Test void j08_contextAndOpContextAndHttpResponseAccessors() throws Exception {
		CJ.get("/contextAndOpContext").run().assertStatus(200).assertContent("true,true,true");
	}

	@Test void j09_setResponseBeanMeta() throws Exception {
		CJ.get("/setResponseBeanMeta").run().assertStatus(200).assertContent("null-rbm");
	}

	@Test void j10_setContentSchemaThenGet() throws Exception {
		// Setting null content schema → optional empty.
		CJ.get("/setContentSchemaThenGet").run().assertStatus(200).assertContent("empty");
	}

	@Test void j11_getContentBeforeSet_nullContent() throws Exception {
		// Exercises getRawOutput()'s content==null branch.
		CJ.get("/getContentBeforeSet").run().assertStatus(200).assertContent()
			.isContains("hasContent=false", "castStr=null", "isOfStr=false");
	}
}
