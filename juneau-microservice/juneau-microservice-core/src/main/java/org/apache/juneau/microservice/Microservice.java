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

import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.IOUtils.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.console.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.ManifestFileVar;
import org.apache.juneau.utils.*;
import org.apache.juneau.cp.MessageBundle;

/**
 * Parent class for all microservices.
 *
 * <p>
 * A microservice defines a simple API for starting and stopping simple Java services contained in executable jars.
 *
 * <p>
 * The general command for creating and starting a microservice from a main method is as follows:
 * <p class='bcode w800'>
 * 	<jk>public static void</jk> main(String[] args) {
 * 		Microservice.<jsm>create</jsm>().args(args).build().start().join();
 *  }
 * </p>
 *
 * <p>
 * Your microservice class must be specified as the <jk>Main-Class</jk> entry in the manifest file of your microservice
 * jar file if it's an executable jar.
 *
 * <h5 class='topic'>Microservice Configuration</h5>
 *
 * This class defines the following method for accessing configuration for your microservice:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #getArgs()} - The command-line arguments passed to the jar file.
 * 	<li>
 * 		{@link #getConfig()} - An external INI-style configuration file.
 * 	<li>
 * 		{@link #getManifest()} - The manifest file for the main jar file.
 * </ul>
 *
 * <h5 class='topic'>Lifecycle Methods</h5>
 *
 * Subclasses must implement the following lifecycle methods:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #init()} - Gets executed immediately following construction.
 * 	<li>
 * 		{@link #start()} - Gets executed during startup.
 * 	<li>
 * 		{@link #stop()} - Gets executed when 'exit' is typed in the console or an external shutdown signal is received.
 * 	<li>
 * 		{@link #kill()} - Can be used to forcibly shut down the service.  Doesn't get called during normal operation.
 * </ul>
 */
public class Microservice implements ConfigEventListener {

	private static volatile Microservice INSTANCE;

	private static void setInstance(Microservice m) {
		synchronized(Microservice.class) {
			INSTANCE = m;
		}
	}

	/**
	 * Returns the Microservice instance.
	 *
	 * <p>
	 * This method only works if there's only one Microservice instance in a JVM.
	 * Otherwise, it's just overwritten by the last instantiated microservice.
	 *
	 * @return The Microservice instance, or <jk>null</jk> if there isn't one.
	 */
	public static Microservice getInstance() {
		synchronized(Microservice.class) {
			return INSTANCE;
		}
	}


	final MessageBundle messages = MessageBundle.of(Microservice.class);

	//-----------------------------------------------------------------------------------------------------------------
	// Properties set in constructor
	//-----------------------------------------------------------------------------------------------------------------
	private final MicroserviceBuilder builder;
	private final Args args;
	private final Config config;
	private final ManifestFile manifest;
	private final VarResolver varResolver;
	private final MicroserviceListener listener;
	private final Map<String,ConsoleCommand> consoleCommandMap = new ConcurrentHashMap<>();
	private final boolean consoleEnabled;
	private final Scanner consoleReader;
	private final PrintWriter consoleWriter;
	private final Thread consoleThread;
	final File workingDir;
	private final String configName;

	//-----------------------------------------------------------------------------------------------------------------
	// Properties set in init()
	//-----------------------------------------------------------------------------------------------------------------
	private volatile Logger logger;

