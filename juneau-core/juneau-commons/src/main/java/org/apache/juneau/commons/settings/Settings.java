/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.juneau.commons.settings;

import static org.apache.juneau.commons.reflect.ReflectionUtils.*;
import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.reflect.*;

/**
 * Encapsulates Java system properties with support for global and per-thread overrides for unit testing.
 *
 * <p>
 * This class provides a thread-safe way to access system properties that can be overridden at both the global level
 * and per-thread level, making it useful for unit tests that need to temporarily change system property
 * values without affecting other tests or threads.
 *
 * <p>
 * Settings instances are created using the {@link Builder} pattern. Use {@link #create()} to create a new builder,
 * or {@link #get()} to get the singleton instance (which is created using default builder settings).
 *
 * <h5 class='section'>Lookup Order:</h5>
 * <p>
 * When retrieving a property value, the lookup order is:
 * <ol>
 * 	<li>Per-thread store (if set via {@link #setLocal(String, String)})
 * 	<li>Global store (if set via {@link #setGlobal(String, String)})
 * 	<li>Sources in reverse order (last source added via {@link Builder#addSource(SettingSource)} is checked first)
 * 	<li>System property source (default, always second-to-last)
 * 	<li>System environment variable source (default, always last)
 * </ol>
 *
 * <h5 class='section'>Sources vs Stores:</h5>
 * <ul class='spaced-list'>
 * 	<li><b>Sources</b> ({@link SettingSource}) - Provide read-only access to property values. Examples: {@link FunctionalSource}
 * 	<li><b>Stores</b> ({@link SettingStore}) - Provide read/write access to property values. Examples: {@link MapStore}, {@link FunctionalStore}
 * 	<li>Stores can be used as sources (they extend {@link SettingSource}), so you can add stores via {@link Builder#addSource(SettingSource)}
 * </ul>
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>System property access - read Java system properties with type conversion via {@link StringSetting}
 * 	<li>Global overrides - override system properties globally for all threads (stored in a {@link SettingStore})
 * 	<li>Per-thread overrides - override system properties for specific threads (stored in a per-thread {@link SettingStore})
 * 	<li>Custom sources - add arbitrary property sources (e.g., Spring properties, environment variables, config files) via the {@link Builder}
 * 	<li>Disable override support - system property to prevent new global overrides from being set
 * 	<li>Type-safe accessors - type conversion methods on {@link StringSetting} for common types: Integer, Long, Boolean, Double, Float, File, Path, URI, Charset
 * 	<li>Resettable suppliers - settings are returned as {@link StringSetting} instances that can be reset to force recomputation
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Get a system property as a StringSetting (using singleton instance)</jc>
 * 	StringSetting <jv>setting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.property"</js>);
 * 	String <jv>value</jv> = <jv>setting</jv>.get();  <jc>// Get the string value</jc>
 *
 * 	<jc>// Get with type conversion using StringSetting methods</jc>
 * 	Setting&lt;Integer&gt; <jv>intSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.int.property"</js>).asInteger();
 * 	Setting&lt;Long&gt; <jv>longSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.long.property"</js>).asLong();
 * 	Setting&lt;Boolean&gt; <jv>boolSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.bool.property"</js>).asBoolean();
 * 	Setting&lt;Double&gt; <jv>doubleSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.double.property"</js>).asDouble();
 * 	Setting&lt;Float&gt; <jv>floatSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.float.property"</js>).asFloat();
 * 	Setting&lt;File&gt; <jv>fileSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.file.property"</js>).asFile();
 * 	Setting&lt;Path&gt; <jv>pathSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.path.property"</js>).asPath();
 * 	Setting&lt;URI&gt; <jv>uriSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.uri.property"</js>).asURI();
 * 	Setting&lt;Charset&gt; <jv>charsetSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.charset.property"</js>).asCharset();
 *
 * 	<jc>// Use custom type conversion</jc>
 * 	Setting&lt;MyCustomType&gt; <jv>customSetting</jv> = Settings.<jsf>get</jsf>().get(<js>"my.custom.property"</js>).asType(MyCustomType.<jk>class</jk>);
 *
 * 	<jc>// Reset a setting to force recomputation</jc>
 * 	<jv>setting</jv>.reset();
 *
 * 	<jc>// Override for current thread (useful in unit tests)</jc>
 * 	Settings.<jsf>get</jsf>().setLocal(<js>"my.property"</js>, <js>"test-value"</js>);
 * 	<jc>// ... test code that uses the override ...</jc>
 * 	Settings.<jsf>get</jsf>().unsetLocal(<js>"my.property"</js>);  <jc>// Remove specific override</jc>
 * 	<jc>// OR</jc>
 * 	Settings.<jsf>get</jsf>().clearLocal();  <jc>// Clear all thread-local overrides</jc>
 *
 * 	<jc>// Set global override (applies to all threads)</jc>
 * 	Settings.<jsf>get</jsf>().setGlobal(<js>"my.property"</js>, <js>"global-value"</js>);
 * 	<jc>// ... code that uses the global override ...</jc>
 * 	Settings.<jsf>get</jsf>().unsetGlobal(<js>"my.property"</js>);  <jc>// Remove specific override</jc>
 * 	<jc>// OR</jc>
 * 	Settings.<jsf>get</jsf>().clearGlobal();  <jc>// Clear all global overrides</jc>
 *
 * 	<jc>// Create a custom Settings instance with custom sources (e.g., Spring properties)</jc>
 * 	MapStore <jv>springSource</jv> = <jk>new</jk> MapStore();
 * 	<jv>springSource</jv>.set(<js>"spring.datasource.url"</js>, <js>"jdbc:postgresql://localhost/db"</js>);
 * 	Settings <jv>custom</jv> = Settings.<jsf>create</jsf>()
 * 		.addSource(<jv>springSource</jv>)
 * 		.addSource(FunctionalSource.<jsf>of</jsf>(System::getProperty))
 * 		.build();
 * </p>
 *
 * <h5 class='section'>System Properties:</h5>
 * <ul class='spaced-list'>
 * 	<li><c>juneau.settings.disableGlobal</c> (system property) or <c>JUNEAU_SETTINGS_DISABLEGLOBAL</c> (system env) - If set to <c>true</c>, prevents new global overrides
 * 		from being set via {@link #setGlobal(String, String)}. Existing global overrides will still be
 * 		returned by {@link #get(String)} until explicitly removed.
 * 		<p class='bnote'>
 * 			Note: This property is read once at class initialization time when creating the singleton instance
 * 			and cannot be changed at runtime. Changing the system property after the class has been loaded will
 * 			have no effect on the singleton instance. However, you can create custom Settings instances using
 * 			{@link #create()} that ignore this property.
 * 		</p>
 * </ul>
 */
