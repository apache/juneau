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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The real-browser canary for client-mode column-search DSL evaluation (WORK-J0612): real DataTables 2.1.8, a
 * client-side table, and {@code JuneauViews.init.setColumnExpr(...)} installing the {@code juneau-dsl}
 * {@code column().search.fixed} predicate built by {@code juneau-search.js}.
 *
 * <p>
 * The node-only {@link ViewsJs_ClientColumnSearch_Test} covers the semantics against fake column APIs; this canary
 * proves the one thing a fake cannot - that the real library's {@code search.fixed} contract (argument order, raw
 * row data, composition with the global search box, {@code col.search()} staying empty) matches what the views
 * runtime assumes.  {@code client-column-search-browser.cjs} drives it with headless Chromium.
 *
 * <h5 class='section'>Off by default:</h5>
 * <p>
 * Disabled unless the {@value #GATE} system property is set, which only the module's opt-in {@code js-tests} Maven
 * profile does: {@code mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test}.  That profile also
 * installs the {@code jquery} and {@code datatables.net} npm packages the fixture page inlines.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul>
 * 	<li class='jc'>{@link DataTablesAjax_BrowserTest} &mdash; the sibling real-DataTables canary (server mode).
 * </ul>
 */
@EnabledIfSystemProperty(named=ClientColumnSearch_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class ClientColumnSearch_BrowserTest extends TestBase {

	/** System property the {@code js-tests} profile sets to enable this class. */
	static final String GATE = "juneau.jsTests";

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("client-column-search-browser.cjs");
		var nodeModules = dir.resolve("node_modules");
		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n"
			+ "<table id=\"t\"></table>\n"
			+ "<script>\n" + Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js")) + "\n</script>\n"
			+ "<script>\n" + Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js")) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.RENDERS_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.VIEWS_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.URLSTATE_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n" + resource(ViewsMixin.SEARCH_JS_RESOURCE) + "\n</script>\n"
			+ "<script>\n"
			+ "window.__clientDt = new DataTable('#t', { data: ["
			+ "{id:'r1',status:'Triaged',name:'alpha'},{id:'r2',status:'New',name:'beta'},"
			+ "{id:'r3',status:'Closed',name:'gamma'},{id:'r4',status:'Open',name:'delta'},"
			+ "{id:'r5',status:'Tri',name:'epsilon'}],"
			+ " columns: [{data:'id',title:'ID'},{data:'status',title:'Status'},{data:'name',title:'Name'}] });\n"
			+ "</script>\n"
			+ "</body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("client-column-search.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));
		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("client-column-search-stdout.json");
		var stderr = dir.resolve("client-column-search-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));
		var p = pb.start();
		if (!p.waitFor(3, TimeUnit.MINUTES)) {
			p.destroyForcibly();
			fail("prober did not finish within 3m; stderr:\n" + quietRead(stderr));
		}
		assertEquals(0, p.exitValue(), () -> "prober exited non-zero; stderr:\n" + quietRead(stderr));
		return Files.readString(stdout);
	}

	private static String quietRead(Path p) {
		try {
			return Files.readString(p);
		} catch (IOException e) {
			return "<unreadable: " + e + ">";
		}
	}

	@Test void a01_noConsoleOrPageErrors() {
		assertEmpty(report::toString, report.get("jsFailures"));
	}

	@Test void a02_inShowsExactlyTheTwoMatchingRows() {
		// "Tri" (r5) must NOT match: $in is whole-value, unlike DataTables' native smart substring search.
		assertBean(report::toString, report, "hasSearchFixed,before,setOk,filtered,nativeSearch,storedExpr",
			"true,[r1,r2,r3,r4,r5],true,[r1,r2],,$in(Triaged,New)");
	}

	@Test void a03_globalSearchBoxStillWorksAlongsideTheColumnPredicate() {
		assertBean(report::toString, report, "hasGlobalBox,globalAndColumn,globalCleared", "true,[r2],[r1,r2]");
	}

	@Test void a04_invalidExpressionInstallsNothing() {
		assertBean(report::toString, report, "badOk,badCode,afterBad", "false,OPERATOR_TYPE,[r1,r2]");
	}

	@Test void a05_copyLinkCarriesTheExpression() {
		assertContains(report::toString, "filter(status=$in(Triaged,New))", String.valueOf(report.get("shareUrl")));
	}
}
