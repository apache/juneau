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
import java.nio.file.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.runtime.*;
import org.apache.juneau.commons.svl.vars.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.microservice.console.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Phase-3 (Tier H14, second pass) lifecycle tests for {@link Microservice}.
 *
 * <p>
 * Phase 2 already covered most builder/runtime surface area; this class targets the residual
 * uncovered branches concentrated around:
 * <ul>
 *   <li>Builder methods left untested in Phase 2 ({@code configStore}, {@code consoleCommands(Class...)} catch path,
 *       {@code manifest(File|Path|String|Class)}, {@code vars(Class...)}, {@code configurations(List)} null-entry skip).
 *   <li>Constructor manifest-fallback file-system path (workingDir with META-INF/MANIFEST.MF on disk).
 *   <li>Constructor config-resolution branches: {@code Main-Config} from manifest, {@code configFile} cmdline arg,
 *       explicit {@code configStore} hit/miss, {@code workingDir}-scoped {@link FileStore}.
 *   <li>Console-mode init paths: builder.consoleWriter==null fallback to {@code createLoggerConsoleWriter()},
 *       {@code Console/commands} config string array (success + bad-class catch).
 *   <li>{@code init()} with {@code Logging/logFile} set so the file-handler / console-handler path runs, plus
 *       {@code Logging/levels} and {@code SystemProperties} sections.
 *   <li>{@code start()} {@code RunningClassWithConfig} branch (config has a name).
 *   <li>{@code stop()} {@code beanStore.close()} catch path (a {@code @PreDestroy} that throws).
 *   <li>{@code getCandidateConfigNames}: {@code configFile} cmdline arg branch and {@code Main-Config} manifest branch.
 *   <li>Protected {@code resolveFile} on the constructed microservice (absolute and relative+workingDir branches).
 *   <li>{@code createLoggerConsoleWriter} writer mechanics: write/flush/close routed through the buffered logger.
 * </ul>
 *
 * <p>
 * Tests are kept tests-only: production behaviour is asserted as-is.  Two paths intentionally not exercised here:
 * {@link Microservice#exit()} and {@link Microservice#kill()} both call {@link System#exit(int)} unconditionally and
 * are not safely reachable from a unit test without bringing down the JVM.
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
class Microservice_Lifecycle_Test extends TestBase {

	// =================================================================================================================
	// A.  Builder methods that were missed in Phase 2.
	// =================================================================================================================

	@Test void a01_builder_configStore_setsField() {
		var store = MemoryStore.create().build();
		var b = Microservice.create().configStore(store);
		assertSame(store, b.configStore);
	}

	/**
	 * Pins the catch path in {@link Microservice.Builder#consoleCommands(Class...)} when the supplied class has no
	 * accessible no-arg constructor.  The builder must wrap the reflective failure as an {@link ExecutableException}.
	 */
	public static class NoNoargConsoleCommand extends ConsoleCommand {
		public NoNoargConsoleCommand(@SuppressWarnings("unused") String requiredArg) {}  // intentionally no no-arg ctor
		@Override public String getName() { return "no-noarg"; }
		@Override public boolean execute(Scanner in, PrintWriter out, Args a) { return false; }
	}

	@SuppressWarnings({"unchecked"})
	@Test void a02_builder_consoleCommands_classes_throwsForNoNoargCtor() {
		assertThrows(ExecutableException.class,
			() -> Microservice.create().consoleCommands(NoNoargConsoleCommand.class));
	}

	@Test void a03_builder_manifest_file(@TempDir Path tmp) throws Exception {
		var f = tmp.resolve("MANIFEST.MF").toFile();
		try (var w = new FileWriter(f)) {
			w.write("Manifest-Version: 1.0\r\nMain-Class: FromFile\r\n\r\n");
		}
		var b = Microservice.create().manifest(f);
		assertNotNull(b.manifest);
		assertEquals("FromFile", b.manifest.get("Main-Class").orElse(null));
	}

	@Test void a04_builder_manifest_path(@TempDir Path tmp) throws Exception {
		var p = tmp.resolve("MANIFEST.MF");
		Files.writeString(p, "Manifest-Version: 1.0\r\nMain-Class: FromPath\r\n\r\n");
		var b = Microservice.create().manifest(p);
		assertNotNull(b.manifest);
		assertEquals("FromPath", b.manifest.get("Main-Class").orElse(null));
	}

	@Test void a05_builder_manifest_stringPath(@TempDir Path tmp) throws Exception {
		var f = tmp.resolve("MANIFEST.MF").toFile();
		try (var w = new FileWriter(f)) {
			w.write("Manifest-Version: 1.0\r\nMain-Class: FromString\r\n\r\n");
		}
		var b = Microservice.create().manifest(f.getAbsolutePath());
		assertNotNull(b.manifest);
		assertEquals("FromString", b.manifest.get("Main-Class").orElse(null));
	}

	@Test
	@SuppressWarnings({
		"unchecked" // Class<? extends Var>[] varargs; generic array creation is safe here.
	})
	void a06_builder_vars_varargs() {
		// vars(Class...) merely chains into the underlying VarResolver builder.  We just need to call it without
		// throwing - subsequent build() should succeed.
		var b = Microservice.create().vars(ArgsVar.class);
		assertNotNull(b);
	}

	@Test void a07_builder_configurations_listSkipsNullEntries() {
		var l = new ArrayList<Class<?>>();
		l.add(null);
		l.add(null);
		var b = Microservice.create().configurations(l);
		assertTrue(b.configurations.isEmpty(),
			"Null entries inside the configurations List must be skipped (not added to the list)");
	}

	// =================================================================================================================
	// B.  Constructor: configStore branches and workingDir-scoped FileStore.
	// =================================================================================================================

	@Test void b01_explicitConfigStore_hit() throws Exception {
		var store = MemoryStore.create().build();
		store.update("test.cfg", "[MySec]\nkey = found\n");
		var ms = Microservice.create()
			.configStore(store)
			.configName("test.cfg")
			.build();
		try {
			assertEquals("test.cfg", ms.getConfig().getName());
			assertEquals("found", ms.getConfig().get("MySec/key").orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void b02_explicitConfigStore_miss_fallsThroughToEmptyConfig() throws Exception {
		var store = MemoryStore.create().build();
		// Store is empty: store.exists("missing.cfg") is false, so the loop body's `if (store.exists(name))` branch
		// returns false and the configBuilder builds without binding to the store.
		var ms = Microservice.create()
			.configStore(store)
			.configName("missing.cfg")
			.build();
		try {
			assertNotNull(ms.getConfig());
		} finally {
			ms.stop();
		}
	}

	@Test void b03_workingDir_setsFileStoreScope(@TempDir Path tmp) throws Exception {
		// Provide an empty workingDir so the constructor takes the `workingDir != null` branch when constructing
		// `cfs` (line ~783) without finding any candidate config files.
		var ms = Microservice.create()
			.workingDir(tmp.toFile())
			.build();
		try {
			assertNotNull(ms.getConfig());
			// The protected resolveFile (lines 1459-1465) is exercised through workingDir:
			var f = invokeProtectedResolveFile(ms, "child/file.cfg");
			assertTrue(f.getPath().contains("child/file.cfg"));
			assertTrue(f.getPath().startsWith(tmp.toFile().getPath()),
				() -> "resolveFile should anchor to workingDir; got=" + f);
		} finally {
			ms.stop();
		}
	}

	@Test void b04_resolveFile_absoluteIgnoresWorkingDir(@TempDir Path tmp) throws Exception {
		var ms = Microservice.create().workingDir(tmp.toFile()).build();
		try {
			var abs = tmp.resolve("absolute.cfg").toAbsolutePath().toString();
			var f = invokeProtectedResolveFile(ms, abs);
			assertEquals(abs, f.getPath(), "Absolute paths must bypass workingDir");
		} finally {
			ms.stop();
		}
	}

	@Test void b05_resolveFile_relativeWithoutWorkingDir() throws Exception {
		var ms = Microservice.create().build();
		try {
			var f = invokeProtectedResolveFile(ms, "rel/path.cfg");
			assertEquals("rel/path.cfg", f.getPath());
		} finally {
			ms.stop();
		}
	}

	// =================================================================================================================
	// C.  Constructor: manifest fallback file-system path (META-INF/MANIFEST.MF resolves to a file under workingDir).
	// =================================================================================================================

	@Test void c01_manifestFallback_readsFromFileSystem(@TempDir Path tmp) throws Exception {
		// Lay down META-INF/MANIFEST.MF under workingDir so the constructor's `f.exists() && f.canRead()` branch
		// (line ~751) is taken.  The manifest must include a Manifest-Version line so the JDK's parser accepts it.
		var metaInf = tmp.resolve("META-INF");
		Files.createDirectories(metaInf);
		Files.writeString(metaInf.resolve("MANIFEST.MF"),
			"Manifest-Version: 1.0\r\nMain-Class: FromMeta\r\nMain-Config: from-meta.cfg\r\n\r\n");
		var ms = Microservice.create().workingDir(tmp.toFile()).build();
		try {
			assertEquals("FromMeta", ms.getManifest().get("Main-Class").orElse(null));
		} finally {
			ms.stop();
		}
	}

	// =================================================================================================================
	// D.  Constructor: getCandidateConfigNames branches.
	// =================================================================================================================

	@Test void d01_candidateConfig_fromConfigFileArg(@TempDir Path tmp) throws Exception {
		// Provide a Main-Class so the manifest is non-empty, and supply --configFile via Args.  Lay down the cfg
		// in workingDir so the `cfs.exists(name)` branch (line ~794) is taken with the args-provided name.
		Files.writeString(tmp.resolve("from-args.cfg"), "[MySec]\nkey = from-args-value\n");
		var ms = Microservice.create()
			.workingDir(tmp.toFile())
			.args(new Args(new String[]{"--configFile", "from-args.cfg"}))
			.build();
		try {
			assertEquals("from-args.cfg", ms.getConfig().getName());
			assertEquals("from-args-value", ms.getConfig().get("MySec/key").orElse(null));
		} finally {
			ms.stop();
		}
	}

	@Test void d02_candidateConfig_fromMainConfigManifestEntry(@TempDir Path tmp) throws Exception {
		// Lay down a config the manifest will point at via Main-Config.
		Files.writeString(tmp.resolve("from-meta.cfg"), "[MySec]\nkey = from-meta-value\n");
		var m = new Manifest();
		m.getMainAttributes().putValue("Manifest-Version", "1.0");
		m.getMainAttributes().putValue("Main-Config", "from-meta.cfg");
		var ms = Microservice.create()
			.workingDir(tmp.toFile())
			.manifest(m)
			.build();
		try {
			assertEquals("from-meta.cfg", ms.getConfig().getName());
			assertEquals("from-meta-value", ms.getConfig().get("MySec/key").orElse(null));
		} finally {
			ms.stop();
		}
	}

	// =================================================================================================================
	// E.  Console initialization: createLoggerConsoleWriter fallback (System.console() is null in unit tests, no
	//     console reader/writer supplied -> the createLoggerConsoleWriter() branch is taken).
	// =================================================================================================================

	@Test void e01_consoleEnabled_noConsole_writesViaLogger() throws Exception {
		// Capture log records emitted by the fallback logger so we can verify the buffered writer is wired up.
		// init() calls LogManager.reset() which clears handlers, so attach the captor AFTER build().
		var rec = new ArrayList<LogRecord>();
		var captor = new Handler() {
			@Override public void publish(LogRecord r) { rec.add(r); }
			@Override public void flush() { /* no-op */ }
			@Override public void close() throws SecurityException { /* no-op */ }
		};
		var ms = Microservice.create().consoleEnabled(true).build();
		var logger = ms.getLogger();
		logger.addHandler(captor);
		try {
			try {
				// Fire the writer's write() then implicit println() flush by writing through ms.out().
				ms.out(Messages.of(Microservice.class), "RunningClassWithoutConfig", "TestApp");
				// The logger should have at least one info record after println('RunningClassWithoutConfig').
			} finally {
				ms.stop();
			}
			assertFalse(rec.isEmpty(), "createLoggerConsoleWriter should route println() via the fallback logger");
		} finally {
			logger.removeHandler(captor);
		}
	}

	@Test void e02_createLoggerConsoleWriter_writeAndFlushAndClose() throws Exception {
		// Build an enabled-console microservice without supplying our own writer so consoleWriter is the fallback.
		// Then exercise PrintWriter-level write/flush/close so all branches in the inner Writer fire:
		//   - newline char ('\n') -> flushBuffer (line 1414)
		//   - carriage return ('\r') -> skipped (line 1416 false branch)
		//   - other char -> append (line 1417)
		//   - flush() -> flushBuffer
		//   - close() -> flushBuffer
		// init() calls LogManager.reset() which clears handlers, so attach the captor AFTER build().
		var captured = new ArrayList<LogRecord>();
		var captor = new Handler() {
			@Override public void publish(LogRecord r) { captured.add(r); }
			@Override public void flush() { /* no-op */ }
			@Override public void close() throws SecurityException { /* no-op */ }
		};
		var ms = Microservice.create().consoleEnabled(true).build();
		var logger = ms.getLogger();
		logger.addHandler(captor);
		try {
			try {
				// Reach the package-private console writer via reflection on the Microservice class.
				var pwField = Microservice.class.getDeclaredField("consoleWriter");
				pwField.setAccessible(true);
				var pw = (PrintWriter) pwField.get(ms);
				assertNotNull(pw);
				// Mix \r and \n and ordinary chars in a single write to exercise all three branches.
				pw.write("ab\rcd\nef");  // 'ab' buffered, '\r' skipped, 'cd' buffered, '\n' flushes 'abcd', 'ef' buffered
				pw.flush();              // flushes 'ef'
				// At this point we should have logged at least one buffered chunk via the logger.
				assertFalse(captured.isEmpty(), "Inner Writer should have flushed buffered content via the logger");
				// Clearing then closing flushes any residual buffer (empty) -> isEmpty() true branch on flushBuffer.
				captured.clear();
				pw.close();
				// No exception expected; close() with empty buffer is a no-op for the logger.
			} finally {
				ms.stop();
			}
		} finally {
			logger.removeHandler(captor);
		}
	}

	// =================================================================================================================
	// F.  Console initialization: Console/commands config string array path (success + class-not-found catch).
	// =================================================================================================================

	@Test void f01_consoleCommands_fromConfig_loadsCommand() throws Exception {
		var cfg = Config.create().memStore().build();
		// Use HelpCommand's FQN - the constructor's loop calls Class.forName(s).getDeclaredConstructor().newInstance().
		cfg.set("Console/commands", HelpCommand.class.getName());
		var ms = Microservice.create()
			.config(cfg)
			.consoleEnabled(true)
			.console(new Scanner(""), new PrintWriter(new StringWriter()))
			.build();
		try {
			assertTrue(ms.getConsoleCommands().containsKey("help"),
				"HelpCommand referenced via Console/commands config should be registered");
		} finally {
			ms.stop();
		}
	}

	@Test void f02_consoleCommands_fromConfig_invalidClass_writesWarningToConsole() throws Exception {
		var cfg = Config.create().memStore().build();
		// Reference a class that does not exist - hits the catch path (line ~833).
		cfg.set("Console/commands", "org.apache.juneau.microservice.DoesNotExist_$$_Bogus_$$");
		var sw = new StringWriter();
		var ms = Microservice.create()
			.config(cfg)
			.consoleEnabled(true)
			.console(new Scanner(""), new PrintWriter(sw, true))
			.build();
		try {
			// Construction should NOT throw; the warning goes to the console writer.
			assertTrue(sw.toString().contains("Could not create console command"),
				() -> "Expected warning emitted to console writer; got=" + sw);
		} finally {
			ms.stop();
		}
	}

	// =================================================================================================================
	// G.  init(): SystemProperties + Logging/logFile path + Logging/levels.
	// =================================================================================================================

	@Test void g01_init_appliesSystemProperties() throws Exception {
		var key = "juneau.test.lifecycle.g01";
		System.clearProperty(key);
		var cfg = Config.create().memStore().build();
		cfg.set("SystemProperties/" + key, "applied");
		var ms = Microservice.create().config(cfg).build();
		try {
			assertEquals("applied", System.getProperty(key));
		} finally {
			System.clearProperty(key);
			ms.stop();
		}
	}

	@Test void g02_init_logFile_buildsHandlers(@TempDir Path tmp) throws Exception {
		// Configure a log file so the FileHandler / ConsoleHandler / LogEntryFormatter wiring path runs.
		// Use LogConfig overrides so we don't pollute the global log manager beyond this test.
		var lc = LogConfig.create()
			.logDir(tmp.toAbsolutePath().toString())
			.logFile("test.log")
			.append()
			.count(1)
			.limit(64 * 1024)
			.fileLevel(Level.INFO)
			.consoleLevel(Level.WARNING)
			.level("org.apache.juneau.microservice.test.lifecycle.g02", Level.FINE);
		var cfg = Config.create().memStore().build();
		// Logging/levels in the config to also cover the config-driven loop branch.
		cfg.set("Logging/levels", "{'org.apache.juneau.microservice.test.lifecycle.g02_cfg':'INFO'}");
		var ms = Microservice.create().config(cfg).logConfig(lc).build();
		try {
			// Verify the file got created (FileHandler attaches and creates the file lazily on first record;
			// initialization itself is the path under test).
			assertTrue(Files.exists(tmp.resolve("test.log")) || Files.list(tmp).findAny().isPresent(),
				"FileHandler should have either created test.log or a rotated companion file");
		} finally {
			ms.stop();
			// Detach handlers we may have left attached to the root logger to avoid bleed-over.
			var root = Logger.getLogger("");
			for (var h : root.getHandlers()) {
				if (h instanceof FileHandler) {
					h.close();
					root.removeHandler(h);
				}
			}
		}
	}

	// =================================================================================================================
	// H.  start(): RunningClassWithConfig branch (config has a name).
	// =================================================================================================================

	@Test void h01_start_withNamedConfig_runsRunningClassWithConfigBranch(@TempDir Path tmp) throws Exception {
		Files.writeString(tmp.resolve("named.cfg"), "[MySec]\nkey = named\n");
		var ms = Microservice.create()
			.workingDir(tmp.toFile())
			.configName("named.cfg")
			.build();
		try {
			assertEquals("named.cfg", ms.getConfig().getName());
			assertSame(ms, ms.start());
		} finally {
			ms.stop();
		}
	}

	// =================================================================================================================
	// I.  stop(): catch path for beanStore.close() throwing (a @PreDestroy that throws).
	// =================================================================================================================

	public static class ThrowingPreDestroyBean {
		@PreDestroy public void onDestroy() {
			throw new RuntimeException("deliberate-pre-destroy-failure");
		}
	}

	@Configuration
	public static class ThrowingPreDestroyConfig {
		@Bean public ThrowingPreDestroyBean throwingBean() { return new ThrowingPreDestroyBean(); }
	}

	@Test void i01_stop_swallowsBeanStoreCloseFailures() throws Exception {
		var ms = Microservice.create().configurations(ThrowingPreDestroyConfig.class).build();
		// Force resolution so the bean is registered + will be visited by close().
		assertNotNull(ms.getBeanStore().getBean(ThrowingPreDestroyBean.class).orElse(null));
		// The throwing @PreDestroy must NOT propagate out of stop(); it should be logged via getLogger().
		assertDoesNotThrow(ms::stop);
	}

	// =================================================================================================================
	// Helpers.
	// =================================================================================================================

	private static File invokeProtectedResolveFile(Microservice ms, String path) throws Exception {
		var m = Microservice.class.getDeclaredMethod("resolveFile", String.class);
		m.setAccessible(true);
		return (File) m.invoke(ms, path);
	}
}
