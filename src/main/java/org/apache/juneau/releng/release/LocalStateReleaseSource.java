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
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.RunStatus;

/**
 * Produces in-progress rows from the engine's persisted {@link RunState} files (via {@link RunStateStore}), mapped
 * into this package's {@link Release} view shape.
 *
 * <p>The run-state files and the Releases-tab rows are different JSON shapes ({@link RunState} carries
 * {@code branch}, {@code steps}, {@code rcHistory}, etc.; {@link Release} is a flattened table row) — this class
 * is the one place that translates between them, rather than re-parsing a run-state file directly as a
 * {@link Release} (which fails on {@code branch} and any other {@link RunState}-only property).
 */
public class LocalStateReleaseSource {

	private final RunStateStore store;

	public LocalStateReleaseSource(RunStateStore store) {
		this.store = store;
	}

	public List<Release> list() {
		var out = new ArrayList<Release>();
		for (var rs : store.loadAll())
			out.add(toRelease(rs));
		return out;
	}

	private static Release toRelease(RunState rs) {
		var r = new Release(rs.version, statusOf(rs.status), "state");
		r.rc = "RC" + rs.rc;
		r.stage = stageOf(rs.status);
		r.voteCloses = rs.voteDeadline == null ? "—" : rs.voteDeadline;
		return r;
	}

	/** Coarse Releases-tab status, per {@link Release}'s {@code "VOTING" | "RELEASED" | "DROPPED" | "DRAFT"}. */
	private static String statusOf(RunStatus status) {
		return switch (status) {
			case AWAITING_VOTE -> "VOTING";
			case RELEASED -> "RELEASED";
			case DROPPED -> "DROPPED";
			case RUNNING, FAILED -> "DRAFT";
		};
	}

	/** Human-readable phase, per {@link Release}'s {@code "Awaiting vote" | "Distributed" | "Cancelled"}. */
	private static String stageOf(RunStatus status) {
		return switch (status) {
			case AWAITING_VOTE -> "Awaiting vote";
			case RELEASED -> "Distributed";
			case DROPPED -> "Cancelled";
			case RUNNING -> "Building";
			case FAILED -> "Failed";
		};
	}
}
