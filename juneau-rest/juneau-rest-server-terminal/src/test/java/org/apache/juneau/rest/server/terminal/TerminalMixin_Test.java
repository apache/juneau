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
package org.apache.juneau.rest.server.terminal;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.BadRequest;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;

class TerminalMixin_Test extends TestBase {

	@Rest
	public static class R extends BasicRestServlet implements TerminalMixin {
		private static final long serialVersionUID = 1L;
		static final Map<String,TerminalSource> SOURCES = new ConcurrentHashMap<>();
		static final List<String> RESOLVED = new CopyOnWriteArrayList<>();

		@Override
		public Optional<TerminalSource> terminalSource(String id, RestRequest req) {
			RESOLVED.add(id);
			return Optional.ofNullable(SOURCES.get(id));
		}
	}

	private static final MockRestClient c = MockRestClient.buildLax(R.class);

	@BeforeAll static void setUp() {
		var t1 = MemoryTerminalSource.create(1024, 100, 30);
		var b = "\u001b[32mok\u001b[0m\r\n".getBytes(UTF_8);
		t1.write(b, 0, b.length);
		R.SOURCES.put("t1", t1);
		var ring = MemoryTerminalSource.create(4, 80, 24);
		ring.write("abcdefgh".getBytes(UTF_8), 0, 8);
		ring.finish();
		R.SOURCES.put("ring", ring);
	}

	@BeforeEach void clear() {
		R.RESOLVED.clear();
	}

	@Test void a01_constants() {
		assertEquals("/juneau-terminal/{id}/bytes", TerminalMixin.BYTES_PATH);
		assertEquals("/juneau-terminal/{id}/raw", TerminalMixin.RAW_PATH);
		assertEquals("/juneau-terminal.js", TerminalMixin.JS_PATH);
		assertEquals("/juneau-terminal.css", TerminalMixin.CSS_PATH);
	}

	@Test void b01_bytesAndHeaders() throws Exception {
		var r = c.get("/juneau-terminal/t1/bytes?from=0").run().assertStatus(200);
		assertArrayEquals("\u001b[32mok\u001b[0m\r\n".getBytes(UTF_8), r.getContent().asBytes());
		r.assertHeader("Content-Type").isContains("application/octet-stream");
		r.assertHeader("Cache-Control").is("no-store");
		r.assertHeader("Term-Next").is("13");
		r.assertHeader("Term-End").is("13");
		r.assertHeader("Term-Done").is("false");
		r.assertHeader("Term-Cols").is("100");
		r.assertHeader("Term-Rows").is("30");
		r.assertHeader("Term-Truncated").is("false");
		r.assertHeader("Term-Error").isNull();
		assertList(R.RESOLVED, "t1");
	}

	@Test void b02_maxLimitsTheChunk() throws Exception {
		var r = c.get("/juneau-terminal/t1/bytes?from=2&max=3").run().assertStatus(200);
		assertEquals("32m", new String(r.getContent().asBytes(), UTF_8));
		r.assertHeader("Term-Next").is("5");
		r.assertHeader("Term-End").is("13");
	}

	@Test void b03_emptyAtTheEnd() throws Exception {
		var r = c.get("/juneau-terminal/t1/bytes?from=13").run().assertStatus(200);
		assertEquals(0, r.getContent().asBytes().length);
		r.assertHeader("Term-Next").is("13");
		r.assertHeader("Term-End").is("13");
		r.assertHeader("Term-Done").is("false");
	}

	@Test void b04_pastTheEndIs416WithEnd() throws Exception {
		c.get("/juneau-terminal/t1/bytes?from=14").run().assertStatus(416).assertHeader("Term-End").is("13");
	}

	@Test void b09_maxZeroIsAnEmptyOk() throws Exception {
		var r = c.get("/juneau-terminal/t1/bytes?from=0&max=0").run().assertStatus(200);
		assertEquals(0, r.getContent().asBytes().length);
		r.assertHeader("Term-Next").is("0");
		r.assertHeader("Term-End").is("13");
	}

	@Test void b10_416CarriesTheSizeAndEnd() throws Exception {
		var r = c.get("/juneau-terminal/t1/bytes?from=14").run().assertStatus(416);
		r.assertHeader("Term-End").is("13");
		r.assertHeader("Term-Cols").is("100");
		r.assertHeader("Term-Rows").is("30");
		r.assertHeader("Term-Next").isNull();
	}

	@Test void b11_nosniffOnBytesAndRaw() throws Exception {
		c.get("/juneau-terminal/t1/bytes").run().assertStatus(200).assertHeader("X-Content-Type-Options").is("nosniff");
		c.get("/juneau-terminal/ring/raw").run().assertStatus(200).assertHeader("X-Content-Type-Options").is("nosniff");
	}

	@Test void b05_truncatedAndDone() throws Exception {
		var r = c.get("/juneau-terminal/ring/bytes?from=0").run().assertStatus(200);
		assertEquals("efgh", new String(r.getContent().asBytes(), UTF_8));
		r.assertHeader("Term-Truncated").is("true");
		r.assertHeader("Term-Done").is("true");
		r.assertHeader("Term-Next").is("8");
	}

