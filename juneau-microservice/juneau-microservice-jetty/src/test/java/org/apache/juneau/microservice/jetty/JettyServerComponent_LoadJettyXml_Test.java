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
package org.apache.juneau.microservice.jetty;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.logging.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Tests how {@link JettyServerComponent} locates the <c>jetty.xml</c> contents.
 */
class JettyServerComponent_LoadJettyXml_Test {

	private static Logger quietLogger(List<LogRecord> records) {
		var l = Logger.getAnonymousLogger();
		l.setUseParentHandlers(false);
		l.addHandler(new Handler() {
			@Override public void publish(LogRecord r) { records.add(r); }
			@Override public void flush() { /* no-op */ }
			@Override public void close() { /* no-op */ }
		});
		return l;
	}

	@Test void a01_unnamedLookupUsesClasspath() throws Exception {
		var xml = JettyServerComponent.loadJettyXml("jetty.xml", false, quietLogger(new ArrayList<>()));
		assertNotNull(xml);
		assertTrue(xml.contains("<Configure"));
	}

	@Test void a02_unnamedLookupIgnoresFileSystem() throws Exception {
		assertNull(JettyServerComponent.loadJettyXml("no-such-resource-" + UUID.randomUUID() + ".xml", false, quietLogger(new ArrayList<>())));
	}

	@Test void a03_namedLookupReadsFileSystem(@TempDir Path dir) throws Exception {
		var f = dir.resolve("custom.xml");
		Files.writeString(f, "<Configure/>");
		var records = new ArrayList<LogRecord>();
		assertEquals("<Configure/>", JettyServerComponent.loadJettyXml(f.toString(), true, quietLogger(records)));
	}

	@Test void a04_namedLookupMissing() throws Exception {
		assertNull(JettyServerComponent.loadJettyXml("no-such-file-" + UUID.randomUUID() + ".xml", true, quietLogger(new ArrayList<>())));
	}

	@Test void a05_warnsWhenWritableByOthers(@TempDir Path dir) throws Exception {
		var f = dir.resolve("custom.xml");
		Files.writeString(f, "<Configure/>");
		Assumptions.assumeTrue(f.getFileSystem().supportedFileAttributeViews().contains("posix"));
		var records = new ArrayList<LogRecord>();
		Files.setPosixFilePermissions(f, PosixFilePermissions.fromString("rw-rw-rw-"));
		JettyServerComponent.loadJettyXml(f.toString(), true, quietLogger(records));
		assertEquals(1, records.size());
		assertEquals(Level.WARNING, records.get(0).getLevel());

		records.clear();
		Files.setPosixFilePermissions(f, PosixFilePermissions.fromString("rw-r--r--"));
		JettyServerComponent.loadJettyXml(f.toString(), true, quietLogger(records));
		assertTrue(records.isEmpty());
	}
}
