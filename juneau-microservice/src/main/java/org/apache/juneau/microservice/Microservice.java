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
import java.util.jar.*;

import org.apache.juneau.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.svl.vars.*;
import org.apache.juneau.utils.*;

/**
 * Parent class for all microservices.
 * <p>
 * A microservice defines a simple API for starting and stopping simple Java services
 * 	contained in executable jars.
 * <p>
 * The general command for invoking these services is...
 * <p class='bcode'>
 * 	java -jar mymicroservice.jar [mymicroservice.cfg]
 * </p>
 * <p>
 * Your microservice class must be specified as the <jk>Main-Class</jk> entry in
 * 	the manifest file of your microservice jar file.
 *
 * <h6 class='topic'>Microservice Configuration</h6>
 *
 * This class defines the following method for accessing configuration for your microservice:
 * <p>
 * <ul class='spaced-list'>
 * 	<li>{@link #getArgs()} - The command-line arguments passed to the jar file.
 * 	<li>{@link #getConfig()} - An external INI-style configuration file.
 * 	<li>{@link #getManifest()} - The manifest file for the main jar file.
 * </ul>
 *
 * <h6 class='topic'>Entrypoint Method</h6>
 *
 * Subclasses must implement a static void main method as the entry point for the microservice.
 * Typically, this method will simply consist of the following...
 * <p>
 * <p class='bcode'>
 * 	<jk>public static void</jk> main(String[] args) <jk>throws</jk> Exception {
 * 		<jk>new</jk> MyMicroservice(args).start();
 * 	}
 * </p>
 *
 * <h6 class='topic'>Lifecycle Methods</h6>
 *
 * Subclasses must implement the following lifecycle methods:
 * <p>
 * <ul class='spaced-list'>
 * 	<li>{@link #start()} - Gets executed during startup.
 * 	<li>{@link #stop()} - Gets executed when 'exit' is typed in the console or an external shutdown signal is received.
 * 	<li>{@link #kill()} - Can be used to forcibly shut down the service.  Doesn't get called during normal operation.
 * </ul>
 *
 * <h6 class='topic'>Lifecycle Listener Methods</h6>
 *
 * Subclasses can optionally implement the following event listener methods:
 * <p>
 * <ul class='spaced-list'>
 * 	<li>{@link #onStart()} - Gets executed before {@link #start()}.
 * 	<li>{@link #onStop()} - Gets executed before {@link #stop()}.
 * 	<li>{@link #onConfigSave(ConfigFile)} - Gets executed after a config file has been saved.
 * 	<li>{@link #onConfigChange(ConfigFile, Set)} - Gets executed after a config file has been modified.
 * </ul>
 *
 * <h6 class='topic'>Other Methods</h6>
 *
 * Subclasses can optionally override the following methods to provide customized behavior:
 * <p>
 * <ul class='spaced-list'>
 * 	<li>{@link #createVarResolver()} - Creates the {@link VarResolver} used to resolve variables in the config file returned by {@link #getConfig()}.
 * </ul>
 */
public abstract class Microservice {

	private static Args args;
	private static ConfigFile cf;
	private static ManifestFile mf;
	
	private String cfPath;

	/**
	 * Constructor.
	 *
	 * @param args Command line arguments.
	 * @throws Exception
	 */
	protected Microservice(String...args) throws Exception {
		Microservice.args = new Args(args);
	}
	
	/**
	 * Specifies the path of the config file for this microservice.
	 * <p>
	 * If you do not specify the config file location, we attempt to resolve it through the following methods:
	 * <ol>
	 * 	<li>The first argument in the command line arguments passed in through the constructor.
	 * 	<li>The value of the <code>Main-ConfigFile</code> entry in the manifest file.
	 * 	<li>A config file in the same location and with the same name as the executable jar file.
	 * 		(e.g. <js>"java -jar myjar.jar"</js> will look for <js>"myjar.cfg"</js>).
	 * </ol>
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
				throw new FileNotFoundException("Could not locate config file at '"+f.getAbsolutePath()+"'");
			if (! f.createNewFile())
				throw new FileNotFoundException("Could not create config file at '"+f.getAbsolutePath()+"'");
		}
		this.cfPath = cfPath;
		return this;
	}
	
	/**
	 * Specifies the config file for this microservice.
	 * <p>
	 * Note that if you use this method instead of {@link #setConfig(String,boolean)}, the config file will not use
	 * the variable resolver constructed from {@link #createVarResolver()}.  
	 *
	 * @param cf The config file for this application, or <jk>null</jk> if no config file is needed.
	 * @return This object (for method chaining).
	 */
	public Microservice setConfig(ConfigFile cf) {
		Microservice.cf = cf;
		return this;
	}

