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
package org.apache.juneau.rest.server;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Coverage tests for {@link RestRequest} accessor and helper methods.
 *
 * <p>Each handler exercises a discrete sliver of {@code RestRequest} surface and writes a sentinel string back
 * to the client; the test asserts the expected sentinel on the response.
 */
@SuppressWarnings({
	"resource"  // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class RestRequest_Coverage_Test extends TestBase {

	private static final String CT_FORM = "application/x-www-form-urlencoded";

	//------------------------------------------------------------------------------------------------------------------
	// Locale / Accept-Language parsing  (toLocale, getLocale)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {
		@RestGet("/locale")
		public String locale(RestRequest req) {
			var l = req.getLocale();
			return l.getLanguage() + "_" + l.getCountry();
		}

		@RestGet("/timeZone")
		public String timeZone(RestRequest req) {
			return req.getTimeZone().map(java.util.TimeZone::getID).orElse("none");
		}

		@RestGet("/messages")
		public String messages(RestRequest req) {
			return req.getMessages() != null ? "ok" : "fail";
		}
	}

	@Test void a01_localeFromAcceptLanguage_langCountry() throws Exception {
		var c = MockRestClient.create(A.class).plainText().build();
		c.get("/locale").header("Accept-Language", "fr-FR").run().assertContent("fr_FR");
	}

	@Test void a02_localeFromAcceptLanguage_langOnly() throws Exception {
		var c = MockRestClient.create(A.class).plainText().build();
		c.get("/locale").header("Accept-Language", "de").run().assertContent("de_");
	}

	@Test void a03_localeFromAcceptLanguage_qValueWinner() throws Exception {
		var c = MockRestClient.create(A.class).plainText().build();
		c.get("/locale").header("Accept-Language", "fr;q=0.5,de;q=0.9").run().assertContent("de_");
	}

	@Test void a04_timeZone_present() throws Exception {
		var c = MockRestClient.create(A.class).plainText().build();
		c.get("/timeZone").header("Time-Zone", "GMT").run().assertContent("GMT");
	}

	@Test void a05_timeZone_absent() throws Exception {
		var c = MockRestClient.create(A.class).plainText().build();
		c.get("/timeZone").run().assertContent("none");
	}

	@Test void a06_getMessages_nonNull() throws Exception {
		var c = MockRestClient.create(A.class).plainText().build();
		c.get("/messages").run().assertContent("ok");
	}

	//------------------------------------------------------------------------------------------------------------------
	// containsX, getX(name), assertX (charset/header/query/form)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet("/contains")
		public String contains(RestRequest req) {
			var hasH = req.containsHeader("X-Foo");
			var hasQ = req.containsQueryParam("q");
			return hasH + "," + hasQ;
		}
		@RestPost("/containsForm")
		public String containsForm(RestRequest req) {
			return Boolean.toString(req.containsFormParam("p1"));
		}
		@RestGet("/charset")
		public String charset(RestRequest req) {
			req.assertCharset();
			return req.getCharset().name();
		}
		@RestPost("/charsetCT")
		public String charsetCT(RestRequest req) {
			return req.getCharset().name();
		}
	}

	@Test void b01_containsHeaderAndQuery_present() throws Exception {
		var c = MockRestClient.create(B.class).plainText().build();
		c.get("/contains?q=1").header("X-Foo", "bar").run().assertContent("true,true");
	}

	@Test void b02_containsHeaderAndQuery_absent() throws Exception {
		var c = MockRestClient.create(B.class).plainText().build();
		c.get("/contains").run().assertContent("false,false");
	}

	@Test void b03_containsFormParam_present() throws Exception {
		var c = MockRestClient.create(B.class).plainText().build();
		c.post("/containsForm", "p1=v").contentType(CT_FORM).run().assertContent("true");
	}

	@Test void b04_containsFormParam_absent() throws Exception {
		var c = MockRestClient.create(B.class).plainText().build();
		c.post("/containsForm", "p2=v").contentType(CT_FORM).run().assertContent("false");
	}

	@Test void b05_charset_default_isUtf8() throws Exception {
		var c = MockRestClient.create(B.class).plainText().build();
		c.get("/charset").run().assertContent("UTF-8");
	}

	@Test void b06_charset_fromContentType() throws Exception {
		var c = MockRestClient.create(B.class).plainText().build();
		c.post("/charsetCT", "x").header("Content-Type", "text/plain;charset=ISO-8859-1").run().assertContent("ISO-8859-1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// getAuthorityPath, getContextPath, getServletPath, getRequestLine, getProtocolVersion
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestGet("/auth")
		public String auth(RestRequest req) { return req.getAuthorityPath(); }

		@RestGet("/ctx")
		public String ctx(RestRequest req) { return req.getContextPath(); }

		@RestGet("/sp")
		public String sp(RestRequest req) { return req.getServletPath(); }

		@RestGet("/line")
		public String line(RestRequest req) {
			var rl = req.getRequestLine();
			return rl.getMethod() + " " + rl.getUri() + " HTTP/" + rl.getProtocolVersion().major() + "." + rl.getProtocolVersion().minor();
		}

		@RestGet("/proto")
		public String proto(RestRequest req) {
			var v = req.getProtocolVersion();
			return v.protocol() + "/" + v.major() + "." + v.minor();
		}

		@RestGet("/uri")
		public String uri(RestRequest req) {
			return req.getUri(true, java.util.Map.of("extra", "x")).toString();
		}

		@RestGet("/uriNoQuery")
		public String uriNoQuery(RestRequest req) {
			return req.getUri(false, null).toString();
		}

		@RestGet("/uriCtx")
		public String uriCtx(RestRequest req) {
			var u = req.getUriContext();
			return u.authority + "|" + u.contextRoot + "|" + u.servletPath;
		}

		@RestGet("/uriRes")
		public String uriRes(RestRequest req) {
			req.getUriResolver();
			req.getUriResolver(org.apache.juneau.marshall.UriResolution.ROOT_RELATIVE, org.apache.juneau.marshall.UriRelativity.RESOURCE);
			return "ok";
		}
	}

	// /auth (authorityPath), /ctx (contextPath), /sp (servletPath) and /uriCtx (uriContext)
	// each exercise a distinct RestRequest accessor in C and just need a 200 status.
	@ParameterizedTest
	@ValueSource(strings = {"/auth", "/ctx", "/sp", "/uriCtx"})
	void c01_accessorEndpointsReturn200(String path) throws Exception {
		var c = MockRestClient.create(C.class).plainText().build();
		c.get(path).run().assertStatus(200);
	}

	@Test void c04_requestLine_andProtocolVersion() throws Exception {
		var c = MockRestClient.create(C.class).plainText().build();
		c.get("/line").run().assertStatus(200).assertContent().asString().isContains("GET", "HTTP/1.1");
		c.get("/proto").run().assertStatus(200).assertContent().asString().isContains("HTTP/1.1");
	}

	@Test void c05_uri_includeQuery_andExtraParams() throws Exception {
		var c = MockRestClient.create(C.class).plainText().build();
		c.get("/uri?a=1").run().assertStatus(200).assertContent().asString().isContains("/uri", "extra=x");
	}

	@Test void c06_uri_excludeQuery() throws Exception {
		var c = MockRestClient.create(C.class).plainText().build();
		c.get("/uriNoQuery?a=1").run().assertStatus(200).assertContent().asString().isContains("/uriNoQuery");
	}

	@Test void c08_uriResolver_bothOverloads() throws Exception {
		var c = MockRestClient.create(C.class).plainText().build();
		c.get("/uriRes").run().assertContent("ok");
	}

	//------------------------------------------------------------------------------------------------------------------
	// getConfig, getOpenApi, getSwagger, getOperationSwagger
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class D {
		@RestGet("/config")
		public String config(RestRequest req) {
			req.getConfig();
			return "ok";
		}

		@RestGet("/swagger")
		public String swagger(RestRequest req) {
			req.getSwagger();
			req.getOperationSwagger();
			return "ok";
		}

		@RestGet("/openapi")
		public String openapi(RestRequest req) {
			req.getOpenApi();
			return "ok";
		}
	}

	// /config (getConfig), /swagger (getSwagger + getOperationSwagger) and /openapi (getOpenApi)
	// each return the "ok" sentinel after exercising their accessor.
	@ParameterizedTest
	@ValueSource(strings = {"/config", "/swagger", "/openapi"})
	void d01_metadataAccessorEndpointsReturnOk(String path) throws Exception {
		var c = MockRestClient.create(D.class).plainText().build();
		c.get(path).run().assertContent("ok");
	}

	//------------------------------------------------------------------------------------------------------------------
	// getRequest(Class) - request bean proxy
	//------------------------------------------------------------------------------------------------------------------

	@Request
	public interface RB {
		@Query
		String getQ();
		@Header("X-H")
		String getH();
		@Path
		String getP();
	}

	@Rest
	public static class E {
		@RestGet("/req/{p}")
		public String req(RestRequest req) {
			var rb = req.getRequest(RB.class);
			return rb.getQ() + "|" + rb.getH() + "|" + rb.getP();
		}
	}

	@Test void e01_requestBeanProxy_query_header_path() throws Exception {
		var c = MockRestClient.create(E.class).plainText().build();
		c.get("/req/p1?q=qv").header("X-H", "hv").run().assertContent("qv|hv|p1");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Debug scope (enable/disable/isEnabled), setDebug, setNoTrace, setException
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {
		@RestGet("/debugFmt")
		public String debugFmt(RestRequest req) throws java.io.IOException {
			req.debug().enable(null);
			req.debug().enable(org.apache.juneau.rest.server.debug.format.BasicTextFormat.class);
			req.debug().disable();
			return Boolean.toString(req.debug().isEnabled());
		}
		@RestGet("/setDebug")
		public String setDebug(RestRequest req) throws java.io.IOException {
			req.setDebug();
			req.setDebug(false);
			return "ok";
		}
		@RestGet("/noTrace")
		public String noTrace(RestRequest req) {
			req.setNoTrace();
			req.setNoTrace(false);
			return "ok";
		}
		@RestGet("/exception")
		public String exception(RestRequest req) {
			req.setException(new RuntimeException("test"));
			return "ok";
		}
		@RestGet("/setCharset")
		public String setCharset(RestRequest req) {
			req.setCharset(java.nio.charset.StandardCharsets.UTF_16);
			return req.getCharset().name();
		}
	}

	@Test void f01_debugScope_enableWithFormat_andDisable() throws Exception {
		var c = MockRestClient.create(F.class).plainText().build();
		c.get("/debugFmt").run().assertContent("false");
	}

	@Test void f02_setDebug_overloads() throws Exception {
		var c = MockRestClient.create(F.class).plainText().build();
		c.get("/setDebug").run().assertContent("ok");
	}

	@Test void f03_setNoTrace_overloads() throws Exception {
		var c = MockRestClient.create(F.class).plainText().build();
		c.get("/noTrace").run().assertContent("ok");
	}

	@Test void f04_setException() throws Exception {
		var c = MockRestClient.create(F.class).plainText().build();
		c.get("/exception").run().assertContent("ok");
	}

	@Test void f05_setCharset() throws Exception {
		var c = MockRestClient.create(F.class).plainText().build();
		c.get("/setCharset").run().assertContent("UTF-16");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Session property maps via query param / header (UON / JSON5)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers=JsonSerializer.class, parsers=JsonParser.class)
	public static class G {
		@RestGet(path="/sp", allowedSerializerOptions="key1", allowedParserOptions="key2")
		public String sp(RestRequest req) {
			var s = req.getSerializerSessionPropertyMap();
			var p = req.getParserSessionPropertyMap();
			return "s:" + s.get("key1") + ",p:" + p.get("key2");
		}
	}

	@Test void g01_sessionPropertiesFromQueryParam() throws Exception {
		var c = MockRestClient.create(G.class).plainText().build();
		c.get("/sp?juneauSerializerOptions=(key1=v1)&juneauParserOptions=(key2=v2)").run().assertContent("s:v1,p:v2");
	}

	@Test void g02_sessionPropertiesFromHeader() throws Exception {
		var c = MockRestClient.create(G.class).plainText().build();
		c.get("/sp")
			.header("X-Juneau-Serializer-Options", "{key1:'h1'}")
			.header("X-Juneau-Parser-Options", "{key2:'h2'}")
			.run()
			.assertContent("s:h1,p:h2");
	}

	@Test void g03_sessionProperties_invalidKeyInQuery_400() throws Exception {
		var c = MockRestClient.create(G.class).plainText().disableRedirectHandling().ignoreErrors().build();
		c.get("/sp?juneauSerializerOptions=(badKey=v)").run().assertStatus(400);
	}

	@Test void g04_sessionProperties_invalidKeyInHeader_400() throws Exception {
		var c = MockRestClient.create(G.class).plainText().disableRedirectHandling().ignoreErrors().build();
		c.get("/sp").header("X-Juneau-Parser-Options", "{badKey:'x'}").run().assertStatus(400);
	}

	@Test void g05_sessionProperties_malformedUon_400() throws Exception {
		var c = MockRestClient.create(G.class).plainText().disableRedirectHandling().ignoreErrors().build();
		c.get("/sp?juneauSerializerOptions=@notAMap").run().assertStatus(400);
	}

	@Test void g06_sessionProperties_malformedJson_400() throws Exception {
		var c = MockRestClient.create(G.class).plainText().disableRedirectHandling().ignoreErrors().build();
		c.get("/sp").header("X-Juneau-Serializer-Options", "{notValidJson:").run().assertStatus(400);
	}

	//------------------------------------------------------------------------------------------------------------------
	// toString - content branch for PUT/POST
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class H {
		@RestPost("/toString")
		public String toString0(RestRequest req) {
			return req.toString();
		}
		@RestGet("/toString")
		public String toStringGet(RestRequest req) {
			return req.toString();
		}
	}

	@Test void h01_toString_post() throws Exception {
		var c = MockRestClient.create(H.class).plainText().build();
		c.post("/toString", "abc").run().assertStatus(200).assertContent().asString().isContains("POST", "Headers");
	}

	@Test void h02_toString_get() throws Exception {
		var c = MockRestClient.create(H.class).plainText().build();
		c.get("/toString").run().assertStatus(200).assertContent().asString().isContains("GET", "Headers");
	}

	//------------------------------------------------------------------------------------------------------------------
	// isPlainText, attribute set/get, opContext / context / partSerializerSession
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class I {
		@RestGet("/plain")
		public String plain(RestRequest req) {
			return Boolean.toString(req.isPlainText());
		}
		@RestGet("/attr")
		public String attr(RestRequest req) {
			req.setAttribute("k", "v");
			return req.getAttribute("k").as(String.class).orElse(null);
		}
		@RestGet("/opCtx")
		public String opCtx(RestRequest req) {
			return req.getOpContext() != null && req.getContext() != null && req.getPartSerializerSession() != null ? "ok" : "fail";
		}
	}

	@Test void i01_isPlainText_true() throws Exception {
		var c = MockRestClient.create(I.class).plainText().build();
		c.get("/plain?plainText=true").run().assertContent("true");
	}

	@Test void i02_isPlainText_false() throws Exception {
		var c = MockRestClient.create(I.class).plainText().build();
		c.get("/plain").run().assertContent("false");
	}

	@Test void i03_attribute_setAndGet() throws Exception {
		var c = MockRestClient.create(I.class).plainText().build();
		c.get("/attr").run().assertContent("v");
	}

	@Test void i04_opContext_andContext_andPartSerializer() throws Exception {
		var c = MockRestClient.create(I.class).plainText().build();
		c.get("/opCtx").run().assertContent("ok");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Allow content param (?content=...) - exercises constructor branch.
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class J {
		@RestPost("/echo")
		public String echo(@Content String body) {
			return body;
		}
	}

	@Test void j01_allowContentParam_default_disabled() throws Exception {
		var c = MockRestClient.create(J.class).plainText().disableRedirectHandling().ignoreErrors().build();
		c.post("/echo", "raw").run().assertStatus(200).assertContent("raw");
	}
}
