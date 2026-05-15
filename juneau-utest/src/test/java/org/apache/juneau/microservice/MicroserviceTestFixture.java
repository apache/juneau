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
package org.apache.juneau.microservice;

import java.net.*;
import java.util.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.jetty.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;
import org.junit.jupiter.api.extension.*;

/**
 * JUnit 5 extension that boots a {@link JettyMicroservice} on an ephemeral port for the duration of a test class.
 *
 * <p>
 * Typical usage:
 *
 * <p class='bjava'>
 * 	<ja>@RegisterExtension</ja>
 * 	<jk>static</jk> MicroserviceTestFixture <jv>fixture</jv> = MicroserviceTestFixture.<jsm>create</jsm>()
 * 		.configurations(MyServerConfig.<jk>class</jk>);
 *
 * 	<ja>@Configuration</ja>
 * 	<jk>static class</jk> MyServerConfig {{
 * 		<ja>@Bean</ja> Servlet myService() {{ <jk>return new</jk> MyRestService(); }}
 * 	}}
 *
 * 	<ja>@Test</ja>
 * 	<jk>void</jk> exampleTest() {{
 * 		<jk>var</jk> rootUrl = <jv>fixture</jv>.getRootUrl();
 * 		<jc>// build a client against rootUrl ...</jc>
 * 	}}
 * </p>
 *
 * <h5 class='section'>How it works:</h5>
 * <ul>
 * 	<li>The fixture supplies its own {@link Server} {@link Bean} factory ({@link EphemeralJettyServerConfig}) that
 * 		binds to port {@code 0} (OS-assigned ephemeral port). User-supplied configurations should typically
 * 		contribute {@code @Bean Servlet} definitions (which the {@link JettyMicroservice} auto-mounts at
 * 		{@code @Rest(path=...)}); a user-supplied {@code @Bean Server} will conflict and is not supported here.
 * 	<li>{@link #beforeAll(ExtensionContext) beforeAll} builds the microservice, calls
 * 		{@link JettyMicroservice#createServer() createServer()}, and then
 * 		{@link JettyMicroservice#start() start()}.
 * 	<li>{@link #afterAll(ExtensionContext) afterAll} calls {@link JettyMicroservice#stop() stop()} so the bound
 * 		port is released and {@code @PreDestroy} hooks on bean-store beans fire.
 * </ul>
 *
 * <p>
 * The fixture is intended to amortize Jetty-startup cost across a whole test class — combined with the dynamic
 * {@link org.apache.juneau.rest.RestChildren#addChild RestChildren.addChild} / {@code removeChild} API, individual
 * test methods can mount and unmount their own child resources against a single long-running server.
 */
public final class MicroserviceTestFixture implements BeforeAllCallback, AfterAllCallback {

	private final List<Class<?>> configurations = new ArrayList<>();
	private JettyMicroservice microservice;
	private URI rootUrl;

	private MicroserviceTestFixture() {}

	/**
	 * Creates a new fixture.
	 *
	 * @return A new fixture instance.
	 */
	public static MicroserviceTestFixture create() {
		return new MicroserviceTestFixture();
	}

	/**
	 * Registers one or more {@code @Configuration} classes whose {@code @Bean Servlet} methods will be auto-mounted
	 * by the microservice. May be called multiple times to append.
	 *
	 * @param cs The configuration classes.
	 * @return This fixture (fluent).
	 */
	public MicroserviceTestFixture configurations(Class<?>... cs) {
		Collections.addAll(configurations, cs);
		return this;
	}

	/**
	 * Returns the root URI of the running microservice (after {@link #beforeAll(ExtensionContext)} has fired).
	 *
	 * @return The root URI, e.g. {@code http://localhost:54321/}.
	 */
	public URI getRootUrl() {
		return rootUrl;
	}

	/**
	 * Returns the actual port the server is listening on (post-{@code start()}).
	 *
	 * @return The bound port.
	 */
	public int getPort() {
		return rootUrl.getPort();
	}

	/**
	 * Returns the underlying microservice instance, useful for fixture-scoped child-resource mutations.
	 *
	 * @return The microservice.
	 */
	public JettyMicroservice getMicroservice() {
		return microservice;
	}

	@Override
	public void beforeAll(ExtensionContext ctx) throws Exception {
		// The user's configurations come first (so @Bean Servlet methods are visible). Our default Server
		// factory is registered last via configurations() — JettyMicroservice resolves Server from the bean
		// store by type, and BeanStore.getBean(...) returns the first registered match.
		var classes = new ArrayList<>(configurations);
		classes.add(EphemeralJettyServerConfig.class);
		microservice = JettyMicroservice.create()
			.configurations(classes.toArray(new Class<?>[0]))
			.build();
		microservice.start();
		// JettyMicroservice.getURI() uses InetAddress.getLocalHost().getHostName() (may resolve to a non-loopback
		// IP on some machines) and ServerConnector.getPort() (returns the configured port = 0, not the bound port).
		// For test fixtures we want a deterministic loopback URL with the actual bound port.
		var localPort = -1;
		for (var c : microservice.getServer().getConnectors()) {
			if (c instanceof ServerConnector sc) {
				localPort = sc.getLocalPort();
				break;
			}
		}
		if (localPort <= 0)
			throw new IllegalStateException("Could not determine local port of ServerConnector after start.");
		rootUrl = URI.create("http://localhost:" + localPort);
	}

	@Override
	public void afterAll(ExtensionContext ctx) throws Exception {
		if (microservice != null)
			microservice.stop();
	}

	/**
	 * Default {@code @Configuration} that supplies a Jetty {@link Server} bound to port {@code 0}.
	 *
	 * <p>
	 * The server has a single {@link ServerConnector} on port 0 (OS-assigned) and a single root
	 * {@link ServletContextHandler} at context path {@code "/"}. {@code JettyMicroservice} discovers the handler
	 * via the {@code "ServletContextHandler"} server attribute (the same convention used by {@code jetty.xml}).
	 */
	@Configuration
	public static class EphemeralJettyServerConfig {

		/**
		 * Provides the bean-supplied Jetty {@link Server} that the microservice consumes during
		 * {@link JettyMicroservice#createServer()}.
		 *
		 * @return A configured {@link Server} bound to port 0.
		 */
		@Bean
		public Server jettyServer() {
			var server = new Server();
			var connector = new ServerConnector(server);
			connector.setPort(0);
			server.addConnector(connector);
			var sch = new ServletContextHandler();
			sch.setContextPath("/");
			server.setAttribute("ServletContextHandler", sch);
			server.setHandler(sch);
			return server;
		}
	}
}
