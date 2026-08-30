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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.ExecutionMode;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.StepContext;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

class ReleasePrepareStepTest {

	private StepContext ctx(String version, Map<String, String> form, List<List<String>> calls) {
		var c = new StepContext();
		c.mode = ExecutionMode.LIVE; // this test asserts the built command/argv, which only spawns in LIVE
		c.run = RunState.create(version, "b", List.of("release-prepare"));
		c.stagingRepo = java.nio.file.Path.of("/staging/git/juneau");
		c.formInputs = form;
		c.log = s -> {
		};
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
				calls.add(x);
				return new ProcResult(0, "");
			}
		};
		return c;
	}

	@Test
	void a01_maintenanceReleaseDerivesNextDevVersion() {
		var calls = new ArrayList<List<String>>();
		var res = new ReleasePrepareStep().apply(ctx("9.2.1", Map.of(), calls));
		assertTrue(res.success);
		var cmd = calls.stream().filter(c -> c.contains("release:prepare")).findFirst().orElseThrow();
		assertTrue(cmd.contains("-DdevelopmentVersion=9.2.2-SNAPSHOT"));
		assertTrue(cmd.contains("-Dtag=juneau-9.2.1-RC1"));
	}

	@Test
	void a02_newMinorReleaseRequiresExplicitDevelopmentVersion() {
		var calls = new ArrayList<List<String>>();
		var res = new ReleasePrepareStep().apply(ctx("10.0.0", Map.of(), calls));
		assertFalse(res.success, "z==0 without developmentVersion must fail");
		assertTrue(res.message.toLowerCase().contains("developmentversion"));
	}

	@Test
	void a03_newMinorReleaseUsesSuppliedDevelopmentVersion() {
		var calls = new ArrayList<List<String>>();
		var res = new ReleasePrepareStep().apply(ctx("10.0.0", Map.of("developmentVersion", "10.1.0-SNAPSHOT"), calls));
		assertTrue(res.success);
		var cmd = calls.stream().filter(c -> c.contains("release:prepare")).findFirst().orElseThrow();
		assertTrue(cmd.contains("-DdevelopmentVersion=10.1.0-SNAPSHOT"));
	}
}
