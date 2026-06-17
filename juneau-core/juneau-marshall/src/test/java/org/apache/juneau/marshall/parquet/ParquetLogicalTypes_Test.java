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

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.bean.*;
import org.junit.jupiter.api.*;

/**
 * Cluster B binary-native logical-type tests (GAP-8/9/10): INT96 read, DECIMAL, and
 * TIME/DATE/TIMESTAMP(micros|nanos).  Read-side decode is always on; write-side native emission is
 * behind the {@code nativeLogicalTypes()} opt-in (default output unchanged).
 */
class ParquetLogicalTypes_Test extends TestBase {

	// =================================================================================
	// B1. INT96 read (GAP-8) — 12-byte little-endian: nanos-of-day (int64) + Julian day (int32).
	// =================================================================================

	/** Builds a 12-byte INT96 page body (REQUIRED, single value) for the given instant. */
	private static byte[] int96Page(long julianDay, long nanosOfDay) {
		var b = new byte[12];
		for (var i = 0; i < 8; i++)
			b[i] = (byte) ((nanosOfDay >> (8 * i)) & 0xFF);
		for (var i = 0; i < 4; i++)
			b[8 + i] = (byte) ((julianDay >> (8 * i)) & 0xFF);
		return b;
	}

	@Test
	void b1_01_int96EpochPlusOneSecond() throws Exception {
		// Julian day 2440588 == 1970-01-01; nanos-of-day = 1e9 -> 1970-01-01T00:00:01Z.
		var page = int96Page(2440588L, 1_000_000_000L);
		var reader = new ParquetColumnReader(page, 1, 0);
		reader.advance();
		var instant = reader.readInt96AsInstant();
		assertEquals(Instant.parse("1970-01-01T00:00:01Z"), instant);
	}

	@Test
	void b1_02_int96WithSubsecondNanos() throws Exception {
		// 2440588 (1970-01-01), nanos-of-day = 1 second + 500ms.
		var page = int96Page(2440588L, 1_500_000_000L);
		var reader = new ParquetColumnReader(page, 1, 0);
		reader.advance();
		var instant = reader.readInt96AsInstant();
		assertEquals(Instant.parse("1970-01-01T00:00:01.500Z"), instant);
	}

	@Test
	void b1_03_int96LaterDay() throws Exception {
		// Julian day 2440589 == 1970-01-02; nanos-of-day = 0 -> 1970-01-02T00:00:00Z.
		var page = int96Page(2440589L, 0L);
		var reader = new ParquetColumnReader(page, 1, 0);
		reader.advance();
		var instant = reader.readInt96AsInstant();
		assertEquals(Instant.parse("1970-01-02T00:00:00Z"), instant);
	}

	// =================================================================================
	// B2. DECIMAL write→read round-trip behind nativeLogicalTypes() (GAP-9).
	// =================================================================================

	@BeanType(properties = "amount")
	public static class DecBean {
		public BigDecimal amount;
		public DecBean() {}
		public DecBean(BigDecimal amount) { this.amount = amount; }
	}

	@Test
	@SuppressWarnings("unchecked")
	void b2_01_decimalNativeRoundTrip() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		var in = List.of(new DecBean(new BigDecimal("123.456")), new DecBean(new BigDecimal("0.000000001")));
		var bytes = ser.serialize(in);
		var out = (List<DecBean>) ParquetParser.DEFAULT.parse(bytes, List.class, DecBean.class);
		assertEquals(0, new BigDecimal("123.456000000").compareTo(out.get(0).amount));
		assertEquals(0, new BigDecimal("0.000000001").compareTo(out.get(1).amount));
	}

	// =================================================================================
	// B3. DATE / TIME / TIMESTAMP-micros write→read round-trip (GAP-10).
	// =================================================================================

	@BeanType(properties = "d,t,ts")
	public static class TemporalBean {
		public LocalDate d;
		public LocalTime t;
		public Instant ts;
		public TemporalBean() {}
		public TemporalBean(LocalDate d, LocalTime t, Instant ts) { this.d = d; this.t = t; this.ts = ts; }
	}

	@Test
	@SuppressWarnings("unchecked")
	void b3_01_temporalNativeRoundTrip() throws Exception {
		var ser = ParquetSerializer.create().nativeLogicalTypes(true).build();
		// A timestamp with microsecond precision the default millis path would truncate.
		var ts = Instant.parse("2026-06-17T12:34:56.123456Z");
		var in = List.of(new TemporalBean(LocalDate.parse("2026-06-17"), LocalTime.parse("12:34:56.123456"), ts));
		var bytes = ser.serialize(in);
		var out = (List<TemporalBean>) ParquetParser.DEFAULT.parse(bytes, List.class, TemporalBean.class);
		assertEquals(LocalDate.parse("2026-06-17"), out.get(0).d);
		// Native TIME(MICROS)/TIMESTAMP(MICROS) round-trips microsecond precision exactly.
		assertEquals(LocalTime.parse("12:34:56.123456"), out.get(0).t);
		assertEquals(ts, out.get(0).ts);
		assertEquals(456, out.get(0).ts.getNano() / 1000 % 1000, "microsecond component must survive");
	}

	@Test
	void b3_02_nativeChangesTemporalWireForm() throws Exception {
		// The native temporal write produces a different (binary INT32/INT64) wire form than the default
		// string/timestamp-millis normalization — the interop point of nativeLogicalTypes().
		var ts = Instant.parse("2026-06-17T12:34:56.123456Z");
		var in = List.of(new TemporalBean(LocalDate.parse("2026-06-17"), LocalTime.parse("12:34:56.123456"), ts));
		var bytesDefault = ParquetSerializer.DEFAULT.serialize(in);
		var bytesNative = ParquetSerializer.create().nativeLogicalTypes(true).build().serialize(in);
		assertFalse(java.util.Arrays.equals(bytesDefault, bytesNative), "native temporal wire form must differ from default");
	}

	// =================================================================================
	// B4. Default output unchanged when nativeLogicalTypes is off.
	// =================================================================================

	@Test
	void b4_01_defaultOutputByteIdenticalWhenFlagOff() throws Exception {
		var in = List.of(new DecBean(new BigDecimal("123.456")));
		var bytesDefault = ParquetSerializer.DEFAULT.serialize(in);
		var bytesFlagOff = ParquetSerializer.create().nativeLogicalTypes(false).build().serialize(in);
		assertArrayEquals(bytesDefault, bytesFlagOff);
	}

	@Test
	void b4_02_nativeChangesWireForm() throws Exception {
		var in = List.of(new DecBean(new BigDecimal("123.456")));
		var bytesDefault = ParquetSerializer.DEFAULT.serialize(in);
		var bytesNative = ParquetSerializer.create().nativeLogicalTypes(true).build().serialize(in);
		// Native DECIMAL (INT64) produces a different wire form than the default UTF-8-string normalization.
		assertFalse(java.util.Arrays.equals(bytesDefault, bytesNative));
	}
}
