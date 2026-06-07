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

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.springboot.*;
import org.apache.juneau.rest.staticfile.*;
import org.junit.jupiter.api.*;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.*;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.context.SpringBootTest.*;
import org.springframework.boot.test.web.server.*;
import org.springframework.boot.web.servlet.*;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.*;

/**
 * Spring Boot {@code META-INF/resources/} bridge test for {@link StaticFilesMixin}.
 *
 * <p>
 * Spring Boot's embedded Tomcat / Jetty auto-serves files placed under the conventional
 * {@code META-INF/resources/} classpath location at the application root (so
 * {@code META-INF/resources/foo.txt} is served at {@code http://host/foo.txt}). A Juneau service
 * deployed under Spring Boot may want to expose the same resources through the mixin's
 * {@code /static/*} or {@code /htdocs/*} mounts &mdash; e.g. to apply the mixin's
 * {@code Cache-Control} headers, or to share asset paths between Spring's and Juneau's
 * static-file handlers.
 *
 * <p>
 * This test pins the bridge: the importer registers a custom {@code @Bean StaticFiles} that
 * adds a {@code cp(Host.class, "/META-INF/resources", true)} classpath search root, and the
 * mixin then serves {@code GET /static/spring-fixture.txt} returning the content of
 * {@code juneau-utest/src/test/resources/META-INF/resources/spring-fixture.txt}.
 *
 * <p>
 * Companion to {@link StaticFilesMixin_Springboot_Test} which exercises the default
 * classpath ({@code static/} + {@code htdocs/}) under the same Spring Boot wiring.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.SpringbootTest
@SpringBootTest(classes = StaticFilesMixin_SpringbootMetaInf_Test.TestApp.class,
	webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StaticFilesMixin_SpringbootMetaInf_Test {

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
	 * Test host: subclass of {@link BasicSpringRestServlet} carrying the static-files mixin AND
	 * an importer-supplied {@code StaticFiles} factory that adds {@code /META-INF/resources/} to
	 * the classpath search list.
	 */
	@Rest(mixins = StaticFilesMixin.class)
	public static class Host extends BasicSpringRestServlet {
		private static final long serialVersionUID = 1L;

		@org.apache.juneau.commons.inject.Bean
		public StaticFiles staticFiles(BeanStore bs) {
			return BasicStaticFiles
				.create(bs)
				.cp(Host.class, "/META-INF/resources", true)
				.build();
		}
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

	@Test void a01_metaInfResourcesServedViaMixinStaticMount() throws Exception {
		var resp = get("/static/spring-fixture.txt");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("spring-boot meta-inf fixture"),
			"Body should contain the META-INF/resources fixture content, was: " + resp.body());
	}

	@Test void a02_metaInfResourcesServedViaLegacyHtdocsMount() throws Exception {
		// FINISHED-101: /htdocs/* is no longer a multi-path default on the mixin, but
		// BasicSpringRestServlet still owns the legacy /htdocs/* via BasicRestOperations#getHtdoc.
		// The same META-INF/resources file is reachable through the legacy mount.
		var resp = get("/htdocs/spring-fixture.txt");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("spring-boot meta-inf fixture"),
			"Body should contain the META-INF/resources fixture content, was: " + resp.body());
	}

	@Test void a03_missingMetaInfResourceReturns404() throws Exception {
		// Files not present in META-INF/resources/ still 404 cleanly even with the extra search root.
		var resp = get("/static/no-such-meta-inf-file.txt");
		assertEquals(404, resp.statusCode());
	}
}
