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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.*;
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

	// Serializes pull/checkout/pull on the single shared checkout so concurrent requests cannot interleave branches.
	private static final Object GIT_LOCK = new Object();

	// Remote pulls are throttled: at most one fetch per interval (settable as [GitServer] pullInterval, in seconds).
	private static final long DEFAULT_PULL_INTERVAL_SECONDS = 60;
	private static final long MIN_PULL_INTERVAL_SECONDS = 1;

	// Upper bound on the size of a single configuration file read from the repository.
	static final long MAX_CONFIG_FILE_BYTES = 1024L * 1024L;
	private static long lastPullNanos;
	private static boolean pulledOnce;

	private final Map<String,ConfigItem> configs = new HashMap<>();

	@SuppressWarnings({
		"java:S1845" // Field name intentionally differs only by case from static field PROJECT
	})
	private String project;
	private final String branch;

	/**
	 * Constructor.
	 *
	 * @param project Project name.
	 * @param branch Branch name.
	 */
	public GetConfiguration(String project, String branch) {
		// Path variables are URL-decoded after the path is split, so an encoded '%2F' or '..' can survive (CWE-22).
		validateProject(project);
		BranchNames.validate(branch);
		this.branch = branch;
		this.project = project;
	}

	/** {@inheritDoc} */
	@Override
	public void execute() throws Exception {

		String pathStr;
		String git;
		long pullInterval;
		var config = Config.create().name("juneau-server-config.cfg").build();
		try {
			pathStr = config.get("GitServer/pathLocal").orElse(null);
			git = config.get("GitServer/gitRemote").orElse(null);
			pullInterval = clampPullInterval(config.get("GitServer/pullInterval").asLong().orElse(DEFAULT_PULL_INTERVAL_SECONDS));
		} finally {
			config.close();
		}

		// Fail fast with a clear message when the operator has not configured the local checkout directory,
		// rather than surfacing an opaque NullPointerException from new File(null) below.
		if (pathStr == null)
			throw new IllegalStateException("Required configuration 'GitServer/pathLocal' is not set.");

		// Trusted operator-configured local git checkout directory (GitServer/pathLocal).
		// Used to decide whether the repository must be cloned.
		var root = new File(pathStr);

		var fileDefaultStr = lcr(APPLICATION).concat(EXT);
		var fileProjectStr = this.project.concat(EXT);

		// The checkout is shared, so the git operations AND the object reads happen under one lock
		// (otherwise concurrent requests could interleave fetches).
		synchronized (GIT_LOCK) {
			try (var gitControl = new GitControl(pathStr, git)) {

				var now = System.nanoTime();
				var due = ! pulledOnce || now - lastPullNanos >= pullInterval * 1_000_000_000L;
				if (! root.isDirectory()) {
					gitControl.cloneRepo();
					pulledOnce = true;
					lastPullNanos = now;
				} else if (due) {
					gitControl.fetchFromRepo();
					pulledOnce = true;
					lastPullNanos = now;
				}

				// The files are read from the object database at the branch tip, so the shared working checkout is never switched or read.
				readFiles(gitControl, fileDefaultStr, fileProjectStr);
			}
		}
	}

	private void readFiles(GitControl gitControl, String fileDefaultStr, String fileProjectStr) throws IOException {
		var lines = gitControl.readBlob(branch, fileDefaultStr, MAX_CONFIG_FILE_BYTES);
		if (lines != null)
			configs.put(APPLICATION, new ConfigItem(lines));

		var linesProject = gitControl.readBlob(branch, fileProjectStr, MAX_CONFIG_FILE_BYTES);
		if (linesProject != null)
			configs.put(PROJECT, new ConfigItem(linesProject));
	}

	static long clampPullInterval(long seconds) {
		return Math.max(seconds, MIN_PULL_INTERVAL_SECONDS);
	}

	/** {@inheritDoc} */
	@Override
	public Map<String,ConfigItem> get() {
		return Map.copyOf(configs);
	}

	// Project becomes a file name, so no separators at all.
	private static void validateProject(String value) {
		if (value == null || value.isEmpty() || value.startsWith("-") || value.contains("\\") || value.contains("..") || value.indexOf('\0') >= 0 || value.contains("/"))
			throw new IllegalArgumentException("Invalid project value.");
	}
}
