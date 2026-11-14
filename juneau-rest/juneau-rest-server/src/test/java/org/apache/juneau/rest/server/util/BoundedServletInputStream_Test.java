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
package org.apache.juneau.rest.server.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;

import org.junit.jupiter.api.*;

/**
 * Tests the bulk-read path of {@link BoundedServletInputStream} at and over its limit.
 */
class BoundedServletInputStream_Test {

	private static BoundedServletInputStream stream(int size, long max) {
		return new BoundedServletInputStream(new ByteArrayInputStream(new byte[size]), max);
	}

	@Test void a01_bulkRead_underLimit() throws Exception {
		var is = stream(5, 10);
		var b = new byte[20];
		assertEquals(5, is.read(b, 0, 20));
		assertEquals(-1, is.read(b, 0, 20));
	}

	@Test void a02_bulkRead_exactlyAtLimit_thenEof() throws Exception {
		var is = stream(10, 10);
		var b = new byte[20];
		assertEquals(10, is.read(b, 0, 20));
		assertEquals(-1, is.read(b, 0, 20));
	}

	@Test void a03_bulkRead_overLimit_readsLimitThenThrows() throws Exception {
		var is = stream(20, 10);
		var b = new byte[32];
		assertEquals(10, is.read(b, 0, 32));
		assertThrows(IOException.class, () -> is.read(b, 0, 32));
	}

	@Test void a04_bulkRead_overLimit_neverSilentlyTruncatesOrReturnsZero() {
		var is = stream(100, 10);
		var b = new byte[4];
		var total = new int[1];
		assertThrows(IOException.class, () -> {
			int r;
			while ((r = is.read(b, 0, 4)) != -1) {
				assertTrue(r > 0, "a bulk read must not return 0 for a non-empty buffer");
				total[0] += r;
			}
		});
		assertEquals(10, total[0]);
	}

	@Test void a05_zeroLengthRead_returnsZero() throws Exception {
		assertEquals(0, stream(5, 0).read(new byte[4], 0, 0));
	}
}
