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
package org.apache.juneau.rest.client.classic;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.nio.charset.*;
import java.util.logging.*;

import org.apache.http.*;
import org.apache.http.entity.*;
import org.apache.http.message.*;
import org.apache.juneau.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.mock.classic.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;

/**
 * Verifies the fix for a bug where calling {@code RestResponse.getContent().asString()}
 * after the response is closed (e.g. inside or after try-with-resources) would throw
 * a "Stream closed" {@link IOException} when {@link DetailLevel#FULL} request logging
 * is enabled.
 *
 * <p>
 * The root cause: {@code RestResponse.close()} previously consumed/closed the underlying
 * Apache HTTP entity stream <i>before</i> running FULL logging. The FULL logging path
 * then tried to read the same (now-closed) entity to render the body, leaving
 * {@code ResponseContent.body} unset. A subsequent user call to
 * {@code getContent().asString()} would re-attempt to read the closed stream and surface
 * the exception to the caller.
 *
 * <p>
 * {@link MockRestClient} alone is insufficient to reproduce the bug because it wraps the
 * response in an {@code InputStreamEntity} backed by {@link ByteArrayInputStream}, whose
 * {@link InputStream#close()} is a no-op — so even after close(), the stream can be
 * re-read. The tests below inject a streaming entity whose underlying input stream
 * faithfully throws "Stream closed" after close.
 */
class RestClient_Logging_StreamClosed_Test extends TestBase {

	@Rest
	public static class A extends BasicRestResource {
		@RestGet
		public String hello() {
			return "hello world";
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test helpers.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Streaming input stream that throws {@link IOException} on any read attempt after close.
	 * Mimics real HTTP client behavior (the Apache HTTP client closes the network stream when
	 * {@code EntityUtils.consume} runs, and subsequent reads fail).
	 */
	private static final class FailingAfterCloseStream extends InputStream {
		private final ByteArrayInputStream delegate;
		private boolean closed;

		FailingAfterCloseStream(byte[] data) {
			delegate = new ByteArrayInputStream(data);
		}

		@Override public int read() throws IOException {
			if (closed) throw new IOException("Stream closed");
			return delegate.read();
		}

		@Override public int read(byte[] b, int off, int len) throws IOException {
			if (closed) throw new IOException("Stream closed");
			return delegate.read(b, off, len);
		}

		@Override public void close() throws IOException {
			closed = true;
			delegate.close();
		}
	}

	/**
	 * Test client that lets us inject an arbitrary {@link HttpEntity} as the response body.
	 */
	public static class TestClient extends MockRestClient {
		private HttpEntity responseEntity;

		public TestClient(MockRestClient.Builder builder) {
			super(builder);
		}

		public TestClient entity(HttpEntity entity) {
			responseEntity = entity;
			return this;
		}

		@Override
		protected MockRestResponse createResponse(RestRequest request, HttpResponse httpResponse, Parser parser) throws RestCallException {
			var r = new BasicHttpResponse(new ProtocolVersion("http", 1, 1), 200, "");
			r.setEntity(responseEntity);
			r.addHeader("Content-Type", "application/json");
			return new MockRestResponse(this, request, r, parser);
		}
	}

	private static InputStreamEntity failingEntity(String body) {
		var data = body.getBytes(StandardCharsets.UTF_8);
		return new InputStreamEntity(new FailingAfterCloseStream(data));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Reproduction / regression tests for the FULL logging stream-closed bug.
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * After fix: closing the response and then calling {@code asString()} should
	 * return the body instead of throwing "Stream closed".
	 */
	@Test void a01_asStringAfterCloseWithFullLogging() throws Exception {
		var c = MockConsole.create();
		var l = MockLogger.create();

		var client = MockRestClient.create(A.class).json()
			.logRequests(DetailLevel.FULL, Level.SEVERE, null)
			.logToConsole()
			.logger(l)
			.console(c)
			.build(TestClient.class)
			.entity(failingEntity("\"hello world\""));

		var response = client.get("/hello").run();
		response.close();
		var body = response.getContent().asString();
		assertEquals("\"hello world\"", body);
	}

	/**
	 * After fix: try-with-resources + asString() works the same as before, without
	 * stream-closed errors, even with the failing stream wrapper.
	 */
	@Test void a02_asStringInsideTryWithResources() throws Exception {
		var c = MockConsole.create();
		var l = MockLogger.create();

		var client = MockRestClient.create(A.class).json()
			.logRequests(DetailLevel.FULL, Level.SEVERE, null)
			.logToConsole()
			.logger(l)
			.console(c)
			.build(TestClient.class)
			.entity(failingEntity("\"hello world\""));

		try (var response = client.get("/hello").run()) {
			var body = response.getContent().asString();
			assertEquals("\"hello world\"", body);
		}
	}

	/**
	 * After fix: FULL logging output still contains the response body (the buffering
	 * fix must not regress what FULL logging actually logs).
	 */
	@Test void b01_fullLoggingStillContainsResponseBody() throws Exception {
		var c = MockConsole.create();
		c.reset();
		var l = MockLogger.create();

		var client = MockRestClient.create(A.class).json()
			.logRequests(DetailLevel.FULL, Level.SEVERE, null)
			.logToConsole()
			.logger(l)
			.console(c)
			.build(TestClient.class)
			.entity(failingEntity("\"hello world\""));

		client.get("/hello").complete();

		var logged = c.toString();
		assertTrue(logged.contains("---response content---"), () -> "Missing response-content marker in: " + logged);
		assertTrue(logged.contains("\"hello world\""), () -> "Missing response body in log output: " + logged);
	}

	/**
	 * After fix: when FULL logging is NOT enabled, the response body is NOT eagerly
	 * cached — original behavior preserved.  Reading via {@code asString()} after
	 * {@code close()} should fail on a non-repeatable closed entity, just like before
	 * the fix.  This guards against accidental over-buffering for the SIMPLE/NONE
	 * paths.
	 */
	@Test void c01_nonFullLoggingDoesNotBufferEntity() throws Exception {
		var l = MockLogger.create();

		var client = MockRestClient.create(A.class).json()
			.logRequests(DetailLevel.NONE, Level.SEVERE, null)
			.logger(l)
			.build(TestClient.class)
			.entity(failingEntity("\"hello world\""));

		var response = client.get("/hello").run();
		response.close();
		assertThrows(RestCallException.class, () -> response.getContent().asString());
	}

	/**
	 * After fix: SIMPLE logging continues to log just the status line (no body access).
	 */
	@Test void c02_simpleLoggingDoesNotTouchBody() throws Exception {
		var c = MockConsole.create();
		c.reset();
		var l = MockLogger.create();

		var client = MockRestClient.create(A.class).json()
			.logRequests(DetailLevel.SIMPLE, Level.SEVERE, null)
			.logToConsole()
			.logger(l)
			.console(c)
			.build(TestClient.class)
			.entity(failingEntity("\"hello world\""));

		client.get("/hello").complete();

		var logged = c.toString();
		assertTrue(logged.contains("HTTP GET"), () -> "Expected SIMPLE log line, got: " + logged);
		assertFalse(logged.contains("---response content---"),
			() -> "SIMPLE log should not contain body markers: " + logged);
	}
}
