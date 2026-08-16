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
import java.io.Writer;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;

class SseLogServletTest {

	@Test
	void framesALineAsAnSseDataEvent() {
		assertEquals("data: hello\n\n", SseLogServlet.sse("hello"));
	}

	@Test
	void framesMultilinePayloadWithPerLineDataPrefix() {
		assertEquals("data: a\ndata: b\n\n", SseLogServlet.sse("a\nb"));
	}

	@Test
	void heartbeatIsAnSseComment() {
		assertEquals(": heartbeat\n\n", SseLogServlet.HEARTBEAT);
	}

	@Test
	void stateSegmentConstantIsNeverARealStepId() {
		// StepRegistry.standard()'s 24 step ids never collide with this; doGet's routing relies on that.
		assertEquals("state", SseLogServlet.STATE_SEGMENT);
	}

	private HttpServletRequest requestFor(String pathInfo) {
		var req = mock(HttpServletRequest.class);
		when(req.getPathInfo()).thenReturn(pathInfo);
		return req;
	}

	private HttpServletResponse responseWriting(PrintWriter out) throws IOException {
		var resp = mock(HttpServletResponse.class);
		when(resp.getWriter()).thenReturn(out);
		return resp;
	}

	@Test
	void consoleOnlyConstructorTreatsStateSegmentAsNoActiveRun() throws IOException {
		// The 2-arg (pre-existing) constructor defaults the state resolvers to always-empty, so a client
		// hitting .../state against a servlet built the old way gets the same graceful fallback as an
		// unknown step — never a 500 or an unrecognized-route surprise.
		var servlet = new SseLogServlet((v, s) -> Optional.empty(), (v, s) -> Optional.empty());
		var capture = new java.io.StringWriter();
		var out = new PrintWriter(capture);

		servlet.doGet(requestFor("/9.2.1/state"), responseWriting(out));

		assertEquals(SseLogServlet.sse("(no active run for 9.2.1)"), capture.toString());
	}

	@Test
	void stateChannelSendsNoActiveRunWhenNoBroadcasterResolves() throws IOException {
		var servlet = new SseLogServlet((v, s) -> Optional.empty(), (v, s) -> Optional.empty(),
				v -> Optional.empty(), v -> Optional.empty());
		var capture = new java.io.StringWriter();
		var out = new PrintWriter(capture);

		servlet.doGet(requestFor("/9.2.1/state"), responseWriting(out));

		assertEquals(SseLogServlet.sse("(no active run for 9.2.1)"), capture.toString());
	}

	/**
	 * A {@link Writer} standing in for the servlet container's response writer: captures everything
	 * written, and can be told to fail on the next write to simulate the client disconnecting (a broken
	 * SSE pipe), which is how the tail loop's {@code checkError()} check is exercised deterministically.
	 */
	private static final class FailableWriter extends Writer {
		final StringBuilder captured = new StringBuilder();
		final CountDownLatch initialSeen;
		final CountDownLatch updateSeen;
		volatile boolean failNext;

		FailableWriter(CountDownLatch initialSeen, CountDownLatch updateSeen) {
			this.initialSeen = initialSeen;
			this.updateSeen = updateSeen;
		}

		@Override
		public synchronized void write(char[] cbuf, int off, int len) throws IOException {
			if (failNext)
				throw new IOException("simulated broken pipe");
			captured.append(cbuf, off, len);
			if (captured.toString().contains("INITIAL_SNAPSHOT"))
				initialSeen.countDown();
			if (captured.toString().contains("UPDATED_SNAPSHOT"))
				updateSeen.countDown();
		}

		@Override
		public void flush() {
			// Nothing buffered beyond write() itself; no-op.
		}

		@Override
		public void close() {
			// Nothing to release.
		}
	}

	@Test
	void stateChannelSendsInitialSnapshotThenTailsLiveUpdatesUntilTheClientDisconnects() throws Exception {
		var initialSeen = new CountDownLatch(1);
		var updateSeen = new CountDownLatch(1);
		var writer = new FailableWriter(initialSeen, updateSeen);
		var out = new PrintWriter(writer);
		var bc = new RunStateBroadcaster();
		var servlet = new SseLogServlet((v, s) -> Optional.empty(), (v, s) -> Optional.empty(),
				v -> Optional.of("INITIAL_SNAPSHOT"), v -> Optional.of(bc));
		var req = requestFor("/9.2.1/state");
		var resp = responseWriting(out);

		var worker = new Thread(() -> servlet.doGet(req, resp));
		worker.setDaemon(true);
		worker.start();

		assertTrue(initialSeen.await(2, TimeUnit.SECONDS), "the current snapshot must be sent on connect");
		// tail() subscribes to bc slightly after the initial send completes; re-publish until the worker
		// thread's subscription has actually landed rather than racing a single publish against it.
		var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
		while (!updateSeen.await(20, TimeUnit.MILLISECONDS)) {
			bc.publish("UPDATED_SNAPSHOT");
			if (System.nanoTime() > deadline)
				fail("a published snapshot must be tailed live");
		}

		writer.failNext = true;
		bc.publish("nudge-the-blocked-poll-loop"); // wake tail()'s poll so its next write hits the failure
		worker.join(2000);
		assertFalse(worker.isAlive(), "the tail loop must exit once writes to the client start failing");

		assertEquals(SseLogServlet.sse("INITIAL_SNAPSHOT") + SseLogServlet.sse("UPDATED_SNAPSHOT"),
				writer.captured.toString());
	}
}
