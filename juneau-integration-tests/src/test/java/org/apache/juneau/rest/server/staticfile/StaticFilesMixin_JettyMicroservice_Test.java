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
package org.apache.juneau.rest.server.staticfile;

import static org.junit.jupiter.api.Assertions.*;

import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity assertion for {@link StaticFilesMixin}.
 *
 * <p>
 * Boots a {@link org.apache.juneau.marshall.microservice.Microservice Microservice} backed by
 * {@link org.apache.juneau.marshall.microservice.jetty.JettyConfiguration JettyConfiguration} on an
 * ephemeral port via {@link MicroserviceTestFixture}, mounts a {@link BasicRestServlet} host with
 * the static-files mixin, and hits the {@code /static/*} URL over real HTTP. The {@code /htdocs/*} mount has been removed from the mixin's default URL surface under
 * FINISHED-101 (single path per op), but the {@code Host} class extends {@link BasicRestServlet}
 * which still owns the legacy {@code /htdocs/*} via {@code BasicRestOperations#getHtdoc}; the
 * second test below verifies that legacy mount still serves end-to-end.
 *
 * <p>
 * Catches things {@code MockRest} can't:
 * <ul>
 * 	<li><b>Real {@code Content-Type} negotiation.</b> The Jetty pipeline serves the file with the
 * 		correct MIME type derived from the file extension (e.g. {@code text/css; charset=UTF-8}).
 * 	<li><b>Real HEAD-via-GET contract.</b> The Servlet container sees the HEAD method and the
 * 		response writer suppresses the body via
 * 		{@link org.apache.juneau.rest.server.server.processor.HttpResourceProcessor}.
 * 	<li><b>Real classpath resource resolution under a JAR.</b> {@code BasicStaticFiles} walks the
 * 		classloader for {@code htdocs/javadoc.css} (contributed by {@code juneau-rest-server}'s
 * 		main resources) so the URL routing flows end-to-end through the real network stack.
 * </ul>
 *
 * @since 10.0.0
 */
@org.apache.juneau.testing.annotations.JettyMicroserviceTest
class StaticFilesMixin_JettyMicroservice_Test extends TestBase {

	/** Test host: vanilla {@link BasicRestServlet} subclass with the static-files mixin attached. */
	@Rest(mixins=StaticFilesMixin.class)
	public static class Host extends BasicRestServlet {
		private static final long serialVersionUID = 1L;
	}

	/** {@code @Configuration} contributing the {@link Host} servlet for auto-mount. */
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

	private static HttpResponse<String> head(String path) throws Exception {
		var req = HttpRequest.newBuilder()
			.uri(URI.create(fixture.getRootUrl() + path))
			.timeout(Duration.ofSeconds(10))
			.method("HEAD", HttpRequest.BodyPublishers.noBody())
			.build();
		return HTTP.send(req, BodyHandlers.ofString());
	}

	@Test void a01_getStaticFileOverRealHttp() throws Exception {
		var resp = get("/static/javadoc.css");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("Licensed to the Apache Software Foundation"),
			"Body should contain the Apache license header");
		var contentType = resp.headers().firstValue("Content-Type").orElse("");
		assertTrue(contentType.startsWith("text/css"),
			"Content-Type should be text/css but was: " + contentType);
		var cacheControl = resp.headers().firstValue("Cache-Control").orElse("");
		assertEquals("max-age=86400, public", cacheControl,
			"Cache-Control should preserve the BasicStaticFiles default");
	}

	@Test void a02_getHtdocsFileViaLegacyMount() throws Exception {
		// FINISHED-101: /htdocs/* is no longer a multi-path default on the mixin, but
		// BasicRestServlet still owns the legacy /htdocs/* via BasicRestOperations#getHtdoc.
		var resp = get("/htdocs/javadoc.css");
		assertEquals(200, resp.statusCode());
		assertTrue(resp.body().contains("Licensed to the Apache Software Foundation"),
			"Body should contain the Apache license header");
	}

	@Test void a03_missingFileReturns404OverRealHttp() throws Exception {
		var resp = get("/static/does-not-exist.css");
		assertEquals(404, resp.statusCode());
	}

	@Test void a04_headOverRealHttpReturnsHeadersWithEmptyBody() throws Exception {
		// HEAD: identical headers to GET, empty body. Compare against the prior GET result.
		var getResp = get("/static/javadoc.css");
		var headResp = head("/static/javadoc.css");

		assertEquals(200, headResp.statusCode());
		assertEquals("", headResp.body(), "HEAD body must be empty");

		var getCt = getResp.headers().firstValue("Content-Type").orElse("");
		var headCt = headResp.headers().firstValue("Content-Type").orElse("");
		assertEquals(getCt, headCt, "Content-Type must match GET");

		var getCacheControl = getResp.headers().firstValue("Cache-Control").orElse("");
		var headCacheControl = headResp.headers().firstValue("Cache-Control").orElse("");
		assertEquals(getCacheControl, headCacheControl, "Cache-Control must match GET");
	}
}
