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
import org.apache.juneau.marshall.csv.*;
import org.apache.juneau.marshall.jsonl.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.sse.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Parameterized contract test for naturally multi-record formats (JSONL, CSV, SSE).
 *
 * <p>
 * For these formats:
 * <ul>
 *   <li>{@code serializeRecords(out)} accepts N {@link RecordWriter#write(Object) write(...)} calls
 *       and emits one record per call.
 *   <li>{@code parseRecords(in)} yields N records via {@link RecordReader#canRead() canRead()} /
 *       {@link RecordReader#read(Class) read(...)}.
 *   <li>{@code parseArrayRecords/serializeArrayRecords} is NOT defined &mdash; that surface only applies to
 *       formats that need to unwrap a top-level wire array.
 *   <li>The record-stream output is wire-equivalent to {@code serialize(List.of(records))}.
 * </ul>
 */
@SuppressWarnings({
	"resource" // Test fixtures use in-memory streams/writers; closing is the record adapter's responsibility, not the test's.
})
class MultiRecordStream_Test extends TestBase {

	enum Format {
		JSONL  (JsonlParser.DEFAULT, JsonlSerializer.DEFAULT),
		CSV    (CsvParser.DEFAULT,   CsvSerializer.DEFAULT),
		SSE    (null,                SseSerializer.DEFAULT);  // SSE has no parser

		final Parser parser;
		final Serializer serializer;

		Format(Parser p, Serializer s) {
			this.parser = p;
			this.serializer = s;
		}
	}

	public static class Bean {
		public String name;
		public int age;
		public Bean() {}
		public Bean(String name, int age) { this.name = name; this.age = age; }
	}

	// =====================================================================================
	// Capability declared
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void a01_capabilityIsNonNone(Format fmt) {
		if (fmt.parser != null)
			assertInstanceOf(RecordReadable.class, fmt.parser, "reader: " + fmt);
		if (fmt.serializer != null)
			assertInstanceOf(RecordWritable.class, fmt.serializer, "writer: " + fmt);
	}

	// =====================================================================================
	// Multi-record round-trip
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void b01_threeBeanRoundTrip(Format fmt) throws Exception {
		assumeTrue(fmt.parser != null && fmt.serializer != null, "writer-only format covered separately");
		// SSE has no parser path, skip.
		assumeTrue(fmt != Format.SSE);

		var beans = List.of(new Bean("a", 1), new Bean("b", 2), new Bean("c", 3));
		var sb = new StringWriter();
		try (var w = ((RecordWritable) fmt.serializer).serializeRecords(sb)) {
			for (var b : beans)
				w.write(b);
		}

		var got = new ArrayList<Bean>();
		try (var r = ((RecordReadable) fmt.parser).parseRecords(sb.toString())) {
			while (r.canRead())
				got.add(r.read(Bean.class));
		}

		assertEquals(3, got.size(), "format=" + fmt);
		assertEquals("a", got.get(0).name);
		assertEquals(1, got.get(0).age);
		assertEquals("c", got.get(2).name);
		assertEquals(3, got.get(2).age);
	}

	// =====================================================================================
	// Wire-equivalence: N writes through recordWriter == serialize(List.of(...))
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void c01_wireEquivalentToSerializeList(Format fmt) throws Exception {
		assumeTrue(fmt.serializer != null);
		assumeTrue(fmt != Format.SSE, "SSE event identity test in d01");

		var beans = List.of(new Bean("a", 1), new Bean("b", 2));

		var streamed = new StringWriter();
		try (var w = ((RecordWritable) fmt.serializer).serializeRecords(streamed)) {
			for (var b : beans)
				w.write(b);
		}

		var bulk = fmt.serializer.serializeToString(beans);

		assertEquals(bulk, streamed.toString(), "stream-write must match serialize(List) for " + fmt);
	}

	// =====================================================================================
	// SSE writer-only, multi-event
	// =====================================================================================

	@Test
	void d01_sseMultipleEvents() throws Exception {
		var sb = new StringWriter();
		try (var w = SseSerializer.DEFAULT.serializeRecords(sb)) {
			w.write(new SseEvent().setEvent("a").setData("1"));
			w.write(new SseEvent().setEvent("b").setData("2"));
		}
		var wire = sb.toString();
		assertTrue(wire.contains("event: a"));
		assertTrue(wire.contains("data: 1"));
		assertTrue(wire.contains("event: b"));
		assertTrue(wire.contains("data: 2"));
	}

	// =====================================================================================
	// recordReader.read(T) returns the same as parse(input, T) for the FIRST record
	// =====================================================================================

	@ParameterizedTest
	@EnumSource(Format.class)
	void e01_firstReadEqualsParse(Format fmt) throws Exception {
		assumeTrue(fmt.parser != null);

		var beans = List.of(new Bean("a", 1), new Bean("b", 2));
		var sb = new StringWriter();
		try (var w = ((RecordWritable) fmt.serializer).serializeRecords(sb)) {
			for (var b : beans)
				w.write(b);
		}
		var wire = sb.toString();

		var viaParse = fmt.parser.parse(wire, Bean.class);
		Bean viaRecord;
		try (var r = ((RecordReadable) fmt.parser).parseRecords(wire)) {
			assertTrue(r.canRead());
			viaRecord = r.read(Bean.class);
		}
		assertEquals(viaParse.name, viaRecord.name);
		assertEquals(viaParse.age, viaRecord.age);
	}
}