	/**
	 * Creates a new microservice builder.
	 *
	 * @return A new microservice builder.
	 */
	public static MicroserviceBuilder create() {
		return new MicroserviceBuilder();
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this microservice.
	 * @throws IOException Problem occurred reading file.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings("resource")
	protected Microservice(MicroserviceBuilder builder) throws IOException, ParseException {
		setInstance(this);
		this.builder = builder.copy();
		this.workingDir = builder.workingDir;
		this.configName = builder.configName;

		this.args = builder.args != null ? builder.args : new Args(new String[0]);

		// --------------------------------------------------------------------------------
		// Try to get the manifest file if it wasn't already set.
		// --------------------------------------------------------------------------------
		ManifestFile manifest = builder.manifest;
		if (manifest == null) {
			Manifest m = new Manifest();

			// If running within an eclipse workspace, need to get it from the file system.
			File f = resolveFile("META-INF/MANIFEST.MF");
			if (f.exists() && f.canRead()) {
				try (FileInputStream fis = new FileInputStream(f)) {
					m.read(fis);
				} catch (IOException e) {
					throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n " + read(f), e);
				}
			} else {
				// Otherwise, read from manifest file in the jar file containing the main class.
				URL url = getClass().getResource("META-INF/MANIFEST.MF");
				if (url != null) {
					try {
						m.read(url.openStream());
					} catch (IOException e) {
						throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n " + read(url.openStream()), e);
					}
				}
			}
			manifest = new ManifestFile(m);
		}
		ManifestFileVar.init(manifest);
		this.manifest = manifest;

		// --------------------------------------------------------------------------------
		// Try to resolve the configuration if not specified.
		// --------------------------------------------------------------------------------
		Config config = builder.config;
		ConfigBuilder configBuilder = builder.configBuilder.varResolver(builder.varResolverBuilder.build()).store(ConfigMemoryStore.DEFAULT);
		if (config == null) {
			ConfigStore store = builder.configStore;
			ConfigFileStore cfs = workingDir == null ? ConfigFileStore.DEFAULT : ConfigFileStore.create().directory(workingDir).build();
			for (String name : getCandidateConfigNames()) {
				 if (store != null) {
					 if (store.exists(name)) {
						 configBuilder.store(store).name(name);
						 break;
					 }
				 } else {
					 if (cfs.exists(name)) {
						 configBuilder.store(cfs).name(name);
						 break;
					 }
					 if (ConfigClasspathStore.DEFAULT.exists(name)) {
						 configBuilder.store(ConfigClasspathStore.DEFAULT).name(name);
						 break;
					 }
				 }
			}
			config = configBuilder.build();
		}
		this.config = config;
		Config.setSystemDefault(this.config);
		this.config.addListener(this);

		//-------------------------------------------------------------------------------------------------------------
		// Var resolver.
		//-------------------------------------------------------------------------------------------------------------
		VarResolverBuilder varResolverBuilder = builder.varResolverBuilder;
		this.varResolver = varResolverBuilder.contextObject(ConfigVar.SESSION_config, config).build();

		// --------------------------------------------------------------------------------
		// Initialize console commands.
		// --------------------------------------------------------------------------------
		this.consoleEnabled = ObjectUtils.firstNonNull(builder.consoleEnabled, config.getBoolean("Console/enabled", false));
		if (consoleEnabled) {
			Console c = System.console();
			this.consoleReader = ObjectUtils.firstNonNull(builder.consoleReader, new Scanner(c == null ? new InputStreamReader(System.in) : c.reader()));
			this.consoleWriter = ObjectUtils.firstNonNull(builder.consoleWriter, c == null ? new PrintWriter(System.out, true) : c.writer());

			for (ConsoleCommand cc : builder.consoleCommands) {
				consoleCommandMap.put(cc.getName(), cc);
			}
			for (String s : config.getStringArray("Console/commands")) {
				ConsoleCommand cc;
				try {
					cc = (ConsoleCommand)Class.forName(s).newInstance();
					consoleCommandMap.put(cc.getName(), cc);
				} catch (Exception e) {
					getConsoleWriter().println("Could not create console command '"+s+"', " + e.getLocalizedMessage());
				}
			}
			consoleThread = new Thread("ConsoleThread") {
				@Override /* Thread */
				public void run() {
					Scanner in = getConsoleReader();
					PrintWriter out = getConsoleWriter();

					out.println(messages.getString("ListOfAvailableCommands"));
					for (ConsoleCommand cc : new TreeMap<>(getConsoleCommands()).values())
						out.append("\t").append(cc.getName()).append(" -- ").append(cc.getInfo()).println();
					out.println();

					while (true) {
						String line = null;
						out.append("> ").flush();
						line = in.nextLine();
						Args args = new Args(line);
						if (! args.isEmpty())
							executeCommand(args, in, out);
					}
				}
			};
			consoleThread.setDaemon(true);
		} else {
			this.consoleReader = null;
			this.consoleWriter = null;
			this.consoleThread = null;
		}

		//-------------------------------------------------------------------------------------------------------------
		// Other.
		//-------------------------------------------------------------------------------------------------------------
		this.listener = builder.listener != null ? builder.listener : new BasicMicroserviceListener();

		init();
	}

	private List<String> getCandidateConfigNames() {
		if (configName != null)
			return Collections.singletonList(configName);

		Args args = getArgs();
		if (getArgs().hasArg("configFile"))
			return Collections.singletonList(args.getArg("configFile"));

		ManifestFile manifest = getManifest();
		if (manifest.containsKey("Main-Config"))
			return Collections.singletonList(manifest.getString("Main-Config"));

		return Config.getCandidateSystemDefaultConfigNames();
	}

	/**
	 * Resolves the specified path.
	 *
	 * <p>
	 * If the working directory has been explicitly specified, relative paths are resolved relative to that.
	 *
	 * @param path The path to resolve.
	 * @return The resolved path.
	 */
	protected File resolveFile(String path) {
		if (Paths.get(path).isAbsolute())
			return new File(path);
		if (workingDir != null)
			return new File(workingDir, path);
		return new File(path);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Abstract lifecycle methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Initializes this microservice.
	 *
	 * <p>
	 * This method can be called whenever the microservice is not started.
	 *
	 * <p>
	 * It will initialize (or reinitialize) the console commands, system properties, and logger.
	 *
	 * @return This object (for method chaining).
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException Couldn't read a file.
	 */
	public synchronized Microservice init() throws IOException, ParseException {

		// --------------------------------------------------------------------------------
		// Set system properties.
		// --------------------------------------------------------------------------------
		Set<String> spKeys = config.getKeys("SystemProperties");
		if (spKeys != null)
			for (String key : spKeys)
				System.setProperty(key, config.getString("SystemProperties/"+key));

		// --------------------------------------------------------------------------------
		// Initialize logging.
		// --------------------------------------------------------------------------------
		this.logger = builder.logger;
		LogConfig logConfig = builder.logConfig != null ? builder.logConfig : new LogConfig();
		if (this.logger == null) {
			LogManager.getLogManager().reset();
			this.logger = Logger.getLogger("");
			String logFile = firstNonNull(logConfig.logFile, config.getString("Logging/logFile"));

			if (isNotEmpty(logFile)) {
				String logDir = firstNonNull(logConfig.logDir, config.getString("Logging/logDir", "."));
				File logDirFile = resolveFile(logDir);
				mkdirs(logDirFile, false);
				logDir = logDirFile.getAbsolutePath();
				System.setProperty("juneau.logDir", logDir);

				boolean append = firstNonNull(logConfig.append, config.getBoolean("Logging/append"));
				int limit = firstNonNull(logConfig.limit, config.getInt("Logging/limit", 1024*1024));
				int count = firstNonNull(logConfig.count, config.getInt("Logging/count", 1));

				FileHandler fh = new FileHandler(logDir + '/' + logFile, limit, count, append);

				Formatter f = logConfig.formatter;
				if (f == null) {
					String format = config.getString("Logging/format", "[{date} {level}] {msg}%n");
					String dateFormat = config.getString("Logging/dateFormat", "yyyy.MM.dd hh:mm:ss");
					boolean useStackTraceHashes = config.getBoolean("Logging/useStackTraceHashes");
					f = new LogEntryFormatter(format, dateFormat, useStackTraceHashes);
				}
				fh.setFormatter(f);
				fh.setLevel(firstNonNull(logConfig.fileLevel, config.getObjectWithDefault("Logging/fileLevel", Level.INFO, Level.class)));
				logger.addHandler(fh);

				ConsoleHandler ch = new ConsoleHandler();
				ch.setLevel(firstNonNull(logConfig.consoleLevel, config.getObjectWithDefault("Logging/consoleLevel", Level.WARNING, Level.class)));
				ch.setFormatter(f);
				logger.addHandler(ch);
			}
		}

		OMap loggerLevels = config.getObject("Logging/levels", OMap.class);
		if (loggerLevels != null)
			for (String l : loggerLevels.keySet())
				Logger.getLogger(l).setLevel(loggerLevels.get(l, Level.class));
		for (String l : logConfig.levels.keySet())
			Logger.getLogger(l).setLevel(logConfig.levels.get(l));

		return this;
	}

	/**
	 * Start this application.
	 *
	 * <p>
	 * Overridden methods MUST call this method FIRST so that the {@link MicroserviceListener#onStart(Microservice)} method is called.
	 *
	 * @return This object (for method chaining).
	 * @throws Exception Error occurred.
	 */
	public synchronized Microservice start() throws Exception {

		if (config.getName() == null)
			err(messages, "RunningClassWithoutConfig", getClass().getSimpleName());
		else
			out(messages, "RunningClassWithConfig", getClass().getSimpleName(), config.getName());

		Runtime.getRuntime().addShutdownHook(
			new Thread("ShutdownHookThread") {
				@Override /* Thread */
				public void run() {
					try {
						Microservice.this.stop();
						Microservice.this.stopConsole();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		);

		listener.onStart(this);

		return this;
	}

	/**
	 * Starts the console thread for this microservice.
	 *
	 * @return This object (for method chaining).
	 * @throws Exception Error occurred
	 */
	public synchronized Microservice startConsole() throws Exception {
		if (consoleThread != null && ! consoleThread.isAlive())
			consoleThread.start();
		return this;
	}

	/**
	 * Stops the console thread for this microservice.
	 *
	 * @return This object (for method chaining).
	 * @throws Exception Error occurred
	 */
	public synchronized Microservice stopConsole() throws Exception {
		if (consoleThread != null && consoleThread.isAlive())
			consoleThread.interrupt();
		return this;
	}

	/**
	 * Returns the command-line arguments passed into the application.
	 *
	 * <p>
	 * This method can be called from the class constructor.
	 *
	 * <p>
	 * See {@link Args} for details on using this method.
	 *
	 * @return The command-line arguments passed into the application.
	 */
	public Args getArgs() {
		return args;
	}

	/**
	 * Returns the external INI-style configuration file that can be used to configure your microservice.
	 *
	 * <p>
	 * The config location is determined in the following order:
	 * <ol class='spaced-list'>
	 * 	<li>
	 * 		The first argument passed to the microservice jar.
	 * 	<li>
	 * 		The <c>Main-Config</c> entry in the microservice jar manifest file.
	 * 	<li>
	 * 		The name of the microservice jar with a <js>".cfg"</js> suffix (e.g.
	 * 		<js>"mymicroservice.jar"</js>-&gt;<js>"mymicroservice.cfg"</js>).
	 * </ol>
	 *
	 * <p>
	 * If all methods for locating the config fail, then this method returns an empty config.
	 *
	 * <p>
	 * Subclasses can set their own config file by using the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link MicroserviceBuilder#configStore(ConfigStore)}
	 * 	<li class='jm'>{@link MicroserviceBuilder#configName(String)}
	 * </ul>
	 *
	 * <p>
	 * String variables are automatically resolved using the variable resolver returned by {@link #getVarResolver()}.
	 *
	 * <p>
	 * This method can be called from the class constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<cc>#--------------------------</cc>
	 * 	<cc># My section</cc>
	 * 	<cc>#--------------------------</cc>
	 * 	<cs>[MySection]</cs>
	 *
	 * 	<cc># An integer</cc>
	 * 	<ck>anInt</ck> = 1
	 *
	 * 	<cc># A boolean</cc>
	 * 	<ck>aBoolean</ck> = true
	 *
	 * 	<cc># An int array</cc>
	 * 	<ck>anIntArray</ck> = 1,2,3
	 *
	 * 	<cc># A POJO that can be converted from a String</cc>
	 * 	<ck>aURL</ck> = http://foo
	 *
	 * 	<cc># A POJO that can be converted from JSON</cc>
	 * 	<ck>aBean</ck> = {foo:'bar',baz:123}
	 *
	 * 	<cc># A system property</cc>
	 * 	<ck>locale</ck> = $S{java.locale, en_US}
	 *
	 * 	<cc># An environment variable</cc>
	 * 	<ck>path</ck> = $E{PATH, unknown}
	 *
	 * 	<cc># A manifest file entry</cc>
	 * 	<ck>mainClass</ck> = $MF{Main-Class}
	 *
	 * 	<cc># Another value in this config file</cc>
	 * 	<ck>sameAsAnInt</ck> = $C{MySection/anInt}
	 *
	 * 	<cc># A command-line argument in the form "myarg=foo"</cc>
	 * 	<ck>myArg</ck> = $A{myarg}
	 *
	 * 	<cc># The first command-line argument</cc>
	 * 	<ck>firstArg</ck> = $A{0}
	 *
	 * 	<cc># Look for system property, or env var if that doesn't exist, or command-line arg if that doesn't exist.</cc>
	 * 	<ck>nested</ck> = $S{mySystemProperty,$E{MY_ENV_VAR,$A{0}}}
	 *
	 * 	<cc># A POJO with embedded variables</cc>
	 * 	<ck>aBean2</ck> = {foo:'$A{0}',baz:$C{MySection/anInt}}
	 * </p>
	 *
	 * <p class='bcode w800'>
	 * 	<jc>// Java code for accessing config entries above.</jc>
	 * 	Config cf = getConfig();
	 *
	 * 	<jk>int</jk> anInt = cf.getInt(<js>"MySection/anInt"</js>);
	 * 	<jk>boolean</jk> aBoolean = cf.getBoolean(<js>"MySection/aBoolean"</js>);
	 * 	<jk>int</jk>[] anIntArray = cf.getObject(<jk>int</jk>[].<jk>class</jk>, <js>"MySection/anIntArray"</js>);
	 * 	URL aURL = cf.getObject(URL.<jk>class</jk>, <js>"MySection/aURL"</js>);
	 * 	MyBean aBean = cf.getObject(MyBean.<jk>class</jk>, <js>"MySection/aBean"</js>);
	 * 	Locale locale = cf.getObject(Locale.<jk>class</jk>, <js>"MySection/locale"</js>);
	 * 	String path = cf.getString(<js>"MySection/path"</js>);
	 * 	String mainClass = cf.getString(<js>"MySection/mainClass"</js>);
	 * 	<jk>int</jk> sameAsAnInt = cf.getInt(<js>"MySection/sameAsAnInt"</js>);
	 * 	String myArg = cf.getString(<js>"MySection/myArg"</js>);
	 * 	String firstArg = cf.getString(<js>"MySection/firstArg"</js>);
	 * </p>
	 *
	 * @return The config file for this application, or <jk>null</jk> if no config file is configured.
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * Returns the main jar manifest file contents as a simple {@link OMap}.
	 *
	 * <p>
	 * This map consists of the contents of {@link Manifest#getMainAttributes()} with the keys and entries converted to
	 * simple strings.
	 * <p>
	 * This method can be called from the class constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode w800'>
	 * 	<jc>// Get Main-Class from manifest file.</jc>
	 * 	String mainClass = Microservice.<jsm>getManifest</jsm>().getString(<js>"Main-Class"</js>, <js>"unknown"</js>);
	 *
	 * 	<jc>// Get Rest-Resources from manifest file.</jc>
	 * 	String[] restResources = Microservice.<jsm>getManifest</jsm>().getStringArray(<js>"Rest-Resources"</js>);
	 * </p>
	 *
	 * @return The manifest file from the main jar, or <jk>null</jk> if the manifest file could not be retrieved.
	 */
	public ManifestFile getManifest() {
		return manifest;
	}

	/**
	 * Returns the variable resolver for resolving variables in strings and files.
	 *
	 * <p>
	 * Variables can be controlled by the following methods:
	 * <ul class='javatree'>
	 * 	<li class='jm'>{@link MicroserviceBuilder#vars(Class...)}
	 * 	<li class='jm'>{@link MicroserviceBuilder#varContext(String, Object)}
	 * </ul>
	 *
	 * @return The VarResolver used by this Microservice, or <jk>null</jk> if it was never created.
	 */
	public VarResolver getVarResolver() {
		return varResolver;
	}

	/**
	 * Returns the logger for this microservice.
	 *
	 * @return The logger for this microservice.
	 */
	public Logger getLogger() {
		return logger;
	}

	/**
	 * Executes a console command.
	 *
	 * @param args
	 * 	The command arguments.
	 * 	<br>The first entry in the arguments is always the command name.
	 * @param in Console input.
	 * @param out Console output.
	 * @return <jk>true</jk> if the command returned <jk>true</jk> meaning the console thread should exit.
	 */
	public boolean executeCommand(Args args, Scanner in, PrintWriter out) {
		ConsoleCommand cc = consoleCommandMap.get(args.getArg(0));
		if (cc == null) {
			out.println(messages.getString("UnknownCommand"));
		} else {
			try {
				return cc.execute(in, out, args);
			} catch (Exception e) {
				e.printStackTrace(out);
			}
		}
		return false;
	}

	/**
	 * Convenience method for executing a console command directly.
	 *
	 * <p>
	 * Allows you to execute a console command outside the console by simulating input and output.
	 *
	 * @param command The command name to execute.
	 * @param input Optional input to the command.  Can be <jk>null</jk>.
	 * @param args Optional command arguments to pass to the command.
	 * @return The command output.
	 */
	public String executeCommand(String command, String input, Object...args) {
		StringWriter sw = new StringWriter();
		List<String> l = new ArrayList<>();
		l.add(command);
		for (Object a : args)
			l.add(stringify(a));
		Args args2 = new Args(l.toArray(new String[l.size()]));
		try (Scanner in = new Scanner(input); PrintWriter out = new PrintWriter(sw)) {
			executeCommand(args2, in, out);
		}
		return sw.toString();
	}

	/**
	 * Joins the application with the current thread.
	 *
	 * <p>
	 * Default implementation is a no-op.
	 *
	 * @return This object (for method chaining).
	 * @throws Exception Error occurred
	 */
	public Microservice join() throws Exception {
		return this;
	}

	/**
	 * Stop this application.
	 *
	 * <p>
	 * Overridden methods MUST call this method LAST so that the {@link MicroserviceListener#onStop(Microservice)} method is called.
	 *
	 * @return This object (for method chaining).
	 * @throws Exception Error occurred
	 */
	public Microservice stop() throws Exception {
		listener.onStop(this);
		return this;
	}

	/**
	 * Stops the console (if it's started) and calls {@link System#exit(int)}.
	 *
	 * @throws Exception Error occurred
	 */
	public void exit() throws Exception {
		try {
			stopConsole();
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.exit(0);
	}

	/**
	 * Kill the JVM by calling <c>System.exit(2);</c>.
	 */
	public void kill() {
		// This triggers the shutdown hook.
		System.exit(2);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the console commands associated with this microservice.
	 *
	 * @return The console commands associated with this microservice as an unmodifiable map.
	 */
	public final Map<String,ConsoleCommand> getConsoleCommands() {
		return consoleCommandMap;
	}

	/**
	 * Returns the console reader.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own console input.
	 *
	 * @return The console reader.  Never <jk>null</jk>.
	 */
	protected Scanner getConsoleReader() {
		return consoleReader;
	}

	/**
	 * Returns the console writer.
	 *
	 * <p>
	 * Subclasses can override this method to provide their own console output.
	 *
	 * @return The console writer.  Never <jk>null</jk>.
	 */
	protected PrintWriter getConsoleWriter() {
		return consoleWriter;
	}

	/**
	 * Prints a localized message to the console writer.
	 *
	 * <p>
	 * Ignored if <js>"Console/enabled"</js> is <jk>false</jk>.
	 *
	 * @param mb The message bundle containing the message.
	 * @param messageKey The message key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void out(MessageBundle mb, String messageKey, Object...args) {
		String msg = mb.getString(messageKey, args);
		if (consoleEnabled)
			getConsoleWriter().println(msg);
		log(Level.INFO, msg);
	}

	/**
	 * Prints a localized message to STDERR.
	 *
	 * <p>
	 * Ignored if <js>"Console/enabled"</js> is <jk>false</jk>.
	 *
	 * @param mb The message bundle containing the message.
	 * @param messageKey The message key.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	public void err(MessageBundle mb, String messageKey, Object...args) {
		String msg = mb.getString(messageKey, args);
		if (consoleEnabled)
			System.err.println(mb.getString(messageKey, args));  // NOT DEBUG
		log(Level.SEVERE, msg);
	}

	/**
	 * Logs a message to the log file.
	 *
	 * @param level The log level.
	 * @param message The message text.
	 * @param args Optional {@link MessageFormat}-style arguments.
	 */
	protected void log(Level level, String message, Object...args) {
		String msg = args.length == 0 ? message : MessageFormat.format(message, args);
		getLogger().log(level, msg);
	}

	@Override /* ConfigChangeListener */
	public void onConfigChange(ConfigEvents events) {
		listener.onConfigChange(this, events);
	}
}
