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

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.email.EmailService;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** §5.14/§8.3: SHA-512 is computed from the {@code .sha512} files §5.12 produces, never pasted from a form. */
class ComposeVoteEmailStepTest {

	private ProcessRunner okRunner() {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return "";
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e) {
				if (c.contains("rev-parse"))
					return new ProcResult(0, "abc1234\n");
				return new ProcResult(0, "");
			}
		};
	}

	private StepContext ctx(Path stateDir) {
		var c = new StepContext();
		c.run = RunState.create("9.2.1", "b", List.of("compose-vote-email"));
		c.runner = okRunner();
		c.stagingRepo = Path.of("/staging/git/juneau");
		c.stateDir = stateDir;
		c.email = new EmailService(stateDir, okRunner());
		c.formInputs = Map.of("srcSha512", "PASTED-BY-HUMAN-SHOULD-BE-IGNORED");
		c.log = s -> {
		};
		return c;
	}

	@Test
	void a01_readsShaFromDistWorkingCopyNotFormInput(@TempDir Path dir) throws Exception {
		var rc = "juneau-9.2.1-RC1";
		var srcDir = dir.resolve("dist/source/" + rc);
		var binDir = dir.resolve("dist/binaries/" + rc);
		Files.createDirectories(srcDir);
		Files.createDirectories(binDir);
		Files.writeString(srcDir.resolve("apache-juneau-9.2.1-src.zip.sha512"), "SRC-SHA-512-VALUE\n");
		Files.writeString(binDir.resolve("apache-juneau-9.2.1-bin.zip.sha512"), "BIN-SHA-512-VALUE\n");

		var c = ctx(dir);
		var res = new ComposeVoteEmailStep().apply(c);
		assertTrue(res.success, res.message);

		var eml = Files.readString(dir.resolve("drafts/9.2.1-RC1-vote.eml"));
		assertTrue(eml.contains("SRC-SHA-512-VALUE"));
		assertTrue(eml.contains("BIN-SHA-512-VALUE"));
		assertFalse(eml.contains("PASTED-BY-HUMAN-SHOULD-BE-IGNORED"), "must never read the sha from a form input");
	}

	@Test
	void a02_absentShaFilesSoftNoteRatherThanFail(@TempDir Path dir) {
		var c = ctx(dir); // no dist/ working copy at all (e.g. under a SAFE rehearsal)
		var res = new ComposeVoteEmailStep().apply(c);
		assertTrue(res.success, "absent checksums must not hard-fail the draft");
	}
}
