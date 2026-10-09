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
 * Shared Node plumbing for the {@code juneau-bus.js} harnesses ({@code bus-*.cjs}).
 *
 * <p>
 * Same contract as {@link RegionsHarness}, but the asset list is the caller's: each classpath resource is copied to a
 * temp file and passed as one argv path, in order, so a harness that also needs {@code juneau-regions.js} or the
 * console shell names it explicitly.  The JSON report is cached per (harness, assets) for the JVM.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>var</jk> <jv>report</jv> = BusHarness.<jsm>run</jsm>(<js>"bus-core.cjs"</js>, ViewsMixin.<jsf>BUS_JS_RESOURCE</jsf>);
 * 	<jsm>assumeTrue</jsm>(<jv>report</jv> != <jk>null</jk>, <js>"node not on PATH"</js>);
 * </p>
 */
final class BusHarness {

	private static final Map<String,Map<String,Object>> CACHE = new ConcurrentHashMap<>();
	private static final Set<String> UNAVAILABLE = ConcurrentHashMap.newKeySet();

	private BusHarness() {}

	/**
	 * Runs {@code src/test/js/<cjsFileName>} with the given classpath resources as argv paths and returns its JSON
	 * report, or null when this machine cannot run it (no Node, or the script is not on any path a Maven or IDE
	 * working directory produces).  A harness that RAN and failed is an assertion failure, never a null.
	 */
	@SuppressWarnings("unchecked")
	static Map<String,Object> run(String cjsFileName, String... classpathResources) {
		var key = cjsFileName + ":" + String.join(",", classpathResources);
		if (UNAVAILABLE.contains(key))
			return null;
		var cached = CACHE.get(key);
		if (cached != null)
			return cached;
		try {
			if (!nodeAvailable()) {
				UNAVAILABLE.add(key);
				return null;
			}
			var harness = locate(cjsFileName);
			if (harness == null) {
				UNAVAILABLE.add(key);
				return null;
			}
			var report = (Map<String,Object>) Json.to(exec(harness, cjsFileName, classpathResources), Map.class);
			CACHE.put(key, report);
			return report;
		} catch (Exception e) {
			throw new AssertionError("could not run " + cjsFileName + ": " + e, e);
		}
	}

	/** Reads a classpath asset's source, for the source-shape assertions a driver opens with. */
	static String source(String resource) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, resource);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	private static String exec(Path harness, String name, String[] resources) throws Exception {
		var temps = new ArrayList<Path>();
		var stdout = Files.createTempFile("bus-stdout-", ".json");
		var stderr = Files.createTempFile("bus-stderr-", ".txt");
		try {
			var args = new ArrayList<String>(List.of("node", harness.toString()));
			for (var r : resources) {
				var base = r.substring(r.lastIndexOf('/') + 1).replace(".js", "");
				var f = Files.createTempFile(base + "-", ".js");
				temps.add(f);
				Files.writeString(f, source(r), UTF_8);
				args.add(f.toString());
			}
			var p = new ProcessBuilder(args).redirectOutput(stdout.toFile()).redirectError(stderr.toFile()).start();
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(name + " did not finish within 60s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(name + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr) + "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			temps.add(stdout);
			temps.add(stderr);
			for (var f : temps)
				Files.deleteIfExists(f);
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

	private static Path locate(String name) {
		var basedir = System.getProperty("basedir");
		if (basedir != null) {
			var p = Path.of(basedir, "src/test/js/" + name);
			if (Files.isRegularFile(p))
				return p.toAbsolutePath().normalize();
		}
		for (var rel : List.of("src/test/js/" + name, "juneau-rest/juneau-rest-server-views/src/test/js/" + name)) {
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
