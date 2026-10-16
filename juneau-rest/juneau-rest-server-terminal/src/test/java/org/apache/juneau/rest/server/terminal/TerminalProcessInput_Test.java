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
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.terminal.TerminalProcess.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.io.*;

/**
 * {@link TerminalProcess} input mode, over a fake {@code python3} runner, and over the real {@code juneau_run.py} for
 * the bytes reaching the job.
 */
@DisabledOnOs(OS.WINDOWS)
class TerminalProcessInput_Test extends TestBase {

	private static final int LIMIT = 64 * 1024;

	@TempDir Path dir;

	private Path bin, run, pidFile;

	@BeforeEach void setup() throws Exception {
		bin = Files.createDirectories(dir.resolve("bin"));
		run = dir.resolve("run");
		pidFile = bin.resolve("pid");
	}

	/** Writes a fake python3 that records its argv (one per line) next to itself, then runs the given shell body. */
	private void fakePython(String body) throws Exception {
		var p = bin.resolve("python3");
		Files.writeString(p, "#!/bin/sh\nfor a in \"$@\"; do printf '%s\\n' \"$a\"; done > \"$(dirname \"$0\")/argv.txt\"\n" + body + "\n");
		Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rwxr-xr-x"));
	}

	/** A fake runner that writes its pid once ready and then sleeps without reading stdin. */
	private void sleepingRunner(String before) throws Exception {
		fakePython(before + "echo $$ > '" + pidFile + ".tmp'\nmv '" + pidFile + ".tmp' '" + pidFile + "'\nexec sleep 300");
	}

	private long awaitPid() throws Exception {
		for (var i = 0; i < 200 && ! Files.exists(pidFile); i++)
			Thread.sleep(25);
		assertTrue(Files.exists(pidFile), "the fake runner never got ready");
		return Long.parseLong(Files.readString(pidFile).trim());
	}

	private Host host() {
		return new Host("Mac OS X", "/x/juneau_run.py", null, bin.toString());
	}

	private TerminalProcess start(boolean input) throws Exception {
		return TerminalProcess.start("maven", List.of("mvn", "-q", "test"), 100, 30, run, host(), input);
	}

	private static Thread inputThread(long pid) {
		var name = "juneau-terminal-input-" + pid;
		return Thread.getAllStackTraces().keySet().stream().filter(t -> t.getName().equals(name)).findFirst().orElse(null);
	}

	private List<String> expectedArgv(String... extra) {
		var argv = new ArrayList<>(List.of("/x/juneau_run.py", "--console", "none", "--pty", "--size", "100x30",
			"--full-log", run.resolve(TerminalProcess.LOG_NAME).toString(), "--events", run.resolve(TerminalProcess.EVENTS_NAME).toString()));
		argv.addAll(List.of(extra));
		argv.addAll(List.of("maven", "--", "mvn", "-q", "test"));
		return argv;
	}

	@Test void a01_argvHasInputFlag() throws Exception {
		fakePython("exit 0");
		try (var p = start(true)) {
			assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
		}
		var argv = Files.readAllLines(bin.resolve("argv.txt"));
		assertEquals(expectedArgv("--input", "stdin"), argv);
		assertEquals(run.resolve(TerminalProcess.EVENTS_NAME).toString(), argv.get(9), "${10} is still the events file");
	}

	@Test void a02_argvWithoutInputUnchanged() throws Exception {
		fakePython("exit 0");
		try (var p = start(false)) {
			assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
		}
		assertEquals(expectedArgv(), Files.readAllLines(bin.resolve("argv.txt")));
		fakePython("exit 0");
		try (var p = TerminalProcess.start("maven", List.of("mvn", "-q", "test"), 100, 30, dir.resolve("run2"), host())) {
			assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
			assertFalse(p.acceptsInput(), "the old overloads start with input off");
		}
		assertFalse(Files.readAllLines(bin.resolve("argv.txt")).contains("--input"));
	}

