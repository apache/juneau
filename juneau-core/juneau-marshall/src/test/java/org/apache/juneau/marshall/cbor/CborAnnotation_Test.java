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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class CborAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Cbor a1 = CborAnnotation.create()
		.description("a")
		.build();

	Cbor a2 = CborAnnotation.create()
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

	@Cbor(
		description={ "a" }
	)
	public static class D1 {}
	Cbor d1 = D1.class.getAnnotationsByType(Cbor.class)[0];

	@Cbor(
		description={ "a" }
	)
	public static class D2 {}
	Cbor d2 = D2.class.getAnnotationsByType(Cbor.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// CborApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(CborApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(CborApplyAnnotation.empty(null));
		assertTrue(CborApplyAnnotation.empty(CborApplyAnnotation.DEFAULT));
		assertFalse(CborApplyAnnotation.empty(CborApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = CborApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = CborApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = CborApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var cbor = CborAnnotation.create().description("test").build();
		var a = CborApplyAnnotation.create().value(cbor).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// CborConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@CborConfig(addBeanTypes = "true")
	public static class G01_Bean {}

	@Test void g01_cborConfigAddBeanTypes() {
		var s = CborSerializer.create().applyAnnotations(G01_Bean.class).build();
		assertNotNull(s);
	}

	@CborConfig(useTags = "true")
	public static class G02_Bean {}

	@Test void g02_cborConfigUseTags() {
		var s = CborSerializer.create().applyAnnotations(G02_Bean.class).build();
		assertNotNull(s);
	}

	@CborConfig
	public static class G03_Bean {}

	@Test void g03_cborConfigParserApply() {
		var p = CborParser.create().applyAnnotations(G03_Bean.class).build();
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// CborBeanPropertyMeta + CborClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_cborBeanPropertyMeta_default() {
		assertNotNull(CborBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_cborBeanPropertyMeta_lookup() {
		var s = CborSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getCborBeanPropertyMeta(bpm));
		assertNotNull(s.getCborBeanPropertyMeta(null));
	}

	@Test void f03_cborClassMeta_lookup() {
		var s = CborSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getCborClassMeta(cm));
	}
}
