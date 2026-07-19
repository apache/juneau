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

import java.io.*;

import org.apache.juneau.commons.logging.*;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.internal.storage.file.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;


/**
 * Git repository control operations for cloning, pulling, pushing, and committing.
 *
 * <p>
 * Provides a simplified wrapper around JGit for server configuration repository management.
 * Used to fetch configuration files from a remote Git repository.
 *
 * <p>
 * The underlying JGit {@link Repository} and {@link Git} handles are opened in the constructor and released by
 * {@link #close()}, so instances must be used with try-with-resources to avoid leaking file/native handles.
 */
public class GitControl implements AutoCloseable {

	private final String localPath;
	private final String remotePath;
	@SuppressWarnings("resource") // Owned by this AutoCloseable; released in close().
	private final Repository localRepo;
	@SuppressWarnings("resource") // Owned by this AutoCloseable; released in close().
	private final Git git;
	private final CredentialsProvider cp;
	private final boolean forcePush;

	/**
	 * Constructor.
	 *
	 * <p>
	 * Equivalent to {@link #GitControl(String, String, String, String, boolean) GitControl(localPath, remotePath,
	 * null, null, false)}: no credentials (anonymous transport) and non-force push.
	 *
	 * @param localPath Local directory path for the repository.
	 * @param remotePath Remote Git repository URI.
	 * @throws IOException If the repository cannot be opened.
	 */
	public GitControl(String localPath, String remotePath) throws IOException {
		this(localPath, remotePath, null, null, false);
	}

	/**
	 * Constructor.
	 *
	 * <p>
	 * Credentials are supplied by the caller (typically from operator configuration or a secret store) rather than
	 * hardcoded.  Pass {@code null} for {@code username} to use anonymous transport.
	 *
	 * @param localPath Local directory path for the repository.
	 * @param remotePath Remote Git repository URI.
	 * @param username Git username, or <jk>null</jk> for anonymous transport.
	 * @param password Git password/token.  Ignored when {@code username} is <jk>null</jk>.
	 * @param forcePush Whether {@link #pushToRepo()} performs a force-push (destructive remote history rewrite).
	 * 	<br>Force-push is opt-in; defaults to <jk>false</jk> in the other constructors.
	 * @throws IOException If the repository cannot be opened.
	 */
	public GitControl(String localPath, String remotePath, String username, String password, boolean forcePush) throws IOException {
		this.localPath = localPath;
		this.remotePath = remotePath;
		this.localRepo = new FileRepository(localPath + "/.git");
		this.cp = username == null ? null : new UsernamePasswordCredentialsProvider(username, password);
		this.forcePush = forcePush;
		git = new Git(localRepo);
	}

	/**
	 * Closes the underlying JGit {@link Git} and {@link Repository} handles.
	 *
	 * <p>
	 * The {@link Git} instance wraps an externally-supplied {@link Repository} (it does not close it on
	 * {@link Git#close()}), so both are released explicitly here.
	 */
	@Override
	public void close() {
		git.close();
		localRepo.close();
	}

	/**
	 * Stages all files for commit.
	 *
	 * @throws GitAPIException If staging fails.
	 */
	public void addToRepo() throws GitAPIException {
		var add = git.add();
		add.addFilepattern(".").call();
	}

	/**
	 * Checks out the specified branch from origin.
	 *
	 * @param name Branch name.
	 * @throws GitAPIException If checkout fails.
	 */
	public void branch(String name) throws GitAPIException {
		git.checkout().setName(name).setStartPoint("origin/".concat(name)).call();
	}

	/**
	 * Clones the remote repository to the local path.
	 *
	 * @throws GitAPIException If clone fails.
	 */
	public void cloneRepo() throws GitAPIException {
		// call() returns a Git handle wrapping the freshly-cloned repo; the clone (working tree on disk) is the
		// side-effect we want, so close the returned handle immediately to avoid leaking file/native handles.
		try (var g = Git.cloneRepository().setURI(remotePath).setDirectory(new File(localPath)).call()) {
			// No further operations needed on the cloned handle.
		}
	}

	/**
	 * Commits staged changes with the specified message.
	 *
	 * @param message Commit message.
	 * @throws JGitInternalException If an internal JGit error occurs.
	 * @throws GitAPIException If commit fails.
	 */
	public void commitToRepo(String message)
		throws JGitInternalException, GitAPIException {
		git.commit().setMessage(message).call();
	}

	/**
	 * Pulls the latest changes from the remote repository.
	 *
	 * @throws GitAPIException If pull fails.
	 */
	public void pullFromRepo() throws GitAPIException {
		git.pull().call();
	}

	/**
	 * Pushes all committed changes to the remote repository.
	 *
	 * @throws JGitInternalException If an internal JGit error occurs.
	 * @throws GitAPIException If push fails.
	 */
	public void pushToRepo() throws JGitInternalException, GitAPIException {
		var pc = git.push();
		if (cp != null)
			pc.setCredentialsProvider(cp);
		// Force-push is opt-in (see constructor); a destructive remote history rewrite is never performed by default.
		pc.setForce(forcePush).setPushAll();
		try {
			var it = pc.call().iterator();
			if (it.hasNext()) {
				Logger.getLogger(GitControl.class).info("{}", it.next().toString());
			}
		} catch (InvalidRemoteException e) {
			Logger.getLogger(GitControl.class).warning(e, "Error pushing to remote repository.");
		}
	}
}