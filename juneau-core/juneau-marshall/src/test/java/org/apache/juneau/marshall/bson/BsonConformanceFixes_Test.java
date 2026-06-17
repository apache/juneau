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
package org.apache.juneau.marshall.bson;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.math.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Regression tests for the Phase-2 BSON conformance fixes (175fe).
 *
 * <p>
 * Each category guards one audited gap:
 * <ul>
 * 	<li><b>a</b> — H1: default {@code writeDatesAsDatetime=true} must not crash on local {@code java.time} types.
 * 	<li><b>b</b> — H2: wire-declared lengths (string/binary/document) are bounds-checked against a configurable max.
 * 	<li><b>c</b> — M1: inbound Decimal128 NaN/Infinity normalizes to {@code null} instead of crashing.
 * 	<li><b>d</b> — M2: out-of-range {@link BigDecimal}/{@link BigInteger} fail cleanly with a {@link SerializeException}.
 * 	<li><b>e</b> — M3: the configured bean type-property name is honored on parse (polymorphism).
 * 	<li><b>f</b> — Guards confirming native fidelity ({@link BsonDecimal128}, int64/datetime-millis) is intact.
 * </ul>
 */
@SuppressWarnings({
	"resource",  // BsonInputStream instances in tests are closed by the test infrastructure / GC; not leak-relevant.
	"unchecked"  // Parser returns Object; cast to Map in tests.
})
class BsonConformanceFixes_Test extends TestBase {

	// ================================================================
	// Helpers (byte-level construction mirrors BsonInputStream_Test)
	// ================================================================

	private static BsonInputStream openIs(byte[] bytes) throws IOException {
		return new BsonInputStream(new ParserPipe(new ByteArrayInputStream(bytes)));
	}

	private static byte[] le4(int v) {
		return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
	}

