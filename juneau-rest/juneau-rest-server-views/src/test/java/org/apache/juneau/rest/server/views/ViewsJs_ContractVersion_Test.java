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
 * Always-on coverage for the dialog-form contract-version handshake (h5): a form-bearing modal opens only when BOTH
 * the modal top-level and the nested form {@code contractVersion} equal the baked-in {@code "1"}; a wrong or missing
 * version on either is a visible refusal and the dialog does not open.  A confirm-only envelope (no form) - whether
 * fetched or a local blank-form-token prompt - stays unversioned and always opens.
 */
class ViewsJs_ContractVersion_Test extends TestBase {

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

	@Test void a01_sourceShape_failLoudHandshakeChecksBothVersions() throws Exception {
		var body = viewsJs();
		assertTrue(body.contains("JUNEAU_DIALOG_FORM_CONTRACT_VERSION"), body);
		assertTrue(body.contains("function openActionDialog("), "openActionDialog present");
		assertTrue(body.contains("payload.contractVersion !== JUNEAU_DIALOG_FORM_CONTRACT_VERSION"), body);
		assertTrue(body.contains("payload.form.contractVersion !== JUNEAU_DIALOG_FORM_CONTRACT_VERSION"), body);
		// Confirm-only (blank form token) stays a local, unversioned prompt.
		assertTrue(body.contains("isBlankToken(action.form)"), body);
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
			var p = Path.of(basedir, "src/test/js/contract-version.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/contract-version.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/contract-version.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("contract-version-stdout-", ".json");
		var stderr = Files.createTempFile("contract-version-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("contract-version.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("contract-version.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null, "node not available or contract-version.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b01_bothVersionsOneOpensTheDialog() {
		var r = report();
		assertEquals(true, r.get("bothV1_opens"));
		assertEquals(true, r.get("bothV1_noRefusal"));
	}

	@Test void b02_wrongModalVersionRefusesWithoutOpening() {
		var r = report();
		assertEquals(true, r.get("modalVersionWrong_noOpen"));
		assertEquals(true, r.get("modalVersionWrong_refusal"));
	}

	@Test void b03_missingFormVersionRefusesWithoutOpening() {
		var r = report();
		assertEquals(true, r.get("formVersionMissing_noOpen"));
		assertEquals(true, r.get("formVersionMissing_refusal"));
	}

	@Test void b04_confirmOnlyFetchedEnvelopeStaysUnversionedAndOpens() {
		var r = report();
		assertEquals(true, r.get("confirmOnlyFetched_opens"));
		assertEquals(true, r.get("confirmOnlyFetched_noRefusal"));
	}

	@Test void b05_confirmOnlyLocalPromptOpensWithoutFetch() {
		var r = report();
		assertEquals(true, r.get("confirmOnlyLocal_opens"));
	}
}
