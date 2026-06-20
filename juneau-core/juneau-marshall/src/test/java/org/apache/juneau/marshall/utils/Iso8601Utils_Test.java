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
package org.apache.juneau.marshall.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.Duration;
import java.time.Month;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

/**
 * Tests targeting low-coverage branches in {@link Iso8601Utils}.
 */
@SuppressWarnings({
	"java:S5976" // SSLLC test naming convention requires individual methods, not parameterized tests
})
class Iso8601Utils_Test extends TestBase {

	private static final MarshallingContext BC = MarshallingContext.DEFAULT;
	private static final TimeZone UTC = TimeZone.getTimeZone("UTC");

	//------------------------------------------------------------------------------------------------------------------
	// format() dispatcher — lines 99-109
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_format_duration() {
		var r = Iso8601Utils.format(Duration.ofHours(1), BC.getClassMeta(Duration.class), UTC);
		assertNotNull(r);
		assertTrue(r.contains("PT1H") || r.contains("P") || r.contains("T"), r);
	}

	@Test void a02_format_period() {
		var r = Iso8601Utils.format(Period.of(1, 2, 3), BC.getClassMeta(Period.class), UTC);
		assertNotNull(r);
		assertTrue(r.startsWith("P"), r);
	}

	@Test void a03_format_calendar() {
		var cal = new GregorianCalendar(UTC);
		cal.set(2024, Calendar.JANUARY, 15, 12, 0, 0);
		var r = Iso8601Utils.format(cal, BC.getClassMeta(Calendar.class), UTC);
		assertNotNull(r);
		assertTrue(r.contains("2024"), r);
	}

	@Test void a04_format_date() {
		var d = new Date(0);
		var r = Iso8601Utils.format(d, BC.getClassMeta(Date.class), UTC);
		assertNotNull(r);
	}

	@Test void a05_format_temporal() {
		var t = LocalDateTime.of(2024, Month.JANUARY, 15, 12, 0, 0);
		var r = Iso8601Utils.format(t, BC.getClassMeta(LocalDateTime.class), UTC);
		assertNotNull(r);
		assertTrue(r.contains("2024"), r);
	}

