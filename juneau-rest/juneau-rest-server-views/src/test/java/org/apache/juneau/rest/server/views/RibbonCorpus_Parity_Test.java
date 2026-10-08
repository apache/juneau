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
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * JS layer of the ribbon parity corpus: runs {@code ribbon-corpus.cjs} over the shipped {@code juneau-ribbon.js} and
 * {@code juneau-search.js} and asserts, per case, that the ribbon encoding, the user/ribbon merge, the derived default
 * state, the client-mode row filter and the console-error contract all match {@code ribbon-corpus.json}.  Skipped (not
 * failed) when Node is not on the PATH.
 */
class RibbonCorpus_Parity_Test extends TestBase {

	private static final String HARNESS = "ribbon-corpus.cjs";
	private static Map<String,Map<?,?>> cached;

	@SuppressWarnings("unchecked")
	private static Map<String,Map<?,?>> report() {
		assumeTrue(nodeAvailable() && locate() != null, "node not available or " + HARNESS + " not found - JS ribbon parity layer skipped");
		if (cached == null) {
			try {
				var parsed = Json.to(exec(), Map.class);
				var byName = new LinkedHashMap<String,Map<?,?>>();
				for (var c : (List<Map<?,?>>) parsed.get("cases"))
					byName.put((String) c.get("name"), c);
				cached = byName;
			} catch (Exception e) {
				throw new AssertionError(HARNESS + " failed: " + e.getMessage(), e);
			}
		}
		return cached;
	}

	static Stream<String> names() throws Exception {
		return RibbonCorpus_Test.corpus().cases.stream().map(c -> c.name);
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("names")
	void a01_jsLayer(String name) {
		var r = report().get(name);
		assertNotNull(r, "harness reported no result for case '" + name + "'");
		assertNull(r.get("threw"), () -> name + ": harness threw " + r.get("threw"));
		for (var flag : List.of("stateOk", "columnSearchesOk", "queryParamsOk", "mergedOk", "rowIdsOk", "consoleErrorOk"))
			assertEquals(true, r.get(flag), () -> name + ": " + flag + " is false; actual=" + r.get("actual"));
	}

	@Test void a02_harnessCoversExactlyTheCorpusCases() throws Exception {
		assertEquals(names().toList(), new ArrayList<>(report().keySet()));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Node plumbing (sibling of ColumnSearch_Parity_Test).
	// -----------------------------------------------------------------------------------------------------------------

	private static String exec() throws Exception {
		var ribbon = Files.createTempFile("juneau-ribbon-", ".js");
		var search = Files.createTempFile("juneau-search-", ".js");
		var corpus = Files.createTempFile("ribbon-corpus-", ".json");
		var stdout = Files.createTempFile("ribbon-corpus-stdout-", ".json");
		var stderr = Files.createTempFile("ribbon-corpus-stderr-", ".txt");
		try {
			Files.writeString(ribbon, asset(ViewsMixin.RIBBON_JS_RESOURCE), UTF_8);
			Files.writeString(search, asset(ViewsMixin.SEARCH_JS_RESOURCE), UTF_8);
			Files.writeString(corpus, Json.of(RibbonCorpus_Test.corpus()), UTF_8);
			var p = new ProcessBuilder(List.of("node", locate().toString(), ribbon.toString(), search.toString(), corpus.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(HARNESS + " did not finish within 60s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(HARNESS + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr) + "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			for (var f : List.of(ribbon, search, corpus, stdout, stderr))
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

	private static Path locate() {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/" + HARNESS);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of("src/test/js/" + HARNESS, "juneau-rest/juneau-rest-server-views/src/test/js/" + HARNESS)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p)) return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}
}
