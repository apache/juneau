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

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.*;

import org.apache.http.*;
import org.apache.http.client.protocol.*;
import org.apache.http.protocol.*;
import org.apache.juneau.*;
import org.junit.jupiter.api.*;

import com.sun.net.httpserver.*;

/**
 * End-to-end tests of the redirect credential guard using two local HTTP servers.
 */
class RedirectCredentialGuard_Test extends TestBase {

	private static HttpServer a, b;
	private static final AtomicReference<String> redirectTo = new AtomicReference<>();

	private static HttpServer start(HttpHandler h) throws Exception {
		var s = HttpServer.create(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0), 0);
		s.createContext("/", h);
		s.start();
		return s;
	}

	private static void reply(HttpExchange x, int code, String body) throws java.io.IOException {
		var bytes = body.getBytes(StandardCharsets.UTF_8);
		x.sendResponseHeaders(code, bytes.length);
		try (var o = x.getResponseBody()) {
			o.write(bytes);
		}
	}

	@BeforeAll static void beforeAll() throws Exception {
		// B echoes the credential-bearing and custom headers it receives.
		b = start(x -> {
			var h = x.getRequestHeaders();
			reply(x, 200, "auth=" + h.getFirst("Authorization") + ";cookie=" + h.getFirst("Cookie") + ";custom=" + h.getFirst("X-Custom-Secret"));
		});
		// A redirects to whatever redirectTo says.
		a = start(x -> {
			x.getResponseHeaders().add("Location", redirectTo.get());
			reply(x, 302, "");
		});
	}

	@AfterAll static void afterAll() {
		a.stop(0);
		b.stop(0);
	}

	private static String base(HttpServer s) {
		return "http://127.0.0.1:" + s.getAddress().getPort();
	}

	private static RestClient.Builder client() {
		return RestClient.create().header("Authorization", "Bearer s3cr3t").header("Cookie", "sid=abc").header("X-Custom-Secret", "custom1");
	}

	@Test void a01_crossOriginStripsCredentials() throws Exception {
		redirectTo.set(base(b) + "/echo");
		try (var c = client().build()) {
			var r = c.get(base(a) + "/start").run().getContent().asString();
			assertTrue(r.contains("auth=null"), r);
			assertTrue(r.contains("cookie=null"), r);
		}
	}

	@Test void a02_sameOriginKeepsCredentials() throws Exception {
		redirectTo.set(base(b) + "/echo");
		try (var c = client().build()) {
			var r = c.get(base(b) + "/start").run().getContent().asString();
			// B answers /start directly (200, no redirect), so aim at A's own path by redirecting A to itself.
			assertTrue(r.contains("auth=Bearer s3cr3t"), r);
		}
		// A real same-origin redirect: a server that redirects /start to /echo on itself.
		var self = new AtomicReference<HttpServer>();
		var s = start(x -> {
			if (x.getRequestURI().getPath().equals("/start")) {
				x.getResponseHeaders().add("Location", base(self.get()) + "/echo");
				reply(x, 302, "");
			} else {
				var h = x.getRequestHeaders();
				reply(x, 200, "auth=" + h.getFirst("Authorization") + ";cookie=" + h.getFirst("Cookie"));
			}
		});
		self.set(s);
		try (var c = client().build()) {
			var r = c.get(base(s) + "/start").run().getContent().asString();
			assertTrue(r.contains("auth=Bearer s3cr3t"), r);
			assertTrue(r.contains("cookie=sid=abc"), r);
		} finally {
			s.stop(0);
		}
	}

	@Test void a03_customHeaderNotStrippedByDefault() throws Exception {
		redirectTo.set(base(b) + "/echo");
		try (var c = client().build()) {
			var r = c.get(base(a) + "/start").run().getContent().asString();
			assertTrue(r.contains("custom=custom1"), r);
		}
	}

	@Test void a04_redirectStripHeadersExtendsSet() throws Exception {
		redirectTo.set(base(b) + "/echo");
		try (var c = client().redirectStripHeaders("x-custom-secret").build()) {
			var r = c.get(base(a) + "/start").run().getContent().asString();
			assertTrue(r.contains("custom=null"), r);
			assertTrue(r.contains("auth=null"), r);
		}
	}

	@Test void a05_differentHostNameSameServerStrips() throws Exception {
		// "localhost" and "127.0.0.1" are different hosts, so this is cross-origin even on the same port.
		redirectTo.set("http://localhost:" + b.getAddress().getPort() + "/echo");
		try (var c = client().build()) {
			var r = c.get(base(a) + "/start").run().getContent().asString();
			assertTrue(r.contains("auth=null"), r);
		}
	}

	@Test void a06_underscoreHostDoesNotThrow() throws Exception {
		var g = new RedirectCredentialGuard(null);
		var ctx = new BasicHttpContext();
		var req = new org.apache.http.message.BasicHttpRequest("GET", "/x");
		req.addHeader("Authorization", "Bearer x");
		ctx.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, new HttpHost("my_svc", 8080, "http"));
		g.process(req, ctx);
		assertTrue(req.containsHeader("Authorization"));  // First request of the exchange.
		ctx.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, new HttpHost("other_svc", 8080, "http"));
		g.process(req, ctx);
		assertFalse(req.containsHeader("Authorization"));
		req.addHeader("Authorization", "Bearer x");
		ctx.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, new HttpHost("my_svc", 8080, "http"));
		g.process(req, ctx);
		assertTrue(req.containsHeader("Authorization"));  // Same origin.
	}

	@Test void a07_httpsToHttpDowngradeStrips() {
		var g = new RedirectCredentialGuard(null);
		var ctx = new BasicHttpContext();
		var req = new org.apache.http.message.BasicHttpRequest("GET", "/x");
		req.addHeader("Authorization", "Bearer x");
		ctx.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, new HttpHost("good.example", 443, "https"));
		g.process(req, ctx);
		ctx.setAttribute(HttpCoreContext.HTTP_TARGET_HOST, new HttpHost("good.example", 80, "http"));
		g.process(req, ctx);
		assertFalse(req.containsHeader("Authorization"));
	}

	@Test void a08_reusedContextAcrossHostsDoesNotMisclassify() throws Exception {
		// Same HttpContext reused for two independent calls to two different hosts; the second call must be
		// treated as a fresh exchange and keep its own credentials.
		var ctx = HttpClientContext.create();
		try (var c = client().build()) {
			redirectTo.set(base(b) + "/echo");
			var r1 = c.get(base(b) + "/one").context(ctx).run().getContent().asString();
			assertTrue(r1.contains("auth=Bearer s3cr3t"), r1);
			var self = new AtomicReference<HttpServer>();
			var s = start(x -> {
				var h = x.getRequestHeaders();
				reply(x, 200, "auth=" + h.getFirst("Authorization"));
			});
			self.set(s);
			try {
				var r2 = c.get(base(s) + "/two").context(ctx).run().getContent().asString();
				assertTrue(r2.contains("auth=Bearer s3cr3t"), r2);
			} finally {
				s.stop(0);
			}
		}
	}
}
