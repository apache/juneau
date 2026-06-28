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

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.jcs.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.prototext.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.api.*;

/**
 * Coverage for the 10.0.0 null-inclusion / null-coercion knobs and the first-class {@link BitSetFormat} /
 * primitive-{@link Optional} handling.
 *
 * <p>
 * Exercises {@code nonDefault} on the serializer side, the {@link Nulls} per-property / context-level policy on the
 * parser side, the shared {@link Optional} contract both sides honor, the {@code MarshalledPropertyPostProcessor}
 * null-coercion transforms, the {@link BitSetFormat} format/parse paths, and the primitive-{@link Optional}
 * converter / {@link ClassMeta} plumbing.
 *
 * <p>
 * All formats touched here ({@code json}, plus the {@code hjson/hocon/ini/jcs/proto/toml/uon/xml/yaml} families used
 * by the per-format inclusion-branch test) live in this module, so the full matrix runs as a unit test with no
 * dependency on the integration-tests surface.
 */
class NullInclusionAndCoercion_Test extends TestBase {

	//====================================================================================================
	// a. Serializer nonDefault — inclusion knob + numeric-default equality (defaultEquals / toBigDecimal)
	//====================================================================================================

	public static class A1 {
		public int i;
		public Integer boxed;
		public String s;
		public double d;
	}

	@Test void a01_nonDefault_omitsPrimitiveDefaults() throws Exception {
		assertEquals("{}", JsonSerializer.create().nonDefault().build().serializeToString(new A1()));
	}

	@Test void a02_nonDefault_emitsNonDefaultPrimitives() throws Exception {
		var b = new A1();
		b.i = 5;
		assertEquals("{\"i\":5}", JsonSerializer.create().nonDefault().build().serializeToString(b));
	}

	@Test void a03_nonDefault_disabledByDefault() throws Exception {
		assertEquals("{\"d\":0.0,\"i\":0}", JsonSerializer.create().build().serializeToString(new A1()));
	}

	@Test void a04_nonDefault_keepNullPropertiesFirst() throws Exception {
		assertEquals("{\"boxed\":null,\"s\":null}", JsonSerializer.create().nonDefault().keepNullProperties().build().serializeToString(new A1()));
	}

	public static class A2 {
		public String name = "hello";
		public int count = 7;
	}

	@Test void a05_nonDefault_usesBeanConstructedDefaults_cached() throws Exception {
		var s = JsonSerializer.create().nonDefault().build();
		// Serialize twice so the per-ClassMeta reference instance is resolved once then read from cache.
		assertEquals("{}", s.serializeToString(new A2()));
		var b = new A2();
		b.name = "world";
		assertEquals("{\"name\":\"world\"}", s.serializeToString(b));
	}

	// Numeric-default equality across Number subtypes — exercises defaultEquals() + toBigDecimal() branches.
	public static class A3 {
		public long l = 5L;
		public float f = 1.5f;
		public BigInteger bi = BigInteger.TEN;
		public BigDecimal bd = new BigDecimal("1.0");
		public double nan = Double.NaN;
		public float fnan = Float.NaN;
	}

	@Test void a06_nonDefault_numericEquality_allMatchDefaults() throws Exception {
		// Every field equals its bean-constructed default (Double + Float NaN both match) → all omitted.
		assertEquals("{}", JsonSerializer.create().nonDefault().build().serializeToString(new A3()));
	}

	// Value-equal-but-not-reference-equal + String + null-default → defaultEquals non-Number/null/BigInteger paths.
	public static class A6 {
		public String name = "x";
		public String nul;                            // reference default is null
		public BigInteger big = new BigInteger("10"); // distinct instance from the value below
	}

	@Test void a09_nonDefault_defaultEquals_nonNumericAndNullAndBigInteger() throws Exception {
		var b = new A6();
		b.name = "y";                       // diverges from "x" (both non-null, non-Number) → emitted
		b.nul = "z";                        // reference default null, value non-null → emitted
		b.big = new BigInteger("10");       // value-equal but != reference → BigDecimal compare → omitted
		var json = JsonSerializer.create().nonDefault().build().serializeToString(b);
		assertTrue(json.contains("\"name\":\"y\""), json);
		assertTrue(json.contains("\"nul\":\"z\""), json);
		assertFalse(json.contains("\"big\""), json);
	}

