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
package org.apache.juneau.rest.server.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.marshall.marshaller.*;

/**
 * Generic Node plumbing for the plain-Node behavioral harnesses under {@code src/test/js}.
 *
 * <p>
 * Each harness is run as {@code node <harness> <asset-file>...}, where every asset is a classpath resource copied
 * to a temp file, and must print exactly one JSON object to stdout.  Reports are cached per harness-and-assets.
 * A machine without Node (or without the harness on any known path) yields {@code null}, which callers turn into a
 * skip.
 */
final class NodeHarness {

	private static final Map<String,Map<?,?>> CACHE = new ConcurrentHashMap<>();
	private static final Set<String> UNAVAILABLE = ConcurrentHashMap.newKeySet();

	private NodeHarness() {}

	/**
	 * Runs {@code harnessName} against the given classpath resources (resolved relative to {@link ViewsMixin}) and
	 * returns its parsed report, or {@code null} when it cannot run on this machine.
	 */
	static Map<?,?> report(String harnessName, String...resources) {
		var key = harnessName + Arrays.toString(resources);
		if (UNAVAILABLE.contains(key))
			return null;
		var cached = CACHE.get(key);
		if (cached != null)
			return cached;
		try {
			var harness = nodeAvailable() ? locate(harnessName) : null;
			if (harness == null) {
				UNAVAILABLE.add(key);
				return null;
			}
			var report = Json.to(run(harness, harnessName, resources), Map.class);
			CACHE.put(key, report);
			return report;
		} catch (Exception e) {
			throw new AssertionError("could not run " + harnessName + ": " + e, e);
		}
	}

	private static String run(Path harness, String harnessName, String[] resources) throws Exception {
		var temps = new ArrayList<Path>();
		var stdout = Files.createTempFile("node-harness-stdout-", ".json");
		var stderr = Files.createTempFile("node-harness-stderr-", ".txt");
		try {
			var args = new ArrayList<String>(List.of("node", harness.toString()));
			for (var r : resources) {
				var f = Files.createTempFile("node-harness-asset-", ".js");
				temps.add(f);
				try (var in = ViewsMixin.class.getResourceAsStream(r)) {
					assertNotNull(in, () -> "missing classpath resource: " + r);
					Files.writeString(f, new String(in.readAllBytes(), UTF_8), UTF_8);
				}
				args.add(f.toString());
			}
			var p = new ProcessBuilder(args).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(harnessName + " did not finish within 60s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(harnessName + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr) + "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			for (var f : temps)
				Files.deleteIfExists(f);
			Files.deleteIfExists(stdout);
			Files.deleteIfExists(stderr);
		}
	}

	private static boolean nodeAvailable() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			if (!p.waitFor(5, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				return false;
			}
			return p.exitValue() == 0;
		} catch (Exception e) {
			return false;
		}
	}

	private static Path locate(String harnessName) {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/" + harnessName);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of("src/test/js/" + harnessName, "juneau-rest/juneau-rest-server-views/src/test/js/" + harnessName)) {
			var p = Path.of(rel);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		return null;
	}

	private static String quietRead(Path p) {
		try { return Files.readString(p, UTF_8); }
		catch (IOException e) { return "(unreadable: " + e.getMessage() + ")"; }
	}
}
