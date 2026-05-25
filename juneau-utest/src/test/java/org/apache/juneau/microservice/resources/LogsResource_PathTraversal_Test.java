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
package org.apache.juneau.microservice.resources;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Regression tests for path-traversal hardening in {@link LogsResource#getFile(String)}.
 *
 * <p>
 * The pre-fix implementation did
 * {@code new File(logDir.getAbsolutePath() + '/' + path)} and only verified
 * {@link File#exists()} — the same vulnerability pattern as {@link DirectoryResource}. Each test
 * below asserts the post-fix behavior: paths that escape the configured log root are rejected
 * with a 403 (Forbidden); paths that stay inside the root continue to work.
 *
 * <p>
 * The funnel for every public operation on this resource (view, parse, download, delete, info)
 * is the private {@code getFile(String)} method, so the fix is applied once and verified across
 * each operation surface here.
 *
 * <p>
 * Test wiring note: {@link LogsResource} initializes its static log-directory field from a
 * {@link Config} resolved out of the REST bean store via {@code @RestInit}. The test provides
 * an in-memory Config via {@link MockRestClient.Builder#overridingBeanStore(BeanStore)} so the
 * static state points at the per-test temp directory.
 *
 * @since 9.5.0
 */
class LogsResource_PathTraversal_Test extends TestBase {

	@TempDir
	static Path tempDir;

	static Path logRoot;
	static Path outsideSecret;
	static Path symlinkInside;
	static Path symlinkEscape;

	@BeforeAll
	static void setup() throws Exception {
		logRoot = tempDir.resolve("logs-root");
		Files.createDirectories(logRoot);
		Files.writeString(logRoot.resolve("inside.log"), "INSIDE_LOG_ROOT");

		var nestedDir = logRoot.resolve("a/b");
		Files.createDirectories(nestedDir);
		Files.writeString(nestedDir.resolve("nested.log"), "NESTED_INSIDE_LOG");

		outsideSecret = tempDir.resolve("outside-secret.log");
		Files.writeString(outsideSecret, "AUDIT_OUTSIDE_LOG_SECRET");

		try {
			symlinkInside = logRoot.resolve("link-to-inside.log");
			Files.createSymbolicLink(symlinkInside, logRoot.resolve("inside.log"));
		} catch (UnsupportedOperationException | IOException e) {
			symlinkInside = null;
		}
		try {
			symlinkEscape = logRoot.resolve("link-to-outside.log");
			Files.createSymbolicLink(symlinkEscape, outsideSecret);
		} catch (UnsupportedOperationException | IOException e) {
			symlinkEscape = null;
		}
	}

	/**
	 * Test resource subclass with a no-arg constructor so {@link MockRestClient} can instantiate
	 * it via reflection. The parent's {@code @RestInit init(Config)} hook reads the log directory
	 * out of the {@link Config} bean we register in {@link #buildClient()}, so the test fixture
	 * is wired in transparently.
	 *
	 * <p>
	 * Mounted at the root (no {@code path} attribute) so test request URIs don't need a
	 * {@code /logs/} prefix.
	 */
	@Rest(
		allowedMethodParams="*"
	)
	public static class TestLogsResource extends LogsResource {
		private static final long serialVersionUID = 1L;
	}

	private static MockRestClient buildClient() {
		var cfg = Config.create().memStore().build();
		cfg.set("Logging/logDir", logRoot.toString());
		cfg.set("Logging/allowDeletes", "true");
		var overlay = new BasicBeanStore().addBean(Config.class, cfg);
		return MockRestClient.create(TestLogsResource.class)
			.overridingBeanStore(overlay)
			.ignoreErrors()
			.noTrace()
			.build();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Baseline — non-traversing requests still work
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t01_normalAccess_view() throws Exception {
		try (var c = buildClient()) {
			c.request("VIEW", "/inside.log").run()
				.assertStatus(200)
				.assertContent().is("INSIDE_LOG_ROOT");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// CWE-22: direct ../ traversal across each operation surface (view, download, delete)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t02_directTraversal_GET_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.get("/../outside-secret.log").run().getStatusCode();
			assertEquals(403, status, "GET /../outside-secret.log must be rejected (path escapes log root)");
		}
	}

	@Test void t03_methodVIEW_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var resp = c.get("/../outside-secret.log?method=VIEW").run();
			assertEquals(403, resp.getStatusCode(), "GET /../outside-secret.log?method=VIEW must be rejected");
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_LOG_SECRET"),
				"Response body must not leak the outside-root secret");
		}
	}

	@Test void t04_methodDOWNLOAD_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var resp = c.get("/../outside-secret.log?method=DOWNLOAD").run();
			assertEquals(403, resp.getStatusCode(), "GET /../outside-secret.log?method=DOWNLOAD must be rejected");
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_LOG_SECRET"),
				"Response body must not leak the outside-root secret");
		}
	}

	@Test void t05_verbVIEW_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.request("VIEW", "/../outside-secret.log").run().getStatusCode();
			assertEquals(403, status, "VIEW /../outside-secret.log must be rejected");
		}
	}

	@Test void t06_verbDOWNLOAD_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.request("DOWNLOAD", "/../outside-secret.log").run().getStatusCode();
			assertEquals(403, status, "DOWNLOAD /../outside-secret.log must be rejected");
		}
	}

	@Test void t07_nestedTraversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.get("/a/b/../../../outside-secret.log").run().getStatusCode();
			assertEquals(403, status, "GET /a/b/../../../outside-secret.log must be rejected");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Absolute path / URL-encoded variants
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t08_absolutePathInRequest_isNotTreatedAsFilesystemAbsolute() throws Exception {
		// On Linux, "/etc/passwd" is an absolute path. Path.resolve(absolute) returns the absolute
		// path verbatim, which fails the startsWith(root) boundary check → 403. On Windows the same
		// path is interpreted as relative to root → 404 (no such file under root). Either is a
		// non-leak outcome.
		try (var c = buildClient()) {
			var status = c.get("/etc/passwd").run().getStatusCode();
			assertTrue(status == 403 || status == 404, "Status must be 403 or 404, was: " + status);
		}
	}

	@Test void t09_urlEncodedTraversal_doesNotLeak() throws Exception {
		// Servlet containers / Apache HttpClient may URL-decode the path before our handler sees
		// it; some containers reject "%2e%2e" outright. Either outcome is acceptable as long as
		// the outside-root secret is NOT returned.
		try (var c = buildClient()) {
			var resp = c.get("/%2e%2e/outside-secret.log").run();
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_LOG_SECRET"),
				"URL-encoded traversal must not leak the outside-root secret. Status was: " + resp.getStatusCode());
			assertNotEquals(200, resp.getStatusCode(),
				"URL-encoded traversal must not return 200. Status was: " + resp.getStatusCode());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Delete traversal (deletes are enabled in the test config)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t10_deleteTraversal_does_not_delete_outside_root() throws Exception {
		assertTrue(Files.exists(outsideSecret), "Pre-condition: outside secret must exist");

		try (var c = buildClient()) {
			var status = c.delete("/../outside-secret.log").run().getStatusCode();
			assertEquals(403, status, "DELETE /../outside-secret.log must be rejected with 403");
			assertTrue(Files.exists(outsideSecret),
				"Outside-root file must NOT be deleted by DELETE traversal");
			assertEquals("AUDIT_OUTSIDE_LOG_SECRET", Files.readString(outsideSecret),
				"Outside-root file content must be unchanged after DELETE traversal attempt");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Symlink handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t11_symlinkInsideRoot_is_followed() throws Exception {
		assumeTrue(symlinkInside != null, "Filesystem does not support symbolic links");

		try (var c = buildClient()) {
			c.request("VIEW", "/link-to-inside.log").run()
				.assertStatus(200)
				.assertContent().is("INSIDE_LOG_ROOT");
		}
	}

	@Test void t12_symlinkEscapesRoot_is_rejected() throws Exception {
		assumeTrue(symlinkEscape != null, "Filesystem does not support symbolic links");

		try (var c = buildClient()) {
			var resp = c.request("VIEW", "/link-to-outside.log").run();
			assertEquals(403, resp.getStatusCode(),
				"Symlink to outside-root must be rejected with 403 (post-existence boundary check)");
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_LOG_SECRET"),
				"Symlink-escape response must not leak the outside-root secret");
		}
	}
}
