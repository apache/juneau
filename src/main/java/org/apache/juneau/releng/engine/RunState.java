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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** The per-target-version run record. Mirrored to {@code rm.state.dir/release-<version>.json}. */
public class RunState {

	public String version; // "9.2.1"
	public String branch; // resolved at preflight
	public int rc = 1;
	public List<RcHistoryEntry> rcHistory = new ArrayList<>();
	public RunStatus status = RunStatus.RUNNING;
	public String currentStepId;
	public String createdAt;
	public String updatedAt;
	public String voteDeadline; // ISO-8601 or null
	public String developmentVersion; // human-supplied when z==0; else null
	public String nexusRepoId; // set by nexus-staging-close
	public String githubReleaseUrl; // set by github-release-create
	public Integer milestoneNumber; // New-Release form field; pre-filled by gh title-match resolution, user-editable
	public List<StepState> steps = new ArrayList<>();

	public RunState() {
		// No-arg constructor required so the JSON parser can instantiate this bean before populating fields.
	}

	/** Build a fresh run with all steps seeded to PENDING. */
	public static RunState create(String version, String branch, List<String> stepIds) {
		var rs = new RunState();
		rs.version = version;
		rs.branch = branch;
		rs.rc = 1;
		rs.status = RunStatus.RUNNING;
		var now = Instant.now().toString();
		rs.createdAt = now;
		rs.updatedAt = now;
		for (var id : stepIds)
			rs.steps.add(new StepState(id));
		rs.currentStepId = stepIds.isEmpty() ? null : stepIds.get(0);
		return rs;
	}

	/** The {@link StepState} for {@code id}, or null. */
	public StepState step(String id) {
		for (var s : steps)
			if (s.id.equals(id))
				return s;
		return null;
	}

	public void touch() {
		updatedAt = Instant.now().toString();
	}
}
