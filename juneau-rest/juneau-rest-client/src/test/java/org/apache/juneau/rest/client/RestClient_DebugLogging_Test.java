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
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.logging.*;
import org.junit.jupiter.api.*;

/**
 * Tests for level-driven REST client debug logging.
 */
@SuppressWarnings({
	"resource" // Clients/responses in tests are closed with try-with-resources.
})
class RestClient_DebugLogging_Test extends TestBase {

	@Test
	void a01_infoTier_emitsBasicOnly() throws Exception {
		var loggerName = getClass().getName() + ".a01";
		var logger = RichLogger.getLogger(loggerName);
		var prevLevel = logger.getLevel();
		logger.setLevel(Level.INFO);
		try (var c = logger.captureEvents(Level.INFO)) {
			try (var client = RestClient.builder()
				.transport(req -> response(200, "ok"))
				.debugLoggerName(loggerName)
				.build();
				var res = client.get("http://example.com/basic").run()) {
				assertSame(logger, client.debugLogger);
				assertEquals(Level.INFO, RestClientDebugPipeline.resolveTier(client.debugLogger));
				res.body().asString();
			}
			assertNotNull(c.last());
			var msg = c.last().getMessage();
			assertEquals(Level.INFO, c.last().getLevel());
			assertTrue(msg.contains("[200] HTTP GET http://example.com/basic"));
			assertFalse(msg.contains("---Request Headers---"));
			assertFalse(msg.contains("---Request Content UTF-8---"));
		} finally {
			logger.setLevel(prevLevel);
		}
	}

	@Test
	void a02_fineTier_includesHeadersWithoutBodies() throws Exception {
		var loggerName = getClass().getName() + ".a02";
		var logger = RichLogger.getLogger(loggerName);
		var prevLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		try (var c = logger.captureEvents(Level.FINE)) {
			try (var client = RestClient.builder()
				.transport(req -> TransportResponse.builder().statusCode(200).header("X-Res", "rv").body(new ByteArrayInputStream("rb".getBytes(StandardCharsets.UTF_8))).build())
				.debugLoggerName(loggerName)
				.build();
				var res = client.get("http://example.com/h").header("Authorization", "secret").run()) {
				res.body().asString();
			}
			assertNotNull(c.last());
			assertEquals(Level.INFO, c.last().getLevel());
			var msg = c.last().getMessage();
			assertTrue(msg.contains("---Request Headers---"));
			assertTrue(msg.contains("Authorization: [REDACTED]"));
			assertTrue(msg.contains("---Response Headers---"));
			assertTrue(msg.contains("X-Res: rv"));
			assertFalse(msg.contains("---Request Content UTF-8---"));
		} finally {
			logger.setLevel(prevLevel);
		}
	}

