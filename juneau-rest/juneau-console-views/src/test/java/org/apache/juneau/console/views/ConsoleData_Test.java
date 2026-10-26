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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The data-source loader in {@code juneau-data.js} under the real bus: config and lookup, URLs and fetch, the parsers,
 * the columnar builder, chunk lists, the cache, DataSource, Derived and boot.  Every check lives in {@code data.cjs};
 * each section returns {@code {check: true | {actual, expected}}}.  Runs when node is on PATH.
 */
class ConsoleData_Test extends TestBase {

	private static Map<?,?> report;

	private static synchronized Map<?,?> report() {
		if (report == null)
			report = NodeHarness.report("data.cjs", ViewsMixin.BUS_JS_RESOURCE, ViewsMixin.DATA_JS_RESOURCE);
		assumeTrue(report != null, "node (or data.cjs) not available - JS layer skipped");
		return report;
	}

	// Every check in the section is true; a failure prints each failed check's {actual, expected}.
	private static void assertSection(String key) {
		var s = (Map<?,?>)report().get(key);
		assertNotNull(s, () -> "section '" + key + "' missing from the harness output");
		assertNull(s.get("$error"), () -> "section '" + key + "' crashed: " + s.get("$error"));
		assertFalse(s.isEmpty(), () -> "section '" + key + "' has no checks");
		var failed = s.entrySet().stream().filter(e -> ! Boolean.TRUE.equals(e.getValue()))
			.collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()), (a, b) -> a,
			TreeMap::new));
		assertEquals(Map.of(), failed, () -> "section '" + key + "' failed checks: " + failed);
	}

	@Test void a01_config_messagesAndLookup() { assertSection("a_config"); }
	@Test void a02_urls_tokensQueryAndFetch() { assertSection("c_urls"); }
	@Test void a03_parsers_csvNdjsonJson() { assertSection("d_parsers"); }
	@Test void a04_frames_builderConcatAndAggregateContract() { assertSection("e_frames"); }
	@Test void a05_chunks_unitsAndOpenChunk() { assertSection("f_chunks"); }
	@Test void a06_cache_idbMemoryEpochAndFallback() { assertSection("g_cache"); }
	@Test void a07_source_generationsCacheAndAuto() { assertSection("h_source"); }
	@Test void a08_derived_transformsAndMirroring() { assertSection("i_derived"); }
	@Test void a09_boot_sourcesDerivedAndPartial() { assertSection("j_boot"); }
}
