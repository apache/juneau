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
package org.apache.juneau.rest.server.bus;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Pins {@link BusFrames} against the shared {@code bus-frames-corpus.json}.  The JS codec
 * ({@code JuneauViews.bus.frames}) is pinned against the same file by {@code bus-frames.cjs}.
 */
class BusFrames_Corpus_Test extends TestBase {

	private static JsonMap corpus;

	@BeforeAll static void load() throws Exception {
		try (var in = BusFrames_Corpus_Test.class.getResourceAsStream("/bus-frames-corpus.json")) {
			assertNotNull(in, "bus-frames-corpus.json not on the test classpath");
			corpus = JsonMap.ofString(new String(in.readAllBytes(), UTF_8), JsonParser.DEFAULT);
		}
	}

	@SuppressWarnings("unchecked")
	private static List<Map<String,Object>> section(String name) {
		var list = (List<Map<String,Object>>)(List<?>) corpus.getList(name);
		assertFalse(list.isEmpty(), name);
		return list;
	}

	@SuppressWarnings("unchecked")
	private static Map<String,Object> frame(Map<String,Object> c) {
		return (Map<String,Object>) c.get("frame");
	}

	/** The corpus parser yields {@code Integer}s; {@code decode} yields {@code Long} sequence numbers.  Compare them as longs. */
	private static Map<String,Object> norm(Map<String,Object> frame) {
		var m = new LinkedHashMap<>(frame);
		if (m.get("seq") instanceof Number n)
			m.put("seq", n.longValue());
		return m;
	}

	@Test void a00_version() {
		assertEquals(1, ((Number) corpus.get("version")).intValue());
	}

	@Test void a01_encode() {
		for (var c : section("encode"))
			assertEquals(c.get("text"), BusFrames.encode(frame(c)), String.valueOf(c.get("name")));
	}

	@Test void a02_decode() throws Exception {
		for (var c : section("decode"))
			assertEquals(norm(frame(c)), norm(BusFrames.decode((String) c.get("text"))), String.valueOf(c.get("name")));
	}

	@Test void a03_bad() {
		for (var c : section("bad")) {
			var name = String.valueOf(c.get("name"));
			var e = assertThrows(BusRefusal.class, () -> BusFrames.decode((String) c.get("text")), name);
			assertEquals("bus:bad-frame", e.code(), name);
			assertEquals(400, e.status(), name);
		}
	}

	@Test void a04_encodeThenDecodeRoundTrips() throws Exception {
		for (var c : section("encode"))
			assertEquals(norm(frame(c)), norm(BusFrames.decode(BusFrames.encode(frame(c)))), String.valueOf(c.get("name")));
	}
}
