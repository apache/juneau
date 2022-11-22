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

import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.internal.ClassUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.internal.FileUtils.*;
import static org.apache.juneau.internal.ObjectUtils.*;

import java.io.*;
import java.io.Console;
import java.net.*;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.jar.*;
import java.util.logging.*;
import java.util.logging.Formatter;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.config.store.FileStore;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.cp.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.console.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.parser.ParseException;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.utils.*;

/**
 * Parent class for all microservices.
 *
 * <p>
 * A microservice defines a simple API for starting and stopping simple Java services contained in executable jars.
 *
 * <p>
 * The general command for creating and starting a microservice from a main method is as follows:
 * <p class='bjava'>
 * 	<jk>public static void</jk> main(String[] <jv>args</jv>) {
 * 		Microservice.<jsm>create</jsm>().args(<jv>args</jv>).build().start().join();
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
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#juneau-microservice-core">juneau-microservice-core</a>
 * </ul>
 */
public class Microservice implements ConfigEventListener {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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

	/**
	 * Creates a new builder for this object.
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
	public static class Builder {

		Args args;
		ManifestFile manifest;
		Logger logger;
		LogConfig logConfig;
		Config config;
		String configName;
		ConfigStore configStore;
		Config.Builder configBuilder = Config.create();
		Boolean consoleEnabled;
		List<ConsoleCommand> consoleCommands = list();
		VarResolver.Builder varResolver = VarResolver.create().defaultVars().vars(ConfigVar.class);
		Scanner consoleReader;
		PrintWriter consoleWriter;
		MicroserviceListener listener;
		File workingDir = System.getProperty("juneau.workingDir") == null ? null : new File(System.getProperty("juneau.workingDir"));

		/**
		 * Constructor.
		 */
		protected Builder() {}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy.
		 */
		protected Builder(Builder copyFrom) {
			this.args = copyFrom.args;
			this.manifest = copyFrom.manifest;
			this.logger = copyFrom.logger;
			this.configName = copyFrom.configName;
			this.logConfig = copyFrom.logConfig == null ? null : copyFrom.logConfig.copy();
			this.consoleEnabled = copyFrom.consoleEnabled;
			this.configBuilder = copyFrom.configBuilder;
			this.varResolver = copyFrom.varResolver;
			this.consoleReader = copyFrom.consoleReader;
			this.consoleWriter = copyFrom.consoleWriter;
			this.workingDir = copyFrom.workingDir;
		}

		/**
		 * Creates a copy of this builder.
		 *
		 * @return A new copy of this builder.
		 */
		public Builder copy() {
			return new Builder(this);
		}

		/**
		 * Instantiate a new microservice using the settings defined on this builder.
		 *
		 * @return A new microservice.
		 * @throws Exception Error occurred.
		 */
		public Microservice build() throws Exception {
			return new Microservice(this);
		}

		/**
		 * Specifies the command-line arguments passed into the Java command.
		 *
		 * <p>
		 * This is required if you use {@link Microservice#getArgs()} or <c>$A</c> string variables.
		 *
		 * @param args
		 * 	The command-line arguments passed into the Java command as a pre-parsed {@link Args} object.
		 * @return This object.
		 */
		public Builder args(Args args) {
			this.args = args;
			return this;
		}

		/**
		 * Specifies the command-line arguments passed into the Java command.
		 *
		 * <p>
		 * This is required if you use {@link Microservice#getArgs()} or <c>$A</c> string variables.
		 *
		 * @param args
		 * 	The command-line arguments passed into the Java command as the raw command-line arguments.
		 * @return This object.
		 */
		public Builder args(String...args) {
			this.args = new Args(args);
			return this;
		}

