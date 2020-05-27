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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class StatusCodesTest {

	//=================================================================================================================
	// OK (200)
	//=================================================================================================================
	@Rest
	public static class A {
		@RestMethod(name=PUT)
		public Reader a01(@Body String b) {
			return new StringReader(b);
		}
	}
	private static MockRest a = MockRest.build(A.class);

	@Test
	public void a01a_OK() throws Exception {
		a.put("/a01", "foo")
			.run()
			.assertStatusCode().is(200);
	}

	//=================================================================================================================
	// Bad Request (400)
	//=================================================================================================================

	@Rest(parsers=JsonParser.class)
	public static class B {
		@RestMethod(name=PUT, path="/nonExistentBeanProperties")
		public String b01(@Body B01 in) {
			return "OK";
		}
		public static class B01 {
			public String f1;
		}
		@RestMethod(name=PUT, path="/wrongDataType")
		public String b02(@Body B02 in) {
			return "OK";
		}
		public static class B02 {
			public int f1;
		}
		@RestMethod(name=PUT, path="/parseIntoNonConstructableBean")
		public String b03(@Body B03 in) {
			return "OK";
		}
		public static class B03 {
			public int f1;
			private B03(){}
		}
		@RestMethod(name=PUT, path="/parseIntoNonStaticInnerClass")
		public String b04(@Body B04 in) {
			return "OK";
		}
		public class B04 {
			public B04(){}
		}
		@RestMethod(name=PUT, path="/parseIntoNonPublicInnerClass")
		public String b05(@Body B05 in) {
			return "OK";
		}
		static class B05 {
			public B05(){}
		}
		@RestMethod(name=PUT, path="/thrownConstructorException")
		public String b06(@Body B06 in) {
			return "OK";
		}
		public static class B06 {
			public int f1;
			private B06(){}
			public static B06 valueOf(String s) {
				throw new RuntimeException("Test error");
			}
		}
		@RestMethod(name=PUT, path="/setParameterToInvalidTypes/{a1}")
		public String b07(@Query("p1") int t1, @Path("a1") int a1, @Header("h1") int h1) {
			return "OK";
		}
	}
	private static MockRest b = MockRest.build(B.class);

	@Test
	public void b01a_nonExistentBeanProperties() throws Exception {
		b.put("/nonExistentBeanProperties?noTrace=true", "{f2:'foo'}")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Unknown property 'f2' encountered while trying to parse into class 'org.apache.juneau.rest.StatusCodesTest$B$B01'"
			);
	}
	@Test
	public void b01b_nonExistentBeanProperties() throws Exception {
		b.put("/nonExistentBeanProperties?noTrace=true", "{f1:'foo', f2:'foo'}")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Unknown property 'f2' encountered while trying to parse into class 'org.apache.juneau.rest.StatusCodesTest$B$B01'"
			);
	}
	@Test
	public void b02_wrongDataType() throws Exception {
		b.put("/wrongDataType?noTrace=true", "{f1:'foo'}")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Invalid number"
			);
	}
	@Test
	public void b03_parseIntoNonConstructableBean() throws Exception {
		b.put("/parseIntoNonConstructableBean?noTrace=true", "{f1:1}")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"could not be instantiated"
			);
	}
	@Test
	public void b04_parseIntoNonStaticInnerClass() throws Exception {
		b.put("/parseIntoNonStaticInnerClass?noTrace=true", "{f1:1}")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"could not be instantiated"
			);
	}
	@Test
	public void b05_parseIntoNonStaticInnerClass() throws Exception {
		b.put("/parseIntoNonPublicInnerClass?noTrace=true", "{f1:1}")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Class is not public"
			);
	}
	@Test
	public void b06_thrownConstructorException() throws Exception {
		b.put("/thrownConstructorException?noTrace=true", "'foo'")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Test error"
			);
	}
	@Test
	public void b07a_setParameterToInvalidTypes_Query() throws Exception {
		b.put("/setParameterToInvalidTypes/123?noTrace=true&p1=foo", "'foo'")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Could not parse query parameter 'p1'."
			);
	}
	@Test
	public void b07a_setParameterToInvalidTypes_Path() throws Exception {
		b.put("/setParameterToInvalidTypes/foo?noTrace=true&p1=1", "'foo'")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Could not parse path parameter 'a1'."
			);
	}
	@Test
	public void b07a_setParameterToInvalidTypes_Header() throws Exception {
		b.put("/setParameterToInvalidTypes/123?noTrace=true&p1=1", "'foo'")
			.header("h1", "foo")
			.json()
			.run()
			.assertStatusCode().is(400)
			.assertBody().contains(
				"Could not parse header 'h1'."
			);
	}

	//=================================================================================================================
	// Not Found (404) and Method Not Allowed (405)
	//=================================================================================================================

	@Rest
	public static class C {
		@RestMethod(name=GET, path="/")
		public String c01() {
			return "OK";
		}
	}
	private static MockRest c = MockRest.build(C.class);

	@Test
	public void c01_badPath() throws Exception {
		c.get("/bad?noTrace=true")
			.run()
			.assertStatusCode().is(404)
			.assertBody().contains(
				"Method 'GET' not found on resource with matching pattern on path '/bad'"
			);
	}
	public void c02_badMethod() throws Exception {
		c.put("?noTrace=true", null)
			.run()
			.assertStatusCode().is(405)
			.assertBody().contains(
				"Method 'PUT' not found on resource."
			);
	}

	//=================================================================================================================
	// Precondition Failed (412)
	//=================================================================================================================

	@Rest
	public static class D {
		@RestMethod(name=GET, matchers=NeverMatcher.class)
		public String d() {
			return "OK";
		}
		public static class NeverMatcher extends RestMatcher {
			@Override /* RestMatcher */
			public boolean matches(RestRequest req) {
				return false;
			}
		}
	}
	private static MockRest d = MockRest.build(D.class);

	@Test
	public void d01() throws Exception {
		d.get("/d?noTrace=true")
			.run()
			.assertStatusCode().is(412)
			.assertBody().contains(
				"Method 'GET' not found on resource on path '/d' with matching matcher."
			);
	}
}
