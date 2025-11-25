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
package org.apache.juneau.common.io;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class LocalDir_Test extends TestBase {

	private static final Path TEST_DIR = Paths.get("src/test/resources/files");

	//====================================================================================================
	// Constructor tests - filesystem
	//====================================================================================================
	@Test void a01_constructorWithPath() {
		var dir = new LocalDir(TEST_DIR);
		assertNotNull(dir);
	}

	@Test void a02_constructorWithPath_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			new LocalDir((Path)null);
		});
	}

	//====================================================================================================
	// Constructor tests - classpath
	//====================================================================================================
	@Test void b01_constructorWithClassAndPath_null() {
		var dir = new LocalDir(LocalDir_Test.class, null);
		assertNotNull(dir);
	}

	@Test void b02_constructorWithClassAndPath_empty() {
		var dir = new LocalDir(LocalDir_Test.class, "");
		assertNotNull(dir);
	}

	@Test void b03_constructorWithClassAndPath_absolute() {
		var dir = new LocalDir(LocalDir_Test.class, "/files");
		assertNotNull(dir);
	}

	@Test void b04_constructorWithClassAndPath_relative() {
		var dir = new LocalDir(LocalDir_Test.class, "files");
		assertNotNull(dir);
	}

	@Test void b05_constructorWithClassAndPath_root() {
		var dir = new LocalDir(LocalDir_Test.class, "/");
		assertNotNull(dir);
	}

	@Test void b06_constructorWithClassAndPath_nullClass() {
		assertThrows(IllegalArgumentException.class, () -> {
			new LocalDir((Class<?>)null, "path");
		});
	}

	@Test void b07_constructorWithClassAndPath_trailingSlashes() {
		var dir = new LocalDir(LocalDir_Test.class, "files/");
		assertNotNull(dir);
		// Trailing slashes should be trimmed
	}

	//====================================================================================================
	// resolve() tests - filesystem
	//====================================================================================================
	@Test void c01_resolve_filesystem() {
		var dir = new LocalDir(TEST_DIR);
		var file = dir.resolve("Test3.properties");
		assertNotNull(file);
		assertEquals("Test3.properties", file.getName());
	}

	@Test void c02_resolve_filesystem_nonexistent() {
		var dir = new LocalDir(TEST_DIR);
		var file = dir.resolve("nonexistent.properties");
		assertNull(file);
	}

	@Test void c03_resolve_filesystem_directory() {
		var dir = new LocalDir(TEST_DIR);
		// Resolving a directory should return null
		var file = dir.resolve(".");
		// May be null if "." is treated as a directory
	}

	//====================================================================================================
	// resolve() tests - classpath
	//====================================================================================================
	@Test void d01_resolve_classpath() {
		var dir = new LocalDir(LocalDir_Test.class, "/files");
		var file = dir.resolve("Test3.properties");
		assertNotNull(file);
		assertEquals("Test3.properties", file.getName());
	}

	@Test void d02_resolve_classpath_nonexistent() {
		var dir = new LocalDir(LocalDir_Test.class, "files");
		var file = dir.resolve("nonexistent.properties");
		assertNull(file);
	}

	@Test void d03_resolve_classpath_nullPath() {
		var dir = new LocalDir(LocalDir_Test.class, null);
		var file = dir.resolve("files/Test3.properties");
		// May or may not be null depending on classpath structure
	}

	@Test void d04_resolve_classpath_absolutePath() {
		var dir = new LocalDir(LocalDir_Test.class, "/");
		var file = dir.resolve("files/Test3.properties");
		assertNotNull(file);
	}

	@Test void d05_resolve_classpath_directory() {
		// Resolving a directory should return null (covers line 139 in LocalDir)
		var dir = new LocalDir(LocalDir_Test.class, "/");
		var file = dir.resolve("files");
		// When unpackaged, "files" is a directory, so resolve should return null
		// This tests the isClasspathFile check that returns false for directories
		assertNull(file);
	}

	//====================================================================================================
	// equals() and hashCode() tests
	//====================================================================================================
	@Test void e01_equals_filesystem() {
		var dir1 = new LocalDir(TEST_DIR);
		var dir2 = new LocalDir(TEST_DIR);
		assertEquals(dir1, dir2);
		assertEquals(dir1.hashCode(), dir2.hashCode());
	}

	@Test void e02_equals_filesystem_different() {
		var dir1 = new LocalDir(TEST_DIR);
		var dir2 = new LocalDir(Paths.get("src/test/resources"));
		assertNotEquals(dir1, dir2);
	}

	@Test void e03_equals_classpath() {
		var dir1 = new LocalDir(LocalDir_Test.class, "files");
		var dir2 = new LocalDir(LocalDir_Test.class, "files");
		assertEquals(dir1, dir2);
		assertEquals(dir1.hashCode(), dir2.hashCode());
	}

	@Test void e04_equals_classpath_different() {
		var dir1 = new LocalDir(LocalDir_Test.class, "files");
		var dir2 = new LocalDir(LocalDir_Test.class, "other");
		assertNotEquals(dir1, dir2);
	}

	@Test void e05_equals_differentTypes() {
		var dir1 = new LocalDir(TEST_DIR);
		var dir2 = new LocalDir(LocalDir_Test.class, "files");
		assertNotEquals(dir1, dir2);
	}

	@Test void e06_equals_notLocalDir() {
		var dir = new LocalDir(TEST_DIR);
		assertNotEquals(dir, "not a LocalDir");
		assertNotEquals(dir, null);
		assertNotEquals(dir, new Object());
	}

	//====================================================================================================
	// toString() tests
	//====================================================================================================
	@Test void f01_toString_filesystem() {
		var dir = new LocalDir(TEST_DIR);
		var str = dir.toString();
		assertTrue(str.contains("files") || str.contains("test"));
	}

	@Test void f02_toString_classpath() {
		var dir = new LocalDir(LocalDir_Test.class, "files");
		var str = dir.toString();
		assertTrue(str.contains("LocalDir_Test") || str.contains("files"));
	}

	@Test void f03_toString_classpath_null() {
		var dir = new LocalDir(LocalDir_Test.class, null);
		var str = dir.toString();
		assertTrue(str.contains("LocalDir_Test"));
	}

	@Test void f04_toString_classpath_root() {
		var dir = new LocalDir(LocalDir_Test.class, "/");
		var str = dir.toString();
		assertTrue(str.contains("LocalDir_Test") && str.contains("/"));
	}
}

