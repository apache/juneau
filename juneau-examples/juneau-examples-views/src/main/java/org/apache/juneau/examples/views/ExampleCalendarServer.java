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
 * Boots {@link ExampleCalendarRest} on an embedded Jetty server.
 *
 * <p>
 * Run {@link #main(String[]) main} to start the server on a fixed port (default {@code 5002}, or pass a port as the
 * first argument) and leave it running so you can open the calendar in a browser. The wiring mirrors
 * {@link ExampleViewsServer}.
 */
@SuppressWarnings({
	"java:S106" // Example walkthrough intentionally prints to stdout; console output is the demo's deliverable.
})
public final class ExampleCalendarServer implements AutoCloseable {

	/** Default listen port used by {@link #main(String[])} when none is supplied. */
	public static final int DEFAULT_PORT = 5002;

	private final Microservice microservice;
	private final URI rootUrl;

	private ExampleCalendarServer(Microservice microservice, URI rootUrl) {
		this.microservice = microservice;
		this.rootUrl = rootUrl;
	}

	/**
	 * Starts the example calendar server.
	 *
	 * @param port The TCP port to listen on, or {@code 0} to let the OS assign an ephemeral port.
	 * @return A running server handle. Close it (or call {@link #close()}) to stop.
	 * @throws Exception If the server fails to start.
	 */
	@SuppressWarnings("resource") // The bean store is handed to (and closed by) the Microservice lifecycle.
	public static ExampleCalendarServer start(int port) throws Exception {
		var jetty = buildServer(port);

		var beanStore = new BasicBeanStore();
		beanStore.addBean(Server.class, jetty);
		beanStore.addBean(Servlet.class, new ExampleCalendarRest());

		var microservice = Microservice.create()
			.beanStore(beanStore)
			.configurations(JettyConfiguration.class)
			.consoleEnabled(false)
			.build();
		microservice.start();

		return new ExampleCalendarServer(microservice, URI.create("http://localhost:" + boundPort(jetty) + "/"));
	}

	/**
	 * Returns the root URL the server is listening on (e.g. {@code http://localhost:5002/}).
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
		System.out.println("Juneau calendar example server is listening at " + server.getRootUrl());
		System.out.println("Open it in a browser to see the current month, then navigate prev/next/today.");
		System.out.println("Press Ctrl-C to stop.");
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