		/**
		 * Specifies the manifest file of the jar file this microservice is contained within.
		 *
		 * <p>
		 * This is required if you use {@link Microservice#getManifest()}.
		 * It's also used to locate initialization values such as <c>Main-Config</c>.
		 *
		 * <p>
		 * If you do not specify the manifest file, we attempt to resolve it through the following methods:
		 * <ol class='spaced-list'>
		 * 	<li>
		 * 		Looking on the file system for a file at <js>"META-INF/MANIFEST.MF"</js>.
		 * 		This is primarily to allow for running microservices from within eclipse workspaces where the manifest file
		 * 		is located in the project root.
		 * 	<li>
		 * 		Using the class loader for this class to find the file at the URL <js>"META-INF/MANIFEST.MF"</js>.
		 * </ol>
		 *
		 * @param value
		 * 	The manifest file of this microservice.
		 * 	<br>Can be any of the following types:
		 * 	<ul>
		 * 		<li>{@link ManifestFile}
		 * 		<li>{@link Manifest}
		 * 		<li>{@link Reader} - Containing the raw contents of the manifest.  Note that the input must end with a newline.
		 * 		<li>{@link InputStream} - Containing the raw contents of the manifest.  Note that the input must end with a newline.
		 * 		<li>{@link File} - File containing the raw contents of the manifest.
		 * 		<li>{@link String} - Path to file containing the raw contents of the manifest.
		 * 		<li>{@link Class} - Finds and loads the manifest file of the jar file that the specified class is contained within.
		 * 	</ul>
		 * @return This object.
		 * @throws IOException Thrown by underlying stream.
		 */
		public Builder manifest(Object value) throws IOException {
			if (value == null)
				this.manifest = null;
			else if (value instanceof ManifestFile)
				this.manifest = (ManifestFile)value;
			else if (value instanceof Manifest)
				this.manifest = new ManifestFile((Manifest)value);
			else if (value instanceof Reader)
				this.manifest = new ManifestFile((Reader)value);
			else if (value instanceof InputStream)
				this.manifest = new ManifestFile((InputStream)value);
			else if (value instanceof File)
				this.manifest = new ManifestFile((File)value);
			else if (value instanceof String)
				this.manifest = new ManifestFile(resolveFile((String)value));
			else if (value instanceof Class)
				this.manifest = new ManifestFile((Class<?>)value);
			else
				throw new BasicRuntimeException("Invalid type passed to Builder.manifest(Object).  Type=[{0}]", className(value));

			return this;
		}

		/**
		 * Specifies the logger used by the microservice and returned by the {@link Microservice#getLogger()} method.
		 *
		 * <p>
		 * Calling this method overrides the default logging mechanism controlled by the {@link #logConfig(LogConfig)} method.
		 *
		 * @param logger The logger to use for logging microservice messages.
		 * @return This object.
		 */
		public Builder logger(Logger logger) {
			this.logger = logger;
			return this;
		}

		/**
		 * Specifies logging instructions for the microservice.
		 *
		 * <p>
		 * If not specified, the values are taken from the <js>"Logging"</js> section of the configuration.
		 *
		 * <p>
		 * This method is ignored if {@link #logger(Logger)} is used to set the microservice logger.
		 *
		 * @param logConfig The log configuration.
		 * @return This object.
		 */
		public Builder logConfig(LogConfig logConfig) {
			this.logConfig = logConfig;
			return this;
		}

		/**
		 * Specifies the config for initializing this microservice.
		 *
		 * <p>
		 * Calling this method overrides the default configuration controlled by the {@link #configName(String)} and {@link #configStore(ConfigStore)} methods.
		 *
		 * @param config The configuration.
		 * @return This object.
		 */
		public Builder config(Config config) {
			this.config = config;
			return this;
		}

		/**
		 * Specifies the config name for initializing this microservice.
		 *
		 * <p>
		 * If you do not specify the config file location, we attempt to resolve it through the following methods:
		 * <ol class='spaced-list'>
		 * 	<li>
		 * 		Resolve file first in working directory, then in classpath, using the following names:
		 * 		<ul>
		 * 			<li>
		 * 				The <js>"configFile"</js> argument in the command line arguments passed in through the constructor.
		 * 			<li>
		 * 				The value of the <c>Main-Config</c> entry in the manifest file.
		 * 			<li>
		 * 				A config file in the same location and with the same name as the executable jar file.
		 * 				(e.g. <js>"java -jar myjar.jar"</js> will look for <js>"myjar.cfg"</js>).
		 * 		</ul>
		 * 	<li>
		 * 		Resolve any <js>"*.cfg"</js> file that can be found in the working directory.
		 * 	<li>
		 * 		Resolve any of the following files in the classpath:  <js>"juneau.cfg"</js>, <js>"system.cfg"</js>
		 * </ol>
		 *
		 * <p>
		 * If no configuration file is found, and empty in-memory configuration is used.
		 *
		 * @param configName The configuration name.
		 * @return This object.
		 */
		public Builder configName(String configName) {
			this.configName = configName;
			return this;
		}

		/**
		 * Specifies the config store to use for storing and retrieving configurations.
		 *
		 * <p>
		 * By default, we use a {@link FileStore} store for configuration files.
		 *
		 * @param configStore The configuration name.
		 * @return This object.
		 */
		public Builder configStore(ConfigStore configStore) {
			this.configStore = configStore;
			return this;
		}

