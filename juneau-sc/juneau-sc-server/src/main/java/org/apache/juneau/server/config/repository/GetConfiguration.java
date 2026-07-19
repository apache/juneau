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

import static org.apache.juneau.commons.utils.FileUtils.*;
import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.config.*;

/**
 * Command that fetches configuration from a Git repository for a given project and branch.
 *
 * <p>
 * Clones or pulls the configured Git repo, then loads application and project-specific
 * configuration files into a map of {@link ConfigItem} values.
 */
public class GetConfiguration implements Command, GetValue<Map<String,ConfigItem>> {

	private static final String APPLICATION = "APPLICATION";
	private static final String PROJECT = "PROJECT";
	private static final String EXT = ".cfg";

	private Map<String,ConfigItem> configs = new HashMap<>();

	@SuppressWarnings({
		"java:S1845" // Field name intentionally differs only by case from static field PROJECT
	})
	private String project;
	private String branch;

	/**
	 * Constructor.
	 *
	 * @param project Project name.
	 * @param branch Branch name.
	 */
	public GetConfiguration(String project, String branch) {
		this.branch = branch;
		this.project = project;
	}

	/** {@inheritDoc} */
	@Override
	public void execute() throws Exception {

		var config = Config.create().name("juneau-server-config.cfg").build();

		var pathStr = config.get("GitServer/pathLocal").orElse(null);

		var git = config.get("GitServer/gitRemote").orElse(null);

		// Fail fast with a clear message when the operator has not configured the local checkout directory,
		// rather than surfacing an opaque NullPointerException from new File(null) below.
		if (pathStr == null)
			throw new IllegalStateException("Required configuration 'GitServer/pathLocal' is not set.");

		// Trusted operator-configured local git checkout directory (GitServer/pathLocal).
		// Serves as the confinement root for the project/application config file lookups below.
		var root = new File(pathStr);

		try (var gitControl = new GitControl(pathStr, git)) {

			if (root.isDirectory()) {
				gitControl.pullFromRepo();
			} else {
				gitControl.cloneRepo();
			}

			gitControl.branch(branch);
			gitControl.pullFromRepo();
		}

		var fileDefaultStr = lcr(APPLICATION).concat(EXT);
		var fileProjectStr = this.project.concat(EXT);

		// Resolve the config file names under the checkout root through the shared boundary check
		// so a crafted project name cannot escape the repo directory (empty = file absent).
		var fileDefault = resolveSafely(root, fileDefaultStr).orElse(null);
		if (fileDefault != null) {
			var lines = new String(Files.readAllBytes(fileDefault.toPath()));
			configs.put(APPLICATION, new ConfigItem(lines));
		}

		var fileProject = resolveSafely(root, fileProjectStr).orElse(null);
		if (fileProject != null) {
			var linesProject = new String(Files.readAllBytes(fileProject.toPath()));
			configs.put(PROJECT, new ConfigItem(linesProject));
		}
	}

	/** {@inheritDoc} */
	@Override
	public Map<String,ConfigItem> get() {
		return Map.copyOf(configs);
	}
}