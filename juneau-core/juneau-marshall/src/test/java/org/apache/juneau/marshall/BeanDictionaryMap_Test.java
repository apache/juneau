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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

class BeanDictionaryMap_Test {

	@SuppressWarnings({
		"rawtypes", // mirrors raw-type signatures in BeanDictionaryMap.append()
		"serial"    // test-only subclass; serialVersionUID not needed
	})
	private static class TestMap extends BeanDictionaryMap {
		TestMap appendClass(String name, Class<?> c) { return (TestMap)append(name, c); }
		TestMap appendCollection(String name, Class<? extends Collection> col, Object entry) { return (TestMap)append(name, col, entry); }
		TestMap appendMap(String name, Class<? extends Map> map, Object k, Object v) { return (TestMap)append(name, map, k, v); }
	}

	// append(String, Class) — basic put
	@Test void a01_append_class() {
		var m = new TestMap().appendClass("str", String.class);
		assertEquals(String.class, m.get("str"));
	}

	// append(String, Collection, Class) — nn(o)=true, o instanceof Class → returns
	@Test void a02_append_collection_class_entry() {
		var m = new TestMap().appendCollection("list", ArrayList.class, String.class);
		assertNotNull(m.get("list"));
	}

	// append(String, Collection, Object[]) — nn(o)=true, isArray(o)=true → recurse into each element
	@Test void a03_append_collection_array_entry() {
		var m = new TestMap().appendCollection("list2", ArrayList.class, new Object[]{String.class});
		assertNotNull(m.get("list2"));
	}

	// append(String, Map, Class, Class) — two valid Class params
	@Test void a04_append_map() {
		var m = new TestMap().appendMap("map", HashMap.class, String.class, Integer.class);
		assertNotNull(m.get("map"));
	}

	// assertValidParameter(null) — nn(o)=false → throws
	@Test void a05_invalid_parameter_null() {
		assertThrows(Exception.class, () -> new TestMap().appendCollection("x", ArrayList.class, null));
	}

	// assertValidParameter(String) — nn(o)=true, not Class, not array → throws
	@Test void a06_invalid_parameter_string() {
		assertThrows(Exception.class, () -> new TestMap().appendCollection("x", ArrayList.class, "not-a-class"));
	}
}
