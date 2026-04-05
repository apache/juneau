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
package org.apache.juneau.jcs;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.json.*;
import org.apache.juneau.serializer.*;
import org.junit.jupiter.api.*;

class JcsStrings_Test extends TestBase {

	@Test
	void b01_simpleString() throws Exception {
		assertEquals("{\"x\":\"hello\"}", JcsSerializer.DEFAULT.serialize(JsonMap.of("x", "hello")));
	}

	@Test
	void b02_escapeBackslash() throws Exception {
		assertEquals("{\"x\":\"\\\\\\\\\"}", JcsSerializer.DEFAULT.serialize(JsonMap.of("x", "\\\\")));
	}

	@Test
	void b03_escapeQuote() throws Exception {
		var m = JsonMap.of("x", "\"");
		assertTrue(JcsSerializer.DEFAULT.serialize(m).contains("\\\""));
	}

	@Test
	void b04_controlChars() throws Exception {
		var m = JsonMap.of("x", "\t\n");
		var s = JcsSerializer.DEFAULT.serialize(m);
		assertTrue(s.contains("\\t"));
		assertTrue(s.contains("\\n"));
	}

	@Test
	void b05_otherControlChars() throws Exception {
		// U+000F (non-predefined) uses lowercase \u000f
		var m = JsonMap.of("x", Character.toString((char) 0x000F));
		var s = JcsSerializer.DEFAULT.serialize(m);
		assertTrue(s.contains("\\u000f"));
	}

	@Test
	void b06_nonAsciiLiteral() throws Exception {
		// € (U+20AC) output as literal UTF-8 per RFC 8785
		var m = JsonMap.of("x", Character.toString((char) 0x20AC));
		var s = JcsSerializer.DEFAULT.serialize(m);
		assertTrue(s.contains(Character.toString((char) 0x20AC)));
	}

	@Test
	void b07_rfcStringExample() throws Exception {
		// RFC 8785 Section 3.2.2: €$\u000f\nA'B\"\\\/
		var s = Character.toString((char) 0x20AC) + "$" + Character.toString((char) 0x000F) + "\nA'B\"\\\\\"/";
		var m = JsonMap.of("string", s);
		var out = JcsSerializer.DEFAULT.serialize(m);
		assertTrue(out.contains("\\u000f"));
		assertTrue(out.contains("\\n"));
		assertTrue(out.contains("\\\\"));
	}

	@Test
	void b08_emojiLiteral() throws Exception {
		// Emoji (surrogate pair) output as literal UTF-8 per RFC 8785
		var emoji = Character.toString(Character.toCodePoint('\uD83D', '\uDE00'));
		var m = JsonMap.of("x", emoji);
		var s = JcsSerializer.DEFAULT.serialize(m);
		// Round-trip: parse back and verify
		var parsed = JsonParser.DEFAULT.parse(s, JsonMap.class);
		assertEquals(emoji, parsed.getString("x"));
	}

	@SuppressWarnings({
		"java:S5778" // assertThrows lambda calls JsonMap.of() and serialize() together; both are needed to reproduce the lone-surrogate error path
	})
	@Test
	void b09_loneSurrogateError() {
		var s = "\uDEAD";
		assertThrows(SerializeException.class, () -> JcsSerializer.DEFAULT.serialize(JsonMap.of("x", s)));
	}

	@Test
	void b10_emptyString() throws Exception {
		assertEquals("{\"x\":\"\"}", JcsSerializer.DEFAULT.serialize(JsonMap.of("x", "")));
	}
}
