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
package org.apache.juneau.rest.annotation;

import org.apache.juneau.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class Rest_AllowedMethodParams_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// @Rest(allowedMethodParams)
	//------------------------------------------------------------------------------------------------------------------

	public static class A {
		@RestOp
		public String get() {
			return "GET";
		}
		@RestOp
		public String put() {
			return "PUT";
		}
		@RestOp
		public String head() {
			// Note that HTTP client is going to ignore this body.
			return "HEAD";
		}
		@RestOp
		public String options() {
			return "OPTIONS";
		}
		@RestOp(method="foo",path="/")
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

	@Test void a01_basic() throws Exception {
		var a1 = MockRestClient.build(A1.class);
		a1.get("/").run().assertContent("GET");
		a1.put("/", "").run().assertContent("PUT");
		a1.head("/").run().assertContent("");
		a1.options("/").run().assertContent("OPTIONS");
		a1.get("/?method=PUT").run().assertContent("GET");
		a1.put("/?method=GET", "").run().assertContent("PUT");
		a1.get("/?method=HEAD").run().assertContent("HEAD");
		a1.get("/?method=OPTIONS").run().assertContent("OPTIONS");
		a1.request("get","/?method=FOO").run().assertContent("GET");

		var a2 = MockRestClient.build(A2.class);
		a2.get("/").run().assertContent("GET");
		a2.put("/", "").run().assertContent("PUT");
		a2.head("/").run().assertContent("");
		a2.options("/").run().assertContent("OPTIONS");
		a2.get("/?method=PUT").run().assertContent("GET");
		a2.put("/?method=GET", "").run().assertContent("GET");
		a2.get("/?method=HEAD").run().assertContent("GET");
		a2.get("/?method=OPTIONS").run().assertContent("GET");
		a2.request("get","/?method=FOO").run().assertContent("GET");

		var a3 = MockRestClient.build(A3.class);
		a3.get("/").run().assertContent("GET");
		a3.put("/", "").run().assertContent("PUT");
		a3.head("/").run().assertContent("");
		a3.options("/").run().assertContent("OPTIONS");
		a3.get("/?method=PUT").run().assertContent("GET");
		a3.put("/?method=GET", "").run().assertContent("GET");
		a3.get("/?method=HEAD").run().assertContent("GET");
		a3.get("/?method=OPTIONS").run().assertContent("GET");
		a3.request("get","/?method=FOO").run().assertContent("GET");

		var a4 = MockRestClient.build(A4.class);
		a4.get("/").run().assertContent("GET");
		a4.put("/", "").run().assertContent("PUT");
		a4.head("/").run().assertContent("");
		a4.options("/").run().assertContent("OPTIONS");
		a4.get("/?method=PUT").run().assertContent("GET");
		a4.put("/?method=GET", "").run().assertContent("PUT");
		a4.get("/?method=HEAD").run().assertContent("GET");
		a4.get("/?method=OPTIONS").run().assertContent("GET");
		a4.request("get","/?method=FOO").run().assertContent("FOO");

		var a5 = MockRestClient.build(A5.class);
		a5.get("/").run().assertContent("GET");
		a5.put("/", "").run().assertContent("PUT");
		a5.head("/").run().assertContent("");
		a5.options("/").run().assertContent("OPTIONS");
		a5.get("/?method=PUT").run().assertContent("PUT");
		a5.put("/?method=GET", "").run().assertContent("GET");
		a5.get("/?method=HEAD").run().assertContent("HEAD");
		a5.get("/?method=OPTIONS").run().assertContent("OPTIONS");
		a5.request("get","/?method=FOO").run().assertContent("FOO");
		a5.get("/?method=Put").run().assertContent("PUT");
		a5.get("/?method=Foo").run().assertContent("FOO");

		var a6 = MockRestClient.build(A6.class);
		a6.get("/").run().assertContent("GET");
		a6.put("/", "").run().assertContent("PUT");
		a6.head("/").run().assertContent("");
		a6.options("/").run().assertContent("OPTIONS");
		a6.get("/?method=PUT").run().assertContent("GET");
		a6.put("/?method=GET", "").run().assertContent("PUT");
		a6.get("/?method=HEAD").run().assertContent("GET");
		a6.get("/?method=OPTIONS").run().assertContent("GET");
		a6.request("get","/?method=FOO").run().assertContent("GET");

		var a7 = MockRestClient.build(A7.class);
		a7.get("/").run().assertContent("GET");
		a7.put("/", "").run().assertContent("PUT");
		a7.head("/").run().assertContent("");
		a7.options("/").run().assertContent("OPTIONS");
		a7.get("/?method=PUT").run().assertContent("GET");
		a7.put("/?method=GET", "").run().assertContent("PUT");
		a7.get("/?method=HEAD").run().assertContent("GET");
		a7.get("/?method=OPTIONS").run().assertContent("GET");
		a7.request("get","/?method=FOO").run().assertContent("GET");

		var a8 = MockRestClient.build(A8.class);
		a8.get("/").run().assertContent("GET");
		a8.put("/", "").run().assertContent("PUT");
		a8.head("/").run().assertContent("");
		a8.options("/").run().assertContent("OPTIONS");
		a8.get("/?method=PUT").run().assertContent("GET");
		a8.put("/?method=GET", "").run().assertContent("PUT");
		a8.get("/?method=HEAD").run().assertContent("GET");
		a8.get("/?method=OPTIONS").run().assertContent("GET");
		a8.request("get","/?method=FOO").run().assertContent("GET");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Security: Malformed method parameters should return 405, not 500 (CWE-74)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_malformedMethodParameter_returns405() throws Exception {
		var a5 = MockRestClient.build(A5.class);

		// Test various malformed method parameters that should return 404
		// These contain special characters that indicate malformed/encoded input
		a5.get("/?method=VIEW%5C%5C%5C%22&noTrace=true").ignoreErrors().run().assertStatus(405);
		a5.get("/?method=VIEW\\\"&noTrace=true").ignoreErrors().run().assertStatus(405);
		a5.get("/?method=VIEW%22&noTrace=true").ignoreErrors().run().assertStatus(405);
		a5.get("/?method=VIEW<script>&noTrace=true").ignoreErrors().run().assertStatus(405);
		a5.get("/?method=VIEW/test&noTrace=true").ignoreErrors().run().assertStatus(405);
		a5.get("/?method=VIEW%2Ftest&noTrace=true").ignoreErrors().run().assertStatus(405);
		// Test with spaces and other invalid characters
		a5.get("/?method=VIEW test&noTrace=true").ignoreErrors().run().assertStatus(405);
		a5.get("/?method=VIEW+test&noTrace=true").ignoreErrors().run().assertStatus(405);
		// Valid method parameters should still work
		a5.get("/?method=VIEW").ignoreErrors().run().assertStatus(405); // 405 because VIEW method doesn't exist, but not 500
		a5.get("/?method=FOO").run().assertContent("FOO"); // Valid method works
	}
}