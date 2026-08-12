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

import org.apache.juneau.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.part.*;
import org.apache.juneau.marshall.json.*;
import org.junit.jupiter.api.*;

/**
 * Unit tests for {@link RestRequest}'s header/query/form-data accumulators, {@link RestRequest#body(Object)},
 * {@link RestRequest#isBodyRepeatable()}, the {@code Accept}/{@code Content-Type} negotiation performed by the
 * private {@code buildTransportRequest()}, path-remainder substitution, and query-string assembly.
 */
@SuppressWarnings("resource") // 'captured[0]' is inspected synchronously by the fake HttpTransport lambda; the response itself is closed via try-with-resources at each call site.
class RestRequest_Test extends TestBase {

	private static TransportRequest[] capture() {
		return new TransportRequest[1];
	}

	private static HttpTransport transport(TransportRequest[] captured) {
		return tReq -> {
			captured[0] = tReq;
			return TransportResponse.builder().statusCode(200).build();
		};
	}

	// ==========================================================================
	// a — header / queryData / formData accumulators
	// ==========================================================================

	@Test
	void a01_headerSupplier_evaluatedLazily() throws Exception {
		var captured = capture();
		var counter = new int[1];
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.get("http://x/").header("X-Dyn", () -> "v" + (++counter[0])).run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("v1", captured[0].getFirstHeader("X-Dyn").value());
	}

