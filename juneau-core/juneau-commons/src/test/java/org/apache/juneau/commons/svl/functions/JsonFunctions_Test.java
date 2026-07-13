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
package org.apache.juneau.commons.svl.functions;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.apache.juneau.commons.svl.*;
import org.junit.jupiter.api.*;

/** Tests for {@link JsonFunctions}. */
class JsonFunctions_Test extends TestBase {

	private final VarResolver vr = VarResolver.create().functions(JsonFunctions.ALL).build();

	// JSON document with double-quotes used internally; the helper escapes them for embedding
	// inside a `"`-quoted script argument (so the script parser sees the entire JSON as one
	// argument despite the inner commas and quotes).
	private static final String JSON_DOC =
		"{\"name\":\"alice\",\"age\":30,\"tags\":[\"a\",\"b\",\"c\"],\"address\":{\"city\":\"sf\"}}";

	/** Wrap a string for use as a {@code "..."}-quoted script arg by escaping {@code "} and {@code \\}. */
	private static String arg(String raw) {
		var sb = new StringBuilder("\"");
		for (var c : raw.toCharArray()) {
			if (c == '"' || c == '\\') sb.append('\\');
			sb.append(c);
		}
		sb.append('"');
		return sb.toString();
	}

	@Test void jsonPath_topLevel() {
		assertEquals("alice", vr.resolve("#{jsonPath(" + arg(JSON_DOC) + ", \"/name\")}"));
	}

	@Test void jsonPath_arrayIndex() {
		assertEquals("b", vr.resolve("#{jsonPath(" + arg(JSON_DOC) + ", \"/tags/1\")}"));
	}

	@Test void jsonPath_nested() {
		assertEquals("sf", vr.resolve("#{jsonPath(" + arg(JSON_DOC) + ", \"/address/city\")}"));
	}

	@Test void jsonPath_missing_emptyDefault() {
		assertEquals("", vr.resolve("#{jsonPath(" + arg(JSON_DOC) + ", \"/nope\")}"));
	}

	@Test void jsonPath_missing_explicitDefault() {
		assertEquals("fallback", vr.resolve("#{jsonPath(" + arg(JSON_DOC) + ", \"/nope\", fallback)}"));
	}

	@Test void get_fromObject() {
		assertEquals("alice", vr.resolve("#{get(" + arg(JSON_DOC) + ", name)}"));
	}

	@Test void get_fromArray() {
		assertEquals("a", vr.resolve("#{get(" + arg("[\"a\",\"b\"]") + ", 0)}"));
	}

	@Test void get_missing() {
		assertEquals("", vr.resolve("#{get(" + arg(JSON_DOC) + ", nope)}"));
	}

	@Test void keys_object() {
		assertEquals("[\"name\",\"age\",\"tags\",\"address\"]",
			vr.resolve("#{keys(" + arg(JSON_DOC) + ")}"));
	}

	@Test void keys_nonObject() {
		assertEquals("[]", vr.resolve("#{keys(" + arg("[\"a\",\"b\"]") + ")}"));
	}

	@Test void values_object() {
		var s = vr.resolve("#{values(" + arg("{\"a\":1,\"b\":2}") + ")}");
		assertEquals("[\"1\",\"2\"]", s);
	}

	@Test void values_array() {
		var s = vr.resolve("#{values(" + arg("[\"a\",\"b\"]") + ")}");
		assertEquals("[\"a\",\"b\"]", s);
	}

	@Test void size_object() {
		assertEquals("4", vr.resolve("#{size(" + arg(JSON_DOC) + ")}"));
	}

	@Test void size_array() {
		assertEquals("2", vr.resolve("#{size(" + arg("[1,2]") + ")}"));
	}

	@Test void size_string() {
		assertEquals("5", vr.resolve("#{size(" + arg("\"hello\"") + ")}"));
	}
}