@SuppressWarnings("java:S115") // Constants use UPPER_snakeCase convention (e.g., ARG_sources, MSG_globalDisabled)
public class Settings {

	// Argument name constants for assertArgNoNulls
	private static final String ARG_sources = "sources";

	// Argument name constants for assertArgNotNull
	private static final String ARG_name = "name";
	private static final String ARG_def = "def";
	private static final String ARG_s = "s";
	private static final String ARG_c = "c";
	private static final String ARG_supplier = "supplier";
	private static final String ARG_source = "source";
	private static final String ARG_type = "type";
	private static final String ARG_function = "function";

	/**
	 * System property source that delegates to {@link System#getProperty(String)}.
	 */
	public static final SettingSource SYSTEM_PROPERTY_SOURCE = FunctionalSource.of(System::getProperty);

	private static final Set<String> FROM_STRING_METHOD_NAMES = new LinkedHashSet<>(Arrays.asList("fromString", "parse", "forName", "valueOf"));

	/**
	 * System environment variable source that delegates to {@link System#getenv(String)}.
	 */
	public static final SettingSource SYSTEM_ENV_SOURCE = FunctionalSource.of(System::getenv);

	private static final String DISABLE_GLOBAL_PROP = "juneau.settings.disableGlobal";
	private static final String MSG_globalDisabled = "Global settings not enabled";
	private static final String MSG_localDisabled = "Local settings not enabled";

	/**
	 * Returns properties for this Settings object itself.
	 * Note that these are initialized at startup and not changeable through System.setProperty().
	 */
	private static final Optional<String> initProperty(String property) {
		var v = SYSTEM_PROPERTY_SOURCE.get(property);
		if (v != null)
			return v;  // Not testable
		v = SYSTEM_ENV_SOURCE.get(property.replace('.', '_').toUpperCase());
		if (v != null)
			return v;  // Not testable
		return opte();
	}

