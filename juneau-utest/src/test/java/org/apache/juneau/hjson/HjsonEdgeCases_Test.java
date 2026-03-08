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
package org.apache.juneau.hjson;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.*;

/**
 * Edge case tests for {@link HjsonSerializer} and {@link HjsonParser}.
 */
@SuppressWarnings("unchecked")
class HjsonEdgeCases_Test extends TestBase {

	@Test
	void f01_emptyInput() throws Exception {
		var m = HjsonParser.DEFAULT.parse("", Map.class, String.class, Object.class);
		assertNull(m);
	}

	@Test
	void f02_onlyComments() throws Exception {
		var hjson = "# comment\n// line\n/* block */";
		var m = HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertNull(m);
	}

	@Test
	void f03_unicodeStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("text", "Hello 世界 café \uD83D\uDE00");
		m.put("key\u4E2D", "value");
		var hjson = HjsonSerializer.DEFAULT.serialize(m);
		var parsed = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals("Hello 世界 café \uD83D\uDE00", parsed.get("text"));
		assertEquals("value", parsed.get("key\u4E2D"));
	}

	@Test
	void f04_windowsLineEndings() throws Exception {
		var hjson = "a: x\r\nb: y";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertBean(m, "a,b", "x,y");
		var hjson2 = "{\"a\":1,\"b\":2}";
		var m2 = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson2, Map.class, String.class, Object.class);
		assertEquals(1, m2.get("a"));
		assertEquals(2, m2.get("b"));
	}

	@Test
	void f05_deeplyNested() throws Exception {
		var sb = new StringBuilder();
		for (var i = 0; i < 12; i++)
			sb.append("{a:").append(i).append(",nest:");
		sb.append("{}");
		for (var i = 0; i < 12; i++)
			sb.append("}");
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(sb.toString(), Map.class, String.class, Object.class);
		var current = m;
		for (var i = 0; i < 12; i++) {
			assertEquals(i, current.get("a"));
			var next = current.get("nest");
			if (i < 11) {
				assertNotNull(next);
				current = (Map<String, Object>) next;
			}
		}
		assertEquals(0, ((Map<?, ?>) current.get("nest")).size());
	}

	@Test
	void f06_quotelessWithTrailingSpaces() throws Exception {
		var hjson = "key:   value with spaces   ";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals("value with spaces", m.get("key"));
	}

	@Test
	void f07_multilineEmpty() throws Exception {
		var hjson = "{\"x\":'''\n'''}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals("", m.get("x"));
	}

	@Test
	void f08_mixedQuoteStyles() throws Exception {
		var hjson = """
			{
			  a: quoteless
			  b: "quoted"
			  c: 'single'
			}
			""";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertBean(m, "a,b,c", "quoteless,quoted,single");
	}

	@Test
	void f09_quotelessNumberVsString() throws Exception {
		var hjson = "{\"n\":5,\"s\":\"5 times\"}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals(5, m.get("n"));
		assertEquals("5 times", m.get("s"));
		var hjson2 = "n: 5\ns: 5 times";
		var m2 = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson2, Map.class, String.class, Object.class);
		assertEquals(5, m2.get("n"));
		assertEquals("5 times", m2.get("s"));
	}

	@Test
	void f10_unicodeEscapeInQuoted() throws Exception {
		var hjson = "{\"a\":\"\\" + "u0041\",\"b\":\"\\" + "u00E9\",\"c\":\"\\" + "u4E2D\"}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals("A", m.get("a"));
		assertEquals("\u00E9", m.get("b"));
		assertEquals("\u4E2D", m.get("c"));
		var emoji = "{\"x\":\"\\" + "uD83D\\" + "uDE00\"}";
		var m2 = (Map<String, Object>) HjsonParser.DEFAULT.parse(emoji, Map.class, String.class, Object.class);
		assertEquals("\uD83D\uDE00", m2.get("x"));
	}

	@Test
	void f11_multilineIndentation() throws Exception {
		var hjson = """
			{
			  desc:
			    '''
			      line1
			      line2
			    '''
			}
			""";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		var desc = (String) m.get("desc");
		assertNotNull(desc);
		assertTrue(desc.contains("line1"));
		assertTrue(desc.contains("line2"));
	}
}
