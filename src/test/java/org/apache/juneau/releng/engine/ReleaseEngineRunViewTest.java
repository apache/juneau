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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.apache.juneau.releng.config.TargetProfile;
import org.apache.juneau.releng.nexus.NexusStagingClient;
import org.apache.juneau.releng.util.ProcessRunner;
import org.apache.juneau.rest.server.views.RunEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * The engine's mirror of each step into the run-view event stream: one step per invocation, how it ended, the notes it
 * recorded, and the test results of the Maven steps.
 */
class ReleaseEngineRunViewTest {

	private static final String V = "9.2.1";

	private static final String REPORT = """
		<?xml version="1.0" encoding="UTF-8"?>
		<testsuite name="org.example.FooTest" tests="2" failures="%d" errors="0" skipped="0" time="0.5">
		  <testcase name="passes" classname="org.example.FooTest" time="0.1"/>
		  <testcase name="breaks" classname="org.example.FooTest" time="0.2">%s</testcase>
		</testsuite>
		""";

	private ProcessRunner runner(Path dir, int verifyExit, boolean writeReport) {
		return new ProcessRunner() {
			@Override
			public List<String> runLines(List<String> c) {
				return List.of();
			}

			@Override
			public String runText(List<String> c) {
				return "";
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String,String> e) {
				if (c.contains("ls-remote"))
					return new ProcResult(0, "sha\trefs/heads/juneau-9.2.1-branch\n");
				if (c.contains("verify")) {
					if (writeReport)
						writeReport(dir, verifyExit != 0);
					return new ProcResult(verifyExit, "");
				}
				return new ProcResult(0, "ok");
			}

			@Override
			public ProcResult run(List<String> c, String s, Map<String,String> e, Consumer<String> k) {
				return run(c, s, e);
			}
		};
	}

	private static Path reportsDir(Path dir) {
		return dir.resolve("staging/git/juneau/juneau-core/target/surefire-reports");
	}