	/**
	 * Specifies the manifest file of the jar file this microservice is contained within.
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
	public Microservice setManifest(Manifest mf) {
		Microservice.mf = new ManifestFile(mf);
		return this;
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
		Microservice.mf = new ManifestFile(new Manifest(new ByteArrayInputStream(s.getBytes("UTF-8"))));
		return this;
	}

	/**
	 * Same as {@link #setManifest(Manifest)} except specified through a {@link File} object.
	 * 
	 * @param f The manifest file of this microservice.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public Microservice setManifest(File f) throws IOException {
		Microservice.mf = new ManifestFile(f);
		return this;
	}

	/**
	 * Same as {@link #setManifest(Manifest)} except finds and loads the manifest file of the jar file that the specified class is contained within.
	 * 
	 * @param c The class whose jar file contains the manifest to use for this microservice.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred while trying to read the manifest file.
	 */
	public Microservice setManifest(Class<?> c) throws IOException {
		Microservice.mf = new ManifestFile(c);
		return this;
	}

	/**
	 * Creates the {@link VarResolver} used to resolve variables in the
	 * config file returned by {@link #getConfig()}.
	 * <p>
	 * The default implementation resolves the following variables:
	 * <ul>
	 * 	<li><code>$S{key}</code>, <code>$S{key,default}</code> - System properties.
	 * 	<li><code>$E{key}</code>, <code>$E{key,default}</code> - Environment variables.
	 * 	<li><code>$C{key}</code>, <code>$C{key,default}</code> - Config file entries.
	 * 	<li><code>$MF{key}</code>, <code>$MF{key,default}</code> - Manifest file entries.
	 * 	<li><code>$ARG{key}</code>, <code>$ARG{key,default}</code> - Command-line arguments.
	 * 	<li><code>$IF{boolArg,thenValue}</code>, <code>$IF{boolArg,thenValue,elseValue}</code> - If-block logic.
	 * 	<li><code>$SWITCH{stringArg,pattern,thenVal...}</code>, <code>$SWITCH{stringArg,pattern,thenVal,elseVal...}</code>  - Switch-block logic.
	 * </ul>
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
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<jc>// Example java code</jc>
	 * 	String myentry = getConfig().getString(<js>"MySection/myEntry"</js>); <jc>// == "[foo]"</js>
	 * </p>
	 *
	 * @return A new {@link VarResolver}.
	 */
	protected VarResolver createVarResolver() {
		return new VarResolver()
			.addVars(SystemPropertiesVar.class, EnvVariablesVar.class, ConfigFileVar.class, ManifestFileVar.class, ArgsVar.class, SwitchVar.class, IfVar.class)
			.setContextObject(ConfigFileVar.SESSION_config, cf)
			.setContextObject(ManifestFileVar.SESSION_manifest, mf)
			.setContextObject(ArgsVar.SESSION_args, args);
	}

	/**
	 * Returns the command-line arguments passed into the application.
	 * <p>
	 * This method can be called from the class constructor.
	 * <p>
	 * See {@link Args} for details on using this method.
	 *
	 * @return The command-line arguments passed into the application.
	 */
	protected static Args getArgs() {
		return args;
	}

