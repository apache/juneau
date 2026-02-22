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

import org.apache.juneau.commons.logging.Logger;

import java.io.*;
import java.util.*;

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
 */
@SuppressWarnings({
	"unused", // Example/demo class - some fields used only for JGit configuration
})
public class GitControl {

	private String localPath;
	private String remotePath;
	private Repository localRepo;
	private Git git;
	private CredentialsProvider cp;
	private String name = "username";
	private String password = "password";

	/**
	 * Constructor.
	 *
	 * @param localPath Local directory path for the repository.
	 * @param remotePath Remote Git repository URI.
	 * @throws IOException If the repository cannot be opened.
	 */
	public GitControl(String localPath, String remotePath) throws IOException {
		this.localPath = localPath;
		this.remotePath = remotePath;
		this.localRepo = new FileRepository(localPath + "/.git");
		cp = new UsernamePasswordCredentialsProvider(this.name, this.password);
		git = new Git(localRepo);
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
	@SuppressWarnings({
		"resource" // Git resources managed by JGit library
	})
	public void cloneRepo() throws GitAPIException {
		Git.cloneRepository().setURI(remotePath).setDirectory(new File(localPath)).call();
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
		pc.setCredentialsProvider(cp).setForce(true).setPushAll();
		try {
			var it = pc.call().iterator();
			if (it.hasNext()) {
				Logger.getLogger(GitControl.class).info("{}", it.next().toString());
			}
		} catch (InvalidRemoteException e) {
			e.printStackTrace();
		}
	}
}