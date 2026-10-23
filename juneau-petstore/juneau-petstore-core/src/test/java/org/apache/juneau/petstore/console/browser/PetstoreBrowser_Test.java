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
package org.apache.juneau.petstore.console.browser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Pins the formatting of {@link PetstoreBrowser#slowest}, which needs no browser.
 */
class PetstoreBrowser_Test {

	@Test void a01_slowestListsTheLongestCasesFirst() {
		var report = new LinkedHashMap<String,Map<String,Object>>();
		report.put("fast", Map.of("ms", 40));
		report.put("slow", Map.of("ms", 9100));
		report.put("mid", Map.of("ms", 800));
		assertEquals("slow 9100 ms, mid 800 ms", PetstoreBrowser.slowest(report, 2));
	}

	@Test void a02_slowestIgnoresCasesWithoutMs() {
		var report = Map.<String,Map<String,Object>>of("a", Map.of("status", 200), "b", Map.of("ms", 5));
		assertEquals("b 5 ms", PetstoreBrowser.slowest(report, 5));
	}
}
