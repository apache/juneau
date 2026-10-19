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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.terminal.TerminalProcess.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.*;

/**
 * {@link TerminalProcess} prompt channel, over the real {@code juneau_run.py} and a small Python job.
 */
@DisabledOnOs(OS.WINDOWS)
class TerminalProcessPrompts_Test extends TestBase {

	/** A job that sends one prompt, reads one answer and prints its length. */
	private static final String JOB = String.join("\n",
		"import json, os, sys",
		"o = os.fdopen(int(os.environ['JUNEAU_PROMPT_FD_OUT']), 'w', buffering=1)",
		"i = os.fdopen(int(os.environ['JUNEAU_PROMPT_FD_IN']), 'r')",
		"o.write(json.dumps({'v':1,'type':'prompt','id':'p1','kind':'secret','message':'Token'}) + '\\n')",
		"a = json.loads(i.readline())",
		"print('GOT', len(a['value']), flush=True)",
		"sys.stderr.write('to-stderr\\n'); sys.stderr.flush()");

	private static final String ANSWER = "{\"v\":1,\"type\":\"answer\",\"id\":\"p1\",\"value\":\"hunter2\"}";

	@TempDir Path dir;

	private Path run;

	/** Collects what the process reports. */
	private static final class Sink implements TerminalPrompts {
		final LinkedBlockingQueue<String> lines = new LinkedBlockingQueue<>();
		final CountDownLatch ended = new CountDownLatch(1);
		final AtomicInteger ends = new AtomicInteger();

		@Override public void onLine(String jsonLine) {
			lines.add(jsonLine);
		}

		@Override public void onEnd() {
			ends.incrementAndGet();
			ended.countDown();
		}
	}

	@BeforeEach void setup() {
		run = dir.resolve("run");
	}

	private Host realHost() {
		var path = System.getenv("PATH");
		assumeTrue(TerminalProcess.findOnPath("python3", path) != null, "python3 not on the PATH - skipped");
		return new Host(System.getProperty("os.name"), TerminalProcessPty_Test.script().toString(), null, path);
	}

	private TerminalProcess startReal(List<String> cmd, TerminalPrompts prompts) throws Exception {
		return TerminalProcess.start("generic", cmd, 80, 24, run, realHost(), TerminalProcess::ensureDone, false, prompts);
	}

	private static String log(TerminalProcess p) throws Exception {
		return new String(p.terminal().read(0, 1 << 20).bytes(), UTF_8);
	}

	@Test void a01_promptLinesDelivered() throws Exception {
		var sink = new Sink();
		try (var p = startReal(List.of("python3", "-c", JOB), sink)) {
			var line = sink.lines.poll(60, TimeUnit.SECONDS);
			assertNotNull(line, "no prompt line arrived");
			assertTrue(line.contains("\"id\": \"p1\"") || line.contains("\"id\":\"p1\""), line);
			assertFalse(line.endsWith("\n"));
			assertTrue(p.acceptsAnswers());
			assertTrue(p.answer(ANSWER));
			assertEquals(0, p.waitFor(60, TimeUnit.SECONDS));
			assertTrue(sink.ended.await(10, TimeUnit.SECONDS));
			assertEquals(1, sink.ends.get());
		}
	}

	@Test void a02_answerReachesScript() throws Exception {
		var sink = new Sink();
		try (var p = startReal(List.of("python3", "-c", JOB), sink)) {
			assertNotNull(sink.lines.poll(60, TimeUnit.SECONDS));
			assertTrue(p.answer(ANSWER));
			assertEquals(0, p.waitFor(60, TimeUnit.SECONDS));
			assertTrue(sink.ended.await(10, TimeUnit.SECONDS));
			assertTrue(log(p).contains("GOT 7"), log(p));
			assertFalse(log(p).contains("hunter2"));
			assertFalse(Files.readString(p.eventsFile()).contains("hunter2"));
			assertFalse(Files.readString(run.resolve("runner.out")).contains("hunter2"));
		}
	}

	/** The runner's own stderr (here the warn for a malformed line) goes to runner.out; the job's stderr stays on its terminal. */
	@Test void a03_stderrGoesToRunnerOut() throws Exception {
		var sink = new Sink();
		var job = String.join("\n",
			"import os, sys",
			"os.write(int(os.environ['JUNEAU_PROMPT_FD_OUT']), b'garbage\\n')",
			"sys.stderr.write('to-stderr\\n'); sys.stderr.flush()");
		try (var p = startReal(List.of("python3", "-c", job), sink)) {
			assertEquals(0, p.waitFor(60, TimeUnit.SECONDS));
			assertTrue(sink.ended.await(10, TimeUnit.SECONDS));
			assertTrue(Files.readString(run.resolve("runner.out")).contains("is not JSON"));
			assertTrue(log(p).contains("to-stderr"), "the job's stderr is on its terminal");
			assertEquals(List.of(), new ArrayList<>(sink.lines), "nothing but valid prompt lines reaches onLine");
		}
	}

	@Test void a04_inputAndPromptsRejected() {
		assertThrows(IllegalArgumentException.class,
			() -> TerminalProcess.start("generic", List.of("true"), 80, 24, run, realHost(), TerminalProcess::ensureDone, true, new Sink()));
		assertThrows(NullPointerException.class,
			() -> TerminalProcess.start("generic", List.of("true"), 80, 24, run, (TerminalPrompts)null));
	}

	@Test void a05_closeEndsRunnerCancelled() throws Exception {
		var sink = new Sink();
		var p = startReal(List.of("python3", "-c", "import time; time.sleep(60)"), sink);
		for (var i = 0; i < 2400 && ! Files.exists(p.log()); i++)
			Thread.sleep(25);
		var t0 = System.nanoTime();
		p.close();
		assertTrue(System.nanoTime() - t0 < TimeUnit.SECONDS.toNanos(10), "close() took too long");
		assertTrue(p.isDone());
		var lines = Files.readAllLines(p.eventsFile());
		assertEquals("{\"ev\":\"done\",\"status\":\"cancelled\"}", lines.get(lines.size() - 1));
		assertTrue(sink.ended.await(10, TimeUnit.SECONDS));
	}

	@Test void a06_answerAfterEndReturnsFalse() throws Exception {
		var sink = new Sink();
		try (var p = startReal(List.of("python3", "-c", "print('hi')"), sink)) {
			assertEquals(0, p.waitFor(60, TimeUnit.SECONDS));
			assertTrue(sink.ended.await(10, TimeUnit.SECONDS));
			assertFalse(p.acceptsAnswers());
			assertFalse(p.answer(ANSWER));
		}
	}
}
