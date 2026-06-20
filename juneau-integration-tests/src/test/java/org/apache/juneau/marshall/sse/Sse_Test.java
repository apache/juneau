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
package org.apache.juneau.marshall.sse;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.junit.jupiter.api.*;

/**
 * Comprehensive tests for the {@code org.apache.juneau.marshall.sse} package and the {@link Sse} marshaller helper.
 *
 * <p>
 * Covers WHATWG spec compliance, round-trips, edge cases, iterator semantics, and a REST smoke test.
 */
@SuppressWarnings({
	"java:S5961", // High assertion count acceptable in comprehensive test
	"java:S5976" // Similar-but-distinct WHATWG spec scenarios read clearer as individually-named tests than a parameterized test.
})
class Sse_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// SseEvent bean.
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_emptyConstructor() {
		var e = new SseEvent();
		assertBean(e, "event,data,id,retry", "<null>,<null>,<null>,<null>");
	}

	@Test void a02_twoArgConstructor() {
		var e = new SseEvent("progress", "step 1");
		assertBean(e, "event,data,id,retry", "progress,step 1,<null>,<null>");
	}

	@Test void a03_setters() {
		var e = new SseEvent()
			.setEvent("update")
			.setData("payload")
			.setId("42")
			.setRetry(5000L);
		assertBean(e, "event,data,id,retry", "update,payload,42,5000");
	}

	@Test void a04_equalsAndHashCode() {
		var e1 = new SseEvent("e", "d").setId("1").setRetry(100L);
		var e2 = new SseEvent("e", "d").setId("1").setRetry(100L);
		var e3 = new SseEvent("e", "different").setId("1").setRetry(100L);
		assertEquals(e1, e1);
		assertEquals(e1, e2);
		assertEquals(e1.hashCode(), e2.hashCode());
		assertNotEquals(e1, e3);
		assertNotEquals("not an event", e1);
		assertNotEquals(null, e1);
	}

	@Test void a05_toString() {
		var e = new SseEvent("e1", "d1").setId("id1").setRetry(50L);
		var s = e.toString();
		assertTrue(s.contains("e1"));
		assertTrue(s.contains("d1"));
		assertTrue(s.contains("id1"));
		assertTrue(s.contains("50"));
	}

	@Test void a06_defaultEventConstant() {
		assertEquals("message", SseEvent.DEFAULT_EVENT);
	}

	@Test void a07_equalsSelfReferenceShortCircuit() {
		// Direct equals() invocation to exercise the this==o reflexive branch — JUnit assertEquals
		// routes through Objects.equals() which short-circuits on identity before calling equals().
		var e = new SseEvent("e", "d").setId("1").setRetry(100L);
		assertTrue(e.equals(e));  // NOSONAR java:S5863 — intentional reflexive equals call for branch coverage
	}

	@Test void a08_equalsDifferentEvent() {
		var e1 = new SseEvent("a", "d").setId("1").setRetry(100L);
		var e2 = new SseEvent("b", "d").setId("1").setRetry(100L);
		assertNotEquals(e1, e2);
	}

	@Test void a09_equalsDifferentId() {
		var e1 = new SseEvent("e", "d").setId("1").setRetry(100L);
		var e2 = new SseEvent("e", "d").setId("2").setRetry(100L);
		assertNotEquals(e1, e2);
	}

	@Test void a10_equalsDifferentRetry() {
		var e1 = new SseEvent("e", "d").setId("1").setRetry(100L);
		var e2 = new SseEvent("e", "d").setId("1").setRetry(200L);
		assertNotEquals(e1, e2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Serializer wire format.
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_singleEvent() throws Exception {
		var e = new SseEvent("progress", "step 1").setId("1").setRetry(3000L);
		var s = Sse.DEFAULT.of(e);
		assertEquals("event: progress\ndata: step 1\nid: 1\nretry: 3000\n\n", s);
	}

	@Test void b02_listOfEvents() throws Exception {
		var events = List.of(
			new SseEvent("progress", "one"),
			new SseEvent("progress", "two")
		);
		var s = Sse.DEFAULT.of(events);
		assertEquals("event: progress\ndata: one\n\nevent: progress\ndata: two\n\n", s);
	}

	@Test void b03_arrayOfEvents() throws Exception {
		var arr = new SseEvent[]{ new SseEvent("e", "a"), new SseEvent("e", "b") };
		var s = Sse.DEFAULT.of(arr);
		assertEquals("event: e\ndata: a\n\nevent: e\ndata: b\n\n", s);
	}

	@Test void b04_objectArrayOfEvents() throws Exception {
		var arr = new Object[]{ new SseEvent("e", "a"), new SseEvent("e", "b") };
		var s = Sse.DEFAULT.of(arr);
		assertEquals("event: e\ndata: a\n\nevent: e\ndata: b\n\n", s);
	}

	@Test void b05_streamOfEvents() throws Exception {
		var events = Stream.of(new SseEvent("e", "a"), new SseEvent("e", "b"));
		var s = Sse.DEFAULT.of(events);
		assertEquals("event: e\ndata: a\n\nevent: e\ndata: b\n\n", s);
	}

	@Test void b06_multiLineData() throws Exception {
		var e = new SseEvent("e", "line one\nline two\nline three");
		var s = Sse.DEFAULT.of(e);
		assertEquals("event: e\ndata: line one\ndata: line two\ndata: line three\n\n", s);
	}

	@Test void b07_eventNullOrEmptyOmitted() throws Exception {
		var e1 = new SseEvent().setData("d");
		assertEquals("data: d\n\n", Sse.DEFAULT.of(e1));
		var e2 = new SseEvent("", "d");
		assertEquals("data: d\n\n", Sse.DEFAULT.of(e2));
	}

	@Test void b08_nullDataOmitted() throws Exception {
		var e = new SseEvent("e", null);
		assertEquals("event: e\n\n", Sse.DEFAULT.of(e));
	}

	@Test void b09_emptyDataEmitsEmptyDataLine() throws Exception {
		var e = new SseEvent("e", "");
		assertEquals("event: e\ndata: \n\n", Sse.DEFAULT.of(e));
	}

	@Test void b10_idEmittedWhenSet() throws Exception {
		var e = new SseEvent().setData("d").setId("123");
		assertEquals("data: d\nid: 123\n\n", Sse.DEFAULT.of(e));
	}

	@Test void b11_retryEmittedWhenSet() throws Exception {
		var e = new SseEvent().setData("d").setRetry(2500L);
		assertEquals("data: d\nretry: 2500\n\n", Sse.DEFAULT.of(e));
	}

	@Test void b12_nullElementsInIterableSkipped() throws Exception {
		var events = new ArrayList<SseEvent>();
		events.add(new SseEvent("e", "a"));
		events.add(null);
		events.add(new SseEvent("e", "b"));
		var s = Sse.DEFAULT.of(events);
		assertEquals("event: e\ndata: a\n\nevent: e\ndata: b\n\n", s);
	}

	@Test void b13_emptyIterable() throws Exception {
		assertEquals("", Sse.DEFAULT.of(List.of()));
	}

	@Test void b14_emptyArray() throws Exception {
		assertEquals("", Sse.DEFAULT.of(new SseEvent[0]));
	}

	@Test void b15_nonEventThrows() {
		assertThrows(SerializeException.class, () -> Sse.DEFAULT.of("not an event"));
	}

	@Test void b16_iterableWithNonEventThrows() {
		var input = List.of("not", "events");
		assertThrows(SerializeException.class, () -> Sse.DEFAULT.of(input));
	}

	@Test void b17_nullInputReturnsEmpty() throws Exception {
		assertEquals("", Sse.DEFAULT.of(null));
	}

	@Test void b18_writeComment() throws Exception {
		var sw = new StringWriter();
		SseSerializer.writeComment(sw, "ping");
		assertEquals(": ping\n\n", sw.toString());
	}

	@Test void b19_writeCommentNullTreatedAsEmpty() throws Exception {
		var sw = new StringWriter();
		SseSerializer.writeComment(sw, null);
		assertEquals(": \n\n", sw.toString());
	}

	@Test void b20_writeCommentMultiline() throws Exception {
		var sw = new StringWriter();
		SseSerializer.writeComment(sw, "line one\nline two");
		assertEquals(": line one\n: line two\n\n", sw.toString());
	}

	@Test void b21_serializeToWriter() throws Exception {
		var sw = new StringWriter();
		Sse.DEFAULT.of(new SseEvent("e", "d"), sw);
		assertEquals("event: e\ndata: d\n\n", sw.toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Parser / SseEventReader compliance.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_canonicalExample() throws Exception {
		var wire = "event: progress\ndata: step 1\nid: 42\nretry: 1000\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data,id,retry", "progress,step 1,42,1000");
	}

	@Test void c02_multiLineData() throws Exception {
		var wire = "data: line one\ndata: line two\ndata: line three\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("line one\nline two\nline three", events.get(0).getData());
	}

	@Test void c03_crLineTerminator() throws Exception {
		var wire = "event: e\rdata: d\r\r";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data", "e,d");
	}

	@Test void c04_lfLineTerminator() throws Exception {
		var wire = "event: e\ndata: d\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data", "e,d");
	}

	@Test void c05_crLfLineTerminator() throws Exception {
		var wire = "event: e\r\ndata: d\r\n\r\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data", "e,d");
	}

	@Test void c06_mixedLineTerminators() throws Exception {
		var wire = "event: e\rdata: line1\ndata: line2\r\n\r\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("line1\nline2", events.get(0).getData());
	}

	@Test void c07_singleBomStripped() throws Exception {
		var wire = "\uFEFFevent: e\ndata: d\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("e", events.get(0).getEvent());
	}

	@Test void c08_secondBomPreserved() throws Exception {
		var wire = "\uFEFFdata: foo\uFEFF\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("foo\uFEFF", events.get(0).getData());
	}

	@Test void c09_idWithNullByteIgnored() throws Exception {
		var wire = "id: ab\u0000cd\ndata: x\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertNull(events.get(0).getId());
	}

	@Test void c10_retryNonDigitIgnored() throws Exception {
		var wire = "retry: abc\ndata: x\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertNull(events.get(0).getRetry());
	}

	@Test void c11_retryEmptyIgnored() throws Exception {
		var wire = "retry: \ndata: x\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertNull(events.get(0).getRetry());
	}

	@Test void c12_retryMixedDigitsIgnored() throws Exception {
		var wire = "retry: 12x4\ndata: x\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertNull(events.get(0).getRetry());
	}

	@Test void c13_commentLinesIgnored() throws Exception {
		var wire = ": ping\n: heartbeat\nevent: e\ndata: d\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data", "e,d");
	}

	@Test void c14_noColonLineSpec() throws Exception {
		var wire = "data\ndata: real\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("\nreal", events.get(0).getData());
	}

	@Test void c15_colonSpaceHandling() throws Exception {
		var wire = "data:  value\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals(" value", events.get(0).getData());
	}

	@Test void c16_noSpaceAfterColon() throws Exception {
		var wire = "data:value\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("value", events.get(0).getData());
	}

	@Test void c17_unknownFieldIgnored() throws Exception {
		var wire = "unknown: x\ndata: real\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("real", events.get(0).getData());
	}

	@Test void c18_multipleEvents() throws Exception {
		var wire = "data: one\n\ndata: two\n\ndata: three\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(3, events.size());
		assertEquals("one", events.get(0).getData());
		assertEquals("two", events.get(1).getData());
		assertEquals("three", events.get(2).getData());
	}

	@Test void c19_emptyEventValueDefaultsToMessage() throws Exception {
		var wire = "data: d\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertNull(events.get(0).getEvent());
	}

	@Test void c20_emptyEventField() throws Exception {
		var wire = "event: \ndata: d\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("", events.get(0).getEvent());
	}

	@Test void c21_emptyIdField() throws Exception {
		var wire = "id: \ndata: d\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("", events.get(0).getId());
	}

	@Test void c22_inputWithoutTrailingBlankLine() throws Exception {
		var wire = "event: e\ndata: d\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data", "e,d");
	}

	@Test void c23_pureBlankLinesProduceNoEvents() throws Exception {
		var wire = "\n\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertTrue(events.isEmpty());
	}

	@Test void c24_emptyInput() throws Exception {
		List<SseEvent> events = Sse.DEFAULT.to("", List.class, SseEvent.class);
		assertTrue(events.isEmpty());
	}

	@Test void c25_parseAsSingleEvent() throws Exception {
		var wire = "event: e\ndata: d\n\n";
		var e = Sse.DEFAULT.to(wire, SseEvent.class);
		assertNotNull(e);
		assertBean(e, "event,data", "e,d");
	}

	@Test void c26_parseAsSingleEventEmptyInput() throws Exception {
		var e = Sse.DEFAULT.to("", SseEvent.class);
		assertNull(e);
	}

	@Test void c27_parseAsArray() throws Exception {
		var wire = "data: one\n\ndata: two\n\n";
		var events = Sse.DEFAULT.to(wire, SseEvent[].class);
		assertEquals(2, events.length);
		assertEquals("one", events[0].getData());
		assertEquals("two", events[1].getData());
	}

	@Test void c28_parseUnsupportedTargetTypeThrows() {
		var wire = "data: x\n\n";
		assertThrows(ParseException.class, () -> Sse.DEFAULT.to(wire, Integer.class));
	}

	@Test void c29_emptyDataFieldEmitsEmptyString() throws Exception {
		// "data:" with no value — exercises processField's fieldValue.isEmpty() branch.
		var wire = "data:\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertEquals("", events.get(0).getData());
	}

	@Test void c30_retryLowAsciiNonDigitIgnored() throws Exception {
		// '!' (0x21) is below '0' — exercises parseRetry's c < '0' branch.
		var wire = "retry: !23\ndata: x\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertNull(events.get(0).getRetry());
	}

	@Test void c31_eventWithoutDataFieldDispatches() throws Exception {
		// Event with metadata fields but no data — exercises dispatch's dataBuf.length() == 0 branch.
		var wire = "event: e\nid: 1\n\n";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data,id,retry", "e,<null>,1,<null>");
	}

	@Test void c32_inputEndsMidLineWithoutTerminator() throws Exception {
		// Stream ends without a final \n — exercises readLine's sawAny=true at EOF branch.
		var wire = "event: e\ndata: d";
		List<SseEvent> events = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(1, events.size());
		assertBean(events.get(0), "event,data", "e,d");
	}

	//------------------------------------------------------------------------------------------------------------------
	// SseEventReader iterator semantics.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_iteratorDrainsOneAtATime() throws Exception {
		var wire = "data: a\n\ndata: b\n\ndata: c\n\n";
		try (var r = new SseEventReader(new StringReader(wire))) {
			assertTrue(r.hasNext());
			assertEquals("a", r.next().getData());
			assertTrue(r.hasNext());
			assertEquals("b", r.next().getData());
			assertTrue(r.hasNext());
			assertEquals("c", r.next().getData());
			assertFalse(r.hasNext());
		}
	}

	@Test void d02_nextWithoutHasNext() throws Exception {
		var wire = "data: a\n\n";
		try (var r = new SseEventReader(new StringReader(wire))) {
			assertEquals("a", r.next().getData());
		}
	}

	@Test void d03_hasNextIdempotent() throws Exception {
		var wire = "data: a\n\n";
		try (var r = new SseEventReader(new StringReader(wire))) {
			assertTrue(r.hasNext());
			assertTrue(r.hasNext());
			assertTrue(r.hasNext());
			assertEquals("a", r.next().getData());
			assertFalse(r.hasNext());
			assertFalse(r.hasNext());
		}
	}

	@Test void d04_nextThrowsNoSuchElement() throws Exception {
		try (var r = new SseEventReader(new StringReader(""))) {
			assertThrows(NoSuchElementException.class, r::next);
		}
	}

	@Test void d05_nullReaderRejected() {
		assertThrows(IllegalArgumentException.class, () -> new SseEventReader(null));
	}

	@Test void d06_closeClosesUnderlyingReader() throws Exception {
		var closed = new boolean[1];
		var sr = new StringReader("data: a\n\n") {
			@Override public void close() { closed[0] = true; super.close(); }
		};
		var r = new SseEventReader(sr);
		r.close();
		assertTrue(closed[0]);
	}

	@Test void d07_toListClosesUnderlyingReader() throws Exception {
		var closed = new boolean[1];
		var sr = new StringReader("data: a\n\ndata: b\n\n") {
			@Override public void close() { closed[0] = true; super.close(); }
		};
		var r = new SseEventReader(sr);
		var events = r.toList();
		assertEquals(2, events.size());
		assertTrue(closed[0]);
	}

	@Test void d08_ioExceptionWrapped() {
		var failingReader = new Reader() {
			@Override public int read(char[] cbuf, int off, int len) throws IOException { throw new IOException("boom"); }
			@Override public void close() { /* intentionally empty */ }
		};
		var r = new SseEventReader(failingReader);
		var ex = assertThrows(UncheckedIOException.class, r::hasNext);
		assertEquals("boom", ex.getCause().getMessage());
	}

	@Test void d09_toListPropagatesIo() {
		var failingReader = new Reader() {
			@Override public int read(char[] cbuf, int off, int len) throws IOException { throw new IOException("boom"); }
			@Override public void close() { /* intentionally empty */ }
		};
		var r = new SseEventReader(failingReader);
		var ex = assertThrows(IOException.class, r::toList);
		assertEquals("boom", ex.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Round-trip via Sse marshaller.
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_roundTripList() throws Exception {
		var events = List.of(
			new SseEvent("progress", "step 1").setId("1"),
			new SseEvent("progress", "step 2").setId("2").setRetry(1000L)
		);
		var wire = Sse.DEFAULT.of(events);
		List<SseEvent> parsed = Sse.DEFAULT.to(wire, List.class, SseEvent.class);
		assertEquals(2, parsed.size());
		assertBean(parsed.get(0), "event,data,id,retry", "progress,step 1,1,<null>");
		assertBean(parsed.get(1), "event,data,id,retry", "progress,step 2,2,1000");
	}

	@Test void e02_roundTripMultiLine() throws Exception {
		var e = new SseEvent("e", "line a\nline b");
		var wire = Sse.DEFAULT.of(e);
		var parsed = Sse.DEFAULT.to(wire, SseEvent.class);
		assertEquals("line a\nline b", parsed.getData());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Defaults / context.
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_serializerMediaType() {
		assertEquals("text/event-stream", SseSerializer.DEFAULT.getResponseContentType().toString());
	}

	@Test void f02_serializerCopy() {
		var copy = SseSerializer.DEFAULT.copy().build();
		assertNotNull(copy);
	}

	@Test void f03_parserCopy() {
		var copy = SseParser.DEFAULT.copy().build();
		assertNotNull(copy);
	}

	@Test void f04_serializerGetSession() {
		assertNotNull(SseSerializer.DEFAULT.getSession());
	}

	@Test void f05_parserGetSession() {
		assertNotNull(SseParser.DEFAULT.getSession());
	}

	@Test void f06_marshallerInstanceMethods() throws Exception {
		var m = Sse.DEFAULT;
		var s = m.of(new SseEvent("e", "d"));
		assertEquals("event: e\ndata: d\n\n", s);
	}

	@Test void f07_explicitConstructor() throws Exception {
		var m = new Sse(SseSerializer.DEFAULT, SseParser.DEFAULT);
		assertNotNull(m);
		assertEquals("event: e\ndata: d\n\n", m.of(new SseEvent("e", "d")));
	}

	@Test void f08_builderHashKeyStable() {
		var b1 = SseSerializer.create();
		var b2 = SseSerializer.create();
		assertEquals(b1.hashKey(), b2.hashKey());
		var pb1 = SseParser.create();
		var pb2 = SseParser.create();
		assertEquals(pb1.hashKey(), pb2.hashKey());
	}

	@Test void f09_splitDataKeepsTrailingEmpties() {
		var parts = SseSerializerSession.splitData("a\nb\n");
		assertEquals(3, parts.size());
		assertEquals("a", parts.get(0));
		assertEquals("b", parts.get(1));
		assertEquals("", parts.get(2));
	}

	@Test void f10_builderCopyConstructorParser() {
		var b1 = SseParser.create();
		var b2 = b1.copy();
		assertNotNull(b2);
		assertNotSame(b1, b2);
	}

	@Test void f11_builderCopyConstructorSerializer() {
		var b1 = SseSerializer.create();
		var b2 = b1.copy();
		assertNotNull(b2);
		assertNotSame(b1, b2);
	}

	@Test void f12_arrayWithNullElement() throws Exception {
		var arr = new SseEvent[]{ new SseEvent("e", "a"), null, new SseEvent("e", "b") };
		// SseEvent[] direct array path: writeEvent(null) returns early.
		var s = Sse.DEFAULT.of(arr);
		assertEquals("event: e\ndata: a\n\nevent: e\ndata: b\n\n", s);
	}

	@Test void f13_streamWithNonEventWrapsInRuntime() {
		// Stream path: writeIfEventUnchecked wraps SerializeException as RuntimeException.
		var s = Stream.of("not an event");
		assertThrows(RuntimeException.class, () -> Sse.DEFAULT.of(s));
	}

	@Test void f14_parseAsObjectClass() throws Exception {
		// Target type Object.class hits the type.isObject() branch.
		var wire = "data: x\n\n";
		var out = Sse.DEFAULT.to(wire, Object.class);
		assertNotNull(out);
		assertTrue(out instanceof List);
	}

	@Test void f15_parseAsRawList() throws Exception {
		// Raw List.class without args hits the "List.class.isAssignableFrom" fallback.
		var wire = "data: x\n\n";
		var out = Sse.DEFAULT.to(wire, (Class<?>) List.class);
		assertNotNull(out);
		assertTrue(out instanceof List);
	}

	@Test void f16_parseAsSseEventEmptyReturnsNull() throws Exception {
		// Empty input + target SseEvent.class hits the events.isEmpty() branch.
		var e = Sse.DEFAULT.to("\n\n", SseEvent.class);
		assertNull(e);
	}

	@Test void f17_parseNullInputReturnsNullViaPublicApi() throws Exception {
		// Null input -> ParserPipe.getReader() returns null -> doParse hits the r==null early-return branch.
		var result = Sse.DEFAULT.to((Object) null, SseEvent.class);
		assertNull(result);
	}

	@Test void f18_doParseWithNullClassMetaReturnsList() throws Exception {
		// Public parse() path requires non-null type (parseInner calls type.isVoid()), so the
		// type==null branch in SseParserSession.doParse() is only reachable via direct invocation.
		// Same package -> protected doParse is accessible.
		var session = SseParser.DEFAULT.getSession();
		try (var pipe = session.createPipe("data: x\n\n")) {
			Object result = session.doParse(pipe, null);
			assertTrue(result instanceof List);
			var events = (List<?>) result;
			assertEquals(1, events.size());
			assertEquals("x", ((SseEvent) events.get(0)).getData());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// REST smoke test.
	//------------------------------------------------------------------------------------------------------------------

	@Rest(serializers = SseSerializer.class)
	public static class SseEndpoint {
		@RestGet(path = "/stream")
		public List<SseEvent> stream() {
			return List.of(
				new SseEvent("progress", "step 1").setId("1"),
				new SseEvent("progress", "step 2").setId("2"),
				new SseEvent("progress", "step 3").setId("3")
			);
		}
	}

	@Test void g01_restEndpointEmitsAllEventsInOrder() throws Exception {
		var c = MockRestClient.buildLax(SseEndpoint.class);
		var body = c.get("/stream").header("Accept", "text/event-stream").run().getContent().asString();
		var expected = """
			event: progress
			data: step 1
			id: 1

			event: progress
			data: step 2
			id: 2

			event: progress
			data: step 3
			id: 3

			""";
		assertEquals(expected, body);
	}
}
