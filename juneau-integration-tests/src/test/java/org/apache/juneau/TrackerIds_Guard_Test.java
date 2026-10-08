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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.junit.jupiter.api.*;

/**
 * Build-wide guard: no work-tracker id may appear in any {@code src/main} or {@code src/test} tree of this repository.
 *
 * <p>
 * Tracker ids are letter-plus-four-digit references into a private work board (a project prefix, a dash, a one-letter
 * project code, then the number, with an optional lettered-child suffix) and a legacy form that used a different
 * prefix.  They mean nothing to a reader of the public source and rot as soon as the board is reorganised, so code,
 * tests, resources and in-code docs say what they mean instead of citing the item.
 *
 * <p>
 * The scan walks the repository from this module's parent directory, considers only files below a
 * {@code src/main} or {@code src/test} directory, and skips build output ({@code target}), {@code node_modules} and
 * {@code .git}.  Files are read as ISO-8859-1 so binary resources cannot raise a decoding error; the pattern is pure
 * ASCII.  There are currently no historical-record files in these trees that need exempting.
 */
class TrackerIds_Guard_Test extends TestBase {

	/** The prefixes and shape are assembled from segments so this guard's own source is not a hit. */
	private static final Pattern TRACKER_ID = Pattern.compile("(?:" + String.join("|", "WO" + "RK", "TO" + "DO") + ")-[A-Z][0-9]{4}[a-z]?");

	private static final Set<String> SKIPPED_DIRS = Set.of("target", "node_modules", ".git");

	/** Files larger than this are not source text and are not scanned. */
	private static final long MAX_BYTES = 8L * 1024 * 1024;

	@Test void a01_noTrackerIdsInSourceTrees() throws IOException {
		var root = Path.of("..").toAbsolutePath().normalize();
		assertTrue(Files.isDirectory(root.resolve("juneau-core")), () -> "Repository root not found at '" + root + "'; the test expects the module basedir as its working directory.");

		var offenders = new ArrayList<String>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override public FileVisitResult preVisitDirectory(Path dir, java.nio.file.attribute.BasicFileAttributes attrs) {
				return SKIPPED_DIRS.contains(dir.getFileName().toString()) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
			}
			@Override public FileVisitResult visitFile(Path file, java.nio.file.attribute.BasicFileAttributes attrs) throws IOException {
				if (attrs.isRegularFile() && attrs.size() <= MAX_BYTES && isInSourceTree(root.relativize(file))) {
					var m = TRACKER_ID.matcher(Files.readString(file, StandardCharsets.ISO_8859_1));
					if (m.find())
						offenders.add(root.relativize(file) + " (" + m.group() + ")");
				}
				return FileVisitResult.CONTINUE;
			}
		});

		Collections.sort(offenders);
		assertTrue(offenders.isEmpty(), () -> "Tracker ids found in source trees (state what the code does instead of citing a work item): " + offenders.stream().collect(Collectors.joining("\n  ", "\n  ", "")));
	}

	/** True when the repo-relative path has a {@code src/main} or {@code src/test} segment pair. */
	private static boolean isInSourceTree(Path relative) {
		for (var i = 0; i < relative.getNameCount() - 1; i++)
			if (relative.getName(i).toString().equals("src") && Set.of("main", "test").contains(relative.getName(i + 1).toString()))
				return true;
		return false;
	}
}
