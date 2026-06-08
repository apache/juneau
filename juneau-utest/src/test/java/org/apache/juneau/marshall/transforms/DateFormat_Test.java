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

@SuppressWarnings({
	"java:S5778"  // assertThrows lambdas with chained calls; intermediate invocations do not throw in practice
})
class DateFormat_Test {

	private static final ZoneId Z = ZoneId.of("Z");
	private static final Date D = Date.from(Instant.parse("2011-12-03T10:15:30Z"));

	@Test void a01_isoVariants() {
		assertEquals("2011-12-03T10:15:30", DateFormat.ISO_LOCAL_DATE_TIME.format(D, Z));
		assertEquals("2011-12-03T10:15:30", DateFormat.NOT_SET.format(D, Z));
		assertEquals("20111203Z", DateFormat.BASIC_ISO_DATE.format(D, Z));
		assertEquals("2011-12-03T10:15:30Z", DateFormat.ISO_INSTANT.format(D, Z));
		assertEquals("2011-12-03", DateFormat.ISO_LOCAL_DATE.format(D, Z));
		assertEquals("2011-12-03T10:15:30Z", DateFormat.ISO_OFFSET_DATE_TIME.format(D, Z));
	}

	@Test void a02_roundTrip_localAndOffset() {
		var s1 = DateFormat.ISO_LOCAL_DATE_TIME.format(D, Z);
		assertEquals(D, DateFormat.ISO_LOCAL_DATE_TIME.parse(s1, Z));
		var s2 = DateFormat.ISO_OFFSET_DATE_TIME.format(D, Z);
		assertEquals(D, DateFormat.ISO_OFFSET_DATE_TIME.parse(s2, Z));
	}

	@Test void a03_millis() {
		assertEquals(Long.toString(D.getTime()), DateFormat.MILLIS.format(D, Z));
		assertEquals(D, DateFormat.MILLIS.parse(Long.toString(D.getTime()), Z));
		assertTrue(DateFormat.MILLIS.isNumeric());
		assertFalse(DateFormat.ISO_LOCAL_DATE_TIME.isNumeric());
		assertFalse(DateFormat.NOT_SET.isNumeric());
	}

	@Test void a04_nullAndBlank() {
		assertNull(DateFormat.ISO_LOCAL_DATE_TIME.format(null, Z));
		assertNull(DateFormat.ISO_LOCAL_DATE_TIME.parse(null, Z));
		assertNull(DateFormat.ISO_LOCAL_DATE_TIME.parse("   ", Z));
	}

	@Test void a05_nullZoneFallsBackToSystem() {
		assertNotNull(DateFormat.ISO_LOCAL_DATE_TIME.format(D, null));
		assertNotNull(DateFormat.ISO_LOCAL_DATE_TIME.parse("2011-12-03T10:15:30", null));
	}

	@Test void a06_allFormatVariantsRoundTrip() {
		var formats = new DateFormat[]{
			DateFormat.BASIC_ISO_DATE,
			DateFormat.ISO_DATE,
			DateFormat.ISO_DATE_TIME,
			DateFormat.ISO_INSTANT,
			DateFormat.ISO_LOCAL_DATE,
			DateFormat.ISO_LOCAL_DATE_TIME,
			DateFormat.ISO_LOCAL_TIME,
			DateFormat.ISO_OFFSET_DATE,
			DateFormat.ISO_OFFSET_DATE_TIME,
			DateFormat.ISO_OFFSET_TIME,
			DateFormat.ISO_ORDINAL_DATE,
			DateFormat.ISO_TIME,
			DateFormat.ISO_WEEK_DATE,
			DateFormat.ISO_ZONED_DATE_TIME,
			DateFormat.RFC_1123_DATE_TIME,
		};
		for (var f : formats) {
			var s = f.format(D, Z);
			assertNotNull(s, "format=" + f);
			var d = f.parse(s, Z);
			assertNotNull(d, "format=" + f);
		}
	}

	@Test void a07_formatterThrowsForMillis() {
		assertThrows(IllegalStateException.class, () -> {
			try {
				var m = DateFormat.class.getDeclaredMethod("formatter");
				m.setAccessible(true);
				m.invoke(DateFormat.MILLIS);
			} catch (InvocationTargetException e) {
				throw e.getCause();
			}
		});
	}
}
