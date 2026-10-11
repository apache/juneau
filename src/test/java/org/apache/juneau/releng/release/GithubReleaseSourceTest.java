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

class GithubReleaseSourceTest {

	private static ProcessRunner stub(String text, java.util.List<String> sawCommand) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> command) {
				return List.of();
			}

			@Override
			public String runText(List<String> command) {
				sawCommand.addAll(command);
				return text;
			}

			@Override
			public ProcResult run(List<String> command, String stdin, Map<String, String> env) {
				return new ProcResult(0, "");
			}
		};
	}

	@Test
	void a01_blankOutputYieldsNoReleases() {
		assertTrue(new GithubReleaseSource(stub("  \n", new java.util.ArrayList<>()), "apache/juneau").list().isEmpty());
	}

	@Test
	void a02_mapsPublishedAndDraftReleases() {
		var cmd = new java.util.ArrayList<String>();
		var json = "[{\"tag_name\":\"juneau-9.2.0\",\"draft\":false,\"html_url\":\"https://x/9.2.0\","
			+ "\"published_at\":\"2026-08-01T12:34:56Z\"},"
			+ "{\"tag_name\":\"juneau-9.3.0\",\"draft\":true},"
			+ "{\"tag_name\":\"juneau-9.4.0\",\"published_at\":\"2026\"}]";
		var rows = new GithubReleaseSource(stub(json, cmd), "apache/juneau").list();
		assertTrue(cmd.contains("repos/apache/juneau/releases"), cmd.toString());
		assertEquals(3, rows.size());

		var a = rows.get(0);
		assertEquals("9.2.0", a.version);
		assertEquals("RELEASED", a.status);
		assertEquals("github", a.source);
		assertEquals("Distributed", a.stage);
		assertEquals("https://x/9.2.0", a.githubReleaseUrl);
		assertEquals("2026-08-01", a.released);

		var b = rows.get(1);
		assertEquals("DRAFT", b.status);
		assertNull(b.githubReleaseUrl);
		assertEquals("—", b.released);

		assertEquals("2026", rows.get(2).released);
	}

	@Test
	void a03_missingTagNameIsToleratedAsEmptyTag() {
		// ReleaseVersion.of("") cannot parse a numeric core; the failure must surface, not be swallowed.
		var src = new GithubReleaseSource(stub("[{\"draft\":false}]", new java.util.ArrayList<>()), "a/b");
		assertThrows(NumberFormatException.class, src::list);
	}
}
