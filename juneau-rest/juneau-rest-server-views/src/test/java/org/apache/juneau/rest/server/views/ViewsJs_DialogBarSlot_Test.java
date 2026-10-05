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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Node-sandbox coverage for the dialog bar slot painter in {@code juneau-views.js} ({@code dialog-bar-slot.cjs}): a bar
 * badge's {@code data-juneau-badge-tone} attribute is set only for a status-tone wire token, and nothing else.
 *
 * <p>
 * Gated on {@code node} being on {@code PATH} (skipped otherwise - no {@code -Pjs-tests} required).
 */
class ViewsJs_DialogBarSlot_Test extends TestBase {

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var views = Files.createTempFile("juneau-views-", ".js");
		var console = Files.createTempFile("juneau-console-", ".js");
		var renders = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(views, asset(ViewsMixin.VIEWS_JS_RESOURCE, ViewsMixin.class), UTF_8);
			Files.writeString(console, asset(ConsoleChromeMixin.CONSOLE_JS_RESOURCE, ConsoleChromeMixin.class), UTF_8);
			Files.writeString(renders, asset(ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.class), UTF_8);
			report = Json.to(runNode(harness, views, console, renders), Map.class);
		} finally {
			Files.deleteIfExists(views);
			Files.deleteIfExists(console);
			Files.deleteIfExists(renders);
		}
	}

	private static String asset(String name, Class<?> anchor) throws IOException {
		try (var in = anchor.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/dialog-bar-slot.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/dialog-bar-slot.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/dialog-bar-slot.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path views, Path console, Path renders) throws Exception {
		var stdout = Files.createTempFile("dialog-bar-stdout-", ".json");
		var stderr = Files.createTempFile("dialog-bar-stderr-", ".txt");
		try {
			var p = new ProcessBuilder(List.of("node", harness.toString(), views.toString(), console.toString(), renders.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("dialog-bar-slot.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("dialog-bar-slot.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}

	private static Map<?,?> r() {
		assumeTrue(report != null, "node not available or dialog-bar-slot.cjs not found - dialog bar slot harness skipped");
		return report;
	}

	@Test void a01_badgeIsPaintedFromJson() {
		var r = r();
		assertBean(r, "paint_badgeCountPainted,paint_badgeNamespaced", "true,true");
	}

	@Test void a02_toneIsLowerCasedWhenItIsAStatusToneToken() {
		var r = r();
		assertBean(r, "tone_upperWarning,tone_error", "warning,error");
	}

	@Test void a03_offPaletteOrAbsentToneSetsNoAttribute() {
		var r = r();
		assertBean(r, "tone_bogus,tone_retiredWarn,tone_retiredAccent,tone_absent", "<null>,<null>,<null>,<null>");
	}
}
