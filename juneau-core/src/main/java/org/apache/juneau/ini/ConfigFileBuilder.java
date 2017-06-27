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
import static org.apache.juneau.internal.FileUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.utils.*;

/**
 * Builder for creating instances of {@link ConfigFile ConfigFiles}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bcode'>
 * 	ConfigFile cf = <jk>new</jk> ConfigFileBuilder().build(<js>"MyConfig.cfg"</js>);
 * 	String setting = cf.get(<js>"MySection/mysetting"</js>);
 * </p>
 */
@SuppressWarnings("hiding")
public class ConfigFileBuilder {

	private WriterSerializer serializer = JsonSerializer.DEFAULT_LAX;
	private ReaderParser parser = JsonParser.DEFAULT;
	private Encoder encoder = new XorEncoder();
	private boolean readOnly = false, createIfNotExists = false;
	private Charset charset = Charset.defaultCharset();
	private List<File> searchPaths = new AList<File>().append(new File("."));

	/**
	 * Specify the encoder to use for encoded config file entries (e.g. <js>"mySecret*={...}"</js>).
	 * <p>
	 * The default value for this setting is an instance of {@link XorEncoder}.
	 *
	 * @param encoder The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public ConfigFileBuilder encoder(Encoder encoder) {
		this.encoder = encoder;
		return this;
	}

	/**
	 * Specify the serializer to use for serializing POJOs when using {@link ConfigFile#put(String, Object)}.
	 * <p>
	 * The default value for this setting is {@link JsonSerializer#DEFAULT_LAX}.
	 *
	 * @param serializer The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public ConfigFileBuilder serializer(WriterSerializer serializer) {
		this.serializer = serializer;
		return this;
	}

	/**
	 * Specify the parser to use for parsing POJOs when using {@link ConfigFile#getObject(String,Class)}.
	 * <p>
	 * The default value for this setting is {@link JsonParser#DEFAULT}
	 *
	 * @param parser The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public ConfigFileBuilder parser(ReaderParser parser) {
		this.parser = parser;
		return this;
	}

	/**
	 * Specify the config file character encoding.
	 * <p>
	 * The default value for this setting is {@link Charset#defaultCharset()}.
	 *
	 * @param charset The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public ConfigFileBuilder charset(Charset charset) {
		this.charset = charset;
		return this;
	}

	/**
	 * Specify the search paths for config files.
	 * <p>
	 * Can contain relative or absolute paths.
	 * <p>
	 * The default value for this setting is <code>[<js>"."</js>]</code>.
	 *
	 * @param searchPaths The new value for this setting.
	 * @return This object (for method chaining).
	 */
	public ConfigFileBuilder paths(String...searchPaths) {
		this.searchPaths = new LinkedList<File>();
		for (String p : searchPaths)
			this.searchPaths.add(new File(p));
		return this;
	}

	/**
	 * Make {@link ConfigFile ConfigFiles} read-only.
	 * <p>
	 * The default value of this setting is <jk>false</jk>.
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileBuilder readOnly() {
		this.readOnly = true;
		return this;
	}

	/**
	 * Create config files if they cannot be found on the file system.
	 * <p>
	 * The default value for this setting is <jk>false</jk>.
	 *
	 * @return This object (for method chaining).
	 */
	public ConfigFileBuilder createIfNotExists() {
		this.createIfNotExists = true;
		return this;
	}

	/**
	 * Returns the config file with the specified absolute or relative path.
	 *
	 * @param path The absolute or relative path of the config file.
	 * @return The config file.
	 * @throws IOException If config file could not be parsed.
	 * @throws FileNotFoundException If config file could not be found.
	 */
	public ConfigFile build(String path) throws IOException {
		return new ConfigFileImpl(resolve(path), readOnly, encoder, serializer, parser, charset);
	}

	/**
	 * Create a new empty config file not backed by any file.
	 *
	 * @return A new config file.
	 * @throws IOException
	 */
	public ConfigFile build() throws IOException {
		return new ConfigFileImpl(null, false, encoder, serializer, parser, charset);
	}

