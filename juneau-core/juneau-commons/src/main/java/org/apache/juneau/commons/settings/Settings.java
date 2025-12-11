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

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.ThrowableUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

import org.apache.juneau.commons.function.*;

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
 * 	<li>System property access - read Java system properties with type conversion
 * 	<li>Global overrides - override system properties globally for all threads (stored in a {@link SettingStore})
 * 	<li>Per-thread overrides - override system properties for specific threads (stored in a per-thread {@link SettingStore})
 * 	<li>Custom sources - add arbitrary property sources (e.g., Spring properties, environment variables, config files) via the {@link Builder}
 * 	<li>Disable override support - system property to prevent new global overrides from being set
 * 	<li>Type-safe accessors - convenience methods for common types: Integer, Long, Boolean, Double, Float, File, Path, URI, Charset
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Get a system property as a string (using singleton instance)</jc>
 * 	Optional&lt;String&gt; <jv>value</jv> = Settings.<jsf>get</jsf>().get(<js>"my.property"</js>);
 *
 * 	<jc>// Get with type conversion</jc>
 * 	Optional&lt;Integer&gt; <jv>intValue</jv> = Settings.<jsf>get</jsf>().getInteger(<js>"my.int.property"</js>);
 * 	Optional&lt;Long&gt; <jv>longValue</jv> = Settings.<jsf>get</jsf>().getLong(<js>"my.long.property"</js>);
 * 	Optional&lt;Boolean&gt; <jv>boolValue</jv> = Settings.<jsf>get</jsf>().getBoolean(<js>"my.bool.property"</js>);
 * 	Optional&lt;Double&gt; <jv>doubleValue</jv> = Settings.<jsf>get</jsf>().getDouble(<js>"my.double.property"</js>);
 * 	Optional&lt;Float&gt; <jv>floatValue</jv> = Settings.<jsf>get</jsf>().getFloat(<js>"my.float.property"</js>);
 * 	Optional&lt;File&gt; <jv>fileValue</jv> = Settings.<jsf>get</jsf>().getFile(<js>"my.file.property"</js>);
 * 	Optional&lt;Path&gt; <jv>pathValue</jv> = Settings.<jsf>get</jsf>().getPath(<js>"my.path.property"</js>);
 * 	Optional&lt;URI&gt; <jv>uriValue</jv> = Settings.<jsf>get</jsf>().getURI(<js>"my.uri.property"</js>);
 * 	Optional&lt;Charset&gt; <jv>charsetValue</jv> = Settings.<jsf>get</jsf>().getCharset(<js>"my.charset.property"</js>);
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
public class Settings {

	/**
	 * System property source that delegates to {@link System#getProperty(String)}.
	 */
	public static final SettingSource SYSTEM_PROPERTY_SOURCE = FunctionalSource.of(System::getProperty);

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
		v = SYSTEM_ENV_SOURCE.get(uc(property.replace('.', '_')));
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
		private Supplier<SettingStore> globalStoreSupplier = () -> new MapStore();
		private Supplier<SettingStore> localStoreSupplier = () -> new MapStore();
		private final List<SettingSource> sources = new ArrayList<>();

		/**
		 * Sets the supplier for the global store.
		 *
		 * @param supplier The supplier for the global store.  Must not be <c>null</c>.  Can supply null to disable global store.
		 * @return This builder for method chaining.
		 */
		public Builder globalStore(OptionalSupplier<SettingStore> supplier) {
			this.globalStoreSupplier = assertArgNotNull("supplier", supplier);
			return this;
		}

		/**
		 * Sets the supplier for the local (per-thread) store.
		 *
		 * @param supplier The supplier for the local store. Must not be <c>null</c>.
		 * @return This builder for method chaining.
		 */
		public Builder localStore(OptionalSupplier<SettingStore> supplier) {
			this.localStoreSupplier = assertArgNotNull("supplier", supplier);
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
			assertVarargsNotNull("sources", sources);
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
			assertArgNotNull("source", source);
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
		 * Builds a Settings instance from this builder.
		 *
		 * @return A new Settings instance.
		 */
		public Settings build() {
			return new Settings(this);
		}
	}

	private static final Settings INSTANCE = new Builder()
		.globalStore(initProperty(DISABLE_GLOBAL_PROP).map(Boolean::valueOf).orElse(false) ? () -> null : () -> new MapStore())
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
	private final ThreadLocal<SettingStore> localStore;
	private final List<SettingSource> sources;

	/**
	 * Constructor.
	 */
	private Settings(Builder builder) {
		this.globalStore = memoizeResettable(builder.globalStoreSupplier);
		this.localStore = ThreadLocal.withInitial(builder.localStoreSupplier);
		this.sources = new CopyOnWriteArrayList<>(builder.sources);
	}

	/**
	 * Returns the value of the specified system property.
	 *
	 * <p>
	 * The lookup order is:
	 * <ol>
	 * 	<li>Per-thread override (if set via {@link #setLocal(String, String)})
	 * 	<li>Global override (if set via {@link #setGlobal(String, String)})
	 * 	<li>Sources in reverse order (last source added via {@link #addSource(SettingSource)} is checked first)
	 * 	<li>System property source (default, always second-to-last)
	 * 	<li>System environment variable source (default, always last)
	 * </ol>
	 *
	 * @param name The property name.
	 * @return The property value, or {@link Optional#empty()} if not found.
	 */
	public Optional<String> get(String name) {
		// 1. Check thread-local override
		var v = localStore.get().get(name);
		if (v != null)
			return v; // v is Optional.empty() if key exists with null value, or Optional.of(value) if present

		// 2. Check global override
		v = globalStore.get().get(name);
		if (v != null)
			return v; // v is Optional.empty() if key exists with null value, or Optional.of(value) if present

		// 3. Check sources in reverse order (last added first)
		for (int i = sources.size() - 1; i >= 0; i--) {
			var source = sources.get(i);
			var result = source.get(name);
			if (result != null)
				return result;
		}

		return opte();
	}

