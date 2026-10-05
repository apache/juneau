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
 * An icon name found in none of the sprite layers draws nothing on a ribbon button (WORK-J0557 U11): the raw label
 * is no longer painted as on-screen text, while it stays the accessible name ({@code aria-label}) and the tooltip.
 * Drives the real {@code buildRibbon(...)} path through {@code ribbon-icon-fallback.cjs}; skipped when Node is absent.
 */
class ViewsJs_RibbonIconFallback_Test extends TestBase {

	private static String ribbonJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RIBBON_JS_RESOURCE)) {
			assertNotNull(in);
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
		var ribbonFile = Files.createTempFile("juneau-ribbon-", ".js");
		try {
			Files.writeString(ribbonFile, ribbonJs(), UTF_8);
			report = Json.to(runNode(harness, ribbonFile), Map.class);
		} finally {
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
			var p = Path.of(basedir, "src/test/js/ribbon-icon-fallback.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/ribbon-icon-fallback.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/ribbon-icon-fallback.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path ribbonJs) throws Exception {
		var stdout = Files.createTempFile("ribbon-icon-fallback-stdout-", ".json");
		var stderr = Files.createTempFile("ribbon-icon-fallback-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), ribbonJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("ribbon-icon-fallback.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("ribbon-icon-fallback.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or ribbon-icon-fallback.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void a01_unknownIconDrawsNothing_butKeepsItsAccessibleName() {
		var unknown = (Map<?,?>)report().get("unknown");
		assertBean(unknown, "built,text,empty,aria,tip", "true,,true,Reload it,Reload it");
	}

	@Test void a02_resolvedIconStillDraws() {
		var known = (Map<?,?>)report().get("known");
		assertBean(known, "built,text,empty,aria", "true,,false,Reload it");
	}
}
