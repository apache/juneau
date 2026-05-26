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
package org.apache.juneau.rest.ops;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity assertion for {@link BasicEchoResource}.
 *
 * <p>
 * Boots a {@link org.apache.juneau.microservice.Microservice Microservice} backed by
 * {@link org.apache.juneau.microservice.jetty.JettyConfiguration JettyConfiguration} on an
 * ephemeral port via {@link MicroserviceTestFixture}, mounts a vanilla {@link RestServlet} host
 * with the echo mixin, and hits {@code /echo/*} and {@code /debug/echo/*} over real HTTP.
 *
 * <p>
 * Catches things {@code MockRest} cannot:
 * <ul>
 * 	<li>Real {@code Content-Type: application/json} negotiation through the Jetty/servlet stack.
 * 	<li>{@code @Rest(debug=@Debug("always"))} resolving end-to-end and unlocking the echo through the
 * 		mixin sub-context's {@link org.apache.juneau.rest.debug.DebugEnablement DebugEnablement}.
 * 	<li>Sensitive-header redaction surviving the network stack &mdash; an {@code Authorization}
 * 		header sent over real HTTP must NEVER be reflected back in the response body.
 * </ul>
 *
 * @since 9.5.0
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
class BasicEchoResource_JettyMicroservice_Test extends TestBase {

	@Rest(mixins=BasicEchoResource.class, debug=@Debug("always"))
	public static class Host extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public BasicEchoResource echo() {
			return BasicEchoResource.create()
				.bodyLimit(1024L)
				.build();
		}
	}

	@Configuration
	public static class HostConfig {
		@Bean public Servlet hostServlet() { return new Host(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(HostConfig.class);

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private static HttpResponse<String> get(String path, String...headers) throws Exception {
		var b = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(10))
			.GET();
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	private static HttpResponse<String> post(String path, String body, String...headers) throws Exception {
		var b = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(10))
			.POST(HttpRequest.BodyPublishers.ofString(body));
		for (var i = 0; i < headers.length; i += 2)
			b.header(headers[i], headers[i + 1]);
		return HTTP.send(b.build(), BodyHandlers.ofString());
	}

	@Test void a01_echoOverRealHttp() throws Exception {
		var resp = get("/echo/jetty/path?q=1");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("\"method\": \"GET\""), "method surfaced: " + resp.body());
		assertTrue(resp.body().contains("\"pathRemainder\": \"jetty/path\""),
			"path remainder surfaced: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("application/json"), "Content-Type was: " + ct);
	}

	@Test void a02_authorizationRedactedOverRealHttp() throws Exception {
		var resp = get("/echo/", "Authorization", "Bearer real-network-secret");
		assertEquals(200, resp.statusCode());
		assertFalse(resp.body().contains("real-network-secret"),
			"Authorization secret must NEVER cross back; body: " + resp.body());
		assertTrue(resp.body().contains(BasicEchoResource.REDACTED));
	}

	@Test void a03_debugEchoLegacyAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /debug/echo/* is no longer a multi-path default. Default-build hosts
		// route the request through the host's normal routing — which returns 404 or 500
		// depending on container error mapping; both indicate "not handled by the echo mixin".
		var resp = get("/debug/echo/abc");
		assertTrue(resp.statusCode() == 404 || resp.statusCode() == 500,
			"expected 404 or 500 (not mounted); got " + resp.statusCode() + ": " + resp.body());
	}

	@Test void a04_postEchoesBody() throws Exception {
		var resp = post("/echo/post", "real-payload");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("\"method\": \"POST\""), "body: " + resp.body());
		assertTrue(resp.body().contains("\"content\": \"real-payload\""), "body: " + resp.body());
	}
}