	@Test void a07_nonDefault_numericEquality_divergent() throws Exception {
		var b = new A3();
		b.l = 6L;
		b.bd = new BigDecimal("2.0");
		b.nan = 1.0;  // NaN default vs non-NaN value → emitted
		var json = JsonSerializer.create().nonDefault().build().serializeToString(b);
		assertTrue(json.contains("\"l\":6"), json);
		assertTrue(json.contains("\"bd\":2.0"), json);
		assertTrue(json.contains("\"nan\":1.0"), json);
		assertFalse(json.contains("\"f\":"), json);
		assertFalse(json.contains("\"bi\":"), json);
	}

	// Bean with no accessible no-arg constructor — nonDefault cannot build a reference instance, so the
	// "no reference instance" fallback fires and every property is emitted.
	public static class A4 {
		public int x;
		public A4(int x) { this.x = x; }
	}

	@Test void a08_nonDefault_noNoArgCtor_emitsEverything() throws Exception {
		assertEquals("{\"x\":5}", JsonSerializer.create().nonDefault().build().serializeToString(new A4(5)));
	}

	//====================================================================================================
	// b. Shared Optional contract on serialize (Optional<T> + OptionalInt/Long/Double)
	//====================================================================================================

	public static class B1 {
		public Optional<String> a = opte();
		public Optional<String> b = opt("x");
		public OptionalInt c = OptionalInt.empty();
		public OptionalInt d = OptionalInt.of(7);
		public OptionalLong e = OptionalLong.of(9L);
		public OptionalDouble f = OptionalDouble.of(2.5);
	}

	@Test void b01_emptyOptionals_omittedByDefault() throws Exception {
		var json = JsonSerializer.DEFAULT.serializeToString(new B1());
		assertFalse(json.contains("\"a\":"), json);
		assertFalse(json.contains("\"c\":"), json);
	}

	@Test void b02_presentOptionals_emittedUnwrapped() throws Exception {
		var json = JsonSerializer.DEFAULT.serializeToString(new B1());
		assertTrue(json.contains("\"b\":\"x\""), json);
		assertTrue(json.contains("\"d\":7"), json);
		assertTrue(json.contains("\"e\":9"), json);
		assertTrue(json.contains("\"f\":2.5"), json);
	}

	@Test void b03_emptyOptionals_emittedAsNullWhenKept() throws Exception {
		var json = JsonSerializer.create().keepNullProperties().build().serializeToString(new B1());
		assertTrue(json.contains("\"a\":null"), json);
		assertTrue(json.contains("\"c\":null"), json);
	}

	public static class B2 {
		public OptionalLong e = OptionalLong.empty();
		public OptionalDouble f = OptionalDouble.empty();
	}

	@Test void b04_emptyPrimitiveLongDouble_unwrapToNull() throws Exception {
		// keepNullProperties forces traversal of the empty OptionalLong/OptionalDouble — exercises both the
		// canIgnoreValue unwrap and the traverse-session getOptionalValue empty branches.
		var json = JsonSerializer.create().keepNullProperties().build().serializeToString(new B2());
		assertTrue(json.contains("\"e\":null"), json);
		assertTrue(json.contains("\"f\":null"), json);
	}

	//====================================================================================================
	// c. Shared Optional contract on parse + top-level primitive-optional coercion
	//====================================================================================================

	@Test void c01_absentAndNullOptional_resolveToEmpty() throws Exception {
		var b = JsonParser.DEFAULT.parse("{\"a\":null,\"c\":null,\"e\":null,\"f\":null}", B1.class);
		assertTrue(b.a.isEmpty());
		assertTrue(b.c.isEmpty());
		assertTrue(b.e.isEmpty());
		assertTrue(b.f.isEmpty());
	}

	@Test void c02_presentPrimitiveOptionals_parse() throws Exception {
		var b = JsonParser.DEFAULT.parse("{\"d\":42,\"e\":43,\"f\":4.5}", B1.class);
		assertEquals(42, b.d.getAsInt());
		assertEquals(43L, b.e.getAsLong());
		assertEquals(4.5, b.f.getAsDouble());
	}