	/**
	 * Creates a new builder for constructing a Settings instance.
	 *
	 * <p>
	 * This method provides a convenient way to create custom Settings instances with specific
	 * configuration. The builder allows you to configure stores, sources, and other settings
	 * before building the final Settings instance.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Create a custom Settings instance</jc>
	 * 	Settings <jv>custom</jv> = Settings.<jsf>create</jsf>()
	 * 		.globalStore(() -&gt; <jk>new</jk> MapStore())
	 * 		.localStore(() -&gt; <jk>new</jk> MapStore())
	 * 		.addSource(FunctionalSource.<jsf>of</jsf>(System::getProperty))
	 * 		.build();
	 * </p>
	 *
	 * @return A new Builder instance.
	 */
	public static Builder create() {
		return new Builder();
	}

	/**
	 * Builder for creating Settings instances.
	 */
	public static class Builder {
		private Supplier<SettingStore> globalStoreSupplier = MapStore::new;
		private Supplier<SettingStore> localStoreSupplier = MapStore::new;
		private final List<SettingSource> sources = new ArrayList<>();
		private final Map<Class<?>,Function<String,?>> customTypeFunctions = new IdentityHashMap<>();

		/**
		 * Sets the supplier for the global store.
		 *
		 * @param supplier The supplier for the global store.  Must not be <c>null</c>.  Can supply null to disable global store.
		 * @return This builder for method chaining.
		 */
		public Builder globalStore(OptionalSupplier<SettingStore> supplier) {
			this.globalStoreSupplier = assertArgNotNull(ARG_supplier, supplier);
			return this;
		}

		/**
		 * Sets the supplier for the local (per-thread) store.
		 *
		 * @param supplier The supplier for the local store. Must not be <c>null</c>.
		 * @return This builder for method chaining.
		 */
		public Builder localStore(OptionalSupplier<SettingStore> supplier) {
			this.localStoreSupplier = assertArgNotNull(ARG_supplier, supplier);
			return this;
		}

		/**
		 * Sets the sources list, replacing any existing sources.
		 *
		 * @param sources The sources to set. Must not be <c>null</c> or contain <c>null</c> elements.
		 * @return This builder for method chaining.
		 */
		@SafeVarargs
		public final Builder setSources(SettingSource...sources) {
			assertArgNoNulls(ARG_sources, sources);
			this.sources.clear();
			for (var source : sources) {
				this.sources.add(source);
			}
			return this;
		}

		/**
		 * Adds a source to the sources list.
		 *
		 * @param source The source to add. Must not be <c>null</c>.
		 * @return This builder for method chaining.
		 */
		public Builder addSource(SettingSource source) {
			assertArgNotNull(ARG_source, source);
			this.sources.add(source);
			return this;
		}

		/**
		 * Adds a functional source to the sources list.
		 *
		 * @param source The functional source to add. Must not be <c>null</c>.
		 * @return This builder for method chaining.
		 */
		public Builder addSource(FunctionalSource source) {
			return addSource((SettingSource)source);
		}

		/**
		 * Registers a custom type conversion function for the specified type.
		 *
		 * <p>
		 * This allows you to add support for converting string values to custom types when using
		 * {@link Settings#get(String, Object)}. The function will be used to convert string values
		 * to the specified type.
		 *
		 * <h5 class='section'>Example:</h5>
		 * <p class='bjava'>
		 * 	<jc>// Register a custom converter for a custom type</jc>
		 * 	Settings <jv>custom</jv> = Settings.<jsf>create</jsf>()
		 * 		.addTypeFunction(Integer.<jk>class</jk>, Integer::valueOf)
		 * 		.addTypeFunction(MyCustomType.<jk>class</jk>, MyCustomType::fromString)
		 * 		.build();
		 *
		 * 	<jc>// Now you can use get() with these types</jc>
		 * 	Integer <jv>intValue</jv> = <jv>custom</jv>.get(<js>"my.int.property"</js>, 0);
		 * 	MyCustomType <jv>customValue</jv> = <jv>custom</jv>.get(<js>"my.custom.property"</js>, MyCustomType.DEFAULT);
		 * </p>
		 *
		 * @param <T> The type to register a converter for.
		 * @param type The class type to register a converter for. Must not be <c>null</c>.
		 * @param function The function that converts a string to the specified type. Must not be <c>null</c>.
		 * @return This builder for method chaining.
		 */
		public <T> Builder addTypeFunction(Class<T> type, Function<String,T> function) {
			assertArgNotNull(ARG_type, type);
			assertArgNotNull(ARG_function, function);
			customTypeFunctions.put(type, function);
			return this;
		}

