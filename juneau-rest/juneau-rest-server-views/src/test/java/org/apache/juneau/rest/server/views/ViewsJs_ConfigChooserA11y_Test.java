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
import org.junit.jupiter.api.*;

/**
 * Node behavioral harness (skipped when {@code node} is absent) for the View Settings dialog's tab ARIA wiring and
 * roving-tabindex keyboard nav (design §3 / F3): {@code config-chooser-a11y.cjs} drives the real
 * {@code juneau-config.js} {@code openChooser} against {@code views-dom-shim.cjs}'s real DOM.
 */
class ViewsJs_ConfigChooserA11y_Test extends TestBase {

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness("config-chooser-a11y.cjs");
		var viewsJs = locateResource(ViewsMixin.VIEWS_JS_RESOURCE);
		var configJs = locateResource(ViewsMixin.CONFIG_JS_RESOURCE);
		if (harness == null || viewsJs == null || configJs == null)
			return;
		report = Json.to(runNode(harness, viewsJs, configJs), Map.class);
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

	private static Path locateHarness(String name) {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/" + name);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/" + name,
			"juneau-rest/juneau-rest-server-views/src/test/js/" + name
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static Path locateResource(String classpathResource) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(classpathResource)) {
			assertNotNull(in, () -> "missing classpath resource: " + classpathResource);
			var tmp = Files.createTempFile("config-chooser-a11y-", ".js");
			Files.write(tmp, in.readAllBytes());
			tmp.toFile().deleteOnExit();
			return tmp;
		}
	}

	private static String runNode(Path harness, Path viewsJs, Path configJs) throws Exception {
		var stdout = Files.createTempFile("config-chooser-a11y-stdout-", ".json");
		var stderr = Files.createTempFile("config-chooser-a11y-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(
				List.of("node", harness.toString(), viewsJs.toString(), configJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("config-chooser-a11y.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("config-chooser-a11y.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or config-chooser-a11y.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void e01_idPrefixesAreUniqueAcrossDialogOpens() {
		// A second dialog's tab ids must never collide with a previously opened dialog's.
		assertBean(report(), "firstIdsUnique,secondIdsUnique,idsNonEmpty,noOverlapAcrossDialogs", "true,true,true,true");
	}

	@Test void e02_tabsCarryFullAriaWiringToTheirPanels() {
		// Every tab's aria-controls points at a role=tabpanel whose aria-labelledby points back; only View is tabbable.
		assertBean(report(), "hasTablist,tabOrder,ariaSelected,tabIndexes,panelsMatchControls",
			"true,[view,search,sort,options],[true,false,false,false],[0,-1,-1,-1],true");
	}

	@Test void e03_arrowKeysMoveFocusAndActivationWithWraparound() {
		assertBean(report(),
			"afterArrowRightActiveTab,afterArrowRightFocused,afterArrowLeftBackToViewTab,wrapArrowLeftFromViewActiveTab,wrapArrowLeftFocusedOptions",
			"search,true,view,options,true");
	}

	@Test void e04_homeAndEndJumpToFirstAndLastTab() {
		assertBean(report(), "homeActiveTab,endActiveTab", "view,options");
	}

	@Test void e05_unrelatedKeysAreNotIntercepted() {
		// A plain Tab keydown must not be treated as tab-strip navigation.
		assertBean(report(), "plainTabKeyIgnored", "true");
	}

	@Test void e06_columnConfigTabsRestrictsWhichTabsAreBuilt() {
		// The initial tab is the first VISIBLE tab, and ArrowRight from the last visible tab wraps to the first
		// visible tab, never to a hidden one.
		assertBean(report(), "restrictedTabOrder,restrictedInitialActiveTab,restrictedPanelCount,restrictedWrapArrowRightFromLastTab",
			"[search,sort],search,2,search");
	}

	@Test void e07_unrestrictedColumnConfigStillBuildsAllFourTabsDefaultingToView() {
		assertBean(report(), "unrestrictedTabOrder,unrestrictedInitialActiveTab", "[view,search,sort,options],view");
	}

	@Test void e08_sortTabIsAnOrderedDirectionalPriorityListInTheDom() {
		// Unchecking removes the row from the ordered list; re-checking an unsorted column appends to the TAIL.
		assertBean(report(), "sortTabInitialOrder,sortTabDirAfterChange,sortTabAfterUncheckOrder,sortTabAfterUncheckDraft,sortTabAfterRecheckDraft",
			"[name,status],desc,[status],[status],[status,name]");
	}
}
