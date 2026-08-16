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

package org.apache.juneau.releng.nexus;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.List;
import java.util.Map;
import org.apache.juneau.marshall.marshaller.Json;

/**
 * A stateful, in-memory model of the parts of the Nexus 2 staging lifecycle the real {@link NexusStagingClient}
 * exercises: profile-scoped repo discovery, single-repo detail, and the {@code bulk/close}, {@code bulk/promote}
 * and {@code bulk/drop} transitions ({@code OPEN → CLOSED → RELEASED}, with {@code DROPPED} reachable from OPEN
 * or CLOSED).
 *
 * <p>Because a SAFE {@code release-perform} is command-logged rather than executed, no real staging repo is
 * ever created; this model therefore <b>lazily synthesizes</b> the OPEN {@code orgapachejuneau-NNNN} repo that
 * {@code release-perform} would have created, on the first discovery call. After a repo reaches a terminal
 * state a fresh OPEN repo is synthesized on the next discovery so a later run starts clean.
 *
 * <p>Only the fields the client parses are faked ({@code repositoryId}, {@code profileId}, state, and the
 * {@code transitioning} flag), plus a Nexus-shaped error envelope for illegal transitions.
 */
public class NexusMockModel {

	static final String OPEN = "open";
	static final String CLOSED = "closed";
	static final String RELEASED = "released";
	static final String DROPPED = "dropped";

	private final String profileId;
	private int counter = 1041;
	private String repoId;
	private String state;

	public NexusMockModel(String profileId) {
		this.profileId = profileId;
	}

	/** Clears state so the next discovery synthesizes a fresh OPEN repo (called on run-start). */
	public synchronized void reset() {
		repoId = null;
		state = null;
	}

	public synchronized String currentRepoId() {
		return repoId;
	}

	public synchronized String currentState() {
		return state;
	}

	private void seedIfNeeded() {
		if (repoId == null || RELEASED.equals(state) || DROPPED.equals(state)) {
			counter++;
			repoId = "orgapachejuneau-" + counter;
			state = OPEN;
		}
	}

	/** {@code GET /profile_repositories/{profileId}} — the discovery list (lazily seeded). */
	public synchronized String profileRepositories() {
		seedIfNeeded();
		return Json.DEFAULT.write(List.of(repoView()));
	}

	/** {@code GET /repository/{repoId}} — single-repo detail. */
	public synchronized String repository(String id) {
		if (repoId == null || !repoId.equals(id))
			throw isex("No such staging repository: %s", id);
		return Json.DEFAULT.write(repoView());
	}

	public synchronized void close(String id) {
		requireRepo(id);
		if (!OPEN.equals(state))
			throw illegal("close", id);
		state = CLOSED;
	}

	public synchronized void promote(String id) {
		requireRepo(id);
		if (!CLOSED.equals(state))
			throw illegal("promote", id);
		state = RELEASED;
	}

	public synchronized void drop(String id) {
		requireRepo(id);
		if (!OPEN.equals(state) && !CLOSED.equals(state))
			throw illegal("drop", id);
		state = DROPPED;
	}

	/** Extracts the single staged repository id from a Nexus {@code bulk/*} request body. */
	@SuppressWarnings({ "unchecked" // Parsed JSON is assigned to its known generic shape.
	})
	static String repoIdFromBody(String body) {
		if (body == null || body.isBlank())
			return null;
		Map<String, Object> parsed = Json.DEFAULT.read(body, Map.class);
		var data = (Map<String, Object>) parsed.get("data");
		if (data == null)
			return null;
		var ids = (List<Object>) data.get("stagedRepositoryIds");
		return (ids == null || ids.isEmpty()) ? null : String.valueOf(ids.get(0));
	}

	private void requireRepo(String id) {
		if (repoId == null || !repoId.equals(id))
			throw isex("No such staging repository: %s", id);
	}

	private RuntimeException illegal(String op, String id) {
		return isex("illegalTransition: cannot %s repository %s in state %s", op, id, state);
	}

	private Map<String, Object> repoView() {
		return Map.of("repositoryId", repoId, "profileId", profileId, "type", state, "state", state, "transitioning",
				Boolean.FALSE, "created", "2026-08-16T12:00:00.000Z", "description", "juneau-release-manager");
	}
}
