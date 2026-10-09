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

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * The JS half of the shared frame corpus (spec §11.4, Q9).  {@code bus-frames.cjs} runs every case of
 * {@code juneau-rest-server-bus/src/test/resources/bus-frames-corpus.json} through {@code JuneauViews.bus.frames}.
 * This test compares what JS produced with the corpus, which Java's {@code BusFrames_Corpus_Test} (Task 14) also
 * reads, so both sides write the same bytes.  It also pins two JS-only groups: the {@code maxFrameBytes} boundary
 * and the {@code SseParser}.  Gated on {@code node} being on {@code PATH}.
 */
class ViewsJs_BusFrames_Test extends TestBase {

	private static Map<String,Object> report;
	private static Map<String,Object> corpus;

	@BeforeAll
	@SuppressWarnings("unchecked")
	static void runHarness() throws Exception {
		var file = Path.of(System.getProperty("basedir"), "../juneau-rest-server-bus/src/test/resources/bus-frames-corpus.json");
		corpus = Json.to(Files.readString(file, UTF_8), Map.class);
		report = BusHarness.run("bus-frames.cjs", "/org/apache/juneau/views/juneau-bus.js");
	}

	private static Map<String,Object> report() {
		assumeTrue(report != null, "node not on PATH");
		return report;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String,Object>> list(Map<String,Object> m, String key) {
		var l = (List<Map<String,Object>>) m.get(key);
		assertNotNull(l, () -> "no '" + key + "' in " + m);
		return l;
	}

	private static void assertJson(String expected, Object actual) {
		try {
			assertEquals(Json.to(expected, Object.class), actual, () -> String.valueOf(actual));
		} catch (Exception e) {
			fail("bad expected JSON: " + expected + ": " + e);
		}
	}

	@Test void a01_versionAndLimit() {
		var r = report();
		assertEquals(corpus.get("version"), r.get("version"));
		assertJson("65536", r.get("maxFrameBytes"));
	}

	@Test void a02_encodeMatchesTheCorpusByteForByte() {
		var cases = list(corpus, "encode");
		var got = list(report(), "encode");
		assertEquals(cases.size(), got.size());
		for (var i = 0; i < cases.size(); i++) {
			var name = cases.get(i).get("name");
			assertEquals(cases.get(i).get("text"), got.get(i).get("ok"), () -> name + ": " + got);
		}
	}

	@Test void a03_decodeMatchesTheCorpusFrames() {
		var cases = list(corpus, "decode");
		var got = list(report(), "decode");
		assertEquals(cases.size(), got.size());
		for (var i = 0; i < cases.size(); i++) {
			var name = cases.get(i).get("name");
			assertEquals(cases.get(i).get("frame"), got.get(i).get("ok"), () -> name + ": " + got);
		}
	}

	@Test void a04_encodedTextRoundTrips() {
		var got = list(report(), "roundTrip");
		assertEquals(list(corpus, "encode").size(), got.size());
		got.forEach(x -> assertEquals(true, x.get("ok"), got::toString));
	}

	@Test void a05_everyBadCaseIsE_JS_58WithItsReason() {
		var cases = list(corpus, "bad");
		var got = list(report(), "bad");
		assertEquals(cases.size(), got.size());
		var reasons = List.of(
			"not-json", "not-json", "not-json", "not-json",
			"bad-version", "bad-version",
			"unknown-type",
			"bad-field", "bad-field", "bad-field", "bad-field", "bad-field", "bad-field", "bad-field", "bad-field", "bad-field",
			"not-json", "not-json", "bad-field");
		assertEquals(reasons.size(), cases.size(), "corpus bad[] changed: update the expected reasons");
		for (var i = 0; i < cases.size(); i++) {
			var name = cases.get(i).get("name");
			var g = got.get(i);
			assertNull(g.get("ok"), () -> name + " decoded: " + g);
			assertEquals("E-JS-58", g.get("code"), () -> name + ": " + g);
			assertEquals(reasons.get(i), g.get("reason"), () -> name + ": " + g);
			assertTrue(((String) g.get("message")).startsWith("bad frame (" + reasons.get(i) + "): "), () -> name + ": " + g);
		}
		// Messages that do not quote the engine's JSON.parse text are pinned exactly.
		assertEquals("bad frame (not-json): frame is not a JSON object", got.get(2).get("message"));
		assertEquals("bad frame (bad-version): v is 2, expected 1", got.get(5).get("message"));
		assertEquals("bad frame (unknown-type): type 'subscribe' is not a v1 frame type", got.get(6).get("message"));
		assertEquals("bad frame (bad-field): pub.topic is missing", got.get(7).get("message"));
		assertEquals("bad frame (bad-field): clear.seq", got.get(12).get("message"));
	}

	@Test @SuppressWarnings("unchecked")
	void a06_maxFrameBytesIsInclusiveAndCountsUtf8() {
		var t = (Map<String,Object>) report().get("tooLarge");
		assertJson("65536", t.get("atLimitBytes"));
		assertEquals("ok", t.get("encodeAtLimit"));
		assertEquals("ok", t.get("decodeAtLimit"));
		assertEquals("too-large", t.get("encodeOverLimit"));
		assertEquals("too-large", t.get("decodeOverLimit"));
		// 'a', 'é', '✓', a surrogate pair, and a lone surrogate (counted as 3, plus 'x').
		assertJson("[1,2,3,4,4]", t.get("utf8"));
	}

	@Test void a07_sseParserHandlesSplitsLineEndsAndComments() {
		assertJson("""
			[
				[{"event":"bus","id":"1","data":"{\\"v\\":1,\\"type\\":\\"ping\\"}"}],
				[{"event":"bus","id":null,"data":"{\\"v\\":1,\\"type\\":\\"ping\\"}"}],
				[{"event":"bus","id":null,"data":"{\\"v\\":1,\\n\\"type\\":\\"ping\\"}"}],
				[{"event":"message","id":null,"data":"x"}],
				[{"event":"bus","id":"7","data":"a"},{"event":"bus","id":null,"data":"b"}],
				[]
			]""", report().get("sse"));
	}

	@Test void a08_sseParserDropsOversizedEventsAndResumes() {
		// The dropped event keeps its event name and id, carries no data, and the next event parses normally.
		assertJson("""
			[
				[{"event":"bus","id":"4","tooLarge":true,"data":""},{"event":"message","id":null,"tooLarge":false,"data":"ok"}],
				[{"event":"message","id":null,"tooLarge":true,"data":""},{"event":"message","id":null,"tooLarge":false,"data":"ok"}],
				[{"event":"message","id":null,"tooLarge":true,"data":""},{"event":"message","id":null,"tooLarge":false,"data":"ok"}]
			]""", report().get("sseCap"));
	}

	@Test @SuppressWarnings("unchecked")
	void a09_sseParserDoesNotBufferAnEndlessLine() {
		var r = (Map<String,Object>) report().get("sseEndlessLine");
		assertTrue(((Number) r.get("buffered")).intValue() <= 65536, () -> "buffered " + r.get("buffered"));
		assertJson("[{\"event\":\"message\",\"id\":null,\"data\":\"\",\"tooLarge\":true},{\"event\":\"message\",\"id\":null,\"data\":\"ok\"}]", r.get("after"));
	}
}
