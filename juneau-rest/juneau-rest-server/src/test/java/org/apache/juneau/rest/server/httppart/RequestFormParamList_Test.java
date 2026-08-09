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
import static org.mockito.Mockito.*;

import java.util.*;

import org.apache.juneau.http.response.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.Part;

/**
 * Tests for the multipart-body size enforcement in {@link RequestFormParamList}, exercised via the
 * package-private {@code checkMultipartLength} overloads.
 *
 * @since 10.0.0
 */
class RequestFormParamList_Test {

	private static Part partOfSize(long size) {
		var p = mock(Part.class);
		when(p.getSize()).thenReturn(size);
		return p;
	}

	// -----------------------------------------------------------------------------------------
	// a — declared Content-Length pre-check
	// -----------------------------------------------------------------------------------------

	@Test void a01_declaredLength_withinMaxInput_ok() {
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(1_000_000L, 500_000L));
	}

	@Test void a02_declaredLength_exceedsMaxInput_throwsPayloadTooLarge() {
		var ex = assertThrows(PayloadTooLarge.class, () -> RequestFormParamList.checkMultipartLength(1_000_000L, 5_000_000L));
		assertEquals(413, ex.getStatusCode());
	}

	@Test void a03_declaredLength_absent_ok() {
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(1_000_000L, -1L));
	}

	@Test void a04_maxInputNotPositive_disablesCheck() {
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(0L, Long.MAX_VALUE));
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(-1L, Long.MAX_VALUE));
	}

	// -----------------------------------------------------------------------------------------
	// b — parsed-parts cumulative-size backstop
	// -----------------------------------------------------------------------------------------

	@Test void b01_parts_withinMaxInput_ok() {
		var parts = List.of(partOfSize(100), partOfSize(200));
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(1_000L, parts));
	}

	@Test void b02_parts_cumulativeExceedsMaxInput_throwsPayloadTooLarge() {
		var parts = List.of(partOfSize(600), partOfSize(600));
		var ex = assertThrows(PayloadTooLarge.class, () -> RequestFormParamList.checkMultipartLength(1_000L, parts));
		assertEquals(413, ex.getStatusCode());
	}

	@Test void b03_nullOrEmptyParts_ok() {
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(1_000L, null));
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(1_000L, List.of()));
	}

	@Test void b04_maxInputNotPositive_disablesCheck() {
		var parts = List.of(partOfSize(Long.MAX_VALUE));
		assertDoesNotThrow(() -> RequestFormParamList.checkMultipartLength(0L, parts));
	}
}
