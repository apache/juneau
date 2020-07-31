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

import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Rest_AllowedMethodParams_Test {

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(allowedMethodParams)
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
	public static class A1 extends A {}

	@Rest(allowedMethodParams="GET")
	public static class A2 extends A {}

	@Rest(allowedMethodParams="get")
	public static class A3 extends A {}

	@Rest(allowedMethodParams="FOO")
	public static class A4 extends A {}

	@Rest(allowedMethodParams="*")
	public static class A5 extends A {}

	@Rest(allowedMethodParams="NONE")
	public static class A6 extends A {}

	@Rest(allowedMethodParams="None")
	public static class A7 extends A {}

	@Rest(allowedMethodParams="None")
	public static class A8 extends A5 {}

	@Test
	public void a01_basic() throws Exception {
		RestClient a1 = MockRestClient.build(A1.class);
		a1.get("/").run().assertBody().is("GET");
		a1.put("/", "").run().assertBody().is("PUT");
		a1.head("/").run().assertBody().is("");
		a1.options("/").run().assertBody().is("OPTIONS");
		a1.get("/?method=PUT").run().assertBody().is("GET");
		a1.put("/?method=GET", "").run().assertBody().is("PUT");
		a1.get("/?method=HEAD").run().assertBody().is("HEAD");
		a1.get("/?method=OPTIONS").run().assertBody().is("OPTIONS");
		a1.request("get","/?method=FOO").run().assertBody().is("GET");

		RestClient a2 = MockRestClient.build(A2.class);
		a2.get("/").run().assertBody().is("GET");
		a2.put("/", "").run().assertBody().is("PUT");
		a2.head("/").run().assertBody().is("");
		a2.options("/").run().assertBody().is("OPTIONS");
		a2.get("/?method=PUT").run().assertBody().is("GET");
		a2.put("/?method=GET", "").run().assertBody().is("GET");
		a2.get("/?method=HEAD").run().assertBody().is("GET");
		a2.get("/?method=OPTIONS").run().assertBody().is("GET");
		a2.request("get","/?method=FOO").run().assertBody().is("GET");

		RestClient a3 = MockRestClient.build(A3.class);
		a3.get("/").run().assertBody().is("GET");
		a3.put("/", "").run().assertBody().is("PUT");
		a3.head("/").run().assertBody().is("");
		a3.options("/").run().assertBody().is("OPTIONS");
		a3.get("/?method=PUT").run().assertBody().is("GET");
		a3.put("/?method=GET", "").run().assertBody().is("GET");
		a3.get("/?method=HEAD").run().assertBody().is("GET");
		a3.get("/?method=OPTIONS").run().assertBody().is("GET");
		a3.request("get","/?method=FOO").run().assertBody().is("GET");

		RestClient a4 = MockRestClient.build(A4.class);
		a4.get("/").run().assertBody().is("GET");
		a4.put("/", "").run().assertBody().is("PUT");
		a4.head("/").run().assertBody().is("");
		a4.options("/").run().assertBody().is("OPTIONS");
		a4.get("/?method=PUT").run().assertBody().is("GET");
		a4.put("/?method=GET", "").run().assertBody().is("PUT");
		a4.get("/?method=HEAD").run().assertBody().is("GET");
		a4.get("/?method=OPTIONS").run().assertBody().is("GET");
		a4.request("get","/?method=FOO").run().assertBody().is("FOO");

		RestClient a5 = MockRestClient.build(A5.class);
		a5.get("/").run().assertBody().is("GET");
		a5.put("/", "").run().assertBody().is("PUT");
		a5.head("/").run().assertBody().is("");
		a5.options("/").run().assertBody().is("OPTIONS");
		a5.get("/?method=PUT").run().assertBody().is("PUT");
		a5.put("/?method=GET", "").run().assertBody().is("GET");
		a5.get("/?method=HEAD").run().assertBody().is("HEAD");
		a5.get("/?method=OPTIONS").run().assertBody().is("OPTIONS");
		a5.request("get","/?method=FOO").run().assertBody().is("FOO");
		a5.get("/?method=Put").run().assertBody().is("PUT");
		a5.get("/?method=Foo").run().assertBody().is("FOO");

		RestClient a6 = MockRestClient.build(A6.class);
		a6.get("/").run().assertBody().is("GET");
		a6.put("/", "").run().assertBody().is("PUT");
		a6.head("/").run().assertBody().is("");
		a6.options("/").run().assertBody().is("OPTIONS");
		a6.get("/?method=PUT").run().assertBody().is("GET");
		a6.put("/?method=GET", "").run().assertBody().is("PUT");
		a6.get("/?method=HEAD").run().assertBody().is("GET");
		a6.get("/?method=OPTIONS").run().assertBody().is("GET");
		a6.request("get","/?method=FOO").run().assertBody().is("GET");

		RestClient a7 = MockRestClient.build(A7.class);
		a7.get("/").run().assertBody().is("GET");
		a7.put("/", "").run().assertBody().is("PUT");
		a7.head("/").run().assertBody().is("");
		a7.options("/").run().assertBody().is("OPTIONS");
		a7.get("/?method=PUT").run().assertBody().is("GET");
		a7.put("/?method=GET", "").run().assertBody().is("PUT");
		a7.get("/?method=HEAD").run().assertBody().is("GET");
		a7.get("/?method=OPTIONS").run().assertBody().is("GET");
		a7.request("get","/?method=FOO").run().assertBody().is("GET");

		RestClient a8 = MockRestClient.build(A8.class);
		a8.get("/").run().assertBody().is("GET");
		a8.put("/", "").run().assertBody().is("PUT");
		a8.head("/").run().assertBody().is("");
		a8.options("/").run().assertBody().is("OPTIONS");
		a8.get("/?method=PUT").run().assertBody().is("GET");
		a8.put("/?method=GET", "").run().assertBody().is("PUT");
		a8.get("/?method=HEAD").run().assertBody().is("GET");
		a8.get("/?method=OPTIONS").run().assertBody().is("GET");
		a8.request("get","/?method=FOO").run().assertBody().is("GET");
	}
}
