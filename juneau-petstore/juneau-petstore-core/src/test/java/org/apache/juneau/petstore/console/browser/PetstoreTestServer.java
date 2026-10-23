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
import java.util.regex.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.data.*;
import org.apache.juneau.petstore.console.ops.*;
import org.apache.juneau.petstore.rest.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.filter.*;
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

		/** @return A 150 ms groom step, so a full groom run takes about 8 s. */
		@Bean public JobsRest.GroomPeriod groomPeriod() {
			return new JobsRest.GroomPeriod(Duration.ofMillis(150));
		}

		/**
		 * @return A 5 s jobs hard timeout, so a browser test can watch a slow restock time out.  A species hold must
		 * 	end well before it (see {@link ShortHoldHost}), or the job is reaped as a hard timeout.
		 */
		@Bean public JobsRest.HardTimeout jobsHardTimeout() {
			return new JobsRest.HardTimeout(Duration.ofSeconds(5));
		}
	}

	/** {@link BrowserHost} plus a 2 s species-row hold: long enough to see a hold, short enough to wait out. */
	public static class ShortHoldHost extends BrowserHost {
		private static final long serialVersionUID = 1L;

		/** @return The species-row hold. */
		@Bean public JobsRest.SpeciesHold speciesHold() {
			return new JobsRest.SpeciesHold(Duration.ofSeconds(2));
		}
	}

	/**
	 * {@link BrowserHost} plus a 7 s species-row hold, so a cancel in any scenario lands in it, and a 12 s hard timeout:
	 * a hold must end well before the timeout, or the job is reaped as a hard timeout instead of being cancelled after
	 * its effect.
	 */
	public static class CancelHoldHost extends BrowserHost {
		private static final long serialVersionUID = 1L;

		/** @return The species-row hold. */
		@Bean public JobsRest.SpeciesHold speciesHold() {
			return new JobsRest.SpeciesHold(Duration.ofSeconds(7));
		}

		/** @return A 12 s jobs hard timeout, longer than the hold. */
		@Override @Bean public JobsRest.HardTimeout jobsHardTimeout() {
			return new JobsRest.HardTimeout(Duration.ofSeconds(12));
		}
	}

	/** Contributes the console and API CSRF boundaries the runners install to the fixture's Jetty; a timings class adds the host. */
	@Configuration
	public static class BrowserConfig {
		// Shared by both boundary beans, as in the runners.
		private final PetstoreApiBoundary apiBoundary = new PetstoreApiBoundary();

		/** @return The console's CSRF boundary, over {@code /console/*}. */
		@Bean(name="consoleBoundary") public ServletFilterRegistration consoleBoundary() {
			return ServletFilterRegistration.create(new PetstoreConsoleBoundary(apiBoundary))
				.urlPatterns(PetstoreConsoleBoundary.URL_PATTERN)
				.dispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC, DispatcherType.ERROR)
				.order(ServletFilterRegistration.HIGHEST_PRECEDENCE)
				.build();
		}

		/** @return The API's CSRF boundary, over {@code /petstore/*}. */
		@Bean(name="apiBoundary") public ServletFilterRegistration apiBoundary() {
			return ServletFilterRegistration.create(apiBoundary)
				.urlPatterns(PetstoreApiBoundary.URL_PATTERN)
				.dispatcherTypes(DispatcherType.REQUEST)
				.order(ServletFilterRegistration.HIGHEST_PRECEDENCE + 10)
				.build();
		}
	}

	private PetstoreTestServer() {}

	/**
	 * The root servlet of a variant; the variants differ only in the host's job timings.  A fixture takes configuration
	 * classes, not parameters, and the timing beans must sit on the host (a bean on a configuration class does not reach
	 * the console resources), so each variant is a class that mounts its own host.
	 */
	@Configuration
	public static class DefaultTimings {
		/** @return The root servlet, with {@link BrowserHost}'s timings. */
		@Bean public Servlet root() {
			return new BrowserHost();
		}
	}

	/** The variant whose species rows hold 2 s after their effect. */
	@Configuration
	public static class ShortHoldTimings {
		/** @return The root servlet, a {@link ShortHoldHost}. */
		@Bean public Servlet root() {
			return new ShortHoldHost();
		}
	}

	/** The variant whose species rows hold 7 s after their effect, for the cancel cases. */
	@Configuration
	public static class CancelHoldTimings {
		/** @return The root servlet, a {@link CancelHoldHost}. */
		@Bean public Servlet root() {
			return new CancelHoldHost();
		}
	}

	/** @return A new fixture with {@link DefaultTimings}; register it with {@code @RegisterExtension} on a static field. */
	public static MicroserviceTestFixture fixture() {
		return fixture(DefaultTimings.class);
	}

	/**
	 * @param timings A timings configuration class, e.g. {@link ShortHoldTimings}.
	 * @return A new fixture; register it with {@code @RegisterExtension} on a static field.
	 */
	public static MicroserviceTestFixture fixture(Class<?> timings) {
		return MicroserviceTestFixture.create().configurations(BrowserConfig.class, timings);
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

	private static final Pattern API_TOKEN = Pattern.compile("data-petstore-csrf=\"([0-9a-f]{128})\"");

	/**
	 * Reads the API's CSRF token the way a page script does: from the admin Photo tab's upload form.
	 *
	 * @param base The base URL from {@link #awaitReady}.
	 * @return The token to send in the API's CSRF header.
	 * @throws Exception If the page cannot be read or carries no token.
	 */
	public static String apiToken(String base) throws Exception {
		var page = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/console/pets/1/photo"))
			.header("Accept", "text/html").header("Authorization", "Bearer petstore-admin").GET().build(), BodyHandlers.ofString());
		var m = API_TOKEN.matcher(page.body());
		if (! m.find())
			throw new IllegalStateException("Admin photo page carries no data-petstore-csrf token");
		return m.group(1);
	}

	private static final Pattern TOKEN = Pattern.compile("<body[^>]*\\sdata-juneau-csrf=\"([^\"]+)\"");

	/**
	 * Builds a console write the way the page sends one: same-origin, JSON, carrying the token from a console page.
	 *
	 * @param base The base URL from {@link #awaitReady}.
	 * @param path The console path, e.g. {@code /console/ops/jobs/groom}.
	 * @param json The JSON body.
	 * @return The POST request.
	 * @throws Exception If the console page cannot be read or carries no token.
	 */
	public static HttpRequest consolePost(String base, String path, String json) throws Exception {
		var page = HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(base + "/console/store")).header("Accept", "text/html").GET().build(), BodyHandlers.ofString());
		var m = TOKEN.matcher(page.body());
		if (! m.find())
			throw new IllegalStateException("Console page carries no data-juneau-csrf token");
		return HttpRequest.newBuilder(URI.create(base + path))
			.header("Origin", base).header(LoopbackBoundary.DEFAULT_CSRF_HEADER, m.group(1))
			.header("Content-Type", "application/json").header("Accept", "application/json")
			.POST(HttpRequest.BodyPublishers.ofString(json)).build();
	}
}