	/**
	 * Returns the value of the specified system property as an Integer.
	 *
	 * <p>
	 * The property value is parsed using {@link Integer#valueOf(String)}. If the property is not found
	 * or cannot be parsed as an integer, returns {@link Optional#empty()}.
	 *
	 * @param name The property name.
	 * @return The property value as an Integer, or {@link Optional#empty()} if not found or not a valid integer.
	 */
	public Optional<Integer> getInteger(String name) {
		return get(name).map(v -> safeOrNull(()->Integer.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Returns the value of the specified system property as a Long.
	 *
	 * <p>
	 * The property value is parsed using {@link Long#valueOf(String)}. If the property is not found
	 * or cannot be parsed as a long, returns {@link Optional#empty()}.
	 *
	 * @param name The property name.
	 * @return The property value as a Long, or {@link Optional#empty()} if not found or not a valid long.
	 */
	public Optional<Long> getLong(String name) {
		return get(name).map(v -> safeOrNull(()->Long.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Returns the value of the specified system property as a Boolean.
	 *
	 * <p>
	 * The property value is parsed using {@link Boolean#parseBoolean(String)}, which returns <c>true</c>
	 * if the value is (case-insensitive) "true", otherwise <c>false</c>. Note that this method will
	 * return <c>Optional.of(false)</c> for any non-empty value that is not "true", and
	 * {@link Optional#empty()} only if the property is not set.
	 *
	 * @param name The property name.
	 * @return The property value as a Boolean, or {@link Optional#empty()} if not found.
	 */
	public Optional<Boolean> getBoolean(String name) {
		return get(name).map(v -> Boolean.parseBoolean(v));
	}

	/**
	 * Returns the value of the specified system property as a Double.
	 *
	 * <p>
	 * The property value is parsed using {@link Double#valueOf(String)}. If the property is not found
	 * or cannot be parsed as a double, returns {@link Optional#empty()}.
	 *
	 * @param name The property name.
	 * @return The property value as a Double, or {@link Optional#empty()} if not found or not a valid double.
	 */
	public Optional<Double> getDouble(String name) {
		return get(name).map(v -> safeOrNull(()->Double.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Returns the value of the specified system property as a Float.
	 *
	 * <p>
	 * The property value is parsed using {@link Float#valueOf(String)}. If the property is not found
	 * or cannot be parsed as a float, returns {@link Optional#empty()}.
	 *
	 * @param name The property name.
	 * @return The property value as a Float, or {@link Optional#empty()} if not found or not a valid float.
	 */
	public Optional<Float> getFloat(String name) {
		return get(name).map(v -> safeOrNull(()->Float.valueOf(v))).filter(Objects::nonNull);
	}

	/**
	 * Returns the value of the specified system property as a File.
	 *
	 * <p>
	 * The property value is converted to a {@link File} using the {@link File#File(String)} constructor.
	 * If the property is not found, returns {@link Optional#empty()}. Note that this method does not
	 * validate that the file path is valid or that the file exists.
	 *
	 * @param name The property name.
	 * @return The property value as a File, or {@link Optional#empty()} if not found.
	 */
	public Optional<File> getFile(String name) {
		return get(name).map(v -> new File(v));
	}

	/**
	 * Returns the value of the specified system property as a Path.
	 *
	 * <p>
	 * The property value is converted to a {@link Path} using {@link Paths#get(String, String...)}.
	 * If the property is not found or the path string is invalid, returns {@link Optional#empty()}.
	 *
	 * @param name The property name.
	 * @return The property value as a Path, or {@link Optional#empty()} if not found or not a valid path.
	 */
	public Optional<Path> getPath(String name) {
		return get(name).map(v -> safeOrNull(()->Paths.get(v))).filter(Objects::nonNull);
	}

	/**
	 * Returns the value of the specified system property as a URI.
	 *
	 * <p>
	 * The property value is converted to a {@link URI} using {@link URI#create(String)}.
	 * If the property is not found or the URI string is invalid, returns {@link Optional#empty()}.
	 *
	 * @param name The property name.
	 * @return The property value as a URI, or {@link Optional#empty()} if not found or not a valid URI.
	 */
	public Optional<URI> getURI(String name) {
		return get(name).map(v -> safeOrNull(()->URI.create(v))).filter(Objects::nonNull);
	}

	/**
	 * Returns the value of the specified system property as a Charset.
	 *
	 * <p>
	 * The property value is converted to a {@link Charset} using {@link Charset#forName(String)}.
	 * If the property is not found or the charset name is not supported, returns {@link Optional#empty()}.
	 *
	 * @param name The property name.
	 * @return The property value as a Charset, or {@link Optional#empty()} if not found or not a valid charset.
	 */
	public Optional<Charset> getCharset(String name) {
		return get(name).map(v -> safeOrNull(()->Charset.forName(v))).filter(Objects::nonNull);
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
		assertArgNotNull("name", name);
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
		assertArgNotNull("name", name);
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
		assertArgNotNull("name", name);
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
		assertArgNotNull("name", name);
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
}

