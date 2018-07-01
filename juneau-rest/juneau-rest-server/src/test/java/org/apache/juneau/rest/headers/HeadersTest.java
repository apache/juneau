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
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.rest.headers;

import static org.apache.juneau.http.HttpMethodName.*;

import org.apache.juneau.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests various aspects of headers in general.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HeadersTest {

	//====================================================================================================
	// HTTP 1.1 headers
	//====================================================================================================

	@RestResource(
		serializers=AnythingSerializer.class,
		parsers=AnythingParser.class,
		encoders=AnythingEncoder.class
	)
	public static class A {

		@RestMethod(name=GET, path="/accept")
		public String accept(Accept accept) {
			return accept.toString();
		}
		@RestMethod(name=GET, path="/acceptCharset")
		public String acceptCharset(AcceptCharset acceptCharset) {
			return acceptCharset.toString();
		}
		@RestMethod(name=GET, path="/acceptEncoding")
		public String acceptEncoding(AcceptEncoding acceptEncoding) {
			return acceptEncoding.toString();
		}
		@RestMethod(name=GET, path="/acceptLanguage")
		public String acceptLanguage(AcceptLanguage acceptLanguage) {
			return acceptLanguage.toString();
		}
		@RestMethod(name=GET, path="/authorization")
		public String authorization(Authorization authorization) {
			return authorization.toString();
		}
		@RestMethod(name=GET, path="/cacheControl")
		public String cacheControl(CacheControl cacheControl) {
			return cacheControl.toString();
		}
		@RestMethod(name=GET, path="/connection")
		public String connection(Connection connection) {
			return connection.toString();
		}
		@RestMethod(name=GET, path="/contentLength")
		public String contentLength(ContentLength contentLength) {
			return contentLength.toString();
		}
		@RestMethod(name=GET, path="/contentType")
		public String contentType(ContentType contentType) {
			return contentType.toString();
		}
		@RestMethod(name=GET, path="/date")
		public String date(org.apache.juneau.http.Date date) {
			return date.toString();
		}
		@RestMethod(name=GET, path="/expect")
		public String expect(Expect expect) {
			return expect.toString();
		}
		@RestMethod(name=GET, path="/from")
		public String from(From from) {
			return from.toString();
		}
		@RestMethod(name=GET, path="/host")
		public String host(Host host) {
			return host.toString();
		}
		@RestMethod(name=GET, path="/ifMatch")
		public String IfMatch(IfMatch ifMatch) {
			return ifMatch.toString();
		}
		@RestMethod(name=GET, path="/ifModifiedSince")
		public String ifModifiedSince(IfModifiedSince ifModifiedSince) {
			return ifModifiedSince.toString();
		}
		@RestMethod(name=GET, path="/ifNoneMatch")
		public String ifNoneMatch(IfNoneMatch ifNoneMatch) {
			return ifNoneMatch.toString();
		}
		@RestMethod(name=GET, path="/ifRange")
		public String ifRange(IfRange ifRange) {
			return ifRange.toString();
		}
		@RestMethod(name=GET, path="/ifUnmodifiedSince")
		public String ifUnmodifiedSince(IfUnmodifiedSince ifUnmodifiedSince) {
			return ifUnmodifiedSince.toString();
		}
		@RestMethod(name=GET, path="/maxForwards")
		public String maxForwards(MaxForwards maxForwards) {
			return maxForwards.toString();
		}
		@RestMethod(name=GET, path="/pragma")
		public String pragma(Pragma pragma) {
			return pragma.toString();
		}
		@RestMethod(name=GET, path="/proxyAuthorization")
		public String proxyAuthorization(ProxyAuthorization proxyAuthorization) {
			return proxyAuthorization.toString();
		}
		@RestMethod(name=GET, path="/range")
		public String range(Range range) {
			return range.toString();
		}
		@RestMethod(name=GET, path="/referer")
		public String referer(Referer referer) {
			return referer.toString();
		}
		@RestMethod(name=GET, path="/te")
		public String te(TE te) {
			return te.toString();
		}
		@RestMethod(name=GET, path="/upgrade")
		public String upgrade(Upgrade upgrade) {
			return upgrade.toString();
		}
		@RestMethod(name=GET, path="/userAgent")
		public String userAgent(UserAgent userAgent) {
			return userAgent.toString();
		}
		@RestMethod(name=GET, path="/warning")
		public String warning(Warning warning) {
			return warning.toString();
		}
	}
	private static MockRest a = MockRest.create(A.class);

	public static class AnythingSerializer extends PlainTextSerializer {
		public AnythingSerializer(PropertyStore ps) {
			super(ps, "text/plain", "*/*");
		}
	}

	public static class AnythingParser extends PlainTextParser {
		public AnythingParser(PropertyStore ps) {
			super(ps, "*/*");
		}
	}

	public static class AnythingEncoder extends IdentityEncoder {
		@Override /* ConfigEncoder */
		public String[] getCodings() {
			return new String[]{"*"};
		}
	}

	@Test
	public void a01a_accept() throws Exception {
		a.get("/accept").accept("text/foo").execute().assertBody("text/foo");
		a.get("/accept").accept("text/foo+bar").execute().assertBody("text/foo+bar");
		a.get("/accept").accept("text/*").execute().assertBody("text/*");
		a.get("/accept").accept("*/foo").execute().assertBody("*/foo");
	}
	@Test
	public void a01b_accept_qValues() throws Exception {
		a.get("/accept").accept("text/foo;q=1.0").execute().assertBody("text/foo");
		a.get("/accept").accept("text/foo;q=0.9").execute().assertBody("text/foo;q=0.9");
		a.get("/accept").accept("text/foo;x=X;q=0.9;y=Y").execute().assertBody("text/foo;x=X;q=0.9;y=Y");
	}
	@Test
	public void a01b_accept_query() throws Exception {
		a.get("/accept?Accept=text/foo").execute().assertBody("text/foo");
	}
	@Test
	public void a02a_acceptCharset() throws Exception {
		a.get("/acceptCharset").acceptCharset("UTF-8").execute().assertBody("UTF-8");
	}
	@Test
	public void a02b_acceptCharset_query() throws Exception {
		a.get("/acceptCharset?Accept-Charset=UTF-8").execute().assertBody("UTF-8");
	}
	@Test
	public void a03a_acceptEncoding() throws Exception {
		a.get("/acceptEncoding").acceptEncoding("foo").execute().assertBody("foo");
		a.get("/acceptEncoding").acceptEncoding("*").execute().assertBody("*");
	}
	@Test
	public void a03b_acceptEncoding_query() throws Exception {
		a.get("/acceptEncoding?Accept-Encoding=*").execute().assertBody("*");
	}
	@Test
	public void a04a_acceptLanguage() throws Exception {
		a.get("/acceptLanguage").acceptLanguage("foo").execute().assertBody("foo");
	}
	@Test
	public void a04b_acceptLanguage_query() throws Exception {
		a.get("/acceptLanguage?Accept-Language=foo").acceptLanguage("foo").execute().assertBody("foo");
	}
	@Test
	public void a05a_authorization() throws Exception {
		a.get("/authorization").authorization("foo").execute().assertBody("foo");
	}
	@Test
	public void a05b_authorization_query() throws Exception {
		a.get("/authorization?Authorization=foo").execute().assertBody("foo");
	}
	@Test
	public void a06a_cacheControl() throws Exception {
		a.get("/cacheControl").cacheControl("foo").execute().assertBody("foo");
	}
	@Test
	public void a06b_cacheControl_query() throws Exception {
		a.get("/cacheControl?Cache-Control=foo").execute().assertBody("foo");
	}
	@Test
	public void a07a_connection() throws Exception {
		a.get("/connection").connection("foo").execute().assertBody("foo");
	}
	@Test
	public void a07b_connection_query() throws Exception {
		a.get("/connection?Connection=foo").execute().assertBody("foo");
	}
	@Test
	public void a08a_contentLength() throws Exception {
		a.get("/contentLength").contentLength(0).execute().assertBody("0");
	}
	@Test
	public void a08b_contentLength_query() throws Exception {
		a.get("/contentLength?Content-Length=0").execute().assertBody("0");
	}
	@Test
	public void a09a_contentType() throws Exception {
		a.get("/contentType").contentType("text/foo").execute().assertBody("text/foo");
	}
	@Test
	public void a09b_contentType_query() throws Exception {
		a.get("/contentType?Content-Type=text/foo").execute().assertBody("text/foo");
	}
	@Test
	public void a10a_date() throws Exception {
		a.get("/date").date("foo").execute().assertBody("foo");
	}
	@Test
	public void a10b_date_query() throws Exception {
		a.get("/date?Date=foo").execute().assertBody("foo");
	}
	@Test
	public void a11a_expect() throws Exception {
		a.get("/expect").expect("100-continue").execute().assertBody("100-continue");
	}
	@Test
	public void a11b_expect_query() throws Exception {
		a.get("/expect?Expect=100-continue").execute().assertBody("100-continue");
	}
	@Test
	public void a12a_from() throws Exception {
		a.get("/from").from("foo").execute().assertBody("foo");
	}
	public void a12b_from_query() throws Exception {
		a.get("/from?From=foo").execute().assertBody("foo");
	}
	@Test
	public void a13a_host() throws Exception {
		a.get("/host").host("localhost").execute().assertBody("localhost");
	}
	@Test
	public void a13b_host_query() throws Exception {
		a.get("/host?Host=localhost").execute().assertBody("localhost");
	}
	@Test
	public void a14a_ifMatch() throws Exception {
		a.get("/ifMatch").ifMatch("foo").execute().assertBody("\"foo\"");
		a.get("/ifMatch").ifMatch("\"foo\"").execute().assertBody("\"foo\"");
		a.get("/ifMatch").ifMatch("W/\"foo\"").execute().assertBody("W/\"foo\"");
		a.get("/ifMatch").ifMatch("W/\"foo\",\"bar\"").execute().assertBody("W/\"foo\", \"bar\"");
	}
	@Test
	public void a14b_ifMatch_query() throws Exception {
		a.get("/ifMatch?If-Match=foo").execute().assertBody("\"foo\"");
	}
	@Test
	public void a15a_ifModifiedSince() throws Exception {
		a.get("/ifModifiedSince").ifModifiedSince("foo").execute().assertBody("foo");
	}
	@Test
	public void a15b_ifModifiedSince_query() throws Exception {
		a.get("/ifModifiedSince?If-Modified-Since=foo").execute().assertBody("foo");
	}
	@Test
	public void a16a_ifNoneMatch() throws Exception {
		a.get("/ifNoneMatch").ifNoneMatch("foo").execute().assertBody("\"foo\"");
		a.get("/ifNoneMatch").ifNoneMatch("\"foo\"").execute().assertBody("\"foo\"");
		a.get("/ifNoneMatch").ifNoneMatch("W/\"foo\"").execute().assertBody("W/\"foo\"");
		a.get("/ifNoneMatch").ifNoneMatch("W/\"foo\",\"bar\"").execute().assertBody("W/\"foo\", \"bar\"");
	}
	@Test
	public void a16b_ifNoneMatch_query() throws Exception {
		a.get("/ifNoneMatch?If-None-Match=foo").execute().assertBody("\"foo\"");
	}
	@Test
	public void a17a_ifRange() throws Exception {
		a.get("/ifRange").ifRange("foo").execute().assertBody("foo");
	}
	@Test
	public void a17b_ifRange_query() throws Exception {
		a.get("/ifRange?If-Range=foo").execute().assertBody("foo");
	}
	@Test
	public void a18a_ifUnmodifiedSince() throws Exception {
		a.get("/ifUnmodifiedSince").ifUnmodifiedSince("foo").execute().assertBody("foo");
	}
	@Test
	public void a18b_ifUnmodifiedSince_query() throws Exception {
		a.get("/ifUnmodifiedSince?If-Unmodified-Since=foo").execute().assertBody("foo");
	}
	@Test
	public void a19a_maxForwards() throws Exception {
		a.get("/maxForwards").maxForwards(123).execute().assertBody("123");
	}
	@Test
	public void a19b_maxForwards_query() throws Exception {
		a.get("/maxForwards?Max-Forwards=123").execute().assertBody("123");
	}
	@Test
	public void a20a_pragma() throws Exception {
		a.get("/pragma").pragma("foo").execute().assertBody("foo");
	}
	@Test
	public void a20b_pragma_query() throws Exception {
		a.get("/pragma?Pragma=foo").execute().assertBody("foo");
	}
	@Test
	public void a21a_proxyAuthorization() throws Exception {
		a.get("/proxyAuthorization").proxyAuthorization("foo").execute().assertBody("foo");
	}
	@Test
	public void a21b_proxyAuthorization_query() throws Exception {
		a.get("/proxyAuthorization?Proxy-Authorization=foo").execute().assertBody("foo");
	}
	@Test
	public void a22a_range() throws Exception {
		a.get("/range").range("foo").execute().assertBody("foo");
	}
	@Test
	public void a22b_range_query() throws Exception {
		a.get("/range?Range=foo").execute().assertBody("foo");
	}
	@Test
	public void a23a_referer() throws Exception {
		a.get("/referer").referer("foo").execute().assertBody("foo");
	}
	@Test
	public void a23b_referer_query() throws Exception {
		a.get("/referer?Referer=foo").execute().assertBody("foo");
	}
	@Test
	public void a24a_te() throws Exception {
		a.get("/te").te("foo").execute().assertBody("foo");
	}
	@Test
	public void a24b_te_query() throws Exception {
		a.get("/te?TE=foo").execute().assertBody("foo");
	}
	@Test
	public void a25a_upgrade() throws Exception {
		a.get("/upgrade").upgrade("foo").execute().assertBody("foo");
	}
	@Test
	public void a25b_upgrade_query() throws Exception {
		a.get("/upgrade?Upgrade=foo").execute().assertBody("foo");
	}
	@Test
	public void a26a_userAgent() throws Exception {
		a.get("/userAgent").userAgent("foo").execute().assertBody("foo");
	}
	@Test
	public void a26b_userAgent_query() throws Exception {
		a.get("/userAgent?User-Agent=foo").execute().assertBody("foo");
	}
	@Test
	public void a27a_warning() throws Exception {
		a.get("/warning").warning("foo").execute().assertBody("foo");
	}
	@Test
	public void a27b_warning_query() throws Exception {
		a.get("/warning?Warning=foo").execute().assertBody("foo");
	}

	//====================================================================================================
	// Custom header.
	//====================================================================================================

	@RestResource(paramResolvers=CustomHeaderParam.class)
	public static class B {
		@RestMethod(name=GET)
		public String customHeader(CustomHeader customHeader) {
			return customHeader.toString();
		}
	}
	static MockRest b = MockRest.create(B.class);

	public static class CustomHeaderParam extends RestMethodParam {
		public CustomHeaderParam() {
			super(RestParamType.HEADER, "Custom", CustomHeader.class);
		}
		@Override
		public Object resolve(RestRequest req, RestResponse res) throws Exception {
			return new CustomHeader(req.getHeader("Custom"));
		}
	}

	public static class CustomHeader {
		public String value;
		public CustomHeader(String value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return value;
		}
	}

	@Test
	public void b01a_customHeader() throws Exception {
		b.get("/").header("Custom", "foo").execute().assertBody("foo");
	}
	@Test
	public void b01b_customHeader_query() throws Exception {
		b.get("?Custom=foo").execute().assertBody("foo");
	}

	//====================================================================================================
	// Default values - Default request headers
	//====================================================================================================

	@RestResource
	public static class C {
		@RestMethod(name=GET, defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public ObjectMap c(RequestHeaders headers) {
			return new ObjectMap()
				.append("h1", headers.getString("H1"))
				.append("h2", headers.getString("H2"))
				.append("h3", headers.getString("H3"));
		}
	}
	static MockRest c = MockRest.create(C.class);

	@Test
	public void c01_defaultRequestHeaders_default() throws Exception {
		c.get("/").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void c02_defaultRequestHeaders_override() throws Exception {
		c.get("/").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void c03_defaultRequestHeaders_override_caseInsensitive() throws Exception {
		c.get("/").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Default request headers, case-insensitive matching
	//====================================================================================================

	@RestResource
	public static class D {
		@RestMethod(name=GET, defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public ObjectMap d(RequestHeaders headers) {
			return new ObjectMap()
				.append("h1", headers.getString("h1"))
				.append("h2", headers.getString("h2"))
				.append("h3", headers.getString("h3"));
		}
	}
	static MockRest d = MockRest.create(D.class);

	@Test
	public void d01_defaultRequestHeadersCaseInsensitive_default() throws Exception {
		d.get("/").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void d02_defaultRequestHeadersCaseInsensitive_override() throws Exception {
		d.get("/").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void d03_defaultRequestHeadersCaseInsensitive_override_caseInsensitive() throws Exception {
		d.get("/").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers.
	//====================================================================================================

	@RestResource
	public static class E {
		@RestMethod(name=GET)
		public ObjectMap e(@Header(name="H1") String h1, @Header("H2") String h2, @Header("H3") String h3) {
			return new ObjectMap()
				.append("h1", h1)
				.append("h2", h2)
				.append("h3", h3);
		}
	}
	static MockRest e = MockRest.create(E.class);

	@Test
	public void e01_annotatedHeaders_default() throws Exception {
		e.get("/").execute().assertBody("{h1:null,h2:null,h3:null}");
	}
	@Test
	public void e02_annotatedHeaders_override() throws Exception {
		e.get("/").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void e03_annotatedHeaders_override_caseInsensitive() throws Exception {
		e.get("/").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers, case-insensitive matching.
	//====================================================================================================

	@RestResource
	public static class F {
		@RestMethod(name=GET)
		public ObjectMap f(@Header("h1") String h1, @Header("h2") String h2, @Header("h3") String h3) {
			return new ObjectMap()
				.append("h1", h1)
				.append("h2", h2)
				.append("h3", h3);
		}
	}
	static MockRest f = MockRest.create(F.class);

	@Test
	public void f01_annotatedHeadersCaseInsensitive_default() throws Exception {
		f.get("/").execute().assertBody("{h1:null,h2:null,h3:null}");
	}
	@Test
	public void f02_annotatedHeadersCaseInsensitive_override() throws Exception {
		f.get("/").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void f03_annotatedHeadersCaseInsensitive_override_caseInsensitive() throws Exception {
		f.get("/").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers with default values.
	//====================================================================================================

	@RestResource
	public static class G {
		@RestMethod(name=GET)
		public ObjectMap g(@Header(name="h1",_default="1") String h1, @Header(name="h2",_default="2") String h2, @Header(name="h3",_default="3") String h3) {
			return new ObjectMap()
				.append("h1", h1)
				.append("h2", h2)
				.append("h3", h3);
		}
	}
	static MockRest g = MockRest.create(G.class);

	@Test
	public void g01_annotatedHeadersDefault_default() throws Exception {
		g.get("/").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void g02_annotatedHeadersDefault_override() throws Exception {
		g.get("/").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void g03_annotatedHeadersDefault_override_caseInsensitive() throws Exception {
		g.get("/").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	@RestResource
	public static class GB {
		@RestMethod(name=GET)
		public ObjectMap g(@Header(value="h1",_default="1") String h1, @Header(value="h2",_default="2") String h2, @Header(value="h3",_default="3") String h3) {
			return new ObjectMap()
				.append("h1", h1)
				.append("h2", h2)
				.append("h3", h3);
		}
	}
	static MockRest gb = MockRest.create(GB.class);

	@Test
	public void gb01_annotatedHeadersDefault_default() throws Exception {
		gb.get("/").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void gb02_annotatedHeadersDefault_override() throws Exception {
		gb.get("/").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void gb03_annotatedHeadersDefault_override_caseInsensitive() throws Exception {
		gb.get("/").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers with default values and default request headers.
	//====================================================================================================

	@RestResource
	public static class H {
		@RestMethod(name=GET, defaultRequestHeaders={"H1:1","H2=2"," H3 : 3 "})
		public ObjectMap h(@Header(value="h1",_default="4") String h1, @Header(value="h2",_default="5") String h2, @Header(value="h3",_default="6") String h3) {
			return new ObjectMap()
				.append("h1", h1)
				.append("h2", h2)
				.append("h3", h3);
		}
	}
	static MockRest h = MockRest.create(H.class);

	@Test
	public void h01_annotatedAndDefaultHeaders_default() throws Exception {
		h.get("/").execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void h02_annotatedAndDefaultHeaders_override() throws Exception {
		h.get("/").header("H1",7).header("H2",8).header("H3",9).execute().assertBody("{h1:'7',h2:'8',h3:'9'}");
	}
	@Test
	public void h03_annotatedAndDefaultHeaders_override_caseInsensitive() throws Exception {
		h.get("/").header("h1",7).header("h2",8).header("h3",9).execute().assertBody("{h1:'7',h2:'8',h3:'9'}");
	}
}
