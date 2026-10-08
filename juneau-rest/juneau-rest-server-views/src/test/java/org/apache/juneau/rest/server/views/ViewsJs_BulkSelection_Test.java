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
import org.junit.jupiter.api.*;

/**
 * Runs the always-on {@code bulk-selection.cjs} Node harness: persistent selection scope, the tri-state select-all
 * header, {@code selectableWhen} gating and per-id row snapshots, on the dependency-free views DOM shim.  Every
 * assertion lives in the harness; this wrapper only locates it, feeds it the served scripts and checks the exit code.
 */
class ViewsJs_BulkSelection_Test extends TestBase {

	@Test void a01_bulkSelectionHarnessPasses() throws Exception {
		assumeTrue(nodeAvailable(), "node not on PATH");
		var harness = locateHarness();
		assumeTrue(harness != null, "bulk-selection.cjs not found");
		var viewsFile = Files.createTempFile("juneau-views-", ".js");
		var rendersFile = Files.createTempFile("juneau-renders-", ".js");
		var stdout = Files.createTempFile("bulk-selection-stdout-", ".txt");
		var stderr = Files.createTempFile("bulk-selection-stderr-", ".txt");
		try {
			Files.writeString(viewsFile, resource(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(rendersFile, resource(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			var p = new ProcessBuilder(List.of("node", harness.toString(), rendersFile.toString(), viewsFile.toString()))
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (! p.waitFor(30, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail("bulk-selection.cjs did not finish within 30s; stderr:\n" + Files.readString(stderr, UTF_8));
			}
			assertEquals(0, p.exitValue(), () -> "bulk-selection.cjs failed:\n" + quietRead(stderr) + "\n" + quietRead(stdout));
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
			var p = Path.of(basedir, "src/test/js/bulk-selection.cjs");
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of("src/test/js/bulk-selection.cjs", "juneau-rest/juneau-rest-server-views/src/test/js/bulk-selection.cjs")) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		return null;
	}
}
