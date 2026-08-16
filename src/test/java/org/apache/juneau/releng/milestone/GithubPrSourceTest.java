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

package org.apache.juneau.releng.milestone;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

/** §8.1: resolves the milestone number by title-matching the version, for the New-Release form pre-fill. */
class GithubPrSourceTest {

	private ProcessRunner runnerReturning(String json) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				assertTrue(c.contains("gh"));
				assertTrue(c.contains("api"));
				assertTrue(c.stream().anyMatch(a -> a.contains("milestones?state=all")));
				return json;
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e) {
				return new ProcResult(0, "");
			}
		};
	}

	@Test
	void resolvesExactTitleMatch() {
		var json = "[{\"number\":12,\"title\":\"9.2.0\"},{\"number\":13,\"title\":\"9.2.1\"}]";
		var src = new GithubPrSource(runnerReturning(json), "apache/juneau");
		assertEquals(13, src.resolveMilestoneNumber("9.2.1"));
	}

	@Test
	void noMatchReturnsNull() {
		var json = "[{\"number\":12,\"title\":\"9.2.0\"}]";
		var src = new GithubPrSource(runnerReturning(json), "apache/juneau");
		assertNull(src.resolveMilestoneNumber("10.0.0"));
	}

	@Test
	void blankResponseReturnsNull() {
		var src = new GithubPrSource(runnerReturning(""), "apache/juneau");
		assertNull(src.resolveMilestoneNumber("9.2.1"));
	}
}
