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

import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_AllowedMethodHeaders_Test {
	//------------------------------------------------------------------------------------------------------------------
	// @Rest(allowedMethodHeaders)
	//------------------------------------------------------------------------------------------------------------------

	public static class A {
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
	public static class A1 extends A {}

	@Rest(allowedMethodHeaders="GET")
	public static class A2 extends A {}

	@Rest(allowedMethodHeaders="get")
	public static class A3 extends A {}

	@Rest(allowedMethodHeaders="FOO")
	public static class A4 extends A {}

	@Rest(allowedMethodHeaders="*")
	public static class A5 extends A {}

	@Rest(allowedMethodHeaders="NONE")
	public static class A6 extends A {}

	@Rest(allowedMethodHeaders="None")
	public static class A7 extends A {}

	@Rest(allowedMethodHeaders="None")
	public static class A8 extends A5 {}

	@Test
	public void c01_basic() throws Exception {
		RestClient a1 = MockRestClient.build(A1.class);
		a1.get("/").run().assertBody().is("GET");
		a1.put("/", "").run().assertBody().is("PUT");
		a1.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		a1.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		a1.request("get","/").header("X-Method","FOO").run().assertBody().is("GET");

		RestClient a2 = MockRestClient.build(A2.class);
		a2.get("/").run().assertBody().is("GET");
		a2.put("/", "").run().assertBody().is("PUT");
		a2.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		a2.put("/", "").header("X-Method", "GET").run().assertBody().is("GET");
		a2.request("get","/").header("X-Method","FOO").run().assertBody().is("GET");

		RestClient a3 = MockRestClient.build(A3.class);
		a3.get("/").run().assertBody().is("GET");
		a3.put("/", "").run().assertBody().is("PUT");
		a3.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		a3.put("/", "").header("X-Method", "GET").run().assertBody().is("GET");
		a3.request("get","/").header("X-Method","FOO").run().assertBody().is("GET");

		RestClient a4 = MockRestClient.build(A4.class);
		a4.get("/").run().assertBody().is("GET");
		a4.put("/", "").run().assertBody().is("PUT");
		a4.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		a4.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		a4.request("get","/").header("X-Method","FOO").run().assertBody().is("FOO");

		RestClient a5 = MockRestClient.build(A5.class);
		a5.get("/").run().assertBody().is("GET");
		a5.put("/", "").run().assertBody().is("PUT");
		a5.get("/").header("X-Method", "PUT").run().assertBody().is("PUT");
		a5.put("/", "").header("X-Method", "GET").run().assertBody().is("GET");
		a5.get("/").header("x-method", "PUT").run().assertBody().is("PUT");
		a5.get("/").header("x-method", "FOO").run().assertBody().is("FOO");
		a5.get("/").header("X-Method", "put").run().assertBody().is("PUT");
		a5.get("/").header("X-Method", "foo").run().assertBody().is("FOO");
		a5.request("get","/").header("X-Method","FOO").run().assertBody().is("FOO");

		RestClient a6 = MockRestClient.build(A6.class);
		a6.get("/").run().assertBody().is("GET");
		a6.put("/", "").run().assertBody().is("PUT");
		a6.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		a6.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		a6.request("get","/").header("X-Method","FOO").run().assertBody().is("GET");

		RestClient a7 = MockRestClient.build(A7.class);
		a7.get("/").run().assertBody().is("GET");
		a7.put("/", "").run().assertBody().is("PUT");
		a7.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		a7.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		a7.request("get","/").header("X-Method","FOO").run().assertBody().is("GET");

		RestClient a8 = MockRestClient.build(A8.class);
		a8.get("/").run().assertBody().is("GET");
		a8.put("/", "").run().assertBody().is("PUT");
		a8.get("/").header("X-Method", "PUT").run().assertBody().is("GET");
		a8.put("/", "").header("X-Method", "GET").run().assertBody().is("PUT");
		a8.request("get","/").header("X-Method","FOO").run().assertBody().is("GET");
	}
}
