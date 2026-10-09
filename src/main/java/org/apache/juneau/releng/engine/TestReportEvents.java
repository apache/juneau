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

package org.apache.juneau.releng.engine;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.apache.juneau.rest.server.runreport.RunEvent;
import org.apache.juneau.rest.server.runreport.ReportLimits;
import org.apache.juneau.rest.server.runreport.ReportResult;
import org.apache.juneau.rest.server.runreport.ReportTest;
import org.apache.juneau.rest.server.runreport.SurefireReportReader;

/**
 * Turns the Surefire and Failsafe reports that a Maven step left in the staging clone into run-view test events.
 *
 * <p>
 * The staging clone is a multi-module build, so the reports are spread over many {@code surefire-reports} folders. Only
 * report files written since the step started are read, so a report left over from an earlier step is never attributed
 * to this one.
 */
final class TestReportEvents {

	private static final Set<String> REPORT_DIRS = Set.of("surefire-reports", "failsafe-reports");

	// Build output and tool folders that never hold a report folder; skipping them keeps the walk short.
	private static final Set<String> SKIPPED_DIRS = Set.of(".git", "node_modules", "classes", "test-classes", "generated-sources",
		"generated-test-sources", "maven-status", "apidocs", "site", "archive-tmp", "lib");

	private static final int MAX_DEPTH = 10;
	private static final long CLOCK_SLACK_MS = 2000;

	private TestReportEvents() {}

	/**
	 * Reads the reports written under {@code root} since {@code since}.
	 *
	 * @param root The staging clone, or null.
	 * @param since When the step started.
	 * @param stepId The run-view step id the tests belong to.
	 * @return The events (a {@code replace} followed by the tests), or an empty list when no fresh report exists.
	 */
	static List<RunEvent> collect(Path root, Instant since, String stepId) {
		if (root == null || ! Files.isDirectory(root))
			return List.of();
		var c = new Collector(since.toEpochMilli() - CLOCK_SLACK_MS);
		try {
			Files.walkFileTree(root, Set.of(), MAX_DEPTH, new SimpleFileVisitor<>() {
				@Override /* FileVisitor */
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					var name = dir.getFileName() == null ? "" : dir.getFileName().toString();
					if (! dir.equals(root) && SKIPPED_DIRS.contains(name))
						return FileVisitResult.SKIP_SUBTREE;
					if (! REPORT_DIRS.contains(name))
						return FileVisitResult.CONTINUE;
					c.readDir(dir);
					return FileVisitResult.SKIP_SUBTREE;
				}
			});
		} catch (IOException e) {
			c.warnings.add("test reports unreadable: " + e.getClass().getSimpleName());
			c.found = true;
		}
		if (! c.found)
			return List.of();
		if (c.capped)
			c.warnings.add("test reports truncated at " + c.limits.maxTests() + " tests");
		return new ReportResult(c.reader.fw(), c.tests, c.warnings, c.capped, 0).toEvents(stepId);
	}

	/**
	 * The reports read so far, and whether any were found or the test cap was reached.
	 */
	private static final class Collector {
		final ReportLimits limits = ReportLimits.DEFAULT;
		final SurefireReportReader reader = new SurefireReportReader();
		final List<ReportTest> tests = new ArrayList<>();
		final List<String> warnings = new ArrayList<>();
		final long cutoff;
		boolean found;
		boolean capped;

		Collector(long cutoff) {
			this.cutoff = cutoff;
		}

		/** Reads the fresh {@code TEST-*.xml} files in {@code dir}, stopping at the test cap. */
		void readDir(Path dir) throws IOException {
			try (var files = Files.newDirectoryStream(dir, "TEST-*.xml")) {
				for (var f : files)
					if (Files.getLastModifiedTime(f).toMillis() >= cutoff && ! read(f))
						break;
			}
		}

		/** Reads one report; returns false once the test cap is reached. */
		private boolean read(Path f) {
			found = true;
			if (tests.size() >= limits.maxTests()) {
				capped = true;
				return false;
			}
			var r = reader.read(f, limits);
			tests.addAll(r.tests());
			warnings.addAll(r.warnings());
			return true;
		}
	}
}
