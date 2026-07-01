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
package org.apache.juneau.microservice.test;

import java.time.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.jetty.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;

/**
 * A {@code @Configuration} that supplies a Jetty {@link Server} bound to an OS-assigned ephemeral port
 * (port {@code 0}) for use by {@link MicroserviceExtension @MicroserviceTest}-driven integration tests.
 *
 * <p>
 * The server has a single {@link ServerConnector} on port 0 and a single root {@link ServletContextHandler}
 * at context path {@code "/"}.  {@code JettyServerComponent} discovers the handler via the
 * {@code "ServletContextHandler"} server attribute (the same convention used by {@code jetty.xml}).
 *
 * <p>
 * The extension installs this {@code @Configuration} automatically after the user-supplied configurations, so
 * a user-supplied {@code @Bean Server} (if any) still wins (the bean store returns the first registered match).
 * Tests supply their {@code @Bean Servlet} definitions, which {@code JettyServerComponent} auto-mounts at the
 * resource's {@code @Rest(path=...)}.
 *
 * @since 10.0.0
 */
@Configuration
public class EphemeralJettyServerConfig {

	/**
	 * Provides the bean-supplied Jetty {@link Server} that {@code JettyServerComponent} consumes during
	 * {@code onStart()}.
	 *
	 * @return A configured {@link Server} bound to port 0 (OS-assigned ephemeral port).
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

	/**
	 * Disables the 30s graceful-shutdown drain wait for integration tests.
	 *
	 * <p>
	 * {@code JettyServerComponent} applies a 30s {@code stopTimeout} default to support zero-downtime k8s
	 * rollouts.  In ephemeral test servers there are no load balancers or in-flight production traffic, so
	 * the drain wait only adds ~1s per test class to the suite runtime.  Setting it to zero restores fast
	 * shutdown.
	 *
	 * @return {@link JettySettings} with {@code stopTimeout} set to zero.
	 */
	@Bean
	public JettySettings jettySettings() {
		return JettySettings.create()
			.stopTimeout(Duration.ZERO)
			.build();
	}
}
