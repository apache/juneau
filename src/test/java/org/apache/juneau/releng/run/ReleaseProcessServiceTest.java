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

import static org.apache.juneau.releng.run.RunnerFixture.*;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.apache.juneau.releng.log.EventBroadcaster;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReleaseProcessServiceTest {

	ReleaseProcessService svc;

	@AfterEach
	void stop() throws Exception {
		if (svc != null) {
			svc.cancel();
			await(() -> svc.live().isEmpty(), 20);
		}
	}

	ReleaseProcessService service(Path home, String fakeScript) throws Exception {
		Files.createDirectories(home);
		Files.writeString(home.resolve("fake-script.json"), fakeScript);
		svc = new ReleaseProcessService(home, scriptsDir(), k -> Optional.empty(), new EventBroadcaster());
		return svc;
	}

	@Test
	void a01_startSpawnsWithAttemptDir(@TempDir Path home) throws Exception {
		var live = service(home, "{\"sleep\":0,\"exit\":0}").start(true);
		await(() -> live.process().isDone(), 20);
		assertTrue(live.dir().startsWith(home.resolve("attempts")));
		var argv = Files.readString(live.dir().resolve("fake-argv.json"));
		assertTrue(argv.contains("\"--attempt-dir\", \"" + live.dir() + "\""), argv);
		assertTrue(argv.contains("--dry-run") && argv.contains("condensed"), argv);
		assertTrue(Files.size(live.dir().resolve("terminal.log")) > 0);
		await(() -> Files.exists(live.dir().resolve(RunFiles.SIDECAR)) && read(live.dir()).contains("\"exit\":0"), 10);
		assertEquals("attempt-" + live.dir().getFileName(), live.attemptId());
	}

	@Test
	void a02_secondStart409(@TempDir Path home) throws Exception {
		var s = service(home, "{\"sleep\":30}");
		s.start(false);
		var e = assertThrows(ReleaseProcessService.BusyException.class, () -> s.resume(false));
		assertTrue(e.getMessage().contains("already running"), e.getMessage());
	}

	@Test
	void a03_cancelRecordsCancelled(@TempDir Path home) throws Exception {
		var s = service(home, "{\"sleep\":30}");
		var live = s.start(false);
		var runJson = home.resolve("runs/9.2.1/run.json");
		await(() -> Files.exists(runJson), 20);
		s.cancel();
		await(() -> live.process().isDone(), 20);
		await(() -> readRun(runJson).contains("\"cancelled\""), 10);
		await(() -> s.live().isEmpty(), 10);
		assertTrue(readRun(runJson).contains("\"cancelled\""), readRun(runJson));
	}

	@Test
	void a04_shutdownHookMarksJrmStopped(@TempDir Path home) throws Exception {
		var s = service(home, "{\"sleep\":30}");
		var live = s.start(false);
		await(() -> Files.exists(home.resolve("runs/9.2.1/run.json")), 20);
		s.shutdown();
		assertTrue(Files.exists(live.dir().resolve(RunFiles.STOPPED_MARKER)));
		await(() -> live.process().isDone(), 20);
		var outcome = new RunFiles(home, d -> false).history().get(0).outcome;
		assertEquals("interrupted (JRM stopped)", outcome);
	}

	@Test
	void a05_dropRcAnswersTheTypedConfirm(@TempDir Path home) throws Exception {
		var s = service(home, "{\"prompts\":[{\"kind\":\"confirm\",\"key\":\"confirm.revert\",\"message\":\"Type RC2\",\"expect\":\"RC2\"}]}");
		var live = s.dropRc("RC2", false);
		await(() -> live.process().isDone(), 20);
		await(() -> read(live.dir()).contains("\"exit\":"), 10);
		var answers = Files.readString(home.resolve("fake-answers.jsonl"));
		assertTrue(answers.contains("\"value\": \"RC2\""), answers);
		assertTrue(Files.readString(live.dir().resolve("fake-argv.json")).contains("--revert"));
	}

	@Test
	void a06_failedLaunchLeavesNoAttempt(@TempDir Path home) throws Exception {
		var s = service(home, "{\"sleep\":0}");
		var previous = System.getProperty("juneau.run.script");
		System.clearProperty("juneau.run.script");
		try {
			assertThrows(IllegalStateException.class, () -> s.start(false));
		} finally {
			System.setProperty("juneau.run.script", previous);
		}
		assertTrue(s.live().isEmpty());
		assertTrue(new RunFiles(home, d -> false).history().isEmpty(), "no phantom interrupted row");
		try (var dirs = Files.list(home.resolve("attempts"))) {
			assertEquals(0, dirs.count());
		}
	}

	static String read(Path dir) {
		try {
			return Files.readString(dir.resolve(RunFiles.SIDECAR)).replace(" ", "");
		} catch (Exception e) {
			return "";
		}
	}

	static String readRun(Path p) {
		try {
			return Files.readString(p);
		} catch (Exception e) {
			return "";
		}
	}
}
