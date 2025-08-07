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

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.apache.juneau.oapi.*;
import org.junit.jupiter.api.*;

class HeaderAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = HeaderAnnotation_Test.class.getName();

	public static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Header a1 = HeaderAnnotation.create()
		.def("def")
        .description("description")
		.name("name")
		.on("on")
		.onClass(X1.class)
		.parser(OpenApiParser.class)
		.serializer(OpenApiSerializer.class)
		.value("value")
		.build();

	Header a2 = HeaderAnnotation.create()
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
		assertObject(a1).asJson().isMatches(""
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
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
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
		Header c1 = HeaderAnnotation.create(C1.class).on(C2.class).build();
		Header c2 = HeaderAnnotation.create("a").on("b").build();
		Header c3 = HeaderAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		Header c4 = HeaderAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertJsonContains(c1, "on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertJsonContains(c2, "on:['a','b']");
		assertJsonContains(c3, "on:['"+CNAME+"$C1.f1','"+CNAME+"$C2.f2']");
		assertJsonContains(c4, "on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Header(
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
	Header d1 = D1.class.getAnnotationsByType(Header.class)[0];

	@Header(
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
	Header d2 = D2.class.getAnnotationsByType(Header.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}