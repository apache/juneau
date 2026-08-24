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
 * Integration coverage for 445m: the {@code juneau-chrome.js} MENU wiring driving the ONE shared
 * {@code juneau-views.js} layer stack (445h) &mdash; renders + views + chrome loaded into a single Node sandbox so a
 * real {@link org.apache.juneau.rest.server.widgets.Behavior#MENU} trigger opens its {@code .jc-menu} list on the
 * shared stack, not on a chrome-local mock.
 *
 * <p>
 * Pins the locked rules that only show up when the two runtimes run together: on open the trigger's
 * {@code aria-controls}'d list is portalled to {@code document.body} ({@code position:fixed}, escaping any
 * overflow-clip ancestor) as a {@code kind:"menu"} light-dismiss layer that never inflates the dialog-kind depth cap;
 * Escape / outside-click dismissal and the {@code aria-expanded} reset all run through that ONE stack; and a chrome
 * menu opened <b>over</b> a views dialog pops off cleanly on Escape while the dialog beneath it survives.  Gated on
 * {@code node} being on {@code PATH} (skipped otherwise &mdash; no {@code -Pjs-tests} required).
 */
class ViewsJs_ChromeMenu_Test extends TestBase {

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
		var chromeFile = Files.createTempFile("juneau-chrome-", ".js");
		try {
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(chromeFile, resource(ViewsMixin.CHROME_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile, chromeFile), Map.class);
		} finally {
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(chromeFile);
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
			var p = Path.of(basedir, "src/test/js/chrome-menu.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/chrome-menu.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/chrome-menu.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path renders, Path views, Path chrome) throws Exception {
		var stdout = Files.createTempFile("chrome-menu-stdout-", ".json");
		var stderr = Files.createTempFile("chrome-menu-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of(
					"node", harness.toString(), renders.toString(), views.toString(), chrome.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("chrome-menu.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("chrome-menu.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or chrome-menu.cjs not found — integration layer skipped");
		return report;
	}

	@Test void a01_harnessLoadedBothRuntimesIntoOneSandbox() {
		var r = r();
		assertEquals(true, r.get("hasViews"), "juneau-views.js must publish the shared layer stack");
		assertEquals(true, r.get("hasChrome"), "juneau-chrome.js must publish its MENU wiring");
	}

	@Test void a02_menuOpensAsPortalledFixedMenuLayerOnTheSharedStack() {
		var r = r();
		assertEquals(true, r.get("open_topIsMenu"), () -> "top layer is not a menu: " + r);
		assertEquals(true, r.get("open_menuOnBody"), () -> "the list was not portalled to body: " + r);
		assertEquals(true, r.get("open_escapedHeader"), () -> "the list is still clipped inside the header: " + r);
		assertEquals(true, r.get("open_positionFixed"), () -> "the list is not position:fixed: " + r);
		assertEquals(true, r.get("open_displayShown"), () -> "the list was not shown on open: " + r);
		assertEquals(true, r.get("open_zIndexSet"), () -> "the stack did not stamp a z-index: " + r);
		assertEquals(true, r.get("open_notADialog"), () -> "a menu inflated the dialog-kind depth cap: " + r);
		assertEquals("true", r.get("open_ariaExpanded"), () -> "aria-expanded did not flip to true: " + r);
	}

	@Test void a03_escapeDismissesThroughTheSharedStack_ariaResetNodeKept() {
		var r = r();
		assertEquals(true, r.get("esc_closed"), () -> "Escape did not pop the menu off the stack: " + r);
		assertEquals("false", r.get("esc_ariaReset"), () -> "onDismiss did not reset aria-expanded: " + r);
		assertEquals(true, r.get("esc_menuHidden"), () -> "onDismiss did not hide the list: " + r);
		assertEquals(true, r.get("esc_menuKeptInBody"), () -> "detachOnPop:false must keep the node for reuse: " + r);
	}

	@Test void a04_outsideClickLightDismissesThroughTheSharedStack() {
		var r = r();
		assertEquals(true, r.get("reopen_topIsMenu"), () -> "the menu did not reopen: " + r);
		assertEquals(true, r.get("light_closed"), () -> "an outside pointerdown did not light-dismiss the menu: " + r);
	}

	@Test void a05_chromeMenuStackedOverADialogPopsCleanly() {
		var r = r();
		assertEquals(true, r.get("stack_dialogFirst"), () -> "the dialog did not open first: " + r);
		assertEquals(true, r.get("stack_menuOverDialog"), () -> "the menu did not stack over the dialog: " + r);
		assertEquals(true, r.get("stack_dialogCountUnchanged"), () -> "the menu touched the dialog-kind depth cap: " + r);
		assertEquals(true, r.get("stack_menuPoppedDialogRemains"),
			() -> "Escape did not pop only the menu, leaving the dialog: " + r);
		assertEquals(true, r.get("stack_dialogSurvives"), () -> "the dialog beneath did not survive: " + r);
		assertEquals("false", r.get("stack_ariaReset"), () -> "the trigger's aria-expanded was not reset: " + r);
	}
}
