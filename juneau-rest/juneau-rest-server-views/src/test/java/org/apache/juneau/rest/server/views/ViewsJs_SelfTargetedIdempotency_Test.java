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
 * The behavioral tripwire for what {@code submitActionDialog} ACTUALLY TRANSMITS as its target: the row's id, or -
 * for a modal that has explicitly opted in with {@code selfTargeted} - the self-bound idempotency key's own value.
 *
 * <p>A source-text presence check cannot carry this property.  The literals {@code "idempotencyKey"} and
 * {@code "targetId"} appear in the function body under EITHER a correct opt-in-gated rule or a regressed blanket
 * "the key always wins" one, so a check that only greps for them (see {@code ViewsJs_ModalResult_Test.g01}) stays
 * green through the exact regression it looks like it is guarding: an unconditional precedence silently stops
 * sending the real row id that an artifact-bound key was minted against, making that key's own binding check
 * vacuous for every existing {@code present=dialog} consumer.
 *
 * <p>So this class has two layers:
 * <ul class='spaced-list'>
 * 	<li><b>A default-gate SHAPE pin</b> (a01/a02, no Node required): the whole {@code targetId} statement, pinned
 * 		exactly, plus the absence of any {@code tr}-null conditional around the opt-in branch.  Substring presence
 * 		is deliberately not enough - {@code modal?.selfTargeted &&} stays present even if a {@code tr == null}
 * 		guard is wrapped around the branch it gates, which is precisely how the row-less case could silently
 * 		regress to omitting the target entirely.
 * 	<li><b>A behavioral harness</b> ({@code selftargeted-idempotency.cjs}) that INVOKES the function once per
 * 		branch and reads the transmitted request body.  Runs when {@code node} is on {@code PATH} (skipped
 * 		otherwise - no {@code -Pjs-tests} required, mirroring {@code ViewsJs_DetailActionDialogRouting_Test}).
 * </ul>
 */
class ViewsJs_SelfTargetedIdempotency_Test extends TestBase {

	/** The one shape the opt-in-gated precedence rule may have. */
	private static final String TARGET_ID_STATEMENT =
		"const targetId = (modal?.selfTargeted && modal?.idempotencyKey != null) "
		+ "? modal.idempotencyKey : (tr?.dataset?.juneauRowId ?? null);";

