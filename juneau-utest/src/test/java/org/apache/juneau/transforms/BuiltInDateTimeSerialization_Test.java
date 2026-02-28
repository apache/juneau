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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Tests built-in first-class date/time and Duration serialization and round-trip parsing.
 */
class BuiltInDateTimeSerialization_Test extends TestBase {

	@BeforeAll static void beforeClass() {
		setTimeZone("GMT-5");
	}

	@AfterAll static void afterClass() {
		unsetTimeZone();
	}

	private static final WriterSerializer JS = Json5Serializer.DEFAULT;
	private static final ReaderParser JP = Json5Parser.DEFAULT;

	//-----------------------------------------------------------------------------------------------------------------
	// Instant
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_json_instant() throws Exception {
		var i = Instant.parse("2012-12-21T12:34:56Z");
		var json = JS.serialize(i);
		assertEquals("'2012-12-21T12:34:56Z'", json);
		var i2 = JP.parse(json, Instant.class);
		assertEquals(i, i2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ZonedDateTime
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_json_zonedDateTime() throws Exception {
		var zdt = ZonedDateTime.parse("2012-12-21T12:34:56Z");
		var json = JS.serialize(zdt);
		assertEquals("'2012-12-21T12:34:56Z'", json);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// LocalDate
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_json_localDate() throws Exception {
		var ld = LocalDate.parse("2012-12-21");
		var json = JS.serialize(ld);
		assertEquals("'2012-12-21'", json);
		var ld2 = JP.parse(json, LocalDate.class);
		assertEquals(ld, ld2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// LocalDateTime
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_json_localDateTime() throws Exception {
		var ldt = LocalDateTime.parse("2012-12-21T12:34:56");
		var json = JS.serialize(ldt);
		assertEquals("'2012-12-21T12:34:56'", json);
		var ldt2 = JP.parse(json, LocalDateTime.class);
		assertEquals(ldt, ldt2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// LocalTime
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_json_localTime() throws Exception {
		var lt = LocalTime.parse("12:34:56");
		var json = JS.serialize(lt);
		assertEquals("'12:34:56'", json);
		var lt2 = JP.parse(json, LocalTime.class);
		assertEquals(lt, lt2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OffsetDateTime
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_json_offsetDateTime() throws Exception {
		var odt = OffsetDateTime.parse("2012-12-21T12:34:56-05:00");
		var json = JS.serialize(odt);
		assertEquals("'2012-12-21T12:34:56-05:00'", json);
		var odt2 = JP.parse(json, OffsetDateTime.class);
		assertEquals(odt, odt2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// OffsetTime
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_json_offsetTime() throws Exception {
		var ot = OffsetTime.parse("12:34:56-05:00");
		var json = JS.serialize(ot);
		assertEquals("'12:34:56-05:00'", json);
		var ot2 = JP.parse(json, OffsetTime.class);
		assertEquals(ot, ot2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Year
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_json_year() throws Exception {
		var y = Year.parse("2012");
		var json = JS.serialize(y);
		assertEquals("'2012'", json);
		var y2 = JP.parse(json, Year.class);
		assertEquals(y, y2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// YearMonth
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_json_yearMonth() throws Exception {
		var ym = YearMonth.parse("2012-12");
		var json = JS.serialize(ym);
		assertEquals("'2012-12'", json);
		var ym2 = JP.parse(json, YearMonth.class);
		assertEquals(ym, ym2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Calendar
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_json_calendar() throws Exception {
		var c = GregorianCalendar.from(ZonedDateTime.parse("2012-12-21T12:34:56Z"));
		var json = JS.serialize(c);
		assertEquals("'2012-12-21T12:34:56Z'", json);
		var c2 = JP.parse(json, Calendar.class);
		assertEquals(c.getTimeInMillis(), c2.getTimeInMillis());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Date
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_json_date() throws Exception {
		var d = Date.from(Instant.from(DateTimeFormatter.ISO_INSTANT.parse("2012-12-21T12:34:56Z")));
		var json = JS.serialize(d);
		assertEquals("'2012-12-21T07:34:56'", json);
		var d2 = JP.parse(json, Date.class);
		assertNotNull(d2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Duration
	//-----------------------------------------------------------------------------------------------------------------

	@Test void l01_json_duration_basic() throws Exception {
		var d = Duration.ofHours(1).plusMinutes(30);
		var json = JS.serialize(d);
		assertEquals("'PT1H30M'", json);
		var d2 = JP.parse(json, Duration.class);
		assertEquals(d, d2);
	}

	@Test void l02_json_duration_hours() throws Exception {
		var d = Duration.ofHours(48);
		var json = JS.serialize(d);
		assertEquals("'PT48H'", json);
		var d2 = JP.parse(json, Duration.class);
		assertEquals(d, d2);
	}

	@Test void l03_json_duration_seconds() throws Exception {
		var d = Duration.ofSeconds(45);
		var json = JS.serialize(d);
		assertEquals("'PT45S'", json);
		var d2 = JP.parse(json, Duration.class);
		assertEquals(d, d2);
	}

	@Test void l04_json_duration_zero() throws Exception {
		var d = Duration.ZERO;
		var json = JS.serialize(d);
		assertEquals("'PT0S'", json);
		var d2 = JP.parse(json, Duration.class);
		assertEquals(d, d2);
	}

	@Test void l05_json_duration_negative() throws Exception {
		var d = Duration.ofHours(-6);
		var json = JS.serialize(d);
		assertEquals("'PT-6H'", json);
		var d2 = JP.parse(json, Duration.class);
		assertEquals(d, d2);
	}

	@Test void l06_json_duration_fractionalSeconds() throws Exception {
		var d = Duration.ofSeconds(20, 345000000);
		var json = JS.serialize(d);
		assertEquals("'PT20.345S'", json);
		var d2 = JP.parse(json, Duration.class);
		assertEquals(d, d2);
	}

	@Test void l07_json_duration_complex() throws Exception {
		var d = Duration.ofHours(26).plusMinutes(3);
		var json = JS.serialize(d);
		assertEquals("'PT26H3M'", json);
		var d2 = JP.parse(json, Duration.class);
		assertEquals(d, d2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean with Duration field
	//-----------------------------------------------------------------------------------------------------------------

	public static class DurationBean {
		public Duration timeout;
		public Duration interval;
	}

	@Test void m01_json_durationBean() throws Exception {
		var bean = new DurationBean();
		bean.timeout = Duration.ofHours(1).plusMinutes(30);
		bean.interval = Duration.ofSeconds(45);

		var json = JS.serialize(bean);
		assertTrue(json.contains("'PT1H30M'"));
		assertTrue(json.contains("'PT45S'"));

		var bean2 = JP.parse(json, DurationBean.class);
		assertEquals(bean.timeout, bean2.timeout);
		assertEquals(bean.interval, bean2.interval);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Null handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test void n01_json_nullDuration() throws Exception {
		var bean = new DurationBean();
		bean.timeout = null;
		bean.interval = null;

		var json = JS.serialize(bean);
		var bean2 = JP.parse(json, DurationBean.class);
		assertNull(bean2.timeout);
		assertNull(bean2.interval);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean with mixed date/time and Duration fields
	//-----------------------------------------------------------------------------------------------------------------

	public static class MixedBean {
		public Instant created;
		public LocalDate date;
		public Duration timeout;
	}

	@Test void o01_json_mixedBean() throws Exception {
		var bean = new MixedBean();
		bean.created = Instant.parse("2012-12-21T12:34:56Z");
		bean.date = LocalDate.parse("2012-12-21");
		bean.timeout = Duration.ofHours(1).plusMinutes(30);

		var json = JS.serialize(bean);
		assertTrue(json.contains("'2012-12-21T12:34:56Z'"));
		assertTrue(json.contains("'2012-12-21'"));
		assertTrue(json.contains("'PT1H30M'"));

		var bean2 = JP.parse(json, MixedBean.class);
		assertEquals(bean.created, bean2.created);
		assertEquals(bean.date, bean2.date);
		assertEquals(bean.timeout, bean2.timeout);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Collections of dates
	//-----------------------------------------------------------------------------------------------------------------

	@Test void p01_json_listOfLocalDates() throws Exception {
		var dates = java.util.List.of(LocalDate.parse("2012-12-21"), LocalDate.parse("2013-01-15"));
		var json = JS.serialize(dates);
		assertTrue(json.contains("'2012-12-21'"));
		assertTrue(json.contains("'2013-01-15'"));
	}
}
