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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Merges historical (git-tag), promoted (GitHub Releases), and in-progress (local state) rows.
 */
public class ReleaseListService {

	private final Supplier<List<Release>> tags;
	private final Supplier<List<Release>> github;
	private final Supplier<List<Release>> state;

	/**
	 * Creates a service merging rows from {@code tags}, {@code github}, and {@code state}.
	 */
	public ReleaseListService(Supplier<List<Release>> tags, Supplier<List<Release>> github,
			Supplier<List<Release>> state) {
		this.tags = tags;
		this.github = github;
		this.state = state;
	}

	/**
	 * The merged, sorted Releases-tab rows from all three sources.
	 */
	public List<Release> list() {
		Map<String, Release> released = m();
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

		// An RC/DROPPED attempt can coexist with a later RELEASED row of the same version.
		var out = tl(released.values());
		out.addAll(state.get());

		out.sort(Comparator.comparing((Release r) -> ReleaseVersion.of(r.version)).reversed()
				.thenComparing(r -> "RELEASED".equals(r.status) ? 1 : 0));
		for (var r : out)
			r.id = r.rowId();
		return out;
	}
}
