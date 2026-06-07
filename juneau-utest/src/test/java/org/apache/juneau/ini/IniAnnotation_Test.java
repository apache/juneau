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
package org.apache.juneau.ini;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class IniAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Default value
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultValue() {
		var a = IniAnnotation.DEFAULT;
		assertNotNull(a);
		assertEquals("", a.section());
		assertEquals("", a.comment());
		assertFalse(a.json5Encoding());
	}

	@Test void a02_defaultEquality() {
		var a1 = IniAnnotation.DEFAULT;
		var a2 = IniAnnotation.DEFAULT;
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations — default values.
	//------------------------------------------------------------------------------------------------------------------

	@Ini
	public static class D1 {}

	@Ini
	public static class D2 {}

	@Test void d01_defaultDeclarativeAnnotations() {
		var d1 = D1.class.getAnnotationsByType(Ini.class)[0];
		var d2 = D2.class.getAnnotationsByType(Ini.class)[0];
		assertEquals("", d1.section());
		assertEquals("", d1.comment());
		assertFalse(d1.json5Encoding());
		assertEquals(d1, d2);
		assertEquals(d1.hashCode(), d2.hashCode());
	}

	@Test void d02_defaultEqualsDeclarative() {
		var d1 = D1.class.getAnnotationsByType(Ini.class)[0];
		assertEquals(IniAnnotation.DEFAULT, d1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations — explicit values.
	//------------------------------------------------------------------------------------------------------------------

	@Ini(section="mySection", comment="# my comment", json5Encoding=true)
	public static class D3 {}

	@Ini(section="mySection", comment="# my comment", json5Encoding=true)
	public static class D4 {}

	@Test void d03_explicitValues() {
		var d3 = D3.class.getAnnotationsByType(Ini.class)[0];
		assertEquals("mySection", d3.section());
		assertEquals("# my comment", d3.comment());
		assertTrue(d3.json5Encoding());
	}

	@Test void d04_explicitEquality() {
		var d3 = D3.class.getAnnotationsByType(Ini.class)[0];
		var d4 = D4.class.getAnnotationsByType(Ini.class)[0];
		assertEquals(d3, d4);
		assertEquals(d3.hashCode(), d4.hashCode());
	}

	@Test void d05_explicitNotEqualDefault() {
		var d3 = D3.class.getAnnotationsByType(Ini.class)[0];
		assertNotEquals(IniAnnotation.DEFAULT, d3);
	}

	//------------------------------------------------------------------------------------------------------------------
	// IniApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(IniApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(IniApplyAnnotation.empty(null));
		assertTrue(IniApplyAnnotation.empty(IniApplyAnnotation.DEFAULT));
		assertFalse(IniApplyAnnotation.empty(IniApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = IniApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationBuilderValue() {
		var a = IniApplyAnnotation.create().value(IniAnnotation.DEFAULT).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// IniConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@IniConfig(kvSeparator = "=", spacedSeparator = "true")
	public static class G01_Class {}

	@Test void g01_iniConfigSerializerApply() {
		var s = IniSerializer.create().applyAnnotations(G01_Class.class).build();
		assertNotNull(s);
	}

	@IniConfig
	public static class G02_Class {}

	@Test void g02_iniConfigParserApply() {
		var p = IniParser.create().applyAnnotations(G02_Class.class).build();
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// IniBeanPropertyMeta + IniClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Ini(section="mySection")
	public static class F03_Bean {
		@Ini(section="propSection", comment="# my prop", json5Encoding=true)
		public String name;
	}

	public static class F04_Bean {
		@Ini
		public String name;
	}

	@Test void f01_iniBeanPropertyMeta_default() {
		assertNotNull(IniBeanPropertyMeta.DEFAULT);
		assertEquals("", IniBeanPropertyMeta.DEFAULT.getSection());
		assertEquals("", IniBeanPropertyMeta.DEFAULT.getComment());
		assertFalse(IniBeanPropertyMeta.DEFAULT.isJson5Encoding());
	}

	@Test void f02_iniBeanPropertyMeta_lookup() {
		var s = IniSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getIniBeanPropertyMeta(bpm));
		assertNotNull(s.getIniBeanPropertyMeta(null));
	}

	@Test void f03_iniBeanPropertyMeta_withAnnotation() {
		var s = IniSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F03_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		var meta = s.getIniBeanPropertyMeta(bpm);
		assertNotNull(meta);
		assertEquals("propSection", meta.getSection());
		assertEquals("# my prop", meta.getComment());
		assertTrue(meta.isJson5Encoding());
	}

	@Test void f04_iniBeanPropertyMeta_emptyAnnotationValues() {
		var s = IniSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F04_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		var meta = s.getIniBeanPropertyMeta(bpm);
		assertNotNull(meta);
		assertEquals("", meta.getSection());
		assertEquals("", meta.getComment());
		assertFalse(meta.isJson5Encoding());
	}

	@Test void f05_iniClassMeta_lookup() {
		var s = IniSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F02_Bean.class);
		var meta = s.getIniClassMeta(cm);
		assertNotNull(meta);
		assertEquals("", meta.getSection());
	}

	@Test void f06_iniClassMeta_withAnnotation() {
		var s = IniSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F03_Bean.class);
		var meta = s.getIniClassMeta(cm);
		assertNotNull(meta);
		assertEquals("mySection", meta.getSection());
	}

	@Ini
	public static class F07_Bean {}

	@Test void f07_iniClassMeta_withEmptyAnnotation() {
		var s = IniSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F07_Bean.class);
		var meta = s.getIniClassMeta(cm);
		assertNotNull(meta);
		assertEquals("", meta.getSection());
	}
}
