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

package org.apache.juneau.releng.release;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

class GitTagReleaseSourceTest {

	private ProcessRunner stub(List<String> lines) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> command) {
				return lines;
			}

			@Override
			public String runText(List<String> command) {
				return String.join("\n", lines);
			}

			@Override
			public ProcResult run(List<String> command, String stdin, Map<String, String> env) {
				return new ProcResult(0, "");
			}
		};
	}

	@Test
	void a01_listsReleasedVersionsExcludingRcs() {
		var runner = stub(
				List.of("juneau-9.2.0", "juneau-9.2.0-RC3", "juneau-9.1.0", "juneau-9.0.1", "juneau-8.2.0-RC1"));
		var src = new GitTagReleaseSource(runner, "/repo");
		var releases = src.list();
		var versions = releases.stream().map(r -> r.version).toList();
		assertEquals(List.of("9.2.0", "9.1.0", "9.0.1"), versions);
		assertTrue(releases.stream().allMatch(r -> "RELEASED".equals(r.status)));
		assertTrue(releases.stream().allMatch(r -> "tag".equals(r.source)));
	}
}
