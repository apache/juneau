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
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.logging.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Tests for {@link JettyServerComponent#createLogDir(String, Logger)}.
 */
class JettyServerComponent_CreateLogDir_Test {

	@TempDir
	Path tmp;

	@Test void a01_createsMissingNestedDirectory() {
		var dir = tmp.resolve("a/b/logs");
		JettyServerComponent.createLogDir(dir.toString(), Logger.getAnonymousLogger());
		assertTrue(Files.isDirectory(dir));
	}

	@Test void a02_existingDirectoryIsLeftAlone() throws Exception {
		var dir = Files.createDirectory(tmp.resolve("logs"));
		var marker = Files.createFile(dir.resolve("keep.log"));
		JettyServerComponent.createLogDir(dir.toString(), Logger.getAnonymousLogger());
		assertTrue(Files.exists(marker));
	}

	@Test void a03_failureIsLoggedNotThrown() throws Exception {
		var file = Files.createFile(tmp.resolve("not-a-dir"));
		var records = new ArrayList<LogRecord>();
		var logger = Logger.getAnonymousLogger();
		logger.setUseParentHandlers(false);
		logger.addHandler(new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no-op */ }
			@Override public void close() { /* no-op */ }
		});
		JettyServerComponent.createLogDir(file.resolve("logs").toString(), logger);
		assertEquals(1, records.size());
		assertEquals(Level.WARNING, records.get(0).getLevel());
	}

	@Test void a04_createdDirectoryIsOwnerOnly() throws Exception {
		Assumptions.assumeTrue(tmp.getFileSystem().supportedFileAttributeViews().contains("posix"));
		var dir = tmp.resolve("private-logs");
		JettyServerComponent.createLogDir(dir.toString(), Logger.getAnonymousLogger());
		assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(dir));
	}

	@Test void a05_existingDirectoryPermissionsAreLeftAlone() throws Exception {
		Assumptions.assumeTrue(tmp.getFileSystem().supportedFileAttributeViews().contains("posix"));
		var dir = Files.createDirectory(tmp.resolve("shared-logs"));
		var perms = PosixFilePermissions.fromString("rwxr-xr-x");
		Files.setPosixFilePermissions(dir, perms);
		JettyServerComponent.createLogDir(dir.toString(), Logger.getAnonymousLogger());
		assertEquals(perms, Files.getPosixFilePermissions(dir));
	}

	@Test void a06_bundledRequestLogFormatOmitsQueryStringAndReferer() throws Exception {
		try (var in = JettyServerComponent.class.getResourceAsStream("/jetty.xml")) {
			var xml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertFalse(xml.contains("EXTENDED_NCSA_FORMAT"));
			assertTrue(xml.contains("\"%m %U %H\""));
			assertFalse(xml.contains("%q"));
			assertFalse(xml.contains("%{Referer}i"));
		}
	}

	@Test void a07_bundledConfigDisablesConsole() throws Exception {
		try (var in = JettyServerComponent.class.getResourceAsStream("/juneau.cfg")) {
			var text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			assertTrue(text.matches("(?s).*\\[Console\\][^\\[]*\\benabled\\s*=\\s*false.*"));
		}
	}
}
