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
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Where {@code buildToolbarRow(...)} puts the DataTables search box: at the canonical {@code search} slot among the
 * tagged ribbon groups ({@code search|entity|filter|export|viewing|page|status}), with the paging pill alone on the left.
 *
 * <p>
 * Drives {@code toolbar-row.cjs}, which loads the view and ribbon runtimes into one window.  Runs when {@code node}
 * is on {@code PATH}, skipped otherwise.
 */
class ViewsJs_ToolbarRow_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, () -> "missing classpath resource: " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var ribbonFile = Files.createTempFile("juneau-ribbon-", ".js");
		try {
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(ribbonFile, resource(ViewsMixin.RIBBON_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile, ribbonFile), Map.class);
		} finally {
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(ribbonFile);
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
			var p = Path.of(basedir, "src/test/js/toolbar-row.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/toolbar-row.cjs",
			"juneau-rest/juneau-console-views/src/test/js/toolbar-row.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs, Path ribbonJs) throws Exception {
		var stdout = Files.createTempFile("toolbar-row-stdout-", ".json");
		var stderr = Files.createTempFile("toolbar-row-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of(
					"node", harness.toString(), rendersJs.toString(), viewsJs.toString(), ribbonJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("toolbar-row.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("toolbar-row.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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

	private static Map<?,?> report() {
		assumeTrue(report != null, "node not available or toolbar-row.cjs not found - behavioral layer skipped");
		return report;
	}

	@Test void a01_harnessLoadedBothRuntimes() {
		assertEquals(true, report().get("hasSeam"));
	}

	@Test void a02_untaggedRibbon_searchStaysAheadOfTheRibbon() {
		assertBean(report(), "untagged_right,untagged_leftIsPillOnly,untagged_rowIsFirstChild,untagged_searchLabelled",
			"search untagged,true,true,true");
	}

	@Test void a03_taggedRibbon_searchTakesItsCanonicalSlot() {
		assertEquals("search entity filter viewing page", report().get("tagged_right"));
	}

	@Test void a04_onlyGroupsRankedBeforeSearch_searchGoesLast() {
		assertEquals("search filter", report().get("onlyBefore_right"));
	}

	@Test void a05_onlyGroupsRankedAfterSearch_searchGoesFirst() {
		assertEquals("search page", report().get("onlyAfter_right"));
	}

	@Test void a06_noRibbon_searchAlone() {
		assertEquals("search", report().get("noRibbon_right"));
	}

	@Test void a07_ribbonSearchSlot_isReplacedByTheFilterBox() {
		assertBean(report(), "slot_filterInRibbon,slot_noPlaceholderLeft,slot_notDuplicatedInRight,slot_filterTagged,slot_inSearchGroup",
			"true,true,true,search,search");
	}

	@Test void a08_ribbonSearchSlot_removedWhenThereIsNoFilterBox() {
		assertEquals(true, report().get("slot_removedWithoutFilter"));
	}
}
