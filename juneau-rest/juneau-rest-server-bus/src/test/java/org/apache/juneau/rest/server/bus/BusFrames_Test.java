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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the {@link BusFrames} typed builders and helpers (the generic codec is pinned by
 * {@link BusFrames_Corpus_Test}).
 */
class BusFrames_Test extends TestBase {

	@Test void a01_typedBuilders() {
		assertEquals("{\"v\":1,\"type\":\"pub\",\"topic\":\"ops.jobs\",\"payload\":{\"a\":1},\"retained\":true,\"seq\":5}",
			BusFrames.pub("ops.jobs", "{\"a\":1}", true, 5));
		assertEquals("{\"v\":1,\"type\":\"clear\",\"topic\":\"ops.jobs\",\"seq\":6}", BusFrames.clear("ops.jobs", 6));
		assertEquals("{\"v\":1,\"type\":\"resync-begin\",\"seq\":1}", BusFrames.resyncBegin(1));
		assertEquals("{\"v\":1,\"type\":\"resync-end\",\"seq\":2}", BusFrames.resyncEnd(2));
		assertEquals("{\"v\":1,\"type\":\"ping\"}", BusFrames.ping());
		assertEquals("{\"v\":1,\"type\":\"error\",\"code\":\"bus:rate-limited\",\"message\":\"rate limited\",\"topic\":\"ops.x\"}",
			BusFrames.error("bus:rate-limited", "rate limited", "ops.x"));
		assertEquals("{\"v\":1,\"type\":\"error\",\"code\":\"bus:slow-consumer\",\"message\":\"slow consumer\"}",
			BusFrames.error("bus:slow-consumer", "slow consumer", null));
	}

	@Test void a02_seqOfEncodedFrame() {
		assertEquals(OptionalLong.of(5), BusFrames.seq(BusFrames.pub("ops.jobs", "{\"seq\":99}", true, 5)));
		assertEquals(OptionalLong.of(1), BusFrames.seq(BusFrames.resyncBegin(1)));
		assertEquals(OptionalLong.empty(), BusFrames.seq(BusFrames.ping()));
		assertEquals(OptionalLong.empty(), BusFrames.seq(BusFrames.error("bus:x", "m", null)));
	}

	@Test void a03_quoteMatchesJsonStringify() {
		assertEquals("\"a\\\"b\\\\c\\n\\r\\t\\b\\f\\u001f\"", BusFrames.quote("a\"b\\c\n\r\t\b\f\u001f"));
		assertEquals("\"\\ud800x\"", BusFrames.quote("\ud800x"), "lone surrogate is escaped, as JSON.stringify does");
		assertEquals("\"\ud83d\ude00\"", BusFrames.quote("\ud83d\ude00"), "a valid pair passes through");
		assertEquals("\"</\"", BusFrames.quote("</"), "solidus is not escaped");
	}

	@Test void a04_utf8Length() {
		assertEquals(3, BusFrames.utf8Length("abc"));
		assertEquals(2, BusFrames.utf8Length("\u00e9"));
		assertEquals(4, BusFrames.utf8Length("\ud83d\ude00"));
	}

	@Test void a05_encodeRejectsUnknownType() {
		var e = assertThrows(IllegalArgumentException.class, () -> BusFrames.encode(Map.of("v", 1, "type", "nope")));
		assertEquals("BusFrames.encode: unknown frame type 'nope'", e.getMessage());
	}

	@Test void a06_encodeRejectsBadFieldTypes() {
		for (var bad : List.<Map<String,Object>>of(
			Map.of("v", 1, "type", "pub", "payload", Map.of()),
			Map.of("v", 1, "type", "pub", "topic", 5, "payload", Map.of()),
			Map.of("v", 1, "type", "clear"),
			Map.of("v", 1, "type", "clear", "topic", List.of()),
			Map.of("v", 1, "type", "error", "message", "m"),
			Map.of("v", 1, "type", "error", "code", "c"),
			Map.of("v", 1, "type", "error", "code", "c", "message", "m", "topic", 7))) {
			assertThrows(IllegalArgumentException.class, () -> BusFrames.encode(bad), bad.toString());
		}
		var e = assertThrows(IllegalArgumentException.class,
			() -> BusFrames.encode(Map.of("v", 1, "type", "pub", "topic", "ops.jobs", "payload", Map.of(), "retained", "yes")));
		assertEquals("BusFrames.encode: 'retained' must be a boolean", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> BusFrames.encode(Map.of("v", 1, "type", "clear")));
		assertEquals("BusFrames.encode: 'topic' must be a string", e.getMessage());
	}

	@Test void a07_decodeAllowsSurroundingWhitespaceOnly() throws Exception {
		assertEquals("ping", BusFrames.decode(" \t\r\n{\"v\":1,\"type\":\"ping\"}\n\n ").get("type"));
		for (var bad : List.of("{\"v\":1,\"type\":\"ping\"} x", "{\"v\":1,\"type\":\"ping\"}/*c*/", "x{\"v\":1,\"type\":\"ping\"}",
			"{\"v\":1,\"type\":\"ping\"}\u00a0", "{\"v\":1,\"type\":\"ping\"}\0"))
			assertEquals("bus:bad-frame", assertThrows(BusRefusal.class, () -> BusFrames.decode(bad), bad).code(), bad);
	}

	@Test void a08_bracesInsideStringsDoNotEndTheObject() throws Exception {
		var f = BusFrames.decode("{\"v\":1,\"type\":\"error\",\"code\":\"c\",\"message\":\"}{ \\\" ] }\"} ");
		assertEquals("}{ \" ] }", f.get("message"));
		assertEquals("bus:bad-frame", assertThrows(BusRefusal.class, () -> BusFrames.decode("{\"v\":1,\"type\":\"ping\",\"x\":\"}\"} {}")).code());
	}

	@Test void a09_seqIsNormalizedToLongAndBounded() throws Exception {
		assertEquals(1L, BusFrames.decode("{\"v\":1,\"type\":\"resync-begin\",\"seq\":1.0}").get("seq"));
		assertEquals(1000L, BusFrames.decode("{\"v\":1,\"type\":\"resync-begin\",\"seq\":1e3}").get("seq"));
		assertEquals(9007199254740991L, BusFrames.decode("{\"v\":1,\"type\":\"resync-begin\",\"seq\":9007199254740991}").get("seq"));
		for (var bad : List.of("9007199254740992", "9007199254740993", "1e300", "-0.5", "\"1\"", "null"))
			assertThrows(BusRefusal.class, () -> BusFrames.decode("{\"v\":1,\"type\":\"resync-begin\",\"seq\":" + bad + "}"), bad);
	}
}
