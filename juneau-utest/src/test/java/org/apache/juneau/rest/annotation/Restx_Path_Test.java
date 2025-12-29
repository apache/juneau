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
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;

class Restx_Path_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Nested children.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(path="/p0", children={A1.class})
	public static class A  {
		@RestOp(path="/")
		public String a(RestContext c) {
			return "A-" + c.getFullPath();
		}
	}
	@Rest(path="/p1", children={A2.class})
	public static class A1 {
		@RestOp(path="/")
		public String a(RestContext c) {
			return "A01-" + c.getFullPath();
		}
	}
	public static class A2a  {
		@RestOp(path="/")
		public String a(RestContext c) {
			return "A02a-" + c.getFullPath();
		}
	}
	@Rest(path="/p2")
	public static class A2 extends A2a {}

	@Test void a01_nestedChildren() throws Exception {
		var a = MockRestClient.build(A.class);
		// Since we're not running from a servlet container, we access A directly with no path.
		// However, the path is still reflected in RestContext.getPath().
		a.get("/").run().assertContent("A-p0");
		a.get("/p1").run().assertContent("A01-p0/p1");
		a.get("/p1/p2").run().assertContent("A02a-p0/p1/p2");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overlapping URL patterns
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class B {
		@RestGet(path="/")
		public String a() {
			return "a";
		}
		@RestGet(path="/*")
		public String b() {
			return "b";
		}
		@RestGet(path="/foo")
		public String c() {
			return "c";
		}
		@RestGet(path="/foo/*")
		public String d() {
			return "d";
		}
		@RestGet(path="/{id}")
		public String e() {
			return "e";
		}
		@RestGet(path="/{id}/*")
		public String f() {
			return "f";
		}
		@RestGet(path="/{id}/foo")
		public String g() {
			return "g";
		}
		@RestGet(path="/{id}/foo/*")
		public String h() {
			return "h";
		}
	}

	@Test void b01_overlappingPaths() throws Exception {
		var b = MockRestClient.build(B.class);
		// [/] = [test5a]
		// [/*] = [test5b]   -- Cannot get called.
		// [/foo] = [test5c]
		// [/foo/*] = [test5d]
		// [/{id}] = [test5e]
		// [/{id}/*] = [test5f]
		// [/{id}/foo] = [test5g]
		// [/{id}/foo/*] = [test5h]
		b.get("/").run().assertContent("a");
		b.get("/foo").run().assertContent("c");
		b.get("/foo/x").run().assertContent("d");
		b.get("/x").run().assertContent("e");
		b.get("/x/x").run().assertContent("f");
		b.get("/x/foo").run().assertContent("g");
		b.get("/x/foo/x").run().assertContent("h");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Overridden URL patterns
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class C1 {
		@RestGet(path="/foo")
		public String a() {
			return "a";
		}
	}

	@Rest
	public static class C2 extends C1 {
		@RestGet(path="/foo")
		public String b() {  // Overrides method on parent.
			return "b";
		}
	}

	@Test void c01_pathOverriddenByChild() throws Exception {
		var c2 = MockRestClient.build(C2.class);
		c2.get("/foo").run().assertContent("b");
	}
}