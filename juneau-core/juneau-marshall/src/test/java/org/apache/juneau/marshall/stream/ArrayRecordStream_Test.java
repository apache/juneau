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
package org.apache.juneau.marshall.stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.bson.*;
import org.apache.juneau.marshall.cbor.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.json5.*;
import org.apache.juneau.marshall.msgpack.*;
import org.apache.juneau.marshall.parquet.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Parameterized contract test for the {@link Parser#parseArrayRecords(Object)} /
 * {@link Serializer#serializeArrayRecords(Object)} surface across every Juneau format declaring a
 * non-{@code NONE} array-record-stream capability.
 *
 * <p>
 * Verifies element streaming round-trips for every supported format.  Each cell:
 * <ol>
 *   <li>Opens an arrayRecordWriter.
 *   <li>Calls {@code write(...)} N times with N test beans.
 *   <li>Closes the writer (emits closing framing).
 *   <li>Opens an arrayRecordReader.
 *   <li>Calls {@code canRead()} / {@code read(Bean.class)} N times.
 *   <li>Asserts the elements come back in order with the right values.
 * </ol>
 */
@SuppressWarnings({
	"resource" // Test fixtures use in-memory streams/writers; closing is the record adapter's responsibility, not the test's.
})
class ArrayRecordStream_Test extends TestBase {

	/**
	 * Formats that genuinely "unwrap" a top-level wire array via {@code arrayRecordReader/Writer}.
	 * Naturally-multi-record formats (JSONL, CSV, SSE) are NOT in this list &mdash; their
	 * {@code recordReader/recordWriter} is already the multi-record cursor; they don't need (or
	 * have) an arrayRecord surface.
	 */
	enum Format {
		JSON     (JsonParser.DEFAULT,    JsonSerializer.DEFAULT,    Mode.TEXT),
		JSON5    (Json5Parser.DEFAULT,   Json5Serializer.DEFAULT,   Mode.TEXT),
		CBOR     (CborParser.DEFAULT,    CborSerializer.DEFAULT,    Mode.BINARY),
		MSGPACK  (MsgPackParser.DEFAULT, MsgPackSerializer.DEFAULT, Mode.BINARY),
		BSON     (BsonParser.DEFAULT,    BsonSerializer.DEFAULT,    Mode.BINARY),
		PARQUET  (ParquetParser.DEFAULT, ParquetSerializer.DEFAULT, Mode.BINARY),
		YAML     (YamlParser.DEFAULT,    YamlSerializer.DEFAULT,    Mode.TEXT);

		final Parser parser;
		final Serializer serializer;
		final Mode mode;

		Format(Parser p, Serializer s, Mode m) {
			this.parser = p;
			this.serializer = s;
			this.mode = m;
		}
	}

	enum Mode { TEXT, BINARY }

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

	/** Self-similar bean for the deep-nesting / self-reference array-element rows (175ab Item 2). */
	public static class Node {
		public String id;
		public Node child;
		public Node() {}
		public Node(String id, Node child) { this.id = id; this.child = child; }
	}

	/** Builds a {@link Node} chain {@code depth} levels deep, null-terminated. */
	private static Node chain(int depth) {
		Node n = null;
		for (var i = depth; i >= 1; i--)
			n = new Node("n" + i, n);
		return n;
	}

	// =====================================================================================
	// Capability declared
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void a01_capabilityIsNonNone(Format fmt) {
		if (fmt.parser != null)
			assertInstanceOf(ArrayRecordReadable.class, fmt.parser, "reader: " + fmt);
		if (fmt.serializer != null)
			assertInstanceOf(ArrayRecordWritable.class, fmt.serializer, "writer: " + fmt);
	}

	// =====================================================================================
	// Round-trip: write 3 beans, read back 3 beans
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void b01_streamThreeBeans(Format fmt) throws Exception {
		assumeTrue(fmt.serializer != null, "no serializer");
		assumeTrue(fmt.parser != null, "no parser (writer-only format)");
		// CSV is alias-to-recordWriter; it serializes a List as a tabular CSV; that round-trips.
		var beans = List.of(new Bean("a", 1), new Bean("b", 2), new Bean("c", 3));

		// Write
		var bytes = writeAll(fmt, beans);

		// Read
		var got = readAll(fmt, bytes, Bean.class);

		assertEquals(3, got.size(), "format=" + fmt);
		assertEquals("a", got.get(0).name);
		assertEquals(1, got.get(0).age);
		assertEquals("c", got.get(2).name);
		assertEquals(3, got.get(2).age);
	}

	// =====================================================================================
	// Round-trip: zero-element edge
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void c01_emptyArray(Format fmt) throws Exception {
		assumeTrue(fmt.serializer != null);
		assumeTrue(fmt.parser != null);
		var bytes = writeAll(fmt, List.of());
		var got = readAll(fmt, bytes, Bean.class);
		assertNotNull(got);
		assertTrue(got.isEmpty(), "format=" + fmt);
	}

	// =====================================================================================
	// Round-trip: one-element edge
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void d01_singleElement(Format fmt) throws Exception {
		assumeTrue(fmt.serializer != null);
		assumeTrue(fmt.parser != null);

		var bytes = writeAll(fmt, List.of(new Bean("solo", 42)));
		var got = readAll(fmt, bytes, Bean.class);
		assertEquals(1, got.size());
		assertEquals("solo", got.get(0).name);
		assertEquals(42, got.get(0).age);
	}

	// =====================================================================================
	// Iterator and stream views
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void e01_iteratorView(Format fmt) throws Exception {
		assumeTrue(fmt.serializer != null);
		assumeTrue(fmt.parser != null);

		var beans = List.of(new Bean("a", 1), new Bean("b", 2));
		var bytes = writeAll(fmt, beans);
		try (var r = openReader(fmt, bytes)) {
			var iter = r.iterator(Bean.class);
			var out = new ArrayList<Bean>();
			iter.forEachRemaining(out::add);
			assertEquals(2, out.size());
			assertEquals("a", out.get(0).name);
			assertEquals(2, out.get(1).age);
		}
	}

	@ParameterizedTest
	@EnumSource(Format.class)
	void e02_streamView(Format fmt) throws Exception {
		assumeTrue(fmt.serializer != null);
		assumeTrue(fmt.parser != null);

		var beans = List.of(new Bean("a", 1), new Bean("b", 2), new Bean("c", 3));
		var bytes = writeAll(fmt, beans);
		try (var r = openReader(fmt, bytes)) {
			var totalAge = r.stream(Bean.class).mapToInt(b -> b.age).sum();
			assertEquals(6, totalAge);
		}
	}

	// =====================================================================================
	// XML / HTML arrayRecordReader (caller-specified root)
	// =====================================================================================

	@Test
	void f01_xmlArrayRecordReader() throws Exception {
		var beans = List.of(new Bean("alice", 30), new Bean("bob", 25));
		var wire = org.apache.juneau.marshall.xml.XmlSerializer.DEFAULT.serialize(beans);
		var got = new ArrayList<Bean>();
		try (var r = org.apache.juneau.marshall.xml.XmlParser.DEFAULT.parseArrayRecords(wire, "object")) {
			while (r.canRead())
				got.add(r.read(Bean.class));
		}
		assertEquals(2, got.size());
		assertEquals("alice", got.get(0).name);
		assertEquals(25, got.get(1).age);
	}

	@Test
	void f02_htmlArrayRecordReader() throws Exception {
		var beans = List.of(new Bean("alice", 30), new Bean("bob", 25));
		var wire = org.apache.juneau.marshall.html.HtmlSerializer.DEFAULT.serialize(beans);
		var got = new ArrayList<Bean>();
		try (var r = org.apache.juneau.marshall.html.HtmlParser.DEFAULT.parseArrayRecords(wire, "tr")) {
			while (r.canRead())
				got.add(r.read(Bean.class));
		}
		assertEquals(2, got.size());
		assertEquals("alice", got.get(0).name);
		assertEquals(25, got.get(1).age);
	}

	/**
	 * Wrapper-element XML doc: the records live under a named child of a wrapper {@code <object>},
	 * alongside a differently-named sibling that must be ignored.  Exercises the real
	 * {@code rootElementName} filter (not the {@code <array>}-envelope no-op path of f01).  The wire
	 * is produced by the serializer so its exact shape is guaranteed round-trippable.
	 */
	@Test
	void f03_xmlArrayRecordReaderWrapperElementFiltersByName() throws Exception {
		var wrapper = new LinkedHashMap<String,Object>();
		wrapper.put("item", List.of(new Bean("alice", 30), new Bean("bob", 25)));
		wrapper.put("note", "ignore me");
		var wire = org.apache.juneau.marshall.xml.XmlSerializer.DEFAULT.serialize(wrapper);

		var got = new ArrayList<Bean>();
		try (var r = org.apache.juneau.marshall.xml.XmlParser.DEFAULT.parseArrayRecords(wire, "item")) {
			while (r.canRead())
				got.add(r.read(Bean.class));
		}
		assertEquals(2, got.size());
		assertEquals("alice", got.get(0).name);
		assertEquals(30, got.get(0).age);
		assertEquals("bob", got.get(1).name);
		assertEquals(25, got.get(1).age);
	}

	/** Single occurrence under the named child still yields exactly one record. */
	@Test
	void f04_xmlArrayRecordReaderSingleNamedChild() throws Exception {
		var wrapper = new LinkedHashMap<String,Object>();
		wrapper.put("item", List.of(new Bean("solo", 42)));
		var wire = org.apache.juneau.marshall.xml.XmlSerializer.DEFAULT.serialize(wrapper);

		var got = new ArrayList<Bean>();
		try (var r = org.apache.juneau.marshall.xml.XmlParser.DEFAULT.parseArrayRecords(wire, "item")) {
			while (r.canRead())
				got.add(r.read(Bean.class));
		}
		assertEquals(1, got.size());
		assertEquals("solo", got.get(0).name);
		assertEquals(42, got.get(0).age);
	}

	/** A root-element hint that matches no child yields an empty (but valid) cursor. */
	@Test
	void f05_xmlArrayRecordReaderUnknownRootYieldsEmpty() throws Exception {
		var wrapper = new LinkedHashMap<String,Object>();
		wrapper.put("item", List.of(new Bean("a", 1)));
		var wire = org.apache.juneau.marshall.xml.XmlSerializer.DEFAULT.serialize(wrapper);

		var got = new ArrayList<Bean>();
		try (var r = org.apache.juneau.marshall.xml.XmlParser.DEFAULT.parseArrayRecords(wire, "missing")) {
			while (r.canRead())
				got.add(r.read(Bean.class));
		}
		assertTrue(got.isEmpty());
	}

	// =====================================================================================
	// MsgPack count-overload (length-prefixed binary)
	// =====================================================================================

	@Test
	void g01_msgpackCountOverloadStreams() throws Exception {
		var beans = List.of(new Bean("a", 1), new Bean("b", 2), new Bean("c", 3));
		var baos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.serializeArrayRecords(baos, 3)) {
			for (var b : beans)
				w.write(b);
		}

		// Wire output should be byte-equal to serializing the list directly.
		var bulk = MsgPackSerializer.DEFAULT.serialize(beans);
		assertArrayEquals(bulk, baos.toByteArray());

		// Round-trip: parse the streamed bytes back and verify.
		@SuppressWarnings("unchecked")
		var got = (List<Bean>) MsgPackParser.DEFAULT.parse(baos.toByteArray(), List.class, Bean.class);
		assertEquals(3, got.size());
		assertEquals("a", got.get(0).name);
		assertEquals(3, got.get(2).age);
	}

	@Test
	void g02_msgpackCountMismatchThrows() throws Exception {
		var baos = new ByteArrayOutputStream();
		var w = MsgPackSerializer.DEFAULT.serializeArrayRecords(baos, 3);
		w.write(new Bean("a", 1));
		w.write(new Bean("b", 2));
		// declared 3, only wrote 2 → close should throw
		assertThrows(IOException.class, w::close);
	}

	@Test
	void g03_msgpackCountOverflowThrows() throws Exception {
		var baos = new ByteArrayOutputStream();
		try (var w = MsgPackSerializer.DEFAULT.serializeArrayRecords(baos, 1)) {
			w.write(new Bean("a", 1));
			var overflow = new Bean("b", 2);
			assertThrows(IllegalStateException.class, () -> w.write(overflow));
		}
	}

	// =====================================================================================
	// Very-deeply-nested array elements (5+ levels) through the true-streaming binary cursors.
	// Exercises element-at-a-time streaming over non-trivial element shapes.
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(value = Format.class, names = {"JSON", "JSON5", "CBOR", "MSGPACK"})
	void h01_deeplyNestedArrayElementsStream(Format fmt) throws Exception {
		var beans = List.of(chain(5), chain(6));
		var bytes = writeAll(fmt, beans);
		var got = readAll(fmt, bytes, Node.class);
		assertEquals(2, got.size());
		assertEquals("n1", got.get(0).id);
		assertEquals("n5", got.get(0).child.child.child.child.id);
		assertNull(got.get(0).child.child.child.child.child);
		assertEquals("n6", got.get(1).child.child.child.child.child.id);
	}

	// =====================================================================================
	// Self-reference / cycle as an array element — 175ab Item 2.  A cycle must surface as a
	// controlled failure, never a raw StackOverflowError, on the streaming write path.
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(value = Format.class, names = {"JSON", "JSON5", "CBOR", "MSGPACK"})
	void h02_selfReferenceArrayElementIsHandled(Format fmt) {
		var a = new Node("a", null);
		a.child = a;  // self-reference
		assertDoesNotThrow(() -> {
			try {
				writeAll(fmt, List.of(a));
			} catch (@SuppressWarnings("unused") Exception expectedControlledFailure) {
				// Controlled failure — acceptable.
			}
		}, "cycle leaked a raw StackOverflowError for " + fmt);
	}

	// =====================================================================================
	// Helpers
	// =====================================================================================

	private static Object writeAll(Format fmt, List<?> beans) throws Exception {
		if (fmt.mode == Mode.BINARY) {
			var baos = new ByteArrayOutputStream();
			try (var w = ((ArrayRecordWritable) fmt.serializer).serializeArrayRecords(baos)) {
				for (var b : beans)
					w.write(b);
			}
			return baos.toByteArray();
		}
		var sb = new StringBuilder();
		try (var w = ((ArrayRecordWritable) fmt.serializer).serializeArrayRecords(sb)) {
			for (var b : beans)
				w.write(b);
		}
		return sb.toString();
	}

	private static <T> List<T> readAll(Format fmt, Object input, Class<T> type) throws Exception {
		var out = new ArrayList<T>();
		try (var r = openReader(fmt, input)) {
			while (r.canRead())
				out.add(r.read(type));
		}
		return out;
	}

	private static RecordReader openReader(Format fmt, Object input) throws Exception {
		// For TEXT mode, input is a String / StringBuilder.
		// For BINARY mode, input is byte[].
		Object actualInput;
		if (fmt.mode == Mode.BINARY)
			actualInput = (input instanceof byte[]) ? input : ((String) input).getBytes();
		else
			actualInput = input.toString();
		return ((ArrayRecordReadable) fmt.parser).parseArrayRecords(actualInput);
	}
}
