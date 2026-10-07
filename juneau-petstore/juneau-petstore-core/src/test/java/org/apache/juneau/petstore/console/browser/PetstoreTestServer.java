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
package org.apache.juneau.petstore.console.browser;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.net.http.HttpResponse.*;
import java.time.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

import jakarta.servlet.*;

/**
 * The browser tests' server: the {@code /console} tree, the {@code /petstore} API and the {@code /petstore-ui}
 * React app over one seeded store, in a real Jetty bound to an ephemeral port.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RegisterExtension</ja>
 * 	<jk>static</jk> MicroserviceTestFixture <jv>server</jv> = PetstoreTestServer.<jsm>fixture</jsm>();
 *
 * 	<ja>@BeforeAll</ja>
 * 	<jk>static void</jk> boot() <jk>throws</jk> Exception {
 * 		PetstoreTestServer.<jsm>awaitReady</jsm>(<jv>server</jv>);
 * 	}
 * </p>
 */
@SuppressWarnings({
	"java:S2925", // awaitReady() needs a back-off between retries; there is no readiness event to await.
	"java:S8692" // awaitReady() polls a real server against a wall-clock deadline.
})
public final class PetstoreTestServer {

	/** The root group: the same children as the runners' RootResources, minus the runner-only admin resources. */
	@Rest(children={PetstoreConsoleResource.class, ConsoleAssetsRest.class, PetStoreResource.class, PetstoreUiResource.class})
	public static class BrowserHost extends BasicRestServletGroup {
		private static final long serialVersionUID = 1L;

		/** @return The seeded store shared by every child. */
		@Bean public PetStore petStore() {
			return PetstoreSeed.create().populate(new PetStore(PetstoreSeed.DEFAULT_CLOCK));
		}
	}

	/** Contributes {@link BrowserHost} to the fixture's Jetty. */
	@Configuration
	public static class BrowserConfig {
		/** @return The root servlet. */
		@Bean public Servlet root() {
			return new BrowserHost();
		}
	}

	private PetstoreTestServer() {}

	/** @return A new fixture; register it with {@code @RegisterExtension} on a static field. */
	public static MicroserviceTestFixture fixture() {
		return MicroserviceTestFixture.create().configurations(BrowserConfig.class);
	}

	/**
	 * Waits until {@code /console/store} answers 200, absorbing the first request's one-time REST setup.
	 *
	 * @param f The started fixture.
	 * @return The base URL, without a trailing slash.
	 * @throws Exception If the server is not ready within 30 seconds.
	 */
	public static String awaitReady(MicroserviceTestFixture f) throws Exception {
		var base = f.getRootUrl().toString().replaceAll("/$", "");
		var http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
		var deadline = Instant.now().plusSeconds(30);
		Exception last = null;
		while (Instant.now().isBefore(deadline)) {
			try {
				var req = HttpRequest.newBuilder(URI.create(base + "/console/store")).timeout(Duration.ofSeconds(20)).header("Accept", "text/html").GET().build();
				if (http.send(req, BodyHandlers.discarding()).statusCode() == 200)
					return base;
			} catch (IOException e) {  // timeout, refused, reset or EOF while Jetty is still starting: retry until the deadline
				last = e;
			}
			Thread.sleep(250);
		}
		throw new IllegalStateException("Petstore test server did not become ready within 30s at '" + base + "'", last);
	}
}