	private static byte[] le8(long v) {
		return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24),
			(byte) (v >> 32), (byte) (v >> 40), (byte) (v >> 48), (byte) (v >> 56)};
	}

	private static byte[] cat(byte[]... arrs) {
		var total = 0;
		for (var a : arrs)
			total += a.length;
		var out = new byte[total];
		var pos = 0;
		for (var a : arrs) {
			System.arraycopy(a, 0, out, pos, a.length);
			pos += a.length;
		}
		return out;
	}

	private static byte[] cstring(String s) {
		var b = s.getBytes(StandardCharsets.UTF_8);
		return cat(b, new byte[]{0x00});
	}

	/** Builds the 16-byte little-endian Decimal128 wire form (low 64 bits, then high 64 bits). */
	private static byte[] decimal128(long high, long low) {
		return cat(le8(low), le8(high));
	}

	/** Builds a single-element BSON document {@code {name: <value>}} of the given element type. */
	private static byte[] doc(int type, String name, byte[] value) {
		var body = cat(new byte[]{(byte) type}, cstring(name), value, new byte[]{0x00});
		return cat(le4(body.length + 4), body);
	}

	// ================================================================
	// a — H1: local java.time temporals must not crash (ISO fallback)
	// ================================================================

	@Test
	void a01_localDateDoesNotCrash() throws Exception {
		// Was: DateTimeException (Instant.from(LocalDate)) under default writeDatesAsDatetime=true.
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("d", LocalDate.of(2020, 1, 1)));
		var parsed = (Map<String,Object>) BsonParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals("2020-01-01", parsed.get("d"));
	}

	@Test
	void a02_localDateTimeDoesNotCrash() throws Exception {
		var v = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("d", v));
		var parsed = (Map<String,Object>) BsonParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals(v.toString(), parsed.get("d"));
	}

	@Test
	void a03_localTimeDoesNotCrash() throws Exception {
		var v = LocalTime.of(10, 15, 30);
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("d", v));
		var parsed = (Map<String,Object>) BsonParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals(v.toString(), parsed.get("d"));
	}

	@Test
	void a04_instantBearingTemporalsStillUseDatetime() throws Exception {
		// Guard: OffsetDateTime/ZonedDateTime/Instant remain on the int64-millis datetime (0x09) path.
		var instant = Instant.ofEpochMilli(1700000000000L);
		var odt = instant.atOffset(ZoneOffset.UTC);
		var zdt = instant.atZone(ZoneOffset.UTC);
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("i", instant, "o", odt, "z", zdt));
		var parsed = (Map<String,Object>) BsonParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals(1700000000000L, parsed.get("i"));
		assertEquals(1700000000000L, parsed.get("o"));
		assertEquals(1700000000000L, parsed.get("z"));
	}

	// ================================================================
	// b — H2: wire-length validation
	// ================================================================

	@Test
	void b01_binaryNegativeLengthRejected() throws Exception {
		try (var is = openIs(le4(-1))) {
			assertThrowsWithMessage(IOException.class, "negative", is::readBinary);
		}
	}

	@Test
	void b02_binaryHugeLengthRejected() throws Exception {
		// 0x7FFFFFFF would OOM via new byte[len] before the fix.
		try (var is = openIs(le4(0x7FFFFFFF))) {
			assertThrowsWithMessage(IOException.class, "exceeds maximum", is::readBinary);
		}
	}

	@Test
	void b03_stringHugeLengthRejected() throws Exception {
		try (var is = openIs(le4(0x7FFFFFFF))) {
			assertThrowsWithMessage(IOException.class, "exceeds maximum", is::readString);
		}
	}

	@Test
	void b04_documentSizeNegativeRejected() throws Exception {
		try (var is = openIs(le4(-1))) {
			assertThrowsWithMessage(IOException.class, "negative", is::readDocumentSize);
		}
	}

	@Test
	void b05_configurableMaxLengthEnforcedEndToEnd() throws Exception {
		// A document with a 32-byte binary parses with a generous cap but fails with a tiny cap.
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("data", new byte[32]));
		var lenient = BsonParser.create().maxLength(1024).build();
		var parsed = (Map<String,Object>) lenient.parse(bytes, Map.class, String.class, Object.class);
		assertEquals(32, ((byte[]) parsed.get("data")).length);

		var strict = BsonParser.create().maxLength(8).build();
		assertThrowsWithMessage(IOException.class, "exceeds maximum", () -> strict.parse(bytes, Map.class, String.class, Object.class));
	}

	@Test
	void b06_maxLengthAffectsCacheKey() {
		// Different maxLength values must NOT collide in the parser cache (hashKey wiring).
		var p1 = BsonParser.create().maxLength(100).build();
		var p2 = BsonParser.create().maxLength(200).build();
		var p3 = BsonParser.create().maxLength(100).build();
		assertNotSame(p1, p2);
		assertSame(p1, p3);
		assertEquals(100, p1.getMaxLength());
		assertEquals(200, p2.getMaxLength());
	}

	@Test
	void b07_maxLengthZeroDisablesCap() throws Exception {
		// A non-positive cap disables the max check (the negative-length guard still applies).
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("data", new byte[32]));
		var p = BsonParser.create().maxLength(0).build();
		var parsed = (Map<String,Object>) p.parse(bytes, Map.class, String.class, Object.class);
		assertEquals(32, ((byte[]) parsed.get("data")).length);
	}

	// ================================================================
	// c — M1: inbound Decimal128 NaN / Infinity
	// ================================================================

	@Test
	void c01_positiveInfinityDecodesToNull() throws Exception {
		try (var is = openIs(decimal128(0x7800000000000000L, 0L))) {
			assertNull(is.readDecimal128());
		}
	}

	@Test
	void c02_negativeInfinityDecodesToNull() throws Exception {
		try (var is = openIs(decimal128(0xF800000000000000L, 0L))) {
			assertNull(is.readDecimal128());
		}
	}

	@Test
	void c03_nanDecodesToNull() throws Exception {
		try (var is = openIs(decimal128(0x7C00000000000000L, 0L))) {
			assertNull(is.readDecimal128());
		}
	}

	@Test
	void c04_infinityInDocumentDoesNotAbortParse() throws Exception {
		// Whole-document parse must not blow up with an unchecked ArithmeticException.
		var bytes = doc(0x13, "d", decimal128(0x7800000000000000L, 0L));
		var parsed = (Map<String,Object>) BsonParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertTrue(parsed.containsKey("d"));
		assertNull(parsed.get("d"));
	}

	// ================================================================
	// d — M2: out-of-range BigDecimal / BigInteger on serialize
	// ================================================================

	@Test
	void d01_bigIntegerBeyondDecimal128Range() {
		var v = BigInteger.valueOf(2).pow(120); // > 113-bit significand
		assertThrowsWithMessage(SerializeException.class, "Decimal128 range", () -> BsonSerializer.DEFAULT.serialize(v));
	}

	@Test
	void d02_bigDecimalTooManySignificantDigits() {
		var v = new BigDecimal("1.2345678901234567890123456789012345"); // 35 significant digits
		assertThrowsWithMessage(SerializeException.class, "Decimal128 range", () -> BsonSerializer.DEFAULT.serialize(v));
	}

	@Test
	void d03_inRangeBigNumbersStillSerialize() throws Exception {
		// Guard: values that DO fit must keep working (no over-eager rejection).
		var v = new BigDecimal("123456789.0123456789");
		var bytes = BsonSerializer.DEFAULT.serialize(v);
		assertEquals(0, v.compareTo(BsonParser.DEFAULT.parse(bytes, BigDecimal.class)));
	}

	// ================================================================
	// e — M3: configured bean type-property name honored on parse
	// ================================================================

	@Marshalled(typeName="e_dog")
	public static class E_Dog {
		public String name;
	}

	@Test
	void e01_customTypePropertyNameRoundTrips() throws Exception {
		var s = BsonSerializer.create().addBeanTypes().addRootType().beanDictionary(E_Dog.class).typePropertyName("_t").keepNullProperties().build();
		var p = BsonParser.create().beanDictionary(E_Dog.class).typePropertyName("_t").build();
		var a = new E_Dog();
		a.name = "Rex";
		var bytes = s.serialize(a);
		var parsed = p.parse(bytes, Object.class);
		assertInstanceOf(E_Dog.class, parsed);
		assertEquals("Rex", ((E_Dog) parsed).name);
	}

	@Test
	void e02_defaultTypePropertyNameStillRoundTrips() throws Exception {
		// Guard: the default discriminator name ("_type") still resolves polymorphic types.
		var s = BsonSerializer.create().addBeanTypes().addRootType().beanDictionary(E_Dog.class).keepNullProperties().build();
		var p = BsonParser.create().beanDictionary(E_Dog.class).build();
		var a = new E_Dog();
		a.name = "Fido";
		var bytes = s.serialize(a);
		var parsed = p.parse(bytes, Object.class);
		assertInstanceOf(E_Dog.class, parsed);
		assertEquals("Fido", ((E_Dog) parsed).name);
	}

	// ================================================================
	// f — Guards: native fidelity must remain intact (not regressed)
	// ================================================================

	@Test
	void f01_decimal128NativeRoundTripIntact() throws Exception {
		var v = new BigDecimal("3.14159265358979");
		var bytes = BsonSerializer.DEFAULT.serialize(v);
		assertEquals(0, v.compareTo(BsonParser.DEFAULT.parse(bytes, BigDecimal.class)));
	}

	@Test
	void f02_int64NativeRoundTripIntact() throws Exception {
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("l", 99999999999L));
		var parsed = (Map<String,Object>) BsonParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals(99999999999L, parsed.get("l"));
	}

	@Test
	void f03_datetimeMillisNativeRoundTripIntact() throws Exception {
		var instant = Instant.ofEpochMilli(1700000000000L);
		var bytes = BsonSerializer.DEFAULT.serialize(JsonMap.of("ts", instant));
		var parsed = (Map<String,Object>) BsonParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertEquals(1700000000000L, parsed.get("ts"));
	}
}
