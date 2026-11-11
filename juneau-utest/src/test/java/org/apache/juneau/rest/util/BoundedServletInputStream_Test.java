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
package org.apache.juneau.rest.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests the bulk read behavior of {@link BoundedServletInputStream} at and over the limit.
 */
class BoundedServletInputStream_Test extends TestBase {

	private static BoundedServletInputStream stream(int size, long max) {
		return new BoundedServletInputStream(new ByteArrayInputStream(new byte[size]), max);
	}

	@Test void a01_bulkReadWithinLimit() throws Exception {
		var s = stream(10, 10);
		var b = new byte[4];
		assertEquals(4, s.read(b, 0, 4));
		assertEquals(4, s.read(b, 0, 4));
		assertEquals(2, s.read(b, 0, 4));
		assertEquals(-1, s.read(b, 0, 4));
	}

	@Test void a02_bulkReadExactlyAtLimitThenEof() throws Exception {
		var s = stream(8, 8);
		var b = new byte[16];
		assertEquals(8, s.read(b, 0, 16));
		assertEquals(-1, s.read(b, 0, 16));
	}

	@Test void a03_bulkReadOverLimitThrowsInsteadOfReturningZero() throws Exception {
		var s = stream(20, 8);
		var b = new byte[16];
		assertEquals(8, s.read(b, 0, 16));
		assertThrows(IOException.class, () -> s.read(b, 0, 16));
	}

	@Test void a04_shortReadsOnlyCountBytesActuallyRead() throws Exception {
		// Underlying stream returns fewer bytes than requested; only those bytes count against the limit.
		var in = new InputStream() {
			int n = 0;
			@Override public int read() { return n++ < 100 ? 1 : -1; }
			@Override public int read(byte[] b, int off, int len) {
				if (n >= 100) return -1;
				b[off] = 1; n++;
				return 1;
			}
		};
		var s = new BoundedServletInputStream(in, 10);
		var b = new byte[8];
		for (var i = 0; i < 10; i++)
			assertEquals(1, s.read(b, 0, 8));
		assertThrows(IOException.class, () -> s.read(b, 0, 8));
	}

	@Test void a05_zeroLengthRead() throws Exception {
		var s = stream(4, 4);
		assertEquals(0, s.read(new byte[1], 0, 0));
	}
}
