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
}
