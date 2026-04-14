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
package org.apache.juneau.jsonschema.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.commons.annotation.Schema;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class SchemaApplyAnnotation_Test extends TestBase {

	private static final String CNAME = SchemaApplyAnnotation_Test.class.getName();

	private static class X1 {}

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	SchemaApply a1 = SchemaApplyAnnotation.create()
		.on("u")
		.onClass(X1.class)
		.value(SchemaAnnotation.create().format("date-time").build())
		.build();

	SchemaApply a2 = SchemaApplyAnnotation.create()
		.on("u")
		.onClass(X1.class)
		.value(SchemaAnnotation.create().format("date-time").build())
		.build();

	@Test void a01_basic() {
		assertBean(a1, "on,onClass,value{format}", "[u],[X1],{date-time}");
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
	// Targeting methods
	//------------------------------------------------------------------------------------------------------------------

	public static class C1 {
		public int f1;
		public void m1() {}
	}
	public static class C2 {
		public int f2;
		public void m2() {}
	}

	@Test void c01_targetingMethods() throws Exception {
		var c1 = SchemaApplyAnnotation.create(C1.class).on(C2.class).build();
		var c2 = SchemaApplyAnnotation.create("a").on("b").build();
		var c3 = SchemaApplyAnnotation.create().on(C1.class.getField("f1")).on(C2.class.getField("f2")).build();
		var c4 = SchemaApplyAnnotation.create().on(C1.class.getMethod("m1")).on(C2.class.getMethod("m2")).build();

		assertBean(c1, "on", "["+CNAME+"$C1,"+CNAME+"$C2]");
		assertBean(c2, "on", "[a,b]");
		assertBean(c3, "on", "["+CNAME+"$C1.f1,"+CNAME+"$C2.f2]");
		assertBean(c4, "on", "["+CNAME+"$C1.m1(),"+CNAME+"$C2.m2()]");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@SchemaApply(on="u", onClass=X1.class, value=@Schema(format="date-time"))
	public static class D1 {}
	SchemaApply d1 = D1.class.getAnnotationsByType(SchemaApply.class)[0];

	@SchemaApply(on="u", onClass=X1.class, value=@Schema(format="date-time"))
	public static class D2 {}
	SchemaApply d2 = D2.class.getAnnotationsByType(SchemaApply.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Dynamic application via BeanContext
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_dynamicApplication() {
		var schema = SchemaAnnotation.create().type("string").format("date-time").build();
		var apply = SchemaApplyAnnotation.create("com.example.Foo").value(schema).build();

		assertBean(apply, "on,value{type,format}", "[com.example.Foo],{string,date-time}");
	}

	@Test void e02_emptyCheck() {
		assertTrue(SchemaApplyAnnotation.empty(null));
		assertTrue(SchemaApplyAnnotation.empty(SchemaApplyAnnotation.DEFAULT));
		assertFalse(SchemaApplyAnnotation.empty(a1));
	}
}
