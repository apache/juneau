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

import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.11 nexus-staging-close: discover the just-created staging repo + close it. Mutating. */
public class NexusStagingCloseStep implements ReleaseStep {
	@Override
	public String id() {
		return "nexus-staging-close";
	}

	@Override
	public String title() {
		return "Close Nexus staging repo";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	private String resolveRepoId(StepContext ctx) {
		var override = ctx.formInputs.get("repoIdOverride");
		if (override != null && !override.isBlank())
			return override;
		return ctx.nexus.findLatestRepo().map(r -> r.id).orElse(null);
	}

	@Override
	public Preview preview(StepContext ctx) {
		var p = new Preview(id(), true);
		p.overrideField = "repoIdOverride";
		var id = resolveRepoId(ctx);
		p.line("Discovered staging repo: " + (id == null ? "(none found — supply override)" : id));
		p.line("Will POST close for this repo id.");
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var id = resolveRepoId(ctx);
		if (id == null)
			return StepResult.fail("No staging repo discovered; supply a manual repoIdOverride.");
		ctx.run.nexusRepoId = id;
		ctx.log.accept("Closing Nexus staging repo " + id);
		ctx.nexus.close(id);
		return StepResult.ok("Closed staging repo " + id + ".");
	}
}
