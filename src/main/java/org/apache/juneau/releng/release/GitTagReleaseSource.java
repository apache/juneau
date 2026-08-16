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
import java.util.List;
import org.apache.juneau.releng.util.ProcessRunner;

/** Produces historical RELEASED rows from {@code git tag juneau-*} (prereleases excluded). */
public class GitTagReleaseSource {

	private final ProcessRunner runner;
	private final String repoDir;

	public GitTagReleaseSource(ProcessRunner runner, String repoDir) {
		this.runner = runner;
		this.repoDir = repoDir;
	}

	public List<Release> list() {
		var tags = runner.runLines(List.of("git", "-C", repoDir, "tag", "--list", "juneau-*"));
		var out = new ArrayList<Release>();
		for (var tag : tags) {
			var v = ReleaseVersion.ofTag(tag);
			if (v.isPrerelease())
				continue;
			var r = new Release(v.version(), "RELEASED", "tag");
			r.stage = "Distributed";
			out.add(r);
		}
		out.sort(Comparator.comparing((Release r) -> ReleaseVersion.of(r.version)).reversed());
		return out;
	}
}
