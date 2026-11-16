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
package org.apache.juneau.marshall.sse;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for the size limits of {@link SseEventReader}.
 */
class SseEventReader_Limits_Test extends TestBase {

	@Test void a01_overlongLineFails() {
		var r = new SseEventReader(new StringReader("data: " + "x".repeat(100) + "\n\n"), 50, 1000);
		assertThrows(UncheckedIOException.class, r::hasNext);
	}

	@Test void a02_unterminatedLineFails() {
		var r = new SseEventReader(new StringReader("x".repeat(100)), 50, 1000);
		assertThrows(UncheckedIOException.class, r::hasNext);
	}

	@Test void a03_oversizedEventDataFails() {
		var sb = new StringBuilder();
		for (var i = 0; i < 20; i++)
			sb.append("data: 0123456789\n");
		var r = new SseEventReader(new StringReader(sb.toString()), 100, 100);
		assertThrows(UncheckedIOException.class, r::hasNext);
	}

	@Test void a04_toListSurfacesIOException() {
		var r = new SseEventReader(new StringReader("data: " + "x".repeat(100) + "\n\n"), 50, 1000);
		assertThrows(IOException.class, r::toList);
	}

	@Test void a05_withinLimitsParses() throws IOException {
		var r = new SseEventReader(new StringReader("data: abc\n\ndata: def\n\n"), 50, 50);
		var l = r.toList();
		assertEquals(2, l.size());
		assertEquals("abc", l.get(0).getData());
	}

	@Test void a06_limitsApplyPerEventNotCumulatively() throws IOException {
		var r = new SseEventReader(new StringReader("data: 0123456789\n\ndata: 0123456789\n\ndata: 0123456789\n\n"), 50, 20);
		assertEquals(3, r.toList().size());
	}

	@Test void a07_nonPositiveLimitsRejected() {
		var r1 = new StringReader("");
		assertThrows(IllegalArgumentException.class, () -> new SseEventReader(r1, 0, 10));
		var r2 = new StringReader("");
		assertThrows(IllegalArgumentException.class, () -> new SseEventReader(r2, 10, 0));
	}
}
