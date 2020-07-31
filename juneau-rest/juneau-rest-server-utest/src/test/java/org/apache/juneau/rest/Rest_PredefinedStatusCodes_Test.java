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

import static org.apache.juneau.http.HttpMethod.*;
import static org.junit.runners.MethodSorters.*;

import java.io.*;

import org.apache.juneau.http.annotation.Body;
import org.apache.juneau.http.annotation.Header;
import org.apache.juneau.http.annotation.Path;
import org.apache.juneau.http.annotation.Query;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_PredefinedStatusCodes_Test {

	//------------------------------------------------------------------------------------------------------------------
	// OK (200)
	//------------------------------------------------------------------------------------------------------------------
	@Rest
	public static class A {
		@RestMethod(name=PUT)
		public Reader a(@Body String b) {
			return new StringReader(b);
		}
	}

	@Test
	public void a01_OK() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		a.put("/a", "foo")
			.run()
			.assertCode().is(200);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Bad Request (400)
	//------------------------------------------------------------------------------------------------------------------

	@Rest(parsers=JsonParser.class)
	public static class B {
		@RestMethod(name=PUT)
		public String a(@Body B1 in) {
			return "OK";
		}
		public static class B1 {
			public String f1;
		}
		@RestMethod(name=PUT)
		public String b(@Body B2 in) {
			return "OK";
		}
		public static class B2 {
			public int f1;
		}
		@RestMethod(name=PUT)
		public String c(@Body B3 in) {
			return "OK";
		}
		public static class B3 {
			public int f1;
			private B3(){}
		}
		@RestMethod(name=PUT)
		public String d(@Body B4 in) {
			return "OK";
		}
		public class B4 {
			public B4(){}
		}
		@RestMethod(name=PUT)
		public String e(@Body B5 in) {
			return "OK";
		}
		static class B5 {
			public B5(){}
		}
		@RestMethod(name=PUT)
		public String f(@Body B6 in) {
			return "OK";
		}
		public static class B6 {
			public int f1;
			private B6(){}
			public static B6 valueOf(String s) {
				throw new RuntimeException("Test error");
			}
		}
		@RestMethod(name=PUT, path="/g/{a1}")
		public String g(@Query("p1") int t1, @Path("a1") int a1, @Header("h1") int h1) {
			return "OK";
		}
	}

	@Test
	public void b01_badRequest() throws Exception {
		RestClient b = MockRestClient.buildLax(B.class);
		b.put("/a?noTrace=true", "{f2:'foo'}", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Unknown property 'f2' encountered while trying to parse into class"
			);
		b.put("/a?noTrace=true", "{f1:'foo', f2:'foo'}", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Unknown property 'f2' encountered while trying to parse into class"
			);
		b.put("/b?noTrace=true", "{f1:'foo'}", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Invalid number"
			);
		b.put("/c?noTrace=true", "{f1:1}", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"could not be instantiated"
			);
		b.put("/d?noTrace=true", "{f1:1}", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"could not be instantiated"
			);
		b.put("/e?noTrace=true", "{f1:1}", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Class is not public"
			);
		b.put("/f?noTrace=true", "'foo'", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Test error"
			);
		b.put("/g/123?noTrace=true&p1=foo", "'foo'", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Could not parse query parameter 'p1'."
			);
		b.put("/g/foo?noTrace=true&p1=1", "'foo'", "application/json")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Could not parse path parameter 'a1'."
			);
		b.put("/g/123?noTrace=true&p1=1", "'foo'", "application/json")
			.header("h1", "foo")
			.run()
			.assertCode().is(400)
			.assertBody().contains(
				"Could not parse header 'h1'."
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Not Found (404) and Method Not Allowed (405)
	//------------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C {
		@RestMethod(name=GET, path="/")
		public String a() {
			return "OK";
		}
	}
	private static MockRestClient c = MockRestClient.buildLax(C.class);

	@Test
	public void c01_badPath() throws Exception {
		c.get("/bad?noTrace=true")
			.run()
			.assertCode().is(404)
			.assertBody().contains(
				"Method 'GET' not found on resource with matching pattern on path '/bad'"
			);
	}
	public void c02_badMethod() throws Exception {
		c.put("?noTrace=true", null)
			.run()
			.assertCode().is(405)
			.assertBody().contains(
				"Method 'PUT' not found on resource."
			);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Precondition Failed (412)
	//------------------------------------------------------------------------------------------------------------------

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
	private static MockRestClient d = MockRestClient.buildLax(D.class);

	@Test
	public void d01() throws Exception {
		d.get("/d?noTrace=true")
			.run()
			.assertCode().is(412)
			.assertBody().contains(
				"Method 'GET' not found on resource on path '/d' with matching matcher."
			);
	}
}