	@Test void c03_topLevelPrimitiveOptional_parse() throws Exception {
		assertEquals(5, JsonParser.DEFAULT.parse("5", OptionalInt.class).getAsInt());
		assertEquals(6L, JsonParser.DEFAULT.parse("6", OptionalLong.class).getAsLong());
		assertEquals(7.5, JsonParser.DEFAULT.parse("7.5", OptionalDouble.class).getAsDouble());
		assertTrue(JsonParser.DEFAULT.parse("null", OptionalInt.class).isEmpty());
	}

	//====================================================================================================
	// cb. BitSet — first-class handling via BitSetFormat (round-trip, no swap registered)
	//====================================================================================================

	public static class CB1 {
		public BitSet bits;
	}

	private static BitSet bitSet(int... indices) {
		var b = new BitSet();
		for (var i : indices)
			b.set(i);
		return b;
	}

	@Test void cb01_indices_default_roundTrip() throws Exception {
		var bean = new CB1();
		bean.bits = bitSet(0, 2, 5);
		var json = JsonSerializer.DEFAULT.serializeToString(bean);
		assertTrue(json.contains("\"bits\":\"0,2,5\""), json);
		assertEquals(bean.bits, JsonParser.DEFAULT.parse(json, CB1.class).bits);
	}

	@Test void cb02_bits_format_roundTrip() throws Exception {
		var s = JsonSerializer.create().bitSetFormat(BitSetFormat.BITS).build();
		var p = JsonParser.create().bitSetFormat(BitSetFormat.BITS).build();
		var bean = new CB1();
		bean.bits = bitSet(0, 2, 5);
		var json = s.serializeToString(bean);
		assertTrue(json.contains("\"bits\":\"101001\""), json);
		assertEquals(bean.bits, p.parse(json, CB1.class).bits);
	}

	@Test void cb03_hex_format_roundTrip() throws Exception {
		var s = JsonSerializer.create().bitSetFormat(BitSetFormat.HEX).build();
		var p = JsonParser.create().bitSetFormat(BitSetFormat.HEX).build();
		var bean = new CB1();
		bean.bits = bitSet(0, 2, 5);
		var json = s.serializeToString(bean);
		assertTrue(json.contains("\"bits\":\"25\""), json);
		assertEquals(bean.bits, p.parse(json, CB1.class).bits);
	}

	public static class CB2 {
		@MarshalledProp(bitSetFormat = BitSetFormat.BITS)
		public BitSet bits;
	}

	@Test void cb04_perProperty_overridesContext() throws Exception {
		var bean = new CB2();
		bean.bits = bitSet(0, 2, 5);
		var json = JsonSerializer.DEFAULT.serializeToString(bean);
		assertTrue(json.contains("\"bits\":\"101001\""), json);
		assertEquals(bean.bits, JsonParser.DEFAULT.parse(json, CB2.class).bits);
	}

	@Marshalled(bitSetFormat = BitSetFormat.HEX)
	public static class CB3 {
		public BitSet bits;
	}

	@Test void cb05_perClass_overridesContext() throws Exception {
		var bean = new CB3();
		bean.bits = bitSet(0, 2, 5);
		var json = JsonSerializer.DEFAULT.serializeToString(bean);
		assertTrue(json.contains("\"bits\":\"25\""), json);
		assertEquals(bean.bits, JsonParser.DEFAULT.parse(json, CB3.class).bits);
	}

	@Test void cb06_emptyBitSet_roundTrip() throws Exception {
		var bean = new CB1();
		bean.bits = new BitSet();
		var json = JsonSerializer.DEFAULT.serializeToString(bean);
		assertEquals(new BitSet(), JsonParser.DEFAULT.parse(json, CB1.class).bits);
	}

	//====================================================================================================
	// cc. BitSetFormat — direct unit coverage of format()/parse()/isNumeric()
	//====================================================================================================

