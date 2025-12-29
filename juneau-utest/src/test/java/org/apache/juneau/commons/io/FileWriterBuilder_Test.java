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

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FileWriterBuilder_Test extends TestBase {

	private static final Path TEST_FILE = Paths.get("target/test-output/FileWriterBuilder_Test.txt");

	@BeforeEach
	void setUp() throws IOException {
		Files.createDirectories(TEST_FILE.getParent());
		if (Files.exists(TEST_FILE)) {
			Files.delete(TEST_FILE);
		}
	}

	@AfterEach
	void tearDown() throws IOException {
		if (Files.exists(TEST_FILE)) {
			Files.delete(TEST_FILE);
		}
	}

	//====================================================================================================
	// create() tests
	//====================================================================================================
	@Test void a01_create() {
		var builder = FileWriterBuilder.create();
		assertNotNull(builder);
	}

	@Test void a02_createWithFile() {
		var file = TEST_FILE.toFile();
		var builder = FileWriterBuilder.create(file);
		assertNotNull(builder);
	}

	@Test void a03_createWithPath() {
		var builder = FileWriterBuilder.create(TEST_FILE.toString());
		assertNotNull(builder);
	}

	//====================================================================================================
	// build() tests
	//====================================================================================================
	@Test void b01_build() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file).build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("test", content);
	}

	@Test void b02_build_noFile() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileWriterBuilder.create().build();
		});
	}

	//====================================================================================================
	// append() tests
	//====================================================================================================
	@Test void c01_append() throws IOException {
		var file = TEST_FILE.toFile();
		// Write initial content
		try (var writer = FileWriterBuilder.create(file).build()) {
			writer.write("initial");
		}
		// Append more content
		try (var writer = FileWriterBuilder.create(file).append().build()) {
			writer.write("appended");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("initialappended", content);
	}

	@Test void c02_append_newFile() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file).append().build()) {
			writer.write("appended");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("appended", content);
	}

	//====================================================================================================
	// buffered() tests
	//====================================================================================================
	@Test void d01_buffered() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file).buffered().build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("test", content);
	}

	@Test void d02_bufferedAndAppend() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file).build()) {
			writer.write("initial");
		}
		try (var writer = FileWriterBuilder.create(file).buffered().append().build()) {
			writer.write("appended");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("initialappended", content);
	}

	//====================================================================================================
	// charset(Charset) tests
	//====================================================================================================
	@Test void e01_charsetCharset() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file)
			.charset(StandardCharsets.UTF_8)
			.build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE, StandardCharsets.UTF_8);
		assertEquals("test", content);
	}

	@Test void e02_charsetCharset_null() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file)
			.charset((Charset)null)
			.build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("test", content);
	}

	@Test void e03_charsetCharset_iso8859_1() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file)
			.charset(StandardCharsets.ISO_8859_1)
			.build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE, StandardCharsets.ISO_8859_1);
		assertEquals("test", content);
	}

	//====================================================================================================
	// charset(String) tests
	//====================================================================================================
	@Test void f01_charsetString() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create(file)
			.charset("UTF-8")
			.build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE, StandardCharsets.UTF_8);
		assertEquals("test", content);
	}

	@Test void f02_charsetString_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileWriterBuilder.create(TEST_FILE.toFile())
				.charset((String)null)
				.build();
		});
	}

	@Test void f03_charsetString_invalid() {
		assertThrows(UnsupportedCharsetException.class, () -> {
			FileWriterBuilder.create(TEST_FILE.toFile())
				.charset("INVALID-CHARSET-NAME")
				.build();
		});
	}

	//====================================================================================================
	// file() tests
	//====================================================================================================
	@Test void g01_fileFile() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create()
			.file(file)
			.build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("test", content);
	}

	@Test void g02_fileString() throws IOException {
		try (var writer = FileWriterBuilder.create()
			.file(TEST_FILE.toString())
			.build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE);
		assertEquals("test", content);
	}

	//====================================================================================================
	// Chaining tests
	//====================================================================================================
	@Test void h01_chaining() throws IOException {
		var file = TEST_FILE.toFile();
		try (var writer = FileWriterBuilder.create()
			.file(file)
			.charset(StandardCharsets.UTF_8)
			.buffered()
			.append()
			.build()) {
			writer.write("test");
		}
		var content = Files.readString(TEST_FILE, StandardCharsets.UTF_8);
		assertEquals("test", content);
	}
}

