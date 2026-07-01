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
package org.apache.juneau.rest.server;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.convention.*;
import org.apache.juneau.rest.server.ops.*;
import org.apache.juneau.rest.server.staticfile.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity assertion for the standalone {@code Basic*Servlet} companions.
 *
 * <p>
 * Boots a {@link org.apache.juneau.marshall.microservice.Microservice Microservice} on an ephemeral port and
 * mounts three standalone servlets as siblings &mdash; {@link VersionServlet} at
 * {@code /version/*}, {@link StaticFilesServlet} at {@code /static/*}, and
 * {@link AdminServlet} at {@code /admin/*} &mdash; relying on the {@code JettyServerComponent}
 * auto-discovery loop to read each servlet's {@code @Rest(paths)} via
 * {@code RestContext.resolveTopLevelPaths(...)}. This is the cross-runtime parity check that the
 * sibling top-level mount + op-at-{@code /*} shape resolves correctly through a real servlet
 * container (which {@code MockRestClient} cannot fully model for {@code paths()}-based mounts).
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class StandaloneServlets_JettyMicroservice_Test extends TestBase {

	@Configuration
	public static class Config {
		@Bean(name="versionServlet") public Servlet versionServlet() { return new VersionServlet(); }
		@Bean(name="staticFilesServlet") public Servlet staticFilesServlet() { return new StaticFilesServlet(); }
		@Bean(name="adminServlet") public Servlet adminServlet() { return new AdminServlet(); }
	}

	@RegisterExtension
	static MicroserviceTestFixture fixture = MicroserviceTestFixture.create()
		.configurations(Config.class);

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

	//-----------------------------------------------------------------------------------------------------------------
	// Version standalone servlet at /version/* with op /* (verifies the null-pathInfo root case).
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_versionServlet() throws Exception {
		var resp = get("/version");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("\"javaVersion\""), "javaVersion key present: " + resp.body());
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("application/json"), "Content-Type was: " + ct);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Static-files standalone servlet at /static/* with op /*.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_staticFilesServlet() throws Exception {
		var resp = get("/static/javadoc.css");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("Licensed to the Apache Software Foundation"),
			"static file body should contain the Apache license header");
		var ct = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(ct.startsWith("text/css"), "Content-Type was: " + ct);
	}

	@Test void b02_staticMissingReturns404() throws Exception {
		var resp = get("/static/does-not-exist.css");
		assertEquals(404, resp.statusCode());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Admin standalone servlet at /admin/* — deny-all guard returns 403 until an auth chain is wired.
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_adminThreadsDeniedByDefault() throws Exception {
		var resp = get("/admin/threads");
		assertEquals(403, resp.statusCode(), "deny-all guard should forbid; body: " + resp.body());
	}
}
