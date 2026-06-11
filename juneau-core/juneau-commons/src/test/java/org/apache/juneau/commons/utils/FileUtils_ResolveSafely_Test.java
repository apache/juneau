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
package org.apache.juneau.commons.utils;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;
import org.apache.juneau.commons.TestBase;

/**
 * Tests for {@link FileUtils#resolveSafely(File, String)} and
 * {@link FileUtils#resolveVirtualPathSafely(String, String)}.
 *
 * <p>
 * The two helpers are the shared canonical implementations behind the path-traversal hardening
 * in {@code DirectoryResource}, {@code LogsResource}, and {@code JspMixin}; the tests
 * here exercise the shared semantic invariants directly so the per-resource regression suites
 * can focus on caller-layer behavior (HTTP status codes, response shape).
 */
@SuppressWarnings({
	"unused" // Unused parameters/variables kept for consistent method signatures across test utilities.
})
class FileUtils_ResolveSafely_Test extends TestBase {

	@TempDir
	Path tempDir;

	//-----------------------------------------------------------------------------------------------------------------
	// resolveSafely(File, String)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void t01_nullUserPath_returnsRoot() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var r = FileUtils.resolveSafely(root, null);
		assertTrue(r.isPresent());
		assertEquals(root.toPath().toRealPath().toFile(), r.get());
	}

	@Test void t02_emptyUserPath_returnsRoot() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var r = FileUtils.resolveSafely(root, "");
		assertTrue(r.isPresent());
		assertEquals(root.toPath().toRealPath().toFile(), r.get());
	}

	@Test void t03_validRelativePath_returnsFile() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var inside = new File(root, "inside.txt");
		Files.writeString(inside.toPath(), "INSIDE");
		var r = FileUtils.resolveSafely(root, "inside.txt");
		assertTrue(r.isPresent());
		assertEquals(inside.getCanonicalFile(), r.get().getCanonicalFile());
	}

	@Test void t04_nonExistentRelativePath_returnsEmpty() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var r = FileUtils.resolveSafely(root, "does-not-exist.txt");
		assertTrue(r.isEmpty());
	}

	@Test void t05_directTraversal_throwsIAE() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		// Even if outside file exists, traversal must be rejected
		Files.writeString(tempDir.resolve("outside.txt"), "OUTSIDE");
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveSafely(root, "../outside.txt"));
	}

	@Test void t06_nestedTraversal_throwsIAE() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		Files.createDirectories(root.toPath().resolve("a/b"));
		Files.writeString(tempDir.resolve("outside.txt"), "OUTSIDE");
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveSafely(root, "a/b/../../../outside.txt"));
	}

	@Test void t07_absolutePath_throwsIAE() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		// Path.resolve treats an absolute arg as the new root, so an absolute path
		// outside the configured root fails the startsWith() boundary check on Linux.
		// On Windows the same path is interpreted relative-to-current-drive-root, which
		// also fails startsWith().
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveSafely(root, "/etc/passwd"));
	}

	@Test void t08_symlinkInsideRoot_resolves() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var inside = root.toPath().resolve("inside.txt");
		Files.writeString(inside, "INSIDE");
		Path link;
		try {
			link = root.toPath().resolve("link.txt");
			Files.createSymbolicLink(link, inside);
		} catch (UnsupportedOperationException | IOException e) {
			assumeTrue(false, "Filesystem does not support symbolic links");
			return;
		}
		var r = FileUtils.resolveSafely(root, "link.txt");
		assertTrue(r.isPresent());
		// Real path of the symlink targets the inside file.
		assertEquals(inside.toRealPath().toFile(), r.get().toPath().toRealPath().toFile());
	}

	@Test void t09_symlinkEscapesRoot_throwsIAE() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var outside = tempDir.resolve("outside.txt");
		Files.writeString(outside, "OUTSIDE");
		try {
			Files.createSymbolicLink(root.toPath().resolve("escape"), outside);
		} catch (UnsupportedOperationException | IOException e) {
			assumeTrue(false, "Filesystem does not support symbolic links");
			return;
		}
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveSafely(root, "escape"));
	}

	@Test void t10_urlEncodedTraversal_throwsIAE_orReturnsEmpty() throws Exception {
		// The helper does not URL-decode — the caller is expected to URL-decode first.
		// "%2e%2e" is treated as a literal segment, so it does NOT trip the boundary
		// check (it's not "..") — but the file with that literal name does not exist,
		// so the result is Optional.empty rather than a leak.
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var r = FileUtils.resolveSafely(root, "%2e%2e/outside.txt");
		// On platforms that reject "%2e%2e" as an invalid path char this throws IAE; // NOSONAR
		// elsewhere it resolves to a non-existent file → Optional.empty. Either way
		// it does not leak.
		if (r.isPresent())
			fail("URL-encoded traversal must not resolve to an existing file");
	}

	@Test void t11_nullRootDir_throwsIAE() {
		assertThrows(IllegalArgumentException.class, () -> FileUtils.resolveSafely(null, "foo.txt"));
	}

	@Test void t12_nestedFileResolves() throws Exception {
		var root = Files.createDirectories(tempDir.resolve("r")).toFile();
		var nested = root.toPath().resolve("a/b/c.txt");
		Files.createDirectories(nested.getParent());
		Files.writeString(nested, "NESTED");
		var r = FileUtils.resolveSafely(root, "a/b/c.txt");
		assertTrue(r.isPresent());
		assertEquals(nested.toRealPath().toFile(), r.get().toPath().toRealPath().toFile());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// resolveVirtualPathSafely(String, String)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void v01_nullUserPath_returnsBasePath() {
		assertEquals("/WEB-INF/views/",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", null));
	}

	@Test void v02_emptyUserPath_returnsBasePath() {
		assertEquals("/WEB-INF/views/",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", ""));
	}

	@Test void v03_basePathWithoutTrailingSlash_normalizesWithTrailingSlash() {
		assertEquals("/WEB-INF/views/",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views", null));
	}

	@Test void v04_validRelativePath_resolves() {
		assertEquals("/WEB-INF/views/hello.jsp",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "hello.jsp"));
	}

	@Test void v05_validLeadingSlashUserPath_resolves() {
		assertEquals("/WEB-INF/views/hello.jsp",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "/hello.jsp"));
	}

	@Test void v06_nestedRelativePath_resolves() {
		assertEquals("/WEB-INF/views/admin/dashboard.jsp",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "admin/dashboard.jsp"));
	}

	@Test void v07_directTraversal_throwsIAE() {
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "../web.xml"));
	}

	@Test void v08_nestedTraversal_throwsIAE() {
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "a/b/../../../web.xml"));
	}

	@Test void v09_absoluteUserPath_treatsAsRelativeAfterStripLeadingSlash() {
		// "/admin/dashboard.jsp" is appended to basePath after stripping the leading slash,
		// so it resolves to /WEB-INF/views/admin/dashboard.jsp — NOT to /admin/dashboard.jsp.
		assertEquals("/WEB-INF/views/admin/dashboard.jsp",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "/admin/dashboard.jsp"));
	}

	@Test void v10_traversalToSiblingOfBase_throwsIAE() {
		// "/WEB-INF/views/../config.xml" normalizes to "/WEB-INF/config.xml", which doesn't
		// start with "/WEB-INF/views/" — rejected.
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "../config.xml"));
	}

	@Test void v11_traversalRetainingPrefix_throwsIAE() {
		// "/WEB-INF/views/foo/../../views2/hack.jsp" — the prefix matches a different dir
		// ("views2", not "views"), so it must be rejected even though it shares the same parent.
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "foo/../../views2/hack.jsp"));
	}

	@Test void v12_urlEncodedTraversal_doesNotEscape() {
		// "%2e%2e" is treated as a literal segment, not as ".." — so it stays inside basePath.
		assertEquals("/WEB-INF/views/%2e%2e/web.xml",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "%2e%2e/web.xml"));
	}

	@Test void v13_nullBasePath_throwsIAE() {
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveVirtualPathSafely(null, "hello.jsp"));
	}

	@Test void v14_blankBasePath_throwsIAE() {
		assertThrows(IllegalArgumentException.class,
			() -> FileUtils.resolveVirtualPathSafely("   ", "hello.jsp"));
	}

	@Test void v15_dotSegmentsCollapse() {
		assertEquals("/WEB-INF/views/hello.jsp",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/views/", "./hello.jsp"));
	}

	@Test void v16_basePathWithDotsNormalizes() {
		// basePath itself contains "./" — should normalize before prefix matching.
		assertEquals("/WEB-INF/views/hello.jsp",
			FileUtils.resolveVirtualPathSafely("/WEB-INF/./views/", "hello.jsp"));
	}
}
