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
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import org.apache.juneau.config.Config;

@SuppressWarnings("javadoc")
public class GetConfiguration implements Command, GetValue<Map<String, ConfigItem>> {

	private static final String APPLICATION = "APPLICATION";
	private static final String PROJECT = "PROJECT";
	private static final String EXT = ".cfg";
	private static final String BAR = "/";

	private Map<String, ConfigItem> configs = new HashMap<>();

	private String project;
	private String branch;

	public GetConfiguration(String project, String branch) {
		this.branch = branch;
		this.project = project;
	}

	@Override
	public void execute() throws Exception {

		Config config = Config.create().name("juneau-server-config.cfg").build();

		String pathStr = config.get("GitServer/pathLocal").orElse(null);

		String git = config.get("GitServer/gitRemote").orElse(null);

		GitControl gitControl = new GitControl(pathStr, git);

		File path = new File(pathStr);

		if (path.isDirectory()) {
			gitControl.pullFromRepo();
		} else {
			gitControl.cloneRepo();
		}

		gitControl.branch(branch);
		gitControl.pullFromRepo();

		String fileDefaultStr = APPLICATION.toLowerCase().concat(EXT);
		String fileProjectStr = this.project.concat(EXT);

		File fileDefault = new File(pathStr.concat(BAR).concat(fileDefaultStr));
		if (fileDefault.exists()) {
			String lines = new String(Files.readAllBytes(fileDefault.toPath()));
			configs.put(APPLICATION, new ConfigItem(lines));
		}

		File fileProject = new File(pathStr.concat(BAR).concat(fileProjectStr));
		if (fileProject.exists()) {
			String linesProject = new String(Files.readAllBytes(fileProject.toPath()));
			configs.put(PROJECT, new ConfigItem(linesProject));
		}
	}

	@Override
	public Map<String, ConfigItem> get() {
		return configs;
	}

}
