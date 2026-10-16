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
package org.apache.juneau.rest.server.view.freemarker.console;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

/**
 * Pins the Java chart handler to the JS chart row that {@code bus-wiring.cjs} builds from the real handler that
 * {@code juneau-charts.js} registers: the ops and roles {@link ChartCardType} accepts, and the implicit
 * {@code zoom:}/{@code legend:} topics it reports, must equal that handler's {@code ops}, {@code roles} and
 * {@code implicit}.  The one allowed difference is {@code refresh},
 * the shell's own op (it is handled before the handler's ops are consulted), which Java lists and the JS table does
 * not.  Gated on {@code node} being on {@code PATH}.
 */
class ChartWiring_JsParity_Test extends TestBase {

	private static final Path VIEWS = Path.of(System.getProperty("basedir", "."), "../juneau-rest-server-views");

	private static final String SHELL_OP = "refresh";

	private static JsonMap report;  // computed once: spawning node three times is wasted work

	private static synchronized JsonMap chartReport() throws Exception {
		if (report == null)
			report = runHarness();
		return report;
	}

	private static JsonMap runHarness() throws Exception {
		var harness = VIEWS.resolve("src/test/js/bus-wiring.cjs");
		var bus = VIEWS.resolve("src/main/resources/org/apache/juneau/views/juneau-bus.js");
		var charts = VIEWS.resolve("../juneau-rest-server-console-ui/src/main/resources/org/apache/juneau/console/juneau-charts.js");
		assumeTrue(Files.exists(harness) && Files.exists(bus) && Files.exists(charts), "views or console-ui module sources not found");
		try {
			assumeTrue(new ProcessBuilder("node", "--version").redirectErrorStream(true).start().waitFor(20, TimeUnit.SECONDS), "node not on PATH");
		} catch (java.io.IOException e) {
			assumeTrue(false, "node not on PATH");
		}
		var stdout = Files.createTempFile("chart-parity-stdout-", ".json");
		var stderr = Files.createTempFile("chart-parity-stderr-", ".txt");
		try {
			var p = new ProcessBuilder("node", harness.toAbsolutePath().normalize().toString(), bus.toAbsolutePath().normalize().toString(),
				charts.toAbsolutePath().normalize().toString())
				.redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
			if (! p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("bus-wiring.cjs did not finish within 60s");
			}
			assertEquals(0, p.exitValue(), () -> "bus-wiring.cjs failed: " + quietRead(stderr));
			return JsonMap.ofString(Files.readString(stdout, UTF_8)).getMap("chart");
		} finally {
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static String quietRead(Path f) {
		try {
			return Files.readString(f, UTF_8);
		} catch (java.io.IOException e) {
			return "(unreadable: " + e + ")";
		}
	}

	@Test void a01_ops_matchTheJsTable() throws Exception {
		var js = new TreeSet<String>();
		for (var o : chartReport().getList("ops"))
			js.add((String)o);
		var java = new TreeSet<>(new ChartCardType().acceptedOps());
		assertTrue(java.remove(SHELL_OP), "Java lists the shell's refresh op");
		assertEquals(js, java, "ChartCardType.acceptedOps() minus the shell's refresh must equal the juneau-charts.js handler's ops");
	}

	@Test void a02_roles_matchTheJsTable() throws Exception {
		var js = new TreeSet<String>();
		for (var o : chartReport().getList("roles"))
			js.add((String)o);
		assertEquals(js, new TreeSet<>(new ChartCardType().acceptedRoles()));
	}

	@Test void a03_implicitTopics_matchTheJsTable() throws Exception {
		var samples = chartReport().getList("samples");
		assertSize(6, samples);
		for (var s : samples) {
			var sample = (Map<?,?>)s;
			var card = new JsonMap((Map<?,?>)sample.get("card"));
			assertEquals(sample.get("implicit"), new ChartCardType().implicitTopics(card), () -> "card " + card);
		}
	}

	// ---- BusWiringValidator.LEGACY_HELPER_KEYS mirrors ChartDef.HELPER_KEYS (the console-ui module cannot see ChartDef) --------

	private static List<String> e74(String key) {
		// The value nests the marker one level down so that it is reached for every key, including 'series' (whose map and
		// list entries skip their 'data' key, but not 'label').
		var contract = JsonMap.ofString("{\"version\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":["
			+ "{\"id\":\"t\",\"type\":\"chart\",\"kind\":\"line\",\"" + key + "\":{\"label\":{\"$fn\":\"Bad\"}}}]}");
		return BusWiringValidator.validate(contract, CardTypeRegistry.standard()).stream()
			.filter(p -> "E-74".equals(p.code())).map(Object::toString).toList();
	}

	@Test void a04_everyHelperKey_isWalkedForFnNames_inTheValidator() {
		assertFalse(ChartDef.HELPER_KEYS.isEmpty());
		for (var key : new TreeSet<>(ChartDef.HELPER_KEYS))
			assertEquals(List.of("E-74 'Bad' is not a valid $fn name"), e74(key), () -> "legacy helper key '" + key + "'");
		assertEquals(List.of(), e74("title"), "a non-helper key is not walked");
	}
}
