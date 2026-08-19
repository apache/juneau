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
package org.apache.juneau.commons.utils;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;
import java.util.zip.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ChecksumUtils}.
 */
class ChecksumUtils_Test extends TestBase {

	@Test void a01_hash8_emptyInput_isAllZeros() {
		// CRC32 of zero bytes is 0, so the zero-padded hex form is all zeros.
		assertEquals("00000000", ChecksumUtils.hash8(new byte[0]));
	}

	@Test void a02_hash8_knownVector_matchesJavaUtilZipCrc32() {
		var bytes = "The quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
		var crc = new CRC32();
		crc.update(bytes);
		assertEquals(String.format("%08x", crc.getValue()), ChecksumUtils.hash8(bytes));
		// Widely-published CRC32 test vector for this exact string - pins the value, not just the delegation.
		assertEquals("414fa339", ChecksumUtils.hash8(bytes));
	}

	@Test void a03_hash8_leadingZeroPadding_isNotTruncated() {
		// CRC32("62") == 0x0012d20a - fewer than 8 significant hex digits, so this pins that hash8 zero-pads
		// rather than truncates (a raw Long.toHexString(...)/Integer.toHexString(...) would drop the leading
		// zeros and return a shorter string).
		assertEquals("0012d20a", ChecksumUtils.hash8("62".getBytes(StandardCharsets.UTF_8)));
	}

	@Test void a04_hash8_distinctInputs_produceDifferentHashes() {
		assertNotEquals(ChecksumUtils.hash8("a".getBytes(StandardCharsets.UTF_8)), ChecksumUtils.hash8("b".getBytes(StandardCharsets.UTF_8)));
	}

	@Test void a05_hash8_sameInput_isDeterministic() {
		var bytes = "deterministic".getBytes(StandardCharsets.UTF_8);
		assertEquals(ChecksumUtils.hash8(bytes), ChecksumUtils.hash8(bytes));
	}
}
