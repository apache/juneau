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
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.email.EmailTemplate;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.14 compose-vote-email: checksums + commit + staging link + 72h deadline, then draft-and-open. */
public class ComposeVoteEmailStep implements ReleaseStep {
	@Override
	public String id() {
		return "compose-vote-email";
	}

	@Override
	public String title() {
		return "Compose [VOTE] email";
	}

	private Map<String, String> gather(StepContext ctx) {
		var tag = "juneau-" + ctx.run.version + "-RC" + ctx.run.rc;
		var commit = ctx.runner.run(List.of("git", "-C", ctx.stagingRepo.toString(), "rev-parse", tag), null, null);
		// srcSha512/binSha512 are computed from the .sha512 files binary-artifacts-stage (§5.12) actually
		// produced in the local dist/dev working copy — never pasted from a form.
		return Map.of("commitHash", commit.ok() ? commit.output().strip() : "(unknown)", "srcSha512",
				readSha512(ctx, "source", "src"), "binSha512", readSha512(ctx, "binaries", "bin"));
	}

	/** Reads the {@code .sha512} file §5.12 wrote for this RC; blank (SAFE soft-note) if absent. */
	private String readSha512(StepContext ctx, String subdir, String kind) {
		var rc = "juneau-" + ctx.run.version + "-RC" + ctx.run.rc;
		var name = "apache-juneau-" + ctx.run.version + "-" + kind + ".zip.sha512";
		var file = ctx.stateDir.resolve("dist").resolve(subdir).resolve(rc).resolve(name);
		try {
			return Files.isRegularFile(file) ? Files.readString(file).strip() : "";
		} catch (IOException e) {
			return "";
		}
	}

	@Override
	public Preview preview(StepContext ctx) {
		var p = new Preview(id(), false);
		for (var line : ctx.email.renderBody(EmailTemplate.VOTE, ctx.run, gather(ctx)).split("\n"))
			p.line(line);
		return p;
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var data = gather(ctx);
		// OQ-E: under SAFE the RC tag + dist checksums were never really produced, so fill-ins may be blank.
		// Note it and continue rather than hard-failing — the draft is still useful for a rehearsal.
		if ("(unknown)".equals(data.get("commitHash")) || data.get("srcSha512").isBlank())
			ctx.log.accept("Note: RC tag/checksums absent (expected under SAFE) — composing draft with placeholders.");
		var path = ctx.email.compose(EmailTemplate.VOTE, ctx.run, data);
		return StepResult.ok("Opened draft: " + path);
	}
}