	/**
	 * Returns the external INI-style configuration file that can be used to configure your microservice.
	 * <p>
	 * The config file location is determined in the following order:
	 * <ol class='spaced-list'>
	 * 	<li>The first argument passed to the microservice jar.
	 * 	<li>The <code>Main-ConfigFile</code> entry in the microservice jar manifest file.
	 * 	<li>The name of the microservice jar with a <js>".cfg"</js> suffix (e.g. <js>"mymicroservice.jar"</js>-&gt;<js>"mymicroservice.cfg"</js>).
	 * </ol>
	 * <p>
	 * If all methods for locating the config file fail, then this method returns <jk>null</jk>.
	 * <p>
	 * Subclasses can set their own config file by calling the {@link #setConfig(ConfigFile)} method.
	 * <p>
	 * String variables defined by {@link #createVarResolver()} are automatically resolved when using this method.
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
	 * 	<ck>myArg</ck> = $ARG{myarg}
	 *
	 * 	<cc># The first command-line argument</cc>
	 * 	<ck>firstArg</ck> = $ARG{0}
	 *
	 * 	<cc># Look for system property, or env var if that doesn't exist, or command-line arg if that doesn't exist.</cc>
	 * 	<ck>nested</ck> = $S{mySystemProperty,$E{MY_ENV_VAR,$ARG{0}}}
	 *
	 * 	<cc># A POJO with embedded variables</cc>
	 * 	<ck>aBean2</ck> = {foo:'$ARG{0}',baz:$C{MySection/anInt}}
	 *
	 * 		</p>
	 * 		<p class='bcode'>
	 * 	<jc>// Java code for accessing config entries above.</jc>
	 * 	ConfigFile cf = getConfig();
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
	protected static ConfigFile getConfig() {
		return cf;
	}

	/**
	 * Returns the main jar manifest file contents as a simple {@link ObjectMap}.
	 * <p>
	 * This map consists of the contents of {@link Manifest#getMainAttributes()} with the keys
	 * 	and entries converted to simple strings.
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
	protected static ManifestFile getManifest() {
		return mf;
	}

	
	//--------------------------------------------------------------------------------
	// Abstract lifecycle methods.
	//--------------------------------------------------------------------------------

	/**
	 * Start this application.
	 * <p>
	 * Default implementation simply calls {@link #onStart()}.
	 * <p>
	 * Overridden methods MUST call this method FIRST so that the {@link #onStart()} method is called.
	 *
	 * @return This object (for method chaining).
	 * @throws Exception
	 */
	public Microservice start() throws Exception {
		
		// --------------------------------------------------------------------------------
		// Try to get the manifest file if it wasn't already set.
		// --------------------------------------------------------------------------------
		if (mf == null) {
			Manifest m = new Manifest();

			// If running within an eclipse workspace, need to get it from the file system.
			File f = new File("META-INF/MANIFEST.MF");
			if (f.exists()) {
				try {
					m.read(new FileInputStream(f));
				} catch (IOException e) {
					System.err.println("Problem detected in MANIFEST.MF.  Contents below:\n" + IOUtils.read(f));
					throw e;
				}
			} else {
				// Otherwise, read from manifest file in the jar file containing the main class.
				URLClassLoader cl = (URLClassLoader)getClass().getClassLoader();
				URL url = cl.findResource("META-INF/MANIFEST.MF");
				if (url != null) {
					try {
						m.read(url.openStream());
					} catch (IOException e) {
						System.err.println("Problem detected in MANIFEST.MF.  Contents below:\n" + IOUtils.read(url.openStream()));
						throw e;
					}
				}
			}
			mf = new ManifestFile(m);
		}

		// --------------------------------------------------------------------------------
		// Resolve the config file if the path was specified.
		// --------------------------------------------------------------------------------
		if (cfPath != null) 
			cf = ConfigMgr.DEFAULT.get(cfPath).getResolving(createVarResolver());
		
		// --------------------------------------------------------------------------------
		// Find config file.
		// Can either be passed in as first parameter, or we discover it using
		// the 'sun.java.command' system property.
		// --------------------------------------------------------------------------------
		if (cf == null) {
			if (args.hasArg(0))
				cfPath = args.getArg(0);
			else if (mf.containsKey("Main-ConfigFile"))
				cfPath = mf.getString("Main-ConfigFile");
			else {
				String cmd = System.getProperty("sun.java.command", "not_found").split("\\s+")[0];
				if (cmd.endsWith(".jar"))
					cfPath = cmd.replace(".jar", ".cfg");
			}

			if (cfPath == null) {
				System.err.println("Running class ["+getClass().getSimpleName()+"] without a config file.");
				cf = ConfigMgr.DEFAULT.create();
			} else {
				System.out.println("Running class ["+getClass().getSimpleName()+"] using config file ["+cfPath+"]");
				cf = ConfigMgr.DEFAULT.get(cfPath).getResolving(createVarResolver());
			}
		}

		if (cfPath != null)
			System.setProperty("juneau.configFile", cfPath);

		// --------------------------------------------------------------------------------
		// Set system properties.
		// --------------------------------------------------------------------------------
		Set<String> spKeys = cf.getSectionKeys("SystemProperties");
		if (spKeys != null)
			for (String key : spKeys)
				System.setProperty(key, cf.get("SystemProperties", key));

		// --------------------------------------------------------------------------------
		// Add a config file change listener.
		// --------------------------------------------------------------------------------
		cf.addListener(new ConfigFileListener() {
			@Override /* ConfigFileListener */
			public void onSave(ConfigFile cf) {
				onConfigSave(cf);
			}
			@Override /* ConfigFileListener */
			public void onChange(ConfigFile cf, Set<String> changes) {
				onConfigChange(cf, changes);
			}
		});
		
		// --------------------------------------------------------------------------------
		// Add exit listeners.
		// --------------------------------------------------------------------------------
		new Thread() {
			@Override /* Thread */
			public void run() {
				Console c = System.console();
				if (c == null)
					System.out.println("No available console.");
				else {
					while (true) {
						String l = c.readLine("\nEnter 'exit' to exit.\n");
						if (l == null || l.equals("exit")) {
							Microservice.this.stop();
							break;
						}
					}
				}
			}
		}.start();
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
	 * Joins the application with the current thread.
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
	 * <p>
	 * Default implementation simply calls {@link #onStop()}.
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
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStart() {}

	/**
	 * Called at the end of the {@link #stop()} call.
	 * <p>
	 * Subclasses can override this method to hook into the lifecycle of this application.
	 */
	protected void onStop() {}

	/**
	 * Called if the {@link ConfigFile#save()} is called on the config file.
	 * <p>
	 * Subclasses can override this method to listen for config file changes.
	 *
	 * @param cf The config file.
	 */
	protected void onConfigSave(ConfigFile cf) {}

	/**
	 * Called if one or more changes occur in the config file.
	 * <p>
	 * Subclasses can override this method to listen for config file changes.
	 *
	 * @param cf The config file.
	 * @param changes The list of keys in the config file being changed.
	 */
	protected void onConfigChange(ConfigFile cf, Set<String> changes) {}
}
