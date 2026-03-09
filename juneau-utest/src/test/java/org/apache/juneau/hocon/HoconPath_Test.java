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
 * Path expression tests for {@link HoconParser}.
 */
@SuppressWarnings("unchecked")
class HoconPath_Test {

	@Test
	void d01_simplePath() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("a.b = 1", Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		assertNotNull(a);
		assertEquals(1, ((Number) a.get("b")).intValue());
	}

	@Test
	void d02_deepPath() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("a.b.c.d = 1", Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		var b = (Map<String, Object>) a.get("b");
		var c = (Map<String, Object>) b.get("c");
		assertEquals(1, ((Number) c.get("d")).intValue());
	}

	@Test
	void d03_quotedPathComponent() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("\"a.b\".c = 1", Map.class, String.class, Object.class);
		var ab = (Map<String, Object>) m.get("a.b");
		assertNotNull(ab);
		assertEquals(1, ((Number) ab.get("c")).intValue());
	}

	@Test
	void d04_mixedPathAndNested() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("a.b { c = 1 }", Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		var b = (Map<String, Object>) a.get("b");
		assertEquals(1, ((Number) b.get("c")).intValue());
	}

	@Test
	void d05_pathMerging() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("a.x = 1\na.y = 2", Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		assertEquals(1, ((Number) a.get("x")).intValue());
		assertEquals(2, ((Number) a.get("y")).intValue());
	}

	@Test
	void d06_pathOverwrite() throws Exception {
		var m = (Map<String, Object>) HoconParser.DEFAULT.parse("a.b = 1\na.b = 2", Map.class, String.class, Object.class);
		var a = (Map<String, Object>) m.get("a");
		assertEquals(2, ((Number) a.get("b")).intValue());
	}
}
