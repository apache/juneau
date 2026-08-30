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

import static org.apache.juneau.commons.utils.Shorts.*;
import java.util.List;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;
import org.apache.juneau.releng.release.ReleaseVersion;

/** §5.8 release-prepare: version bump + RC tag + push. Mutating. z==0 requires explicit developmentVersion (§14.1). */
public class ReleasePrepareStep implements ReleaseStep {
	@Override
	public String id() {
		return "release-prepare";
	}

	@Override
	public String title() {
		return "release:prepare";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	/** Next dev version: derived for maintenance (z>0), human-supplied for z==0 (returns null -> caller fails). */
	static String nextDevVersion(StepContext ctx) {
		var v = ReleaseVersion.of(ctx.run.version);
		if (v.maintenance() > 0)
			return v.major() + "." + v.minor() + "." + (v.maintenance() + 1) + "-SNAPSHOT";
		var supplied = ctx.formInputs.get("developmentVersion");
		if (supplied == null && ctx.run.developmentVersion != null)
			supplied = ctx.run.developmentVersion;
		return ib(supplied) ? null : supplied;
	}

	@Override
	public Preview preview(StepContext ctx) {
		var next = nextDevVersion(ctx);
		var p = new Preview(id(), true);
		p.line("mvn release:prepare -DreleaseVersion=" + ctx.run.version + " -Dtag=juneau-" + ctx.run.version + "-RC"
				+ ctx.run.rc + " -DdevelopmentVersion="
				+ (next == null ? "<REQUIRED — supply developmentVersion>" : next));
		if (next == null)
			p.line("z==0 release: an explicit developmentVersion input is REQUIRED.");
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var next = nextDevVersion(ctx);
		if (next == null)
			return StepResult.fail("z==0 release requires an explicit developmentVersion.");
		ctx.run.developmentVersion = next;
		var res = ctx.dryRunOr(List.of("mvn", "-f", ctx.stagingRepo.toString() + "/pom.xml", "release:prepare",
				"-DautoVersionSubmodules=true", "-DreleaseVersion=" + ctx.run.version,
				"-Dtag=juneau-" + ctx.run.version + "-RC" + ctx.run.rc, "-DdevelopmentVersion=" + next));
		return res.ok() ? StepResult.ok("release:prepare complete; tag pushed.")
				: StepResult.fail("mvn release:prepare failed.");
	}
}
