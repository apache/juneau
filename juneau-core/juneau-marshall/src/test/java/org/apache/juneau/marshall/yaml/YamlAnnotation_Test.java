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
package org.apache.juneau.marshall.yaml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class YamlAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Default value
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultValue() {
		var a = YamlAnnotation.DEFAULT;
		assertNotNull(a);
	}

	@Test void a02_defaultEquality() {
		var a1 = YamlAnnotation.DEFAULT;
		var a2 = YamlAnnotation.DEFAULT;
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations.
	//------------------------------------------------------------------------------------------------------------------

	@Yaml
	public static class D1 {}

	@Yaml
	public static class D2 {}

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		var d1 = D1.class.getAnnotationsByType(Yaml.class)[0];
		var d2 = D2.class.getAnnotationsByType(Yaml.class)[0];
		assertEquals(d1, d2);
		assertEquals(d1.hashCode(), d2.hashCode());
	}

	@Test void d02_defaultEqualsDeclarative() {
		var d1 = D1.class.getAnnotationsByType(Yaml.class)[0];
		assertEquals(YamlAnnotation.DEFAULT, d1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// YamlApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(YamlApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(YamlApplyAnnotation.empty(null));
		assertTrue(YamlApplyAnnotation.empty(YamlApplyAnnotation.DEFAULT));
		assertFalse(YamlApplyAnnotation.empty(YamlApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = YamlApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationBuilderValue() {
		var a = YamlApplyAnnotation.create().value(YamlAnnotation.DEFAULT).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// YamlBeanPropertyMeta + YamlClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_yamlBeanPropertyMeta_default() {
		assertNotNull(YamlBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_yamlBeanPropertyMeta_lookup() {
		var s = YamlSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getYamlBeanPropertyMeta(bpm));
		assertNotNull(s.getYamlBeanPropertyMeta(null));
	}

	@Test void f03_yamlClassMeta_lookup() {
		var s = YamlSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getYamlClassMeta(cm));
	}
}
