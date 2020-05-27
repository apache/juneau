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
package org.apache.juneau.rest.annotation;

import static org.junit.runners.MethodSorters.*;
import static org.apache.juneau.http.HttpMethod.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestResourceTest {

	//====================================================================================================
	// @Rest(allowBodyParam)
	//====================================================================================================

	@Rest(allowBodyParam="true")
	public static class A1 {
		@RestMethod
		public OMap put(@Body OMap body) {
			return body;
		}
	}
	@Rest(allowBodyParam="false")
	public static class A2 {
		@RestMethod
		public OMap put(@Body OMap body) {
			return body;
		}
	}
	@Rest(allowBodyParam="false")
	public static class A3 extends A1 {}

	@Rest(allowBodyParam="true")
	public static class A4 extends A2 {}

	static MockRestClient a1 = MockRestClient.build(A1.class);
	static MockRestClient a2 = MockRestClient.build(A2.class);
	static MockRestClient a3 = MockRestClient.build(A3.class);
	static MockRestClient a4 = MockRestClient.build(A4.class);

	@Test
	public void a01_allowBodyParam_true() throws Exception {
		a1.put("/", "{a:'b'}").run().assertBody().is("{a:'b'}");
		a1.put("/?body=(c=d)", "{a:'b'}").run().assertBody().is("{c:'d'}");
	}

	@Test
	public void a02_allowBodyParam_false() throws Exception {
		a2.put("/", "{a:'b'}").run().assertBody().is("{a:'b'}");
		a2.put("/?body=(c=d)", "{a:'b'}").run().assertBody().is("{a:'b'}");
	}

	@Test
	public void a03_allowBodyParam_overridden_false() throws Exception {
		a3.put("/", "{a:'b'}").run().assertBody().is("{a:'b'}");
		a3.put("/?body=(c=d)", "{a:'b'}").run().assertBody().is("{a:'b'}");
	}

	@Test
	public void a04_allowBodyParam_overridden_true() throws Exception {
		a4.put("/", "{a:'b'}").run().assertBody().is("{a:'b'}");
		a4.put("/?body=(c=d)", "{a:'b'}").run().assertBody().is("{c:'d'}");
	}

	//====================================================================================================
	// @Rest(allowedHeaderParams)
	//====================================================================================================

	public static class B {
		@RestMethod
		public String put(RequestHeaders h) {
			return "Accept="+h.getAccept()+",Content-Type=" + h.getContentType() + ",Custom=" + h.getString("Custom");
		}
	}

	@Rest()
	public static class B1 extends B {}

	@Rest(allowedHeaderParams="Accept, Content-Type")
	public static class B2 extends B {}

	@Rest(allowedHeaderParams="ACCEPT, CONTENT-TYPE")
	public static class B3 extends B {}

	@Rest(allowedHeaderParams="Custom")
	public static class B4 extends B {}

	@Rest(allowedHeaderParams="*")
	public static class B5 extends B {}

	@Rest(allowedHeaderParams="NONE")
	public static class B6 extends B {}

	@Rest(allowedHeaderParams="None")
	public static class B7 extends B {}

	@Rest(allowedHeaderParams="None")
	public static class B8 extends B5 {}

	static MockRestClient b1 = MockRestClient.build(B1.class);
	static MockRestClient b2 = MockRestClient.build(B2.class);
	static MockRestClient b3 = MockRestClient.build(B3.class);
	static MockRestClient b4 = MockRestClient.build(B4.class);
	static MockRestClient b5 = MockRestClient.build(B5.class);
	static MockRestClient b6 = MockRestClient.build(B6.class);
	static MockRestClient b7 = MockRestClient.build(B7.class);
	static MockRestClient b8 = MockRestClient.build(B8.class);

	@Test
	public void b01_allowedHeaderParams_default() throws Exception {
		b1.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b1.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		b1.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
	}

	@Test
	public void b02_allowedHeaderParams_defaultExplicit() throws Exception {
		b2.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b2.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		b2.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
	}

	@Test
	public void b03_allowedHeaderParams_caseSensitivity() throws Exception {
		b3.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b3.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		b3.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
	}

	@Test
	public void b04_allowedHeaderParams_customHeaderOnly() throws Exception {
		b4.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b4.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=bar3");
		b4.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=bar3");
	}

	@Test
	public void b05_allowedHeaderParams_allHeaders() throws Exception {
		b5.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b5.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=bar3");
		b5.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=bar3");
	}

	@Test
	public void b06_allowedHeaderParams_none() throws Exception {
		b6.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b6.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b6.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
	}

	@Test
	public void b07_allowedHeaderParams_none_caseSensitivity() throws Exception {
		b7.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b7.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b7.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
	}

	@Test
	public void b08_allowedHeaderParams_none_overridingParent() throws Exception {
		b8.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b8.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b8.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").run().assertBody().is("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
	}

	//====================================================================================================
	// @Rest(allowedMethodHeaders)
	//====================================================================================================

	public static class C {
		@RestMethod
		public String get() {
			return "GET";
		}
		@RestMethod
		public String put() {
			return "PUT";
		}
		@RestMethod(name="foo",path="/")
		public String foo() {
			return "FOO";
		}
	}

	@Rest()
	public static class C1 extends C {}

	@Rest(allowedMethodHeaders="GET")
	public static class C2 extends C {}

	@Rest(allowedMethodHeaders="get")
	public static class C3 extends C {}

	@Rest(allowedMethodHeaders="FOO")
	public static class C4 extends C {}

	@Rest(allowedMethodHeaders="*")
	public static class C5 extends C {}

	@Rest(allowedMethodHeaders="NONE")
	public static class C6 extends C {}

	@Rest(allowedMethodHeaders="None")
	public static class C7 extends C {}

	@Rest(allowedMethodHeaders="None")
	public static class C8 extends C5 {}

	static MockRestClient c1 = MockRestClient.build(C1.class);
	static MockRestClient c2 = MockRestClient.build(C2.class);
	static MockRestClient c3 = MockRestClient.build(C3.class);
	static MockRestClient c4 = MockRestClient.build(C4.class);
	static MockRestClient c5 = MockRestClient.build(C5.class);
	static MockRestClient c6 = MockRestClient.build(C6.class);
	static MockRestClient c7 = MockRestClient.build(C7.class);
	static MockRestClient c8 = MockRestClient.build(C8.class);

	@Test
	public void c01_allowedMethodHeaders_default() throws Exception {
		c1.get("/").run().assertBody().is("GET");
		c1.put("/", "").run().assertBody().is("PUT");
		c1.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		c1.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		c1.request(GET,"/").header("X-Method","FOO").run().assertBody().is("GET");
	}

	@Test
	public void c02_allowedMethodHeaders_GET_only() throws Exception {
		c2.get("/").run().assertBody().is("GET");
		c2.put("/", "").run().assertBody().is("PUT");
		c2.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		c2.put("/", "").header("X-Method", "GET").run().assertBody().is("GET");
		c2.request(GET,"/").header("X-Method","FOO").run().assertBody().is("GET");
	}

	@Test
	public void c03_allowedMethodHeaders_GET_caseSensitivity() throws Exception {
		c3.get("/").run().assertBody().is("GET");
		c3.put("/", "").run().assertBody().is("PUT");
		c3.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		c3.put("/", "").header("X-Method", "GET").run().assertBody().is("GET");
		c3.request(GET,"/").header("X-Method","FOO").run().assertBody().is("GET");
	}

	@Test
	public void c04_allowedMethodHeaders_FOO_only() throws Exception {
		c4.get("/").run().assertBody().is("GET");
		c4.put("/", "").run().assertBody().is("PUT");
		c4.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		c4.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		c4.request(GET,"/").header("X-Method","FOO").run().assertBody().is("FOO");
	}

	@Test
	public void c05_allowedMethodHeaders_allMethods() throws Exception {
		c5.get("/").run().assertBody().is("GET");
		c5.put("/", "").run().assertBody().is("PUT");
		c5.get("/").header("X-Method", "PUT").run().assertBody().is("PUT");
		c5.put("/", "").header("X-Method", "GET").run().assertBody().is("GET");
		c5.request(GET,"/").header("X-Method","FOO").run().assertBody().is("FOO");
	}

	@Test
	public void c06_allowedMethodHeaders_none() throws Exception {
		c6.get("/").run().assertBody().is("GET");
		c6.put("/", "").run().assertBody().is("PUT");
		c6.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		c6.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		c6.request(GET,"/").header("X-Method","FOO").run().assertBody().is("GET");
	}

	@Test
	public void c07_allowedMethodHeaders_none_caseSensitivity() throws Exception {
		c7.get("/").run().assertBody().is("GET");
		c7.put("/", "").run().assertBody().is("PUT");
		c7.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		c7.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		c7.request(GET,"/").header("X-Method","FOO").run().assertBody().is("GET");
	}

	@Test
	public void c08_allowedMethodHeaders_none_overridingParent() throws Exception {
		c8.get("/").run().assertBody().is("GET");
		c8.put("/", "").run().assertBody().is("PUT");
		c8.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		c8.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		c8.request(GET,"/").header("X-Method","FOO").run().assertBody().is("GET");
	}

	@Test
	public void c09_allowedMethodHeaders_caseInsensitiveHeaderName() throws Exception {
		c5.get("/").header("x-method", "PUT").run().assertBody().is("PUT");
		c5.get("/").header("x-method", "FOO").run().assertBody().is("FOO");
	}

	@Test
	public void c10_allowedMethodHeaders_caseInsensitiveHeaderValue() throws Exception {
		c5.get("/").header("X-Method", "put").run().assertBody().is("PUT");
		c5.get("/").header("X-Method", "foo").run().assertBody().is("FOO");
	}

	//====================================================================================================
	// @Rest(allowedMethodParams)
	//====================================================================================================

	public static class D {
		@RestMethod
		public String get() {
			return "GET";
		}
		@RestMethod
		public String put() {
			return "PUT";
		}
		@RestMethod
		public String head() {
			// Note that HTTP client is going to ignore this body.
			return "HEAD";
		}
		@RestMethod
		public String options() {
			return "OPTIONS";
		}
		@RestMethod(name="foo",path="/")
		public String foo() {
			return "FOO";
		}
	}

	@Rest()
	public static class D1 extends D {}

	@Rest(allowedMethodParams="GET")
	public static class D2 extends D {}

	@Rest(allowedMethodParams="get")
	public static class D3 extends D {}

	@Rest(allowedMethodParams="FOO")
	public static class D4 extends D {}

	@Rest(allowedMethodParams="*")
	public static class D5 extends D {}

	@Rest(allowedMethodParams="NONE")
	public static class D6 extends D {}

	@Rest(allowedMethodParams="None")
	public static class D7 extends D {}

	@Rest(allowedMethodParams="None")
	public static class D8 extends D5 {}

	static MockRestClient d1 = MockRestClient.build(D1.class);
	static MockRestClient d2 = MockRestClient.build(D2.class);
	static MockRestClient d3 = MockRestClient.build(D3.class);
	static MockRestClient d4 = MockRestClient.build(D4.class);
	static MockRestClient d5 = MockRestClient.build(D5.class);
	static MockRestClient d6 = MockRestClient.build(D6.class);
	static MockRestClient d7 = MockRestClient.build(D7.class);
	static MockRestClient d8 = MockRestClient.build(D8.class);

	@Test
	public void d01_allowedMethodHeaders_default() throws Exception {
		d1.get("/").run().assertBody().is("GET");
		d1.put("/", "").run().assertBody().is("PUT");
		d1.head("/").run().assertBody().is("");
		d1.options("/").run().assertBody().is("OPTIONS");
		d1.get("/?method=PUT").run().assertBody().is("GET");
		d1.put("/?method=GET", "").run().assertBody().is("PUT");
		d1.get("/?method=HEAD").run().assertBody().is("HEAD");
		d1.get("/?method=OPTIONS").run().assertBody().is("OPTIONS");
		d1.request(GET,"/?method=FOO").run().assertBody().is("GET");
	}

	@Test
	public void d02_allowedMethodParams_GET_only() throws Exception {
		d2.get("/").run().assertBody().is("GET");
		d2.put("/", "").run().assertBody().is("PUT");
		d2.head("/").run().assertBody().is("");
		d2.options("/").run().assertBody().is("OPTIONS");
		d2.get("/?method=PUT").run().assertBody().is("GET");
		d2.put("/?method=GET", "").run().assertBody().is("GET");
		d2.get("/?method=HEAD").run().assertBody().is("GET");
		d2.get("/?method=OPTIONS").run().assertBody().is("GET");
		d2.request(GET,"/?method=FOO").run().assertBody().is("GET");
	}

	@Test
	public void d03_allowedMethodParams_GET_caseSensitivity() throws Exception {
		d3.get("/").run().assertBody().is("GET");
		d3.put("/", "").run().assertBody().is("PUT");
		d3.head("/").run().assertBody().is("");
		d3.options("/").run().assertBody().is("OPTIONS");
		d3.get("/?method=PUT").run().assertBody().is("GET");
		d3.put("/?method=GET", "").run().assertBody().is("GET");
		d3.get("/?method=HEAD").run().assertBody().is("GET");
		d3.get("/?method=OPTIONS").run().assertBody().is("GET");
		d3.request(GET,"/?method=FOO").run().assertBody().is("GET");
	}

	@Test
	public void d04_allowedMethodParams_FOO_only() throws Exception {
		d4.get("/").run().assertBody().is("GET");
		d4.put("/", "").run().assertBody().is("PUT");
		d4.head("/").run().assertBody().is("");
		d4.options("/").run().assertBody().is("OPTIONS");
		d4.get("/?method=PUT").run().assertBody().is("GET");
		d4.put("/?method=GET", "").run().assertBody().is("PUT");
		d4.get("/?method=HEAD").run().assertBody().is("GET");
		d4.get("/?method=OPTIONS").run().assertBody().is("GET");
		d4.request(GET,"/?method=FOO").run().assertBody().is("FOO");
	}

	@Test
	public void d05_allowedMethodParams_allMethods() throws Exception {
		d5.get("/").run().assertBody().is("GET");
		d5.put("/", "").run().assertBody().is("PUT");
		d5.head("/").run().assertBody().is("");
		d5.options("/").run().assertBody().is("OPTIONS");
		d5.get("/?method=PUT").run().assertBody().is("PUT");
		d5.put("/?method=GET", "").run().assertBody().is("GET");
		d5.get("/?method=HEAD").run().assertBody().is("HEAD");
		d5.get("/?method=OPTIONS").run().assertBody().is("OPTIONS");
		d5.request(GET,"/?method=FOO").run().assertBody().is("FOO");
	}

	@Test
	public void d06_allowedMethodParams_none() throws Exception {
		d6.get("/").run().assertBody().is("GET");
		d6.put("/", "").run().assertBody().is("PUT");
		d6.head("/").run().assertBody().is("");
		d6.options("/").run().assertBody().is("OPTIONS");
		d6.get("/?method=PUT").run().assertBody().is("GET");
		d6.put("/?method=GET", "").run().assertBody().is("PUT");
		d6.get("/?method=HEAD").run().assertBody().is("GET");
		d6.get("/?method=OPTIONS").run().assertBody().is("GET");
		d6.request(GET,"/?method=FOO").run().assertBody().is("GET");
	}

	@Test
	public void d07_allowedMethodParams_none_caseSensitivity() throws Exception {
		d7.get("/").run().assertBody().is("GET");
		d7.put("/", "").run().assertBody().is("PUT");
		d7.head("/").run().assertBody().is("");
		d7.options("/").run().assertBody().is("OPTIONS");
		d7.get("/?method=PUT").run().assertBody().is("GET");
		d7.put("/?method=GET", "").run().assertBody().is("PUT");
		d7.get("/?method=HEAD").run().assertBody().is("GET");
		d7.get("/?method=OPTIONS").run().assertBody().is("GET");
		d7.request(GET,"/?method=FOO").run().assertBody().is("GET");
	}

	@Test
	public void d08_allowedMethodParams_none_overridingParent() throws Exception {
		d8.get("/").run().assertBody().is("GET");
		d8.put("/", "").run().assertBody().is("PUT");
		d8.head("/").run().assertBody().is("");
		d8.options("/").run().assertBody().is("OPTIONS");
		d8.get("/?method=PUT").run().assertBody().is("GET");
		d8.put("/?method=GET", "").run().assertBody().is("PUT");
		d8.get("/?method=HEAD").run().assertBody().is("GET");
		d8.get("/?method=OPTIONS").run().assertBody().is("GET");
		d8.request(GET,"/?method=FOO").run().assertBody().is("GET");
	}

	@Test
	public void d09_allowedMethodHeaders_caseInsensitiveParamValue() throws Exception {
		d5.get("/?method=Put").run().assertBody().is("PUT");
		d5.get("/?method=Foo").run().assertBody().is("FOO");
	}
}
