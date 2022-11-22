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

import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.collections.JsonMap.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.common.internal.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.store.ConfigStore;
import org.apache.juneau.cp.Messages;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.microservice.console.ConsoleCommand;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.reflect.ClassInfo;
import org.apache.juneau.rest.annotation.Rest;
import org.apache.juneau.rest.servlet.RestServlet;
import org.apache.juneau.svl.*;
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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../../index.html#juneau-microservice-jetty">juneau-microservice-jetty</a>
 * </ul>
 */
public class JettyMicroservice extends Microservice {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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

	/**
	 * Creates a new microservice builder.
	 *
	 * @return A new microservice builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Builder
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	public static class Builder extends Microservice.Builder {

		String jettyXml;
		int[] ports;
		Boolean jettyXmlResolveVars;
		Map<String,Servlet> servlets = map();
		Map<String,Object> servletAttributes = map();
		JettyMicroserviceListener listener;
		JettyServerFactory factory;

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy settings from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			this.jettyXml = copyFrom.jettyXml;
			this.ports = copyFrom.ports;
			this.jettyXmlResolveVars = copyFrom.jettyXmlResolveVars;
			this.servlets = copyOf(copyFrom.servlets);
			this.servletAttributes = copyOf(copyFrom.servletAttributes);
			this.listener = copyFrom.listener;
		}

		@Override /* MicroserviceBuilder */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * Specifies the contents or location of the <c>jetty.xml</c> file used by the Jetty server.
		 *
		 * <p>
		 * If you do not specify this value, it is pulled from the following in the specified order:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<c>Jetty/config</c> setting in the config file.
		 * 		<c>Jetty-Config</c> setting in the manifest file.
		 * </ul>
		 *
		 * <p>
		 * By default, we look for the <c>jetty.xml</c> file in the following locations:
		 * <ul class='spaced-list'>
		 * 	<li><c>jetty.xml</c> in home directory.
		 * 	<li><c>files/jetty.xml</c> in home directory.
		 * 	<li><c>/jetty.xml</c> in classpath.
		 * 	<li><c>/files/jetty.xml</c> in classpath.
		 * </ul>
		 *
		 * @param jettyXml
		 * 	The contents or location of the file.
		 * 	<br>Can be any of the following:
		 * 	<ul>
		 * 		<li>{@link String} - Relative path to file on file system or classpath.
		 * 		<li>{@link File} - File on file system.
		 * 		<li>{@link InputStream} - Raw contents as <c>UTF-8</c> encoded stream.
		 * 		<li>{@link Reader} - Raw contents.
		 * 	</ul>
		 *
		 * @param resolveVars
		 * 	If <jk>true</jk>, SVL variables in the file will automatically be resolved.
		 * @return This object.
		 * @throws IOException Thrown by underlying stream.
		 */
		public Builder jettyXml(Object jettyXml, boolean resolveVars) throws IOException {
			if (jettyXml instanceof String)
				this.jettyXml = read(resolveFile(jettyXml.toString()));
			else if (jettyXml instanceof File)
				this.jettyXml = read((File)jettyXml);
			else if (jettyXml instanceof InputStream)
				this.jettyXml = read((InputStream)jettyXml);
			else if (jettyXml instanceof Reader)
				this.jettyXml = read((Reader)jettyXml);
			else
				throw new BasicRuntimeException("Invalid object type passed to jettyXml(Object): {0}", className(jettyXml));
			this.jettyXmlResolveVars = resolveVars;
			return this;
		}

		/**
		 * Specifies the ports to use for the web server.
		 *
		 * <p>
		 * You can specify multiple ports.  The first available will be used.  <js>'0'</js> indicates to try a random port.
		 * The resulting available port gets set as the system property <js>"availablePort"</js> which can be referenced in the
		 * <c>jetty.xml</c> file as <js>"$S{availablePort}"</js> (assuming resolveVars is enabled).
		 *
		 * <p>
		 * If you do not specify this value, it is pulled from the following in the specified order:
		 * <ul class='spaced-list'>
		 * 	<li>
		 * 		<c>Jetty/port</c> setting in the config file.
		 * 	<li>
		 * 		<c>Jetty-Port</c> setting in the manifest file.
		 * 	<li>
		 * 		<c>8000</c>
		 * </ul>
		 *
		 * Jetty/port", mf.getWithDefault("Jetty-Port", new int[]{8000}
		 * @param ports The ports to use for the web server.
		 * @return This object.
		 */
		public Builder ports(int...ports) {
			this.ports = ports;
			return this;
		}

