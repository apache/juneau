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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;

import javax.servlet.*;

import org.apache.juneau.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.json.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.rest.annotation.*;
import org.eclipse.jetty.security.*;
import org.eclipse.jetty.security.authentication.*;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.ssl.*;
import org.eclipse.jetty.servlet.*;
import org.eclipse.jetty.util.security.*;
import org.eclipse.jetty.util.ssl.*;

/**
 * Entry point for Juneau microservice that implements a REST interface using Jetty on a single port.
 *
 * <h6 class='topic'>Jetty Server Details</h6>
 * <p>
 * The Jetty server is created by the {@link #createServer()} method and started with the {@link #startServer()} method.
 * These methods can be overridden to provided customized behavior.
 * <p>
 *
 * <h6 class='topic'>Defining REST Resources</h6>
 * <p>
 * Top-level REST resources are defined by the {@link #getResourceMap()} method.
 * This method can be overridden to provide a customized list of REST resources.
 * <p>
 *
 * <h6 class='topic'>Logging</h6>
 * <p>
 * Logging is initialized by the {@link #initLogging()} method.
 * This method can be overridden to provide customized logging behavior.
 *
 * <h6 class='topic'>Lifecycle Listener Methods</h6>
 * Subclasses can optionally implement the following event listener methods:
 * <ul class='spaced-list'>
 * 	<li>{@link #onStart()} - Gets executed before {@link #start()}.
 * 	<li>{@link #onStop()} - Gets executed before {@link #stop()}.
 * 	<li>{@link #onCreateServer()} - Gets executed before {@link #createServer()}.
 * 	<li>{@link #onStartServer()} - Gets executed before {@link #startServer()}.
 * 	<li>{@link #onPostStartServer()} - Gets executed after {@link #startServer()}.
 * 	<li>{@link #onStopServer()} - Gets executed before {@link #stop()}.
 * 	<li>{@link #onPostStopServer()} - Gets executed after {@link #stop()}.
 * </ul>
 */
public class RestMicroservice extends Microservice {

	Server server;
	int port;
	String contextPath;
	Logger logger;

