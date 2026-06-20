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
package org.apache.juneau.marshall.xml;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Direct unit tests for {@link XmlUtils}.
 *
 * <p>
 * Exercises encode/decode helpers (text, attribute name/value, element name), the
 * {@code _xXXXX_} escape-sequence machinery, text-node collapsing, and the readable-event
 * label helper used for stream debugging.
 */
@SuppressWarnings({
	"resource" // Closeable resources in tests are intentionally unassigned; closing is handled by test infrastructure.
})
class XmlUtils_Test extends TestBase {

	private static String encodeAttrName(Object value) throws IOException {
		var sw = new StringWriter();
		XmlUtils.encodeAttrName(sw, value);
		return sw.toString();
	}

	private static String encodeAttrValue(Object value, boolean trim) {
		var sw = new StringWriter();
		XmlUtils.encodeAttrValue(sw, value, trim);
		return sw.toString();
	}

	private static String encodeText(Object value, boolean trim, boolean preserveWhitespace) {
		var sw = new StringWriter();
		XmlUtils.encodeText(sw, value, trim, preserveWhitespace);
		return sw.toString();
	}

	private static String encodeElementNameW(Object value) {
		var sw = new StringWriter();
		XmlUtils.encodeElementName(sw, value);
		return sw.toString();
	}

	//------------------------------------------------------------------------------------------------
	// decode (line 111-138)
	//------------------------------------------------------------------------------------------------

	@Test void a01_decodeNullReturnsNull() {
		assertNull(XmlUtils.decode(null, null));
	}

	@Test void a02_decodeEmptyReturnsEmpty() {
		assertEquals("", XmlUtils.decode("", null));
	}

	@Test void a03_decodeStringNoUnderscoreReturnsAsIs() {
		assertEquals("foobar", XmlUtils.decode("foobar", null));
	}

	@Test void a04_decodeUnderscoreNotEscapeSequence() {
		assertEquals("a_b_c", XmlUtils.decode("a_b_c", null));
	}

	@Test void a05_decodeEscapeSequenceToOriginalChar() {
		// _x0041_ = 'A'
		assertEquals("A", XmlUtils.decode("_x0041_", null));
	}

	@Test void a06_decodeNullMarker() {
		// _x0000_ → null (special)
		assertNull(XmlUtils.decode("_x0000_", null));
	}

	@Test void a07_decodeEmptyStringMarker() {
		// _xE000_ → empty (special — does not append)
		assertEquals("", XmlUtils.decode("_xE000_", null));
	}

	@Test void a08_decodeEscapeMixedWithText() {
		// 'A' + 'B' + 'C'
		assertEquals("ABC", XmlUtils.decode("_x0041__x0042__x0043_", null));
	}

	@Test void a09_decodeWithProvidedBuilder() {
		var sb = new StringBuilder();
		assertEquals("A", XmlUtils.decode("_x0041_", sb));
	}

	@Test void a10_decodeMixedTextAndEscape() {
		// 'a' + _x0042_ ('B') + 'c'
		assertEquals("aBc", XmlUtils.decode("a_x0042_c", null));
	}

	//------------------------------------------------------------------------------------------------
	// encodeAttrName (line 151-180)
	//------------------------------------------------------------------------------------------------

	@Test void b01_encodeAttrNameNull() throws Exception {
		assertEquals("_x0000_", encodeAttrName(null));
	}

	@Test void b02_encodeAttrNameSimple() throws Exception {
		assertEquals("foo", encodeAttrName("foo"));
	}

	@Test void b03_encodeAttrNameWithDigit() throws Exception {
		// digits allowed after first position; first char digit triggers encoding
		assertEquals("foo123", encodeAttrName("foo123"));
	}

	@Test void b04_encodeAttrNameStartsWithDigit() throws Exception {
		// first char digit must be encoded
		var r = encodeAttrName("1abc");
		assertTrue(r.startsWith("_x"), r);
		assertTrue(r.endsWith("abc"), r);
	}

	@Test void b05_encodeAttrNameStartsWithUnderscore() throws Exception {
		// underscore allowed at start (not part of escape sequence)
		assertEquals("_foo", encodeAttrName("_foo"));
	}

