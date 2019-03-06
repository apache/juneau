package org.apache.juneau.server.config.repository;

import java.io.File;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

class CloneRepository implements Command {

	private String branch;

	public CloneRepository(String branch) {
		this.branch = branch;
	}

	@Override
	public void execute() {

		try {
			Git.cloneRepository().setURI("https://github.com/marcelosv/juneau-config-test.git")
					.setDirectory(new File("/home/marcelo/desenvolvimento/tmp/testgit"))
					.setBranch("refs/heads/" + branch).call();
		} catch (GitAPIException e) {
			e.printStackTrace();
		}

	}

}
