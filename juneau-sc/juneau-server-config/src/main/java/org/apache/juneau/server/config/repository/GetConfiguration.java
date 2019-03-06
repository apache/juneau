package org.apache.juneau.server.config.repository;

public class GetConfiguration implements Command {

	private String project;
	private String branch;

	public GetConfiguration(String project, String branch) {
		this.branch = branch;
		this.project = project;
	}

	@Override
	public void execute() {
		new CloneRepository(branch).execute();
	}

}
