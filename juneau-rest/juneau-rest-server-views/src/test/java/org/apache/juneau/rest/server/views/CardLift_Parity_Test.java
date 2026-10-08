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
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Parity corpus ({@code card-lift-corpus.json}): cross-checks that {@code SelectionDef}'s field defaults and
 * {@code BulkMutateDef}'s wire form agree with the {@code selection}/{@code bulk} sub-shapes
 * {@code NS.card.liftCatalog} ({@code card-lift.cjs}) produces for the same corpus cases. This test does not build a
 * full SLOT_META envelope &mdash; there is no single Java method that does that; the JS leg is the authoritative
 * full-envelope check, and {@link #jsLeg_cardLiftHarnessPasses()} runs it.
 */
class CardLift_Parity_Test extends TestBase {

	private static List<JsonMap> cases() throws Exception {
		var corpus = JsonMap.ofString(Files.readString(Path.of("src/test/resources/card-lift-corpus.json")));
		var out = new ArrayList<JsonMap>();
		for (var o : corpus.getList("cases"))
			out.add((JsonMap)o);
		return out;
	}

	private static JsonMap toSelectionJson(SelectionDef d) {
		var m = new JsonMap();
		m.put("rowIdField", d.rowIdField());
		m.put("selectAll", d.selectAll());
		m.put("scope", d.scope().wire());
		if (d.labelField() != null)
			m.put("labelField", d.labelField());
		return m;
	}

	private static SelectionDef.Scope scopeOf(String wire) {
		for (var v : SelectionDef.Scope.values())
			if (v.wire().equals(wire))
				return v;
		return fail("unknown scope wire '" + wire + "'");
	}

	private static RowAction.BulkMode bulkModeOf(String wire) {
		for (var v : RowAction.BulkMode.values())
			if (v.wire().equals(wire))
				return v;
		return fail("unknown bulkMode wire '" + wire + "'");
	}

	/** {@code liftCatalog}'s selection defaults (selectAll true, scope persistent) are {@code SelectionDef}'s defaults. */
	@Test void selectionDefaults_matchLiftCatalogAssumptions() throws Exception {
		for (var c : cases()) {
			var catalogSelection = c.getMap("catalog").getMap("selection");
			var expectedSelection = c.getMap("expected") == null ? null : c.getMap("expected").getMap("selection");
			if (catalogSelection == null || expectedSelection == null)
				continue;
			var s = SelectionDef.create(catalogSelection.getString("rowIdField"));
			if (catalogSelection.containsKey("selectAll"))
				s.selectAll(catalogSelection.getBoolean("selectAll"));
			if (catalogSelection.containsKey("scope"))
				s.scope(scopeOf(catalogSelection.getString("scope")));
			if (catalogSelection.containsKey("labelField"))
				s.labelField(catalogSelection.getString("labelField"));
			// selectableWhen is passed through untouched by liftCatalog; the JS leg covers it.
			var expected = new JsonMap();
			expectedSelection.forEach((k, v) -> {
				if (! "selectableWhen".equals(k))
					expected.put(k, v);
			});
			assertEquals(expected, toSelectionJson(s), c.getString("name") + ": selection mismatch");
		}
	}

	/**
	 * The lifted bulk block carries {@code BulkMutateDef}'s own contract version, and each action's author key
	 * {@code mode} arrives as the wire key {@code bulkMode} with the same token {@code RowAction} serializes.
	 */
	@Test void bulkWireForm_matchesLiftCatalogAssumptions() throws Exception {
		for (var c : cases()) {
			var expectedBulk = c.getMap("expected") == null ? null : c.getMap("expected").getMap("bulk");
			if (expectedBulk == null)
				continue;
			assertEquals(BulkMutateDef.CONTRACT_VERSION, expectedBulk.getString("contractVersion"), c.getString("name"));
			for (var o : expectedBulk.getList("actions")) {
				var action = (JsonMap)o;
				assertFalse(action.containsKey("mode"), () -> c.getString("name") + ": author key 'mode' leaked to the wire");
				var mode = action.getString("bulkMode");
				if (mode == null)
					continue;
				var wire = Json.to(Json.of(RowAction.create(action.getString("id")).bulkMode(bulkModeOf(mode))), Map.class);
				assertEquals(mode, wire.get("bulkMode"), c.getString("name") + ": bulkMode token mismatch");
			}
		}
	}

	/** Standard operators carry no {@code help}; only custom operators do. */
	@Test void searchMeta_helpOnlyOnCustomOperators() {
		var meta = Column.searchMeta("a", "text", null, List.of(Map.of("name", "$mine", "help", "Mine only")));
		boolean sawCustom = false;
		for (var o : meta.getList("operators")) {
			var op = (Map<?,?>)o;
			if (Boolean.TRUE.equals(op.get("custom"))) {
				sawCustom = true;
				assertEquals("Mine only", op.get("help"));
			} else {
				assertFalse(op.containsKey("help"), () -> "standard operator carries help: " + op);
			}
		}
		assertTrue(sawCustom, meta.toString());
	}

	/** The JS leg: runs {@code card-lift.cjs} over the same corpus. */
	@Test void jsLeg_cardLiftHarnessPasses() throws Exception {
		assumeTrue(nodeAvailable(), "node not on PATH");
		var harness = locateHarness();
		assumeTrue(harness != null, "card-lift.cjs not found");
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		var stdout = Files.createTempFile("card-lift-stdout-", ".txt");
		var stderr = Files.createTempFile("card-lift-stderr-", ".txt");
		try {
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			var p = new ProcessBuilder(List.of("node", harness.toString(), rendersFile.toString(), viewsFile.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (! p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("card-lift.cjs did not finish within 30s; stderr:\n" + Files.readString(stderr, UTF_8));
			}
			assertEquals(0, p.exitValue(), () -> "card-lift.cjs failed:\n" + quietRead(stderr) + "\n" + quietRead(stdout));
		} finally {
			Files.deleteIfExists(viewsFile);
			Files.deleteIfExists(rendersFile);
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String resource(String name) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(name)) {
			assertNotNull(in);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p, UTF_8);
		} catch (IOException e) {
			return "(unreadable: " + e.getMessage() + ")";
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (! p.waitFor(5, TimeUnit.SECONDS)) {
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
			var p = Path.of(basedir, "src/test/js/card-lift.cjs");
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of("src/test/js/card-lift.cjs", "juneau-rest/juneau-rest-server-views/src/test/js/card-lift.cjs")) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		return null;
	}
}
