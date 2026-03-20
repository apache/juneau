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
package org.apache.juneau.jcs;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.jupiter.api.*;

/**
 * Edge case tests for JCS serialization.
 */
class JcsEdgeCases_Test extends TestBase {

	@Test
	void f01_emptyBean() throws Exception {
		assertEquals("{}", JcsSerializer.DEFAULT.serialize(JsonMap.of()));
	}

	@Test
	void f02_nullRoot() throws Exception {
		assertEquals("null", JcsSerializer.DEFAULT.serialize(null));
	}

	@Test
	void f03_booleanRoot() throws Exception {
		assertEquals("true", JcsSerializer.DEFAULT.serialize(true));
		assertEquals("false", JcsSerializer.DEFAULT.serialize(false));
	}

	@Test
	void f04_numberRoot() throws Exception {
		assertEquals("42", JcsSerializer.DEFAULT.serialize(42));
		assertEquals("3.14", JcsSerializer.DEFAULT.serialize(3.14));
	}

	@Test
	void f05_stringRoot() throws Exception {
		assertEquals("\"hello\"", JcsSerializer.DEFAULT.serialize("hello"));
	}

	@Test
	void f06_arrayRoot() throws Exception {
		var a = list(1, 2, 3);
		assertEquals("[1,2,3]", JcsSerializer.DEFAULT.serialize(a));
	}

	@Test
	void f07_deepNesting() throws Exception {
		var deep = JsonMap.of("a", JsonMap.of("b", JsonMap.of("c", JsonMap.of("d", JsonMap.of("e", 1)))));
		assertEquals("{\"a\":{\"b\":{\"c\":{\"d\":{\"e\":1}}}}}", JcsSerializer.DEFAULT.serialize(deep));
	}

	@Test
	void f08_duplicateMapKeys() throws Exception {
		// JSON semantics: last value wins. JCS output is sorted.
		var m = new LinkedHashMap<String, Integer>();
		m.put("x", 1);
		m.put("a", 2);
		m.put("x", 3);  // overwrites
		assertEquals("{\"a\":2,\"x\":3}", JcsSerializer.DEFAULT.serialize(m));
	}

	@Test
	void f09_largeObject() throws Exception {
		var m = new TreeMap<String, Integer>();
		for (var i = 0; i < 100; i++)
			m.put("k" + i, i);
		var s = JcsSerializer.DEFAULT.serialize(m);
		// All keys should be sorted (k0, k1, ..., k9, k10, ..., k99)
		assertTrue(s.startsWith("{\"k0\":0,\"k1\":1"));
		assertTrue(s.contains("\"k99\":99"));
		assertFalse(s.contains(" "));
	}

	@Test
	void f10_emptyStrings() throws Exception {
		var m = JsonMap.of("", "emptyVal", "emptyKey", "");
		assertEquals("{\"\":\"emptyVal\",\"emptyKey\":\"\"}", JcsSerializer.DEFAULT.serialize(m));
	}
}