	/**
	 * Create a new config file backed by the specified file.
	 * <p>
	 * This method is provided primarily for testing purposes.
	 *
	 * @param f The file to create a config file from.
	 * @return A new config file.
	 * @throws IOException
	 */
	public ConfigFile build(File f) throws IOException {
		return new ConfigFileImpl(f, false, encoder, serializer, parser, charset);
	}

	/**
	 * Create a new config file not backed by a file.
	 *
	 * @param r The reader containing an INI-formatted file to initialize the config file from.
	 * @return A new config file.
	 * @throws IOException
	 */
	public ConfigFile build(Reader r) throws IOException {
		return new ConfigFileImpl(null, false, encoder, serializer, parser, charset).load(r);
	}

	private File resolve(String path) throws IOException {

		// Handle absolute file.
		File f = new File(path);
		if (f.isAbsolute()) {
			if (createIfNotExists)
				create(f);
			if (f.exists())
				return f;
			throw new FileNotFoundException("Could not find config file '"+path+"'");
		}

		if (searchPaths.isEmpty())
			throw new FileNotFoundException("No search paths specified in ConfigFileBuilder.");

		// Handle paths relative to search paths.
		for (File sf : searchPaths) {
			f = new File(sf.getAbsolutePath() + "/" + path);
			if (f.exists())
				return f;
		}

		if (createIfNotExists) {
			f = new File(searchPaths.get(0).getAbsolutePath() + "/" + path);
			create(f);
			return f;
		}

		throw new FileNotFoundException("Could not find config file '"+path+"'");
	}

	/**
	 * Implements command-line features for working with INI configuration files.
	 * <p>
	 * Invoke as a normal Java program...
	 * <p>
	 * <p class='bcode'>
	 * 	java org.apache.juneau.ini.ConfigFileBuilder [args]
	 * </p>
	 * <p>
	 * Arguments can be any of the following...
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		No arguments
	 * 		<br>Prints usage message.
	 * 	<li>
	 * 		<code>createBatchEnvFile -configfile &lt;configFile&gt; -envfile &lt;batchFile&gt; [-verbose]</code>
	 * 		<br>Creates a batch file that will set each config file entry as an environment variable.
	 * 		<br>Characters in the keys that are not valid as environment variable names (e.g. <js>'/'</js> and <js>'.'</js>)
	 * 		will be converted to underscores.
	 * 	<li>
	 * 		<code>createShellEnvFile -configFile &lt;configFile&gt; -envFile &lt;configFile&gt; [-verbose]</code>
	 * 		Creates a shell script that will set each config file entry as an environment variable.
	 * 		<br>Characters in the keys that are not valid as environment variable names (e.g. <js>'/'</js> and <js>'.'</js>)
	 * 		will be converted to underscores.
	 * 	<li>
	 * 		<code>setVals -configFile &lt;configFile&gt; -vals [var1=val1 [var2=val2...]] [-verbose]</code>
	 * 		Sets values in config files.
	 * </ul>
	 * <p>
	 * For example, the following command will create the file <code>'MyConfig.bat'</code> from the contents of the
	 * file <code>'MyConfig.cfg'</code>.
	 * <p class='bcode'>
	 * 	java org.apache.juneau.ini.ConfigFileBuilder createBatchEnvFile -configfile C:\foo\MyConfig.cfg
	 * 		-batchfile C:\foo\MyConfig.bat
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
				ConfigFile cf = new ConfigFileBuilder().build(configFile);

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
		System.err.println("---Usage---"); // NOT DEBUG
		System.err.println("java -cp juneau.jar org.apache.juneau.ini.ConfigFile createBatchEnvFile -configFile <configFile> -envFile <envFile> [-verbose]"); // NOT DEBUG
		System.err.println("java -cp juneau.jar org.apache.juneau.ini.ConfigFile createShellEnvFile -configFile <configFile> -envFile <envFile> [-verbose]"); // NOT DEBUG
		System.err.println("java -cp juneau.jar org.apache.juneau.ini.ConfigFile setVals -configFile <configFile> -vals [var1 val1 [var2 val2...]] [-verbose]"); // NOT DEBUG
		int rc = Integer.getInteger("exit.2", 2);
		if (rc != 0)
			System.exit(rc);
	}
}
