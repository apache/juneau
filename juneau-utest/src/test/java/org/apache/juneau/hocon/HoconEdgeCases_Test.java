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
package org.apache.juneau.hocon;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.TestBase;
import org.junit.jupiter.api.*;

/**
 * Edge case tests for {@link HoconSerializer} and {@link HoconParser}.
 */
@SuppressWarnings("unchecked")
class HoconEdgeCases_Test extends TestBase {

	@Test
	void i01_emptyInput() throws Exception {
		var m = HoconParser.DEFAULT.parse("", Map.class, String.class, Object.class);
		assertNull(m);
	}

	@Test
	void i02_onlyComments() throws Exception {
		var hocon = "# comment\n// line";
		var m = HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertNull(m);
	}

	@Test
	void i03_unicodeStrings() throws Exception {
		var m = new LinkedHashMap<String, Object>();
		m.put("text", "Hello 世界 café \uD83D\uDE00");
		m.put("key\u4E2D", "value");
		var hocon = HoconSerializer.DEFAULT.serialize(m);
		var parsed = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("Hello 世界 café \uD83D\uDE00", parsed.get("text"));
		assertEquals("value", parsed.get("key\u4E2D"));
	}

	@Test
	void i04_windowsLineEndings() throws Exception {
		var hocon = "a = x\r\nb = y";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertBean(m, "a,b", "x,y");
	}

	@Test
	void i05_deeplyNested() throws Exception {
		var hocon = "n0.n1.n2.n3.n4.n5.n6.n7.n8.n9 = 99";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var current = m;
		for (var i = 0; i < 9; i++) {
			var key = "n" + i;
			var next = current.get(key);
			assertNotNull(next);
			current = (Map<String, Object>) next;
		}
		assertEquals(99, ((Number) current.get("n9")).intValue());
	}

	@Test
	void i06_emptyObject() throws Exception {
		var hocon = "{}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertNotNull(m);
		assertTrue(m.isEmpty());
	}

	@Test
	void i07_emptyArray() throws Exception {
		var hocon = "arr = []";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var arr = (List<?>) m.get("arr");
		assertNotNull(arr);
		assertTrue(arr.isEmpty());
	}

	@Test
	void i08_tripleQuotedString() throws Exception {
		var hocon = "x = \"\"\"line1\nline2\"\"\"";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("line1\nline2", m.get("x"));
	}

	@Test
	void i09_valueWithSpaces() throws Exception {
		var hocon = "key = \"value with spaces\"";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("value with spaces", m.get("key"));
	}

	@Test
	void i10_hashCommentInline() throws Exception {
		var hocon = "a = 1 # comment\nb = 2";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(1, ((Number) m.get("a")).intValue());
		assertEquals(2, ((Number) m.get("b")).intValue());
	}
}
