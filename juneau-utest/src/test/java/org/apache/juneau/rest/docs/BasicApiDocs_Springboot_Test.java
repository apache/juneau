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
package org.apache.juneau.rest.docs;

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
 * Real-Spring-Boot deployment-parity assertion for the api-docs mixin pack.
 *
 * <p>
 * Boots a full Spring Boot context with embedded Tomcat on a random port, registers a
 * {@link BasicSpringRestServlet}-based host wired up with the four-mixin pack
 * ({@link SwaggerUiMixin} + {@link RedocMixin} &mdash; transitively pulling in
 * {@link SwaggerMixin} + {@link OpenApiMixin}) via {@link ServletRegistrationBean},
 * and hits the six canonical api-docs URLs over real HTTP via {@link HttpClient}.
 *
 * <p>
 * Each URL is validated against the shared
 * {@link BasicApiDocsTestFixtures#sixUrlAssertions() fixture} so any drift between this Spring
 * Boot path and the {@code MockRest} baseline ({@link BasicApiDocs_TransitiveDedupe_Test}) or the
 * real-Jetty path ({@link BasicApiDocs_JettyMicroservice_Test}) is caught immediately.
 *
 * <p>
 * <b>What this catches that {@code MockRest} and the Jetty path cannot:</b>
 * <ul>
 * 	<li>Spring's bean store adapter ({@link SpringBeanStore}) resolving the Juneau
 * 		{@code OpenApiProvider} / {@code SwaggerProvider} beans through
 * 		{@link org.springframework.context.ApplicationContext#getBeanProvider(Class)
 * 		ApplicationContext.getBeanProvider(...)} end-to-end.
 * 	<li>The {@code SpringRestServlet} {@code @Autowired ApplicationContext} field being populated
 * 		by Spring before {@link jakarta.servlet.Servlet#init(jakarta.servlet.ServletConfig)
 * 		Servlet.init()} runs.
 * 	<li>Real embedded-Tomcat {@code Accept} negotiation across the docs endpoints &mdash; including
 * 		the {@code defaultAccept="text/html"} hook on UI mixins.
 * </ul>
 *
 * <p>
 * Uses the JDK's built-in {@link HttpClient} rather than Spring's {@code TestRestTemplate} (which
 * was removed/restructured in Spring Boot 4.0). Same client used by
 * {@link BasicApiDocs_JettyMicroservice_Test} so the wire-level call shape is byte-identical
 * across both real-container tests.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.SpringbootTest
@SpringBootTest(classes = BasicApiDocs_Springboot_Test.TestApp.class,
	webEnvironment = WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class BasicApiDocs_Springboot_Test {

	/**
	 * Minimal Spring Boot application that registers a single {@link Host} servlet at {@code /*}.
	 *
	 * <p>
	 * Stays narrow on purpose: no extra autoconfiguration, no global filters, no security &mdash;
	 * we want to exercise the Juneau mixin walk + serializer pipeline through Spring's bean store,
	 * nothing else.
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
	 * Test host: subclass of {@link BasicSpringRestServlet} carrying the four-mixin pack.
	 *
	 * <p>
	 * {@link BasicSpringRestServlet} itself declares only an empty {@code @Rest} (no mixins) so
	 * this subclass adds them explicitly. {@code @Rest} is {@link java.lang.annotation.Inherited
	 * Inherited}, so this child annotation aggregates with the parent's via the mixin walk
	 * (parent-first, child-appended; {@code LinkedHashSet} dedupes by class identity).
	 */
	@Rest(mixins = {SwaggerUiMixin.class, RedocMixin.class})
	public static class Host extends BasicSpringRestServlet {
		private static final long serialVersionUID = 1L;
	}

	@LocalServerPort
	int port;

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private BasicApiDocsTestFixtures.Response get(String path, String accept) {
		try {
			var reqBuilder = HttpRequest.newBuilder()
				.uri(URI.create("http://localhost:" + port + path))
				.timeout(Duration.ofSeconds(10))
				.GET();
			if (accept != null)
				reqBuilder.header("Accept", accept);
			var resp = HTTP.send(reqBuilder.build(), BodyHandlers.ofString());
			var ct = resp.headers().firstValue("Content-Type").orElse(null);
			return new BasicApiDocsTestFixtures.Response(resp.statusCode(), ct, resp.body());
		} catch (Exception e) {
			throw new IllegalStateException("HTTP call failed for " + path + " (Accept=" + accept + ")", e);
		}
	}

	@Test void a01_allSixUrlsServeExpectedShape() {
		BasicApiDocsTestFixtures.assertAllSixUrls(this::get);
	}
}
