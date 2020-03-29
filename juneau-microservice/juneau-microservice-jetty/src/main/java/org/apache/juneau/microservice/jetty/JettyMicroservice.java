// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.microservice.jetty;

import static org.apache.juneau.internal.SystemUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.ClassInfo;
import org.apache.juneau.rest.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.*;

/**
 * Entry point for Juneau microservice that implements a REST interface using Jetty on a single port.
 *
 * <h5 class='topic'>Jetty Server Details</h5>
 *
 * The Jetty server is created by the {@link #createServer()} method and started with the {@link #startServer()} method.
 * These methods can be overridden to provided customized behavior.
 *
 * <h5 class='topic'>Defining REST Resources</h5>
 *
 * Top-level REST resources are defined in the <c>jetty.xml</c> file as normal servlets.
 */
public class JettyMicroservice extends Microservice {

	private static volatile JettyMicroservice INSTANCE;

	private static void setInstance(JettyMicroservice m) {
		synchronized(JettyMicroservice.class) {
			INSTANCE = m;
		}
	}

	/**
	 * Returns the Microservice instance.
	 * <p>
	 * This method only works if there's only one Microservice instance in a JVM.
	 * Otherwise, it's just overwritten by the last instantiated microservice.
	 *
	 * @return The Microservice instance, or <jk>null</jk> if there isn't one.
	 */
	public static JettyMicroservice getInstance() {
		synchronized(JettyMicroservice.class) {
			return INSTANCE;
		}
	}

	/**
	 * Entry-point method.
	 *
	 * @param args Command line arguments.
	 * @throws Exception Error occurred.
	 */
	public static void main(String[] args) throws Exception {
		JettyMicroservice
			.create()
			.args(args)
			.build()
			.start()
			.startConsole()
			.join();
	}

	final MessageBundle messages = MessageBundle.create(JettyMicroservice.class);

	//-----------------------------------------------------------------------------------------------------------------
	// Properties set in constructor
	//-----------------------------------------------------------------------------------------------------------------
	private final JettyMicroserviceBuilder builder;
	final JettyMicroserviceListener listener;
	private final JettyServerFactory factory;

	//-----------------------------------------------------------------------------------------------------------------
	// Properties set in constructor
	//-----------------------------------------------------------------------------------------------------------------
	volatile Server server;

