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
package org.apache.juneau.examples.mcp.secured;

import java.net.*;

import org.apache.juneau.commons.inject.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.jetty.*;
import org.eclipse.jetty.ee11.servlet.*;
import org.eclipse.jetty.server.*;

import jakarta.servlet.*;

/**
 * Boots {@link SecuredExampleMcpServer} on an embedded Jetty server, alongside an in-process
 * {@link OfflineAuthorizationServer} that stands in for a real OAuth 2.1 authorization server.
 *
 * <p>
 * Mirrors {@link org.apache.juneau.examples.mcp.ExampleServer} (read that class first &mdash; this one only
 * documents what is different). Run {@link #main(String[]) main} to start both servers on fixed ports and
 * print everything a {@link SecuredExampleClient} run needs to copy: the endpoint, the offline authorization
 * server's token endpoint, and the randomly-generated demo client id/secret. The in-process end-to-end test
 * uses {@link #start(int) start(0)} to boot on an OS-assigned ephemeral port instead.
 *
 * <p>
 * <b>The resource URL is resolved before the secured servlet is even constructed.</b> {@link SecuredExampleMcpServer}
 * needs its own address up front (see its class javadoc), which is awkward on an OS-assigned ephemeral port
 * because normally nothing knows that port until Jetty finishes starting. The fix: {@link ServerConnector#open()}
 * can be, and by Jetty's own convention normally is (see {@code Server.doStart()}'s "open network connector to
 * ensure ports are available" step), called to bind the listening socket BEFORE the rest of the server starts.
 * Calling it ourselves, one step earlier than usual, lets us read {@link ServerConnector#getLocalPort()}
 * immediately &mdash; before building {@link SecuredExampleMcpServer} or starting the {@link Microservice} at
 * all. Jetty's own {@code Server.start()} then finds the connector already open and skips rebinding it.
 */
public final class SecuredExampleServer implements AutoCloseable {

	/** Default listen port used by {@link #main(String[])} when none is supplied. */
	public static final int DEFAULT_PORT = 5001;

	private final Microservice microservice;
	private final OfflineAuthorizationServer authServer;
	private final URI rootUrl;

	private SecuredExampleServer(Microservice microservice, OfflineAuthorizationServer authServer, URI rootUrl) {
		this.microservice = microservice;
		this.authServer = authServer;
		this.rootUrl = rootUrl;
	}

	/**
	 * Starts the secured example MCP server and its offline authorization server.
	 *
	 * @param port The TCP port the MCP server listens on, or {@code 0} to let the OS assign an ephemeral port.
	 * 	(The offline authorization server always uses its own separate OS-assigned ephemeral port.)
	 * @return A running server handle. Close it (or call {@link #close()}) to stop both servers.
	 * @throws Exception If either server fails to start.
	 */
	@SuppressWarnings("resource") // The bean store is handed to (and closed by) the Microservice lifecycle.
	public static SecuredExampleServer start(int port) throws Exception {
		var authServer = OfflineAuthorizationServer.start();
		// M9: hoisted above the try so a failure AFTER connector.open() (which has already bound a real OS
		// socket) can still close it in the catch below - Server.stop() alone will not close a connector that
		// was opened but never handed to a started Server.
		ServerConnector connector = null;
		try {
			var jetty = new Server();
			connector = new ServerConnector(jetty);
			connector.setPort(port);
			jetty.addConnector(connector);
			// Bind the (possibly ephemeral) listen socket now, before the servlet context (and therefore
			// SecuredExampleMcpServer's RestContext) is built, so the resource URL below is real and final
			// by the time anything asks the servlet for it. See the class javadoc for why that ordering
			// matters here specifically.
			connector.open();
			var rootUrl = URI.create("http://localhost:" + connector.getLocalPort() + "/");

			var handler = new ServletContextHandler();
			handler.setContextPath("/");
			jetty.setAttribute("ServletContextHandler", handler);
			jetty.setHandler(handler);

			var securedServlet = new SecuredExampleMcpServer(authServer, rootUrl);

			var beanStore = new BasicBeanStore();
			beanStore.addBean(Server.class, jetty);
			beanStore.addBean(Servlet.class, securedServlet);

			var microservice = Microservice.create()
				.beanStore(beanStore)
				.configurations(JettyConfiguration.class)
				.consoleEnabled(false)
				.build();
			microservice.start();

			return new SecuredExampleServer(microservice, authServer, rootUrl);
		} catch (Exception e) {
			if (connector != null)
				connector.close();
			authServer.close();
			throw e;
		}
	}

	/**
	 * Returns the root URL the secured MCP server is listening on (e.g. {@code http://localhost:5001/}).
	 *
	 * @return The root URL. Never <jk>null</jk>.
	 */
	public URI getRootUrl() {
		return rootUrl;
	}

	/**
	 * Returns the offline authorization server backing this instance, so a caller (e.g. the end-to-end test,
	 * or {@link #main(String[])} printing a startup banner) can read its token endpoint and demo credentials.
	 *
	 * @return The offline authorization server. Never <jk>null</jk>.
	 */
	public OfflineAuthorizationServer getAuthServer() {
		return authServer;
	}

	@Override
	public void close() throws Exception {
		try {
			microservice.stop();
		} finally {
			authServer.close();
		}
	}

	/**
	 * Runs both servers until the process is killed, printing everything {@link SecuredExampleClient} needs.
	 *
	 * <p>
	 * M10: the token endpoint is no longer printed here &mdash; {@link SecuredExampleClient} now discovers it
	 * itself via RFC 9728 PRM + RFC 8414 discovery, exactly as a real client would, so only the endpoint and
	 * demo client credentials need to be copied.
	 *
	 * @param args Optional single argument: the port the MCP server listens on (defaults to {@link #DEFAULT_PORT}).
	 * @throws Exception If either server fails to start.
	 */
	public static void main(String[] args) throws Exception {
		var port = args.length > 0 ? Integer.parseInt(args[0]) : DEFAULT_PORT;
		var server = start(port);
		var auth = server.getAuthServer();
		System.out.println("Juneau SECURED MCP example server is listening at " + server.getRootUrl());
		System.out.println("Demo client id:     " + auth.clientId());
		System.out.println("Demo client secret: " + auth.clientSecret());
		System.out.println();
		System.out.println("Drive it with:  SecuredExampleClient " + server.getRootUrl() + " "
			+ auth.clientId() + " " + auth.clientSecret());
		System.out.println("Press Ctrl-C to stop.");
		Thread.currentThread().join();
	}
}
