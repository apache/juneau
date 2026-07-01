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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.lang.reflect.*;
import java.math.*;
import java.util.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * White-box coverage for the format-neutral {@code marshall.stream} helper layer:
 * {@link PojoWalker}, {@link RecordAdapter}, {@link StreamingArrayRecord}, the role-interface
 * default methods, and the {@link RecordReader} / {@link TokenReader} / {@link TokenWriter}
 * default methods that the per-format cursors inherit but do not all exercise.
 */
@SuppressWarnings({
	"resource" // In-memory cursors; closing is the helper's responsibility, not the test's.
})
class StreamInternals_Test extends TestBase {

	// =================================================================================
	// A. PojoWalker — every value-shape branch + every Options knob
	// =================================================================================

	@Nested class A_pojoWalker extends TestBase {

		public static class Bean {
			public int x = 1;
		}

		enum Color { RED, GREEN }

		private static String emit(Object value, PojoWalker.Options opts) throws IOException {
			var sw = new StringWriter();
			try (var w = new JsonTokenWriter(sw)) {
				PojoWalker.walk(w, value, opts);
			}
			return sw.toString();
		}

		private static String emit(Object value) throws IOException {
			return emit(value, PojoWalker.Options.DEFAULT);
		}

		@Test void a01_twoArgOverloadUsesDefaultOptions() throws Exception {
			// Exercises the walk(w, value) 2-arg overload (delegates to DEFAULT options).
			var sw = new StringWriter();
			try (var w = new JsonTokenWriter(sw)) {
				PojoWalker.walk(w, "hi");
			}
			assertEquals("\"hi\"", sw.toString());
		}

		@Test void a02_nullEmitsNil() throws Exception {
			assertEquals("null", emit(null));
		}

		@Test void a03_charSequence() throws Exception {
			assertEquals("\"hi\"", emit("hi"));
		}

		@Test void a04_charSequenceTrimmed() throws Exception {
			var trim = new PojoWalker.Options(true, false, false, false, false, true, null);
			assertEquals("\"hi\"", emit("  hi  ", trim));
		}

		@Test void a05_boolean() throws Exception {
			assertEquals("true", emit(Boolean.TRUE));
		}

		@Test void a06_byteArrayGoesThroughBinary() throws Exception {
			assertEquals("\"" + Base64.getEncoder().encodeToString(new byte[]{1, 2}) + "\"", emit(new byte[]{1, 2}));
		}

		@Test void a07_character() throws Exception {
			assertEquals("\"A\"", emit('A'));
		}

		@Test void a08_enum() throws Exception {
			assertEquals("\"RED\"", emit(Color.RED));
		}

		@Test void a09_allNumberSubtypes() throws Exception {
			// Long / Integer / Short / Byte / Double / Float / BigDecimal / BigInteger + an
			// unrecognized Number subtype (AtomicInteger) that falls through to number(Number).
			var nums = new ArrayList<Number>();
			nums.add(7L);
			nums.add(7);
			nums.add((short) 7);
			nums.add((byte) 7);
			nums.add(2.5d);
			nums.add(1.5f);
			nums.add(new BigDecimal("3.3"));
			nums.add(new BigInteger("9"));
			nums.add(new AtomicInteger(99));
			assertEquals("[7,7,7,7,2.5,1.5,3.3,9,99]", emit(nums));
		}

		@Test void a10_map() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", "x");
			assertEquals("{\"a\":1,\"b\":\"x\"}", emit(m));
		}

		@Test void a11_mapWithNullKeyAndNullValue() throws Exception {
			// Null key renders as the literal "null" field name; null value emitted (keepNullProperties default true).
			var m = new LinkedHashMap<String, Object>();
			m.put(null, 1);
			m.put("b", null);
			assertEquals("{\"null\":1,\"b\":null}", emit(m));
		}

