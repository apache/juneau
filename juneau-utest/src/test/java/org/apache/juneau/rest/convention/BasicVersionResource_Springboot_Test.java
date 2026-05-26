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
package org.apache.juneau.rest.convention;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.springboot.*;
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
 * Real-Spring-Boot deployment-parity assertion for {@link BasicVersionResource}.
 *
 * <p>
 * Boots a full Spring Boot context with embedded Tomcat on a random port, registers a
 * {@link BasicSpringRestServlet}-based host with the version mixin via
 * {@link ServletRegistrationBean}, supplies a Spring {@code @Bean BasicVersionResource}, and hits
 * {@code /version}, {@code /info}, and {@code /about} over real HTTP.
 *
 * <p>
 * Catches things {@code MockRest} and the
 * {@link BasicVersionResource_JettyMicroservice_Test Jetty parity test} cannot:
 * <ul>
 * 	<li>Spring's bean store adapter ({@code SpringBeanStore}) resolving the host's
 * 		{@code @Bean BasicVersionResource} during the FINISHED-72 mixin walk through
 * 		{@link org.springframework.context.ApplicationContext#getBean(Class)
 * 		ApplicationContext.getBean(...)}.
 * 	<li>End-to-end format-pinned JSON ({@link org.apache.juneau.rest.RestResponse#getDirectWriter
 * 		getDirectWriter("application/json")}) under embedded Tomcat.
 * </ul>
 *
 * @since 9.5.0
 */
@org.apache.juneau.testing.annotations.SpringbootTest
@SpringBootTest(classes = BasicVersionResource_Springboot_Test.TestApp.class,
	webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BasicVersionResource_Springboot_Test {

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

		@Bean
		public BasicVersionResource versionResource() {
			return BasicVersionResource.create()
				.entry("name", "spring-test")
				.entry("version", "0.0.2")
				.entry("gitBranch", "release")
				.fromJavaVersion()
				.build();
		}
	}

	@Rest(mixins = BasicVersionResource.class)
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

	@Test void a01_versionUnderSpringBoot() throws Exception {
		var resp = get("/version");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("\"name\": \"spring-test\""), "Body: " + resp.body());
		assertTrue(resp.body().contains("\"version\": \"0.0.2\""));
		assertTrue(resp.body().contains("\"gitBranch\": \"release\""));
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("application/json"), "Content-Type: " + ct);
	}

	@Test void a02_infoLegacyAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /info is no longer a multi-path default. Default-build hosts route
		// the request through the host's normal routing (404 or 500 from Spring Boot's
		// error-page mapping); both indicate "not handled by the version mixin".
		var resp = get("/info");
		assertTrue(resp.statusCode() == 404 || resp.statusCode() == 500,
			"expected 404 or 500 (not mounted); got " + resp.statusCode() + ": " + resp.body());
	}

	@Test void a03_aboutLegacyAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /about is no longer a multi-path default. Migration covered by
		// BasicVersionResource_SvlPathOverride_Test#a02.
		var resp = get("/about");
		assertTrue(resp.statusCode() == 404 || resp.statusCode() == 500,
			"expected 404 or 500 (not mounted); got " + resp.statusCode() + ": " + resp.body());
	}
}