	@Test
	void a02_queryDataVarargs_applied() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.get("http://x/").queryData(HttpPartBean.of("a", "1"), HttpPartBean.of("b", "2")).run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		var q = captured[0].getUri().getRawQuery();
		assertTrue(q.contains("a=1") && q.contains("b=2"), "Unexpected query: " + q);
	}

	@Test
	void a03_queryDataSupplier_evaluatedLazily() throws Exception {
		var captured = capture();
		var counter = new int[1];
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.get("http://x/").queryData("c", () -> "v" + (++counter[0])).run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertTrue(captured[0].getUri().getRawQuery().contains("c=v1"));
	}

	@Test
	void a04b_formData_callerContentTypeHeaderWins() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.post("http://x/").header("Content-Type", "text/existing").formData("d", "4").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("text/existing", captured[0].getFirstHeader("Content-Type").value());
		assertEquals(1, captured[0].getHeaders().stream().filter(h -> "Content-Type".equalsIgnoreCase(h.name())).count());
	}

	@Test
	void a04_formDataVarargs_applied() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.post("http://x/").formData(HttpPartBean.of("d", "4"), HttpPartBean.of("e", "5")).run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		var body = captured[0].getBody();
		assertNotNull(body);
		var out = new ByteArrayOutputStream();
		body.writeTo(out);
		var written = out.toString(java.nio.charset.StandardCharsets.UTF_8);
		assertTrue(written.contains("d=4") && written.contains("e=5"), "Unexpected form body: " + written);
		assertEquals("application/x-www-form-urlencoded", body.getContentType());
		assertEquals(written.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, body.getContentLength());
		assertTrue(body.isRepeatable());
	}

	// ==========================================================================
	// b — body(Object)
	// ==========================================================================

	private static final class NullContentTypeBody implements HttpBody {
		@Override public String getContentType() { return null; }
		@Override public void writeTo(OutputStream out) throws IOException { out.write("x".getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
		@Override public boolean isRepeatable() { return true; }
	}

	@Test
	void b01_body_httpBodyConverter_nullContentType_noHeaderAdded() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			var req = client.post("http://x/").body((Object) new NullContentTypeBody());
			assertTrue(req.isBodyRepeatable());
			try (var res = req.run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertNull(captured[0].getFirstHeader("Content-Type"));
	}

	@Test
	void b02_body_byteArrayConverter_contentTypeHeaderAdded() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			var req = client.post("http://x/").body((Object) new byte[] {1, 2, 3});
			assertTrue(req.isBodyRepeatable());
			try (var res = req.run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("application/octet-stream", captured[0].getFirstHeader("Content-Type").value());
	}

	@Test
	void b03_body_byteArrayConverter_callerContentTypeHeaderWins() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.post("http://x/").header("Content-Type", "text/existing").body((Object) new byte[] {1}).run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("text/existing", captured[0].getFirstHeader("Content-Type").value());
		// The converted body's own "application/octet-stream" must not be duplicated alongside the caller's header.
		assertEquals(1, captured[0].getHeaders().stream().filter(h -> "Content-Type".equalsIgnoreCase(h.name())).count());
	}

	@Test
	void b04_body_noConverterMatch_noDefaultSerializer_throwsIllegalState() throws Exception {
		try (var client = RestClient.create()) {
			var req = client.post("http://x/");
			var ex = assertThrows(IllegalStateException.class, () -> req.body(new Object()));
			assertTrue(ex.getMessage().contains("No default serializer"), "Unexpected message: " + ex.getMessage());
		}
	}

	@Test
	void b05_body_noConverterMatch_defaultSerializerWithContentType_used() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).defaultSerializer(JsonSerializer.DEFAULT).build()) {
			try (var res = client.post("http://x/").body(java.util.Map.of("a", 1)).run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("application/json", captured[0].getFirstHeader("Content-Type").value());
		var out = new ByteArrayOutputStream();
		captured[0].getBody().writeTo(out);
		assertEquals("{\"a\":1}", out.toString(java.nio.charset.StandardCharsets.UTF_8));
	}

	@Test
	void b06_body_noConverterMatch_defaultSerializerWithNullContentType_fallsBackToApplicationJson() throws Exception {
		var captured = capture();
		var serializer = JsonSerializer.create().produces(null).build();
		try (var client = RestClient.builder().transport(transport(captured)).defaultSerializer(serializer).build()) {
			try (var res = client.post("http://x/").body(java.util.Map.of("a", 1)).run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("application/json", captured[0].getFirstHeader("Content-Type").value());
	}

	@Test
	void b07_body_null_clearsPriorBody() throws Exception {
		try (var client = RestClient.create()) {
			var req = client.post("http://x/").body((Object) new byte[] {1});
			req.body((Object) null);
			assertTrue(req.isBodyRepeatable(), "A cleared body is trivially repeatable");
		}
	}

	// ==========================================================================
	// c — Accept header selection (buildTransportRequest)
	// ==========================================================================

	@Test
	void c01_defaultParserWithNoMediaTypes_noAcceptHeaderAdded() throws Exception {
		var captured = capture();
		var parser = org.apache.juneau.marshall.json.JsonParser.create().consumes("").build();
		try (var client = RestClient.builder().transport(transport(captured)).defaultParser(parser).build()) {
			try (var res = client.get("http://x/").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertNull(captured[0].getFirstHeader("Accept"));
	}

	// ==========================================================================
	// d — path-remainder substitution (applyPathSubstitutions)
	// ==========================================================================

	@Test
	void d01_pathRemainder_templateEndsWithSlashStar_trimmedThenAppended() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.get("http://x/foo/*").pathData("/*", "bar/baz").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("/foo/bar/baz", captured[0].getUri().getRawPath());
	}

	@Test
	void d02_pathRemainder_templateEndsWithSlash_notDoubled() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.get("http://x/foo/").pathData("/*", "bar").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("/foo/bar", captured[0].getUri().getRawPath());
	}

	@Test
	void d03_pathRemainder_templateWithoutTrailingSlash_slashInserted() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.get("http://x/foo").pathData("/*", "bar").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("/foo/bar", captured[0].getUri().getRawPath());
	}

	@Test
	void d04_pathRemainder_emptyStringRemainder_leavesTemplateUnchanged() throws Exception {
		var captured = capture();
		try (var client = RestClient.builder().transport(transport(captured)).build()) {
			try (var res = client.get("http://x/foo/*").pathData("/*", "").run()) {
				assertEquals(200, res.getStatusCode());
			}
		}
		assertEquals("/foo/*", captured[0].getUri().getRawPath());
	}

	// ==========================================================================
	// e — appendQuery malformed-URI wrapping
	// ==========================================================================

	@Test
	void e01_appendQuery_malformedBaseUrl_wrapsAsIllegalArgumentException() throws Exception {
		try (var client = RestClient.create()) {
			// An unencoded space in the host portion of the URL is invalid regardless of query data, and the
			// queryData-non-empty branch of appendQuery is reached before URI parsing fails. run() itself wraps
			// whatever buildTransportRequest() throws as a TransportException, so unwrap the cause to verify
			// appendQuery's own IllegalArgumentException ("Invalid URL: ...") was the root cause.
			var req = client.get("http://exa mple/").queryData("a", "1");
			var ex = assertThrows(TransportException.class, req::run);
			assertInstanceOf(IllegalArgumentException.class, ex.getCause());
			assertTrue(ex.getCause().getMessage().startsWith("Invalid URL:"), "Unexpected message: " + ex.getCause().getMessage());
		}
	}
}