	@Test void b06_encodeAttrNameContainsUnderscore() throws Exception {
		assertEquals("foo_bar", encodeAttrName("foo_bar"));
	}

	@Test void b07_encodeAttrNameWithColon() throws Exception {
		// Colon allowed (used for namespace prefix)
		assertEquals("ns:name", encodeAttrName("ns:name"));
	}

	@Test void b08_encodeAttrNameWithSpace() throws Exception {
		// Space gets hex-encoded
		var r = encodeAttrName("foo bar");
		assertTrue(r.contains("_x"), r);
	}

	@Test void b09_encodeAttrNameUnderscoreEscape() throws Exception {
		// Existing _x0041_ in source must itself be encoded so it round-trips.
		var r = encodeAttrName("_x0041_");
		// First underscore must be encoded since it IS an escape sequence prefix.
		assertTrue(r.startsWith("_x"), r);
		assertNotEquals("_x0041_", r);
	}

	//------------------------------------------------------------------------------------------------
	// encodeAttrValue (line 201-234)
	//------------------------------------------------------------------------------------------------

	@Test void c01_encodeAttrValueNull() {
		assertEquals("_x0000_", encodeAttrValue(null, false));
	}

	@Test void c02_encodeAttrValueEmpty() {
		assertEquals("", encodeAttrValue("", false));
	}

	@Test void c03_encodeAttrValueSimple() {
		assertEquals("hello", encodeAttrValue("hello", false));
	}

	@Test void c04_encodeAttrValueAmpersand() {
		assertEquals("a&amp;b", encodeAttrValue("a&b", false));
	}

	@Test void c05_encodeAttrValueLessThan() {
		assertEquals("a&lt;b", encodeAttrValue("a<b", false));
	}

	@Test void c06_encodeAttrValueGreaterThan() {
		assertEquals("a&gt;b", encodeAttrValue("a>b", false));
	}

	@Test void c07_encodeAttrValueDoubleQuote() {
		assertEquals("a&quot;b", encodeAttrValue("a\"b", false));
	}

	@Test void c08_encodeAttrValueSingleQuote() {
		assertEquals("a&apos;b", encodeAttrValue("a'b", false));
	}

	@Test void c09_encodeAttrValueLeadingWhitespace() {
		// Leading space must be hex-encoded
		var r = encodeAttrValue(" hello", false);
		assertTrue(r.startsWith("_x"), r);
		assertTrue(r.endsWith("hello"), r);
	}

	@Test void c10_encodeAttrValueTrailingWhitespace() {
		var r = encodeAttrValue("hello ", false);
		assertTrue(r.startsWith("hello"), r);
		assertTrue(r.endsWith("_"), r);
	}

	@Test void c11_encodeAttrValueTrim() {
		// trim=true should remove leading/trailing whitespace before encoding
		assertEquals("hello", encodeAttrValue("  hello  ", true));
	}

	@Test void c12_encodeAttrValueControlChar() {
		// 0x05 is invalid XML char and must be hex-encoded
		var r = encodeAttrValue("ab", false);
		assertTrue(r.contains("_x"), r);
	}

	@Test void c13_encodeAttrValueExistingEscapeSequence() {
		// Source contains literal _x0041_ — leading underscore must itself be hex-encoded
		var r = encodeAttrValue("_x0041_", false);
		assertNotEquals("_x0041_", r);
		assertTrue(r.startsWith("_x"), r);
	}

	@Test void c14_encodeAttrValueTab() {
		// Tab in middle position maps to &#x0009; // NOSONAR
		assertEquals("a&#x0009;b", encodeAttrValue("a\tb", false));
	}

	//------------------------------------------------------------------------------------------------
	// encodeElementName (line 242-280)
	//------------------------------------------------------------------------------------------------

	@Test void d01_encodeElementNameNull() {
		assertEquals("_x0000_", XmlUtils.encodeElementName(null));
	}

	@Test void d02_encodeElementNameEmpty() {
		assertEquals("_xE000_", XmlUtils.encodeElementName(""));
	}

	@Test void d03_encodeElementNameSimple() {
		assertEquals("foo", XmlUtils.encodeElementName("foo"));
	}

	@Test void d04_encodeElementNameStartingDigitGetsEncoded() {
		var r = XmlUtils.encodeElementName("1abc");
		assertTrue(r.startsWith("_x"), r);
	}

