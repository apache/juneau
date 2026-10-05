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

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Shells out to external tools (git, mvn, gpg, gh, etc.), buffered or streamed, with optional stdin/env/timeout.
 */
public interface ProcessRunner {

	/**
	 * Runs a command and returns stdout split into trimmed non-empty lines.
	 */
	List<String> runLines(List<String> command);

	/**
	 * Runs a command and returns the full stdout as one string.
	 */
	String runText(List<String> command);

	/**
	 * Result of a raw run: exit code + combined stdout/stderr (never throws on non-zero).
	 */
	record ProcResult(int exitCode, String output) {
		/**
		 * Whether the command exited zero.
		 */
		public boolean ok() {
			return exitCode == 0;
		}
	}

	/**
	 * Runs a command with optional stdin and extra environment variables, returning the exit code and
	 * output without throwing. Keeps secrets off argv (pass them via {@code stdin} or {@code env}).
	 *
	 * <p>Child stdin is always closed after any {@code stdin} bytes are written, so a process that
	 * prompts {@code [Y/n]} cannot hang the caller waiting for a tty.
	 */
	ProcResult run(List<String> command, String stdin, Map<String, String> env);

	/**
	 * Streaming variant of {@link #run(List, String, Map)}: invokes {@code lineSink} for each
	 * combined stdout/stderr line as it arrives (for live SSE tailing), and also accumulates the
	 * full output into the returned {@link ProcResult}. Never throws on non-zero exit. Default delegates to a
	 * buffered run then replays lines; the real implementation overrides for true line-at-a-time tailing.
	 */
	default ProcResult run(List<String> command, String stdin, Map<String, String> env, Consumer<String> lineSink) {
		return runStreamingDefault(command, stdin, env, lineSink);
	}

	/**
	 * Timeout-capable variant of {@link #run(List, String, Map)}. Default ignores the timeout and delegates
	 * to {@link #run(List, String, Map)}; {@link DefaultProcessRunner} honors it and destroy-forcibly on expiry.
	 *
	 * @param command the command and arguments
	 * @param stdin optional stdin bytes, or {@code null}
	 * @param env extra environment variables, or {@code null}
	 * @param timeout {@code null} or non-positive means unbounded. Ignored by this default; {@link DefaultProcessRunner} honors it.
	 */
	default ProcResult run(List<String> command, String stdin, Map<String, String> env, Duration timeout) {
		return run(command, stdin, env);
	}

	/**
	 * Default streaming impl for stubs that don't override it: falls back to a buffered run then replays.
	 */
	default ProcResult runStreamingDefault(List<String> command, String stdin, Map<String, String> env,
			Consumer<String> lineSink) {
		var res = run(command, stdin, env);
		if (lineSink != null && res.output() != null)
			for (var line : res.output().split("\n", -1))
				if (!line.isEmpty())
					lineSink.accept(line);
		return res;
	}
}
