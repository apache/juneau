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
package org.apache.juneau.bson;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.TestBase;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link BsonParser}.
 */
class BsonParser_Test extends TestBase {

	@Test
	void a01_parseMap() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("a", 42, "b", "hello"));
		var p = BsonParser.create().build();
		var result = p.parse(bytes, JsonMap.class);
		assertNotNull(result);
		assertEquals(42, result.get("a"));
		assertEquals("hello", result.get("b"));
	}

	@Test
	void a02_parseList() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(List.of(1, 2, 3));
		var p = BsonParser.create().build();
		var result = p.parse(bytes, List.class);
		assertNotNull(result);
		assertEquals(List.of(1, 2, 3), result);
	}

	@Test
	void a03_parseFromInputStream() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var bytes = s.serialize(JsonMap.of("k", "v"));
		var p = BsonParser.create().build();
		try (var is = new java.io.ByteArrayInputStream(bytes)) {
			var result = p.parse(is, JsonMap.class);
			assertNotNull(result);
			assertEquals("v", result.get("k"));
		}
	}

	@Test
	void a04_nullKeyString() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().nullKeyString("__null__").build();
		var m = new JsonMap();
		m.put(null, "val");
		var bytes = s.serialize(m);
		var p = BsonParser.create().nullKeyString("__null__").build();
		var result = p.parse(bytes, JsonMap.class);
		assertNotNull(result);
		assertTrue(result.containsKey(null));
		assertEquals("val", result.get(null));
	}
}
