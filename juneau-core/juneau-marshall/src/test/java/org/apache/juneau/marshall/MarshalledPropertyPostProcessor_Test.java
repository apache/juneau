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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.time.Duration;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.swap.*;
import org.junit.jupiter.api.*;

/**
 * Branch-coverage closure tests for {@link MarshalledPropertyPostProcessor} dispatch paths.
 *
 * <p>
 * Targets short-circuit arms in {@code applyPropertyFormats} / {@code applyClassFormats} (a typed
 * field with {@code @MarshalledProp} / {@code @Marshalled} default = NOT_SET), edge cases in the
 * XMLGregorianCalendar swap, the Bug #18 {@code temporalAccessorSwap} MILLIS branch, and other
 * swap-internal arms the cross-serializer/parser matrix doesn't reach because it always supplies a
 * non-NOT_SET context format and never feeds null / blank values through swap.unswap.
 *
 * <p>
 * Lives in {@code org.apache.juneau.marshall} so it can use package-private MPP internals if needed.
 */
@SuppressWarnings({
	"java:S1186", // Annotation-presence-only fixtures don't need test body wiring beyond round-trip.
	"java:S2699" // Several tests assert through the roundTripDefault helper (and the l01 no-op guard) that Sonar can't see as assertions.
})
class MarshalledPropertyPostProcessor_Test {

	//------------------------------------------------------------------------------------------------------------------
	// Group A — @MarshalledProp default (all formats NOT_SET) on each typed field.
	// Each bean below carries @MarshalledProp with no explicit format setter, so every per-format
	// "Type-matches && format != NOT_SET" check in MarshalledPropertyPostProcessor.applyPropertyFormats
	// (lines ~425-455) short-circuits on the second conjunct.  applyContextFormats then installs the
	// context-level swap (or fall-through) so the round-trip is semantically a no-op.
	//------------------------------------------------------------------------------------------------------------------

	public static class A_Duration   { @MarshalledProp public Duration       f; }
	public static class A_Period     { @MarshalledProp public Period         f; }
	public static class A_Calendar   { @MarshalledProp public Calendar       f; }
	public static class A_Date       { @MarshalledProp public Date           f; }
	public static class A_Instant    { @MarshalledProp public Instant        f; }
	public static class A_ZoneId     { @MarshalledProp public ZoneId         f; }
	public static class A_TimeZone   { @MarshalledProp public TimeZone       f; }
	public static class A_Locale     { @MarshalledProp public Locale         f; }
	public static class A_ByteArray  { @MarshalledProp public byte[]         f; }
	public enum AEnum { X, Y }
	public static class A_Enum       { @MarshalledProp public AEnum          f; }
	public static class A_Uuid       { @MarshalledProp public UUID           f; }
	public static class A_BigInteger { @MarshalledProp public BigInteger     f; }
	public static class A_BigDecimal { @MarshalledProp public BigDecimal     f; }
	public static class A_Boolean    { @MarshalledProp public Boolean        f; }
	public static class A_Float      { @MarshalledProp public Float          f; }
	public static class A_Double     { @MarshalledProp public Double         f; }
	public static class A_Currency   { @MarshalledProp public Currency       f; }
	public static class A_Class      { @MarshalledProp public Class<?>       f; }
	public static class A_MonthDay   { @MarshalledProp public MonthDay       f; }

