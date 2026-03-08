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
 * Tests for {@link HjsonParser}.
 */
@SuppressWarnings("unchecked")
class HjsonParser_Test extends TestBase {

	@Test
	void a01_quotedStrings() throws Exception {
		var hjson = "{\"name\":\"Alice\",\"age\":30}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertBean(m, "name,age", "Alice,30");
	}

	@Test
	void a02_quotelessStrings() throws Exception {
		var hjson = """
			{
			  name: Alice
			  age: 30
			}
			""";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertBean(m, "name,age", "Alice,30");
	}

	@Test
	void a03_rootBraceless() throws Exception {
		var hjson = "name: Bob\nage: 25";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertBean(m, "name,age", "Bob,25");
	}

	@Test
	void a04_numberAndBoolean() throws Exception {
		var hjson = "{\"count\":42,\"flag\":true}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals(42, m.get("count"));
		assertEquals(true, m.get("flag"));
	}

	@Test
	void a05_nullValue() throws Exception {
		var hjson = "{\"name\":\"x\",\"middle\":null}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertNull(m.get("middle"));
		assertEquals("x", m.get("name"));
	}

	@Test
	void a06_emptyInput() throws Exception {
		var m = HjsonParser.DEFAULT.parse("", Map.class, String.class, Object.class);
		assertNull(m);
	}

	@Test
	void a07_loneStringAtRoot() throws Exception {
		assertEquals("foo bar", HjsonParser.DEFAULT.parse("foo bar", String.class));
		assertEquals("𤭢𤭢", HjsonParser.DEFAULT.parse("𤭢𤭢", String.class));
	}

	@Test
	void b01_hashComments() throws Exception {
		var hjson = """
			{
			  # comment
			  name: Alice
			  age: 30
			}
			""";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertBean(m, "name,age", "Alice,30");
	}

	@Test
	void b02_slashComments() throws Exception {
		var hjson = """
			{
			  // comment
			  name: Bob
			}
			""";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals("Bob", m.get("name"));
	}

	@Test
	void b03_blockComments() throws Exception {
		var hjson = """
			{
			  /* block */
			  x: 1
			}
			""";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals(1, m.get("x"));
	}

	@Test
	void b04_trailingCommas() throws Exception {
		var hjson = "{\"a\":1,\"b\":2}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertBean(m, "a,b", "1,2");
	}

	@Test
	void b05_quotelessValueEdgeCases() throws Exception {
		var hjson = "n: 5\ns: 5 times\nb: true";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		assertEquals(5, m.get("n"));
		assertEquals("5 times", m.get("s"));
		assertEquals(true, m.get("b"));
	}

	@Test
	void b06_nestedObjects() throws Exception {
		var hjson = "{\"outer\":{\"inner\":\"value\"}}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		var outer = (Map<String, Object>) m.get("outer");
		assertNotNull(outer);
		assertEquals("value", outer.get("inner"));
	}

	@Test
	void b07_arrays() throws Exception {
		var hjson = "{\"arr\":[1,2,3]}";
		var m = (Map<String, Object>) HjsonParser.DEFAULT.parse(hjson, Map.class, String.class, Object.class);
		var arr = m.get("arr");
		assertTrue(arr instanceof java.util.List);
	}
}