	@Test void a03_acceptsInputOnlyWhenEnabled() throws Exception {
		sleepingRunner("");
		try (var p = start(false)) {
			awaitPid();
			assertFalse(p.acceptsInput());
			assertFalse(p.write(new byte[]{'x'}));
		}
		Files.delete(pidFile);
		run = dir.resolve("run2");
		try (var p = start(true)) {
			awaitPid();
			assertTrue(p.acceptsInput());
			assertTrue(p.write(new byte[0]), "an empty write reports acceptsInput()");
			p.close();
			assertFalse(p.acceptsInput());
			assertFalse(p.write(new byte[0]));
			assertFalse(p.write(new byte[]{'x'}));
		}
	}

	/** Starts the real {@code juneau_run.py} in input mode, or skips the test when {@code python3} is not on the PATH. */
	private TerminalProcess startReal(List<String> cmd) throws Exception {
		var path = System.getenv("PATH");
		assumeTrue(TerminalProcess.findOnPath("python3", path) != null, "python3 not on the PATH - skipped");
		var host = new Host(System.getProperty("os.name"), TerminalProcessPty_Test.script().toString(), null, path);
		return TerminalProcess.start("generic", cmd, 80, 24, run, host, true);
	}

	/** The job's log so far, as text. */
	private static String log(TerminalProcess p) throws Exception {
		return new String(p.terminal().read(0, 1 << 20).bytes(), UTF_8);
	}

	/**
	 * Waits up to 60 s for the job to print its ready marker, so input is not sent before it has its terminal.
	 *
	 * <p>
	 * Starting the runner and the job can take several seconds on a loaded machine (a parallel build); the wait ends as
	 * soon as the marker shows, so the limit only matters when something is really wrong.
	 */
	private static void awaitLog(TerminalProcess p, String marker) throws Exception {
		for (var i = 0; i < 2400 && ! log(p).contains(marker); i++)
			Thread.sleep(25);
		assertTrue(log(p).contains(marker), () -> "never printed " + marker + ": " + uncheckedLog(p));
	}

	private static String uncheckedLog(TerminalProcess p) {
		try {
			return log(p);
		} catch (Exception e) {
			return e.toString();
		}
	}

	@Test void a04_writeReachesJob() throws Exception {
		try (var p = startReal(List.of("sh", "-c", "echo READY; read a; echo \"got $a\""))) {
			awaitLog(p, "READY");
			assertTrue(p.write("y\n".getBytes(UTF_8)));
			assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
			assertTrue(log(p).contains("got y"), () -> uncheckedLog(p));
		}
	}

	@Test void a05_ctrlC() throws Exception {
		try (var p = startReal(List.of("sh", "-c", "echo READY; exec sleep 30"))) {
			awaitLog(p, "READY");
			assertTrue(p.write(new byte[]{3}));
			assertEquals(130, p.waitFor(5, TimeUnit.SECONDS), "^C through the terminal's line discipline is SIGINT");
		}
	}

	/** The runner started with the given signal ignored, as a background job of a non-interactive shell has it: the key must still reach the job. */
	private void keyWithSignalIgnoredForRunner(String signal, int key, int expectedExit) throws Exception {
		var real = TerminalProcess.findOnPath("python3", System.getenv("PATH"));
		assumeTrue(real != null, "python3 not on the PATH - skipped");
		fakePython("trap '' " + signal + "\nexec '" + real + "' \"$@\"");
		var host = new Host(System.getProperty("os.name"), TerminalProcessPty_Test.script().toString(), null, bin.toString());
		try (var p = TerminalProcess.start("generic", List.of("sh", "-c", "echo READY; exec sleep 30"), 80, 24, run, host, true)) {
			awaitLog(p, "READY");
			assertTrue(p.write(new byte[]{(byte)key}));
			assertEquals(expectedExit, p.waitFor(5, TimeUnit.SECONDS), "the key reaches the job although the runner inherited an ignored " + signal);
		}
	}

	@Test void a05b_ctrlCWithSigintIgnoredForRunner() throws Exception {
		keyWithSignalIgnoredForRunner("INT", 3, 130);
	}

	@Test void a05c_ctrlBackslashWithSigquitIgnoredForRunner() throws Exception {
		keyWithSignalIgnoredForRunner("QUIT", 0x1c, 131);
	}