	/**
	 * Main method.
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
	}

	
	//--------------------------------------------------------------------------------
	// Methods implemented on Microservice API
	//--------------------------------------------------------------------------------

	@Override /* Microservice */
	public RestMicroservice start() throws Exception {
		super.start();
		initLogging();
		createServer();
		startServer();
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice join() throws Exception {
		server.join();
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice stop() {
		Thread t = new Thread() {
			@Override /* Thread */
			public void run() {
				try {
					if (server.isStopping() || server.isStopped())
						return;
					onStopServer();
					logger.warning("Stopping server.");
					server.stop();
					logger.warning("Server stopped.");
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
			e.printStackTrace();
		}
		super.stop();
		return this;
	}

	
	//--------------------------------------------------------------------------------
	// RestMicroservice API methods.
	//--------------------------------------------------------------------------------
	
	/**
	 * Returns the port that this microservice started up on.
	 * @return The port that this microservice started up on.
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Returns the URI where this microservice is listening on.
	 * @return The URI where this microservice is listening on.
	 */
	public URI getURI() {
		String scheme = getConfig().getBoolean("REST/useSsl") ? "https" : "http";
		String hostname = "localhost";
		String ctx = "/".equals(contextPath) ? null : contextPath;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {}
		try {
			return new URI(scheme, null, hostname, port, ctx, null, null);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Initialize the logging for this microservice.
	 * <p>
	 * Subclasses can override this method to provide customized logging.
	 * <p>
	 * The default implementation uses the <cs>Logging</cs> section in the config file to set up logging:
	 * <p class='bcode'>
	 * 	<cc>#================================================================================
	 * 	# Logger settings
	 * 	# See FileHandler Java class for details.
	 * 	#================================================================================</cc>
	 * 	<cs>[Logging]</cs>
	 *
	 * 	<cc># The directory where to create the log file.
	 * 	# Default is ".".</cc>
	 * 	<ck>logDir</ck> = logs
	 *
	 * 	<cc># The name of the log file to create for the main logger.
	 * 	# The logDir and logFile make up the pattern that's passed to the FileHandler
	 * 	# constructor.
	 * 	# If value is not specified, then logging to a file will not be set up.</cc>
	 * 	<ck>logFile</ck> = microservice.%g.log
	 *
	 * 	<cc># Whether to append to the existing log file or create a new one.
	 * 	# Default is false.</cc>
	 * 	<ck>append</ck> =
	 *
	 * 	<cc># The SimpleDateFormat format to use for dates.
	 * 	# Default is "yyyy.MM.dd hh:mm:ss".</cc>
	 * 	<ck>dateFormat</ck> =
	 *
	 * 	<cc># The log message format.
	 * 	# The value can contain any of the following variables:
	 * 	# 	{date} - The date, formatted per dateFormat.
	 * 	#	{class} - The class name.
	 * 	#	{method} - The method name.
	 * 	#	{logger} - The logger name.
	 * 	#	{level} - The log level name.
	 * 	#	{msg} - The log message.
	 * 	#	{threadid} - The thread ID.
	 * 	#	{exception} - The localized exception message.
	 * 	# Default is "[{date} {level}] {msg}%n".</cc>
	 * 	<ck>format</ck> =
	 *
	 * 	<cc># The maximum log file size.
	 * 	# Suffixes available for numbers.
	 * 	# See ConfigFile.getInt(String,int) for details.
	 * 	# Default is 1M.</cc>
	 * 	<ck>limit</ck> = 10M
	 *
	 * 	<cc># Max number of log files.
	 * 	# Default is 1.</cc>
	 * 	<ck>count</ck> = 5
	 *
	 * 	<cc># Default log levels.
	 * 	# Keys are logger names.
	 * 	# Values are serialized Level POJOs.</cc>
	 * 	<ck>levels</ck> = { org.apache.juneau:'INFO' }
	 *
	 * 	<cc># Only print unique stack traces once and then refer to them by a simple 8 character hash identifier.
	 * 	# Useful for preventing log files from filling up with duplicate stack traces.
	 * 	# Default is false.</cc>
	 * 	<ck>useStackTraceHashes</ck> = true
	 *
	 * 	<cc># The default level for the console logger.
	 * 	# Default is WARNING.</cc>
	 * 	<ck>consoleLevel</ck> = WARNING
	 * </p>
	 *
	 * @throws Exception
	 */
	protected void initLogging() throws Exception {
		ConfigFile cf = getConfig();
		logger = Logger.getLogger("");
		String logFile = cf.getString("Logging/logFile");
		if (! isEmpty(logFile)) {
			LogManager.getLogManager().reset();
			String logDir = cf.getString("Logging/logDir", ".");
			mkdirs(new File(logDir), false);
			boolean append = cf.getBoolean("Logging/append");
			int limit = cf.getInt("Logging/limit", 1024*1024);
			int count = cf.getInt("Logging/count", 1);
			FileHandler fh = new FileHandler(logDir + '/' + logFile, limit, count, append);

			boolean useStackTraceHashes = cf.getBoolean("Logging/useStackTraceHashes");
			String format = cf.getString("Logging/format", "[{date} {level}] {msg}%n");
			String dateFormat = cf.getString("Logging/dateFormat", "yyyy.MM.dd hh:mm:ss");
			fh.setFormatter(new LogEntryFormatter(format, dateFormat, useStackTraceHashes));
			logger.addHandler(fh);

			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(Level.parse(cf.getString("Logging/consoleLevel", "WARNING")));
			ch.setFormatter(new LogEntryFormatter(format, dateFormat, false));
			logger.addHandler(ch);
		}
		ObjectMap loggerLevels = cf.getObject("Logging/levels", ObjectMap.class);
		if (loggerLevels != null)
		for (String l : loggerLevels.keySet())
			Logger.getLogger(l).setLevel(loggerLevels.get(Level.class, l));
	}

	/**
	 * Method used to create (but not start) an instance of a Jetty server.
	 * <p>
	 * Subclasses can override this method to customize the Jetty server before it is started.
	 * <p>
	 * The default implementation is configured by the following values in the config file:
	 * <p>
	 * <p class='bcode'>
	 * 	<cc>#================================================================================
	 * 	# REST settings
	 * 	#================================================================================</cc>
	 * 	<cs>[REST]</cs>
	 *
	 * 	<cc># The HTTP port number to use.
	 * 	# Default is Rest-Port setting in manifest file, or 8000.
	 * 	# Can also specify a comma-delimited lists of ports to try, including 0 meaning
	 * 	# try a random port.</cc>
	 * 	<ck>port</ck> = 10000
	 *
	 * 	<cc># The context root of the Jetty server.
	 * 	# Default is Rest-ContextPath in manifest file, or "/".</cc>
	 * 	<ck>contextPath</ck> =
	 *
	 * 	<cc># Authentication:  NONE, BASIC.
	 * 	# Default is Rest-AuthType in manifest file, or NONE.</cc>
	 * 	<ck>authType</ck> = NONE
	 *
	 * 	<cc># The BASIC auth username.
	 * 	# Default is Rest-LoginUser in manifest file.</cc>
	 * 	<ck>loginUser</ck> =
	 *
	 * 	<cc># The BASIC auth password.
	 * 	# Default is Rest-LoginPassword in manifest file.</cc>
	 * 	<ck>loginPassword</ck> =
	 *
	 * 	<cc># The BASIC auth realm.
	 * 	# Default is Rest-AuthRealm in manifest file.</cc>
	 * 	<ck>authRealm</ck> =
	 *
	 * 	<cc># Enable SSL support.</cc>
	 * 	<ck>useSsl</ck> = false
	 *
	 * 	<cc>#================================================================================
	 * 	# Bean properties on the org.eclipse.jetty.util.ssl.SslSocketFactory class
	 * 	#--------------------------------------------------------------------------------
	 * 	# Ignored if REST/useSsl is false.
	 * 	#================================================================================</cc>
	 * 	<cs>[REST-SslContextFactory]</cs>
	 * 	<ck>keyStorePath</ck> = client_keystore.jks
	 * 	<ck>keyStorePassword*</ck> = {HRAaRQoT}
	 * 	<ck>excludeCipherSuites</ck> = TLS_DHE.*, TLS_EDH.*
	 * 	<ck>excludeProtocols</ck> = SSLv3
	 * 	<ck>allowRenegotiate</ck> = false
	 * </p>
	 *
	 * @return The newly-created server.
	 * @throws Exception
	 */
	protected Server createServer() throws Exception {
		onCreateServer();

		ConfigFile cf = getConfig();
		ObjectMap mf = getManifest();
		
		int[] ports = cf.getObjectWithDefault("REST/port", mf.get(int[].class, "Rest-Port", new int[]{8000}), int[].class);

		port = findOpenPort(ports);
		if (port == 0) {
			System.err.println("Open port not found.  Tried " + JsonSerializer.DEFAULT_LAX.toString(ports));
			System.exit(1);
		}
			
		contextPath = cf.getString("REST/contextPath", mf.getString("Rest-ContextPath", "/"));

		if (cf.getBoolean("REST/useSsl")) {

			SslContextFactory sslContextFactory = new SslContextFactory();

			// Write the properties in REST-SslContextFactory to the bean setters on sslContextFactory.
			// Throws an exception if section contains unknown properties.
			// Only look for bean properties of type String/String/boolean/int since class has multiple
			// 	setters with the same name (e.g. setKeyStore(KeyStore) and setKeyStore(String)).
			ObjectMap m = cf.writeProperties("REST-SslContextFactory", sslContextFactory, false, String.class, String[].class, boolean.class, int.class);

			// We're using Jetty 8 that doesn't allow regular expression matching in SslContextFactory.setExcludeCipherSuites(),
			// so to prevent having the config file list all old cipher suites, exclude the known bad ones.
			String[] excludeCipherSuites = combine(
				split("SSL_RSA_WITH_DES_CBC_SHA,SSL_DHE_RSA_WITH_DES_CBC_SHA,SSL_DHE_DSS_WITH_DES_CBC_SHA,SSL_RSA_EXPORT_WITH_RC4_40_MD5,SSL_RSA_EXPORT_WITH_DES40_CBC_SHA,SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,TLS_DHE_RSA_WITH_AES_256_CBC_SHA256,TLS_DHE_DSS_WITH_AES_256_CBC_SHA256,TLS_DHE_RSA_WITH_AES_256_CBC_SHA,TLS_DHE_DSS_WITH_AES_256_CBC_SHA,TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,TLS_DHE_RSA_WITH_AES_128_CBC_SHA,TLS_DHE_DSS_WITH_AES_128_CBC_SHA"),
				sslContextFactory.getExcludeCipherSuites()
			);
			sslContextFactory.setExcludeCipherSuites(excludeCipherSuites);

			logger.log(Level.WARNING, "SSL properties set: {0}", JsonSerializer.DEFAULT_LAX.toString(m));

			SslSocketConnector connector = new SslSocketConnector(sslContextFactory);
			connector.setPort(port);

			server = new Server();
			server.setConnectors(new Connector[] { connector });

		} else {
			server = new Server(port);
		}

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);

		String authType = cf.getString("REST/authType", mf.getString("Rest-AuthType", "NONE"));
		if (authType.equals("BASIC"))
			context.setSecurityHandler(basicAuth(cf, mf));

		context.setContextPath(contextPath);
		server.setHandler(context);

		for (Map.Entry<String,Class<? extends Servlet>> e : getResourceMap().entrySet())
			context.addServlet(e.getValue(), e.getKey()).setInitOrder(0);

		return server;
	}
	
	private static int findOpenPort(int[] ports) {
		for (int port : ports) {
			try {
				// If port is 0, try a random port between ports[0] and 32767.
				if (port == 0)
					port = new Random().nextInt(32767 - ports[0] + 1) + ports[0];
				ServerSocket ss = new ServerSocket(port);
				ss.close();
				return port;
			} catch (IOException e) {}
		}
		return 0;
	}

	/**
	 * Method used to start the Jetty server created by {@link #createServer()}.
	 * <p>
	 * Subclasses can override this method to customize server startup.
	 * 
	 * @return The port that this server started on.
	 * @throws Exception
	 */
	protected int startServer() throws Exception {
		onStartServer();
		server.start();
		logger.warning("Server started on port " + port);
		onPostStartServer();
		return port;
	}

	/**
	 * Returns the resource map to use for this microservice.
	 * <p>
	 * <p>
	 * Subclasses can override this method to programmatically specify their resources.
	 * <p>
	 * The default implementation is configured by the following values in the config file:
	 * <p>
	 * <p class='bcode'>
	 *
	 * 	<cc>#================================================================================
	 * 	# REST settings
	 * 	#================================================================================</cc>
	 * 	<cs>[REST]</cs>
	 *
	 * 	<cc># A JSON map of servlet paths to servlet classes.
	 * 	# Example:
	 * 	# 	resourceMap = {'/*':'com.foo.MyServlet'}
	 * 	# Either resourceMap or resources must be specified if it's not defined in
	 * 	# the manifest file.</cc>
	 * 	<ck>resourceMap</ck> =
	 *
	 * 	<cc># A comma-delimited list of names of classes that extend from Servlet.
	 * 	# Resource paths are pulled from @RestResource.path() annotation, or
	 * 	# 	"/*" if annotation not specified.
	 * 	# Example:
	 * 	# 	resources = com.foo.MyServlet
	 * 	 * 	# Default is Rest-Resources in manifest file.
	 * 	# Either resourceMap or resources must be specified if it's not defined in
	 * 	# the manifest file.</cc>
	 * 	<ck>resources</ck> =
	 * </p>
	 * <p>
	 * In most cases, the rest resources will be specified in the manifest file since
	 * 	it's not likely to be a configurable property:
	 * <p>
	 * <p class='bcode'>
	 * 	<mk>Rest-Resources:</mk> org.apache.juneau.microservice.sample.RootResources
	 * </p>
	 *
	 * @return The map of REST resources.
	 * @throws ClassNotFoundException
	 * @throws ParseException
	 */
	@SuppressWarnings("unchecked")
	protected Map<String,Class<? extends Servlet>> getResourceMap() throws ClassNotFoundException, ParseException {
		ConfigFile cf = getConfig();
		ObjectMap mf = getManifest();
		Map<String,Class<? extends Servlet>> rm = new LinkedHashMap<String,Class<? extends Servlet>>();

		ObjectMap resourceMap = cf.getObject("REST/resourceMap", ObjectMap.class);
		String[] resources = cf.getStringArray("REST/resources", mf.getStringArray("Rest-Resources"));

		if (resourceMap != null && ! resourceMap.isEmpty()) {
			for (Map.Entry<String,Object> e : resourceMap.entrySet()) {
				Class<?> c = Class.forName(e.getValue().toString());
				if (! isParentClass(Servlet.class, c))
					throw new ClassNotFoundException("Invalid class specified as resource.  Must be a Servlet.  Class='"+c.getName()+"'");
				rm.put(e.getKey(), (Class<? extends Servlet>)c);
			}
		} else if (resources.length > 0) {
			for (String resource : resources) {
				Class<?> c = Class.forName(resource);
				if (! isParentClass(Servlet.class, c))
					throw new ClassNotFoundException("Invalid class specified as resource.  Must be a Servlet.  Class='"+c.getName()+"'");
				RestResource rr = c.getAnnotation(RestResource.class);
				String path = rr == null ? "/*" : rr.path();
				if (! path.endsWith("*"))
					path += (path.endsWith("/") ? "*" : "/*");
				rm.put(path, (Class<? extends Servlet>)c);
			}
		}
		return rm;
	}

	/**
	 * Called when {@link ConfigFile#save()} is called on the config file.
	 * <p>
	 * The default behavior is configured by the following value in the config file:
	 * <p>
	 * <p class='bcode'>
	 * 	<cs>[REST]</cs>
	 *
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
			String saveConfigAction = cf.getString("REST/saveConfigAction", "NOTHING");
			if (saveConfigAction.equals("RESTART_SERVER")) {
				new Thread() {
					@Override /* Thread */
					public void run() {
						try {
							RestMicroservice.this.stop();
							RestMicroservice.this.start();
						} catch (Exception e) {
							logger.log(Level.SEVERE, e.getLocalizedMessage(), e);
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

	
	//--------------------------------------------------------------------------------
	// Lifecycle listener methods.
	//--------------------------------------------------------------------------------

	/**
	 * Called before {@link #createServer()} is called.
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onCreateServer() {}

	/**
	 * Called before {@link #startServer()} is called.
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStartServer() {}

	/**
	 * Called after the Jetty server is started.
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onPostStartServer() {}

	/**
	 * Called before the Jetty server is stopped.
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStopServer() {}

	/**
	 * Called after the Jetty server is stopped.
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
	public RestMicroservice setConfig(ConfigFile cf) {
		super.setConfig(cf);
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice setManifest(Manifest mf) {
		super.setManifest(mf);
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice setManifestContents(String...contents) throws IOException {
		super.setManifestContents(contents);
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice setManifest(File f) throws IOException {
		super.setManifest(f);
		return this;
	}

	@Override /* Microservice */
	public RestMicroservice setManifest(Class<?> c) throws IOException {
		super.setManifest(c);
		return this;
	}
	
	//--------------------------------------------------------------------------------
	// Other methods.
	//--------------------------------------------------------------------------------

	private static final SecurityHandler basicAuth(ConfigFile cf, ObjectMap mf) {

		HashLoginService l = new HashLoginService();
		String user = cf.getString("REST/loginUser", mf.getString("Rest-LoginUser"));
		String pw = cf.getString("REST/loginPassword", mf.getString("Rest-LoginPassword"));
		String realm = cf.getString("REST/authRealm", mf.getString("Rest-AuthRealm", ""));
		String ctx = cf.getString("REST/contextPath", mf.getString("Rest-ContextPath", "/"));

		l.putUser(user, Credential.getCredential(pw), new String[] { "user" });
		l.setName(realm);

		Constraint constraint = new Constraint();
		constraint.setName(Constraint.__BASIC_AUTH);
		constraint.setRoles(new String[] { "user" });
		constraint.setAuthenticate(true);

		ConstraintMapping cm = new ConstraintMapping();
		cm.setConstraint(constraint);
		cm.setPathSpec(ctx);

		ConstraintSecurityHandler csh = new ConstraintSecurityHandler();
		csh.setAuthenticator(new BasicAuthenticator());
		csh.setRealmName("myrealm");
		csh.addConstraintMapping(cm);
		csh.setLoginService(l);

		return csh;
	}
}
