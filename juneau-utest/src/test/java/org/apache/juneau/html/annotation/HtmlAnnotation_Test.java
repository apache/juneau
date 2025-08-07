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
package org.apache.juneau.html.annotation;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.junit.jupiter.api.*;

class HtmlAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = HtmlAnnotation_Test.class.getName();

	private static class X1 extends HtmlRender<Object> {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Html a1 = HtmlAnnotation.create()
		.anchorText("a")
        .description("description")
		.format(HtmlFormat.XML)
		.link("c")
		.noTableHeaders(true)
		.noTables(true)
		.on("d")
		.render(X1.class)
		.build();

	Html a2 = HtmlAnnotation.create()
		.anchorText("a")
        .description("description")
		.format(HtmlFormat.XML)
		.link("c")
		.noTableHeaders(true)
		.noTables(true)
		.on("d")
		.render(X1.class)
		.build();

	@Test void a01_basic() {
		assertJson(a1, ""
			+ "{"
				+ "anchorText:'a',"
                + "description:['description'],"
				+ "format:'XML',"
				+ "link:'c',"
				+ "noTableHeaders:true,"
				+ "noTables:true,"
				+ "on:['d'],"
				+ "onClass:[],"
				+ "render:'"+CNAME+"$X1'"
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
		Html c1 = HtmlAnnotation.create(C1.class).on(C2.class).build();
		Html c2 = HtmlAnnotation.create("a").on("b").build();
		Html c3 = HtmlAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		Html c4 = HtmlAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertObject(c1).asJson().isContains("on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertObject(c2).asJson().isContains("on:['a','b']");
		assertObject(c3).asJson().isContains("on:['"+CNAME+"$C1.f1','"+CNAME+"$C2.f2']");
		assertObject(c4).asJson().isContains("on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Html(
		anchorText="a",
        description={ "description" },
		format=HtmlFormat.XML,
		link="c",
		noTableHeaders=true,
		noTables=true,
		on="d",
		render=X1.class
	)
	public static class D1 {}
	Html d1 = D1.class.getAnnotationsByType(Html.class)[0];

	@Html(
		anchorText="a",
        description={ "description" },
		format=HtmlFormat.XML,
		link="c",
		noTableHeaders=true,
		noTables=true,
		on="d",
		render=X1.class
	)
	public static class D2 {}
	Html d2 = D2.class.getAnnotationsByType(Html.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}