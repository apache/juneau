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

import static java.nio.file.StandardOpenOption.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * {@link FileTerminalSource} nudges.
 */
@SuppressWarnings({
	"java:S2925" // Timing-dependent process/file test; it polls or waits on real time
})
class FileTerminalSourceNudge_Test extends TestBase {

	private record Nudged(String id, long end, boolean done) {}

	@TempDir Path dir;

	private Path log;

	@BeforeEach void setup() {
		log = dir.resolve("run.log");
	}

	private void append(String s) throws Exception {
		Files.writeString(log, s, CREATE, APPEND);
	}

	private static Thread watcher() {
		return Thread.getAllStackTraces().keySet().stream().filter(x -> x.getName().startsWith("juneau-terminal-nudge-file")
			&& x.isAlive()).findFirst().orElse(null);
	}

	private static void awaitEnd(CopyOnWriteArrayList<Nudged> seen, long end) throws Exception {
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (seen.stream().noneMatch(n -> n.end() >= end) && System.nanoTime() < deadline)
			Thread.sleep(20);
		assertTrue(seen.stream().anyMatch(n -> n.end() >= end), "no nudge reaching " + end + ": " + seen);
	}

	@Test void a01_growthPublishes() throws Exception {
		append("aaaa");
		var seen = new CopyOnWriteArrayList<Nudged>();
		try (var s = FileTerminalSource.create(log).build()) {
			s.nudges("t1", (id, end, done) -> seen.add(new Nudged(id, end, done)));
			awaitEnd(seen, 4);
			append("bbbb");
			awaitEnd(seen, 8);
			assertTrue(seen.stream().noneMatch(Nudged::done));
			assertEquals("t1", seen.get(0).id());
			for (var i = 1; i < seen.size(); i++)
				assertTrue(seen.get(i).end() > seen.get(i - 1).end(), "not strictly increasing: " + seen);
		}
	}

	@Test void a02_noGrowthNoPublish() throws Exception {
		append("aaaa");
		var seen = new CopyOnWriteArrayList<Nudged>();
		try (var s = FileTerminalSource.create(log).build()) {
			s.nudges("t1", (id, end, done) -> seen.add(new Nudged(id, end, done)));
			awaitEnd(seen, 4);
			var n = seen.size();
			Thread.sleep(500);
			assertEquals(n, seen.size(), "published without growth: " + seen);
		}
	}

	@Test void a03_closeStops() throws Exception {
		var s = FileTerminalSource.create(log).build();
		s.nudges("t1", (id, end, done) -> {});
		var t = watcher();
		assertNotNull(t);
		assertTrue(t.isDaemon());
		s.close();
		t.join(2000);
		assertFalse(t.isAlive());
		assertThrows(IllegalStateException.class, () -> s.nudges("t1", (id, end, done) -> {}));
	}

	@Test void a04_validation() {
		TerminalNudgePublisher n = (id, end, done) -> {};
		try (var s = FileTerminalSource.create(log).build()) {
			assertThrows(NullPointerException.class, () -> s.nudges(null, n));
			assertThrows(NullPointerException.class, () -> s.nudges("t1", null));
			assertThrows(IllegalArgumentException.class, () -> s.nudges("bad id", n));
			assertSame(s, s.nudges("t1", n));
			assertThrows(IllegalStateException.class, () -> s.nudges("t2", n));
		}
	}

	@Test void a05_vanishedFileKeepsWatching() throws Exception {
		append("aaaa");
		var seen = new CopyOnWriteArrayList<Nudged>();
		try (var s = FileTerminalSource.create(log).build()) {
			s.nudges("t1", (id, end, done) -> seen.add(new Nudged(id, end, done)));
			awaitEnd(seen, 4);
			Files.delete(log);
			Thread.sleep(300);
			assertNotNull(watcher(), "the watcher died when the file vanished");
			append("0123456789");
			awaitEnd(seen, 10);
		}
	}

	@Test void a06_publisherFailureDoesNotKillWatcher() throws Exception {
		var seen = new CopyOnWriteArrayList<Nudged>();
		try (var s = FileTerminalSource.create(log).build()) {
			s.nudges("t1", (id, end, done) -> {
				seen.add(new Nudged(id, end, done));
				throw new IllegalStateException("boom");
			});
			append("aaaa");
			awaitEnd(seen, 4);
			append("bbbb");
			awaitEnd(seen, 8);
		}
	}
}
