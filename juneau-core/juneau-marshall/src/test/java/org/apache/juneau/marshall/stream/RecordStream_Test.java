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
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.hjson.*;
import org.apache.juneau.marshall.hocon.*;
import org.apache.juneau.marshall.html.*;
import org.apache.juneau.marshall.ini.*;
import org.apache.juneau.marshall.jcs.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.markdown.*;
import org.apache.juneau.marshall.oapi.*;
import org.apache.juneau.marshall.parquet.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.plaintext.*;
import org.apache.juneau.marshall.prototext.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.sse.*;
import org.apache.juneau.marshall.toml.*;
import org.apache.juneau.marshall.uon.*;
import org.apache.juneau.marshall.urlencoding.*;
import org.apache.juneau.marshall.xml.*;
import org.apache.juneau.marshall.yaml.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Parameterized contract test for the {@link RecordReader} / {@link RecordWriter} surface across
 * every Juneau format declaring a non-{@code NONE} record-stream capability.
 *
 * <p>
 * Walks a data-shape coverage matrix (scalars, beans with collection / map / array properties,
 * multi-dimensional collections, top-level Map&lt;String,...&gt; / List&lt;Map&gt;, empty / single-element
 * / heterogeneous payloads) against every format.  Each cell roundtrips via
 * {@code recordWriter.write(...) → wire → recordReader.read(...)} and compares semantically.
 * Format-specific limitations are explicit per-row {@code skips} with documented reasons.
 *
 * <p>
 * FULL-format token surfaces (json, jsonl, json5, cbor, msgpack) are still covered by their
 * own {@code *TokenStream_Test} files for the structural API; this test exercises only the
 * record-stream entry points.
 */
@SuppressWarnings({
	"resource" // Test fixtures use in-memory streams/writers; closing is the record adapter's responsibility, not the test's.
})
class RecordStream_Test extends TestBase {

	// =====================================================================================
	// Format catalog
	// =====================================================================================

	/**
	 * One row per format.  Skips encode format-specific limitations declared in the plan.
	 */
	enum Format {

