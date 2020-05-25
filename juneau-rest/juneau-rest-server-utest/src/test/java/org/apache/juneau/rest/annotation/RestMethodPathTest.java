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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class RestMethodPathTest {

	//=================================================================================================================
	// Overlapping URL patterns
	//=================================================================================================================

	@Rest
	public static class A {
		@RestMethod(name=GET, path="/")
		public String a01a() {
			return "a";
		}
		@RestMethod(name=GET, path="/*")
		public String a01b() {
			return "b";
		}
		@RestMethod(name=GET, path="/foo")
		public String a01c() {
			return "c";
		}
		@RestMethod(name=GET, path="/foo/*")
		public String a01d() {
			return "d";
		}
		@RestMethod(name=GET, path="/{id}")
		public String a01e() {
			return "e";
		}
		@RestMethod(name=GET, path="/{id}/*")
		public String a01f() {
			return "f";
		}
		@RestMethod(name=GET, path="/{id}/foo")
		public String a01g() {
			return "g";
		}
		@RestMethod(name=GET, path="/{id}/foo/*")
		public String a01h() {
			return "h";
		}
	}
	static MockRest a = MockRest.build(A.class);

	@Test
	public void a01_overlappingPaths() throws Exception {
		// [/] = [test5a]
		// [/*] = [test5b]   -- Cannot get called.
		// [/foo] = [test5c]
		// [/foo/*] = [test5d]
		// [/{id}] = [test5e]
		// [/{id}/*] = [test5f]
		// [/{id}/foo] = [test5g]
		// [/{id}/foo/*] = [test5h]
		a.get("/").execute().assertBody("a");
		a.get("/foo").execute().assertBody("c");
		a.get("/foo/x").execute().assertBody("d");
		a.get("/x").execute().assertBody("e");
		a.get("/x/x").execute().assertBody("f");
		a.get("/x/foo").execute().assertBody("g");
		a.get("/x/foo/x").execute().assertBody("h");
	}

	//=================================================================================================================
	// Overridden URL patterns
	//=================================================================================================================

	@Rest
	public static class B1 {
		@RestMethod(name=GET, path="/foo")
		public String b01a() {
			return "a";
		}
	}

	@Rest
	public static class B2 extends B1 {
		@RestMethod(name=GET, path="/foo")
		public String b02a() {  // Overrides method on parent.
			return "b";
		}
	}
	static MockRest b2 = MockRest.build(B2.class);

	@Test
	public void b01_pathOverriddenByChild() throws Exception {
		b2.get("/foo").execute().assertBody("b");
	}
}
