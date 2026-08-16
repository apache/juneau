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

package org.apache.juneau.releng.engine;

import java.util.List;
import org.apache.juneau.releng.release.ReleaseVersion;
import org.apache.juneau.releng.util.ProcessRunner;

/** Resolves the target branch for a version and verifies (never creates) its remote existence. */
public class BranchResolver {

	private final ProcessRunner runner;
	private final String repoDir; // rm.repo.dir — the maintainer's existing working clone

	public BranchResolver(ProcessRunner runner, String repoDir) {
		this.runner = runner;
		this.repoDir = repoDir;
	}

	/** {@code master} when z==0, else {@code juneau-<version>-branch}. */
	public String resolve(String version) {
		var v = ReleaseVersion.of(version);
		return v.maintenance() == 0 ? "master" : "juneau-" + version + "-branch";
	}

	/** {@code git ls-remote --heads origin <branch>} against {@code rm.repo.dir} — true if a ref comes back. */
	public boolean remoteBranchExists(String branch) {
		var res = runner.run(List.of("git", "-C", repoDir, "ls-remote", "--heads", "origin", branch), null, null);
		return res.ok() && res.output() != null && res.output().contains("refs/heads/" + branch);
	}

	/** Human instructions shown when the target branch is missing (never auto-created). */
	public String missingBranchInstructions(String branch) {
		return "Branch '" + branch + "' does not exist on origin. This engine does not create maintenance "
				+ "branches.\nTo create it: git checkout -b " + branch + " <appropriate-source-commit> "
				+ "&& git push origin " + branch;
	}
}
