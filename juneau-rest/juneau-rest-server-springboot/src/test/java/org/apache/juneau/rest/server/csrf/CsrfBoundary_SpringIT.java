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
package org.apache.juneau.rest.server.csrf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.logging.*;

import org.apache.juneau.rest.server.filter.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.context.SpringBootTest.*;
import org.springframework.boot.test.web.server.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.*;
import org.springframework.context.annotation.*;
import org.springframework.core.*;
import org.springframework.core.env.*;
import org.springframework.test.annotation.*;
import org.springframework.test.context.*;
import org.springframework.test.context.NestedTestConfiguration.*;
import org.springframework.web.filter.*;

import jakarta.servlet.http.*;

/**
 * {@link RequestBoundaryFilter} applying a {@link CsrfBoundary} under Spring Boot's embedded Tomcat, over a real
 * socket: how the boundary's filter order interacts with each {@code server.forward-headers-strategy}, and the
 * container's session cookie.
 *
 * <p>
 * Every case that differs in properties or filter order runs in its own application context.  The proxied requests
 * model a proxy that terminated TLS on the same host and port: {@code X-Forwarded-Proto: https} and
 * {@code X-Forwarded-Port} naming the port the client connected to, so after a correct rewrite the server's own
 * origin is {@code https://127.0.0.1:<port>}.
 *
 * <p>
 * Binds a socket, so it is off by default.  Run it with {@code -Djuneau.busIt=true}, outside the sandbox.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
@EnabledIfSystemProperty(named="juneau.busIt", matches="true",
	disabledReason="binds a socket; run with -Dtest='CsrfBoundary_SpringIT' -Djuneau.busIt=true")
class CsrfBoundary_SpringIT {

	private static final String JSON = "application/json";
	private static final String CSRF = LoopbackBoundary.DEFAULT_CSRF_HEADER;

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	/** Held strongly: the logging manager keeps loggers only weakly, and a re-created one would lose the handler. */
	private static final Logger BOUNDARY_LOG = Logger.getLogger(CsrfBoundary.class.getName());

	//-----------------------------------------------------------------------------------------------------------------
	// Application
	//-----------------------------------------------------------------------------------------------------------------

	/** Counts the writes that reached the application. */
	public static final class Writes {
		final AtomicInteger count = new AtomicInteger();
	}

	/**
	 * The application under test.  The boundary's order and origin source, its token strategy, and whether the
	 * application registers its own {@link ForwardedHeaderFilter} (and at what order) come from properties, so each
	 * context states its whole setup in its {@code @SpringBootTest}.
	 */
	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class App {

		@Bean public Writes writes() { return new Writes(); }

		@Bean public FilterRegistrationBean<RequestBoundaryFilter> boundaryFilter(Environment env) {
			var b = CsrfBoundary.create()
				.originSource(env.getProperty("csrf.origin-source", CsrfBoundary.OriginSource.class, CsrfBoundary.OriginSource.HOST_HEADER));
			if (env.getProperty("csrf.per-process", Boolean.class, false))
				b.tokens(CsrfTokens.perProcess());
			var reg = new FilterRegistrationBean<>(new RequestBoundaryFilter(b.build()));
			reg.addUrlPatterns("/*");
			reg.setOrder(env.getProperty("csrf.boundary-order", Integer.class, Ordered.HIGHEST_PRECEDENCE));
			return reg;
		}

		@Bean
		@ConditionalOnProperty("csrf.app-forwarded-filter-order")
		public FilterRegistrationBean<ForwardedHeaderFilter> appForwardedHeaderFilter(Environment env) {
			var reg = new FilterRegistrationBean<>(new ForwardedHeaderFilter());
			reg.addUrlPatterns("/*");
			reg.setOrder(env.getRequiredProperty("csrf.app-forwarded-filter-order", Integer.class));
			return reg;
		}

		@Bean public ServletRegistrationBean<HttpServlet> tokenServlet() {
			return new ServletRegistrationBean<>(new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override protected void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
					res.setContentType("text/plain");
					res.getWriter().write(String.valueOf(req.getAttribute(RequestBoundaryFilter.TOKEN_ATTRIBUTE)));
				}
			}, "/token");
		}

		@Bean public ServletRegistrationBean<HttpServlet> writeServlet(Writes writes) {
			return new ServletRegistrationBean<>(new HttpServlet() {
				private static final long serialVersionUID = 1L;
				@Override protected void doPost(HttpServletRequest req, HttpServletResponse res) throws IOException {
					writes.count.incrementAndGet();
					res.setContentType("text/plain");
					res.getWriter().write("written");
				}
			}, "/write");
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Shared test body
	//-----------------------------------------------------------------------------------------------------------------

	/** One context's view of the application: its port, its write counter and its filter registrations. */
	abstract static class Context {

		@LocalServerPort int port;
		@Autowired Writes writes;
		@Autowired ApplicationContext ctx;

		String base() { return "http://127.0.0.1:" + port; }

		HttpResponse<String> send(String method, String path, String...headers) throws Exception {
			var b = HttpRequest.newBuilder(URI.create(base() + path)).timeout(Duration.ofSeconds(10));
			for (var i = 0; i < headers.length; i += 2)
				b.header(headers[i], headers[i + 1]);
			b.method(method, "GET".equals(method) ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString("{}"));
			return HTTP.send(b.build(), BodyHandlers.ofString());
		}

		/** The forwarded headers of a proxy that terminated TLS in front of this server's own host and port. */
		String[] tls() {
			return new String[] { "X-Forwarded-Proto", "https", "X-Forwarded-Port", String.valueOf(port) };
		}

		String token(String...headers) throws Exception {
			var res = send("GET", "/token", headers);
			assertEquals(200, res.statusCode(), res::body);
			assertTrue(res.body().matches("[0-9a-f]{128}"), res::body);
			return res.body();
		}

		/** A same-origin JSON write from the page the proxy served over https. */
		HttpResponse<String> tlsWrite(String token) throws Exception {
			var h = new ArrayList<>(List.of(tls()));
			h.addAll(List.of("Origin", "https://127.0.0.1:" + port, "Content-Type", JSON, CSRF, token));
			return send("POST", "/write", h.toArray(String[]::new));
		}

		/** How many {@link ForwardedHeaderFilter} registrations this context holds. */
		long forwardedFilters() {
			return ctx.getBeansOfType(FilterRegistrationBean.class).values().stream()
				.filter(r -> r.getFilter() instanceof ForwardedHeaderFilter).count();
		}

		void assertTlsWritePasses() throws Exception {
			var res = tlsWrite(token(tls()));
			assertEquals(200, res.statusCode(), res::body);
			assertEquals(1, writes.count.get());
		}
	}

	static String rejection(HttpResponse<String> res) {
		return res.headers().firstValue(RequestBoundaryFilter.REJECTION_HEADER).orElse(null);
	}

	static String sessionCookie(HttpResponse<String> res) {
		for (var c : res.headers().allValues("Set-Cookie"))
			if (c.startsWith("JSESSIONID="))
				return c.substring(0, c.indexOf(';') < 0 ? c.length() : c.indexOf(';'));
		return null;
	}

	/** Runs {@code r} with the boundary's warnings captured. */
	static List<LogRecord> captureWarnings(ThrowingRunnable r) throws Exception {
		var records = new ArrayList<LogRecord>();
		var h = new Handler() {
			@Override public void publish(LogRecord x) { records.add(x); }
			@Override public void flush() { /* Nothing buffered. */ }
			@Override public void close() { /* Nothing held. */ }
		};
		h.setLevel(Level.WARNING);
		BOUNDARY_LOG.addHandler(h);
		try {
			r.run();
		} finally {
			BOUNDARY_LOG.removeHandler(h);
		}
		return records;
	}

	interface ThrowingRunnable {
		void run() throws Exception;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) Filter order and forwarded headers
	//-----------------------------------------------------------------------------------------------------------------

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@SpringBootTest(classes=App.class, webEnvironment=WebEnvironment.RANDOM_PORT, properties={
		"server.address=127.0.0.1",
		"server.forward-headers-strategy=framework",
		"csrf.origin-source=CONTAINER",
		"csrf.per-process=true",
		"csrf.boundary-order=" + (Ordered.HIGHEST_PRECEDENCE + 10)
	})
	@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
	class B01 extends Context {
		@Test void b01_frameworkStrategyBoundaryOrderedAfterForwardedHeaderFilter_passes() throws Exception {
			assertEquals(1, forwardedFilters(), "the framework strategy registers its own ForwardedHeaderFilter");
			assertTlsWritePasses();
		}
	}

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@SpringBootTest(classes=App.class, webEnvironment=WebEnvironment.RANDOM_PORT, properties={
		"server.address=127.0.0.1",
		"server.forward-headers-strategy=none",
		"csrf.origin-source=CONTAINER",
		"csrf.per-process=true",
		"csrf.app-forwarded-filter-order=0",
		"csrf.boundary-order=-10"
	})
	@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
	class B02 extends Context {
		@Test void b02_misorderedBoundaryRefusedAndWarns() throws Exception {
			assertEquals(1, forwardedFilters(), "only the application's own ForwardedHeaderFilter is registered");
			var tok = token(tls());
			// The scheme warning fires once per JVM, so an earlier test in this fork may already have spent it.
			var reset = CsrfBoundary.class.getDeclaredMethod("resetWarnings");
			reset.setAccessible(true);
			reset.invoke(null);
			var res = new AtomicReference<HttpResponse<String>>();
			var warnings = captureWarnings(() -> res.set(tlsWrite(tok)));
			assertEquals(403, res.get().statusCode(), res.get()::body);
			assertEquals("ORIGIN_MISMATCH", rejection(res.get()));
			assertEquals(0, writes.count.get());
			assertTrue(warnings.stream().anyMatch(x -> x.getMessage().contains("only in its scheme")),
				() -> "no scheme-mismatch warning: " + warnings.stream().map(LogRecord::getMessage).toList());
		}
	}

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@SpringBootTest(classes=App.class, webEnvironment=WebEnvironment.RANDOM_PORT, properties={
		"server.address=127.0.0.1",
		"server.forward-headers-strategy=none",
		"csrf.origin-source=CONTAINER",
		"csrf.per-process=true",
		"csrf.app-forwarded-filter-order=0",
		"csrf.boundary-order=10"
	})
	@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
	class B02b extends Context {
		@Test void b02b_correctlyOrderedBoundaryPasses() throws Exception {
			assertEquals(1, forwardedFilters());
			assertTlsWritePasses();
		}
	}

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@SpringBootTest(classes=App.class, webEnvironment=WebEnvironment.RANDOM_PORT, properties={
		"server.address=127.0.0.1",
		"server.forward-headers-strategy=native",
		"csrf.origin-source=CONTAINER",
		"csrf.per-process=true",
		"csrf.boundary-order=" + Ordered.HIGHEST_PRECEDENCE
	})
	@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
	class B03 extends Context {
		@Test void b03_nativeStrategyOrderIndependent() throws Exception {
			assertEquals(0, forwardedFilters(), "the native strategy rewrites in the container, not in a filter");
			assertTlsWritePasses();
		}
	}

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@SpringBootTest(classes=App.class, webEnvironment=WebEnvironment.RANDOM_PORT, properties={
		"server.address=127.0.0.1",
		"server.forward-headers-strategy=native",
		"csrf.origin-source=CONTAINER",
		"csrf.per-process=true",
		"csrf.boundary-order=" + Ordered.LOWEST_PRECEDENCE
	})
	@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
	class B03b extends Context {
		@Test void b03b_nativeStrategyOrderIndependent_lowestPrecedence() throws Exception {
			assertTlsWritePasses();
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b) No proxy, per-session tokens
	//-----------------------------------------------------------------------------------------------------------------

	@Nested
	@NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
	@SpringBootTest(classes=App.class, webEnvironment=WebEnvironment.RANDOM_PORT, properties={
		"server.address=127.0.0.1",
		"csrf.boundary-order=" + Ordered.HIGHEST_PRECEDENCE
	})
	@DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
	class B04 extends Context {

		@Test void b04_hostHeaderModeAtHighestPrecedencePasses() throws Exception {
			var before = writes.count.get();
			var page = send("GET", "/token", "Accept", "text/html");
			var cookie = sessionCookie(page);
			assertNotNull(cookie, () -> page.headers().map().toString());
			var res = send("POST", "/write", "Cookie", cookie, "Origin", base(), "Content-Type", JSON, CSRF, page.body());
			assertEquals(200, res.statusCode(), res::body);

			var cross = send("POST", "/write", "Cookie", cookie, "Origin", "http://evil.example", "Content-Type", JSON, CSRF, page.body());
			assertEquals(403, cross.statusCode(), cross::body);
			assertEquals("ORIGIN_MISMATCH", rejection(cross));
			assertEquals(before + 1, writes.count.get());
		}

		@Test void b05_springSessionCookieDelivered() throws Exception {
			assertNull(sessionCookie(send("GET", "/token", "Accept", "*/*")));
			var page = send("GET", "/token", "Accept", "text/html", "Sec-Fetch-Mode", "navigate", "Sec-Fetch-Dest", "document");
			assertEquals(200, page.statusCode(), page::body);
			var cookie = sessionCookie(page);
			assertNotNull(cookie, () -> page.headers().map().toString());
			var tok = page.body();
			assertTrue(tok.matches("[0-9a-f]{128}"), tok);

			var before = writes.count.get();
			var ok = send("POST", "/write", "Cookie", cookie, "Origin", base(), "Content-Type", JSON, CSRF, tok);
			assertEquals(200, ok.statusCode(), ok::body);

			var noSession = send("POST", "/write", "Origin", base(), "Content-Type", JSON, CSRF, tok);
			assertEquals(403, noSession.statusCode(), noSession::body);
			assertEquals("CSRF_TOKEN_MISMATCH", rejection(noSession));
			assertEquals(before + 1, writes.count.get());
		}
	}
}
