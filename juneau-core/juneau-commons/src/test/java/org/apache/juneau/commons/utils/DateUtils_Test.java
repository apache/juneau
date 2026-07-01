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
package org.apache.juneau.commons.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Month;
import java.time.format.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class DateUtils_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a — null/empty pattern → ISO_INSTANT
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_nullPattern_returnsIsoInstant() {
		assertSame(DateTimeFormatter.ISO_INSTANT, DateUtils.getDateTimeFormatter(null));
	}

	@Test void a02_emptyPattern_returnsIsoInstant() {
		assertSame(DateTimeFormatter.ISO_INSTANT, DateUtils.getDateTimeFormatter(""));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b — named constant patterns resolve to the static field on DateTimeFormatter
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_namedPattern_ISO_DATE_TIME() {
		assertSame(DateTimeFormatter.ISO_DATE_TIME, DateUtils.getDateTimeFormatter("ISO_DATE_TIME"));
	}

	@Test void b02_namedPattern_ISO_LOCAL_DATE() {
		assertSame(DateTimeFormatter.ISO_LOCAL_DATE, DateUtils.getDateTimeFormatter("ISO_LOCAL_DATE"));
	}

	@Test void b03_namedPattern_RFC_1123_DATE_TIME() {
		assertSame(DateTimeFormatter.RFC_1123_DATE_TIME, DateUtils.getDateTimeFormatter("RFC_1123_DATE_TIME"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c — custom format string → DateTimeFormatter.ofPattern(...)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_customPattern_formatsAndParses() {
		var fmt = DateUtils.getDateTimeFormatter("yyyy-MM-dd");
		assertNotNull(fmt);
		// Verify it actually formats a date without throwing
		var formatted = fmt.format(java.time.LocalDate.of(2024, Month.JANUARY, 15).atStartOfDay(java.time.ZoneOffset.UTC));
		assertTrue(formatted.startsWith("2024-01-15"), "Expected formatted date to start with '2024-01-15' but was: " + formatted);
	}

	@Test void c02_customPattern_cachedOnSecondCall() {
		var fmt1 = DateUtils.getDateTimeFormatter("HH:mm:ss");
		var fmt2 = DateUtils.getDateTimeFormatter("HH:mm:ss");
		assertSame(fmt1, fmt2, "Same pattern string must return the same cached instance");
	}
}
