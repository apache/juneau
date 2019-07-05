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
package org.apache.juneau.config;

import static org.apache.juneau.config.ConfigMod.*;
import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.internal.ThrowableUtils.*;

import java.beans.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.config.encode.*;
import org.apache.juneau.config.encode.ConfigEncoder;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.http.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;

/**
 * Main configuration API class.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='link'>{@doc juneau-config}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@ConfigurableContext
public final class Config extends Context implements ConfigEventListener, Writable {

	private static boolean DISABLE_AUTO_SYSTEM_PROPS = Boolean.getBoolean("juneau.disableAutoSystemProps");
	private static volatile Config SYSTEM_DEFAULT = findSystemDefault();

	/**
	 * Sets a system default configuration.
	 *
	 * @param systemDefault The new system default configuration.
	 */
	public synchronized static void setSystemDefault(Config systemDefault) {
		SYSTEM_DEFAULT = systemDefault;
	}

	/**
	 * Returns the system default configuration.
	 *
	 * @return The system default configuration, or <jk>null</jk> if it doesn't exist.
	 */
	public synchronized static Config getSystemDefault() {
		return SYSTEM_DEFAULT;
	}

	private synchronized static Config findSystemDefault() {

		for (String n : getCandidateSystemDefaultConfigNames()) {
			Config config = find(n);
			if (config != null) {
				if (! DISABLE_AUTO_SYSTEM_PROPS)
					config.setSystemProperties();
				return config;
			}
		}

		return null;
	}

	/**
	 * Returns the list of candidate system default configuration file names.
	 *
	 * <p>
	 * If the <js>"juneau.configFile"</js> system property is set, returns a singleton of that value.
	 * <br>Otherwise, returns a list consisting of the following values:
	 * <ol>
	 * 	<li>File with same name as jar file but with <js>".cfg"</js> extension.  (e.g. <js>"myjar.cfg"</js>)
	 * 	<li>Any file ending in <js>".cfg"</js> in the home directory (names ordered alphabetically).
	 * 	<li><js>"juneau.cfg"</js>
	 * 	<li><js>"default.cfg"</js>
	 * 	<li><js>"application.cfg"</js>
	 * 	<li><js>"app.cfg"</js>
	 * 	<li><js>"settings.cfg"</js>
	 * 	<li><js>"application.properties"</js>
	 * </ol>
	 * <p>
	 *
	 * @return
	 * 	A list of candidate file names.
	 * 	<br>The returned list is modifiable.
	 * 	<br>Each call constructs a new list.
	 */
	public synchronized static List<String> getCandidateSystemDefaultConfigNames() {
		List<String> l = new ArrayList<>();

		String s = System.getProperty("juneau.configFile");
		if (s != null) {
			l.add(s);
			return l;
		}

		String cmd = System.getProperty("sun.java.command", "not_found").split("\\s+")[0];
		if (cmd.endsWith(".jar") && ! cmd.contains("surefirebooter")) {
			cmd = cmd.replaceAll(".*?([^\\\\\\/]+)\\.jar$", "$1");
			l.add(cmd + ".cfg");
			cmd = cmd.replaceAll("[\\.\\_].*$", "");  // Try also without version in jar name.
			l.add(cmd + ".cfg");
		}

		Set<File> files = new TreeSet<>(Arrays.asList(new File(".").listFiles()));
		for (File f : files)
			if (f.getName().endsWith(".cfg"))
				l.add(f.getName());

		l.add("juneau.cfg");
		l.add("default.cfg");
		l.add("application.cfg");
		l.add("app.cfg");
		l.add("settings.cfg");
		l.add("application.properties");

		return l;
	}

	private synchronized static Config find(String name) {
		if (name == null)
			return null;
		if (ConfigFileStore.DEFAULT.exists(name))
			return Config.create(name).store(ConfigFileStore.DEFAULT).build();
		if (ConfigClasspathStore.DEFAULT.exists(name))
			return Config.create(name).store(ConfigClasspathStore.DEFAULT).build();
		return null;
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Configurable properties
	//-------------------------------------------------------------------------------------------------------------------

	static final String PREFIX = "Config";

	/**
	 * Configuration property:  Configuration name.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.name.s"</js>
	 * 	<li><b>Data type:</b>  <c>String</c>
	 * 	<li><b>Default:</b>  <js>"Configuration.cfg"</js>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#name(String)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * Specifies the configuration name.
	 * <br>This is typically the configuration file name, although
	 * the name can be anything identifiable by the {@link ConfigStore} used for retrieving and storing the configuration.
	 */
	public static final String CONFIG_name = PREFIX + ".name.s";

	/**
	 * Configuration property:  Configuration store.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.store.o"</js>
	 * 	<li><b>Data type:</b>  {@link ConfigStore}
	 * 	<li><b>Default:</b>  {@link ConfigFileStore#DEFAULT}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#store(ConfigStore)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The configuration store used for retrieving and storing configurations.
	 */
	public static final String CONFIG_store = PREFIX + ".store.o";

	/**
	 * Configuration property:  POJO serializer.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.serializer.o"</js>
	 * 	<li><b>Data type:</b>  {@link WriterSerializer}
	 * 	<li><b>Default:</b>  {@link SimpleJsonSerializer#DEFAULT}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#serializer(Class)}
	 * 			<li class='jm'>{@link ConfigBuilder#serializer(WriterSerializer)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The serializer to use for serializing POJO values.
	 */
	public static final String CONFIG_serializer = PREFIX + ".serializer.o";

	/**
	 * Configuration property:  POJO parser.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.parser.o"</js>
	 * 	<li><b>Data type:</b>  {@link ReaderParser}
	 * 	<li><b>Default:</b>  {@link JsonParser#DEFAULT}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#parser(Class)}
	 * 			<li class='jm'>{@link ConfigBuilder#parser(ReaderParser)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The parser to use for parsing values to POJOs.
	 */
	public static final String CONFIG_parser = PREFIX + ".parser.o";

	/**
	 * Configuration property:  Value encoder.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.encoder.o"</js>
	 * 	<li><b>Data type:</b>  {@link ConfigEncoder}
	 * 	<li><b>Default:</b>  {@link ConfigXorEncoder#INSTANCE}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#encoder(Class)}
	 * 			<li class='jm'>{@link ConfigBuilder#encoder(ConfigEncoder)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The encoder to use for encoding encoded configuration values.
	 */
	public static final String CONFIG_encoder = PREFIX + ".encoder.o";

	/**
	 * Configuration property:  SVL variable resolver.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.varResolver.o"</js>
	 * 	<li><b>Data type:</b>  {@link VarResolver}
	 * 	<li><b>Default:</b>  {@link VarResolver#DEFAULT}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#varResolver(Class)}
	 * 			<li class='jm'>{@link ConfigBuilder#varResolver(VarResolver)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The resolver to use for resolving SVL variables.
	 */
	public static final String CONFIG_varResolver = PREFIX + ".varResolver.o";

	/**
	 * Configuration property:  Binary value line length.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.binaryLineLength.i"</js>
	 * 	<li><b>Data type:</b>  <c>Integer</c>
	 * 	<li><b>Default:</b>  <c>-1</c>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#binaryLineLength(int)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When serializing binary values, lines will be split after this many characters.
	 * <br>Use <c>-1</c> to represent no line splitting.
	 */
	public static final String CONFIG_binaryLineLength = PREFIX + ".binaryLineLength.i";

	/**
	 * Configuration property:  Binary value format.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.binaryFormat.s"</js>
	 * 	<li><b>Data type:</b>  {@link BinaryFormat}
	 * 	<li><b>Default:</b>  {@link BinaryFormat#BASE64}
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#binaryFormat(BinaryFormat)}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * The format to use when persisting byte arrays.
	 *
	 * <p>
	 * Possible values:
	 * <ul>
	 * 	<li>{@link BinaryFormat#BASE64} - BASE64-encoded string.
	 * 	<li>{@link BinaryFormat#HEX} - Hexadecimal.
	 * 	<li>{@link BinaryFormat#SPACED_HEX} - Hexadecimal with spaces between bytes.
	 * </ul>
	 */
	public static final String CONFIG_binaryFormat = PREFIX + ".binaryFormat.s";

	/**
	 * Configuration property:  Multi-line values should always be on separate lines.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.multiLineValuesOnSeparateLines.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#multiLineValuesOnSeparateLines()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, multi-line values will always be placed on a separate line from the key.
	 */
	public static final String CONFIG_multiLineValuesOnSeparateLines = PREFIX + ".multiLineValuesOnSeparateLines.b";

	/**
	 * Configuration property:  Read-only.
	 *
	 * <h5 class='section'>Property:</h5>
	 * <ul>
	 * 	<li><b>Name:</b>  <js>"Config.readOnly.b"</js>
	 * 	<li><b>Data type:</b>  <c>Boolean</c>
	 * 	<li><b>Default:</b>  <jk>false</jk>
	 * 	<li><b>Methods:</b>
	 * 		<ul>
	 * 			<li class='jm'>{@link ConfigBuilder#readOnly()}
	 * 		</ul>
	 * </ul>
	 *
	 * <h5 class='section'>Description:</h5>
	 * <p>
	 * When enabled, attempts to call any setters on this object will throw an {@link UnsupportedOperationException}.
	 */
	public static final String CONFIG_readOnly = PREFIX + ".readOnly.b";

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	private final String name;
	private final ConfigStore store;
	private final WriterSerializer serializer;
	private final ReaderParser parser;
	private final ConfigEncoder encoder;
	private final VarResolverSession varSession;
	private final int binaryLineLength;
	private final BinaryFormat binaryFormat;
	private final boolean multiLineValuesOnSeparateLines, readOnly;
	private final ConfigMap configMap;
	private final BeanSession beanSession;
	private final List<ConfigEventListener> listeners = Collections.synchronizedList(new LinkedList<ConfigEventListener>());


	/**
	 * Instantiates a new clean-slate {@link ConfigBuilder} object.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> ConfigBuilder()</code>.
	 *
	 * @return A new {@link ConfigBuilder} object.
	 */
	public static ConfigBuilder create() {
		return new ConfigBuilder();
	}

	/**
	 * Same as {@link #create()} but initializes the builder with the specified config name.
	 *
	 * <p>
	 * This is equivalent to simply calling <code><jk>new</jk> ConfigBuilder().name(name)</code>.
	 *
	 * @param name The configuration name.
	 * @return A new {@link ConfigBuilder} object.
	 */
	public static ConfigBuilder create(String name) {
		return new ConfigBuilder().name(name);
	}

	@Override /* Context */
	public ConfigBuilder builder() {
		return new ConfigBuilder(getPropertyStore());
	}

	/**
	 * Constructor.
	 *
	 * @param ps
	 * 	The property store containing all the settings for this object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public Config(PropertyStore ps) throws IOException {
		super(ps, true);

		name = getStringProperty(CONFIG_name, "Configuration.cfg");
		store = getInstanceProperty(CONFIG_store, ConfigStore.class, ConfigFileStore.DEFAULT);
		configMap = store.getMap(name);
		configMap.register(this);
		serializer = getInstanceProperty(CONFIG_serializer, WriterSerializer.class, SimpleJsonSerializer.DEFAULT);
		parser = getInstanceProperty(CONFIG_parser, ReaderParser.class, JsonParser.DEFAULT);
		beanSession = parser.createBeanSession();
		encoder = getInstanceProperty(CONFIG_encoder, ConfigEncoder.class, ConfigXorEncoder.INSTANCE);
		varSession = getInstanceProperty(CONFIG_varResolver, VarResolver.class, VarResolver.DEFAULT)
			.builder()
			.vars(ConfigVar.class)
			.contextObject(ConfigVar.SESSION_config, this)
			.build()
			.createSession();
		binaryLineLength = getIntegerProperty(CONFIG_binaryLineLength, -1);
		binaryFormat = getProperty(CONFIG_binaryFormat, BinaryFormat.class, BinaryFormat.BASE64);
		multiLineValuesOnSeparateLines = getBooleanProperty(CONFIG_multiLineValuesOnSeparateLines, false);
		readOnly = getBooleanProperty(CONFIG_readOnly, false);
	}

	Config(Config copyFrom, VarResolverSession varSession) {
		super(null, true);
		name = copyFrom.name;
		store = copyFrom.store;
		configMap = copyFrom.configMap;
		configMap.register(this);
		serializer = copyFrom.serializer;
		parser = copyFrom.parser;
		encoder = copyFrom.encoder;
		this.varSession = varSession;
		binaryLineLength = copyFrom.binaryLineLength;
		binaryFormat = copyFrom.binaryFormat;
		multiLineValuesOnSeparateLines = copyFrom.multiLineValuesOnSeparateLines;
		readOnly = copyFrom.readOnly;
		beanSession = copyFrom.beanSession;
	}

	/**
	 * Creates a copy of this config using the specified var session for resolving variables.
	 *
	 * <p>
	 * This creates a shallow copy of the config but replacing the variable resolver.
	 *
	 * @param varSession The var session used for resolving string variables.
	 * @return A new config object.
	 */
	public Config resolving(VarResolverSession varSession) {
		return new Config(this, varSession);
	}

	/**
	 * Returns the name associated with this config (usually a file name).
	 *
	 * @return The name associated with this config, or <jk>null</jk> if it has no name.
	 */
	public String getName() {
		return name;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Workhorse getters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the specified value as a string from the config file.
	 *
	 * <p>
	 * Unlike {@link #getString(String)}, this method doesn't replace SVL variables.
	 *
	 * @param key The key.
	 * @return The value, or <jk>null</jk> if the section or value doesn't exist.
	 */
	public String get(String key) {

		String sname = sname(key);
		String skey = skey(key);

		ConfigEntry ce = configMap.getEntry(sname, skey);

		if (ce == null || ce.getValue() == null)
			return null;

		String val = ce.getValue();
		for (ConfigMod m : ConfigMod.asModifiersReverse(ce.getModifiers())) {
			if (m == ENCODED) {
				if (encoder.isEncoded(val))
					val = encoder.decode(key, val);
			}
		}

		return val;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Takes the settings defined in this configuration and sets them as system properties.
	 *
	 * @return This object (for method chaining).
	 */
	public Config setSystemProperties() {
		for (String section : getSections()) {
			for (String key : getKeys(section)) {
				String k = (section.isEmpty() ? key : section + '/' + key);
				System.setProperty(k, get(k));
			}
		}
		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Workhorse setters
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Sets a value in this config.
	 *
	 * @param key The key.
	 * @param value The value.
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set(String key, String value) {
		checkWrite();
		assertFieldNotNull(key, "key");
		String sname = sname(key);
		String skey = skey(key);

		ConfigEntry ce = configMap.getEntry(sname, skey);
		if (ce == null && value == null)
			return this;

		String mod = ce == null ? "" : ce.getModifiers();

		String s = stringify(value);
		for (ConfigMod m : ConfigMod.asModifiers(mod)) {
			if (m == ENCODED) {
				s = encoder.encode(key, s);
			}
		}

		configMap.setEntry(sname, skey, s, null, null, null);
		return this;
	}

	/**
	 * Adds or replaces an entry with the specified key with a POJO serialized to a string using the registered
	 * serializer.
	 *
	 * <p>
	 * Equivalent to calling <c>put(key, value, isEncoded(key))</c>.
	 *
	 * @param key The key.
	 * @param value The new value POJO.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException
	 * 	If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set(String key, Object value) throws SerializeException {
		return set(key, value, null);
	}

	/**
	 * Same as {@link #set(String, Object)} but allows you to specify the serializer to use to serialize the
	 * value.
	 *
	 * @param key The key.
	 * @param value The new value.
	 * @param serializer
	 * 	The serializer to use for serializing the object.
	 * 	If <jk>null</jk>, then uses the predefined serializer on the config file.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException
	 * 	If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set(String key, Object value, Serializer serializer) throws SerializeException {
		return set(key, serialize(value, serializer));
	}

	/**
	 * Same as {@link #set(String, Object)} but allows you to specify all aspects of a value.
	 *
	 * @param key The key.
	 * @param value The new value.
	 * @param serializer
	 * 	The serializer to use for serializing the object.
	 * 	If <jk>null</jk>, then uses the predefined serializer on the config file.
	 * @param modifier
	 * 	Optional modifier to apply to the value.
	 * 	<br>If <jk>null</jk>, then previous value will not be replaced.
	 * @param comment
	 * 	Optional same-line comment to add to this value.
	 * 	<br>If <jk>null</jk>, then previous value will not be replaced.
	 * @param preLines
	 * 	Optional comment or blank lines to add before this entry.
	 * 	<br>If <jk>null</jk>, then previous value will not be replaced.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException
	 * 	If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set(String key, Object value, Serializer serializer, ConfigMod modifier, String comment, List<String> preLines) throws SerializeException {
		return set(key, value, serializer, modifier == null ? null : new ConfigMod[]{modifier}, comment, preLines);
	}

	/**
	 * Same as {@link #set(String, Object)} but allows you to specify all aspects of a value.
	 *
	 * @param key The key.
	 * @param value The new value.
	 * @param serializer
	 * 	The serializer to use for serializing the object.
	 * 	If <jk>null</jk>, then uses the predefined serializer on the config file.
	 * @param modifiers
	 * 	Optional modifiers to apply to the value.
	 * 	<br>If <jk>null</jk>, then previous value will not be replaced.
	 * @param comment
	 * 	Optional same-line comment to add to this value.
	 * 	<br>If <jk>null</jk>, then previous value will not be replaced.
	 * @param preLines
	 * 	Optional comment or blank lines to add before this entry.
	 * 	<br>If <jk>null</jk>, then previous value will not be replaced.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws SerializeException
	 * 	If serializer could not serialize the value or if a serializer is not registered with this config file.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set(String key, Object value, Serializer serializer, ConfigMod[] modifiers, String comment, List<String> preLines) throws SerializeException {
		checkWrite();
		assertFieldNotNull(key, "key");
		String sname = sname(key);
		String skey = skey(key);

		String s = serialize(value, serializer);
		if (modifiers != null) {
			for (ConfigMod m : modifiers) {
				if (m == ENCODED) {
					s = encoder.encode(key, s);
				}
			}
		}

		configMap.setEntry(sname, skey, s, modifiers == null ? null : ConfigMod.asString(modifiers), comment, preLines);
		return this;
	}

	/**
	 * Removes an entry with the specified key.
	 *
	 * @param key The key.
	 * @return The previous value, or <jk>null</jk> if the section or key did not previously exist.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config remove(String key) {
		checkWrite();
		String sname = sname(key);
		String skey = skey(key);
		configMap.removeEntry(sname, skey);
		return this;
	}

	/**
	 * Encodes and unencoded entries in this config.
	 *
	 * <p>
	 * If any entries in the config are marked as encoded but not actually encoded,
	 * this will encode them.
	 *
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config encodeEntries() {
		checkWrite();
		for (String section : configMap.getSections()) {
			for (String key : configMap.getKeys(section)) {
				ConfigEntry ce = configMap.getEntry(section, key);
				if (ce != null && ce.hasModifier('*') && ! encoder.isEncoded(ce.getValue())) {
					configMap.setEntry(section, key, encoder.encode(section + '/' + key, ce.getValue()), null, null, null);
				}
			}
		}

		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// API methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Gets the entry with the specified key.
	 *
	 * <p>
	 * The key can be in one of the following formats...
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"key"</js> - A value in the default section (i.e. defined above any <c>[section]</c> header).
	 * 	<li>
	 * 		<js>"section/key"</js> - A value from the specified section.
	 * </ul>
	 *
	 * @param key The key.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public String getString(String key) {
		String s = get(key);
		if (s == null)
			return null;
		if (varSession != null)
			s = varSession.resolve(s);
		return s;
	}

	/**
	 * Gets the entry with the specified key.
	 *
	 * <p>
	 * The key can be in one of the following formats...
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"key"</js> - A value in the default section (i.e. defined above any <c>[section]</c> header).
	 * 	<li>
	 * 		<js>"section/key"</js> - A value from the specified section.
	 * </ul>
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the section or key does not exist.
	 */
	public String getString(String key, String def) {
		String s = get(key);
		if (isEmpty(s))
			return def;
		if (varSession != null)
			s = varSession.resolve(s);
		return s;
	}

	/**
	 * Gets the entry with the specified key, splits the value on commas, and returns the values as trimmed strings.
	 *
	 * @param key The key.
	 * @return The value, or an empty array if the section or key does not exist.
	 */
	public String[] getStringArray(String key) {
		return getStringArray(key, new String[0]);
	}

	/**
	 * Same as {@link #getStringArray(String)} but returns a default value if the value cannot be found.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the section or key does not exist or is blank.
	 */
	public String[] getStringArray(String key, String[] def) {
		String s = getString(key);
		if (isEmpty(s))
			return def;
		String[] r = split(s);
		return r.length == 0 ? def : r;
	}

	/**
	 * Convenience method for getting int config values.
	 *
	 * <p>
	 * <js>"K"</js>, <js>"M"</js>, and <js>"G"</js> can be used to identify kilo, mega, and giga.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<code><js>"100K"</js> => 1024000</code>
	 * 	<li>
	 * 		<code><js>"100M"</js> => 104857600</code>
	 * </ul>
	 *
	 * <p>
	 * Uses {@link Integer#decode(String)} underneath, so any of the following integer formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @param key The key.
	 * @return The value, or <c>0</c> if the value does not exist or the value is empty.
	 */
	public int getInt(String key) {
		return getInt(key, 0);
	}

	/**
	 * Same as {@link #getInt(String)} but returns a default value if not set.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the value does not exist or the value is empty.
	 */
	public int getInt(String key, int def) {
		String s = getString(key);
		if (isEmpty(s))
			return def;
		return parseIntWithSuffix(s);
	}

	/**
	 * Convenience method for getting boolean config values.
	 *
	 * @param key The key.
	 * @return The value, or <jk>false</jk> if the section or key does not exist or cannot be parsed as a boolean.
	 */
	public boolean getBoolean(String key) {
		return getBoolean(key, false);
	}

	/**
	 * Convenience method for getting boolean config values.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the section or key does not exist or cannot be parsed as a boolean.
	 */
	public boolean getBoolean(String key, boolean def) {
		String s = getString(key);
		return isEmpty(s) ? def : Boolean.parseBoolean(s);
	}

	/**
	 * Convenience method for getting long config values.
	 *
	 * <p>
	 * <js>"K"</js>, <js>"M"</js>, and <js>"G"</js> can be used to identify kilo, mega, and giga.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<code><js>"100K"</js> => 1024000</code>
	 * 	<li>
	 * 		<code><js>"100M"</js> => 104857600</code>
	 * </ul>
	 *
	 * <p>
	 * Uses {@link Long#decode(String)} underneath, so any of the following number formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @param key The key.
	 * @return The value, or <c>0</c> if the value does not exist or the value is empty.
	 */
	public long getLong(String key) {
		return getLong(key, 0);
	}

	/**
	 * Same as {@link #getLong(String)} but returns a default value if not set.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the value does not exist or the value is empty.
	 */
	public long getLong(String key, long def) {
		String s = getString(key);
		if (isEmpty(s))
			return def;
		return parseLongWithSuffix(s);
	}

	/**
	 * Convenience method for getting double config values.
	 *
	 * <p>
	 * Uses {@link Double#valueOf(String)} underneath, so any of the following number formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @param key The key.
	 * @return The value, or <c>0</c> if the value does not exist or the value is empty.
	 */
	public double getDouble(String key) {
		return getDouble(key, 0);
	}

	/**
	 * Same as {@link #getDouble(String)} but returns a default value if not set.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the value does not exist or the value is empty.
	 */
	public double getDouble(String key, double def) {
		String s = getString(key);
		if (isEmpty(s))
			return def;
		return Double.valueOf(s);
	}

	/**
	 * Convenience method for getting float config values.
	 *
	 * <p>
	 * Uses {@link Float#valueOf(String)} underneath, so any of the following number formats are supported:
	 * <ul>
	 * 	<li><js>"0x..."</js>
	 * 	<li><js>"0X..."</js>
	 * 	<li><js>"#..."</js>
	 * 	<li><js>"0..."</js>
	 * </ul>
	 *
	 * @param key The key.
	 * @return The value, or <c>0</c> if the value does not exist or the value is empty.
	 */
	public float getFloat(String key) {
		return getFloat(key, 0);
	}

	/**
	 * Same as {@link #getFloat(String)} but returns a default value if not set.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the value does not exist or the value is empty.
	 */
	public float getFloat(String key, float def) {
		String s = getString(key);
		if (isEmpty(s))
			return def;
		return Float.valueOf(s);
	}

	/**
	 * Convenience method for getting byte array config values.
	 *
	 * <p>
	 * This is equivalent to calling the following:
	 * <p class='bcode w800'>
	 * 	<jk>byte</jk>[] b = config.getObject(key, <jk>byte</jk>[].<jk>class</jk>);
	 * </p>
	 *
	 * Byte arrays are stored as encoded strings, typically BASE64, but dependent on the {@link #CONFIG_binaryFormat} setting.
	 *
	 * @param key The key.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException If value could not be converted to a byte array.
	 */
	public byte[] getBytes(String key) throws ParseException {
		String s = get(key);
		if (s == null)
			return null;
		if (s.isEmpty())
			return new byte[0];
		return getObject(key, byte[].class);
	}

	/**
	 * Same as {@link #getBytes(String)} but with a default value if the entry doesn't exist.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @return The value, or the default value if the section or key does not exist.
	 * @throws ParseException If value could not be converted to a byte array.
	 */
	public byte[] getBytes(String key, byte[] def) throws ParseException {
		String s = get(key);
		if (s == null)
			return def;
		if (s.isEmpty())
			return def;
		return getObjectWithDefault(key, def, byte[].class);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 *
	 * <p>
	 * The key can be in one of the following formats...
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		<js>"key"</js> - A value in the default section (i.e. defined above any <c>[section]</c> header).
	 * 	<li>
	 * 		<js>"section/key"</js> - A value from the specified section.
	 * </ul>
	 *
	 * <p>
	 * The type can be a simple type (e.g. beans, strings, numbers) or parameterized type (collections/maps).
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 *
	 * 	<jc>// Parse into a linked-list of strings.</jc>
	 * 	List l = cf.getObject(<js>"MySection/myListOfStrings"</js>, LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of beans.</jc>
	 * 	List l = cf.getObject(<js>"MySection/myListOfBeans"</js>, LinkedList.<jk>class</jk>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of linked-lists of strings.</jc>
	 * 	List l = cf.getObject(<js>"MySection/my2dListOfStrings"</js>, LinkedList.<jk>class</jk>,
	 * 		LinkedList.<jk>class</jk>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of string keys/values.</jc>
	 * 	Map m = cf.getObject(<js>"MySection/myMap"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map containing string keys and values of lists containing beans.</jc>
	 * 	Map m = cf.getObject(<js>"MySection/myMapOfListsOfBeans"</js>, TreeMap.<jk>class</jk>, String.<jk>class</jk>,
	 * 		List.<jk>class</jk>, MyBean.<jk>class</jk>);
	 * </p>
	 *
	 * <p>
	 * <c>Collection</c> classes are assumed to be followed by zero or one objects indicating the element type.
	 *
	 * <p>
	 * <c>Map</c> classes are assumed to be followed by zero or two meta objects indicating the key and value
	 * types.
	 *
	 * <p>
	 * The array can be arbitrarily long to indicate arbitrarily complex data structures.
	 *
	 * <h5 class='section'>Notes:</h5>
	 * <ul class='spaced-list'>
	 * 	<li>
	 * 		Use the {@link #getObject(String, Class)} method instead if you don't need a parameterized map/collection.
	 * </ul>
	 *
	 * @param key The key.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public <T> T getObject(String key, Type type, Type...args) throws ParseException {
		return getObject(key, (Parser)null, type, args);
	}

	/**
	 * Same as {@link #getObject(String, Type, Type...)} but allows you to specify the parser to use to parse the value.
	 *
	 * @param key The key.
	 * @param parser
	 * 	The parser to use for parsing the object.
	 * 	If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public <T> T getObject(String key, Parser parser, Type type, Type...args) throws ParseException {
		assertFieldNotNull(type, "type");
		return parse(getString(key), parser, type, args);
	}

	/**
	 * Same as {@link #getObject(String, Type, Type...)} except optimized for a non-parameterized class.
	 *
	 * <p>
	 * This is the preferred parse method for simple types since you don't need to cast the results.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 *
	 * 	<jc>// Parse into a string.</jc>
	 * 	String s = cf.getObject(<js>"MySection/mySimpleString"</js>, String.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean.</jc>
	 * 	MyBean b = cf.getObject(<js>"MySection/myBean"</js>, MyBean.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a bean array.</jc>
	 * 	MyBean[] b = cf.getObject(<js>"MySection/myBeanArray"</js>, MyBean[].<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a linked-list of objects.</jc>
	 * 	List l = cf.getObject(<js>"MySection/myList"</js>, LinkedList.<jk>class</jk>);
	 *
	 * 	<jc>// Parse into a map of object keys/values.</jc>
	 * 	Map m = cf.getObject(<js>"MySection/myMap"</js>, TreeMap.<jk>class</jk>);
	 * </p>
	 *
	 * @param <T> The class type of the object being created.
	 * @param key The key.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public <T> T getObject(String key, Class<T> type) throws ParseException {
		return getObject(key, (Parser)null, type);
	}

	/**
	 * Same as {@link #getObject(String, Class)} but allows you to specify the parser to use to parse the value.
	 *
	 * @param <T> The class type of the object being created.
	 * @param key The key.
	 * @param parser
	 * 	The parser to use for parsing the object.
	 * 	If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param type The object type to create.
	 * @return The parsed object.
	 * @throws ParseException Malformed input encountered.
	 * @see BeanSession#getClassMeta(Type,Type...) for argument syntax for maps and collections.
	 */
	public <T> T getObject(String key, Parser parser, Class<T> type) throws ParseException {
		assertFieldNotNull(type, "c");
		return parse(getString(key), parser, type);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 *
	 * <p>
	 * Same as {@link #getObject(String, Class)}, but with a default value.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @param type The class to convert the value to.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public <T> T getObjectWithDefault(String key, T def, Class<T> type) throws ParseException {
		return getObjectWithDefault(key, null, def, type);
	}

	/**
	 * Same as {@link #getObjectWithDefault(String, Object, Class)} but allows you to specify the parser to use to parse
	 * the value.
	 *
	 * @param key The key.
	 * @param parser
	 * 	The parser to use for parsing the object.
	 * 	If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param def The default value if the value does not exist.
	 * @param type The class to convert the value to.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public <T> T getObjectWithDefault(String key, Parser parser, T def, Class<T> type) throws ParseException {
		assertFieldNotNull(type, "c");
		T t = parse(getString(key), parser, type);
		return (t == null ? def : t);
	}

	/**
	 * Gets the entry with the specified key and converts it to the specified value.
	 *
	 * <p>
	 * Same as {@link #getObject(String, Type, Type...)}, but with a default value.
	 *
	 * @param key The key.
	 * @param def The default value if the value does not exist.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public <T> T getObjectWithDefault(String key, T def, Type type, Type...args) throws ParseException {
		return getObjectWithDefault(key, null, def, type, args);
	}

	/**
	 * Same as {@link #getObjectWithDefault(String, Object, Type, Type...)} but allows you to specify the parser to use
	 * to parse the value.
	 *
	 * @param key The key.
	 * @param parser
	 * 	The parser to use for parsing the object.
	 * 	If <jk>null</jk>, then uses the predefined parser on the config file.
	 * @param def The default value if the value does not exist.
	 * @param type
	 * 	The object type to create.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * @param args
	 * 	The type arguments of the class if it's a collection or map.
	 * 	<br>Can be any of the following: {@link ClassMeta}, {@link Class}, {@link ParameterizedType}, {@link GenericArrayType}
	 * 	<br>Ignored if the main type is not a map or collection.
	 * @throws ParseException If parser could not parse the value or if a parser is not registered with this config file.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 */
	public <T> T getObjectWithDefault(String key, Parser parser, T def, Type type, Type...args) throws ParseException {
		assertFieldNotNull(type, "type");
		T t = parse(getString(key), parser, type, args);
		return (t == null ? def : t);
	}

	/**
	 * Convenience method for returning a config entry as an {@link ObjectMap}.
	 *
	 * @param key The key.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException Malformed input encountered.
	 */
	public ObjectMap getObjectMap(String key) throws ParseException {
		return getObject(key, ObjectMap.class);
	}

	/**
	 * Convenience method for returning a config entry as an {@link ObjectMap}.
	 *
	 * @param key The key.
	 * @param def The default value.
	 * @return The value, or the default value if the section or key does not exist.
	 * @throws ParseException Malformed input encountered.
	 */
	public ObjectMap getObjectMap(String key, ObjectMap def) throws ParseException {
		return getObjectWithDefault(key, def, ObjectMap.class);
	}

	/**
	 * Convenience method for returning a config entry as an {@link ObjectList}.
	 *
	 * @param key The key.
	 * @return The value, or <jk>null</jk> if the section or key does not exist.
	 * @throws ParseException Malformed input encountered.
	 */
	public ObjectList getObjectList(String key) throws ParseException {
		return getObject(key, ObjectList.class);
	}

	/**
	 * Convenience method for returning a config entry as an {@link ObjectList}.
	 *
	 * @param key The key.
	 * @param def The default value.
	 * @return The value, or the default value if the section or key does not exist.
	 * @throws ParseException Malformed input encountered.
	 */
	public ObjectList getObjectList(String key, ObjectList def) throws ParseException {
		return getObjectWithDefault(key, def, ObjectList.class);
	}

	/**
	 * Returns the keys of the entries in the specified section.
	 *
	 * @param section
	 * 	The section name to write from.
	 * 	<br>If empty, refers to the default section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return
	 * 	An unmodifiable set of keys, or an empty set if the section doesn't exist.
	 */
	public Set<String> getKeys(String section) {
		return configMap.getKeys(section(section));
	}

	/**
	 * Copies the entries in a section to the specified bean by calling the public setters on that bean.
	 *
	 * @param section
	 * 	The section name to write from.
	 * 	<br>If empty, refers to the default section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param bean The bean to set the properties on.
	 * @param ignoreUnknownProperties
	 * 	If <jk>true</jk>, don't throw an {@link IllegalArgumentException} if this section contains a key that doesn't
	 * 	correspond to a setter method.
	 * @return An object map of the changes made to the bean.
	 * @throws ParseException If parser was not set on this config file or invalid properties were found in the section.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config writeProperties(String section, Object bean, boolean ignoreUnknownProperties) throws ParseException {
		checkWrite();
		assertFieldNotNull(bean, "bean");
		section = section(section);

		Set<String> keys = configMap.getKeys(section);
		if (keys == null)
			throw new IllegalArgumentException("Section '"+section+"' not found in configuration.");

		BeanMap<?> bm = beanSession.toBeanMap(bean);
		for (String k : keys) {
			BeanPropertyMeta bpm = bm.getPropertyMeta(k);
			if (bpm == null) {
				if (! ignoreUnknownProperties)
					throw new ParseException("Unknown property ''{0}'' encountered in configuration section ''{1}''.", k, section);
			} else {
				bm.put(k, getObject(section + '/' + k, bpm.getClassMeta().getInnerClass()));
			}
		}

		return this;
	}

	/**
	 * Shortcut for calling <code>getSectionAsBean(sectionName, c, <jk>false</jk>)</code>.
	 *
	 * @param section
	 * 	The section name to write from.
	 * 	<br>If empty, refers to the default section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param c The bean class to create.
	 * @return A new bean instance.
	 * @throws ParseException Malformed input encountered.
	 */
	public <T> T getSectionAsBean(String section, Class<T> c) throws ParseException {
		return getSectionAsBean(section, c, false);
	}

	/**
	 * Converts this config file section to the specified bean instance.
	 *
	 * <p>
	 * Key/value pairs in the config file section get copied as bean property values to the specified bean class.
	 *
	 * <h5 class='figure'>Example config file</h5>
	 * <p class='bcode w800'>
	 * 	<cs>[MyAddress]</cs>
	 * 	<ck>name</ck> = <cv>John Smith</cv>
	 * 	<ck>street</ck> = <cv>123 Main Street</cv>
	 * 	<ck>city</ck> = <cv>Anywhere</cv>
	 * 	<ck>state</ck> = <cv>NY</cv>
	 * 	<ck>zip</ck> = <cv>12345</cv>
	 * </p>
	 *
	 * <h5 class='figure'>Example bean</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public class</jk> Address {
	 * 		public String name, street, city;
	 * 		public StateEnum state;
	 * 		public int zip;
	 * 	}
	 * </p>
	 *
	 * <h5 class='figure'>Example usage</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 * 	Address myAddress = cf.getSectionAsBean(<js>"MySection"</js>, Address.<jk>class</jk>);
	 * </p>
	 *
	 * @param section
	 * 	The section name to write from.
	 * 	<br>If empty, refers to the default section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param c The bean class to create.
	 * @param ignoreUnknownProperties
	 * 	If <jk>false</jk>, throws a {@link ParseException} if the section contains an entry that isn't a bean property
	 * 	name.
	 * @return A new bean instance, or <jk>null</jk> if the section doesn't exist.
	 * @throws ParseException Unknown property was encountered in section.
	 */
	public <T> T getSectionAsBean(String section, Class<T> c, boolean ignoreUnknownProperties) throws ParseException {
		assertFieldNotNull(c, "c");
		section = section(section);

		if (! configMap.hasSection(section))
			return null;

		Set<String> keys = configMap.getKeys(section);

		BeanMap<T> bm = beanSession.newBeanMap(c);
		for (String k : keys) {
			BeanPropertyMeta bpm = bm.getPropertyMeta(k);
			if (bpm == null) {
				if (! ignoreUnknownProperties)
					throw new ParseException("Unknown property ''{0}'' encountered in configuration section ''{1}''.", k, section);
			} else {
				bm.put(k, getObject(section + '/' + k, bpm.getClassMeta().getInnerClass()));
			}
		}

		return bm.getBean();
	}

	/**
	 * Returns a section of this config copied into an {@link ObjectMap}.
	 *
	 * @param section
	 * 	The section name to write from.
	 * 	<br>If empty, refers to the default section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return A new {@link ObjectMap}, or <jk>null</jk> if the section doesn't exist.
	 * @throws ParseException Malformed input encountered.
	 */
	public ObjectMap getSectionAsMap(String section) throws ParseException {
		section = section(section);

		if (! configMap.hasSection(section))
			return null;

		Set<String> keys = configMap.getKeys(section);

		ObjectMap om = new ObjectMap();
		for (String k : keys)
			om.put(k, getObject(section + '/' + k, Object.class));
		return om;
	}

	/**
	 * Returns the section names defined in this config.
	 *
	 * @return The section names defined in this config.
	 */
	public Set<String> getSections() {
		return Collections.unmodifiableSet(configMap.getSections());
	}

	/**
	 * Wraps a config file section inside a Java interface so that values in the section can be read and
	 * write using getters and setters.
	 *
	 * <h5 class='figure'>Example config file</h5>
	 * <p class='bcode w800'>
	 * 	<cs>[MySection]</cs>
	 * 	<ck>string</ck> = <cv>foo</cv>
	 * 	<ck>int</ck> = <cv>123</cv>
	 * 	<ck>enum</ck> = <cv>ONE</cv>
	 * 	<ck>bean</ck> = <cv>{foo:'bar',baz:123}</cv>
	 * 	<ck>int3dArray</ck> = <cv>[[[123,null],null],null]</cv>
	 * 	<ck>bean1d3dListMap</ck> = <cv>{key:[[[[{foo:'bar',baz:123}]]]]}</cv>
	 * </p>
	 *
	 * <h5 class='figure'>Example interface</h5>
	 * <p class='bcode w800'>
	 * 	<jk>public interface</jk> MyConfigInterface {
	 *
	 * 		String getString();
	 * 		<jk>void</jk> setString(String x);
	 *
	 * 		<jk>int</jk> getInt();
	 * 		<jk>void</jk> setInt(<jk>int</jk> x);
	 *
	 * 		MyEnum getEnum();
	 * 		<jk>void</jk> setEnum(MyEnum x);
	 *
	 * 		MyBean getBean();
	 * 		<jk>void</jk> setBean(MyBean x);
	 *
	 * 		<jk>int</jk>[][][] getInt3dArray();
	 * 		<jk>void</jk> setInt3dArray(<jk>int</jk>[][][] x);
	 *
	 * 		Map&lt;String,List&lt;MyBean[][][]&gt;&gt; getBean1d3dListMap();
	 * 		<jk>void</jk> setBean1d3dListMap(Map&lt;String,List&lt;MyBean[][][]&gt;&gt; x);
	 * 	}
	 * </p>
	 *
	 * <h5 class='figure'>Example usage</h5>
	 * <p class='bcode w800'>
	 * 	Config cf = Config.<jsm>create</jsm>().name(<js>"MyConfig.cfg"</js>).build();
	 *
	 * 	MyConfigInterface ci = cf.getSectionAsInterface(<js>"MySection"</js>, MyConfigInterface.<jk>class</jk>);
	 *
	 * 	<jk>int</jk> myInt = ci.getInt();
	 *
	 * 	ci.setBean(<jk>new</jk> MyBean());
	 *
	 * 	cf.save();
	 * </p>
	 *
	 * <h5 class='section'>Notes</h5>
	 * <ul class='spaced-list'>
	 * 	<li>Calls to setters when the configuration is read-only will cause {@link UnsupportedOperationException} to be thrown.
	 * </ul>
	 *
	 * @param section
	 * 	The section name to write from.
	 * 	<br>If empty, refers to the default section.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param c The proxy interface class.
	 * @return The proxy interface.
	 */
	@SuppressWarnings("unchecked")
	public <T> T getSectionAsInterface(String section, final Class<T> c) {
		assertFieldNotNull(c, "c");
		final String section2 = section(section);

		if (! c.isInterface())
			throw new IllegalArgumentException("Class '"+c.getName()+"' passed to getSectionAsInterface() is not an interface.");

		InvocationHandler h = new InvocationHandler() {

			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				BeanInfo bi = Introspector.getBeanInfo(c, null);
				for (PropertyDescriptor pd : bi.getPropertyDescriptors()) {
					Method rm = pd.getReadMethod(), wm = pd.getWriteMethod();
					if (method.equals(rm))
						return Config.this.getObject(section2 + '/' + pd.getName(), rm.getGenericReturnType());
					if (method.equals(wm))
						return Config.this.set(section2 + '/' + pd.getName(), args[0]);
				}
				throw new UnsupportedOperationException("Unsupported interface method.  method='" + method + "'");
			}
		};

		return (T)Proxy.newProxyInstance(c.getClassLoader(), new Class[] { c }, h);
	}

	/**
	 * Returns <jk>true</jk> if this section contains the specified key and the key has a non-blank value.
	 *
	 * @param key The key.
	 * @return <jk>true</jk> if this section contains the specified key and the key has a non-blank value.
	 */
	public boolean exists(String key) {
		return isNotEmpty(getString(key, null));
	}

	/**
	 * Creates the specified section if it doesn't exist.
	 *
	 * <p>
	 * Returns the existing section if it already exists.
	 *
	 * @param name
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank for the default section.
	 * @param preLines
	 * 	Optional comment and blank lines to add immediately before the section.
	 * 	<br>If <jk>null</jk>, previous pre-lines will not be replaced.
	 * @return The appended or existing section.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config setSection(String name, List<String> preLines) {
		try {
			return setSection(section(name), preLines, null);
		} catch (SerializeException e) {
			throw new RuntimeException(e);  // Impossible.
		}
	}

	/**
	 * Creates the specified section if it doesn't exist.
	 *
	 * @param name
	 * 	The section name.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank for the default section.
	 * @param preLines
	 * 	Optional comment and blank lines to add immediately before the section.
	 * 	<br>If <jk>null</jk>, previous pre-lines will not be replaced.
	 * @param contents
	 * 	Values to set in the new section.
	 * 	<br>Can be <jk>null</jk>.
	 * @return The appended or existing section.
	 * @throws SerializeException Contents could not be serialized.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config setSection(String name, List<String> preLines, Map<String,Object> contents) throws SerializeException {
		checkWrite();
		configMap.setSection(section(name), preLines);

		if (contents != null)
			for (Map.Entry<String,Object> e : contents.entrySet())
				set(section(name) + '/' + e.getKey(), e.getValue());

		return this;
	}

	/**
	 * Removes the section with the specified name.
	 *
	 * @param name The name of the section to remove
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config removeSection(String name) {
		checkWrite();
		configMap.removeSection(name);
		return this;
	}

	/**
	 * Creates the specified import statement if it doesn't exist.
	 *
	 * @param sectionName
	 * 	The section name where to place the import statement.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank for the default section.
	 * @param importName
	 * 	The import name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @param preLines
	 * 	Optional comment and blank lines to add immediately before the import statement.
	 * 	<br>If <jk>null</jk>, previous pre-lines will not be replaced.
	 * @return The appended or existing import statement.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config setImport(String sectionName, String importName, List<String> preLines) {
		checkWrite();
		configMap.setImport(section(name), importName, preLines);
		return this;
	}

	/**
	 * Removes the import statement with the specified name from the specified section.
	 *
	 * @param sectionName
	 * 	The section name where to place the import statement.
	 * 	<br>Must not be <jk>null</jk>.
	 * 	<br>Use blank for the default section.
	 * @param importName
	 * 	The import name.
	 * 	<br>Must not be <jk>null</jk>.
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config removeImport(String sectionName, String importName) {
		checkWrite();
		configMap.removeImport(sectionName, importName);
		return this;
	}

	/**
	 * Loads the contents of the specified map of maps into this config.
	 *
	 * @param m The maps to load.
	 * @return This object (for method chaining).
	 * @throws SerializeException Value could not be serialized.
	 */
	public Config load(Map<String,Map<String,Object>> m) throws SerializeException {
		if (m != null)
			for (Map.Entry<String,Map<String,Object>> e : m.entrySet()) {
				setSection(e.getKey(), null, e.getValue());
			}
		return this;
	}

	/**
	 * Commit the changes in this config to the store.
	 *
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config commit() throws IOException {
		checkWrite();
		configMap.commit();
		return this;
	}

	/**
	 * Saves this config file to the specified writer as an INI file.
	 *
	 * <p>
	 * The writer will automatically be closed.
	 *
	 * @param w The writer to send the output to.
	 * @return This object (for method chaining).
	 * @throws IOException If a problem occurred trying to send contents to the writer.
	 */
	@Override /* Writable */
	public Writer writeTo(Writer w) throws IOException {
		return configMap.writeTo(w);
	}

	/**
	 * Add a listener to this config to react to modification events.
	 *
	 * <p>
	 * Listeners should be removed using {@link #removeListener(ConfigEventListener)}.
	 *
	 * @param listener The new listener to add.
	 * @return This object (for method chaining).
	 */
	public synchronized Config addListener(ConfigEventListener listener) {
		listeners.add(listener);
		return this;
	}

	/**
	 * Removes a listener from this config.
	 *
	 * @param listener The listener to remove.
	 * @return This object (for method chaining).
	 */
	public synchronized Config removeListener(ConfigEventListener listener) {
		listeners.remove(listener);
		return this;
	}

	/**
	 * Closes this configuration object by unregistering it from the underlying config map.
	 *
	 * @throws IOException Thrown by underlying stream.
	 */
	public void close() throws IOException {
		configMap.unregister(this);
	}

	/**
	 * Overwrites the contents of the config file.
	 *
	 * @param contents The new contents of the config file.
	 * @param synchronous Wait until the change has been persisted before returning this map.
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 * @throws InterruptedException Thread was interrupted.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config load(Reader contents, boolean synchronous) throws IOException, InterruptedException {
		checkWrite();
		configMap.load(IOUtils.read(contents), synchronous);
		return this;
	}

	/**
	 * Overwrites the contents of the config file.
	 *
	 * @param contents The new contents of the config file.
	 * @param synchronous Wait until the change has been persisted before returning this map.
	 * @return This object (for method chaining).
	 * @throws IOException Thrown by underlying stream.
	 * @throws InterruptedException Thread was interrupted.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config load(String contents, boolean synchronous) throws IOException, InterruptedException {
		checkWrite();
		configMap.load(contents, synchronous);
		return this;
	}

	/**
	 * Does a rollback of any changes on this config currently in memory.
	 *
	 * @return This object (for method chaining).
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config rollback() {
		checkWrite();
		configMap.rollback();
		return this;
	}

	/**
	 * Returns the values in this config map as a map of maps.
	 *
	 * <p>
	 * This is considered a snapshot copy of the config map.
	 *
	 * <p>
	 * The returned map is modifiable, but modifications to the returned map are not reflected in the config map.
	 *
	 * @return A copy of this config as a map of maps.
	 */
	@Override /* Context */
	public ObjectMap toMap() {
		return configMap.asMap();
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	ConfigMap getConfigMap() {
		return configMap;
	}

	List<ConfigEventListener> getListeners() {
		return Collections.unmodifiableList(listeners);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Interface methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Unused.
	 */
	@Override /* Context */
	public Session createSession(SessionArgs args) {
		throw new UnsupportedOperationException();
	}

	/**
	 * Unused.
	 */
	@Override /* Context */
	public SessionArgs createDefaultSessionArgs() {
		throw new UnsupportedOperationException();
	}

	@Override /* ConfigEventListener */
	public void onConfigChange(ConfigEvents events) {
		for (ConfigEventListener l : listeners)
			l.onConfigChange(events);
	}

	@Override /* Writable */
	public MediaType getMediaType() {
		return MediaType.PLAIN;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Private methods
	//-----------------------------------------------------------------------------------------------------------------

	private String serialize(Object value, Serializer serializer) throws SerializeException {
		if (value == null)
			return "";
		if (serializer == null)
			serializer = this.serializer;
		Class<?> c = value.getClass();
		if (value instanceof CharSequence)
			return nlIfMl((CharSequence)value);
		if (isSimpleType(c))
			return value.toString();

		if (value instanceof byte[]) {
			String s = null;
			byte[] b = (byte[])value;
			if (binaryFormat == BinaryFormat.HEX)
				s = toHex(b);
			else if (binaryFormat == BinaryFormat.SPACED_HEX)
				s = toSpacedHex(b);
			else
				s = base64Encode(b);
			int l = binaryLineLength;
			if (l <= 0 || s.length() <= l)
				return s;
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < s.length(); i += l)
				sb.append(binaryLineLength > 0 ? "\n" : "").append(s.substring(i, Math.min(s.length(), i + l)));
			return sb.toString();
		}

		String r = null;
		if (multiLineValuesOnSeparateLines)
			r = "\n" + (String)serializer.serialize(value);
		else
			r = (String)serializer.serialize(value);

		if (r.startsWith("'"))
			return r.substring(1, r.length()-1);
		return r;
	}

	private String nlIfMl(CharSequence cs) {
		String s = cs.toString();
		if (s.indexOf('\n') != -1 && multiLineValuesOnSeparateLines)
			return "\n" + s;
		return s;
	}

	@SuppressWarnings({ "unchecked" })
	private <T> T parse(String s, Parser parser, Type type, Type...args) throws ParseException {

		if (isEmpty(s))
			return null;

		if (isSimpleType(type))
			return (T)beanSession.convertToType(s, (Class<?>)type);

		if (type == byte[].class) {
			if (s.indexOf('\n') != -1)
				s = s.replaceAll("\n", "");
			try {
				switch (binaryFormat) {
					case HEX: return (T)fromHex(s);
					case SPACED_HEX: return (T)fromSpacedHex(s);
					default: return (T)base64Decode(s);
				}
			} catch (Exception e) {
				throw new ParseException(e, "Value could not be converted to a byte array.");
			}
		}

		if (parser == null)
			parser = this.parser;

		if (parser instanceof JsonParser) {
			char s1 = firstNonWhitespaceChar(s);
			if (isArray(type) && s1 != '[')
				s = '[' + s + ']';
			else if (s1 != '[' && s1 != '{' && ! "null".equals(s))
				s = '\'' + s + '\'';
		}

		return parser.parse(s, type, args);
	}

	private boolean isSimpleType(Type t) {
		if (! (t instanceof Class))
			return false;
		Class<?> c = (Class<?>)t;
		return (c == String.class || c.isPrimitive() || c.isAssignableFrom(Number.class) || c == Boolean.class || c.isEnum());
	}

	private boolean isArray(Type t) {
		if (! (t instanceof Class))
			return false;
		Class<?> c = (Class<?>)t;
		return (c.isArray());
	}

	private String sname(String key) {
		assertFieldNotNull(key, "key");
		int i = key.indexOf('/');
		if (i == -1)
			return "";
		return key.substring(0, i);
	}

	private String skey(String key) {
		int i = key.indexOf('/');
		if (i == -1)
			return key;
		return key.substring(i+1);
	}

	private String section(String section) {
		assertFieldNotNull(section, "section");
		if (isEmpty(section))
			return "";
		return section;
	}

	private void checkWrite() {
		if (readOnly)
			throw new UnsupportedOperationException("Cannot call this method on a read-only configuration.");
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Other methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* Object */
	public String toString() {
		return configMap.toString();
	}

	@Override /* Object */
	protected void finalize() throws Throwable {
		close();
	}
}
