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
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.jar.*;
import java.util.logging.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.microservice.console.*;
import org.apache.juneau.microservice.resources.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.svl.vars.ManifestFileVar;
import org.apache.juneau.utils.*;

/**
 * Parent class for all microservices.
 * 
 * <p>
 * A microservice defines a simple API for starting and stopping simple Java services contained in executable jars.
 * 
 * <p>
 * The general command for invoking these services is...
 * <p class='bcode'>
 * 	java -jar mymicroservice.jar [mymicroservice.cfg]
 * </p>
 * 
 * <p>
 * Your microservice class must be specified as the <jk>Main-Class</jk> entry in the manifest file of your microservice 
 * jar file.
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
 * <h5 class='topic'>Entry point Method</h5>
 * 
 * Subclasses must implement a static void main method as the entry point for the microservice.
 * Typically, this method will simply consist of the following...
 * <p class='bcode'>
 * 	<jk>public static void</jk> main(String[] args) <jk>throws</jk> Exception {
 * 		<jk>new</jk> MyMicroservice(args).start();
 * 	}
 * </p>
 * 
 * <h5 class='topic'>Lifecycle Methods</h5>
 * 
 * Subclasses must implement the following lifecycle methods:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #start()} - Gets executed during startup.
 * 	<li>
 * 		{@link #stop()} - Gets executed when 'exit' is typed in the console or an external shutdown signal is received.
 * 	<li>
 * 		{@link #kill()} - Can be used to forcibly shut down the service.  Doesn't get called during normal operation.
 * </ul>
 * 
 * <h5 class='topic'>Lifecycle Listener Methods</h5>
 * 
 * Subclasses can optionally implement the following event listener methods:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #onStart()} - Gets executed before {@link #start()}.
 * 	<li>
 * 		{@link #onStop()} - Gets executed before {@link #stop()}.
 * 	<li>
 * 		{@link #onConfigChange(List)} - Gets executed after a config file has been modified.
 * </ul>
 * 
 * <h5 class='topic'>Other Methods</h5>
 * 
 * Subclasses can optionally override the following methods to provide customized behavior:
 * <ul class='spaced-list'>
 * 	<li>
 * 		{@link #createVarResolver()} - Creates the {@link VarResolver} used to resolve variables in the config file 
 * 		returned by {@link #getConfig()}.
 * </ul>
 */
public abstract class Microservice implements ConfigEventListener {

	private static volatile Microservice INSTANCE;

	private final MessageBundle mb = MessageBundle.create(Microservice.class, "Messages");
	private final Scanner consoleReader;
	private final PrintWriter consoleWriter;

	private Logger logger;
	private Args args;
	private Config cf;
	private ManifestFile mf;
	private VarResolver vr;
	private Map<String,ConsoleCommand> consoleCommands;
	private boolean consoleEnabled = true;
	
	private String cfPath;

	/**
	 * Returns the Microservice instance.  
	 * <p>
	 * This method only works if there's only one Microservice instance in a JVM.  
	 * Otherwise, it's just overwritten by the last call to {@link #Microservice(String...)}.
	 * 
	 * @return The Microservice instance, or <jk>null</jk> if there isn't one.
	 */
	public static Microservice getInstance() {
		synchronized(Microservice.class) {
			return INSTANCE;
		}
	}
	
	/**
	 * Constructor.
	 * 
	 * @param args Command line arguments.
	 * @throws Exception
	 */
	protected Microservice(String...args) throws Exception {
		setInstance(this);
		Console c = System.console();
		consoleReader = new Scanner(c == null ? new InputStreamReader(System.in) : c.reader());
		consoleWriter = c == null ? new PrintWriter(System.out, true) : c.writer();
		setArgs(new Args(args));
		setManifest(this.getClass());
	}
	
	private static void setInstance(Microservice m) {
		synchronized(Microservice.class) {
			INSTANCE = m;
		}
	}


