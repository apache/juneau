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
package org.apache.juneau.petstore.console;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * Fails when something the petstore docs tour (pages 19.01-19.18) documents disappears from the code, or when a
 * FailLoud test is added without a docs row.
 *
 * <p>
 * The manifest {@code docs-catalogue.txt} has two row forms (blank lines and {@code #} comments are ignored):
 * <ul>
 * 	<li>{@code page|<P-id>|<path>|<docs file>} for each documented console page.
 * 	<li>{@code failloud|<test method>|<fixture file or ->|<asserted code or ->} for each {@link PetstoreFailLoud_Test} row.
 * </ul>
 *
 * <p>
 * Rules (a)-(e) always run.  Rule (f), the docs-checkout check, runs only when the system property
 * {@code juneau.docs.dir} names a juneau-docs checkout.
 */
class PetstoreDocsCatalogue_Test {

	private static final String MANIFEST = "/org/apache/juneau/petstore/console/docs-catalogue.txt";
	private static final String FIXTURE_DIR = "org/apache/juneau/petstore/console/failloud/";
	private static final Path FAILLOUD_SOURCE = Path.of("src/test/java/org/apache/juneau/petstore/console/PetstoreFailLoud_Test.java");
	private static final String FAILLOUD_DOC = "19.18.PetstoreFailLoud.md";

	record PageRow(String id, String path, String docsFile) {}
	record FailLoudRow(String method, String fixture, String code) {}

	private static final List<PageRow> PAGE_ROWS = new ArrayList<>();
	private static final List<FailLoudRow> FAILLOUD_ROWS = new ArrayList<>();

	@BeforeAll static void load() throws IOException {
		PAGE_ROWS.clear();
		FAILLOUD_ROWS.clear();
		try (var in = PetstoreDocsCatalogue_Test.class.getResourceAsStream(MANIFEST)) {
			assertNotNull(in, "Missing manifest " + MANIFEST);
			var n = 0;
			for (var line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
				n++;
				var l = line.strip();
				if (l.isEmpty() || l.startsWith("#"))
					continue;
				var f = l.split("\\|", -1);
				if (f.length == 4 && f[0].equals("page"))
					PAGE_ROWS.add(new PageRow(f[1], f[2], f[3]));
				else if (f.length == 4 && f[0].equals("failloud"))
					FAILLOUD_ROWS.add(new FailLoudRow(f[1], f[2], f[3]));
				else
					fail("Manifest line " + n + " is malformed: " + l);
			}
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - pages
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_manifestIsNotEmpty() {
		assertFalse(PAGE_ROWS.isEmpty(), "No page rows in the docs manifest");
		assertFalse(FAILLOUD_ROWS.isEmpty(), "No failloud rows in the docs manifest");
	}

	@Test void a02_everyDocumentedPageIsStillTested() {
		var live = new HashSet<String>();
		for (var p : PetstorePages_ContractTest.PAGES)
			live.add(p.id() + "|" + p.path());
		var missing = new ArrayList<String>();
		for (var r : PAGE_ROWS) {
			var builderOnly = PetstorePages_ContractTest.BUILDER_ONLY.contains(r.path());
			if (! builderOnly && ! live.contains(r.id() + "|" + r.path()))
				missing.add(r.id() + " " + r.path());
		}
		assertTrue(missing.isEmpty(), "Documented pages missing from PetstorePages_ContractTest.PAGES / BUILDER_ONLY: " + missing);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - FailLoud
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_everyCataloguedMethodExists() {
		var names = new HashSet<String>();
		for (var m : PetstoreFailLoud_Test.class.getDeclaredMethods())
			names.add(m.getName());
		var missing = FAILLOUD_ROWS.stream().map(FailLoudRow::method).filter(m -> ! names.contains(m)).toList();
		assertTrue(missing.isEmpty(), "Catalogued FailLoud methods not found on PetstoreFailLoud_Test: " + missing);
	}

	@Test void b02_everyCataloguedFixtureResolves() {
		var missing = new ArrayList<String>();
		for (var r : FAILLOUD_ROWS)
			if (! r.fixture().equals("-") && PetstoreDocsCatalogue_Test.class.getClassLoader().getResource(FIXTURE_DIR + r.fixture()) == null)
				missing.add(r.fixture());
		assertTrue(missing.isEmpty(), "Catalogued fixtures not on the classpath: " + missing);
	}

	@Test void b03_everyCataloguedCodeIsAsserted() throws IOException {
		var statements = assertionStatements(stripComments(Files.readString(FAILLOUD_SOURCE)));
		var missing = new ArrayList<String>();
		for (var r : FAILLOUD_ROWS)
			if (! r.code().equals("-") && statements.stream().noneMatch(s -> s.contains(r.code())))
				missing.add(r.method() + " -> " + r.code());
		assertTrue(missing.isEmpty(), "Catalogued codes not found inside an assertion in PetstoreFailLoud_Test (comments do not count): " + missing);
	}

	@Test void b04_everyFailLoudTestIsCatalogued() {
		var catalogued = new HashSet<String>();
		for (var r : FAILLOUD_ROWS)
			catalogued.add(r.method());
		var missing = new ArrayList<String>();
		for (var m : PetstoreFailLoud_Test.class.getDeclaredMethods())
			if (m.isAnnotationPresent(Test.class) && ! catalogued.contains(m.getName()))
				missing.add(m.getName());
		assertTrue(missing.isEmpty(), "PetstoreFailLoud_Test methods with no docs-catalogue.txt row (document them in 19.18): " + missing);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - docs checkout (optional)
	//-----------------------------------------------------------------------------------------------------------------

	// disabledReason (unlike a failed assumption) lands in the skipped test's message in the Surefire report.
	@Test
	@EnabledIfSystemProperty(named="juneau.docs.dir", matches=".+", disabledReason="juneau.docs.dir is not set; skipping the docs-checkout check")
	void c01_docsCheckoutMentionsEveryRow() throws IOException {
		var dir = System.getProperty("juneau.docs.dir");
		var topics = Path.of(dir, "pages", "topics");
		var problems = new ArrayList<String>();
		for (var r : PAGE_ROWS)
			if (! Files.isRegularFile(topics.resolve(r.docsFile())))
				problems.add("missing docs file " + r.docsFile() + " (" + r.id() + ")");
		var fl = topics.resolve(FAILLOUD_DOC);
		if (! Files.isRegularFile(fl)) {
			problems.add("missing docs file " + FAILLOUD_DOC);
		} else {
			var text = Files.readString(fl);
			for (var r : FAILLOUD_ROWS) {
				var id = r.method().substring(0, r.method().indexOf('_'));
				if (! text.contains(id))
					problems.add(FAILLOUD_DOC + " does not mention test id " + id);
				if (! r.code().equals("-") && ! text.contains(r.code()))
					problems.add(FAILLOUD_DOC + " does not mention code " + r.code());
			}
		}
		assertTrue(problems.isEmpty(), "Docs checkout out of step with docs-catalogue.txt: " + problems);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// helpers
	//-----------------------------------------------------------------------------------------------------------------

	/** Removes {@code //} and block comments, leaving string and char literals intact. */
	static String stripComments(String src) {
		var sb = new StringBuilder();
		var i = 0;
		var n = src.length();
		while (i < n) {
			var c = src.charAt(i);
			if (c == '"' || c == '\'') {
				var j = i + 1;
				while (j < n && src.charAt(j) != c) {
					if (src.charAt(j) == '\\')
						j++;
					j++;
				}
				sb.append(src, i, Math.min(j + 1, n));
				i = j + 1;
			} else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
				while (i < n && src.charAt(i) != '\n')
					i++;
			} else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
				var end = src.indexOf("*/", i + 2);
				i = end < 0 ? n : end + 2;
			} else {
				sb.append(c);
				i++;
			}
		}
		return sb.toString();
	}

	/** Statements (split on {@code ;}) that contain an {@code assert} call. */
	static List<String> assertionStatements(String code) {
		var out = new ArrayList<String>();
		for (var s : code.split(";"))
			if (s.contains("assert"))
				out.add(s);
		return out;
	}
}
