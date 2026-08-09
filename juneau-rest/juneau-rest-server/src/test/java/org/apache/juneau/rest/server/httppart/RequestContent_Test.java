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
package org.apache.juneau.rest.server.httppart;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link RequestContent#getReader()}'s buffer-sizing policy, exercised via the package-private
 * {@link RequestContent#computeReaderBufferSize(int, long)} helper.
 *
 * @since 10.0.0
 */
class RequestContent_Test {

	private static final long DEFAULT_MAX_INPUT = 100_000_000L;

	@Test void a01_noContentLength_usesDefaultFloor() {
		assertEquals(8192, RequestContent.computeReaderBufferSize(-1, DEFAULT_MAX_INPUT));
		assertEquals(8192, RequestContent.computeReaderBufferSize(0, DEFAULT_MAX_INPUT));
	}

	@Test void a02_smallContentLength_usesDefaultFloor() {
		assertEquals(8192, RequestContent.computeReaderBufferSize(100, DEFAULT_MAX_INPUT));
	}

	@Test void a03_contentLengthUnderMaxInput_usesDeclaredLength() {
		assertEquals(50_000, RequestContent.computeReaderBufferSize(50_000, DEFAULT_MAX_INPUT));
	}

	@Test void a04_spoofedContentLengthNearIntMax_clampedToMaxInput() {
		assertEquals(DEFAULT_MAX_INPUT, RequestContent.computeReaderBufferSize(Integer.MAX_VALUE - 1, DEFAULT_MAX_INPUT));
	}

	@Test void a05_spoofedContentLength_clampedToSmallConfiguredMaxInput() {
		assertEquals(1_000, RequestContent.computeReaderBufferSize(Integer.MAX_VALUE - 1, 1_000L));
	}

	@Test void a06_maxInputNotPositive_fallsBackToIntMax() {
		assertEquals(Integer.MAX_VALUE, RequestContent.computeReaderBufferSize(Integer.MAX_VALUE, 0L));
		assertEquals(Integer.MAX_VALUE, RequestContent.computeReaderBufferSize(Integer.MAX_VALUE, -1L));
	}

	@Test void a07_maxInputAboveIntRange_resultClampedToIntMax() {
		assertEquals(Integer.MAX_VALUE, RequestContent.computeReaderBufferSize(Integer.MAX_VALUE, Long.MAX_VALUE));
	}
}
