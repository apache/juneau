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
package org.apache.juneau.rest.httppart;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Verifies that the reader buffer size derived from the client-supplied content length is clamped to the
 * effective maximum-input ceiling, so a spoofed content length can't drive an oversized pre-allocation.
 */
class RequestContent_BufferSize_Test extends TestBase {

	@Test void a01_absentLengthUsesDefault() {
		assertEquals(8192, RequestContent.computeReaderBufferSize(0, 1_000_000));
		assertEquals(8192, RequestContent.computeReaderBufferSize(-1, 1_000_000));
	}

	@Test void a02_smallLengthRaisedToDefault() {
		assertEquals(8192, RequestContent.computeReaderBufferSize(100, 1_000_000));
	}

	@Test void a03_lengthUsedWhenWithinCeiling() {
		assertEquals(50_000, RequestContent.computeReaderBufferSize(50_000, 1_000_000));
	}

	@Test void a04_lengthClampedToCeiling() {
		// A spoofed near-max content length must not size the buffer past the ceiling.
		assertEquals(1_000_000, RequestContent.computeReaderBufferSize(Integer.MAX_VALUE, 1_000_000));
	}

	@Test void a05_noCeilingHonorsLength() {
		assertEquals(Integer.MAX_VALUE, RequestContent.computeReaderBufferSize(Integer.MAX_VALUE, 0));
	}
}
