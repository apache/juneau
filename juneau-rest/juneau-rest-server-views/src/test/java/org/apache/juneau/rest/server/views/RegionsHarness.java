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
 * Shared Node plumbing for the {@code juneau-regions.js} behavioral harnesses.
 *
 * <p>
 * The region runtime's behavioral coverage is split across harness scripts by SUBJECT - the populate primitive,
 * the message bus, the initial-broadcast barrier, the declarative default populator - and each one is driven by its
 * own test class so a failure names the layer that broke.  Splitting the driver plumbing out of those classes is not
 * just deduplication: it means every harness runs the SAME loader against the SAME assets in the same order, so a
 * "passes in one class, fails in another" result can only come from the harness under test.  The declarative default's
 * harness alone also loads {@code juneau-helpers.js} (via {@link #reportWithHelpers}), because the reserved default
 * paints through {@code ctx.helpers[...]} - the other three harnesses deliberately do not carry that dependency.
 *
 * <p>
 * Each harness is spawned once per JVM and its JSON report cached, because the barrier harness alone builds
 * twenty-odd independent pages.
 */
final class RegionsHarness {

	private static final Map<String,Map<?,?>> CACHE = new ConcurrentHashMap<>();
	private static final Set<String> FAILED = ConcurrentHashMap.newKeySet();

	private RegionsHarness() {}

	/**
	 * Runs the named harness and returns its report, or null when this machine cannot run it (no Node, or the script
	 * is not on any of the paths a Maven or IDE working directory produces).  Callers turn a null into a skip.
	 */
	static Map<?,?> report(String harnessName) {
		return reportImpl(harnessName, false);
	}

	/**
	 * Runs the named harness with {@code juneau-helpers.js} ALSO loaded (a fourth argv path), for the declarative
	 * default's own harness - the reserved default paints via {@code ctx.helpers[...]}, so its harness needs the
	 * helper library the three original harnesses (primitive/bus/barrier) deliberately do not load.
	 */
	static Map<?,?> reportWithHelpers(String harnessName) {
		return reportImpl(harnessName, true);
	}

	private static Map<?,?> reportImpl(String harnessName, boolean withHelpers) {
		var cacheKey = (withHelpers ? "helpers:" : "") + harnessName;
		if (FAILED.contains(cacheKey))
			return null;
		var cached = CACHE.get(cacheKey);
		if (cached != null)
			return cached;
		try {
			if (!nodeAvailable())
				return markUnavailable(cacheKey);
			var harness = locate(harnessName);
			if (harness == null)
				return markUnavailable(cacheKey);
			var report = Json.to(run(harness, harnessName, withHelpers), Map.class);
			CACHE.put(cacheKey, report);
			return report;
		} catch (Exception e) {
			// A harness that cannot be READ is a skip; a harness that RAN and failed has already called fail().
			throw new AssertionError("could not run " + harnessName + ": " + e, e);
		}
	}

	private static Map<?,?> markUnavailable(String harnessName) {
		FAILED.add(harnessName);
		return null;
	}

	private static String run(Path harness, String harnessName, boolean withHelpers) throws Exception {
		var renders = Files.createTempFile("juneau-renders-", ".js");
		var views = Files.createTempFile("juneau-views-", ".js");
		var regions = Files.createTempFile("juneau-regions-", ".js");
		var helpers = withHelpers ? Files.createTempFile("juneau-helpers-", ".js") : null;
		var stdout = Files.createTempFile("regions-stdout-", ".json");
		var stderr = Files.createTempFile("regions-stderr-", ".txt");
		try {
			Files.writeString(renders, asset(ViewsMixin.RENDERS_JS_RESOURCE), UTF_8);
			Files.writeString(views, asset(ViewsMixin.VIEWS_JS_RESOURCE), UTF_8);
			Files.writeString(regions, asset(ViewsMixin.REGIONS_JS_RESOURCE), UTF_8);
			var args = new ArrayList<String>(List.of(
				"node", harness.toString(), renders.toString(), views.toString(), regions.toString()));
			if (helpers != null) {
				Files.writeString(helpers, asset(ViewsMixin.HELPERS_JS_RESOURCE), UTF_8);
				args.add(helpers.toString());
			}
			var p = new ProcessBuilder(args)
				.redirectOutput(stdout.toFile())
				.redirectError(stderr.toFile())
				.start();
			if (!p.waitFor(60, TimeUnit.SECONDS)) {
				p.destroyForcibly();
				fail(harnessName + " did not finish within 60s; stderr:\n" + quietRead(stderr));
			}
			if (p.exitValue() != 0)
				fail(harnessName + " exited " + p.exitValue() + "; stderr:\n" + quietRead(stderr)
					+ "\nstdout:\n" + quietRead(stdout));
			return Files.readString(stdout, UTF_8);
		} finally {
			var cleanup = new ArrayList<Path>(List.of(renders, views, regions, stdout, stderr));
			if (helpers != null)
				cleanup.add(helpers);
			for (var f : cleanup)
				Files.deleteIfExists(f);
		}
	}

	private static String asset(String resource) throws IOException {
		try (var in = ViewsMixin.class.getResourceAsStream(resource)) {
			assertNotNull(in, resource);
			return new String(in.readAllBytes(), UTF_8);
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
		for (var rel : List.of(
			"src/test/js/" + harnessName,
			"juneau-rest/juneau-rest-server-views/src/test/js/" + harnessName
		)) {
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

	/** Reads the region runtime's source, for the source-shape assertions each driver opens with. */
	static String regionsJs() throws IOException {
		return asset(ViewsMixin.REGIONS_JS_RESOURCE);
	}
}
