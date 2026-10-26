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

import static org.junit.jupiter.api.Assumptions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/**
 * Locates the real {@code juneau_terminal.py} (the installed 10.0.0-SNAPSHOT, or {@code JUNEAU_TERMINAL_PY}) and the fixture
 * {@code fake-scripts/release.py}; skips the test when python3 or the runner is missing.
 */
final class RunnerFixture {

	private RunnerFixture() {}

	static Path scriptsDir() throws Exception {
		var runner = System.getenv("JUNEAU_TERMINAL_PY") != null ? Path.of(System.getenv("JUNEAU_TERMINAL_PY"))
			: Path.of(System.getProperty("user.home"), ".m2/repository/org/apache/juneau/juneau-terminal/10.0.0-SNAPSHOT/juneau-terminal-10.0.0-SNAPSHOT.py");
		assumeTrue(Files.isRegularFile(runner), "juneau-terminal is not installed: " + runner);
		boolean python;
		try {
			python = new ProcessBuilder("python3", "--version").start().waitFor() == 0;
		} catch (java.io.IOException e) {
			python = false;
		}
		assumeTrue(python, "python3 is not on PATH");
		System.setProperty("juneau.terminal.script", runner.toString());
		return Path.of(RunnerFixture.class.getResource("/fake-scripts/release.py").toURI()).getParent();
	}

	@SuppressWarnings({
		"java:S2925" // This is the bounded poll helper: the conditions it waits on raise no signal.
	})
	static void await(BooleanSupplier condition, int seconds) throws InterruptedException {
		var end = System.nanoTime() + TimeUnit.SECONDS.toNanos(seconds);
		while (! condition.getAsBoolean()) {
			if (System.nanoTime() > end)
				throw new AssertionError("timed out after " + seconds + " s");
			Thread.sleep(50);
		}
	}
}
