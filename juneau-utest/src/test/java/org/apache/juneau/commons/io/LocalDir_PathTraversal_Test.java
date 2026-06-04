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
package org.apache.juneau.commons.io;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Path-traversal (CWE-22) boundary tests for {@link LocalDir#resolve(String)}.
 *
 * <p>
 * Covers both the filesystem-root branch (delegates to {@code FileUtils.resolveSafely}) and
 * the classpath-resource branch ({@code ..} segment rejection).
 */
@SuppressWarnings({
	"java:S5976" // Explicit per-case tests preferred for readability/diagnostics over a single parameterized test.
})
class LocalDir_PathTraversal_Test extends TestBase {

	private static final Path TEST_DIR = Paths.get("src/test/resources/files");

	//====================================================================================================
	// Filesystem branch — happy-path resolution
	//====================================================================================================

	@Test void a01_filesystem_happyPath() {
		var dir = new LocalDir(TEST_DIR);
		var file = dir.resolve("Test3.properties");
		assertNotNull(file);
		assertEquals("Test3.properties", file.getName());
	}

	@Test void a02_filesystem_happyPath_subdirectory() {
		var dir = new LocalDir(TEST_DIR);
		var file = dir.resolve("test1");
		// "test1" is a subdirectory, not a file; resolve should return null for directories
		assertNull(file);
	}

	@Test void a03_filesystem_nonexistent_returnsNull() {
		var dir = new LocalDir(TEST_DIR);
		var file = dir.resolve("nonexistent.txt");
		assertNull(file);
	}

	//====================================================================================================
	// Filesystem branch — path-traversal rejection
	//====================================================================================================

	@Test void b01_filesystem_singleDotDot_throws() {
		var dir = new LocalDir(TEST_DIR);
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("../pom.xml"));
	}

	@Test void b02_filesystem_multiSegmentDotDot_throws() {
		var dir = new LocalDir(TEST_DIR);
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("../../pom.xml"));
	}

	@Test void b03_filesystem_deepEscape_throws() {
		var dir = new LocalDir(TEST_DIR);
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("../../../etc/passwd"));
	}

	@Test void b04_filesystem_absolutePathInjection_throws() {
		var dir = new LocalDir(TEST_DIR);
		// An absolute path bypasses root-relative resolution (Path.resolve replaces the base entirely on Unix).
		// FileUtils.resolveSafely rejects it via the startsWith(root) boundary check.
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("/etc/passwd"));
	}

	@Test void b05_filesystem_dotDotWithSandwich_throws() {
		// "subdir/../../../etc/passwd" — traversal buried inside an otherwise-plausible path
		var dir = new LocalDir(TEST_DIR);
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("test1/../../../etc/passwd"));
	}

	@Test void b06_filesystem_encodedTraversal_notDecoded() {
		// "%2e%2e" is NOT decoded by LocalDir (no URL-decode step in the call chain).
		// It is therefore treated as a literal path segment, not as "..".
		// This test documents that contract: the path simply won't match any real file.
		var dir = new LocalDir(TEST_DIR);
		var result = dir.resolve("%2e%2e/etc/passwd");
		// The literal segment "%2e%2e" doesn't exist as a subdirectory → null, no throw
		assertNull(result);
	}

	//====================================================================================================
	// Filesystem branch — symlink out-of-root rejection
	//====================================================================================================

	@Test void c01_filesystem_symlinkOutOfRoot_throws() throws IOException {
		assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"),
			"Symlink test skipped on Windows");

		// Create a temporary root directory with a symlink pointing outside it
		var tmpRoot = Files.createTempDirectory("juneau-localdir-test-");
		var outsideTarget = Files.createTempFile("juneau-outside-", ".txt");
		outsideTarget.toFile().deleteOnExit();
		var symlink = tmpRoot.resolve("escape.txt");
		try {
			Files.createSymbolicLink(symlink, outsideTarget);
			var dir = new LocalDir(tmpRoot);
			// Resolving through the symlink should be rejected because it escapes the root
			assertThrows(IllegalArgumentException.class, () -> dir.resolve("escape.txt"));
		} finally {
			Files.deleteIfExists(symlink);
			Files.deleteIfExists(tmpRoot);
		}
	}

	@Test void c02_filesystem_symlinkInsideRoot_allowed() throws IOException {
		assumeFalse(System.getProperty("os.name", "").toLowerCase().contains("win"),
			"Symlink test skipped on Windows");

		// A symlink that resolves INSIDE the root is allowed
		var tmpRoot = Files.createTempDirectory("juneau-localdir-test-");
		var realFile = Files.createTempFile(tmpRoot, "real-", ".txt");
		var symlink = tmpRoot.resolve("link.txt");
		try {
			Files.createSymbolicLink(symlink, realFile.getFileName());
			var dir = new LocalDir(tmpRoot);
			// Symlink resolves inside root → allowed; file is readable
			assertDoesNotThrow(() -> dir.resolve("link.txt"));
		} finally {
			Files.deleteIfExists(symlink);
			Files.deleteIfExists(realFile);
			Files.deleteIfExists(tmpRoot);
		}
	}

	//====================================================================================================
	// Classpath branch — happy-path resolution
	//====================================================================================================

	@Test void d01_classpath_happyPath() {
		var dir = new LocalDir(LocalDir_PathTraversal_Test.class, "/files");
		var file = dir.resolve("Test3.properties");
		assertNotNull(file);
		assertEquals("Test3.properties", file.getName());
	}

	@Test void d02_classpath_nonexistent_returnsNull() {
		var dir = new LocalDir(LocalDir_PathTraversal_Test.class, "/files");
		var file = dir.resolve("nonexistent.properties");
		assertNull(file);
	}

	//====================================================================================================
	// Classpath branch — path-traversal rejection
	//====================================================================================================

	@Test void e01_classpath_singleDotDot_throws() {
		var dir = new LocalDir(LocalDir_PathTraversal_Test.class, "/files");
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("../something"));
	}

	@Test void e02_classpath_multiSegmentDotDot_throws() {
		var dir = new LocalDir(LocalDir_PathTraversal_Test.class, "/files");
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("../../something"));
	}

	@Test void e03_classpath_dotDotInMiddle_throws() {
		var dir = new LocalDir(LocalDir_PathTraversal_Test.class, "/files");
		assertThrows(IllegalArgumentException.class, () -> dir.resolve("subdir/../../../passwd"));
	}

	@Test void e04_classpath_encodedTraversal_notDecoded() {
		// "%2e%2e" is NOT decoded; treated as a literal segment, not as ".."
		// This documents the contract: no URL-decode occurs in LocalDir.
		var dir = new LocalDir(LocalDir_PathTraversal_Test.class, "/files");
		var result = dir.resolve("%2e%2e/something");
		// Literal segment won't match a classpath resource → null, no throw
		assertNull(result);
	}
}
