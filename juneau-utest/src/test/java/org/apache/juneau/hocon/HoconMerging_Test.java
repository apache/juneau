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
 * Object merging tests for {@link HoconParser}.
 */
@SuppressWarnings("unchecked")
class HoconMerging_Test {

	@Test
	void f01_duplicateObjectsMerge() throws Exception {
		var hocon = "a { x = 1 }\na { y = 2 }";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		assertEquals(1, ((Number) a.get("x")).intValue());
		assertEquals(2, ((Number) a.get("y")).intValue());
	}

	@Test
	void f02_scalarOverwrite() throws Exception {
		var hocon = "k = 1\nk = 2";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(2, ((Number) m.get("k")).intValue());
	}

	@Test
	void f03_objectOverScalar() throws Exception {
		var hocon = "a = 1\na { b = 2 }";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		assertNotNull(a);
		assertEquals(2, ((Number) a.get("b")).intValue());
	}

	@Test
	void f04_scalarOverObject() throws Exception {
		var hocon = "a { b = 1 }\na = 2";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		assertEquals(2, ((Number) m.get("a")).intValue());
	}

	@Test
	void f05_deepMerge() throws Exception {
		var hocon = "a { b { x = 1 } }\na { b { y = 2 } }";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		var b = (Map<String, Object>) a.get("b");
		assertEquals(1, ((Number) b.get("x")).intValue());
		assertEquals(2, ((Number) b.get("y")).intValue());
	}

	@Test
	void f06_plusEqualsArray() throws Exception {
		var hocon = "list = [a, b]\nlist += c";
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse(hocon, Map.class, String.class, Object.class);
		var list = (List<?>) m.get("list");
		assertNotNull(list);
		assertEquals(3, list.size());
		assertEquals("a", list.get(0));
		assertEquals("b", list.get(1));
		assertEquals("c", list.get(2));
	}
}
