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

import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client2.*;
import org.apache.juneau.rest.mock2.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Restx_Path_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Nested children.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/p0", children={A1.class})
	public static class A  {
		@RestMethod(path="/")
		public String a(RestContext c) {
			return "A-" + c.getPath();
		}
	}
	@Rest(path="/p1", children={A2.class})
	public static class A1 {
		@RestMethod(path="/")
		public String a(RestContext c) {
			return "A01-" + c.getPath();
		}
	}
	public static class A2a  {
		@RestMethod(path="/")
		public String a(RestContext c) {
			return "A02a-" + c.getPath();
		}
	}
	@Rest(path="/p2")
	public static class A2 extends A2a {}

	@Test
	public void a01_nestedChildren() throws Exception {
		RestClient a = MockRestClient.build(A.class);
		// Since we're not running from a servlet container, we access A directly with no path.
		// However, the path is still reflected in RestContext.getPath().
		a.get("/").run().assertBody().is("A-p0");
		a.get("/p1").run().assertBody().is("A01-p0/p1");
		a.get("/p1/p2").run().assertBody().is("A02a-p0/p1/p2");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overlapping URL patterns
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestMethod(path="/")
		public String a() {
			return "a";
		}
		@RestMethod(path="/*")
		public String b() {
			return "b";
		}
		@RestMethod(path="/foo")
		public String c() {
			return "c";
		}
		@RestMethod(path="/foo/*")
		public String d() {
			return "d";
		}
		@RestMethod(path="/{id}")
		public String e() {
			return "e";
		}
		@RestMethod(path="/{id}/*")
		public String f() {
			return "f";
		}
		@RestMethod(path="/{id}/foo")
		public String g() {
			return "g";
		}
		@RestMethod(path="/{id}/foo/*")
		public String h() {
			return "h";
		}
	}

	@Test
	public void b01_overlappingPaths() throws Exception {
		RestClient b = MockRestClient.build(B.class);
		// [/] = [test5a]
		// [/*] = [test5b]   -- Cannot get called.
		// [/foo] = [test5c]
		// [/foo/*] = [test5d]
		// [/{id}] = [test5e]
		// [/{id}/*] = [test5f]
		// [/{id}/foo] = [test5g]
		// [/{id}/foo/*] = [test5h]
		b.get("/").run().assertBody().is("a");
		b.get("/foo").run().assertBody().is("c");
		b.get("/foo/x").run().assertBody().is("d");
		b.get("/x").run().assertBody().is("e");
		b.get("/x/x").run().assertBody().is("f");
		b.get("/x/foo").run().assertBody().is("g");
		b.get("/x/foo/x").run().assertBody().is("h");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden URL patterns
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C1 {
		@RestMethod(path="/foo")
		public String a() {
			return "a";
		}
	}

	@Rest
	public static class C2 extends C1 {
		@RestMethod(path="/foo")
		public String b() {  // Overrides method on parent.
			return "b";
		}
	}

	@Test
	public void c01_pathOverriddenByChild() throws Exception {
		RestClient c2 = MockRestClient.build(C2.class);
		c2.get("/foo").run().assertBody().is("b");
	}
}
