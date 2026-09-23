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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** §5.13/§8.2: real existence/size check of the six ASF-convention dist/dev files. */
class DevDistVerifyStepTest {

	private final List<List<String>> calls = new ArrayList<>();
	private final List<String> logLines = new ArrayList<>();

	private StepContext ctx(Path stateDir) {
		var c = new StepContext();
		c.run = RunState.create("9.2.1", "b", List.of("dev-dist-verify"));
		c.target = TargetProfile.prodDefault();
		c.stateDir = stateDir;
		c.availid = "jdoe";
		c.ldapPassword = "s3cr3t";
		c.formInputs = Map.of();
		c.log = logLines::add;
		c.runner = new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> x) {
				return List.of();
			}

			@Override
			public String runText(List<String> x) {
				return "";
			}

			@Override
			public ProcResult run(List<String> x, String s, Map<String, String> e) {
				calls.add(x);
				return new ProcResult(0, "");
			}

			@Override
			public ProcResult run(List<String> x, String s, Map<String, String> e,
					java.util.function.Consumer<String> k) {
				return run(x, s, e);
			}
		};
		return c;
	}

	private void seedAllSixFiles(Path stateDir) throws Exception {
		var rc = "juneau-9.2.1-RC1";
		for (var kindDir : List.of("source", "binaries")) {
			var dir = stateDir.resolve("dist").resolve(kindDir).resolve(rc);
			Files.createDirectories(dir);
			var kind = "source".equals(kindDir) ? "src" : "bin";
			for (var ext : List.of("", ".asc", ".sha512"))
				Files.writeString(dir.resolve("apache-juneau-9.2.1-" + kind + ".zip" + ext), "content");
		}
	}

	@Test
	void a01_liveWithAllSixFilesPresentSucceedsWithoutOpeningWhenHeadless(@TempDir Path dir) throws Exception {
		seedAllSixFiles(dir);
		var res = new DevDistVerifyStep().apply(ctx(dir));
		assertTrue(res.success, res.message);
	}

	@Test
	void a02_liveWithMissingFileFails(@TempDir Path dir) throws Exception {
		seedAllSixFiles(dir);
		Files.delete(dir.resolve("dist/source/juneau-9.2.1-RC1/apache-juneau-9.2.1-src.zip.sha512"));
		var res = new DevDistVerifyStep().apply(ctx(dir));
		assertFalse(res.success, "a missing dist file must hard-fail");
		assertTrue(res.message.contains("src.zip.sha512"));
	}

	@Test
	void a03_liveWithEmptyFileFails(@TempDir Path dir) throws Exception {
		seedAllSixFiles(dir);
		Files.writeString(dir.resolve("dist/binaries/juneau-9.2.1-RC1/apache-juneau-9.2.1-bin.zip"), "");
		var res = new DevDistVerifyStep().apply(ctx(dir));
		assertFalse(res.success, "a zero-size dist file must hard-fail");
	}

}