		/**
		 * Specifies that the Java console is enabled for this microservice.
		 *
		 * <p>
		 * If not specified, this value is taken from the <js>"Console/enabled"</js> configuration setting.
		 * If not specified in the configuration, defaults to <jk>false</jk>.
		 *
		 * @param consoleEnabled <jk>true</jk> if the Java console is enabled for this microservice.
		 * @return This object.
		 */
		public Builder consoleEnabled(boolean consoleEnabled) {
			this.consoleEnabled = consoleEnabled;
			return this;
		}

		/**
		 * Specifies console commands to make available on the Java console.
		 *
		 * <p>
		 * Note that these are ignored if the console is not enabled via {@link #consoleEnabled(boolean)}.
		 *
		 * <p>
		 * This list augments the commands defined via the <js>"Console/commands"</js> configuration setting.
		 *
		 * <p>
		 * This method can only be used on console commands with no-arg constructors.
		 *
		 * @param consoleCommands The list of console commands to append to the list of available commands.
		 * @return This object.
		 * @throws ExecutableException Exception occurred on invoked constructor/method/field.
		 */
		@SuppressWarnings("unchecked")
		public Builder consoleCommands(Class<? extends ConsoleCommand>...consoleCommands) throws ExecutableException {
			try {
				for (Class<? extends ConsoleCommand> cc : consoleCommands)
					this.consoleCommands.add(cc.getDeclaredConstructor().newInstance());
			} catch (Exception e) {
				throw new ExecutableException(e);
			}
			return this;
		}

		/**
		 * Specifies console commands to make available on the Java console.
		 *
		 * <p>
		 * Note that these are ignored if the console is not enabled via {@link #consoleEnabled(boolean)}.
		 *
		 * <p>
		 * This list augments the commands defined via the <js>"Console/commands"</js> configuration setting.
		 *
		 * @param consoleCommands The list of console commands to append to the list of available commands.
		 * @return This object.
		 */
		public Builder consoleCommands(ConsoleCommand...consoleCommands) {
			addAll(this.consoleCommands, consoleCommands);
			return this;
		}

		/**
		 * Specifies the console input and output.
		 *
		 * <p>
		 * If not specified, uses the console returned by {@link System#console()}.
		 * If that is not available, uses {@link System#in} and {@link System#out}.
		 *
		 * <p>
		 * Note that these are ignored if the console is not enabled via {@link #consoleEnabled(boolean)}.
		 *
		 * @param consoleReader The console input.
		 * @param consoleWriter The console output.
		 * @return This object.
		 */
		public Builder console(Scanner consoleReader, PrintWriter consoleWriter) {
			this.consoleReader = consoleReader;
			this.consoleWriter = consoleWriter;
			return this;
		}

		/**
		 * Augments the set of variables defined in the configuration and var resolver.
		 *
		 * <p>
		 * This calls {@link org.apache.juneau.svl.VarResolver.Builder#vars(Class[])} on the var resolver used to construct the configuration
		 * object returned by {@link Microservice#getConfig()} and the var resolver returned by {@link Microservice#getVarResolver()}.
		 *
		 * @param vars The set of variables to append to the var resolver builder.
		 * @return This object.
		 */
		@SuppressWarnings("unchecked")
		public Builder vars(Class<? extends Var>...vars) {
			varResolver.vars(vars);
			return this;
		}

		/**
		 * Adds a bean for vars defined in the var resolver.
		 *
		 * <p>
		 * This calls {@link org.apache.juneau.svl.VarResolver.Builder#bean(Class,Object)} on the var resolver used to construct the configuration
		 * object returned by {@link Microservice#getConfig()} and the var resolver returned by {@link Microservice#getVarResolver()}.
		 *
		 * @param c The bean type.
		 * @param value The bean.
		 * @param <T> The bean type.
		 * @return This object.
		 */
		public <T> Builder varBean(Class<T> c, T value) {
			varResolver.bean(c, value);
			return this;
		}

		/**
		 * Specifies the directory to use to resolve the config file and other paths defined with the config file.
		 *
		 * @param workingDir The working directory, or <jk>null</jk> to use the underlying working directory.
		 * @return This object.
		 */
		public Builder workingDir(File workingDir) {
			this.workingDir = workingDir;
			return this;
		}

		/**
		 * Specifies the directory to use to resolve the config file and other paths defined with the config file.
		 *
		 * @param workingDir The working directory, or <jk>null</jk> to use the underlying working directory.
		 * @return This object.
		 */
		public Builder workingDir(String workingDir) {
			this.workingDir = new File(workingDir);
			return this;
		}

