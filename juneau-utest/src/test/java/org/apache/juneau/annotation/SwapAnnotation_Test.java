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
package org.apache.juneau.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SwapAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = SwapAnnotation_Test.class.getName();

	private static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Swap a1 = SwapAnnotation.create()
		.description("description")
		.impl(X1.class)
		.mediaTypes("mediaTypes")
		.on("on")
		.onClass(X1.class)
		.template("template")
		.value(X1.class)
		.build();

	Swap a2 = SwapAnnotation.create()
		.description("description")
		.impl(X1.class)
		.mediaTypes("mediaTypes")
		.on("on")
		.onClass(X1.class)
		.template("template")
		.value(X1.class)
		.build();

	@Test void a01_basic() {
		assertJson(""
			+ "{"
				+ "description:['description'],"
				+ "impl:'"+CNAME+"$X1',"
				+ "mediaTypes:['mediaTypes'],"
				+ "on:['on'],"
				+ "onClass:['"+CNAME+"$X1'],"
				+ "template:'template',"
				+ "value:'"+CNAME+"$X1'"
			+ "}", a1
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
		var c1 = SwapAnnotation.create(C1.class).on(C2.class).build();
		var c2 = SwapAnnotation.create("a").on("b").build();
		var c3 = SwapAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		var c4 = SwapAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertBean(c1, "on", "["+CNAME+"$C1,"+CNAME+"$C2]");
		assertBean(c2, "on", "[a,b]");
		assertBean(c3, "on", "["+CNAME+"$C1.f1,"+CNAME+"$C2.f2]");
		assertBean(c4, "on", "["+CNAME+"$C1.m1(),"+CNAME+"$C2.m2()]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Swap(
		description={ "description" },
		impl=X1.class,
		mediaTypes="mediaTypes",
		on="on",
		onClass=X1.class,
		template="template",
		value=X1.class
	)
	public static class D1 {}
	Swap d1 = D1.class.getAnnotationsByType(Swap.class)[0];

	@Swap(
		description={ "description" },
		impl=X1.class,
		mediaTypes="mediaTypes",
		on="on",
		onClass=X1.class,
		template="template",
		value=X1.class
	)
	public static class D2 {}
	Swap d2 = D2.class.getAnnotationsByType(Swap.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}