	@Test void a01_marshalledProp_notSetShortCircuit_eachTypedField() {
		roundTripDefault(new A_Duration(),   "f", Duration.ofHours(1));
		roundTripDefault(new A_Period(),     "f", Period.ofDays(3));
		roundTripDefault(new A_Date(),       "f", new Date(0L));
		roundTripDefault(new A_Instant(),    "f", Instant.parse("2026-05-22T12:00:00Z"));
		roundTripDefault(new A_ZoneId(),     "f", ZoneId.of("UTC"));
		roundTripDefault(new A_TimeZone(),   "f", TimeZone.getTimeZone("UTC"));
		roundTripDefault(new A_Locale(),     "f", Locale.US);
		roundTripDefault(new A_ByteArray(),  "f", new byte[] { 1, 2, 3 });
		roundTripDefault(new A_Enum(),       "f", AEnum.X);
		roundTripDefault(new A_Uuid(),       "f", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
		roundTripDefault(new A_BigInteger(), "f", BigInteger.valueOf(42));
		roundTripDefault(new A_BigDecimal(), "f", new BigDecimal("1.5"));
		roundTripDefault(new A_Boolean(),    "f", Boolean.TRUE);
		roundTripDefault(new A_Float(),      "f", 1.5f);
		roundTripDefault(new A_Double(),     "f", 1.5d);
		roundTripDefault(new A_Currency(),   "f", Currency.getInstance("USD"));
		roundTripDefault(new A_Class(),      "f", String.class);
		roundTripDefault(new A_MonthDay(),   "f", MonthDay.of(6, 15));
	}

	@Test void a02_marshalledProp_notSetShortCircuit_calendarField() {
		var c = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		c.setTimeInMillis(0L);
		var bean = new A_Calendar();
		bean.f = c;
		// Calendar isn't directly round-trippable in Json5 without a swap; just verify serialize doesn't throw.
		assertNotNull(Json5Serializer.DEFAULT.serialize(bean));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group B — @Marshalled at class level (all formats NOT_SET).
	// Exercises applyClassFormats short-circuits at lines ~465-495.
	//------------------------------------------------------------------------------------------------------------------

	@Marshalled public static class B_Duration   { public Duration       f; }
	@Marshalled public static class B_Period     { public Period         f; }
	@Marshalled public static class B_Date       { public Date           f; }
	@Marshalled public static class B_Instant    { public Instant        f; }
	@Marshalled public static class B_ZoneId     { public ZoneId         f; }
	@Marshalled public static class B_TimeZone   { public TimeZone       f; }
	@Marshalled public static class B_Locale     { public Locale         f; }
	@Marshalled public static class B_ByteArray  { public byte[]         f; }
	@Marshalled public static class B_Enum       { public AEnum          f; }
	@Marshalled public static class B_Uuid       { public UUID           f; }
	@Marshalled public static class B_BigInteger { public BigInteger     f; }
	@Marshalled public static class B_BigDecimal { public BigDecimal     f; }
	@Marshalled public static class B_Boolean    { public Boolean        f; }
	@Marshalled public static class B_Float      { public Float          f; }
	@Marshalled public static class B_Double     { public Double         f; }
	@Marshalled public static class B_Currency   { public Currency       f; }
	@Marshalled public static class B_Class      { public Class<?>       f; }
	@Marshalled public static class B_MonthDay   { public MonthDay       f; }

	@Test void b01_marshalled_notSetShortCircuit_eachTypedField() {
		roundTripDefault(new B_Duration(),   "f", Duration.ofHours(1));
		roundTripDefault(new B_Period(),     "f", Period.ofDays(3));
		roundTripDefault(new B_Date(),       "f", new Date(0L));
		roundTripDefault(new B_Instant(),    "f", Instant.parse("2026-05-22T12:00:00Z"));
		roundTripDefault(new B_ZoneId(),     "f", ZoneId.of("UTC"));
		roundTripDefault(new B_TimeZone(),   "f", TimeZone.getTimeZone("UTC"));
		roundTripDefault(new B_Locale(),     "f", Locale.US);
		roundTripDefault(new B_ByteArray(),  "f", new byte[] { 1, 2, 3 });
		roundTripDefault(new B_Enum(),       "f", AEnum.X);
		roundTripDefault(new B_Uuid(),       "f", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
		roundTripDefault(new B_BigInteger(), "f", BigInteger.valueOf(42));
		roundTripDefault(new B_BigDecimal(), "f", new BigDecimal("1.5"));
		roundTripDefault(new B_Boolean(),    "f", Boolean.TRUE);
		roundTripDefault(new B_Float(),      "f", 1.5f);
		roundTripDefault(new B_Double(),     "f", 1.5d);
		roundTripDefault(new B_Currency(),   "f", Currency.getInstance("USD"));
		roundTripDefault(new B_Class(),      "f", String.class);
		roundTripDefault(new B_MonthDay(),   "f", MonthDay.of(6, 15));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group C — XMLGregorianCalendar swap body edge cases.
	// Exercises the null/empty arms of the lazy DatatypeFactory.newXMLGregorianCalendar in
	// MarshalledPropertyPostProcessor.xmlGregorianCalendarSwap (lines ~1062-1070), which the
	// matrix's a01 (basic) tests never reach because they always seed a non-null value.
	//------------------------------------------------------------------------------------------------------------------

	public static class C_XmlGregorianCalendar { public XMLGregorianCalendar f; }

	@Test void c01_xmlGregorianCalendar_nullValue_serialize() {
		var bean = new C_XmlGregorianCalendar();
		bean.f = null;
		var s = Json5Serializer.DEFAULT.serialize(bean);
		var b = Json5Parser.DEFAULT.parse(s, C_XmlGregorianCalendar.class);
		assertNull(b.f);
	}

	@Test void c02_xmlGregorianCalendar_emptyWire_parsesAsNull() {
		// Empty string in wire — exercises the s.isEmpty() true branch in the unswap path.
		var b = Json5Parser.DEFAULT.parse("{f:''}", C_XmlGregorianCalendar.class);
		assertNull(b.f);
	}

	@Test void c03_xmlGregorianCalendar_nullWire_unswap() {
		// Explicit null wire — exercises the o == null true branch in the unswap path.
		var b = Json5Parser.DEFAULT.parse("{f:null}", C_XmlGregorianCalendar.class);
		assertNull(b.f);
	}

	@Test void c04_xmlGregorianCalendar_validWire_roundTrips() throws Exception {
		var seed = DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-05-22T12:00:00Z");
		var bean = new C_XmlGregorianCalendar();
		bean.f = seed;
		var s = Json5Serializer.DEFAULT.serialize(bean);
		var b = Json5Parser.DEFAULT.parse(s, C_XmlGregorianCalendar.class);
		assertNotNull(b.f);
		assertEquals(seed.toXMLFormat(), b.f.toXMLFormat());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group D — Bug #18 carryover: TemporalFormat.MILLIS on the temporalAccessorSwap path.
	// The MILLIS-numeric arm in temporalAccessorSwap.swap is structurally identical to temporalSwap's
	// (line 727 in MPP) but is only exercised when propertyClass is a TemporalAccessor (MonthDay) and
	// the format is MILLIS.  MonthDay falls back to ISO_OFFSET_TIME-like text via the carve-out, so
	// the numeric arm isn't taken; this test pins the format-dispatch around the helper call.
	//------------------------------------------------------------------------------------------------------------------

	public static class D_MonthDay { public MonthDay f; }

	@Test void d01_temporalAccessorSwap_millisFormatRoundTrips() {
		var s = Json5Serializer.create().temporalFormat(TemporalFormat.MILLIS).build();
		var p = Json5Parser.create().temporalFormat(TemporalFormat.MILLIS).build();
		var bean = new D_MonthDay();
		bean.f = MonthDay.of(6, 15);
		var wire = s.serialize(bean);
		var back = p.parse(wire, D_MonthDay.class);
		assertEquals(bean.f, back.f);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group E — @MarshalledProp explicitly setting format + bean property with same field type.
	// Exercises the "Type-matches && format != NOT_SET" true-true branch in applyPropertyFormats
	// (the main happy path for each format).  Without an explicit fixture, the matrix only hits this
	// branch indirectly when a context-level format is set on a Type-matching property without an
	// annotation — which goes through applyContextFormats, not applyPropertyFormats.
	//------------------------------------------------------------------------------------------------------------------

	public static class E_DurationFmt   { @MarshalledProp(durationFormat   = DurationFormat.SECONDS) public Duration   f; }
	public static class E_PeriodFmt     { @MarshalledProp(periodFormat     = PeriodFormat.DAYS)      public Period     f; }
	public static class E_DateFmt       { @MarshalledProp(dateFormat       = DateFormat.ISO_INSTANT) public Date       f; }
	public static class E_InstantFmt    { @MarshalledProp(temporalFormat   = TemporalFormat.ISO_INSTANT) public Instant f; }
	public static class E_ZoneIdFmt     { @MarshalledProp(timeZoneFormat   = TimeZoneFormat.ID)      public ZoneId     f; }
	public static class E_TimeZoneFmt   { @MarshalledProp(timeZoneFormat   = TimeZoneFormat.ID)      public TimeZone   f; }
	public static class E_LocaleFmt     { @MarshalledProp(localeFormat     = LocaleFormat.UNDERSCORE) public Locale    f; }
	public static class E_ByteArrayFmt  { @MarshalledProp(binaryFormat     = BinaryFormat.HEX)       public byte[]     f; }
	public static class E_EnumFmt       { @MarshalledProp(enumFormat       = EnumFormat.NAME)        public AEnum      f; }
	public static class E_UuidFmt       { @MarshalledProp(uuidFormat       = UuidFormat.STANDARD)    public UUID       f; }
	public static class E_BigIntFmt     { @MarshalledProp(bigNumberFormat  = BigNumberFormat.NUMBER) public BigInteger f; }
	public static class E_BigDecFmt     { @MarshalledProp(bigNumberFormat  = BigNumberFormat.NUMBER) public BigDecimal f; }
	public static class E_BooleanFmt    { @MarshalledProp(booleanFormat    = BooleanFormat.TRUE_FALSE) public Boolean  f; }
	public static class E_FloatFmt      { @MarshalledProp(floatFormat      = FloatFormat.NaN_AS_NULL) public Float    f; }
	public static class E_DoubleFmt     { @MarshalledProp(floatFormat      = FloatFormat.NaN_AS_NULL) public Double   f; }
	public static class E_CurrencyFmt   { @MarshalledProp(currencyFormat   = CurrencyFormat.ISO_CODE) public Currency f; }
	public static class E_ClassFmt      { @MarshalledProp(classFormat      = ClassFormat.FQCN)       public Class<?>   f; }
	public static class E_MonthDayFmt   { @MarshalledProp(temporalFormat   = TemporalFormat.ISO_LOCAL_DATE) public MonthDay f; }

	@Test void e01_marshalledProp_explicitFormat_eachTypedField() {
		roundTripDefault(new E_DurationFmt(),  "f", Duration.ofHours(1));
		roundTripDefault(new E_PeriodFmt(),    "f", Period.ofDays(3));
		roundTripDefault(new E_DateFmt(),      "f", new Date(0L));
		roundTripDefault(new E_InstantFmt(),   "f", Instant.parse("2026-05-22T12:00:00Z"));
		roundTripDefault(new E_ZoneIdFmt(),    "f", ZoneId.of("UTC"));
		roundTripDefault(new E_TimeZoneFmt(),  "f", TimeZone.getTimeZone("UTC"));
		roundTripDefault(new E_LocaleFmt(),    "f", Locale.US);
		roundTripDefault(new E_ByteArrayFmt(), "f", new byte[] { 1, 2, 3 });
		roundTripDefault(new E_EnumFmt(),      "f", AEnum.X);
		roundTripDefault(new E_UuidFmt(),      "f", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
		roundTripDefault(new E_BigIntFmt(),    "f", BigInteger.valueOf(42));
		roundTripDefault(new E_BigDecFmt(),    "f", new BigDecimal("1.5"));
		roundTripDefault(new E_BooleanFmt(),   "f", Boolean.TRUE);
		roundTripDefault(new E_FloatFmt(),     "f", 1.5f);
		roundTripDefault(new E_DoubleFmt(),    "f", 1.5d);
		roundTripDefault(new E_CurrencyFmt(),  "f", Currency.getInstance("USD"));
		roundTripDefault(new E_ClassFmt(),     "f", String.class);
		roundTripDefault(new E_MonthDayFmt(),  "f", MonthDay.of(6, 15));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group F — @Marshalled (class-level) with explicit formats on each typed field.
	// Exercises the "Type-matches && format != NOT_SET" true-true branch in applyClassFormats
	// (lines 465-495).  Mirrors group E for @MarshalledProp.
	//------------------------------------------------------------------------------------------------------------------

	@Marshalled(durationFormat   = DurationFormat.SECONDS) public static class F_Duration   { public Duration   f; }
	@Marshalled(periodFormat     = PeriodFormat.DAYS)      public static class F_Period     { public Period     f; }
	@Marshalled(dateFormat       = DateFormat.ISO_INSTANT) public static class F_Date       { public Date       f; }
	@Marshalled(temporalFormat   = TemporalFormat.ISO_INSTANT) public static class F_Instant { public Instant   f; }
	@Marshalled(timeZoneFormat   = TimeZoneFormat.ID)      public static class F_ZoneId     { public ZoneId     f; }
	@Marshalled(timeZoneFormat   = TimeZoneFormat.ID)      public static class F_TimeZone   { public TimeZone   f; }
	@Marshalled(localeFormat     = LocaleFormat.UNDERSCORE) public static class F_Locale    { public Locale     f; }
	@Marshalled(binaryFormat     = BinaryFormat.HEX)       public static class F_ByteArray  { public byte[]     f; }
	@Marshalled(enumFormat       = EnumFormat.NAME)        public static class F_Enum       { public AEnum      f; }
	@Marshalled(uuidFormat       = UuidFormat.STANDARD)    public static class F_Uuid       { public UUID       f; }
	@Marshalled(bigNumberFormat  = BigNumberFormat.NUMBER) public static class F_BigInt     { public BigInteger f; }
	@Marshalled(bigNumberFormat  = BigNumberFormat.NUMBER) public static class F_BigDec     { public BigDecimal f; }
	@Marshalled(booleanFormat    = BooleanFormat.TRUE_FALSE) public static class F_Boolean  { public Boolean    f; }
	@Marshalled(floatFormat      = FloatFormat.NaN_AS_NULL) public static class F_Float     { public Float      f; }
	@Marshalled(floatFormat      = FloatFormat.NaN_AS_NULL) public static class F_Double    { public Double     f; }
	@Marshalled(currencyFormat   = CurrencyFormat.ISO_CODE) public static class F_Currency  { public Currency   f; }
	@Marshalled(classFormat      = ClassFormat.FQCN)       public static class F_Class      { public Class<?>   f; }
	@Marshalled(temporalFormat   = TemporalFormat.ISO_LOCAL_DATE) public static class F_MonthDay { public MonthDay f; }

	@Test void f01_marshalled_explicitFormat_eachTypedField() {
		roundTripDefault(new F_Duration(),  "f", Duration.ofHours(1));
		roundTripDefault(new F_Period(),    "f", Period.ofDays(3));
		roundTripDefault(new F_Date(),      "f", new Date(0L));
		roundTripDefault(new F_Instant(),   "f", Instant.parse("2026-05-22T12:00:00Z"));
		roundTripDefault(new F_ZoneId(),    "f", ZoneId.of("UTC"));
		roundTripDefault(new F_TimeZone(),  "f", TimeZone.getTimeZone("UTC"));
		roundTripDefault(new F_Locale(),    "f", Locale.US);
		roundTripDefault(new F_ByteArray(), "f", new byte[] { 1, 2, 3 });
		roundTripDefault(new F_Enum(),      "f", AEnum.X);
		roundTripDefault(new F_Uuid(),      "f", UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
		roundTripDefault(new F_BigInt(),    "f", BigInteger.valueOf(42));
		roundTripDefault(new F_BigDec(),    "f", new BigDecimal("1.5"));
		roundTripDefault(new F_Boolean(),   "f", Boolean.TRUE);
		roundTripDefault(new F_Float(),     "f", 1.5f);
		roundTripDefault(new F_Double(),    "f", 1.5d);
		roundTripDefault(new F_Currency(),  "f", Currency.getInstance("USD"));
		roundTripDefault(new F_Class(),     "f", String.class);
		roundTripDefault(new F_MonthDay(),  "f", MonthDay.of(6, 15));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group G — Combined @MarshalledProp + @Swap on the same field.
	//------------------------------------------------------------------------------------------------------------------

	public static class IdentitySwap extends ObjectSwap<String,String> {
		@Override
		public String swap(MarshallingSession session, String o) {
			return o;
		}
		@Override
		public String unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
			return o;
		}
	}

	public static class G_MarshalledPropPlusSwap {
		@MarshalledProp(format = "x")
		@Swap(IdentitySwap.class)
		public String f;
	}

	@Test void g01_marshalledProp_andSwap_shortCircuits() {
		roundTripDefault(new G_MarshalledPropPlusSwap(), "f", "hello");
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group H — float-primitive field with explicit FloatFormat via @MarshalledProp.
	// Without an annotation, applyContextFormats's requiresFloatSwap predicate returns false for
	// primitive float / double, so the swap factory isn't built.  An explicit @MarshalledProp builds
	// the swap unconditionally, exercising the float.class branch of isFloatType inside the factory
	// (line 957) and the blank-string fall-through inside floatSwap.unswap (line 996).
	//------------------------------------------------------------------------------------------------------------------

	public static class H_PrimitiveFloat  { @MarshalledProp(floatFormat = FloatFormat.NaN_AS_NULL) public float  f; }
	public static class H_PrimitiveDouble { @MarshalledProp(floatFormat = FloatFormat.NaN_AS_NULL) public double f; }

	@Test void h01_floatSwap_primitiveTypes() {
		roundTripDefault(new H_PrimitiveFloat(),  "f", 1.5f);
		roundTripDefault(new H_PrimitiveDouble(), "f", 1.5d);
	}

	@Test void h02_floatSwap_blankStringParsesToZero() {
		var p = Json5Parser.create().build();
		var b = p.parse("{f:''}", E_FloatFmt.class);
		assertEquals(Float.valueOf(0.0f), b.f);
	}

	@Test void h03_floatSwap_blankStringDouble() {
		var p = Json5Parser.create().build();
		var b = p.parse("{f:''}", E_DoubleFmt.class);
		assertEquals(Double.valueOf(0.0d), b.f);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group I — XMLGregorianCalendar field/getter/setter combined with @MarshalledProp.
	//------------------------------------------------------------------------------------------------------------------

	public static class I_XgcField {
		@MarshalledProp(format = "x")
		public XMLGregorianCalendar f;
	}

	public static class I_XgcGetter {
		private XMLGregorianCalendar f;
		@MarshalledProp(format = "x")
		public XMLGregorianCalendar getF() { return f; }
		public void setF(XMLGregorianCalendar v) { f = v; }
	}

	public static class I_XgcSetter {
		private XMLGregorianCalendar f;
		public XMLGregorianCalendar getF() { return f; }
		@MarshalledProp(format = "x")
		public void setF(XMLGregorianCalendar v) { f = v; }
	}

	@Test void i01_xmlGregorianCalendar_marshalledPropOnField_swapAlreadySet() throws Exception {
		var seed = DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-05-22T12:00:00Z");
		var bean = new I_XgcField();
		bean.f = seed;
		assertNotNull(Json5Serializer.DEFAULT.serialize(bean));
	}

	@Test void i02_xmlGregorianCalendar_marshalledPropOnGetter_swapAlreadySet() throws Exception {
		var seed = DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-05-22T12:00:00Z");
		var bean = new I_XgcGetter();
		bean.setF(seed);
		assertNotNull(Json5Serializer.DEFAULT.serialize(bean));
	}

	@Test void i03_xmlGregorianCalendar_marshalledPropOnSetter_swapAlreadySet() throws Exception {
		var seed = DatatypeFactory.newInstance().newXMLGregorianCalendar("2026-05-22T12:00:00Z");
		var bean = new I_XgcSetter();
		bean.setF(seed);
		assertNotNull(Json5Serializer.DEFAULT.serialize(bean));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group J — Child-swap path inside installSwapAwareTransforms.
	// A List<byte[]> field with a context-level BinaryFormat threads the byte[] subtype's swap through
	// rawTypeMeta.hasChildSwaps() / getChildObjectSwapForSwap / getChildObjectSwapForUnswap,
	// exercising the child-swap arms at lines 226-229 (read transform) and 246-251 (write transform).
	//------------------------------------------------------------------------------------------------------------------

	public static class J_ListOfBytes {
		public List<byte[]> f;
	}

	@Test void j01_childSwap_listOfByteArrays_hex() {
		var bean = new J_ListOfBytes();
		bean.f = new ArrayList<>();
		bean.f.add(new byte[] { 1, 2, 3 });
		bean.f.add(new byte[] { 4, 5, 6 });
		var s = Json5Serializer.create().binaryFormat(BinaryFormat.HEX).build();
		var wire = s.serialize(bean);
		assertNotNull(wire);
		var p = Json5Parser.create().binaryFormat(BinaryFormat.HEX).build();
		var back = p.parse(wire, J_ListOfBytes.class);
		assertEquals(2, back.f.size());
		assertArrayEquals(new byte[] { 1, 2, 3 }, back.f.get(0));
		assertArrayEquals(new byte[] { 4, 5, 6 }, back.f.get(1));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group K — Number-typed field with a context-registered swap on a subclass.
	// Registering an ObjectSwap on a subclass (BigInteger) populates the Number type's childSwaps map.
	// installSwapAwareTransforms then walks the read/write transforms through the child-swap arms at
	// lines 226-229 (readTransform) and 246-251 (writeTransform).
	//------------------------------------------------------------------------------------------------------------------

	public static class BigIntStringSwap extends ObjectSwap<BigInteger,String> {
		@Override
		public String swap(MarshallingSession session, BigInteger o) {
			return o.toString();
		}
		@Override
		public BigInteger unswap(MarshallingSession session, String o, ClassMeta<?> hint) {
			return new BigInteger(o);
		}
	}

	public static class K_NumberField {
		public Number f;
	}

	@Test void k01_childSwap_numberField_bigIntegerValue() {
		var bean = new K_NumberField();
		bean.f = BigInteger.valueOf(42);
		var s = Json5Serializer.create().swaps(BigIntStringSwap.class).build();
		var p = Json5Parser.create().swaps(BigIntStringSwap.class).build();
		var wire = s.serialize(bean);
		assertNotNull(wire);
		var back = p.parse(wire, K_NumberField.class);
		assertNotNull(back.f);
	}

	@Test void k02_childSwap_numberField_nullValue() {
		// Null value on a Number field — exercises the o == null arms inside the child-swap
		// read/write transforms (lines 224, 247) without the actual child-swap path firing.
		var bean = new K_NumberField();
		bean.f = null;
		var s = Json5Serializer.create().swaps(BigIntStringSwap.class).build();
		var p = Json5Parser.create().swaps(BigIntStringSwap.class).build();
		var wire = s.serialize(bean);
		var back = p.parse(wire, K_NumberField.class);
		assertNull(back.f);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Group L — Direct MPP invocation for the null-marshallingContext defensive guard.
	// {@link MarshalledPropertyPostProcessor#process(Object, BeanPropertyMeta.Builder)} short-circuits
	// when the marshalling context is null (the bean-modeling-only path), so the builder argument is
	// never dereferenced — a null builder is fine here.
	//------------------------------------------------------------------------------------------------------------------

	@Test void l01_process_nullMarshallingContext_isNoop() {
		// Cast to the SPI interface so the overload resolves to the public (Object, Builder) override
		// — that's where the null-context defensive guard lives.  The static `process(MarshallingContext,
		// Builder)` overload would otherwise win on argument typing and NPE before the guard.
		org.apache.juneau.commons.bean.BeanPropertyPostProcessor pp = MarshalledPropertyPostProcessor.INSTANCE;
		pp.process(null, null);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Helpers
	//------------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"unchecked"  // Unchecked cast required for generic test utility.
	})
	private static <T> T roundTripDefault(T bean, String propertyName, Object value) {
		// Reflectively set the property so the helper stays generic across the per-type fixture classes.
		try {
			bean.getClass().getField(propertyName).set(bean, value);
			var s = Json5Serializer.DEFAULT.serialize(bean);
			var b = (T) Json5Parser.DEFAULT.parse(s, bean.getClass());
			assertNotNull(b, "parsed bean was null for " + bean.getClass().getSimpleName());
			return b;
		} catch (ReflectiveOperationException e) {
			throw new AssertionError(e);
		} catch (Exception e) {
			throw new AssertionError("round-trip failed for " + bean.getClass().getSimpleName(), e);
		}
	}
}
