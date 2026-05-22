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
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that top-level (non-bean-property) values for types covered by the inline anonymous swaps registered in
 * {@link org.apache.juneau.swap.DefaultSwaps} round-trip cleanly through several wire formats.
 *
 * <p>
 * The three thin delegating swaps that previously lived as standalone classes (LocaleSwap, TimeZoneSwap, ZoneIdSwap)
 * were inlined into {@code DefaultSwaps} as anonymous {@link org.apache.juneau.swap.StringSwap} subclasses; this test
 * confirms that the inlining did not regress top-level serialization or parsing for any of the three.
 */
class TopLevelDefaultSwapsRoundTrip_Test extends TestBase {

	private static final WriterSerializer JS = Json5Serializer.DEFAULT;
	private static final ReaderParser JP = Json5Parser.DEFAULT;
	private static final WriterSerializer XS = XmlSerializer.DEFAULT;
	private static final ReaderParser XP = XmlParser.DEFAULT;

	//-----------------------------------------------------------------------------------------------------------------
	// Locale
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_locale_json5() throws Exception {
		var a = Locale.US;
		var json = JS.serialize(a);
		assertEquals("'en-US'", json);
		assertEquals(a, JP.parse(json, Locale.class));
	}

	@Test void a02_locale_xml() throws Exception {
		var a = Locale.US;
		var xml = XS.serialize(a);
		assertEquals(a, XP.parse(xml, Locale.class));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// TimeZone
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_timeZone_json5() throws Exception {
		var a = TimeZone.getTimeZone("America/New_York");
		var json = JS.serialize(a);
		assertEquals("'America/New_York'", json);
		var a2 = JP.parse(json, TimeZone.class);
		assertEquals(a.getID(), a2.getID());
	}

	@Test void b02_timeZone_xml() throws Exception {
		var a = TimeZone.getTimeZone("America/New_York");
		var xml = XS.serialize(a);
		var a2 = XP.parse(xml, TimeZone.class);
		assertEquals(a.getID(), a2.getID());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// ZoneId
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_zoneId_json5() throws Exception {
		var a = ZoneId.of("America/New_York");
		var json = JS.serialize(a);
		assertEquals("'America/New_York'", json);
		assertEquals(a, JP.parse(json, ZoneId.class));
	}

	@Test void c02_zoneId_xml() throws Exception {
		var a = ZoneId.of("America/New_York");
		var xml = XS.serialize(a);
		assertEquals(a, XP.parse(xml, ZoneId.class));
	}
}
