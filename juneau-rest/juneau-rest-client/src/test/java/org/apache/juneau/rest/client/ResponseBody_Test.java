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
package org.apache.juneau.rest.client;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;

import org.apache.juneau.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link ResponseBody#asString(Charset)}, {@link ResponseBody#asBytes()}, and the
 * {@code negotiatedParser()} helper backing {@link ResponseBody#asCursor(Class)}/{@link ResponseBody#as(Class)}.
 */
class ResponseBody_Test extends TestBase {

	@SuppressWarnings("resource") // 'tr' is handed to (and closed by) the returned RestResponse.
	private static RestResponse response(int statusCode, String contentType, InputStream bodyStream) {
		var b = TransportResponse.builder().statusCode(statusCode);
		if (contentType != null)
			b.header("Content-Type", contentType);
		if (bodyStream != null)
			b.body(bodyStream);
		return new RestResponse(b.build(), RestClient.builder().parser(JsonParser.DEFAULT).build());
	}

	// ==========================================================================
	// a — asString(Charset) / asBytes()
	// ==========================================================================

	@Test
	void a01_asStringWithCharset_noBody_returnsNull() throws Exception {
		try (var resp = response(204, null, null)) {
			assertNull(resp.body().asString(StandardCharsets.UTF_8));
		}
	}

	@Test
	void a02_asStringWithCharset_decodesUsingGivenCharset() throws Exception {
		var bytes = "caf\u00e9".getBytes(StandardCharsets.ISO_8859_1);
		try (var resp = response(200, "text/plain", new ByteArrayInputStream(bytes))) {
			assertEquals("caf\u00e9", resp.body().asString(StandardCharsets.ISO_8859_1));
		}
	}

	@Test
	void a03_asBytes_noBody_returnsNull() throws Exception {
		try (var resp = response(204, null, null)) {
			assertNull(resp.body().asBytes());
		}
	}

	// ==========================================================================
	// b — negotiatedParser() (indirectly via as(Class)/asCursor(Class))
	// ==========================================================================

	@Test
	void b01_negotiatedParser_noContentTypeHeader_matchesUnsupportedMediaType() throws Exception {
		try (var resp = response(200, null, new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)))) {
			var ex = assertThrows(UnsupportedMediaType.class, () -> resp.body().as(Object.class));
			assertTrue(ex.getMessage().contains("'null'"), "Unexpected message: " + ex.getMessage());
		}
	}

	@Test
	void b02_negotiatedParser_unmatchedContentType_throwsUnsupportedMediaType() throws Exception {
		try (var resp = response(200, "application/x-unregistered", new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8)))) {
			var ex = assertThrows(UnsupportedMediaType.class, () -> resp.body().as(Object.class));
			assertTrue(ex.getMessage().contains("application/x-unregistered"), "Unexpected message: " + ex.getMessage());
		}
	}

	@Test
	void b03_negotiatedParser_matchedContentType_parses() throws Exception {
		try (var resp = response(200, "application/json", new ByteArrayInputStream("\"hi\"".getBytes(StandardCharsets.UTF_8)))) {
			assertEquals("hi", resp.body().as(String.class));
		}
	}
}
