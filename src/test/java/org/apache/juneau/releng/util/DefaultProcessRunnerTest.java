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

package org.apache.juneau.releng.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

class DefaultProcessRunnerTest {

	private final DefaultProcessRunner runner = new DefaultProcessRunner();

	@Test
	void a01_runTextReturnsOutput() {
		assertEquals("hi\n", runner.runText(List.of("sh", "-c", "echo hi")));
	}

	@Test
	void a02_runTextThrowsOnNonZeroExit() {
		var cmd = List.of("sh", "-c", "echo boom; exit 3");
		var e = assertThrows(IllegalStateException.class, () -> runner.runText(cmd));
		assertTrue(e.getMessage().contains("exit 3"), e.getMessage());
		assertTrue(e.getMessage().contains("boom"), e.getMessage());
	}

	@Test
	void a03_runLinesStripsAndDropsBlankLines() {
		assertEquals(List.of("a", "b"), runner.runLines(List.of("sh", "-c", "printf '  a  \\n\\n b\\n   \\n'")));
	}

	@Test
	void b01_runPassesStdinAndEnv() {
		var res = runner.run(List.of("sh", "-c", "read x; echo \"$x-$RM_TEST_VAR\""), "in\n", Map.of("RM_TEST_VAR", "v"));
		assertEquals(0, res.exitCode());
		assertEquals("in-v\n", res.output());
	}

	@Test
	void b02_zeroAndNegativeTimeoutMeanNoTimeout() {
		var cmd = List.of("sh", "-c", "echo ok");
		assertEquals("ok\n", runner.run(cmd, null, null, Duration.ZERO).output());
		assertEquals("ok\n", runner.run(cmd, null, null, Duration.ofSeconds(-1)).output());
	}

	@Test
	void b03_timeoutNotReachedReturnsNormally() {
		var res = runner.run(List.of("sh", "-c", "echo ok"), null, null, Duration.ofSeconds(10));
		assertTrue(res.ok());
		assertEquals("ok\n", res.output());
	}

	@Test
	void b04_nullSinkIsAllowed() {
		assertTrue(runner.run(List.of("sh", "-c", "echo ok"), null, null, (Consumer<String>)null).ok());
	}

	@Test
	void c01_missingExecutableIsReportedAsError() {
		var cmd = List.of("/nonexistent/rm-no-such-binary");
		var e = assertThrows(IllegalStateException.class, () -> runner.run(cmd, null, null));
		assertTrue(e.getMessage().contains("Error running"), e.getMessage());
	}

	@Test
	void c02_interruptIsPropagatedAndFlagRestored() {
		Thread.currentThread().interrupt();
		try {
			var cmd = List.of("sleep", "5");
			var e = assertThrows(IllegalStateException.class, () -> runner.run(cmd, null, null));
			assertTrue(e.getMessage().contains("Interrupted"), e.getMessage());
			assertTrue(Thread.currentThread().isInterrupted());
		} finally {
			Thread.interrupted(); // clear the flag for later tests
		}
	}

	@Test
	void d01_interfaceDefaultsStreamOutputLinesAndIgnoreTimeout() {
		var calls = new ArrayList<List<String>>();
		ProcessRunner fake = new ProcessRunner() {
			@Override public List<String> runLines(List<String> c) { return List.of(); }
			@Override public String runText(List<String> c) { return ""; }
			@Override public ProcResult run(List<String> c, String s, Map<String, String> e) {
				calls.add(c);
				return new ProcResult(0, "a\n\nb\n");
			}
		};
		var sink = new ArrayList<String>();
		var res = fake.run(List.of("x"), null, null, sink::add);
		assertTrue(res.ok());
		assertEquals(List.of("a", "b"), sink);
		assertEquals(0, fake.run(List.of("y"), null, null, Duration.ofSeconds(1)).exitCode());
		assertEquals(2, calls.size());
		// null sink and null output are both tolerated
		var nullOut = new ProcessRunner() {
			@Override public List<String> runLines(List<String> c) { return List.of(); }
			@Override public String runText(List<String> c) { return ""; }
			@Override public ProcResult run(List<String> c, String s, Map<String, String> e) { return new ProcResult(1, null); }
		};
		assertFalse(nullOut.run(List.of("z"), null, null, sink::add).ok());
		assertTrue(fake.runStreamingDefault(List.of("x"), null, null, null).ok());
	}

	@Test
	void e01_svnArgsLiterals() {
		assertEquals("--username", SvnArgs.USERNAME);
		assertEquals("--password-from-stdin", SvnArgs.PASSWORD_FROM_STDIN);
	}
}
