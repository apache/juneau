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
package org.apache.juneau.marshall.jsonl;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.apache.juneau.marshall.stream.TokenStreamAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.lang.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.stream.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for the public JSONL / NDJSON token-streaming surface.
 */
@SuppressWarnings({
	"resource" // Token readers are closed via try-with-resources; JDT's flow analysis over chained factory calls yields false-positive leak reports.
})
class JsonlTokenStream_Test extends TestBase {

	// =================================================================================
	// A. Reader — JSONL flat-sequence semantics
	// =================================================================================

	@Nested class A_reader extends TestBase {

		@Test void a01_singleLine() throws Exception {
			try (var r = JsonlParser.DEFAULT.parseTokens("{\"a\":1}\n")) {
				assertSequence(r,
					TokenType.START_OBJECT,
					TokenType.FIELD_NAME,
					TokenType.VALUE_NUMBER,
					TokenType.END_OBJECT,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void a02_multipleLinesFlat() throws Exception {
			try (var r = JsonlParser.DEFAULT.parseTokens("{\"a\":1}\n{\"b\":2}\n")) {
				assertSequence(r,
					TokenType.START_OBJECT, TokenType.FIELD_NAME, TokenType.VALUE_NUMBER, TokenType.END_OBJECT,
					TokenType.START_OBJECT, TokenType.FIELD_NAME, TokenType.VALUE_NUMBER, TokenType.END_OBJECT,
					TokenType.END_OF_STREAM);
			}
		}

		@Test void a03_scalarPerLine() throws Exception {
			try (var r = JsonlParser.DEFAULT.parseTokens("1\n2\n3\n")) {
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(1L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(2L, r.getNumber().longValue());
				assertEquals(TokenType.VALUE_NUMBER, r.next()); assertEquals(3L, r.getNumber().longValue());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}

		@Test void a04_capability() throws Exception {
			assertInstanceOf(TokenReadable.class, JsonlParser.DEFAULT);
			try (var r = JsonlParser.DEFAULT.parseTokens("1\n")) {
				assertReaderStreaming(r);
			}
		}
	}

	// =================================================================================
	// B. Writer — newline after each top-level value
	// =================================================================================

	@Nested class B_writer extends TestBase {

		@Test void b01_singleObjectGetsTrailingNewline() throws Exception {
			var sb = new StringBuilder();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(sb)) {
				w.startObject();
				w.fieldName("a"); w.number(1);
				w.endObject();
			}
			assertEquals("{\"a\":1}\n", sb.toString());
		}

		@Test void b02_multipleObjects() throws Exception {
			var sb = new StringBuilder();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(sb)) {
				w.startObject(); w.fieldName("a"); w.number(1); w.endObject();
				w.startObject(); w.fieldName("b"); w.number(2); w.endObject();
			}
			assertEquals("{\"a\":1}\n{\"b\":2}\n", sb.toString());
		}

		@Test void b03_topLevelScalars() throws Exception {
			var sb = new StringBuilder();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(sb)) {
				w.number(1);
				w.number(2);
				w.string("hi");
			}
			assertEquals("1\n2\n\"hi\"\n", sb.toString());
		}

		@Test void b04_capability() throws Exception {
			assertInstanceOf(TokenWritable.class, JsonlSerializer.DEFAULT);
			var sb = new StringBuilder();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(sb)) {
				assertWriterStreaming(w);
			}
		}

		@Test void b05_writeAfterCloseThrows() throws Exception {
			var sb = new StringBuilder();
			var w = JsonlSerializer.DEFAULT.serializeTokens(sb);
			w.startObject().endObject();
			w.close();
			// The JsonlTokenWriter guards its own directly-implemented methods (not just the delegate).
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::startObject);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::startArray);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::endObject);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::endArray);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.fieldName("a"));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.string("x"));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.number(1));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.bool(true));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", w::nil);
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.binary(new byte[]{1}));
			assertThrowsWithMessage(IOException.class, "Token writer is closed.", () -> w.object(42));
			// close() stays idempotent and flush() after close stays a safe no-op.
			assertDoesNotThrow(w::close);
			assertDoesNotThrow(w::flush);
			// The pre-close content was flushed (BCT string-shape assertion on the produced output).
			assertString("{}\n", sb);
		}
	}

	// =================================================================================
	// C. read() — the natural JSONL primary API
	// =================================================================================

	@Nested class C_read extends TestBase {

		public static class Record {
			public String name;
			public int age;
		}

		@Test void c01_streamRecords() throws Exception {
			var input = "{\"name\":\"alice\",\"age\":30}\n{\"name\":\"bob\",\"age\":40}\n";
			var records = new java.util.ArrayList<Record>();
			try (var r = JsonlParser.DEFAULT.parseTokens(input)) {
				while (r.canRead())
					records.add(r.read(Record.class));
			}
			assertEquals(2, records.size());
			assertEquals("alice", records.get(0).name);
			assertEquals(30, records.get(0).age);
			assertEquals("bob", records.get(1).name);
			assertEquals(40, records.get(1).age);
		}

		@Test void c02_emptyInput() throws Exception {
			try (var r = JsonlParser.DEFAULT.parseTokens("")) {
				assertFalse(r.canRead());
				assertEquals(TokenType.END_OF_STREAM, r.next());
			}
		}
	}

	// =================================================================================
	// D. Round-trip
	// =================================================================================

	@Nested class D_roundTrip extends TestBase {

		@Test void d01_writeThenRead() throws Exception {
			var sb = new StringBuilder();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(sb)) {
				w.startObject(); w.fieldName("x"); w.number(1); w.endObject();
				w.startObject(); w.fieldName("x"); w.number(2); w.endObject();
			}
			var produced = sb.toString();

			var values = new java.util.ArrayList<Long>();
			try (var r = JsonlParser.DEFAULT.parseTokens(produced)) {
				while (r.canRead()) {
					var m = r.read(java.util.Map.class);
					values.add(((Number) m.get("x")).longValue());
				}
			}
			assertEquals(java.util.List.of(1L, 2L), values);
		}
	}

	// =================================================================================
	// E. object() — POJO-walking writer bridge with JSONL trailing newlines
	// =================================================================================

	@Nested class E_object extends TestBase {

		public static class EBean {
			public String name;
			public int age;
		}

		@Test void e01_objectEmitsTrailingNewline() throws Exception {
			var b1 = new EBean(); b1.name = "alice"; b1.age = 30;
			var b2 = new EBean(); b2.name = "bob";   b2.age = 40;
			var sb = new StringBuilder();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(sb)) {
				w.object(b1);
				w.object(b2);
			}
			// Each top-level value emits its trailing newline via the JsonlTokenWriter wrapper.
			// BeanMap iterates properties in alphabetical order, so age comes before name.
			assertEquals(JsonlSerializer.DEFAULT.serializeToString(b1) + JsonlSerializer.DEFAULT.serializeToString(b2),
				sb.toString());
		}

		@Test void e02_streamRecords() throws Exception {
			var records = java.util.List.of(
				java.util.Map.of("x", 1),
				java.util.Map.of("x", 2),
				java.util.Map.of("x", 3));
			var sb = new StringBuilder();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(sb)) {
				for (var r : records)
					w.object(r);
			}
			assertEquals("{\"x\":1}\n{\"x\":2}\n{\"x\":3}\n", sb.toString());
		}
	}

	// =================================================================================
	// F. File-backed writer resource ownership
	// =================================================================================

	@Nested class F_fileResource extends TestBase {

		@Test void f01_closeClosesOwnedStream() throws Exception {
			// The File factory registers the stream it opened as 'owned'; close() must close it.
			// Use the package-private (out, settings, owned) constructor with a probe Closeable to
			// directly assert the owned resource is closed.
			var closed = Flag.create();
			Closeable probe = closed::set;
			var w = new JsonlTokenWriter(new StringWriter(), JsonTokenWriter.Settings.DEFAULT, probe);
			w.startObject(); w.fieldName("a"); w.number(1); w.endObject();
			w.close();
			assertTrue(closed.isSet(), "Owned stream should be closed after close().");
		}

		@Test void f02_secondCloseIsNoop() throws Exception {
			// close() is idempotent: a second call must not re-close the owned stream.
			var closeCount = IntegerHolder.create();
			Closeable probe = () -> closeCount.add(1);
			var w = new JsonlTokenWriter(new StringWriter(), JsonTokenWriter.Settings.DEFAULT, probe);
			w.close();
			w.close();
			assertEquals(1, closeCount.get());
		}

		@Test void f03_fileWriterFlushesFullContent() throws Exception {
			// End-to-end: the File factory's BufferedOutputStream is owned and closed, so the full
			// content (including JSONL trailing newlines) is on disk after close().
			var tmp = File.createTempFile("juneau-jsonl-tokenstream-", ".jsonl");
			tmp.deleteOnExit();
			try (var w = JsonlSerializer.DEFAULT.serializeTokens(tmp)) {
				w.startObject(); w.fieldName("a"); w.number(1); w.endObject();
				w.startObject(); w.fieldName("b"); w.number(2); w.endObject();
			}
			// Raw-bytes proof: the owned file stream was flushed+closed, so the full JSONL content
			// (including trailing newlines) is on disk after close().
			assertString("{\"a\":1}\n{\"b\":2}\n", Files.readString(tmp.toPath(), StandardCharsets.UTF_8));

			// BCT state proof: stream the records back off disk and assert each parsed record's shape.
			try (var r = JsonlParser.DEFAULT.parseTokens(tmp)) {
				assertTrue(r.canRead());
				assertBean(r.read(java.util.Map.class), "a", "1");
				assertTrue(r.canRead());
				assertBean(r.read(java.util.Map.class), "b", "2");
				assertFalse(r.canRead());
			}
		}
	}

	// =================================================================================
	// G. Array-record stream — JSONL aliases its line record stream (no surrounding [...])
	// =================================================================================

	@Nested class G_arrayRecords extends TestBase {

		@Test void g01_capability() {
			// JSONL advertises the array-record reader/writer roles (inherited from the JSON layer)
			// and reports O(1)-memory array-record streaming.
			assertInstanceOf(ArrayRecordReadable.class, JsonlParser.DEFAULT);
			assertInstanceOf(ArrayRecordWritable.class, JsonlSerializer.DEFAULT);
			assertTrue(((ArrayRecordReadable) JsonlParser.DEFAULT).isArrayRecordStreaming());
			assertTrue(((ArrayRecordWritable) JsonlSerializer.DEFAULT).isArrayRecordStreaming());
		}

		@Test void g02_roundTrip() throws Exception {
			// Write several records via serializeArrayRecords(...).
			var sb = new StringBuilder();
			try (RecordWriter w = JsonlSerializer.DEFAULT.serializeArrayRecords(sb)) {
				assertTrue(w.isStreaming());
				w.write(java.util.Map.of("x", 1));
				w.write(java.util.Map.of("x", 2));
				w.write(java.util.Map.of("x", 3));
			}

			// Output is line-delimited JSONL — NOT a bracketed top-level [...] array.
			assertString("{\"x\":1}\n{\"x\":2}\n{\"x\":3}\n", sb);

			// Read them back via parseArrayRecords(...) and assert each record's shape with BCT.
			var records = new java.util.ArrayList<java.util.Map<?, ?>>();
			try (RecordReader r = JsonlParser.DEFAULT.parseArrayRecords(sb.toString())) {
				assertTrue(r.isStreaming());
				while (r.canRead())
					records.add(r.read(java.util.Map.class));
			}
			assertEquals(3, records.size());
			assertBean(records.get(0), "x", "1");
			assertBean(records.get(1), "x", "2");
			assertBean(records.get(2), "x", "3");
		}
	}
}
