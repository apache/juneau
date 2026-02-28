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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.msgpack.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

class StreamableSerializationTest extends TestBase {

	static final JsonSerializer JSON = JsonSerializer.create().json5().build();
	static final XmlSerializer XML = XmlSerializer.create().sq().ns().build();
	static final UonSerializer UON = UonSerializer.DEFAULT;
	static final HtmlSerializer HTML = HtmlSerializer.create().sq().build();
	static final MsgPackSerializer MSGPACK = MsgPackSerializer.DEFAULT;
	static final CsvSerializer CSV = CsvSerializer.DEFAULT;

	//====================================================================================================
	// Iterator serialization with JSON (lazy path)
	//====================================================================================================
	@Test void a01_iteratorWithJson() throws Exception {
		var i = List.of("foo", "bar", "baz").iterator();
		assertEquals("['foo','bar','baz']", JSON.serialize(i));
	}

	//====================================================================================================
	// Iterable (non-Collection) serialization with JSON
	//====================================================================================================
	@Test void a02_iterableWithJson() throws Exception {
		Iterable<String> iterable = () -> List.of("foo", "bar", "baz").iterator();
		assertEquals("['foo','bar','baz']", JSON.serialize(iterable));
	}

	//====================================================================================================
	// Stream serialization with JSON
	//====================================================================================================
	@Test void a03_streamWithJson() throws Exception {
		var stream = Stream.of("foo", "bar", "baz");
		assertEquals("['foo','bar','baz']", JSON.serialize(stream));
	}

	//====================================================================================================
	// Iterator with XML serializer
	//====================================================================================================
	@Test void a04_iteratorWithXml() throws Exception {
		var i = List.of("foo", "bar", "baz").iterator();
		var result = XML.serialize(i);
		assertTrue(result.contains("foo"), "XML output should contain 'foo': " + result);
		assertTrue(result.contains("bar"), "XML output should contain 'bar': " + result);
		assertTrue(result.contains("baz"), "XML output should contain 'baz': " + result);
	}

	//====================================================================================================
	// Iterable with XML serializer
	//====================================================================================================
	@Test void a05_iterableWithXml() throws Exception {
		Iterable<String> iterable = () -> List.of("foo", "bar", "baz").iterator();
		var result = XML.serialize(iterable);
		assertTrue(result.contains("foo"), "XML output should contain 'foo': " + result);
		assertTrue(result.contains("bar"), "XML output should contain 'bar': " + result);
	}

	//====================================================================================================
	// Stream with XML serializer
	//====================================================================================================
	@Test void a06_streamWithXml() throws Exception {
		var stream = Stream.of("foo", "bar", "baz");
		var result = XML.serialize(stream);
		assertTrue(result.contains("foo"), "XML output should contain 'foo': " + result);
		assertTrue(result.contains("baz"), "XML output should contain 'baz': " + result);
	}

	//====================================================================================================
	// Iterator with UON serializer
	//====================================================================================================
	@Test void a07_iteratorWithUon() throws Exception {
		var i = List.of("foo", "bar", "baz").iterator();
		assertEquals("@(foo,bar,baz)", UON.serialize(i));
	}

	//====================================================================================================
	// Iterator with MsgPack serializer (materialized path)
	//====================================================================================================
	@Test void a08_iteratorWithMsgPack() throws Exception {
		var i = List.of("foo", "bar", "baz").iterator();
		var result = MSGPACK.serialize(i);
		assertNotNull(result);
		assertTrue(result.length > 0, "MsgPack output should not be empty");
	}

	//====================================================================================================
	// Iterator with HTML serializer (materialized path)
	//====================================================================================================
	@Test void a09_iteratorWithHtml() throws Exception {
		var i = List.of("foo", "bar", "baz").iterator();
		var result = HTML.serialize(i);
		assertTrue(result.contains("foo"), "HTML output should contain 'foo': " + result);
		assertTrue(result.contains("bar"), "HTML output should contain 'bar': " + result);
		assertTrue(result.contains("baz"), "HTML output should contain 'baz': " + result);
	}