		/**
		 * Builds a Settings instance from this builder.
		 *
		 * @return A new Settings instance.
		 */
		public Settings build() {
			return new Settings(this);
		}
	}

	private static final Settings INSTANCE = new Builder()
		.globalStore(initProperty(DISABLE_GLOBAL_PROP).map(Boolean::valueOf).orElse(false) ? () -> null : MapStore::new)
		.setSources(SYSTEM_ENV_SOURCE, SYSTEM_PROPERTY_SOURCE)
		.build();

	/**
	 * Returns the singleton instance of Settings.
	 *
	 * @return The singleton Settings instance.
	 */
	public static Settings get() {
		return INSTANCE;
	}

	private final ResettableSupplier<SettingStore> globalStore;
	@SuppressWarnings("java:S5164") // Cleanup method provided: cleanup()
	private final ThreadLocal<SettingStore> localStore;
	private final List<SettingSource> sources;
	private final Map<Class<?>,Function<String,?>> toTypeFunctions;

	/**
	 * Constructor.
	 */
	private Settings(Builder builder) {
		this.globalStore = memr(builder.globalStoreSupplier);
		this.localStore = ThreadLocal.withInitial(builder.localStoreSupplier);
		this.sources = new CopyOnWriteArrayList<>(builder.sources);
		this.toTypeFunctions = new ConcurrentHashMap<>(builder.customTypeFunctions);
	}

	/**
	 * Returns a {@link StringSetting} for the specified system property.
	 *
	 * <p>
	 * The returned {@link StringSetting} is a resettable supplier that caches the lookup result.
	 * Use the {@link StringSetting#asInteger()}, {@link StringSetting#asBoolean()}, etc. methods
	 * to convert to different types.
	 *
	 * <p>
	 * The lookup order is:
	 * <ol>
	 * 	<li>Per-thread override (if set via {@link #setLocal(String, String)})
	 * 	<li>Global override (if set via {@link #setGlobal(String, String)})
	 * 	<li>Sources in reverse order (last source added via {@link Builder#addSource(SettingSource)} is checked first)
	 * 	<li>System property source (default, always second-to-last)
	 * 	<li>System environment variable source (default, always last)
	 * </ol>
	 *
	 * @param name The property name. Must not be <jk>null</jk>.
	 * @return A {@link StringSetting} that provides the property value, or <jk>null</jk> if not found.
	 */
	public StringSetting get(String name) {
		assertArgNotNull(ARG_name, name);
		return new StringSetting(this, () -> {
			// 1. Check thread-local override
			var v = localStore.get().get(name);
			if (v != null)
				return v.orElse(null); // v is Optional.empty() if key exists with null value, or Optional.of(value) if present

			// 2. Check global override
			v = globalStore.get().get(name);
			if (v != null)
				return v.orElse(null); // v is Optional.empty() if key exists with null value, or Optional.of(value) if present

			// 3. Check sources in reverse order (last added first)
			for (int i = sources.size() - 1; i >= 0; i--) {
				var source = sources.get(i);
				var result = source.get(name);
				if (result != null)
					return result.orElse(null);
			}

			return null;
		});
	}

