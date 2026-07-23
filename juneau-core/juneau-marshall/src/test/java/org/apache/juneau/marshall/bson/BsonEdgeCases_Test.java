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
package org.apache.juneau.marshall.bson;

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Edge-case tests for BSON serialization and parsing.
 */
class BsonEdgeCases_Test extends TestBase {

	@Test
	void a01_emptyMap() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.write(JsonMap.of());
		var result = p.read(bytes, JsonMap.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void a02_emptyList() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.write(List.of());
		var result = p.read(bytes, List.class);
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void a03_nullValueInMap() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var m = new JsonMap();
		m.put("a", 1);
		m.put("b", null);
		var bytes = s.write(m);
		var result = p.read(bytes, JsonMap.class);
		assertNotNull(result);
		assertEquals(1, result.get("a"));
		assertNull(result.get("b"));
	}

	@Test
	void a04_nestedEmptyStructures() throws Exception {
		Map<String,Object> m = new LinkedHashMap<>();
		m.put("emptyMap", new LinkedHashMap<>());
		m.put("emptyList", List.of());
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.write(m);
		var result = p.read(bytes, Map.class);
		assertNotNull(result);
		assertTrue(((Map<?,?>)result.get("emptyMap")).isEmpty());
		assertTrue(((List<?>)result.get("emptyList")).isEmpty());
	}

	@Test
	void a05_unicodeStrings() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var value = "café \uD83D\uDE00";
		var bytes = s.write(JsonMap.of("s", value));
		var result = p.read(bytes, JsonMap.class);
		assertEquals(value, result.get("s"));
	}

	@Test
	void a06_optionalWrappedRoundTrip() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.write(o(42));
		var result = p.read(bytes, Optional.class);
		assertNotNull(result);
		assertTrue(result.isPresent());
		assertEquals(42, result.get());
	}
}
