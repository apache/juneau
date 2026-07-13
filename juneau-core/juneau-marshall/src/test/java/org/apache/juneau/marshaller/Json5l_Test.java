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

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"unchecked", // Parser returns Object; cast to Map/List<JsonMap> in tests
	"resource"   // Stream/reader instances are intentional short-lived test fixtures; auto-close not required for these assertions.
})
class Json5l_Test extends TestBase {

	@Test void a01_of() throws Exception {
		var a = "foo";
		var b = JsonMap.of("foo", "bar");
		var c = list(JsonMap.of("a", 1), JsonMap.of("b", 2));

		assertEquals("\"foo\"", Json5l.of(a).trim());
		{ var sw = stringWriter(); Json5l.DEFAULT.write(a, sw); assertEquals("\"foo\"", sw.toString().trim()); }
		assertTrue(Json5l.of(b).contains("\"foo\":\"bar\""));
		var sw = new StringWriter();
		Json5l.DEFAULT.write(b, sw);
		assertTrue(sw.toString().contains("\"foo\":\"bar\""));
		var json5l = Json5l.of(c);
		assertTrue(json5l.contains("\"a\":1"));
		assertTrue(json5l.contains("\"b\":2"));
		assertEquals(2, json5l.split("\n").length);
	}

	@Test void a02_to() throws Exception {
		var a = "'foo'";
		var b = "{foo:'bar'}";
		var c = "{a:1}\n{b:2}";

		assertEquals("foo", Json5l.to(a, String.class));
		assertEquals("foo", Json5l.DEFAULT.read(stringReader(a), String.class));
		var m = (Map<String,String>) Json5l.to(b, Map.class, String.class, String.class);
		assertEquals("bar", m.get("foo"));
		m = (Map<String,String>) Json5l.DEFAULT.read(stringReader(b), Map.class, String.class, String.class);
		assertEquals("bar", m.get("foo"));
		var list = (List<JsonMap>) Json5l.to(c, List.class, JsonMap.class);
		assertEquals(2, list.size());
		assertEquals(1, list.get(0).getInt("a"));
		assertEquals(2, list.get(1).getInt("b"));
	}

	@Test void a03_roundTrip() throws Exception {
		var a = list(JsonMap.of("x", 1), JsonMap.of("y", 2));
		var json5l = Json5l.of(a);
		var b = (List<JsonMap>) Json5l.to(json5l, List.class, JsonMap.class);
		assertEquals(2, b.size());
		assertEquals(1, b.get(0).getInt("x"));
		assertEquals(2, b.get(1).getInt("y"));
	}

	@Test void a04_defaultInstance() throws Exception {
		var a = list(JsonMap.of("k", "v"));
		var json5l = Json5l.of(a);
		assertTrue(json5l.contains("\"k\":\"v\""));
		var b = (List<JsonMap>) Json5l.to(json5l, List.class, JsonMap.class);
		assertEquals(1, b.size());
		assertEquals("v", b.get(0).getString("k"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private static Writer stringWriter() {
		return new StringWriter();
	}

	private static Reader stringReader(String s) {
		return new StringReader(s);
	}
}
