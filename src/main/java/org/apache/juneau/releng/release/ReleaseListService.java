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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Supplier;

/** Merges historical (git-tag), promoted (GitHub Releases), and in-progress (local state) rows. */
public class ReleaseListService {

	private final Supplier<List<Release>> tags;
	private final Supplier<List<Release>> github;
	private final Supplier<List<Release>> state;

	public ReleaseListService(Supplier<List<Release>> tags, Supplier<List<Release>> github,
			Supplier<List<Release>> state) {
		this.tags = tags;
		this.github = github;
		this.state = state;
	}

	public List<Release> list() {
		// 1. Released rows keyed by version; git tags first, then enriched by GitHub Releases.
		var released = new LinkedHashMap<String, Release>();
		for (var r : tags.get())
			released.put(r.version, r);
		for (var g : github.get()) {
			var existing = released.get(g.version);
			if (existing == null) {
				released.put(g.version, g);
				continue;
			}
			existing.githubReleaseUrl = g.githubReleaseUrl;
			if (g.released != null && !"—".equals(g.released))
				existing.released = g.released;
			if (existing.milestoneUrl == null)
				existing.milestoneUrl = g.milestoneUrl;
		}

		// 2. In-progress rows (local state) are kept as distinct rows (an RC/DROPPED attempt can
		//    coexist with a later RELEASED row of the same version — see the design mockup).
		var out = new ArrayList<Release>(released.values());
		out.addAll(state.get());

		// 3. Sort: version desc; within a version, in-progress (non-RELEASED) rows first.
		out.sort(Comparator.comparing((Release r) -> ReleaseVersion.of(r.version)).reversed()
				.thenComparing(r -> "RELEASED".equals(r.status) ? 1 : 0));
		return out;
	}
}
