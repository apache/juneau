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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** One row in the Releases tab. */
public class Release {

	/** Apache Juneau's issue tracker (same URL as {@code apache/juneau} {@code issueManagement}). */
	public static final String JIRA_PROJECT_URL = "https://issues.apache.org/jira/browse/JUNEAU";

	/** Dist-release prefix used by {@code EmailService} announcement drafts. */
	public static final String DIST_RELEASE_PREFIX = "https://dist.apache.org/repos/dist/release/juneau/";

	/** Release-notes landing used by {@code EmailService} announcement drafts. */
	public static final String RELEASE_NOTES_URL = "https://juneau.apache.org/#release-notes";

	public String id; // version|rc|status — Detail View expand key
	public String version; // "9.2.1"
	public String stage; // "Awaiting vote" | "Distributed" | "Cancelled"
	public String rc; // "RC1" or "—"
	public String status; // "VOTING" | "RELEASED" | "DROPPED" | "DRAFT"
	public String voteCloses; // ISO datetime or "—"
	public String released; // date or "—"
	public String milestoneUrl; // may be null
	public String githubReleaseUrl; // may be null
	public String source; // "github" | "tag" | "state"

	public Release() {
	}

	public Release(String version, String status, String source) {
		this.version = version;
		this.status = status;
		this.source = source;
		this.stage = "—";
		this.rc = "—";
		this.voteCloses = "—";
		this.released = "—";
		this.id = rowId();
	}

	/**
	 * Stable Detail View row id. Version is not unique (a DROPPED RC can sit beside a later RELEASED row).
	 *
	 * @return {@code version|rc|status}.
	 */
	public String rowId() {
		return nz(version) + "|" + nz(rc) + "|" + nz(status);
	}

	/**
	 * Jira issues whose {@code fixVersion} is this row's {@link #version}, in the JUNEAU project.
	 *
	 * <p>
	 * Built from {@link #JIRA_PROJECT_URL}'s project key plus {@link #version} — there is no stored Jira id
	 * on this bean. Returns {@code null} when version is blank.
	 *
	 * @return A Jira issue-search URL, or {@code null}.
	 */
	public String jiraVersionUrl() {
		if (version == null || version.isBlank())
			return null;
		var jql = "project = JUNEAU AND fixVersion = \"" + version + "\"";
		return "https://issues.apache.org/jira/issues/?jql=" + URLEncoder.encode(jql, StandardCharsets.UTF_8);
	}

	/**
	 * Apache dist/release directory for this version, matching {@code EmailService} announcement copy.
	 *
	 * @return The dist URL when this row is {@code RELEASED}, otherwise {@code null}.
	 */
	public String distUrl() {
		if (!"RELEASED".equals(status) || version == null || version.isBlank())
			return null;
		return DIST_RELEASE_PREFIX + version + "/";
	}

	/**
	 * GitHub tag URL using {@code GitTagReleaseSource}'s {@code juneau-<version>} tag spelling.
	 *
	 * @return The tag URL, or {@code null} when version is blank.
	 */
	public String githubTagUrl() {
		if (version == null || version.isBlank())
			return null;
		return "https://github.com/apache/juneau/releases/tag/juneau-" + version;
	}

	private static String nz(String s) {
		return s == null || s.isBlank() ? "—" : s;
	}
}