	/**
	 * Looks up a system property, returning a default value if not found.
	 *
	 * <p>
	 * This method searches for a value using the same lookup order as {@link #get(String)}.
	 * If a value is found, it is converted to the type of the default value using {@link #toType(String, Class)}.
	 * Supported types include any type that has a static method with signature <c>public static &lt;T&gt; T anyName(String arg)</c>
	 * or a public constructor with signature <c>public T(String arg)</c>, such as {@link Boolean}, {@link Integer}, {@link java.nio.charset.Charset}, {@link java.io.File}, etc.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// System property: -Dmy.property=true</jc>
	 * 	Boolean <jv>flag</jv> = get(<js>"my.property"</js>, <jk>false</jk>);  <jc>// true</jc>
	 *
	 * 	<jc>// Environment variable: MY_PROPERTY=UTF-8</jc>
	 * 	Charset <jv>charset</jv> = get(<js>"my.property"</js>, Charset.defaultCharset());  <jc>// UTF-8</jc>
	 *
	 * 	<jc>// Not found, returns default</jc>
	 * 	String <jv>value</jv> = get(<js>"nonexistent"</js>, <js>"default"</js>);  <jc>// "default"</jc>
	 * </p>
	 *
	 * @param <T> The type to convert the value to.
	 * @param name The property name.
	 * @param def The default value to return if not found.
	 * @return The found value (converted to type T), or the default value if not found.
	 * @see #get(String)
	 * @see #toType(String, Class)
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String name, T def) {
		assertArgNotNull(ARG_def, def);
		return get(name).asType((Class<T>)def.getClass()).orElse(def);
	}

	/**
	 * Sets a global override for the specified property.
	 *
	 * <p>
	 * This override will apply to all threads and takes precedence over system properties.
	 * However, per-thread overrides (set via {@link #setLocal(String, String)}) will still take precedence
	 * over global overrides.
	 *
	 * <p>
	 * If the <c>juneau.settings.disableGlobal</c> system property is set to <c>true</c>,
	 * this method will throw an exception. This allows system administrators
	 * to prevent applications from overriding system properties globally.
	 *
	 * <p>
	 * Setting a value to <c>null</c> will store an empty optional, effectively overriding the system
	 * property to return empty. Use {@link #unsetGlobal(String)} to completely remove the override.
	 *
	 * @param name The property name.
	 * @param value The override value, or <c>null</c> to set an empty override.
	 * @return This object for method chaining.
	 * @see #unsetGlobal(String)
	 * @see #clearGlobal()
	 */
	public Settings setGlobal(String name, String value) {
		assertArgNotNull(ARG_name, name);
		globalStore.orElseThrow(()->illegalState(MSG_globalDisabled)).set(name, value);
		return this;
	}

	/**
	 * Removes a global override for the specified property.
	 *
	 * <p>
	 * After calling this method, the property will fall back to the system property value
	 * (or per-thread override if one exists).
	 *
	 * @param name The property name.
	 * @see #setGlobal(String, String)
	 * @see #clearGlobal()
	 */
	public void unsetGlobal(String name) {
		assertArgNotNull(ARG_name, name);
		globalStore.orElseThrow(()->illegalState(MSG_globalDisabled)).unset(name);
	}

	/**
	 * Sets a per-thread override for the specified property.
	 *
	 * <p>
	 * This override will only apply to the current thread and takes precedence over global overrides
	 * and system properties. This is particularly useful in unit tests where you need to temporarily
	 * change a system property value without affecting other threads or tests.
	 *
	 * <p>
	 * Setting a value to <c>null</c> will store an empty optional, effectively overriding the property
	 * to return empty for this thread. Use {@link #unsetLocal(String)} to completely remove the override.
	 *
	 * @param name The property name.
	 * @param value The override value, or <c>null</c> to set an empty override.
	 * @return This object for method chaining.
	 * @see #unsetLocal(String)
	 * @see #clearLocal()
	 */
	public Settings setLocal(String name, String value) {
		assertArgNotNull(ARG_name, name);
		assertState(nn(localStore.get()), MSG_localDisabled);
		localStore.get().set(name, value);
		return this;
	}

	/**
	 * Removes a per-thread override for the specified property.
	 *
	 * <p>
	 * After calling this method, the property will fall back to the global override (if set)
	 * or the system property value for the current thread.
	 *
	 * @param name The property name.
	 * @see #setLocal(String, String)
	 * @see #clearLocal()
	 */
	public void unsetLocal(String name) {
		assertArgNotNull(ARG_name, name);
		assertState(nn(localStore.get()), MSG_localDisabled);
		localStore.get().unset(name);
	}

