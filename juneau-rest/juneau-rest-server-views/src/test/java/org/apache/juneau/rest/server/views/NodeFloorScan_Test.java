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
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.*;

import org.junit.jupiter.api.*;

/**
 * Static backstop for the Node floor (see {@code juneau.node.floor} in the root pom): fails when a shipped script
 * ({@code src/main/resources/**}/*.js) or a Node harness ({@code src/test/js/**}/*.cjs) in any module calls a builtin
 * that is newer than the floor.
 *
 * <p>
 * The harness runs themselves already strip these builtins through {@code src/test/js/node-floor-preload.cjs}; this
 * scan additionally covers shipped scripts that no Node harness exercises, and pins the same list in one readable
 * place.  Keep it in step with the preload.
 *
 * <p>
 * A call that is genuinely safe (for example a method of the same name on an unrelated object) is exempted by putting
 * {@code node-floor-ok} and a reason in a comment on the same line.
 */
class NodeFloorScan_Test {

	private static final String ALLOW_MARKER = "node-floor-ok";

	/**
	 * Method calls, matched as {@code .name(} so a local variable, property or keyword of the same name is not a hit.
	 */
	private static final Pattern METHOD_CALL = Pattern.compile(
		"\\.(toSorted|toReversed|toSpliced|with|isWellFormed|toWellFormed|transfer|transferToFixedLength"
		+ "|union|intersection|difference|symmetricDifference|isSubsetOf|isSupersetOf|isDisjointFrom)\\s*\\(");

	/** Static builtins, matched as {@code Owner.name} followed by a call or a reference. */
	private static final Pattern STATIC_BUILTIN = Pattern.compile(
		"\\b(Object\\.groupBy|Map\\.groupBy|Promise\\.withResolvers|Promise\\.try|Array\\.fromAsync|RegExp\\.escape|Error\\.isError)\\b");

	/** A line comment: {@code //} at the start of the line or after whitespace (so {@code http://} is left alone). */
	private static final Pattern LINE_COMMENT = Pattern.compile("(^|\\s)//.*$");

	private static final Set<String> SKIPPED_DIRS = Set.of("target", "node_modules", ".git", "vendor");

	/** Logs the build agent's Node version once, so the real floor can be read off the CI log; never fails the test. */
	private static void logNodeVersion() {
		try {
			var p = new ProcessBuilder("node", "--version").redirectErrorStream(true).start();
			var out = new String(p.getInputStream().readAllBytes(), UTF_8).trim();
			p.waitFor(10, java.util.concurrent.TimeUnit.SECONDS);
			System.out.println("[NodeFloorScan] node --version: " + out);
		} catch (Exception e) {
			System.out.println("[NodeFloorScan] node --version unavailable: " + e);
		}
	}

	@Test void a01_noBuiltinsNewerThanTheNodeFloor() throws IOException {
		logNodeVersion();
		var root = repoRoot();
		assumeTrue(root != null, "repository root not found");
		var hits = new ArrayList<String>();
		var scanned = new int[1];
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				// Never enter target/ and friends: sibling forks create and delete temp files there while this scan runs.
				var name = dir.getFileName();
				return !dir.equals(root) && name != null && SKIPPED_DIRS.contains(name.toString()) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path f, BasicFileAttributes attrs) {
				if (isScanned(f)) {
					scanned[0]++;
					scan(root, f, hits);
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path f, IOException e) throws IOException {
				if (e instanceof NoSuchFileException)
					return FileVisitResult.CONTINUE;
				throw e;
			}
		});
		assertTrue(scanned[0] > 0, "no scripts found under " + root);
		assertTrue(hits.isEmpty(), () -> "Builtins newer than the Node floor (the CI Node lacks them; use sort()/slice().sort() style equivalents, "
			+ "or add a 'node-floor-ok: <reason>' comment on the line):\n  " + String.join("\n  ", hits));
	}

	@Test void a02_patternsMatchOnlyCalls() {
		assertTrue(METHOD_CALL.matcher("urls.toSorted()").find());
		assertTrue(METHOD_CALL.matcher("xs.with (1, 2)").find());
		assertTrue(METHOD_CALL.matcher("a.union(b)").find());
		assertFalse(METHOD_CALL.matcher("with (obj) { x }").find());
		assertFalse(METHOD_CALL.matcher("const with = 1; o.with = 2;").find());
		assertFalse(METHOD_CALL.matcher("o.difference").find());
		assertTrue(STATIC_BUILTIN.matcher("Object.groupBy(rows, f)").find());
		assertFalse(STATIC_BUILTIN.matcher("groupBy: ['a']").find());
		assertFalse(STATIC_BUILTIN.matcher("function groupByTransform(frame)").find());
	}

	private static boolean isScanned(Path f) {
		if (!Files.isRegularFile(f))
			return false;
		var s = f.toString().replace('\\', '/');
		for (var d : SKIPPED_DIRS)
			if (s.contains("/" + d + "/"))
				return false;
		if (s.endsWith(".min.js") || s.endsWith("/node-floor-preload.cjs"))  // the preload names the builtins it removes
			return false;
		return (s.endsWith(".js") && s.contains("/src/main/resources/")) || (s.endsWith(".cjs") && s.contains("/src/test/js/"));
	}

	private static void scan(Path root, Path f, List<String> hits) {
		try {
			var lines = Files.readAllLines(f, UTF_8);
			for (var i = 0; i < lines.size(); i++) {
				var line = lines.get(i);
				if (line.contains(ALLOW_MARKER))
					continue;
				var code = LINE_COMMENT.matcher(line).replaceFirst("");
				var m = METHOD_CALL.matcher(code);
				var s = STATIC_BUILTIN.matcher(code);
				if (m.find() || s.find())
					hits.add(root.relativize(f) + ":" + (i + 1) + ": " + line.trim());
			}
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/** The directory holding the root pom: the nearest ancestor of the module directory that has a {@code juneau-core} sibling module. */
	private static Path repoRoot() {
		var basedir = System.getProperty("basedir");
		var p = Path.of(basedir == null ? "." : basedir).toAbsolutePath().normalize();
		for (; p != null; p = p.getParent())
			if (Files.isDirectory(p.resolve("juneau-core")) && Files.isDirectory(p.resolve("juneau-rest")))
				return p;
		return null;
	}
}
