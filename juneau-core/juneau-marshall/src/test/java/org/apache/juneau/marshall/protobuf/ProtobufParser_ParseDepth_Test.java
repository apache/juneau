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
package org.apache.juneau.marshall.protobuf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Recursion-depth guard tests for {@link ProtobufParserSession}'s nested-message parse path (READY-389:
 * shared {@link ParserSession} parse-depth budget applied to Protobuf, mirroring MsgPack's prior per-codec
 * guard).
 *
 * <p>
 * Before this fix, {@code ProtobufParserSession.readMessage} recursed once per nested {@code MESSAGE} field
 * (or map entry/value sub-message) with no depth cap; an adversarial deeply-nested message would only fail
 * once the JVM call stack was actually exhausted ({@link StackOverflowError}), rather than failing gracefully
 * via a bounded {@link ParseException}.
 */
class ProtobufParser_ParseDepth_Test extends TestBase {

	public static class Nested {
		public Nested child;
		public Nested() {}
	}

	/**
	 * Hand-builds a protobuf message with {@code depth} levels of nesting via field 1 (a {@code Nested child}
	 * MESSAGE field), the innermost level being an empty (leafless) sub-message.
	 */
	private static byte[] nestedMessage(int depth) {
		var msg = new byte[0];
		for (var i = 0; i < depth; i++) {
			var out = new ByteArrayOutputStream();
			var w = new ProtobufWriter(out);
			w.writeTag(1, WireType.LEN);
			w.writeLenDelimited(msg);
			msg = out.toByteArray();
		}
		return msg;
	}

	@Test
	void a01_deeplyNestedMessagesFailWithParseException() {
		// 1100 levels of nested "child" MESSAGE fields -> exceeds the shared ParserSession maxParseDepth
		// budget (default 1000).
		var msg = nestedMessage(1100);
		var e = assertThrows(ParseException.class, () -> ProtobufParser.DEFAULT.read(msg, Nested.class));
		var text = String.valueOf(e.getMessage());
		assertTrue(
			text.contains("Maximum parse depth exceeded") || text.contains("Depth too deep"),
			"Expected a graceful depth-failure ParseException.  Actual:\n" + text);
	}

	@Test
	void a02_moderateNestingStillParses() throws Exception {
		// 10 levels of nesting -> well within the depth budget; legitimate shallow payloads must be
		// unaffected by the new guard.
		var msg = nestedMessage(10);
		var b = ProtobufParser.DEFAULT.read(msg, Nested.class);
		var cur = b;
		for (var i = 0; i < 10; i++) {
			assertNotNull(cur.child);
			cur = cur.child;
		}
		assertNull(cur.child);
	}
}
