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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"java:S5778" /* assertThrows lambdas with chained calls; intermediate invocations do not throw in practice */})
class CalendarFormat_Test {

	private static final ZoneId Z = ZoneId.of("Z");
	private static final Calendar C = GregorianCalendar.from(ZonedDateTime.parse("2011-12-03T10:15:30Z"));

	@Test void a01_isoVariants() {
		assertEquals("2011-12-03T10:15:30Z", CalendarFormat.ISO_OFFSET_DATE_TIME.format(C, Z));
		assertEquals("2011-12-03T10:15:30Z", CalendarFormat.NOT_SET.format(C, Z));
		assertEquals("20111203Z", CalendarFormat.BASIC_ISO_DATE.format(C, Z));
		assertEquals("2011-12-03Z", CalendarFormat.ISO_DATE.format(C, Z));
		assertEquals("2011-12-03T10:15:30Z", CalendarFormat.ISO_INSTANT.format(C, Z));
		assertEquals("2011-12-03", CalendarFormat.ISO_LOCAL_DATE.format(C, Z));
		assertEquals("2011-12-03T10:15:30", CalendarFormat.ISO_LOCAL_DATE_TIME.format(C, Z));
		assertEquals("2011-12-03T10:15:30Z[UTC]", CalendarFormat.ISO_ZONED_DATE_TIME.format(C, Z));
		assertEquals("Sat, 3 Dec 2011 10:15:30 GMT", CalendarFormat.RFC_1123_DATE_TIME.format(C, Z));
	}

	@Test void a02_roundTrip_isoOffsetDateTime() {
		var s = CalendarFormat.ISO_OFFSET_DATE_TIME.format(C, Z);
		var c = CalendarFormat.ISO_OFFSET_DATE_TIME.parse(s, Z);
		assertEquals(C.getTimeInMillis(), c.getTimeInMillis());
	}

	@Test void a03_millis() {
		assertEquals(Long.toString(C.getTimeInMillis()), CalendarFormat.MILLIS.format(C, Z));
		assertTrue(CalendarFormat.MILLIS.isNumeric());
		assertFalse(CalendarFormat.ISO_OFFSET_DATE_TIME.isNumeric());
		assertFalse(CalendarFormat.NOT_SET.isNumeric());
		assertEquals(C.getTimeInMillis(), CalendarFormat.MILLIS.parse(Long.toString(C.getTimeInMillis()), Z).getTimeInMillis());
	}

	@Test void a04_xmlFormat_optIn() {
		var s = CalendarFormat.XML_FORMAT.format(C, Z);
		assertNotNull(s);
		var c = CalendarFormat.XML_FORMAT.parse(s, Z);
		assertEquals(C.getTimeInMillis(), c.getTimeInMillis());
	}

	@Test void a05_nullAndBlank() {
		assertNull(CalendarFormat.ISO_OFFSET_DATE_TIME.format(null, Z));
		assertNull(CalendarFormat.ISO_OFFSET_DATE_TIME.parse(null, Z));
		assertNull(CalendarFormat.ISO_OFFSET_DATE_TIME.parse("   ", Z));
	}

	@Test void a06_nullZoneFallsBackToSystem() {
		assertNotNull(CalendarFormat.ISO_OFFSET_DATE_TIME.parse("2011-12-03T10:15:30Z", null));
	}

	@Test void a07_zuluZoneStatic() {
		assertEquals(ZoneId.of("Z"), CalendarFormat.zuluZone());
	}

	@Test void a08_allFormatVariantsRoundTrip() {
		var formats = new CalendarFormat[]{
			CalendarFormat.BASIC_ISO_DATE,
			CalendarFormat.ISO_DATE,
			CalendarFormat.ISO_DATE_TIME,
			CalendarFormat.ISO_INSTANT,
			CalendarFormat.ISO_LOCAL_DATE,
			CalendarFormat.ISO_LOCAL_DATE_TIME,
			CalendarFormat.ISO_LOCAL_TIME,
			CalendarFormat.ISO_OFFSET_DATE,
			CalendarFormat.ISO_OFFSET_DATE_TIME,
			CalendarFormat.ISO_OFFSET_TIME,
			CalendarFormat.ISO_ORDINAL_DATE,
			CalendarFormat.ISO_TIME,
			CalendarFormat.ISO_WEEK_DATE,
			CalendarFormat.ISO_ZONED_DATE_TIME,
			CalendarFormat.RFC_1123_DATE_TIME,
		};
		for (var f : formats) {
			var s = f.format(C, Z);
			assertNotNull(s, "format=" + f);
			var c = f.parse(s, Z);
			assertNotNull(c, "format=" + f);
		}
	}

	@Test void a09_xmlFormat_fromNonGregorianCalendar() {
		var nonGreg = new Calendar.Builder().setInstant(C.getTimeInMillis()).setTimeZone(TimeZone.getTimeZone("UTC")).build();
		var s = CalendarFormat.XML_FORMAT.format(nonGreg, Z);
		assertNotNull(s);
	}

	@Test void a10_formatterThrowsForNonFormatterFormats() {
		assertThrows(IllegalStateException.class, () -> {
			try {
				var m = CalendarFormat.class.getDeclaredMethod("formatter");
				m.setAccessible(true);
				m.invoke(CalendarFormat.MILLIS);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		});
		assertThrows(IllegalStateException.class, () -> {
			try {
				var m = CalendarFormat.class.getDeclaredMethod("formatter");
				m.setAccessible(true);
				m.invoke(CalendarFormat.XML_FORMAT);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		});
	}
}
