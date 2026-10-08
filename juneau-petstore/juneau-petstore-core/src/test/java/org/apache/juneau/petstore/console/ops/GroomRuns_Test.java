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

import static java.util.stream.Collectors.*;
import static org.apache.juneau.BasicTestUtils.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.ConsoleOutputLine.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"java:S2925" // Waits for a 1 ms-period run to finish; the log has no completion hook to wait on.
})
class GroomRuns_Test extends TestBase {

	private static GroomRuns.Run await(GroomRuns.Run r) throws Exception {
		for (var i = 0; i < 1000 && ! r.log().tail(0).terminal; i++)
			Thread.sleep(5);
		assertTrue(r.log().tail(0).terminal, () -> "Run '" + r.id() + "' did not finish");
		return r;
	}

	private static String visible(ConsoleOutputLine l) {
		if (l.frags == null)
			return l.text;
		return l.frags.stream().map(f -> f.text == null ? "" : f.text).collect(joining());
	}

	@Test void a01_aRunWritesTheScriptAndFinishes() throws Exception {
		try (var g = new GroomRuns(1)) {
			var r = await(g.start("Rex", false, null));
			var lines = r.log().stream().toList();
			assertBean(r.log().tail(0), "state,stateStyle,terminal", "DONE,SUCCESS,true");
			assertEquals(GroomRuns.SCRIPT.size() - 1 + GroomRuns.TRANSCRIPT.size(), lines.size());
			assertString("Grooming Rex", lines.get(0).text);
			assertList(lines.stream().filter(l -> l.ui != null && Boolean.TRUE.equals(l.ui.marker)).map(GroomRuns_Test::visible).toList(),
				"Grooming Rex", "Wash", "Dry", "Trim");
			assertList(lines.stream().filter(l -> l.ui != null && l.ui.icon != null).map(l -> l.ui.icon).toList(), "check", "cancel", "check");
			assertList(lines.stream().map(l -> l.level).distinct().sorted().toList(), "SEVERE", "WARNING", "INFO", "FINE");
			assertTrue(lines.stream().anyMatch(l -> l.level == Level.SEVERE && l.text.contains("\n    at Dryer.heat(Dryer.java:42)")));
			assertList(lines.stream().filter(l -> l.ui != null && l.ui.image != null).map(l -> l.ui.image.src).toList(), GroomRuns.PHOTO);
		}
	}

	@Test void a02_progressStripLinksEachWashPass() throws Exception {
		try (var g = new GroomRuns(1)) {
			var lines = await(g.start("Rex", false, null)).log().stream().toList();
			var passes = lines.stream().filter(l -> l.text != null && l.text.startsWith("Wash pass ")).map(l -> "#" + GroomRuns.ANCHOR_PREFIX + l.n).toList();
			var strip = lines.stream().filter(l -> l.frags != null && l.frags.stream().anyMatch(f -> f.href != null)).findFirst().orElseThrow();
			assertString("Wash passes ", strip.frags.get(0).text);
			assertEquals(passes, strip.frags.stream().skip(1).map(f -> f.href).toList());
			assertBean(strip.frags.get(1), "style,tooltip,label", "SUCCESS,Pass 1: clean,Pass 1");
			assertBean(strip.frags.get(GroomRuns.WASH_PASSES), "style,tooltip", "WARN,Pass " + GroomRuns.WASH_PASSES + ": rinse again");
		}
	}

	@Test void a03_ansiTranscriptIsConvertedAndStripped() throws Exception {
		try (var g = new GroomRuns(1)) {
			var text = await(g.start("Rex", false, null)).log().stream().map(GroomRuns_Test::visible).toList();
			assertTrue(text.contains("groomer 2.1 starting"), () -> "lines: " + text);
			assertTrue(text.contains("drying 100%"), () -> "lines: " + text);
			assertTrue(text.stream().noneMatch(t -> t.contains("\u001b") || t.contains("\u0007") || t.contains("groom-pet")), () -> "lines: " + text);
		}
	}

	@Test void a04_verboseRunShowsLoadEarlier() throws Exception {
		try (var g = new GroomRuns(60_000)) {
			var r = g.start("Rex", true, null);
			assertEquals(GroomRuns.VERBOSE_LINES, r.log().size());
			assertNotNull(r.log().tail(5000).before);
			assertEquals(Level.FINE, r.log().stream().findFirst().orElseThrow().level);
		}
	}

	@Test void a05_sourcesAreLookedUpByRunId() throws Exception {
		try (var g = new GroomRuns(1)) {
			var r = await(g.start("Rex", false, null));
			assertSame(r.log(), g.source(r.id()).orElseThrow());
			assertSame(r.fileSource(), g.source(r.id() + GroomRuns.FILE_SUFFIX).orElseThrow());
			assertTrue(g.source("nope").isEmpty());
			assertTrue(g.source("nope" + GroomRuns.FILE_SUFFIX).isEmpty());
			assertTrue(g.source(null).isEmpty());
			assertTrue(r.id().matches("[0-9a-f]{32}"), r.id());
		}
	}

	@Test void a06_fileSourceTailsTheTranscript() throws Exception {
		try (var g = new GroomRuns(1)) {
			var r = await(g.start("Rex", false, null));
			for (var i = 0; i < 1000 && ! r.fileSource().tail(0).terminal; i++)
				Thread.sleep(5);
			var p = r.fileSource().tail(1000);
			assertBean(p, "state,terminal", "DONE,true");
			assertEquals(GroomRuns.SCRIPT.size(), p.lines.size());
			assertString("groomer 2.1 starting", visible(p.lines.get(0)));
			assertString("groom finished", visible(p.lines.get(p.lines.size() - 1)));
		}
	}

	@Test void a07_sameKeySameRun() throws Exception {
		try (var g = new GroomRuns(60_000)) {
			var a = g.start("Rex", false, "k-a07");
			var b = g.start("Max", true, "k-a07");
			assertSame(a, b);
			assertEquals(1, g.list().size());
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
