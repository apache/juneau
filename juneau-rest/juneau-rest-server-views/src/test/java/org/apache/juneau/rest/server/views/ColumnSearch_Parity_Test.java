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
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Always-on parity check that {@code juneau-search.js} filters byte-for-byte the way the server-side
 * {@link InMemoryBeanQueryContext} does.
 *
 * <p>
 * The Node harness ({@code src/test/js/column-search.cjs}) loads the served {@code juneau-search.js} asset into a
 * {@code vm} sandbox and re-runs the SAME shared {@code search-corpus.json} corpus that {@code SearchCorpus_Test}
 * runs against the Java engine, emitting one pass/fail flag per case id. Case ids are read directly from the
 * corpus resource (never hardcoded here), so adding or renaming a case in the corpus does not require touching
 * this file. Every case id is asserted here, so a divergence between the JS and Java engines names the exact case
 * that drifted. When Node is unavailable (or the harness script is not on any working-directory path), the whole
 * class skips.
 */
class ColumnSearch_Parity_Test extends TestBase {

	private static Map<?,?> report() {
		var r = run();
		assumeTrue(r != null, "node not available or column-search.cjs not found - JS parity layer skipped");
		return r;
	}

	@Test void a01_everyCorpusCaseMatchesServerSide() {
		var r = report();
		var ids = caseIds();
		// Guard against a harness that silently dropped cases: the count must match the ids asserted below.
		assertEquals((long) ids.size(), ((Number) r.get("caseCount")).longValue(),
			() -> "harness emitted a different case count than expected: " + r);
		for (var id : ids)
			assertEquals(true, r.get(id), () -> "column-search parity case " + id + " diverged; failures=" + r.get("failures"));
		assertEquals(true, r.get("allPass"), () -> "column-search parity failures: " + r.get("failures"));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Corpus case ids (mirror of SearchCorpus_Test.loadCorpus(), but only the "id" field is needed here).
	// -----------------------------------------------------------------------------------------------------------------

	@SuppressWarnings({
		"unchecked" // The (List<Map<String,Object>>) cast of the parsed JSON corpus is safe because the corpus file is a JSON array of objects.
	})
	private static List<String> caseIds() {
		try (var in = ColumnSearch_Parity_Test.class.getResourceAsStream(CORPUS)) {
			assertNotNull(in, CORPUS);
			var json = new String(in.readAllBytes(), UTF_8);
			var cases = (List<Map<String,Object>>) (List<?>) Json.to(json, List.class);
			var out = new ArrayList<String>(cases.size());
			for (var kase : cases)
				out.add((String) kase.get("id"));
			return out;
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Node plumbing (sibling of HelpersHarness, two assets: the rendered juneau-search.js plus the shared corpus).
	// -----------------------------------------------------------------------------------------------------------------

	private static final String HARNESS = "column-search.cjs";
	private static final String CORPUS = "search-corpus.json";
	private static Map<?,?> cached;
	private static boolean failed;

	private static synchronized Map<?,?> run() {
		if (failed)
			return null;
		if (cached != null)
			return cached;
		try {
			if (!nodeAvailable())
				return markUnavailable();
			var harness = locate("src/test/js/" + HARNESS);
			var corpus = locate("src/test/resources/org/apache/juneau/rest/server/views/" + CORPUS);
			if (harness == null || corpus == null)
				return markUnavailable();
			cached = Json.to(exec(harness, corpus), Map.class);
			return cached;
		} catch (Exception e) {
			throw new AssertionError("could not run " + HARNESS + ": " + e, e);
		}
	}

	private static Map<?,?> markUnavailable() {
		failed = true;
		return null;
	}

	private static String exec(Path harness, Path corpus) throws Exception {
		var search = Files.createTempFile("juneau-search-", ".js");
		var stdout = Files.createTempFile("column-search-stdout-", ".json");
		var stderr = Files.createTempFile("column-search-stderr-", ".txt");
		try {
			Files.writeString(search, asset(ViewsMixin.SEARCH_JS_RESOURCE), UTF_8);
			var p = new ProcessBuilder(List.of("node", harness.toString(), search.toString(), corpus.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(HARNESS + " did not finish within 60s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(HARNESS + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			for (var f : List.of(search, stdout, stderr))
				Files.deleteIfExists(f);
		}
	}

	private static String asset(String resource) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, resource);
			return new String(in.readAllBytes(), UTF_8);
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

	/** Locates a file given its path relative to this module's root, trying {@code basedir} first (Surefire's
	 * working directory under multi-module reactors), then a couple of relative fallbacks for IDE/ad-hoc runs. */
	private static Path locate(String relToModule) {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, relToModule);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of(
			relToModule,
			"juneau-rest/juneau-rest-server-views/" + relToModule
		)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}
}
