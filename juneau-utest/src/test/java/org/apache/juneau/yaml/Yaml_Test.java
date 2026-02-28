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
package org.apache.juneau.yaml;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"serial"})
class Yaml_Test extends TestBase {

	@Test void a01_basicString() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("hello", s.serialize("hello"));
	}

	@Test void a02_basicNumber() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("123", s.serialize(123));
	}

	@Test void a03_basicBoolean() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("true", s.serialize(true));
		assertEquals("false", s.serialize(false));
	}

	@Test void a04_basicNull() throws Exception {
		var s = YamlSerializer.DEFAULT;
		assertEquals("null", s.serialize(null));
	}

	@Test void a05_basicMap() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("a", "1");
		m.put("b", 2);
		// Map should serialize as: "a: '1'\nb: 2" (strings that look like numbers are quoted)
		String result = s.serialize(m);
		assertTrue(result.contains("a:"), "Expected 'a:' in: " + result);
		assertTrue(result.contains("b:"), "Expected 'b:' in: " + result);
	}

	@Test void a06_basicList() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var l = List.of("a", "b", "c");
		String result = s.serialize(l);
		assertTrue(result.contains("- a"), "Expected '- a' in: " + result);
		assertTrue(result.contains("- b"), "Expected '- b' in: " + result);
	}

	@Test void a07_nestedMap() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var inner = new LinkedHashMap<String,Object>();
		inner.put("x", "1");
		var outer = new LinkedHashMap<String,Object>();
		outer.put("inner", inner);
		String result = s.serialize(outer);
		assertTrue(result.contains("inner:"), "Expected 'inner:' in: " + result);
		assertTrue(result.contains("x:"), "Expected 'x:' in: " + result);
	}

	@Test void a08_mapWithList() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("name", "John");
		m.put("hobbies", List.of("reading", "coding"));
		String result = s.serialize(m);
		assertTrue(result.contains("name: John"), "Expected 'name: John' in: " + result);
		assertTrue(result.contains("- reading"), "Expected '- reading' in: " + result);
	}

	@Test void a09_specialCharQuoting() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("key", "value: with colon");
		String result = s.serialize(m);
		assertTrue(result.contains("\"value: with colon\"") || result.contains("'value: with colon'"),
			"Special chars should be quoted in: " + result);
	}

	@Test void a10_reservedWords() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		m.put("val", "true");
		String result = s.serialize(m);
		assertTrue(result.contains("\"true\""), "Reserved word 'true' should be quoted in: " + result);
	}

	@Test void a11_emptyMap() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var m = new LinkedHashMap<String,Object>();
		String result = s.serialize(m);
		assertNotNull(result);
	}

	@Test void a12_emptyList() throws Exception {
		var s = YamlSerializer.DEFAULT;
		var l = List.of();
		String result = s.serialize(l);
		assertNotNull(result);
	}
}
