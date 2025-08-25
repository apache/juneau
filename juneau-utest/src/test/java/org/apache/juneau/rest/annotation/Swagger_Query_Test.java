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
import org.apache.juneau.bean.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.junit.jupiter.api.*;

class Swagger_Query_Test extends SimpleTestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Query("Q")
		@Schema(d= {"a","b"}, t="string")
		public static class A1 {
			public A1(String x) { /* no-op */ }
		}
		@RestGet
		public void a(A1 q) { /* no-op */ }

		@Query(
			name="Q",
			schema=@Schema(description="a\nb",type="string")
		)
		public static class A2 {
			public A2(String x) { /* no-op */ }
		}
		@RestPut
		public void b(A2 q) { /* no-op */ }

		@Query(
			name="Q",
			schema=@Schema(description="b\nc",type="string")
		)
		@Schema(d={"a","b"}, t="string")
		public static class A3 {
			public A3(String x) { /* no-op */ }
		}
		@RestPost
		public void c(A3 q) { /* no-op */ }

		@Query("Q")
		public static class A4 {}
		@RestDelete
		public void d(A4 q) { /* no-op */ }
	}

	@Test void a01_fromPojo() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(A.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","put","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","post","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("b\nc", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","delete","query","Q");
		assertEquals("Q", x.getName());
	}

	@Rest
	public static class B {

		@Query(name="Q")
		public static class B1 {}
		@RestGet
		public void a(B1 q) { /* no-op */ }

		@Query("Q")
		public static class B2 {
			public String f1;
		}
		@RestPut
		public void b(B2 q) { /* no-op */ }

		@Query("Q")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestPost
		public void c(B3 q) { /* no-op */ }

		@Query("Q")
		public static class B4 {}
		@RestDelete
		public void d(B4 q) { /* no-op */ }
	}

	@Test void b01_schemaFromPojo() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(B.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","query","Q");
		assertJson(x, "{'in':'query',name:'Q',type:'string'}");

		x = s.getParameterInfo("/b","put","query","Q");
		assertJson(x, "{'in':'query',name:'Q',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","post","query","Q");
		assertJson(x, "{'in':'query',name:'Q',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","delete","query","Q");
		assertJson(x, "{'in':'query',name:'Q',type:'string'}");
	}

	@Rest
	public static class D {

		@RestGet
		public void a(
			@Query("Q")
			@Schema(d= {"a","b"}, t="string")
			String q
		) { /* no-op */ }

		@RestPut
		public void b(
			@Query(
				name="Q",
				schema=@Schema(description="a\nb",type="string")
			)
			String q
		) { /* no-op */ }

		@RestPost
		public void c(
			@Query(
				name="Q",
				schema=@Schema(description="b\nc",type="string")
			)
			@Schema(d= {"a","b"}, t="string")
			String q
		) { /* no-op */ }

		@RestDelete
		public void d(@Query("Q") String q) {/* no-op */}
	}

	@Test void d01_fromParameter() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(D.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","put","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","post","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("b\nc", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","delete","query","Q");
		assertEquals("Q", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {

		@RestGet
		public void a(@Query("Q") String q) { /* no-op */ }
	}

	@Test void e01_schemaFromParameter() {
		org.apache.juneau.bean.swagger.Swagger s = getSwagger(E.class);

		ParameterInfo x = s.getParameterInfo("/a","get","query","Q");
		assertJson(x, "{'in':'query',name:'Q',type:'string'}");
	}
}