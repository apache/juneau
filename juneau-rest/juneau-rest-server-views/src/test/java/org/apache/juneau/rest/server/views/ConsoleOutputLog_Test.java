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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.junit.jupiter.api.*;

class ConsoleOutputLog_Test extends TestBase {

	/** A clock the test moves by hand. */
	static final class TestClock extends Clock {
		Instant now = Instant.parse("2026-10-07T13:00:00Z");
		@Override public ZoneId getZone() { return ZoneOffset.UTC; }
		@Override public Clock withZone(ZoneId zone) { return this; }
		@Override public Instant instant() { return now; }
	}

	private static ConsoleOutputLog log(int lines) {
		var log = new ConsoleOutputLog();
		for (var i = 1; i <= lines; i++)
			log.append(ConsoleOutputLine.info("line " + i));
		return log;
	}

	private static List<Long> ns(ConsoleOutputPage p) {
		return p.lines.stream().map(l -> l.n).collect(Collectors.toList());
	}

	private static List<String> texts(ConsoleOutputPage p) {
		return p.lines.stream().map(l -> l.text).collect(Collectors.toList());
	}

	private static boolean noneOpen(ConsoleOutputPage p) {
		return p.lines.stream().allMatch(l -> l.open == null);
	}

	/** Renders the log as {@code n:text} per line, with {@code *} on the open line, joined by {@code |}. */
	private static String show(ConsoleOutputLog log) {
		return log.page(null, 1000).validate().lines.stream()
			.map(l -> l.n + ":" + (l.text != null ? l.text : l.frags.stream().map(f -> f.text).collect(Collectors.joining()))
				+ (l.open != null ? "*" : ""))
			.collect(Collectors.joining("|"));
	}

	/** Returns one scripted chunk per read; before read i (i >= 1) it runs a hook and records the log. */
	private static final class ChunkReader extends Reader {
		private final ConsoleOutputLog log;
		private final Deque<String> chunks;
		private final IntConsumer before;
		final List<String> seen = new ArrayList<>();
		private int reads;

		ChunkReader(ConsoleOutputLog log, IntConsumer before, String...chunks) {
			this.log = log;
			this.before = before;
			this.chunks = new ArrayDeque<>(Arrays.asList(chunks));
		}

		@Override public int read(char[] b, int off, int len) {
			if (reads > 0) {
				before.accept(reads);
				seen.add(show(log));
			}
			reads++;
			var c = chunks.poll();
			if (c == null)
				return -1;
			c.getChars(0, c.length(), b, off);
			return c.length();
		}

		@Override public void close() { /* Nothing to close. */ }
	}

