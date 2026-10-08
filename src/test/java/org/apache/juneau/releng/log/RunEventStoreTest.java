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

package org.apache.juneau.releng.log;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.apache.juneau.rest.server.runreport.RunEvent;
import org.apache.juneau.rest.server.runreport.RunEvent.DoneStatus;
import org.apache.juneau.rest.server.runreport.RunEvent.EndStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunEventStoreTest {

	private static List<RunEvent> read(RunEventStore store, boolean terminal) {
		return store.source("9.2.1", () -> terminal).page(null, 1000).events();
	}

	private static String ids(List<RunEvent> events) {
		var sb = new StringBuilder();
		for (var e : events) {
			var m = e.toContractMap();
			sb.append(m.get("ev")).append(':').append(m.getOrDefault("id", "")).append(m.containsKey("status") ? ":" + m.get("status") : "").append(' ');
		}
		return sb.toString().trim();
	}

	@Test
	void a01_runIdReplacesDots() {
		assertEquals("9_2_1", RunEventStore.runId("9.2.1"));
	}

	@Test
	void a02_beginEndWritesAStepAndItsEnd(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		assertEquals("build", store.begin("9.2.1", "build", "Build", 3));
		store.end("9.2.1", "build", EndStatus.OK, 1200);
		var events = read(store, false);
		assertEquals("step:build end:build:ok", ids(events));
		assertEquals(1, events.get(0).toContractMap().get("rawLine"), "a step links to the first line of its console log");
	}

	@Test
	void a03_aSecondAttemptGetsADottedIdAndTitle(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		store.begin("9.2.1", "build", "Build", 3);
		store.end("9.2.1", "build", EndStatus.FAIL, -1);
		assertEquals("build.2", store.begin("9.2.1", "build", "Build", 3));
		assertEquals("build.3", store.begin("9.2.1", "build", "Build", 3));
		var events = read(store, false);
		assertEquals("step:build end:build:fail step:build.2 end:build.2:skip step:build.3", ids(events));
		assertEquals("Build (attempt 3)", events.get(4).toContractMap().get("title"));
		// The run-view region folds "<id>.<n>" (n >= 2) steps under the plain-id step; keep that naming.
		assertTrue(events.get(2).toContractMap().get("id").toString().matches("build\\.[2-9]"));
		assertTrue(events.get(2).toContractMap().get("title").toString().endsWith("(attempt 2)"));
	}

	@Test
	void a04_waitingMarksTheLatestAttemptWaiting(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		store.begin("9.2.1", "gate", "Gate", 1);
		store.waiting("9.2.1", "gate", "Gate");
		assertEquals("step:gate step:gate:waiting", ids(read(store, false)));
	}

	@Test
	void a05_endOfAStepThatNeverRanIsIgnored(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		store.end("9.2.1", "nope", EndStatus.OK, 1);
		assertTrue(read(store, false).isEmpty());
	}

	@Test
	void a06_endOpenEndsEveryOpenAttempt(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		store.begin("9.2.1", "a", "A", 1);
		store.begin("9.2.1", "b", "B", 2);
		store.end("9.2.1", "a", EndStatus.OK, 1);
		store.endOpen("9.2.1", EndStatus.SKIP);
		assertEquals("step:a step:b end:a:ok end:b:skip", ids(read(store, false)));
	}

	@Test
	void a07_noteIsClippedAndBlankNotesAreDropped(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		store.note("9.2.1", RunEvent.Level.INFO, "x".repeat(5000), null, null);
		store.note("9.2.1", RunEvent.Level.INFO, "  ", null, null);
		store.note("9.2.1", RunEvent.Level.WARN, "see", "https://example.org/r", null);
		var events = read(store, false);
		assertEquals(2, events.size());
		assertEquals(1000, ((String)events.get(0).toContractMap().get("text")).length());
		assertEquals("https://example.org/r", events.get(1).toContractMap().get("href"));
	}

	@Test
	void a08_theSourceIsTerminalWhenTheSupplierSaysSo(@TempDir Path dir) {
		var store = new RunEventStore(dir);
		store.done("9.2.1", DoneStatus.OK);
		assertEquals(1, read(store, false).size());
		assertTrue(new RunEventStore(dir).source("9.2.1", () -> true).page(null, 10).terminal());
	}

	@Test
	void a09_aNewInstanceOverTheSameDirectoryContinuesTheHistory(@TempDir Path dir) {
		new RunEventStore(dir).begin("9.2.1", "build", "Build", 1);
		assertEquals("build.2", new RunEventStore(dir).begin("9.2.1", "build", "Build", 1));
		assertTrue(Files.isRegularFile(dir.resolve("logs/9.2.1-events.jsonl")));
	}

	@Test
	void a10_anUnwritableDirectoryNeverThrows(@TempDir Path dir) throws Exception {
		Files.writeString(dir.resolve("logs"), "a file where the folder should be");
		var store = new RunEventStore(dir);
		assertDoesNotThrow(() -> store.begin("9.2.1", "build", "Build", 1));
	}
}
