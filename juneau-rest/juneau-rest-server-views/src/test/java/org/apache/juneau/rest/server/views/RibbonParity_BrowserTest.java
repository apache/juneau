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

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The wiring layer of the ribbon parity contract: boots a REAL DataTable (jQuery + DataTables from the js-tests
 * npm install) with the real shipped scripts and checks that the shared {@code ribbon-corpus.json} semantics reach
 * the wire in server mode and the DOM in client mode.
 *
 * <p>
 * The pure layers are covered by {@link RibbonCorpus_Test} (Java) and {@link RibbonCorpus_Parity_Test} (JS); this
 * class proves the two callers - {@code buildServerAjax} and {@code installClientRibbonFilter} - actually feed them.
 * Nothing touches the network: the page and both data endpoints are answered by the prober's request routing.
 *
 * <p>
 * Disabled unless the {@value ConfigPersistence_BrowserTest#GATE} system property is set, which only the module's
 * opt-in {@code js-tests} Maven profile does.
 */
@EnabledIfSystemProperty(named=ConfigPersistence_BrowserTest.GATE, matches="true",
	disabledReason="JS-execution harness is opt-in; run with `mvn -Pjs-tests -f juneau-rest/juneau-rest-server-views/pom.xml test`")
class RibbonParity_BrowserTest extends TestBase {

	private static Map<?,?> report;

	private static String resource(String path) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	@BeforeAll
	static void probe() throws Exception {
		var dir = Path.of(requiredProperty("juneau.jsTests.dir"));
		var harness = Path.of(requiredProperty("juneau.jsTests.harness")).getParent().resolve("ribbon-parity-browser.cjs");
		var nodeModules = dir.resolve("node_modules");

		// Server table: corpus case jrm-dropped-only, persisted ON, so the FIRST request is already filtered.
		// Client table: corpus case foundry-review-phase with a default member, over the corpus rows.
		var serverDef = "{id:'srv',contractVersion:'5',dataMode:'server',dataUrl:'http://corpus.test/server-rows',"
			+ "columns:[{data:'id',title:'Id',searchable:false},{data:'status',title:'Status'}],"
			+ "ribbon:[{type:'option',id:'dropped-only',title:'Dropped only',column:'status',value:'$eq(DROPPED)',persist:true}]}";
		var clientDef = "{id:'cli',contractVersion:'5',dataMode:'client',dataUrl:'http://corpus.test/client-rows',"
			+ "columns:[{data:'id',title:'Id'},{data:'status',title:'Status'},{data:'stream',title:'Stream'},"
			+ "{data:'phase',title:'Phase'},{data:'isNew',title:'New'}],"
			+ "ribbon:[{type:'optionGroup',id:'reviewStateBucket',default:'reviewed',options:["
			+ "{id:'all',title:'All'},"
			+ "{id:'waiting',title:'Waiting',column:'phase',value:'$in(Waiting,\"Partially reviewed\")'},"
			+ "{id:'reviewed',title:'Reviewed',column:'phase',value:'$in(\"Ready to push\",\"Committed, not pushed\",Completed)'}]}]}";

		var fixture = "<!DOCTYPE html><html><head><meta charset=\"utf-8\"></head><body>\n"
			+ "<table id=\"srv\" data-juneau-view=\"srv\"></table>\n<table id=\"cli\" data-juneau-view=\"cli\"></table>\n"
			+ "<script>localStorage.setItem('juneau.view.srv.ribbon.dropped-only','true');</script>\n"
			+ script(Files.readString(nodeModules.resolve("jquery/dist/jquery.min.js")))
			+ script(Files.readString(nodeModules.resolve("datatables.net/js/dataTables.min.js")))
			+ script(resource(ViewsMixin.DATATABLES_JS_RESOURCE))
			+ script(resource(ViewsMixin.RENDERS_JS_RESOURCE))
			+ script(resource(ViewsMixin.VIEWS_JS_RESOURCE))
			+ script(resource(ViewsMixin.URLSTATE_JS_RESOURCE))
			+ script(resource(ViewsMixin.SEARCH_JS_RESOURCE))
			+ script(resource(ViewsMixin.PAGESTATE_JS_RESOURCE))
			+ script(resource(ViewsMixin.CONFIG_JS_RESOURCE))
			+ script(resource(ViewsMixin.RIBBON_JS_RESOURCE))
			+ script("JuneauViews.init.initTableFromDef(document.getElementById('srv')," + serverDef + ");\n"
				+ "JuneauViews.init.initTableFromDef(document.getElementById('cli')," + clientDef + ");\n"
				+ "window.__corpusReady = true;")
			+ "</body></html>";
		var fixtureFile = Files.createDirectories(dir.resolve("fixtures")).resolve("ribbon-parity.html");
		Files.write(fixtureFile, fixture.getBytes(UTF_8));

		report = Json.to(run(dir, harness, fixtureFile), Map.class);
	}

	private static String script(String body) {
		return "<script>\n" + body + "\n</script>\n";
	}

	private static String requiredProperty(String name) {
		var v = System.getProperty(name);
		assertNotNull(v, () -> "-D" + name + " not set; the js-tests profile is responsible for providing it");
		return v;
	}

	/** Runs the prober, failing with its stderr attached (its exit code alone is not a diagnosis). */
	private static String run(Path dir, Path harness, Path fixture) throws Exception {
		var cmd = List.of(System.getProperty("juneau.jsTests.node", "node"), harness.toString(), fixture.toString());
		var stdout = dir.resolve("ribbon-parity-stdout.json");
		var stderr = dir.resolve("ribbon-parity-stderr.txt");
		var pb = new ProcessBuilder(cmd).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
		pb.environment().put("NODE_PATH", dir.resolve("node_modules").toString());
		pb.environment().put("PLAYWRIGHT_BROWSERS_PATH", requiredProperty("juneau.jsTests.browsers"));
		pb.environment().put("CORPUS_ROWS", Json.of(RibbonCorpus_Test.corpus().rows));

		var p = pb.start();
		if (! p.waitFor(3, TimeUnit.MINUTES)) {
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

	@Test void a01_serverMode_firstRequestBodyCarriesTheRibbonSearch() {
		assertEquals("$eq(DROPPED)", report.get("serverFirstStatusSearch"), report::toString);
	}

	@Test void a02_serverMode_togglingOffClearsIt() {
		var v = report.get("serverAfterToggleStatusSearch");
		assertTrue(v == null || "".equals(v), report::toString);
	}

	@Test void b01_clientMode_defaultMemberFiltersOnFirstDraw() {
		assertEquals(3, ((Number)report.get("clientInitialRows")).intValue(), report::toString);   // rows 3, 4, 6
	}

	@Test void b02_clientMode_allShowsEveryRow() {
		assertEquals(6, ((Number)report.get("clientAllRows")).intValue(), report::toString);
	}

	@Test void b03_clientMode_switchingMemberRefilters() {
		assertEquals(2, ((Number)report.get("clientWaitingRows")).intValue(), report::toString);   // rows 1, 2
	}

	@Test void c01_noConsoleErrors() {
		assertEquals(List.of(), report.get("errors"), report::toString);
	}
}
