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

import static org.apache.juneau.rest.testutils.TestUtils.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests various aspects of headers in general.
 */
@SuppressWarnings({})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HeadersTest {

	//====================================================================================================
	// HTTP 1.1 headers
	//====================================================================================================

	@Rest(
		serializers=AnythingSerializer.class,
		parsers=AnythingParser.class,
		encoders=AnythingEncoder.class,
		allowedHeaderParams="*"
	)
	public static class A {

		@RestMethod
		public String accept(Accept accept) {
			return accept.toString();
		}
		@RestMethod
		public String acceptCharset(AcceptCharset acceptCharset) {
			return acceptCharset.toString();
		}
		@RestMethod
		public String acceptEncoding(AcceptEncoding acceptEncoding) {
			return acceptEncoding.toString();
		}
		@RestMethod
		public String acceptLanguage(AcceptLanguage acceptLanguage) {
			return acceptLanguage.toString();
		}
		@RestMethod
		public String authorization(Authorization authorization) {
			return authorization.toString();
		}
		@RestMethod
		public String cacheControl(CacheControl cacheControl) {
			return cacheControl.toString();
		}
		@RestMethod
		public String connection(Connection connection) {
			return connection.toString();
		}
		@RestMethod
		public String contentLength(ContentLength contentLength) {
			return contentLength.toString();
		}
		@RestMethod
		public String contentType(ContentType contentType) {
			return contentType.toString();
		}
		@RestMethod
		public String date(org.apache.juneau.http.Date date) {
			return date.toString();
		}
		@RestMethod
		public String expect(Expect expect) {
			return expect.toString();
		}
		@RestMethod
		public String from(From from) {
			return from.toString();
		}
		@RestMethod
		public String host(Host host) {
			return host.toString();
		}
		@RestMethod
		public String ifMatch(IfMatch ifMatch) {
			return ifMatch.toString();
		}
		@RestMethod
		public String ifModifiedSince(IfModifiedSince ifModifiedSince) {
			return ifModifiedSince.toString();
		}
		@RestMethod
		public String ifNoneMatch(IfNoneMatch ifNoneMatch) {
			return ifNoneMatch.toString();
		}
		@RestMethod
		public String ifRange(IfRange ifRange) {
			return ifRange.toString();
		}
		@RestMethod
		public String ifUnmodifiedSince(IfUnmodifiedSince ifUnmodifiedSince) {
			return ifUnmodifiedSince.toString();
		}
		@RestMethod
		public String maxForwards(MaxForwards maxForwards) {
			return maxForwards.toString();
		}
		@RestMethod
		public String pragma(Pragma pragma) {
			return pragma.toString();
		}
		@RestMethod
		public String proxyAuthorization(ProxyAuthorization proxyAuthorization) {
			return proxyAuthorization.toString();
		}
		@RestMethod
		public String range(Range range) {
			return range.toString();
		}
		@RestMethod
		public String referer(Referer referer) {
			return referer.toString();
		}
		@RestMethod
		public String te(TE te) {
			return te.toString();
		}
		@RestMethod
		public String upgrade(Upgrade upgrade) {
			return upgrade.toString();
		}
		@RestMethod
		public String userAgent(UserAgent userAgent) {
			return userAgent.toString();
		}
		@RestMethod
		public String warning(Warning warning) {
			return warning.toString();
		}
	}
	private static MockRest a = MockRest.build(A.class);

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
		a.get("/date").date("Wed, 21 Oct 2015 07:28:00 GMT").execute().assertBody("Wed, 21 Oct 2015 07:28:00 GMT");
	}
	@Test
	public void a10b_date_query() throws Exception {
		a.get("/date?Date=Wed, 21 Oct 2015 07:28:00 GMT").execute().assertBody("Wed, 21 Oct 2015 07:28:00 GMT");
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
		a.get("/ifModifiedSince").ifModifiedSince("Wed, 21 Oct 2015 07:28:00 GMT").execute().assertBody("Wed, 21 Oct 2015 07:28:00 GMT");
	}
	@Test
	public void a15b_ifModifiedSince_query() throws Exception {
		a.get("/ifModifiedSince?If-Modified-Since=Wed, 21 Oct 2015 07:28:00 GMT").execute().assertBody("Wed, 21 Oct 2015 07:28:00 GMT");
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
		a.get("/ifUnmodifiedSince").ifUnmodifiedSince("Wed, 21 Oct 2015 07:28:00 GMT").execute().assertBody("Wed, 21 Oct 2015 07:28:00 GMT");
	}
	@Test
	public void a18b_ifUnmodifiedSince_query() throws Exception {
		a.get("/ifUnmodifiedSince?If-Unmodified-Since=Wed, 21 Oct 2015 07:28:00 GMT").execute().assertBody("Wed, 21 Oct 2015 07:28:00 GMT");
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

	@Rest(
		paramResolvers=CustomHeaderParam.class,
		allowedHeaderParams="Custom"
	)
	public static class B {
		@RestMethod
		public String customHeader(CustomHeader customHeader) {
			return customHeader.toString();
		}
	}
	static MockRest b = MockRest.build(B.class);

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
		b.get("/customHeader").header("Custom", "foo").execute().assertBody("foo");
	}
	@Test
	public void b01b_customHeader_query() throws Exception {
		b.get("/customHeader?Custom=foo").execute().assertBody("foo");
	}

	//====================================================================================================
	// Default values - Default request headers
	//====================================================================================================

	@Rest
	public static class C {
		@RestMethod(reqHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap c(RequestHeaders headers) {
			return OMap.of()
				.a("h1", headers.getString("H1"))
				.a("h2", headers.getString("H2"))
				.a("h3", headers.getString("H3"));
		}
	}
	static MockRest c = MockRest.build(C.class);

	@Test
	public void c01_reqHeaders_default() throws Exception {
		c.get("/c").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void c02_reqHeaders_override() throws Exception {
		c.get("/c").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void c03_reqHeaders_override_caseInsensitive() throws Exception {
		c.get("/c").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Default request headers, case-insensitive matching
	//====================================================================================================

	@Rest
	public static class D {
		@RestMethod(reqHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap d(RequestHeaders headers) {
			return OMap.of()
				.a("h1", headers.getString("h1"))
				.a("h2", headers.getString("h2"))
				.a("h3", headers.getString("h3"));
		}
	}
	static MockRest d = MockRest.build(D.class);

	@Test
	public void d01_reqHeadersCaseInsensitive_default() throws Exception {
		d.get("/d").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void d02_reqHeadersCaseInsensitive_override() throws Exception {
		d.get("/d").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void d03_reqHeadersCaseInsensitive_override_caseInsensitive() throws Exception {
		d.get("/d").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers.
	//====================================================================================================

	@Rest
	public static class E {
		@RestMethod
		public OMap e(@Header(name="H1") String h1, @Header("H2") String h2, @Header("H3") String h3) {
			return OMap.of()
				.a("h1", h1)
				.a("h2", h2)
				.a("h3", h3);
		}
	}
	static MockRest e = MockRest.build(E.class);

	@Test
	public void e01_annotatedHeaders_default() throws Exception {
		e.get("/e").execute().assertBody("{h1:null,h2:null,h3:null}");
	}
	@Test
	public void e02_annotatedHeaders_override() throws Exception {
		e.get("/e").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void e03_annotatedHeaders_override_caseInsensitive() throws Exception {
		e.get("/e").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers, case-insensitive matching.
	//====================================================================================================

	@Rest
	public static class F {
		@RestMethod
		public OMap f(@Header("h1") String h1, @Header("h2") String h2, @Header("h3") String h3) {
			return OMap.of()
				.a("h1", h1)
				.a("h2", h2)
				.a("h3", h3);
		}
	}
	static MockRest f = MockRest.build(F.class);

	@Test
	public void f01_annotatedHeadersCaseInsensitive_default() throws Exception {
		f.get("/f").execute().assertBody("{h1:null,h2:null,h3:null}");
	}
	@Test
	public void f02_annotatedHeadersCaseInsensitive_override() throws Exception {
		f.get("/f").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void f03_annotatedHeadersCaseInsensitive_override_caseInsensitive() throws Exception {
		f.get("/f").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers with default values.
	//====================================================================================================

	@Rest
	public static class G {
		@RestMethod
		public OMap g(@Header(name="h1",_default="1") String h1, @Header(name="h2",_default="2") String h2, @Header(name="h3",_default="3") String h3) {
			return OMap.of()
				.a("h1", h1)
				.a("h2", h2)
				.a("h3", h3);
		}
	}
	static MockRest g = MockRest.build(G.class);

	@Test
	public void g01_annotatedHeadersDefault_default() throws Exception {
		g.get("/g").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void g02_annotatedHeadersDefault_override() throws Exception {
		g.get("/g").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void g03_annotatedHeadersDefault_override_caseInsensitive() throws Exception {
		g.get("/g").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	@Rest
	public static class GB {
		@RestMethod
		public OMap g(@Header(value="h1",_default="1") String h1, @Header(value="h2",_default="2") String h2, @Header(value="h3",_default="3") String h3) {
			return OMap.of()
				.a("h1", h1)
				.a("h2", h2)
				.a("h3", h3);
		}
	}
	static MockRest gb = MockRest.build(GB.class);

	@Test
	public void gb01_annotatedHeadersDefault_default() throws Exception {
		gb.get("/g").execute().assertBody("{h1:'1',h2:'2',h3:'3'}");
	}
	@Test
	public void gb02_annotatedHeadersDefault_override() throws Exception {
		gb.get("/g").header("H1",4).header("H2",5).header("H3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void gb03_annotatedHeadersDefault_override_caseInsensitive() throws Exception {
		gb.get("/g").header("h1",4).header("h2",5).header("h3",6).execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}

	//====================================================================================================
	// Default values - Annotated headers with default values and default request headers.
	//====================================================================================================

	@Rest
	public static class H {
		@RestMethod(reqHeaders={"H1:1","H2=2"," H3 : 3 "})
		public OMap h(@Header(value="h1",_default="4") String h1, @Header(value="h2",_default="5") String h2, @Header(value="h3",_default="6") String h3) {
			return OMap.of()
				.a("h1", h1)
				.a("h2", h2)
				.a("h3", h3);
		}
	}
	static MockRest h = MockRest.build(H.class);

	@Test
	public void h01_annotatedAndDefaultHeaders_default() throws Exception {
		h.get("/h").execute().assertBody("{h1:'4',h2:'5',h3:'6'}");
	}
	@Test
	public void h02_annotatedAndDefaultHeaders_override() throws Exception {
		h.get("/h").header("H1",7).header("H2",8).header("H3",9).execute().assertBody("{h1:'7',h2:'8',h3:'9'}");
	}
	@Test
	public void h03_annotatedAndDefaultHeaders_override_caseInsensitive() throws Exception {
		h.get("/h").header("h1",7).header("h2",8).header("h3",9).execute().assertBody("{h1:'7',h2:'8',h3:'9'}");
	}

	//====================================================================================================
	// Swagger on default headers.
	//====================================================================================================

	Swagger sa = getSwagger(A.class);

	@Test
	public void sa01_accept() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/accept","get","header","Accept");
		assertObjectEquals("{'in':'header',name:'Accept',type:'string'}", pi);
	}
	@Test
	public void sa02_acceptCharset() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/acceptCharset","get","header","Accept-Charset");
		assertObjectEquals("{'in':'header',name:'Accept-Charset',type:'string'}", pi);
	}
	@Test
	public void sa03_acceptEncoding() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/acceptEncoding","get","header","Accept-Encoding");
		assertObjectEquals("{'in':'header',name:'Accept-Encoding',type:'string'}", pi);
	}
	@Test
	public void sa04_acceptLanguage() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/acceptLanguage","get","header","Accept-Language");
		assertObjectEquals("{'in':'header',name:'Accept-Language',type:'string'}", pi);
	}
	@Test
	public void sa05_authorization() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/authorization","get","header","Authorization");
		assertObjectEquals("{'in':'header',name:'Authorization',type:'string'}", pi);
	}
	@Test
	public void sa06_cacheControl() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/cacheControl","get","header","Cache-Control");
		assertObjectEquals("{'in':'header',name:'Cache-Control',type:'string'}", pi);
	}
	@Test
	public void sa07_connection() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/connection","get","header","Connection");
		assertObjectEquals("{'in':'header',name:'Connection',type:'string'}", pi);
	}
	@Test
	public void sa08_contentLength() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/contentLength","get","header","Content-Length");
		assertObjectEquals("{'in':'header',name:'Content-Length',type:'integer',format:'int64'}", pi);
	}
	@Test
	public void sa09_contentType() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/contentType","get","header","Content-Type");
		assertObjectEquals("{'in':'header',name:'Content-Type',type:'string'}", pi);
	}
	@Test
	public void sa10_date() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/date","get","header","Date");
		assertObjectEquals("{'in':'header',name:'Date',type:'string'}", pi);
	}
	@Test
	public void sa11_expect() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/expect","get","header","Expect");
		assertObjectEquals("{'in':'header',name:'Expect',type:'string'}", pi);
	}
	@Test
	public void sa12_() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/from","get","header","From");
		assertObjectEquals("{'in':'header',name:'From',type:'string'}", pi);
	}
	@Test
	public void sa13_host() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/host","get","header","Host");
		assertObjectEquals("{'in':'header',name:'Host',type:'string'}", pi);
	}
	@Test
	public void sa14_ifMatch() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/ifMatch","get","header","If-Match");
		assertObjectEquals("{'in':'header',name:'If-Match',type:'string'}", pi);
	}
	@Test
	public void sa15_ifModifiedSince() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/ifModifiedSince","get","header","If-Modified-Since");
		assertObjectEquals("{'in':'header',name:'If-Modified-Since',type:'string'}", pi);
	}
	@Test
	public void sa16_ifNoneMatch() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/ifNoneMatch","get","header","If-None-Match");
		assertObjectEquals("{'in':'header',name:'If-None-Match',type:'string'}", pi);
	}
	@Test
	public void sa17_ifRange() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/ifRange","get","header","If-Range");
		assertObjectEquals("{'in':'header',name:'If-Range',type:'string'}", pi);
	}
	@Test
	public void sa18_ifUnmodifiedSince() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/ifUnmodifiedSince","get","header","If-Unmodified-Since");
		assertObjectEquals("{'in':'header',name:'If-Unmodified-Since',type:'string'}", pi);
	}
	@Test
	public void sa19_maxForwards() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/maxForwards","get","header","Max-Forwards");
		assertObjectEquals("{'in':'header',name:'Max-Forwards',type:'integer',format:'int32'}", pi);
	}
	@Test
	public void sa20_pragma() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/pragma","get","header","Pragma");
		assertObjectEquals("{'in':'header',name:'Pragma',type:'string'}", pi);
	}
	@Test
	public void sa21_proxyAuthorization() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/proxyAuthorization","get","header","Proxy-Authorization");
		assertObjectEquals("{'in':'header',name:'Proxy-Authorization',type:'string'}", pi);
	}
	@Test
	public void sa22_range() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/range","get","header","Range");
		assertObjectEquals("{'in':'header',name:'Range',type:'string'}", pi);
	}
	@Test
	public void sa23_referer() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/referer","get","header","Referer");
		assertObjectEquals("{'in':'header',name:'Referer',type:'string'}", pi);
	}
	@Test
	public void sa24_te() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/te","get","header","TE");
		assertObjectEquals("{'in':'header',name:'TE',type:'string'}", pi);
	}
	@Test
	public void sa25_upgrade() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/upgrade","get","header","Upgrade");
		assertObjectEquals("{'in':'header',name:'Upgrade',type:'array',items:{type:'string'},collectionFormat:'csv'}", pi);
	}
	@Test
	public void sa26_userAgent() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/userAgent","get","header","User-Agent");
		assertObjectEquals("{'in':'header',name:'User-Agent',type:'string'}", pi);
	}
	@Test
	public void sa27_warning() throws Exception {
		ParameterInfo pi = sa.getParameterInfo("/warning","get","header","Warning");
		assertObjectEquals("{'in':'header',name:'Warning',type:'string'}", pi);
	}
}