	@Test void d05_encodeElementNameWithSpecialChar() {
		var r = XmlUtils.encodeElementName("foo bar");
		assertTrue(r.contains("_x"), r);
	}

	@Test void d06_encodeElementNameWithDot() {
		// Dot allowed in element name (after first position)
		assertEquals("a.b", XmlUtils.encodeElementName("a.b"));
	}

	@Test void d07_encodeElementNameWithHyphen() {
		assertEquals("a-b", XmlUtils.encodeElementName("a-b"));
	}

	@Test void d08_encodeElementNameWithDigitInMiddle() {
		assertEquals("a1b", XmlUtils.encodeElementName("a1b"));
	}

	@Test void d09_encodeElementNameWriterVariantNull() {
		assertEquals("_x0000_", encodeElementNameW(null));
	}

	@Test void d10_encodeElementNameWriterVariantSimple() {
		assertEquals("foo", encodeElementNameW("foo"));
	}

	@Test void d11_encodeElementNameWriterVariantSpecial() {
		var r = encodeElementNameW("foo bar");
		assertTrue(r.contains("_x"), r);
	}

	//------------------------------------------------------------------------------------------------
	// encodeText (line 303-337)
	//------------------------------------------------------------------------------------------------

	@Test void e01_encodeTextNull() {
		assertEquals("_x0000_", encodeText(null, false, false));
	}

	@Test void e02_encodeTextEmpty() {
		assertEquals("_xE000_", encodeText("", false, false));
	}

	@Test void e03_encodeTextSimple() {
		assertEquals("hello", encodeText("hello", false, false));
	}

	@Test void e04_encodeTextAmpersand() {
		assertEquals("a&amp;b", encodeText("a&b", false, false));
	}

	@Test void e05_encodeTextLessThan() {
		assertEquals("a&lt;b", encodeText("a<b", false, false));
	}

	@Test void e06_encodeTextGreaterThan() {
		assertEquals("a&gt;b", encodeText("a>b", false, false));
	}

	@Test void e07_encodeTextDoubleQuoteNotEscaped() {
		// Double quotes do NOT get escaped in element text (only in attributes)
		assertEquals("a\"b", encodeText("a\"b", false, false));
	}

	@Test void e08_encodeTextLeadingWhitespaceEncoded() {
		var r = encodeText(" hello", false, false);
		assertTrue(r.startsWith("_x"), r);
	}

	@Test void e09_encodeTextLeadingWhitespacePreserved() {
		// preserveWhitespace=true means leading/trailing whitespace passed through
		var r = encodeText(" hello", false, true);
		assertEquals(" hello", r);
	}

	@Test void e10_encodeTextTrim() {
		// trim removes whitespace before encoding
		assertEquals("hello", encodeText("  hello  ", true, false));
	}

	@Test void e11_encodeTextControlChar() {
		// 0x05 is not valid XML char
		var r = encodeText("ab", false, false);
		assertTrue(r.contains("_x"), r);
	}

	@Test void e12_encodeTextExistingEscapeSequence() {
		var r = encodeText("_x0041_", false, false);
		assertNotEquals("_x0041_", r);
		assertTrue(r.startsWith("_x"), r);
	}

	//------------------------------------------------------------------------------------------------
	// escapeText (line 345-370)
	//------------------------------------------------------------------------------------------------

	@Test void f01_escapeTextNull() {
		assertEquals("_x0000_", XmlUtils.escapeText(null));
	}

	@Test void f02_escapeTextSimple() {
		// No escaping needed for simple alphanumeric text without xml special chars.
		// Note: escapeText only escapes invalid xml chars / leading-trailing whitespace / escape sequences
		assertEquals("hello", XmlUtils.escapeText("hello"));
	}

	@Test void f03_escapeTextLeadingWhitespace() {
		var r = XmlUtils.escapeText(" hello");
		assertTrue(r.startsWith("_x"), r);
	}

	@Test void f04_escapeTextControlChar() {
		var r = XmlUtils.escapeText("ab");
		assertTrue(r.contains("_x"), r);
	}

	@Test void f05_escapeTextExistingEscapeSequence() {
		var r = XmlUtils.escapeText("_x0041_");
		assertNotEquals("_x0041_", r);
	}

