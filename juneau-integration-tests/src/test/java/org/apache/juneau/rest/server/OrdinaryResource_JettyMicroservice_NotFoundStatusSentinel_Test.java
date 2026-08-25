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
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * [READY-403] Real-container regression coverage for {@link RestSession#run()}'s {@code NotFound} status
 * sentinel (Bug B) on an ORDINARY single-path {@code @Rest} resource &mdash; i.e. one that relies on
 * the default {@code @Rest.path()} mount, which normalizes to the servlet-container WILDCARD path-spec
 * {@code "/*"} (see {@code JettyServerComponent#restPathsFor}/{@code #normalizePathSpec}), as opposed to
 * {@code HealthServlet}'s auto-mounted EXACT multi-path spec.
 *
 * <p>
 * {@code RestSession_NotFoundStatusSentinel_Test} (in {@code juneau-rest-mock}) already proves the sentinel fix
 * in-process, but its mock harness dispatches directly through {@code RestContext.execute(...)}, bypassing real
 * servlet-container URL routing entirely. This class instead boots a real Jetty connector so the unmatched
 * request path is routed by the CONTAINER first: because this resource's default mount is the wildcard
 * path-spec {@code "/*"}, Jetty routes any sub-path to this servlet, so the request genuinely reaches
 * {@code RestSession.run()}'s {@code findOperation}/{@code NotFound} path &mdash; unlike an EXACT servlet mount
 * (e.g. {@code HealthServlet}'s {@code /healthz}), where a sub-path 404s at the container level before Juneau
 * ever runs (see {@code HealthServlet_JettyHttpConnector_Test#a02_subPathUnderExactMountIsCleanContainer404}).
 *
 * <p>
 * Before the Bug B fix, a real container's {@code HttpServletResponse} defaults {@code getStatus()}
 * to the servlet-spec {@code 200} (not the mock's coincidental {@code 0}), so the old {@code getStatus() == 0}
 * sentinel in {@code RestSession.run()}'s {@code NotFound} catch was skipped and the miss was mis-mapped to a
 * {@code 500} by {@code RestContext.handleError}. This test pins the fixed behavior &mdash; a genuine 404,
 * rendered by Juneau's own error writer rather than a bare container 404 page &mdash; against future regression.
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.JettyMicroserviceTest
class OrdinaryResource_JettyMicroservice_NotFoundStatusSentinel_Test extends TestBase {

	// -----------------------------------------------------------------------------------------------------------------
	// Resource — an ordinary, single-path @Rest resource: no path()/paths() override, so it auto-mounts at the
	// default wildcard "/*" servlet path-spec.
	// -----------------------------------------------------------------------------------------------------------------

	@Rest
	public static class OrdinaryServlet extends RestServlet {
		private static final long serialVersionUID = 1L;

		@RestGet(path="/foo")
		public String foo() {
			return "OK";
		}
	}

	@Configuration
	public static class Config {
		@Bean(name="ordinaryServlet")
		public Servlet ordinaryServlet() { return new OrdinaryServlet(); }
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
			.timeout(Duration.ofSeconds(15))
			.GET()
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	@Test void a01_matchedOperation_returns200_sanityThatDispatchIsReached() throws Exception {
		var resp = get("/foo");
		assertEquals(200, resp.statusCode(), "body: " + resp.body());
		assertTrue(resp.body().contains("OK"), "body: " + resp.body());
	}

	@Test void a02_unmatchedPath_returnsJuneauProduced404_notContainer404_notServerError500() throws Exception {
		// Bug B regression guard: over a real Jetty connector (200-default HttpServletResponse), a
		// genuine unmatched-path miss on this ordinary wildcard-mounted resource must still surface as 404 —
		// not the pre-fix 500 mis-mapping, and not a bare container-level 404 page.
		var resp = get("/nonexistent");

		assertEquals(404, resp.statusCode(), "body: " + resp.body());

		// Prove this 404 was rendered by Juneau's RestContext#handleError — which explicitly sets
		// Content-Type: text/plain and Content-Encoding: identity, and writes an "HTTP 404: Not Found" body —
		// rather than a bare Jetty container 404 page (text/html), which would never reach RestSession.run() at
		// all, let alone RestContext#handleError.
		var contentType = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(contentType.startsWith("text/plain"), "Content-Type: " + contentType);
		assertEquals("identity", resp.headers().firstValue("Content-Encoding").orElse(null));
		assertTrue(resp.body().contains("HTTP 404: Not Found"), "body: " + resp.body());
	}
}
