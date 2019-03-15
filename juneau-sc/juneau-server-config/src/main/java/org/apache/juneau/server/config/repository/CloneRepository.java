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
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

class CloneRepository implements Command {

	private String branch;

	public CloneRepository(String branch) {
		this.branch = branch;
	}

	@Override
	public void execute() {

		String path = "/home/marcelo/desenvolvimento/tmp/juneau-config-test";
		
		try {
		FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
		repositoryBuilder.setGitDir( new File(path) );
		Repository repository = repositoryBuilder.build();
		boolean repositoryExists = repository.getRef( "HEAD" ) != null;
		
		
			//Files.delete(Paths.get(path));
		} catch (IOException e1) {
		}
		
		try {
			Git.cloneRepository().setURI("https://github.com/marcelosv/juneau-config-test.git")
					.setDirectory(new File(path))
					.setBranch("refs/heads/" + branch).call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}

	}

}
