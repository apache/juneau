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
package org.apache.juneau.marshall.protobuf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-targeted tests for the protobuf binary codec (wire-type enum, error paths, builders, annotations).
 */
@SuppressWarnings({
	"resource" // ProtobufWriter wraps a caller-owned OutputStream and its inherited close() is a no-op; Eclipse JDT flags the locally-created writers as unclosed, but there is no real resource to release.
})
class ProtobufCoverage_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// WireType
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void w01_fromCodeAll() {
		assertEquals(WireType.VARINT, WireType.fromCode(0));
		assertEquals(WireType.I64, WireType.fromCode(1));
		assertEquals(WireType.LEN, WireType.fromCode(2));
		assertEquals(WireType.SGROUP, WireType.fromCode(3));
		assertEquals(WireType.EGROUP, WireType.fromCode(4));
		assertEquals(WireType.I32, WireType.fromCode(5));
	}

	@Test
	void w02_fromCodeInvalid() {
		assertThrows(IllegalArgumentException.class, () -> WireType.fromCode(7));
	}

	@Test
	void w03_fromTag() {
		assertEquals(WireType.VARINT, WireType.fromTag(0x08));  // field1, varint
		assertEquals(WireType.LEN, WireType.fromTag(0x12));     // field2, len
		assertEquals(WireType.I32, WireType.fromTag(0x0D));     // field1, i32
		assertEquals(2, WireType.LEN.code());
	}

	//------------------------------------------------------------------------------------------------------------------
	// ProtobufWriter error paths
	//------------------------------------------------------------------------------------------------------------------

	private static class FailingOutputStream extends OutputStream {
		@Override public void write(int b) throws IOException { throw new IOException("boom"); }
		@Override public void write(byte[] b, int off, int len) throws IOException { throw new IOException("boom"); }
		@Override public void flush() throws IOException { throw new IOException("boom"); }
	}

	@Test
	void x01_writerWriteError() {
		var w = new ProtobufWriter(new FailingOutputStream());
		assertThrows(SerializeException.class, () -> w.writeVarint(1));
	}

	@Test
	void x02_writerBulkWriteError() {
		var w = new ProtobufWriter(new FailingOutputStream());
		assertThrows(SerializeException.class, () -> w.writeLenDelimited(new byte[]{1, 2, 3}));
	}

	@Test
	void x03_writerFlushError() {
		var w = new ProtobufWriter(new FailingOutputStream());
		assertThrows(SerializeException.class, w::flush);
	}

	//------------------------------------------------------------------------------------------------------------------
	// ProtobufReader error paths
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void y01_readVarintEof() {
		assertThrows(IOException.class, () -> new ProtobufReader(new byte[0]).readVarint());
	}

	@Test
	void y02_readTagTruncated() {
		// 0x80 = continuation bit set but no following byte.
		assertThrows(IOException.class, () -> new ProtobufReader(new byte[]{(byte)0x80}).readTag());
	}

	@Test
	void y03_readLenDelimitedTruncated() {
		// len 5 but only 1 byte available.
		assertThrows(IOException.class, () -> new ProtobufReader(new byte[]{0x05, 0x01}).readLenDelimited());
	}

	@Test
	void y04_readFixedEof() {
		assertThrows(IOException.class, () -> new ProtobufReader(new byte[]{0x01}).readFixed32());
		assertThrows(IOException.class, () -> new ProtobufReader(new byte[]{0x01}).readFixed64());
	}

	@Test
	void y05_skipUnsupportedWireType() {
		assertThrows(IOException.class, () -> new ProtobufReader(new byte[]{0x00}).skipField(WireType.SGROUP));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Builder copy + properties
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void z01_serializerCopy() {
		var s = ProtobufSerializer.create().addBeanTypesProtobuf().nativeTypes().build();
		var s2 = s.copy().build();
		assertNotNull(s2);
		assertNotNull(s.getSession());
	}

	@Test
	void z02_parserCopy() {
		var p = ProtobufParser.create().nativeTypes().build();
		var p2 = p.copy().build();
		assertNotNull(p2);
		assertNotNull(p.getSession());
	}

	@Test
	void z03_serializerBuilderCopy() {
		var b = ProtobufSerializer.create().addBeanTypesProtobuf();
		var b2 = b.copy();
		assertNotNull(b2.build());
	}

	@Test
	void z04_parserBuilderCopy() {
		var b = ProtobufParser.create().nativeTypes(false);
		var b2 = b.copy();
		assertNotNull(b2.build());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Annotation builders
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void a01_protobufAnnotationGetters() {
		var a = ProtobufAnnotation.create().fieldNumber(7).type(ProtobufScalarType.FIXED64).description("a", "b").build();
		assertEquals(7, a.fieldNumber());
		assertEquals(ProtobufScalarType.FIXED64, a.type());
		assertArrayEquals(new String[]{"a", "b"}, a.description());
	}

	public static class E04 {}

	@Test
	void a02_applyAnnotation() {
		assertNotNull(ProtobufApplyAnnotation.DEFAULT);
		assertTrue(ProtobufApplyAnnotation.empty(null));
		assertTrue(ProtobufApplyAnnotation.empty(ProtobufApplyAnnotation.DEFAULT));
		assertFalse(ProtobufApplyAnnotation.empty(ProtobufApplyAnnotation.create(E04.class).build()));
		assertNotNull(ProtobufApplyAnnotation.create("myClass").build());
		var pb = ProtobufAnnotation.create().fieldNumber(2).build();
		assertNotNull(ProtobufApplyAnnotation.create().value(pb).onClass(E04.class).build());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Scalar-type variety round-trips (covers encode/decode switch arms)
	//------------------------------------------------------------------------------------------------------------------

	public static class ScalarTypes {
		@Protobuf(type=ProtobufScalarType.FIXED32) public int f32;
		@Protobuf(type=ProtobufScalarType.FIXED64) public long f64;
		@Protobuf(type=ProtobufScalarType.SFIXED32) public int sf32;
		@Protobuf(type=ProtobufScalarType.SFIXED64) public long sf64;
		@Protobuf(type=ProtobufScalarType.UINT32) public int u32;
		@Protobuf(type=ProtobufScalarType.INT64) public long i64;
		public ScalarTypes() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void b01_scalarTypeVarietyRoundTrip() throws Exception {
		var a = new ScalarTypes();
		a.f32 = 123456; a.f64 = 9_999_999_999L; a.sf32 = -42; a.sf64 = -9_999_999_999L; a.u32 = (int)4_000_000_000L; a.i64 = -5;
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var b = ProtobufParser.DEFAULT.read(bytes, ScalarTypes.class);
		assertEquals(123456, b.f32);
		assertEquals(9_999_999_999L, b.f64);
		assertEquals(-42, b.sf32);
		assertEquals(-9_999_999_999L, b.sf64);
		assertEquals((int)4_000_000_000L, b.u32);
		assertEquals(-5, b.i64);
	}

	public static class DateBean {
		public java.util.Date date;
		public DateBean() {}
		public DateBean(java.util.Date date) { this.date = date; }
	}

	@Test
	void b02_dateRoundTrip() throws Exception {
		var d = new java.util.Date(1_700_000_000_000L);
		var bytes = ProtobufSerializer.DEFAULT.write(new DateBean(d));
		var b = ProtobufParser.DEFAULT.read(bytes, DateBean.class);
		assertEquals(d, b.date);
	}

	@ProtobufApply(on="org.apache.juneau.marshall.protobuf.ProtobufCoverage_Test$ApplyTarget", value=@Protobuf(fieldNumber=3))
	public static class ApplyConfig {}

	public static class ApplyTarget { public int x; }

	@Test
	void a03_applyAnnotationFlowsToContext() {
		var s = ProtobufSerializer.create().applyAnnotations(ApplyConfig.class).build();
		assertNotNull(s);
	}

	public static class A04_C1 { public int f1; public void m1() { /* No-op method used only as a reflection target in a04_applyTargetingMethods. */ } }
	public static class A04_C2 { public int f2; public void m2() { /* No-op method used only as a reflection target in a04_applyTargetingMethods. */ } }

	@Test
	void a04_applyTargetingMethods() throws Exception {
		var c1 = ProtobufApplyAnnotation.create(A04_C1.class).on(A04_C2.class).build();
		var c2 = ProtobufApplyAnnotation.create("a").on("b").build();
		var c3 = ProtobufApplyAnnotation.create().on(A04_C1.class.getField("f1")).on(A04_C2.class.getField("f2")).build();
		var c4 = ProtobufApplyAnnotation.create().on(A04_C1.class.getMethod("m1")).on(A04_C2.class.getMethod("m2")).build();
		assertNotNull(c1);
		assertNotNull(c2);
		assertNotNull(c3);
		assertNotNull(c4);
	}

	@Test
	void a05_applyDynamicViaContext() {
		var pb = ProtobufAnnotation.create().fieldNumber(5).build();
		// on non-empty => first isEmptyArray false branch + b.annotations() path.
		assertNotNull(MarshallingContext.create().annotations(ProtobufApplyAnnotation.create().on("x").value(pb).build()).build());
		// onClass-only => first isEmptyArray true, second false.
		assertNotNull(MarshallingContext.create().annotations(ProtobufApplyAnnotation.create().onClass(E04.class).value(pb).build()).build());
		// both empty => early return.
		assertNotNull(MarshallingContext.create().annotations(ProtobufApplyAnnotation.DEFAULT).build());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Temporal / date-time string forms
	//------------------------------------------------------------------------------------------------------------------

	public static class Temporals {
		public java.util.Calendar cal;
		public Duration dur;
		public LocalDate ld;
		public Period per;
		public Temporals() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void c01_temporalsRoundTrip() throws Exception {
		var a = new Temporals();
		a.cal = java.util.GregorianCalendar.from(ZonedDateTime.parse("2024-01-02T03:04:05Z"));
		a.dur = Duration.ofSeconds(90);
		a.ld = LocalDate.of(2024, Month.JANUARY, 2);
		a.per = Period.of(1, 2, 3);
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var b = ProtobufParser.DEFAULT.read(bytes, Temporals.class);
		assertEquals(a.dur, b.dur);
		assertEquals(a.ld, b.ld);
		assertEquals(a.per, b.per);
		assertEquals(a.cal.toInstant(), b.cal.toInstant());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Map with bean values
	//------------------------------------------------------------------------------------------------------------------

	public static class Holder {
		public java.util.Map<String,ProtobufContainers_Test.Inner> m;
		public Holder() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void c02_mapBeanValueRoundTrip() throws Exception {
		var a = new Holder();
		a.m = new java.util.LinkedHashMap<>();
		a.m.put("k", new ProtobufContainers_Test.Inner(99));
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var b = ProtobufParser.DEFAULT.read(bytes, Holder.class);
		assertEquals(99, b.m.get("k").id);
	}

	// Temporal values carried as map values flow un-swapped, exercising the date/calendar/temporal/duration/period
	// scalar string paths in both the serializer and parser sessions (bean-property values are swapped by BeanMap).
	public static class C03_TemporalMaps {
		public java.util.Map<String,java.util.Date> dates;
		public java.util.Map<String,java.util.Calendar> cals;
		public java.util.Map<String,LocalDate> lds;
		public java.util.Map<String,Duration> durs;
		public java.util.Map<String,Period> pers;
		public C03_TemporalMaps() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void c03_temporalMapValuesRoundTrip() throws Exception {
		var a = new C03_TemporalMaps();
		var d = new java.util.Date(1_700_000_000_000L);
		var cal = java.util.GregorianCalendar.from(ZonedDateTime.parse("2024-01-02T03:04:05Z"));
		a.dates = java.util.Map.of("k", d);
		a.cals = java.util.Map.of("k", cal);
		a.lds = java.util.Map.of("k", LocalDate.of(2024, Month.JANUARY, 2));
		a.durs = java.util.Map.of("k", Duration.ofSeconds(90));
		a.pers = java.util.Map.of("k", Period.of(1, 2, 3));
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var b = ProtobufParser.DEFAULT.read(bytes, C03_TemporalMaps.class);
		assertEquals(d, b.dates.get("k"));
		assertEquals(cal.toInstant(), b.cals.get("k").toInstant());
		assertEquals(LocalDate.of(2024, Month.JANUARY, 2), b.lds.get("k"));
		assertEquals(Duration.ofSeconds(90), b.durs.get("k"));
		assertEquals(Period.of(1, 2, 3), b.pers.get("k"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Error / presence paths
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void d01_rawMapRootRejected() {
		var m = new java.util.LinkedHashMap<String,Object>();
		m.put("a", 1);
		assertThrows(SerializeException.class, () -> ProtobufSerializer.DEFAULT.write(m));
	}

	public static class Cycle {
		public String name;
		public Cycle self;
		public Cycle() { /* Public no-arg constructor required for Juneau bean recognition. */ }
	}

	@Test
	void d02_recursionGuardSkipsCycle() throws Exception {
		var c = new Cycle();
		c.name = "x";
		c.self = c;
		var s = ProtobufSerializer.create().detectRecursions().ignoreRecursions().build();
		var bytes = s.write(c);
		assertNotNull(bytes);
	}

	@Test
	void d03_addBeanTypesSerializeStillWorks() throws Exception {
		var s = ProtobufSerializer.create().addBeanTypesProtobuf().build();
		var bytes = s.write(new ProtobufContainers_Test.Inner(7));
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Meta-provider null paths, properties()/toString(), and session creation
	//------------------------------------------------------------------------------------------------------------------

	@Test
	void f01_metaProviderNullPaths() {
		assertNotNull(ProtobufSerializer.DEFAULT.getProtobufBeanPropertyMeta(null));
		assertNotNull(ProtobufParser.DEFAULT.getProtobufBeanPropertyMeta(null));
		var cm = ProtobufSerializer.DEFAULT.getMarshallingContext().getClassMeta(ProtobufContainers_Test.Inner.class);
		assertNotNull(ProtobufSerializer.DEFAULT.getProtobufClassMeta(cm));
		assertNotNull(ProtobufParser.DEFAULT.getProtobufClassMeta(cm));
	}

	@Test
	void f02_propertiesAndSession() {
		assertNotNull(ProtobufSerializer.DEFAULT.toString());
		assertNotNull(ProtobufParser.DEFAULT.toString());
		assertNotNull(ProtobufSerializer.DEFAULT.getSession());
		assertNotNull(ProtobufParser.DEFAULT.getSession());
	}

}
