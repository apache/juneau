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

package org.apache.juneau.releng.rest;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.milestone.GithubPrSource;
import org.apache.juneau.releng.milestone.MilestoneService;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;

/** §8.1: the New-Release page's client-side milestone pre-fill endpoint. */
class MilestoneRestTest {

	private ProcessRunner runnerReturning(String json) {
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
	void resolveReturnsMatchedMilestoneNumber() {
		var runner = runnerReturning("[{\"number\":13,\"title\":\"9.2.1\"}]");
		var rest = new MilestoneRest(new MilestoneService(), new GithubPrSource(runner, "apache/juneau"), runner,
				"/repo");
		var out = rest.resolve("9.2.1");
		assertEquals(13, out.milestoneNumber);
	}

	@Test
	void resolveReturnsNullWhenNoMatch() {
		var runner = runnerReturning("[]");
		var rest = new MilestoneRest(new MilestoneService(), new GithubPrSource(runner, "apache/juneau"), runner,
				"/repo");
		var out = rest.resolve("10.0.0");
		assertNull(out.milestoneNumber);
	}
}
