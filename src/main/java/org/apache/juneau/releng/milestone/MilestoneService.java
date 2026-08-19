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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.juneau.releng.release.ReleaseVersion;

/** Milestone helpers: previous-tag selection and "** Changes" changelog generation. */
public class MilestoneService {

	// "Bump <dep> from <old> to <new>" with optional trailing " in <path>".
	private static final Pattern BUMP = Pattern
			.compile("^Bump (?<dep>.+?) from (?<from>\\S+) to (?<to>\\S+?)(?: in (?<path>\\S+))?$");

	/** The release tag immediately preceding {@code version} (prereleases excluded). */
	public String previousTag(List<String> tags, String version) {
		var prev = ReleaseVersion.highestReleasedBelow(tags, version);
		return prev == null ? null : "juneau-" + prev.version();
	}

	/** Groups Dependabot bump PRs into one {@link ChangelogEntry} per dependency, sorted by dependency. */
	public List<ChangelogEntry> generateChanges(List<PullRequest> prs) {
		// Preserve encounter order within a group, but iterate PRs by ascending number for stable from/to + refs.
		var sorted = cp(prs);
		sorted.sort((a, b) -> Integer.compare(a.number, b.number));

		var groups = new LinkedHashMap<String, ChangelogEntry.Builder>();
		for (var pr : sorted) {
			var m = BUMP.matcher(pr.title == null ? "" : pr.title.strip());
			if (!m.matches())
				continue; // non-bump / human PR -> excluded from ** Changes
			var dep = m.group("dep");
			var path = m.group("path");
			var key = path == null ? dep : dep + " in " + path;
			groups.computeIfAbsent(key, ChangelogEntry.Builder::new).add(m.group("from"), m.group("to"), pr.number);
		}

		var out = new ArrayList<ChangelogEntry>();
		for (var b : groups.values())
			out.add(b.build());
		out.sort((a, b) -> a.dependency.compareToIgnoreCase(b.dependency));
		return out;
	}

	/** Renders the full "** Changes" section text. */
	public String renderChangesSection(List<PullRequest> prs) {
		var sb = new StringBuilder("** Changes\n\n");
		for (var e : generateChanges(prs))
			sb.append(e.toLine()).append('\n');
		return sb.toString();
	}
}
