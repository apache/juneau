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

import org.junit.jupiter.api.Test;

class ReleaseTest {

	@Test
	void a01_defaultsAndRowId() {
		var r = new Release("9.2.1", "RELEASED", "tag");
		assertEquals("9.2.1|—|RELEASED", r.id);
		assertEquals("—", r.stage);
		assertEquals("—", r.rc);
		assertEquals("—", r.voteCloses);
		assertEquals("—", r.released);
		r.rc = "RC2";
		assertEquals("9.2.1|RC2|RELEASED", r.rowId());
	}

	@Test
	void a02_noArgConstructorRowIdUsesPlaceholders() {
		assertEquals("—|—|—", new Release().rowId());
	}

	@Test
	void b01_urlsForARealVersion() {
		var r = new Release("9.2.1", "RELEASED", "tag");
		assertTrue(r.jiraVersionUrl().startsWith("https://issues.apache.org/jira/issues/?jql="));
		assertTrue(r.jiraVersionUrl().contains("9.2.1"));
		assertEquals(Release.DIST_RELEASE_PREFIX + "9.2.1/", r.distUrl());
		assertEquals("https://github.com/apache/juneau/releases/tag/juneau-9.2.1", r.githubTagUrl());
	}

	@Test
	void b02_distUrlOnlyForReleased() {
		assertNull(new Release("9.2.1", "DRAFT", "state").distUrl());
	}

	@Test
	void b03_blankVersionYieldsNoUrls() {
		for (var v : new String[] { null, "", "  " }) {
			var r = new Release(v, "RELEASED", "tag");
			assertNull(r.jiraVersionUrl());
			assertNull(r.distUrl());
			assertNull(r.githubTagUrl());
		}
	}
}
