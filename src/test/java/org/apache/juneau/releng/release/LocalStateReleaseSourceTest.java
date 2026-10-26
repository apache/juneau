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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.apache.juneau.releng.run.RunFiles;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Releases-tab rows built from {@code release.py}'s run files.
 */
class LocalStateReleaseSourceTest {

	private static void run(Path home, String version, String phase, String deadline) throws Exception {
		var d = Files.createDirectories(home.resolve("runs").resolve(version));
		Files.writeString(d.resolve("run.json"), "{\"schema\":1,\"details\":{\"version\":\"" + version
			+ "\",\"rc\":2},\"phase\":\"" + phase + "\"" + (deadline == null ? "" : ",\"voteDeadline\":\"" + deadline + "\"") + "}");
	}

	@Test
	void a01_phaseToStage(@TempDir Path dir) throws Exception {
		var expected = Map.of(
			"rc", "DRAFT,Building RC2,—",
			"vote", "VOTING,Voting (closes 2026-10-23T12:00:00-04:00),2026-10-23T12:00:00-04:00",
			"vote-failed", "DRAFT,Vote failed,—",
			"promote", "DRAFT,Promoting,—",
			"released", "RELEASED,Released,—");
		var n = 0;
		for (var e : expected.entrySet()) {
			var home = dir.resolve("h" + n++);
			run(home, "9.2.1", e.getKey(), "vote".equals(e.getKey()) ? "2026-10-23T12:00:00-04:00" : null);
			var rows = new LocalStateReleaseSource(new RunFiles(home, p -> false)).list();
			assertSize(1, rows);
			assertBean(rows.get(0), "version,rc,source", "9.2.1,RC2,state");
			assertBean(rows.get(0), "status,stage,voteCloses", e.getValue());
		}
	}

	@Test
	void a02_noRunsYieldsNoRows(@TempDir Path home) {
		assertTrue(new LocalStateReleaseSource(new RunFiles(home, p -> false)).list().isEmpty());
	}
}
