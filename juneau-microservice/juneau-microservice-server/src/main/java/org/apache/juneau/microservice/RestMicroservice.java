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
package org.apache.juneau.microservice;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.xml.*;

/**
 * Entry point for Juneau microservice that implements a REST interface using Jetty on a single port.
 *
 * <h6 class='topic'>Jetty Server Details</h6>
 * 
 * The Jetty server is created by the {@link #createServer()} method and started with the {@link #startServer()} method.
 * These methods can be overridden to provided customized behavior.
 *
 * <h6 class='topic'>Defining REST Resources</h6>
 * 
 * Top-level REST resources are defined in the <code>jetty.xml</code> file as normal servlets.
 *
 * <h6 class='topic'>Logging</h6>
 * 
 * Logging is initialized by the {@link #initLogging()} method.
 * This method can be overridden to provide customized logging behavior.
 *
 * <h6 class='topic'>Lifecycle Listener Methods</h6>
 * Subclasses can optionally implement the following event listener methods:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #onStart()} - Gets executed before {@link #start()}.
 * 	<li>
 * 		{@link #onStop()} - Gets executed before {@link #stop()}.
 * 	<li>
 * 		{@link #onCreateServer()} - Gets executed before {@link #createServer()}.
 * 	<li>
 * 		{@link #onStartServer()} - Gets executed before {@link #startServer()}.
 * 	<li>
 * 		{@link #onPostStartServer()} - Gets executed after {@link #startServer()}.
 * 	<li>
 * 		{@link #onStopServer()} - Gets executed before {@link #stop()}.
 * 	<li>
 * 		{@link #onPostStopServer()} - Gets executed after {@link #stop()}.
 * </ul>
 */
public class RestMicroservice extends Microservice {
	
	Server server;
	private Object jettyXml;
	private final MessageBundle mb = MessageBundle.create(RestMicroservice.class, "Messages");
	
	private static volatile RestMicroservice INSTANCE;
	
	/**
	 * Returns the Microservice instance.  
	 * <p>
	 * This method only works if there's only one Microservice instance in a JVM.  
	 * Otherwise, it's just overwritten by the last call to {@link #RestMicroservice(String...)}.
	 * 
	 * @return The Microservice instance, or <jk>null</jk> if there isn't one.
	 */
	public static RestMicroservice getInstance() {
		synchronized(RestMicroservice.class) {
			return INSTANCE;
		}
	}
	
	/**
	 * Main method.
	 * 
	 * <p>
	 * Subclasses must also implement this method!
	 *
	 * @param args Command line arguments.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		new RestMicroservice(args).start().join();
	}

	/**
	 * Constructor.
	 *
	 * @param args Command line arguments.
	 * @throws Exception
	 */
	public RestMicroservice(String...args) throws Exception {
		super(args);
		setInstance(this);
	}
	
	private static void setInstance(RestMicroservice rm) {
		synchronized(RestMicroservice.class) {
			INSTANCE = rm;
		}
	}

	//--------------------------------------------------------------------------------
	// Methods implemented on Microservice API
	//--------------------------------------------------------------------------------

