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
package org.apache.juneau.rest.server.console;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Mechanical, {@code basedir}-relative assertion of the three-module dependency graph.
 *
 * <p>
 * This is an <b>invariant/regression assertion, not a shown-failing-first TDD RED</b>: the
 * {@code view-freemarker ↛ -datatables} half is already true on {@code main} before this feature exists. It fails
 * only if someone later wires a forbidden edge (e.g. points {@code console-ui-freemarker} at {@code -datatables}),
 * and passes once the module graph is exactly what is documented:
 *
 * <pre>
 * console-ui                       → rest-server
 * console-ui-freemarker            → console-ui + view-freemarker     (NOT -datatables)
 * console-ui-freemarker-datatables → console-ui-freemarker + datatables (the ONLY module allowed to touch both)
 * datatables                       → rest-server + bean-html5          (NOT console-ui — stays generic)
 * view-freemarker                  → rest-server + freemarker          (NOT -datatables — untouched)
 * </pre>
 *
 * <p>
 * Reads sibling {@code pom.xml} files by a {@code basedir}-relative file read (Maven Surefire's default working
 * directory is the module's own {@code basedir}, so {@code user.dir}'s parent is {@code juneau-rest/}) rather than
 * as test <i>resources</i>, which would be fragile (resources are copied per-module, not shared across siblings).
 */
class ModuleGraph_Test extends TestBase {

	private static String pomOf(String moduleArtifactId) throws IOException {
		var restDir = new File(System.getProperty("user.dir")).getParentFile();
		var pom = new File(restDir, moduleArtifactId + "/pom.xml");
		assertTrue(pom.isFile(), () -> "Expected sibling pom not found: " + pom.getAbsolutePath());
		return Files.readString(pom.toPath());
	}

	private static boolean hasDependency(String pomXml, String artifactId) {
		return pomXml.contains("<artifactId>" + artifactId + "</artifactId>");
	}

	@ParameterizedTest
	@ValueSource(strings = {
		"juneau-rest-server-view-freemarker",
		"juneau-rest-server-console-ui-freemarker",
		"juneau-rest-server-console-ui",
	})
	void a01_moduleDoesNotDependOnDatatables(String moduleArtifactId) throws IOException {
		var pom = pomOf(moduleArtifactId);
		assertFalse(hasDependency(pom, "juneau-rest-server-datatables"));
	}

	@Test void a04_datatables_doesNotDependOnConsoleUiFamily() throws IOException {
		var pom = pomOf("juneau-rest-server-datatables");
		assertFalse(hasDependency(pom, "juneau-rest-server-console-ui"));
		assertFalse(hasDependency(pom, "juneau-rest-server-console-ui-freemarker"));
		assertFalse(hasDependency(pom, "juneau-rest-server-console-ui-freemarker-datatables"));
	}

	@Test void a05_consoleUiFreemarkerDatatables_dependsOnBothHalves() throws IOException {
		var pom = pomOf("juneau-rest-server-console-ui-freemarker-datatables");
		assertContainsAll(pom, "<artifactId>juneau-rest-server-console-ui-freemarker</artifactId>", "<artifactId>juneau-rest-server-datatables</artifactId>");
	}

	@Test void a06_consoleUi_dependsOnlyOnRestServer() throws IOException {
		var pom = pomOf("juneau-rest-server-console-ui");
		assertTrue(hasDependency(pom, "juneau-rest-server"));
	}

	@Test void a07_consoleUiFreemarker_dependsOnConsoleUiAndViewFreemarker() throws IOException {
		var pom = pomOf("juneau-rest-server-console-ui-freemarker");
		assertContainsAll(pom, "<artifactId>juneau-rest-server-console-ui</artifactId>", "<artifactId>juneau-rest-server-view-freemarker</artifactId>");
	}

	/**
	 * {@code console-ui} stays engine- and toolkit-agnostic: it must not depend on the {@code views} toolkit.
	 */
	@Test void a12_consoleUi_doesNotDependOnViews() throws IOException {
		var pom = pomOf("juneau-rest-server-console-ui");
		assertFalse(hasDependency(pom, "juneau-rest-server-views"));
	}

	/** C4 deleted the theme-packs module; custom palettes are FTL-authored, so nothing may re-introduce it. */
	@Test void a13_restParentPom_listsNoThemePacksModule() throws IOException {
		var restPom = Files.readString(new File(new File(System.getProperty("user.dir")).getParentFile(), "pom.xml").toPath());
		assertFalse(restPom.contains("<module>juneau-rest-server-theme-packs</module>"));
	}

	/**
	 * {@code PageContractAssert} (C1-D3) ships at main scope so adopters can use it under any test framework;
	 * it must not pull in JUnit or opentest4j.
	 */
	@Test void a20_pageContractAssert_hasNoTestFrameworkImports() throws Exception {
		var src = java.nio.file.Files.readString(java.nio.file.Path.of(System.getProperty("user.dir"),
			"src/main/java/org/apache/juneau/rest/server/console/test/PageContractAssert.java"));
		assertFalse(src.contains("org.junit"), "PageContractAssert must not import JUnit");
		assertFalse(src.contains("org.opentest4j"), "PageContractAssert must not import opentest4j");
	}

	/**
	 * {@code views} now composes the console shell script (C1 Task 5) by taking a compile-scope dependency on
	 * {@code console-ui}; that edge is one-directional &mdash; {@code console-ui} stays engine/toolkit-agnostic and
	 * must still not depend back on {@code views} (see {@link #a12_consoleUi_doesNotDependOnViews()}).
	 */
	@Test void a21_viewsDependsOnConsoleUi_consoleUiStillNotOnViews() throws IOException {
		var viewsPom = pomOf("juneau-rest-server-views");
		assertTrue(hasDependency(viewsPom, "juneau-rest-server-console-ui"));
		var consoleUiPom = pomOf("juneau-rest-server-console-ui");
		assertFalse(hasDependency(consoleUiPom, "juneau-rest-server-views"));
	}
}
