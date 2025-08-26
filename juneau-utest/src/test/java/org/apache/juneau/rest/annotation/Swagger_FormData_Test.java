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

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

class Swagger_FormData_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@FormData(
			name="F"
		)
		@Schema(description={"a","b"}, type="string")
		public static class A1 {
			public A1(String x) {}
		}
		@RestGet
		public void a(A1 f) { /* no-op */ }

		@FormData(
			name="F",
			schema=@Schema(description="a\nb",type="string")
		)
		public static class A2 {
			public A2(String x) {}
		}
		@RestPut
		public void b(A2 f) { /* no-op */ }

		@FormData(
			name="F",
			schema=@Schema(description="b\nc",type="string")
		)
		@Schema(description={"a","b"}, type="string")
		public static class A3 {
			public A3(String x) {}
		}
		@RestPost
		public void c(A3 f) { /* no-op */ }

		@FormData("F")
		public static class A4 {}
		@RestDelete
		public void d(A4 f) { /* no-op */ }
	}

	@Test void a01_fromPojo() {
		var s = getSwagger(A.class);
		var x = s.getParameterInfo("/a","get","formData","F");

		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","put","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","post","formData","F");
		assertEquals("F", x.getName());
		assertEquals("b\nc", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","delete","formData","F");
		assertEquals("F", x.getName());
	}

	@Rest
	public static class B {

		@FormData(name="F")
		public static class B1 {}
		@RestGet
		public void a(B1 f) { /* no-op */ }

		@FormData("F")
		public static class B2 {
			public String f1;
		}
		@RestPut
		public void b(B2 f) { /* no-op */ }

		@FormData("F")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(B3 f) { /* no-op */ }

		@FormData("F")
		public static class B4 {}
		@RestDelete
		public void d(B4 f) { /* no-op */ }
	}

	@Test void b01_schemaFromPojo() {
		var s = getSwagger(B.class);
		var x = s.getParameterInfo("/a","get","formData","F");

		assertJson(x, "{'in':'formData',name:'F',type:'string'}");

		x = s.getParameterInfo("/b","put","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","post","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","delete","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'string'}");
	}

	@Rest
	public static class D {

		@RestGet
		public void a(
			@FormData(
				name="F"
			)
			@Schema(description={"a","b"}, type="string")
			String f) { /* no-op */ }

		@RestPut
		public void b(
			@FormData(
				name="F",
				schema=@Schema(description="a\nb",type="string")
			) String f) { /* no-op */ }

		@RestPost
		public void c(
			@FormData(
				name="F",
				schema=@Schema(description="b\nc",type="string")
			)
			@Schema(description={"a","b"}, type="string")
			String f) { /* no-op */ }

		@RestDelete
		public void d(@FormData("F") String f) { /* no-op */ }
	}

	@Test void d01_fromParameter() {
		var s = getSwagger(D.class);
		var x = s.getParameterInfo("/a","get","formData","F");

		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","put","formData","F");
		assertEquals("F", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","post","formData","F");
		assertEquals("F", x.getName());
		assertEquals("b\nc", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","delete","formData","F");
		assertEquals("F", x.getName());
	}

	@Rest
	public static class E {

		@RestGet
		public void a(@FormData(name="F") String f) { /* no-op */ }

		public static class E2 {
			public String f1;
		}
		@RestPut
		public void b(@FormData("F") E2 b) { /* no-op */ }

		public static class E3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(@FormData("F") E3 b) { /* no-op */ }

		public static class E4 {}
		@RestDelete
		public void d(@FormData("F") E4 b) { /* no-op */ }

		@RestOp
		public void e(@FormData("F") Integer b) { /* no-op */ }

		@RestGet
		public void f(@FormData("F") Boolean b) { /* no-op */ }
	}

	@Test void e01_schemaFromParameter() {
		var s = getSwagger(E.class);
		var x = s.getParameterInfo("/a","get","formData","F");

		assertJson(x, "{'in':'formData',name:'F',type:'string'}");

		x = s.getParameterInfo("/b","put","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","post","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","delete","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'string'}");

		x = s.getParameterInfo("/e","get","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'integer',format:'int32'}");

		x = s.getParameterInfo("/f","get","formData","F");
		assertJson(x, "{'in':'formData',name:'F',type:'boolean'}");
	}
}