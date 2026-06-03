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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.time.*;
import java.time.chrono.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class TemporalFormat_Test {

	private static final ZoneId Z = ZoneId.of("Z");

	@Test void a01_defaultPerSubtype() {
		assertEquals("2011-12-03T10:15:30Z", TemporalFormat.DEFAULT.format(Instant.parse("2011-12-03T10:15:30Z"), Z));
		assertEquals("2011-12-03T10:15:30Z", TemporalFormat.NOT_SET.format(Instant.parse("2011-12-03T10:15:30Z"), Z));
		assertEquals("2011-12-03", TemporalFormat.DEFAULT.format(LocalDate.parse("2011-12-03"), Z));
		assertEquals("2011-12-03T10:15:30", TemporalFormat.DEFAULT.format(LocalDateTime.parse("2011-12-03T10:15:30"), Z));
		assertEquals("10:15:30", TemporalFormat.DEFAULT.format(LocalTime.parse("10:15:30"), Z));
		assertEquals("2012", TemporalFormat.DEFAULT.format(Year.of(2012), Z));
		assertEquals("2012-12", TemporalFormat.DEFAULT.format(YearMonth.of(2012, 12), Z));
	}

	@Test void a02_isoVariants_onInstant() {
		var i = Instant.parse("2011-12-03T10:15:30Z");
		assertEquals("2011-12-03T10:15:30Z", TemporalFormat.ISO_INSTANT.format(i, Z));
		assertEquals("2011-12-03T10:15:30Z", TemporalFormat.ISO_OFFSET_DATE_TIME.format(i, Z));
		assertEquals("20111203Z", TemporalFormat.BASIC_ISO_DATE.format(i, Z));
	}

	@Test void a03_roundTrip_perSubtype() {
		var i = Instant.parse("2011-12-03T10:15:30Z");
		assertEquals(i, TemporalFormat.DEFAULT.parse(TemporalFormat.DEFAULT.format(i, Z), Instant.class, Z));
		var ld = LocalDate.parse("2011-12-03");
		assertEquals(ld, TemporalFormat.DEFAULT.parse(TemporalFormat.DEFAULT.format(ld, Z), LocalDate.class, Z));
		var ldt = LocalDateTime.parse("2011-12-03T10:15:30");
		assertEquals(ldt, TemporalFormat.DEFAULT.parse(TemporalFormat.DEFAULT.format(ldt, Z), LocalDateTime.class, Z));
	}

	@Test void a04_millis_instant() {
		var i = Instant.parse("2011-12-03T10:15:30Z");
		assertEquals(Long.toString(i.toEpochMilli()), TemporalFormat.MILLIS.format(i, Z));
		assertEquals(i, TemporalFormat.MILLIS.parse(Long.toString(i.toEpochMilli()), Instant.class, Z));
	}

	@Test void a05_millis_midnightUtcRule_localDate() {
		var ld = LocalDate.parse("2011-12-03");
		var expectedMillis = ld.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
		assertEquals(Long.toString(expectedMillis), TemporalFormat.MILLIS.format(ld, Z));
		assertEquals(ld, TemporalFormat.MILLIS.parse(Long.toString(expectedMillis), LocalDate.class, Z));
	}

	@Test void a06_millis_midnightUtcRule_localDateTime() {
		var ldt = LocalDateTime.parse("2011-12-03T10:15:30");
		var expectedMillis = ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
		assertEquals(Long.toString(expectedMillis), TemporalFormat.MILLIS.format(ldt, Z));
		assertEquals(ldt, TemporalFormat.MILLIS.parse(Long.toString(expectedMillis), LocalDateTime.class, Z));
	}

	@Test void a07_millis_midnightUtcRule_yearAndYearMonth() {
		var y = Year.of(2012);
		assertEquals(Long.toString(y.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()),
			TemporalFormat.MILLIS.format(y, Z));
		var ym = YearMonth.of(2012, 12);
		assertEquals(Long.toString(ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()),
			TemporalFormat.MILLIS.format(ym, Z));
	}

	@Test void a08_millis_localTime_fallsBackToIsoString() {
		var lt = LocalTime.parse("10:15:30");
		assertEquals("10:15:30", TemporalFormat.MILLIS.format(lt, Z));
		assertEquals(lt, TemporalFormat.MILLIS.parse("10:15:30", LocalTime.class, Z));
	}

	@Test void a10_isNumeric() {
		assertTrue(TemporalFormat.MILLIS.isNumeric());
		assertFalse(TemporalFormat.DEFAULT.isNumeric());
		assertFalse(TemporalFormat.NOT_SET.isNumeric());
		assertFalse(TemporalFormat.ISO_INSTANT.isNumeric());
	}

	@Test void a11_nullAndBlank() {
		assertNull(TemporalFormat.ISO_INSTANT.format(null, Z));
		assertNull(TemporalFormat.ISO_INSTANT.parse(null, Instant.class, Z));
		assertNull(TemporalFormat.ISO_INSTANT.parse("   ", Instant.class, Z));
	}

	@Test void a12_lenientEpochMillis_forInstantLikeOnly() {
		var i = Instant.parse("2011-12-03T10:15:30Z");
		// Instant: lenient parse accepts a numeric string as epoch millis under DEFAULT/NOT_SET.
		assertEquals(i, TemporalFormat.DEFAULT.parse(Long.toString(i.toEpochMilli()), Instant.class, Z));
		// Year: a numeric value like "2012" must be parsed as a year, not epoch millis.
		assertEquals(Year.of(2012), TemporalFormat.DEFAULT.parse("2012", Year.class, Z));
	}

	@Test void a13_allFormatVariants_onZonedDateTime() {
		var zdt = ZonedDateTime.parse("2011-12-03T10:15:30Z[UTC]");
		var formats = new TemporalFormat[]{
			TemporalFormat.BASIC_ISO_DATE,
			TemporalFormat.ISO_DATE,
			TemporalFormat.ISO_DATE_TIME,
			TemporalFormat.ISO_INSTANT,
			TemporalFormat.ISO_LOCAL_DATE,
			TemporalFormat.ISO_LOCAL_DATE_TIME,
			TemporalFormat.ISO_LOCAL_TIME,
			TemporalFormat.ISO_OFFSET_DATE,
			TemporalFormat.ISO_OFFSET_DATE_TIME,
			TemporalFormat.ISO_OFFSET_TIME,
			TemporalFormat.ISO_ORDINAL_DATE,
			TemporalFormat.ISO_TIME,
			TemporalFormat.ISO_WEEK_DATE,
			TemporalFormat.ISO_ZONED_DATE_TIME,
			TemporalFormat.RFC_1123_DATE_TIME,
			TemporalFormat.ISO_YEAR,
			TemporalFormat.ISO_YEAR_MONTH,
		};
		for (var f : formats) {
			var s = f.format(zdt, Z);
			assertNotNull(s, "format=" + f);
		}
	}

	@Test void a14_formatVariants_perSubtype() {
		var ldt = LocalDateTime.parse("2011-12-03T10:15:30");
		assertNotNull(TemporalFormat.ISO_LOCAL_DATE_TIME.format(ldt, Z));
		assertNotNull(TemporalFormat.ISO_DATE_TIME.format(ldt, Z));
		var ot = OffsetTime.parse("10:15:30+01:00");
		assertNotNull(TemporalFormat.ISO_OFFSET_TIME.format(ot, Z));
		assertNotNull(TemporalFormat.ISO_OFFSET_DATE_TIME.format(ot, Z));
		var odt = OffsetDateTime.parse("2011-12-03T10:15:30+01:00");
		assertNotNull(TemporalFormat.BASIC_ISO_DATE.format(odt, Z));
		var lt = LocalTime.parse("10:15:30");
		assertNotNull(TemporalFormat.ISO_LOCAL_TIME.format(lt, Z));
	}

	@Test void a15_roundTrip_moreSubtypes() {
		var y = Year.of(2012);
		assertEquals(y, TemporalFormat.ISO_YEAR.parse(TemporalFormat.ISO_YEAR.format(y, Z), Year.class, Z));
		var ym = YearMonth.of(2012, 5);
		assertEquals(ym, TemporalFormat.ISO_YEAR_MONTH.parse(TemporalFormat.ISO_YEAR_MONTH.format(ym, Z), YearMonth.class, Z));
		var zdt = ZonedDateTime.parse("2011-12-03T10:15:30Z[UTC]");
		assertNotNull(TemporalFormat.ISO_ZONED_DATE_TIME.parse(TemporalFormat.ISO_ZONED_DATE_TIME.format(zdt, Z), ZonedDateTime.class, Z));
	}

	@Test void a16_millis_fromEpochToAllSupportedSubtypes() {
		var i = Instant.parse("2011-12-03T10:15:30Z");
		var millis = i.toEpochMilli();
		assertEquals(i, TemporalFormat.MILLIS.parse(Long.toString(millis), Instant.class, Z));
		assertNotNull(TemporalFormat.MILLIS.parse(Long.toString(millis), ZonedDateTime.class, Z));
		assertNotNull(TemporalFormat.MILLIS.parse(Long.toString(millis), OffsetDateTime.class, Z));
		assertNotNull(TemporalFormat.MILLIS.parse(Long.toString(millis), LocalDateTime.class, Z));
		assertNotNull(TemporalFormat.MILLIS.parse(Long.toString(millis), LocalDate.class, Z));
		assertNotNull(TemporalFormat.MILLIS.parse(Long.toString(millis), OffsetTime.class, Z));
		assertNotNull(TemporalFormat.MILLIS.parse(Long.toString(millis), YearMonth.class, Z));
		assertNotNull(TemporalFormat.MILLIS.parse(Long.toString(millis), Year.class, Z));
	}

	@Test void a17_formatterThrowsForMillis() {
		assertThrows(IllegalStateException.class, () -> {
			try {
				var m = TemporalFormat.class.getDeclaredMethod("formatter", Class.class);
				m.setAccessible(true);
				m.invoke(TemporalFormat.MILLIS, Instant.class);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		});
	}

	@Test void a18_nullZoneFallsBackToSystem() {
		var i = Instant.parse("2011-12-03T10:15:30Z");
		assertEquals("2011-12-03T10:15:30Z", TemporalFormat.ISO_INSTANT.format(i, null));
		assertEquals(i, TemporalFormat.ISO_INSTANT.parse("2011-12-03T10:15:30Z", Instant.class, null));
	}

	@Test void a19_isAllDigits_signedAndEdgeCases() {
		assertEquals(Instant.ofEpochMilli(-1), TemporalFormat.DEFAULT.parse("-1", Instant.class, Z));
		assertEquals(Instant.ofEpochMilli(1), TemporalFormat.DEFAULT.parse("+1", Instant.class, Z));
	}

	@Test void a21_default_chronoLocalDateUsesIsoInstant() {
		var hd = HijrahDate.now();
		var s = TemporalFormat.DEFAULT.format(hd, Z);
		assertNotNull(s);
	}

	@Test void a22_default_chronoLocalDateTimeUsesIsoInstant() {
		var hd = HijrahDate.now();
		var hldt = hd.atTime(LocalTime.NOON);
		var s = TemporalFormat.DEFAULT.format(hldt, Z);
		assertNotNull(s);
	}

	@Test void a23_default_chronoZonedDateTimeUsesIsoInstant() {
		var hd = HijrahDate.now();
		var hzdt = hd.atTime(LocalTime.NOON).atZone(Z);
		var s = TemporalFormat.DEFAULT.format(hzdt, Z);
		assertNotNull(s);
	}

	@Test void a24_millis_offsetDateTime() {
		var odt = OffsetDateTime.parse("2011-12-03T10:15:30+01:00");
		assertEquals(Long.toString(odt.toInstant().toEpochMilli()), TemporalFormat.MILLIS.format(odt, Z));
	}

	@Test void a25_millis_zonedDateTime() {
		var zdt = ZonedDateTime.parse("2011-12-03T10:15:30+01:00[Europe/Paris]");
		assertEquals(Long.toString(zdt.toInstant().toEpochMilli()), TemporalFormat.MILLIS.format(zdt, Z));
	}

	@Test void a26_zoneOptional_offsetTimeCoercion() {
		var ot = OffsetTime.parse("10:15:30+01:00");
		assertNotNull(TemporalFormat.ISO_LOCAL_TIME.format(ot, Z));
	}

	@Test void a27_isoDate_onInstant() {
		var i = Instant.parse("2011-12-03T10:15:30Z");
		assertNotNull(TemporalFormat.ISO_DATE.format(i, Z));
		assertNotNull(TemporalFormat.ISO_LOCAL_DATE.format(i, Z));
	}
}
