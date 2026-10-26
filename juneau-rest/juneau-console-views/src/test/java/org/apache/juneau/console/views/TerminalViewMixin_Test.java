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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.terminal.report.*;
import org.apache.juneau.rest.server.servlet.*;
import org.apache.juneau.terminal.report.TerminalEvent.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({
	"resource" // The shared mock client lives for the whole test class, and the in-memory mock requests and responses hold no OS resources.
})
class TerminalViewMixin_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements TerminalViewMixin {
		private static final long serialVersionUID = 1L;
		static final Map<String,TerminalViewSource> SOURCES = new ConcurrentHashMap<>();
		static final List<String> RESOLVED = new CopyOnWriteArrayList<>();

		@Override
		public Optional<TerminalViewSource> terminalViewSource(String runId, RestRequest req) {
			RESOLVED.add(runId);
			return Optional.ofNullable(SOURCES.get(runId));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(R.class);
	private static final TerminalViewLog LOG = TerminalViewLog.create();

	@BeforeAll static void setUp() {
		LOG.append(TerminalEvent.step("a", "Build"));
		LOG.append(TerminalEvent.end("a", EndStatus.OK));
		R.SOURCES.put("r1", LOG);
	}

	@BeforeEach void clear() {
		R.RESOLVED.clear();
	}

	@SuppressWarnings({
		"unchecked" // Json.to(..., Map.class) returns a raw Map; JSON object keys are always strings.
	})
	private static Map<String,Object> get(String path) throws Exception {
		var body = c.get(path).header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		return Json.to(body, Map.class);
	}

	@Test void a01_constants() {
		assertEquals("/juneau-terminal-view", TerminalViewMixin.RUN_VIEW_PREFIX);
		assertEquals("/juneau-terminal-view/{runId}/events", TerminalViewMixin.EVENTS_PATH);
	}

	@Test void b01_okFirstPage() throws Exception {
		c.get("/juneau-terminal-view/r1/events").header("Accept", "application/json").run().assertStatus(200)
			.assertContent().isContains("\"contractVersion\":\"1\"");
		assertList(R.RESOLVED, "r1");
	}

	@Test void b02_cacheControlAndContentType() throws Exception {
		var r = c.get("/juneau-terminal-view/r1/events").run().assertStatus(200);
		r.assertHeader("Cache-Control").is("no-store");
		r.assertHeader("Content-Type").isContains("application/json");
	}

	@Test void b03_unknownRunIs404() throws Exception {
		c.get("/juneau-terminal-view/nope/events").run().assertStatus(404);
		assertList(R.RESOLVED, "nope");
	}

	@Test void b04_badRunIdIs400BeforeResolver() throws Exception {
		c.get("/juneau-terminal-view/a%20b/events").run().assertStatus(400);
		c.get("/juneau-terminal-view/a.b/events").run().assertStatus(400);
		assertList(R.RESOLVED);
	}

	@Test void b05_badAfterIs400() throws Exception {
		c.get("/juneau-terminal-view/r1/events?after=a%20b").run().assertStatus(400);
	}

	@Test void b06_staleTokenIs410() throws Exception {
		var log = TerminalViewLog.create();
		log.append(TerminalEvent.note(Level.INFO, "x"));
		R.SOURCES.put("r2", log);
		var token = (String)get("/juneau-terminal-view/r2/events").get("next");
		log.reset();
		c.get("/juneau-terminal-view/r2/events?after=" + token).run().assertStatus(410);
		c.get("/juneau-terminal-view/r2/events").run().assertStatus(200);
	}

	@SuppressWarnings({
		"unchecked" // Json.to(..., Map.class) returns a raw Map; JSON object keys are always strings.
	})
	@Test void b07_secondRequestUsesNext() throws Exception {
		var log = TerminalViewLog.create();
		log.append(TerminalEvent.note(Level.INFO, "one"));
		R.SOURCES.put("r3", log);
		var p1 = get("/juneau-terminal-view/r3/events");
		log.append(TerminalEvent.note(Level.INFO, "two"));
		var p2 = get("/juneau-terminal-view/r3/events?after=" + p1.get("next"));
		var events = (List<Map<String,Object>>)p2.get("events");
		assertSize(1, events);
		assertBean(events.get(0), "seq,text", "2,two");
		assertEquals(false, p2.get("more"));
	}

	@Test void b08_pageSizeCappedAt2000() throws Exception {
		var log = TerminalViewLog.create();
		for (var i = 0; i < 2500; i++)
			log.append(TerminalEvent.note(Level.INFO, "n"));
		R.SOURCES.put("r4", log);
		var p1 = get("/juneau-terminal-view/r4/events");
		assertSize(2000, ((List<?>)p1.get("events")));
		assertEquals(true, p1.get("more"));
		var p2 = get("/juneau-terminal-view/r4/events?after=" + p1.get("next"));
		assertSize(500, ((List<?>)p2.get("events")));
		assertEquals(false, p2.get("more"));
	}

	@Test void b09_moreNeverWithEmptyEvents() throws Exception {
		var p = get("/juneau-terminal-view/r1/events");
		var more = (Boolean)p.get("more");
		assertTrue(! more || ! ((List<?>)p.get("events")).isEmpty());
		var empty = get("/juneau-terminal-view/r1/events?after=" + p.get("next"));
		assertEquals(false, empty.get("more"));
		assertTrue(((List<?>)empty.get("events")).isEmpty());
	}
}
