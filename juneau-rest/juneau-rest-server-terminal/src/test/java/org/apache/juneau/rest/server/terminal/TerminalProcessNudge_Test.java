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
package org.apache.juneau.rest.server.terminal;

import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.terminal.TerminalProcess.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.*;

/**
 * {@link TerminalProcess} nudges, over a fake {@code python3} runner.
 */
@DisabledOnOs(OS.WINDOWS)
class TerminalProcessNudge_Test extends TestBase {

	private record Nudged(String id, long end, boolean done) {}

	@TempDir Path dir;

	private Path bin, run;

	@BeforeEach void setup() throws Exception {
		bin = Files.createDirectories(dir.resolve("bin"));
		run = dir.resolve("run");
	}

	/** Writes a fake python3; the byte log is its eighth argument. */
	private void fakePython(String body) throws Exception {
		var p = bin.resolve("python3");
		Files.writeString(p, "#!/bin/sh\nLOGFILE=\"$8\"\n" + body + "\n");
		Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
	}

	private TerminalProcess start() throws Exception {
		return TerminalProcess.start("maven", List.of("mvn", "-q", "test"), 100, 30, run, new Host("Mac OS X", "/x/juneau_run.py", null, bin.toString()), false);
	}

	/** The live nudge watcher thread, if there is one. */
	private static Optional<Thread> nudgeThread() {
		return Thread.getAllStackTraces().keySet().stream().filter(x -> x.getName().startsWith("juneau-terminal-nudge-") && x.isAlive()).findFirst();
	}

	private static void awaitDead(Thread t, long millis) throws Exception {
		t.join(millis);
		assertFalse(t.isAlive(), "the watcher thread is still alive");
	}

	@Test void a01_growthPublishes() throws Exception {
		fakePython("printf aaaa >> \"$LOGFILE\"\nsleep 0.15\nprintf bbbb >> \"$LOGFILE\"\nsleep 0.15\nprintf cccc >> \"$LOGFILE\"\nsleep 0.3\nexit 0");
		var seen = new CopyOnWriteArrayList<Nudged>();
		try (var p = start()) {
			p.nudges("t1", (id, end, done) -> seen.add(new Nudged(id, end, done)));
			assertTrue(await(() -> seen.stream().anyMatch(Nudged::done), Duration.ofSeconds(5)), () -> "no done nudge: " + seen);
			assertEquals(1, seen.stream().filter(Nudged::done).count());
			var ends = seen.stream().filter(n -> ! n.done()).mapToLong(Nudged::end).toArray();
			for (var i = 1; i < ends.length; i++)
				assertTrue(ends[i] > ends[i - 1], "ends not strictly increasing: " + seen);
			var last = seen.get(seen.size() - 1);
			assertTrue(last.done());
			assertEquals(Files.size(p.log()), last.end());
			assertEquals("t1", last.id());
		}
	}

	@Test void a02_threadNamed() throws Exception {
		fakePython("exec sleep 30");
		try (var p = start()) {
			p.nudges("t1", (id, end, done) -> {});
			var t = nudgeThread().orElse(null);
			assertNotNull(t);
			assertTrue(t.isDaemon());
			assertTrue(t.getName().matches("juneau-terminal-nudge-\\d+"));
		}
	}

	@Test void a03_closeStops() throws Exception {
		fakePython("exec sleep 30");
		var p = start();
		p.nudges("t1", (id, end, done) -> {});
		var t = nudgeThread().orElseThrow();
		p.close();
		awaitDead(t, 2000);
	}

	@SuppressWarnings({
		"java:S2925" // Deliberate delay: the fake exit hook is slow on purpose, so close() has a wait to observe.
	})
	@Test void a03b_closeStopsWatcherFirst() throws Exception {
		fakePython("exec sleep 30");
		// The exit hook is slow, so close() waits about two seconds for it; the watcher must not run through that wait.
		DoneWriter slow = (events, code) -> {
			try {
				Thread.sleep(2000);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		};
		var p = TerminalProcess.start("maven", List.of("mvn", "-q", "test"), 100, 30, run, new Host("Mac OS X", "/x/juneau_run.py", null, bin.toString()), slow);
		p.nudges("t1", (id, end, done) -> {});
		var t = nudgeThread().orElseThrow();
		var closer = new Thread(p::close);
		closer.start();
		awaitDead(t, 500);
		assertTrue(closer.isAlive(), "close() should still be waiting for the exit hook");
		closer.join(10_000);
		assertFalse(closer.isAlive());
	}

	@Test void a04_validation() throws Exception {
		fakePython("exec sleep 30");
		TerminalNudgePublisher n = (id, end, done) -> {};
		try (var p = start()) {
			assertThrows(NullPointerException.class, () -> p.nudges(null, n));
			assertThrows(NullPointerException.class, () -> p.nudges("t1", null));
			assertThrows(IllegalArgumentException.class, () -> p.nudges("bad id", n));
			assertSame(p, p.nudges("t1", n));
			assertThrows(IllegalStateException.class, () -> p.nudges("t2", n));
		}
		run = dir.resolve("run2");
		var q = start();
		q.close();
		assertThrows(IllegalStateException.class, () -> q.nudges("t1", n));
	}

	@Test void a05_throwingPublisher() throws Exception {
		fakePython("printf aaaa >> \"$LOGFILE\"\nsleep 0.15\nprintf bbbb >> \"$LOGFILE\"\nexit 0");
		var calls = new java.util.concurrent.atomic.AtomicInteger();
		try (var p = start()) {
			LogRecordCapture.quietly(TerminalProcess.class, () -> {
				p.nudges("t1", (id, end, done) -> {
					calls.incrementAndGet();
					throw new RuntimeException("boom");
				});
				try {
					assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
					assertTrue(await(() -> nudgeThread().isEmpty(), Duration.ofSeconds(5)), "the watcher thread did not exit");
				} catch (Exception e) {
					throw new AssertionError(e);
				}
			});
			assertTrue(calls.get() >= 1);
		}
	}
}