	@Override /* Microservice */
	public RestMicroservice start() throws Exception {
		super.start();
		createServer();
		startServer();
		startConsole();
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice join() throws Exception {
		server.join();
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice stop() {
		final Logger logger = getLogger();
		final MessageBundle mb2 = mb;
		Thread t = new Thread() {
			@Override /* Thread */
			public void run() {
				try {
					if (server.isStopping() || server.isStopped())
						return;
					onStopServer();
					out(mb2, "StoppingServer");
					server.stop();
					out(mb2, "ServerStopped");
					onPostStopServer();
				} catch (Exception e) {
					logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
				}
			}
		};
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			logger.log(Level.WARNING, e.getLocalizedMessage(), e);
		}
		super.stop();
		return this;
	}


	//--------------------------------------------------------------------------------
	// RestMicroservice API methods.
	//--------------------------------------------------------------------------------

	/**
	 * Returns the port that this microservice started up on.
	 * <p>
	 * The value is determined by looking at the <code>Server/Connectors[ServerConnector]/port</code> value in the 
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
	 * The value is determined by looking at the <code>Server/Handlers[ServletContextHandler]/contextPath</code> value 
	 * in the Jetty configuration.
	 * 
	 * @return The context path that this microservice is using.
	 */
	public String getContextPath() {
		for (Handler h : getServer().getHandlers()) {
			if (h instanceof HandlerCollection) {
				for (Handler h2 : ((HandlerCollection)h).getChildHandlers())
					if (h2 instanceof ServletContextHandler) 
						return ((ServletContextHandler)h2).getContextPath();
			}
			if (h instanceof ServletContextHandler) 
				return ((ServletContextHandler)h).getContextPath();
		}
		throw new RuntimeException("Could not locate ServletContextHandler in Jetty server.");
	}
	
	/**
	 * Returns whether this microservice is using <js>"http"</js> or <js>"https"</js>.
	 * <p>
	 * The value is determined by looking for the existence of an SSL Connection Factorie by looking for the
	 * <code>Server/Connectors[ServerConnector]/ConnectionFactories[SslConnectionFactory]</code> value in the Jetty
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
	 * Simply uses <code>InetAddress.getLocalHost().getHostName()</code>.
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
	 * if a jetty.xml is not specified via a <code>REST/jettyXml</code> setting:
	 * <p class='bcode'>
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
	 * @throws Exception
	 */
	protected Server createServer() throws Exception {
		onCreateServer();

		ConfigFile cf = getConfig();
		ObjectMap mf = getManifest();
		VarResolver vr = getVarResolver();
		
		int[] ports = cf.getObjectWithDefault("Jetty/port", mf.getWithDefault("Jetty-Port", new int[]{8000}, int[].class), int[].class);
		int availablePort = findOpenPort(ports);
		System.setProperty("availablePort", String.valueOf(availablePort));
		
		if (jettyXml == null)
			jettyXml = cf.getString("Jetty/config", mf.getString("Jetty-Config", null));
		
		if (jettyXml == null)
			throw new FormattedRuntimeException("Jetty.xml file location was not specified in the configuration file (Jetty/config) or manifest file (Jetty-Config).");
		
		String xmlConfig = null;
		
		if (jettyXml instanceof String) 
			jettyXml = new File(jettyXml.toString());
		
		if (jettyXml instanceof File) {
			File f = (File)jettyXml;
			if (f.exists())
				xmlConfig = IOUtils.read((File)jettyXml);
			else 
				throw new FormattedRuntimeException("Jetty.xml file ''{0}'' was specified but not found on the file system.", f.getName());
		} else {
			xmlConfig = IOUtils.read(jettyXml);
		}
		
		if (cf.getBoolean("Jetty/resolveVars", false))
			xmlConfig = vr.resolve(xmlConfig);
		
		getLogger().info(xmlConfig);
		
		XmlConfiguration config = new XmlConfiguration(new ByteArrayInputStream(xmlConfig.getBytes()));
		server = (Server)config.configure();
		
		return server;
	}
	
	/**
	 * Adds an arbitrary servlet to this microservice.
	 * 
	 * @param servlet The servlet instance.
	 * @param pathSpec The context path of the servlet.
	 * @return This object (for method chaining).
	 * @throws RuntimeException if {@link #createServer()} has not previously been called.
	 */
	public RestMicroservice addServlet(Servlet servlet, String pathSpec) {
		for (Handler h : getServer().getHandlers()) {
			if (h instanceof ServletContextHandler) {
				ServletHolder sh = new ServletHolder(servlet);
				((ServletContextHandler)h).addServlet(sh, pathSpec);
				return this;
			}
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
	public RestMicroservice addServletAttribute(String name, Object value) {
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

	/**
	 * Method used to start the Jetty server created by {@link #createServer()}.
	 * 
	 * <p>
	 * Subclasses can override this method to customize server startup.
	 *
	 * @return The port that this server started on.
	 * @throws Exception
	 */
	protected int startServer() throws Exception {
		onStartServer();
		server.start();
		out(mb, "ServerStarted", getPort());
		onPostStartServer();
		return getPort();
	}

	/**
	 * Called when {@link ConfigFile#save()} is called on the config file.
	 * 
	 * <p>
	 * The default behavior is configured by the following value in the config file:
	 * <p class='bcode'>
	 * 	<cc># What to do when the config file is saved.
	 * 	# Possible values:
	 * 	# 	NOTHING - Don't do anything. (default)
	 * 	#	RESTART_SERVER - Restart the Jetty server.
	 * 	#	RESTART_SERVICE - Shutdown and exit with code '3'.</cc>
	 * 	<ck>saveConfigAction</ck> = RESTART_SERVER
	 * </p>
	 */
	@Override /* Microservice */
	protected void onConfigSave(ConfigFile cf) {
		try {
			String saveConfigAction = cf.getString("saveConfigAction", "NOTHING");
			if (saveConfigAction.equals("RESTART_SERVER")) {
				new Thread() {
					@Override /* Thread */
					public void run() {
						try {
							RestMicroservice.this.stop();
							RestMicroservice.this.start();
						} catch (Exception e) {
							getLogger().log(Level.SEVERE, e.getLocalizedMessage(), e);
						}
					}
				}.start();
			} else if (saveConfigAction.equals("RESTART_SERVICE")) {
				stop();
				System.exit(3);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Sets the <code>jetty.xml</code> used to configure the Jetty server.
	 * 
	 * <p>
	 * 
	 * @param jettyXml 
	 * 	The <code>jetty.xml</code>.
	 * 	<br>Can be any of the following:
	 * 	<ul>
	 * 		<li>A {@link File} representing the location on the file system.
	 * 		<li>An {@link InputStream} containing the contents of the file.
	 * 		<li>A {@link String} representing the file system path.
	 * 	</ul>
	 * @return This object (for method chaining).
	 */
	public RestMicroservice setJettyXml(Object jettyXml) {
		if (jettyXml instanceof String || jettyXml instanceof File || jettyXml instanceof InputStream || jettyXml instanceof Reader)
			this.jettyXml = jettyXml;
		else
			throw new FormattedRuntimeException("Invalid object type passed to setJettyXml()", jettyXml == null ? null : jettyXml.getClass().getName());
		return this;
	}

	
	//--------------------------------------------------------------------------------
	// Lifecycle listener methods.
	//--------------------------------------------------------------------------------

	/**
	 * Called before {@link #createServer()} is called.
	 * 
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onCreateServer() {}

	/**
	 * Called before {@link #startServer()} is called.
	 * 
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStartServer() {}

	/**
	 * Called after the Jetty server is started.
	 * 
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onPostStartServer() {}

	/**
	 * Called before the Jetty server is stopped.
	 * 
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStopServer() {}

	/**
	 * Called after the Jetty server is stopped.
	 * 
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onPostStopServer() {}


	//--------------------------------------------------------------------------------
	// Overridden methods.
	//--------------------------------------------------------------------------------

	@Override /* Microservice */
	public RestMicroservice setConfig(String cfPath, boolean create) throws IOException {
		super.setConfig(cfPath, create);
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice setManifestContents(String...contents) throws IOException {
		super.setManifestContents(contents);
		return this;
	}
}
