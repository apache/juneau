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
package org.apache.juneau.marshall.csv;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S1186" // Empty test method intentional for framework testing
})
class CsvAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// Basic tests
	//------------------------------------------------------------------------------------------------------------------

	Csv a1 = CsvAnnotation.create()
		.description("a")
		.build();

	Csv a2 = CsvAnnotation.create()
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

	@Csv(
		description={ "a" }
	)
	public static class D1 {}
	Csv d1 = D1.class.getAnnotationsByType(Csv.class)[0];

	@Csv(
		description={ "a" }
	)
	public static class D2 {}
	Csv d2 = D2.class.getAnnotationsByType(Csv.class)[0];

	@Test void d01_comparisonWithDeclarativeAnnotations() {
		assertEqualsAll(a1, d1, d2);
		assertNotEqualsAny(a1.hashCode(), 0, -1);
		assertEqualsAll(a1.hashCode(), d1.hashCode(), d2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// CsvApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}
	public static class E04_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(CsvApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(CsvApplyAnnotation.empty(null));
		assertTrue(CsvApplyAnnotation.empty(CsvApplyAnnotation.DEFAULT));
		assertFalse(CsvApplyAnnotation.empty(CsvApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = CsvApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationCreateWithClass() {
		var a = CsvApplyAnnotation.create(E04_Class.class).build();
		assertNotNull(a);
	}

	@Test void e05_applyAnnotationCreateWithString() {
		var a = CsvApplyAnnotation.create("myClass").build();
		assertNotNull(a);
	}

	@Test void e06_applyAnnotationBuilderValue() {
		var csv = CsvAnnotation.create().description("test").build();
		var a = CsvApplyAnnotation.create().value(csv).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// CsvBeanPropertyMeta / CsvClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_csvBeanPropertyMeta_default() {
		assertNotNull(CsvBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_csvBeanPropertyMeta_lookup() {
		var s = CsvSerializer.DEFAULT;
		var bm = MarshallingContext.DEFAULT.getBeanMeta(F02_Bean.class);
		var bpm = bm.getPropertyMeta("name");
		var meta = s.getCsvBeanPropertyMeta(bpm);
		assertNotNull(meta);
	}

	@Test void f03_csvBeanPropertyMeta_lookupNull() {
		var s = CsvSerializer.DEFAULT;
		var meta = s.getCsvBeanPropertyMeta(null);
		assertNotNull(meta);
		assertSame(CsvBeanPropertyMeta.DEFAULT, meta);
	}

	@Test void f04_csvClassMeta_lookup() {
		var s = CsvSerializer.DEFAULT;
		var cm = MarshallingContext.DEFAULT.getClassMeta(F02_Bean.class);
		var meta = s.getCsvClassMeta(cm);
		assertNotNull(meta);
	}

	//------------------------------------------------------------------------------------------------------------------
	// CsvConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@CsvConfig
	public static class G01_Bean {}

	@Test void g01_csvConfigSerializerApply() {
		var s = CsvSerializer.create().applyAnnotations(G01_Bean.class).build();
		assertNotNull(s);
	}

	@CsvConfig(rank = 1)
	public static class G02_Bean {}

	@Test void g02_csvConfigSerializerApplyWithRank() {
		var s = CsvSerializer.create().applyAnnotations(G02_Bean.class).build();
		assertNotNull(s);
	}

	@CsvConfig
	public static class G03_Bean {}

	@Test void g03_csvConfigParserApply() {
		var p = CsvParser.create().applyAnnotations(G03_Bean.class).build();
		assertNotNull(p);
	}
}
