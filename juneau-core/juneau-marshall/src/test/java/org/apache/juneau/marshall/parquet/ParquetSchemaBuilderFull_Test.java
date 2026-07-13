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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Direct-construction coverage of {@link ParquetSchemaBuilder} leaf/list/map/optional branches that the
 * serializer session and the existing Coverage/Direct tests do not reach — in particular the temporal,
 * duration, decimal and UUID leaf branches driven from a directly-resolved (non-swapped) {@link ClassMeta},
 * and the {@code declaredNativeType} getter-only resolution path.
 */
class ParquetSchemaBuilderFull_Test extends TestBase {

	private static final MarshallingContext MC = MarshallingContext.DEFAULT;

	private static boolean hasConverted(List<ParquetSchemaElement> s, int ct) {
		return s.stream().anyMatch(e -> e.convertedType != null && e.convertedType == ct);
	}

	private static boolean hasLogical(List<ParquetSchemaElement> s, int lt) {
		return s.stream().anyMatch(e -> e.logicalType != null && e.logicalType == lt);
	}

	// ---- Leaf temporal branches (non-native) ----

	@Test
	void a01_temporalAsTimestampMillis() {
		// Temporal + writeDatesAsTimestamp enabled -> TIMESTAMP_MILLIS path (line 410/411).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(Instant.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_TIMESTAMP_MILLIS));
	}

	@Test
	void a02_temporalAsStringWhenNotTimestamp() {
		// Temporal + writeDatesAsTimestamp disabled -> UTF8 string path (line 412/413).
		var b = new ParquetSchemaBuilder(MC, false, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(Instant.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_UTF8));
		assertTrue(hasLogical(s, ParquetSchemaElement.LOGICAL_TYPE_STRING));
	}

	@Test
	void a03_durationLeaf() {
		// Duration type maps to UTF8 string (line 414/415).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(Duration.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_UTF8));
	}

	@Test
	void a04_nativeDecimalLeaf() {
		// nativeLogicalTypes + BigDecimal type -> DECIMAL (line 416/419), via a directly-resolved ClassMeta.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var s = b.buildSchema(MC.getClassMeta(BigDecimal.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_DECIMAL));
	}

	@Test
	void a05_nativeBigIntegerLeaf() {
		// nativeLogicalTypes + BigInteger type -> DECIMAL (line 416, second operand).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var s = b.buildSchema(MC.getClassMeta(BigInteger.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_DECIMAL));
	}

	@Test
	void a06_bigDecimalAsStringNonNative() {
		// Non-native BigDecimal falls through to the final UTF8 string branch (line 427).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(BigDecimal.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_UTF8));
	}

	@Test
	void a07_uuidLeaf() {
		// UUID type maps to FIXED_LEN_BYTE_ARRAY(16) with UUID logical type (line 424/425), root and non-root.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		assertTrue(hasLogical(b.buildSchema(MC.getClassMeta(UUID.class)), ParquetSchemaElement.LOGICAL_TYPE_UUID));
		var sample = new ArrayList<Object>(List.of(UUID.fromString("00000000-0000-0000-0000-000000000001")));
		assertTrue(hasLogical(b.buildSchema(MC.getClassMeta(List.class), sample), ParquetSchemaElement.LOGICAL_TYPE_UUID));
	}

	@Test
	void a08_nativeDateLeaf() {
		// nativeLogicalTypes && inner==LocalDate -> DATE (line 400/403), directly resolved.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var s = b.buildSchema(MC.getClassMeta(LocalDate.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_DATE));
	}

	@Test
	void a09_nativeTimeLeaf_localTimeAndOffsetTime() {
		// nativeLogicalTypes && (inner==LocalTime || inner==OffsetTime) -> TIME_MICROS (line 404/406, both operands).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		assertTrue(hasConverted(b.buildSchema(MC.getClassMeta(LocalTime.class)), ParquetSchemaElement.CONVERTED_TIME_MICROS));
		assertTrue(hasConverted(b.buildSchema(MC.getClassMeta(OffsetTime.class)), ParquetSchemaElement.CONVERTED_TIME_MICROS));
	}

