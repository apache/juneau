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

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.junit.jupiter.api.*;

class ConsoleOutputLine_Test extends TestBase {

	/** Single quotes in the expected JSON are turned into double quotes for readability. */
	private static void assertJson(String expected, Object actual) {
		assertEquals(Json.to(expected.replace('\'', '"'), Map.class), Json.to(Json.of(actual), Map.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a - factories and contract map
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_factories() {
		assertBean(ConsoleOutputLine.info("x"), "level,text", "INFO,x");
		assertBean(ConsoleOutputLine.warning("x"), "level,text", "WARNING,x");
		assertBean(ConsoleOutputLine.severe("x"), "level,text", "SEVERE,x");
		assertBean(ConsoleOutputLine.fine("x"), "level,text", "FINE,x");
		assertBean(ConsoleOutputLine.of(Level.FINE, "y"), "level,text", "FINE,y");
		assertEquals(2, ConsoleOutputLine.frags(Frag.text("a"), Frag.block()).frags.size());
	}

	@Test void a02_contractMapOmitsAbsentMembers() {
		assertJson("{'level':'INFO','text':'hello'}", ConsoleOutputLine.info("hello").validate().toContractMap());
	}

	@Test void a03_contractMapFull() {
		var line = ConsoleOutputLine.severe("BUILD FAILED")
			.n(42).instant(Instant.parse("2026-10-07T13:04:05.123Z"))
			.style(Style.ERROR).color("#2e7d32").icon("cancel").image("/run/42/chart.png", "Coverage chart").marker(true);
		line.ui.image.width = 480;
		assertJson("{'n':42,'instant':'2026-10-07T13:04:05.123Z','level':'SEVERE','text':'BUILD FAILED',"
			+ "'ui':{'style':'error','color':'#2e7d32','icon':'cancel','image':{'src':'/run/42/chart.png','alt':'Coverage chart','width':480},'marker':true}}",
			line.validate().toContractMap());
	}

	@Test void a04_instantAlwaysMillis() {
		var m = ConsoleOutputLine.info("x").instant(Instant.parse("2026-10-07T13:04:05Z")).validate().toContractMap();
		assertEquals("2026-10-07T13:04:05.000Z", m.get("instant"));
		var m2 = ConsoleOutputLine.info("x").instant(Instant.parse("2026-10-07T13:04:05.123456789Z")).validate().toContractMap();
		assertEquals("2026-10-07T13:04:05.123Z", m2.get("instant"));
	}

	@Test void a05_fragsContractMap() {
		var line = ConsoleOutputLine.frags(
			Frag.text("[").style(Style.MUTED),
			Frag.text("ok").bold(true),
			Frag.block().color("#2e7d32").tooltip("step 3: build\nok").href("#step-3").label("Step 3"));
		assertJson("{'level':'INFO','frags':[{'text':'[','style':'muted'},{'text':'ok','bold':true},"
			+ "{'block':true,'color':'#2e7d32','tooltip':'step 3: build\\nok','href':'#step-3','label':'Step 3'}]}",
			line.validate().toContractMap());
	}

	@Test void a06_uiOmittedWhenEmpty() {
		var line = ConsoleOutputLine.info("x").marker(false);
		assertFalse(line.validate().toContractMap().containsKey("ui"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - validation
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_textXorFrags() {
		var both = ConsoleOutputLine.info("x");
		both.frags = List.of(Frag.text("y"));
		assertThrowsWithMessage(IllegalArgumentException.class, "exactly one of text or frags", both::validate);
		var neither = new ConsoleOutputLine();
		assertThrowsWithMessage(IllegalArgumentException.class, "exactly one of text or frags", neither::validate);
	}

	@Test void b02_fragCount() {
		assertThrowsWithMessage(IllegalArgumentException.class, "1..512", () -> ConsoleOutputLine.frags().validate());
		var many = new Frag[513];
		Arrays.fill(many, Frag.text("a"));
		assertThrowsWithMessage(IllegalArgumentException.class, "1..512", () -> ConsoleOutputLine.frags(many).validate());
		var max = new Frag[512];
		Arrays.fill(max, Frag.text("a"));
		assertDoesNotThrow(() -> ConsoleOutputLine.frags(max).validate());
	}

	@Test void b03_fragMixing() {
		var t = Frag.text("a");
		t.tooltip = "x";
		assertThrowsWithMessage(IllegalArgumentException.class, "text fragment", () -> ConsoleOutputLine.frags(t).validate());
		var b = Frag.block();
		b.text = "a";
		assertThrowsWithMessage(IllegalArgumentException.class, "block fragment", () -> ConsoleOutputLine.frags(b).validate());
		var b2 = Frag.block();
		b2.bold = true;
		assertThrowsWithMessage(IllegalArgumentException.class, "block fragment", () -> ConsoleOutputLine.frags(b2).validate());
		var t2 = new Frag();
		assertThrowsWithMessage(IllegalArgumentException.class, "text fragment", () -> ConsoleOutputLine.frags(t2).validate());
	}

	@Test void b04_vectorsColorHrefSrc() throws Exception {
		var v = ConsoleOutputChecks_Test.vectors();
		for (var s : v.get("color").get("accept"))
			assertDoesNotThrow(() -> ConsoleOutputLine.info("x").color(s).validate(), s);
		for (var s : v.get("color").get("reject")) {
			assertThrowsWithMessage(IllegalArgumentException.class, "ui.color", () -> ConsoleOutputLine.info("x").color(s).validate());
			assertThrowsWithMessage(IllegalArgumentException.class, "frags[0].color", () -> ConsoleOutputLine.frags(Frag.text("a").color(s)).validate());
		}
		for (var s : v.get("href").get("accept"))
			assertDoesNotThrow(() -> ConsoleOutputLine.frags(Frag.block().href(s)).validate(), s);
		for (var s : v.get("href").get("reject"))
			assertThrowsWithMessage(IllegalArgumentException.class, "frags[0].href", () -> ConsoleOutputLine.frags(Frag.block().href(s)).validate());
		for (var s : v.get("imageSrc").get("accept"))
			assertDoesNotThrow(() -> ConsoleOutputLine.info("x").image(s, "").validate(), s);
		for (var s : v.get("imageSrc").get("reject"))
			assertThrowsWithMessage(IllegalArgumentException.class, "ui.image.src", () -> ConsoleOutputLine.info("x").image(s, "a").validate());
	}

	@Test void b05_iconGrammar() throws Exception {
		var v = ConsoleOutputChecks_Test.vectors();
		for (var s : v.get("icon").get("accept"))
			assertDoesNotThrow(() -> ConsoleOutputLine.info("x").icon(s).validate(), s);
		for (var s : v.get("icon").get("reject"))
			assertThrowsWithMessage(IllegalArgumentException.class, "ui.icon", () -> ConsoleOutputLine.info("x").icon(s).validate());
	}

	@Test void b06_imageWidthAndAlt() {
		var l1 = ConsoleOutputLine.info("x").image("/a.png", "a");
		l1.ui.image.width = 0;
		assertThrowsWithMessage(IllegalArgumentException.class, "ui.image.width", l1::validate);
		var l2 = ConsoleOutputLine.info("x").image("/a.png", "a");
		l2.ui.image.width = 4097;
		assertThrowsWithMessage(IllegalArgumentException.class, "ui.image.width", l2::validate);
		var l3 = ConsoleOutputLine.info("x").image("/a.png", "a");
		l3.ui.image.width = 4096;
		assertDoesNotThrow(l3::validate);
		assertThrowsWithMessage(IllegalArgumentException.class, "ui.image.alt", () -> ConsoleOutputLine.info("x").image("/a.png", null).validate());
	}

	@Test void b07_tooltipAndLabelCaps() {
		assertDoesNotThrow(() -> ConsoleOutputLine.frags(Frag.block().tooltip("t".repeat(2048)).label("l".repeat(256))).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "tooltip", () -> ConsoleOutputLine.frags(Frag.block().tooltip("t".repeat(2049))).validate());
		assertThrowsWithMessage(IllegalArgumentException.class, "label", () -> ConsoleOutputLine.frags(Frag.block().label("l".repeat(257))).validate());
	}

	@Test void b08_textTruncation() {
		var line = ConsoleOutputLine.info("a".repeat(65_536 + 10)).validate();
		assertEquals("a".repeat(65_536) + "… [truncated 10 chars]", line.text);
		var exact = ConsoleOutputLine.info("a".repeat(65_536)).validate();
		assertEquals(65_536, exact.text.length());
	}

	@Test void b09_truncationDoesNotSplitSurrogates() {
		var s = "a".repeat(65_535) + "😀" + "b";
		var line = ConsoleOutputLine.info(s).validate();
		assertEquals("a".repeat(65_535) + "… [truncated 3 chars]", line.text);
	}

	@Test void b10_controlCharacters() {
		assertEquals("a\tb\nc\uFFFDd\uFFFDe", ConsoleOutputLine.info("a\tb\nc\u001bd\u0007e").validate().text);
		var f = ConsoleOutputLine.frags(Frag.text("x\u0000y")).validate();
		assertEquals("x\uFFFDy", f.frags.get(0).text);
	}

	@Test void b11_emptyTextAllowed() {
		assertJson("{'level':'INFO','text':''}", ConsoleOutputLine.info("").validate().toContractMap());
	}

	@Test void b12_nMustBePositive() {
		assertThrowsWithMessage(IllegalArgumentException.class, "n must be >= 1", () -> ConsoleOutputLine.info("x").n(0).validate());
	}

	@Test void b13_messageClipsValue() {
		var e = assertThrows(IllegalArgumentException.class, () -> ConsoleOutputLine.info("x").color("#" + "z".repeat(100)).validate());
		assertTrue(e.getMessage().contains("z".repeat(63) + "…"), e.getMessage());
		assertFalse(e.getMessage().contains("z".repeat(65)), e.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - copy
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_copyIsDeep() {
		var line = ConsoleOutputLine.frags(Frag.block().tooltip("t")).icon("check").image("/a.png", "a").n(3);
		var c = line.copy();
		assertNotSame(line, c);
		assertNotSame(line.frags, c.frags);
		assertNotSame(line.frags.get(0), c.frags.get(0));
		assertNotSame(line.ui, c.ui);
		assertNotSame(line.ui.image, c.ui.image);
		assertEquals(Json.of(line.toContractMap()), Json.of(c.toContractMap()));
		c.frags.get(0).tooltip = "changed";
		assertEquals("t", line.frags.get(0).tooltip);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e - open line
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_openIsSerializedOnlyWhenTrue() {
		assertJson("{'n':3,'level':'INFO','text':'abc','open':true}", ConsoleOutputLine.info("abc").n(3).open(true).validate().toContractMap());
		assertJson("{'n':3,'level':'INFO','text':'abc'}", ConsoleOutputLine.info("abc").n(3).open(false).validate().toContractMap());
		assertNull(ConsoleOutputLine.info("abc").open(false).open);
	}

	@Test void e02_copyKeepsOpen() {
		assertEquals(Boolean.TRUE, ConsoleOutputLine.info("abc").open(true).copy().open);
	}

	@Test void e03_afterLastCr() {
		assertEquals("b", ConsoleOutputLine.afterLastCr("a\rb"));
		assertEquals("abc", ConsoleOutputLine.afterLastCr("abc\r"));
		assertEquals("b", ConsoleOutputLine.afterLastCr("a\rb\r\r"));
		assertEquals("", ConsoleOutputLine.afterLastCr(""));
		assertEquals("", ConsoleOutputLine.afterLastCr("\r"));
		assertEquals("plain", ConsoleOutputLine.afterLastCr("plain"));
	}

	@Test void e04_decoderCopyIsIndependent() {
		var d = new AnsiDecoder();
		d.line(Level.INFO, "\u001b[31mred");
		var c = d.copy();
		c.line(Level.INFO, "\u001b[0m");
		var f = d.line(Level.INFO, "still").frags.get(0);
		assertEquals("still", f.text);
		assertEquals(Style.ERROR, f.style);
	}
}
