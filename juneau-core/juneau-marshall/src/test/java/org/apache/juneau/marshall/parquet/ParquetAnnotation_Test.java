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
package org.apache.juneau.marshall.parquet;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class ParquetAnnotation_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Default value
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_defaultValue() {
		var a = ParquetAnnotation.DEFAULT;
		assertNotNull(a);
		assertEquals("", a.parquetType());
		assertEquals("", a.logicalType());
	}

	@Test void a02_defaultEquality() {
		var a1 = ParquetAnnotation.DEFAULT;
		var a2 = ParquetAnnotation.DEFAULT;
		assertEquals(a1, a2);
		assertEquals(a1.hashCode(), a2.hashCode());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations — default values.
	//------------------------------------------------------------------------------------------------------------------

	@Parquet
	public static class D1 {}

	@Parquet
	public static class D2 {}

	@Test void d01_defaultDeclarativeAnnotations() {
		var d1 = D1.class.getAnnotationsByType(Parquet.class)[0];
		var d2 = D2.class.getAnnotationsByType(Parquet.class)[0];
		assertEquals("", d1.parquetType());
		assertEquals("", d1.logicalType());
		assertEquals(d1, d2);
		assertEquals(d1.hashCode(), d2.hashCode());
	}

	@Test void d02_defaultEqualsDeclarative() {
		var d1 = D1.class.getAnnotationsByType(Parquet.class)[0];
		assertEquals(ParquetAnnotation.DEFAULT, d1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Comparison with declared annotations — explicit values.
	//------------------------------------------------------------------------------------------------------------------

	@Parquet(parquetType="BYTE_ARRAY", logicalType="STRING")
	public static class D3 {}

	@Parquet(parquetType="BYTE_ARRAY", logicalType="STRING")
	public static class D4 {}

	@Test void d03_explicitValues() {
		var d3 = D3.class.getAnnotationsByType(Parquet.class)[0];
		assertEquals("BYTE_ARRAY", d3.parquetType());
		assertEquals("STRING", d3.logicalType());
	}

	@Test void d04_explicitEquality() {
		var d3 = D3.class.getAnnotationsByType(Parquet.class)[0];
		var d4 = D4.class.getAnnotationsByType(Parquet.class)[0];
		assertEquals(d3, d4);
		assertEquals(d3.hashCode(), d4.hashCode());
	}

	@Test void d05_explicitNotEqualDefault() {
		var d3 = D3.class.getAnnotationsByType(Parquet.class)[0];
		assertNotEquals(ParquetAnnotation.DEFAULT, d3);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ParquetApplyAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class E02_Class {}

	@Test void e01_applyAnnotationDefault() {
		assertNotNull(ParquetApplyAnnotation.DEFAULT);
	}

	@Test void e02_applyAnnotationEmpty() {
		assertTrue(ParquetApplyAnnotation.empty(null));
		assertTrue(ParquetApplyAnnotation.empty(ParquetApplyAnnotation.DEFAULT));
		assertFalse(ParquetApplyAnnotation.empty(ParquetApplyAnnotation.create(E02_Class.class).build()));
	}

	@Test void e03_applyAnnotationCreate() {
		var a = ParquetApplyAnnotation.create().build();
		assertNotNull(a);
	}

	@Test void e04_applyAnnotationBuilderValue() {
		var a = ParquetApplyAnnotation.create().value(ParquetAnnotation.DEFAULT).build();
		assertNotNull(a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ParquetConfigAnnotation tests.
	//------------------------------------------------------------------------------------------------------------------

	@ParquetConfig(compressionCodec = "GZIP", rowGroupSize = "1048576", pageSize = "65536")
	public static class G01_Class {}

	@Test void g01_parquetConfigSerializerApply() {
		var s = ParquetSerializer.create().applyAnnotations(G01_Class.class).build();
		assertNotNull(s);
	}

	@ParquetConfig(addBeanTypes = "true")
	public static class G02_Class {}

	@Test void g02_parquetConfigAddBeanTypes() {
		var s = ParquetSerializer.create().applyAnnotations(G02_Class.class).build();
		assertNotNull(s);
	}

	@ParquetConfig
	public static class G03_Class {}

	@Test void g03_parquetConfigParserApply() {
		var p = ParquetParser.create().applyAnnotations(G03_Class.class).build();
		assertNotNull(p);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ParquetBeanPropertyMeta + ParquetClassMeta tests.
	//------------------------------------------------------------------------------------------------------------------

	public static class F02_Bean { public String name; }

	@Test void f01_parquetBeanPropertyMeta_default() {
		assertNotNull(ParquetBeanPropertyMeta.DEFAULT);
	}

	@Test void f02_parquetBeanPropertyMeta_lookup() {
		var s = ParquetSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var bm = bc.getBeanMeta(F02_Bean.class);
		assertNotNull(bm);
		var bpm = bm.getPropertyMeta("name");
		assertNotNull(bpm);
		assertNotNull(s.getParquetBeanPropertyMeta(bpm));
		assertNotNull(s.getParquetBeanPropertyMeta(null));
	}

	@Test void f03_parquetClassMeta_lookup() {
		var s = ParquetSerializer.DEFAULT;
		var bc = MarshallingContext.DEFAULT;
		var cm = bc.getClassMeta(F02_Bean.class);
		assertNotNull(s.getParquetClassMeta(cm));
	}
}
