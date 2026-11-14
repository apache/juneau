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

	// Serializes pull/checkout/pull on the single shared checkout so concurrent requests cannot interleave branches.
	private static final Object GIT_LOCK = new Object();

	// Remote pulls are throttled: at most one fetch per interval (settable as [GitServer] pullInterval, in seconds).
	private static final long DEFAULT_PULL_INTERVAL_SECONDS = 60;
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
		validate("project", project, false);
		validate("branch", branch, true);
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
			pullInterval = config.get("GitServer/pullInterval").asLong().orElse(DEFAULT_PULL_INTERVAL_SECONDS);
		} finally {
			config.close();
		}

		// Fail fast with a clear message when the operator has not configured the local checkout directory,
		// rather than surfacing an opaque NullPointerException from new File(null) below.
		if (pathStr == null)
			throw new IllegalStateException("Required configuration 'GitServer/pathLocal' is not set.");

		// Trusted operator-configured local git checkout directory (GitServer/pathLocal).
		// Serves as the confinement root for the project/application config file lookups below.
		var root = new File(pathStr);

		var fileDefaultStr = lcr(APPLICATION).concat(EXT);
		var fileProjectStr = this.project.concat(EXT);

		// The checkout is shared, so the git operations AND the file reads must happen under one lock
		// (otherwise another request could switch branches between checkout and read).
		synchronized (GIT_LOCK) {
			try (var gitControl = new GitControl(pathStr, git)) {

				var now = System.nanoTime();
				var due = ! pulledOnce || now - lastPullNanos >= pullInterval * 1_000_000_000L;
				if (! root.isDirectory()) {
					gitControl.cloneRepo();
					pulledOnce = true;
					lastPullNanos = now;
				} else if (due) {
					gitControl.pullFromRepo();
					pulledOnce = true;
					lastPullNanos = now;
				}

				gitControl.branch(branch);
				if (due)
					gitControl.pullFromRepo();
			}

			readFiles(root, fileDefaultStr, fileProjectStr);
		}
	}

	private void readFiles(File root, String fileDefaultStr, String fileProjectStr) throws IOException {
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

	// Project becomes a file name, so no separators at all.  Branch is only passed to git as a ref name, so
	// legitimate names such as "feature/foo" are allowed, but '..', backslash and NUL are not.
	private static void validate(String name, String value, boolean allowSlash) {
		if (value == null || value.isEmpty() || value.contains("\\") || value.contains("..") || value.indexOf('\0') >= 0 || (! allowSlash && value.contains("/")))
			throw new IllegalArgumentException("Invalid " + name + " value.");
	}
}
