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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.juneau.releng.run.RunFile;
import org.apache.juneau.releng.run.RunFiles;

/**
 * Produces in-progress rows from {@code release.py}'s run files (via {@link RunFiles}), mapped into this package's
 * {@link Release} view shape.
 *
 * <p>The run files and the Releases-tab rows are different shapes; this class is the one place that translates
 * between them.
 */
public class LocalStateReleaseSource {

	private final RunFiles files;

	/**
	 * Creates a source backed by {@code files}.
	 *
	 * @param files The run files reader.
	 */
	public LocalStateReleaseSource(RunFiles files) {
		this.files = files;
	}

	/**
	 * In-progress rows, one per run file that carries a version.
	 *
	 * @return The rows.
	 */
	public List<Release> list() {
		return files.all().stream().filter(r -> r.version != null).map(LocalStateReleaseSource::toRelease)
			.collect(Collectors.toCollection(ArrayList::new));
	}

	private static Release toRelease(RunFile rf) {
		var r = new Release(rf.version, statusOf(rf.phase), "state");
		r.rc = "RC" + rf.rc;
		r.stage = stageOf(rf);
		r.voteCloses = rf.voteDeadline == null ? "—" : rf.voteDeadline;
		return r;
	}

	/** Coarse Releases-tab status: {@code vote} is VOTING, {@code released} is RELEASED, anything else is DRAFT. */
	private static String statusOf(String phase) {
		return switch (phase == null ? "" : phase) {
			case "vote" -> "VOTING";
			case "released" -> "RELEASED";
			default -> "DRAFT";
		};
	}

	/** Human-readable phase. */
	private static String stageOf(RunFile rf) {
		return switch (rf.phase == null ? "" : rf.phase) {
			case "vote" -> "Voting (closes " + (rf.voteDeadline == null ? "—" : rf.voteDeadline) + ")";
			case "vote-failed" -> "Vote failed";
			case "promote" -> "Promoting";
			case "released" -> "Released";
			default -> "Building RC" + rf.rc;
		};
	}
}
