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
import java.nio.file.attribute.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.*;

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
 * The scan lists files with {@code git ls-files --cached --others --exclude-standard} from this module's parent directory
 * (so gitignored files such as {@code __pycache__} are never read), falling back to a directory walk when git is not
 * available; it considers only files below a
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

		var offenders = scan(root, candidateFiles(root));
		assertTrue(offenders.isEmpty(), () -> "Tracker ids found in source trees (state what the code does instead of citing a work item): " + offenders.stream().collect(Collectors.joining("\n  ", "\n  ", "")));
	}

	@Test void a02_gitListingSkipsIgnoredFilesButKeepsUntrackedOnes(@TempDir Path dir) throws Exception {
		if (! git(dir, "init", "-q"))
			return;  // git not available; the walk fallback is covered by a03
		var id = "WO" + "RK-J0001";
		Files.writeString(dir.resolve(".gitignore"), "ignored/\n");
		var ignored = Files.createDirectories(dir.resolve("ignored/src/main"));
		Files.writeString(ignored.resolve("A.pyc"), id);
		var untracked = Files.createDirectories(dir.resolve("m/src/test"));
		Files.writeString(untracked.resolve("B.txt"), id);

		var listed = gitFiles(dir);
		assertNotNull(listed);
		assertTrue(listed.contains(dir.resolve("m/src/test/B.txt")), "untracked, non-ignored file must be listed");
		assertFalse(listed.contains(dir.resolve("ignored/src/main/A.pyc")), "ignored file must not be listed");
		assertEquals(1, scan(dir, listed).size());
	}

	@Test void a03_walkFallbackFindsIds(@TempDir Path dir) throws Exception {
		var src = Files.createDirectories(dir.resolve("m/src/main"));
		Files.writeString(src.resolve("C.txt"), "WO" + "RK-J0001");
		Files.writeString(Files.createDirectories(dir.resolve("m/target/src/main")).resolve("D.txt"), "WO" + "RK-J0001");
		var found = scan(dir, walkFiles(dir));
		assertEquals(1, found.size());
		assertTrue(found.get(0).startsWith("m/src/main/C.txt"), found.get(0));
	}

	/** Files to scan: tracked plus untracked-but-not-ignored when git is usable, otherwise a plain directory walk. */
	private static List<Path> candidateFiles(Path root) throws IOException {
		var listed = gitFiles(root);
		return listed != null ? listed : walkFiles(root);
	}

	/** Returns the files git would track (cached plus untracked, excluding ignored), or null when git cannot answer. */
	private static List<Path> gitFiles(Path root) {
		try {
			var pb = new ProcessBuilder("git", "ls-files", "-z", "--cached", "--others", "--exclude-standard").directory(root.toFile()).redirectErrorStream(false);
			pb.redirectError(ProcessBuilder.Redirect.DISCARD);
			var proc = pb.start();
			var out = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
			if (proc.waitFor() != 0)
				return null;
			var result = new ArrayList<Path>();
			for (var name : out.split("\0"))
				if (! name.isEmpty())
					result.add(root.resolve(name));
			return result;
		} catch (IOException e) {
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}

	private static boolean git(Path dir, String...args) throws Exception {
		var cmd = new ArrayList<String>();
		cmd.add("git");
		cmd.addAll(List.of(args));
		try {
			var pb = new ProcessBuilder(cmd).directory(dir.toFile());
			pb.redirectErrorStream(true);
			var proc = pb.start();
			proc.getInputStream().readAllBytes();
			return proc.waitFor() == 0;
		} catch (IOException e) {
			return false;
		}
	}

	private static List<Path> walkFiles(Path root) throws IOException {
		var files = new ArrayList<Path>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				return SKIPPED_DIRS.contains(dir.getFileName().toString()) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
			}
			@Override public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
				if (attrs.isRegularFile())
					files.add(file);
				return FileVisitResult.CONTINUE;
			}
		});
		return files;
	}

	/** Scans the source-tree files among the candidates; returns sorted "relative-path (id)" entries. */
	private static List<String> scan(Path root, List<Path> candidates) throws IOException {
		var offenders = new ArrayList<String>();
		for (var file : candidates) {
			var rel = root.relativize(file);
			if (isInSourceTree(rel) && Files.isRegularFile(file) && Files.size(file) <= MAX_BYTES && ! hasSkippedDir(rel)) {
				var m = TRACKER_ID.matcher(Files.readString(file, StandardCharsets.ISO_8859_1));
				if (m.find())
					offenders.add(rel + " (" + m.group() + ")");
			}
		}
		offenders.sort(null);
		return offenders;
	}

	private static boolean hasSkippedDir(Path relative) {
		for (var i = 0; i < relative.getNameCount() - 1; i++)
			if (SKIPPED_DIRS.contains(relative.getName(i).toString()))
				return true;
		return false;
	}

	/** True when the repo-relative path has a {@code src/main} or {@code src/test} segment pair. */
	private static boolean isInSourceTree(Path relative) {
		for (var i = 0; i < relative.getNameCount() - 1; i++)
			if (relative.getName(i).toString().equals("src") && Set.of("main", "test").contains(relative.getName(i + 1).toString()))
				return true;
		return false;
	}
}