	//====================================================================================================
	// Stream with HTML serializer
	//====================================================================================================
	@Test void a10_streamWithHtml() throws Exception {
		var stream = Stream.of("foo", "bar", "baz");
		var result = HTML.serialize(stream);
		assertTrue(result.contains("foo"), "HTML output should contain 'foo': " + result);
		assertTrue(result.contains("baz"), "HTML output should contain 'baz': " + result);
	}

	//====================================================================================================
	// Iterator with CSV serializer
	//====================================================================================================
	@Test void a11_iteratorWithCsv() throws Exception {
		var i = List.of("foo", "bar", "baz").iterator();
		var result = CSV.serialize(i);
		assertTrue(result.contains("foo"), "CSV output should contain 'foo': " + result);
		assertTrue(result.contains("bar"), "CSV output should contain 'bar': " + result);
	}

	//====================================================================================================
	// Empty Iterator edge case
	//====================================================================================================
	@Test void a12_emptyIterator() throws Exception {
		var i = Collections.emptyIterator();
		assertEquals("[]", JSON.serialize(i));
	}

	//====================================================================================================
	// Empty Stream edge case
	//====================================================================================================
	@Test void a13_emptyStream() throws Exception {
		var stream = Stream.empty();
		assertEquals("[]", JSON.serialize(stream));
	}

	//====================================================================================================
	// Numeric Iterator
	//====================================================================================================
	@Test void a14_numericIterator() throws Exception {
		var i = List.of(1, 2, 3).iterator();
		assertEquals("[1,2,3]", JSON.serialize(i));
	}

	//====================================================================================================
	// Iterable with UON serializer
	//====================================================================================================
	@Test void a15_iterableWithUon() throws Exception {
		Iterable<String> iterable = () -> List.of("foo", "bar", "baz").iterator();
		assertEquals("@(foo,bar,baz)", UON.serialize(iterable));
	}

	//====================================================================================================
	// Stream with UON serializer
	//====================================================================================================
	@Test void a16_streamWithUon() throws Exception {
		var stream = Stream.of("foo", "bar", "baz");
		assertEquals("@(foo,bar,baz)", UON.serialize(stream));
	}

	//====================================================================================================
	// Enumeration serialization (backward compat via native support)
	//====================================================================================================
	@Test void a17_enumerationWithJson() throws Exception {
		var v = new Vector<>(List.of("foo", "bar", "baz"));
		var e = v.elements();
		var result = JSON.serialize(e);
		assertEquals("['foo','bar','baz']", result);
	}

	//====================================================================================================
	// Stream with MsgPack serializer (materialized path)
	//====================================================================================================
	@Test void a19_streamWithMsgPack() throws Exception {
		var stream = Stream.of("foo", "bar", "baz");
		var result = MSGPACK.serialize(stream);
		assertNotNull(result);
		assertTrue(result.length > 0, "MsgPack output should not be empty");
	}

	//====================================================================================================
	// Iterable with MsgPack serializer (materialized path)
	//====================================================================================================
	@Test void a20_iterableWithMsgPack() throws Exception {
		Iterable<String> iterable = () -> List.of("foo", "bar", "baz").iterator();
		var result = MSGPACK.serialize(iterable);
		assertNotNull(result);
		assertTrue(result.length > 0, "MsgPack output should not be empty");
	}

	//====================================================================================================
	// Mixed-type Iterator
	//====================================================================================================
	@Test void a21_mixedTypeIterator() throws Exception {
		var i = List.<Object>of("foo", 123, true).iterator();
		assertEquals("['foo',123,true]", JSON.serialize(i));
	}

	//====================================================================================================
	// Iterable with CSV serializer
	//====================================================================================================
	@Test void a22_iterableWithCsv() throws Exception {
		Iterable<String> iterable = () -> List.of("foo", "bar", "baz").iterator();
		var result = CSV.serialize(iterable);
		assertTrue(result.contains("foo"), "CSV output should contain 'foo': " + result);
	}

	//====================================================================================================
	// Stream with CSV serializer
	//====================================================================================================
	@Test void a23_streamWithCsv() throws Exception {
		var stream = Stream.of("foo", "bar", "baz");
		var result = CSV.serialize(stream);
		assertTrue(result.contains("foo"), "CSV output should contain 'foo': " + result);
	}
}
