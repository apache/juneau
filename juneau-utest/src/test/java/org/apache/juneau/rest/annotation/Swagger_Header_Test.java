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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

class Swagger_Header_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Header(
			name="H"
		)
		@Schema(description={"a","b"}, type="string")
		public static class A1 {
			public A1(String x) {}
		}
		@RestGet
		public void a(A1 h) { /* no-op */ }

		@Header(
			name="H",
			schema=@Schema(description="a\nb",type="string")
		)
		public static class A2 {
			public A2(String x) {}
		}
		@RestPut
		public void b(A2 h) { /* no-op */ }

		@Header(
			name="H",
			schema=@Schema(description="b\nc",type="string")
		)
		@Schema(description={"a","b"}, type="string")
		public static class A3 {
			public A3(String x) {}
		}
		@RestPost
		public void c(A3 h) { /* no-op */ }
	}

	@Test void a01_fromPojo() {
		var s = getSwagger(A.class);
		var x = s.getParameterInfo("/a","get","header","H");

		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
		x = s.getParameterInfo("/b","put","header","H");
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());
		x = s.getParameterInfo("/c","post","header","H");
		assertEquals("b\nc", x.getDescription());
		assertEquals("string", x.getType());	}

	@Rest
	public static class B {

		@Header(name="H")
		public static class B1 {}
		@RestGet
		public void a(B1 h) { /* no-op */ }

		@Header(name="H")
		public static class B2 {
			public String f1;
		}
		@RestPut
		public void b(B2 b) { /* no-op */ }

		@Header(name="H")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(B3 b) { /* no-op */ }

		@Header(name="H")
		public static class B4 {}
		@RestDelete
		public void d(B4 b) { /* no-op */ }
	}

	@Test void b01_schemaFromPojo() {
		var s = getSwagger(B.class);
		var x = s.getParameterInfo("/a","get","header","H");

		assertBean(x, "in,name,type", "header,H,string");

		x = s.getParameterInfo("/b","put","header","H");
		assertBean(x, "in,name,type,schema{properties{f1{type}}}", "header,H,object,{{{string}}}");

		x = s.getParameterInfo("/c","post","header","H");
		assertBean(x, "in,name,type,items{type}", "header,H,array,{string}");

		x = s.getParameterInfo("/d","delete","header","H");
		assertBean(x, "in,name,type", "header,H,string");
	}

	@Rest
	public static class D {

		@RestGet
		public void a(
			@Header(
				name="H"
			)
			@Schema(description={"a","b"}, type="string")
			String h) { /* no-op */ }

		@RestPut
		public void b(
			@Header(
				name="H",
				schema=@Schema(description="a\nb",type="string")
			) String h) { /* no-op */ }

		@RestPost
		public void c(
			@Header(
				name="H",
				schema=@Schema(description="b\nc",type="string")
			)
			@Schema(description={"a","b"}, type="string")
			String h) { /* no-op */ }

		@RestDelete
		public void d(@Header("H") String h) { /* no-op */ }
	}

	@Test void d01_fromParameter() {
		var s = getSwagger(D.class);
		var x = s.getParameterInfo("/a","get","header","H");

		assertBean(x, "name,description,type", "H,a\nb,string");

		x = s.getParameterInfo("/b","put","header","H");
		assertBean(x, "name,description,type", "H,a\nb,string");

		x = s.getParameterInfo("/c","post","header","H");
		assertBean(x, "name,description,type", "H,b\nc,string");

		x = s.getParameterInfo("/d","delete","header","H");
		assertEquals("H", x.getName());
	}

	@Rest
	public static class E {

		@RestGet
		public void a(@Header(name="H") String h) { /* no-op */ }

		public static class E2 {
			public String f1;
		}
		@RestPut
		public void b(@Header("H") E2 b) { /* no-op */ }

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(@Header("H") E3 b) { /* no-op */ }

		public static class E4 {}
		@RestDelete
		public void d(@Header("H") E4 b) { /* no-op */ }

		@RestOp
		public void e(@Header("H") Integer b) { /* no-op */ }

		@RestGet
		public void f(@Header("H") Boolean b) { /* no-op */ }
	}

	@Test void e01_schemaFromParameter() {
		var s = getSwagger(E.class);
		var x = s.getParameterInfo("/a","get","header","H");

		assertBean(x, "in,name,type", "header,H,string");

		x = s.getParameterInfo("/b","put","header","H");
		assertBean(x, "in,name,type,schema{properties{f1{type}}}", "header,H,object,{{{string}}}");

		x = s.getParameterInfo("/c","post","header","H");
		assertBean(x, "in,name,type,items{type}", "header,H,array,{string}");

		x = s.getParameterInfo("/d","delete","header","H");
		assertBean(x, "in,name,type", "header,H,string");

		x = s.getParameterInfo("/e","get","header","H");
		assertBean(x, "in,name,type,format", "header,H,integer,int32");

		x = s.getParameterInfo("/f","get","header","H");
		assertBean(x, "in,name,type", "header,H,boolean");
	}
}