	@Test void a06_bufferBound() throws Exception {
		sleepingRunner("");
		try (var p = start(true)) {
			awaitPid();
			var piece = new byte[4096];
			var refused = false;
			// The writer drains into the OS pipe (up to about 64 KiB) before the 64 KiB pending buffer fills.
			for (var sent = 0; sent < 32 * LIMIT && ! refused; sent += piece.length)
				refused = ! p.write(piece);
			assertTrue(refused, "a job that never reads must eventually make write return false");
			assertTrue(p.acceptsInput(), "busy is not a failure");
			assertFalse(p.write(new byte[LIMIT + 1]), "more than the whole buffer is never taken");
		}
	}

	@Test void a07_writeAfterExitIsFalse() throws Exception {
		fakePython("exit 0");
		try (var p = start(true)) {
			assertEquals(0, p.waitFor(30, TimeUnit.SECONDS));
			assertFalse(p.acceptsInput());
			assertFalse(p.write("y\n".getBytes(UTF_8)));
		}
	}

	@Test void a08_noWriterWithoutInput() throws Exception {
		sleepingRunner("");
		try (var p = start(false)) {
			assertNull(inputThread(awaitPid()));
		}
	}

	@Test void a09_writeDuringClose() throws Exception {
		sleepingRunner("");
		try (var p = start(true)) {
			var pid = awaitPid();
			var writer = inputThread(pid);
			assertNotNull(writer, "an input process has a writer thread");
			assertTrue(writer.isDaemon());
			var closed = new AtomicBoolean();
			var stop = new AtomicBoolean();
			var busy = new CountDownLatch(1);
			var failures = new ConcurrentLinkedQueue<String>();
			var t = new Thread(() -> {
				var piece = new byte[1024];
				// Bounded: without the buffer limit this would queue without end.
				for (var accepted = 0L; ! stop.get() && accepted < 32L * LIMIT;) {
					var after = closed.get();
					try {
						var ok = p.write(piece);
						if (ok)
							accepted += piece.length;
						else
							busy.countDown();
						if (after && ok)
							failures.add("write returned true after close()");
					} catch (Throwable e) {
						failures.add(e.toString());
					}
				}
			});
			t.start();
			try {
				// Full pending buffer: the writer thread is blocked on the full pipe when close() runs.
				assertTrue(busy.await(10, TimeUnit.SECONDS), "the buffer never filled");
				p.close();
				closed.set(true);
				for (var i = 0; i < 40 && writer.isAlive(); i++)
					Thread.sleep(25);
				assertFalse(writer.isAlive(), "the writer thread outlived close() by 1 s");
				Thread.sleep(100);
			} finally {
				stop.set(true);
				t.join(5000);
			}
			assertEquals(List.of(), new ArrayList<>(failures));
			assertFalse(p.acceptsInput());
		}
	}

	@Test void a10_brokenPipe() throws Exception {
		sleepingRunner("exec 0<&-\n");
		try (var p = start(true)) {
			awaitPid();
			assertTrue(p.acceptsInput());
			assertTrue(p.write(new byte[]{'x'}), "the write is queued; the broken pipe is the writer thread's to find");
			for (var i = 0; i < 200 && p.acceptsInput(); i++)
				Thread.sleep(25);
			assertFalse(p.acceptsInput(), "a broken pipe turns input off");
			assertFalse(p.write(new byte[]{'y'}));
		}
	}

	/**
	 * With input off, {@link TerminalProcess#close()} only kills.
	 *
	 * <p>
	 * HTT: the "never touches stdin" half is covered by code review only.  The JDK closes a process's stdin itself when
	 * the process exits, so from outside it looks the same either way, and no test seam is allowed for it.
	 */
	@Test void a11_closeWithoutInputLeavesStdin() throws Exception {
		sleepingRunner("");
		try (var p = start(false)) {
			awaitPid();
			p.close();
			assertTrue(p.isDone());
			assertTrue(p.executor().isShutdown());
			var lines = Files.readAllLines(p.eventsFile());
			assertEquals("{\"ev\":\"done\",\"status\":\"fail\"}", lines.get(lines.size() - 1));
			assertFalse(p.acceptsInput());
		}
	}
}
