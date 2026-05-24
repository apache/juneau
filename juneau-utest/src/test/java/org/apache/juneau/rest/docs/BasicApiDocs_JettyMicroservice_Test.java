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

import org.apache.juneau.*;
import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.servlet.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.*;

import jakarta.servlet.*;

/**
 * Real-Jetty deployment-parity assertion for the api-docs mixin pack.
 *
 * <p>
 * Boots a {@link org.apache.juneau.microservice.Microservice Microservice} backed by
 * {@link org.apache.juneau.microservice.jetty.JettyConfiguration JettyConfiguration} on an
 * ephemeral port via {@link MicroserviceTestFixture}, mounts a {@link BasicRestServlet} host that
 * inherits the four-mixin pack
 * ({@link BasicSwaggerUiResource} + {@link BasicRedocResource} &mdash; transitively pulling in
 * {@link BasicSwaggerResource} + {@link BasicOpenApiResource}), and then hits the six canonical
 * api-docs URLs over real HTTP.
 *
 * <p>
 * Each URL is validated against the shared
 * {@link BasicApiDocsTestFixtures#sixUrlAssertions() fixture} so any drift between this real-Jetty
 * path and the {@code MockRest} baseline ({@link BasicApiDocs_TransitiveDedupe_Test}) or the
 * Spring Boot path ({@code BasicApiDocs_Springboot_Test}) is caught immediately.
 *
 * <p>
 * <b>What this catches that {@code MockRest} cannot:</b>
 * <ul>
 * 	<li><b>Real {@code Accept} negotiation.</b> The microservice runs the live content-negotiation
 * 		pipeline end-to-end, including the {@code defaultAccept="text/html"} hook on the UI mixins
 * 		and the {@code getDirectWriter(...)}-driven format pin on {@code /openapi.json} /
 * 		{@code /openapi.yaml}.
 * 	<li><b>Real wire-format {@code Content-Type}.</b> Charset / boundary parameter drift is
 * 		visible here; {@code MockRest} normalizes some of that out.
 * 	<li><b>Real {@code BasicBeanStore} registration through the microservice lifecycle.</b> Mixin
 * 		instances are resolved through the actual {@code @Bean Servlet} auto-mount + bean-store
 * 		lookup path rather than {@code MockRest}'s simplified mount.
 * </ul>
 *
 * @since 9.5.0
 */
class BasicApiDocs_JettyMicroservice_Test extends TestBase {

	/** Test host: vanilla {@link BasicRestServlet} subclass; inherits the four-mixin pack. */
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

	private static BasicApiDocsTestFixtures.Response get(String path, String accept) {
		try {
			var reqBuilder = HttpRequest.newBuilder()
				.uri(URI.create(fixture.getRootUrl() + path))
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
		BasicApiDocsTestFixtures.assertAllSixUrls(BasicApiDocs_JettyMicroservice_Test::get);
	}
}
