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

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.junit.jupiter.api.*;

import jakarta.servlet.http.*;

/**
 * Verifies that a multipart request body is bounded by the effective maximum-input ceiling: it is rejected on
 * the declared content length up front, and again on the cumulative parsed-part size as a backstop.
 */
class RequestFormParams_MaxInput_Test extends TestBase {

	@Test void a01_declaredLengthWithinCeilingAccepted() {
		RequestFormParams.checkMultipartLength(1_000_000, 500_000L);
	}

	@Test void a02_declaredLengthOverCeilingRejected() {
		assertThrows(PayloadTooLarge.class, () -> RequestFormParams.checkMultipartLength(1_000_000, 2_000_000L));
	}

	@Test void a03_absentDeclaredLengthIgnored() {
		RequestFormParams.checkMultipartLength(1_000_000, -1L);
	}

	@Test void a04_disabledCeilingSkipsDeclaredLengthCheck() {
		RequestFormParams.checkMultipartLength(0, Long.MAX_VALUE);
	}

	@Test void a05_cumulativePartsWithinCeilingAccepted() {
		RequestFormParams.checkMultipartLength(1_000_000, List.of(part(400_000), part(400_000)));
	}

	@Test void a06_cumulativePartsOverCeilingRejected() {
		assertThrows(PayloadTooLarge.class,
			() -> RequestFormParams.checkMultipartLength(1_000_000, List.of(part(600_000), part(600_000))));
	}

	@Test void a07_emptyOrNullPartsIgnored() {
		RequestFormParams.checkMultipartLength(1_000_000, (Collection<Part>)null);
		RequestFormParams.checkMultipartLength(1_000_000, List.of());
	}

	private static Part part(long size) {
		return new Part() {
			@Override public InputStream getInputStream() { throw new UnsupportedOperationException(); }
			@Override public String getContentType() { return null; }
			@Override public String getName() { return "f"; }
			@Override public String getSubmittedFileName() { return null; }
			@Override public long getSize() { return size; }
			@Override public void write(String fileName) { throw new UnsupportedOperationException(); }
			@Override public void delete() { throw new UnsupportedOperationException(); }
			@Override public String getHeader(String name) { return null; }
			@Override public Collection<String> getHeaders(String name) { return List.of(); }
			@Override public Collection<String> getHeaderNames() { return List.of(); }
		};
	}
}
