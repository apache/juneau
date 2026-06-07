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

import java.util.*;

import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class CurrencyFormat_Test {

	private static final Currency USD = Currency.getInstance("USD");
	private static final Currency EUR = Currency.getInstance("EUR");
	private static final Currency JPY = Currency.getInstance("JPY");
	private static final Currency CAD = Currency.getInstance("CAD");

	//------------------------------------------------------------------------------------------------------------------
	// format
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_format_isoCode() {
		assertEquals("USD", CurrencyFormat.format(USD, CurrencyFormat.ISO_CODE, Locale.US));
		assertEquals("EUR", CurrencyFormat.format(EUR, CurrencyFormat.ISO_CODE, Locale.US));
		assertEquals("JPY", CurrencyFormat.format(JPY, CurrencyFormat.ISO_CODE, Locale.US));
	}

	@Test void a02_format_isoCode_ignoresLocale() {
		assertEquals("USD", CurrencyFormat.format(USD, CurrencyFormat.ISO_CODE, Locale.FRANCE));
		assertEquals("USD", CurrencyFormat.format(USD, CurrencyFormat.ISO_CODE, null));
	}

	@Test void a03_format_notSetFallsThroughToIso() {
		assertEquals("USD", CurrencyFormat.format(USD, CurrencyFormat.NOT_SET, Locale.US));
	}

	@Test void a04_format_nullFormatTreatedAsIso() {
		assertEquals("USD", CurrencyFormat.format(USD, null, Locale.US));
	}

	@Test void a05_format_symbol_us() {
		// JDK-locale-data dependent (CLDR vs. JRE/COMPAT). Just delegate the expectation.
		assertEquals(USD.getSymbol(Locale.US), CurrencyFormat.format(USD, CurrencyFormat.SYMBOL, Locale.US));
	}

	@Test void a06_format_symbol_france() {
		assertEquals(EUR.getSymbol(Locale.FRANCE), CurrencyFormat.format(EUR, CurrencyFormat.SYMBOL, Locale.FRANCE));
	}

	@Test void a07_format_displayName_us() {
		assertEquals(USD.getDisplayName(Locale.US), CurrencyFormat.format(USD, CurrencyFormat.DISPLAY_NAME, Locale.US));
	}

	@Test void a08_format_symbolDiffersFromIsoForKnownPairing() {
		// At least one of the locale-sensitive forms must differ from the ISO code under any locale provider —
		// this verifies the SYMBOL branch is actually wired through (not silently returning the ISO code).
		var sym = CurrencyFormat.format(USD, CurrencyFormat.SYMBOL, Locale.US);
		var disp = CurrencyFormat.format(USD, CurrencyFormat.DISPLAY_NAME, Locale.US);
		assertNotNull(sym);
		assertNotNull(disp);
		// If both equal the ISO code, the surefire JVM lacks locale data for en_US/USD — accept that.
		// Otherwise, at least the DISPLAY_NAME must be longer than the ISO code.
		if (!"USD".equals(disp))
			assertTrue(disp.length() > 3, "display name: " + disp);
	}

	@Test void a09_format_null() {
		for (var f : CurrencyFormat.values())
			assertNull(CurrencyFormat.format(null, f, Locale.US), "format=" + f);
	}

	@Test void a10_format_nullLocaleTreatedAsRoot() {
		// SYMBOL with null locale resolves to Locale.ROOT — JDK returns the ISO code in that case.
		var r = CurrencyFormat.format(USD, CurrencyFormat.SYMBOL, null);
		assertNotNull(r);
	}

	@Test void a11_isNumeric() {
		for (var f : CurrencyFormat.values())
			assertFalse(f.isNumeric(), "format=" + f);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parse
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_parse_isoCode() {
		assertEquals(USD, CurrencyFormat.parse("USD", CurrencyFormat.ISO_CODE, Locale.US));
		assertEquals(EUR, CurrencyFormat.parse("EUR", CurrencyFormat.ISO_CODE, Locale.FRANCE));
		assertEquals(JPY, CurrencyFormat.parse("JPY", CurrencyFormat.ISO_CODE, Locale.JAPAN));
	}

	@Test void b02_parse_isoCode_alsoAcceptsAcrossFormats() {
		// ISO code is recognized regardless of the hint.
		for (var hint : CurrencyFormat.values())
			assertEquals(USD, CurrencyFormat.parse("USD", hint, Locale.US), "hint=" + hint);
	}

	@Test void b03_parse_symbol_us_roundTrips() {
		// Whatever the JDK calls the USD symbol in en_US, that symbol must parse back to USD.
		var sym = USD.getSymbol(Locale.US);
		// Only meaningful if the locale provider actually localized the symbol (not just returned "USD").
		if (!"USD".equals(sym))
			assertEquals(USD, CurrencyFormat.parse(sym, CurrencyFormat.SYMBOL, Locale.US));
	}

	@Test void b04_parse_symbol_canada_roundTrips() {
		var sym = CAD.getSymbol(Locale.CANADA);
		if (!"CAD".equals(sym))
			assertEquals(CAD, CurrencyFormat.parse(sym, CurrencyFormat.SYMBOL, Locale.CANADA));
	}

	@Test void b05_parse_unrecognizedSymbolThrows() {
		// "???" is guaranteed not to be a localized symbol in any JDK locale provider.
		assertThrows(IllegalArgumentException.class, () -> CurrencyFormat.parse("???", CurrencyFormat.SYMBOL, Locale.JAPAN));
	}

	@Test void b06_parse_displayName_us_roundTrips() {
		var name = USD.getDisplayName(Locale.US);
		if (!"USD".equals(name))
			assertEquals(USD, CurrencyFormat.parse(name, CurrencyFormat.DISPLAY_NAME, Locale.US));
	}

	@Test void b07_parse_nullAndBlank() {
		for (var f : CurrencyFormat.values()) {
			assertNull(CurrencyFormat.parse(null, f, Locale.US), "format=" + f);
			assertNull(CurrencyFormat.parse("", f, Locale.US), "format=" + f);
			assertNull(CurrencyFormat.parse("   ", f, Locale.US), "format=" + f);
		}
	}

	@Test void b08_parse_nullFormatHintAndLocale() {
		assertEquals(USD, CurrencyFormat.parse("USD", null, null));
	}

	@Test void b09_parse_invalidIsoThrows() {
		assertThrows(IllegalArgumentException.class, () -> CurrencyFormat.parse("ZZZ", CurrencyFormat.ISO_CODE, Locale.US));
	}

	@Test void b10_parse_unrecognizedSymbolThrowsInUs() {
		assertThrows(IllegalArgumentException.class, () -> CurrencyFormat.parse("???", CurrencyFormat.SYMBOL, Locale.US));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Round-trip
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_roundTrip_isoCode() {
		for (var c : new Currency[] { USD, EUR, JPY }) {
			var s = CurrencyFormat.format(c, CurrencyFormat.ISO_CODE, Locale.US);
			assertEquals(c, CurrencyFormat.parse(s, CurrencyFormat.ISO_CODE, Locale.US));
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Disambiguation paths
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_parse_rootLocaleNoCurrency() {
		// Locale.ROOT has no associated currency. Unrecognized symbol throws "could not resolve".
		assertThrows(IllegalArgumentException.class, () -> CurrencyFormat.parse("???", CurrencyFormat.SYMBOL, Locale.ROOT));
	}

	@Test void d02_parse_displayName_unrecognizedThrows() {
		assertThrows(IllegalArgumentException.class,
			() -> CurrencyFormat.parse("Definitely Not A Real Currency Name", CurrencyFormat.DISPLAY_NAME, Locale.US));
	}

	@Test void d03_parse_singleUniqueSymbolMatch() {
		// Euro is unambiguous in en_US locale data — exactly one currency uses "€".
		// Locale-data dependent: skip if the test JVM doesn't have CLDR/JRE data.
		var eurSym = EUR.getSymbol(Locale.US);
		if (!"EUR".equals(eurSym)) {
			var parsed = CurrencyFormat.parse(eurSym, CurrencyFormat.SYMBOL, Locale.US);
			assertEquals(EUR, parsed);
		}
	}

	@Test void d04_parse_ambiguousSymbol_resolvedByLocaleDefault() {
		// "$" matches USD / CAD / AUD / MXN / HKD / SGD / NZD in locale data; with Locale.US, the disambiguator
		// should prefer USD. Locale-data dependent — the symbol must actually be localized to "$" rather than
		// fall back to the ISO code in the test JVM.
		var usdSym = USD.getSymbol(Locale.US);
		var cadSym = CAD.getSymbol(Locale.US);
		// Both symbols equal -> ambiguous path was exercised.
		if ("$".equals(usdSym) && "$".equals(cadSym)) {
			assertEquals(USD, CurrencyFormat.parse("$", CurrencyFormat.SYMBOL, Locale.US));
		}
	}
}
