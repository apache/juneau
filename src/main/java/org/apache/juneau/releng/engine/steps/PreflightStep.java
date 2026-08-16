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
import org.apache.juneau.releng.engine.BranchResolver;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.1 preflight: verify toolchain + target branch exists (never creates). Read-only; preview == apply. */
public class PreflightStep implements ReleaseStep {

	private final BranchResolver branches;

	public PreflightStep(BranchResolver branches) {
		this.branches = branches;
	}

	@Override
	public String id() {
		return "preflight";
	}

	@Override
	public String title() {
		return "Preflight checks";
	}

	@Override
	public Preview preview(StepContext ctx) {
		var p = new Preview(id(), false);
		for (var tool : List.of("wget", "gpg", "svn", "git", "java", "mvn")) {
			var res = ctx.runner.run(List.of("which", tool), null, null);
			p.line((res.ok() ? "[ok] " : "[MISSING] ") + tool);
		}
		var branch = branches.resolve(ctx.run.version);
		p.line("Target version: " + ctx.run.version + " RC" + ctx.run.rc);
		p.line("Resolved branch: " + branch);
		p.line("Branch exists on origin: " + branches.remoteBranchExists(branch));
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var branch = branches.resolve(ctx.run.version);
		ctx.run.branch = branch;
		ctx.log.accept("Resolved target branch: " + branch);
		if (!branches.remoteBranchExists(branch)) {
			var msg = branches.missingBranchInstructions(branch);
			ctx.log.accept(msg);
			return StepResult.fail(msg);
		}
		return StepResult.ok("Preflight passed; branch " + branch + " exists.");
	}
}