	/**
	 * Specifies the path of the config file for this microservice.
	 * 
	 * <p>
	 * If you do not specify the config file location, we attempt to resolve it through the following methods:
	 * <ol>
	 * 	<li>The first argument in the command line arguments passed in through the constructor.
	 * 	<li>The value of the <code>Main-Config</code> entry in the manifest file.
	 * 	<li>A config file in the same location and with the same name as the executable jar file.
	 * 		(e.g. <js>"java -jar myjar.jar"</js> will look for <js>"myjar.cfg"</js>).
	 * </ol>
	 * 
	 * <p>
	 * If this path does not exist, a {@link FileNotFoundException} will be thrown from the {@link #start()} command.
	 * 
	 * @param cfPath The absolute or relative path of the config file.
	 * @param create Create the file if it doesn't exist.
	 * @return This object (for method chaining).
	 * @throws IOException If config file does not exist at the specified location or could not be read or created.
	 */
	public Microservice setConfig(String cfPath, boolean create) throws IOException {
		File f = new File(cfPath);
		if (! f.exists()) {
			if (! create)
				throw new FileNotFoundException("Could not locate config at '"+f.getAbsolutePath()+"'.");
			if (! f.createNewFile())
				throw new FileNotFoundException("Could not create config at '"+f.getAbsolutePath()+"'.");
		}
		this.cfPath = cfPath;
		return this;
	}

	/**
	 * Specifies the config for this microservice.
	 * 
	 * <p>
	 * Note that if you use this method instead of {@link #setConfig(String,boolean)}, the config file will not use
	 * the variable resolver constructed from {@link #createVarResolver()}.
	 * 
	 * @param cf The config file for this application, or <jk>null</jk> if no config file is needed.
	 */
	public void setConfig(Config cf) {
		this.cf = cf;
	}

	/**
	 * Specifies the manifest file of the jar file this microservice is contained within.
	 * 
	 * <p>
	 * If you do not specify the manifest file, we attempt to resolve it through the following methods:
	 * <ol>
	 * 	<li>Looking on the file system for a file at <js>"META-INF/MANIFEST.MF"</js>.
	 * 		This is primarily to allow for running microservices from within eclipse workspaces where the manifest file
	 * 		is located in the project root.
	 * 	<li>Using the class loader for this class to find the file at the URL <js>"META-INF/MANIFEST.MF"</js>.
	 * </ol>
	 * 
	 * @param mf The manifest file of this microservice.
	 * @return This object (for method chaining).
	 */
	public Microservice setManifest(ManifestFile mf) {
		this.mf = mf;
		ManifestFileVar.init(this.mf);
		return this;
	}

	/**
	 * Shortcut for calling <code>setManifest(<jk>new</jk> ManifestFile(mf))</code>.
	 * 
	 * @param mf The manifest file of this microservice.
	 * @return This object (for method chaining).
	 */
	public Microservice setManifest(Manifest mf) {
		return setManifest(new ManifestFile(mf));
	}

	/**
	 * Convenience method for specifying the manifest contents directly.
	 * 
	 * @param contents The lines in the manifest file.
	 * @return This object (for method chaining).
	 * @throws IOException
	 */
	public Microservice setManifestContents(String...contents) throws IOException {
		String s = StringUtils.join(contents, "\n") + "\n";
		return setManifest(new ManifestFile(new Manifest(new ByteArrayInputStream(s.getBytes("UTF-8")))));
	}

