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
package org.apache.juneau.rest.server.datatables;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
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
 * Behavioral (Node, skipped when node is absent) coverage of {@code juneau-datatables.js}: an HTTP 400 carrying
 * {@code X-BeanQuery-Error} is shown with the server's message through the DataTables error path, and every other
 * failure keeps the DataTables default.
 */
class DataTablesClientErrors_Test extends TestBase {

	private static Map<?,?> report;

	@BeforeAll
	static void probe() throws Exception {
		var harness = locate("src/test/js/datatables-error.cjs", "juneau-rest/juneau-rest-server-datatables/src/test/js/datatables-error.cjs");
		if (harness == null || ! nodeAvailable())
			return;
		var js = Files.createTempFile("juneau-datatables-", ".js");
		var out = Files.createTempFile("datatables-error-out-", ".json");
		var err = Files.createTempFile("datatables-error-err-", ".txt");
		try (var in = DataTablesMixin.class.getResourceAsStream("juneau-datatables.js")) {
			assertNotNull(in);
			Files.write(js, in.readAllBytes());
			var p = new ProcessBuilder("node", harness.toString(), js.toString()).redirectOutput(out.toFile()).redirectError(err.toFile()).start();
			if (! p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("datatables-error.cjs timed out");
			}
			if (p.exitValue() != 0)
				fail("datatables-error.cjs exited " + p.exitValue() + ": " + Files.readString(err, UTF_8));
			report = Json.to(Files.readString(out, UTF_8), Map.class);
		} finally {
			Files.deleteIfExists(js);
			Files.deleteIfExists(out);
			Files.deleteIfExists(err);
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
		} catch (IOException | InterruptedException e) {
			return false;
		}
	}

	private static Path locate(String... rels) {
		for (var rel : rels) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static Map<?,?> r() {
		assumeTrue(report != null, "node not available or harness not found - behavioral layer skipped");
		return report;
	}

	@Test void a01_listenerBoundAndBodyStillSerialized() {
		assertBean(r(), "bound,bodyString", "true,2");
	}

	@Test void a02_serverMessageShownForBeanQuery400() {
		assertBean(r(), "handled,alert1,event1", "true,DataTables warning: table id=rel - Unknown column 'x'.,error.dt");
	}

	@Test void a03_plainTextBodyAndCodeFallback() {
		assertBean(r(), "handledPlain,alert2,handledEmpty,alert3",
			"true,DataTables warning: table id=rel - Bad op.,true,DataTables warning: table id=rel - Invalid query (TOO_MANY_CLAUSES).");
	}

	@Test void a04_otherFailuresKeepDataTablesDefault() {
		assertBean(r(), "ignoredNoHeader,ignored500,ignoredSuccess,alertCount", "true,true,true,3");
	}

	@Test void a05_errModeHonored() {
		assertBean(r(), "noneAlerts,fnMode", "3,DataTables warning: table id=rel - fn");
	}

	@Test void a06_htmlBodyFallsBackToCode() {
		assertEquals("Invalid query (C).", r().get("helperMessage"));
	}
}
