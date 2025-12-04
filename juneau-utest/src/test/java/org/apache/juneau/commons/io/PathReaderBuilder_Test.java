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
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
/**
 * Tests {@link PathReaderBuilder}.
 */
class PathReaderBuilder_Test extends TestBase {

	private static final Path PATH = Paths.get("src/test/resources/files/Test3.properties");

	@Test void a01_allowNoFile() throws IOException {
		final var p = new Properties();
		try (Reader r = PathReaderBuilder.create().allowNoFile().build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertNull(p.get("file"));
		p.clear();
		try (Reader r = PathReaderBuilder.create().allowNoFile().path("this file does not exist, at all.").build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertNull(p.get("file"));
	}

	@Test void a02_allowNoFileException() {
		assertThrows(IllegalStateException.class, () -> PathReaderBuilder.create().build());  // NOSONAR
		assertThrows(NoSuchFileException.class, () -> PathReaderBuilder.create().path("this file does not exist, at all.").build());
	}

	@Test void a03_charsetCharset() throws IOException {
		final var p = new Properties();
		try (Reader r = PathReaderBuilder.create().path(PATH).charset(StandardCharsets.UTF_8).build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertEquals("files/Test3.properties", p.get("file"));
		p.clear();
		try (Reader r = PathReaderBuilder.create().path(PATH).charset((Charset) null).build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertEquals("files/Test3.properties", p.get("file"));
	}

	@Test void a04_charsetString() throws IOException {
		final var p = new Properties();
		try (Reader r = PathReaderBuilder.create().path(PATH).charset(StandardCharsets.UTF_8.name()).build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertEquals("files/Test3.properties", p.get("file"));
		p.clear();
		try (Reader r = PathReaderBuilder.create().path(PATH).charset((String) null).build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertEquals("files/Test3.properties", p.get("file"));
	}

	@Test void a05_create() throws IOException {
		final var p = new Properties();
		try (Reader r = PathReaderBuilder.create(PATH).build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertEquals("files/Test3.properties", p.get("file"));
	}

	@Test void a06_pathPath() throws IOException {
		final var p = new Properties();
		try (Reader r = PathReaderBuilder.create().path(PATH).build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertEquals("files/Test3.properties", p.get("file"));
	}

	@Test void a07_pathString() throws IOException {
		final var p = new Properties();
		try (Reader r = PathReaderBuilder.create().path(PATH.toString()).build()) {
			p.load(new StringReader(read(r, Files.size(PATH))));
		}
		assertEquals("files/Test3.properties", p.get("file"));
	}
}