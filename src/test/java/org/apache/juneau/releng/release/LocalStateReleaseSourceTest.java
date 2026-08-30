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
import java.nio.file.Path;
import java.util.List;
import org.apache.juneau.releng.engine.RunState;
import org.apache.juneau.releng.engine.RunStateStore;
import org.apache.juneau.releng.engine.RunStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Reproduces the "Unknown property 'branch'" parse failure: a persisted {@link RunState} file (which has
 * {@code branch}, {@code steps}, etc.) must not be re-parsed directly as a {@link Release} bean.
 */
class LocalStateReleaseSourceTest {

	@Test
	void a01_mapsRunningRunStateToAnInProgressReleaseRowWithoutParseError(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight", "javadoc-verify"));
		store.save(rs);

		var rows = assertDoesNotThrow(() -> new LocalStateReleaseSource(store).list());

		assertEquals(1, rows.size());
		var row = rows.get(0);
		assertEquals("9.2.1", row.version);
		assertEquals("RC1", row.rc);
		assertEquals("DRAFT", row.status);
		assertEquals("Building", row.stage);
		assertEquals("state", row.source);
	}

	@Test
	void a02_mapsAwaitingVoteRunStateToAVotingRow(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var rs = RunState.create("9.2.1", "juneau-9.2.1-branch", List.of("preflight"));
		rs.status = RunStatus.AWAITING_VOTE;
		rs.voteDeadline = "2026-08-20T00:00:00Z";
		store.save(rs);

		var rows = new LocalStateReleaseSource(store).list();

		assertEquals(1, rows.size());
		assertEquals("VOTING", rows.get(0).status);
		assertEquals("Awaiting vote", rows.get(0).stage);
		assertEquals("2026-08-20T00:00:00Z", rows.get(0).voteCloses);
	}

	@Test
	void a03_emptyStateDirYieldsNoRows(@TempDir Path dir) {
		var rows = new LocalStateReleaseSource(new RunStateStore(dir)).list();
		assertTrue(rows.isEmpty());
	}
}
