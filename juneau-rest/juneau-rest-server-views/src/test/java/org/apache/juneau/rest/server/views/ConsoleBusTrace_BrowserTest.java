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

import static org.apache.juneau.rest.server.views.ConsoleBusBrowserSupport.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;

/**
 * The bus trace flag in real Chromium (spec 6.4): {@code ?juneau-bus-trace} logs each publish as a {@code [bus]} console.debug line, and the
 * history ring records the master/detail traffic, including a suppressed unchanged publish, with or without the flag.
 */
@EnabledIfSystemProperty(named=ConsoleBusBrowserSupport.GATE, matches="true", disabledReason=ConsoleBusBrowserSupport.DISABLED)
class ConsoleBusTrace_BrowserTest extends TestBase {

	private static Map<String,Object> traced;
	private static Map<String,Object> quiet;

	@BeforeAll
	static void probe() throws Exception {
		traced = run("trace", "master-detail.json", true, "{declare:[{topic:'app.region-picked',retain:true}]}", "?juneau-bus-trace");
		quiet = run("trace", "master-detail.json", true, "{declare:[{topic:'app.region-picked',retain:true}]}", "");
	}

	private static List<String> topics(Map<String,Object> report) {
		return list(report.get("history")).stream().map(h -> map(h).get("topic").toString()).toList();
	}

	@Test void a01_theFlagLogsEveryPublishAsABusDebugLine() {
		var lines = list(traced.get("busDebug")).stream().map(Object::toString).toList();
		assertFalse(lines.isEmpty());
		assertTrue(lines.stream().allMatch(l -> l.startsWith("[bus] ")), () -> lines.toString());
		for (var t : List.of("card:changes", "filter:changes", "selection:changes", "redraw:tasks"))
			assertTrue(lines.stream().anyMatch(l -> l.contains(" " + t + " ")), () -> t + " not traced in " + lines);
		assertTrue(lines.stream().anyMatch(l -> l.contains("suppressed (unchanged)")), () -> lines.toString());
		assertEquals(List.of(), list(traced.get("jsFailures")));
	}

	@Test void a02_noFlagMeansNoDebugLinesButTheHistoryStillFills() {
		assertEquals(List.of(), list(quiet.get("busDebug")));
		var t = topics(quiet);
		assertTrue(t.contains("selection:changes") && t.contains("redraw:tasks"), t::toString);
		assertTrue(list(quiet.get("history")).stream().anyMatch(h -> Boolean.TRUE.equals(map(h).get("suppressed"))), () -> quiet.toString());
		assertEquals(topics(traced), t, "the flag must not change what is published");
	}
}
