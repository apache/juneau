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
package org.apache.juneau.marshaller;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link Bson} marshaller.
 */
class Bson_Test extends TestBase {

	@Test
	void a01_of() throws Exception {
		var map = JsonMap.of("foo", "bar", "num", 42);
		var bytes = Bson.of(map);
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a02_ofToOutputStream() throws Exception {
		var map = JsonMap.of("foo", "bar");
		var out = new ByteArrayOutputStream();
		Bson.of(map, out);
		var bytes = out.toByteArray();
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	@Test
	void a03_to() throws Exception {
		var map = JsonMap.of("foo", "bar", "num", 42);
		var bytes = Bson.of(map);
		var parsed = Bson.to(bytes, JsonMap.class);
		assertNotNull(parsed);
		assertEquals("bar", parsed.get("foo"));
		assertEquals(42, parsed.get("num"));
	}

	@Test
	void a04_toFromInputStream() throws Exception {
		var map = JsonMap.of("k", "v");
		var bytes = Bson.of(map);
		try (var is = new ByteArrayInputStream(bytes)) {
			var parsed = Bson.to(is, JsonMap.class);
			assertNotNull(parsed);
			assertEquals("v", parsed.get("k"));
		}
	}

	@Test
	void a05_roundTrip() throws Exception {
		var original = JsonMap.of("a", 1, "b", "hello", "c", true);
		var bytes = Bson.of(original);
		var roundTrip = Bson.to(bytes, JsonMap.class);
		assertEquals(original.get("a"), roundTrip.get("a"));
		assertEquals(original.get("b"), roundTrip.get("b"));
		assertEquals(original.get("c"), roundTrip.get("c"));
	}

	@Test
	void a06_defaultInstance() throws Exception {
		var bson = Bson.DEFAULT;
		var map = JsonMap.of("x", 1);
		var bytes = bson.write(map);
		var parsed = bson.read(bytes, JsonMap.class);
		assertEquals(1, parsed.get("x"));
	}
}