		JSON       (JsonParser.DEFAULT,        JsonSerializer.DEFAULT),
		JSON5      (org.apache.juneau.marshall.json5.Json5Parser.DEFAULT,
		            org.apache.juneau.marshall.json5.Json5Serializer.DEFAULT),
		JSONL      (org.apache.juneau.marshall.jsonl.JsonlParser.DEFAULT,
		            org.apache.juneau.marshall.jsonl.JsonlSerializer.DEFAULT, Skip.MULTI_RECORD),
		CBOR       (org.apache.juneau.marshall.cbor.CborParser.DEFAULT,
		            org.apache.juneau.marshall.cbor.CborSerializer.DEFAULT, Mode.BINARY),
		MSGPACK    (org.apache.juneau.marshall.msgpack.MsgPackParser.DEFAULT,
		            org.apache.juneau.marshall.msgpack.MsgPackSerializer.DEFAULT, Mode.BINARY),
		BSON       (BsonParser.DEFAULT,         BsonSerializer.DEFAULT,         Mode.BINARY),
		PROTO      (PrototextParser.DEFAULT,        PrototextSerializer.DEFAULT,        Mode.BINARY, Skip.PROTO_LIMITATIONS),
		PARQUET    (ParquetParser.DEFAULT,      ParquetSerializer.DEFAULT,      Mode.BINARY, Skip.PARQUET_LIMITATIONS),
		YAML       (YamlParser.DEFAULT,         YamlSerializer.DEFAULT),
		TOML       (TomlParser.DEFAULT,         TomlSerializer.DEFAULT,         Skip.NESTED_ROOT_REQUIRED, Skip.LIMITED_BEAN_BINDING),
		HOCON      (HoconParser.DEFAULT,        HoconSerializer.DEFAULT,        Skip.NESTED_ROOT_REQUIRED, Skip.LIMITED_BEAN_BINDING),
		HJSON      (HjsonParser.DEFAULT,        HjsonSerializer.DEFAULT,        Skip.LIMITED_BEAN_BINDING),
		CSV        (CsvParser.DEFAULT,          CsvSerializer.DEFAULT,          Skip.CSV_FLAT_TABULAR_ONLY),
		PLAINTEXT  (PlainTextParser.DEFAULT,    PlainTextSerializer.DEFAULT,    Skip.PLAINTEXT_TOSTRING_ONLY),
		MARKDOWN   (MarkdownParser.DEFAULT,     MarkdownSerializer.DEFAULT,     Skip.MARKDOWN_FRAGMENT_ONLY),
		SSE        (null,                       SseSerializer.DEFAULT,          Skip.SSE_EVENT_ENVELOPE),
		INI        (IniParser.DEFAULT,          IniSerializer.DEFAULT,          Skip.NESTED_ROOT_REQUIRED, Skip.LIMITED_BEAN_BINDING),
		UON        (UonParser.DEFAULT,          UonSerializer.DEFAULT,          Skip.LIMITED_BEAN_BINDING),
		OAPI       (OpenApiParser.DEFAULT,      OpenApiSerializer.DEFAULT,      Skip.LIMITED_BEAN_BINDING),
		URLENC     (UrlEncodingParser.DEFAULT,  UrlEncodingSerializer.DEFAULT,  Skip.URLENC_LIMITATIONS),
		XML        (XmlParser.DEFAULT,          XmlSerializer.DEFAULT),
		HTML       (HtmlParser.DEFAULT,         HtmlSerializer.DEFAULT),
		// Writer-only formats below — read side is null.
		JSONSCHEMA (null,                       JsonSchemaSerializer.DEFAULT,   Skip.WRITER_ONLY_OUTPUT_ONLY),
		JCS        (null,                       JcsSerializer.DEFAULT,          Skip.WRITER_ONLY_OUTPUT_ONLY),
		SOAP       (null,                       org.apache.juneau.marshall.soap.SoapXmlSerializer.create().build(),
		            Skip.WRITER_ONLY_OUTPUT_ONLY);

		final Parser parser;
		final Serializer serializer;
		final Set<Skip> skips;
		final Mode mode;

		Format(Parser p, Serializer s, Object... opts) {
			this.parser = p;
			this.serializer = s;
			var ss = EnumSet.noneOf(Skip.class);
			Mode m = Mode.TEXT;
			for (var o : opts) {
				if (o instanceof Skip k) ss.add(k);
				else if (o instanceof Mode mm) m = mm;
			}
			this.skips = Collections.unmodifiableSet(ss);
			this.mode = m;
		}

		boolean has(Skip s) { return skips.contains(s); }
	}

	enum Mode { TEXT, BINARY }

	enum Skip {
		/** CSV is row-based; payloads must be a List of flat beans/maps. */
		CSV_FLAT_TABULAR_ONLY,
		/** PlainText only round-trips strings via toString(). */
		PLAINTEXT_TOSTRING_ONLY,
		/** Markdown is fragment-mode (table/list); only specific shapes round-trip cleanly. */
		MARKDOWN_FRAGMENT_ONLY,
		/** SSE wraps a single SseEvent envelope, not arbitrary POJOs. */
		SSE_EVENT_ENVELOPE,
		/** Writer-only format (no parser); only the writer side is tested. */
		WRITER_ONLY_OUTPUT_ONLY,
		/** TOML/HOCON/INI require a non-scalar, non-collection root (must be a bean or Map). */
		NESTED_ROOT_REQUIRED,
		/** JSONL produces multiple records on the wire; we test it via separate multi-record assertions. */
		MULTI_RECORD,
		/** Prototext's wire format requires beans with explicit field tags; many shapes don't round-trip. */
		PROTO_LIMITATIONS,
		/** Parquet's row-group layout doesn't roundtrip arbitrary nested shapes for single-bean payloads. */
		PARQUET_LIMITATIONS,
		/** UrlEncoding handles only string-keyed top-level structures cleanly. */
		URLENC_LIMITATIONS,
		/** TOML/HOCON/UON: bean-property generic-type round-trip (Map&lt;String,Bean&gt;) is lossy. */
		LIMITED_BEAN_BINDING;
	}

