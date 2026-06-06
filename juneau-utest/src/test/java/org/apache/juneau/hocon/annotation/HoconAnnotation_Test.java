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
package org.apache.juneau.hocon.annotation;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.hocon.*;
import org.junit.jupiter.api.*;

class HoconAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Default value
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultValue() {
		var a = HoconAnnotation.DEFAULT;
		assertNotNull(a);
	}

	@Test void a02_defaultEquality() {
		var a1 = HoconAnnotation.DEFAULT;
		var a2 = HoconAnnotation.DEFAULT;
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Hocon
	public static class D1 {}

	@Hocon
	public static class D2 {}

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		var d1 = D1.class.getAnnotationsByType(Hocon.class)[0];
		var d2 = D2.class.getAnnotationsByType(Hocon.class)[0];
		assertEquals(d1, d2);
		assertEquals(d1.hashCode(), d2.hashCode());
	}

	@Test void d02_defaultEqualsDeclarative() {
		var d1 = D1.class.getAnnotationsByType(Hocon.class)[0];
		assertEquals(HoconAnnotation.DEFAULT, d1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HoconApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(HoconApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(HoconApplyAnnotation.empty(null));
		assertTrue(HoconApplyAnnotation.empty(HoconApplyAnnotation.DEFAULT));
		assertFalse(HoconApplyAnnotation.empty(HoconApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = HoconApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationBuilderValue() {
		var a = HoconApplyAnnotation.create().value(HoconAnnotation.DEFAULT).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HoconConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@HoconConfig(useEqualsSign = "true")
	public static class G01_Class {}

	@Test void g01_hoconConfigSerializerApply() {
		var s = HoconSerializer.create().applyAnnotations(G01_Class.class).build();
		assertNotNull(s);
	}

	@HoconConfig(resolveSubstitutions = "true")
	public static class G02_Class {}

	@Test void g02_hoconConfigParserApply() {
		var p = HoconParser.create().applyAnnotations(G02_Class.class).build();
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// HoconBeanPropertyMeta + HoconClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_hoconBeanPropertyMeta_default() {
		assertNotNull(HoconBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_hoconBeanPropertyMeta_lookup() {
		var s = HoconSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getHoconBeanPropertyMeta(bpm));
		assertNotNull(s.getHoconBeanPropertyMeta(null));
	}

	@Test void f03_hoconClassMeta_lookup() {
		var s = HoconSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getHoconClassMeta(cm));
	}
}
