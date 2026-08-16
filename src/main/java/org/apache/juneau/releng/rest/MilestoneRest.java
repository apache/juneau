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

package org.apache.juneau.releng.rest;

import java.util.List;
import org.apache.juneau.http.Path;
import org.apache.juneau.rest.server.Rest;
import org.apache.juneau.rest.server.RestGet;
import org.apache.juneau.rest.server.servlet.BasicRestResource;
import org.apache.juneau.releng.milestone.GithubPrSource;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.milestone.PullRequest;
import org.apache.juneau.releng.util.ProcessRunner;

/** Milestone tab: preview the "** Changes" section for a version. */
@Rest(path = "/milestones", title = "Milestones")
public class MilestoneRest extends BasicRestResource {

	private final MilestoneService service;
	private final GithubPrSource prSource;
	private final ProcessRunner runner;
	private final String repoDir;

	public MilestoneRest(MilestoneService service, GithubPrSource prSource, ProcessRunner runner, String repoDir) {
		this.service = service;
		this.prSource = prSource;
		this.runner = runner;
		this.repoDir = repoDir;
	}

	/** Dry-run preview: the generated "** Changes" section for the given version's milestone. */
	@RestGet(path = "/{version}/changelog", produces = "text/plain")
	public String changelog(@Path("version") String version) {
		var prs = prSource.forMilestone(version);
		return service.renderChangesSection(prs);
	}

	/** JSON list of merged PRs for the version's milestone (curl/CLI). */
	@RestGet("/{version}/prs")
	public List<PullRequest> prs(@Path("version") String version) {
		return prSource.forMilestone(version);
	}

	/** The previous release tag the changelog is computed against (diagnostic). */
	@RestGet(path = "/{version}/previous-tag", produces = "text/plain")
	public String previousTag(@Path("version") String version) {
		var tags = runner.runLines(List.of("git", "-C", repoDir, "tag", "--list", "juneau-*"));
		var prev = service.previousTag(tags, version);
		return prev == null ? "(none)" : prev;
	}

	/**
	 * §8.1 milestone form pre-fill: resolves the milestone number by title-matching {@code version}, for the
	 * New-Release page to populate its (user-overridable) milestone field before starting a run.
	 */
	@RestGet("/{version}/resolve")
	public MilestoneResolution resolve(@Path("version") String version) {
		var out = new MilestoneResolution();
		out.milestoneNumber = prSource.resolveMilestoneNumber(version);
		return out;
	}

	public static class MilestoneResolution {
		public Integer milestoneNumber;
	}
}
