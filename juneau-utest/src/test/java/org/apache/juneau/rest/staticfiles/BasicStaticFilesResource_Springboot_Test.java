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
package org.apache.juneau.rest.staticfiles;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.springboot.*;
import org.apache.juneau.rest.staticfile.*;
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
 * Real-Spring-Boot deployment-parity assertion for {@link BasicStaticFilesResource}.
 *
 * <p>
 * Boots a full Spring Boot context with embedded Tomcat on a random port, registers a
 * {@link BasicSpringRestServlet}-based host with the static-files mixin via
 * {@link ServletRegistrationBean}, and hits the {@code /static/*} and {@code /htdocs/*} URLs over
 * real HTTP.
 *
 * <p>
 * Catches things {@code MockRest} and the {@link BasicStaticFilesResource_JettyMicroservice_Test
 * Jetty parity test} cannot:
 * <ul>
 * 	<li>Spring's bean store adapter ({@code SpringBeanStore}) resolving Juneau {@code StaticFiles}
 * 		beans through {@link org.springframework.context.ApplicationContext#getBeanProvider(Class)
 * 		ApplicationContext.getBeanProvider(...)} end-to-end.
 * 	<li>Real embedded-Tomcat {@code Content-Type} negotiation for the {@code text/css} response on
 * 		a CSS file served from a classpath JAR.
 * 	<li>The {@code SpringRestServlet} {@code @Autowired ApplicationContext} field being populated
 * 		by Spring before {@link jakarta.servlet.Servlet#init(jakarta.servlet.ServletConfig)
 * 		Servlet.init()} runs &mdash; without which the mixin's {@code RestContext.getStaticFiles()}
 * 		lookup would fail.
 * </ul>
 *
 * @since 9.5.0
 */
@SpringBootTest(classes = BasicStaticFilesResource_Springboot_Test.TestApp.class,
	webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BasicStaticFilesResource_Springboot_Test {

	/**
	 * Minimal Spring Boot application that registers a single {@link Host} servlet at {@code /*}.
	 */
	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestApp {

		@Bean
		public Host hostServlet() {
			return new Host();
		}

		@Bean
		public ServletRegistrationBean<Host> hostRegistration(Host servlet) {
			return new ServletRegistrationBean<>(servlet, "/*");
		}
	}

	/**
	 * Test host: subclass of {@link BasicSpringRestServlet} carrying the static-files mixin.
	 */
	@Rest(mixins = BasicStaticFilesResource.class)
	public static class Host extends BasicSpringRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@LocalServerPort
	int port;

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private HttpResponse<String> get(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(10))
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	private HttpResponse<String> head(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:" + port + path))
			.timeout(Duration.ofSeconds(10))
			.method("HEAD", HttpRequest.BodyPublishers.noBody())
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	@Test void a01_staticPathServesFileUnderSpringBoot() throws Exception {
		var resp = get("/static/javadoc.css");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("Licensed to the Apache Software Foundation"),
			"Body should contain the Apache license header");
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("text/css"),
			"Content-Type should be text/css but was: " + ct);
	}

	@Test void a02_htdocsPathServesFileUnderSpringBoot() throws Exception {
		var resp = get("/htdocs/javadoc.css");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("Licensed to the Apache Software Foundation"),
			"Body should contain the Apache license header");
	}

	@Test void a03_missingFileReturns404UnderSpringBoot() throws Exception {
		var resp = get("/static/does-not-exist.css");
		assertEquals(404, resp.statusCode());
	}

	@Test void a04_headProbeUnderSpringBoot() throws Exception {
		var resp = head("/static/javadoc.css");
		assertEquals(200, resp.statusCode());
		assertEquals("", resp.body(), "HEAD body must be empty");
	}

	@Test void a05_cacheControlPreservedUnderSpringBoot() throws Exception {
		var resp = get("/static/javadoc.css");
		assertEquals(200, resp.statusCode());
		assertEquals("max-age=86400, public",
			resp.headers().firstValue("Cache-Control").orElse(""));
	}
}
