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
 * JUnit 5 extension that boots a {@link Microservice} backed by {@link JettyConfiguration} on an ephemeral port for
 * the duration of a test class.
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
 * 	<li>The fixture installs {@link JettyConfiguration} plus a private {@link EphemeralJettyServerConfig}
 * 		{@code @Configuration} that contributes a Jetty {@link Server} bound to port {@code 0} (OS-assigned
 * 		ephemeral port). User-supplied configurations should typically contribute {@code @Bean Servlet} definitions
 * 		which {@link JettyServerComponent} auto-mounts at {@code @Rest(path=...)}.
 * 	<li>{@link #beforeAll(ExtensionContext) beforeAll} builds and {@link Microservice#start() starts} the service,
 * 		then probes the bound port via the {@link Server}'s {@link ServerConnector}.
 * 	<li>{@link #afterAll(ExtensionContext) afterAll} calls {@link Microservice#stop() stop()} so the bound port is
 * 		released and {@code @PreDestroy} hooks on bean-store beans fire.
 * </ul>
 *
 * <p>
 * The fixture is intended to amortize Jetty-startup cost across a whole test class — combined with the dynamic
 * {@link org.apache.juneau.rest.server.RestChildren#addChild RestChildren.addChild} / {@code removeChild} API, individual
 * test methods can mount and unmount their own child resources against a single long-running server.
 */
@SuppressWarnings({
	"resource" // The Microservice (and bean-store Closeables it owns) is stopped in afterAll(); the builder result is not an unmanaged leak.
})
public final class MicroserviceTestFixture implements BeforeAllCallback, AfterAllCallback {

	private final List<Class<?>> configurations = new ArrayList<>();
	private Microservice microservice;
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
	public Microservice getMicroservice() {
		return microservice;
	}

	/**
	 * Returns the {@link JettyServerComponent} bean for the running microservice.
	 *
	 * @return The Jetty server component.
	 */
	public JettyServerComponent getJettyServerComponent() {
		return microservice.getBeanStore().getBean(JettyServerComponent.class).orElseThrow();
	}

	@Override
	public void beforeAll(ExtensionContext ctx) throws Exception {
		// The user's configurations come first (so @Bean Servlet methods are visible); EphemeralJettyServerConfig
		// supplies the Jetty Server bound to port 0; JettyConfiguration wires the lifecycle and remaining defaults.
		// JettyServerComponent resolves Server from the bean store by type, and BeanStore.getBean(...) returns the
		// first registered match (so a user-supplied @Bean Server would still win).
		var classes = new ArrayList<>(configurations);
		classes.add(EphemeralJettyServerConfig.class);
		classes.add(JettyConfiguration.class);
		microservice = Microservice.create()
			.configurations(classes.toArray(new Class<?>[0]))
			.build();
		microservice.start();
		// JettyServerComponent.getURI() uses InetAddress.getLocalHost().getHostName() (may resolve to a non-loopback
		// IP on some machines) and ServerConnector.getPort() (returns the configured port = 0, not the bound port).
		// For test fixtures we want a deterministic loopback URL with the actual bound port.
		var server = getJettyServerComponent().getServer();
		var localPort = -1;
		for (var c : server.getConnectors()) {
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
	 * {@link ServletContextHandler} at context path {@code "/"}. {@link JettyServerComponent} discovers the handler
	 * via the {@code "ServletContextHandler"} server attribute (the same convention used by {@code jetty.xml}).
	 */
	@Configuration
	public static class EphemeralJettyServerConfig {

		/**
		 * Provides the bean-supplied Jetty {@link Server} that {@link JettyServerComponent} consumes during
		 * {@code onStart()}.
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
