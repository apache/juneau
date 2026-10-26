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
package org.apache.juneau.console.views;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * The pre-paint color-scheme script ({@link ConsoleChromeMixin#SCHEME_SCRIPT}, the exact constant) over its full
 * precedence matrix: a stored choice wins only on a page that enables the toggle, then the server pin, then the OS;
 * anything else is light.  Runs in plain Node ({@code scheme-script.cjs}); skipped when Node is not available.
 */
@SuppressWarnings({
	"unchecked" // The JSON report is parsed into raw Maps.
})
class SchemeScript_Test extends TestBase {

	/** The guard that limits the stored choice to toggle pages; a03 removes it from a copy. */
	private static final String TOGGLE_GUARD = "if(e.hasAttribute(\"data-jc-scheme-toggle\"))";

	/**
	 * Every matrix row and the scheme it must resolve to, one row per line:
	 * {@code toggle stored pin os -> scheme}.  Written out rather than computed, so a precedence change has to edit
	 * this table.
	 */
	private static final String EXPECTED = """
		no none none light -> light
		no none none dark -> dark
		no none light light -> light
		no none light dark -> light
		no none dark light -> dark
		no none dark dark -> dark
		no light none light -> light
		no light none dark -> dark
		no light light light -> light
		no light light dark -> light
		no light dark light -> dark
		no light dark dark -> dark
		no dark none light -> light
		no dark none dark -> dark
		no dark light light -> light
		no dark light dark -> light
		no dark dark light -> dark
		no dark dark dark -> dark
		no garbage none light -> light
		no garbage none dark -> dark
		no garbage light light -> light
		no garbage light dark -> light
		no garbage dark light -> dark
		no garbage dark dark -> dark
		yes none none light -> light
		yes none none dark -> dark
		yes none light light -> light
		yes none light dark -> light
		yes none dark light -> dark
		yes none dark dark -> dark
		yes light none light -> light
		yes light none dark -> light
		yes light light light -> light
		yes light light dark -> light
		yes light dark light -> light
		yes light dark dark -> light
		yes dark none light -> dark
		yes dark none dark -> dark
		yes dark light light -> dark
		yes dark light dark -> dark
		yes dark dark light -> dark
		yes dark dark dark -> dark
		yes garbage none light -> light
		yes garbage none dark -> dark
		yes garbage light light -> light
		yes garbage light dark -> light
		yes garbage dark light -> dark
		yes garbage dark dark -> dark
		""";

	private static Map<String,String> expected() {
		var m = new LinkedHashMap<String,String>();
		for (var line : EXPECTED.strip().split("\n")) {
			var p = line.strip().split("\\s+");
			assertEquals(6, p.length, () -> "bad expected row: " + line);
			m.put("toggle=" + p[0] + " stored=" + p[1] + " pin=" + p[2] + " os=" + p[3], p[5]);
		}
		return m;
	}

	private static Map<String,Map<String,Object>> run(String script) throws Exception {
		var harness = locateHarness();
		assumeTrue(harness != null && nodeAvailable(), "node not available or scheme-script.cjs not found");
		var src = Files.createTempFile("scheme-script-", ".js");
		var stdout = Files.createTempFile("scheme-script-stdout-", ".json");
		var stderr = Files.createTempFile("scheme-script-stderr-", ".txt");
		try {
			Files.writeString(src, script, UTF_8);
			var p = new ProcessBuilder(List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), src.toString()))
				.redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
			if (! p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("scheme-script.cjs did not finish within 60s; stderr:\n" + quietRead(stderr));
			}
			assertEquals(0, p.exitValue(), () -> "scheme-script.cjs exited non-zero; stderr:\n" + quietRead(stderr));
			return (Map<String,Map<String,Object>>)Json.to(Files.readString(stdout, UTF_8), Map.class);
		} finally {
			Files.deleteIfExists(src);
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	@Test void a01_matrix() throws Exception {
		var rows = run(ConsoleChromeMixin.SCHEME_SCRIPT).get("rows");
		var exp = expected();
		assertSize(48, exp);
		assertEquals(exp.keySet(), rows.keySet());
		var wrong = new ArrayList<String>();
		exp.forEach((k, v) -> {
			if (! v.equals(rows.get(k)))
				wrong.add(k + ": expected " + v + ", got " + rows.get(k));
		});
		assertEquals(List.of(), wrong);
	}

	@Test void a02_failureRows() throws Exception {
		var f = run(ConsoleChromeMixin.SCHEME_SCRIPT).get("failures");
		assertEquals("dark", f.get("storageThrowsFallsToPin"), f::toString);
		assertEquals("dark", f.get("storageThrowsFallsToOs"), f::toString);
		assertEquals("light", f.get("matchMediaMissing"), f::toString);
		assertEquals("dark", f.get("matchMediaMissingPinned"), f::toString);
	}

	/** Without the toggle guard, a stored choice leaks onto pages that never offered the control. */
	@Test void a03_guardMatters() throws Exception {
		assertTrue(ConsoleChromeMixin.SCHEME_SCRIPT.contains(TOGGLE_GUARD), "guard text moved; update TOGGLE_GUARD");
		var rows = run(ConsoleChromeMixin.SCHEME_SCRIPT.replace(TOGGLE_GUARD, "")).get("rows");
		var exp = expected();
		var differ = exp.keySet().stream().filter(k -> ! exp.get(k).equals(rows.get(k))).toList();
		assertFalse(differ.isEmpty(), "removing the toggle guard changed no row");
		assertTrue(differ.contains("toggle=no stored=dark pin=none os=light"), differ::toString);
		assertTrue(differ.stream().allMatch(k -> k.startsWith("toggle=no ")), differ::toString);
	}

	private static Path locateHarness() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/scheme-script.cjs");
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of("src/test/js/scheme-script.cjs", "juneau-rest/juneau-console-views/src/test/js/scheme-script.cjs")) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder(System.getProperty("juneau.jsTests.node", "node"), "--version").redirectErrorStream(true).start();
			if (! p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (IOException e) {
			return false;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return false;
		}
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p, UTF_8);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}
}
