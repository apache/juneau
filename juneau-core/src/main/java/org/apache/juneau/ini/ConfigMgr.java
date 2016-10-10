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
package org.apache.juneau.ini;

import static org.apache.juneau.ini.ConfigFileFormat.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Manager for retrieving shared instances of {@link ConfigFile ConfigFiles}.
 *
 * <h6 class='topic'>Example:</h6>
 * <p class='bcode'>
 * 	ConfigFile cf = ConfigMgr.<jsf>DEFAULT</jsf>.get(<js>"MyConfig.cfg"</js>);
 * 	String setting = cf.get(<js>"MySection/mysetting"</js>);
 * </p>
 */
public class ConfigMgr {

	/**
	 * Default reusable configuration manager.
	 * <ul class='spaced-list'>
	 * 	<li>Read-only: <jk>false</jk>.
	 * 	<li>Encoder: {@link XorEncoder}.
	 * 	<li>Serializer: {@link JsonSerializer#DEFAULT}.
	 * 	<li>Parser: {@link JsonParser#DEFAULT}.
	 * 	<li>Charset: {@link Charset#defaultCharset()}.
	 * 	<li>Search paths: [<js>"."</js>].
	 * </ul>
	 */
	public static final ConfigMgr DEFAULT = new ConfigMgr(false, new XorEncoder(), JsonSerializer.DEFAULT, JsonParser.DEFAULT, Charset.defaultCharset(), new String[]{"."});

	private ConcurrentHashMap<String,File> files = new ConcurrentHashMap<String,File>();
	private ConcurrentHashMap<File,ConfigFile> configs = new ConcurrentHashMap<File,ConfigFile>();
	private final WriterSerializer serializer;
	private final ReaderParser parser;
	private final Encoder encoder;
	private final boolean readOnly;
	private final Charset charset;
	private final List<File> searchPaths = new LinkedList<File>();

	/**
	 * Create a custom configuration manager.
	 *
	 * @param readOnly Make {@link ConfigFile ConfigFiles} read-only.
	 * @param encoder Optional.  Specify the encoder to use for encoded config file entries (e.g. <js>"mySecret*={...}"</js>).
	 * @param serializer Optional.  Specify the serializer to use for serializing POJOs when using {@link ConfigFile#put(String, Object)}.
	 * @param parser Optional.  Specify the parser to use for parsing POJOs when using {@link ConfigFile#getObject(Class,String)}.
	 * @param charset Optional.  Specify the config file character encoding.  If <jk>null</jk>, uses {@link Charset#defaultCharset()}.
	 * @param searchPaths Specify the search paths for config files.  Can contain relative or absolute paths.
	 */
	public ConfigMgr(boolean readOnly, Encoder encoder, WriterSerializer serializer, ReaderParser parser, Charset charset, String[] searchPaths) {
		this.readOnly = readOnly;
		this.encoder = encoder;
		this.serializer = serializer;
		this.parser = parser;
		this.charset = charset;
		if (searchPaths != null)
			for (String p : searchPaths)
				this.searchPaths.add(new File(p));
	}

	/**
	 * Returns the config file with the specified absolute or relative path.
	 * <p>
	 * Multiple calls to the same path return the same <code>ConfigFile</code> instance.
	 *
	 * @param path The absolute or relative path of the config file.
	 * @return The config file.
	 * @throws IOException If config file could not be parsed.
	 * @throws FileNotFoundException If config file could not be found.
	 */
	public ConfigFile get(String path) throws IOException {
		return get(path, false);
	}

	/**
	 * Returns the config file with the specified absolute or relative path.
	 * <p>
	 * Multiple calls to the same path return the same <code>ConfigFile</code> instance.
	 * <p>
	 * If file doesn't exist and <code>create</code> is <jk>true</jk>, the configuration file will be
	 * create in the location identified by the first entry in the search paths.
	 *
	 * @param path The absolute or relative path of the config file.
	 * @param create Create the config file if it doesn't exist.
	 * @return The config file.
	 * @throws IOException If config file could not be parsed.
	 * @throws FileNotFoundException If config file could not be found or could not be created.
	 */
	public ConfigFile get(String path, boolean create) throws IOException {

		File f = resolve(path, create);

		ConfigFile cf = configs.get(f);
		if (cf != null)
			return cf;

		cf = new ConfigFileImpl(f, readOnly, encoder, serializer, parser, charset);
		configs.putIfAbsent(f, cf);
		return configs.get(f);
	}

	/**
	 * Create a new empty config file not backed by any file.
	 *
	 * @return A new config file.
	 * @throws IOException
	 */
	public ConfigFile create() throws IOException {
		return new ConfigFileImpl(null, false, encoder, serializer, parser, charset);
	}

	/**
	 * Create a new config file backed by the specified file.
	 * Note that {@link #get(String)} is the preferred method for getting access to config files
	 * 	since this method will create a new config file each time it is called.
	 * This method is provided primarily for testing purposes.
	 *
	 * @param f The file to create a config file from.
	 * @return A new config file.
	 * @throws IOException
	 */
	public ConfigFile create(File f) throws IOException {
		return new ConfigFileImpl(f, false, encoder, serializer, parser, charset);
	}

	/**
	 * Create a new config file not backed by a file.
	 *
	 * @param r The reader containing an INI-formatted file to initialize the config file from.
	 * @return A new config file.
	 * @throws IOException
	 */
	public ConfigFile create(Reader r) throws IOException {
		return new ConfigFileImpl(null, false, encoder, serializer, parser, charset).load(r);
	}

	/**
	 * Reloads any config files that were modified.
	 * @throws IOException
	 */
	public void loadIfModified() throws IOException {
		for (ConfigFile cf : configs.values())
			cf.loadIfModified();
	}

