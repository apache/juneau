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

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

/**
 * Precedence chain tests for the new Phase 1 format settings:
 * {@code @MarshalledProp} &gt; {@code @Marshalled} &gt; {@code MarshallingContext} &gt; default.
 */
class DateTimeLocaleFormatPlacement_Test {

	private static final Instant T = Instant.parse("2012-12-21T12:34:56Z");

	@BeforeAll static void beforeAll() {
		TestUtils.setTimeZone("GMT");
	}

	@AfterAll static void afterAll() {
		TestUtils.unsetTimeZone();
	}

	//------------------------------------------------------------------------------------------------------------------
	// CalendarFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 { public Calendar c = GregorianCalendar.from(T.atZone(ZoneId.of("Z"))); }

	@Test void a01_calendar_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().calendarFormat(CalendarFormat.ISO_INSTANT).build();
		assertEquals("{c:'2012-12-21T12:34:56Z'}", s.serialize(new A01()));
	}

	@Marshalled(calendarFormat = CalendarFormat.MILLIS)
	public static class A02 { public Calendar c = GregorianCalendar.from(T.atZone(ZoneId.of("Z"))); }

	@Test void a02_calendar_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().calendarFormat(CalendarFormat.ISO_INSTANT).build();
		assertEquals("{c:" + T.toEpochMilli() + "}", s.serialize(new A02()));
	}

	@Marshalled(calendarFormat = CalendarFormat.ISO_INSTANT)
	public static class A03 {
		@MarshalledProp(calendarFormat = CalendarFormat.MILLIS)
		public Calendar c = GregorianCalendar.from(T.atZone(ZoneId.of("Z")));
	}

	@Test void a03_calendar_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{c:" + T.toEpochMilli() + "}", s.serialize(new A03()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// DateFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class B01 { public Date d = Date.from(T); }

	@Test void b01_date_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().dateFormat(DateFormat.ISO_INSTANT).build();
		assertEquals("{d:'2012-12-21T12:34:56Z'}", s.serialize(new B01()));
	}

	@Marshalled(dateFormat = DateFormat.MILLIS)
	public static class B02 { public Date d = Date.from(T); }

	@Test void b02_date_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().dateFormat(DateFormat.ISO_INSTANT).build();
		assertEquals("{d:" + T.toEpochMilli() + "}", s.serialize(new B02()));
	}

	@Marshalled(dateFormat = DateFormat.ISO_INSTANT)
	public static class B03 {
		@MarshalledProp(dateFormat = DateFormat.MILLIS)
		public Date d = Date.from(T);
	}

	@Test void b03_date_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{d:" + T.toEpochMilli() + "}", s.serialize(new B03()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// TemporalFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class C01 { public Instant i = T; }

	@Test void c01_temporal_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().temporalFormat(TemporalFormat.ISO_INSTANT).build();
		assertEquals("{i:'2012-12-21T12:34:56Z'}", s.serialize(new C01()));
	}

	@Marshalled(temporalFormat = TemporalFormat.MILLIS)
	public static class C02 { public Instant i = T; }

	@Test void c02_temporal_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().temporalFormat(TemporalFormat.ISO_INSTANT).build();
		assertEquals("{i:" + T.toEpochMilli() + "}", s.serialize(new C02()));
	}

	@Marshalled(temporalFormat = TemporalFormat.ISO_INSTANT)
	public static class C03 {
		@MarshalledProp(temporalFormat = TemporalFormat.MILLIS)
		public Instant i = T;
	}

	@Test void c03_temporal_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{i:" + T.toEpochMilli() + "}", s.serialize(new C03()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// TimeZoneFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class D01 { public ZoneId z = ZoneId.of("America/Los_Angeles"); }

	@Test void d01_timeZone_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().timeZoneFormat(TimeZoneFormat.OFFSET).build();
		var json = (String) s.serialize(new D01());
		assertTrue(json.startsWith("{z:'") && json.endsWith("'}"), "Unexpected: " + json);
		assertFalse(json.contains("America/Los_Angeles"), "OFFSET should not emit IANA id: " + json);
	}

	@Marshalled(timeZoneFormat = TimeZoneFormat.OFFSET)
	public static class D02 { public ZoneId z = ZoneId.of("America/Los_Angeles"); }

	@Test void d02_timeZone_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().timeZoneFormat(TimeZoneFormat.ID).build();
		var json = (String) s.serialize(new D02());
		assertFalse(json.contains("America/Los_Angeles"), "Class-level OFFSET should win: " + json);
	}

	@Marshalled(timeZoneFormat = TimeZoneFormat.ID)
	public static class D03 {
		@MarshalledProp(timeZoneFormat = TimeZoneFormat.OFFSET)
		public ZoneId z = ZoneId.of("America/Los_Angeles");
	}

	@Test void d03_timeZone_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		var json = (String) s.serialize(new D03());
		assertFalse(json.contains("America/Los_Angeles"), "Property-level OFFSET should win: " + json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// LocaleFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class E01 { public Locale l = Locale.US; }

	@Test void e01_locale_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().localeFormat(LocaleFormat.UNDERSCORE).build();
		assertEquals("{l:'en_US'}", s.serialize(new E01()));
	}

	@Marshalled(localeFormat = LocaleFormat.UNDERSCORE)
	public static class E02 { public Locale l = Locale.US; }

	@Test void e02_locale_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().localeFormat(LocaleFormat.BCP_47).build();
		assertEquals("{l:'en_US'}", s.serialize(new E02()));
	}

	@Marshalled(localeFormat = LocaleFormat.BCP_47)
	public static class E03 {
		@MarshalledProp(localeFormat = LocaleFormat.UNDERSCORE)
		public Locale l = Locale.US;
	}

	@Test void e03_locale_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{l:'en_US'}", s.serialize(new E03()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @MarshalledConfig integration
	//------------------------------------------------------------------------------------------------------------------

	@MarshalledConfig(
		calendarFormat = CalendarFormat.MILLIS,
		dateFormat = DateFormat.MILLIS,
		temporalFormat = TemporalFormat.ISO_INSTANT,
		timeZoneFormat = TimeZoneFormat.OFFSET,
		localeFormat = LocaleFormat.UNDERSCORE
	)
	static class F01Config {}

	public static class F01Bean {
		public Calendar c = GregorianCalendar.from(T.atZone(ZoneId.of("Z")));
		public Date d = Date.from(T);
		public Instant i = T;
		public Locale l = Locale.US;
	}

	@Test void f01_marshalledConfig_applies() throws Exception {
		var s = Json5Serializer.create().applyAnnotations(F01Config.class).build();
		var json = (String) s.serialize(new F01Bean());
		assertTrue(json.contains("c:" + T.toEpochMilli()), "calendar millis: " + json);
		assertTrue(json.contains("d:" + T.toEpochMilli()), "date millis: " + json);
		assertTrue(json.contains("i:'2012-12-21T12:34:56Z'"), "temporal iso instant: " + json);
		assertTrue(json.contains("l:'en_US'"), "locale underscore: " + json);
	}
}
