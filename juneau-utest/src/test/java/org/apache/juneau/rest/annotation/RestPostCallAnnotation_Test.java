// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file		*
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance		    *
// * with the License.  You may obtain a copy of the License at														      *
// *																														 *
// *  http://www.apache.org/licenses/LICENSE-2.0																		     *
// *																														 *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the		*
// * specific language governing permissions and limitations under the License.										      *
// ***************************************************************************************************************************
package org.apache.juneau.rest.annotation;

import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class RestPostCallAnnotation_Test extends SimpleTestBase {

	private static final String CNAME = RestPostCallAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	RestPostCall a1 = RestPostCallAnnotation.create()
		.description("description")
		.on("a")
		.build();

	RestPostCall a2 = RestPostCallAnnotation.create()
		.description("description")
		.on("a")
		.build();

	@Test void a01_basic() {
		assertJson(a1, ""
			+ "{"
				+ "description:['description'],"
				+ "on:['a']"
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
		RestPostCall c4 = RestPostCallAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertJsonContains(c4, "on:['"+CNAME+"$C1.m1()','"+CNAME+"$C2.m2()']");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@RestPostCall(
		description={ "description" },
		on="a"
	)
	public static class D1 {}
	RestPostCall d1 = D1.class.getAnnotationsByType(RestPostCall.class)[0];

	@RestPostCall(
		description={ "description" },
		on="a"
	)
	public static class D2 {}
	RestPostCall d2 = D2.class.getAnnotationsByType(RestPostCall.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}