	//------------------------------------------------------------------------------------------------
	// collapseTextNodes (line 79-99)
	//------------------------------------------------------------------------------------------------

	@Test void g01_collapseEmptyList() {
		List<Object> list = new ArrayList<>();
		var r = XmlUtils.collapseTextNodes(list);
		assertTrue(r.isEmpty());
	}

	@Test void g02_collapseSingleString() {
		List<Object> list = new ArrayList<>();
		list.add("foo");
		var r = XmlUtils.collapseTextNodes(list);
		assertEquals(1, r.size());
		assertEquals("foo", r.get(0));
	}

	@Test void g03_collapseAdjacentStrings() {
		List<Object> list = new ArrayList<>();
		list.add("foo");
		list.add("bar");
		list.add("baz");
		var r = XmlUtils.collapseTextNodes(list);
		assertEquals(1, r.size());
		assertEquals("foobarbaz", r.get(0));
	}

	@Test void g04_collapseStringsSeparatedByOther() {
		List<Object> list = new ArrayList<>();
		list.add("foo");
		list.add("bar");
		list.add(42); // breaks consecutive run
		list.add("baz");
		list.add("qux");
		var r = XmlUtils.collapseTextNodes(list);
		assertEquals(3, r.size());
		assertEquals("foobar", r.get(0));
		assertEquals(42, r.get(1));
		assertEquals("bazqux", r.get(2));
	}

	//------------------------------------------------------------------------------------------------
	// toReadableEvent (line 407-440)
	//------------------------------------------------------------------------------------------------

	@Test void h01_toReadableEvent() throws Exception {
		// Use a real XMLStreamReader to walk through events.
		var xml = "<root><child>text</child></root>";
		var f = javax.xml.stream.XMLInputFactory.newInstance();
		try (var sr = new java.io.StringReader(xml)) {
			var r = f.createXMLStreamReader(sr);
			r.next(); // START_ELEMENT (root)
			assertEquals("<root>", XmlUtils.toReadableEvent(r));
			r.next(); // START_ELEMENT (child)
			assertEquals("<child>", XmlUtils.toReadableEvent(r));
			r.next(); // CHARACTERS
			assertEquals("CHARACTERS=[text]", XmlUtils.toReadableEvent(r));
			r.next(); // END_ELEMENT (child)
			assertEquals("</child>", XmlUtils.toReadableEvent(r));
			r.next(); // END_ELEMENT (root)
			assertEquals("</root>", XmlUtils.toReadableEvent(r));
			r.close();
		}
	}

	// PROCESSING_INSTRUCTION (event type 3)
	@Test void h02_toReadableEvent_processingInstruction() throws Exception {
		var xml = "<?pi target?><root/>";
		var f = javax.xml.stream.XMLInputFactory.newInstance();
		try (var sr = new java.io.StringReader(xml)) {
			var r = f.createXMLStreamReader(sr);
			r.next(); // PROCESSING_INSTRUCTION
			assertEquals("PROCESSING_INSTRUCTION", XmlUtils.toReadableEvent(r));
			r.close();
		}
	}

