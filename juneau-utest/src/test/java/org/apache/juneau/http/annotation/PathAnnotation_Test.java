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
package org.apache.juneau.http.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.oapi.*;
import org.junit.jupiter.api.*;

class PathAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = PathAnnotation_Test.class.getName();

	public static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Path a1 = PathAnnotation.create()
		.def("def")
		.description("description")
		.name("name")
		.on("on")
		.onClass(X1.class)
		.parser(OpenApiParser.class)
		.serializer(OpenApiSerializer.class)
		.value("value")
		.build();

	Path a2 = PathAnnotation.create()
		.def("def")
		.description("description")
		.name("name")
		.on("on")
		.onClass(X1.class)
		.parser(OpenApiParser.class)
		.serializer(OpenApiSerializer.class)
		.value("value")
		.build();

	@Test void a01_basic() {
		assertJsonMatches(a1, ""
			+ "{"
				+ "def:'def',"
				+ "description:['description'],"
				+ "name:'name',"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"$X1'],"
				+ "parser:'org.apache.juneau.oapi.OpenApiParser',"
				+ "schema:{*},"
				+ "serializer:'org.apache.juneau.oapi.OpenApiSerializer',"
				+ "value:'value'"
			+ "}"
		);
	}

	@Test void a02_testEquivalency() {
		assertEquals(a2, a1);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_testEquivalencyInPropertyStores() {
		var bc1 = BeanContext.create().annotations(a1).build();
		var bc2 = BeanContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}  // NOSONAR
	}
	public static class C2 {
		public int f2;
		public void m2() {}  // NOSONAR
	}

	@Test void c01_otherMethods() throws Exception {
		var c1 = PathAnnotation.create(C1.class).on(C2.class).build();
		var c2 = PathAnnotation.create("a").on("b").build();
		var c3 = PathAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		var c4 = PathAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertJsonContains(c1, "on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertJsonContains(c2, "on:['a','b']");
		assertJsonContains(c3, "on:['"+CNAME+"$C1.f1','"+CNAME+"$C2.f2']");
		assertJsonContains(c4, "on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Path(
		def="def",
		description={ "description" },
		name="name",
		on="on",
		onClass=X1.class,
		parser=OpenApiParser.class,
		serializer=OpenApiSerializer.class,
		value="value"
	)
	public static class D1 {}
	Path d1 = D1.class.getAnnotationsByType(Path.class)[0];

	@Path(
		def="def",
		description={ "description" },
		name="name",
		on="on",
		onClass=X1.class,
		parser=OpenApiParser.class,
		serializer=OpenApiSerializer.class,
		value="value"
	)
	public static class D2 {}
	Path d2 = D2.class.getAnnotationsByType(Path.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}