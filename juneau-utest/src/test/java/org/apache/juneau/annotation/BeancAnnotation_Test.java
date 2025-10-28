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
package org.apache.juneau.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class BeancAnnotation_Test extends TestBase {

	private static final String CNAME = BeancAnnotation_Test.class.getName();

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Beanc a1 = BeancAnnotation.create()
		.description("a")
		.on("b")
		.properties("c")
		.build();

	Beanc a2 = BeancAnnotation.create()
		.description("a")
		.on("b")
		.properties("c")
		.build();

	@Test void a01_basic() {
		assertBean(a1, "description,on,properties", "[a],[b],c");
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

	public static class C1 {}
	public static class C2 {}

	@Test void c01_otherMethods() throws Exception {
		var c1 = BeancAnnotation.create("a").on("b").build();
		var c2 = BeancAnnotation.create().on(C1.class.getConstructor()).on(C2.class.getConstructor()).build();

		assertBean(c1, "on", "[a,b]");
		assertBean(c2, "on", "["+CNAME+"$C1(),"+CNAME+"$C2()]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Beanc(
		description={ "a" },
		on="b",
		properties="c"
	)
	public static class D1 {}
	Beanc d1 = D1.class.getAnnotationsByType(Beanc.class)[0];

	@Beanc(
		description={ "a" },
		on="b",
		properties="c"
	)
	public static class D2 {}
	Beanc d2 = D2.class.getAnnotationsByType(Beanc.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}
}