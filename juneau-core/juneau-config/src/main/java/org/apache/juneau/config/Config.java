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

import static org.apache.juneau.common.internal.ArgUtils.*;
import static org.apache.juneau.common.internal.IOUtils.*;
import static org.apache.juneau.common.internal.StringUtils.*;
import static org.apache.juneau.common.internal.ThrowableUtils.*;
import static org.apache.juneau.internal.CollectionUtils.*;

import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.config.event.*;
import org.apache.juneau.config.internal.*;
import org.apache.juneau.config.mod.*;
import org.apache.juneau.config.store.*;
import org.apache.juneau.config.vars.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.svl.*;
import org.apache.juneau.utils.*;

/**
 * Main configuration API class.
 *
 * <h5 class='section'>Notes:</h5><ul>
 * 	<li class='note'>This class is thread safe and reusable.
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="../../../../index.html#juneau-config">Overview &gt; juneau-config</a>
 * </ul>
 */
public final class Config extends Context implements ConfigEventListener {

	//-----------------------------------------------------------------------------------------------------------------
	// Static
	//-----------------------------------------------------------------------------------------------------------------

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
		List<String> l = list();

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

		Set<File> files = sortedSet(new File(".").listFiles());
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
		if (FileStore.DEFAULT.exists(name))
			return Config.create(name).store(FileStore.DEFAULT).build();
		if (ClasspathStore.DEFAULT.exists(name))
			return Config.create(name).store(ClasspathStore.DEFAULT).build();
		return null;
	}

	/**
	 * Creates a new builder for this object.
	 *
	 * @return A new builder.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Same as {@link #create()} but initializes the builder with the specified config name.
	 *
	 * @param name The configuration name.
	 * @return A new builder.
	 */
	public static Builder create(String name) {
		return new Builder().name(name);
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Builder
	//-------------------------------------------------------------------------------------------------------------------

	/**
	 * Builder class.
	 */
	@FluentSetters
	public static class Builder extends Context.Builder {

		String name;
		ConfigStore store;
		WriterSerializer serializer;
		ReaderParser parser;
		Map<Character,Mod> mods;
		VarResolver varResolver;
		int binaryLineLength;
		BinaryFormat binaryFormat;
		boolean multiLineValuesOnSeparateLines, readOnly;

		/**
		 * Constructor, default settings.
		 */
		protected Builder() {
			super();
			name = env("Config.name", "Configuration.cfg");
			store = FileStore.DEFAULT;
			serializer = Json5Serializer.DEFAULT;
			parser = JsonParser.DEFAULT;
			mods = map();
			mods(XorEncodeMod.INSTANCE);
			varResolver = VarResolver.DEFAULT;
			binaryLineLength = env("Config.binaryLineLength", -1);
			binaryFormat = env("Config.binaryFormat", BinaryFormat.BASE64);
			multiLineValuesOnSeparateLines = env("Config.multiLineValuesOnSeparateLines", false);
			readOnly = env("Config.readOnly", false);
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The bean to copy from.
		 */
		protected Builder(Config copyFrom) {
			super(copyFrom);
			name = copyFrom.name;
			store = copyFrom.store;
			serializer = copyFrom.serializer;
			parser = copyFrom.parser;
			mods = copyOf(copyFrom.mods);
			varResolver = copyFrom.varResolver;
			binaryLineLength = copyFrom.binaryLineLength;
			binaryFormat = copyFrom.binaryFormat;
			multiLineValuesOnSeparateLines = copyFrom.multiLineValuesOnSeparateLines;
			readOnly = copyFrom.readOnly;
		}

		/**
		 * Copy constructor.
		 *
		 * @param copyFrom The builder to copy from.
		 */
		protected Builder(Builder copyFrom) {
			super(copyFrom);
			name = copyFrom.name;
			store = copyFrom.store;
			serializer = copyFrom.serializer;
			parser = copyFrom.parser;
			mods = copyOf(copyFrom.mods);
			varResolver = copyFrom.varResolver;
			binaryLineLength = copyFrom.binaryLineLength;
			binaryFormat = copyFrom.binaryFormat;
			multiLineValuesOnSeparateLines = copyFrom.multiLineValuesOnSeparateLines;
			readOnly = copyFrom.readOnly;
		}

		@Override /* Context.Builder */
		public Builder copy() {
			return new Builder(this);
		}

		@Override /* Context.Builder */
		public Config build() {
			return build(Config.class);
		}

		//-----------------------------------------------------------------------------------------------------------------
		// Properties
		//-----------------------------------------------------------------------------------------------------------------

		/**
		 * Configuration name.
		 *
		 * <p>
		 * Specifies the configuration name.
		 * <br>This is typically the configuration file name, although
		 * the name can be anything identifiable by the {@link ConfigStore} used for retrieving and storing the configuration.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"Config.name"
		 * 		<li>Environment variable <js>"CONFIG_NAME"
		 * 		<li><js>"Configuration.cfg"</js>
		 * 	</ul>
		 * @return This object.
		 */
		public Builder name(String value) {
			name = value;
			return this;
		}

		/**
		 * Configuration store.
		 *
		 * <p>
		 * The configuration store used for retrieving and storing configurations.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link FileStore#DEFAULT}.
		 * @return This object.
		 */
		public Builder store(ConfigStore value) {
			store = value;
			return this;
		}

		/**
		 * Configuration store.
		 *
		 * <p>
		 * Convenience method for calling <code>store(ConfigMemoryStore.<jsf>DEFAULT</jsf>)</code>.
		 *
		 * @return This object.
		 */
		public Builder memStore() {
			store = MemoryStore.DEFAULT;
			return this;
		}

		/**
		 * POJO serializer.
		 *
		 * <p>
		 * The serializer to use for serializing POJO values.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link Json5Serializer#DEFAULT}
		 * @return This object.
		 */
		public Builder serializer(WriterSerializer value) {
			serializer = value;
			return this;
		}

		/**
		 * POJO parser.
		 *
		 * <p>
		 * The parser to use for parsing values to POJOs.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link JsonParser#DEFAULT}.
		 * @return This object.
		 */
		public Builder parser(ReaderParser value) {
			parser = value;
			return this;
		}

		/**
		 * Adds a value modifier.
		 *
		 * <p>
		 * Modifiers are used to modify entry value before being persisted.
		 *
		 * @param values
		 * 	The mods to apply to this config.
		 * @return This object.
		 */
		public Builder mods(Mod...values) {
			for (Mod value : values)
				mods.put(value.getId(), value);
			return this;
		}

		/**
		 * SVL variable resolver.
		 *
		 * <p>
		 * The resolver to use for resolving SVL variables.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is {@link VarResolver#DEFAULT}.
		 * @return This object.
		 */
		public Builder varResolver(VarResolver value) {
			varResolver = value;
			return this;
		}

		/**
		 * Binary value line length.
		 *
		 * <p>
		 * When serializing binary values, lines will be split after this many characters.
		 * <br>Use <c>-1</c> to represent no line splitting.
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"Config.binaryLineLength"
		 * 		<li>Environment variable <js>"CONFIG_BINARYLINELENGTH"
		 * 		<li><c>-1</c>
		 * 	</ul>
		 * @return This object.
		 */
		public Builder binaryLineLength(int value) {
			binaryLineLength = value;
			return this;
		}

		/**
		 * Binary value format.
		 *
		 * <p>
		 * The format to use when persisting byte arrays.
		 *
		 * <ul class='values'>
		 * 	<li>{@link BinaryFormat#BASE64} - BASE64-encoded string.
		 * 	<li>{@link BinaryFormat#HEX} - Hexadecimal.
		 * 	<li>{@link BinaryFormat#SPACED_HEX} - Hexadecimal with spaces between bytes.
		 * </ul>
		 *
		 * @param value
		 * 	The new value for this property.
		 * 	<br>The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"Config.binaryFormat"
		 * 		<li>Environment variable <js>"CONFIG_BINARYFORMAT"
		 * 		<li>{@link BinaryFormat#BASE64}
		 * 	</ul>
		 * @return This object.
		 */
		public Builder binaryFormat(BinaryFormat value) {
			binaryFormat = value;
			return this;
		}

		/**
		 * Multi-line values on separate lines.
		 *
		 * <p>
		 * When enabled, multi-line values will always be placed on a separate line from the key.
		 *
		 * <p>
		 * The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"Config.multiLineValuesOnSeparateLine"
		 * 		<li>Environment variable <js>"CONFIG_MULTILINEVALUESONSEPARATELINE"
		 * 		<li><jk>false</jk>
		 * 	</ul>
		 *
		 * @return This object.
		 */
		public Builder multiLineValuesOnSeparateLines() {
			multiLineValuesOnSeparateLines = true;
			return this;
		}

		/**
		 * Read-only mode.
		 *
		 * <p>
		 * When enabled, attempts to call any setters on this object will throw an {@link UnsupportedOperationException}.
		 *
		 * <p>
		 * 	The default is the first value found:
		 * 	<ul>
		 * 		<li>System property <js>"Config.readOnly"
		 * 		<li>Environment variable <js>"CONFIG_READONLY"
		 * 		<li><jk>false</jk>
		 * 	</ul>
		 *
		 * @return This object.
		 */
		public Builder readOnly() {
			readOnly = true;
			return this;
		}

		// <FluentSetters>

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder annotations(Annotation...values) {
			super.annotations(values);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder apply(AnnotationWorkList work) {
			super.apply(work);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(java.lang.Class<?>...fromClasses) {
			super.applyAnnotations(fromClasses);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder applyAnnotations(Method...fromMethods) {
			super.applyAnnotations(fromMethods);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder cache(Cache<HashKey,? extends org.apache.juneau.Context> value) {
			super.cache(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug() {
			super.debug();
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder debug(boolean value) {
			super.debug(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder impl(Context value) {
			super.impl(value);
			return this;
		}

		@Override /* GENERATED - org.apache.juneau.Context.Builder */
		public Builder type(Class<? extends org.apache.juneau.Context> value) {
			super.type(value);
			return this;
		}

		// </FluentSetters>
	}

	//-------------------------------------------------------------------------------------------------------------------
	// Instance
	//-------------------------------------------------------------------------------------------------------------------

	final String name;
	final ConfigStore store;
	final WriterSerializer serializer;
	final ReaderParser parser;
	final Map<Character,Mod> mods;
	final VarResolver varResolver;
	final int binaryLineLength;
	final BinaryFormat binaryFormat;
	final boolean multiLineValuesOnSeparateLines, readOnly;
	final BeanSession beanSession;
	final VarResolverSession varSession;

	private final ConfigMap configMap;
	private final List<ConfigEventListener> listeners = synced(linkedList());


	@Override /* Context */
	public Builder copy() {
		return new Builder(this);
	}

	/**
	 * Constructor.
	 *
	 * @param builder The builder for this object.
	 * @throws IOException Thrown by underlying stream.
	 */
	public Config(Builder builder) throws IOException {
		super(builder);

		name = builder.name;
		store = builder.store;
		configMap = store.getMap(name);
		configMap.register(this);
		serializer = builder.serializer;
		parser = builder.parser;
		beanSession = parser.getBeanContext().getSession();
		mods = unmodifiable(copyOf(builder.mods));
		varResolver = builder.varResolver;
		varSession = varResolver
			.copy()
			.vars(ConfigVar.class)
			.bean(Config.class, this)
			.build()
			.createSession();
		binaryLineLength = builder.binaryLineLength;
		binaryFormat = builder.binaryFormat;
		multiLineValuesOnSeparateLines = builder.multiLineValuesOnSeparateLines;
		readOnly = builder.readOnly;
	}

	Config(Config copyFrom, VarResolverSession varSession) {
		super(copyFrom);
		name = copyFrom.name;
		store = copyFrom.store;
		configMap = copyFrom.configMap;
		configMap.register(this);
		serializer = copyFrom.serializer;
		parser = copyFrom.parser;
		mods = copyFrom.mods;
		varResolver = copyFrom.varResolver;
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
	private String getRaw(String key) {

		String sname = sname(key);
		String skey = skey(key);

		ConfigMapEntry ce = configMap.getEntry(sname, skey);

		if (ce == null)
			return null;

		return removeMods(ce.getModifiers(), ce.getValue());
	}

	String applyMods(String mods, String x) {
		if (mods != null && x != null)
			for (int i = 0; i < mods.length(); i++)
				x = getMod(mods.charAt(i)).doApply(x);
		return x;
	}

	String removeMods(String mods, String x) {
		if (mods != null && x != null)
			for (int i = mods.length()-1; i > -1; i--)
				x = getMod(mods.charAt(i)).doRemove(x);
		return x;
	}

	Mod getMod(char id) {
		Mod x = mods.get(id);
		return x == null ? Mod.NO_OP : x;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Utility methods
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Takes the settings defined in this configuration and sets them as system properties.
	 *
	 * @return This object.
	 */
	public Config setSystemProperties() {
		for (String section : getSectionNames()) {
			for (String key : getKeys(section)) {
				String k = (section.isEmpty() ? key : section + '/' + key);
				System.setProperty(k, getRaw(k));
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
	 * @return This object.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config set(String key, String value) {
		checkWrite();
		assertArgNotNull("key", key);
		String sname = sname(key);
		String skey = skey(key);

		ConfigMapEntry ce = configMap.getEntry(sname, skey);
		if (ce == null && value == null)
			return this;

		String s = applyMods(ce == null ? null : ce.getModifiers(), stringify(value));

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
	public Config set(String key, Object value, Serializer serializer, String modifiers, String comment, List<String> preLines) throws SerializeException {
		checkWrite();
		assertArgNotNull("key", key);
		String sname = sname(key);
		String skey = skey(key);
		modifiers = nullIfEmpty(modifiers);

		String s = applyMods(modifiers, serialize(value, serializer));

		configMap.setEntry(sname, skey, s, modifiers, comment, preLines);
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
	 * @return This object.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config applyMods() {
		checkWrite();
		for (String section : configMap.getSections()) {
			for (String key : configMap.getKeys(section)) {
				ConfigMapEntry ce = configMap.getEntry(section, key);
				if (ce.getModifiers() != null) {
					String mods = ce.getModifiers();
					String value = ce.getValue();
					for (int i = 0; i < mods.length(); i++) {
						Mod mod = getMod(mods.charAt(i));
						if (! mod.isApplied(value)) {
							configMap.setEntry(section, key, mod.apply(value), null, null, null);
						}
					}
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
	 * <p>
	 * If entry does not exist, returns an empty {@link Entry} object.
	 *
	 * @param key The key.
	 * @return The entry bean, never <jk>null</jk>.
	 */
	public Entry get(String key) {
		return new Entry(this, configMap, sname(key), skey(key));
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
	 * <p>
	 * If entry does not exist, returns <jk>null</jk>.
	 *
	 * <h5 class='section'>Notes:</h5><ul>
	 * 	<li class='note'>This method is equivalent to calling <c>get(<jv>key</jv>).orElse(<jk>null</jk>);</c>.
	 * </ul>
	 *
	 * @param key The key.
	 * @return The entry value, or <jk>null</jk> if it doesn't exist.
	 */
	public String getString(String key) {
		return new Entry(this, configMap, sname(key), skey(key)).orElse(null);
	}

	/**
	 * Gets the section with the specified name.
	 *
	 * <p>
	 * If section does not exist, returns an empty {@link Section} object.
	 *
	 * @param name The section name.  <jk>null</jk> and blank refer to the default section.
	 * @return The section bean, never <jk>null</jk>.
	 */
	public Section getSection(String name) {
		return new Section(this, configMap, emptyIfNull(name));
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
	 * Returns the section names defined in this config.
	 *
	 * @return The section names defined in this config.
	 */
	public Set<String> getSectionNames() {
		return unmodifiable(configMap.getSections());
	}

	/**
	 * Returns <jk>true</jk> if this section contains the specified key and the key has a non-blank value.
	 *
	 * @param key The key.
	 * @return <jk>true</jk> if this section contains the specified key and the key has a non-blank value.
	 */
	public boolean exists(String key) {
		return isNotEmpty(get(key).as(String.class).orElse(null));
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
			throw asRuntimeException(e);  // Impossible.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
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
	 * @return This object.
	 * @throws IOException If a problem occurred trying to send contents to the writer.
	 */
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
	 * @return This object.
	 */
	public synchronized Config addListener(ConfigEventListener listener) {
		listeners.add(listener);
		return this;
	}

	/**
	 * Removes a listener from this config.
	 *
	 * @param listener The listener to remove.
	 * @return This object.
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
	 * @return This object.
	 * @throws IOException Thrown by underlying stream.
	 * @throws InterruptedException Thread was interrupted.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config load(Reader contents, boolean synchronous) throws IOException, InterruptedException {
		checkWrite();
		configMap.load(read(contents), synchronous);
		return this;
	}

	/**
	 * Overwrites the contents of the config file.
	 *
	 * @param contents The new contents of the config file.
	 * @param synchronous Wait until the change has been persisted before returning this map.
	 * @return This object.
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
	 * @return This object.
	 * @throws UnsupportedOperationException If configuration is read only.
	 */
	public Config rollback() {
		checkWrite();
		configMap.rollback();
		return this;
	}

	/**
	 * Returns the contents of this config as a simple map.
	 *
	 * @return The contents of this config as a simple map.
	 */
	public JsonMap toMap() {
		return configMap.asMap();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Test methods
	//-----------------------------------------------------------------------------------------------------------------

	ConfigMap getConfigMap() {
		return configMap;
	}

	List<ConfigEventListener> getListeners() {
		return unmodifiable(listeners);
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Interface methods
	//-----------------------------------------------------------------------------------------------------------------

	@Override /* ConfigEventListener */
	public synchronized void onConfigChange(ConfigEvents events) {
		for (ConfigEventListener l : listeners)
			l.onConfigChange(events);
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

	private boolean isSimpleType(Type t) {
		if (! (t instanceof Class))
			return false;
		Class<?> c = (Class<?>)t;
		return (c == String.class || c.isPrimitive() || c.isAssignableFrom(Number.class) || c == Boolean.class || c.isEnum());
	}

	private String sname(String key) {
		assertArgNotNull("key", key);
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
		assertArgNotNull("section", section);
		if (isEmpty(section))
			return "";
		return section;
	}

	void checkWrite() {
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