	private static Path writeReport(Path dir, boolean failing) {
		try {
			var reports = reportsDir(dir);
			Files.createDirectories(reports);
			var file = reports.resolve("TEST-org.example.FooTest.xml");
			Files.writeString(file, REPORT.formatted(failing ? 1 : 0, failing ? "<failure message=\"boom\">trace</failure>" : ""));
			return file;
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	private ReleaseEngine engine(Path dir, ProcessRunner runner) {
		var branches = new BranchResolver(runner, "/repo");
		return ReleaseEngine.forTests(new RunStateStore(dir), StepRegistry.standard(branches), runner, branches, dir);
	}

	private ReleaseEngine engine(Path dir) {
		return engine(dir, runner(dir, 0, false));
	}

	private void satisfyAllPredecessorsOf(Path dir, String stepId) {
		var store = new RunStateStore(dir);
		var rs = store.load(V).orElseThrow();
		var ids = StepRegistry.standard(new BranchResolver(runner(dir, 0, false), "/repo")).ids();
		for (var i = 0; i < ids.indexOf(stepId); i++)
			rs.step(ids.get(i)).status = StepStatus.SUCCEEDED;
		store.save(rs);
	}

	private List<RunEvent> events(ReleaseEngine eng) {
		return eng.runViewSource(V.replace('.', '_')).orElseThrow().page(null, 5000).events();
	}

	/** One compact token per event: {@code step:id}, {@code end:id:status}, {@code note:level:step}, {@code test:status}. */
	private List<String> summary(ReleaseEngine eng) {
		var out = new ArrayList<String>();
		for (var e : events(eng)) {
			var m = e.toContractMap();
			switch (e.kind()) {
				case STEP -> out.add("step:" + m.get("id") + (m.containsKey("status") ? ":" + m.get("status") : ""));
				case END -> out.add("end:" + m.get("id") + ":" + m.get("status"));
				case NOTE -> out.add("note:" + m.get("level") + ":" + m.get("step"));
				case TEST -> out.add("test:" + m.get("status"));
				default -> out.add(e.kind().name().toLowerCase());
			}
		}
		return out;
	}

	private Map<String,Object> first(ReleaseEngine eng, RunEvent.Kind kind) {
		return events(eng).stream().filter(e -> e.kind() == kind).findFirst().orElseThrow().toContractMap();
	}

	@Test
	void a01_aSuccessfulStepIsAStepThenAnOkEnd(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		eng.apply(V, "preflight", Map.of());

		assertEquals(List.of("step:preflight", "end:preflight:ok"), summary(eng));
		var step = first(eng, RunEvent.Kind.STEP);
		assertEquals(1, step.get("n"));
		assertEquals(eng.registry().byId("preflight").title(), step.get("title"));
		assertTrue(first(eng, RunEvent.Kind.END).containsKey("ms"));
	}

	@Test
	void a02_aFailedStepEndsFailAndAResumeIsANewAttempt(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "tally-vote-result");

		assertFalse(eng.apply(V, "tally-vote-result", Map.of()).success);
		assertTrue(eng.apply(V, "tally-vote-result", Map.of("voteOutcome", "passed", "tally", "+3 binding\nsecond line")).success);

		assertEquals(List.of("step:tally-vote-result", "end:tally-vote-result:fail", "step:tally-vote-result.2",
			"note:info:tally-vote-result.2", "end:tally-vote-result.2:ok"), summary(eng).stream().filter(s -> ! s.startsWith("end:vote-gate")).toList());
		var steps = events(eng).stream().filter(e -> e.kind() == RunEvent.Kind.STEP).map(e -> (String)e.toContractMap().get("title")).toList();
		assertEquals(List.of("Tally vote result", "Tally vote result (attempt 2)"), steps);
	}

	@Test
	void a03_theVoteGateStaysOpenAsWaitingUntilTheTallyPasses(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "vote-gate");

		eng.apply(V, "vote-gate", Map.of());
		assertEquals(List.of("step:vote-gate", "step:vote-gate:waiting"), summary(eng));

		eng.apply(V, "tally-vote-result", Map.of("voteOutcome", "passed", "tally", "+3 binding\nsecond line"));
		var tail = summary(eng).subList(2, summary(eng).size());
		assertEquals(List.of("step:tally-vote-result", "note:info:tally-vote-result", "end:vote-gate:ok", "end:tally-vote-result:ok"), tail);
		var note = events(eng).stream().filter(e -> e.kind() == RunEvent.Kind.NOTE).findFirst().orElseThrow().toContractMap();
		assertEquals("Vote passed — +3 binding", note.get("text"), "only the first line of the tally");
	}

	@Test
	void a04_aRejectedTallyIsAWarningAndLeavesTheGateWaiting(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "vote-gate");
		eng.apply(V, "vote-gate", Map.of());

		eng.apply(V, "tally-vote-result", Map.of("voteOutcome", "rejected"));

