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
import java.util.*;

import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.*;
import org.eclipse.jgit.internal.storage.file.*;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.transport.*;

@SuppressWarnings({ "javadoc", "unused" })
public class GitControl {

	private String localPath, remotePath;
	private Repository localRepo;
	private Git git;
	private CredentialsProvider cp;
	private String name = "username";
	private String password = "password";

	public GitControl(String localPath, String remotePath) throws IOException {
		this.localPath = localPath;
		this.remotePath = remotePath;
		this.localRepo = new FileRepository(localPath + "/.git");
		cp = new UsernamePasswordCredentialsProvider(this.name, this.password);
		git = new Git(localRepo);
	}

	public void addToRepo() throws IOException, NoFilepatternException, GitAPIException {
		var add = git.add();
		add.addFilepattern(".").call();
	}

	public void branch(String name) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException, CheckoutConflictException, GitAPIException {
		git.checkout().setName(name).setStartPoint("origin/".concat(name)).call();
	}

	@SuppressWarnings("resource")
	public void cloneRepo() throws IOException, NoFilepatternException, GitAPIException {
		Git.cloneRepository().setURI(remotePath).setDirectory(new File(localPath)).call();
	}

	public void commitToRepo(String message)
		throws IOException, NoHeadException, NoMessageException, ConcurrentRefUpdateException, JGitInternalException, WrongRepositoryStateException, GitAPIException {
		git.commit().setMessage(message).call();
	}

	public void pullFromRepo() throws IOException, WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException, InvalidRemoteException, CanceledException, RefNotFoundException,
		NoHeadException, GitAPIException {
		git.pull().call();
	}

	public void pushToRepo() throws IOException, JGitInternalException, InvalidRemoteException, GitAPIException {
		var pc = git.push();
		pc.setCredentialsProvider(cp).setForce(true).setPushAll();
		try {
			var it = pc.call().iterator();
			if (it.hasNext()) {
				System.out.println(it.next().toString());
			}
		} catch (InvalidRemoteException e) {
			e.printStackTrace();
		}
	}
}