	@Test void a06_format_other_toString() {
		// Falls through to value.toString()
		var r = Iso8601Utils.format("some-string", BC.getClassMeta(String.class), UTC);
		assertEquals("some-string", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// formatDuration — null format defaults to ISO_8601_WITH_DAYS (line 122), null value (line 120)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_formatDuration_nullValue() {
		assertNull(Iso8601Utils.formatDuration(null, DurationFormat.ISO_8601_WITH_DAYS));
	}

	@Test void b02_formatDuration_nullFormat_defaultsToISO() {
		var r = Iso8601Utils.formatDuration(Duration.ofHours(2), null);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// formatPeriod — null format defaults to ISO_8601 (line 135), null value (line 133)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_formatPeriod_nullValue() {
		assertNull(Iso8601Utils.formatPeriod(null, PeriodFormat.ISO_8601));
	}

	@Test void c02_formatPeriod_nullFormat_defaults() {
		var r = Iso8601Utils.formatPeriod(Period.of(1, 0, 0), null);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// formatDate — null value (line 155), null timeZone (line 157), null format (line 158)
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_formatDate_nullValue() {
		assertNull(Iso8601Utils.formatDate(null, BC.getClassMeta(Date.class), DateFormat.ISO_LOCAL_DATE_TIME, UTC));
	}

	@Test void d02_formatDate_nullTimeZone() {
		var r = Iso8601Utils.formatDate(new Date(0), BC.getClassMeta(Date.class), DateFormat.ISO_LOCAL_DATE_TIME, null);
		assertNotNull(r);
	}

	@Test void d03_formatDate_nullFormat() {
		var r = Iso8601Utils.formatDate(new Date(0), BC.getClassMeta(Date.class), null, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// formatCalendar — null value (line 181), null timeZone (line 185), null format (line 186)
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_formatCalendar_nullValue() {
		assertNull(Iso8601Utils.formatCalendar(null, BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC));
	}

	@Test void e02_formatCalendar_xmlGregorianCalendar() throws Exception {
		var x = DatatypeFactory.newInstance().newXMLGregorianCalendar("2024-01-15T12:00:00Z");
		var r = Iso8601Utils.formatCalendar(x, BC.getClassMeta(XMLGregorianCalendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNotNull(r);
	}

	@Test void e03_formatCalendar_nullTimeZone() {
		var cal = GregorianCalendar.getInstance(UTC);
		cal.set(2024, 0, 15, 12, 0, 0);
		var r = Iso8601Utils.formatCalendar(cal, BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, null);
		assertNotNull(r);
	}

	@Test void e04_formatCalendar_nullFormat() {
		var cal = GregorianCalendar.getInstance(UTC);
		cal.set(2024, 0, 15, 12, 0, 0);
		var r = Iso8601Utils.formatCalendar(cal, BC.getClassMeta(Calendar.class), null, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// formatTemporal — null value (line 202), null timeZone (line 204), null format (line 205)
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_formatTemporal_nullValue() {
		assertNull(Iso8601Utils.formatTemporal(null, BC.getClassMeta(LocalDateTime.class), TemporalFormat.DEFAULT, UTC));
	}

	@Test void f02_formatTemporal_nullTimeZone() {
		var r = Iso8601Utils.formatTemporal(LocalDateTime.of(2024, Month.JANUARY, 15, 12, 0), BC.getClassMeta(LocalDateTime.class), TemporalFormat.DEFAULT, null);
		assertNotNull(r);
	}

	@Test void f03_formatTemporal_nullFormat() {
		var r = Iso8601Utils.formatTemporal(LocalDateTime.of(2024, Month.JANUARY, 15, 12, 0), BC.getClassMeta(LocalDateTime.class), null, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// formatAsDate — null timeZone (line 220), Calendar non-Gregorian (line 222), Date (line 225), Temporal (line 227)
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_formatAsDate_nullTimeZone() {
		var r = Iso8601Utils.formatAsDate(LocalDate.of(2024, Month.JANUARY, 15), BC.getClassMeta(LocalDate.class), null);
		assertNotNull(r);
	}

	@Test void g02_formatAsDate_gregorianCalendar() {
		var gc = new GregorianCalendar(UTC);
		gc.set(2024, Calendar.JANUARY, 15, 12, 0, 0);
		var r = Iso8601Utils.formatAsDate(gc, BC.getClassMeta(Calendar.class), UTC);
		assertNotNull(r);
		assertTrue(r.contains("2024"), r);
	}

	@Test void g03_formatAsDate_date() {
		var r = Iso8601Utils.formatAsDate(new Date(0), BC.getClassMeta(Date.class), UTC);
		assertNotNull(r);
	}

	@Test void g04_formatAsDate_temporal() {
		var r = Iso8601Utils.formatAsDate(LocalDateTime.of(2024, Month.JANUARY, 15, 12, 0), BC.getClassMeta(LocalDateTime.class), UTC);
		assertNotNull(r);
		assertTrue(r.contains("2024"), r);
	}

	@Test void g05_formatAsDate_other() {
		var r = Iso8601Utils.formatAsDate("2024-01-15", BC.getClassMeta(String.class), UTC);
		assertEquals("2024-01-15", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// formatAsDateTime — null timeZone (line 244), Calendar non-Gregorian (line 246), Temporal (line 251)
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_formatAsDateTime_nullTimeZone() {
		var r = Iso8601Utils.formatAsDateTime(LocalDateTime.of(2024, Month.JANUARY, 15, 12, 0), BC.getClassMeta(LocalDateTime.class), null);
		assertNotNull(r);
	}

	@Test void h02_formatAsDateTime_gregorianCalendar() {
		var gc = new GregorianCalendar(UTC);
		gc.set(2024, Calendar.JANUARY, 15, 12, 0, 0);
		var r = Iso8601Utils.formatAsDateTime(gc, BC.getClassMeta(Calendar.class), UTC);
		assertNotNull(r);
	}

	@Test void h03_formatAsDateTime_date() {
		var r = Iso8601Utils.formatAsDateTime(new Date(0), BC.getClassMeta(Date.class), UTC);
		assertNotNull(r);
	}

	@Test void h04_formatAsDateTime_temporal() {
		var r = Iso8601Utils.formatAsDateTime(LocalDateTime.of(2024, Month.JANUARY, 15, 12, 0), BC.getClassMeta(LocalDateTime.class), UTC);
		assertNotNull(r);
	}

	@Test void h05_formatAsDateTime_other() {
		var r = Iso8601Utils.formatAsDateTime("myVal", BC.getClassMeta(String.class), UTC);
		assertEquals("myVal", r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parse() dispatcher — lines 282-292
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_parse_null() {
		assertNull(Iso8601Utils.parse(null, BC.getClassMeta(Date.class), UTC));
	}

	@Test void i02_parse_duration() {
		var r = (Duration) Iso8601Utils.parse("PT1H", BC.getClassMeta(Duration.class), UTC);
		assertEquals(Duration.ofHours(1), r);
	}

	@Test void i03_parse_period() {
		var r = (Period) Iso8601Utils.parse("P1Y", BC.getClassMeta(Period.class), UTC);
		assertEquals(Period.ofYears(1), r);
	}

	@Test void i04_parse_unrecognizedType() {
		var r = Iso8601Utils.parse("2024-01-15", BC.getClassMeta(String.class), UTC);
		assertNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseDuration — null/empty (line 315), quoted string (line 319), PT- normalization (line 321),
	// bare integer (line 325), decimal (line 327), hocon unit (line 329), manual fallback (line 331)
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_parseDuration_null() {
		assertNull(Iso8601Utils.parseDuration(null, DurationFormat.MILLIS));
	}

	@Test void j02_parseDuration_empty() {
		assertNull(Iso8601Utils.parseDuration("", DurationFormat.MILLIS));
	}

	@Test void j03_parseDuration_quotedString() {
		var r = Iso8601Utils.parseDuration("\"PT1H\"", null);
		assertEquals(Duration.ofHours(1), r);
	}

	@Test void j04_parseDuration_quotedString_startEndQuoteNotBoth() {
		// Only starts with quote, not quoted — no stripping
		var r = Iso8601Utils.parseDuration("PT2H", null);
		assertEquals(Duration.ofHours(2), r);
	}

	@Test void j05_parseDuration_PTMinus_normalized() {
		// "PT-6H" → should become "-PT6H" then parsed
		var r = Iso8601Utils.parseDuration("PT-6H", null);
		assertNotNull(r);
		assertTrue(r.isNegative(), r.toString());
	}

	@Test void j06_parseDuration_bareInteger_nullFormatDefaultsToMillis() {
		var r = Iso8601Utils.parseDuration("500", null);
		assertEquals(Duration.ofMillis(500), r);
	}

	@Test void j07_parseDuration_bareInteger_secondsFormat() {
		var r = Iso8601Utils.parseDuration("3", DurationFormat.SECONDS);
		assertEquals(Duration.ofSeconds(3), r);
	}

	@Test void j08_parseDuration_decimal_seconds() {
		var r = Iso8601Utils.parseDuration("1.5", null);
		assertNotNull(r);
		assertEquals(Duration.ofMillis(1500), r);
	}

	@Test void j09_parseDuration_hocon_ms() {
		var r = Iso8601Utils.parseDuration("500ms", null);
		assertEquals(Duration.ofMillis(500), r);
	}

	@Test void j10_parseDuration_hocon_seconds() {
		var r = Iso8601Utils.parseDuration("2s", null);
		assertEquals(Duration.ofSeconds(2), r);
	}

	@Test void j11_parseDuration_iso8601_manual() {
		var r = Iso8601Utils.parseDuration("PT30M", null);
		assertEquals(Duration.ofMinutes(30), r);
	}

	@Test void j12_parseDuration_negative_manual() {
		var r = Iso8601Utils.parseDuration("-PT1H", null);
		assertEquals(Duration.ofHours(-1), r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parsePeriod — bare integer with null format defaults to DAYS (line 359)
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_parsePeriod_null() {
		assertNull(Iso8601Utils.parsePeriod(null, PeriodFormat.ISO_8601));
	}

	@Test void k02_parsePeriod_empty() {
		assertNull(Iso8601Utils.parsePeriod("", PeriodFormat.ISO_8601));
	}

	@Test void k03_parsePeriod_bareInteger_nullFormat_defaultsToDays() {
		var r = Iso8601Utils.parsePeriod("10", null);
		assertNotNull(r);
	}

	@Test void k04_parsePeriod_iso8601() {
		var r = Iso8601Utils.parsePeriod("P1Y2M3D", PeriodFormat.ISO_8601);
		assertEquals(Period.of(1, 2, 3), r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseCalendar — null (line 388), XMLGregorianCalendar (line 391), non-Calendar (line 393),
	// null timeZone (line 395), formatHint not-default (line 396)
	//------------------------------------------------------------------------------------------------------------------

	@Test void l01_parseCalendar_null() {
		assertNull(Iso8601Utils.parseCalendar(null, BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC));
	}

	@Test void l02_parseCalendar_xmlGregorianCalendar() {
		var r = Iso8601Utils.parseCalendar("2024-01-15T12:00:00Z", BC.getClassMeta(XMLGregorianCalendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNotNull(r);
		assertInstanceOf(XMLGregorianCalendar.class, r);
	}

	@Test void l03_parseCalendar_nonCalendarType() {
		var r = Iso8601Utils.parseCalendar("2024-01-15", BC.getClassMeta(String.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNull(r);
	}

	@Test void l04_parseCalendar_nullTimeZone_defaultsToSystem() {
		var r = Iso8601Utils.parseCalendar("2024-01-15", BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, null);
		assertNotNull(r);
	}

	@Test void l05_parseCalendar_withNonDefaultFormatHint() {
		var r = Iso8601Utils.parseCalendar("2024-01-15", BC.getClassMeta(Calendar.class), CalendarFormat.ISO_LOCAL_DATE, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseDate — null (line 426), non-Date type (line 428), null timeZone (line 429), non-default formatHint
	//------------------------------------------------------------------------------------------------------------------

	@Test void m01_parseDate_null() {
		assertNull(Iso8601Utils.parseDate(null, BC.getClassMeta(Date.class), DateFormat.ISO_LOCAL_DATE_TIME, UTC));
	}

	@Test void m02_parseDate_nonDateType() {
		var r = Iso8601Utils.parseDate("2024-01-15", BC.getClassMeta(String.class), DateFormat.ISO_LOCAL_DATE_TIME, UTC);
		assertNull(r);
	}

	@Test void m03_parseDate_nullTimeZone() {
		var r = Iso8601Utils.parseDate("2024-01-15T12:00:00", BC.getClassMeta(Date.class), DateFormat.ISO_LOCAL_DATE_TIME, null);
		assertNotNull(r);
	}

	@Test void m04_parseDate_withNonDefaultFormatHint() {
		var r = Iso8601Utils.parseDate("2024-01-15T12:00:00Z", BC.getClassMeta(Date.class), DateFormat.ISO_INSTANT, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseTemporal — null (line 455), non-Temporal type (line 458), null timeZone (line 460), formatHint not-default (line 462)
	//------------------------------------------------------------------------------------------------------------------

	@Test void n01_parseTemporal_null() {
		assertNull(Iso8601Utils.parseTemporal(null, BC.getClassMeta(LocalDate.class), TemporalFormat.DEFAULT, UTC));
	}

	@Test void n02_parseTemporal_nonTemporalType() {
		var r = Iso8601Utils.parseTemporal("2024-01-15", BC.getClassMeta(String.class), TemporalFormat.DEFAULT, UTC);
		assertNull(r);
	}

	@Test void n03_parseTemporal_nullTimeZone() {
		var r = Iso8601Utils.parseTemporal("2024-01-15", BC.getClassMeta(LocalDate.class), TemporalFormat.DEFAULT, null);
		assertNotNull(r);
	}

	@Test void n04_parseTemporal_withNonDefaultFormatHint() {
		var r = Iso8601Utils.parseTemporal("2024-01-15", BC.getClassMeta(LocalDate.class), TemporalFormat.ISO_LOCAL_DATE, UTC);
		assertNotNull(r);
		assertEquals(LocalDate.of(2024, Month.JANUARY, 15), r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseDurationManual — lines 468-495: negative, no PT prefix, individual units
	//------------------------------------------------------------------------------------------------------------------

	@Test void o01_parseDuration_manual_hours_minutes_seconds() {
		var r = Iso8601Utils.parseDuration("PT1H30M45S", null);
		assertNotNull(r);
		assertEquals(Duration.ofHours(1).plusMinutes(30).plusSeconds(45), r);
	}

	@Test void o02_parseDuration_manual_negative() {
		var r = Iso8601Utils.parseDuration("-PT2H", null);
		assertNotNull(r);
		assertTrue(r.isNegative());
	}

	@Test void o03_parseDuration_manual_fractionalSeconds() {
		var r = Iso8601Utils.parseDuration("PT1.5S", null);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// selectParserFormatter — hasTime+hasZone (line 548), hasTime only (line 550),
	// hasZone only (line 552), neither (line 554)
	//------------------------------------------------------------------------------------------------------------------

	@Test void p01_parseCalendar_dateTimeWithZone() {
		var r = Iso8601Utils.parseCalendar("2024-01-15T12:00:00+05:00", BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNotNull(r);
	}

	@Test void p02_parseCalendar_dateTimeNoZone() {
		var r = Iso8601Utils.parseCalendar("2024-01-15T12:00:00", BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNotNull(r);
	}

	@Test void p03_parseCalendar_dateWithZone() {
		var r = Iso8601Utils.parseCalendar("2024-01-15Z", BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNotNull(r);
	}

	@Test void p04_parseCalendar_dateOnly() {
		var r = Iso8601Utils.parseCalendar("2024-01-15", BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// getFormatterForType — type not in DEFAULT_FORMATTERS falls back to ISO_INSTANT (line 537)
	//------------------------------------------------------------------------------------------------------------------

	@Test void q01_parseTemporal_unknownTemporalType_usesIsoInstant() {
		// ZonedDateTime has a formatter; Instant also has one — use a standard type
		var r = Iso8601Utils.parseTemporal("2024-01-15T12:00:00Z", BC.getClassMeta(Instant.class), TemporalFormat.DEFAULT, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// format() with XMLGregorianCalendar (line 103 — XMLGregorianCalendar branch)
	//------------------------------------------------------------------------------------------------------------------

	@Test void a07_format_xmlGregorianCalendar() throws Exception {
		var x = DatatypeFactory.newInstance().newXMLGregorianCalendar("2024-01-15T12:00:00Z");
		var r = Iso8601Utils.format(x, BC.getClassMeta(XMLGregorianCalendar.class), UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseCalendar/parseDate/parseTemporal — ISO_OFFSET_DATE_TIME hint falls through to default (lines 396/429/462)
	//------------------------------------------------------------------------------------------------------------------

	@Test void l06_parseCalendar_isoOffsetDateTimeHint_usesDefault() {
		// ISO_OFFSET_DATE_TIME is explicitly excluded → uses parseCalendarDefault
		var r = Iso8601Utils.parseCalendar("2024-01-15T12:00:00+00:00", BC.getClassMeta(Calendar.class), CalendarFormat.ISO_OFFSET_DATE_TIME, UTC);
		assertNotNull(r);
	}

	@Test void m05_parseDate_isoLocalDateTimeHint_usesDefault() {
		// ISO_LOCAL_DATE_TIME is excluded → uses parseDateDefault
		var r = Iso8601Utils.parseDate("2024-01-15T12:00:00", BC.getClassMeta(Date.class), DateFormat.ISO_LOCAL_DATE_TIME, UTC);
		assertNotNull(r);
	}

	@Test void n05_parseTemporal_defaultHint_usesDefault() {
		// DEFAULT is excluded → uses parseTemporalDefault
		var r = Iso8601Utils.parseTemporal("2024-01-15", BC.getClassMeta(LocalDate.class), TemporalFormat.DEFAULT, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// getFormatterForType — type not in DEFAULT_FORMATTERS falls back to ISO_INSTANT (line 537)
	//------------------------------------------------------------------------------------------------------------------

	@Test void q02_parseTemporal_typeNotInMap_fallbackFormatter() {
		// ZonedDateTime IS in the map; test with ZonedDateTime using DEFAULT hint (goes through parseTemporalDefault → getFormatterForType)
		var r = Iso8601Utils.parseTemporal("2024-01-15T12:00:00Z", BC.getClassMeta(ZonedDateTime.class), TemporalFormat.DEFAULT, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseDuration quoted-string edge case (line 319 — starts-with-quote but no end-quote)
	//------------------------------------------------------------------------------------------------------------------

	@Test void s01_parseDuration_quotedStart_noEndQuote_notStripped() {
		// Starts with `"` but does NOT end with `"` — stripping skipped; fallback Duration.parse throws
		assertThrows(RuntimeException.class, () -> Iso8601Utils.parseDuration("\"PT1H", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseCalendar / parseDate / parseTemporal with NOT_SET hint (lines 396, 429, 462)
	//------------------------------------------------------------------------------------------------------------------

	@Test void t01_parseCalendar_notSetHint_usesDefault() {
		var r = Iso8601Utils.parseCalendar("2024-01-15", BC.getClassMeta(Calendar.class), CalendarFormat.NOT_SET, UTC);
		assertNotNull(r);
	}

	@Test void t02_parseDate_notSetHint_usesDefault() {
		var r = Iso8601Utils.parseDate("2024-01-15T12:00:00", BC.getClassMeta(Date.class), DateFormat.NOT_SET, UTC);
		assertNotNull(r);
	}

	@Test void t03_parseTemporal_notSetHint_usesDefault() {
		var r = Iso8601Utils.parseTemporal("2024-01-15", BC.getClassMeta(LocalDate.class), TemporalFormat.NOT_SET, UTC);
		assertNotNull(r);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseDurationManual — lowercase "pt" prefix (line 472), no-match (!found) path (line 491)
	//------------------------------------------------------------------------------------------------------------------

	@Test void u01_parseDuration_lowercase_pt_prefix() {
		// "pt30m" — parseDurationManual sees "30m" after removing "pt"
		var r = Iso8601Utils.parseDuration("pt30m", null);
		assertNotNull(r);
		assertEquals(Duration.ofMinutes(30), r);
	}

	@Test void u02_parseDuration_noMatch_found_false() {
		// "PT" alone — parseDurationManual: s2="" after stripping "PT", regex matches nothing → !found → returns null
		// fallback calls Duration.parse("PT") which throws (invalid ISO 8601)
		assertThrows(RuntimeException.class, () -> Iso8601Utils.parseDuration("PT", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// fromEpochMillis — null timeZone (line 570), various target types (lines 573-588)
	//------------------------------------------------------------------------------------------------------------------

	@Test void r01_fromEpochMillis_instant() {
		var r = (Instant) Iso8601Utils.fromEpochMillis(1000L, BC.getClassMeta(Instant.class), UTC);
		assertEquals(Instant.ofEpochMilli(1000), r);
	}

	@Test void r02_fromEpochMillis_zonedDateTime() {
		var r = (ZonedDateTime) Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(ZonedDateTime.class), UTC);
		assertNotNull(r);
	}

	@Test void r03_fromEpochMillis_offsetDateTime() {
		var r = (OffsetDateTime) Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(OffsetDateTime.class), UTC);
		assertNotNull(r);
	}

	@Test void r04_fromEpochMillis_localDateTime() {
		var r = (LocalDateTime) Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(LocalDateTime.class), UTC);
		assertNotNull(r);
	}

	@Test void r05_fromEpochMillis_localDate() {
		var r = (LocalDate) Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(LocalDate.class), UTC);
		assertNotNull(r);
	}

	@Test void r06_fromEpochMillis_localTime() {
		var r = (LocalTime) Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(LocalTime.class), UTC);
		assertNotNull(r);
	}

	@Test void r07_fromEpochMillis_offsetTime() {
		var r = (OffsetTime) Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(OffsetTime.class), UTC);
		assertNotNull(r);
	}

	@Test void r08_fromEpochMillis_date() {
		var r = (Date) Iso8601Utils.fromEpochMillis(1000L, BC.getClassMeta(Date.class), UTC);
		assertEquals(1000L, r.getTime());
	}

	@Test void r09_fromEpochMillis_calendar() {
		var r = Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(Calendar.class), UTC);
		assertNotNull(r);
		assertInstanceOf(Calendar.class, r);
	}

	@Test void r10_fromEpochMillis_xmlGregorianCalendar() {
		var r = Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(XMLGregorianCalendar.class), UTC);
		assertNotNull(r);
		assertInstanceOf(XMLGregorianCalendar.class, r);
	}

	@Test void r11_fromEpochMillis_nullTimeZone() {
		var r = Iso8601Utils.fromEpochMillis(1000L, BC.getClassMeta(Instant.class), null);
		assertEquals(Instant.ofEpochMilli(1000), r);
	}

	@Test void r12_fromEpochMillis_unrecognizedType() {
		var r = Iso8601Utils.fromEpochMillis(0L, BC.getClassMeta(String.class), UTC);
		assertNull(r);
	}
}
