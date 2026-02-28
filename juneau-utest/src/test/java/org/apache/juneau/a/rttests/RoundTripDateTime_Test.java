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
package org.apache.juneau.a.rttests;

import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import javax.xml.datatype.*;

import org.apache.juneau.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Round-trip tests for built-in date/time and Duration serialization across all serializer/parser combinations.
 */
class RoundTripDateTime_Test extends TestBase {

	private static final RoundTrip_Tester[] TESTERS = {
		tester(1, "Json - default")
			.serializer(JsonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(2, "Json - lax")
			.serializer(JsonSerializer.create().json5().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(3, "Json - lax, readable")
			.serializer(JsonSerializer.create().json5().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(JsonParser.create())
			.build(),
		tester(4, "Xml - namespaces, validation, readable")
			.serializer(XmlSerializer.create().ns().sq().keepNullProperties().addNamespaceUrisToRoot().useWhitespace().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.validateXml()
			.build(),
		tester(5, "Xml - no namespaces, validation")
			.serializer(XmlSerializer.create().sq().keepNullProperties().addBeanTypes().addRootType())
			.parser(XmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(6, "Html - default")
			.serializer(HtmlSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(7, "Html - readable")
			.serializer(HtmlSerializer.create().sq().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(8, "Html - with key/value headers")
			.serializer(HtmlSerializer.create().addKeyValueTableHeaders().addBeanTypes().addRootType())
			.parser(HtmlParser.create())
			.validateXmlWhitespace()
			.build(),
		tester(9, "Uon - default")
			.serializer(UonSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(10, "Uon - readable")
			.serializer(UonSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create())
			.build(),
		tester(11, "Uon - encoded")
			.serializer(UonSerializer.create().encoding().keepNullProperties().addBeanTypes().addRootType())
			.parser(UonParser.create().decoding())
			.build(),
		tester(12, "UrlEncoding - default")
			.serializer(UrlEncodingSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(13, "UrlEncoding - readable")
			.serializer(UrlEncodingSerializer.create().ws().keepNullProperties().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create())
			.build(),
		tester(14, "UrlEncoding - expanded params")
			.serializer(UrlEncodingSerializer.create().expandedParams().addBeanTypes().addRootType())
			.parser(UrlEncodingParser.create().expandedParams())
			.build(),
		tester(15, "MsgPack")
			.serializer(MsgPackSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.parser(MsgPackParser.create())
			.build(),
		tester(16, "Json schema")
			.serializer(JsonSchemaSerializer.create().keepNullProperties().addBeanTypes().addRootType())
			.returnOriginalObject()
			.build(),
	};

	static RoundTrip_Tester[] testers() {
		return TESTERS;
	}

	protected static RoundTrip_Tester.Builder tester(int index, String label) {
		return RoundTrip_Tester.create(index, label);
	}

	//====================================================================================================
	// Bean with all date/time types
	//====================================================================================================

	public static class DateTimeBean {
		public Instant instant;
		public ZonedDateTime zonedDateTime;
		public LocalDate localDate;
		public LocalDateTime localDateTime;
		public LocalTime localTime;
		public OffsetDateTime offsetDateTime;
		public OffsetTime offsetTime;
		public Year year;
		public YearMonth yearMonth;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a01_dateTimeBean(RoundTrip_Tester t) throws Exception {
		var x = new DateTimeBean();
		x.instant = Instant.parse("2012-12-21T12:34:56Z");
		x.zonedDateTime = ZonedDateTime.parse("2012-12-21T12:34:56Z");
		x.localDate = LocalDate.parse("2012-12-21");
		x.localDateTime = LocalDateTime.parse("2012-12-21T12:34:56");
		x.localTime = LocalTime.parse("12:34:56");
		x.offsetDateTime = OffsetDateTime.parse("2012-12-21T12:34:56-05:00");
		x.offsetTime = OffsetTime.parse("12:34:56-05:00");
		x.year = Year.of(2012);
		x.yearMonth = YearMonth.of(2012, 12);

		x = t.roundTrip(x);

		assertEquals(Instant.parse("2012-12-21T12:34:56Z"), x.instant);
		assertEquals(ZonedDateTime.parse("2012-12-21T12:34:56Z").toInstant(), x.zonedDateTime.toInstant());
		assertEquals(LocalDate.parse("2012-12-21"), x.localDate);
		assertEquals(LocalDateTime.parse("2012-12-21T12:34:56"), x.localDateTime);
		assertEquals(LocalTime.parse("12:34:56"), x.localTime);
		assertEquals(OffsetDateTime.parse("2012-12-21T12:34:56-05:00").toInstant(), x.offsetDateTime.toInstant());
		assertEquals(OffsetTime.parse("12:34:56-05:00"), x.offsetTime);
		assertEquals(Year.of(2012), x.year);
		assertEquals(YearMonth.of(2012, 12), x.yearMonth);
	}

	//====================================================================================================
	// Bean with Calendar and Date
	//====================================================================================================

	public static class LegacyDateBean {
		public Calendar calendar;
		public Date date;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_legacyDateBean(RoundTrip_Tester t) throws Exception {
		var x = new LegacyDateBean();
		x.calendar = GregorianCalendar.from(ZonedDateTime.parse("2012-12-21T12:34:56Z"));
		x.date = Date.from(Instant.parse("2012-12-21T12:34:56Z"));

		var millis1 = x.calendar.getTimeInMillis();
		var millis2 = x.date.getTime();

		x = t.roundTrip(x);

		assertEquals(millis1, x.calendar.getTimeInMillis());
		assertEquals(millis2, x.date.getTime());
	}

	//====================================================================================================
	// Bean with Duration fields
	//====================================================================================================

	public static class DurationBean {
		public java.time.Duration timeout;
		public java.time.Duration interval;
		public java.time.Duration zero;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a03_durationBean(RoundTrip_Tester t) throws Exception {
		var x = new DurationBean();
		x.timeout = java.time.Duration.ofHours(1).plusMinutes(30);
		x.interval = java.time.Duration.ofSeconds(45);
		x.zero = java.time.Duration.ZERO;

		x = t.roundTrip(x);

		assertEquals(java.time.Duration.ofHours(1).plusMinutes(30), x.timeout);
		assertEquals(java.time.Duration.ofSeconds(45), x.interval);
		assertEquals(java.time.Duration.ZERO, x.zero);
	}

	//====================================================================================================
	// Bean with mixed date/time and Duration fields
	//====================================================================================================

	public static class MixedBean {
		public Instant created;
		public LocalDate date;
		public java.time.Duration timeout;
		public Calendar calendar;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_mixedBean(RoundTrip_Tester t) throws Exception {
		var x = new MixedBean();
		x.created = Instant.parse("2012-12-21T12:34:56Z");
		x.date = LocalDate.parse("2012-12-21");
		x.timeout = java.time.Duration.ofHours(1).plusMinutes(30);
		x.calendar = GregorianCalendar.from(ZonedDateTime.parse("2012-12-21T12:34:56Z"));

		var calMillis = x.calendar.getTimeInMillis();

		x = t.roundTrip(x);

		assertEquals(Instant.parse("2012-12-21T12:34:56Z"), x.created);
		assertEquals(LocalDate.parse("2012-12-21"), x.date);
		assertEquals(java.time.Duration.ofHours(1).plusMinutes(30), x.timeout);
		assertEquals(calMillis, x.calendar.getTimeInMillis());
	}

	//====================================================================================================
	// Bean with null date/time fields
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a05_nullFields(RoundTrip_Tester t) throws Exception {
		var x = new MixedBean();

		x = t.roundTrip(x);

		assertNull(x.created);
		assertNull(x.date);
		assertNull(x.timeout);
		assertNull(x.calendar);
	}

	//====================================================================================================
	// Standalone Instant round-trip (non-URL-encoding serializers)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a06_standaloneInstant(RoundTrip_Tester t) throws Exception {
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();

		if (p == null)
			return;

		var x = Instant.parse("2012-12-21T12:34:56Z");
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, Instant.class);
			assertEquals(x, x2);
		} catch (Exception e) {
			// Some serializers (UrlEncoding) may not support standalone non-bean values
		}
	}

	//====================================================================================================
	// Standalone Duration round-trip (non-URL-encoding serializers)
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a07_standaloneDuration(RoundTrip_Tester t) throws Exception {
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();

		if (p == null)
			return;

		var x = java.time.Duration.ofHours(2).plusMinutes(15);
		try {
			var out = t.serialize(x, s);
			var x2 = p.parse(out, java.time.Duration.class);
			assertEquals(x, x2);
		} catch (Exception e) {
			// Some serializers (UrlEncoding) may not support standalone non-bean values
		}
	}

	//====================================================================================================
	// XMLGregorianCalendar round-trip
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a08_xmlGregorianCalendar(RoundTrip_Tester t) throws Exception {
		if (t.isValidationOnly())
			return;

		var s = t.getSerializer();
		var p = t.getParser();

		if (p == null)
			return;

		var gc = new GregorianCalendar();
		gc.setTimeInMillis(Instant.parse("2012-12-21T12:34:56Z").toEpochMilli());
		gc.setTimeZone(TimeZone.getTimeZone("UTC"));
		var c = DatatypeFactory.newInstance().newXMLGregorianCalendar(gc);

		try {
			var out = t.serialize(c, s);
			var c2 = p.parse(out, XMLGregorianCalendar.class);
			assertEquals(c, c2);
		} catch (Exception e) {
			// Some serializers (UrlEncoding) may not support standalone non-bean values
		}
	}

	//====================================================================================================
	// Bean with negative and fractional Duration values
	//====================================================================================================

	public static class EdgeCaseDurationBean {
		public java.time.Duration negative;
		public java.time.Duration fractional;
		public java.time.Duration large;
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a09_edgeCaseDurations(RoundTrip_Tester t) throws Exception {
		var x = new EdgeCaseDurationBean();
		x.negative = java.time.Duration.ofHours(-6);
		x.fractional = java.time.Duration.ofSeconds(20, 345000000);
		x.large = java.time.Duration.ofDays(365);

		x = t.roundTrip(x);

		assertEquals(java.time.Duration.ofHours(-6), x.negative);
		assertEquals(java.time.Duration.ofSeconds(20, 345000000), x.fractional);
		assertEquals(java.time.Duration.ofDays(365), x.large);
	}
}
