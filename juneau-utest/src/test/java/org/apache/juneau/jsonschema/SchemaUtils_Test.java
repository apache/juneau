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
package org.apache.juneau.jsonschema;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Smoke tests for {@link SchemaUtils}.
 *
 * <p>
 * Confirms the empty-instead-of-null contract on {@link SchemaUtils#parseMap(Object)},
 * {@link SchemaUtils#parseMap(String[])}, and {@link SchemaUtils#parseSet(String[])}.
 */
class SchemaUtils_Test extends TestBase {

	@Test void a01_parseMapObjectNullReturnsEmptyMutableJsonMap() throws Exception {
		var m = SchemaUtils.parseMap((Object)null);
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertEquals(JsonMap.class, m.getClass());
		m.put("x", 1);
		assertEquals(1, m.size());
	}

	@Test void a02_parseMapObjectEmptyStringReturnsEmptyMutableJsonMap() throws Exception {
		var m = SchemaUtils.parseMap("");
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertEquals(JsonMap.class, m.getClass());
		m.put("x", 1);
		assertEquals(1, m.size());
	}

	@Test void a03_parseMapObjectIgnoreSentinelStillReturnsIgnoreMap() throws Exception {
		var m = SchemaUtils.parseMap("IGNORE");
		assertNotNull(m);
		assertEquals(Boolean.TRUE, m.get("ignore"));
	}

	@Test void a04_parseMapObjectIgnoreSentinelMixedCase() throws Exception {
		var m = SchemaUtils.parseMap("ignore");
		assertNotNull(m);
		assertEquals(Boolean.TRUE, m.get("ignore"));
	}

	@Test void a05_parseMapObjectPopulatedJson() throws Exception {
		var m = SchemaUtils.parseMap("{a:1,b:'two'}");
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
		assertEquals("two", m.getString("b"));
	}

	@Test void a06_parseMapStringArrayEmptyReturnsEmptyMutableJsonMap() throws Exception {
		var m = SchemaUtils.parseMap(new String[0]);
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertEquals(JsonMap.class, m.getClass());
		m.put("x", 1);
		assertEquals(1, m.size());
	}

	@Test void a07_parseMapStringArrayJoinedEmptyReturnsEmptyMutableJsonMap() throws Exception {
		var m = SchemaUtils.parseMap(new String[]{""});
		assertNotNull(m);
		assertTrue(m.isEmpty());
		assertEquals(JsonMap.class, m.getClass());
		m.put("x", 1);
		assertEquals(1, m.size());
	}

	@Test void a08_parseMapStringArrayPopulated() throws Exception {
		var m = SchemaUtils.parseMap(new String[]{"{a:1, b:'two'}"});
		assertEquals(2, m.size());
		assertEquals(1, m.getInt("a"));
		assertEquals("two", m.getString("b"));
	}

	@Test void b01_parseSetEmptyArrayReturnsEmptyMutableSet() throws Exception {
		var s = SchemaUtils.parseSet(new String[0]);
		assertNotNull(s);
		assertTrue(s.isEmpty());
		s.add("x");
		assertEquals(1, s.size());
	}

	@Test void b02_parseSetJoinedEmptyReturnsEmptyMutableSet() throws Exception {
		var s = SchemaUtils.parseSet(new String[]{""});
		assertNotNull(s);
		assertTrue(s.isEmpty());
		s.add("x");
		assertEquals(1, s.size());
	}

	@Test void b03_parseSetCdlPopulated() throws Exception {
		var s = SchemaUtils.parseSet(new String[]{"a,b,c"});
		assertEquals(3, s.size());
		assertTrue(s.contains("a"));
		assertTrue(s.contains("b"));
		assertTrue(s.contains("c"));
	}

	@Test void b04_parseSetJsonArrayPopulated() throws Exception {
		var s = SchemaUtils.parseSet(new String[]{"['a','b','c']"});
		assertEquals(3, s.size());
		assertTrue(s.contains("a"));
		assertTrue(s.contains("b"));
		assertTrue(s.contains("c"));
	}
}
