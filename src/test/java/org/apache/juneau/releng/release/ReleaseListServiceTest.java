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
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;

class ReleaseListServiceTest {

	private static Release rel(String v, String status, String source) {
		return new Release(v, status, source);
	}

	@Test
	void enrichesTagReleaseWithGithubUrlAndKeepsSingleRow() {
		Supplier<List<Release>> tags = () -> List.of(rel("9.2.0", "RELEASED", "tag"), rel("9.1.0", "RELEASED", "tag"));
		var gh = rel("9.2.0", "RELEASED", "github");
		gh.githubReleaseUrl = "https://x/9.2.0";
		gh.released = "2025-12-30";
		Supplier<List<Release>> github = () -> List.of(gh);
		Supplier<List<Release>> state = List::of;

		var result = new ReleaseListService(tags, github, state).list();

		assertEquals(2, result.size(), "9.2.0 from tag+github must collapse to one row");
		var top = result.get(0);
		assertEquals("9.2.0", top.version);
		assertEquals("https://x/9.2.0", top.githubReleaseUrl);
		assertEquals("2025-12-30", top.released);
	}

	@Test
	void inProgressStateRowsSortAboveReleasedOfSameVersion() {
		Supplier<List<Release>> tags = () -> List.of(rel("9.2.0", "RELEASED", "tag"));
		Supplier<List<Release>> github = List::of;
		var rc = rel("9.2.1", "VOTING", "state");
		rc.rc = "RC1";
		rc.stage = "Awaiting vote";
		Supplier<List<Release>> state = () -> List.of(rc);

		var result = new ReleaseListService(tags, github, state).list();

		assertEquals("9.2.1", result.get(0).version);
		assertEquals("VOTING", result.get(0).status);
		assertEquals("9.2.0", result.get(1).version);
	}
}