	/**
	 * Clears all per-thread overrides for the current thread.
	 *
	 * <p>
	 * After calling this method, all properties will fall back to global overrides (if set)
	 * or system property values for the current thread.
	 *
	 * <p>
	 * This is typically called in a <c>@AfterEach</c> or <c>@After</c> test method to clean up
	 * thread-local overrides after a test completes.
	 *
	 * @return This object for method chaining.
	 * @see #setLocal(String, String)
	 * @see #unsetLocal(String)
	 */
	public Settings clearLocal() {
		assertState(nn(localStore.get()), MSG_localDisabled);
		localStore.get().clear();
		return this;
	}

	/**
	 * Cleans up thread-local storage for the current thread.
	 *
	 * <p>
	 * This method should be called when a thread is finished using this Settings instance to prevent memory leaks
	 * in thread pool environments where threads are reused.
	 */
	public void cleanup() {
		if (localStore != null) {
			localStore.remove();
		}
	}

	/**
	 * Clears all global overrides.
	 *
	 * <p>
	 * After calling this method, all properties will fall back to resolver values
	 * (or per-thread overrides if they exist).
	 *
	 * @return This object for method chaining.
	 * @see #setGlobal(String, String)
	 * @see #unsetGlobal(String)
	 */
	public Settings clearGlobal() {
		globalStore.orElseThrow(()->illegalState(MSG_globalDisabled)).clear();
		return this;
	}

	/**
	 * Converts a string to the specified type using reflection to find conversion methods or constructors.
	 *
	 * <p>
	 * This method attempts to convert a string to the specified type using the following lookup order:
	 * <ol>
	 * 	<li>Custom type functions registered via {@link Builder#addTypeFunction(Class, Function)}</li>
	 * 	<li>Special handling for {@link String} (returns the string as-is)</li>
	 * 	<li>Special handling for {@link Enum} types (uses {@link Enum#valueOf(Class, String)})</li>
	 * 	<li>Reflection lookup for static methods with signature <c>public static &lt;T&gt; T anyName(String arg)</c>
	 * 		on the target class</li>
	 * 	<li>Reflection lookup for static methods on the superclass (for abstract classes like {@link Charset}
	 * 		where concrete implementations need to use the abstract class's static method)</li>
	 * 	<li>Reflection lookup for public constructors with signature <c>public T(String arg)</c></li>
	 * </ol>
	 *
	 * <p>
	 * When a conversion method or constructor is found via reflection, it is cached in the
	 * <c>toTypeFunctions</c> map for future use.
	 *
	 * <h5 class='section'>Examples:</h5>
	 * <ul class='spaced-list'>
	 * 	<li><c>Boolean</c> - Uses <c>Boolean.valueOf(String)</c> static method
	 * 	<li><c>Integer</c> - Uses <c>Integer.valueOf(String)</c> static method
	 * 	<li><c>Charset</c> - Uses <c>Charset.forName(String)</c> static method (even for concrete implementations)
	 * 	<li><c>File</c> - Uses <c>File(String)</c> constructor
	 * </ul>
	 *
	 * @param <T> The target type.
	 * @param s The string to convert. Must not be <jk>null</jk>.
	 * @param c The target class. Must not be <jk>null</jk>.
	 * @return The converted value.
	 * @throws RuntimeException If the type is not supported for conversion (no static method or constructor found).
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <T> T toType(String s, Class<T> c) {
		assertArgNotNull(ARG_s, s);
		assertArgNotNull(ARG_c, c);
		var f = (Function<String,T>)toTypeFunctions.get(c);
		if (f == null) {
			if (c == String.class)
				return (T)s;
			if (c.isEnum())
				return (T)Enum.valueOf((Class<? extends Enum>)c, s);
			ClassInfoTyped<T> ci = info(c);
			f = ci.getDeclaredMethod(x -> x.isStatic() && x.hasParameterTypes(String.class) && x.hasReturnType(c) && FROM_STRING_METHOD_NAMES.contains(x.getName())).map(x -> (Function<String,T>)s2 -> x.invoke(null, s2)).orElse(null);
			if (f == null)
				f = ci.getPublicConstructor(x -> x.hasParameterTypes(String.class)).map(x -> (Function<String,T>)x::newInstance).orElse(null);
			if (f != null)
				toTypeFunctions.putIfAbsent(c, f);
		}
		if (f == null)
			throw rex("Invalid env type: {0}", c);
		return f.apply(s);
	}
}