	@Test void cc01_format_allConstants() {
		var bs = bitSet(0, 2, 5);
		assertEquals("0,2,5", BitSetFormat.format(bs, BitSetFormat.INDICES));
		assertEquals("0,2,5", BitSetFormat.format(bs, BitSetFormat.NOT_SET));   // NOT_SET → INDICES
		assertEquals("0,2,5", BitSetFormat.format(bs, null));                   // null → INDICES
		assertEquals("101001", BitSetFormat.format(bs, BitSetFormat.BITS));
		assertEquals("25", BitSetFormat.format(bs, BitSetFormat.HEX));
		assertEquals("", BitSetFormat.format(new BitSet(), BitSetFormat.INDICES));
		assertEquals("", BitSetFormat.format(new BitSet(), BitSetFormat.BITS));
		assertEquals("", BitSetFormat.format(new BitSet(), BitSetFormat.HEX));
		assertNull(BitSetFormat.format(null, BitSetFormat.INDICES));            // null value → null
	}

	@Test void cc02_parse_allConstants() {
		var expected = bitSet(0, 2, 5);
		assertEquals(expected, BitSetFormat.parse("0,2,5", BitSetFormat.INDICES));
		assertEquals(expected, BitSetFormat.parse(" 0 , 2 , 5 ", BitSetFormat.NOT_SET));  // NOT_SET → INDICES, trims
		assertEquals(expected, BitSetFormat.parse("0,2,5", null));                        // null → INDICES
		assertEquals(expected, BitSetFormat.parse("101001", BitSetFormat.BITS));
		assertEquals(expected, BitSetFormat.parse("25", BitSetFormat.HEX));
		assertEquals(new BitSet(), BitSetFormat.parse("", BitSetFormat.INDICES));         // empty → empty BitSet
		assertEquals(new BitSet(), BitSetFormat.parse("   ", BitSetFormat.HEX));          // blank → empty BitSet
		assertNull(BitSetFormat.parse(null, BitSetFormat.INDICES));                       // null → null
	}

	@Test void cc03_parse_invalidIndices_throws() {
		assertThrows(IllegalArgumentException.class, () -> BitSetFormat.parse("0,x,5", BitSetFormat.INDICES));
	}

	@Test void cc04_parse_invalidHex_throws() {
		assertThrows(IllegalArgumentException.class, () -> BitSetFormat.parse("2", BitSetFormat.HEX));   // odd length
		assertThrows(IllegalArgumentException.class, () -> BitSetFormat.parse("zz", BitSetFormat.HEX));  // bad nibble
	}

	@Test void cc05_isNumeric_alwaysFalse() {
		for (var f : BitSetFormat.values())
			assertFalse(f.isNumeric());
	}

	//====================================================================================================
	// d. Parser context-level nulls modes (LEAVE / EMPTY / DEFAULT / SKIP)
	//====================================================================================================

	public static class D1 {
		public String s;
		public Integer i;
		public List<String> list;
	}

	@Test void d01_nulls_LEAVE_default() throws Exception {
		var b = JsonParser.DEFAULT.parse("{\"s\":null,\"i\":null,\"list\":null}", D1.class);
		assertNull(b.s);
		assertNull(b.i);
		assertNull(b.list);
	}

	@Test void d02_nulls_EMPTY() throws Exception {
		var b = JsonParser.create().nulls(Nulls.EMPTY).build().parse("{\"s\":null,\"i\":null,\"list\":null}", D1.class);
		assertEquals("", b.s);
		assertNull(b.i);            // boxed Integer has no empty sentinel → LEAVE fallback
		assertNotNull(b.list);
		assertTrue(b.list.isEmpty());
	}

	public static class D2 {
		public String s = "hello";
		public int i = 5;
	}

	@Test void d03_nulls_DEFAULT() throws Exception {
		var b = JsonParser.create().nulls(Nulls.DEFAULT).build().parse("{\"s\":null,\"i\":null}", D2.class);
		assertEquals("hello", b.s);
		assertEquals(5, b.i);
	}

	@Test void d04_nulls_SKIP() throws Exception {
		var b = JsonParser.create().nulls(Nulls.SKIP).build().parse("{\"s\":null,\"i\":null}", D2.class);
		assertEquals("hello", b.s);
		assertEquals(5, b.i);
	}

	@Test void d05_nulls_nullArg_treatedAsNotSet() throws Exception {
		// nulls(null) → NOT_SET → behaves as LEAVE.
		var b = JsonParser.create().nulls(null).build().parse("{\"s\":null}", D1.class);
		assertNull(b.s);
	}