	/**
	 * Same as {@link #setManifest(Manifest)} except specified through a {@link File} object.
	 * 
	 * @param f The manifest file of this microservice.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public Microservice setManifest(File f) throws IOException {
		return setManifest(new ManifestFile(f));
	}

	/**
	 * Same as {@link #setManifest(Manifest)} except finds and loads the manifest file of the jar file that the  
	 * specified class is contained within.
	 * 
	 * @param c The class whose jar file contains the manifest to use for this microservice.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public Microservice setManifest(Class<?> c) throws IOException {
		return setManifest(new ManifestFile(c));
	}

	/**
	 * Creates the {@link VarResolver} used to resolve variables in the config file returned by {@link #getConfig()}.
	 * 
	 * <p>
	 * The default implementation resolves the following variables:
	 * <ul class='doctree'>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.SystemPropertiesVar} - <code>$S{key[,default]}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.EnvVariablesVar} - <code>$E{key[,default]}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.ArgsVar} - <code>$A{key[,default]}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.ManifestFileVar} - <code>$MF{key[,default]}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.IfVar} - <code>$IF{arg,then[,else]}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.SwitchVar} - <code>$SW{arg,pattern1:then1[,pattern2:then2...]}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.CoalesceVar} - <code>$CO{arg1[,arg2...]}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.PatternMatchVar} - <code>$PM{arg,pattern}</code> 
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.NotEmptyVar} - <code>$NE{arg}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.UpperCaseVar} - <code>$UC{arg}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.svl.vars.LowerCaseVar} - <code>$LC{arg}</code>
	 * 	<li class='jc'>{@link org.apache.juneau.config.vars.ConfigVar} - <code>$C{key[,default]}</code>
	 * </ul>
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own variables.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
	 * 	<jd>/**
	 * 	 * Augment default var resolver with a custom $B{...} variable that simply wraps strings inside square brackets.
	 * 	 * /</jd>
	 * 	<ja>@Override</ja> <jc>// Microservice</jc>
	 * 	<jk>protected</jk> StringVarResolver createVarResolver() {
	 * 		<jk>return super</jk>.createVarResolver()
	 * 			.addVar(<js>"B"</js>,
	 * 				<jk>new</jk> StringVarWithDefault() {
	 * 					<ja>@Override</ja> <jc>// StringVar</jc>
	 * 					<jk>public</jk> String resolve(String varVal) {
	 * 						<jk>return</jk> <js>'['</js> + varVal + <js>']'</js>;
	 * 					}
	 * 				}
	 * 			);
	 * 	}
	 * </p>
	 * <p class='bcode'>
	 * 	<cc># Example config file</cc>
	 * 	<cs>[MySection]</cs>
	 * 	<ck>myEntry</ck> = $B{foo}
	 * </p>
	 * <p class='bcode'>
	 * 	<jc>// Example java code</jc>
	 * 	String myentry = getConfig().getString(<js>"MySection/myEntry"</js>); <jc>// == "[foo]"</js>
	 * </p>
	 * 
	 * @return A new {@link VarResolver}.
	 */
	protected VarResolverBuilder createVarResolver() {
		VarResolverBuilder b = new VarResolverBuilder()
			.defaultVars()
			.vars(ConfigVar.class, SwitchVar.class, IfVar.class);
		if (cf != null)
			b.contextObject(ConfigVar.SESSION_config, cf);
		return b;
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
	 * Sets the arguments for this microservice.
	 * 
	 * @param args The arguments for this microservice.
	 * @return This object (for method chaining).
	 */
	public Microservice setArgs(Args args) {
		this.args = args;
		ArgsVar.init(args);
		return this;
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
	 * 		The <code>Main-Config</code> entry in the microservice jar manifest file.
	 * 	<li>
	 * 		The name of the microservice jar with a <js>".cfg"</js> suffix (e.g. 
	 * 		<js>"mymicroservice.jar"</js>-&gt;<js>"mymicroservice.cfg"</js>).
	 * </ol>
	 * 
	 * <p>
	 * If all methods for locating the config fail, then this method returns <jk>null</jk>.
	 * 
	 * <p>
	 * Subclasses can set their own config file by calling the {@link #setConfig(Config)} method.
	 * 
	 * <p>
	 * String variables defined by {@link #createVarResolver()} are automatically resolved when using this method.
	 * 
	 * <p>
	 * This method can be called from the class constructor.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
	 * <p class='bcode'>
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
		return cf;
	}

	/**
	 * Returns the main jar manifest file contents as a simple {@link ObjectMap}.
	 * 
	 * <p>
	 * This map consists of the contents of {@link Manifest#getMainAttributes()} with the keys and entries converted to 
	 * simple strings.
	 * <p>
	 * This method can be called from the class constructor.
	 * 
	 * <h5 class='section'>Example:</h5>
	 * <p class='bcode'>
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
		return mf;
	}

	/**
	 * Returns the variable resolver for resolving variables in strings and files.
	 * <p>
	 * See the {@link #createVarResolver()} method for the list of available resolution variables.
	 * 
	 * @return The VarResolver used by this Microservice, or <jk>null</jk> if it was never created.
	 */
	public VarResolver getVarResolver() {
		return vr;
	}

	/**
	 * Returns the logger for this microservice.
	 * 
	 * @return The logger for this microservice.
	 */
	public Logger getLogger() {
		return logger;
	}
	
	//--------------------------------------------------------------------------------
	// Abstract lifecycle methods.
	//--------------------------------------------------------------------------------

	/**
	 * Start this application.
	 * 
	 * <p>
	 * Default implementation simply calls {@link #onStart()}.
	 * 
	 * <p>
	 * Overridden methods MUST call this method FIRST so that the {@link #onStart()} method is called.
	 * 
	 * @return This object (for method chaining).
	 * @throws Exception
	 */
	@SuppressWarnings("resource")
	public Microservice start() throws Exception {

		// --------------------------------------------------------------------------------
		// Try to get the manifest file if it wasn't already set.
		// --------------------------------------------------------------------------------
		if (mf == null) {
			Manifest m = new Manifest();

			// If running within an eclipse workspace, need to get it from the file system.
			File f = new File("META-INF/MANIFEST.MF");
			if (f.exists()) {
				try (FileInputStream fis = new FileInputStream(f)) {
					m.read(fis);
				} catch (IOException e) {
					throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n " + read(f), e);
				}
			} else {
				// Otherwise, read from manifest file in the jar file containing the main class.
				URLClassLoader cl = (URLClassLoader)getClass().getClassLoader();
				URL url = cl.findResource("META-INF/MANIFEST.MF");
				if (url != null) {
					try {
						m.read(url.openStream());
					} catch (IOException e) {
						throw new IOException("Problem detected in MANIFEST.MF.  Contents below:\n " + read(url.openStream()), e);
					}
				}
			}
			mf = new ManifestFile(m);
		}

		// --------------------------------------------------------------------------------
		// Resolve the config file if the path was specified.
		// --------------------------------------------------------------------------------
		ConfigBuilder cfb = Config.create();
		if (cfPath != null)
			cf = cfb.name(cfPath).varResolver(createVarResolver().defaultVars().build()).build();

		
		// --------------------------------------------------------------------------------
		// Find config file.
		// Can either be passed in as first parameter, or we discover it using
		// the 'sun.java.command' system property.
		// --------------------------------------------------------------------------------
		if (cf == null) {
			if (args.hasArg(0))
				cfPath = args.getArg(0);
			else if (mf.containsKey("Main-Config"))
				cfPath = mf.getString("Main-Config");
			else {
				String cmd = System.getProperty("sun.java.command", "not_found").split("\\s+")[0];
				if (cmd.endsWith(".jar"))
					cfPath = cmd.replace(".jar", ".cfg");
			}

			if (cfPath == null) {
				cf = cfb.build();
			} else {
				cf = cfb.name(cfPath).varResolver(createVarResolver().build()).build();
			}
		}

		vr = createVarResolver().build();
		
		if (cfPath != null)
			System.setProperty("juneau.configFile", cfPath);
		
		// --------------------------------------------------------------------------------
		// Set system properties.
		// --------------------------------------------------------------------------------
		Set<String> spKeys = cf.getKeys("SystemProperties");
		if (spKeys != null)
			for (String key : spKeys)
				System.setProperty(key, cf.getString("SystemProperties/"+key));

		// --------------------------------------------------------------------------------
		// Initialize logging.
		// --------------------------------------------------------------------------------
		try {
			initLogging();
		} catch (Exception e) {
			// If logging can be initialized, just print a stack trace and continue.
			e.printStackTrace();
		}

		// --------------------------------------------------------------------------------
		// Add a config file change listener.
		// --------------------------------------------------------------------------------
		cf.addListener(this);

		consoleEnabled = cf.getBoolean("Console/enabled", true);

		if (cfPath == null) {
			err(mb, "RunningClassWithoutConfig", getClass().getSimpleName());
		} else {
			out(mb, "RunningClassWithConfig", getClass().getSimpleName(), cfPath);
		}
				
		Runtime.getRuntime().addShutdownHook(
			new Thread() {
				@Override /* Thread */
				public void run() {
					Microservice.this.stop();
				}
			}
		);
		onStart();
		return this;
	}

	/**
	 * Start the console for this application.
	 * 
	 * <p>
	 * Note that this is typically started after all initialization has occurred so that the console output isn't polluted.
	 * 
	 * @return This object (for method chaining).
	 * @throws Exception
	 */
	protected Microservice startConsole() throws Exception {
		consoleCommands = new LinkedHashMap<>();
		for (ConsoleCommand cc : createConsoleCommands())
			consoleCommands.put(cc.getName(), cc);
		consoleCommands = unmodifiableMap(consoleCommands);
		
		final Map<String,ConsoleCommand> commands = consoleCommands;
		final MessageBundle mb2 = mb;
		if (! consoleCommands.isEmpty()) {
			new Thread() {
				@Override /* Thread */
				@SuppressWarnings("resource")  // Must not close System.in!
				public void run() {
					Scanner in = getConsoleReader();
					PrintWriter out = getConsoleWriter();
					
					out.println(mb2.getString("ListOfAvailableCommands"));
					for (ConsoleCommand cc : commands.values()) 
						out.append("\t").append(cc.getName()).append(" -- ").append(cc.getInfo()).println();
					out.println();
					
					while (true) {
						String line = null;
						out.append("> ").flush();
						line = in.nextLine();
						Args args = new Args(line);
						if (! args.isEmpty()) {
							ConsoleCommand cc = commands.get(args.getArg(0));
							if (cc == null) {
								out.println(mb2.getString("UnknownCommand"));
							} else {
								try {
									if (cc.execute(in, out, args))
										break;
								} catch (Exception e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}.start();
		}
		return this;
	}
	
	/**
	 * Initialize the logging for this microservice.
	 * 
	 * <p>
	 * Subclasses can override this method to provide customized logging.
	 * 
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
	 * 	# See Config.getInt(String,int) for details.
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
		Config cf = getConfig();
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
			fh.setLevel(cf.getObjectWithDefault("Logging/fileLevel", Level.INFO, Level.class));
			logger.addHandler(fh);

			ConsoleHandler ch = new ConsoleHandler();
			ch.setLevel(cf.getObjectWithDefault("Logging/consoleLevel", Level.WARNING, Level.class));
			ch.setFormatter(new LogEntryFormatter(format, dateFormat, false));
			logger.addHandler(ch);
		}
		ObjectMap loggerLevels = cf.getObject("Logging/levels", ObjectMap.class);
		if (loggerLevels != null)
			for (String l : loggerLevels.keySet())
				Logger.getLogger(l).setLevel(loggerLevels.get(l, Level.class));
	}

	/**
	 * Joins the application with the current thread.
	 * 
	 * <p>
	 * Default implementation is a no-op.
	 * 
	 * @return This object (for method chaining).
	 * @throws Exception
	 */
	public Microservice join() throws Exception {
		return this;
	}

	/**
	 * Stop this application.
	 * 
	 * <p>
	 * Default implementation simply calls {@link #onStop()}.
	 * 
	 * <p>
	 * Overridden methods MUST call this method LAST so that the {@link #onStop()} method is called.
	 * 
	 * @return This object (for method chaining).
	 */
	public Microservice stop() {
		onStop();
		return this;
	}

	/**
	 * Kill the JVM by calling <code>System.exit(2);</code>.
	 */
	public void kill() {
		// This triggers the shutdown hook.
		System.exit(2);
	}


	//--------------------------------------------------------------------------------
	// Lifecycle listener methods.
	// Subclasses can override these methods to run code on certain events.
	//--------------------------------------------------------------------------------

	/**
	 * Called at the beginning of the {@link #start()} call.
	 * 
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStart() {}

	/**
	 * Called at the end of the {@link #stop()} call.
	 * 
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStop() {}

	/**
	 * Called if one or more changes occur in the config file.
	 * 
	 * <p>
	 * Subclasses can override this method to listen for config file changes.
	 * 
	 * @param events The list of changes in the config file.
	 */
	@Override /* ConfigEventListener */
	public void onConfigChange(List<ConfigEvent> events) {}

	
	//--------------------------------------------------------------------------------
	// Other methods.
	//--------------------------------------------------------------------------------
	
	/**
	 * Returns the console commands associated with this microservice.
	 * 
	 * @return The console commands associated with this microservice as an unmodifiable map.
	 */
	public final Map<String,ConsoleCommand> getConsoleCommands() {
		return consoleCommands;
	}
	
 	/**
	 * Constructs the list of available console commands.
	 * 
	 * <p>
	 * By default, uses the <js>"Console/commands"</js> list in the config file.
	 * Subclasses can override this method and modify or augment this list to provide their own console commands.
	 * 
	 * <p>
	 * The order of the commands returned by this method is the order they will be listed 
	 * 
	 * @return A mutable list of console command instances.
	 * @throws Exception
	 */
	public List<ConsoleCommand> createConsoleCommands() throws Exception {
		ArrayList<ConsoleCommand> l = new ArrayList<>();
		for (String s : cf.getStringArray("Console/commands"))
			l.add((ConsoleCommand)Class.forName(s).newInstance());
		return l;
	}
	
	/**
	 * Returns the console reader.
	 * 
	 * <p>
	 * Subclasses can override this method to provide their own console input.
	 * 
	 * @return The console reader.  Never <jk>null</jk>.
	 */
	public Scanner getConsoleReader() {
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
	public PrintWriter getConsoleWriter() {
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
	protected void out(MessageBundle mb, String messageKey, Object...args) {
		if (consoleEnabled)
			getConsoleWriter().println(mb.getString(messageKey, args));
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
	protected void err(MessageBundle mb, String messageKey, Object...args) {
		if (consoleEnabled)
			System.err.println(mb.getString(messageKey, args));  // NOT DEBUG
	}
}
