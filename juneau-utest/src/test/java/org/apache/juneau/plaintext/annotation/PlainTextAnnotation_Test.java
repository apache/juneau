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
package org.apache.juneau.plaintext.annotation;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.plaintext.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class PlainTextAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	PlainText a1 = PlainTextAnnotation.create()
		.description("a")
		.build();

	PlainText a2 = PlainTextAnnotation.create()
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

	@PlainText(
		description={ "a" }
	)
	public static class D1 {}
	PlainText d1 = D1.class.getAnnotationsByType(PlainText.class)[0];

	@PlainText(
		description={ "a" }
	)
	public static class D2 {}
	PlainText d2 = D2.class.getAnnotationsByType(PlainText.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// PlainTextApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(PlainTextApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(PlainTextApplyAnnotation.empty(null));
		assertTrue(PlainTextApplyAnnotation.empty(PlainTextApplyAnnotation.DEFAULT));
		assertFalse(PlainTextApplyAnnotation.empty(PlainTextApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = PlainTextApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = PlainTextApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = PlainTextApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var pt = PlainTextAnnotation.create().description("test").build();
		var a = PlainTextApplyAnnotation.create().value(pt).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// PlainTextBeanPropertyMeta + PlainTextClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_plainTextBeanPropertyMeta_default() {
		assertNotNull(PlainTextBeanPropertyMeta.DEFAULT);
	}

	public static class F02_Bean { public String name; }

	@Test void f02_plainTextBeanPropertyMeta_lookup() {
		var s = PlainTextSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getPlainTextBeanPropertyMeta(bpm));
		assertNotNull(s.getPlainTextBeanPropertyMeta(null));
	}

	@Test void f03_plainTextClassMeta_lookup() {
		var s = PlainTextSerializer.DEFAULT;
		var bc = s.getMarshallingContext();
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getPlainTextClassMeta(cm));
	}
}
