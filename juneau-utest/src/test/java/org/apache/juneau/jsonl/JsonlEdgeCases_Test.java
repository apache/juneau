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
package org.apache.juneau.jsonl;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Edge case tests for {@link JsonlSerializer} and {@link JsonlParser}.
 */
@SuppressWarnings({
	"unchecked" // Parser returns Object; cast to List<JsonMap>/List<Object> in tests
})
class JsonlEdgeCases_Test extends TestBase {

	@Test
	void a01_veryLargeCollection() throws Exception {
		var a = IntStream.range(0, 10_000)
			.mapToObj(i -> JsonMap.of("id", i, "name", "item" + i))
			.toList();
		var jsonl = Jsonl.of(a);
		var lines = jsonl.split("\n");
		assertEquals(10_000, lines.length);
		var b = (List<JsonMap>) Jsonl.to(jsonl, List.class, JsonMap.class);
		assertBean(b, "0{id,name},9999{id,name}", "{0,item0},{9999,item9999}");
	}

	@Test
	void a02_unicodeContent() throws Exception {
		var a = list(JsonMap.of("text", "Hello 世界 café \uD83D\uDE00"));
		var jsonl = Jsonl.of(a);
		var b = (List<JsonMap>) Jsonl.to(jsonl, List.class, JsonMap.class);
		assertBean(b, "0{text}", "{Hello 世界 café \uD83D\uDE00}");
	}

	@Test
	void a03_specialCharactersInStrings() throws Exception {
		var a = list(JsonMap.of("s", "a\nb\tc\"d\\e"));
		var jsonl = Jsonl.of(a);
		var b = (List<JsonMap>) Jsonl.to(jsonl, List.class, JsonMap.class);
		assertBean(b, "0{s}", "{a\nb\tc\"d\\e}");
	}

	@Test
	void a04_deeplyNestedObjects() throws Exception {
		var inner = JsonMap.of("depth", 10);
		for (var i = 9; i >= 1; i--) {
			inner = JsonMap.of("depth", i, "child", inner);
		}
		var a = list(inner);
		var jsonl = Jsonl.of(a);
		var b = (List<JsonMap>) Jsonl.to(jsonl, List.class, JsonMap.class);
		assertEquals(1, b.size());
		var c = b.get(0);
		for (var i = 1; i <= 10; i++) {
			assertEquals(i, c.getInt("depth"));
			c = c.get("child", JsonMap.class);
			if (i < 10)
				assertNotNull(c);
		}
	}

	@Test
	void a05_emptyObjects() throws Exception {
		var a = list(JsonMap.of(), JsonList.of());
		var jsonl = Jsonl.of(a);
		var b = (List<Object>) Jsonl.to(jsonl, List.class, Object.class);
		assertEquals(2, b.size());
		assertTrue(b.get(0) instanceof JsonMap);
		assertEquals(0, ((JsonMap) b.get(0)).size());
		assertTrue(b.get(1) instanceof JsonList);
		assertEquals(0, ((JsonList) b.get(1)).size());
	}

	@Test
	void a06_mixedLineEndings() throws Exception {
		var jsonl = "{\"a\":1}\r\n{\"b\":2}";
		var b = (List<JsonMap>) Jsonl.to(jsonl, List.class, JsonMap.class);
		assertBean(b, "0{a},1{b}", "{1},{2}");
	}

	@Test
	void a07_whitespaceOnlyLines() throws Exception {
		var jsonl = "{\"a\":1}\n   \n\t\n{\"b\":2}";
		var b = (List<JsonMap>) Jsonl.to(jsonl, List.class, JsonMap.class);
		assertBean(b, "0{a},1{b}", "{1},{2}");
	}

	@Test
	void a08_booleanAndNullLines() throws Exception {
		var jsonl = "true\nfalse\nnull";
		var b = (List<Object>) Jsonl.to(jsonl, List.class, Object.class);
		assertBean(b, "0,1,2", "true,false,<null>");
	}

	@Test
	void a09_numericLines() throws Exception {
		var jsonl = "1\n2.5\n-3\n1e10";
		var b = (List<Object>) Jsonl.to(jsonl, List.class, Object.class);
		assertBean(b, "0,1,2,3", "1,2.5,-3,1.0E10");
	}
}
