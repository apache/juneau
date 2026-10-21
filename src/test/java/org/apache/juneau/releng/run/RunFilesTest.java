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
package org.apache.juneau.releng.run;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunFilesTest {

	static Path writeRun(Path home, String version, String phase, String attemptsJson) throws Exception {
		var dir = Files.createDirectories(home.resolve("runs").resolve(version));
		var json = "{\"schema\":1,\"details\":{\"version\":\"" + version + "\",\"rc\":2},\"phase\":\"" + phase
			+ "\",\"last\":\"vote-gate\",\"voteDeadline\":\"2026-10-23T12:00:00-04:00\",\"attempts\":" + attemptsJson + "}";
		return Files.writeString(dir.resolve("run.json"), json);
	}

	static String attempt(Path dir, String status, long pid) {
		return "{\"n\":1,\"dir\":\"" + dir + "\",\"command\":\"start\",\"origin\":\"ui\",\"pid\":" + pid
			+ ",\"status\":\"" + status + "\",\"started\":\"20261020-091000\",\"step\":\"nexus-close\"}";
	}

	@Test
	void a01_activeRun(@TempDir Path home) throws Exception {
		writeRun(home, "9.2.0", "released", "[]");
		writeRun(home, "9.2.1", "vote", "[]");
		var files = new RunFiles(home, pid -> false, d -> false);
		var active = files.active().orElseThrow();
		assertBean(active, "version,rc,phase,last,voteDeadline", "9.2.1,2,vote,vote-gate,2026-10-23T12:00:00-04:00");
		assertSize(2, files.all());
	}

	@Test
	void a02_deadPidRunningIsInterrupted(@TempDir Path home) throws Exception {
		var att = Files.createDirectories(home.resolve("attempts/20261020-091000-001"));
		writeRun(home, "9.2.1", "rc", "[" + attempt(att, "running", 4242) + "]");
		assertEquals("interrupted", new RunFiles(home, pid -> false, d -> false).history().get(0).outcome);
		assertEquals("running", new RunFiles(home, pid -> pid == 4242, d -> false).history().get(0).outcome);
	}

	@Test
	void a03_jrmStoppedMarker(@TempDir Path home) throws Exception {
		var att = Files.createDirectories(home.resolve("attempts/20261020-091000-001"));
		Files.writeString(att.resolve(RunFiles.STOPPED_MARKER), "");
		writeRun(home, "9.2.1", "rc", "[" + attempt(att, "cancelled", 4242) + "]");
		var a = new RunFiles(home, pid -> false, d -> false).history().get(0);
		assertBean(a, "outcome,step,id", "interrupted (JRM stopped),nexus-close,attempt-20261020-091000-001");
	}

	@Test
	void a04_sidecarOnlyDryRunIsInHistoryTagged(@TempDir Path home) throws Exception {
		var att = Files.createDirectories(home.resolve("attempts/20261020-100000-001"));
		Files.writeString(att.resolve(RunFiles.SIDECAR),
			"{\"command\":\"start\",\"origin\":\"ui\",\"dryRun\":true,\"startedAt\":\"20261020-100000\",\"exit\":0}");
		var h = new RunFiles(home, pid -> false, d -> false).history();
		assertSize(1, h);
		assertBean(h.get(0), "command,origin,dryRun,outcome,label", "start,ui,true,ok,20261020-100000-001");
	}

	@Test
	void a05_sidecarMergesIntoRunRecordByDir(@TempDir Path home) throws Exception {
		var att = Files.createDirectories(home.resolve("attempts/20261020-091000-001"));
		Files.writeString(att.resolve(RunFiles.SIDECAR), "{\"command\":\"start\",\"origin\":\"ui\",\"dryRun\":false,\"exit\":0}");
		writeRun(home, "9.2.1", "vote", "[" + attempt(att, "paused", 1) + "]");
		var h = new RunFiles(home, pid -> false, d -> false).history();
		assertSize(1, h);
		assertBean(h.get(0), "outcome,exit,label", "paused,0,#1");
	}

	@Test
	void a06_corruptRunJsonIsSkippedNotGuessed(@TempDir Path home) throws Exception {
		Files.createDirectories(home.resolve("runs/9.2.1"));
		Files.writeString(home.resolve("runs/9.2.1/run.json"), "{not json");
		var files = new RunFiles(home, pid -> false, d -> false);
		assertTrue(files.active().isEmpty());
		assertSize(0, files.all());
	}

	@Test
	void a07_attemptDirsNumberLikeTheScript(@TempDir Path home) throws Exception {
		var a = AttemptDirs.create(home, "20261020-091000");
		var b = AttemptDirs.create(home, "20261020-091000");
		assertEquals("20261020-091000-001", a.getFileName().toString());
		assertEquals("20261020-091000-002", b.getFileName().toString());
	}

	@Test
	void a09_validJsonWithWrongTypesIsSkippedNotThrown(@TempDir Path home) throws Exception {
		Files.createDirectories(home.resolve("runs/9.2.1"));
		Files.writeString(home.resolve("runs/9.2.1/run.json"), "{\"details\":5,\"attempts\":\"x\",\"phase\":\"rc\"}");
		var files = new RunFiles(home, pid -> false, d -> false);
		assertDoesNotThrow(files::all);
		assertSize(0, files.all());
	}

	@Test
	void a08_wrongTypedAttemptFieldsDoNotThrow(@TempDir Path home) throws Exception {
		var att = Files.createDirectories(home.resolve("attempts/20261020-091000-001"));
		writeRun(home, "9.2.1", "rc", "[{\"n\":1,\"dir\":\"" + att + "\",\"command\":42,\"status\":\"paused\"},{\"n\":2,\"command\":7}]");
		var h = new RunFiles(home, pid -> false, d -> false).history();
		assertSize(1, h);
		assertBean(h.get(0), "command,origin,outcome", "<null>,cli,paused");
	}
}
