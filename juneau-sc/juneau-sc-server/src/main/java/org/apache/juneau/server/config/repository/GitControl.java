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
package org.apache.juneau.server.config.repository;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.ConcurrentRefUpdateException;
import org.eclipse.jgit.api.errors.DetachedHeadException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidConfigurationException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoFilepatternException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.NoMessageException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.api.errors.WrongRepositoryStateException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.PushResult;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

@SuppressWarnings({"javadoc","unused"})
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

	public void cloneRepo() throws IOException, NoFilepatternException, GitAPIException {
		Git.cloneRepository().setURI(remotePath).setDirectory(new File(localPath)).call();
	}

	public void addToRepo() throws IOException, NoFilepatternException, GitAPIException {
		AddCommand add = git.add();
		add.addFilepattern(".").call();
	}

	public void commitToRepo(String message) throws IOException, NoHeadException, NoMessageException,
			ConcurrentRefUpdateException, JGitInternalException, WrongRepositoryStateException, GitAPIException {
		git.commit().setMessage(message).call();
	}

	public void branch(String name) throws RefAlreadyExistsException, RefNotFoundException, InvalidRefNameException,
			CheckoutConflictException, GitAPIException {
		git.checkout().setName(name).setStartPoint("origin/".concat(name)).call();
	}

	public void pushToRepo() throws IOException, JGitInternalException, InvalidRemoteException, GitAPIException {
		PushCommand pc = git.push();
		pc.setCredentialsProvider(cp).setForce(true).setPushAll();
		try {
			Iterator<PushResult> it = pc.call().iterator();
			if (it.hasNext()) {
				System.out.println(it.next().toString());
			}
		} catch (InvalidRemoteException e) {
			e.printStackTrace();
		}
	}

	public void pullFromRepo()
			throws IOException, WrongRepositoryStateException, InvalidConfigurationException, DetachedHeadException,
			InvalidRemoteException, CanceledException, RefNotFoundException, NoHeadException, GitAPIException {
		git.pull().call();
	}

}