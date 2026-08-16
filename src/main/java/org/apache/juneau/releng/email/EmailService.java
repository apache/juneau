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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.util.ProcessRunner;

/** Composes the four release emails into {@code .eml} drafts and opens them in the mail client. */
public class EmailService {

	private final Path draftsDir; // rm.state.dir/drafts
	private final ProcessRunner runner;

	public EmailService(Path stateDir, ProcessRunner runner) {
		this.draftsDir = stateDir.resolve("drafts");
		this.runner = runner;
	}

	/** Render + write the {@code .eml}, then {@code open} it; returns the draft path. */
	public Path compose(EmailTemplate t, RunState rs) {
		return compose(t, rs, Map.of());
	}

	public Path compose(EmailTemplate t, RunState rs, Map<String, String> extra) {
		var body = renderBody(t, rs, extra);
		var subject = subject(t, rs);
		var eml = "To: " + t.to + "\nSubject: " + subject + "\n\n" + body;
		try {
			Files.createDirectories(draftsDir);
			var name = rs.version + "-RC" + rs.rc + "-" + t.name().toLowerCase() + ".eml";
			var path = draftsDir.resolve(name);
			Files.writeString(path, eml, StandardCharsets.UTF_8);
			runner.run(List.of("open", path.toString()), null, null);
			return path;
		} catch (IOException e) {
			throw isex(e, "Cannot write email draft for %s", t);
		}
	}

	public String subject(EmailTemplate t, RunState rs) {
		return t.subjectPrefix + " Apache Juneau " + rs.version + " RC" + rs.rc;
	}

	/**
	 * The plain-text body per template. {@code extra} carries step-computed values (checksums, tally,
	 * links). The four optional narrative fields are pulled from {@code rs} (falling back to a matching
	 * {@code extra} value for backward-compat, e.g. announcement {@code highlights}); each block is omitted
	 * entirely when its source value is blank, so the terse mechanical output is preserved unchanged.
	 */
	public String renderBody(EmailTemplate t, RunState rs, Map<String, String> extra) {
		var summary = narrative(rs.releaseSummary, extra, "releaseSummary");
		var highlights = narrative(rs.highlights, extra, "highlights");
		var knownIssues = narrative(rs.knownIssues, extra, "knownIssues");
		var acks = narrative(rs.acknowledgements, extra, "acknowledgements");
		return switch (t) {
		case PROPOSE -> paragraphs("I propose to release Apache Juneau %s (RC%d) from branch %s."
				.formatted(rs.version, rs.rc, rs.branch), summary,
				"If there are last-minute blockers, please reply on this thread before the build starts.");
		case VOTE -> paragraphs("Please vote on releasing Apache Juneau %s RC%d.".formatted(rs.version, rs.rc),
				labeled("Highlights:", highlights), labeled("Known issues:", knownIssues), """
						Staging repo:   %s
						Commit:         %s
						Source SHA-512: %s
						Binary SHA-512: %s
						Dist (dev):     https://dist.apache.org/repos/dist/dev/juneau/"""
						.formatted(nz(rs.nexusRepoId), extra.getOrDefault("commitHash", ""),
								extra.getOrDefault("srcSha512", ""), extra.getOrDefault("binSha512", "")),
				"The vote is open for at least 72 hours and closes %s.".formatted(nz(rs.voteDeadline)), """
						[ ] +1  Release this package
						[ ]  0  No opinion
						[ ] -1  Do not release (please provide a reason)""");
		case RESULT -> paragraphs(
				"The vote to release Apache Juneau %s RC%d has %s.".formatted(rs.version, rs.rc,
						extra.getOrDefault("outcome", "")),
				extra.getOrDefault("tally", ""), labeled("Acknowledgements:", acks),
				"Thanks to everyone who voted.");
		case ANNOUNCEMENT -> paragraphs(
				"The Apache Juneau team is pleased to announce the release of Apache Juneau %s."
						.formatted(rs.version),
				summary, highlights, labeled("Known issues:", knownIssues), """
						Downloads:      %s
						GitHub Release: %s
						Release notes:  https://juneau.apache.org/#release-notes"""
						.formatted("https://dist.apache.org/repos/dist/release/juneau/" + rs.version + "/",
								nz(rs.githubReleaseUrl)),
				labeled("Acknowledgements:", acks), "The Apache Juneau Team");
		};
	}

	/** Joins the non-blank sections with a single blank line between each and a trailing newline. */
	private static String paragraphs(String... sections) {
		var kept = new ArrayList<String>();
		for (var s : sections)
			if (s != null && !s.isEmpty())
				kept.add(s);
		return String.join("\n\n", kept) + "\n";
	}

	/** A header line above the content, or null when the content is blank (so the whole block is omitted). */
	private static String labeled(String header, String content) {
		return content == null ? null : header + "\n" + content;
	}

	/**
	 * The narrative value for a field: the RunState value when non-blank, else a matching {@code extra}
	 * value (backward-compat), else null. Trailing whitespace is trimmed so a stray textarea newline never
	 * produces a dangling blank line in the email.
	 */
	private static String narrative(String primary, Map<String, String> extra, String key) {
		if (primary != null && !primary.isBlank())
			return primary.stripTrailing();
		var e = extra == null ? null : extra.get(key);
		return (e != null && !e.isBlank()) ? e.stripTrailing() : null;
	}

	private static String nz(String s) {
		return s == null ? "" : s;
	}
}