	/**
	 * Delete all configuration files registered with this config manager.
	 */
	public void deleteAll() {
		for (File f : configs.keySet())
			FileUtils.delete(f);
		files.clear();
		configs.clear();
	}

	private File resolve(String path, boolean create) throws IOException {

		// See if it's cached.
		File f = files.get(path);
		if (f != null)
			return f;

		// Handle absolute file.
		f = new File(path);
		if (f.isAbsolute()) {
			if (create)
				FileUtils.create(f);
			if (f.exists())
				return addFile(path, f);
			throw new FileNotFoundException("Could not find config file '"+path+"'");
		}

		if (searchPaths.isEmpty())
			throw new FileNotFoundException("No search paths specified on ConfigMgr.");

		// Handle paths relative to search paths.
		for (File sf : searchPaths) {
			f = new File(sf.getAbsolutePath() + "/" + path);
			if (f.exists())
				return addFile(path, f);
		}

		if (create) {
			f = new File(searchPaths.get(0).getAbsolutePath() + "/" + path);
			FileUtils.create(f);
				return addFile(path, f);
		}

		throw new FileNotFoundException("Could not find config file '"+path+"'");
	}

	private File addFile(String path, File f) {
		files.putIfAbsent(path, f);
		return files.get(path);
	}

	/**
	 * Implements command-line features for working with INI configuration files.
	 * <p>
	 * Invoke as a normal Java program...
	 * <p>
	 * <p class='bcode'>
	 * 	java org.apache.juneau.ini.ConfigMgr [args]
	 * </p>
	 * <p>
	 * Arguments can be any of the following...
	 * <ul class='spaced-list'>
	 * 	<li>No arguments<br>
	 * 		Prints usage message.<br>
	 * 	<li><code>createBatchEnvFile -configfile &lt;configFile&gt; -envfile &lt;batchFile&gt; [-verbose]</code><br>
	 * 		Creates a batch file that will set each config file entry as an environment variable.<br>
	 * 		Characters in the keys that are not valid as environment variable names (e.g. <js>'/'</js> and <js>'.'</js>)
	 * 			will be converted to underscores.<br>
	 * 	<li><code>createShellEnvFile -configFile &lt;configFile&gt; -envFile &lt;configFile&gt; [-verbose]</code>
	 * 		Creates a shell script that will set each config file entry as an environment variable.<br>
	 * 		Characters in the keys that are not valid as environment variable names (e.g. <js>'/'</js> and <js>'.'</js>)
	 * 			will be converted to underscores.<br>
	 * 	<li><code>setVals -configFile &lt;configFile&gt; -vals [var1=val1 [var2=val2...]] [-verbose]</code>
	 * 		Sets values in config files.<br>
	 * </ul>
	 * <p>
	 * For example, the following command will create the file <code>'MyConfig.bat'</code> from the contents of the file <code>'MyConfig.cfg'</code>.
	 * <p class='bcode'>
	 * 	java org.apache.juneau.ini.ConfigMgr createBatchEnvFile -configfile C:\foo\MyConfig.cfg -batchfile C:\foo\MyConfig.bat
	 * </p>
	 *
	 * @param args Command-line arguments
	 */
	public static void main(String[] args) {

		Args a = new Args(args);
		String command = a.getArg(0);
		String configFile = a.getArg("configFile");
		String envFile = a.getArg("envFile");
		List<String> vals = a.getArgs("vals");

		if (command == null || ! (command.equals("createBatchEnvFile") || command.equals("createShellEnvFile") || command.equals("setVals")))
			printUsageAndExit();
		else if (configFile.isEmpty())
			printUsageAndExit();
		else if (command.equals("setVals") && vals.isEmpty())
			printUsageAndExit();
		else if ((command.equals("createBatchEnvFile") || command.equals("createShellEnvFile")) && envFile.isEmpty())
			printUsageAndExit();
		else {
			try {
				ConfigFile cf = ConfigMgr.DEFAULT.get(configFile);

				if (command.equalsIgnoreCase("setVals")) {
					for (String val : vals) {
						String[] x = val.split("\\=");
						if (x.length != 2)
							throw new RuntimeException("Invalid format for value: '"+val+"'.  Must be in the format 'key=value'");
						cf.put(x[0], x[1]);
					}
					cf.save();
					return;

				} else if (command.equalsIgnoreCase("createBatchEnvFile")) {
					Writer fw = new OutputStreamWriter(new FileOutputStream(envFile), Charset.defaultCharset());
					try {
						cf.serializeTo(fw, BATCH);
					} finally {
						fw.close();
					}
					return;

				} else if (command.equalsIgnoreCase("createShellEnvFile")) {
					Writer fw = new OutputStreamWriter(new FileOutputStream(envFile), Charset.defaultCharset());
					try {
						cf.serializeTo(fw, SHELL);
					} finally {
						fw.close();
					}
					return;
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private static void printUsageAndExit() {
		System.err.println("---Usage---");
		System.err.println("java -cp juneau.jar org.apache.juneau.ini.ConfigFile createBatchEnvFile -configFile <configFile> -envFile <envFile> [-verbose]");
		System.err.println("java -cp juneau.jar org.apache.juneau.ini.ConfigFile createShellEnvFile -configFile <configFile> -envFile <envFile> [-verbose]");
		System.err.println("java -cp juneau.jar org.apache.juneau.ini.ConfigFile setVals -configFile <configFile> -vals [var1 val1 [var2 val2...]] [-verbose]");
		int rc = Integer.getInteger("exit.2", 2);
		if (rc != 0)
			System.exit(rc);
	}
}
