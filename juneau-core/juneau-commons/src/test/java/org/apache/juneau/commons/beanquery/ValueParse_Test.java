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

import static org.apache.juneau.commons.beanquery.SearchType.*;
import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;

class ValueParse_Test extends TestBase {

	private static void assertBadValue(String message, Executable e) {
		var ex = assertThrows(BeanQuerySyntaxException.class, e);
		assertEquals(BeanQuerySyntaxException.Code.BAD_VALUE, ex.code());
		assertEquals(message, ex.getMessage());
	}

	@Test void a01_numeric() {
		assertEquals(new BigDecimal("42"), NUMERIC.parse("42"));
		assertEquals(new BigDecimal("-3.5"), NUMERIC.parse("-3.5"));
		assertBadValue("Value 'abc' is not a valid numeric.", () -> NUMERIC.parse("abc"));
	}

	@Test void a02_boolean() {
		assertEquals(Boolean.TRUE, BOOLEAN.parse("true"));
		assertEquals(Boolean.TRUE, BOOLEAN.parse("TRUE"));
		assertEquals(Boolean.FALSE, BOOLEAN.parse("false"));
		assertBadValue("Value 'yes' is not a valid boolean.", () -> BOOLEAN.parse("yes"));
	}

	@Test void a03_timestamp_offset() {
		assertEquals(OffsetDateTime.parse("2026-09-30T12:00:00Z"), TIMESTAMP.parse("2026-09-30T12:00:00Z"));
		assertEquals(OffsetDateTime.parse("2026-09-30T12:00:00-04:00"), TIMESTAMP.parse("2026-09-30T12:00:00-04:00"));
	}

	@Test void a04_timestamp_localDateTime() {
		assertEquals(LocalDateTime.parse("2026-09-30T12:00:00"), TIMESTAMP.parse("2026-09-30T12:00:00"));
	}

	@Test void a05_timestamp_localDate() {
		assertEquals(LocalDate.parse("2026-09-30"), TIMESTAMP.parse("2026-09-30"));
	}

