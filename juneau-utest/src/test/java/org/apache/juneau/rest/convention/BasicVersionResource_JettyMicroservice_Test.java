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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity assertion for {@link BasicVersionResource}.
 *
 * <p>
 * Boots a {@link org.apache.juneau.microservice.Microservice Microservice} backed by
 * {@link org.apache.juneau.microservice.jetty.JettyConfiguration JettyConfiguration} on an
 * ephemeral port via {@link MicroserviceTestFixture}, mounts a vanilla {@link RestServlet} host
 * with the version mixin and a {@code @Bean BasicVersionResource} factory configuring known
 * entries, and hits {@code /version}, {@code /info}, and {@code /about} over real HTTP.
 *
 * <p>
 * Catches things {@code MockRest} cannot:
 * <ul>
 * 	<li>Real {@code Content-Type: application/json} negotiation through the Jetty/servlet stack.
 * 	<li>JSON serialization through {@link org.apache.juneau.rest.RestResponse#getDirectWriter
 * 		getDirectWriter("application/json")} on a vanilla {@link RestServlet} host (no JSON
 * 		serializer wired up explicitly).
 * 	<li>Mixin-walk + bean-store override flow when {@code @Bean BasicVersionResource} is
 * 		registered on the host: the mixin serves the host-supplied configuration end-to-end through
 * 		the network stack.
 * </ul>
 *
 * @since 9.5.0
 */
class BasicVersionResource_JettyMicroservice_Test extends TestBase {

	@Rest(mixins=BasicVersionResource.class)
	public static class Host extends RestServlet {
		private static final long serialVersionUID = 1L;
		@Bean public BasicVersionResource version() {
			return BasicVersionResource.create()
				.entry("name", "jetty-test")
				.entry("version", "0.0.1")
				.entry("gitCommit", "deadbeef")
				.entry("gitBranch", "main")
				.fromJavaVersion()
				.build();
		}
	}

	@Configuration
	public static class HostConfig {
		@Bean
		public Servlet hostServlet() {
			return new Host();
		}
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(HostConfig.class);

	private static final HttpClient HTTP = HttpClient.newBuilder()
		.connectTimeout(Duration.ofSeconds(5))
		.followRedirects(HttpClient.Redirect.NEVER)
		.build();

	private static HttpResponse<String> get(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(10))
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	@Test void a01_versionOverRealHttp() throws Exception {
		var resp = get("/version");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("\"name\": \"jetty-test\""), "name surfaced in body: " + resp.body());
		assertTrue(resp.body().contains("\"version\": \"0.0.1\""), "version surfaced");
		assertTrue(resp.body().contains("\"gitCommit\": \"deadbeef\""), "gitCommit surfaced");
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("application/json"), "Content-Type was: " + ct);
	}

	@Test void a02_infoLegacyAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /info is no longer a multi-path default. Migration covered by
		// BasicVersionResource_SvlPathOverride_Test#a02.
		var resp = get("/info");
		assertTrue(resp.statusCode() == 404 || resp.statusCode() == 500,
			"expected 404 or 500 (not mounted); got " + resp.statusCode() + ": " + resp.body());
	}

	@Test void a03_aboutLegacyAliasNotMountedByDefault() throws Exception {
		// FINISHED-101: /about is no longer a multi-path default.
		var resp = get("/about");
		assertTrue(resp.statusCode() == 404 || resp.statusCode() == 500,
			"expected 404 or 500 (not mounted); got " + resp.statusCode() + ": " + resp.body());
	}

	@Test void a04_javaVersionSurfacesWithoutManifest() throws Exception {
		var resp = get("/version");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("\"javaVersion\""), "javaVersion key present");
	}
}
