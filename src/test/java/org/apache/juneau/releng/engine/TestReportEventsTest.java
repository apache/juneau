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

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestReportEventsTest {

	private static final String XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<testsuite name="org.example.FooTest" tests="1" failures="0" errors="0" skipped="0" time="0.1">
		  <testcase name="passes" classname="org.example.FooTest" time="0.1"/>
		</testsuite>
		""";

	private static Path report(Path root, String module, String dirName) throws Exception {
		var dir = root.resolve(module).resolve("target").resolve(dirName);
		Files.createDirectories(dir);
		return Files.writeString(dir.resolve("TEST-org.example.FooTest.xml"), XML);
	}

	@Test
	void a01_aFreshReportBecomesTestEventsForTheStep(@TempDir Path root) throws Exception {
		report(root, "core", "surefire-reports");
		var events = TestReportEvents.collect(root, Instant.now().minusSeconds(60), "build");
		assertTrue(events.stream().anyMatch(e -> "test".equals(e.toContractMap().get("ev"))), events.toString());
	}

	@Test
	void a02_failsafeReportsAreReadToo(@TempDir Path root) throws Exception {
		report(root, "it", "failsafe-reports");
		assertFalse(TestReportEvents.collect(root, Instant.now().minusSeconds(60), "build").isEmpty());
	}

	@Test
	void a03_aReportOlderThanTheStepIsIgnored(@TempDir Path root) throws Exception {
		Files.setLastModifiedTime(report(root, "core", "surefire-reports"), FileTime.fromMillis(1_000));
		assertTrue(TestReportEvents.collect(root, Instant.now(), "build").isEmpty());
	}

	@Test
	void a04_noReportsOrNoDirectoryIsEmpty(@TempDir Path root) {
		assertTrue(TestReportEvents.collect(root, Instant.now(), "build").isEmpty());
		assertTrue(TestReportEvents.collect(root.resolve("missing"), Instant.now(), "build").isEmpty());
	}

	@Test
	void a05_reportsUnderClassesAreNotWalked(@TempDir Path root) throws Exception {
		var dir = root.resolve("core/target/classes/surefire-reports");
		Files.createDirectories(dir);
		Files.writeString(dir.resolve("TEST-x.xml"), XML);
		assertTrue(TestReportEvents.collect(root, Instant.now().minusSeconds(60), "build").isEmpty());
	}
}