		/**
		 * Registers an event listener for this microservice.
		 *
		 * @param listener An event listener for this microservice.
		 * @return This object.
		 */
		public Builder listener(MicroserviceListener listener) {
			this.listener = listener;
			return this;
		}

		/**
		 * Resolves the specified path.
		 *
		 * <p>
		 * If the working directory has been explicitly specified, relative paths are resolved relative to that.
		 *
		 * @param path The path to resolve.
		 * @return The resolved file.
		 */
		protected File resolveFile(String path) {
			if (Paths.get(path).isAbsolute())
				return new File(path);
			if (workingDir != null)
				return new File(workingDir, path);
			return new File(path);
		}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Instance
	//-----------------------------------------------------------------------------------------------------------------

	final Messages messages = Messages.of(Microservice.class);

	private final Builder builder;
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

	private volatile Logger logger;

	/**
	 * Constructor.
	 *
	 * @param builder The builder containing the settings for this microservice.
	 * @throws IOException Problem occurred reading file.
	 * @throws ParseException Malformed input encountered.
	 */
	@SuppressWarnings("resource")
	protected Microservice(Builder builder) throws IOException, ParseException {
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
					throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n"+read(f), e);
				}
			} else {
				// Otherwise, read from manifest file in the jar file containing the main class.
				URL url = getClass().getResource("META-INF/MANIFEST.MF");
				if (url != null) {
					try {
						m.read(url.openStream());
					} catch (IOException e) {
						throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n"+read(url.openStream()), e);
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
		Config.Builder configBuilder = builder.configBuilder.varResolver(builder.varResolver.build()).store(MemoryStore.DEFAULT);
		if (config == null) {
			ConfigStore store = builder.configStore;
			FileStore cfs = workingDir == null ? FileStore.DEFAULT : FileStore.create().directory(workingDir).build();
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
					 if (ClasspathStore.DEFAULT.exists(name)) {
						 configBuilder.store(ClasspathStore.DEFAULT).name(name);
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
		this.varResolver = builder.varResolver.bean(Config.class, config).build();

		// --------------------------------------------------------------------------------
		// Initialize console commands.
		// --------------------------------------------------------------------------------
		this.consoleEnabled = ObjectUtils.firstNonNull(builder.consoleEnabled, config.get("Console/enabled").asBoolean().orElse(false));
		if (consoleEnabled) {
			Console c = System.console();
			this.consoleReader = ObjectUtils.firstNonNull(builder.consoleReader, new Scanner(c == null ? new InputStreamReader(System.in) : c.reader()));
			this.consoleWriter = ObjectUtils.firstNonNull(builder.consoleWriter, c == null ? new PrintWriter(System.out, true) : c.writer());

			for (ConsoleCommand cc : builder.consoleCommands) {
				consoleCommandMap.put(cc.getName(), cc);
			}
			for (String s : config.get("Console/commands").asStringArray().orElse(new String[0])) {
				ConsoleCommand cc;
				try {
					cc = (ConsoleCommand)Class.forName(s).getDeclaredConstructor().newInstance();
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
		// Other
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
	 * @return This object.
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
				System.setProperty(key, config.get("SystemProperties/"+key).orElse(null));

		// --------------------------------------------------------------------------------
		// Initialize logging.
		// --------------------------------------------------------------------------------
		this.logger = builder.logger;
		LogConfig logConfig = builder.logConfig != null ? builder.logConfig : new LogConfig();
		if (this.logger == null) {
			LogManager.getLogManager().reset();
			this.logger = Logger.getLogger("");
			String logFile = firstNonNull(logConfig.logFile, config.get("Logging/logFile").orElse(null));

			if (isNotEmpty(logFile)) {
				String logDir = firstNonNull(logConfig.logDir, config.get("Logging/logDir").orElse("."));
				File logDirFile = resolveFile(logDir);
				mkdirs(logDirFile, false);
				logDir = logDirFile.getAbsolutePath();
				System.setProperty("juneau.logDir", logDir);

				boolean append = firstNonNull(logConfig.append, config.get("Logging/append").asBoolean().orElse(false));
				int limit = firstNonNull(logConfig.limit, config.get("Logging/limit").asInteger().orElse(1024*1024));
				int count = firstNonNull(logConfig.count, config.get("Logging/count").asInteger().orElse(1));

				FileHandler fh = new FileHandler(logDir + '/' + logFile, limit, count, append);

				Formatter f = logConfig.formatter;
				if (f == null) {
					String format = config.get("Logging/format").orElse("[{date} {level}] {msg}%n");
					String dateFormat = config.get("Logging/dateFormat").orElse("yyyy.MM.dd hh:mm:ss");
					boolean useStackTraceHashes = config.get("Logging/useStackTraceHashes").asBoolean().orElse(false);
					f = new LogEntryFormatter(format, dateFormat, useStackTraceHashes);
				}
				fh.setFormatter(f);
				fh.setLevel(firstNonNull(logConfig.fileLevel, config.get("Logging/fileLevel").as(Level.class).orElse(Level.INFO)));
				logger.addHandler(fh);

				ConsoleHandler ch = new ConsoleHandler();
				ch.setLevel(firstNonNull(logConfig.consoleLevel, config.get("Logging/consoleLevel").as(Level.class).orElse(Level.WARNING)));
				ch.setFormatter(f);
				logger.addHandler(ch);
			}
		}

		JsonMap loggerLevels = config.get("Logging/levels").as(JsonMap.class).orElseGet(JsonMap::new);
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * 	<li class='jm'>{@link Builder#configStore(ConfigStore)}
	 * 	<li class='jm'>{@link Builder#configName(String)}
	 * </ul>
	 *
	 * <p>
	 * String variables are automatically resolved using the variable resolver returned by {@link #getVarResolver()}.
	 *
	 * <p>
	 * This method can be called from the class constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bini'>
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
	 * <p class='bjava'>
	 * 	<jc>// Java code for accessing config entries above.</jc>
	 * 	Config <jv>config</jv> = getConfig();
	 *
	 * 	<jk>int</jk> <jv>anInt</jv> = <jv>config</jv>.get(<js>"MySection/anInt"</js>).asInteger().orElse(-1);
	 * 	<jk>boolean</jk> <jv>aBoolean</jv> = <jv>config</jv>.get(<js>"MySection/aBoolean"</js>).asBoolean().orElse(<jk>false</jk>);
	 * 	<jk>int</jk>[] <jv>anIntArray</jv> = <jv>config</jv>.get(<js>"MySection/anIntArray"</js>).as(<jk>int</jk>[].<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	URL <jv>aURL</jv> = <jv>config</jv>.get(<js>"MySection/aURL"</js>).as(URL.<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	MyBean <jv>aBean</jv> = <jv>config</jv>.get(<js>"MySection/aBean"</js>).as(MyBean.<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	Locale <jv>locale</jv> = <jv>config</jv>.get(<js>"MySection/locale"</js>).as(Locale.<jk>class</jk>).orElse(<jk>null</jk>);
	 * 	String <jv>path</jv> = <jv>config</jv>.get(<js>"MySection/path"</js>).orElse(<jk>null</jk>);
	 * 	String <jv>mainClass</jv> = <jv>config</jv>.get(<js>"MySection/mainClass"</js>).orElse(<jk>null</jk>);
	 * 	<jk>int</jk> <jv>sameAsAnInt</jv> = <jv>config</jv>.get(<js>"MySection/sameAsAnInt"</js>).asInteger().orElse(<jk>null</jk>);
	 * 	String <jv>myArg</jv> = <jv>config</jv>.getString(<js>"MySection/myArg"</js>);
	 * 	String <jv>firstArg</jv> = <jv>config</jv>.getString(<js>"MySection/firstArg"</js>);
	 * </p>
	 *
	 * @return The config file for this application, or <jk>null</jk> if no config file is configured.
	 */
	public Config getConfig() {
		return config;
	}

	/**
	 * Returns the main jar manifest file contents as a simple {@link JsonMap}.
	 *
	 * <p>
	 * This map consists of the contents of {@link Manifest#getMainAttributes()} with the keys and entries converted to
	 * simple strings.
	 * <p>
	 * This method can be called from the class constructor.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Get Main-Class from manifest file.</jc>
	 * 	String <jv>mainClass</jv> = Microservice.<jsm>getManifest</jsm>().getString(<js>"Main-Class"</js>, <js>"unknown"</js>);
	 *
	 * 	<jc>// Get Rest-Resources from manifest file.</jc>
	 * 	String[] <jv>restResources</jv> = Microservice.<jsm>getManifest</jsm>().getStringArray(<js>"Rest-Resources"</js>);
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
	 * 	<li class='jm'>{@link Builder#vars(Class...)}
	 * 	<li class='jm'>{@link Builder#varBean(Class,Object)}
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
		List<String> l = list();
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
	 * @return This object.
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
	 * @return This object.
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
	// Other methods
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
	public void out(Messages mb, String messageKey, Object...args) {
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
	public void err(Messages mb, String messageKey, Object...args) {
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
