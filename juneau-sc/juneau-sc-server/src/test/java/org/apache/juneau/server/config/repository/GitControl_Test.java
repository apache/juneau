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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.secret.*;
import org.eclipse.jgit.api.*;
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
@SuppressWarnings({
	"resource" // new BasicBeanStore().addBean(...) is a fluent self-return; the try-with-resources on 'beanStore' already closes the same instance the constructor produced.
})
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
		try (var beanStore = new BasicBeanStore()) {
			assertNull(GitControl.findCredentialsProvider(null, "GIT_TOKEN", beanStore));
		}
	}

	@Test void a02_contributedStoreResolvesSecret() throws Exception {
		var store = new InMemorySecretStore();
		store.store("GIT_TOKEN", "hunter2".toCharArray());

		try (var beanStore = new BasicBeanStore().addBean(SecretStore.class, store)) {
			var cp = GitControl.findCredentialsProvider("svc-account", "GIT_TOKEN", beanStore);
			assertInstanceOf(UsernamePasswordCredentialsProvider.class, cp);
			assertArrayEquals("hunter2".toCharArray(), passwordOf(cp));
		}
	}

	@Test void a03_absentSecretYieldsEmptyPassword() throws Exception {
		// No store contributed -> InMemorySecretStore default -> key absent -> empty password (never a stale literal).
		var cp = GitControl.findCredentialsProvider("svc-account", "GIT_TOKEN", null);
		assertInstanceOf(UsernamePasswordCredentialsProvider.class, cp);
		assertArrayEquals(new char[0], passwordOf(cp));
	}

	@Test void a04_emptyBeanStoreFallsBackToInMemoryDefault() throws Exception {
		try (var beanStore = new BasicBeanStore()) {
			var cp = GitControl.findCredentialsProvider("svc-account", "GIT_TOKEN", beanStore);
			assertArrayEquals(new char[0], passwordOf(cp));
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Constructor integration.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_secretStoreConstructorWiresCredentialsAndClosesCleanly(@TempDir File dir) throws IOException {
		var store = new InMemorySecretStore();
		store.store("GIT_TOKEN", "hunter2".toCharArray());

		try (var beanStore = new BasicBeanStore().addBean(SecretStore.class, store);
				var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git", "svc-account", "GIT_TOKEN", beanStore, false)) {
			assertNotNull(gitControl);
		}
	}

	@Test void b02_nullUsernameConstructorUsesAnonymousTransport(@TempDir File dir) throws IOException {
		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git", null, "GIT_TOKEN", null, false)) {
			assertNotNull(gitControl);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// Branch validation and object-database reads.
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_branchRejectsInvalidNames(@TempDir File dir) throws IOException {
		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git")) {
			for (var b : new String[]{null, "", "..", "a/../b", "a\\b", "a\0b", "-x", "--orphan", "HEAD", "a b", "a;b", "a~1"})
				assertThrows(IllegalArgumentException.class, () -> gitControl.branch(b), String.valueOf(b));
		}
	}

	@Test void c02_readBlobRejectsInvalidBranch(@TempDir File dir) throws IOException {
		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git")) {
			assertThrows(IllegalArgumentException.class, () -> gitControl.readBlob("-x", "a.cfg", 10));
			assertThrows(IllegalArgumentException.class, () -> gitControl.readBlob("HEAD", "a.cfg", 10));
		}
	}

	private static void commit(File dir, String branch, String file, String content) throws Exception {
		try (var g = Git.init().setDirectory(dir).setInitialBranch("main").call()) {
			if (neq("main", branch))
				g.checkout().setCreateBranch(true).setName(branch).call();
			Files.writeString(new File(dir, file).toPath(), content);
			g.add().addFilepattern(".").call();
			g.commit().setMessage("c").setSign(false).setAuthor("t", "t@example.com").setCommitter("t", "t@example.com").call();
		}
	}

	@Test void d01_readBlobReadsCommittedContentNotWorkingTree(@TempDir File dir) throws Exception {
		commit(dir, "main", "a.cfg", "[S]\nk=v\n");
		Files.writeString(new File(dir, "a.cfg").toPath(), "changed");
		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git")) {
			assertEquals("[S]\nk=v\n", gitControl.readBlob("main", "a.cfg", 1024));
			assertNull(gitControl.readBlob("main", "missing.cfg", 1024));
			assertThrows(IOException.class, () -> gitControl.readBlob("nope", "a.cfg", 1024));
		}
	}

	@Test void d02_readBlobReadsRequestedBranchWhileAnotherIsCheckedOut(@TempDir File dir) throws Exception {
		commit(dir, "main", "a.cfg", "main-content");
		try (var g = Git.open(dir)) {
			g.checkout().setCreateBranch(true).setName("other").call();
			Files.writeString(new File(dir, "a.cfg").toPath(), "other-content");
			g.add().addFilepattern(".").call();
			g.commit().setMessage("o").setSign(false).setAuthor("t", "t@example.com").setCommitter("t", "t@example.com").call();
		}
		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git")) {
			assertEquals("main-content", gitControl.readBlob("main", "a.cfg", 1024));
			assertEquals("other-content", gitControl.readBlob("other", "a.cfg", 1024));
		}
	}

	@Test void d03_readBlobRefusesOversizedBlob(@TempDir File dir) throws Exception {
		commit(dir, "main", "a.cfg", "x".repeat(11));
		try (var gitControl = new GitControl(dir.getAbsolutePath(), "https://example.com/repo.git")) {
			assertEquals("x".repeat(11), gitControl.readBlob("main", "a.cfg", 11));
			assertThrows(IOException.class, () -> gitControl.readBlob("main", "a.cfg", 10));
		}
	}
}
