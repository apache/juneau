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
package org.apache.juneau.petstore.console.ops;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.terminal.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S2925" // Waits for a 1 ms-period run to finish; the run has no completion hook to wait on.
})
class GroomRuns_Test extends TestBase {

	private static final Pattern ANSI = Pattern.compile("\u001b\\[[0-9;]*m");

	private static GroomRuns.Run await(GroomRuns.Run r) throws Exception {
		for (var i = 0; i < 1000 && ! r.done(); i++)
			Thread.sleep(5);
		assertTrue(r.done(), () -> "Run '" + r.id() + "' did not finish");
		return r;
	}

	/** Reads the whole source from offset 0. */
	private static String read(TerminalSource s) throws IOException {
		var out = new ByteArrayOutputStream();
		s.copyTo(out);
		return out.toString(UTF_8);
	}

	private static List<String> plain(TerminalSource s) throws IOException {
		return Arrays.stream(ANSI.matcher(read(s)).replaceAll("").split("\r\n")).toList();
	}

	@Test void a01_aRunWritesTheScriptAndFinishes() throws Exception {
		try (var g = new GroomRuns(1)) {
			var r = await(g.start("Rex", false, null));
			var lines = plain(r.terminal());
			assertEquals(GroomRuns.SCRIPT.size() + GroomRuns.TRANSCRIPT.size() - 1 + 2, lines.size() - 1, () -> "lines: " + lines);
			assertString("Grooming Rex", lines.get(0));
			assertTrue(lines.contains("\u2713 All tools ready"), () -> "lines: " + lines);
			assertTrue(lines.contains("\u2717 Bow tie: none in stock"), () -> "lines: " + lines);
			assertTrue(lines.contains("    at Dryer.heat(Dryer.java:42)"), () -> "lines: " + lines);
			assertTrue(read(r.terminal()).contains("\u001b[1m" + "Grooming Rex"), "styled marker");
			assertBean(r.terminal().read(0, 1), "done", "false");
			assertBean(r.row(), "state,lines", "DONE," + lines.size());
		}
	}

	@Test void a02_progressStripShowsEachWashPass() throws Exception {
		try (var g = new GroomRuns(1)) {
			var text = read(await(g.start("Rex", false, null)).terminal());
			var strip = Arrays.stream(text.split("\r\n")).filter(l -> l.startsWith("Wash passes ")).findFirst().orElseThrow();
			assertEquals(GroomRuns.WASH_PASSES, strip.chars().filter(c -> c == '\u25a0').count());
			assertEquals(GroomRuns.WASH_PASSES - 2, strip.split("\u001b\\[32m", -1).length - 1);
		}
	}

	@Test void a03_ansiTranscriptIsWrittenAsIs() throws Exception {
		try (var g = new GroomRuns(1)) {
			var text = read(await(g.start("Rex", false, null)).terminal());
			assertTrue(text.contains("\u001b[1mgroomer 2.1\u001b[0m starting"), "bold title kept");
			assertTrue(text.contains("drying  10%\rdrying  40%\rdrying  70%\rdrying 100%"), "carriage-return counter kept");
		}
	}

	@Test void a04_verboseRunWritesTheBulkUpFront() throws Exception {
		try (var g = new GroomRuns(60_000)) {
			var r = g.start("Rex", true, null);
			assertEquals(GroomRuns.VERBOSE_LINES, r.row().getInt("lines"));
			assertTrue(plain(r.terminal()).get(0).startsWith("verbose: inspected coat patch 1 of "));
			assertBean(r.terminal().read(0, 1), "truncated", "false");
		}
	}

	@SuppressWarnings({
		"resource" // The run owns its terminal sources; the enclosing GroomRuns closes them.
	})
	@Test void a05_sourcesAreLookedUpByRunId() throws Exception {
		try (var g = new GroomRuns(1)) {
			var r = await(g.start("Rex", false, null));
			assertSame(r.terminal(), g.source(r.id()).orElseThrow());
			assertSame(r.fileSource(), g.source(r.id() + GroomRuns.FILE_SUFFIX).orElseThrow());
			assertTrue(g.source("nope").isEmpty());
			assertTrue(g.source("nope" + GroomRuns.FILE_SUFFIX).isEmpty());
			assertTrue(g.source(null).isEmpty());
			assertTrue(r.id().matches("[0-9a-f]{32}"), r.id());
		}
	}

	@SuppressWarnings({
		"resource" // The run owns its terminal sources; the enclosing GroomRuns closes them.
	})
	@Test void a06_fileSourceTailsTheTranscript() throws Exception {
		try (var g = new GroomRuns(1)) {
			var r = await(g.start("Rex", false, null));
			var c = r.fileSource().read(0, TerminalSource.MAX_READ_BYTES);
			assertBean(c, "done,cols,rows", "true," + GroomRuns.COLS + "," + GroomRuns.ROWS);
			var lines = plain(r.fileSource());
			assertEquals(GroomRuns.SCRIPT.size(), lines.size(), () -> "lines: " + lines);
			assertTrue(lines.get(0).endsWith("groomer 2.1 starting"), () -> "lines: " + lines); // The OSC title survives the SGR-only strip.
			assertString("groom finished", lines.get(lines.size() - 1));
		}
	}

	@Test void a07_sameKeySameRun() throws Exception {
		try (var g = new GroomRuns(60_000)) {
			var a = g.start("Rex", false, "k-a07");
			var b = g.start("Max", true, "k-a07");
			assertSame(a, b);
			assertSize(1, g.list());
		}
	}

	@Test void a08_oldRunsAreDroppedWithTheirFiles() throws Exception {
		try (var g = new GroomRuns(60_000)) {
			var first = g.start("First", false, null);
			GroomRuns.Run last = null;
			for (var i = 0; i < GroomRuns.MAX_RUNS; i++)
				last = g.start("Pet " + i, false, null);
			assertEquals(GroomRuns.MAX_RUNS, g.list().size());
			assertTrue(g.get(first.id()).isEmpty());
			assertFalse(Files.exists(first.file()));
			assertFalse(Files.exists(first.sizeFile));
			assertSame(last, g.latest());
		}
	}

	@Test void a09_closeDeletesEveryFile() throws Exception {
		Path f;
		try (var g = new GroomRuns(60_000)) {
			f = g.start("Rex", false, null).file();
			assertTrue(Files.exists(f));
		}
		assertFalse(Files.exists(f));
	}

	@Test void a10_badPeriod() {
		assertThrowsWithMessage(IllegalArgumentException.class, "periodMs must be at least 1; got 0", () -> new GroomRuns(0));
	}

	@Test void a11_blankPetNameDefaults() throws Exception {
		try (var g = new GroomRuns(60_000)) {
			assertBean(g.start("  ", false, null).row(), "pet,state,lines,verbose", "Rex,RUNNING,0,false");
		}
	}
}
