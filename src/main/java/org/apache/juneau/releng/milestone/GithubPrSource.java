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

package org.apache.juneau.releng.milestone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.util.ProcessRunner;

/** Fetches merged PRs attached to a milestone via {@code gh pr list}. */
public class GithubPrSource {

	private final ProcessRunner runner;
	private final String repoSlug;

	public GithubPrSource(ProcessRunner runner, String repoSlug) {
		this.runner = runner;
		this.repoSlug = repoSlug;
	}

	@SuppressWarnings({ "unchecked" // Parsed JSON is assigned/cast to its known generic shape.
	})
	public List<PullRequest> forMilestone(String milestoneTitle) {
		var json = runner.runText(List.of("gh", "pr", "list", "--repo", repoSlug, "--state", "merged", "--search",
				"milestone:\"" + milestoneTitle + "\"", "--json", "number,title,author", "--limit", "500"));
		if (json.isBlank())
			return List.of();
		List<Map<String, Object>> parsed = Json.DEFAULT.read(json, List.class);
		var out = new ArrayList<PullRequest>();
		for (var m : parsed) {
			var pr = new PullRequest();
			pr.number = ((Number) m.getOrDefault("number", 0)).intValue();
			pr.title = String.valueOf(m.getOrDefault("title", ""));
			var author = (Map<String, Object>) m.get("author");
			pr.authorLogin = author == null ? "" : String.valueOf(author.getOrDefault("login", ""));
			out.add(pr);
		}
		return out;
	}

	/**
	 * Resolves the GitHub milestone number whose title exactly matches {@code versionTitle} (e.g. "10.0.0"),
	 * via {@code gh api repos/<slug>/milestones?state=all}. Returns null if none matches (§8.1) — the
	 * New-Release form field is then left blank for the human to fill in manually.
	 */
	@SuppressWarnings({ "unchecked" // Parsed JSON is assigned/cast to its known generic shape.
	})
	public Integer resolveMilestoneNumber(String versionTitle) {
		var json = runner.runText(List.of("gh", "api", "repos/" + repoSlug + "/milestones?state=all"));
		if (json.isBlank())
			return null;
		List<Map<String, Object>> parsed = Json.DEFAULT.read(json, List.class);
		for (var m : parsed)
			if (versionTitle.equals(String.valueOf(m.getOrDefault("title", ""))))
				return ((Number) m.get("number")).intValue();
		return null;
	}
}
