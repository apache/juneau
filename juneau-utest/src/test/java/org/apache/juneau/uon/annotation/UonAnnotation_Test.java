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
package org.apache.juneau.uon.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.uon.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class UonAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Uon a1 = UonAnnotation.create()
		.description("a")
		.build();

	Uon a2 = UonAnnotation.create()
		.description("a")
		.build();

	@Test void a01_basic() {
		assertBean(a1, "description", "[a]");
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
		var bc1 = MarshallingContext.create().annotations(a1).build();
		var bc2 = MarshallingContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Uon(
		description={ "a" }
	)
	public static class D1 {}
	Uon d1 = D1.class.getAnnotationsByType(Uon.class)[0];

	@Uon(
		description={ "a" }
	)
	public static class D2 {}
	Uon d2 = D2.class.getAnnotationsByType(Uon.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// UonApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(UonApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(UonApplyAnnotation.empty(null));
		assertTrue(UonApplyAnnotation.empty(UonApplyAnnotation.DEFAULT));
		assertFalse(UonApplyAnnotation.empty(UonApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = UonApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = UonApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = UonApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var uon = UonAnnotation.create().description("test").build();
		var a = UonApplyAnnotation.create().value(uon).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// UonBeanPropertyMeta + UonClassMeta lookup tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_uonBeanPropertyMeta_default() {
		assertNotNull(UonBeanPropertyMeta.DEFAULT);
	}

	public static class F02_Bean { public String name; }

	@Test void f02_uonBeanPropertyMeta_lookup() {
		var s = UonSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getUonBeanPropertyMeta(bpm));
		// null path
		assertNotNull(s.getUonBeanPropertyMeta(null));
	}

	@Test void f03_uonClassMeta_lookup() {
		var s = UonSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getUonClassMeta(cm));
	}

	//------------------------------------------------------------------------------------------------------------------
	// UonConfigAnnotation tests (already covered elsewhere; included here for completeness).
	//------------------------------------------------------------------------------------------------------------------

	@UonConfig(addBeanTypes = "true")
	public static class G01_Bean {}

	@Test void g01_uonConfigAddBeanTypes() {
		var s = UonSerializer.create().applyAnnotations(G01_Bean.class).build();
		assertNotNull(s);
	}

	@UonConfig(encoding = "true")
	public static class G02_Bean {}

	@Test void g02_uonConfigEncoding() {
		var s = UonSerializer.create().applyAnnotations(G02_Bean.class).build();
		assertNotNull(s);
	}

	@UonConfig(decoding = "true", validateEnd = "true")
	public static class G03_Bean {}

	@Test void g03_uonConfigParserApply() {
		var p = UonParser.create().applyAnnotations(G03_Bean.class).build();
		assertNotNull(p);
	}
}
