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

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.http.Content;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.json.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty regression coverage for {@link RequestContent} draining a parsed request entity to end-of-stream.
 *
 * <p>
 * A reader parser stops as soon as the top-level value is complete, so without an explicit drain the
 * zero-length terminating chunk of a {@code Transfer-Encoding: chunked} request body is never read whenever it
 * arrives after the JSON bytes.  The container then finds unconsumed request content when the (already committed)
 * response completes and aborts the connection &mdash; with no {@code Connection: close} header, because the
 * headers have already gone out &mdash; so a client reusing that pooled keep-alive connection for its next request
 * fails with an immediate EOF.  Streaming HTTP clients (the JDK {@code HttpClient} with an
 * {@code ofInputStream} publisher, Apache HttpClient with a streaming entity) routinely flush the terminator
 * separately from the content, so this surfaced as an intermittent stale-connection failure on the MCP
 * elicitation resume POST.
 *
 * <p>
 * The exchanges below are driven over a raw socket so the terminator timing is deterministic rather than at the
 * mercy of a client library's write scheduling.
 */
@org.apache.juneau.testing.JettyMicroserviceTest
@SuppressWarnings({
	"java:S2925" // The sleep deliberately holds back the chunk terminator past the handler's completion; there is no server-side seam to latch on.
})
class RequestContent_ChunkedKeepAlive_JettyMicroservice_Test extends TestBase {

	@Rest(paths="/echo/*", serializers=JsonSerializer.class, parsers=JsonParser.class, defaultAccept="application/json")
	public static class EchoServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestPost("/")
		public JsonMap echo(@Content JsonMap body) {
			return body;
		}
	}

	@Configuration
	public static class Config {
		@Bean(name="echoServlet")
		public Servlet echoServlet() { return new EchoServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(Config.class);

	/** How long the terminating chunk is held back: comfortably longer than the handler takes on a warm connection. */
	private static final long TERMINATOR_DELAY_MS = 300;

	@Test void a01_lateChunkTerminator_keepsConnectionAlive() throws Exception {
		var uri = fixture.getRootUrl();
		try (var socket = new Socket(uri.getHost(), uri.getPort())) {
			socket.setSoTimeout(10_000);
			var out = socket.getOutputStream();
			var in = new BufferedInputStream(socket.getInputStream());
			var statuses = new ArrayList<String>();

			// Warm-up exchange, so the late-terminator exchange below is handled faster than the delay.
			statuses.add(exchange(out, in, "{\"n\":1}", 0));
			// The JSON is complete but its 0-chunk arrives only after the handler has returned.
			statuses.add(exchange(out, in, "{\"n\":2}", TERMINATOR_DELAY_MS));
			// Reuses the same keep-alive connection; fails with EOF/broken pipe if the server aborted it.
			statuses.add(exchange(out, in, "{\"n\":3}", 0));

			assertList(statuses, "HTTP/1.1 200 OK {\"n\":1}", "HTTP/1.1 200 OK {\"n\":2}", "HTTP/1.1 200 OK {\"n\":3}");
		}
	}

	// Sends one chunked POST (terminator delayed by delayMs) and returns "<status-line> <body>" of the response.
	private static String exchange(OutputStream out, InputStream in, String json, long delayMs) throws Exception {
		var body = json.getBytes(UTF_8);
		out.write(("POST /echo/ HTTP/1.1\r\nHost: localhost\r\nContent-Type: application/json\r\nAccept: application/json\r\n"
			+ "Transfer-Encoding: chunked\r\n\r\n" + Integer.toHexString(body.length) + "\r\n").getBytes(ISO_8859_1));
		out.write(body);
		out.write("\r\n".getBytes(ISO_8859_1));
		out.flush();
		if (delayMs > 0)
			Thread.sleep(delayMs);
		out.write("0\r\n\r\n".getBytes(ISO_8859_1));
		out.flush();
		return readResponse(in);
	}

	// Reads one HTTP/1.1 response (chunked or Content-Length framed) and returns "<status-line> <body>".
	private static String readResponse(InputStream in) throws IOException {
		var status = readLine(in);
		if (status == null)
			throw new EOFException("Connection closed by server before a response status line was received.");
		var chunked = false;
		var contentLength = -1;
		for (var line = readLine(in); line != null && ! line.isEmpty(); line = readLine(in)) {
			var lc = line.toLowerCase(Locale.ROOT);
			if (lc.startsWith("transfer-encoding:") && lc.contains("chunked"))
				chunked = true;
			else if (lc.startsWith("content-length:"))
				contentLength = Integer.parseInt(lc.substring(15).trim());
		}
		var body = new ByteArrayOutputStream();
		if (chunked) {
			for (var size = Integer.parseInt(readLine(in).trim(), 16); size > 0; size = Integer.parseInt(readLine(in).trim(), 16)) {
				body.write(in.readNBytes(size));
				readLine(in);
			}
			readLine(in);
		} else if (contentLength > 0) {
			body.write(in.readNBytes(contentLength));
		}
		return status + " " + body.toString(UTF_8);
	}

	private static String readLine(InputStream in) throws IOException {
		var sb = new StringBuilder();
		for (var c = in.read(); c != -1; c = in.read()) {
			if (c == '\n')
				return sb.toString();
			if (c != '\r')
				sb.append((char)c);
		}
		return sb.isEmpty() ? null : sb.toString();
	}
}
