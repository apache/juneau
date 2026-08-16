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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.Preview;
import org.apache.juneau.releng.engine.ReleaseStep;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.engine.StepResult;

/** §5.12 binary-artifacts-stage: pull signed artifacts, rename to ASF convention, commit to dist/dev SVN. Mutating. */
public class BinaryArtifactsStageStep implements ReleaseStep {
	@Override
	public String id() {
		return "binary-artifacts-stage";
	}

	@Override
	public String title() {
		return "Stage dist/dev artifacts";
	}

	@Override
	public boolean mutating() {
		return true;
	}

	@Override
	public Preview preview(StepContext ctx) {
		return new Preview(id(), true).line("svn checkout " + ctx.target.distDevBase())
				.line("wget signed source + binary artifacts from " + ctx.run.nexusRepoId)
				.line("rename -> apache-juneau-" + ctx.run.version + "-{src,bin}.zip[.asc|.sha512]")
				.line("svn add + commit RC" + ctx.run.rc);
	}

	@Override
	public StepResult apply(StepContext ctx) {
		var rc = "juneau-" + ctx.run.version + "-RC" + ctx.run.rc;
		var dist = ctx.stateDir.resolve("dist");
		var source = dist.resolve("source");
		var binaries = dist.resolve("binaries");
		var sourceRc = source.resolve(rc);
		var binariesRc = binaries.resolve(rc);
		var pw = ctx.ldapPassword + "\n";

		// svn auth: --username <availid> --password-from-stdin (passphrase via stdin, never argv).
		var co = ctx.dryRunOr(List.of("svn", "checkout", "--username", ctx.availid, "--password-from-stdin",
				ctx.target.distDevBase(), dist.toString()), pw, null);
		if (!co.ok())
			return StepResult.fail("svn checkout of dist/dev failed.");

		// Best-effort: clears whatever prior RC directories are present. The "*" here mirrors
		// juneau-release.sh's shell-expanded `svn rm dist/source/*` textually; ProcessRunner has no
		// shell, so it's passed as one literal argument (a no-op against a fresh/empty checkout, which
		// is the common case since ASF dist working copies normally hold at most one prior RC).
		ctx.dryRunOr(List.of("svn", "rm", source + "/*"));
		ctx.dryRunOr(List.of("svn", "rm", binaries + "/*"));
		ctx.dryRunOr(List.of("mkdir", sourceRc.toString()));
		ctx.dryRunOr(List.of("mkdir", binariesRc.toString()));

		var nexusUrl = ctx.target.nexusBaseUrl() + "/content/repositories/" + ctx.run.nexusRepoId
				+ "/org/apache/juneau/";
		ctx.log.accept("Fetching signed source + binary artifacts from " + nexusUrl);
		ctx.dryRunOr(List.of("wget", "-e", "robots=off", "--recursive", "--no-parent", "--no-directories", "-A",
				"*-source-release*", "-P", sourceRc.toString(), nexusUrl));
		ctx.dryRunOr(List.of("wget", "-e", "robots=off", "--recursive", "--no-parent", "--no-directories", "-A",
				"juneau-distrib*-bin.zip*", "-P", binariesRc.toString(), nexusUrl));

		ctx.log.accept("Renaming to apache-juneau-" + ctx.run.version + "-{src,bin}.zip[.asc|.sha512]");
		renameAndChecksum(ctx, sourceRc, "juneau-" + ctx.run.version + "-source-release.zip",
				"apache-juneau-" + ctx.run.version + "-src.zip");
		renameAndChecksum(ctx, binariesRc, "juneau-distrib-" + ctx.run.version + "-bin.zip",
				"apache-juneau-" + ctx.run.version + "-bin.zip");

		ctx.dryRunOr(List.of("svn", "add", sourceRc.toString()));
		ctx.dryRunOr(List.of("svn", "add", binariesRc.toString()));
		var commit = ctx.dryRunOr(
				List.of("svn", "commit", dist.toString(), "-m", rc, "--username", ctx.availid, "--password-from-stdin"),
				pw, Map.of());
		return commit.ok() ? StepResult.ok("Artifacts staged + committed to dist/dev.")
				: StepResult.fail("svn commit to dist/dev failed.");
	}

	/**
	 * Renames the downloaded artifact (+ its {@code .asc}) to the ASF convention, regenerates its SHA-512
	 * checksum via {@code gpg --print-md SHA512}, and clears stray {@code .sha1}/{@code .md5} mirrors that
	 * Nexus may have served alongside it. All mutating calls route through {@code dryRunOr} (Tier B); the
	 * checksum file itself is only written in LIVE, since {@code ProcessRunner} has no shell to honor the
	 * script's {@code > file.sha512} redirection.
	 */
	private void renameAndChecksum(StepContext ctx, Path dir, String downloaded, String renamed) {
		ctx.dryRunOr(List.of("mv", dir.resolve(downloaded).toString(), dir.resolve(renamed).toString()));
		ctx.dryRunOr(
				List.of("mv", dir.resolve(downloaded + ".asc").toString(), dir.resolve(renamed + ".asc").toString()));
		var sha = ctx.dryRunOr(List.of("gpg", "--print-md", "SHA512", dir.resolve(renamed).toString()));
		if (ctx.live() && sha.ok())
			writeShaFile(dir.resolve(renamed + ".sha512"), sha.output());
		ctx.dryRunOr(List.of("rm", dir + "/*.sha1"));
		ctx.dryRunOr(List.of("rm", dir + "/*.md5"));
	}

	private static void writeShaFile(Path path, String content) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, content, StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw isex(e, "Cannot write %s", path);
		}
	}
}
