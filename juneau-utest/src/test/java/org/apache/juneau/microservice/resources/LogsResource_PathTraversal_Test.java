// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.microservice.resources;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Regression tests for path-traversal hardening in {@link LogsResource}.
 *
 * <p>
 * Paths escaping the configured log directory must be rejected with 403 and must neither be read nor deleted.
 */
class LogsResource_PathTraversal_Test extends TestBase {

	@TempDir
	static Path tempDir;

	static Path logDir;
	static Path outsideSecret;

	@BeforeAll
	static void setup() throws Exception {
		logDir = tempDir.resolve("logs");
		Files.createDirectories(logDir.resolve("sub"));
		Files.writeString(logDir.resolve("inside.log"), "INSIDE_LOG");
		Files.writeString(logDir.resolve("sub/nested.log"), "NESTED_LOG");
		outsideSecret = tempDir.resolve("outside-secret.txt");
		Files.writeString(outsideSecret, "AUDIT_OUTSIDE_SECRET");
		var cfg = tempDir.resolve("logs-test.cfg");
		Files.writeString(cfg, "[Logging]\nlogDir = " + logDir.toString().replace("\\", "/") + "\nallowDeletes = true\n");
		System.setProperty("LogsResource_PathTraversal_Test.cfg", cfg.toString());
	}

	@AfterAll
	static void teardown() {
		System.clearProperty("LogsResource_PathTraversal_Test.cfg");
	}

	@Rest(config = "$S{LogsResource_PathTraversal_Test.cfg}", allowedMethodParams = "*")
	public static class TestLogsResource extends LogsResource {
		private static final long serialVersionUID = 1L;
	}

	private static MockRestClient client() {
		return MockRestClient.buildLax(TestLogsResource.class);
	}

	@Test void t01_legitFiles() throws Exception {
		try (var c = client()) {
			c.request("VIEW", "/inside.log").run().assertStatus(200).assertContent().is("INSIDE_LOG");
			c.request("VIEW", "/sub/nested.log").run().assertStatus(200).assertContent().is("NESTED_LOG");
			c.get("/sub").run().assertStatus(200);
			c.get("/").run().assertStatus(200);
		}
	}

	@Test void t02_missingFile_404() throws Exception {
		try (var c = client()) {
			c.request("VIEW", "/nope.log").run().assertStatus(404);
		}
	}

	@Test void t03_traversal_view_download_get() throws Exception {
		try (var c = client()) {
			for (var p : new String[]{"/../outside-secret.txt", "/%2e%2e/outside-secret.txt", "/%2e%2e%2foutside-secret.txt", "/sub/../../outside-secret.txt"}) {
				for (var m : new String[]{"VIEW", "DOWNLOAD", "GET", "PARSE"}) {
					var r = c.request(m, p).run();
					assertNotEquals(200, r.getStatusCode(), m + " " + p);
					assertFalse(r.getContent().asString().contains("AUDIT_OUTSIDE_SECRET"), m + " " + p + " leaked");
				}
			}
			c.request("VIEW", "/../outside-secret.txt").run().assertStatus(403);
		}
	}

	@Test void t04_traversal_delete_leavesOutsideFile() throws Exception {
		try (var c = client()) {
			for (var p : new String[]{"/../outside-secret.txt", "/%2e%2e/outside-secret.txt", "/%2e%2e%2foutside-secret.txt"}) {
				var r = c.delete(p).run();
				assertNotEquals(200, r.getStatusCode(), "DELETE " + p);
				assertNotEquals(303, r.getStatusCode(), "DELETE " + p);
				assertTrue(Files.exists(outsideSecret), "Outside file deleted by DELETE " + p);
			}
			// Deleting the parent directory of logDir must also be refused.
			c.delete("/..").run();
			assertTrue(Files.exists(outsideSecret));
			assertTrue(Files.exists(logDir.resolve("inside.log")));
		}
	}
}
