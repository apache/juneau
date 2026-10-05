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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

class SearchType_Test extends TestBase {

	@Test
	void a01_bareHelpIsTheExactSpecStringPerType() {
		assertEquals(
			"Matches any value containing this text (case-insensitive). Use $eq(...) for an exact, case-sensitive match.",
			SearchType.TEXT.bareHelp());
		assertEquals(
			"Matches any value containing this text (case-insensitive). Use $eq(...) for an exact, case-sensitive match.",
			SearchType.ID.bareHelp());
		assertEquals("Matches one value exactly, case-insensitive.", SearchType.ENUM.bareHelp());
		assertEquals(
			"Matches a number exactly. Use $gt / $gte / $lt / $lte / $between for a range, or a duration such as -24h or -PT24H (= -86400000 ms).",
			SearchType.NUMERIC.bareHelp());
		assertEquals("Matches an exact instant; a bare date means UTC midnight exactly, so for a whole day use $between(d, d+1). Also accepts a duration such as -24h or -PT24H, relative to the request time.", SearchType.TIMESTAMP.bareHelp());
		assertEquals("Matches any version starting with this prefix, e.g. 250 matches 250.1 and 250.2.3.", SearchType.VERSION.bareHelp());
		assertEquals("Matches true or false exactly.", SearchType.BOOLEAN.bareHelp());
	}

	@Test
	void a02_bareHelpIsNonBlankForEveryType() {
		for (var t : SearchType.values())
			assertFalse(t.bareHelp().isBlank(), () -> t + " has a blank bareHelp()");
	}

	@Test
	void b01_parse_delegatesToValueParse() {
		assertEquals(new java.math.BigDecimal("42"), SearchType.NUMERIC.parse("42"));
		assertEquals("Bob", SearchType.TEXT.parse("Bob"));
	}

	@Test
	void c01_fromWire_isCaseInsensitiveAndNullSafe() {
		assertSame(SearchType.TIMESTAMP, SearchType.fromWire(" Timestamp "));
		for (var t : SearchType.values())
			assertSame(t, SearchType.fromWire(t.wire()));
		assertNull(SearchType.fromWire(null));
		assertNull(SearchType.fromWire(""));
		assertNull(SearchType.fromWire("bogus"));
	}
}
