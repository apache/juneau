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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * Fails when a bundled stylesheet or script references a {@code --jc-*} custom property that nothing defines.
 *
 * <p>
 * A typo in {@code var(--jc-...)} otherwise degrades silently to the fallback (or to nothing) at runtime.
 * A name counts as defined when a bundled stylesheet declares it, a theme token ({@link Theme#OPEN}) or chrome role alias ({@link ConsoleChromeMixin#roleAliasNames()}) provides it,
 * or a script stamps it with {@code style.setProperty}. A use is checked from {@code var(...)} in CSS and JS and from
 * {@code getPropertyValue(...)} in JS.
 */
class JcVar_Lint_Test extends TestBase {

	/**
	 * Modules whose bundled {@code .css} and {@code .js} are scanned, relative to this module's base directory.
	 * Read from the source trees (not the classpath) so a module this one does not depend on is still covered;
	 * Maven and Eclipse both run tests with the module directory as the working directory.
	 */
	private static final List<String> MODULES = List.of(
		"../juneau-console/src/main/resources",
		"src/main/resources",
		"../juneau-console-widgets/src/main/resources",
		"../juneau-terminal-region/src/main/resources");

	/**
	 * Names used only as {@code var(--jc-x, fallback)} override hooks: no bundled sheet defines them, an embedding
	 * application may.  Each entry needs a reason.  Currently empty.
	 */
	private static final Set<String> FALLBACK_ONLY = Set.of();

	/** Third-party and generated files that are not ours to lint. */
	private static boolean vendored(Path p) {
		var n = p.getFileName().toString();
		return n.endsWith(".min.js") || n.endsWith(".min.css") || p.toString().contains("/vendor/") || p.toString().contains("/META-INF/");
	}

	private static List<Path> resources() throws IOException {
		var base = Path.of(System.getProperty("basedir", "")).toAbsolutePath();
		var files = new ArrayList<Path>();
		for (var m : MODULES) {
			var dir = base.resolve(m).normalize();
			assertTrue(Files.isDirectory(dir), dir + " not found; run from the juneau-console-views module directory");
			try (var s = Files.walk(dir)) {
				s.filter(p -> p.toString().endsWith(".css") || p.toString().endsWith(".js"))
					.filter(p -> ! vendored(p))
					.forEach(files::add);
			}
		}
		assertTrue(files.size() > 30, "scan found only " + files.size() + " files; the directory scan has probably stopped matching");
		return files;
	}

	private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
	private static final Pattern LINE_COMMENT = Pattern.compile("(?m)^\\s*//.*$");
	private static final Pattern DECLARATION = Pattern.compile("(--jc-[a-z0-9-]+)\\s*:");
	private static final Pattern SET_PROPERTY = Pattern.compile("setProperty\\(\\s*[\"'](--jc-[a-z0-9-]+)[\"']");
	private static final Pattern VAR_USE = Pattern.compile("var\\(\\s*(--jc-[a-z0-9-]+)");
	private static final Pattern GET_PROPERTY = Pattern.compile("getPropertyValue\\(\\s*[\"'](--jc-[a-z0-9-]+)[\"']");

	private static String read(Path path) throws IOException {
		var s = Files.readString(path, StandardCharsets.UTF_8);
		s = BLOCK_COMMENT.matcher(s).replaceAll("");
		return path.toString().endsWith(".js") ? LINE_COMMENT.matcher(s).replaceAll("") : s;
	}

	private static Set<String> find(Pattern p, String text) {
		var names = new TreeSet<String>();
		var m = p.matcher(text);
		while (m.find())
			names.add(m.group(1));
		return names;
	}

	private static TreeSet<String> definedByTheme() {
		var names = new TreeSet<String>(Theme.OPEN.getTokens().keySet());
		names.addAll(ConsoleChromeMixin.roleAliasNames());
		return names;
	}

	@Test void a01_everyUsedNameIsDefined() throws IOException {
		var defined = definedByTheme();
		var used = new TreeMap<String,Path>();
		for (var path : resources()) {
			var text = read(path);
			defined.addAll(find(DECLARATION, text));
			defined.addAll(find(SET_PROPERTY, text));
			for (var n : find(VAR_USE, text))
				used.putIfAbsent(n, path);
			for (var n : find(GET_PROPERTY, text))
				used.putIfAbsent(n, path);
		}
		var undefined = used.entrySet().stream()
			.filter(e -> ! defined.contains(e.getKey()) && ! FALLBACK_ONLY.contains(e.getKey()))
			.map(e -> e.getKey() + " (" + e.getValue() + ")")
			.toList();
		assertTrue(undefined.isEmpty(), "Undefined --jc-* custom properties: " + undefined);
	}

	@Test void a02_fallbackAllowlistHasNoStaleEntries() throws IOException {
		var defined = definedByTheme();
		var used = new TreeSet<String>();
		for (var path : resources()) {
			var text = read(path);
			defined.addAll(find(DECLARATION, text));
			defined.addAll(find(SET_PROPERTY, text));
			used.addAll(find(VAR_USE, text));
		}
		for (var n : FALLBACK_ONLY) {
			assertTrue(used.contains(n), n + " is allowlisted but no longer used; remove it");
			assertFalse(defined.contains(n), n + " is now defined; remove it from the allowlist");
		}
	}
}