		@Test void a12_sortMapsWithNullKey() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("z", 1);
			m.put(null, 2);
			var sort = new PojoWalker.Options(true, false, false, true, false, false, null);
			// Null key sorts as empty string (first).
			assertEquals("{\"null\":2,\"z\":1}", emit(m, sort));
		}

		@Test void a13_skipNullPropertiesWhenDisabled() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", null);
			var skipNulls = new PojoWalker.Options(false, false, false, false, false, false, null);
			assertEquals("{\"a\":1}", emit(m, skipNulls));
		}

		@Test void a14_trimEmptyMaps() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("b", Map.of());
			var trim = new PojoWalker.Options(true, true, false, false, false, false, null);
			assertEquals("{\"a\":1}", emit(m, trim));
		}

		@Test void a15_trimEmptyCollectionsAndArrays() throws Exception {
			var m = new LinkedHashMap<String, Object>();
			m.put("a", 1);
			m.put("emptyList", List.of());
			m.put("emptyArray", new int[0]);
			var trim = new PojoWalker.Options(true, false, true, false, false, false, null);
			assertEquals("{\"a\":1}", emit(m, trim));
		}

		@Test void a16_collection() throws Exception {
			assertEquals("[1,2,3]", emit(List.of(1, 2, 3)));
		}

		@Test void a17_sortCollections() throws Exception {
			var sort = new PojoWalker.Options(true, false, false, false, true, false, null);
			assertEquals("[\"a\",\"b\",\"c\"]", emit(List.of("c", "a", "b"), sort));
		}

		@Test void a18_array() throws Exception {
			assertEquals("[10,20]", emit(new int[]{10, 20}));
		}

		@Test void a19_sortArray() throws Exception {
			var sort = new PojoWalker.Options(true, false, false, false, true, false, null);
			assertEquals("[\"a\",\"b\",\"c\"]", emit(new String[]{"c", "a", "b"}, sort));
		}

		@Test void a20_iterable() throws Exception {
			// A bare Iterable that is NOT a Collection (covers the dedicated walkIterable branch).
			Iterable<Integer> it = () -> List.of(1, 2).iterator();
			assertEquals("[1,2]", emit(it));
		}

		@Test void a21_bean() throws Exception {
			assertEquals("{\"x\":1}", emit(new Bean()));
		}

		@Test void a22_nonBeanFallbackToString() throws Exception {
			// UUID is a registered non-bean: emit its canonical string form.
			var uuid = UUID.fromString("00000000-0000-0000-0000-000000000001");
			assertEquals("\"" + uuid + "\"", emit(uuid));
		}

		@Test void a23_recursionGuardOnCollection() {
			var l = new ArrayList<Object>();
			l.add(l);
			assertThrowsWithMessage(IllegalStateException.class, "Recursion detected", () -> emit(l));
		}

		@Test void a24_recursionGuardOnMap() {
			var m = new LinkedHashMap<String, Object>();
			m.put("self", m);
			assertThrowsWithMessage(IllegalStateException.class, "Recursion detected", () -> emit(m));
		}

		@Test void a25_sharedAcyclicReferenceAllowed() throws Exception {
			// The same node reachable by two distinct paths is permitted (not flagged as recursion).
			var shared = List.of(1, 2);
			assertEquals("[[1,2],[1,2]]", emit(List.of(shared, shared)));
		}

		@Test void a26_sortMapsEmptyMapStaysCompact() throws Exception {
			var sort = new PojoWalker.Options(true, false, false, true, false, false, null);
			assertEquals("{}", emit(Map.of(), sort));
		}

		@Test void a27_trimEmptyMapsKeepsNonEmptyMapValue() throws Exception {
			var trim = new PojoWalker.Options(true, true, false, false, false, false, null);
			assertEquals("{\"a\":{\"k\":1}}", emit(Map.of("a", Map.of("k", 1)), trim));
		}

		@Test void a28_trimEmptyCollectionsKeepsNonEmptyCollectionValue() throws Exception {
			var trim = new PojoWalker.Options(true, false, true, false, false, false, null);
			assertEquals("{\"a\":[1]}", emit(Map.of("a", List.of(1)), trim));
		}

		@Test void a29_trimEmptyCollectionsKeepsNonEmptyArrayValue() throws Exception {
			var trim = new PojoWalker.Options(true, false, true, false, false, false, null);
			var m = new LinkedHashMap<String, Object>();
			m.put("a", new int[]{1});
			assertEquals("{\"a\":[1]}", emit(m, trim));
		}

		@Test void a30_sortCollectionsEmptyCollectionStaysCompact() throws Exception {
			var sort = new PojoWalker.Options(true, false, false, false, true, false, null);
			assertEquals("[]", emit(List.of(), sort));
		}

		@Test void a31_sortCollectionsWithNullElement() throws Exception {
			var sort = new PojoWalker.Options(true, false, false, false, true, false, null);
			// Null element sorts as empty string (first).
			assertEquals("[null,\"b\"]", emit(Arrays.asList("b", null), sort));
		}

		@Test void a32_sortArrayEmptyStaysCompact() throws Exception {
			var sort = new PojoWalker.Options(true, false, false, false, true, false, null);
			assertEquals("[]", emit(new int[0], sort));
		}

		@Test void a33_sortArrayWithNullElement() throws Exception {
			var sort = new PojoWalker.Options(true, false, false, false, true, false, null);
			assertEquals("[null,\"b\"]", emit(new String[]{"b", null}, sort));
		}
	}

	// =================================================================================
	// B. RecordAdapter — all four buffered factories + edge paths
	// =================================================================================

	@Nested class B_recordAdapter extends TestBase {

		private static ParserSession parserSession() {
			return JsonParser.DEFAULT.getSession();
		}

		private static SerializerSession serializerSession() {
			return JsonSerializer.DEFAULT.getSession();
		}

		@Test void b01_readerSingleShot() throws Exception {
			try (var r = RecordAdapter.reader(parserSession(), "42")) {
				assertFalse(r.isStreaming());
				assertTrue(r.canRead());
				assertEquals(42, r.read(Integer.class));
				assertFalse(r.canRead());
				assertThrowsWithMessage(IllegalStateException.class, "already produced its single root value", () -> r.read(Integer.class));
			}
		}

		@Test void b02_readerViaClassMeta() throws Exception {
			var cm = MarshallingContext.DEFAULT.getClassMeta(Integer.class);
			try (var r = RecordAdapter.reader(parserSession(), "42")) {
				assertEquals(42, r.read(cm));
			}
		}

		@Test void b03_readerViaType() throws Exception {
			try (var r = RecordAdapter.reader(parserSession(), "42")) {
				assertEquals(42, r.<Integer>read((Type) Integer.class));
			}
		}

		@Test void b04_readerClosesCloseableInput() throws Exception {
			var sr = new StringReader("42");
			try (var r = RecordAdapter.reader(parserSession(), sr)) {
				assertEquals(42, r.read(Integer.class));
			}
			// close() must have closed the handed-in Closeable input.
			assertThrows(IOException.class, sr::read);
		}

		@Test void b05_arrayReaderIteratesElements() throws Exception {
			try (var r = RecordAdapter.arrayReader(parserSession(), "[1,2,3]")) {
				assertFalse(r.isStreaming());
				var seen = new ArrayList<Integer>();
				while (r.canRead())
					seen.add(r.read(Integer.class));
				assertList(seen, "1", "2", "3");
				assertThrowsWithMessage(IllegalStateException.class, "Array stream is exhausted", () -> r.read(Integer.class));
			}
		}

		@Test void b06_arrayReaderViaClassMetaAndType() throws Exception {
			var cm = MarshallingContext.DEFAULT.getClassMeta(Integer.class);
			try (var r = RecordAdapter.arrayReader(parserSession(), "[5,6]")) {
				assertEquals(5, r.read(cm));
				assertEquals(6, r.<Integer>read((Type) Integer.class));
			}
		}

		@Test void b07_arrayReaderNullParseYieldsEmpty() throws Exception {
			// Parsing "null" as a List yields null → adapter treats it as an empty stream.
			try (var r = RecordAdapter.arrayReader(parserSession(), "null")) {
				assertFalse(r.canRead());
			}
		}

		@Test void b08_arrayReaderWrapsParseExceptionAsIOException() {
			assertThrows(IOException.class, () -> RecordAdapter.arrayReader(parserSession(), "[1,2"));
		}

		@Test void b09_arrayWriterBuffersAndEmitsArray() throws Exception {
			var sb = new StringBuilder();
			try (var w = RecordAdapter.arrayWriter(serializerSession(), sb)) {
				assertFalse(w.isStreaming());
				w.write(1);
				w.write(2);
				w.flush();
			}
			assertEquals("[1,2]", sb.toString());
		}

		@Test void b10_arrayWriterRejectsWriteAfterClose() throws Exception {
			var sb = new StringBuilder();
			var w = RecordAdapter.arrayWriter(serializerSession(), sb);
			w.close();
			assertThrowsWithMessage(IllegalStateException.class, "Array stream is closed", () -> w.write(1));
			assertDoesNotThrow(w::close);
		}

		@Test void b11_arrayWriterFlushFlushesFlushableOutput() throws Exception {
			var sw = new StringWriter();
			try (var w = RecordAdapter.arrayWriter(serializerSession(), sw)) {
				w.write(1);
				assertDoesNotThrow(w::flush);
			}
			assertEquals("[1]", sw.toString());
		}

		@Test void b12_writerSingleShot() throws Exception {
			var sb = new StringBuilder();
			try (var w = RecordAdapter.writer(serializerSession(), sb)) {
				assertFalse(w.isStreaming());
				w.write(Map.of("a", 1));
				assertThrowsWithMessage(IllegalStateException.class, "already accepted its single root value", () -> w.write(2));
			}
			assertEquals("{\"a\":1}", sb.toString());
		}

		@Test void b13_writerFlushAndCloseCloseableOutput() throws Exception {
			var sw = new StringWriter();
			try (var w = RecordAdapter.writer(serializerSession(), sw)) {
				w.write(7);
				assertDoesNotThrow(w::flush);
			}
			assertEquals("7", sw.toString());
		}

		@Test void b14_writerFlushNonFlushableOutputIsNoop() throws Exception {
			// StringBuilder is not Flushable — flush() must be a safe no-op.
			try (var w = RecordAdapter.writer(serializerSession(), new StringBuilder())) {
				assertDoesNotThrow(w::flush);
			}
		}

		@Test void b15_writerWrapsSerializeExceptionAsIOException() throws Exception {
			try (var w = RecordAdapter.writer(serializerSession(), new StringBuilder())) {
				assertThrows(IOException.class, () -> w.write(new BadBean()));
			}
		}

		@Test void b16_arrayWriterWrapsSerializeExceptionAsIOException() throws Exception {
			var w = RecordAdapter.arrayWriter(serializerSession(), new StringBuilder());
			w.write(new BadBean());
			// The buffered serialize happens on close(), wrapping the SerializeException as IOException.
			assertThrows(IOException.class, w::close);
		}

		/** A bean whose getter throws, forcing the serializer to raise a SerializeException. */
		public static class BadBean {
			public int getX() {
				throw new RuntimeException("boom-serialize");
			}
		}
	}

	// =================================================================================
	// C. StreamingArrayRecord — O(1) array-element wrappers
	// =================================================================================

	@Nested class C_streamingArrayRecord extends TestBase {

		@Test void c01_readerNonArrayThrows() throws Exception {
			// Cursor positioned at a scalar (not START_ARRAY) must be rejected.
			try (var cursor = JsonParser.DEFAULT.parseTokens("42")) {
				assertThrowsWithMessage(ParseException.class, "Expected START_ARRAY", () -> StreamingArrayRecord.reader(cursor));
			}
		}

		@Test void c02_readerStreamsElementsAndCloses() throws Exception {
			var cm = MarshallingContext.DEFAULT.getClassMeta(Integer.class);
			try (var r = StreamingArrayRecord.reader(JsonParser.DEFAULT.parseTokens("[1,2,3]"))) {
				assertTrue(r.isStreaming());
				assertEquals(1, r.read(Integer.class));
				assertEquals(2, r.read(cm));
				assertEquals(3, r.<Integer>read((Type) Integer.class));
				assertFalse(r.canRead());
			}
		}

		@Test void c03_writerStreamsElements() throws Exception {
			var sb = new StringBuilder();
			try (var w = StreamingArrayRecord.writer(JsonSerializer.DEFAULT.serializeTokens(sb))) {
				assertTrue(w.isStreaming());
				w.write(1);
				w.write(2);
				assertDoesNotThrow(w::flush);
			}
			assertEquals("[1,2]", sb.toString());
		}

		@Test void c04_writerRejectsWriteAfterCloseAndIsIdempotent() throws Exception {
			var sb = new StringBuilder();
			var w = StreamingArrayRecord.writer(JsonSerializer.DEFAULT.serializeTokens(sb));
			w.write(1);
			w.close();
			assertThrowsWithMessage(IllegalStateException.class, "Array stream is closed", () -> w.write(2));
			assertDoesNotThrow(w::close);
			assertEquals("[1]", sb.toString());
		}
	}

	// =================================================================================
	// D. Role-interface default methods (functional-interface lambdas use the defaults)
	// =================================================================================

	@Nested class D_roleDefaults extends TestBase {

		@Test void d01_recordReadableIsStreamingDefault() {
			RecordReadable rr = input -> null;
			assertTrue(rr.isRecordStreaming());
		}

		@Test void d02_recordWritableIsStreamingDefault() {
			RecordWritable rw = output -> null;
			assertTrue(rw.isRecordStreaming());
		}

		@Test void d03_arrayRecordReadableRootNameDefaultDelegates() throws Exception {
			var marker = RecordAdapter.reader(JsonParser.DEFAULT.getSession(), "1");
			ArrayRecordReadable ar = input -> marker;
			// The 2-arg (rootElementName) default ignores the name and delegates to the 1-arg form.
			assertSame(marker, ar.parseArrayRecords("x", "item"));
			assertTrue(ar.isArrayRecordStreaming());
			marker.close();
		}

		@Test void d04_arrayRecordWritableCountDefaultDelegates() throws Exception {
			var marker = RecordAdapter.writer(JsonSerializer.DEFAULT.getSession(), new StringBuilder());
			ArrayRecordWritable aw = output -> marker;
			// The count-prefixed default ignores the count and delegates to the 1-arg form.
			assertSame(marker, aw.serializeArrayRecords(new StringBuilder(), 3));
			assertTrue(aw.isArrayRecordStreaming());
			marker.close();
		}
	}

	// =================================================================================
	// E. RecordReader default iterator/stream views + RuntimeParseException wrapping
	// =================================================================================

	@Nested class E_recordReaderViews extends TestBase {

		/** A reader whose canRead()/read() throw checked exceptions, to exercise the wrapping. */
		static final class ThrowingReader implements RecordReader {
			private final boolean failOnCanRead;
			ThrowingReader(boolean failOnCanRead) { this.failOnCanRead = failOnCanRead; }
			@Override public boolean canRead() throws IOException {
				if (failOnCanRead)
					throw new IOException("boom-canRead");
				return true;
			}
			@Override public <T> T read(Class<T> type) throws ParseException {
				throw new ParseException("boom-read");
			}
			@Override public <T> T read(ClassMeta<T> type) throws ParseException {
				throw new ParseException("boom-read");
			}
			@Override public <T> T read(Type type, Type... args) throws ParseException {
				throw new ParseException("boom-read");
			}
			@Override public boolean isStreaming() { return true; }
			@Override public void close() {
				// No-op: this stub holds no underlying resource to release.
			}
		}

		@Test void e01_streamHappyPath() throws Exception {
			try (var r = StreamingArrayRecord.reader(JsonParser.DEFAULT.parseTokens("[1,2,3]"))) {
				assertEquals(3, r.stream(Integer.class).count());
			}
		}

		@Test void e02_iteratorHappyPath() throws Exception {
			try (var r = StreamingArrayRecord.reader(JsonParser.DEFAULT.parseTokens("[1,2]"))) {
				var it = r.iterator(Integer.class);
				var seen = new ArrayList<Integer>();
				while (it.hasNext())
					seen.add(it.next());
				assertList(seen, "1", "2");
			}
		}

		@Test void e03_iteratorHasNextWrapsCheckedException() {
			var it = new ThrowingReader(true).iterator(Integer.class);
			var e = assertThrows(RuntimeParseException.class, it::hasNext);
			assertInstanceOf(IOException.class, e.getCause());
		}

		@Test void e04_iteratorNextWrapsCheckedException() {
			var it = new ThrowingReader(false).iterator(Integer.class);
			var e = assertThrows(RuntimeParseException.class, it::next);
			assertInstanceOf(ParseException.class, e.getCause());
		}
	}

	// =================================================================================
	// F. TokenReader / TokenWriter default methods (bare implementations use the defaults)
	// =================================================================================

	@Nested class F_tokenDefaults extends TestBase {

		/** Minimal TokenReader stub that overrides only the abstract methods, leaving the defaults. */
		static final class StubTokenReader implements TokenReader {
			@Override public TokenType next() { return TokenType.END_OF_STREAM; }
			@Override public TokenType getCurrentToken() { return TokenType.NOT_AVAILABLE; }
			@Override public int getDepth() { return 0; }
			@Override public String getFieldName() { return null; }
			@Override public String getString() { return null; }
			@Override public Number getNumber() { return null; }
			@Override public String getNumberLexeme() { return null; }
			@Override public boolean getBool() { return false; }
			@Override public byte[] getBinary() { return null; }
			@Override public void skipChildren() {
				// No-op: the stub never sits on a structural token, so there are no children to skip.
			}
			@Override public void close() {
				// No-op: this stub holds no underlying resource to release.
			}
		}

		/** Minimal TokenWriter stub that overrides only the abstract methods, leaving object()/write() defaults. */
		static final class StubTokenWriter implements TokenWriter {
			@Override public TokenWriter startObject() { return this; }
			@Override public TokenWriter endObject() { return this; }
			@Override public TokenWriter startArray() { return this; }
			@Override public TokenWriter endArray() { return this; }
			@Override public TokenWriter fieldName(String name) { return this; }
			@Override public TokenWriter string(String value) { return this; }
			@Override public TokenWriter number(Number value) { return this; }
			@Override public TokenWriter number(long value) { return this; }
			@Override public TokenWriter number(double value) { return this; }
			@Override public TokenWriter number(BigDecimal value) { return this; }
			@Override public TokenWriter number(BigInteger value) { return this; }
			@Override public TokenWriter bool(boolean value) { return this; }
			@Override public TokenWriter nil() { return this; }
			@Override public TokenWriter binary(byte[] value) { return this; }
			@Override public void flush() {
				// No-op: this stub buffers nothing, so there is nothing to flush.
			}
			@Override public void close() {
				// No-op: this stub holds no underlying resource to release.
			}
		}

		@Test void f01_tokenReaderCanReadDefaultIsFalse() throws Exception {
			try (var r = new StubTokenReader()) {
				assertFalse(r.canRead());
				assertTrue(r.isStreaming());
			}
		}

		@Test void f02_tokenReaderReadDefaultsThrow() {
			var cm = MarshallingContext.DEFAULT.getClassMeta(Integer.class);
			try (var r = new StubTokenReader()) {
				assertThrows(UnsupportedOperationException.class, () -> r.read(Integer.class));
				assertThrows(UnsupportedOperationException.class, () -> r.read(cm));
				assertThrows(UnsupportedOperationException.class, () -> r.read((Type) Integer.class));
			}
		}

		@Test void f03_tokenWriterObjectDefaultThrows() {
			try (var w = new StubTokenWriter()) {
				assertThrows(UnsupportedOperationException.class, () -> w.object(42));
				// write(Object) default delegates to object(Object).
				assertThrows(UnsupportedOperationException.class, () -> w.write(42));
				assertTrue(w.isStreaming());
			}
		}
	}
}
