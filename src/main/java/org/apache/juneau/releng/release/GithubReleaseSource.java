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

package org.apache.juneau.releng.release;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.marshall.marshaller.Json;
import org.apache.juneau.releng.util.ProcessRunner;

/** Produces rows from GitHub Releases via {@code gh release list --repo <slug>}. */
public class GithubReleaseSource {

	private final ProcessRunner runner;
	private final String repoSlug;

	public GithubReleaseSource(ProcessRunner runner, String repoSlug) {
		this.runner = runner;
		this.repoSlug = repoSlug;
	}

	@SuppressWarnings({ "unchecked" // Parsed JSON is cast to its known generic shape.
	})
	public List<Release> list() {
		var json = runner.runText(List.of("gh", "api", "repos/" + repoSlug + "/releases", "--paginate"));
		if (json.isBlank())
			return List.of();
		List<Map<String, Object>> parsed = Json.DEFAULT.read(json, List.class);
		var out = new ArrayList<Release>();
		for (var m : parsed) {
			var tag = String.valueOf(m.getOrDefault("tag_name", ""));
			var v = ReleaseVersion.ofTag(tag);
			var draft = Boolean.TRUE.equals(m.get("draft"));
			var r = new Release(v.version(), draft ? "DRAFT" : "RELEASED", "github");
			r.stage = "Distributed";
			r.githubReleaseUrl = str(m.get("html_url"));
			var published = str(m.get("published_at"));
			r.released = published == null ? "—" : published.substring(0, Math.min(10, published.length()));
			out.add(r);
		}
		return out;
	}

	private static String str(Object o) {
		return o == null ? null : String.valueOf(o);
	}
}
