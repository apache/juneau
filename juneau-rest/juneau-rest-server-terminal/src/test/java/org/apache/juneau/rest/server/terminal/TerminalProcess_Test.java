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
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.logging.*;
import org.apache.juneau.rest.server.runreport.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.terminal.TerminalProcess.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.*;

class TerminalProcess_Test extends TestBase {

	@TempDir Path dir;

	private Path bin, run;

	@BeforeEach void setup() throws Exception {
		bin = Files.createDirectories(dir.resolve("bin"));
		run = dir.resolve("run");
	}

	/** Writes a fake python3 that records its argv (one per line) next to itself, then runs the given shell body. */
	private void fakePython(String body) throws Exception {
		var p = bin.resolve("python3");
		Files.writeString(p, "#!/bin/sh\nfor a in \"$@\"; do printf '%s\\n' \"$a\"; done > \"$(dirname \"$0\")/argv.txt\"\n" + body + "\n");
		Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
	}

	private Host host(String prop, String env) {
		return new Host("Mac OS X", prop, env, bin.toString());
	}

	private static List<String> events(Path f) throws Exception {
		return Files.readAllLines(f);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void a01_argvOrder() throws Exception {
		fakePython("exit 0");
		var p = TerminalProcess.start("maven", List.of("mvn", "-q", "test"), 100, 30, run, host("/x/juneau_run.py", null));
		assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
		var log = run.resolve(TerminalProcess.LOG_NAME).toString();
		var ev = run.resolve(TerminalProcess.EVENTS_NAME).toString();
		assertEquals(List.of("/x/juneau_run.py", "--console", "none", "--pty", "--size", "100x30", "--full-log", log,
			"--events", ev, "maven", "--", "mvn", "-q", "test"), Files.readAllLines(bin.resolve("argv.txt")));
		assertEquals("{\"cols\":100,\"rows\":30}", Files.readString(run.resolve(TerminalProcess.LOG_NAME + ".size")));
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void a02_scriptFromPropertyThenEnv() throws Exception {
		fakePython("exit 0");
		TerminalProcess.start("generic", List.of("true"), 80, 24, run.resolve("1"), host("/prop.py", "/env.py")).waitFor(30, TimeUnit.SECONDS);
		assertEquals("/prop.py", Files.readAllLines(bin.resolve("argv.txt")).get(0));
		TerminalProcess.start("generic", List.of("true"), 80, 24, run.resolve("2"), host(null, "/env.py")).waitFor(30, TimeUnit.SECONDS);
		assertEquals("/env.py", Files.readAllLines(bin.resolve("argv.txt")).get(0));
		var e = assertThrows(IllegalStateException.class, () -> TerminalProcess.start("generic", List.of("true"), 80, 24, run.resolve("3"), host(null, null)));
		assertTrue(e.getMessage().contains(TerminalProcess.SCRIPT_PROPERTY) && e.getMessage().contains(TerminalProcess.SCRIPT_ENV), e.getMessage());
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void a03_missingPython() {
		assertThrowsWithMessage(IllegalStateException.class, "python3", () -> TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null)));
	}

	@Test void a04_windowsRefused() {
		var h = new Host("Windows 11", "/x.py", null, bin.toString());
		assertThrows(UnsupportedOperationException.class, () -> TerminalProcess.start("generic", List.of("true"), 80, 24, run, h));
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void a05_badArguments() throws Exception {
		fakePython("exit 0");
		var h = host("/x.py", null);
		assertThrowsWithMessage(IllegalArgumentException.class, "tool must be one of", () -> TerminalProcess.start("make", List.of("true"), 80, 24, run, h));
		assertThrowsWithMessage(IllegalArgumentException.class, "cmd", () -> TerminalProcess.start("generic", List.of(), 80, 24, run, h));
		assertThrowsWithMessage(IllegalArgumentException.class, "size", () -> TerminalProcess.start("generic", List.of("true"), 0, 24, run, h));
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void b01_missingDoneIsAppended() throws Exception {
		fakePython("printf '{\"ev\":\"step\",\"id\":\"a\",\"title\":\"A\"}\\n{\"ev\":\"note\",\"level\":\"info\",\"te' > \"${10}\"\nexit 5");
		var p = TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null));
		assertEquals(5, p.waitFor(30, TimeUnit.SECONDS));
		assertTrue(p.isDone());
		var lines = events(p.eventsFile());
		assertEquals(4, lines.size(), lines.toString());
		assertEquals("{\"ev\":\"note\",\"level\":\"warn\",\"text\":\"runner exited without done, exit 5\"}", lines.get(2));
		assertEquals("{\"ev\":\"done\",\"status\":\"fail\"}", lines.get(3));
		var page = new RunViewPage[1];
		var records = LogRecordCapture.quietly(FileRunViewSource.class, () -> page[0] = p.events().page(null, 100));
		assertEquals(1, records.size(), records::toString);
		assertTrue(records.get(0).getMessage().contains("skipped malformed line 2"), records.get(0).getMessage());
		assertTrue(page[0].terminal());
		assertEquals(RunEvent.Kind.DONE, page[0].events().get(page[0].events().size() - 1).kind());
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void b02_existingDoneIsKept() throws Exception {
		fakePython("printf '{\"ev\":\"done\",\"status\":\"ok\"}\\n' > \"${10}\"\nexit 0");
		var p = TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null));
		assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
		assertEquals(List.of("{\"ev\":\"done\",\"status\":\"ok\"}"), events(p.eventsFile()));
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void b03_sourcesShareTheDoneFlag() throws Exception {
		var go = bin.resolve("go");
		fakePython("printf 'hi' > \"$8\"\nwhile [ ! -e '" + go + "' ]; do sleep 0.05; done\nexit 0");
		var p = TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null));
		assertFalse(p.terminal().read(0, 10).done());
		assertFalse(p.isDone());
		Files.writeString(go, "");
		p.waitFor(30, TimeUnit.SECONDS);
		assertTrue(p.terminal().read(2, 10).done());
		assertTrue(p.events().page(null, 10).terminal());
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void a06_cmdIsATokenListNotAShellString() throws Exception {
		fakePython("exit 0");
		var cmd = List.of("echo", "a; touch pwned1", "$(touch pwned2)", "`touch pwned3`", "x y");
		TerminalProcess.start("generic", cmd, 80, 24, run, host("/x.py", null)).waitFor(30, TimeUnit.SECONDS);
		var argv = Files.readAllLines(bin.resolve("argv.txt"));
		assertEquals(cmd, argv.subList(argv.size() - cmd.size(), argv.size()));
		for (var f : List.of("pwned1", "pwned2", "pwned3"))
			assertFalse(Files.exists(run.resolve(f)) || Files.exists(bin.resolve(f)), f);
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void a07_aUsedRunDirectoryIsRefused() throws Exception {
		fakePython("exit 0");
		var h = host("/x.py", null);
		for (var name : List.of(TerminalProcess.LOG_NAME, TerminalProcess.EVENTS_NAME)) {
			var d = run.resolve(name + ".dir");
			Files.createDirectories(d);
			Files.writeString(d.resolve(name), "x");
			assertThrowsWithMessage(IllegalStateException.class, name + "' is not empty", () -> TerminalProcess.start("generic", List.of("true"), 80, 24, d, h));
			assertFalse(Files.exists(bin.resolve("argv.txt")), "the runner must not have started");
		}
		var empty = run.resolve("empty");
		Files.createDirectories(empty);
		Files.writeString(empty.resolve(TerminalProcess.LOG_NAME), "");
		Files.writeString(empty.resolve(TerminalProcess.EVENTS_NAME), "");
		assertEquals(0, TerminalProcess.start("generic", List.of("true"), 80, 24, empty, h).waitFor(30, TimeUnit.SECONDS));
	}

	@Test void b04_onlyTheTailIsScannedForDone() throws Exception {
		Files.createDirectories(run);
		var f = run.resolve("e.jsonl");
		var note = "{\"ev\":\"note\",\"level\":\"info\",\"text\":\"" + "x".repeat(100) + "\"}\n";
		Files.writeString(f, "{\"ev\":\"done\",\"status\":\"ok\"}\n" + note.repeat(TerminalProcess.TAIL_BYTES / note.length() + 2));
		TerminalProcess.ensureDone(f, 3);
		var lines = Files.readAllLines(f);
		assertEquals("{\"ev\":\"done\",\"status\":\"fail\"}", lines.get(lines.size() - 1));
		var g = run.resolve("g.jsonl");
		Files.writeString(g, note.repeat(TerminalProcess.TAIL_BYTES / note.length() + 2) + "{\"ev\":\"done\",\"status\":\"ok\"}\n");
		var before = Files.size(g);
		TerminalProcess.ensureDone(g, 0);
		assertEquals(before, Files.size(g));
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void b05_exitHookRunsOnADaemonThreadThatIsShutDown() throws Exception {
		fakePython("exit 0");
		var p = TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null));
		p.waitFor(30, TimeUnit.SECONDS);
		var t = p.exitThread();
		assertNotNull(t);
		assertTrue(t.isDaemon());
		assertTrue(t.getName().startsWith("juneau-terminal-exit-"), t.getName());
		assertTrue(p.executor().isShutdown());
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void b06_closeKillsAndWaitsForTheDoneEvent() throws Exception {
		fakePython("exec sleep 300");
		var p = TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null));
		p.close();
		assertTrue(p.isDone());
		assertTrue(p.executor().isShutdown());
		var lines = events(p.eventsFile());
		assertEquals("{\"ev\":\"done\",\"status\":\"fail\"}", lines.get(lines.size() - 1));
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void b07_closeKillsTheWholeProcessTree() throws Exception {
		var pidFile = bin.resolve("pid");
		fakePython("sleep 300 &\necho $! > '" + pidFile + ".tmp'\nmv '" + pidFile + ".tmp' '" + pidFile + "'\nwait");
		var p = TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null));
		long pid = -1;
		try {
			for (var i = 0; i < 200 && ! Files.exists(pidFile); i++)
				Thread.sleep(50);
			assertTrue(Files.exists(pidFile), "the grandchild never started");
			pid = Long.parseLong(Files.readString(pidFile).trim());
			assertTrue(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false), "the grandchild should be running");
			p.close();
			for (var i = 0; i < 100 && ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false); i++)
				Thread.sleep(50);
			assertFalse(ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false), "the grandchild survived close()");
		} finally {
			if (pid > 0)
				ProcessHandle.of(pid).ifPresent(ProcessHandle::destroyForcibly);
			p.close();
		}
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void b08_exitHookFailureStillFinishes() throws Exception {
		fakePython("exit 3");
		DoneWriter boom = (f, code) -> { throw new IllegalStateException("boom"); };
		var p = new TerminalProcess[1];
		var records = LogRecordCapture.quietly(TerminalProcess.class, () -> {
			p[0] = TerminalProcess.start("generic", List.of("true"), 80, 24, run, host("/x.py", null), boom);
			assertEquals(3, p[0].waitFor(30, TimeUnit.SECONDS));
		});
		assertEquals(1, records.size(), records::toString);
		assertTrue(records.get(0).getMessage().contains("Could not append the done event"), records.get(0).getMessage());
		assertTrue(p[0].isDone());
		assertTrue(p[0].executor().isShutdown());
	}

	@DisabledOnOs(OS.WINDOWS)
	@Test void c01_findOnPath() throws Exception {
		fakePython("exit 0");
		assertEquals(bin.resolve("python3"), TerminalProcess.findOnPath("python3", "/nonexistent" + java.io.File.pathSeparator + bin));
		assertNull(TerminalProcess.findOnPath("python3", null));
		assertNull(TerminalProcess.findOnPath("nope", bin.toString()));
		Files.writeString(bin.resolve("plain"), "not executable");
		assertNull(TerminalProcess.findOnPath("plain", bin.toString()));
	}
}
