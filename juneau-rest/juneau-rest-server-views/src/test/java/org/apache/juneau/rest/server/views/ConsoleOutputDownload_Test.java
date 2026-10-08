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

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

class ConsoleOutputDownload_Test extends TestBase {

	static final Map<String,ConsoleOutputSource> SOURCES = new ConcurrentHashMap<>();
	static final AtomicInteger CLOSED = new AtomicInteger();

	@Rest
	public static class R extends BasicRestServlet implements ConsoleOutputMixin {
		private static final long serialVersionUID = 1L;
		@Override
		public Optional<ConsoleOutputSource> consoleOutputSource(String logId, RestRequest req) {
			return Optional.ofNullable(SOURCES.get(logId));
		}
	}

	/** Wraps a log so the test can see stream() being closed, and optionally fail mid-write. */
	static final class Tracked implements ConsoleOutputSource {
		private final ConsoleOutputLog log;
		private final int failAt;
		Tracked(ConsoleOutputLog log, int failAt) { this.log = log; this.failAt = failAt; }
		@Override public ConsoleOutputPage page(String after, int max) { return log.page(after, max); }
		@Override public ConsoleOutputPage tail(int n) { return log.tail(n); }
		@Override public ConsoleOutputPage before(String token, int limit) { return log.before(token, limit); }
		@Override public Stream<ConsoleOutputLine> stream() {
			var count = new AtomicInteger();
			return log.stream().peek(x -> {
				if (count.incrementAndGet() == failAt)
					throw new IllegalStateException("boom");
			}).onClose(CLOSED::incrementAndGet);
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(R.class);

	@BeforeAll static void setUp() {
		var small = new ConsoleOutputLog();
		small.append(ConsoleOutputLine.info("first"));
		small.append(ConsoleOutputLine.severe("two\nlines").style(ConsoleOutputLine.Style.ERROR));
		small.append(ConsoleOutputLine.info("third"));
		SOURCES.put("small", new Tracked(small, -1));
		SOURCES.put("failing", new Tracked(small, 2));
		var big = new ConsoleOutputLog(200_000);
		for (var i = 1; i <= 100_000; i++)
			big.append(ConsoleOutputLine.info("l" + i));
		SOURCES.put("big", new Tracked(big, -1));
	}

	@BeforeEach void reset() {
		CLOSED.set(0);
	}

	private static String download(String id, String accept) throws Exception {
		var r = c.get("/juneau-console-output/" + id + "/download").header("Accept", accept).run().assertStatus(200);
		r.assertHeader("Content-Type").isContains("application/jsonl");
		return r.getContent().asString();
	}

	@Test void a01_oneCompactRecordPerLine() throws Exception {
		var body = download("small", "application/jsonl");
		var rows = body.split("\n");
		assertEquals(3, rows.length, body);
		assertTrue(body.endsWith("\n"));
		assertBean(Json.to(rows[0], Map.class), "n,level,text", "1,INFO,first");
		assertEquals(Json5.to("{n:2,level:'SEVERE',text:'two\\nlines',ui:{style:'error'}}", Map.class),
			withoutInstant(Json.to(rows[1], Map.class)));
	}

	@Test void a02_newlineInTextStaysEscaped() throws Exception {
		var body = download("small", "application/jsonl");
		assertTrue(body.contains("two\\nlines"), body);
		assertEquals("two\nlines", Json.to(body.split("\n")[1], Map.class).get("text"));
	}

	@Test void a03_contentTypeIndependentOfAccept() throws Exception {
		var a = download("small", "application/jsonl");
		assertEquals(a, download("small", "*/*"));
		assertEquals(a, download("small", "application/json5l"));
	}

	@Test void a04_attachmentHeader() throws Exception {
		c.get("/juneau-console-output/small/download").run().assertStatus(200)
			.assertHeader("Content-Disposition").is("attachment; filename=\"small.jsonl\"")
			.assertHeader("Cache-Control").is("no-store");
	}

	@Test void a05_contractMapsOnly() throws Exception {
		var body = download("small", "application/jsonl");
		assertFalse(body.contains("null"), body);
		assertFalse(body.contains("\"frags\""), body);
	}

	@Test void b01_streamClosedAfterWrite() throws Exception {
		download("small", "application/jsonl");
		assertEquals(1, CLOSED.get());
	}

	@Test void b02_streamClosedOnMidWriteFailure() throws Exception {
		c.get("/juneau-console-output/failing/download").run();
		assertEquals(1, CLOSED.get());
	}

	@Test void c01_hundredThousandLines() throws Exception {
		var body = download("big", "application/jsonl");
		var rows = body.split("\n");
		assertEquals(100_000, rows.length);
		assertBean(Json.to(rows[99_999], Map.class), "n,text", "100000,l100000");
		assertEquals(1, CLOSED.get());
	}

	@Test void d01_fileName() {
		assertEquals("abc-1_x.jsonl", ConsoleOutputEndpoints.fileName("abc-1_x") + ".jsonl");
		assertEquals("a_b_c", ConsoleOutputEndpoints.fileName("a\"b/c"));
	}

	private static Map<?,?> withoutInstant(Map<?,?> m) {
		var copy = new LinkedHashMap<Object,Object>(m);
		copy.remove("instant");
		return copy;
	}
}
