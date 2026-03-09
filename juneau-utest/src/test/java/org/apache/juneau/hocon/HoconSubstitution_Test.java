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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Substitution tests for {@link HoconParser}.
 */
@SuppressWarnings("unchecked")
class HoconSubstitution_Test {

	@Test
	void e01_simpleSubstitution() throws Exception {
		var hocon = "x = hello\nval = ${x}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("hello", m.get("x"));
		assertEquals("hello", m.get("val"));
	}

	@Test
	void e02_nestedPathSubstitution() throws Exception {
		var hocon = "a { b { c = 42 } }\nv = ${a.b.c}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(42, ((Number) m.get("v")).intValue());
	}

	@Test
	void e03_optionalSubstitutionPresent() throws Exception {
		var hocon = "x = hello\nval = ${?x}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("hello", m.get("val"));
	}

	@Test
	void e04_optionalSubstitutionMissing() throws Exception {
		var hocon = "val = ${?missing}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertNull(m.get("val"));
	}

	@Test
	void e05_requiredSubstitutionMissing() {
		var hocon = "val = ${missing}";
		assertThrows(Exception.class, () -> HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class));
	}

	@Test
	void e06_circularSubstitution() {
		var hocon = "a = ${b}\nb = ${a}";
		assertThrows(Exception.class, () -> HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class));
	}

	@Test
	void e07_stringConcatSubstitution() throws Exception {
		var hocon = "name = World\nval = \"Hello \" ${name}";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("Hello World", m.get("val"));
	}

	@Test
	void e08_selfReferentialAppend() throws Exception {
		var hocon = "path = /usr\npath = ${path}\"/bin\"";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals("/usr/bin", m.get("path"));
	}
}