	@Test
	void a03_finestTier_includesBodiesWithTruncation() throws Exception {
		var loggerName = getClass().getName() + ".a03";
		var logger = RichLogger.getLogger(loggerName);
		var prevLevel = logger.getLevel();
		logger.setLevel(Level.FINEST);
		// Gate forced on: the response body must actually render for this test's truncation-at-cap intent to be
		// observable at all once the secure-by-default no-dump gate exists (Phase 3).
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		try (var c = logger.captureEvents(Level.FINEST)) {
			try (var client = RestClient.builder()
				.transport(req -> {
					if (req.getBody() != null) {
						try {
							req.getBody().writeTo(new ByteArrayOutputStream());
						} catch (IOException e) {
							throw new RuntimeException(e);
						}
					}
					// Renderable Content-Type required on the response side: isBodyRenderable(null) is false by
					// design (fail-closed on absent/unknown types), so an untyped response would otherwise still
					// render the "body not rendered" placeholder instead of the raw truncated text.
					return response(200, "uvwxyz", "text/plain");
				})
				.debugLoggerName(loggerName)
				.debugFormatter(new BasicRestClientDebugFormatter().bodyCap(4))
				.build();
				var res = client.post("http://example.com/body").bodyString("abcdef").run()) {
				res.body().asString();
			}
			assertNotNull(c.last());
			assertEquals(Level.INFO, c.last().getLevel());
			var msg = c.last().getMessage();
			assertTrue(msg.contains("---Request Content---"));
			assertTrue(msg.contains("abcd"));
			assertTrue(msg.contains("…[truncated 2 bytes]"));
			assertTrue(msg.contains("---Response Content---"));
			assertTrue(msg.contains("uvwx"));
			// Proves the old unconditional UTF-8+hex dump is gone, not just that the new label is present.
			assertFalse(msg.contains("UTF-8---"));
			assertFalse(msg.contains("Content Hex"));
		} finally {
			logger.setLevel(prevLevel);
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void a04_finestTier_drainsBodyOnCloseWhenPartiallyRead() throws Exception {
		var loggerName = getClass().getName() + ".a04";
		var logger = RichLogger.getLogger(loggerName);
		var prevLevel = logger.getLevel();
		logger.setLevel(Level.FINEST);
		BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(Boolean.TRUE);
		try (var c = logger.captureEvents(Level.FINEST)) {
			try (var client = RestClient.builder()
				.transport(req -> response(200, "abcdef", "text/plain"))
				.debugLoggerName(loggerName)
				.build();
				var res = client.get("http://example.com/partial").run()) {
				var stream = res.body().asStream();
				assertNotNull(stream);
				assertEquals('a', stream.read());
			}
			assertNotNull(c.last());
			var msg = c.last().getMessage();
			assertTrue(msg.contains("---Response Content---"));
			assertTrue(msg.contains("abcdef"));
			assertFalse(msg.contains("UTF-8---"));
			assertFalse(msg.contains("Content Hex"));
		} finally {
			logger.setLevel(prevLevel);
			BasicRestClientDebugFormatter.resetAllowDumpBodiesForTest(null);
		}
	}

	@Test
	void a05_defaultLoggerName_isRestClientClassName() throws Exception {
		var logger = RichLogger.getLogger(RestClient.class.getName());
		var prevLevel = logger.getLevel();
		logger.setLevel(Level.INFO);
		try (var c = logger.captureEvents(Level.INFO)) {
			try (var client = RestClient.builder().transport(req -> response(200, "ok")).build();
				var res = client.get("http://example.com/default").run()) {
				res.body().asString();
			}
			assertNotNull(c.last());
			assertEquals(RestClient.class.getName(), c.last().getLoggerName());
		} finally {
			logger.setLevel(prevLevel);
		}
	}

	/**
	 * Proves stable-{@code INFO} stamping applies uniformly to the second {@code emit(...)} call site
	 * ({@code RestRequest.run()}'s transport-exception synthetic-response path), not just the normal
	 * {@code RestResponse.close()} path exercised by {@code a01}-{@code a04}.
	 */
	@Test
	void a06_transportExceptionPath_stampsInfoRegardlessOfTier() throws Exception {
		var loggerName = getClass().getName() + ".a06";
		var logger = RichLogger.getLogger(loggerName);
		var prevLevel = logger.getLevel();
		logger.setLevel(Level.FINE);
		try (var c = logger.captureEvents(Level.FINE)) {
			try (var client = RestClient.builder()
				.transport(req -> { throw new TransportException("boom"); })
				.debugLoggerName(loggerName)
				.build()) {
				assertThrows(TransportException.class, () -> client.get("http://example.com/err").run());
			}
			assertNotNull(c.last());
			assertEquals(Level.INFO, c.last().getLevel());
		} finally {
			logger.setLevel(prevLevel);
		}
	}

	private static TransportResponse response(int statusCode, String body) {
		return TransportResponse.builder()
			.statusCode(statusCode)
			.reasonPhrase("OK")
			.body(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))
			.build();
	}

	private static TransportResponse response(int statusCode, String body, String contentType) {
		return TransportResponse.builder()
			.statusCode(statusCode)
			.reasonPhrase("OK")
			.header("Content-Type", contentType)
			.body(new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)))
			.build();
	}
}
