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
import static org.apache.juneau.BasicTestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class TerminalSource_Test extends TestBase {

	@Test void a01_chunkChecksItsMembers() {
		var b = new byte[2];
		assertThrowsWithMessage(IllegalArgumentException.class, "next must be between", () -> new TerminalChunk(b, 1, 5, false, 80, 24, false, false));
		assertThrowsWithMessage(IllegalArgumentException.class, "next must be between", () -> new TerminalChunk(b, 6, 5, false, 80, 24, false, false));
		assertThrowsWithMessage(IllegalArgumentException.class, "at least 1x1", () -> new TerminalChunk(b, 2, 5, false, 0, 24, false, false));
		assertThrowsWithMessage(IllegalArgumentException.class, "gone requires done", () -> new TerminalChunk(b, 2, 5, false, 80, 24, false, true));
		assertEquals(5, new TerminalChunk(b, 2, 5, true, 80, 24, false, true).end());
	}

	@Test void a02_copyToStopsAtTheEndItSawFirst() throws Exception {
		// A producer that is always one byte ahead: without the stop, copyTo would never return.
		var reads = new AtomicInteger();
		TerminalSource growing = (from, max) -> {
			if (reads.incrementAndGet() > 10)
				throw new AssertionError("copyTo kept reading past the end it saw first");
			return new TerminalChunk("x".getBytes(UTF_8), from + 1, from + 2, false, 80, 24, false, false);
		};
		var out = new ByteArrayOutputStream();
		growing.copyTo(out);
		assertEquals("xx", out.toString(UTF_8));
	}
}
