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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public interface ProcessRunner {

	/** Runs a command and returns stdout split into trimmed non-empty lines. */
	List<String> runLines(List<String> command);

	/** Runs a command and returns the full stdout as one string. */
	String runText(List<String> command);

	/** Result of a raw run: exit code + combined stdout/stderr (never throws on non-zero). */
	record ProcResult(int exitCode, String output) {
		public boolean ok() {
			return exitCode == 0;
		}
	}

	/**
	 * Runs a command with optional stdin and extra environment variables, returning the exit code and
	 * output without throwing. Keeps secrets off argv (pass them via {@code stdin} or {@code env}).
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

	/** Default streaming impl for stubs that don't override it: falls back to a buffered run then replays. */
	default ProcResult runStreamingDefault(List<String> command, String stdin, Map<String, String> env,
			Consumer<String> lineSink) {
		var res = run(command, stdin, env);
		if (lineSink != null && res.output() != null)
			for (var line : res.output().split("\n", -1))
				if (!line.isEmpty())
					lineSink.accept(line);
		return res;
	}

	/** Default real implementation using {@link ProcessBuilder}. */
	class Default implements ProcessRunner {
		private static final String MSG_INTERRUPTED = "Interrupted running: %s";
		private static final String MSG_ERROR = "Error running: %s";

		@Override
		public List<String> runLines(List<String> command) {
			var out = new ArrayList<String>();
			for (var line : runText(command).split("\n")) {
				var t = line.strip();
				if (!t.isEmpty())
					out.add(t);
			}
			return out;
		}

		@Override
		public String runText(List<String> command) {
			try {
				var p = new ProcessBuilder(command).redirectErrorStream(true).start();
				String text;
				try (var r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
					var sb = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null)
						sb.append(line).append('\n');
					text = sb.toString();
				}
				var code = p.waitFor();
				if (code != 0)
					throw isex("Command failed (exit %s): %s\n%s", code, command, text);
				return text;
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw isex(e, MSG_INTERRUPTED, command);
			} catch (Exception e) {
				throw isex(e, MSG_ERROR, command);
			}
		}

		@Override
		public ProcResult run(List<String> command, String stdin, Map<String, String> env) {
			try {
				var pb = new ProcessBuilder(command).redirectErrorStream(true);
				if (env != null)
					pb.environment().putAll(env);
				var p = pb.start();
				if (stdin != null) {
					try (var os = p.getOutputStream()) {
						os.write(stdin.getBytes(StandardCharsets.UTF_8));
					}
				}
				String text;
				try (var r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
					var sb = new StringBuilder();
					String line;
					while ((line = r.readLine()) != null)
						sb.append(line).append('\n');
					text = sb.toString();
				}
				var code = p.waitFor();
				return new ProcResult(code, text);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw isex(e, MSG_INTERRUPTED, command);
			} catch (Exception e) {
				throw isex(e, MSG_ERROR, command);
			}
		}

		@Override
		public ProcResult run(List<String> command, String stdin, Map<String, String> env, Consumer<String> lineSink) {
			try {
				var pb = new ProcessBuilder(command).redirectErrorStream(true);
				if (env != null)
					pb.environment().putAll(env);
				var p = pb.start();
				if (stdin != null) {
					try (var os = p.getOutputStream()) {
						os.write(stdin.getBytes(StandardCharsets.UTF_8));
					}
				}
				var sb = new StringBuilder();
				try (var r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = r.readLine()) != null) {
						sb.append(line).append('\n');
						if (lineSink != null)
							lineSink.accept(line);
					}
				}
				var code = p.waitFor();
				return new ProcResult(code, sb.toString());
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw isex(e, MSG_INTERRUPTED, command);
			} catch (Exception e) {
				throw isex(e, MSG_ERROR, command);
			}
		}
	}
}