		var s = summary(eng);
		assertTrue(s.contains("note:warn:tally-vote-result"), s.toString());
		assertFalse(s.contains("end:vote-gate:ok"), s.toString());
	}

	@Test
	void a05_aReviewGateWaitsThenEndsWhenConfirmed(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "javadoc-verify");

		eng.apply(V, "javadoc-verify", Map.of());
		assertEquals(List.of("step:javadoc-verify", "step:javadoc-verify:waiting"), summary(eng));

		eng.confirmReview(V, "javadoc-verify");
		assertEquals("end:javadoc-verify:ok", summary(eng).get(2));
	}

	@Test
	void a06_aSkippedStepIsAStepThenASkipEnd(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);

		eng.skip(V, "test-workspace-verify");

		assertEquals(List.of("step:test-workspace-verify", "end:test-workspace-verify:skip"), summary(eng));
	}

	@Test
	void a07_composedEmailsAreNotesUnderTheirStep(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "compose-propose-email");

		eng.apply(V, "compose-propose-email", Map.of());

		assertEquals(List.of("step:compose-propose-email", "note:info:compose-propose-email", "end:compose-propose-email:ok"), summary(eng));
		var text = (String)first(eng, RunEvent.Kind.NOTE).get("text");
		assertTrue(text.startsWith("Composed the PROPOSE email draft: "), text);
		assertFalse(text.contains(dir.toString()), "a file name, not a path");
	}

	@Test
	void b01_aMavenStepFeedsItsFreshSurefireReportsBeforeItEnds(@TempDir Path dir) {
		var eng = engine(dir, runner(dir, 0, true));
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "build-verify");

		eng.apply(V, "build-verify", Map.of());

		assertEquals(List.of("step:build-verify", "replace", "test:pass", "test:pass", "end:build-verify:ok"), summary(eng));
		var test = first(eng, RunEvent.Kind.TEST);
		assertEquals("build-verify", test.get("step"));
		assertEquals("surefire", test.get("fw"));
		assertEquals("org.example.FooTest", test.get("suite"));
	}

	@Test
	void b02_aFailingMavenStepKeepsTheFailedTestsAndEndsFail(@TempDir Path dir) {
		var eng = engine(dir, runner(dir, 1, true));
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "build-verify");

		assertFalse(eng.apply(V, "build-verify", Map.of()).success);

		assertEquals(List.of("step:build-verify", "replace", "test:pass", "test:fail", "end:build-verify:fail"), summary(eng));
	}

	@Test
	void b03_aReportLeftByAnEarlierRunIsNotAttributedToTheStep(@TempDir Path dir) throws IOException {
		var stale = writeReport(dir, true);
		Files.setLastModifiedTime(stale, FileTime.fromMillis(1_000));
		var eng = engine(dir);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "build-verify");

		eng.apply(V, "build-verify", Map.of());

		assertEquals(List.of("step:build-verify", "end:build-verify:ok"), summary(eng));
	}

	@Test
	void c01_finalizingTheRunIsDoneAndMakesTheSourceTerminal(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "finalize-run");
		var source = eng.runViewSource("9_2_1").orElseThrow();
		assertFalse(source.page(null, 100).terminal());

		eng.apply(V, "finalize-run", Map.of());

		var s = summary(eng);
		assertEquals(List.of("step:finalize-run", "end:finalize-run:ok", "done"), s);
		assertTrue(eng.runViewSource("9_2_1").orElseThrow().page(null, 100).terminal());
	}

	@Test
	void c02_aRunNobodyStartedHasNoSource(@TempDir Path dir) {
		var eng = engine(dir);
		eng.start(V, null);
		assertTrue(eng.runViewSource("9_9_9").isEmpty());
		assertTrue(eng.runViewSource("9_2_1").isPresent());
		assertTrue(eng.runViewSource("9_2_1").orElseThrow().page(null, 100).events().isEmpty(), "nothing has run yet");
	}

	@Test
	void c03_aStepInterruptedByARestartEndsFail(@TempDir Path dir) {
		var eng = engine(dir);
		var rs = eng.start(V, null);
		eng.runEvents().begin(V, "preflight", "Preflight", 1);
		var store = new RunStateStore(dir);
		rs = store.load(V).orElseThrow();
		rs.step("preflight").status = StepStatus.RUNNING;
		rs.currentStepId = "preflight";
		store.save(rs);

		eng.recoverOnBoot();

		assertEquals(List.of("step:preflight", "end:preflight:fail"), summary(eng));
	}

	@Test
	void c04_droppingTheCandidateEndsOpenStepsAndSaysSo(@TempDir Path dir) {
		var runner = runner(dir, 0, false);
		var eng = engine(dir, runner);
		eng.start(V, null);
		satisfyAllPredecessorsOf(dir, "vote-gate");
		eng.apply(V, "vote-gate", Map.of());
		var drop = new DropRcService(new RunStateStore(dir), eng.registry(), runner, dir.resolve("staging/git/juneau"), dir,
			NexusStagingClient.forTests((m, p, b) -> ""), TargetProfile.prodDefault(), eng.runEvents());

		drop.apply(V, "vote rejected", () -> "me", () -> "pw");

		var s = summary(eng);
		assertEquals(List.of("step:vote-gate", "step:vote-gate:waiting", "end:vote-gate:skip", "note:warn:null"), s);
		var text = (String)events(eng).get(events(eng).size() - 1).toContractMap().get("text");
		assertEquals("RC1 dropped: vote rejected; continuing with RC2", text);
	}
}
