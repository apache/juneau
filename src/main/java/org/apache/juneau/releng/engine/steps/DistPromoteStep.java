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
import org.apache.juneau.releng.release.ReleaseVersion;
import org.apache.juneau.releng.util.SvnArgs;

/** §5.19 dist-promote: svn move dist/dev -> dist/release/<version>; remove prior release on the line. Mutating. */
public class DistPromoteStep implements ReleaseStep {

	// The six ASF-convention artifact files a fixed binary-artifacts-stage (§5.12) commits to dist/dev.
	private static final List<String> EXTENSIONS = List.of("", ".asc", ".sha512");

	@Override
	public String id() {
		return "dist-promote";
	}

	@Override
	public String title() {
		return "Promote dist/dev -> dist/release";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		var rc = "juneau-" + ctx.run.version + "-RC" + ctx.run.rc;
		var prior = priorReleaseOnLine(ctx);
		var p = new Preview(id(), true).line(
				"svn move dist/dev/juneau/{source,binaries}/" + rc + " -> dist/release/juneau/" + ctx.run.version);
		p.line(prior == null ? "No prior release on this line to remove."
				: "svn rm dist/release/juneau/" + prior + " (mirror hygiene)");
		p.line("svn commit.");
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var rc = "juneau-" + ctx.run.version + "-RC" + ctx.run.rc;
		var version = ctx.run.version;
		var dev = ctx.stateDir.resolve("dist-dev");
		var release = ctx.stateDir.resolve("dist-release");
		var pw = ctx.ldapPassword + "\n";

		var coDev = ctx.dryRunOr(List.of("svn", "checkout", SvnArgs.USERNAME, ctx.availid,
				SvnArgs.PASSWORD_FROM_STDIN, ctx.target.distDevBase(), dev.toString()), pw, null);
		if (!coDev.ok())
			return StepResult.fail("svn checkout of dist/dev failed.");
		var coRelease = ctx.dryRunOr(List.of("svn", "checkout", SvnArgs.USERNAME, ctx.availid,
				SvnArgs.PASSWORD_FROM_STDIN, ctx.target.distReleaseBase(), release.toString()), pw, null);
		if (!coRelease.ok())
			return StepResult.fail("svn checkout of dist/release failed.");

		var destDir = release.resolve(version);
		ctx.dryRunOr(List.of("mkdir", destDir.toString()));
		for (var ext : EXTENSIONS) {
			var name = "apache-juneau-" + version + "-src.zip" + ext;
			ctx.dryRunOr(List.of("svn", "mv", dev.resolve("source").resolve(rc).resolve(name).toString(),
					destDir.resolve(name).toString()));
		}
		for (var ext : EXTENSIONS) {
			var name = "apache-juneau-" + version + "-bin.zip" + ext;
			ctx.dryRunOr(List.of("svn", "mv", dev.resolve("binaries").resolve(rc).resolve(name).toString(),
					destDir.resolve(name).toString()));
		}

		var prior = priorReleaseOnLine(ctx);
		if (prior != null)
			ctx.dryRunOr(List.of("svn", "rm", release.resolve(prior).toString()));

		var commit = ctx.dryRunOr(List.of("svn", "commit", dev.toString(), release.toString(), "-m",
				"Apache Juneau " + version, SvnArgs.USERNAME, ctx.availid, SvnArgs.PASSWORD_FROM_STDIN), pw,
				Map.of());
		return commit.ok() ? StepResult.ok("Promoted to dist/release.") : StepResult.fail("svn promote failed.");
	}

	/** The highest non-prerelease version on {@code ctx.run.version}'s major.minor line, strictly below it. */
	private static String priorReleaseOnLine(StepContext ctx) {
		var tags = ctx.runner.runLines(List.of("git", "-C", ctx.repoDir, "tag", "--list", "juneau-*"));
		var target = ReleaseVersion.of(ctx.run.version);
		ReleaseVersion best = null;
		for (var t : tags) {
			var v = ReleaseVersion.ofTag(t);
			var onSameLine = !v.isPrerelease() && v.major() == target.major() && v.minor() == target.minor();
			if (onSameLine && v.compareTo(target) < 0 && (best == null || v.compareTo(best) > 0))
				best = v;
		}
		return best == null ? null : best.version();
	}
}