	// =====================================================================================
	// Test beans
	// =====================================================================================

	public static class Flat {
		public String name;
		public int age;
		public Flat() {}
		public Flat(String name, int age) { this.name = name; this.age = age; }
		@Override public boolean equals(Object o) { return o instanceof Flat f && Objects.equals(name, f.name) && age == f.age; }
		@Override public int hashCode() { return Objects.hash(name, age); }
	}

	public static class Nested {
		public String label;
		public Flat inner;
		public Nested() {}
		public Nested(String label, Flat inner) { this.label = label; this.inner = inner; }
		@Override public boolean equals(Object o) { return o instanceof Nested n && Objects.equals(label, n.label) && Objects.equals(inner, n.inner); }
		@Override public int hashCode() { return Objects.hash(label, inner); }
	}

	public static class WithCollections {
		public List<String> tags;
		public String[] aliases;
		public Set<Integer> ids;
		public WithCollections() {}
		public WithCollections(List<String> tags, String[] aliases, Set<Integer> ids) { this.tags = tags; this.aliases = aliases; this.ids = ids; }
	}

	public static class WithMap {
		public Map<String,String> attrs;
		public Map<String,Flat> people;
		public WithMap() {}
		public WithMap(Map<String,String> attrs, Map<String,Flat> people) { this.attrs = attrs; this.people = people; }
	}

	public static class Deep2D {
		public List<List<Flat>> grid;
		public Flat[][] matrix;
		public Deep2D() {}
		public Deep2D(List<List<Flat>> g, Flat[][] m) { this.grid = g; this.matrix = m; }
	}

	/** Self-similar bean used for the deep-nesting and self-reference/cycle matrix rows (175ab Item 2). */
	public static class Node {
		public String id;
		public Node child;
		public Node() {}
		public Node(String id, Node child) { this.id = id; this.child = child; }
		@Override public boolean equals(Object o) { return o instanceof Node n && Objects.equals(id, n.id) && Objects.equals(child, n.child); }
		@Override public int hashCode() { return Objects.hash(id, child); }
	}

	/** Builds a {@link Node} chain {@code depth} levels deep, null-terminated. */
	private static Node chain(int depth) {
		Node n = null;
		for (var i = depth; i >= 1; i--)
			n = new Node("n" + i, n);
		return n;
	}

	// =====================================================================================
	// Capability test
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void a01_capabilityIsNonNone(Format fmt) {
		if (fmt.parser != null)
			assertInstanceOf(RecordReadable.class, fmt.parser,
				"Reader capability must be non-NONE for " + fmt);
		if (fmt.serializer != null)
			assertInstanceOf(RecordWritable.class, fmt.serializer,
				"Writer capability must be non-NONE for " + fmt);
	}

