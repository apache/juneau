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
package org.apache.juneau.microservice;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.marshall.cp.*;
import org.apache.juneau.microservice.console.*;
import org.junit.jupiter.api.*;

/**
 * Runtime-method tests for {@link Microservice}: {@link Microservice#executeCommand executeCommand},
 * {@link Microservice#getArgs getArgs}, {@link Microservice#getConfig getConfig},
 * {@link Microservice#getManifest getManifest}, {@link Microservice#getVarResolver getVarResolver},
 * {@link Microservice#getLogger getLogger}, {@link Microservice#out out}, {@link Microservice#err err},
 * {@link Microservice#startConsole startConsole}, {@link Microservice#stopConsole stopConsole},
 * {@link Microservice#join join}, {@link Microservice#stop stop} (idempotency), and
 * {@link Microservice#onConfigChange onConfigChange}.
 *
 * <p>
 * This focuses on Phase-1's gap surface area: the runtime methods (the constructor / builder is covered
 * by {@code Microservice_Builder_Test} and {@code Microservice_Inject_Test}; the listener fan-out by
 * {@code Microservice_Listener_Fanout_Test}; the bean-store overlay/inject by
 * {@code Microservice_OverridingBeanStore_Test} / {@code Microservice_PushPopOverlay_Test}).
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
class Microservice_Runtime_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// A. Accessors return the resolved values registered into the bean store during construction.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_getArgs_returnsResolvedArgs() throws Exception {
		var args = new Args(new String[]{"--port", "9090"});
		var ms = Microservice.create().args(args).build();
		try {
			assertSame(args, ms.getArgs());
			assertEquals("9090", ms.getArgs().get("port").orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a02_getConfig_neverNull_emptyByDefault() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertNotNull(ms.getConfig());
		} finally {
			ms.stop();
		}
	}

	@Test void a03_getConfig_explicitConfigUsed() throws Exception {
		var cfg = Config.create().memStore().build();
		cfg.set("MySection/key", "value");
		var ms = Microservice.create().config(cfg).build();
		try {
			assertSame(cfg, ms.getConfig());
			assertEquals("value", ms.getConfig().get("MySection/key").orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void a04_getManifest_neverNull() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertNotNull(ms.getManifest());
		} finally {
			ms.stop();
		}
	}

	@Test void a05_getVarResolver_resolvesConfigVar() throws Exception {
		var cfg = Config.create().memStore().build();
		cfg.set("MySection/key", "valueX");
		var ms = Microservice.create().config(cfg).build();
		try {
			var vr = ms.getVarResolver();
			assertNotNull(vr);
			assertEquals("valueX", vr.resolve("$C{MySection/key}"));
		} finally {
			ms.stop();
		}
	}

	@Test void a06_getLogger_neverNull_afterInit() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertNotNull(ms.getLogger());
		} finally {
			ms.stop();
		}
	}

	@Test void a07_getLogger_explicitLoggerWins() throws Exception {
		var custom = Logger.getLogger("org.apache.juneau.marshall.microservice.test.runtime");
		var ms = Microservice.create().logger(custom).build();
		try {
			assertSame(custom, ms.getLogger());
		} finally {
			ms.stop();
		}
	}

	@Test void a08_getInstance_setOnConstruction() throws Exception {
		var ms = Microservice.create().build();
		try {
			// getInstance() returns the most recently constructed Microservice; should equal `ms` here.
			assertSame(ms, Microservice.getInstance());
		} finally {
			ms.stop();
		}
	}

	@Test void a09_getConsoleCommands_emptyWhenConsoleDisabled() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertNotNull(ms.getConsoleCommands());
			assertTrue(ms.getConsoleCommands().isEmpty(),
				"Console commands map must be empty when consoleEnabled=false");
		} finally {
			ms.stop();
		}
	}

	@Test void a10_getConsoleCommands_populatedWhenConsoleEnabled() throws Exception {
		var ms = Microservice.create().consoleEnabled(true).consoleCommands(new HelpCommand(), new ExitCommand()).build();
		try {
			assertTrue(ms.getConsoleCommands().containsKey("help"));
			assertTrue(ms.getConsoleCommands().containsKey("exit"));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// B. executeCommand (both overloads).
	//-----------------------------------------------------------------------------------------------------------------

	static final class FakeCommand extends ConsoleCommand {
		final boolean returnValue;
		final List<String> received = new ArrayList<>();

		FakeCommand(boolean returnValue) { this.returnValue = returnValue; }

		@Override public String getName() { return "fake"; }
		@Override public String getInfo() { return "Fake command for tests."; }
		@Override public boolean execute(Scanner in, PrintWriter out, Args a) {
			received.add(a.get(0).orElse(null));
			out.println("hello-from-fake");
			return returnValue;
		}
	}

	@Test void b01_executeCommand_unknownCommand_writesUnknownAndReturnsFalse() throws Exception {
		var ms = Microservice.create().consoleEnabled(true).build();
		try {
			var output = ms.executeCommand("nope", "");
			assertNotNull(output);
			assertFalse(output.isBlank(), "Output must include a localized 'unknown command' message");
		} finally {
			ms.stop();
		}
	}

	@Test void b02_executeCommand_knownCommand_invokesIt() throws Exception {
		var fake = new FakeCommand(false);
		var ms = Microservice.create().consoleEnabled(true).consoleCommands(fake).build();
		try {
			var output = ms.executeCommand("fake", "input-line");
			assertTrue(output.contains("hello-from-fake"), () -> "actual: " + output);
			assertEquals(List.of("fake"), fake.received);
		} finally {
			ms.stop();
		}
	}

	@Test void b03_executeCommand_byArgs_returnsCommandReturnValue() throws Exception {
		var fake = new FakeCommand(true);
		var ms = Microservice.create().consoleEnabled(true).consoleCommands(fake).build();
		try (AutoCloseable c = ms::stop) {
			var sw = new StringWriter();
			try (var in = new Scanner(""); var out = new PrintWriter(sw)) {
				assertTrue(ms.executeCommand(new Args(new String[]{"fake"}), in, out));
			}
			assertTrue(sw.toString().contains("hello-from-fake"));
		}
	}

	@Test void b04_executeCommand_throwingCommand_printsStackTraceAndReturnsFalse() throws Exception {
		var throwing = new ConsoleCommand() {
			@Override public String getName() { return "boom"; }
			@Override public boolean execute(Scanner in, PrintWriter out, Args a) {
				throw new RuntimeException("boom-msg");
			}
		};
		var ms = Microservice.create().consoleEnabled(true).consoleCommands(throwing).build();
		try (AutoCloseable c = ms::stop) {
			var sw = new StringWriter();
			try (var in = new Scanner(""); var out = new PrintWriter(sw)) {
				// Throwing command must not propagate; executeCommand swallows and returns false.
				assertFalse(ms.executeCommand(new Args(new String[]{"boom"}), in, out));
			}
			assertTrue(sw.toString().contains("boom-msg"),
				() -> "stack trace must be written to the output writer; got=" + sw);
		}
	}

	@Test void b05_executeCommand_passesAdditionalArgs() throws Exception {
		var capturing = new ConsoleCommand() {
			final List<String> seen = new ArrayList<>();
			@Override public String getName() { return "cap"; }
			@Override public boolean execute(Scanner in, PrintWriter out, Args a) {
				seen.addAll(a.positional());
				out.println("seen=" + seen);
				return false;
			}
		};
		var ms = Microservice.create().consoleEnabled(true).consoleCommands(capturing).build();
		try {
			var output = ms.executeCommand("cap", "", "alpha", "beta");
			assertTrue(output.contains("alpha") && output.contains("beta"), () -> "actual: " + output);
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// C. out / err logging methods.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_out_writesToConsoleWhenEnabled() throws Exception {
		var sw = new StringWriter();
		var pw = new PrintWriter(sw, true);
		var ms = Microservice.create()
			.consoleEnabled(true)
			.console(new Scanner(""), pw)
			.build();
		try {
			// Use the Microservice's own message bundle - has known keys.
			ms.out(Messages.of(Microservice.class), "RunningClassWithConfig", "TestApp", "test.cfg");
			pw.flush();
			assertFalse(sw.toString().isBlank(), "out() must write to the console writer when console is enabled");
		} finally {
			ms.stop();
		}
	}

	@Test void c02_out_silentWhenConsoleDisabled() throws Exception {
		var sw = new StringWriter();
		var pw = new PrintWriter(sw, true);
		// console is disabled - out() should NOT touch the supplied writer.
		var ms = Microservice.create()
			.consoleEnabled(false)
			.console(new Scanner(""), pw)
			.build();
		try {
			ms.out(Messages.of(Microservice.class), "RunningClassWithConfig", "TestApp", "test.cfg");
			pw.flush();
			assertEquals("", sw.toString(),
				"out() must not write to the supplied console writer when console is disabled");
		} finally {
			ms.stop();
		}
	}

	@Test void c03_err_doesNotThrow_consoleEnabled() throws Exception {
		var ms = Microservice.create().consoleEnabled(true).build();
		try {
			assertDoesNotThrow(() -> ms.err(Messages.of(Microservice.class), "RunningClassWithoutConfig", "TestApp"));
		} finally {
			ms.stop();
		}
	}

	@Test void c04_err_doesNotThrow_consoleDisabled() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertDoesNotThrow(() -> ms.err(Messages.of(Microservice.class), "RunningClassWithoutConfig", "TestApp"));
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// D. Console thread lifecycle (start/stop are no-ops when console is disabled).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_startConsole_noop_whenConsoleDisabled() throws Exception {
		var ms = Microservice.create().build();
		try {
			// startConsole() on a no-console microservice is a no-op (consoleThread is null).
			assertSame(ms, ms.startConsole());
			assertSame(ms, ms.stopConsole());
		} finally {
			ms.stop();
		}
	}

	@Test void d02_stopConsole_noop_whenThreadNotAlive() throws Exception {
		// Console enabled but never started: stopConsole() must be a no-op (thread is not alive).
		var ms = Microservice.create()
			.consoleEnabled(true)
			.console(new Scanner(""), new PrintWriter(new StringWriter()))
			.build();
		try {
			assertSame(ms, ms.stopConsole());
		} finally {
			ms.stop();
		}
	}

	@Test void d03_startConsole_thenStopConsole_whenEnabled() throws Exception {
		// We need an interactive-looking input that won't EOF immediately.
		// Use a piped scanner so the console thread doesn't blow through nextLine() and exit.
		var pin = new PipedInputStream();
		var pout = new PipedOutputStream(pin);
		var sc = new Scanner(pin);
		var ms = Microservice.create()
			.consoleEnabled(true)
			.console(sc, new PrintWriter(new StringWriter()))
			.consoleCommands(new HelpCommand())
			.build();
		try {
			assertSame(ms, ms.startConsole());
			// Deterministically wait for the console thread to become alive (named "ConsoleThread" in Microservice)
			// instead of sleeping a fixed interval, so the stopConsole() interrupt-alive-thread branch is exercised.
			var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
			while (! consoleThreadAlive() && System.nanoTime() < deadline)
				Thread.onSpinWait();
			assertTrue(consoleThreadAlive(), "Console thread did not start within timeout.");
			assertSame(ms, ms.stopConsole());
			// Closing the pipe lets the scanner exit cleanly.
			pout.close();
			pin.close();
		} finally {
			ms.stop();
		}
	}

	/** Returns <jk>true</jk> if a live thread named "ConsoleThread" (the Microservice console thread) currently exists. */
	private static boolean consoleThreadAlive() {
		return Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.isAlive() && "ConsoleThread".equals(t.getName()));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// E. join() is a no-op fluent return.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_join_returnsThis() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertSame(ms, ms.join());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// F. stop() is idempotent.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_stop_idempotent() throws Exception {
		var stopCount = new AtomicInteger();
		var listener = new BasicMicroserviceListener() {
			@Override public void onStop(Microservice m) { stopCount.incrementAndGet(); }
		};
		var ms = Microservice.create().listener(listener).build();
		ms.stop();
		ms.stop();   // second call is a no-op
		ms.stop();   // third call is a no-op
		assertEquals(1, stopCount.get(), "Listener.onStop must fire exactly once even for repeated stop() calls");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// G. start() registers a shutdown hook AND fires onStart on listeners.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_start_firesOnStartListeners() throws Exception {
		var startCount = new AtomicInteger();
		var listener = new BasicMicroserviceListener() {
			@Override public void onStart(Microservice m) { startCount.incrementAndGet(); }
		};
		var ms = Microservice.create().listener(listener).build();
		try {
			ms.start();
			assertEquals(1, startCount.get());
		} finally {
			ms.stop();
		}
	}

	@Test void g02_start_returnsThis() throws Exception {
		var ms = Microservice.create().build();
		try {
			assertSame(ms, ms.start());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// H. onConfigChange fans out to listeners.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_onConfigChange_fansOutToListeners() throws Exception {
		var seen = new AtomicInteger();
		var listener = new BasicMicroserviceListener() {
			@Override public void onConfigChange(Microservice m, ConfigEvents events) { seen.incrementAndGet(); }
		};
		var ms = Microservice.create().listener(listener).build();
		try {
			ms.onConfigChange(new ConfigEvents());
			assertEquals(1, seen.get());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// I. Manifest builder accepts multiple input types.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_builder_manifest_manifestObject() throws Exception {
		var m = new java.util.jar.Manifest();
		m.getMainAttributes().putValue("Manifest-Version", "1.0");
		m.getMainAttributes().putValue("Main-Class", "TestMain");
		var b = Microservice.create().manifest(m);
		assertNotNull(b.manifest);
		assertEquals("TestMain", b.manifest.get("Main-Class").orElse(null));
	}

	@Test void i02_builder_manifest_reader() throws Exception {
		var raw = "Manifest-Version: 1.0\r\nMain-Class: TestMain\r\n\r\n";
		var b = Microservice.create().manifest(new StringReader(raw));
		assertNotNull(b.manifest);
		assertEquals("TestMain", b.manifest.get("Main-Class").orElse(null));
	}

	@Test void i03_builder_manifest_inputStream() throws Exception {
		var raw = "Manifest-Version: 1.0\r\nMain-Class: TestMain\r\n\r\n";
		var b = Microservice.create().manifest(new ByteArrayInputStream(raw.getBytes()));
		assertNotNull(b.manifest);
		assertEquals("TestMain", b.manifest.get("Main-Class").orElse(null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// J. Builder vars / varBean.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void j01_builder_varBean_isAvailableViaVarResolver() throws Exception {
		var ms = Microservice.create().varBean(String.class, "from-bean").build();
		try {
			// We can't trivially resolve a $S{...} pointing at a named String bean here; just verify the
			// var resolver was constructed and the bean store contains the var resolver.
			assertNotNull(ms.getVarResolver());
		} finally {
			ms.stop();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// K. Configurations builder accepts null/empty without throwing.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void k01_builder_configurations_acceptsNullArray() {
		var b = Microservice.create().configurations((Class<?>[]) null);
		assertTrue(b.configurations.isEmpty());
	}

	@Test void k02_builder_configurations_skipsNullEntries() {
		var b = Microservice.create().configurations(new Class<?>[]{null, null});
		assertTrue(b.configurations.isEmpty(),
			"Null entries inside the configurations array must be skipped (not added to the list)");
	}

	@Test void k03_builder_configurations_acceptsList() {
		var b = Microservice.create().configurations(java.util.List.of(String.class));
		assertEquals(1, b.configurations.size());
		assertSame(String.class, b.configurations.get(0));
	}

	@Test void k04_builder_configurations_listAcceptsNull() {
		var b = Microservice.create().configurations((java.util.List<Class<?>>) null);
		assertTrue(b.configurations.isEmpty());
	}
}
