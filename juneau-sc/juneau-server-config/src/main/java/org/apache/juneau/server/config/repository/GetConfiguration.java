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

public class GetConfiguration implements Command {

	private String project;
	private String branch;

	public GetConfiguration(String project, String branch) {
		this.branch = branch;
		this.project = project;
	}

	@Override
	public void execute() throws Exception {

		GitControl gitControl = new GitControl("/home/marcelo/desenvolvimento/tmp/juneau-config-test",
				"https://github.com/marcelosv/juneau-config-test.git");

		String pathDefalt = "/home/marcelo/desenvolvimento/tmp/juneau-config-test";

		File path = new File(pathDefalt);

		if (path.isDirectory()) {
			gitControl.pullFromRepo();
		} else {
			gitControl.cloneRepo();
		}

		gitControl.branch(branch);
		gitControl.pullFromRepo();

	}

}
