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

import org.apache.juneau.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;
import org.junit.runners.*;

/**
 * Tests that validate the behavior of @RestResource.
 */
@SuppressWarnings({"javadoc"})
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class RestResourceTest {

	//====================================================================================================
	// @RestResource(allowBodyParam)
	//====================================================================================================

	@RestResource(allowBodyParam="true")
	public static class A1 {
		@RestMethod
		public ObjectMap put(@Body ObjectMap body) {
			return body;
		}
	}
	@RestResource(allowBodyParam="false")
	public static class A2 {
		@RestMethod
		public ObjectMap put(@Body ObjectMap body) {
			return body;
		}
	}
	@RestResource(allowBodyParam="false")
	public static class A3 extends A1 {}

	@RestResource(allowBodyParam="true")
	public static class A4 extends A2 {}

	static MockRest a1 = MockRest.build(A1.class, null);
	static MockRest a2 = MockRest.build(A2.class, null);
	static MockRest a3 = MockRest.build(A3.class, null);
	static MockRest a4 = MockRest.build(A4.class, null);

	@Test
	public void a01_allowBodyParam_true() throws Exception {
		a1.put("/", "{a:'b'}").execute().assertBody("{a:'b'}");
		a1.put("/?body=(c=d)", "{a:'b'}").execute().assertBody("{c:'d'}");
	}

	@Test
	public void a02_allowBodyParam_false() throws Exception {
		a2.put("/", "{a:'b'}").execute().assertBody("{a:'b'}");
		a2.put("/?body=(c=d)", "{a:'b'}").execute().assertBody("{a:'b'}");
	}

	@Test
	public void a03_allowBodyParam_overridden_false() throws Exception {
		a3.put("/", "{a:'b'}").execute().assertBody("{a:'b'}");
		a3.put("/?body=(c=d)", "{a:'b'}").execute().assertBody("{a:'b'}");
	}

	@Test
	public void a04_allowBodyParam_overridden_true() throws Exception {
		a4.put("/", "{a:'b'}").execute().assertBody("{a:'b'}");
		a4.put("/?body=(c=d)", "{a:'b'}").execute().assertBody("{c:'d'}");
	}

	//====================================================================================================
	// @RestResource(allowedHeaderParams)
	//====================================================================================================

	public static class B {
		@RestMethod
		public String put(RequestHeaders h) {
			return "Accept="+h.getAccept()+",Content-Type=" + h.getContentType() + ",Custom=" + h.getString("Custom");
		}
	}

	@RestResource()
	public static class B1 extends B {}

	@RestResource(allowedHeaderParams="Accept, Content-Type")
	public static class B2 extends B {}

	@RestResource(allowedHeaderParams="ACCEPT, CONTENT-TYPE")
	public static class B3 extends B {}

	@RestResource(allowedHeaderParams="Custom")
	public static class B4 extends B {}

	@RestResource(allowedHeaderParams="*")
	public static class B5 extends B {}

	@RestResource(allowedHeaderParams="NONE")
	public static class B6 extends B {}

	@RestResource(allowedHeaderParams="None")
	public static class B7 extends B {}

	@RestResource(allowedHeaderParams="None")
	public static class B8 extends B5 {}

	static MockRest b1 = MockRest.build(B1.class, null);
	static MockRest b2 = MockRest.build(B2.class, null);
	static MockRest b3 = MockRest.build(B3.class, null);
	static MockRest b4 = MockRest.build(B4.class, null);
	static MockRest b5 = MockRest.build(B5.class, null);
	static MockRest b6 = MockRest.build(B6.class, null);
	static MockRest b7 = MockRest.build(B7.class, null);
	static MockRest b8 = MockRest.build(B8.class, null);

	@Test
	public void b01_allowedHeaderParams_default() throws Exception {
		b1.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b1.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		b1.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
	}

	@Test
	public void b02_allowedHeaderParams_defaultExplicit() throws Exception {
		b2.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b2.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		b2.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
	}

	@Test
	public void b03_allowedHeaderParams_caseSensitivity() throws Exception {
		b3.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b3.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
		b3.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=foo3");
	}

	@Test
	public void b04_allowedHeaderParams_customHeaderOnly() throws Exception {
		b4.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b4.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=bar3");
		b4.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=bar3");
	}

	@Test
	public void b05_allowedHeaderParams_allHeaders() throws Exception {
		b5.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b5.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=bar3");
		b5.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+bar1,Content-Type=text/plain+bar2,Custom=bar3");
	}

	@Test
	public void b06_allowedHeaderParams_none() throws Exception {
		b6.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b6.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b6.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
	}

	@Test
	public void b07_allowedHeaderParams_none_caseSensitivity() throws Exception {
		b7.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b7.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b7.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
	}

	@Test
	public void b08_allowedHeaderParams_none_overridingParent() throws Exception {
		b8.put("/", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b8.put("/?Accept=text/plain%2Bbar1&Content-Type=text/plain%2Bbar2&Custom=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
		b8.put("/?ACCEPT=text/plain%2Bbar1&CONTENT-TYPE=text/plain%2Bbar2&CUSTOM=bar3", "").accept("text/plain+foo1").contentType("text/plain+foo2").header("Custom", "foo3").execute().assertBody("Accept=text/plain+foo1,Content-Type=text/plain+foo2,Custom=foo3");
	}

	//====================================================================================================
	// @RestResource(allowedMethodHeaders)
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

	@RestResource()
	public static class C1 extends C {}

	@RestResource(allowedMethodHeaders="GET")
	public static class C2 extends C {}

	@RestResource(allowedMethodHeaders="get")
	public static class C3 extends C {}

	@RestResource(allowedMethodHeaders="FOO")
	public static class C4 extends C {}

	@RestResource(allowedMethodHeaders="*")
	public static class C5 extends C {}

	@RestResource(allowedMethodHeaders="NONE")
	public static class C6 extends C {}

	@RestResource(allowedMethodHeaders="None")
	public static class C7 extends C {}

	@RestResource(allowedMethodHeaders="None")
	public static class C8 extends C5 {}

	static MockRest c1 = MockRest.build(C1.class, null);
	static MockRest c2 = MockRest.build(C2.class, null);
	static MockRest c3 = MockRest.build(C3.class, null);
	static MockRest c4 = MockRest.build(C4.class, null);
	static MockRest c5 = MockRest.build(C5.class, null);
	static MockRest c6 = MockRest.build(C6.class, null);
	static MockRest c7 = MockRest.build(C7.class, null);
	static MockRest c8 = MockRest.build(C8.class, null);

	@Test
	public void c01_allowedMethodHeaders_default() throws Exception {
		c1.get("/").execute().assertBody("GET");
		c1.put("/", "").execute().assertBody("PUT");
		c1.get("/").header("X-Method", "PUT").execute().assertBody("GET");
		c1.put("/", "").header("X-Method", "GET").execute().assertBody("PUT");
		c1.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("GET");
	}

	@Test
	public void c02_allowedMethodHeaders_GET_only() throws Exception {
		c2.get("/").execute().assertBody("GET");
		c2.put("/", "").execute().assertBody("PUT");
		c2.get("/").header("X-Method", "PUT").execute().assertBody("GET");
		c2.put("/", "").header("X-Method", "GET").execute().assertBody("GET");
		c2.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("GET");
	}

	@Test
	public void c03_allowedMethodHeaders_GET_caseSensitivity() throws Exception {
		c3.get("/").execute().assertBody("GET");
		c3.put("/", "").execute().assertBody("PUT");
		c3.get("/").header("X-Method", "PUT").execute().assertBody("GET");
		c3.put("/", "").header("X-Method", "GET").execute().assertBody("GET");
		c3.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("GET");
	}

	@Test
	public void c04_allowedMethodHeaders_FOO_only() throws Exception {
		c4.get("/").execute().assertBody("GET");
		c4.put("/", "").execute().assertBody("PUT");
		c4.get("/").header("X-Method", "PUT").execute().assertBody("GET");
		c4.put("/", "").header("X-Method", "GET").execute().assertBody("PUT");
		c4.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("FOO");
	}

	@Test
	public void c05_allowedMethodHeaders_allMethods() throws Exception {
		c5.get("/").execute().assertBody("GET");
		c5.put("/", "").execute().assertBody("PUT");
		c5.get("/").header("X-Method", "PUT").execute().assertBody("PUT");
		c5.put("/", "").header("X-Method", "GET").execute().assertBody("GET");
		c5.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("FOO");
	}

	@Test
	public void c06_allowedMethodHeaders_none() throws Exception {
		c6.get("/").execute().assertBody("GET");
		c6.put("/", "").execute().assertBody("PUT");
		c6.get("/").header("X-Method", "PUT").execute().assertBody("GET");
		c6.put("/", "").header("X-Method", "GET").execute().assertBody("PUT");
		c6.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("GET");
	}

	@Test
	public void c07_allowedMethodHeaders_none_caseSensitivity() throws Exception {
		c7.get("/").execute().assertBody("GET");
		c7.put("/", "").execute().assertBody("PUT");
		c7.get("/").header("X-Method", "PUT").execute().assertBody("GET");
		c7.put("/", "").header("X-Method", "GET").execute().assertBody("PUT");
		c7.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("GET");
	}

	@Test
	public void c08_allowedMethodHeaders_none_overridingParent() throws Exception {
		c8.get("/").execute().assertBody("GET");
		c8.put("/", "").execute().assertBody("PUT");
		c8.get("/").header("X-Method", "PUT").execute().assertBody("GET");
		c8.put("/", "").header("X-Method", "GET").execute().assertBody("PUT");
		c8.request("GET","/",null,"").header("X-Method","FOO").execute().assertBody("GET");
	}

	@Test
	public void c09_allowedMethodHeaders_caseInsensitiveHeaderName() throws Exception {
		c5.get("/").header("x-method", "PUT").execute().assertBody("PUT");
		c5.get("/").header("x-method", "FOO").execute().assertBody("FOO");
	}

	@Test
	public void c10_allowedMethodHeaders_caseInsensitiveHeaderValue() throws Exception {
		c5.get("/").header("X-Method", "put").execute().assertBody("PUT");
		c5.get("/").header("X-Method", "foo").execute().assertBody("FOO");
	}

	//====================================================================================================
	// @RestResource(allowedMethodParams)
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

	@RestResource()
	public static class D1 extends D {}

	@RestResource(allowedMethodParams="GET")
	public static class D2 extends D {}

	@RestResource(allowedMethodParams="get")
	public static class D3 extends D {}

	@RestResource(allowedMethodParams="FOO")
	public static class D4 extends D {}

	@RestResource(allowedMethodParams="*")
	public static class D5 extends D {}

	@RestResource(allowedMethodParams="NONE")
	public static class D6 extends D {}

	@RestResource(allowedMethodParams="None")
	public static class D7 extends D {}

	@RestResource(allowedMethodParams="None")
	public static class D8 extends D5 {}

	static MockRest d1 = MockRest.build(D1.class, null);
	static MockRest d2 = MockRest.build(D2.class, null);
	static MockRest d3 = MockRest.build(D3.class, null);
	static MockRest d4 = MockRest.build(D4.class, null);
	static MockRest d5 = MockRest.build(D5.class, null);
	static MockRest d6 = MockRest.build(D6.class, null);
	static MockRest d7 = MockRest.build(D7.class, null);
	static MockRest d8 = MockRest.build(D8.class, null);

	@Test
	public void d01_allowedMethodHeaders_default() throws Exception {
		d1.get("/").execute().assertBody("GET");
		d1.put("/", "").execute().assertBody("PUT");
		d1.head("/").execute().assertBody("HEAD");
		d1.options("/").execute().assertBody("OPTIONS");
		d1.get("/?method=PUT").execute().assertBody("GET");
		d1.put("/?method=GET", "").execute().assertBody("PUT");
		d1.get("/?method=HEAD").execute().assertBody("HEAD");
		d1.get("/?method=OPTIONS").execute().assertBody("OPTIONS");
		d1.request("GET","/?method=FOO",null,"").execute().assertBody("GET");
	}

	@Test
	public void d02_allowedMethodParams_GET_only() throws Exception {
		d2.get("/").execute().assertBody("GET");
		d2.put("/", "").execute().assertBody("PUT");
		d2.head("/").execute().assertBody("HEAD");
		d2.options("/").execute().assertBody("OPTIONS");
		d2.get("/?method=PUT").execute().assertBody("GET");
		d2.put("/?method=GET", "").execute().assertBody("GET");
		d2.get("/?method=HEAD").execute().assertBody("GET");
		d2.get("/?method=OPTIONS").execute().assertBody("GET");
		d2.request("GET","/?method=FOO",null,"").execute().assertBody("GET");
	}

	@Test
	public void d03_allowedMethodParams_GET_caseSensitivity() throws Exception {
		d3.get("/").execute().assertBody("GET");
		d3.put("/", "").execute().assertBody("PUT");
		d3.head("/").execute().assertBody("HEAD");
		d3.options("/").execute().assertBody("OPTIONS");
		d3.get("/?method=PUT").execute().assertBody("GET");
		d3.put("/?method=GET", "").execute().assertBody("GET");
		d3.get("/?method=HEAD").execute().assertBody("GET");
		d3.get("/?method=OPTIONS").execute().assertBody("GET");
		d3.request("GET","/?method=FOO",null,"").execute().assertBody("GET");
	}

	@Test
	public void d04_allowedMethodParams_FOO_only() throws Exception {
		d4.get("/").execute().assertBody("GET");
		d4.put("/", "").execute().assertBody("PUT");
		d4.head("/").execute().assertBody("HEAD");
		d4.options("/").execute().assertBody("OPTIONS");
		d4.get("/?method=PUT").execute().assertBody("GET");
		d4.put("/?method=GET", "").execute().assertBody("PUT");
		d4.get("/?method=HEAD").execute().assertBody("GET");
		d4.get("/?method=OPTIONS").execute().assertBody("GET");
		d4.request("GET","/?method=FOO",null,"").execute().assertBody("FOO");
	}

	@Test
	public void d05_allowedMethodParams_allMethods() throws Exception {
		d5.get("/").execute().assertBody("GET");
		d5.put("/", "").execute().assertBody("PUT");
		d5.head("/").execute().assertBody("HEAD");
		d5.options("/").execute().assertBody("OPTIONS");
		d5.get("/?method=PUT").execute().assertBody("PUT");
		d5.put("/?method=GET", "").execute().assertBody("GET");
		d5.get("/?method=HEAD").execute().assertBody("HEAD");
		d5.get("/?method=OPTIONS").execute().assertBody("OPTIONS");
		d5.request("GET","/?method=FOO",null,"").execute().assertBody("FOO");
	}

	@Test
	public void d06_allowedMethodParams_none() throws Exception {
		d6.get("/").execute().assertBody("GET");
		d6.put("/", "").execute().assertBody("PUT");
		d6.head("/").execute().assertBody("HEAD");
		d6.options("/").execute().assertBody("OPTIONS");
		d6.get("/?method=PUT").execute().assertBody("GET");
		d6.put("/?method=GET", "").execute().assertBody("PUT");
		d6.get("/?method=HEAD").execute().assertBody("GET");
		d6.get("/?method=OPTIONS").execute().assertBody("GET");
		d6.request("GET","/?method=FOO",null,"").execute().assertBody("GET");
	}

	@Test
	public void d07_allowedMethodParams_none_caseSensitivity() throws Exception {
		d7.get("/").execute().assertBody("GET");
		d7.put("/", "").execute().assertBody("PUT");
		d7.head("/").execute().assertBody("HEAD");
		d7.options("/").execute().assertBody("OPTIONS");
		d7.get("/?method=PUT").execute().assertBody("GET");
		d7.put("/?method=GET", "").execute().assertBody("PUT");
		d7.get("/?method=HEAD").execute().assertBody("GET");
		d7.get("/?method=OPTIONS").execute().assertBody("GET");
		d7.request("GET","/?method=FOO",null,"").execute().assertBody("GET");
	}

	@Test
	public void d08_allowedMethodParams_none_overridingParent() throws Exception {
		d8.get("/").execute().assertBody("GET");
		d8.put("/", "").execute().assertBody("PUT");
		d8.head("/").execute().assertBody("HEAD");
		d8.options("/").execute().assertBody("OPTIONS");
		d8.get("/?method=PUT").execute().assertBody("GET");
		d8.put("/?method=GET", "").execute().assertBody("PUT");
		d8.get("/?method=HEAD").execute().assertBody("GET");
		d8.get("/?method=OPTIONS").execute().assertBody("GET");
		d8.request("GET","/?method=FOO",null,"").execute().assertBody("GET");
	}

	@Test
	public void d09_allowedMethodHeaders_caseInsensitiveParamValue() throws Exception {
		d5.get("/?method=Put").execute().assertBody("PUT");
		d5.get("/?method=Foo").execute().assertBody("FOO");
	}
}
