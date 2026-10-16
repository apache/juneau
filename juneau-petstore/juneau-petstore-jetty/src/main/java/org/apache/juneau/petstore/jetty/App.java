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
package org.apache.juneau.petstore.jetty;

import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.commons.settings.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.examples.*;
import org.apache.juneau.microservice.jetty.*;
import org.apache.juneau.petstore.console.*;

import jakarta.servlet.*;

/**
 * Petstore Jetty/Microservice deployment entry point.
 *
 * <p>
 * Locates {@code juneau-petstore-jetty.cfg} (in the working directory or on the classpath), wires Jetty via
 * {@link JettyConfiguration}, and mounts {@link RootResources} as the root servlet.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
public class App {

	/**
	 * Setting (system property, environment, or any registered source) listing the host names the admin resources
	 * answer to: comma-separated, no ports.  Unset or blank, only the loopback names {@link AdminBoundary#LOOPBACK_HOSTS}
	 * are answered.  Independent of {@value PetstoreApiBoundary#ALLOWED_HOSTS_PROPERTY}.
	 */
	public static final String ADMIN_ALLOWED_HOSTS_PROPERTY = "petstore.admin.allowedHosts";

	/**
	 * Entry point.
	 *
	 * @param args Command-line arguments forwarded to {@link Microservice}.
	 * @throws Exception If the microservice fails to start.
	 */
	public static void main(String[] args) throws Exception {
		// @formatter:off
		Microservice
			.create()
			.args(args)
			.configurations(JettyConfiguration.class, AppConfig.class)
			.build()
			.start()
			.startConsole()
			.join();
		// @formatter:on
	}

	/**
	 * Application-specific configuration class contributing the top-level REST servlet and the console, API and admin
	 * boundary filters.
	 */
	@Configuration
	public static class AppConfig {

		// Shared by both boundary beans: the console publishes tokens from the instance the API filter verifies them with.
		private final PetstoreApiBoundary apiBoundary = new PetstoreApiBoundary();

		/**
		 * Provides the top-level REST servlet, auto-mounted by {@code JettyServerComponent} at
		 * {@link org.apache.juneau.rest.server.Rest#path()}.
		 *
		 * @return The root servlet.
		 */
		@Bean
		public Servlet rootResources() {
			return new RootResources();
		}

		/**
		 * Provides the console's CSRF boundary, covering {@code /console} only.
		 *
		 * <p>
		 * The {@code /petstore} API is covered by {@link #apiBoundary()} and the admin resources by
		 * {@link #adminBoundary()}; {@code /petstore-ui} is outside all three: see {@link PetstoreConsoleBoundary}.
		 * It runs ahead of routing and ahead of authentication, for the request, forward, async and error dispatches;
		 * the include dispatch is deliberately not mapped.  It is built over the same {@link PetstoreApiBoundary} that
		 * {@link #apiBoundary()} mounts, so the API token its pages carry is one the API filter accepts.
		 *
		 * @return The filter registration.
		 */
		@Bean(name="consoleBoundary")
		public ServletFilterRegistration consoleBoundary() {
			return ServletFilterRegistration.create(new PetstoreConsoleBoundary(apiBoundary))
				.urlPatterns(PetstoreConsoleBoundary.URL_PATTERN)
				.dispatcherTypes(DispatcherType.REQUEST, DispatcherType.FORWARD, DispatcherType.ASYNC, DispatcherType.ERROR)
				.order(ServletFilterRegistration.HIGHEST_PRECEDENCE)
				.build();
		}

		/**
		 * Provides the API's CSRF boundary, covering {@code /petstore/*}.
		 *
		 * <p>
		 * It runs only on the original request: an internal forward or error dispatch to the API is not a new
		 * attacker-controlled request.  The order keeps it just behind {@link #consoleBoundary()}; the two patterns never
		 * overlap.
		 *
		 * @return The filter registration.
		 */
		@Bean(name="apiBoundary")
		public ServletFilterRegistration apiBoundary() {
			return ServletFilterRegistration.create(apiBoundary)
				.urlPatterns(PetstoreApiBoundary.URL_PATTERN)
				.dispatcherTypes(DispatcherType.REQUEST)
				.order(ServletFilterRegistration.HIGHEST_PRECEDENCE + 10)
				.build();
		}

		/**
		 * Provides the admin resources' CSRF boundary, covering {@code /config}, {@code /logs} and {@code /shutdown}.
		 *
		 * <p>
		 * A filter of its own, answering to the host names in {@value App#ADMIN_ALLOWED_HOSTS_PROPERTY}, or to the
		 * loopback names only while that is unset.  The admin resources have no forms, so a browser write to them is
		 * refused, and command-line writes are admitted: see {@link AdminBoundary}.  The boundary does not authenticate:
		 * the admin resources must not be exposed on an untrusted network.
		 *
		 * @return The filter registration.
		 * @throws IllegalArgumentException If {@value App#ADMIN_ALLOWED_HOSTS_PROPERTY} holds an entry that is blank or
		 * 	not a bare host name.
		 */
		@Bean(name="adminBoundary")
		public ServletFilterRegistration adminBoundary() {
			return AdminBoundary.registration("", hosts(Settings.get().get(ADMIN_ALLOWED_HOSTS_PROPERTY).orElse(null)));
		}

		// The comma-separated entries, trimmed, blank ones kept so the boundary refuses them; none when unset.
		private static String[] hosts(String setting) {
			if (setting == null || setting.isBlank())
				return new String[0];
			return Arrays.stream(setting.split(",", -1)).map(String::trim).toArray(String[]::new);
		}
	}
}
