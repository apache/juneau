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

package org.apache.juneau.releng.nexus;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MavenSettingsCredentialsTest {

	private static Path settings(Path dir, String serversXml) throws Exception {
		var f = dir.resolve("settings.xml");
		Files.writeString(f, "<settings><servers>" + serversXml + "</servers></settings>");
		return f;
	}

	@Test
	void a01_resolvesMatchingServer(@TempDir Path dir) throws Exception {
		var f = settings(dir, "<server><id>other</id><username>x</username><password>y</password></server>"
			+ "<server><id>apache.releases.https</id><username>jb</username><password>pw</password></server>");
		var c = MavenSettingsCredentials.resolve("apache.releases.https", f);
		assertEquals("jb", c.username());
		assertEquals("pw", c.password());
	}

	@Test
	void a02_missingFileFails(@TempDir Path dir) {
		var missing = dir.resolve("nope.xml");
		var e = assertThrows(IllegalStateException.class,
			() -> MavenSettingsCredentials.resolve("s", missing));
		assertTrue(e.getMessage().contains("not found"), e.getMessage());
	}

	@Test
	void a03_unknownServerFails(@TempDir Path dir) throws Exception {
		var f = settings(dir, "<server><id>a</id><username>u</username><password>p</password></server>");
		var e = assertThrows(IllegalStateException.class, () -> MavenSettingsCredentials.resolve("zzz", f));
		assertTrue(e.getMessage().contains("No <server"), e.getMessage());
	}

	@Test
	void a04_encryptedPasswordFails(@TempDir Path dir) throws Exception {
		var f = settings(dir, "<server><id>a</id><username>u</username><password>{abc=}</password></server>");
		var e = assertThrows(IllegalStateException.class, () -> MavenSettingsCredentials.resolve("a", f));
		assertTrue(e.getMessage().contains("Encrypted"), e.getMessage());
	}

	@Test
	void a04b_passwordOnlyEndingWithBraceIsNotTreatedAsEncrypted(@TempDir Path dir) throws Exception {
		var f = settings(dir, "<server><id>a</id><username>u</username><password>p}</password></server>");
		assertEquals("p}", MavenSettingsCredentials.resolve("a", f).password());
	}

	@Test
	void a04c_passwordOnlyStartingWithBraceIsNotTreatedAsEncrypted(@TempDir Path dir) throws Exception {
		var f = settings(dir, "<server><id>a</id><username>u</username><password>{p</password></server>");
		assertEquals("{p", MavenSettingsCredentials.resolve("a", f).password());
	}

	@Test
	void a05_missingUsernameOrPasswordFails(@TempDir Path dir) throws Exception {
		var noPw = settings(dir, "<server><id>a</id><username>u</username></server>");
		assertThrows(IllegalStateException.class, () -> MavenSettingsCredentials.resolve("a", noPw));
		var noUser = settings(dir, "<server><id>a</id><password>p</password></server>");
		var e = assertThrows(IllegalStateException.class, () -> MavenSettingsCredentials.resolve("a", noUser));
		assertTrue(e.getMessage().contains("missing username/password"), e.getMessage());
	}

	@Test
	void a06_malformedXmlFails(@TempDir Path dir) throws Exception {
		var f = dir.resolve("settings.xml");
		Files.writeString(f, "<settings><servers>");
		var e = assertThrows(IllegalStateException.class, () -> MavenSettingsCredentials.resolve("a", f));
		assertTrue(e.getMessage().contains("Cannot parse"), e.getMessage());
	}

	@Test
	void a07_doctypeIsRejected(@TempDir Path dir) throws Exception {
		var f = dir.resolve("settings.xml");
		Files.writeString(f, "<!DOCTYPE settings [<!ENTITY x \"y\">]><settings/>");
		assertThrows(IllegalStateException.class, () -> MavenSettingsCredentials.resolve("a", f));
	}

	@Test
	void a08_defaultPathUsesUserHome(@TempDir Path dir) throws Exception {
		var old = System.getProperty("user.home");
		try {
			Files.createDirectories(dir.resolve(".m2"));
			Files.writeString(dir.resolve(".m2").resolve("settings.xml"),
				"<settings><servers><server><id>s</id><username>u</username><password>p</password></server></servers></settings>");
			System.setProperty("user.home", dir.toString());
			assertEquals("u", MavenSettingsCredentials.resolve("s").username());
			var client = NexusStagingClient.create("http://127.0.0.1:1", "prof", "s");
			assertNotNull(client);
		} finally {
			System.setProperty("user.home", old);
		}
	}
}
