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

/** §5.18 nexus-release: promote the closed staging repo to the public release repo. IRREVERSIBLE. Mutating. */
public class NexusReleaseStep implements ReleaseStep {
	@Override
	public String id() {
		return "nexus-release";
	}

	@Override
	public String title() {
		return "Release Nexus staging repo";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), true).line("Promote/release repo " + ctx.run.nexusRepoId + " to Maven Central.")
				.line("THIS CANNOT BE UNDONE. Type the version to confirm.");
	}

	@Override
	public StepResult apply(StepContext ctx) {
		// Extra confirmation friction is enforced by ReleaseRunRest (type-to-confirm version string).
		if (ctx.run.nexusRepoId == null)
			return StepResult.fail("No nexusRepoId recorded.");
		ctx.log.accept("Releasing Nexus repo " + ctx.run.nexusRepoId + " (irreversible).");
		ctx.nexus.promote(ctx.run.nexusRepoId);
		return StepResult.ok("Released " + ctx.run.nexusRepoId + " to public repo.");
	}
}