		/**
		 * Adds a servlet to the servlet container.
		 *
		 * <p>
		 * This method can only be used with servlets with no-arg constructors.
		 * <br>The path is pulled from the {@link Rest#path()} annotation.
		 *
		 * @param c The servlet to add to the servlet container.
		 * @return This object.
		 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
		 */
		public Builder servlet(Class<? extends RestServlet> c) throws ExecutableException {
			RestServlet rs;
			try {
				rs = c.getDeclaredConstructor().newInstance();
			} catch (Exception e) {
				throw new ExecutableException(e);
			}
			return servlet(rs, '/' + rs.getPath());
		}

		/**
		 * Adds a servlet to the servlet container.
		 *
		 * <p>
		 * This method can only be used with servlets with no-arg constructors.
		 *
		 * @param c The servlet to add to the servlet container.
		 * @param path The servlet path spec.
		 * @return This object.
		 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
		 */
		public Builder servlet(Class<? extends Servlet> c, String path) throws ExecutableException {
			try {
				return servlet(c.getDeclaredConstructor().newInstance(), path);
			} catch (Exception e) {
				throw new ExecutableException(e);
			}
		}

		/**
		 * Adds a servlet instance to the servlet container.
		 *
		 * @param servlet The servlet to add to the servlet container.
		 * @param path The servlet path spec.
		 * @return This object.
		 */
		public Builder servlet(Servlet servlet, String path) {
			servlets.put(path, servlet);
			return this;
		}

		/**
		 * Adds a set of servlets to the servlet container.
		 *
		 * @param servlets
		 * 	A map of servlets to add to the servlet container.
		 * 	<br>Keys are path specs for the servlet.
		 * @return This object.
		 */
		public Builder servlets(Map<String,Servlet> servlets) {
			if (servlets != null)
				this.servlets.putAll(servlets);
			return this;
		}

		/**
		 * Adds a servlet attribute to the servlet container.
		 *
		 * @param name The attribute name.
		 * @param value The attribute value.
		 * @return This object.
		 */
		public Builder servletAttribute(String name, Object value) {
			this.servletAttributes.put(name, value);
			return this;
		}

		/**
		 * Adds a set of servlet attributes to the servlet container.
		 *
		 * @param values The map of attributes.
		 * @return This object.
		 */
		public Builder servletAttribute(Map<String,Object> values) {
			if (values != null)
				this.servletAttributes.putAll(values);
			return this;
		}

