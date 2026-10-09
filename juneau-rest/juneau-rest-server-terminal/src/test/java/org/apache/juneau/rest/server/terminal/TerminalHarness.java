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
package org.apache.juneau.rest.server.terminal;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.staticfile.*;

/**
 * Node plumbing for this module's harnesses under {@code src/test/js}.
 *
 * <p>
 * The assets a harness reads are classpath resources copied to temp files: this module's
 * {@code juneau-terminal.js} and {@code .css}, and the xterm.js bundle and stylesheet from the WebJar.
 */
final class TerminalHarness {

	/** The four assets, in the order {@code terminal-browser.cjs} takes them. */
	static final List<String> BROWSER_ASSETS = List.of("xterm.js", "xterm.css", "juneau-terminal.js", "juneau-terminal.css");

	private static final Map<String,Map<?,?>> CACHE = new ConcurrentHashMap<>();

	private TerminalHarness() {}

	/**
	 * Runs the plain-Node harness {@code terminal.cjs} over the runtime and the real xterm.js, or returns
	 * {@code null} when Node is not on the {@code PATH}.
	 */
	static Map<?,?> nodeReport() {
		return CACHE.computeIfAbsent("terminal.cjs", k -> {
			if (! nodeAvailable())
				return null;
			try {
				var out = run(List.of("node", locate("terminal.cjs").toString()), List.of("juneau-terminal.js", "xterm.js"), Map.of(), 60);
				return Json.to(out, Map.class);
			} catch (Exception e) {
				throw new AssertionError("could not run terminal.cjs: " + e, e);
			}
		});
	}

	/**
	 * Runs {@code cmd} with the named assets appended as temp-file arguments and returns its stdout.
	 *
	 * <p>
	 * {@code cmd} is a token list given to {@link ProcessBuilder}, never a shell string.  The child and its descendants are
	 * killed in {@code finally}, so a failed or interrupted wait leaves nothing running.
	 *
	 * @param cmd The command.
	 * @param assets Names from {@link #BROWSER_ASSETS}.
	 * @param env Extra environment variables.
	 * @param seconds The longest wait.
	 */
	static String run(List<String> cmd, List<String> assets, Map<String,String> env, int seconds) throws Exception {
		var temps = new ArrayList<Path>();
		var stdout = Files.createTempFile("terminal-harness-stdout-", ".json");
		var stderr = Files.createTempFile("terminal-harness-stderr-", ".txt");
		Process p = null;
		try {
			var args = new ArrayList<>(cmd);
			for (var a : assets) {
				var f = Files.createTempFile("terminal-harness-", "-" + a);
				temps.add(f);
				Files.write(f, asset(a));
				args.add(f.toString());
			}
			var pb = new ProcessBuilder(args).redirectOutput(stdout.toFile()).redirectError(stderr.toFile());
			pb.environment().putAll(env);
			p = pb.start();
			if (! p.waitFor(seconds, TimeUnit.SECONDS))
				fail(cmd + " did not finish within " + seconds + "s; stderr:\n" + quietRead(stderr));
			if (p.exitValue() != 0)
				fail(cmd + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr) + "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			if (p != null && p.isAlive()) {
				p.descendants().forEach(ProcessHandle::destroyForcibly);
				p.destroyForcibly();
			}
			for (var f : temps)
				Files.deleteIfExists(f);
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	/** The bytes of one of {@link #BROWSER_ASSETS}. */
	static byte[] asset(String name) throws IOException {
		var path = switch (name) {
			case "xterm.js" -> WebJarResolver.WEBJARS_ROOT + WebJarResolver.resolvePath(TerminalMixin.XTERM_JS_ASSET);
			case "xterm.css" -> WebJarResolver.WEBJARS_ROOT + WebJarResolver.resolvePath(TerminalMixin.XTERM_CSS_ASSET);
			case "juneau-terminal.js" -> TerminalEndpoints.JS_RESOURCE.substring(1);
			case "juneau-terminal.css" -> TerminalEndpoints.CSS_RESOURCE.substring(1);
			default -> throw new IllegalArgumentException(name);
		};
		try (var in = TerminalHarness.class.getClassLoader().getResourceAsStream(path)) {
			assertNotNull(in, () -> "missing classpath resource: " + path);
			return in.readAllBytes();
		}
	}

	/** {@code src/test/js/<name>} under the module, from {@code basedir} or the working directory. */
	static Path locate(String name) {
		var basedir = System.getProperty("basedir");
		for (var p : List.of(Path.of(basedir == null ? "." : basedir, "src/test/js", name), Path.of("juneau-rest/juneau-rest-server-terminal/src/test/js", name)))
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		throw new AssertionError("cannot find src/test/js/" + name);
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (! p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	static String quietRead(Path p) {
		try {
			return Files.readString(p, UTF_8);
		} catch (IOException e) {
			return "(unreadable: " + e.getMessage() + ")";
		}
	}
}
