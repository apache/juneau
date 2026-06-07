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
import org.apache.juneau.config.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Regression tests for path-traversal hardening in {@link DirectoryResource#getFile(String)}.
 *
 * <p>
 * The pre-fix implementation did
 * {@code new File(rootDir.getAbsolutePath() + '/' + path)} and only verified
 * {@link File#exists()} — so {@code ..} segments, absolute paths, and symlinks pointing outside
 * the configured root could all read files the JVM had access to. Each test below asserts the
 * post-fix behavior: paths that escape the configured root are rejected with a 403 (Forbidden);
 * paths that stay inside the root continue to work.
 *
 * <p>
 * The funnel for every public operation on this resource (view, download, delete, upload, info)
 * is the private {@code getFile(String)} method, so the fix is applied once and verified across
 * each operation surface here.
 *
 * @since 10.0.0
 */
class DirectoryResource_PathTraversal_Test extends TestBase {

	@TempDir
	static Path tempDir;

	static Path rootDir;
	static Path outsideSecret;
	static Path symlinkInside;
	static Path symlinkEscape;

	@BeforeAll
	static void setup() throws Exception {
		rootDir = tempDir.resolve("dir-root");
		Files.createDirectories(rootDir);
		Files.writeString(rootDir.resolve("inside.txt"), "INSIDE_ROOT");

		var nestedDir = rootDir.resolve("a/b");
		Files.createDirectories(nestedDir);
		Files.writeString(nestedDir.resolve("nested.txt"), "NESTED_INSIDE");

		// File OUTSIDE the configured root — what the path-traversal attack tries to read.
		outsideSecret = tempDir.resolve("outside-secret.txt");
		Files.writeString(outsideSecret, "AUDIT_OUTSIDE_SECRET");

		// File OUTSIDE the configured root that the upload-traversal attack tries to overwrite/create.
		Files.writeString(tempDir.resolve("outside-upload-target.txt"), "ORIGINAL_OUTSIDE");

		// Best-effort symlinks. Skip with assumption-failure on platforms / filesystems that don't
		// support them (e.g. some Windows configurations) so the rest of the matrix still runs.
		try {
			symlinkInside = rootDir.resolve("link-to-inside.txt");
			Files.createSymbolicLink(symlinkInside, rootDir.resolve("inside.txt"));
		} catch (UnsupportedOperationException | IOException e) {
			symlinkInside = null;
		}
		try {
			symlinkEscape = rootDir.resolve("link-to-outside");
			Files.createSymbolicLink(symlinkEscape, outsideSecret);
		} catch (UnsupportedOperationException | IOException e) {
			symlinkEscape = null;
		}
	}

	/**
	 * Test resource subclass with a no-arg constructor so {@link MockRestClient} can instantiate
	 * it via reflection. Hardcodes a Config that points at the test's {@link #rootDir} and enables
	 * every operation so the boundary check is exercised on each surface (view, download, delete,
	 * upload, info).
	 */
	@Rest(
		allowedMethodParams="*"
	)
	public static class TestDirResource extends DirectoryResource {
		private static final long serialVersionUID = 1L;

		public TestDirResource() {
			super(buildConfig());
		}

		private static Config buildConfig() {
			var cfg = Config.create().memStore().build();
			cfg.set(DIRECTORY_RESOURCE_rootDir, rootDir.toString());
			cfg.set(DIRECTORY_RESOURCE_allowViews, "true");
			cfg.set(DIRECTORY_RESOURCE_allowUploads, "true");
			cfg.set(DIRECTORY_RESOURCE_allowDeletes, "true");
			return cfg;
		}
	}

	private static MockRestClient buildClient() {
		return MockRestClient.buildLax(TestDirResource.class);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Baseline — non-traversing requests still work
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t01_normalAccess_view() throws Exception {
		try (var c = buildClient()) {
			c.request("VIEW", "/inside.txt").run()
				.assertStatus(200)
				.assertContent().is("INSIDE_ROOT");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// CWE-22: direct ../ traversal across each operation surface (view, download, delete)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t02_directTraversal_GET_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.get("/../outside-secret.txt").run().getStatusCode();
			assertEquals(403, status, "GET /../outside-secret.txt must be rejected (path escapes root)");
		}
	}

	@Test void t03_methodVIEW_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var resp = c.get("/../outside-secret.txt?method=VIEW").run();
			assertEquals(403, resp.getStatusCode(), "GET /../outside-secret.txt?method=VIEW must be rejected");
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_SECRET"),
				"Response body must not leak the outside-root secret");
		}
	}

	@Test void t04_methodDOWNLOAD_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var resp = c.get("/../outside-secret.txt?method=DOWNLOAD").run();
			assertEquals(403, resp.getStatusCode(), "GET /../outside-secret.txt?method=DOWNLOAD must be rejected");
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_SECRET"),
				"Response body must not leak the outside-root secret");
		}
	}

	@Test void t05_verbVIEW_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.request("VIEW", "/../outside-secret.txt").run().getStatusCode();
			assertEquals(403, status, "VIEW /../outside-secret.txt must be rejected");
		}
	}

	@Test void t06_verbDOWNLOAD_traversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.request("DOWNLOAD", "/../outside-secret.txt").run().getStatusCode();
			assertEquals(403, status, "DOWNLOAD /../outside-secret.txt must be rejected");
		}
	}

	@Test void t07_nestedTraversal_returns403() throws Exception {
		try (var c = buildClient()) {
			var status = c.get("/a/b/../../../outside-secret.txt").run().getStatusCode();
			assertEquals(403, status, "GET /a/b/../../../outside-secret.txt must be rejected");
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
			var resp = c.get("/%2e%2e/outside-secret.txt").run();
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_SECRET"),
				"URL-encoded traversal must not leak the outside-root secret. Status was: " + resp.getStatusCode());
			assertNotEquals(200, resp.getStatusCode(),
				"URL-encoded traversal must not return 200. Status was: " + resp.getStatusCode());
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Upload / delete traversal (uploads + deletes are enabled in TestDirResource)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t10_uploadTraversal_does_not_create_outside_root() throws Exception {
		// PUT /../outside-uploaded.txt — getFile(...) is called BEFORE the upload, so the
		// boundary check fires regardless of whether the target file exists. Pre-fix code would
		// have returned NotFound (since the target didn't exist) at first, but NotFound is the
		// pre-existing legacy behavior even for inside-root paths that don't exist. The point of
		// this test is the negative assertion: no file is created outside the root.
		var outsideUpload = tempDir.resolve("outside-uploaded-by-test.txt");
		assertFalse(Files.exists(outsideUpload), "Pre-condition: upload target must not exist");

		try (var c = buildClient()) {
			var status = c.put("/../outside-uploaded-by-test.txt", "ATTACK_PAYLOAD").run().getStatusCode();
			// Pre-fix: would have been 404 (legacy "file must exist before PUT" behavior). Post-fix:
			// the boundary check fires first and returns 403. Either way, the outside file must NOT
			// be created.
			assertTrue(status == 403 || status == 404,
				"PUT to outside-root path must be rejected with 403/404, was: " + status);
			assertFalse(Files.exists(outsideUpload),
				"Outside-root file must NOT be created by PUT traversal");
		}
	}

	@Test void t11_deleteTraversal_does_not_delete_outside_root() throws Exception {
		// DELETE /../outside-secret.txt — the secret file must remain present after the attack.
		assertTrue(Files.exists(outsideSecret), "Pre-condition: outside secret must exist");

		try (var c = buildClient()) {
			var status = c.delete("/../outside-secret.txt").run().getStatusCode();
			assertEquals(403, status, "DELETE /../outside-secret.txt must be rejected with 403");
			assertTrue(Files.exists(outsideSecret),
				"Outside-root file must NOT be deleted by DELETE traversal");
			assertEquals("AUDIT_OUTSIDE_SECRET", Files.readString(outsideSecret),
				"Outside-root file content must be unchanged after DELETE traversal attempt");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Symlink handling
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t12_symlinkInsideRoot_is_followed() throws Exception {
		assumeTrue(symlinkInside != null, "Filesystem does not support symbolic links");

		try (var c = buildClient()) {
			c.request("VIEW", "/link-to-inside.txt").run()
				.assertStatus(200)
				.assertContent().is("INSIDE_ROOT");
		}
	}

	@Test void t13_symlinkEscapesRoot_is_rejected() throws Exception {
		assumeTrue(symlinkEscape != null, "Filesystem does not support symbolic links");

		try (var c = buildClient()) {
			var resp = c.request("VIEW", "/link-to-outside").run();
			assertEquals(403, resp.getStatusCode(),
				"Symlink to outside-root must be rejected with 403 (post-existence boundary check)");
			assertFalse(resp.getContent().asString().contains("AUDIT_OUTSIDE_SECRET"),
				"Symlink-escape response must not leak the outside-root secret");
		}
	}
}
