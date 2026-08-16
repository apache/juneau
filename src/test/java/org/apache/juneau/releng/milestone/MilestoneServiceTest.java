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
import org.junit.jupiter.api.Test;

class MilestoneServiceTest {

	private final MilestoneService svc = new MilestoneService();

	@Test
	void previousTagIsHighestReleasedBelowVersion() {
		var tags = List.of("juneau-9.2.0", "juneau-9.1.0", "juneau-9.0.1", "juneau-9.2.0-RC3");
		assertEquals("juneau-9.2.0", svc.previousTag(tags, "9.2.1"));
	}

	@Test
	void groupsDependabotBumpsForSameDependency() {
		var prs = List.of(new PullRequest(308, "Bump spring.version from 4.0.1 to 4.0.3", "dependabot[bot]"),
				new PullRequest(316, "Bump spring.version from 4.0.3 to 4.0.6", "dependabot[bot]"),
				new PullRequest(306, "Bump org.apache:apache from 35 to 37", "dependabot[bot]"),
				new PullRequest(999, "Add CBOR support", "jamesbognar"));

		var entries = svc.generateChanges(prs);

		assertEquals(2, entries.size(), "human PR excluded; spring bumps collapse to one");
		var spring = entries.stream().filter(e -> e.dependency.equals("spring.version")).findFirst().orElseThrow();
		assertEquals("4.0.1", spring.fromVersion);
		assertEquals("4.0.6", spring.toVersion);
		assertEquals(List.of(308, 316), spring.prNumbers);
		assertEquals("    * Bump spring.version from 4.0.1 to 4.0.6 #308, #316.", spring.toLine());
	}

	@Test
	void preservesInPathQualifier() {
		var prs = List
				.of(new PullRequest(282, "Bump js-yaml from 3.14.1 to 3.14.2 in /juneau-docs", "dependabot[bot]"));
		var entries = svc.generateChanges(prs);
		assertEquals("js-yaml in /juneau-docs", entries.get(0).dependency);
		assertEquals("    * Bump js-yaml in /juneau-docs from 3.14.1 to 3.14.2 #282.", entries.get(0).toLine());
	}

	@Test
	void rendersChangesSectionSortedByDependency() {
		var prs = List.of(new PullRequest(316, "Bump spring.version from 4.0.3 to 4.0.6", "dependabot[bot]"),
				new PullRequest(306, "Bump org.apache:apache from 35 to 37", "dependabot[bot]"));
		var section = svc.renderChangesSection(prs);
		var expected = """
				** Changes

				    * Bump org.apache:apache from 35 to 37 #306.
				    * Bump spring.version from 4.0.3 to 4.0.6 #316.
				""";
		assertEquals(expected, section);
	}
}
