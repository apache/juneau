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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

class ConsoleOutputEndpoints_Test extends TestBase {

	static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T13:00:00Z"), ZoneOffset.UTC);
	static final Map<String,ConsoleOutputSource> SOURCES = new ConcurrentHashMap<>();

	/** Option 2 of spec §3.5: custom paths that delegate to ConsoleOutputEndpoints. */
	@Rest
	public static class E extends BasicRestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/log/{name}/lines")
		public void lines(@Path("name") String name, RestRequest req, RestResponse res) throws IOException {
			ConsoleOutputEndpoints.lines(Optional.ofNullable(SOURCES.get(name)), req, res);
		}
	}

	/** A small-log source whose tail falls back to a from-start forward page (spec §3.2). */
	static final class FallbackSource implements ConsoleOutputSource {
		private final ConsoleOutputLog log;
		FallbackSource(ConsoleOutputLog log) { this.log = log; }
		@Override public ConsoleOutputPage page(String after, int max) { return log.page(after, max); }
		@Override public ConsoleOutputPage tail(int n) { return page(null, ConsoleOutputEndpoints.MAX_PAGE_LINES); }
		@Override public ConsoleOutputPage before(String token, int limit) { throw new UnsupportedOperationException(); }
		@Override public Stream<ConsoleOutputLine> stream() { return log.stream(); }
	}

	private static final MockRestClient c = MockRestClient.buildLax(E.class);

	private static ConsoleOutputLog log(int count) {
		var log = new ConsoleOutputLog(ConsoleOutputLog.DEFAULT_MAX_LINES, CLOCK);
		for (var i = 1; i <= count; i++)
			log.append(ConsoleOutputLine.info("line " + i));
		return log;
	}

	@BeforeAll static void setUp() {
		SOURCES.put("five", log(5));
		SOURCES.put("big", log(6000));
		SOURCES.put("fallback", new FallbackSource(log(5)));
	}

	private static Map<?,?> get(String url) throws Exception {
		var body = c.get(url).header("Accept", "application/json").run().assertStatus(200).getContent().asString();
		return Json.to(body, Map.class);
	}

	private static List<String> ns(Map<?,?> page) {
		return ((List<?>)page.get("lines")).stream().map(x -> String.valueOf(((Map<?,?>)x).get("n"))).toList();
	}

	private static String error(String url, int status) throws Exception {
		return c.get(url).header("Accept", "application/json").run().assertStatus(status).getContent().asString();
	}

	//------------------------------------------------------------------------------------------------------------------
	// a) Request forms
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_fromStart() throws Exception {
		var p = get("/log/five/lines");
		assertBean(p, "contractVersion,next,more,hasEarlier,terminal", "1,5,false,false,false");
		assertList(ns(p), "1", "2", "3", "4", "5");
		assertFalse(p.containsKey("before"));
	}

	@Test void a02_after() throws Exception {
		var p = get("/log/five/lines?after=3");
		assertBean(p, "next,more,hasEarlier,before", "5,false,true,3");
		assertList(ns(p), "4", "5");
	}

	@Test void a03_afterAtEndIsEmptyAndEchoesToken() throws Exception {
		var p = get("/log/five/lines?after=5");
		assertBean(p, "next,more", "5,false");
		assertList(ns(p));
	}

	@Test void a04_forwardPageCappedAtMaxPageLines() throws Exception {
		var p = get("/log/big/lines");
		assertEquals(ConsoleOutputEndpoints.MAX_PAGE_LINES, ns(p).size());
		assertBean(p, "next,more", "2000,true");
	}

	@Test void a05_tail() throws Exception {
		var p = get("/log/five/lines?tail=2");
		assertBean(p, "next,more,hasEarlier,before", "5,false,true,3");
		assertList(ns(p), "4", "5");
	}

	@Test void a06_tailNotCappedByForwardCap() throws Exception {
		var p = get("/log/big/lines?tail=5000");
		assertEquals(5000, ns(p).size());
		assertEquals("1001", ns(p).get(0));
	}

	@Test void a07_beforeWithLimit() throws Exception {
		var p = get("/log/five/lines?before=3&limit=2");
		assertBean(p, "more,hasEarlier,before", "false,true,1");
		assertList(ns(p), "2", "3");
		assertFalse(p.containsKey("next"));
	}

	@Test void a08_beforeDefaultLimit() throws Exception {
		var p = get("/log/big/lines?before=5000");
		assertEquals(ConsoleOutputEndpoints.DEFAULT_LIMIT, ns(p).size());
		assertEquals("5000", ns(p).get(ns(p).size() - 1));
	}

	@Test void a09_beforeAtStart() throws Exception {
		var p = get("/log/five/lines?before=2&limit=10");
		assertBean(p, "hasEarlier", "false");
		assertList(ns(p), "1", "2");
		assertFalse(p.containsKey("before"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b) 400s
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_mixedForms() throws Exception {
		assertTrue(error("/log/five/lines?after=1&tail=2", 400).contains("at most one of after, tail and before"));
		assertTrue(error("/log/five/lines?tail=2&before=3", 400).contains("at most one of after, tail and before"));
		assertTrue(error("/log/five/lines?after=1&before=3", 400).contains("at most one of after, tail and before"));
	}

	@Test void b02_limitWithoutBefore() throws Exception {
		assertTrue(error("/log/five/lines?limit=2", 400).contains("limit requires before"));
		assertTrue(error("/log/five/lines?tail=2&limit=2", 400).contains("limit requires before"));
	}

	@Test void b03_tailOutOfRange() throws Exception {
		for (var v : List.of("0", "10001", "-1", "abc", ""))
			assertTrue(error("/log/five/lines?tail=" + v, 400).contains("tail must be an integer in 1..10000"), v);
	}

	@Test void b04_limitOutOfRange() throws Exception {
		for (var v : List.of("0", "10001", "x"))
			assertTrue(error("/log/five/lines?before=3&limit=" + v, 400).contains("limit must be an integer in 1..10000"), v);
	}

	@Test void b05_badTokenGrammar() throws Exception {
		assertTrue(error("/log/five/lines?after=a%20b", 400).contains("after is not a valid token"));
		assertTrue(error("/log/five/lines?after=", 400).contains("after is not a valid token"));
		assertTrue(error("/log/five/lines?before=a%2Fb", 400).contains("before is not a valid token"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c) 404 / 410
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_unknownSource() throws Exception {
		error("/log/nope/lines", 404);
	}

	@Test void c02_unknownAfterTokenIsGone() throws Exception {
		assertTrue(error("/log/five/lines?after=99", 410).contains("unknown or stale console-output token"));
	}

	@Test void c03_unknownBeforeTokenIsGone() throws Exception {
		error("/log/five/lines?before=99", 410);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d) Headers, fallback tail, constants
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_noStoreAndJson() throws Exception {
		var r = c.get("/log/five/lines").header("Accept", "*/*").run().assertStatus(200);
		r.assertHeader("Cache-Control").is("no-store");
		r.assertHeader("Content-Type").isContains("application/json");
		assertEquals("1", Json.to(r.getContent().asString(), Map.class).get("contractVersion"));
	}

	@Test void d02_fallbackTailIsFromStartForwardPage() throws Exception {
		var p = get("/log/fallback/lines?tail=2");
		assertBean(p, "hasEarlier,next,more", "false,5,false");
		assertList(ns(p), "1", "2", "3", "4", "5");
		assertFalse(p.containsKey("before"));
	}

	@Test void d03_forwardCapsAgree() {
		assertEquals(ConsoleOutputEndpoints.MAX_PAGE_LINES, FileConsoleOutputSource.MAX_PAGE_LINES);
		assertEquals(2000, ConsoleOutputEndpoints.MAX_PAGE_LINES);
		assertEquals(10_000, ConsoleOutputEndpoints.MAX_LIMIT);
	}

	@Test void d04_contractMapNotBean() throws Exception {
		var body = c.get("/log/five/lines?tail=1").header("Accept", "application/json").run().getContent().asString();
		assertFalse(body.contains("null"), body);
		assertFalse(body.contains("\"kind\""), body);
		var line = (Map<?,?>)((List<?>)Json.to(body, Map.class).get("lines")).get(0);
		assertBean(line, "n,level,text", "5,INFO,line 5");
	}

	@Test void d05_checkLogId() {
		assertEquals("abc_1-X", ConsoleOutputEndpoints.checkLogId("abc_1-X"));
		for (var bad : Arrays.asList(null, "", "a.b", "a/b", "a b", "x".repeat(129)))
			assertThrows(org.apache.juneau.http.response.BadRequest.class, () -> ConsoleOutputEndpoints.checkLogId(bad));
	}
}
