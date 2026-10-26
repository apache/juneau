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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReleaseListServiceTest {

	private static Release rel(String v, String status, String source) {
		return new Release(v, status, source);
	}

	@Test
	void a01_enrichesTagReleaseWithGithubUrlAndKeepsSingleRow() {
		Supplier<List<Release>> tags = () -> List.of(rel("9.2.0", "RELEASED", "tag"), rel("9.1.0", "RELEASED", "tag"));
		var gh = rel("9.2.0", "RELEASED", "github");
		gh.githubReleaseUrl = "https://x/9.2.0";
		gh.released = "2025-12-30";
		Supplier<List<Release>> github = () -> List.of(gh);
		Supplier<List<Release>> state = List::of;

		var result = new ReleaseListService(tags, github, state).list();

		assertSize(() -> "9.2.0 from tag+github must collapse to one row", 2, result);
		var top = result.get(0);
		assertBean(top, "version,githubReleaseUrl,released", "9.2.0,https://x/9.2.0,2025-12-30");
	}

	@Test
	void a02_inProgressStateRowsSortAboveReleasedOfSameVersion() {
		Supplier<List<Release>> tags = () -> List.of(rel("9.2.0", "RELEASED", "tag"));
		Supplier<List<Release>> github = List::of;
		var rc = rel("9.2.1", "VOTING", "state");
		rc.rc = "RC1";
		rc.stage = "Awaiting vote";
		Supplier<List<Release>> state = () -> List.of(rc);

		var result = new ReleaseListService(tags, github, state).list();

		assertBean(result.get(0), "version,status", "9.2.1,VOTING");
		assertEquals("9.2.0", result.get(1).version);
	}

	@Test
	void a03_githubOnlyReleaseIsAdded() {
		var gh = rel("9.3.0", "RELEASED", "github");
		var result = new ReleaseListService(List::of, () -> List.of(gh), List::of).list();
		assertSize(1, result);
		assertEquals("9.3.0|—|RELEASED", result.get(0).id);
	}

	@Test
	void a04_githubMergeKeepsExistingDateWhenGithubHasNone_andCopiesMilestoneUrl() {
		var tag = rel("9.2.0", "RELEASED", "tag");
		tag.released = "2025-01-01";
		var gh = rel("9.2.0", "RELEASED", "github");
		gh.released = "—";
		gh.milestoneUrl = "https://m/1";
		var result = new ReleaseListService(() -> List.of(tag), () -> List.of(gh), List::of).list();
		assertBean(result.get(0), "released,milestoneUrl", "2025-01-01,https://m/1");

		var tag2 = rel("9.2.0", "RELEASED", "tag");
		tag2.milestoneUrl = "https://keep";
		var gh2 = rel("9.2.0", "RELEASED", "github");
		gh2.released = null;
		gh2.milestoneUrl = "https://other";
		var result2 = new ReleaseListService(() -> List.of(tag2), () -> List.of(gh2), List::of).list();
		assertBean(result2.get(0), "released,milestoneUrl", "—,https://keep");
	}

	@Test
	void a05_sameVersionReleasedSortsAfterAttempt() {
		var released = rel("9.2.0", "RELEASED", "tag");
		var dropped = rel("9.2.0", "DROPPED", "state");
		dropped.rc = "RC1";
		var result = new ReleaseListService(() -> List.of(released), List::of, () -> List.of(dropped)).list();
		assertEquals("DROPPED", result.get(0).status);
		assertEquals("RELEASED", result.get(1).status);
		// and with the inputs the other way round, the comparator still orders RELEASED last
		var result2 = new ReleaseListService(List::of, List::of, () -> List.of(released, dropped)).list();
		assertEquals("DROPPED", result2.get(0).status);
	}
}