	//====================================================================================================
	// e. EMPTY-mode emptyValueFor — exhaustive per-type coverage
	//====================================================================================================

	public static class E1 {
		public String s;
		public CharSequence cs;
		public int[] arr;
		public List<String> list;
		public Collection<String> coll;
		public Set<String> set;
		public Map<String,String> map;
		public Optional<String> opt;
		public OptionalInt oi;
		public OptionalLong ol;
		public OptionalDouble od;
		public int pi;
		public long pl;
		public double pd;
		public float pf;
		public short psh;
		public byte pby;
		public boolean pbo;
		public char pch;
		public Object obj;          // Object/unknown → no empty sentinel → null (LEAVE fallback)
		public StringBuilder sb;    // reference type with no canonical empty → null (LEAVE fallback)
	}

	@Test void e01_empty_substitutesPerType() throws Exception {
		var json = "{\"s\":null,\"cs\":null,\"arr\":null,\"list\":null,\"coll\":null,\"set\":null,\"map\":null,"
			+ "\"opt\":null,\"oi\":null,\"ol\":null,\"od\":null,\"pi\":null,\"pl\":null,\"pd\":null,\"pf\":null,"
			+ "\"psh\":null,\"pby\":null,\"pbo\":null,\"pch\":null,\"obj\":null,\"sb\":null}";
		var b = JsonParser.create().nulls(Nulls.EMPTY).build().parse(json, E1.class);
		assertEquals("", b.s);
		assertEquals("", b.cs);
		assertNull(b.obj);
		assertNull(b.sb);
		assertNotNull(b.arr);
		assertEquals(0, b.arr.length);
		assertTrue(b.list.isEmpty());
		assertTrue(b.coll.isEmpty());
		assertTrue(b.set.isEmpty());
		assertTrue(b.map.isEmpty());
		assertTrue(b.opt.isEmpty());
		assertTrue(b.oi.isEmpty());
		assertTrue(b.ol.isEmpty());
		assertTrue(b.od.isEmpty());
		assertEquals(0, b.pi);
		assertEquals(0L, b.pl);
		assertEquals(0d, b.pd);
		assertEquals(0f, b.pf);
		assertEquals((short) 0, b.psh);
		assertEquals((byte) 0, b.pby);
		assertFalse(b.pbo);
		assertEquals((char) 0, b.pch);
	}

	//====================================================================================================
	// f. @MarshalledProp(nulls=...) per-property override + Optional contract under coercion
	//====================================================================================================

	public static class F1 {
		@MarshalledProp(nulls = Nulls.EMPTY)
		public String s;
		@MarshalledProp(nulls = Nulls.SKIP)
		public String t = "initial";
		public String u;  // inherits context default
	}

	@Test void f01_perProperty_overridesContext() throws Exception {
		var b = JsonParser.DEFAULT.parse("{\"s\":null,\"t\":null,\"u\":null}", F1.class);
		assertEquals("", b.s);
		assertEquals("initial", b.t);
		assertNull(b.u);
	}

	public static class F2 {
		public Optional<String> a;
		@MarshalledProp(nulls = Nulls.EMPTY)
		public Optional<String> b;
		@MarshalledProp(nulls = Nulls.DEFAULT)
		public Optional<String> c = opt("seed");
		public OptionalInt d;
		public OptionalLong e;
		public OptionalDouble f;
	}

	@Test void f02_optionalContract_underCoercion() throws Exception {
		var b = JsonParser.create().nulls(Nulls.EMPTY).build().parse("{\"a\":null,\"b\":null,\"d\":null,\"e\":null,\"f\":null}", F2.class);
		assertTrue(b.a.isEmpty());
		assertTrue(b.b.isEmpty());
		assertTrue(b.d.isEmpty());
		assertTrue(b.e.isEmpty());
		assertTrue(b.f.isEmpty());
	}

	@Test void f03_optional_DEFAULT_usesBeanDefault() throws Exception {
		var b = JsonParser.DEFAULT.parse("{\"c\":null}", F2.class);
		assertTrue(b.c.isPresent());
		assertEquals("seed", b.c.orElse(null));
	}