	private static String viewsJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.VIEWS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.VIEWS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String rendersJs() throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(ViewsMixin.RENDERS_JS_RESOURCE)) {
			assertNotNull(in, () -> "missing classpath resource: " + ViewsMixin.RENDERS_JS_RESOURCE);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	/** Extracts a named function's body: from `function <name>(` to the next top-level `\n\t}`. */
	private static String functionBody(String body, String signature) {
		var start = body.indexOf(signature);
		assertTrue(start >= 0, () -> "'" + signature + "' not found:\n" + body);
		var end = body.indexOf("\n\t}", start);
		return body.substring(start, end < 0 ? body.length() : end);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Default-gate SHAPE pin: the statement itself, and no tr-null guard around the opt-in branch
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_sourceShape_targetIdStatementIsExactlyTheOptInGatedTernary() throws Exception {
		var fnBody = functionBody(viewsJs(), "function submitActionDialog(");
		var start = fnBody.indexOf("const targetId");
		assertTrue(start >= 0, () -> "submitActionDialog must compute a targetId:\n" + fnBody);
		var end = fnBody.indexOf(';', start);
		assertTrue(end >= 0, fnBody);
		// Whitespace-normalized so the statement may wrap across lines; everything else is pinned exactly - which
		// is what forbids `tr` appearing anywhere except the non-opt-in arm, guard included.
		var statement = fnBody.substring(start, end + 1).replaceAll("\\s+", " ");
		assertEquals(TARGET_ID_STATEMENT, statement,
			"the opt-in-gated precedence rule must have exactly this shape - a `tr` read in (or a guard around) "
			+ "the selfTargeted arm is the row-less regression this pin exists to catch");
	}

	@Test void a02_sourceShape_noRowNullConditionalAnywhereInTheSubmitSeam() throws Exception {
		var fnBody = functionBody(viewsJs(), "function submitActionDialog(");
		for (var guard : new String[]{"tr == null", "tr != null", "tr === null", "tr !== null", "!tr", "! tr"})
			assertFalse(fnBody.contains(guard),
				() -> "a `" + guard + "` conditional in submitActionDialog would gate the opt-in branch on the "
					+ "presence of a row, reviving the original row-less bug:\n" + fnBody);
		// Exactly one target computation, and exactly one attachment of it: a second computation further down could
		// quietly undo the first.
		assertTrue(fnBody.contains("if (targetId != null) extra.targetId = targetId;"), fnBody);
		var withoutAttachment = fnBody.replace("if (targetId != null) extra.targetId = targetId;", "");
		assertEquals(1, withoutAttachment.split("targetId =", -1).length - 1, withoutAttachment);
		// The rest of the seam is unchanged: the key attachment and the delegation to the row submit path.
		assertTrue(fnBody.contains("if (modal?.idempotencyKey != null) extra.idempotencyKey = modal.idempotencyKey;"), fnBody);
		assertTrue(fnBody.contains("submitRowAction(action, table, tr, ctx, extra);"), fnBody);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) Behavioral: the three branches, invoked, read off the transmitted request body
	//------------------------------------------------------------------------------------------------------------------

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
			var p = Path.of(basedir, "src/test/js/selftargeted-idempotency.cjs");
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			"src/test/js/selftargeted-idempotency.cjs",
			"juneau-rest/juneau-rest-server-views/src/test/js/selftargeted-idempotency.cjs"
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String runNode(Path harness, Path rendersJs, Path viewsJs) throws Exception {
		var stdout = Files.createTempFile("selftargeted-idempotency-stdout-", ".json");
		var stderr = Files.createTempFile("selftargeted-idempotency-stderr-", ".txt");
		try {
			var pb = new ProcessBuilder(List.of("node", harness.toString(), rendersJs.toString(), viewsJs.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile());
			var p = pb.start();
			if (!p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("selftargeted-idempotency.cjs did not finish within 30s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail("selftargeted-idempotency.cjs exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
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
		assumeTrue(report != null,
			"node not available or selftargeted-idempotency.cjs not found — behavioral layer skipped");
		return report;
	}

	@Test void b00_harnessReachedTheSubmitSeam() {
		assertEquals(true, report().get("hasSubmitActionDialog"), report()::toString);
	}

	/** Case (i): the opt-in wins over a real, DIFFERENT row id. */
	@Test void b01_optIn_withARow_transmitsTheKeyNotTheRowId() {
		var r = report();
		assertEquals(true, r.get("optIn_withRow_rowIdWasDifferent"), "the fixture must not accidentally agree");
		assertEquals("k-0123456789abcdef", r.get("optIn_withRow_targetId"),
			() -> "a selfTargeted modal's key must be transmitted as the target even when a row id exists: " + r);
		assertEquals("k-0123456789abcdef", r.get("optIn_withRow_idempotencyKey"), r::toString);
	}

	/**
	 * Case (ii): the direct regression guard.  A key with NO opt-in - the shape the example app's ack form uses,
	 * minted against a real row id - must keep transmitting that row's id, or its own binding check is vacuous.
	 */
	@Test void b02_noOptIn_withARow_transmitsTheRowIdNotTheKey() {
		var r = report();
		assertEquals("row-42", r.get("noOptIn_withRow_targetId"),
			() -> "an artifact-bound key must NOT hijack the target: this is the framework-wide regression that "
				+ "an unconditional precedence rule would cause for every existing present=dialog consumer: " + r);
		assertEquals("k-0123456789abcdef", r.get("noOptIn_withRow_idempotencyKey"),
			() -> "the key itself still rides the submit, unchanged: " + r);
	}

	/**
	 * Case (iii): the direct regression guard for the ORIGINAL row-less bug.  With no row there is nothing to fall
	 * back to, so a {@code tr == null} guard reintroduced around the opt-in branch would silently go back to
	 * omitting the target - and cases (i)/(ii) would both stay green while it did.
	 */
	@Test void b03_optIn_rowless_transmitsTheKeyWithNoRowToFallBackOn() {
		var r = report();
		assertEquals("k-0123456789abcdef", r.get("optIn_rowless_targetId"),
			() -> "a row-less selfTargeted submit must carry the key as its target, not omit the target: " + r);
		assertEquals("k-0123456789abcdef", r.get("optIn_rowless_idempotencyKey"), r::toString);
	}

	/** The fallbacks that must not move: no key, and an opt-in with no key to honour it with. */
	@Test void b04_fallbacks_areUnchanged() {
		var r = report();
		assertEquals("row-42", r.get("noKey_withRow_targetId"), r::toString);
		assertEquals(false, r.get("noKey_withRow_hasIdempotencyKey"), r::toString);
		assertEquals(false, r.get("noKey_rowless_hasTargetId"),
			() -> "with no key and no row there is no target to invent: " + r);
		assertEquals("row-42", r.get("optInNoKey_withRow_targetId"),
			() -> "the gate requires BOTH the opt-in and a key; with no key it falls back to the row id: " + r);
		assertEquals(false, r.get("optInNoKey_rowless_hasTargetId"), r::toString);
	}
}
