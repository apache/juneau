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
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class HtmlLinkAnnotation_Test {

	private static final String CNAME = HtmlLinkAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	HtmlLink a1 = HtmlLinkAnnotation.create()
		.nameProperty("a")
		.on("b")
		.uriProperty("c")
		.build();

	HtmlLink a2 = HtmlLinkAnnotation.create()
		.nameProperty("a")
		.on("b")
		.uriProperty("c")
		.build();

	@Test
	public void a01_basic() {
		assertObject(a1).asJson().is(""
			+ "{"
				+ "nameProperty:'a',"
				+ "on:['b'],"
				+ "onClass:[],"
				+ "uriProperty:'c'"
			+ "}"
		);
	}

	@Test
	public void a02_testEquivalency() {
		assertObject(a1).is(a2);
		assertInteger(a1.hashCode()).is(a2.hashCode()).isNotAny(0,-1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PropertyStore equivalency.
	//------------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_testEquivalencyInPropertyStores() {
		BeanContext bc1 = BeanContext.create().annotations(a1).build();
		BeanContext bc2 = BeanContext.create().annotations(a2).build();
		assertTrue(bc1 == bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Other methods.
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}
	}
	public static class C2 {
		public int f2;
		public void m2() {}
	}

	@Test
	public void c01_otherMethods() throws Exception {
		HtmlLink c1 = HtmlLinkAnnotation.create(C1.class).on(C2.class).build();
		HtmlLink c2 = HtmlLinkAnnotation.create("a").on("b").build();

		assertObject(c1).asJson().isContains("on:['"+CNAME+"$C1','"+CNAME+"$C2']");
		assertObject(c2).asJson().isContains("on:['a','b']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@HtmlLink(
		nameProperty="a",
		on="b",
		uriProperty="c"
	)
	public static class D1 {}
	HtmlLink d1 = D1.class.getAnnotationsByType(HtmlLink.class)[0];

	@HtmlLink(
		nameProperty="a",
		on="b",
		uriProperty="c"
	)
	public static class D2 {}
	HtmlLink d2 = D2.class.getAnnotationsByType(HtmlLink.class)[0];

	@Test
	public void d01_comparisonWithDeclarativeAnnotations() {
		assertObject(d1).is(d2).is(a1);
		assertInteger(d1.hashCode()).is(d2.hashCode()).is(a1.hashCode()).isNotAny(0,-1);
	}
}
