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

/** The milestone-close step: close the GitHub Milestone via {@code gh api}. Mutating; idempotent. */
public class MilestoneCloseStep implements ReleaseStep {
	@Override
	public String id() {
		return "milestone-close";
	}

	@Override
	public String title() {
		return "Close milestone";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), true).line("Close milestone #" + ctx.run.milestoneNumber + " for " + ctx.run.version);
	}

	@Override
	public StepResult apply(StepContext ctx) {
		if (ctx.run.milestoneNumber == null)
			return StepResult.ok("No milestone recorded; skipping.");
		var env = Map.of("GH_TOKEN", ctx.githubToken);
		var res = ctx.dryRunOr(
				List.of("gh", "api", "repos/" + ctx.target.repoSlug() + "/milestones/" + ctx.run.milestoneNumber, "-X",
						"PATCH", "-f", "state=closed"),
				null, env);
		return res.ok() ? StepResult.ok("Milestone closed.") : StepResult.fail("Milestone close failed.");
	}
}
