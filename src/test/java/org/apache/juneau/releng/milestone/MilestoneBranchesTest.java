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

/**
 * Remaining branches of {@link GithubPrSource#forMilestone} and {@link MilestoneService#generateChanges}.
 */
class MilestoneBranchesTest {

	private static ProcessRunner returning(String json) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return json;
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String, String> e) {
				return new ProcResult(0, "");
			}
		};
	}

	@Test
	void a01_blankPrListIsEmpty() {
		assertTrue(new GithubPrSource(returning(" "), "apache/juneau").forMilestone("9.2.1").isEmpty());
	}

	@Test
	void a02_prListParsedWithAndWithoutAuthor() {
		var json = "[{\"number\":5,\"title\":\"Bump a from 1 to 2\",\"author\":{\"login\":\"dependabot\"}},{\"number\":6}]";
		var prs = new GithubPrSource(returning(json), "apache/juneau").forMilestone("9.2.1");
		assertEquals(2, prs.size());
		assertEquals("dependabot", prs.get(0).authorLogin);
		assertEquals("", prs.get(1).authorLogin);
		assertEquals("", prs.get(1).title);
	}

	@Test
	void a03_prWithNullTitleIsNotABump() {
		var pr = new PullRequest();
		pr.number = 1;
		pr.title = null;
		assertTrue(new MilestoneService().generateChanges(List.of(pr)).isEmpty());
	}
}
