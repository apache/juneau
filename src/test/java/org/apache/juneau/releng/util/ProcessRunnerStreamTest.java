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

package org.apache.juneau.releng.util;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessRunnerStreamTest {

	@Test
	void a01_streamsEachLineToSinkAndReturnsExitCode() {
		var runner = new ProcessRunner.Default();
		var lines = new ArrayList<String>();
		var res = runner.run(List.of("sh", "-c", "printf 'a\\nb\\nc\\n'"), null, null, lines::add);
		assertEquals(0, res.exitCode());
		assertEquals(List.of("a", "b", "c"), lines);
		assertTrue(res.output().contains("a"));
	}

	@Test
	void a02_nonZeroExitStillStreamsAndReports() {
		var runner = new ProcessRunner.Default();
		var lines = new ArrayList<String>();
		var res = runner.run(List.of("sh", "-c", "echo hi; exit 7"), null, null, lines::add);
		assertEquals(7, res.exitCode());
		assertFalse(res.ok());
		assertEquals(List.of("hi"), lines);
	}
}
