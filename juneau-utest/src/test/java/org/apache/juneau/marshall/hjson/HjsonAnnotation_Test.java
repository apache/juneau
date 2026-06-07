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
package org.apache.juneau.marshall.hjson;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class HjsonAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Default value
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultValue() {
		var a = HjsonAnnotation.DEFAULT;
		assertNotNull(a);
	}

	@Test void a02_defaultEquality() {
		var a1 = HjsonAnnotation.DEFAULT;
		var a2 = HjsonAnnotation.DEFAULT;
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Hjson
	public static class D1 {}

	@Hjson
	public static class D2 {}

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		var d1 = D1.class.getAnnotationsByType(Hjson.class)[0];
		var d2 = D2.class.getAnnotationsByType(Hjson.class)[0];
		assertEquals(d1, d2);
		assertEquals(d1.hashCode(), d2.hashCode());
	}

	@Test void d02_defaultEqualsDeclarative() {
		var d1 = D1.class.getAnnotationsByType(Hjson.class)[0];
		assertEquals(HjsonAnnotation.DEFAULT, d1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HjsonApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(HjsonApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(HjsonApplyAnnotation.empty(null));
		assertTrue(HjsonApplyAnnotation.empty(HjsonApplyAnnotation.DEFAULT));
		assertFalse(HjsonApplyAnnotation.empty(HjsonApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = HjsonApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationBuilderValue() {
		var a = HjsonApplyAnnotation.create().value(HjsonAnnotation.DEFAULT).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HjsonConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@HjsonConfig(useMultilineStrings = "true")
	public static class G01_Class {}

	@Test void g01_hjsonConfigSerializerApply() {
		var s = HjsonSerializer.create().applyAnnotations(G01_Class.class).build();
		assertNotNull(s);
	}

	@HjsonConfig
	public static class G02_Class {}

	@Test void g02_hjsonConfigParserApply() {
		var p = HjsonParser.create().applyAnnotations(G02_Class.class).build();
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HjsonBeanPropertyMeta + HjsonClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_hjsonBeanPropertyMeta_default() {
		assertNotNull(HjsonBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_hjsonBeanPropertyMeta_lookup() {
		var s = HjsonSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getHjsonBeanPropertyMeta(bpm));
		assertNotNull(s.getHjsonBeanPropertyMeta(null));
	}

	@Test void f03_hjsonClassMeta_lookup() {
		var s = HjsonSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getHjsonClassMeta(cm));
	}
}
