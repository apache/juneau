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
package org.apache.juneau.server.config.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.secret.*;
import org.eclipse.jgit.transport.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

/**
 * Coverage for {@link GitControl}'s {@link SecretStore}-backed credential resolution.
 *
 * <p>
 * The {@code findCredentialsProvider} tests exercise the resolution logic directly without opening a repository; the
 * construction test confirms the {@link SecretStore} constructor wires the resolved credentials through without
 * regressing the anonymous default.
 */
class GitControl_Test {

	private static char[] passwordOf(CredentialsProvider cp) throws Exception {
		var pass = new CredentialItem.Password();
		assertTrue(cp.get(new URIish("https://example.com/repo.git"), pass));
		return pass.getValue();
	}

	// -----------------------------------------------------------------------------------------------------------------
	// findCredentialsProvider - the SecretStore resolution logic.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_nullUsernameYieldsAnonymousTransport() {
		assertNull(GitControl.findCredentialsProvider(null, "GIT_TOKEN", new BasicBeanStore()));
	}

	@Test void a02_contributedStoreResolvesSecret() throws Exception {
		var store = new InMemorySecretStore();
		store.store("GIT_TOKEN", "hunter2".toCharArray());
		var beanStore = new BasicBeanStore().addBean(SecretStore.class, store);

		var cp = GitControl.findCredentialsProvider("svc-account", "GIT_TOKEN", beanStore);
		assertInstanceOf(UsernamePasswordCredentialsProvider.class, cp);
		assertArrayEquals("hunter2".toCharArray(), passwordOf(cp));
	}

	@Test void a03_absentSecretYieldsEmptyPassword() throws Exception {
		// No store contributed -> InMemorySecretStore default -> key absent -> empty password (never a stale literal).
		var cp = GitControl.findCredentialsProvider("svc-account", "GIT_TOKEN", null);
		assertInstanceOf(UsernamePasswordCredentialsProvider.class, cp);
		assertArrayEquals(new char[0], passwordOf(cp));
	}

	@Test void a04_emptyBeanStoreFallsBackToInMemoryDefault() throws Exception {
		var cp = GitControl.findCredentialsProvider("svc-account", "GIT_TOKEN", new BasicBeanStore());
		assertArrayEquals(new char[0], passwordOf(cp));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Constructor integration.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_secretStoreConstructorWiresCredentialsAndClosesCleanly(@TempDir File dir) throws IOException {
		var store = new InMemorySecretStore();
		store.store("GIT_TOKEN", "hunter2".toCharArray());
		var beanStore = new BasicBeanStore().addBean(SecretStore.class, store);

		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git", "svc-account", "GIT_TOKEN", beanStore, false)) {
			assertNotNull(gitControl);
		}
	}

	@Test void b02_nullUsernameConstructorUsesAnonymousTransport(@TempDir File dir) throws IOException {
		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git", null, "GIT_TOKEN", null, false)) {
			assertNotNull(gitControl);
		}
	}
}