	@Test void b06_goneLogAnswersFromWhateverItIs(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
		var log = dir.resolve("g.log");
		Files.writeString(log, "abc");
		R.SOURCES.put("gone", FileTerminalSource.create(log).build());
		// A source that reports its own offsets as 0 when gone: the endpoint still answers with from.
		R.SOURCES.put("gone0", (from, max) -> new TerminalChunk(new byte[0], 0, 0, true, 80, 24, false, true));
		try {
			c.get("/juneau-terminal/gone/bytes?from=0").run().assertStatus(200);
			Files.delete(log);
			// Below the old end, at it, and past it: never 416, and Term-Next never rewinds.
			for (var from : List.of("1", "3", "99")) {
				var r = c.get("/juneau-terminal/gone/bytes?from=" + from).run().assertStatus(200);
				assertEquals(0, r.getContent().asBytes().length);
				r.assertHeader("Term-Done").is("true");
				r.assertHeader("Term-Error").is("gone");
				r.assertHeader("Term-Next").is(from);
				r.assertHeader("Term-End").is(from);
			}
			c.get("/juneau-terminal/gone0/bytes?from=7").run().assertStatus(200).assertHeader("Term-Next").is("7");
		} finally {
			R.SOURCES.remove("gone");
			R.SOURCES.remove("gone0");
		}
	}

	@Test void b07_maxIsCappedBeforeTheSourceSeesIt() throws Exception {
		var asked = new CopyOnWriteArrayList<Integer>();
		R.SOURCES.put("rec", (from, max) -> {
			asked.add(max);
			return new TerminalChunk(new byte[0], from, from, false, 80, 24, false, false);
		});
		try {
			c.get("/juneau-terminal/rec/bytes?max=999999999").run().assertStatus(200);
			assertList(asked, TerminalSource.MAX_READ_BYTES);
		} finally {
			R.SOURCES.remove("rec");
		}
	}

	@Test void b08_fromAtTheEndOfAFinishedSourceIsDone() throws Exception {
		var r = c.get("/juneau-terminal/ring/bytes?from=8").run().assertStatus(200);
		assertEquals(0, r.getContent().asBytes().length);
		r.assertHeader("Term-Next").is("8");
		r.assertHeader("Term-End").is("8");
		r.assertHeader("Term-Done").is("true");
		r.assertHeader("Term-Truncated").is("false");
		c.get("/juneau-terminal/ring/bytes?from=9").run().assertStatus(416).assertHeader("Term-End").is("8");
	}

	@Test void c01_badIdIs400BeforeLookup() throws Exception {
		c.get("/juneau-terminal/bad.id/bytes").run().assertStatus(400);
		c.get("/juneau-terminal/bad.id/raw").run().assertStatus(400);
		assertEmpty(R.RESOLVED);
	}

	@Test void c02_unknownIdIs404() throws Exception {
		c.get("/juneau-terminal/nope/bytes").run().assertStatus(404);
		c.get("/juneau-terminal/nope/raw").run().assertStatus(404);
	}

	@Test void c03_badFromOrMaxIs400() throws Exception {
		for (var q : List.of("from=-1", "from=x", "from=01", "max=-1", "from=99999999999999999"))
			c.get("/juneau-terminal/t1/bytes?" + q).run().assertStatus(400);
	}

	@Test void c04_rawChecksTheIdItself() {
		// The id becomes the download's file name, so the helper checks it even when a custom path calls it directly.
		var src = Optional.<TerminalSource>of(MemoryTerminalSource.create(4, 80, 24));
		for (var id : List.of("a\"b", "../x", "", "a b"))
			assertThrows(BadRequest.class, () -> TerminalEndpoints.raw(src, id, null), id);
	}

	@Test void d01_rawIsAnAttachment() throws Exception {
		var r = c.get("/juneau-terminal/ring/raw").run().assertStatus(200);
		assertEquals("efgh", new String(r.getContent().asBytes(), UTF_8));
		r.assertHeader("Content-Disposition").is("attachment; filename=\"ring.log\"");
		r.assertHeader("Content-Type").isContains("application/octet-stream");
	}

	@Test void d02_rawOfAGoneSourceIsEmptyAndSaysGone(@org.junit.jupiter.api.io.TempDir Path dir) throws Exception {
		var log = dir.resolve("g.log");
		Files.writeString(log, "abc");
		var src = FileTerminalSource.create(log).build();
		src.read(0, 10);
		Files.delete(log);
		R.SOURCES.put("rawgone", src);
		try {
			var r = c.get("/juneau-terminal/rawgone/raw").run().assertStatus(200);
			assertEquals(0, r.getContent().asBytes().length);
			r.assertHeader("Term-Error").is("gone");
		} finally {
			R.SOURCES.remove("rawgone");
		}
	}

	@Test void e01_assetsAreServed() throws Exception {
		c.get("/juneau-terminal.js").run().assertStatus(200).assertHeader("Content-Type").isContains("text/javascript")
			.assertContent().isContains("JuneauConsoleCards");
		c.get("/juneau-terminal.css").run().assertStatus(200).assertHeader("Content-Type").isContains("text/css")
			.assertContent().isContains(".juneau-term");
	}

	@Test void e02_assetUrlRejectsUnknownPath() {
		assertThrows(IllegalArgumentException.class, () -> TerminalMixin.terminalAssetUrl(null, "/nope.js"));
	}
}
