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

package org.apache.juneau.releng.engine;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.juneau.releng.log.RunEventStore;
import org.apache.juneau.rest.server.runreport.RunEvent.DoneStatus;
import org.apache.juneau.rest.server.runreport.RunEvent.EndStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuditServiceTest {

	private static String kinds(List<AuditEntry> rows) {
		return rows.stream().map(r -> r.run + ":" + r.kind + ":" + r.subject).reduce((a, b) -> a + " " + b).orElse("");
	}

	@Test
	void a01_emptyStateDirIsEmpty(@TempDir Path dir) {
		assertTrue(new AuditService(new RunStateStore(dir), new RunEventStore(dir)).list().isEmpty());
	}

	@Test
	void a02_flattensEventsAndDroppedRcsAndLeavesTestEventsOut(@TempDir Path dir) {
		var events = new RunEventStore(dir);
		var store = new RunStateStore(dir);
		events.begin("9.2.1", "build", "Build", 1);
		events.note("9.2.1", org.apache.juneau.rest.server.runreport.RunEvent.Level.INFO, "hello", null, "build");
		events.end("9.2.1", "build", EndStatus.OK, 1200);
		events.done("9.2.1", DoneStatus.OK);
		var rs = RunState.create("9.2.1", "b", List.of("build"));
		rs.rcHistory.add(new RcHistoryEntry(1, "2026-10-01T00:00:00Z", "bad RC"));
		store.save(rs);

		var rows = new AuditService(store, events).list();

		assertEquals("9.2.1:step:build 9.2.1:note:build 9.2.1:end:build 9.2.1:done:null 9.2.1:rc-dropped:RC1", kinds(rows));
		var step = rows.get(0);
		assertEquals(1, step.seq);
		assertEquals("Build", step.detail);
		assertEquals("1200 ms", rows.get(2).detail);
		var dropped = rows.get(4);
		assertEquals("2026-10-01T00:00:00Z", dropped.at);
		assertEquals("bad RC", dropped.detail);
	}

	@Test
	void a03_readsOnlyAndNeverWrites(@TempDir Path dir) throws Exception {
		var events = new RunEventStore(dir);
		events.begin("9.2.1", "build", "Build", 1);
		var file = dir.resolve("logs/9.2.1-events.jsonl");
		var before = Files.readString(file);
		var stamp = Files.getLastModifiedTime(file);

		new AuditService(new RunStateStore(dir), events).list();

		assertEquals(before, Files.readString(file));
		assertEquals(stamp, Files.getLastModifiedTime(file));
		try (var files = Files.list(dir)) {
			assertEquals(List.of("logs"), files.map(p -> p.getFileName().toString()).toList());
		}
	}

	@Test
	void a04_runsWithoutEventsStillShowTheirDroppedRcs(@TempDir Path dir) {
		var store = new RunStateStore(dir);
		var rs = RunState.create("9.3.0", "b", List.of("build"));
		rs.rcHistory.add(new RcHistoryEntry(2, "2026-10-02T00:00:00Z", "why"));
		store.save(rs);

		assertEquals("9.3.0:rc-dropped:RC2", kinds(new AuditService(store, new RunEventStore(dir)).list()));
	}

	@Test
	void a05_aCorruptEventLineIsSkipped(@TempDir Path dir) throws Exception {
		var events = new RunEventStore(dir);
		events.begin("9.2.1", "build", "Build", 1);
		Files.writeString(dir.resolve("logs/9.2.1-events.jsonl"), "not json\n", java.nio.file.StandardOpenOption.APPEND);
		events.done("9.2.1", DoneStatus.OK);

		assertEquals("9.2.1:step:build 9.2.1:done:null", kinds(new AuditService(new RunStateStore(dir), events).list()));
	}
}
