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
package org.apache.juneau.config.store;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Regression tests for path-traversal hardening in {@link FileStore}.
 */
class ConfigFileStore_PathTraversal_Test extends TestBase {

	@TempDir
	Path tempDir;

	private FileStore store() throws Exception {
		var d = tempDir.resolve("store");
		Files.createDirectories(d);
		Files.writeString(tempDir.resolve("secret.cfg"), "SECRET");
		return FileStore.create().directory(d.toFile()).build();
	}

	@Test void a01_legitNested() throws Exception {
		var fs = store();
		assertNull(fs.write("sub/X.cfg", null, "foo"));
		assertTrue(fs.exists("sub/X.cfg"));
		assertEquals("foo", fs.read("sub/X.cfg"));
		assertEquals("foo", fs.read("sub/X"));
	}

	@Test void a02_relativeTraversal() throws Exception {
		var fs = store();
		assertFalse(fs.exists("../secret.cfg"));
		assertFalse(fs.exists("../secret"));
		assertThrows(IllegalArgumentException.class, () -> fs.read("../secret.cfg"));
		assertThrows(IllegalArgumentException.class, () -> fs.read("../secret"));
		assertThrows(IllegalArgumentException.class, () -> fs.write("../evil.cfg", null, "x"));
		assertFalse(Files.exists(tempDir.resolve("evil.cfg")));
		assertEquals("SECRET", Files.readString(tempDir.resolve("secret.cfg")));
	}

	@Test void a03_absolutePath_stillSupported() throws Exception {
		// Absolute names are used by @Rest(config="/abs/path.cfg"); intentionally not restricted.
		var fs = store();
		var abs = tempDir.resolve("secret.cfg").toString();
		assertTrue(fs.exists(abs));
		assertEquals("SECRET", fs.read(abs));
	}

	@Test void a04_symlinkEscape() throws Exception {
		var fs = store();
		try {
			Files.createSymbolicLink(tempDir.resolve("store").resolve("link.cfg"), tempDir.resolve("secret.cfg"));
		} catch (@SuppressWarnings("unused") UnsupportedOperationException | IOException e) {
			Assumptions.abort("Symlinks not supported.");
		}
		assertFalse(fs.exists("link.cfg"));
		assertThrows(IllegalArgumentException.class, () -> fs.read("link.cfg"));
	}
}