	/**
	 * Creates a new microservice builder.
	 *
	 * @return A new microservice builder.
	 */
	public static JettyMicroserviceBuilder create() {
		return new JettyMicroserviceBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The constructor arguments.
	 * @throws IOException Problem occurred reading file.
	 * @throws ParseException Malformed content found in config file.
	 */
	protected JettyMicroservice(JettyMicroserviceBuilder builder) throws ParseException, IOException {
		super(builder);
		setInstance(this);
		this.builder = builder.copy();
		this.listener = builder.listener != null ? builder.listener : new BasicJettyMicroserviceListener();
		this.factory = builder.factory != null ? builder.factory : new BasicJettyServerFactory();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Methods implemented on Microservice API
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Microservice */
	public synchronized JettyMicroservice init() throws ParseException, IOException {
		super.init();
		return this;
	}

	@Override /* Microservice */
	public synchronized JettyMicroservice startConsole() throws Exception {
		super.startConsole();
		return this;
	}

	@Override /* Microservice */
	public synchronized JettyMicroservice stopConsole() throws Exception {
		super.stopConsole();
		return this;
	}

	@Override /* Microservice */
	public synchronized JettyMicroservice start() throws Exception {
		super.start();
		createServer();
		startServer();
		return this;
	}

	@Override /* Microservice */
	public JettyMicroservice join() throws Exception {
		server.join();
		return this;
	}

	@Override /* Microservice */
	public synchronized JettyMicroservice stop() throws Exception {
		final Logger logger = getLogger();
		final MessageBundle mb2 = messages;
		Thread t = new Thread("JettyMicroserviceStop") {
			@Override /* Thread */
			public void run() {
				try {
					if (server == null || server.isStopping() || server.isStopped())
						return;
					listener.onStopServer(JettyMicroservice.this);
					out(mb2, "StoppingServer");
					server.stop();
					out(mb2, "ServerStopped");
					listener.onPostStopServer(JettyMicroservice.this);
				} catch (Exception e) {
					logger.log(Level.WARNING, e.getLocalizedMessage(), e);
				}
			}
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		super.stop();

		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// JettyMicroservice API methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the port that this microservice started up on.
	 * <p>
	 * The value is determined by looking at the <c>Server/Connectors[ServerConnector]/port</c> value in the
	 * Jetty configuration.
	 *
	 * @return The port that this microservice started up on.
	 */
	public int getPort() {
		for (Connector c : getServer().getConnectors())
			if (c instanceof ServerConnector)
				return ((ServerConnector)c).getPort();
		throw new RuntimeException("Could not locate ServerConnector in Jetty server.");
	}

	/**
	 * Returns the context path that this microservice is using.
	 * <p>
	 * The value is determined by looking at the <c>Server/Handlers[ServletContextHandler]/contextPath</c> value
	 * in the Jetty configuration.
	 *
	 * @return The context path that this microservice is using.
	 */
	public String getContextPath() {
		for (Handler h : getServer().getHandlers()) {
			if (h instanceof HandlerCollection)
				for (Handler h2 : ((HandlerCollection)h).getChildHandlers())
					if (h2 instanceof ServletContextHandler)
						return ((ServletContextHandler)h2).getContextPath();
			if (h instanceof ServletContextHandler)
				return ((ServletContextHandler)h).getContextPath();
		}
		throw new RuntimeException("Could not locate ServletContextHandler in Jetty server.");
	}

	/**
	 * Returns whether this microservice is using <js>"http"</js> or <js>"https"</js>.
	 * <p>
	 * The value is determined by looking for the existence of an SSL Connection Factorie by looking for the
	 * <c>Server/Connectors[ServerConnector]/ConnectionFactories[SslConnectionFactory]</c> value in the Jetty
	 * configuration.
	 *
	 * @return Whether this microservice is using <js>"http"</js> or <js>"https"</js>.
	 */
	public String getProtocol() {
		for (Connector c : getServer().getConnectors())
			if (c instanceof ServerConnector)
				for (ConnectionFactory cf : ((ServerConnector)c).getConnectionFactories())
					if (cf instanceof SslConnectionFactory)
						return "https";
		return "http";
	}

	/**
	 * Returns the hostname of this microservice.
	 * <p>
	 * Simply uses <c>InetAddress.getLocalHost().getHostName()</c>.
	 *
	 * @return The hostname of this microservice.
	 */
	public String getHostName() {
		String hostname = "localhost";
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {}
		return hostname;
	}

	/**
	 * Returns the URI where this microservice is listening on.
	 *
	 * @return The URI where this microservice is listening on.
	 */
	public URI getURI() {
		String cp = getContextPath();
		try {
			return new URI(getProtocol(), null, getHostName(), getPort(), "/".equals(cp) ? null : cp, null, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Method used to create (but not start) an instance of a Jetty server.
	 *
	 * <p>
	 * Subclasses can override this method to customize the Jetty server before it is started.
	 *
	 * <p>
	 * The default implementation is configured by the following values in the config file
	 * if a jetty.xml is not specified via a <c>REST/jettyXml</c> setting:
	 * <p class='bcode w800'>
	 * 	<cc>#================================================================================
	 * 	# Jetty settings
	 * 	#================================================================================</cc>
	 * 	<cs>[Jetty]</cs>
	 *
	 * 	<cc># Path of the jetty.xml file used to configure the Jetty server.</cc>
	 * 	<ck>config</ck> = jetty.xml
	 *
	 * 	<cc># Resolve Juneau variables in the jetty.xml file.</cc>
	 * 	<ck>resolveVars</ck> = true
	 *
	 * 	<cc># Port to use for the jetty server.
	 * 	# You can specify multiple ports.  The first available will be used.  '0' indicates to try a random port.
	 * 	# The resulting available port gets set as the system property "availablePort" which can be referenced in the
	 * 	# jetty.xml file as "$S{availablePort}" (assuming resolveVars is enabled).</cc>
	 * 	<ck>port</ck> = 10000,0,0,0
	 * </p>
	 *
	 * @return The newly-created server.
	 * @throws ParseException Configuration file contains malformed input.
	 * @throws IOException File could not be read.
	 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
	 */
	public Server createServer() throws ParseException, IOException, ExecutableException {
		listener.onCreateServer(this);

		Config cf = getConfig();
		OMap mf = getManifest();
		VarResolver vr = getVarResolver();

		int[] ports = ObjectUtils.firstNonNull(builder.ports, cf.getObjectWithDefault("Jetty/port", mf.getWithDefault("Jetty-Port", new int[]{8000}, int[].class), int[].class));
		int availablePort = findOpenPort(ports);
		setProperty("availablePort", availablePort, false);

		String jettyXml = builder.jettyXml;
		String jettyConfig = cf.getString("Jetty/config", mf.getString("Jetty-Config", "jetty.xml"));
		boolean resolveVars = ObjectUtils.firstNonNull(builder.jettyXmlResolveVars, cf.getBoolean("Jetty/resolveVars"));

		if (jettyXml == null)
			jettyXml = IOUtils.loadSystemResourceAsString("jetty.xml", ".", "files");
		if (jettyXml == null)
			throw new FormattedRuntimeException("jetty.xml file ''{0}'' was not found on the file system or classpath.", jettyConfig);

		if (resolveVars)
			jettyXml = vr.resolve(jettyXml);

		getLogger().info(jettyXml);

		try {
			server = factory.create(jettyXml);
		} catch (Exception e2) {
			throw new ExecutableException(e2);
		}

		for (String s : cf.getStringArray("Jetty/servlets", new String[0])) {
			try {
				ClassInfo c = ClassInfo.of(Class.forName(s));
				if (c.isChildOf(RestServlet.class)) {
					RestServlet rs = (RestServlet)c.newInstance();
					addServlet(rs, rs.getPath());
				} else {
					throw new FormattedRuntimeException("Invalid servlet specified in Jetty/servlets.  Must be a subclass of RestServlet.", s);
				}
			} catch (ClassNotFoundException e1) {
				throw new ExecutableException(e1);
			}
		}

		for (Map.Entry<String,Object> e : cf.getMap("Jetty/servletMap", OMap.EMPTY_MAP).entrySet()) {
			try {
				ClassInfo c = ClassInfo.of(Class.forName(e.getValue().toString()));
				if (c.isChildOf(Servlet.class)) {
					Servlet rs = (Servlet)c.newInstance();
					addServlet(rs, e.getKey());
				} else {
					throw new FormattedRuntimeException("Invalid servlet specified in Jetty/servletMap.  Must be a subclass of Servlet.", e.getValue());
				}
			} catch (ClassNotFoundException e1) {
				throw new ExecutableException(e1);
			}
		}

		for (Map.Entry<String,Object> e : cf.getMap("Jetty/servletAttributes", OMap.EMPTY_MAP).entrySet())
			addServletAttribute(e.getKey(), e.getValue());

		for (Map.Entry<String,Servlet> e : builder.servlets.entrySet())
			addServlet(e.getValue(), e.getKey());

		for (Map.Entry<String,Object> e : builder.servletAttributes.entrySet())
			addServletAttribute(e.getKey(), e.getValue());

		setProperty("juneau.serverPort", availablePort, false);

		return server;
	}

	/**
	 * Calls {@link Server#destroy()} on the underlying Jetty server if it exists.
	 *
	 * @throws Exception Error occurred.
	 */
	public void destroyServer() throws Exception {
		if (server != null)
			server.destroy();
		server = null;
	}

	/**
	 * Adds an arbitrary servlet to this microservice.
	 *
	 * @param servlet The servlet instance.
	 * @param pathSpec The context path of the servlet.
	 * @return This object (for method chaining).
	 * @throws RuntimeException if {@link #createServer()} has not previously been called.
	 */
	public JettyMicroservice addServlet(Servlet servlet, String pathSpec) {
		ServletHolder sh = new ServletHolder(servlet);
		if (pathSpec != null && ! pathSpec.endsWith("/*"))
			pathSpec = trimTrailingSlashes(pathSpec) + "/*";
		getServletContextHandler().addServlet(sh, pathSpec);
		return this;
	}

	/**
	 * Finds and returns the servlet context handler define in the Jetty container.
	 *
	 * @return The servlet context handler.
	 * @throws RuntimeException if context handler is not defined.
	 */
	protected ServletContextHandler getServletContextHandler() {
		for (Handler h : getServer().getHandlers()) {
			ServletContextHandler sch = getServletContextHandler(h);
			if (sch != null)
				return sch;
		}
		throw new RuntimeException("Servlet context handler not found in jetty server.");
	}

	/**
	 * Adds a servlet attribute to the Jetty server.
	 *
	 * @param name The server attribute name.
	 * @param value The context path of the servlet.
	 * @return This object (for method chaining).
	 * @throws RuntimeException if {@link #createServer()} has not previously been called.
	 */
	public JettyMicroservice addServletAttribute(String name, Object value) {
		getServer().setAttribute(name, value);
		return this;
	}

	/**
	 * Returns the underlying Jetty server.
	 *
	 * @return The underlying Jetty server, or <jk>null</jk> if {@link #createServer()} has not yet been called.
	 */
	public Server getServer() {
		if (server == null)
			throw new RuntimeException("Server not found.  createServer() must be called first.");
		return server;
	}

	/**
	 * Method used to start the Jetty server created by {@link #createServer()}.
	 *
	 * <p>
	 * Subclasses can override this method to customize server startup.
	 *
	 * @return The port that this server started on.
	 * @throws Exception Error occurred.
	 */
	protected int startServer() throws Exception {
		listener.onStartServer(this);
		server.start();
		out(messages, "ServerStarted", getPort());
		listener.onPostStartServer(this);
		return getPort();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods.
	//-----------------------------------------------------------------------------------------------------------------

	private static ServletContextHandler getServletContextHandler(Handler h) {
		if (h instanceof ServletContextHandler)
			return (ServletContextHandler)h;
		if (h instanceof HandlerCollection) {
			for (Handler h2 : ((HandlerCollection)h).getHandlers()) {
				ServletContextHandler sch = getServletContextHandler(h2);
				if (sch != null)
					return sch;
			}
		}
		return null;
	}

	private static int findOpenPort(int[] ports) {
		for (int port : ports) {
			// If port is 0, try a random port between ports[0] and 32767.
			if (port == 0)
				port = new Random().nextInt(32767 - ports[0] + 1) + ports[0];
			try (ServerSocket ss = new ServerSocket(port)) {
				return port;
			} catch (IOException e) {}
		}
		return 0;
	}
}