	@Test void a06_timestamp_epochMillis() {
		assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(1_800_000_000_000L), ZoneOffset.UTC), TIMESTAMP.parse("1800000000000"));
		assertEquals(OffsetDateTime.ofInstant(Instant.ofEpochMilli(-1000L), ZoneOffset.UTC), TIMESTAMP.parse("-1000"));
	}

	@Test void a07_timestamp_bad() {
		assertBadValue("Value 'not-a-date' is not a valid timestamp.", () -> TIMESTAMP.parse("not-a-date"));
	}

	@Test void a08_textIdEnumVersion_passThrough() {
		assertEquals("Bob", TEXT.parse("Bob"));
		assertEquals("abc-123", ID.parse("abc-123"));
		assertEquals("OPEN", ENUM.parse("OPEN"));
		assertEquals("10.0.0", VERSION.parse("10.0.0"));
	}

	@Test void a09_nullTreatedAsBlank() {
		assertBadValue("Value '' is not a valid numeric.", () -> NUMERIC.parse(null));
		assertEquals("", TEXT.parse(null));
	}

	@Test void a10_whitespaceStripped() {
		assertEquals(new BigDecimal("42"), NUMERIC.parse(" 42 "));
		assertEquals(Boolean.TRUE, BOOLEAN.parse(" true "));
		assertEquals(LocalDate.parse("2026-09-30"), TIMESTAMP.parse(" 2026-09-30 "));
		assertEquals("  x ", TEXT.parse("  x "));
	}

	@Test void a11_numeric_forms() {
		assertEquals(0, new BigDecimal("1000").compareTo((BigDecimal)NUMERIC.parse("1E+3")));
		assertEquals(new BigDecimal("1E+3"), NUMERIC.parse("1E+3"));
		assertEquals(new BigDecimal("1.5E-2"), NUMERIC.parse("1.5e-2"));
		assertBadValue("Value '' is not a valid numeric.", () -> NUMERIC.parse(""));
		assertBadValue("Value '1.2.3' is not a valid numeric.", () -> NUMERIC.parse("1.2.3"));
		assertBadValue("Value 'NaN' is not a valid numeric.", () -> NUMERIC.parse("NaN"));
	}

	@Test void a12_numeric_absurdMagnitude_rejected() {
		assertBadValue("Value '1E+999999999' is not a valid numeric.", () -> NUMERIC.parse("1E+999999999"));
		assertBadValue("Value '1E-999999999' is not a valid numeric.", () -> NUMERIC.parse("1E-999999999"));
		assertBadValue("Value '1E+1001' is not a valid numeric.", () -> NUMERIC.parse("1E+1001"));
		assertEquals(new BigDecimal("1E+1000"), NUMERIC.parse("1E+1000"));
		var digits = "1".repeat(1001);
		assertBadValue("Value '" + digits + "' is not a valid numeric.", () -> NUMERIC.parse(digits));
		assertEquals(new BigDecimal("1".repeat(1000)), NUMERIC.parse("1".repeat(1000)));
	}

	@Test void a13_boolean_blank() {
		assertBadValue("Value '' is not a valid boolean.", () -> BOOLEAN.parse(""));
		assertBadValue("Value '  ' is not a valid boolean.", () -> BOOLEAN.parse("  "));
		assertBadValue("Value 'tru' is not a valid boolean.", () -> BOOLEAN.parse("tru"));
	}

	@Test void a14_timestamp_moreForms() {
		assertEquals(OffsetDateTime.parse("2026-09-30T12:00:00.123456789Z"), TIMESTAMP.parse("2026-09-30T12:00:00.123456789Z"));
		assertEquals(LocalDateTime.parse("2026-09-30T12:00"), TIMESTAMP.parse("2026-09-30T12:00"));
		assertEquals(LocalDateTime.parse("2026-09-30T12:00:00.5"), TIMESTAMP.parse("2026-09-30T12:00:00.5"));
	}

	@Test void a15_timestamp_bad() {
		assertBadValue("Value '2026-13-40' is not a valid timestamp.", () -> TIMESTAMP.parse("2026-13-40"));
		assertBadValue("Value '' is not a valid timestamp.", () -> TIMESTAMP.parse(""));
		assertBadValue("Value '+1000' is not a valid timestamp.", () -> TIMESTAMP.parse("+1000"));
		assertBadValue("Value '1.5' is not a valid timestamp.", () -> TIMESTAMP.parse("1.5"));
		var tooBig = "9223372036854775808";
		assertBadValue("Value '" + tooBig + "' is not a valid timestamp.", () -> TIMESTAMP.parse(tooBig));
		// Every long is a representable instant (there is no DateTimeException path), so the extremes parse.
		assertEquals(Instant.ofEpochMilli(Long.MAX_VALUE), ((OffsetDateTime)TIMESTAMP.parse(Long.toString(Long.MAX_VALUE))).toInstant());
		assertEquals(Instant.ofEpochMilli(Long.MIN_VALUE), ((OffsetDateTime)TIMESTAMP.parse(Long.toString(Long.MIN_VALUE))).toInstant());
	}

	@Test void b01_valueFormatRoundTrip() {
		var instantNanos = Instant.parse("2026-09-30T12:00:00.123456789Z");
		var utc = ZoneOffset.UTC;
		var cal = GregorianCalendar.from(Instant.parse("2026-09-30T12:00:00.250Z").atZone(utc));
		var date = Date.from(Instant.parse("2026-09-30T12:00:00.250Z"));
		var zoned = ZonedDateTime.parse("2026-09-30T12:00:00+02:00[Europe/Paris]");
		var offset = OffsetDateTime.parse("2026-09-30T12:00:00-04:00");

		// Numbers normalise to BigDecimal (compareTo).
		for (Object n : List.<Object>of(42, 42L, BigInteger.valueOf(42), 0.5d, 0.5f, new BigDecimal("1E+3"), new BigDecimal("-3.50")))
			assertEquals(0, new BigDecimal(ValueFormat.format(n)).compareTo((BigDecimal)NUMERIC.parse(ValueFormat.format(n))), "number " + n);
		assertEquals(0, new BigDecimal("1000").compareTo((BigDecimal)NUMERIC.parse(ValueFormat.format(new BigDecimal("1E+3")))));
		assertEquals(0, new BigDecimal("0.5").compareTo((BigDecimal)NUMERIC.parse(ValueFormat.format(0.5f))));

		assertEquals(Boolean.TRUE, BOOLEAN.parse(ValueFormat.format(true)));
		assertEquals(Boolean.FALSE, BOOLEAN.parse(ValueFormat.format(false)));

		// Instant-like values are emitted as UTC instants, so each parses back as the same instant at UTC.
		assertEquals(instantNanos.atOffset(utc), TIMESTAMP.parse(ValueFormat.format(instantNanos)));
		assertEquals(date.toInstant().atOffset(utc), TIMESTAMP.parse(ValueFormat.format(date)));
		assertEquals(cal.toInstant().atOffset(utc), TIMESTAMP.parse(ValueFormat.format(cal)));
		assertEquals(zoned.toInstant().atOffset(utc), TIMESTAMP.parse(ValueFormat.format(zoned)));
		assertEquals(offset.toInstant().atOffset(utc), TIMESTAMP.parse(ValueFormat.format(offset)));
		assertEquals(offset.toInstant(), ((OffsetDateTime)TIMESTAMP.parse(ValueFormat.format(offset))).toInstant());

		// Local forms round-trip unchanged.
		var ldt = LocalDateTime.parse("2026-09-30T12:00:00.5");
		var ld = LocalDate.parse("2026-09-30");
		assertEquals(ldt, TIMESTAMP.parse(ValueFormat.format(ldt)));
		assertEquals(ld, TIMESTAMP.parse(ValueFormat.format(ld)));
	}

	private static final Instant T0 = Instant.parse("2026-09-30T12:00:00Z");

	@Test void c01_numeric_relativeDuration_isSignedMillis() {
		assertEquals(new BigDecimal("-86400000"), NUMERIC.parse("-24h"));
		assertEquals(new BigDecimal("3600000"), NUMERIC.parse("+1h"));
		assertEquals(new BigDecimal("1500"), NUMERIC.parse("+1500ms"));
		assertEquals(new BigDecimal("604800000"), NUMERIC.parse("+7d"));
		assertEquals(new BigDecimal("-90000"), NUMERIC.parse("-90s"));
		assertEquals(new BigDecimal("120000"), NUMERIC.parse("+2m"));
		assertEquals(new BigDecimal("0"), NUMERIC.parse("+0ms"));
	}

	@Test void c02_numeric_relativeDuration_unitsLowercaseOnly_andStripped() {
		assertEquals(new BigDecimal("1"), NUMERIC.parse("  +1ms "));
		for (var bad : List.of("+1H", "+1MS", "-1M", "+1D", "+1S"))
			assertBadValue("Value '" + bad + "' is not a valid numeric.", () -> NUMERIC.parse(bad));
		assertBadValue("Value '-1M' is not a valid timestamp.", () -> TIMESTAMP.parse("-1M", T0));
	}

	@Test void c03_numeric_bareSignedNumber_isNotADuration() {
		assertEquals(new BigDecimal("-24"), NUMERIC.parse("-24"));
		assertEquals(new BigDecimal("5"), NUMERIC.parse("+5"));
	}

	@Test void c04_numeric_relativeDuration_ignoresRequestTime() {
		assertEquals(NUMERIC.parse("-24h", T0), NUMERIC.parse("-24h", T0.plusSeconds(999999)));
	}

	@Test void c05_timestamp_relativeDuration_resolvesAgainstSuppliedInstant() {
		assertEquals(OffsetDateTime.parse("2026-09-29T12:00:00Z"), TIMESTAMP.parse("-24h", T0));
		assertEquals(OffsetDateTime.parse("2026-10-07T12:00:00Z"), TIMESTAMP.parse("+7d", T0));
		assertEquals(OffsetDateTime.parse("2026-09-30T11:59:30Z"), TIMESTAMP.parse("-30s", T0));
		assertEquals(OffsetDateTime.parse("2026-09-30T12:00:00.090Z"), TIMESTAMP.parse("+90ms", T0));
		assertEquals(OffsetDateTime.parse("2026-09-30T11:45:00Z"), TIMESTAMP.parse("-15m", T0));
	}

	@Test void c06_timestamp_relativeDuration_defaultOverloadUsesNow() {
		var before = Instant.now().minusSeconds(5);
		var v = (OffsetDateTime)TIMESTAMP.parse("+0ms");
		assertFalse(v.toInstant().isBefore(before) || v.toInstant().isAfter(Instant.now().plusSeconds(5)));
	}

	@Test void c07_timestamp_malformedUnit_isBadValue() {
		assertBadValue("Value '-24x' is not a valid timestamp.", () -> TIMESTAMP.parse("-24x", T0));
		assertBadValue("Value '+24' is not a valid timestamp.", () -> TIMESTAMP.parse("+24", T0));
	}

	@Test void c08_timestamp_bareNegativeNumber_isEpochMillis_notDuration() {
		assertEquals(OffsetDateTime.parse("1969-12-31T23:59:59.995Z"), TIMESTAMP.parse("-5", T0));
	}

	@Test void c09_relativeDuration_overflow_isBadValue() {
		assertBadValue("Value '+9999999999999999d' is not a valid numeric.", () -> NUMERIC.parse("+9999999999999999d"));
		assertBadValue("Value '-99999999999999999999ms' is not a valid timestamp.", () -> TIMESTAMP.parse("-99999999999999999999ms", T0));
	}

	@Test void c09b_timestamp_relativeDuration_epochOverflow_isBadValue() {
		// 106751991167d fits a long of milliseconds, but adding request time overflows epoch milliseconds.
		assertBadValue("Value '+106751991167d' is not a valid timestamp.", () -> TIMESTAMP.parse("+106751991167d", T0));
		assertBadValue("Value '-106751991167d' is not a valid timestamp.", () -> TIMESTAMP.parse("-106751991167d", Instant.ofEpochMilli(-1_000_000_000_000_000_000L)));
		assertEquals(OffsetDateTime.parse("2026-09-30T12:00:00.001Z"), TIMESTAMP.parse("+1ms", T0));
	}

	@Test void c09c_parseAbsolute_neverReadsDurationOnTimestamp() {
		assertBadValue("Value '-24h' is not a valid timestamp.", () -> ValueParse.parseAbsolute(TIMESTAMP, "-24h"));
		assertEquals(new BigDecimal("-86400000"), ValueParse.parseAbsolute(NUMERIC, "-24h"));
		assertEquals(OffsetDateTime.parse("2026-09-30T12:00:00Z"), ValueParse.parseAbsolute(TIMESTAMP, "2026-09-30T12:00:00Z"));
	}

	@Test void c09d_isoDuration_numeric_isSignedMillis() {
		assertEquals(new BigDecimal("86400000"), NUMERIC.parse("PT24H"));
		assertEquals(new BigDecimal("-86400000"), NUMERIC.parse("-PT24H"));
		assertEquals(new BigDecimal("1800000"), NUMERIC.parse("+PT30M"));
		assertEquals(new BigDecimal("86400000"), NUMERIC.parse("P1D"));
		assertEquals(new BigDecimal("90061500"), NUMERIC.parse("P1DT1H1M1.5S"));
		assertEquals(new BigDecimal("1500"), NUMERIC.parse("  pt1.5s "));
		assertEquals(new BigDecimal("0"), NUMERIC.parse("PT0S"));
	}

	@Test void c09e_isoDuration_timestamp_resolvesAgainstSuppliedInstant() {
		assertEquals(OffsetDateTime.parse("2026-09-29T12:00:00Z"), TIMESTAMP.parse("-PT24H", T0));
		assertEquals(OffsetDateTime.parse("2026-09-30T12:30:00Z"), TIMESTAMP.parse("+PT30M", T0));
		assertEquals(OffsetDateTime.parse("2026-10-01T12:00:00Z"), TIMESTAMP.parse("P1D", T0));
		assertEquals(OffsetDateTime.parse("2026-09-30T12:00:00Z"), TIMESTAMP.parse("PT0S", T0));
	}

	@Test void c09f_isoDuration_parityWithRelativeLiteral() {
		for (var pair : List.of(new String[]{"-24h", "-PT24H"}, new String[]{"+30m", "+PT30M"}, new String[]{"+1d", "P1D"}, new String[]{"+90s", "PT90S"}, new String[]{"+1500ms", "PT1.5S"})) {
			assertEquals(NUMERIC.parse(pair[0], T0), NUMERIC.parse(pair[1], T0));
			assertEquals(TIMESTAMP.parse(pair[0], T0), TIMESTAMP.parse(pair[1], T0));
		}
	}

	@Test void c09g_isoDuration_junk_isBadValue() {
		for (var bad : List.of("PTX", "P", "PT", "P1DT", "-P", "P1Y", "P1M", "P1W", "PT-1H", "-PT-1H", "+PT+1H", "PT1H-", "PT24", "PTH", "PT9223372036854775807H"))
			assertBadValue("Value '" + bad + "' is not a valid numeric.", () -> NUMERIC.parse(bad));
		assertBadValue("Value 'PTX' is not a valid timestamp.", () -> TIMESTAMP.parse("PTX", T0));
		assertBadValue("Value '-PT-1H' is not a valid timestamp.", () -> TIMESTAMP.parse("-PT-1H", T0));
		assertBadValue("Value 'PT9999999999999999H' is not a valid timestamp.", () -> TIMESTAMP.parse("PT9999999999999999H", T0));
	}

	@Test void c09h_isoDuration_otherTypes_andCells() {
		assertBadValue("Value 'PT24H' is not a valid boolean.", () -> BOOLEAN.parse("PT24H", T0));
		assertEquals("PT24H", TEXT.parse("PT24H", T0));
		assertEquals("PT24H", ENUM.parse("PT24H", T0));
		assertBadValue("Value 'PT24H' is not a valid timestamp.", () -> ValueParse.parseAbsolute(TIMESTAMP, "PT24H"));
		assertEquals(new BigDecimal("86400000"), ValueParse.parseAbsolute(NUMERIC, "PT24H"));
	}

	@Test void c10_relativeDuration_otherTypes() {
		assertBadValue("Value '-24h' is not a valid boolean.", () -> BOOLEAN.parse("-24h", T0));
		assertEquals("-24h", TEXT.parse("-24h", T0));
		assertEquals("-24h", ENUM.parse("-24h", T0));
	}

	@Test void c11_nullRequestTime_rejected() {
		assertThrows(IllegalArgumentException.class, () -> TIMESTAMP.parse("-24h", null));
	}

	@Test void c12_roundTrip_valueFormatRelativeDuration() {
		var rd = RelativeDuration.of(-24, RelativeDuration.Unit.H);
		assertEquals(new BigDecimal("-86400000"), NUMERIC.parse(ValueFormat.format(rd)));
		assertEquals(OffsetDateTime.parse("2026-09-29T12:00:00Z"), TIMESTAMP.parse(ValueFormat.format(rd), T0));
		for (var u : RelativeDuration.Unit.values()) {
			for (var n : new long[] {-3, 0, 7}) {
				var text = ValueFormat.format(RelativeDuration.of(n, u));
				assertEquals(T0.toEpochMilli() + ((BigDecimal)NUMERIC.parse(text)).longValue(), ((OffsetDateTime)TIMESTAMP.parse(text, T0)).toInstant().toEpochMilli(), text);
			}
		}
	}
}
