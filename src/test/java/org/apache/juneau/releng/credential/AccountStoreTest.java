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

package org.apache.juneau.releng.credential;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

class AccountStoreTest {

	@Test
	void unsetAccountIsEmpty(@TempDir Path dir) {
		var store = new AccountStore(dir);
		assertTrue(store.get(CredentialSpec.APACHE_LDAP).isEmpty());
	}

	@Test
	void putThenGetReturnsTheAccount(@TempDir Path dir) {
		var store = new AccountStore(dir);
		store.put(CredentialSpec.APACHE_LDAP, "jbognar");
		assertEquals("jbognar", store.get(CredentialSpec.APACHE_LDAP).orElseThrow());
	}

	@Test
	void survivesAFreshInstancePointedAtTheSameDir(@TempDir Path dir) {
		new AccountStore(dir).put(CredentialSpec.GPG, "ABCD1234");

		var reloaded = new AccountStore(dir);
		assertEquals("ABCD1234", reloaded.get(CredentialSpec.GPG).orElseThrow());
	}

	@Test
	void distinctSpecsAreStoredIndependently(@TempDir Path dir) {
		var store = new AccountStore(dir);
		store.put(CredentialSpec.APACHE_LDAP, "jbognar");
		store.put(CredentialSpec.GPG, "ABCD1234");

		var reloaded = new AccountStore(dir);
		assertEquals("jbognar", reloaded.get(CredentialSpec.APACHE_LDAP).orElseThrow());
		assertEquals("ABCD1234", reloaded.get(CredentialSpec.GPG).orElseThrow());
	}

	@Test
	void removeForgetsThePersistedAccount(@TempDir Path dir) {
		var store = new AccountStore(dir);
		store.put(CredentialSpec.APACHE_LDAP, "jbognar");
		store.remove(CredentialSpec.APACHE_LDAP);

		assertTrue(store.get(CredentialSpec.APACHE_LDAP).isEmpty());
		assertTrue(new AccountStore(dir).get(CredentialSpec.APACHE_LDAP).isEmpty());
	}

	@Test
	void createsTheStateDirIfAbsent(@TempDir Path dir) {
		var nested = dir.resolve("nested/state");
		var store = new AccountStore(nested);
		store.put(CredentialSpec.APACHE_LDAP, "jbognar");
		assertTrue(nested.resolve("accounts.properties").toFile().isFile());
	}

	// -----------------------------------------------------------------------------------------------------------
	// Explicit permissions rather than whatever the umask happens to be (finding F4).
	// -----------------------------------------------------------------------------------------------------------

	@Test
	@EnabledOnOs({ OS.MAC, OS.LINUX })
	void accountsFileIsOwnerOnly(@TempDir Path dir) throws Exception {
		// The file holds no secret, so this is hygiene -- but under a default umask it lands world-readable, and
		// it names the Apache account this machine releases as.
		new AccountStore(dir).put(CredentialSpec.APACHE_LDAP, "jbognar");
		var perms = Files.getPosixFilePermissions(dir.resolve("accounts.properties"));
		assertEquals(PosixFilePermissions.fromString("rw-------"), perms);
	}

	@Test
	@EnabledOnOs({ OS.MAC, OS.LINUX })
	void stateDirIsOwnerOnly(@TempDir Path dir) throws Exception {
		var nested = dir.resolve("nested/state");
		new AccountStore(nested).put(CredentialSpec.GPG, "ABCD1234");
		assertEquals(PosixFilePermissions.fromString("rwx------"), Files.getPosixFilePermissions(nested));
	}

	@Test
	@EnabledOnOs({ OS.MAC, OS.LINUX })
	void anExistingWideOpenFileIsNarrowedOnTheNextWrite(@TempDir Path dir) throws Exception {
		// A file written before this change keeps its mode unless something narrows it, and the natural mistake is
		// to set permissions only at creation time.
		var store = new AccountStore(dir);
		store.put(CredentialSpec.APACHE_LDAP, "jbognar");
		var file = dir.resolve("accounts.properties");
		Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-rw-rw-"));

		store.put(CredentialSpec.GPG, "ABCD1234");
		assertEquals(PosixFilePermissions.fromString("rw-------"), Files.getPosixFilePermissions(file));
	}
}
