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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.junit.jupiter.api.*;

class ConsoleOutputLine_Ansi_Test extends TestBase {

	private static final String ESC = "\u001b";

	@SuppressWarnings("unchecked")
	private static List<Map<String,Object>> vectors() throws IOException {
		try (var in = ConsoleOutputLine_Ansi_Test.class.getResourceAsStream("console-output-ansi-vectors.json")) {
			assertNotNull(in, "console-output-ansi-vectors.json missing from the test classpath");
			return Json.to(new String(in.readAllBytes(), StandardCharsets.UTF_8), List.class);
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> wire(ConsoleOutputLine line) {
		return Json.to(Json.of(line.toContractMap()), Map.class);
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> expected(String json) {
		var m = new LinkedHashMap<String,Object>(Json.to(json.replace('\'', '"'), Map.class));
		m.putIfAbsent("level", "INFO");
		return m;
	}

	@Test void a01_vectors() throws Exception {
		for (var v : vectors()) {
			@SuppressWarnings("unchecked")
			var exp = new LinkedHashMap<String,Object>((Map<String,Object>)v.get("expect"));
			exp.putIfAbsent("level", "INFO");
			var in = (String)v.get("in");
			assertEquals(exp, wire(ConsoleOutputLine.fromAnsi(in)), () -> "vector " + v.get("id"));
		}
	}

	@Test void a02_tooManyParamsIgnored() {
		var in = ESC + "[" + "1;".repeat(32) + "31mx";
		assertEquals(expected("{'text':'x'}"), wire(ConsoleOutputLine.fromAnsi(in)));
	}

	@Test void a03_thirtyTwoParamsApplied() {
		var in = ESC + "[" + "1;".repeat(31) + "31mx";
		assertEquals(expected("{'frags':[{'text':'x','style':'error','bold':true}]}"), wire(ConsoleOutputLine.fromAnsi(in)));
	}

	@Test void a04_paramAbove65535Ignored() {
		assertEquals(expected("{'text':'x'}"), wire(ConsoleOutputLine.fromAnsi(ESC + "[65536;31mx")));
		assertEquals(expected("{'frags':[{'text':'x','style':'error'}]}"), wire(ConsoleOutputLine.fromAnsi(ESC + "[65535;31mx")));
	}

	@Test void a05_fragmentOverflow() {
		var line = ConsoleOutputLine.fromAnsi((ESC + "[31ma" + ESC + "[0mb").repeat(40_000));
		assertEquals(512, line.frags.size());
		for (var i = 0; i < 510; i++) {
			var f = line.frags.get(i);
			assertEquals(i % 2 == 0 ? "a" : "b", f.text, "frag " + i);
			assertEquals(i % 2 == 0 ? Style.ERROR : null, f.style, "frag " + i);
		}
		var merged = line.frags.get(510);
		assertEquals(Style.ERROR, merged.style);
		assertEquals(65_536 - 510, merged.text.length());
		assertTrue(merged.text.startsWith("abab"));
		var suffix = line.frags.get(511);
		assertEquals("… [truncated 14464 chars]", suffix.text);
		assertEquals(Style.MUTED, suffix.style);
		var visible = 0;
		for (var i = 0; i < 511; i++)
			visible += line.frags.get(i).text.length();
		assertEquals(65_536, visible);
	}

	@Test void a06_decoderCarriesStateAcrossLines() {
		var d = new AnsiDecoder();
		assertEquals(expected("{'frags':[{'text':'first','style':'error'}]}"), wire(d.line(Level.INFO, ESC + "[31mfirst")));
		assertEquals(expected("{'frags':[{'text':'second','style':'error'}]}"), wire(d.line(Level.INFO, "second")));
		d.reset();
		assertEquals(expected("{'text':'third'}"), wire(d.line(Level.INFO, "third")));
	}

	@Test void a07_staticFormsDoNotLeakState() {
		ConsoleOutputLine.fromAnsi(ESC + "[31mx");
		assertEquals(expected("{'text':'y'}"), wire(ConsoleOutputLine.fromAnsi("y")));
	}

	@Test void a08_levelAndNull() {
		assertEquals(expected("{'level':'SEVERE','text':'x'}"), wire(ConsoleOutputLine.fromAnsi(Level.SEVERE, "x")));
		assertEquals(expected("{'text':''}"), wire(ConsoleOutputLine.fromAnsi(null)));
		assertEquals(expected("{'text':'x'}"), wire(ConsoleOutputLine.fromAnsi(null, "x")));
	}

	@Test void a09_every256ColourIsValidOrDropped() {
		for (var n = 0; n < 256; n++) {
			var line = ConsoleOutputLine.fromAnsi(ESC + "[38;5;" + n + "mx");
			if (line.frags != null && line.frags.get(0).color != null)
				assertTrue(ConsoleOutputChecks.isSafeColor(line.frags.get(0).color), "index " + n);
		}
	}

	@Test void a10_luminanceThresholds() {
		assertEquals(0.0, AnsiDecoder.luminance(0, 0, 0), 1e-9);
		assertEquals(1.0, AnsiDecoder.luminance(255, 255, 255), 1e-9);
		assertTrue(AnsiDecoder.luminance(0x12, 0x12, 0x12) < 0.05);
		assertTrue(AnsiDecoder.luminance(0xee, 0xee, 0xee) > 0.85);
		assertTrue(AnsiDecoder.luminance(0xff, 0xff, 0x00) > 0.85);
		var grey = AnsiDecoder.luminance(0x80, 0x80, 0x80);
		assertTrue(grey > 0.05 && grey < 0.85);
	}

	@Test void a11_longPlainInputTruncatesAsText() {
		var line = ConsoleOutputLine.fromAnsi("a".repeat(65_540));
		assertNull(line.frags);
		assertEquals("a".repeat(65_536) + "… [truncated 4 chars]", line.text);
	}

	@Test void a12_truncationDoesNotSplitSurrogatesInFrags() {
		var in = ESC + "[31m" + "a".repeat(65_535) + "😀b";
		var line = ConsoleOutputLine.fromAnsi(in);
		assertEquals(2, line.frags.size());
		assertEquals("a".repeat(65_535), line.frags.get(0).text);
		assertEquals("… [truncated 3 chars]", line.frags.get(1).text);
	}

	@Test void a13_neverThrowsOnGarbage() {
		var r = new Random(42);
		for (var i = 0; i < 2000; i++) {
			var sb = new StringBuilder();
			for (var j = 0; j < 40; j++)
				sb.append((char)(r.nextBoolean() ? 0x1b : r.nextInt(0xa0)));
			var s = sb.toString();
			assertDoesNotThrow(() -> ConsoleOutputLine.fromAnsi(s), s);
		}
	}

	@Test void a14_fixedColoursContrastOnLightBackground() {
		// The four fixed magenta/cyan colours must keep at least 4.5:1 against the lightest console background (#f7f7f7).
		var bg = AnsiDecoder.luminance(0xf7, 0xf7, 0xf7);
		for (var code : new int[]{35, 36, 95, 96}) {
			var hex = ConsoleOutputLine.fromAnsi(ESC + "[" + code + "mx").frags.get(0).color;
			var l = AnsiDecoder.luminance(Integer.parseInt(hex.substring(1, 3), 16), Integer.parseInt(hex.substring(3, 5), 16), Integer.parseInt(hex.substring(5, 7), 16));
			assertTrue((bg + 0.05) / (l + 0.05) >= 4.5, "SGR " + code + " " + hex);
		}
	}
}
