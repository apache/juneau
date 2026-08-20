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
 * Always-on coverage for the cell-popover trigger wrapper and {@code fillCellPopover} sink.
 */
class ViewsJs_CellPopover_Test extends TestBase {

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@Test void a01_sourceShape_wrapperAndDelegatedListener() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("function appendPopoverTrigger("), body);
		assertTrue(body.contains("function fillCellPopover("), body);
		assertTrue(body.contains("function initCellPopover("), body);
		assertTrue(body.contains("e.stopPropagation()"), body);
		assertTrue(body.contains("data-juneau-popover-col"), body);
		assertTrue(body.contains("initCellPopover(table, ctx, viewDef)"), body);
		assertFalse(body.contains("function fillCellPopover(") && body.substring(
			body.indexOf("function fillCellPopover("),
			body.indexOf("function fillCellPopover(") + 800).contains("innerHTML"),
			"fillCellPopover must not assign innerHTML");
	}

	private static Map<?,?> report;

	@BeforeAll
	static void probeIfNodeAvailable() throws Exception {
		if (!nodeAvailable())
			return;
		var harness = locateHarness();
		if (harness == null)
			return;
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		try {
			Files.writeString(viewsFile, viewsJs(), UTF_8);
			Files.writeString(rendersFile, rendersJs(), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
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
			var p = Path.of(basedir, "src/test/js/cell-popover.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/cell-popover.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/cell-popover.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("cell-popover-stdout-", ".json");
		var stderr = Files.createTempFile("cell-popover-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("cell-popover.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("cell-popover.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or cell-popover.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_triggerIsSibling_notWrappingAnchor() {
		var r = report();
		assertEquals(true, r.get("trigger_hasButton"));
		assertEquals(true, r.get("trigger_hasAnchor"));
		assertEquals(true, r.get("trigger_buttonAfterAnchor"));
		assertEquals(true, r.get("trigger_noButtonWrap"));
		assertEquals(true, r.get("trigger_titleEscaped"));
	}

	@Test void b02_tsZuluCoexistsWithTrigger() {
		var r = report();
		assertEquals(true, r.get("ts_hasDataAttr"));
		assertEquals(true, r.get("ts_noTrigger"));
		assertEquals(true, r.get("ts_plusTrigger"));
	}

	@Test void b03_fillFromRowData_textOnly() {
		var r = report();
		assertEquals(true, r.get("fill_title"));
		assertEquals(true, r.get("fill_actual"));
		assertEquals(true, r.get("fill_missingBlank"));
		assertEquals(true, r.get("fill_noTsHost"));
		assertEquals(true, r.get("fill_htmlShapedFallsBack"));
		assertEquals(true, r.get("fill_noElementCopy"));
		assertEquals(true, r.get("freeze_dateStillText"));
	}
}
