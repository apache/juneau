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

import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class LocalFile_Test extends TestBase {

	private static final Path TEST_FILE = Paths.get("src/test/resources/files/Test3.properties");

	//====================================================================================================
	// Constructor tests - filesystem
	//====================================================================================================
	@Test void a01_constructorWithPath() {
		var file = new LocalFile(TEST_FILE);
		assertEquals("Test3.properties", file.getName());
	}

	@Test void a02_constructorWithPath_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			new LocalFile((Path)null);
		});
	}

	@Test void a03_constructorWithPath_rootPath() {
		var path = Paths.get("/");
		assertThrows(IllegalArgumentException.class, () -> {
			new LocalFile(path);
		});
	}

	//====================================================================================================
	// Constructor tests - classpath
	//====================================================================================================
	@Test void b01_constructorWithClassAndPath() {
		var file = new LocalFile(LocalFile_Test.class, "files/Test3.properties");
		assertEquals("Test3.properties", file.getName());
	}

	@Test void b02_constructorWithClassAndPath_noSlash() {
		var file = new LocalFile(LocalFile_Test.class, "Test3.properties");
		assertEquals("Test3.properties", file.getName());
	}

	@Test void b03_constructorWithClassAndPath_nestedPath() {
		var file = new LocalFile(LocalFile_Test.class, "files/subdir/Test3.properties");
		assertEquals("Test3.properties", file.getName());
	}

	@Test void b04_constructorWithClassAndPath_nullClass() {
		assertThrows(IllegalArgumentException.class, () -> {
			new LocalFile((Class<?>)null, "path");
		});
	}

	@Test void b05_constructorWithClassAndPath_nullPath() {
		assertThrows(IllegalArgumentException.class, () -> {
			new LocalFile(LocalFile_Test.class, null);
		});
	}

	//====================================================================================================
	// getName() tests
	//====================================================================================================
	@Test void c01_getName_filesystem() {
		var file = new LocalFile(TEST_FILE);
		assertEquals("Test3.properties", file.getName());
	}

	@Test void c02_getName_classpath() {
		var file = new LocalFile(LocalFile_Test.class, "files/Test3.properties");
		assertEquals("Test3.properties", file.getName());
	}

	@Test void c03_getName_classpath_noSlash() {
		var file = new LocalFile(LocalFile_Test.class, "Test3.properties");
		assertEquals("Test3.properties", file.getName());
	}

	//====================================================================================================
	// read() tests - filesystem
	//====================================================================================================
	@Test void d01_read_filesystem() throws IOException {
		var file = new LocalFile(TEST_FILE);
		try (var is = file.read()) {
			var p = new Properties();
			p.load(new StringReader(read(is, Files.size(TEST_FILE))));
			assertEquals("files/Test3.properties", p.get("file"));
		}
	}

	@Test void d02_read_filesystem_multipleTimes() throws IOException {
		var file = new LocalFile(TEST_FILE);
		String content1;
		try (var is1 = file.read()) {
			content1 = read(is1, Files.size(TEST_FILE));
			assertNotNull(content1);
		}
		try (var is2 = file.read()) {
			var content2 = read(is2, Files.size(TEST_FILE));
			assertNotNull(content2);
			assertEquals(content1, content2);
		}
	}

	//====================================================================================================
	// read() tests - classpath
	//====================================================================================================
	@Test void d03_read_classpath() throws IOException {
		var file = new LocalFile(LocalFile_Test.class, "/files/Test3.properties");
		try (var is = file.read()) {
			assertNotNull(is);
			var p = new Properties();
			p.load(new StringReader(read(is)));
			assertEquals("files/Test3.properties", p.get("file"));
		}
	}

	@Test void d04_read_classpath_nonexistent() {
		var file = new LocalFile(LocalFile_Test.class, "/nonexistent.properties");
		assertThrows(IOException.class, () -> {
			file.read();
		});
	}

	//====================================================================================================
	// cache() tests
	//====================================================================================================
	@Test void e01_cache_filesystem() throws IOException {
		var file = new LocalFile(TEST_FILE);
		var cached = file.cache();
		assertSame(file, cached);
		// After caching, read should return cached content
		String content1;
		try (var is1 = file.read()) {
			content1 = read(is1, Files.size(TEST_FILE));
		}
		try (var is2 = file.read()) {
			var content2 = read(is2, Files.size(TEST_FILE));
			assertEquals(content1, content2);
		}
	}

	@Test void e02_cache_classpath() throws IOException {
		var file = new LocalFile(LocalFile_Test.class, "/files/Test3.properties");
		var cached = file.cache();
		assertSame(file, cached);
		// After caching, read should return cached content
		String content1;
		try (var is1 = file.read()) {
			content1 = read(is1, 1000);
		}
		try (var is2 = file.read()) {
			var content2 = read(is2, 1000);
			assertEquals(content1, content2);
		}
	}

	@Test void e03_cache_multipleCalls() throws IOException {
		var file = new LocalFile(TEST_FILE);
		file.cache();
		file.cache(); // Should be safe to call multiple times
		file.cache();
		try (var is = file.read()) {
			assertNotNull(is);
		}
	}

	//====================================================================================================
	// size() tests
	//====================================================================================================
	@Test void f01_size_filesystem() throws IOException {
		var file = new LocalFile(TEST_FILE);
		var size = file.size();
		assertTrue(size > 0);
		assertEquals(Files.size(TEST_FILE), size);
	}

	@Test void f02_size_classpath() throws IOException {
		var file = new LocalFile(LocalFile_Test.class, "files/Test3.properties");
		var size = file.size();
		assertEquals(-1, size); // Classpath files return -1
	}
}

