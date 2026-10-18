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
package org.apache.juneau.releng.log;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.rest.server.runreport.RunEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Branch and error-path coverage for the log package.
 */
class LogBranchesTest {

	// ---- VoteDeadlineTimer ----

	private static ProcessRunner capturing(List<List<String>> seen, CountDownLatch latch) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return "";
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e) {
				seen.add(c);
				latch.countDown();
				return new ProcResult(0, "");
			}
		};
	}

	@Test
	@SuppressWarnings({
		"java:S2925" // Asserts that nothing fires; the sleep is the observation window for the (absent) timer callback.
	})
	void a01_timerSkipsWithoutWebhookOrWithPastDeadline() throws Exception {
		var seen = new java.util.concurrent.CopyOnWriteArrayList<List<String>>();
		new VoteDeadlineTimer(capturing(seen, new CountDownLatch(1)), "").arm("9.2.1", Instant.now().plusMillis(50));
		new VoteDeadlineTimer(capturing(seen, new CountDownLatch(1)), null).arm("9.2.1", Instant.now().plusMillis(50));
		new VoteDeadlineTimer(capturing(seen, new CountDownLatch(1)), "http://hook").arm("9.2.1", Instant.now().minusSeconds(5));
		Thread.sleep(300);
		assertTrue(seen.isEmpty());
	}

	@Test
	void a02_timerPingsWebhookAtDeadline() throws Exception {
		var seen = new java.util.concurrent.CopyOnWriteArrayList<List<String>>();
		var latch = new CountDownLatch(1);
		new VoteDeadlineTimer(capturing(seen, latch), "http://hook").arm("9.2.1", Instant.now().plusMillis(100));
		assertTrue(latch.await(5, TimeUnit.SECONDS));
		var cmd = seen.get(0);
		assertEquals("curl", cmd.get(0));
		assertEquals("http://hook", cmd.get(cmd.size() - 1));
		assertTrue(cmd.stream().anyMatch(a -> a.contains("9.2.1 vote window has closed")));
	}

	// ---- RunLog ----

	@Test
	void a03_runLogErrorsAndSize(@TempDir Path dir) throws Exception {
		var blocker = Files.writeString(dir.resolve("blocker"), "x");
		var blockedLog = blocker.resolve("sub/x.log");
		assertThrows(RuntimeException.class, () -> new RunLog(blockedLog));

		var log = new RunLog(dir.resolve("a.log"));
		assertEquals(dir.resolve("a.log"), log.file());
		assertEquals(0, log.size(), "no file yet");
		var asDir = new RunLog(dir.resolve("d.log"));
		Files.createDirectories(dir.resolve("d.log"));
		assertThrows(RuntimeException.class, () -> asDir.append("x"));
		assertThrows(RuntimeException.class, asDir::reset);
		assertEquals(0, asDir.size());
	}

	// ---- RunStateSseServlet ----

	private static HttpServletRequest req(String pathInfo) {
		var r = mock(HttpServletRequest.class);
		when(r.getPathInfo()).thenReturn(pathInfo);
		return r;
	}

	@Test
	@SuppressWarnings({
		"resource" // Mock HttpServletResponse objects are never closed; that responsibility is on the caller/container.
	})
	void b01_writerFailureEndsTheStreamQuietly() throws Exception {
		var servlet = new RunStateSseServlet(v -> Optional.empty(), v -> Optional.empty());
		var resp = mock(HttpServletResponse.class);
		when(resp.getWriter()).thenThrow(new IOException("gone"));
		servlet.doGet(req("/9.2.1/state"), resp);
		verify(resp).setStatus(HttpServletResponse.SC_NO_CONTENT);

		var committed = mock(HttpServletResponse.class);
		when(committed.getWriter()).thenThrow(new IOException("gone"));
		when(committed.isCommitted()).thenReturn(true);
		servlet.doGet(req("/9.2.1/state"), committed);
		verify(committed, never()).setStatus(HttpServletResponse.SC_NO_CONTENT);
	}

	@Test
	void b02_pathInfoShapes() {
		var servlet = new RunStateSseServlet(v -> Optional.empty(), v -> Optional.empty());
		for (var path : new String[] { null, "", "9.2.1", "/9.2.1", "9.2.1/other" }) {
			var resp = mock(HttpServletResponse.class);
			servlet.doGet(req(path), resp);
			verify(resp).setStatus(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@Test
	@SuppressWarnings({
		"resource" // Mock HttpServletResponse object is never closed; that responsibility is on the caller/container.
	})
	void b03_noInitialSnapshotButABroadcasterTailsUntilInterrupted() throws Exception {
		var resp = mock(HttpServletResponse.class);
		when(resp.getWriter()).thenReturn(new PrintWriter(new StringWriter()));
		var servlet = new RunStateSseServlet(v -> Optional.empty(), v -> Optional.of(new RunStateBroadcaster()));
		var t = new Thread(() -> servlet.doGet(req("9.2.1/state"), resp));
		t.setDaemon(true);
		t.start();
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
		while (t.getState() != Thread.State.TIMED_WAITING && System.nanoTime() < deadline)
			Thread.onSpinWait();
		assertEquals(Thread.State.TIMED_WAITING, t.getState(), "servlet is tailing the broadcaster queue");
		t.interrupt();
		t.join(2000);
		assertFalse(t.isAlive());
	}

	@Test
	@SuppressWarnings({
		"resource" // Mock HttpServletResponse object is never closed; that responsibility is on the caller/container.
	})
	void b04_unsubscribeFailureIsSwallowed() throws Exception {
		var capture = new StringWriter();
		var resp = mock(HttpServletResponse.class);
		when(resp.getWriter()).thenReturn(new PrintWriter(capture));
		var bc = new RunStateBroadcaster() {
			@Override
			public AutoCloseable subscribe(Consumer<String> sink) {
				Thread.currentThread().interrupt(); // makes the first poll stop the loop immediately
				return () -> {
					throw new Exception("close failed");
				};
			}
		};
		var servlet = new RunStateSseServlet(v -> Optional.empty(), v -> Optional.of(bc));
		servlet.doGet(req("9.2.1/state"), resp);
		assertTrue(Thread.interrupted());
	}

	// ---- RunEventStore ----

	@Test
	void c01_noteAndWaitingGuards(@TempDir Path dir) throws Exception {
		var store = new RunEventStore(dir);
		store.waiting("9.2.1", "ghost", "Ghost"); // never ran: nothing written
		store.note("9.2.1", RunEvent.Level.INFO, null, null, null);
		store.note("9.2.1", RunEvent.Level.INFO, "  ", null, null);
		assertFalse(Files.exists(dir.resolve("logs/9.2.1-events.jsonl")));
		store.note("9.2.1", RunEvent.Level.INFO, "hello", " ", null);
		store.note("9.2.1", RunEvent.Level.INFO, "x".repeat(2000), null, null);
		var lines = Files.readAllLines(dir.resolve("logs/9.2.1-events.jsonl"));
		assertEquals(2, lines.size());
		assertFalse(lines.get(0).contains("href"));
	}

	@Test
	void c02_attemptsIgnoreNonNumericSuffixes(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		assertEquals("build", store.begin("9.2.1", "build", "Build", 1));
		assertEquals("build.x", store.begin("9.2.1", "build.x", "Other", 2));
		assertEquals("build.2", store.begin("9.2.1", "build", "Build", 1));
	}

	@Test
	void c03_scanToleratesForeignAndBrokenLines(@TempDir Path dir) throws Exception {
		var store = new RunEventStore(dir);
		var file = dir.resolve("logs/9.2.1-events.jsonl");
		Files.createDirectories(file.getParent());
		Files.writeString(file, "{\"ev\":\"note\",\"x\":{\"ev\":\"end\"}}\n{\"ev\":\"step\" broken\n");
		assertEquals("build", store.begin("9.2.1", "build", "Build", 1));
	}
}