		/**
		 * Specifies the factory to use for creating the Jetty {@link Server} instance.
		 *
		 * <p>
		 * If not specified, uses {@link BasicJettyServerFactory}.
		 *
		 * @param value The new value for this property.
		 * @return This object.
		 */
		public Builder jettyServerFactory(JettyServerFactory value) {
			this.factory = value;
			return this;
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Inherited from MicroserviceBuilder
		//-----------------------------------------------------------------------------------------------------------------

		@Override /* MicroserviceBuilder */
		public JettyMicroservice build() throws Exception {
			return new JettyMicroservice(this);
		}

		@Override /* MicroserviceBuilder */
		public Builder args(Args args) {
			super.args(args);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder args(String...args) {
			super.args(args);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder manifest(Object manifest) throws IOException {
			super.manifest(manifest);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder logger(Logger logger) {
			super.logger(logger);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder config(Config config) {
			super.config(config);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder configName(String configName) {
			super.configName(configName);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder configStore(ConfigStore configStore) {
			super.configStore(configStore);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder consoleEnabled(boolean consoleEnabled) {
			super.consoleEnabled(consoleEnabled);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder consoleCommands(ConsoleCommand...consoleCommands) {
			super.consoleCommands(consoleCommands);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder console(Scanner consoleReader, PrintWriter consoleWriter) {
			super.console(consoleReader, consoleWriter);
			return this;
		}

		@Override /* MicroserviceBuilder */
		@SuppressWarnings("unchecked")
		public Builder vars(Class<? extends Var>...vars) {
			super.vars(vars);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public <T> Builder varBean(Class<T> c, T value) {
			super.varBean(c, value);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder workingDir(File path) {
			super.workingDir(path);
			return this;
		}

		@Override /* MicroserviceBuilder */
		public Builder workingDir(String path) {
			super.workingDir(path);
			return this;
		}

		/**
		 * Registers an event listener for this microservice.
		 *
		 * @param listener An event listener for this microservice.
		 * @return This object.
		 */
		public Builder listener(JettyMicroserviceListener listener) {
			super.listener(listener);
			this.listener = listener;
			return this;
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final Messages messages = Messages.of(JettyMicroservice.class);

	private final Builder builder;
	final JettyMicroserviceListener listener;
	private final JettyServerFactory factory;

	volatile Server server;

	/**
	 * Constructor.
	 *
	 * @param builder The constructor arguments.
	 * @throws IOException Problem occurred reading file.
	 * @throws ParseException Malformed content found in config file.
	 */
	protected JettyMicroservice(Builder builder) throws ParseException, IOException {
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
		final Messages mb2 = messages;
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
			throw asRuntimeException(e);
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
	 * <p class='bini'>
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
		JsonMap mf = getManifest();
		VarResolver vr = getVarResolver();

		int[] ports = ObjectUtils.firstNonNull(builder.ports, cf.get("Jetty/port").as(int[].class).orElseGet(()->mf.getWithDefault("Jetty-Port", new int[]{8000}, int[].class)));
		int availablePort = findOpenPort(ports);

		if (System.getProperty("availablePort") == null)
			System.setProperty("availablePort", String.valueOf(availablePort));

		String jettyXml = builder.jettyXml;
		String jettyConfig = cf.get("Jetty/config").orElse(mf.getString("Jetty-Config", "jetty.xml"));
		boolean resolveVars = ObjectUtils.firstNonNull(builder.jettyXmlResolveVars, cf.get("Jetty/resolveVars").asBoolean().orElse(false));

		if (jettyXml == null)
			jettyXml = IOUtils.loadSystemResourceAsString("jetty.xml", ".", "files");
		if (jettyXml == null)
			throw new BasicRuntimeException("jetty.xml file ''{0}'' was not found on the file system or classpath.", jettyConfig);

		if (resolveVars)
			jettyXml = vr.resolve(jettyXml);

		getLogger().info(jettyXml);

		try {
			server = factory.create(jettyXml);
		} catch (Exception e2) {
			throw new ExecutableException(e2);
		}

		for (String s : cf.get("Jetty/servlets").asStringArray().orElse(new String[0])) {
			try {
				ClassInfo c = ClassInfo.of(Class.forName(s));
				if (c.isChildOf(RestServlet.class)) {
					RestServlet rs = (RestServlet)c.newInstance();
					addServlet(rs, rs.getPath());
				} else {
					throw new BasicRuntimeException("Invalid servlet specified in Jetty/servlets.  Must be a subclass of RestServlet: {0}", s);
				}
			} catch (ClassNotFoundException e1) {
				throw new ExecutableException(e1);
			}
		}

		cf.get("Jetty/servletMap").asMap().orElse(EMPTY_MAP).forEach((k,v) -> {
			try {
				ClassInfo c = ClassInfo.of(Class.forName(v.toString()));
				if (c.isChildOf(Servlet.class)) {
					Servlet rs = (Servlet)c.newInstance();
					addServlet(rs, k);
				} else {
					throw new BasicRuntimeException("Invalid servlet specified in Jetty/servletMap.  Must be a subclass of Servlet: {0}", v);
				}
			} catch (ClassNotFoundException e1) {
				throw new ExecutableException(e1);
			}
		});

		cf.get("Jetty/servletAttributes").asMap().orElse(EMPTY_MAP).forEach((k,v) -> addServletAttribute(k, v));

		builder.servlets.forEach((k,v) -> addServlet(v, k));

		builder.servletAttributes.forEach((k,v) -> addServletAttribute(k, v));

		if (System.getProperty("juneau.serverPort") == null)
			System.setProperty("juneau.serverPort", String.valueOf(availablePort));

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
	 * @return This object.
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
	 * @return This object.
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
	// Utility methods
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
