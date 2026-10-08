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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.RunEvent.*;
import org.junit.jupiter.api.*;

class RunViewLog_Test extends TestBase {

	private static RunEvent note(String text) {
		return RunEvent.note(Level.INFO, text);
	}

	private static List<Long> seqs(RunViewPage p) {
		return p.events().stream().map(RunEvent::seq).toList();
	}

	@Test void a01_numbersFromOne() {
		var log = RunViewLog.create();
		assertEquals(1L, log.append(RunEvent.step("a", "A")).seq());
		assertEquals(2L, log.append(RunEvent.end("a", EndStatus.OK)).seq());
	}

	@Test void a02_pagingWithToken() {
		var log = RunViewLog.create();
		for (var i = 1; i <= 5; i++)
			log.append(note("n" + i));
		var p1 = log.page(null, 2);
		assertEquals(List.of(1L, 2L), seqs(p1));
		assertTrue(p1.more());
		var p2 = log.page(p1.next(), 10);
		assertEquals(List.of(3L, 4L, 5L), seqs(p2));
		assertFalse(p2.more());
		assertTrue(log.page(p2.next(), 10).events().isEmpty());
		assertDoesNotThrow(() -> p1.validate());
		assertDoesNotThrow(() -> p2.validate());
	}

	@Test void a03_firstPageOfEmptyLogHasToken() {
		var p = RunViewLog.create().page(null, 10);
		assertNotNull(p.next());
		assertDoesNotThrow(() -> p.validate());
		assertTrue(p.next().endsWith(".0"));
	}

	@Test void a04_appendAll() {
		var log = RunViewLog.create();
		var out = log.appendAll(List.of(note("a"), note("b")));
		assertEquals(List.of(1L, 2L), out.stream().map(RunEvent::seq).toList());
	}

	@Test void b01_resetInvalidatesOldTokens() {
		var log = RunViewLog.create();
		log.append(note("x"));
		var t = log.page(null, 10).next();
		log.reset();
		assertThrows(RunViewSource.UnknownTokenException.class, () -> log.page(t, 10));
		assertEquals(1L, log.append(note("y")).seq());
	}

	@Test void b02_malformedTokenIsUnknown() {
		var log = RunViewLog.create();
		assertThrows(RunViewSource.UnknownTokenException.class, () -> log.page("zzz", 10));
		assertThrows(RunViewSource.UnknownTokenException.class, () -> log.page("abcdef.x", 10));
		assertThrows(RunViewSource.UnknownTokenException.class, () -> log.page("a b", 10));
	}

	@Test void b03_tokenAheadOfLogIsUnknown() {
		var log = RunViewLog.create();
		var t = log.page(null, 10).next();
		var epoch = t.substring(0, 6);
		assertThrows(RunViewSource.UnknownTokenException.class, () -> log.page(epoch + ".99", 10));
	}

	@Test void c01_fullLogThrows() {
		var log = RunViewLog.create(2);
		log.append(note("a"));
		log.append(note("b"));
		assertThrowsWithMessage(IllegalStateException.class, "RunViewLog is full (2 events)", () -> log.append(note("c")));
	}

	@Test void c02_appendAfterCloseThrows() {
		var log = RunViewLog.create();
		log.close();
		assertThrowsWithMessage(IllegalStateException.class, "closed", () -> log.append(note("a")));
	}

	@Test void c03_doneDoesNotClose() {
		var log = RunViewLog.create();
		log.append(RunEvent.done(DoneStatus.OK));
		assertDoesNotThrow(() -> log.append(note("late")));
		assertFalse(log.isTerminal());
	}

	@Test void c04_closeMakesPagesTerminal() {
		var log = RunViewLog.create();
		for (var i = 0; i < 3; i++)
			log.append(note("n"));
		log.close();
		assertTrue(log.isTerminal());
		var p1 = log.page(null, 2);
		assertTrue(p1.more());
		assertFalse(p1.terminal());
		assertTrue(log.page(p1.next(), 2).terminal());
	}

	@Test void c05_oversizeEventRejected() {
		var ctl = "\u0001";
		var e = RunEvent.test("a", "jest", ctl.repeat(512), ctl.repeat(512), TestStatus.FAIL).withMsg(ctl.repeat(2000)).withTrace(ctl.repeat(8000));
		assertThrows(IllegalArgumentException.class, () -> RunViewLog.create().append(e));
	}

	@Test void c06_maxClamped() {
		var log = RunViewLog.create();
		log.append(note("a"));
		assertEquals(1, log.page(null, 0).events().size());
		assertEquals(1, log.page(null, Integer.MAX_VALUE).events().size());
	}

	@Test void e01_concurrentAppendsAreGapFree() throws Exception {
		var log = RunViewLog.create();
		var pool = Executors.newFixedThreadPool(8);
		var latch = new CountDownLatch(8);
		for (var t = 0; t < 8; t++)
			pool.submit(() -> {
				for (var i = 0; i < 500; i++)
					log.append(note("x"));
				latch.countDown();
			});
		assertTrue(latch.await(20, TimeUnit.SECONDS));
		pool.shutdown();
		assertEquals(LongStream.rangeClosed(1, 4000).boxed().toList(), seqs(log.page(null, 10_000)));
	}
}
