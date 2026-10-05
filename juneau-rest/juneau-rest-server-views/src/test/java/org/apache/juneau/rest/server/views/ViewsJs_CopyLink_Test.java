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
 * Always-on behavioral coverage for the Copy-link toolbar button (framework parity): {@code isCopyLinkVisible}'s
 * visibility rule, {@code mountCopyLinkButton}'s idempotency, and that its click handler is wired through
 * {@code NS.init.copyShareableUrl} rather than a closed-over reference - driven against the real
 * {@code juneau-views.js} through {@code src/test/js/copy-link.cjs}.  Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_CopyLink_Test extends TestBase {

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in, name);
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
		try {
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			report = Json.to(runNode(harness, rendersFile, viewsFile), Map.class);
		} finally {
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(viewsFile);
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
			var p = Path.of(basedir, "src/test/js/copy-link.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/copy-link.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/copy-link.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("copy-link-stdout-", ".json");
		var stderr = Files.createTempFile("copy-link-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("copy-link.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("copy-link.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or copy-link.cjs not found - behavioral layer skipped");
		return report;
	}

	@Test void a01_unsetPrimary_withExactlyOneTable_isVisible() {
		assertBean(report(), "singleTableUnsetPrimary", "true");
	}

	@Test void a02_unsetPrimary_withSeveralTables_isNotFirstWins_isHiddenOnAll() {
		assertBean(report(), "severalTablesNeitherVisible", "true");
	}

	@Test void a03_explicitPrimaryTrue_winsEvenAmongSeveralTables() {
		assertBean(report(), "explicitPrimaryVisibleAmongSeveral", "true");
	}

	@Test void a04_explicitPrimaryFalse_hidesItEvenAsTheOnlyTable() {
		assertBean(report(), "primaryFalseHidesIt", "true");
	}

	@Test void a05_copyLinkFalse_hidesAPrimaryViewsButton() {
		assertBean(report(), "copyLinkFalseHidesIt", "true");
	}

	@Test void a06_mountingTwice_neverDuplicatesTheButton() {
		assertBean(report(), "mountedButtonCount", "1");
	}

	@Test void a07_clickHandler_callsTheExportedCopyShareableUrl_withTableAndCtx() {
		assertBean(report(), "clickCalledExportedFnWithTableAndCtx", "true");
	}

	@Test void a08_noUrlStateModule_rendersNoButton() {
		assertBean(report(), "noUrlStateNoButton", "true");
	}

	@Test void a09_rejectedCopy_isCaughtAndAnnouncesFailure() {
		assertBean(report(), "rejectedCopyHandled,rejectedCopyAnnounced", "true,true");
	}
}
