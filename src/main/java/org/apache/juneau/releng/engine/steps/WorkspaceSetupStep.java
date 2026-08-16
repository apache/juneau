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

import java.nio.file.Files;
import java.util.List;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.3 workspace-setup: clean staging clone of the resolved branch (idempotent; re-pulls on re-run). */
public class WorkspaceSetupStep implements ReleaseStep {
	@Override
	public String id() {
		return "workspace-setup";
	}

	@Override
	public String title() {
		return "Workspace setup";
	}

	@Override
	public boolean mutating() {
		return false;
	} // local only, but destructive .m2 cleanup -> apply gate

	@Override
	public Preview preview(StepContext ctx) {
		var p = new Preview(id(), false);
		var git = ctx.stagingRepo;
		p.line("Staging clone: " + git);
		p.line("Clone already present: " + Files.isDirectory(git.resolve(".git")));
		p.line("Resolved branch: " + ctx.run.branch);
		p.line("Will run: git checkout " + ctx.run.branch + " && git pull --ff-only origin " + ctx.run.branch);
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var git = ctx.stagingRepo;
		var gitStr = git.toString();
		if (!Files.isDirectory(git.resolve(".git"))) {
			var clone = ctx.exec(List.of("git", "clone", ctx.target.cloneUrl(), gitStr));
			if (!clone.ok())
				return StepResult.fail("git clone failed");
		} else {
			ctx.log.accept("Staging clone exists; skipping clone (idempotent).");
		}
		if (!ctx.exec(List.of("git", "-C", gitStr, "checkout", ctx.run.branch)).ok())
			return StepResult.fail("git checkout " + ctx.run.branch + " failed");
		if (!ctx.exec(List.of("git", "-C", gitStr, "pull", "--ff-only", "origin", ctx.run.branch)).ok())
			return StepResult.fail("git pull --ff-only failed");
		ctx.exec(List.of("git", "-C", gitStr, "config", "user.name", ctx.availid));
		ctx.exec(List.of("git", "-C", gitStr, "config", "user.email", ctx.committerEmail));
		return StepResult.ok("Staging clone ready on " + ctx.run.branch + " (fast-forwarded).");
	}
}
