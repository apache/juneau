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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.rest.testutils.TestUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.http.annotation.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class Swagger_Query_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// Swagger tests
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class A {

		@Query(
			n="Q",
			d= {"a","b"},
			t="string"
		)
		public static class A1 {
			public A1(String x) {}
		}
		@RestMethod
		public void a(A1 q) {}

		@Query(
			n="Q",
			api={
				"description: 'a\nb',",
				"type:'string'"
			}
		)
		public static class A2 {
			public A2(String x) {}
		}
		@RestMethod
		public void b(A2 q) {}

		@Query(
			n="Q",
			api={
				"description: 'b\nc',",
				"type:'string'"
			},
			d={"a","b"},
			t="string"
		)
		public static class A3 {
			public A3(String x) {}
		}
		@RestMethod
		public void c(A3 q) {}

		@Query("Q")
		public static class A4 {}
		@RestMethod
		public void d(A4 q) {}
	}

	@Test
	public void a01_fromPojo() throws Exception {
		Swagger s = getSwagger(A.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","get","query","Q");
		assertEquals("Q", x.getName());
	}

	@Rest
	public static class B {

		@Query(n="Q")
		public static class B1 {}
		@RestMethod
		public void a(B1 q) {}

		@Query("Q")
		public static class B2 {
			public String f1;
		}
		@RestMethod
		public void b(B2 q) {}

		@Query("Q")
		public static class B3 extends LinkedList<String> {
			private static final long serialVersionUID = 1L;
		}
		@RestMethod
		public void c(B3 q) {}

		@Query("Q")
		public static class B4 {}
		@RestMethod
		public void d(B4 q) {}
	}

	@Test
	public void b01_schemaFromPojo() throws Exception {
		Swagger s = getSwagger(B.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","query","Q");
		assertObject(x).json().is("{'in':'query',name:'Q',type:'string'}");

		x = s.getParameterInfo("/b","get","query","Q");
		assertObject(x).json().is("{'in':'query',name:'Q',type:'object',schema:{properties:{f1:{type:'string'}}}}");

		x = s.getParameterInfo("/c","get","query","Q");
		assertObject(x).json().is("{'in':'query',name:'Q',type:'array',items:{type:'string'}}");

		x = s.getParameterInfo("/d","get","query","Q");
		assertObject(x).json().is("{'in':'query',name:'Q',type:'string'}");
	}

	@Rest
	public static class C {

		@Query(n="Q", ex={"{f1:'a'}"})
		public static class C1 {
			public String f1;
		}
		@RestMethod
		public void a(C1 q) {}
	}

	@Test
	public void c01_exampleFromPojo() throws Exception {
		Swagger s = getSwagger(C.class);

		ParameterInfo x = s.getParameterInfo("/a","get","query","Q");
		assertEquals("{f1:'a'}", x.getExample());
	}

	@Rest
	public static class D {

		@RestMethod
		public void a(
			@Query(
				n="Q",
				d= {"a","b"},
				t="string"
			)
			String q) {}

		@RestMethod
		public void b(
			@Query(
				n="Q",
				api={
					"description: 'a\nb',",
					"type:'string'"
				}
			)
			String q) {}

		@RestMethod
		public void c(
			@Query(
				n="Q",
				api={
					"description: 'b\nc',",
					"type:'string'"
				},
				d= {"a","b"},
				t="string"
			)
			String q) {}

		@RestMethod
		public void d(@Query("Q") String q) {}
	}

	@Test
	public void d01_fromParameter() throws Exception {
		Swagger s = getSwagger(D.class);
		ParameterInfo x;

		x = s.getParameterInfo("/a","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/b","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/c","get","query","Q");
		assertEquals("Q", x.getName());
		assertEquals("a\nb", x.getDescription());
		assertEquals("string", x.getType());

		x = s.getParameterInfo("/d","get","query","Q");
		assertEquals("Q", x.getName());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Schema
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class E {

		@RestMethod
		public void a(@Query("Q") String q) {}
	}

	@Test
	public void e01_schemaFromParameter() throws Exception {
		Swagger s = getSwagger(E.class);

		ParameterInfo x = s.getParameterInfo("/a","get","query","Q");
		assertObject(x).json().is("{'in':'query',name:'Q',type:'string'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Examples
	//-----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class F {

		@RestMethod
		public void a(@Query(n="Q",ex={"a","b"}) String q) {}
	}

	@Test
	public void f01_exampleFromParameter() throws Exception {
		Swagger s = getSwagger(F.class);

		ParameterInfo x = s.getParameterInfo("/a","get","query","Q");
		assertEquals("a\nb", x.getExample());
	}
}
