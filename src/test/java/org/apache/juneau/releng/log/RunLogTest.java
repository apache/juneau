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

package org.apache.juneau.releng.log;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RunLogTest {

	// Per-step naming: "<version>-RC<n>-<stepId>.log". RunLog itself is path-agnostic — the naming
	// convention is applied by the caller when constructing one RunLog per step.

	@Test
	void appendsToDiskAndFansOutToBroadcaster(@TempDir Path dir) throws Exception {
		var bc = new LogBroadcaster();
		var got = new ArrayList<String>();
		try (var subscription = bc.subscribe(got::add)) {
			var log = new RunLog(dir.resolve("9.2.1-RC1-preflight.log"), bc);

			log.append("line one");
			log.append("line two");

			var onDisk = Files.readString(dir.resolve("9.2.1-RC1-preflight.log"));
			assertEquals("line one\nline two\n", onDisk);
			assertEquals(List.of("line one", "line two"), got);
		}
	}

	@Test
	void sizeReportsCurrentByteOffset(@TempDir Path dir) {
		var log = new RunLog(dir.resolve("9.2.1-RC1-preflight.log"), new LogBroadcaster());
		log.append("abc");
		assertEquals(4, log.size()); // "abc\n"
	}

	@Test
	void lineSinkFeedsBothDiskAndBroadcaster(@TempDir Path dir) throws Exception {
		var bc = new LogBroadcaster();
		var got = new ArrayList<String>();
		try (var subscription = bc.subscribe(got::add)) {
			var log = new RunLog(dir.resolve("9.2.1-RC1-preflight.log"), bc);
			log.lineSink().accept("x");
			assertEquals("x\n", Files.readString(dir.resolve("9.2.1-RC1-preflight.log")));
			assertEquals(List.of("x"), got);
		}
	}

	@Test
	void resetTruncatesOnDiskContentForInPlaceOverwrite(@TempDir Path dir) throws Exception {
		// An ad-hoc re-run overwrites the step's log from scratch.
		var log = new RunLog(dir.resolve("9.2.1-RC1-release-prepare.log"), new LogBroadcaster());
		log.append("first invocation, line 1");
		log.append("first invocation, line 2");

		log.reset();
		log.append("second invocation, line 1");

		assertEquals("second invocation, line 1\n", Files.readString(dir.resolve("9.2.1-RC1-release-prepare.log")));
	}

	@Test
	void resetOnANeverWrittenLogIsANoOp(@TempDir Path dir) {
		// A step's first-ever invocation also calls reset() before appending; must not fail
		// just because the file doesn't exist yet.
		var log = new RunLog(dir.resolve("9.2.1-RC1-build-verify.log"), new LogBroadcaster());
		assertDoesNotThrow(log::reset);
		log.append("first line ever");
		assertEquals("first line ever\n", contentsOrEmpty(dir.resolve("9.2.1-RC1-build-verify.log")));
	}

	private static String contentsOrEmpty(Path p) {
		try {
			return Files.readString(p);
		} catch (Exception e) {
			return "";
		}
	}
}