	private static ChunkReader chunks(ConsoleOutputLog log, String...chunks) {
		return new ChunkReader(log, i -> {}, chunks);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a - append
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_appendAssignsN() {
		var log = new ConsoleOutputLog();
		assertEquals(1, log.append(ConsoleOutputLine.info("a")));
		assertEquals(2, log.append(ConsoleOutputLine.info("b")));
		assertEquals(2, log.size());
	}

	@Test void a02_appendDoesNotMutateCaller() {
		var log = new ConsoleOutputLog();
		var line = ConsoleOutputLine.info("same").icon("check");
		assertEquals(1, log.append(line));
		assertEquals(2, log.append(line));
		assertNull(line.n);
		var p = log.page(null, 10);
		assertList(ns(p), 1L, 2L);
		assertNotSame(line, p.lines.get(0));
		line.ui.icon = "cancel";
		assertEquals("check", log.page(null, 10).lines.get(0).ui.icon);
	}

	@Test void a03_appendIgnoresCallerN() {
		var log = new ConsoleOutputLog();
		log.append(ConsoleOutputLine.info("x").n(99));
		assertList(ns(log.page(null, 10)), 1L);
	}

	@Test void a04_appendValidates() {
		var log = new ConsoleOutputLog();
		assertThrowsWithMessage(IllegalArgumentException.class, "ui.color", () -> log.append(ConsoleOutputLine.info("x").color("red")));
		assertEquals(0, log.size());
		assertThrows(NullPointerException.class, () -> log.append(null));
	}

	@Test void a05_cap() {
		var log = new ConsoleOutputLog(3);
		for (var i = 0; i < 3; i++)
			assertTrue(log.append(ConsoleOutputLine.info("x")) > 0);
		assertEquals(-1, log.append(ConsoleOutputLine.info("refused")));
		assertEquals(-1, log.append(ConsoleOutputLine.info("refused")));
		var p = log.page(null, 10);
		assertEquals(4, p.lines.size());
		assertBean(p.lines.get(3), "n,level,text", "4,SEVERE,… output truncated");
		assertThrowsWithMessage(IllegalArgumentException.class, "maxLines must be >= 1", () -> new ConsoleOutputLog(0));
	}

	@Test void a06_appendAnsiCarriesState() {
		var log = new ConsoleOutputLog();
		log.appendAnsi(Level.INFO, "\u001b[31mfirst");
		log.appendAnsi(Level.WARNING, "second");
		var p = log.page(null, 10);
		assertEquals(Style.ERROR, p.lines.get(1).frags.get(0).style);
		assertEquals(Level.WARNING, p.lines.get(1).level);
	}

	@Test void a07_appendRawReader() throws Exception {
		var log = new ConsoleOutputLog();
		var count = log.appendRaw(Level.INFO, new StringReader("a\r\nb\n\u001b[32mc"));
		assertEquals(3, count);
		var p = log.page(null, 10);
		assertBean(p.lines.get(0), "text", "a");
		assertBean(p.lines.get(1), "text", "b");
		assertEquals(Style.SUCCESS, p.lines.get(2).frags.get(0).style);
		assertThrows(NullPointerException.class, () -> log.appendRaw(Level.INFO, (Reader)null));
	}

	@Test void a08_appendRawStream() throws Exception {
		var log = new ConsoleOutputLog();
		var count = log.appendRaw(Level.INFO, new ByteArrayInputStream("é\nz\n".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
		assertEquals(2, count);
		assertBean(log.page(null, 10).lines.get(0), "text", "é");
	}

	//------------------------------------------------------------------------------------------------------------------
	// b - paging
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_emptyLogFromStart() {
		var p = new ConsoleOutputLog().page(null, 10).validate();
		assertBean(p, "next,more,hasEarlier,terminal,state", "0,false,false,false,PENDING");
		assertEquals(0, p.lines.size());
	}

	@Test void b02_forwardPaging() {
		var log = log(5);
		var p1 = log.page(null, 2).validate();
		assertList(ns(p1), 1L, 2L);
		assertBean(p1, "next,more,hasEarlier", "2,true,false");
		var p2 = log.page(p1.next, 2).validate();
		assertList(ns(p2), 3L, 4L);
		assertBean(p2, "next,more,hasEarlier,before", "4,true,true,2");
		var p3 = log.page(p2.next, 2).validate();
		assertList(ns(p3), 5L);
		assertBean(p3, "next,more", "5,false");
		var p4 = log.page(p3.next, 2).validate();
		assertEquals(0, p4.lines.size());
		assertBean(p4, "next,more", "5,false");
	}

	@Test void b03_tail() {
		var log = log(10);
		var p = log.tail(3).validate();
		assertList(ns(p), 8L, 9L, 10L);
		assertBean(p, "next,more,hasEarlier,before", "10,false,true,7");
		var all = log.tail(50).validate();
		assertEquals(10, all.lines.size());
		assertBean(all, "hasEarlier", "false");
		assertNull(all.before);
	}

	@Test void b04_before() {
		var log = log(10);
		var p = log.before("7", 4).validate();
		assertEquals(ConsoleOutputPage.Kind.EARLIER, p.kind);
		assertList(ns(p), 4L, 5L, 6L, 7L);
		assertBean(p, "more,hasEarlier,before", "false,true,3");
		assertNull(p.next);
		var first = log.before("3", 10).validate();
		assertList(ns(first), 1L, 2L, 3L);
		assertBean(first, "hasEarlier", "false");
	}

	@Test void b05_tailBeforeContiguity() {
		var log = log(123);
		var collected = new ArrayList<Long>();
		var t = log.tail(10);
		collected.addAll(0, ns(t));
		var token = t.before;
		while (token != null) {
			var b = log.before(token, 17);
			collected.addAll(0, ns(b));
			token = b.before;
		}
		assertEquals(LongStream.rangeClosed(1, 123).boxed().collect(Collectors.toList()), collected);
	}

	@Test void b06_unknownTokens() {
		var log = log(3);
		for (var bad : List.of("abc", "-1", "4", "01", "1.5", ""))
			assertThrows(ConsoleOutputSource.UnknownTokenException.class, () -> log.page(bad, 10), bad);
		assertThrows(ConsoleOutputSource.UnknownTokenException.class, () -> log.before("9", 10));
		assertDoesNotThrow(() -> log.page("3", 10));
	}

	@Test void b07_charBudget() {
		var log = new ConsoleOutputLog();
		var big = "x".repeat(65_536);
		for (var i = 0; i < 200; i++)
			log.append(ConsoleOutputLine.info(big));
		var perPage = ConsoleOutputPage.MAX_PAGE_CHARS / 65_536;
		var fwd = log.page(null, 10_000).validate();
		assertEquals(perPage, fwd.lines.size());
		assertTrue(fwd.more);
		var tail = log.tail(200).validate();
		assertEquals(perPage, tail.lines.size());
		assertEquals(200L, tail.lines.get(perPage - 1).n);
		assertTrue(tail.hasEarlier);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c - state
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_pendingToRunningWithZeroLines() {
		var clock = new TestClock();
		var log = new ConsoleOutputLog(100, clock);
		var p0 = log.page(null, 10).validate();
		assertBean(p0, "state,terminal,next", "PENDING,false,0");
		assertNull(p0.startedAt);
		log.start();
		var p1 = log.page(null, 10).validate();
		assertBean(p1, "state,stateStyle,terminal,next", "RUNNING,ACCENT,false,0");
		assertEquals(clock.now, p1.startedAt);
		assertEquals(0, p1.lines.size());
	}

	@Test void c02_completeSetsDuration() {
		var clock = new TestClock();
		var log = new ConsoleOutputLog(100, clock);
		log.start();
		log.append(ConsoleOutputLine.info("x"));
		clock.now = clock.now.plusSeconds(83);
		log.complete("DONE", Style.SUCCESS);
		var p = log.page(null, 10).validate();
		assertBean(p, "state,stateStyle,terminal,durationMs,more", "DONE,SUCCESS,true,83000,false");
		log.complete("FAILED", Style.ERROR);
		assertBean(log.page(null, 10), "state", "DONE");
	}

	@Test void c03_terminalOnlyOnLastPage() {
		var log = log(5);
		log.complete("DONE", null);
		var p1 = log.page(null, 2).validate();
		assertBean(p1, "terminal,more", "false,true");
		assertNull(p1.durationMs);
		var p3 = log.page("4", 2).validate();
		assertBean(p3, "terminal,more", "true,false");
	}

	@Test void c04_completeWithoutStart() {
		var log = new ConsoleOutputLog();
		log.complete("CANCELLED", Style.MUTED);
		var p = log.page(null, 10).validate();
		assertBean(p, "state,terminal", "CANCELLED,true");
		assertNull(p.durationMs);
	}

	@Test void c05_setState() {
		var log = new ConsoleOutputLog();
		log.setState("QUEUED", Style.MUTED);
		assertBean(log.page(null, 1), "state,stateStyle", "QUEUED,MUTED");
	}

	//------------------------------------------------------------------------------------------------------------------
	// d - stream and concurrency
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_streamIsASnapshotOfCopies() {
		var log = log(3);
		try (var s = log.stream()) {
			var l = s.collect(Collectors.toList());
			assertEquals(3, l.size());
			l.get(0).text = "changed";
		}
		assertBean(log.page(null, 1).lines.get(0), "text", "line 1");
	}

	@Test void d02_concurrentAppendAndRead() throws Exception {
		var log = new ConsoleOutputLog();
		var pool = Executors.newFixedThreadPool(5);
		try {
			var writer = pool.submit(() -> { for (var i = 0; i < 20_000; i++) log.append(ConsoleOutputLine.info("l" + i)); });
			var readers = new ArrayList<Future<?>>();
			for (var r = 0; r < 4; r++)
				readers.add(pool.submit(() -> {
					String token = null;
					long last = 0;
					while (last < 20_000) {
						var p = log.page(token, 500).validate();
						for (var l : p.lines)
							assertEquals(++last, l.n.longValue());
						token = p.next;
					}
					return null;
				}));
			writer.get(30, TimeUnit.SECONDS);
			for (var f : readers)
				f.get(30, TimeUnit.SECONDS);
		} finally {
			pool.shutdownNow();
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// e - the open trailing line
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_openAppendDotClose() {
		var log = new ConsoleOutputLog();
		assertEquals(1L, log.openLine(ConsoleOutputLine.info("ORDERS: filling UID")));
		assertTrue(log.appendText("..."));
		assertTrue(log.dot());
		var open = log.page(null, 10).validate().lines.get(0);
		assertBean(open, "n,text", "1,ORDERS: filling UID....");
		assertEquals(Boolean.TRUE, open.open);
		assertTrue(log.closeLine(" 1819 rows in 4s"));
		var closed = log.page(null, 10).validate().lines.get(0);
		assertBean(closed, "n,text", "1,ORDERS: filling UID.... 1819 rows in 4s");
		assertNull(closed.open);
		assertEquals(1, log.size());
	}

	@Test void e02_noOpenLineReturnsFalse() {
		var log = new ConsoleOutputLog();
		assertFalse(log.appendText("x"));
		assertFalse(log.dot());
		assertFalse(log.closeLine());
		assertFalse(log.closeLine("x"));
		log.append(ConsoleOutputLine.info("closed"));
		assertFalse(log.appendText("x"));
		assertList(texts(log.page(null, 10)), "closed");
	}

	@Test void e03_appendOpenLineAndAppendAnsiCloseIt() {
		var log = new ConsoleOutputLog();
		log.openLine(ConsoleOutputLine.info("a"));
		log.append(ConsoleOutputLine.info("b"));
		assertFalse(log.appendText("x"));
		log.openLine(ConsoleOutputLine.info("c"));
		log.openLine(ConsoleOutputLine.info("d"));
		log.appendAnsi(Level.INFO, "e");
		var p = log.page(null, 10).validate();
		assertList(texts(p), "a", "b", "c", "d", "e");
		assertTrue(noneOpen(p));
	}

	@Test void e04_completeClosesIt() {
		var log = new ConsoleOutputLog();
		log.start();
		log.openLine(ConsoleOutputLine.info("work"));
		log.complete("DONE", Style.SUCCESS);
		var p = log.page(null, 10).validate();
		assertBean(p, "terminal,next", "true,1");
		assertTrue(noneOpen(p));
		assertFalse(log.dot());
	}

	@Test void e05_appendIgnoresTheCallersOpenFlag() {
		var log = new ConsoleOutputLog();
		log.append(ConsoleOutputLine.info("x").open(true));
		assertTrue(noneOpen(log.page(null, 10)));
		assertFalse(log.dot());
	}

	@Test void e06_pageEndingOnTheOpenLinePointsBeforeIt() {
		var log = log(3);
		log.openLine(ConsoleOutputLine.info("work"));
		var p1 = log.page(null, 100).validate();
		assertList(ns(p1), 1L, 2L, 3L, 4L);
		assertBean(p1, "next,more", "3,false");
		log.dot();
		var p2 = log.page(p1.next, 100).validate();
		assertList(ns(p2), 4L);
		assertBean(p2, "next,more", "3,false");
		assertBean(p2.lines.get(0), "text", "work.");
		log.closeLine();
		log.append(ConsoleOutputLine.info("after"));
		var p3 = log.page(p2.next, 100).validate();
		assertList(ns(p3), 4L, 5L);
		assertBean(p3, "next,more", "5,false");
		assertTrue(noneOpen(p3));
	}

	@Test void e07_pageStoppedShortOfTheOpenLineHasMore() {
		var log = log(3);
		log.openLine(ConsoleOutputLine.info("work"));
		var p = log.page(null, 3).validate();
		assertBean(p, "next,more", "3,true");
		assertTrue(noneOpen(p));
	}

	@Test void e08_tailPointsBeforeTheOpenLine() {
		var log = log(3);
		log.openLine(ConsoleOutputLine.info("work"));
		var p = log.tail(10).validate();
		assertList(ns(p), 1L, 2L, 3L, 4L);
		assertBean(p, "next", "3");
		assertEquals(Boolean.TRUE, p.lines.get(3).open);
	}

	@Test void e09_beforeAndStreamIncludeTheOpenLine() {
		var log = log(2);
		log.openLine(ConsoleOutputLine.info("work"));
		var b = log.before("3", 10).validate();
		assertList(ns(b), 1L, 2L, 3L);
		assertEquals(Boolean.TRUE, b.lines.get(2).open);
		try (var s = log.stream()) {
			var l = s.collect(Collectors.toList());
			assertEquals(3, l.size());
			assertEquals(Boolean.TRUE, l.get(2).open);
		}
	}

	@Test void e10_capWrapsOntoANewOpenLine() {
		var log = new ConsoleOutputLog();
		log.openLine(ConsoleOutputLine.warning("h"));
		var max = ConsoleOutputLine.MAX_TEXT_CHARS;
		assertTrue(log.appendText("x".repeat(max + 10)));
		var p = log.page(null, 10).validate();
		assertEquals(2, p.lines.size());
		assertEquals(max, p.lines.get(0).text.length());
		assertNull(p.lines.get(0).open);
		assertEquals("x".repeat(11), p.lines.get(1).text);
		assertEquals(Level.WARNING, p.lines.get(1).level);
		assertEquals(Boolean.TRUE, p.lines.get(1).open);
	}

	@Test void e11_maxLines() {
		var log = new ConsoleOutputLog(2);
		log.append(ConsoleOutputLine.info("a"));
		log.openLine(ConsoleOutputLine.info("b"));
		assertTrue(log.dot());
		assertEquals(-1L, log.openLine(ConsoleOutputLine.info("c")));
		assertFalse(log.dot());
		var p = log.page(null, 10).validate();
		assertList(texts(p), "a", "b.", "… output truncated");
		assertTrue(noneOpen(p));
	}

	@Test void e12_fragHeadGetsOnePlainTailFrag() {
		var log = new ConsoleOutputLog();
		log.openLine(ConsoleOutputLine.frags(Frag.text("step ").style(Style.ACCENT)));
		log.dot();
		log.dot();
		var l = log.page(null, 10).validate().lines.get(0);
		assertEquals(2, l.frags.size());
		assertBean(l.frags.get(1), "text", "..");
		assertNull(l.frags.get(1).style);
		assertEquals(Style.ACCENT, l.frags.get(0).style);
	}

	@Test void e13_concurrentDotsAndPaging() throws Exception {
		var log = new ConsoleOutputLog();
		var pool = Executors.newFixedThreadPool(3);
		try {
			var writer = pool.submit(() -> {
				for (var i = 0; i < 200; i++) {
					log.openLine(ConsoleOutputLine.info("job " + i));
					for (var d = 0; d < 20; d++)
						log.dot();
					log.closeLine(" ok");
				}
			});
			var readers = new ArrayList<Future<?>>();
			for (var r = 0; r < 2; r++)
				readers.add(pool.submit(() -> {
					String token = null;
					while (! writer.isDone()) {
						var p = log.page(token, 50).validate();
						token = p.next;
					}
					return null;
				}));
			writer.get(30, TimeUnit.SECONDS);
			for (var f : readers)
				f.get(30, TimeUnit.SECONDS);
			assertTrue(noneOpen(log.page(null, 1000)));
			assertEquals(200, log.size());
		} finally {
			pool.shutdownNow();
		}
	}

	@Test void e14_setTailReplacesOnlyTheTail() {
		var log = new ConsoleOutputLog();
		log.openLine(ConsoleOutputLine.info("Performing task x: "));
		assertTrue(log.setTail("100 of 200 complete"));
		assertBean(log.page(null, 10).lines.get(0), "text,open", "Performing task x: 100 of 200 complete,true");
		assertTrue(log.setTail("2 of 2"));
		assertBean(log.page(null, 10).lines.get(0), "text", "Performing task x: 2 of 2");
		log.dot();
		assertTrue(log.closeLine());
		var l = log.page(null, 10).validate().lines.get(0);
		assertBean(l, "n,text", "1,Performing task x: 2 of 2.");
		assertNull(l.open);
	}

	@Test void e15_setTailWithNoOpenLine() {
		var log = new ConsoleOutputLog();
		assertFalse(log.setTail("x"));
		log.openLine(ConsoleOutputLine.info("a"));
		log.closeLine();
		assertFalse(log.setTail("x"));
		assertList(texts(log.page(null, 10)), "a");
	}

	@Test void e16_setTailNullOrEmptyClearsTheTail() {
		var log = new ConsoleOutputLog();
		log.openLine(ConsoleOutputLine.info("head"));
		log.appendText(" tail");
		assertTrue(log.setTail(null));
		assertBean(log.page(null, 10).lines.get(0), "text", "head");
		log.appendText("!");
		assertTrue(log.setTail(""));
		assertBean(log.page(null, 10).lines.get(0), "text", "head");
	}

	@Test void e17_setTailTruncatesAtTheCapAndNeverWraps() {
		var log = new ConsoleOutputLog();
		log.openLine(ConsoleOutputLine.info("head"));
		var max = ConsoleOutputLine.MAX_TEXT_CHARS;
		assertTrue(log.setTail("x".repeat(max * 2)));
		var p = log.page(null, 10).validate();
		assertEquals(1, p.lines.size());
		assertEquals(max, p.lines.get(0).text.length());
		assertTrue(p.lines.get(0).text.startsWith("headxxx"));
		assertEquals(Boolean.TRUE, p.lines.get(0).open);
	}

	@Test void e18_setTailOnAFragHead() {
		var log = new ConsoleOutputLog();
		log.openLine(ConsoleOutputLine.frags(Frag.text("task ").style(Style.ACCENT)));
		log.setTail("1 of 2");
		log.setTail("2 of 2");
		var l = log.page(null, 10).validate().lines.get(0);
		assertEquals(2, l.frags.size());
		assertBean(l.frags.get(1), "text", "2 of 2");
	}

	//------------------------------------------------------------------------------------------------------------------
	// f - appendRaw and the open line
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_partialLinePublishedOpenThenClosed() throws Exception {
		var log = new ConsoleOutputLog();
		var r = chunks(log, "first\npart", "ial\n");
		assertEquals(2, log.appendRaw(Level.INFO, r));
		assertList(r.seen, "1:first|2:part*", "1:first|2:partial");
	}

	@Test void f02_eofClosesIt() throws Exception {
		var log = new ConsoleOutputLog();
		var r = chunks(log, "abc");
		assertEquals(1, log.appendRaw(Level.INFO, r));
		assertList(r.seen, "1:abc*");
		assertEquals("1:abc", show(log));
	}

	@Test void f03_incompleteEscapeHeldBack() throws Exception {
		var log = new ConsoleOutputLog();
		var r = chunks(log, "\u001b[31mred\u001b[", "0m done\n");
		log.appendRaw(Level.INFO, r);
		assertEquals("1:red*", r.seen.get(0));
		assertEquals("1:red done", show(log));
		var l = log.page(null, 10).lines.get(0);
		assertEquals(Style.ERROR, l.frags.get(0).style);
	}

	@Test void f04_openTextDoesNotLeakSgrState() throws Exception {
		var log = new ConsoleOutputLog();
		log.appendRaw(Level.INFO, chunks(log, "a\u001b[31mb", "\n"));
		var frags = log.page(null, 10).lines.get(0).frags;
		assertBean(frags.get(0), "text", "a");
		assertNull(frags.get(0).style);
		assertEquals(Style.ERROR, frags.get(1).style);
	}

	@Test void f05_bareCrRewritesTheOpenLine() throws Exception {
		var log = new ConsoleOutputLog();
		var r = chunks(log, "Performing 1 of 200", "\rPerforming 200 of 200", "\rok\n");
		log.appendRaw(Level.INFO, r);
		assertList(r.seen, "1:Performing 1 of 200*", "1:Performing 200 of 200*", "1:ok");
	}

	@Test void f06_crLfSplitAcrossReadsIsALineEnding() throws Exception {
		var log = new ConsoleOutputLog();
		var r = chunks(log, "abc\r", "\ndef\n");
		assertEquals(2, log.appendRaw(Level.INFO, r));
		assertEquals("1:abc*", r.seen.get(0));
		assertEquals("1:abc|2:def", show(log));
	}

	@Test void f07_closedLineKeepsItsFinalCrSegment() throws Exception {
		var log = new ConsoleOutputLog();
		log.appendRaw(Level.INFO, new StringReader("a\rb\rc\nx\r\n"));
		assertEquals("1:c|2:x", show(log));
	}

	@Test void f08_trailingCrNeverBlanksALine() throws Exception {
		var log = new ConsoleOutputLog();
		var r = chunks(log, "abc\r");
		log.appendRaw(Level.INFO, r);
		assertEquals("1:abc*", r.seen.get(0));
		assertEquals("1:abc", show(log));
	}

	@Test void f09_splitSurrogateHeldBack() throws Exception {
		var log = new ConsoleOutputLog();
		var r = chunks(log, "a\uD83D", "\uDE00\n");
		log.appendRaw(Level.INFO, r);
		assertEquals("1:a*", r.seen.get(0));
		assertEquals("1:a😀", show(log));
	}

	@Test void f10_anotherWriterClosesTheOpenLine() throws Exception {
		var log = new ConsoleOutputLog();
		var r = new ChunkReader(log, i -> { if (i == 1) log.append(ConsoleOutputLine.info("other")); }, "part", "ial\n");
		log.appendRaw(Level.INFO, r);
		assertEquals("1:part|2:other|3:ial", show(log));
	}

	@Test void f11_twoRawReadersInterleave() throws Exception {
		var log = new ConsoleOutputLog();
		var err = new ChunkReader(log, i -> {}, "E1", "\n");
		var out = new ChunkReader(log, i -> {
			try {
				if (i == 1)
					log.appendRaw(Level.SEVERE, err);
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}, "o1", "o2\n");
		log.appendRaw(Level.INFO, out);
		assertEquals("1:o1|2:E1|3:o2", show(log));
		assertTrue(noneOpen(log.page(null, 10)));
	}

	@Test void f12_hugeNewlineLessStreamShowsTheCappedTextThenClosesExactly() throws Exception {
		var log = new ConsoleOutputLog();
		var max = ConsoleOutputLine.MAX_TEXT_CHARS;
		var total = max * 4 + 123;
		var all = "x".repeat(total);
		var seenOpen = new ArrayList<ConsoleOutputLine>();
		var r = new Reader() {
			private int pos;
			private boolean nl;
			@Override public int read(char[] b, int off, int len) {
				if (pos == total) {
					if (nl)
						return -1;
					seenOpen.add(log.page(null, 10).lines.get(0));
					nl = true;
					b[off] = '\n';
					return 1;
				}
				var n = Math.min(Math.min(len, 8192), total - pos);
				all.getChars(pos, pos + n, b, off);
				pos += n;
				return n;
			}
			@Override public void close() { /* nothing */ }
		};
		assertEquals(1, log.appendRaw(Level.INFO, r));
		// The open line shows the capped text, as one line.
		assertEquals(1, seenOpen.size());
		assertEquals(Boolean.TRUE, seenOpen.get(0).open);
		assertTrue(seenOpen.get(0).text.startsWith("x".repeat(max)));
		// Closing it decodes the whole text: the same line a one-shot append produces.
		var ref = new ConsoleOutputLog();
		ref.appendAnsi(Level.INFO, all);
		var l = log.page(null, 10).lines.get(0);
		assertNull(l.open);
		assertEquals(ref.page(null, 10).lines.get(0).text, l.text);
		assertEquals(1, log.size());
	}
}