	// =====================================================================================
	// Scalar round-trips — every format that can read+write strings handles this
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void b01_stringScalar(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE), "SSE wraps SseEvent only");
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY), "Markdown fragments don't round-trip arbitrary scalars");
		assumeFalse(fmt.has(Skip.NESTED_ROOT_REQUIRED), "Format requires a bean/map root");
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY), "CSV requires a tabular root");
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS), "Prototext requires bean schema");
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS), "Parquet requires row-shaped data");
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS), "UrlEncoding requires top-level map shape");
		assumeFalse(fmt.has(Skip.MULTI_RECORD), "JSONL multi-record covered separately");
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY), "writer-only no roundtrip");
		var got = roundTrip(fmt, "hello", String.class);
		assertEquals("hello", got);
	}

	// =====================================================================================
	// Flat bean — most formats handle this
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void c01_flatBean(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE), "SSE wraps SseEvent only");
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY), "PlainText only round-trips strings");
		assumeFalse(fmt.has(Skip.MULTI_RECORD), "JSONL multi-record covered separately");
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY), "writer-only no roundtrip");
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS), "Prototext requires schema annotations");
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS), "Parquet requires List wrapper");
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS), "UrlEncoding requires top-level map shape");
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY), "Markdown serializer/parser asymmetric for single-bean key/value table");
		var got = roundTrip(fmt, new Flat("alice", 30), Flat.class);
		assertEquals("alice", got.name);
		assertEquals(30, got.age);
	}

	// =====================================================================================
	// Nested bean — bean-with-bean property
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void d01_nestedBean(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY), "CSV is flat tabular only");
		var bean = new Nested("outer", new Flat("alice", 30));
		var got = roundTrip(fmt, bean, Nested.class);
		assertEquals("outer", got.label);
		assertEquals("alice", got.inner.name);
		assertEquals(30, got.inner.age);
	}

	// =====================================================================================
	// Bean with collection / array properties
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void e01_beanWithCollections(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY));
		var bean = new WithCollections(List.of("a","b"), new String[]{"x","y"}, new LinkedHashSet<>(List.of(1,2,3)));
		var got = roundTrip(fmt, bean, WithCollections.class);
		assertEquals(List.of("a","b"), got.tags);
		assertArrayEquals(new String[]{"x","y"}, got.aliases);
		assertEquals(Set.of(1,2,3), got.ids);
	}

	// =====================================================================================
	// Bean with map properties
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void f01_beanWithMaps(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY));
		var attrs = new LinkedHashMap<String,String>();
		attrs.put("k1", "v1");
		attrs.put("k2", "v2");
		var people = new LinkedHashMap<String,Flat>();
		people.put("a", new Flat("alice", 30));
		people.put("b", new Flat("bob", 25));
		var bean = new WithMap(attrs, people);
		var got = roundTrip(fmt, bean, WithMap.class);
		assertEquals("v1", got.attrs.get("k1"));
		assertEquals("v2", got.attrs.get("k2"));
		assertEquals("alice", got.people.get("a").name);
		assertEquals(25, got.people.get("b").age);
	}

	// =====================================================================================
	// Multi-dimensional collections
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void g01_multiDimensional(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY));
		var grid = List.of(
			List.of(new Flat("a", 1), new Flat("b", 2)),
			List.of(new Flat("c", 3))
		);
		var matrix = new Flat[][]{ {new Flat("x", 10)}, {new Flat("y", 20), new Flat("z", 30)} };
		var bean = new Deep2D(grid, matrix);
		var got = roundTrip(fmt, bean, Deep2D.class);
		assertEquals(2, got.grid.size());
		assertEquals("a", got.grid.get(0).get(0).name);
		assertEquals(2, got.matrix.length);
		assertEquals("z", got.matrix[1][1].name);
	}

	// =====================================================================================
	// Very-deeply-nested structure (5+ levels) — 175ab Item 2 (recursion-limit row)
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void g02_deeplyNestedFiveLevels(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY));
		var got = roundTrip(fmt, chain(5), Node.class);
		assertEquals("n1", got.id);
		assertEquals("n2", got.child.id);
		assertEquals("n3", got.child.child.id);
		assertEquals("n4", got.child.child.child.id);
		assertEquals("n5", got.child.child.child.child.id);
		assertNull(got.child.child.child.child.child);
	}

	// =====================================================================================
	// Self-reference / cycle — 175ab Item 2 (depends on the 175aa PojoWalker cycle guard).
	// Per-format cycle handling legitimately differs: most formats (json, json5, cbor, msgpack,
	// hjson, uon, oapi) surface a self-reference as a *controlled* failure — a SerializeException
	// (the base session converts a StackOverflowError into a recursion message) or the PojoWalker
	// IllegalStateException on the token-streaming path — while a few (bson, yaml, xml, html) bound
	// the walk and complete without throwing.  The cross-format invariant verified here is that a
	// cycle is ALWAYS handled in bounded time and NEVER leaks a raw StackOverflowError to the caller.
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void g03_selfReferenceCycleIsHandled(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY), "PlainText emits toString(), never recurses");
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY));
		assumeFalse(fmt.has(Skip.NESTED_ROOT_REQUIRED));

		var a = new Node("a", null);
		a.child = a;  // self-reference

		// Either a controlled throw (SerializeException/IOException/IllegalStateException) or a
		// bounded, throw-free completion is acceptable; a raw StackOverflowError is NOT.
		assertDoesNotThrow(() -> {
			try {
				serialize(fmt, a);
			} catch (@SuppressWarnings("unused") Exception expectedControlledFailure) {
				// Controlled failure — acceptable per-format cycle handling.
			}
		}, "cycle leaked a raw StackOverflowError for " + fmt);
	}

	// =====================================================================================
	// Top-level List<Bean>
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	@SuppressWarnings("unchecked")
	void h01_listOfBeans(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.NESTED_ROOT_REQUIRED));
		var beans = List.of(new Flat("alice", 30), new Flat("bob", 25));
		// Use List.class with element type to satisfy parsers like CSV that need element type.
		var wire = serialize(fmt, beans);
		var got = (List<Flat>) parseWithElement(fmt, wire, List.class, Flat.class);
		assertEquals(2, got.size());
		assertEquals("alice", got.get(0).name);
		assertEquals("bob", got.get(1).name);
	}

	// =====================================================================================
	// Top-level Map<String,Bean>
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	@SuppressWarnings("unchecked")
	void i01_mapOfBeans(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY));
		var map = new LinkedHashMap<String,Flat>();
		map.put("a", new Flat("alice", 30));
		map.put("b", new Flat("bob", 25));
		var wire = serialize(fmt, map);
		var got = (Map<String,Flat>) parseWithElement(fmt, wire, Map.class, String.class, Flat.class);
		assertEquals("alice", got.get("a").name);
		assertEquals(25, got.get("b").age);
	}

	// =====================================================================================
	// Empty / single-element edges
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	@SuppressWarnings("unchecked")
	void j01_emptyList(Format fmt) throws Exception {
		assumeWriteCapable(fmt);
		assumeFalse(fmt.has(Skip.SSE_EVENT_ENVELOPE));
		assumeFalse(fmt.has(Skip.PLAINTEXT_TOSTRING_ONLY));
		assumeFalse(fmt.has(Skip.CSV_FLAT_TABULAR_ONLY), "CSV needs at least one row");
		assumeFalse(fmt.has(Skip.WRITER_ONLY_OUTPUT_ONLY));
		assumeFalse(fmt.has(Skip.MULTI_RECORD));
		assumeFalse(fmt.has(Skip.PROTO_LIMITATIONS));
		assumeFalse(fmt.has(Skip.PARQUET_LIMITATIONS));
		assumeFalse(fmt.has(Skip.URLENC_LIMITATIONS));
		assumeFalse(fmt.has(Skip.LIMITED_BEAN_BINDING));
		assumeFalse(fmt.has(Skip.MARKDOWN_FRAGMENT_ONLY));
		assumeFalse(fmt.has(Skip.NESTED_ROOT_REQUIRED));
		var wire = serialize(fmt, List.of());
		var got = (List<Flat>) parseWithElement(fmt, wire, List.class, Flat.class);
		assertNotNull(got);
		assertEquals(0, got.size());
	}

	// =====================================================================================
	// SSE special-case: writer-only over SseEvent
	// =====================================================================================

	@Test
	void k01_sseWriterEmitsEnvelope() throws Exception {
		var fmt = Format.SSE;
		var ev = new SseEvent().setEvent("ping").setData("hello").setId("42");
		var wire = serialize(fmt, ev);
		assertTrue(wire.contains("event: ping"));
		assertTrue(wire.contains("data: hello"));
		assertTrue(wire.contains("id: 42"));
	}

	// =====================================================================================
	// PlainText special-case: only String round-trips
	// =====================================================================================

	@Test
	void l01_plainTextRoundTripsStringOnly() throws Exception {
		var got = roundTrip(Format.PLAINTEXT, "the quick brown fox", String.class);
		assertEquals("the quick brown fox", got);
	}

	// =====================================================================================
	// JsonSchema / JCS / SOAP writer-only: writer produces non-empty output
	// =====================================================================================

	@ParameterizedTest
	@ValueSource(strings = {"JSONSCHEMA", "JCS", "SOAP"})
	void m01_writerOnlyProducesOutput(String fmtName) throws Exception {
		var fmt = Format.valueOf(fmtName);
		var wire = serialize(fmt, new Flat("alice", 30));
		assertNotNull(wire);
		assertFalse(wire.isEmpty(), "Writer output must be non-empty for " + fmt);
	}

	// =====================================================================================
	// Helpers
	// =====================================================================================

	private static void assumeWriteCapable(Format fmt) {
		assumeTrue(fmt.serializer != null, "format has no serializer: " + fmt);
	}

	/**
	 * For naturally-multi-record formats (CSV, JSONL, SSE), a top-level {@link List} payload is
	 * written as N {@code write(...)} calls (one per element).  For single-rooted formats the
	 * whole list is written as a single root value.
	 */
	private static boolean naturallyMultiRecord(Format fmt) {
		return fmt == Format.CSV || fmt == Format.JSONL || fmt == Format.SSE;
	}

	private static String serialize(Format fmt, Object value) throws Exception {
		if (fmt.mode == Mode.BINARY) {
			var baos = new ByteArrayOutputStream();
			writeAll(fmt, ((RecordWritable) fmt.serializer).serializeRecords(baos), value);
			return Base64.getEncoder().encodeToString(baos.toByteArray());
		}
		var sb = new StringBuilder();
		writeAll(fmt, ((RecordWritable) fmt.serializer).serializeRecords(sb), value);
		return sb.toString();
	}

	private static byte[] serializeBytes(Format fmt, Object value) throws Exception {
		var baos = new ByteArrayOutputStream();
		writeAll(fmt, ((RecordWritable) fmt.serializer).serializeRecords(baos), value);
		return baos.toByteArray();
	}

	private static void writeAll(Format fmt, RecordWriter w, Object value) throws IOException {
		try (w) {
			if (naturallyMultiRecord(fmt) && value instanceof List<?> list) {
				for (var v : list)
					w.write(v);
			} else {
				w.write(value);
			}
		}
	}

	private static <T> T parse(Format fmt, String wire, Class<T> type) throws Exception {
		Object input = (fmt.mode == Mode.BINARY) ? Base64.getDecoder().decode(wire) : wire;
		try (var r = ((RecordReadable) fmt.parser).parseRecords(input)) {
			return r.read(type);
		}
	}

	private static Object parseWithElement(Format fmt, String wire, Class<?> outer, Class<?>... element) throws Exception {
		Object input = (fmt.mode == Mode.BINARY) ? Base64.getDecoder().decode(wire) : wire;
		// Use Parser.parse(Object input, Type type, Type... args) for generic type binding.
		var args = new java.lang.reflect.Type[element.length];
		System.arraycopy(element, 0, args, 0, element.length);
		return fmt.parser.parse(input, (java.lang.reflect.Type) outer, args);
	}

	private static <T> T roundTrip(Format fmt, Object value, Class<T> type) throws Exception {
		assertNotNull(fmt.parser, "format has no parser: " + fmt);
		if (fmt.mode == Mode.BINARY) {
			var bytes = serializeBytes(fmt, value);
			try (var r = ((RecordReadable) fmt.parser).parseRecords(bytes)) {
				return r.read(type);
			}
		}
		var wire = serialize(fmt, value);
		return parse(fmt, wire, type);
	}
}