	// @MarshalledProp(nulls=...) on the getter and setter surfaces (not just the field).
	public static class F4 {
		private String g;
		private String s;
		@MarshalledProp(nulls = Nulls.EMPTY)
		public String getG() { return g; }
		public void setG(String v) { g = v; }
		public String getS() { return s; }
		@MarshalledProp(nulls = Nulls.EMPTY)
		public void setS(String v) { s = v; }
	}

	@Test void f04_perProperty_nullsOnGetterAndSetter() throws Exception {
		var b = JsonParser.DEFAULT.parse("{\"g\":null,\"s\":null}", F4.class);
		assertEquals("", b.g);
		assertEquals("", b.s);
	}

	//====================================================================================================
	// g. Per-format inclusion-branch coverage (canIgnoreValue true-path on every serializer family)
	//====================================================================================================

	public static class G1 {
		public Optional<String> a = opte();  // non-null but ignorable → reaches canIgnoreValue
		public String b = "keep";
	}

	@Test void g01_perFormat_ignoresEmptyOptional() {
		// An empty Optional is a non-null value that survives the null pre-filter and reaches canIgnoreValue,
		// whose ignore-branch then fires on every serializer family.  serialize(Object) traverses the bean
		// regardless of char/stream output shape.
		var bean = new G1();
		List<Serializer> serializers = List.of(
			HjsonSerializer.DEFAULT, HoconSerializer.DEFAULT, IniSerializer.DEFAULT, JcsSerializer.DEFAULT,
			PrototextSerializer.DEFAULT, TomlSerializer.DEFAULT, UonSerializer.DEFAULT, XmlSerializer.DEFAULT,
			YamlSerializer.DEFAULT);
		for (var s : serializers)
			assertDoesNotThrow(() -> s.serialize(bean), () -> s.getClass().getSimpleName());
	}

	//====================================================================================================
	// h. Primitive-optional conversions (BasicConverter / CachingConverter)
	//====================================================================================================

	@Test void h01_convert_nullAndNumber() throws Exception {
		var bc = MarshallingContext.DEFAULT;
		assertEquals(OptionalInt.empty(), bc.convertToType(null, OptionalInt.class));
		assertEquals(OptionalLong.empty(), bc.convertToType(null, OptionalLong.class));
		assertEquals(OptionalDouble.empty(), bc.convertToType(null, OptionalDouble.class));
		assertEquals(OptionalInt.of(5), bc.convertToType(5, OptionalInt.class));
		assertEquals(OptionalLong.of(6L), bc.convertToType("6", OptionalLong.class));
		assertEquals(OptionalDouble.of(7.5), bc.convertToType(7.5, OptionalDouble.class));
	}

	@Test void h02_convert_fromOptionalInputs() throws Exception {
		var bc = MarshallingContext.DEFAULT;
		assertEquals(OptionalInt.of(9), bc.convertToType(opt(9), OptionalInt.class));
		assertEquals(OptionalLong.empty(), bc.convertToType(opte(), OptionalLong.class));
		assertEquals(OptionalLong.of(4L), bc.convertToType(OptionalInt.of(4), OptionalLong.class));
		assertEquals(OptionalInt.of(3), bc.convertToType(OptionalLong.of(3L), OptionalInt.class));
		assertEquals(OptionalInt.of(2), bc.convertToType(OptionalDouble.of(2.0), OptionalInt.class));
		// Cross-type empties (different out-type avoids the identity short-circuit) → exercise each empty-unwrap
		// branch plus the OptionalDouble empty sentinel.
		assertEquals(OptionalLong.empty(), bc.convertToType(OptionalInt.empty(), OptionalLong.class));
		assertEquals(OptionalInt.empty(), bc.convertToType(OptionalLong.empty(), OptionalInt.class));
		assertEquals(OptionalInt.empty(), bc.convertToType(OptionalDouble.empty(), OptionalInt.class));
		assertEquals(OptionalDouble.empty(), bc.convertToType(opte(), OptionalDouble.class));
	}

	@Test void h03_convert_unwrappedToNull_yieldsEmpty() throws Exception {
		// Optional containing a blank string → unwraps then coerces to a null number → empty primitive optional.
		assertEquals(OptionalInt.empty(), MarshallingContext.DEFAULT.convertToType(opt(""), OptionalInt.class));
	}
}
