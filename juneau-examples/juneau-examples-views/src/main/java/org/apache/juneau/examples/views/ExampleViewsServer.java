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
package org.apache.juneau.examples.views;

import java.net.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.jetty.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;

import jakarta.servlet.*;

/**
 * Boots {@link ExampleViewsRest} on an embedded Jetty server.
 *
 * <p>
 * Run {@link #main(String[]) main} to start the server on a fixed port (default {@code 5001}, or pass a
 * port as the first argument) and leave it running so you can open the page in a browser. The wiring
 * mirrors {@code juneau-examples-mcp}'s {@code ExampleServer}: a Jetty {@link Server} plus the
 * {@link ExampleViewsRest} servlet are placed in a {@link BasicBeanStore}, and {@link JettyConfiguration}
 * (added via {@link org.apache.juneau.microservice.Microservice.Builder#configurations(Class...)})
 * auto-mounts the {@link org.apache.juneau.rest.server.Rest @Rest} servlet at {@code "/"} and drives the
 * server lifecycle.
 */
@SuppressWarnings({
	"java:S106" // Example walkthrough intentionally prints to stdout; console output is the demo's deliverable.
})
public final class ExampleViewsServer implements AutoCloseable {

	/** Default listen port used by {@link #main(String[])} when none is supplied. */
	public static final int DEFAULT_PORT = 5001;

	private final Microservice microservice;
	private final URI rootUrl;

	private ExampleViewsServer(Microservice microservice, URI rootUrl) {
		this.microservice = microservice;
		this.rootUrl = rootUrl;
	}

	/**
	 * Starts the example views server.
	 *
	 * @param port The TCP port to listen on, or {@code 0} to let the OS assign an ephemeral port.
	 * @return A running server handle. Close it (or call {@link #close()}) to stop.
	 * @throws Exception If the server fails to start.
	 */
	@SuppressWarnings("resource") // The bean store is handed to (and closed by) the Microservice lifecycle.
	public static ExampleViewsServer start(int port) throws Exception {
		var jetty = buildServer(port);

		var beanStore = new BasicBeanStore();
		beanStore.addBean(Server.class, jetty);
		beanStore.addBean(Servlet.class, new ExampleViewsRest());

		var microservice = Microservice.create()
			.beanStore(beanStore)
			.configurations(JettyConfiguration.class)
			// This example has no interactive commands to offer; skip the Java console entirely so
			// startup doesn't print "Could not create console command" for whatever default console
			// commands a discovered config might otherwise register.
			.consoleEnabled(false)
			.build();
		microservice.start();

		return new ExampleViewsServer(microservice, URI.create("http://localhost:" + boundPort(jetty) + "/"));
	}

	/**
	 * Returns the root URL the server is listening on (e.g. {@code http://localhost:5001/}).
	 *
	 * @return The root URL. Never <jk>null</jk>.
	 */
	public URI getRootUrl() {
		return rootUrl;
	}

	@Override
	public void close() throws Exception {
		microservice.stop();
	}

	/**
	 * Runs the server until the process is killed.
	 *
	 * @param args Optional single argument: the port to listen on (defaults to {@link #DEFAULT_PORT}).
	 * @throws Exception If the server fails to start.
	 */
	@SuppressWarnings("resource") // example server runs for the JVM lifetime; closed on process exit.
	public static void main(String[] args) throws Exception {
		var port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
		var server = start(port);
		System.out.println("Juneau views example server is listening at " + server.getRootUrl());
		System.out.println("Open it in a browser to see the Catalog (Active/Archived) and Audit Log tabs.");
		System.out.println("Press Ctrl-C to stop.");
		// Microservice.join() defaults to a documented no-op, so it won't actually block here; join the
		// main thread directly to keep the process (and the Jetty daemon threads it started) alive.
		Thread.currentThread().join();
	}

	/** Builds a Jetty server with a single connector on {@code port} and a root servlet context. */
	@SuppressWarnings("resource") // connector is added to and owned by the server; closed when the server stops.
	private static Server buildServer(int port) {
		var server = new Server();
		var connector = new ServerConnector(server);
		connector.setPort(port);
		server.addConnector(connector);
		var handler = new ServletContextHandler();
		handler.setContextPath("/");
		// JettyServerComponent discovers the handler via this attribute (same convention as jetty.xml).
		server.setAttribute("ServletContextHandler", handler);
		server.setHandler(handler);
		return server;
	}

	/** Reads the actual bound port (meaningful even when the server was started on port 0). */
	private static int boundPort(Server server) {
		for (var connector : server.getConnectors())
			if (connector instanceof ServerConnector sc)
				return sc.getLocalPort();
		throw new IllegalStateException("Could not determine the bound port of the Jetty server.");
	}
}
