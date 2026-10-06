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
package org.apache.juneau.commons.beanquery;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.TestBase;
import org.junit.jupiter.api.*;

class ValueFormat_Test extends TestBase {

	@Test
	void a01_charSequence() {
		assertEquals("abc", ValueFormat.format("abc"));
		assertEquals("abc", ValueFormat.format(new StringBuilder("abc")));
	}

	@Test
	void a02_integerFamily() {
		assertEquals("42", ValueFormat.format(42));
		assertEquals("42", ValueFormat.format(42L));
		assertEquals("42", ValueFormat.format((short) 42));
		assertEquals("42", ValueFormat.format((byte) 42));
		assertEquals("42", ValueFormat.format(BigInteger.valueOf(42)));
	}

	@Test
	void a03_doubleFamily() {
		assertEquals("1000", ValueFormat.format(1000.0));
		assertEquals("0.5", ValueFormat.format(0.5));
		assertEquals("1000", ValueFormat.format(1e3));
		assertEquals("1000", ValueFormat.format(new BigDecimal("1E+3")));
	}

	@Test
	void a04_doubleFamily_nonFinite_throws() {
		assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(Double.NaN));
		assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(Double.POSITIVE_INFINITY));
		assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(Float.NaN));
	}

	@Test
	void a05_boolean() {
		assertEquals("true", ValueFormat.format(true));
		assertEquals("false", ValueFormat.format(false));
	}

	enum Status { OPEN, CLOSED }

	@Test
	void a06_enum() {
		assertEquals("OPEN", ValueFormat.format(Status.OPEN));
	}

	@Test
	void a07_instantFamily() {
		var instant = Instant.parse("2026-09-30T12:00:00Z");
		assertEquals("2026-09-30T12:00:00Z", ValueFormat.format(instant));
		assertEquals("2026-09-30T12:00:00Z", ValueFormat.format(instant.atOffset(java.time.ZoneOffset.UTC)));
		assertEquals("2026-09-30T12:00:00Z", ValueFormat.format(instant.atZone(java.time.ZoneOffset.UTC)));
		assertEquals("2026-09-30T12:00:00Z", ValueFormat.format(Date.from(instant)));
		var cal = GregorianCalendar.from(instant.atZone(java.time.ZoneOffset.UTC));
		assertEquals("2026-09-30T12:00:00Z", ValueFormat.format(cal));
	}

	@Test
	void a08_localDate() {
		assertEquals("2026-09-30", ValueFormat.format(LocalDate.of(2026, 9, 30)));
	}

	@Test
	void a09_localDateTime() {
		assertEquals("2026-09-30T12:00:00", ValueFormat.format(LocalDateTime.of(2026, 9, 30, 12, 0, 0)));
	}

	@Test
	void a10_null_throws() {
		var e = assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(null));
		assertTrue(e.getMessage().contains("null"));
	}

	@Test
	void a11_unsupportedType_throws() {
		var hoistedArg1 = new Object();
		assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(hoistedArg1));
	}

	@Test
	void a12_otherNumber_decimal() {
		assertEquals("42", ValueFormat.format(new AtomicIntegerLike(42)));
	}

	@Test
	void a13_otherNumber_unparseable_throws() {
		var hoistedArg2 = new UnparseableNumber();
		assertThrows(IllegalArgumentException.class, () -> ValueFormat.format(hoistedArg2));
	}

	@Test
	void a14_relativeDuration_negative() {
		assertEquals("-24h", ValueFormat.format(RelativeDuration.of(-24, RelativeDuration.Unit.H)));
	}

	@Test
	void a15_relativeDuration_positive_signIsExplicit() {
		assertEquals("+7d", ValueFormat.format(RelativeDuration.of(7, RelativeDuration.Unit.D)));
	}

	@Test
	void a16_relativeDuration_zero_signIsExplicit() {
		assertEquals("+0ms", ValueFormat.format(RelativeDuration.of(0, RelativeDuration.Unit.MS)));
	}

	/** A Number subtype that is none of the built-in boxed types, to exercise the "other Number" row. */
	static final class AtomicIntegerLike extends Number {
		private static final long serialVersionUID = 1L;
		private final int v;
		AtomicIntegerLike(int v) { this.v = v; }
		@Override public int intValue() { return v; }
		@Override public long longValue() { return v; }
		@Override public float floatValue() { return (float)v; }
		@Override public double doubleValue() { return v; }
		@Override public String toString() { return String.valueOf(v); }
	}

	/** A Number subtype whose toString() is not parseable by {@code new BigDecimal(String)}, to exercise the catch branch. */
	static final class UnparseableNumber extends Number {
		private static final long serialVersionUID = 1L;
		@Override public int intValue() { return 0; }
		@Override public long longValue() { return 0; }
		@Override public float floatValue() { return 0f; }
		@Override public double doubleValue() { return 0; }
		@Override public String toString() { return "abc"; }
	}
}
