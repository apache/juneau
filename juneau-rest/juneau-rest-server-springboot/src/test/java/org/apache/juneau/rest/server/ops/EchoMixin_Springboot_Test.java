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
 * 	<li>{@code @Rest(debug=@Debug("always"))} resolving through Spring's container into the mixin
 * 		sub-context's debug enablement.
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
			return EchoMixin.create().bodyLimit(2048L).build();
		}
	}

	@Rest(mixins=EchoMixin.class, debug=@Debug("always"))
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
}
