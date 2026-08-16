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
package org.apache.juneau.rest.server.ops;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;
import java.util.logging.*;

import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.springboot.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.context.SpringBootTest.*;
import org.springframework.boot.test.web.server.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.*;
import org.springframework.test.annotation.*;

/**
 * Real-Spring-Boot deployment-parity assertion for {@link EchoMixin}.
 *
 * <p>
 * Boots a full Spring Boot context with embedded Tomcat on a random port, registers a
 * {@link BasicSpringRestServlet}-based host with the echo mixin via
 * {@link ServletRegistrationBean}, supplies a Spring {@code @Bean EchoMixin}, and hits
 * {@code /echo/*} over real HTTP.
 *
 * <p>
 * Catches things {@code MockRest} and the Jetty parity test cannot:
 * <ul>
 * 	<li>Spring's bean store adapter ({@code SpringBeanStore}) resolving the host's
 * 		{@code @Bean EchoMixin} during the mixin walk through
 * 		{@link org.springframework.context.ApplicationContext#getBean(Class)
 * 		ApplicationContext.getBean(...)}.
 * 	<li>End-to-end format-pinned JSON ({@link org.apache.juneau.rest.server.server.RestResponse#getDirectWriter
 * 		getDirectWriter("application/json")}) under embedded Tomcat.
 * 	<li>Explicit {@code @Bean EchoMixin.create().enabled()} enablement driving reachability for the mixin-served
 * 		echo op under a real Spring container, independent of the {@link Host} servlet's JUL logger level.
 * </ul>
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.SpringbootTest
@SpringBootTest(classes = EchoMixin_Springboot_Test.TestApp.class,
	webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class EchoMixin_Springboot_Test {

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {

		@Bean public Host hostServlet() { return new Host(); }

		@Bean public ServletRegistrationBean<Host> hostRegistration(Host servlet) {
			return new ServletRegistrationBean<>(servlet, "/*");
		}

		@Bean public EchoMixin echoResource() {
			return EchoMixin.create().enabled().bodyLimit(2048L).build();
		}
	}

	@Rest(mixins=EchoMixin.class)
	public static class Host extends BasicSpringRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@LocalServerPort
	int port;

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private HttpResponse<String> get(String path, String...headers) throws Exception {
		var b = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(10))
			.GET();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	/**
	 * The echo op is contributed by {@link EchoMixin} and its resolved debug logger is named after the
	 * <b>host</b> resource class ({@link Host}) that composes the mixin &mdash; not {@link EchoMixin} itself
	 * (see {@code RestOpContext.hostResourceClass()}). This logger no longer gates {@code /echo/*} reachability
	 * (reachability is the explicit {@code EchoMixin.create().enabled()} switch); {@code a03} drives it below
	 * {@code FINE} to prove that.
	 *
	 * <p>
	 * Held in a {@code static final} field (rather than re-resolved via {@code Logger.getLogger(...)} on demand):
	 * {@code java.util.logging}'s {@code LogManager} only holds loggers by a weak reference, so a logger with no
	 * other strong referent can be garbage-collected and silently re-created at its default level.
	 */
	private static final Logger HOST_LOGGER = Logger.getLogger(Host.class.getName());

	@AfterAll
	static void restoreHostLogger() {
		// Static, process-global JUL state — restore to "inherit from root" so other test classes
		// in the same JVM aren't left with an elevated level on this host's logger.
		HOST_LOGGER.setLevel(null);
	}

	@Test void a01_echoUnderSpringBoot() throws Exception {
		var resp = get("/echo/spring/abc?q=1");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("\"method\": \"GET\""), "Body: " + resp.body());
		assertTrue(resp.body().contains("\"pathRemainder\": \"spring/abc\""), "Body: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("application/json"), "Content-Type: " + ct);
	}

	@Test void a02_authorizationRedactedUnderSpringBoot() throws Exception {
		var resp = get("/echo/", "Authorization", "Bearer spring-secret-token");
		assertEquals(200, resp.statusCode());
		assertFalse(resp.body().contains("spring-secret-token"),
			"Authorization secret must NEVER cross back through Spring; body: " + resp.body());
	}

	@Test void a03_echoServesRegardlessOfLoggerLevel() throws Exception {
		// Decoupling: with the host logger below FINE (isDebug()==false), the explicitly-enabled endpoint still serves.
		HOST_LOGGER.setLevel(Level.INFO);
		var resp = get("/echo/anything");
		assertEquals(200, resp.statusCode(), "Body: " + resp.body());
	}
}
