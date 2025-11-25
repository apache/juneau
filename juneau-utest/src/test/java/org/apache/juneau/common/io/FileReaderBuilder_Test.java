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

import static org.apache.juneau.common.utils.IOUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class FileReaderBuilder_Test extends TestBase {

	private static final Path PATH = Paths.get("src/test/resources/files/Test3.properties");

	//====================================================================================================
	// create() tests
	//====================================================================================================
	@Test void a01_create() {
		var builder = FileReaderBuilder.create();
		assertNotNull(builder);
	}

	@Test void a02_createWithFile() {
		var file = PATH.toFile();
		var builder = FileReaderBuilder.create(file);
		assertNotNull(builder);
	}

	//====================================================================================================
	// build() tests
	//====================================================================================================
	@Test void b01_build() throws IOException {
		var file = PATH.toFile();
		var reader = FileReaderBuilder.create(file).build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	@Test void b02_build_noFile() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileReaderBuilder.create().build();
		});
	}

	@Test void b03_build_fileNotFound() {
		assertThrows(FileNotFoundException.class, () -> {
			FileReaderBuilder.create(new File("nonexistent.txt")).build();
		});
	}

	//====================================================================================================
	// allowNoFile() tests
	//====================================================================================================
	@Test void c01_allowNoFile_noFile() throws IOException {
		var reader = FileReaderBuilder.create().allowNoFile().build();
		assertNotNull(reader);
		var content = read(reader, 0);
		assertEquals("", content);
		reader.close();
	}

	@Test void c02_allowNoFile_nonexistentFile() throws IOException {
		var reader = FileReaderBuilder.create()
			.file("nonexistent.txt")
			.allowNoFile()
			.build();
		assertNotNull(reader);
		var content = read(reader, 0);
		assertEquals("", content);
		reader.close();
	}

	@Test void c03_allowNoFile_existingFile() throws IOException {
		var file = PATH.toFile();
		var reader = FileReaderBuilder.create(file)
			.allowNoFile()
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	//====================================================================================================
	// charset(Charset) tests
	//====================================================================================================
	@Test void d01_charsetCharset() throws IOException {
		var file = PATH.toFile();
		var reader = FileReaderBuilder.create(file)
			.charset(StandardCharsets.UTF_8)
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	@Test void d02_charsetCharset_null() throws IOException {
		var file = PATH.toFile();
		var reader = FileReaderBuilder.create(file)
			.charset((Charset)null)
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	@Test void d03_charsetCharset_iso8859_1() throws IOException {
		var file = PATH.toFile();
		var reader = FileReaderBuilder.create(file)
			.charset(StandardCharsets.ISO_8859_1)
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	//====================================================================================================
	// charset(String) tests
	//====================================================================================================
	@Test void e01_charsetString() throws IOException {
		var file = PATH.toFile();
		var reader = FileReaderBuilder.create(file)
			.charset("UTF-8")
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	@Test void e02_charsetString_null() {
		assertThrows(IllegalArgumentException.class, () -> {
			FileReaderBuilder.create(PATH.toFile())
				.charset((String)null)
				.build();
		});
	}

	@Test void e03_charsetString_invalid() {
		assertThrows(UnsupportedCharsetException.class, () -> {
			FileReaderBuilder.create(PATH.toFile())
				.charset("INVALID-CHARSET-NAME")
				.build();
		});
	}

	//====================================================================================================
	// file() tests
	//====================================================================================================
	@Test void f01_fileFile() throws IOException {
		var file = PATH.toFile();
		var reader = FileReaderBuilder.create()
			.file(file)
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	@Test void f02_fileString() throws IOException {
		var reader = FileReaderBuilder.create()
			.file(PATH.toString())
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}

	//====================================================================================================
	// Chaining tests
	//====================================================================================================
	@Test void g01_chaining() throws IOException {
		var reader = FileReaderBuilder.create()
			.file(PATH.toFile())
			.charset(StandardCharsets.UTF_8)
			.allowNoFile()
			.build();
		assertNotNull(reader);
		var p = new Properties();
		p.load(new StringReader(read(reader, Files.size(PATH))));
		assertEquals("files/Test3.properties", p.get("file"));
		reader.close();
	}
}

