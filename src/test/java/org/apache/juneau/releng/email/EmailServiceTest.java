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

package org.apache.juneau.releng.email;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.util.ProcessRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EmailServiceTest {

	private ProcessRunner recording(List<List<String>> calls) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return "";
			}

			@Override
			public ProcResult run(List<String> c, String stdin, Map<String, String> env) {
				calls.add(c);
				return new ProcResult(0, "");
			}
		};
	}

	@Test
	void a01_writesEmlAndInvokesOpen(@TempDir Path dir) throws Exception {
		var calls = new ArrayList<List<String>>();
		var svc = new EmailService(dir, recording(calls));
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));

		var path = svc.compose(EmailTemplate.PROPOSE, rs);

		assertTrue(Files.isRegularFile(path));
		var eml = Files.readString(path);
		assertTrue(eml.startsWith("To: dev@juneau.apache.org\n"));
		assertTrue(eml.contains("Subject: [PROPOSE] Apache Juneau 9.2.1 RC1"));
		assertTrue(calls.stream().anyMatch(c -> c.get(0).equals("open") && c.get(1).equals(path.toString())));
	}

	@Test
	void a02_voteEmailIncludesChecksumsAndDeadline(@TempDir Path dir) {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		rs.nexusRepoId = "orgapachejuneau-1042";
		rs.voteDeadline = "2026-08-18T12:00:00Z";
		var body = svc.renderBody(EmailTemplate.VOTE, rs,
				Map.of("srcSha512", "aaa", "binSha512", "bbb", "commitHash", "deadbeef"));
		assertTrue(body.contains("orgapachejuneau-1042"));
		assertTrue(body.contains("2026-08-18"));
		assertTrue(body.contains("deadbeef"));
	}

	@Test
	void b01_proposeBodyMatchesFixtureByteForByte(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		var expected = Files.readString(Path.of("src/test/resources/email/propose.expected.txt"));
		assertEquals(expected, svc.renderBody(EmailTemplate.PROPOSE, rs, Map.of()));
	}

	@Test
	void b02_voteBodyMatchesFixtureByteForByte(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		rs.nexusRepoId = "orgapachejuneau-1042";
		rs.voteDeadline = "2026-08-18T12:00:00Z";
		var expected = Files.readString(Path.of("src/test/resources/email/vote.expected.txt"));
		assertEquals(expected, svc.renderBody(EmailTemplate.VOTE, rs,
				Map.of("srcSha512", "aaa", "binSha512", "bbb", "commitHash", "deadbeef")));
	}

	@Test
	void b03_resultBodyMatchesFixtureByteForByte(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		var expected = Files.readString(Path.of("src/test/resources/email/result.expected.txt"));
		assertEquals(expected, svc.renderBody(EmailTemplate.RESULT, rs,
				Map.of("outcome", "passed", "tally", "Binding +1: 3, Non-binding +1: 2, 0: 0, -1: 0")));
	}

	@Test
	void b04_announcementBodyMatchesFixtureByteForByte(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		rs.githubReleaseUrl = "https://github.com/apache/juneau/releases/tag/9.2.1";
		var expected = Files.readString(Path.of("src/test/resources/email/announcement.expected.txt"));
		assertEquals(expected, svc.renderBody(EmailTemplate.ANNOUNCEMENT, rs,
				Map.of("highlights", "- Bug fixes\n- Performance improvements")));
	}

	// The four optional narrative fields, threaded through the emails per the field->email mapping.
	private static final String SUMMARY = "This patch release fixes a regression in the REST client and refreshes dependencies.";
	private static final String HIGHLIGHTS = "- Fixed a NullPointerException in RestClient\n- Upgraded Jetty to 12.0.x";
	private static final String KNOWN_ISSUES = "- The OpenAPI 3.1 parser does not yet support webhooks.";
	private static final String ACKS = "Thanks to Jane Doe and John Smith for their contributions.";

	private RunState populatedRun() {
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		rs.releaseSummary = SUMMARY;
		rs.highlights = HIGHLIGHTS;
		rs.knownIssues = KNOWN_ISSUES;
		rs.acknowledgements = ACKS;
		return rs;
	}

	@Test
	void c01_proposeBodyWithNarrativeMatchesFixture(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var expected = Files.readString(Path.of("src/test/resources/email/propose.populated.expected.txt"));
		assertEquals(expected, svc.renderBody(EmailTemplate.PROPOSE, populatedRun(), Map.of()));
	}

	@Test
	void c02_voteBodyWithNarrativeMatchesFixture(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = populatedRun();
		rs.nexusRepoId = "orgapachejuneau-1042";
		rs.voteDeadline = "2026-08-18T12:00:00Z";
		var expected = Files.readString(Path.of("src/test/resources/email/vote.populated.expected.txt"));
		assertEquals(expected, svc.renderBody(EmailTemplate.VOTE, rs,
				Map.of("srcSha512", "aaa", "binSha512", "bbb", "commitHash", "deadbeef")));
	}

	@Test
	void c03_resultBodyWithNarrativeMatchesFixture(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var expected = Files.readString(Path.of("src/test/resources/email/result.populated.expected.txt"));
		assertEquals(expected, svc.renderBody(EmailTemplate.RESULT, populatedRun(),
				Map.of("outcome", "passed", "tally", "Binding +1: 3, Non-binding +1: 2, 0: 0, -1: 0")));
	}

	@Test
	void c04_announcementBodyWithNarrativeMatchesFixture(@TempDir Path dir) throws Exception {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = populatedRun();
		rs.githubReleaseUrl = "https://github.com/apache/juneau/releases/tag/9.2.1";
		var expected = Files.readString(Path.of("src/test/resources/email/announcement.populated.expected.txt"));
		// Announcement highlights come from the RunState field here (not extra), exercising the field path.
		assertEquals(expected, svc.renderBody(EmailTemplate.ANNOUNCEMENT, rs, Map.of()));
	}

	@Test
	void d01_blankNarrativeFieldsAreOmittedEntirely(@TempDir Path dir) {
		var svc = new EmailService(dir, recording(new ArrayList<>()));
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		rs.releaseSummary = "   "; // whitespace-only counts as blank
		var body = svc.renderBody(EmailTemplate.ANNOUNCEMENT, rs, Map.of());
		assertFalse(body.contains("Known issues:"));
		assertFalse(body.contains("Acknowledgements:"));
		// No dangling blank block from the blank summary: the intro is immediately followed by the mechanical
		// section with exactly one blank line between them.
		assertTrue(body.contains("Apache Juneau 9.2.1.\n\nDownloads:"), body);
	}
}
