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
package org.apache.juneau.marshall.toml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class TomlAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Default value
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultValue() {
		var a = TomlAnnotation.DEFAULT;
		assertNotNull(a);
	}

	@Test void a02_defaultEquality() {
		var a1 = TomlAnnotation.DEFAULT;
		var a2 = TomlAnnotation.DEFAULT;
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Toml
	public static class D1 {}

	@Toml
	public static class D2 {}

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		var d1 = D1.class.getAnnotationsByType(Toml.class)[0];
		var d2 = D2.class.getAnnotationsByType(Toml.class)[0];
		assertEquals(d1, d2);
		assertEquals(d1.hashCode(), d2.hashCode());
	}

	@Test void d02_defaultEqualsDeclarative() {
		var d1 = D1.class.getAnnotationsByType(Toml.class)[0];
		assertEquals(TomlAnnotation.DEFAULT, d1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// TomlApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(TomlApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(TomlApplyAnnotation.empty(null));
		assertTrue(TomlApplyAnnotation.empty(TomlApplyAnnotation.DEFAULT));
		assertFalse(TomlApplyAnnotation.empty(TomlApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = TomlApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationBuilderValue() {
		var a = TomlApplyAnnotation.create().value(TomlAnnotation.DEFAULT).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// TomlBeanPropertyMeta + TomlClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_tomlBeanPropertyMeta_default() {
		assertNotNull(TomlBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_tomlBeanPropertyMeta_lookup() {
		var s = TomlSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getTomlBeanPropertyMeta(bpm));
		assertNotNull(s.getTomlBeanPropertyMeta(null));
	}

	@Test void f03_tomlClassMeta_lookup() {
		var s = TomlSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getTomlClassMeta(cm));
	}
}
