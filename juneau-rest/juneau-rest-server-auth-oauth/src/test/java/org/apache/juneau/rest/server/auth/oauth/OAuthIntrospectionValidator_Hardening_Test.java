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
package org.apache.juneau.rest.server.auth.oauth;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.time.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.auth.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * Tests for the audience requirement, issuer check, timeouts and negative cache of {@link OAuthIntrospectionValidator}.
 *
 * @since 10.0.0
 */
class OAuthIntrospectionValidator_Hardening_Test extends TestBase {

	private HttpServer server;
	private volatile String nextResponse = "{\"active\":false}";
	private volatile long delayMs;
	private final AtomicInteger calls = new AtomicInteger();
	private final CountDownLatch release = new CountDownLatch(1);

	@BeforeEach
	void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
		server.createContext("/introspect", ex -> {
			calls.incrementAndGet();
			if (delayMs > 0) {
				try {
					release.await(delayMs, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			var body = nextResponse.getBytes(StandardCharsets.UTF_8);
			ex.getResponseHeaders().add("Content-Type", "application/json");
			ex.sendResponseHeaders(200, body.length);
			try (var os = ex.getResponseBody()) {
				os.write(body);
			} catch (IOException e) {
				// Client timed out and went away.
			}
		});
		server.start();
	}

	@AfterEach
	void stopServer() {
		release.countDown();
		server.stop(0);
	}

	private OAuthIntrospectionValidator.Builder builder() {
		return OAuthIntrospectionValidator.create()
			.introspectionEndpoint(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/introspect"))
			.clientId("client")
			.clientSecret("secret");
	}

	@Test void a01_build_requiresAudienceOrExplicitAny() {
		var noAudience = builder();
		assertThrows(IllegalStateException.class, noAudience::build);
		assertNotNull(builder().audience("api").build());
		assertNotNull(builder().anyAudience().build());
	}

	@Test void b01_issuer_mismatchRejected_matchAccepted() throws Exception {
		var v = builder().anyAudience().issuer("https://idp.example").build();
		nextResponse = "{\"active\":true,\"sub\":\"a\",\"iss\":\"https://other.example\"}";
		assertThrows(AuthenticationException.class, () -> v.validate("t1"));
		nextResponse = "{\"active\":true,\"sub\":\"a\"}";
		assertThrows(AuthenticationException.class, () -> v.validate("t2"));
		nextResponse = "{\"active\":true,\"sub\":\"a\",\"iss\":\"https://idp.example\"}";
		assertEquals("a", v.validate("t3").getName());
	}

	@Test void b02_noIssuerConfigured_issuerIgnored() throws Exception {
		var v = builder().anyAudience().build();
		nextResponse = "{\"active\":true,\"sub\":\"a\",\"iss\":\"https://other.example\"}";
		assertEquals("a", v.validate("t1").getName());
	}

	@Test void c01_timeout_slowEndpointFailsFast() {
		delayMs = 3000;
		var v = builder().anyAudience().httpTimeout(Duration.ofMillis(300)).build();
		var start = System.nanoTime();
		assertThrows(AuthenticationException.class, () -> v.validate("slow"));
		assertTrue(Duration.ofNanos(System.nanoTime() - start).toMillis() < 2500);
	}

	@Test void c02_httpTimeout_invalidRejected() {
		var b = builder();
		assertThrows(IllegalArgumentException.class, () -> b.httpTimeout(Duration.ZERO));
	}

	@Test void d01_negativeCache_inactiveTokenNotReintrospected() {
		var v = builder().anyAudience().build();
		nextResponse = "{\"active\":false}";
		assertThrows(AuthenticationException.class, () -> v.validate("bad"));
		assertThrows(AuthenticationException.class, () -> v.validate("bad"));
		assertEquals(1, calls.get());
		assertThrows(AuthenticationException.class, () -> v.validate("other-bad"));
		assertEquals(2, calls.get());
	}

	@Test void d02_negativeCache_disabledWithZero() {
		var v = builder().anyAudience().negativeCacheTtl(Duration.ZERO).build();
		assertThrows(AuthenticationException.class, () -> v.validate("bad"));
		assertThrows(AuthenticationException.class, () -> v.validate("bad"));
		assertEquals(2, calls.get());
	}

	@Test void d03_negativeCache_audienceMismatchRemembered() {
		var v = builder().audience("api").build();
		nextResponse = "{\"active\":true,\"sub\":\"a\",\"aud\":\"x\"}";
		assertThrows(AuthenticationException.class, () -> v.validate("t"));
		assertThrows(AuthenticationException.class, () -> v.validate("t"));
		assertEquals(1, calls.get());
	}

	@Test void d04_negativeCache_expires() {
		var clock = new MutableClock();
		var v = builder().anyAudience().clock(clock).negativeCacheTtl(Duration.ofSeconds(30)).build();
		assertThrows(AuthenticationException.class, () -> v.validate("bad"));
		clock.advance(Duration.ofSeconds(31));
		assertThrows(AuthenticationException.class, () -> v.validate("bad"));
		assertEquals(2, calls.get());
	}

	@Test void d05_negativeCacheTtl_negativeRejected() {
		var b = builder();
		var negative = Duration.ofSeconds(-1);
		assertThrows(IllegalArgumentException.class, () -> b.negativeCacheTtl(negative));
	}

	@Test void e01_cache_isPerValidator() throws Exception {
		// Each validator owns its own default cache, so a token accepted under one policy is re-checked under another.
		nextResponse = "{\"active\":true,\"sub\":\"a\",\"scope\":\"read\",\"aud\":\"api\"}";
		var lax = builder().audience("api").build();
		var strict = builder().audience("api").requiredScopes("admin").build();
		assertEquals("a", lax.validate("shared").getName());
		assertThrows(AuthenticationException.class, () -> strict.validate("shared"));
		assertNotSame(lax.getTokenCache(), strict.getTokenCache());
	}

	@Test void e02_sharedCache_eachValidatorReappliesOwnChecks() throws Exception {
		// A cache shared by differently configured validators holds only the raw introspection result.
		nextResponse = "{\"active\":true,\"sub\":\"a\",\"aud\":\"api-one\"}";
		var shared = BoundedLruTokenCache.create();
		var one = builder().audience("api-one").tokenCache(shared).build();
		var two = builder().audience("api-two").tokenCache(shared).build();
		assertEquals("a", one.validate("t").getName());
		assertEquals(1, calls.get());
		assertEquals("a", one.validate("t").getName());
		assertEquals(1, calls.get());
		assertThrows(AuthenticationException.class, () -> two.validate("t"));
		assertEquals(1, calls.get());
	}

	@Test void e03_cacheHit_expiredTokenRejected() throws Exception {
		var clock = new MutableClock();
		var exp = clock.instant().plusSeconds(60).getEpochSecond();
		nextResponse = "{\"active\":true,\"sub\":\"a\",\"exp\":" + exp + "}";
		var v = builder().anyAudience().clock(clock).build();
		assertEquals("a", v.validate("t").getName());
		clock.advance(Duration.ofSeconds(61));
		assertThrows(AuthenticationException.class, () -> v.validate("t"));
	}

	@Test void f01_errorCode_withUnsafeCharacters_notReflected() {
		var v = builder().anyAudience().build();
		nextResponse = "{\"error\":\"bad\\\\code\"}";
		var ex = assertThrows(AuthenticationException.class, () -> v.validate("t"));
		assertFalse(String.valueOf(ex.getMessage()).contains("\\"));
	}

	private static final class MutableClock extends Clock {
		private Instant now = Instant.parse("2026-01-01T00:00:00Z");
		void advance(Duration d) {
			now = now.plus(d);
		}
		@Override public ZoneId getZone() {
			return ZoneOffset.UTC;
		}
		@Override public Clock withZone(ZoneId zone) {
			return this;
		}
		@Override public Instant instant() {
			return now;
		}
	}
}
