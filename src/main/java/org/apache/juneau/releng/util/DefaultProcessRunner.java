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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Default real {@link ProcessRunner} implementation using {@link ProcessBuilder}.
 *
 * <p>Example:
 * <p class="bjava">
 * 	ProcessRunner <jv>runner</jv> = <jk>new</jk> DefaultProcessRunner();
 * 	String <jv>version</jv> = <jv>runner</jv>.runText(List.of(<js>"git"</js>, <js>"--version"</js>));
 * </p>
 */
public class DefaultProcessRunner implements ProcessRunner {
	private static final String MSG_INTERRUPTED = "Interrupted running: %s";
	private static final String MSG_ERROR = "Error running: %s";
	private static final int CODE_TIMEOUT = 124;

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
		var res = execute(command, null, null, null, null);
		if (!res.ok())
			throw isex("Command failed (exit %s): %s\n%s", res.exitCode(), command, res.output());
		return res.output();
	}

	@Override
	public ProcResult run(List<String> command, String stdin, Map<String, String> env) {
		return execute(command, stdin, env, null, null);
	}

	@Override
	public ProcResult run(List<String> command, String stdin, Map<String, String> env, Duration timeout) {
		return execute(command, stdin, env, timeout, null);
	}

	@Override
	public ProcResult run(List<String> command, String stdin, Map<String, String> env, Consumer<String> lineSink) {
		return execute(command, stdin, env, null, lineSink);
	}

	private ProcResult execute(List<String> command, String stdin, Map<String, String> env, Duration timeout,
			Consumer<String> lineSink) {
		try {
			var pb = new ProcessBuilder(command).redirectErrorStream(true);
			if (env != null)
				pb.environment().putAll(env);
			var p = pb.start();
			// Always close child stdin (after optional bytes) so apt-get/brew cannot block on [Y/n].
			try (var os = p.getOutputStream()) {
				if (stdin != null)
					os.write(stdin.getBytes(StandardCharsets.UTF_8));
			}
			var sb = new StringBuffer();
			var reader = new Thread(() -> {
				try (var r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = r.readLine()) != null) {
						sb.append(line).append('\n');
						if (lineSink != null)
							lineSink.accept(line);
					}
				} catch (Exception e) {
					// Destroyed process or closed stream — accumulated output is still returned.
				}
			}, "rm-process-stdout");
			reader.setDaemon(true);
			reader.start();
			boolean finished;
			if (timeout == null || timeout.isZero() || timeout.isNegative()) {
				p.waitFor();
				finished = true;
			} else {
				finished = p.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
			}
			if (!finished) {
				p.destroyForcibly();
				reader.join(1000);
				return new ProcResult(CODE_TIMEOUT, sb + "Timed out after " + timeout + "\n");
			}
			reader.join();
			return new ProcResult(p.exitValue(), sb.toString());
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw isex(e, MSG_INTERRUPTED, command);
		} catch (Exception e) {
			throw isex(e, MSG_ERROR, command);
		}
	}
}
