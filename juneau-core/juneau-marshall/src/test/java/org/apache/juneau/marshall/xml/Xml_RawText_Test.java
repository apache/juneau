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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.marshall.xml.XmlFormat.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link XmlFormat#RAWTEXT} content format.
 *
 * <p>
 * RAWTEXT emits a scalar String content property <b>verbatim</b> (no {@code &amp;}/{@code &lt;}/{@code &gt;} entity
 * escaping and no {@code _x####_} whitespace/invalid-char encoding), mirroring HTML5 raw-text element semantics
 * ({@code <script>}/{@code <style>}).  On the parse side it is read back verbatim (no decode, no trim).
 */
class Xml_RawText_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// Serialize
	//-----------------------------------------------------------------------------------------------------------------

	// A body containing raw '<', '>' and '&' must be emitted VERBATIM (no entity escaping).
	@Test void a01_serializeVerbatim() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var t = new A();
		t.f1 = "f1";
		t.f2 = "if (a < b) { return a & b > c; }";
		assertEquals("<A f1='f1'>if (a < b) { return a & b > c; }</A>", s.write(t));
	}

	// Whitespace (newlines/tabs) must be emitted literally (contrast with TEXT_PWS's &#x000a;/&#x0009;/_x0020_).
	@Test void a02_serializeWhitespaceVerbatim() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var t = new A();
		t.f1 = "f1";
		t.f2 = "\n\tx();\n";
		assertEquals("<A f1='f1'>\n\tx();\n</A>", s.write(t));
	}

	// A literal _x####_ sequence must NOT be encoded on serialize (verbatim).
	@Test void a03_serializeNoUnderscoreEncoding() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var t = new A();
		t.f1 = "f1";
		t.f2 = "  _x0041_ leading";
		assertEquals("<A f1='f1'>  _x0041_ leading</A>", s.write(t));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Parse (round-trip) of StAX-safe content
	//-----------------------------------------------------------------------------------------------------------------

	// StAX-safe content (no raw '<' or bare '&') round-trips verbatim (no decode, no trim).
	@Test void b01_roundTripSafeContent() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new A();
		t.f1 = "f1";
		t.f2 = "var x = 1;\n";
		var r = s.write(t);
		var t2 = p.read(r, A.class);
		assertEquals(t.f2, t2.f2);
		assertEquals(json(t), json(t2));
	}

	// Caller-escaped markup (ViewTable-style '<' -> \u003c) round-trips byte-identical.
	@Test void b02_roundTripEscapedMarkup() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new A();
		t.f1 = "f1";
		t.f2 = "{\"a\":\"\\u003cb\\u003e\"}";
		var r = s.write(t);
		var t2 = p.read(r, A.class);
		assertEquals(t.f2, t2.f2);
	}

	// A literal _x0041_ in RAWTEXT content must NOT be decoded on parse (stays verbatim).
	@Test void b03_roundTripNoUnderscoreDecode() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new A();
		t.f1 = "f1";
		t.f2 = "value _x0041_ here";
		var r = s.write(t);
		var t2 = p.read(r, A.class);
		assertEquals("value _x0041_ here", t2.f2);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// LIMITATION: verbatim raw '<' is not round-trippable through Juneau's StAX parser.
	//-----------------------------------------------------------------------------------------------------------------

	// This test intentionally documents (and locks in) the constraint from the design: RAWTEXT emits raw '<'
	// verbatim (browser-correct), but Juneau parses via a StAX XMLStreamReader, so the resulting non-well-formed
	// XML CANNOT be parsed back.  Callers embedding markup must escape '<' -> \u003c (see b02).
	@Test void b04_rawAngleBracketNotParseable() throws Exception {
		var s = XmlSerializer.DEFAULT_SQ;
		var p = XmlParser.DEFAULT;
		var t = new A();
		t.f1 = "f1";
		t.f2 = "if (a < b) {}";
		var r = s.write(t);
		// The serialized output is verbatim (browser-correct)...
		assertEquals("<A f1='f1'>if (a < b) {}</A>", r);
		// ...but is not well-formed XML, so parsing it back fails.
		assertThrows(ParseException.class, () -> p.read(r, A.class));
	}

	@Marshalled(typeName="A")
	public static class A {
		@Xml(format=ATTR) public String f1;
		@Xml(format=RAWTEXT) public String f2;
	}
}
