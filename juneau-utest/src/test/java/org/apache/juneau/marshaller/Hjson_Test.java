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

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link Hjson}.
 */
@SuppressWarnings("unchecked")
class Hjson_Test {

	@Test
	void a01_of() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "test");
		a.put("count", 42);
		var hjson = Hjson.of(a);
		assertNotNull(hjson);
		assertTrue(hjson.contains("name") && hjson.contains("test"));
		assertTrue(hjson.contains("count") && hjson.contains("42"));
	}

	@Test
	void a02_to() throws Exception {
		var hjson = "{\"name\":\"Alice\",\"age\":30}";
		var m = (Map<String, Object>) Hjson.to(hjson, Map.class, String.class, Object.class);
		assertBean(m, "name,age", "Alice,30");
	}

	@Test
	void a03_roundTrip() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("name", "foo");
		a.put("value", 123);
		var hjson = Hjson.of(a);
		var b = (Map<String, Object>) Hjson.to(hjson, Map.class, String.class, Object.class);
		assertBean(b, "name,value", "foo,123");
	}

	@Test
	void a04_defaultInstance() throws Exception {
		var a = new LinkedHashMap<String, Object>();
		a.put("k", "v");
		var hjson = Hjson.DEFAULT.write(a);
		assertTrue(hjson.contains("k") && hjson.contains("v"));
		var b = (Map<String, Object>) Hjson.DEFAULT.read(hjson, Map.class, String.class, Object.class);
		assertBean(b, "k", "v");
	}
}