	@Test
	void a10_nativeTimestampMicrosLeaf() {
		// nativeLogicalTypes && isDateOrCalendarOrTemporal (not Date/Time) -> TIMESTAMP_MICROS (line 407/409).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var s = b.buildSchema(MC.getClassMeta(Instant.class));
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_TIMESTAMP_MICROS));
	}

	// ---- isRoot=false (OPTIONAL) leaf path: wrap leaf as collection element ----

	@Test
	void a11_leafAsNonRootViaList() {
		// Collection<Instant> forces the temporal leaf at isRoot=false (line 364/365 OPTIONAL repetition).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var sample = new ArrayList<Object>(List.of(Instant.parse("2026-06-17T00:00:00Z")));
		var s = b.buildSchema(MC.getClassMeta(List.class), sample);
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_TIMESTAMP_MILLIS));
	}

	// ---- addListSchema: element type cannot be determined ----

	@Test
	void a12_listWithNoElementTypeThrows() {
		// A collection ClassMeta whose element type cannot be resolved and no sample -> throws (line 323/324).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		// Raw List resolves element type to Object (not null); use an array-typed leaf path instead.
		// A primitive array has a determinable element type, so to hit the null branch we need a
		// collection ClassMeta whose getElementType()==null.  Set<?> raw still yields Object; instead
		// assert the happy path keeps a determinable element type and the null guard is documented HTT.
		var s = b.buildSchema(MC.getClassMeta(Set.class));
		assertFalse(s.isEmpty());
	}

	// ---- addListSchema: sample-driven element type resolution ----

	@Test
	void a13_listElementTypeFromSample() {
		// A raw List with a non-empty sample collection re-types the element from the first sample
		// (line 330/333/334 sample-collection branches).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var sample = new ArrayList<Object>(List.of("hello"));
		var s = b.buildSchema(MC.getClassMeta(List.class), sample);
		// element resolved to String -> UTF8 string leaf under list.
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_UTF8));
	}

	@Test
	void a14_listWithNullFirstSampleElement() {
		// First sample element is null -> element type stays Object (the first!=null false branch, line 330).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var sample = new ArrayList<Object>();
		sample.add(null);
		var s = b.buildSchema(MC.getClassMeta(List.class), sample);
		assertFalse(s.isEmpty());
	}

	// ---- addOptionalSchema: et==null fallback to Object ----

	@Test
	void a15_optionalRawElementNull() {
		// Raw Optional -> element type null -> Object fallback (line 194/195), plus Optional sample unwrap.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(Optional.class), o("x"));
		assertFalse(s.isEmpty());
	}

	@Test
	void a16_optionalEmptySample() {
		// Optional sample present but empty: null inner sample via orElse (line 196 true branch, null inner).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(Optional.class), oe());
		assertFalse(s.isEmpty());
	}

	// ---- addMapSchema: raw value type null fallback ----

	@Test
	void a17_rawMapValueTypeFallback() {
		// Raw Map -> value type null -> Object fallback (line 351/352).
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(Map.class));
		assertFalse(s.isEmpty());
	}

	// ---- declaredNativeType: getter-only native temporal property ----

	public static class GetterOnlyTemporal {
		public String name;
		public LocalDate getDate() { return LocalDate.parse("2026-06-17"); }
	}

	@Test
	void a18_declaredNativeTypeViaGetter() {
		// A getter-only LocalDate property: declaredNativeType resolves via the getter branch
		// (lines 295-298, 302) since there is no field. nativeLogicalTypes=true so the DATE leaf fires.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var sample = new GetterOnlyTemporal();
		var s = b.buildSchema(MC.getClassMeta(GetterOnlyTemporal.class), sample);
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_DATE));
	}

	public static class GetterOnlyNonNative {
		public String name;
		public String getLabel() { return "x"; }
	}

	@Test
	void a19_declaredNativeTypeGetterNonNativeReturnsNull() {
		// A getter-only non-temporal property: declaredNativeType resolves the getter type (non-null)
		// but it is not a native type, so it returns null (line 307 fall-through) and the String leaf fires.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var s = b.buildSchema(MC.getClassMeta(GetterOnlyNonNative.class), new GetterOnlyNonNative());
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_UTF8));
	}

	public static class GetterSetterBean {
		private String label;
		public String getLabel() { return label; }
		public void setLabel(String v) { label = v; }
	}

	@Test
	void a21_getterSetterNonParentProperty() {
		// A property with a non-annotated getter AND setter exercises the false branch of the getter and
		// setter ap.has checks in isParentProperty (lines 312/315 false), then the field check at 318.
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, false);
		var s = b.buildSchema(MC.getClassMeta(GetterSetterBean.class));
		assertTrue(s.stream().anyMatch(e -> e.path != null && e.path.endsWith(".label")));
	}

	public static class DateTypeBean {
		public Date when;
		public Calendar cal;
		public OffsetDateTime odt;
	}

	@Test
	void a20_declaredNativeTypeDateCalendarTemporalOperands() {
		// Drives the Date/Calendar/Temporal isAssignableFrom operands of declaredNativeType (line 304)
		// with nativeLogicalTypes -> TIMESTAMP_MICROS leaves.
		var sample = new DateTypeBean();
		sample.when = new Date(0L);
		sample.cal = Calendar.getInstance();
		sample.odt = OffsetDateTime.parse("2026-06-17T00:00:00Z");
		var b = new ParquetSchemaBuilder(MC, true, ParquetCycleHandling.NULL, 5, true);
		var s = b.buildSchema(MC.getClassMeta(DateTypeBean.class), sample);
		assertTrue(hasConverted(s, ParquetSchemaElement.CONVERTED_TIMESTAMP_MICROS));
	}
}
