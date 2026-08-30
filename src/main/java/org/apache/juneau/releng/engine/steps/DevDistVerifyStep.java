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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;
import org.apache.juneau.releng.util.SvnArgs;

/** §5.13 dev-dist-verify: confirm the 6 expected files present + non-empty; open dist URL for spot-check. */
public class DevDistVerifyStep implements ReleaseStep {

	private static final List<String> KINDS = List.of("src", "bin");
	private static final List<String> EXTENSIONS = List.of("", ".asc", ".sha512");

	@Override
	public String id() {
		return "dev-dist-verify";
	}

	@Override
	public String title() {
		return "Verify dist/dev";
	}

	@Override
	public boolean reviewGate() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), false).line("Check src/bin × {.zip,.zip.asc,.zip.sha512} exist + non-empty.");
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var missing = missingOrEmptyFiles(ctx);
		if (!missing.isEmpty()) {
			// SAFE fidelity note (§8): binary-artifacts-stage's staging was command-logged in SAFE, so these
			// files were never really produced — soft-note rather than fail the rehearsal (OQ-E). In LIVE the
			// files are expected to exist for real, so their absence is a genuine failure.
			if (!ctx.live()) {
				ctx.log.accept("Note (SAFE: artifacts not staged) — missing/empty: " + missing);
			} else {
				return StepResult.fail("Missing or empty dist/dev file(s): " + missing);
			}
		}

		var url = ctx.target.distDevBase() + "/";
		if (ctx.live())
			ctx.exec(List.of("open", url));
		else
			ctx.log.accept("SAFE: staging was command-logged, so no browser is opened for " + url);
		return StepResult.ok("Dist files present — spot-check the opened URL, then confirm.");
	}

	/** Refreshes the local dist/dev working copy, then checks the 6 ASF-convention files for this RC. */
	private List<String> missingOrEmptyFiles(StepContext ctx) {
		var rc = "juneau-" + ctx.run.version + "-RC" + ctx.run.rc;
		var dist = ctx.stateDir.resolve("dist");
		ctx.dryRunOr(List.of("svn", "checkout", SvnArgs.USERNAME, ctx.availid, SvnArgs.PASSWORD_FROM_STDIN,
				ctx.target.distDevBase(), dist.toString()), ctx.ldapPassword + "\n", null);

		var missing = new ArrayList<String>();
		for (var kind : KINDS) {
			var subdir = "src".equals(kind) ? "source" : "binaries";
			for (var ext : EXTENSIONS) {
				var name = "apache-juneau-" + ctx.run.version + "-" + kind + ".zip" + ext;
				var file = dist.resolve(subdir).resolve(rc).resolve(name);
				if (!Files.isRegularFile(file) || fileSize(file) == 0)
					missing.add(name);
			}
		}
		return missing;
	}

	private static long fileSize(Path file) {
		try {
			return Files.size(file);
		} catch (IOException e) {
			return 0;
		}
	}
}
