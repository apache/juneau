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

package org.apache.juneau.releng.engine.steps;

import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.20 github-release-create: gh release create (no binary assets); GH_TOKEN via env. Mutating; retry-safe. */
public class GithubReleaseCreateStep implements ReleaseStep {
	@Override
	public String id() {
		return "github-release-create";
	}

	@Override
	public String title() {
		return "Create GitHub Release";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), true).line(
				"gh release create juneau-" + ctx.run.version + "-RC" + ctx.run.rc + " --repo " + ctx.target.ghSlug()
						+ " --title " + ctx.run.version + " (notes from release-notes + PRs + dist)");
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var tag = "juneau-" + ctx.run.version + "-RC" + ctx.run.rc;
		var env = Map.of("GH_TOKEN", ctx.githubToken);
		// Idempotency: treat "already exists" as success. Only probed LIVE; in SAFE the create is
		// command-logged unconditionally (no gh call is made to check for a real, pre-existing release).
		if (ctx.live()) {
			var exists = ctx.runner.run(List.of("gh", "release", "view", tag, "--repo", ctx.target.ghSlug()), null,
					env);
			if (exists.ok()) {
				ctx.log.accept("GitHub Release already exists for " + tag);
				return StepResult.ok("Already exists.");
			}
		}
		var notes = ctx.formInputs.getOrDefault("releaseNotes", "See release notes.");
		var res = ctx.dryRunOr(List.of("gh", "release", "create", tag, "--repo", ctx.target.ghSlug(), "--title",
				ctx.run.version, "--notes", notes), null, env);
		return res.ok() ? StepResult.ok("GitHub Release created.") : StepResult.fail("gh release create failed.");
	}
}
