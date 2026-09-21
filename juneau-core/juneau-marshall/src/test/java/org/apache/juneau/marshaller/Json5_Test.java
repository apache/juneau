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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // Stream/reader instances are intentional short-lived test fixtures; auto-close not required for these assertions.
})
class Json5_Test extends TestBase {

	@Test void a01_to() throws Exception {
		var in1 = "foo";
		var in2 = JsonMap.of("foo", "bar");
		var expected1 = "'foo'";
		var expected2 = "{foo:'bar'}";

		assertString(expected1, Json5.of(in1));
		{ var sw1 = stringWriter(); Json5.DEFAULT.write(in1, sw1); assertString(expected1, sw1); }
		assertString(expected2, Json5.of(in2));
		{ var sw2 = stringWriter(); Json5.DEFAULT.write(in2, sw2); assertString(expected2, sw2); }
	}

	@Test void a02_from() throws Exception {
		var in1 = "'foo'";
		var in2 = "{foo:'bar'}";
		var expected1 = "foo";
		var expected2 = "{foo:'bar'}";

		assertString(expected1, Json5.to(in1, String.class));
		assertString(expected1, Json5.DEFAULT.read(stringReader(in1), String.class));
		assertJson(expected2, Json5.to(in2, Map.class, String.class, String.class));
		assertJson(expected2, Json5.DEFAULT.read(stringReader(in2), Map.class, String.class, String.class));
	}

	@Test void a03_trailingCommas() throws Exception {
		var obj = Json5.to("{a:1,}", JsonMap.class);
		assertEquals(1, obj.getInt("a"));
		assertEquals(1, obj.size());

		var nested = Json5.to("{a:{b:2,},c:[3,],}", JsonMap.class);
		assertEquals(2, nested.getMap("a").getInt("b"));
		assertEquals(3, nested.getList("c").getInt(0));
		assertEquals(1, nested.getList("c").size());

		var arr = Json5.to("[1,2,]", JsonList.class);
		assertEquals(2, arr.size());
		assertEquals(1, arr.getInt(0));
		assertEquals(2, arr.getInt(1));

		var emptyish = Json5.to("[1,]", JsonList.class);
		assertEquals(1, emptyish.size());
		assertEquals(1, emptyish.getInt(0));
	}

	@Test void a04_json5Numbers() throws Exception {
		var m = Json5.to("{a:+1,b:.5,c:5.,d:0x1F,e:Infinity,f:NaN,g:-Infinity}", JsonMap.class);
		assertEquals(1, m.getInt("a"));
		assertEquals(0.5, m.get("b", Double.class));
		assertEquals(5.0, m.get("c", Double.class));
		assertEquals(0x1F, m.getInt("d"));
		assertEquals(Double.POSITIVE_INFINITY, m.get("e", Double.class));
		assertTrue(Double.isNaN(m.get("f", Double.class)));
		assertEquals(Double.NEGATIVE_INFINITY, m.get("g", Double.class));
	}

	@Test void a05_lineContinuation() throws Exception {
		assertEquals("foobar", Json5.to("'foo\\\nbar'", String.class));
		assertEquals("foobar", Json5.to("\"foo\\\nbar\"", String.class));
	}

	@Test void a06_commentsUnquotedKeysSingleQuotes() throws Exception {
		var m = Json5.to("{/*c*/a:1, // trail\nb:'x'}", JsonMap.class);
		assertEquals(1, m.getInt("a"));
		assertEquals("x", m.getString("b"));
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