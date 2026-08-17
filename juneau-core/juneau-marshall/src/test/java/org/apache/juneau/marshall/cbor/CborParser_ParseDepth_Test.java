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
package org.apache.juneau.marshall.cbor;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Recursion-depth guard tests for {@link CborParserSession}'s databind parse path (READY-389: shared
 * {@link ParserSession} parse-depth budget applied to CBOR, mirroring MsgPack's prior per-codec guard).
 *
 * <p>
 * Before this fix, {@code CborParserSession.readAnything} recursed once per nesting level with no depth
 * cap of its own; an adversarial deeply-nested array/map would only fail once the JVM call stack was
 * actually exhausted ({@link StackOverflowError}), rather than failing gracefully via a bounded
 * {@link ParseException} the way MsgPack already did.
 */
class CborParser_ParseDepth_Test extends TestBase {

	@Test void a01_deeplyNestedArraysFailWithParseException() {
		// 1100 nested definite-length-1 array headers (CBOR major type 4, additional info 1 -> 0x81) then a
		// terminal UINT 0 (0x00) -> exceeds the shared ParserSession maxParseDepth budget (default 1000).
		var sb = new StringBuilder();
		for (var i = 0; i < 1100; i++)
			sb.append("81 ");
		sb.append("00");
		var input = sb.toString();
		var e = assertThrows(ParseException.class, () -> CborParser.DEFAULT.read(fromSpacedHex(input), Object.class));
		var msg = String.valueOf(e.getMessage());
		// Graceful depth-failure ParseException; the soft maxParseDepth guard is expected to fire before any
		// real StackOverflowError, but a constrained CI thread stack falling back to the StackOverflowError
		// wrapper is also an acceptable pass (mirrors the MsgPack conformance test's tolerance).
		assertTrue(
			msg.contains("Maximum parse depth exceeded") || msg.contains("Depth too deep"),
			"Expected a graceful depth-failure ParseException.  Actual:\n" + msg);
	}

	@Test void a02_moderateNestingStillParses() throws Exception {
		// 10 nested arrays then a terminal UINT 5 -> well within the depth budget; legitimate shallow
		// payloads must be unaffected by the new guard.
		var sb = new StringBuilder();
		for (var i = 0; i < 10; i++)
			sb.append("81 ");
		sb.append("05");
		Object o = CborParser.DEFAULT.read(fromSpacedHex(sb.toString()), Object.class);
		for (var i = 0; i < 10; i++) {
			var l = (List<?>) o;
			assertEquals(1, l.size());
			o = l.get(0);
		}
		assertEquals(5L, o);
	}
}