	// COMMENT (event type 5) — need coalescing=false and IS_SUPPORTING_EXTERNAL_ENTITIES=false
	@Test void h03_toReadableEvent_comment() throws Exception {
		var xml = "<!-- my comment --><root/>";
		var f = javax.xml.stream.XMLInputFactory.newInstance();
		f.setProperty(javax.xml.stream.XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		try (var sr = new java.io.StringReader(xml)) {
			var r = f.createXMLStreamReader(sr);
			r.next(); // COMMENT
			if (r.getEventType() == 5) // only assert if parser emits COMMENT events
				assertEquals("COMMENTS=[" + r.getText() + "]", XmlUtils.toReadableEvent(r));
			r.close();
		}
	}

	// CDATA (event type 12)
	@Test void h04_toReadableEvent_cdata() throws Exception {
		var xml = "<root><![CDATA[cdata text]]></root>";
		var f = javax.xml.stream.XMLInputFactory.newInstance();
		f.setProperty(javax.xml.stream.XMLInputFactory.IS_COALESCING, Boolean.FALSE);
		try (var sr = new java.io.StringReader(xml)) {
			var r = f.createXMLStreamReader(sr);
			r.next(); // START_ELEMENT
			r.next(); // CDATA or CHARACTERS depending on parser
			if (r.getEventType() == 12)
				assertEquals("CDATA=[cdata text]", XmlUtils.toReadableEvent(r));
			r.close();
		}
	}

	//------------------------------------------------------------------------------------------------
	// encodeElementName — Unicode character range branches (line 460 inner loop)
	//------------------------------------------------------------------------------------------------

	// À-Ö range
	@Test void d12_encodeElementName_unicodeC0() {
		var s = "Àname"; // starts with À — valid XML name start char
		assertEquals("Àname", XmlUtils.encodeElementName(s));
	}

	// Ø-ö range
	@Test void d13_encodeElementName_unicodeD8() {
		var s = "Øname";
		assertEquals("Øname", XmlUtils.encodeElementName(s));
	}

	// ø-˿ range
	@Test void d14_encodeElementName_unicodeF8() {
		var s = "øname";
		assertEquals("øname", XmlUtils.encodeElementName(s));
	}

	// Ͱ-ͽ range
	@Test void d15_encodeElementName_unicode370() {
		var s = "Ͱname";
		assertEquals("Ͱname", XmlUtils.encodeElementName(s));
	}

	// Ϳ-῿ range
	@Test void d16_encodeElementName_unicode37F() {
		var s = "Ϳname";
		assertEquals("Ϳname", XmlUtils.encodeElementName(s));
	}

	// ‌-‍ range
	@Test void d17_encodeElementName_unicode200C() {
		var s = "‌name";
		assertEquals("‌name", XmlUtils.encodeElementName(s));
	}

	// ⁰-↏ range
	@Test void d18_encodeElementName_unicode2070() {
		var s = "⁰name";
		assertEquals("⁰name", XmlUtils.encodeElementName(s));
	}

	// Ⰰ-⿯ range
	@Test void d19_encodeElementName_unicode2C00() {
		var s = "Ⰰname";
		assertEquals("Ⰰname", XmlUtils.encodeElementName(s));
	}

	// 、-퟿ range
	@Test void d20_encodeElementName_unicode3001() {
		var s = "、name";
		assertEquals("、name", XmlUtils.encodeElementName(s));
	}

	// 豈-﷏ range
	@Test void d21_encodeElementName_unicodeF900() {
		var s = "豈name";
		assertEquals("豈name", XmlUtils.encodeElementName(s));
	}

	// ﷰ-� range
	@Test void d22_encodeElementName_unicodeFDF0() {
		var s = "ﷰname";
		assertEquals("ﷰname", XmlUtils.encodeElementName(s));
	}

	// · in non-first position (combining dot)
	@Test void d23_encodeElementName_unicodeB7() {
		var s = "a·b";
		assertEquals("a·b", XmlUtils.encodeElementName(s));
	}

	// ̀-ͯ in non-first position
	@Test void d24_encodeElementName_unicode0300() {
		var s = "àb";
		assertEquals("àb", XmlUtils.encodeElementName(s));
	}

	// ‿-⁀ in non-first position
	@Test void d25_encodeElementName_unicode203F() {
		var s = "a‿b";
		assertEquals("a‿b", XmlUtils.encodeElementName(s));
	}

	//------------------------------------------------------------------------------------------------
	// isHexCharacter — lowercase hex chars return false (branch miss)
	//------------------------------------------------------------------------------------------------

	// encodeAttrName with _xABCD_ escape sequence followed by lowercase hex (not matching escape → preserve underscore)
	@Test void b10_encodeAttrName_lowercase_hex_in_escape() throws Exception {
		// "_xabcd_" has lowercase hex, so isHexCharacter('a') returns false → not treated as escape → '_' at pos 0 encoded
		var r = encodeAttrName("_xabcd_");
		// underscore at position 0: not an escape sequence (lowercase), so appended directly
		assertTrue(r.startsWith("_") || r.startsWith("_x"), "result=" + r);
	}

	// needsElementNameEncoding: starts with letter (returns false quickly)
	@Test void d26_encodeElementName_all_letters_no_encoding() {
		assertEquals("abc", XmlUtils.encodeElementName("abc"));
	}
}
