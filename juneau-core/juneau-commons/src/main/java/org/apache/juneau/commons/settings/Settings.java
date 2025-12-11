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
import static org.apache.juneau.commons.utils.Utils.*;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Encapsulates Java system properties with support for global and per-thread overrides for unit testing.
 *
 * <p>
 * This class provides a thread-safe way to access system properties that can be overridden at both the global level
 * and per-thread level, making it useful for unit tests that need to temporarily change system property
 * values without affecting other tests or threads.
 *
 * <h5 class='section'>Lookup Order:</h5>
 * <p>
 * When retrieving a property value, the lookup order is:
 * <ol>
 * 	<li>Per-thread override (if set via {@link #setLocal(String, String)})
 * 	<li>Global override (if set via {@link #setGlobal(String, String)})
 * 	<li>Sources in reverse order (last source added via {@link #addSource(SettingSource)} is checked first)
 * 	<li>System property source (default, always second-to-last)
 * 	<li>System environment variable source (default, always last)
 * </ol>
 *
 * <h5 class='section'>Features:</h5>
 * <ul class='spaced-list'>
 * 	<li>System property access - read Java system properties with type conversion
 * 	<li>Global overrides - override system properties globally for all threads
 * 	<li>Per-thread overrides - override system properties for specific threads (useful for unit tests)
 * 	<li>Custom sources - add arbitrary property sources (e.g., Spring properties, environment variables, config files)
 * 	<li>Disable override support - system property to prevent new global overrides from being set
 * 	<li>Type-safe accessors - convenience methods for common types: Integer, Long, Boolean, Double, Float, File, Path, URI, Charset
 * </ul>
 *
 * <h5 class='section'>Usage Examples:</h5>
 * <p class='bjava'>
 * 	<jc>// Get a system property as a string</jc>
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
 * 	<jc>// Add a custom source (e.g., Spring properties)</jc>
 * 	MapSource <jv>springSource</jv> = <jk>new</jk> MapSource();
 * 	<jv>springSource</jv>.set(<js>"spring.datasource.url"</js>, <js>"jdbc:postgresql://localhost/db"</js>);
 * 	Settings.<jsf>get</jsf>().addSource(<jv>springSource</jv>);
 * </p>
 *
 * <h5 class='section'>System Properties:</h5>
 * <ul class='spaced-list'>
 * 	<li><c>juneau.settings.disableGlobal</c> - If set to <c>true</c>, prevents new global overrides
 * 		from being set via {@link #setGlobal(String, String)}. Existing global overrides will still be
 * 		returned by {@link #get(String)} until explicitly removed.
 * 	<li><c>juneau.settings.disableCustomSources</c> - If set to <c>true</c>, prevents custom sources
 * 		from being added via {@link #addSource(SettingSource)} or {@link #setSources(SettingSource...)}.
 * </ul>
 */
public class Settings {

	private static final Settings INSTANCE = new Settings();

	/**
	 * Returns the singleton instance of Settings.
	 *
	 * @return The singleton Settings instance.
	 */
	public static Settings get() {
		return INSTANCE;
	}

	private final MapSource globalOverrides = new MapSource();
	private final ThreadLocal<MapSource> threadOverrides = new ThreadLocal<>();
	private final List<SettingSource> sources = new CopyOnWriteArrayList<>();

	/**
	 * System property source that delegates to {@link System#getProperty(String)}.
	 */
	public static final SettingSource SYSTEM_PROPERTY_SOURCE = new ReadOnlySource(x -> System.getProperty(x));

	/**
	 * System environment variable source that delegates to {@link System#getenv(String)}.
	 */
	public static final SettingSource SYSTEM_ENV_SOURCE = new ReadOnlySource(x -> System.getenv(x));

	/**
	 * Returns properties for this Settings object itself.
	 * Note that these are initialized at startup and not changeable through System.setProperty().
	 */
	private static final Optional<String> initProperty(String property) {
		var v = SYSTEM_PROPERTY_SOURCE.get(property);
		if (v != null)
			return v;
		v = SYSTEM_ENV_SOURCE.get(property);
		if (v != null)
			return v;
		return opte();
	}

	private static final String DISABLE_CUSTOM_SOURCES_PROP = "juneau.settings.disableCustomSources";
	private static final String DISABLE_GLOBAL_PROP = "juneau.settings.disableGlobal";
	private static final boolean DISABLE_CUSTOM_SOURCES = initProperty(DISABLE_CUSTOM_SOURCES_PROP).map(Boolean::valueOf).orElse(false);
	private static final boolean DISABLE_GLOBAL = initProperty(DISABLE_GLOBAL_PROP).map(Boolean::valueOf).orElse(false);

	/**
	 * Constructor.
	 */
	private Settings() {
		// Initialize with system sources as defaults (env first, then property, so property has higher precedence)
		sources.add(SYSTEM_ENV_SOURCE);
		sources.add(SYSTEM_PROPERTY_SOURCE);
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
		var localSource = threadOverrides.get();
		if (localSource != null) {
			var v = localSource.get(name);
			if (v != null)
				return v; // v is Optional.empty() if key exists with null value, or Optional.of(value) if present
		}

		// 2. Check global override
		var v = globalOverrides.get(name);
		if (v != null)
			return v; // v is Optional.empty() if key exists with null value, or Optional.of(value) if present

		// 3. Check sources in reverse order (last added first)
		for (int i = sources.size() - 1; i >= 0; i--) {
			var source = sources.get(i);
			if (source == null)
				continue; // Skip null sources (defensive check)
			var result = source.get(name);
			if (result == null)
				continue; // Key not in this source, try next source
			// If result is Optional.empty(), it means key exists with null value, so return it
			// If result is present, return it
			return result;
		}

		return Optional.empty();
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
	 * @see #unsetGlobal(String)
	 * @see #clearGlobal()
	 */
	public void setGlobal(String name, String value) {
		assertArg(! DISABLE_GLOBAL, "Global settings have been disabled via ''{0}''", DISABLE_GLOBAL_PROP);
		globalOverrides.set(name, value);
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
		assertArg(! DISABLE_GLOBAL, "Global settings have been disabled via ''{0}''", DISABLE_GLOBAL_PROP);
		globalOverrides.unset(name);
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
	 * @see #unsetLocal(String)
	 * @see #clearLocal()
	 */
	public void setLocal(String name, String value) {
		var localSource = threadOverrides.get();
		if (localSource == null) {
			localSource = new MapSource();
			threadOverrides.set(localSource);
		}
		localSource.set(name, value);
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
		var localSource = threadOverrides.get();
		if (localSource != null)
			localSource.unset(name);
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
	 * @see #setLocal(String, String)
	 * @see #unsetLocal(String)
	 */
	public void clearLocal() {
		threadOverrides.remove();
	}

	/**
	 * Clears all global overrides.
	 *
	 * <p>
	 * After calling this method, all properties will fall back to resolver values
	 * (or per-thread overrides if they exist).
	 *
	 * @see #setGlobal(String, String)
	 * @see #unsetGlobal(String)
	 */
	public void clearGlobal() {
		globalOverrides.clear();
	}

	/**
	 * Adds a source to the source list.
	 *
	 * <p>
	 * Sources are checked in reverse order (last added is checked first), after global overrides
	 * but before the system property source. This allows you to add custom property sources such
	 * as Spring properties, environment variables, or configuration files.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Add a Spring properties source</jc>
	 * 	MapSource <jv>springSource</jv> = <jk>new</jk> MapSource();
	 * 	<jv>springSource</jv>.set(<js>"spring.datasource.url"</js>, <js>"jdbc:postgresql://localhost/db"</js>);
	 * 	Settings.<jsf>get</jsf>().addSource(<jv>springSource</jv>);
	 * </p>
	 *
	 * @param source The source. Must not be <c>null</c>.
	 * @return This object for method chaining.
	 * @see #setSources(SettingSource...)
	 */
	public Settings addSource(SettingSource source) {
		assertArg(! DISABLE_CUSTOM_SOURCES, "Global custom sources have been disabled via ''{0}''", DISABLE_CUSTOM_SOURCES_PROP);
		assertArgNotNull("source", source);
		// Add at the end - since we iterate in reverse order, this means it's checked first (before system sources)
		sources.add(source);
		return this;
	}

	/**
	 * Sets the source list, replacing all existing sources.
	 *
	 * <p>
	 * This method clears all existing sources (except the system property and environment variable sources which are always
	 * present) and adds the specified sources in order. The sources will be checked in reverse
	 * order (last source in the array is checked first).
	 *
	 * <p>
	 * Note that using this method resets existing sources, so if you want to maintain resolving environment variables
	 * or system properties, you'll need to add {@link #SYSTEM_ENV_SOURCE} and {@link #SYSTEM_PROPERTIES_SOURCE} to the
	 * list of sources added to this method.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jc>// Set multiple sources</jc>
	 * 	MapSource <jv>springSource</jv> = <jk>new</jk> MapSource();
	 * 	MapSource <jv>envSource</jv> = <jk>new</jk> MapSource();
	 * 	Settings.<jsf>get</jsf>().setSources(<jv>springSource</jv>, <jv>envSource</jv>);
	 * 	<jc>// Lookup order: local &gt; global &gt; envSource &gt; springSource &gt; system properties &gt; system env vars</jc>
	 * </p>
	 *
	 * <p>
	 * Note that the order of precedence of the sources is in reverse order.  So sources at the end of the list are
	 * checked before sources at the beginning of the list.
	 *
	 * @param sources The sources to add. Must not be <c>null</c> or contain <c>null</c> elements.
	 * @return This object for method chaining.
	 * @see #addSource(SettingSource)
	 */
	@SafeVarargs
	public final Settings setSources(SettingSource...sources) {
		assertArg(! DISABLE_CUSTOM_SOURCES, "Global custom sources have been disabled via ''{0}''", DISABLE_CUSTOM_SOURCES_PROP);
		assertVarargsNotNull("sources", sources);
		// Remove all sources
		this.sources.clear();
		// Add new sources
		for (var source : sources) {
			this.sources.add(source);
		}
		return this;
	}

	/**
	 * Resets the sources list to default (only system sources).
	 * Package-private for testing purposes.
	 */
	void resetSources() {
		this.sources.clear();
		this.sources.add(SYSTEM_ENV_SOURCE);
		this.sources.add(SYSTEM_PROPERTY_SOURCE);
	}
}

