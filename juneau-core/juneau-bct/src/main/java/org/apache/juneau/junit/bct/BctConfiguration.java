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
package org.apache.juneau.junit.bct;

import static org.apache.juneau.commons.utils.AssertionUtils.*;
import static org.apache.juneau.commons.utils.Utils.*;

import org.apache.juneau.commons.function.*;
import org.apache.juneau.commons.settings.*;

/**
 * Configuration utility for Bean-Centric Testing (BCT) framework.
 *
 * <p>
 * This class provides thread-local configuration settings for BCT assertions, allowing test-specific
 * customization of behavior such as map sorting, collection sorting, and bean converter selection.
 *
 * <p>
 * Configuration is managed using thread-local storage, ensuring that parallel test execution doesn't
 * interfere with each other's settings. Settings are typically configured via the {@link org.apache.juneau.junit.bct.annotations.BctConfig @BctConfig}
 * annotation, but can also be set programmatically.
 *
 * <h5 class='section'>Configuration Properties:</h5>
 * <ul>
 * 	<li><b>{@link #BCT_SORT_MAPS}:</b> Enable sorting of maps in assertions</li>
 * 	<li><b>{@link #BCT_SORT_COLLECTIONS}:</b> Enable sorting of collections in assertions</li>
 * 	<li><b>Bean Converter:</b> Custom bean converter instance for property access and conversion</li>
 * </ul>
 *
 * <h5 class='section'>Usage via Annotation:</h5>
 * <p class='bjava'>
 * 	<ja>@BctConfig</ja>(sortMaps=TriState.<jsf>TRUE</jsf>, beanConverter=MyConverter.<jk>class</jk>)
 * 	<jk>class</jk> MyTest {
 * 		<ja>@Test</ja>
 * 		<ja>@BctConfig</ja>(sortCollections=TriState.<jsf>TRUE</jsf>)
 * 		<jk>void</jk> testSomething() {
 * 			<jc>// sortMaps=true (from class), sortCollections=true (from method),</jc>
 * 			<jc>// beanConverter=MyConverter (from class)</jc>
 * 		}
 * 	}
 * </p>
 *
 * <h5 class='section'>Usage via Programmatic API:</h5>
 * <p class='bjava'>
 * 	<ja>@BeforeEach</ja>
 * 	<jk>void</jk> setUp() {
 * 		BctConfiguration.<jsm>set</jsm>(BctConfiguration.<jsf>BCT_SORT_MAPS</jsf>, <jk>true</jk>);
 * 		BctConfiguration.<jsm>set</jsm>(<jk>new</jk> MyCustomConverter());
 * 	}
 *
 * 	<ja>@AfterEach</ja>
 * 	<jk>void</jk> tearDown() {
 * 		BctConfiguration.<jsm>clear</jsm>();
 * 	}
 * </p>
 *
 * <h5 class='section'>Thread Safety:</h5>
 * <p>
 * All methods in this class are thread-safe. Configuration is stored in thread-local storage,
 * ensuring that each test thread has its own isolated configuration that doesn't interfere
 * with other concurrently running tests.
 * </p>
 *
 * @see org.apache.juneau.junit.bct.annotations.BctConfig
 * @see org.apache.juneau.junit.bct.BctAssertions
 */
public class BctConfiguration {

	// Thread-local memoized supplier for default converter (defaults to BasicBeanConverter.DEFAULT)
	private static final ThreadLocal<ResettableSupplier<BeanConverter>> CONVERTER_SUPPLIER = ThreadLocal.withInitial(() -> memr(() -> BasicBeanConverter.DEFAULT));

	/**
	 * Configuration property name for enabling map sorting in BCT assertions.
	 *
	 * <p>
	 * When set to <jk>true</jk>, maps will be sorted by key before comparison in assertions.
	 * This ensures consistent ordering regardless of the map's internal ordering.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BctConfiguration.<jsm>set</jsm>(BctConfiguration.<jsf>BCT_SORT_MAPS</jsf>, <jk>true</jk>);
	 * </p>
	 */
	public static final String BCT_SORT_MAPS = "Bct.sortMaps";

	/**
	 * Configuration property name for enabling collection sorting in BCT assertions.
	 *
	 * <p>
	 * When set to <jk>true</jk>, collections will be sorted before comparison in assertions.
	 * This ensures consistent ordering regardless of the collection's internal ordering.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BctConfiguration.<jsm>set</jsm>(BctConfiguration.<jsf>BCT_SORT_COLLECTIONS</jsf>, <jk>true</jk>);
	 * </p>
	 */
	public static final String BCT_SORT_COLLECTIONS = "Bct.sortCollections";

	// Thread-local override for method-level converter customization
	private static final ThreadLocal<BeanConverter> CONVERTER_OVERRIDE = new ThreadLocal<>();

	private static final Settings SETTINGS = Settings.create().build();

	/**
	 * Sets a thread-local configuration value.
	 *
	 * <p>
	 * The value is stored in thread-local storage and will only affect the current thread.
	 * This is the recommended way to set configuration for individual tests.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BctConfiguration.<jsm>set</jsm>(BctConfiguration.<jsf>BCT_SORT_MAPS</jsf>, <jk>true</jk>);
	 * </p>
	 *
	 * @param name The configuration property name (e.g., {@link #BCT_SORT_MAPS}).
	 * @param value The value to set.
	 * @see #get(String)
	 * @see #clear()
	 */
	public static void set(String name, Object value) {
		SETTINGS.setLocal(name, s(value));
	}

	/**
	 * Sets a global configuration value.
	 *
	 * <p>
	 * Global values apply to all threads and persist until explicitly cleared.
	 * Use with caution in multi-threaded test environments.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	BctConfiguration.<jsm>setGlobal</jsm>(BctConfiguration.<jsf>BCT_SORT_MAPS</jsf>, <jk>true</jk>);
	 * </p>
	 *
	 * @param name The configuration property name (e.g., {@link #BCT_SORT_MAPS}).
	 * @param value The value to set.
	 * @see #get(String)
	 * @see #clearGlobal()
	 */
	public static void setGlobal(String name, Object value) {
		SETTINGS.setGlobal(name, s(value));
	}

	/**
	 * Gets a configuration setting by name.
	 *
	 * <p>
	 * Returns the setting object which can be used to check if a value is set and retrieve it.
	 * Thread-local values take precedence over global values.
	 *
	 * @param name The configuration property name (e.g., {@link #BCT_SORT_MAPS}).
	 * @return The setting object, or <jk>null</jk> if not set.
	 * @see #get(String, Object)
	 * @see #set(String, Object)
	 */
	public static StringSetting get(String name) {
		return SETTINGS.get(name);
	}

	/**
	 * Gets a configuration value by name with a default value.
	 *
	 * <p>
	 * Returns the configured value if set, otherwise returns the provided default value.
	 * Thread-local values take precedence over global values.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>boolean</jk> <jv>sortMaps</jv> = BctConfiguration.<jsm>get</jsm>(BctConfiguration.<jsf>BCT_SORT_MAPS</jsf>, <jk>false</jk>);
	 * </p>
	 *
	 * @param <T> The type of the value.
	 * @param name The configuration property name (e.g., {@link #BCT_SORT_MAPS}).
	 * @param def The default value to return if not set.
	 * @return The configured value, or the default if not set.
	 * @see #get(String)
	 * @see #set(String, Object)
	 */
	public static <T> T get(String name, T def) {
		return SETTINGS.get(name, def);
	}

	/**
	 * Clears all thread-local configuration settings.
	 *
	 * <p>
	 * This method removes all thread-local settings including:
	 * <ul>
	 * 	<li>Configuration properties (e.g., {@link #BCT_SORT_MAPS}, {@link #BCT_SORT_COLLECTIONS})</li>
	 * 	<li>Bean converter override</li>
	 * </ul>
	 *
	 * <p>
	 * This is typically called in test teardown methods (e.g., {@code @AfterEach}) to ensure
	 * a clean state for subsequent tests. The {@link org.apache.juneau.junit.bct.annotations.BctConfigExtension BctConfigExtension}
	 * automatically calls this method after each test.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@AfterEach</ja>
	 * 	<jk>void</jk> tearDown() {
	 * 		BctConfiguration.<jsm>clear</jsm>();
	 * 	}
	 * </p>
	 *
	 * @see #clearGlobal()
	 * @see #set(String, Object)
	 */
	public static void clear() {
		SETTINGS.clearLocal();
		CONVERTER_OVERRIDE.remove();
	}

	/**
	 * Clears all global configuration settings.
	 *
	 * <p>
	 * This method removes all global settings. Use with caution as this affects all threads.
	 *
	 * @see #clear()
	 * @see #setGlobal(String, Object)
	 */
	public static void clearGlobal() {
		SETTINGS.clearGlobal();
	}

	/**
	 * Sets a custom bean converter for the current thread.
	 *
	 * <p>
	 * This method allows you to override the default converter ({@link BasicBeanConverter#DEFAULT}) for all
	 * assertions in the current test method. The converter will be used by all assertion methods in the current thread.
	 *
	 * <p>
	 * This is particularly useful when you need custom property access, stringification, or conversion logic
	 * for specific tests. The converter can be configured via the {@link org.apache.juneau.junit.bct.annotations.BctConfig @BctConfig}
	 * annotation or set programmatically.
	 *
	 * <h5 class='section'>Usage Example:</h5>
	 * <p class='bjava'>
	 * 	<ja>@BeforeEach</ja>
	 * 	<jk>void</jk> setUp() {
	 * 		<jk>var</jk> <jv>customConverter</jv> = BasicBeanConverter.<jsm>builder</jsm>()
	 * 			.defaultSettings()
	 * 			.addStringifier(LocalDate.<jk>class</jk>, <jp>date</jp> -> <jp>date</jp>.format(DateTimeFormatter.<jsf>ISO_LOCAL_DATE</jsf>))
	 * 			.build();
	 * 		BctConfiguration.<jsm>set</jsm>(<jv>customConverter</jv>);
	 * 	}
	 *
	 * 	<ja>@Test</ja>
	 * 	<jk>void</jk> testWithCustomConverter() {
	 * 		<jc>// All assertions use the custom converter</jc>
	 * 		<jsm>assertBean</jsm>(<jv>event</jv>, <js>"date"</js>, <js>"2023-12-01"</js>);
	 * 	}
	 *
	 * 	<ja>@AfterEach</ja>
	 * 	<jk>void</jk> tearDown() {
	 * 		BctConfiguration.<jsm>clear</jsm>(); <jc>// Also clears converter override</jc>
	 * 	}
	 * </p>
	 *
	 * <h5 class='section'>Thread Safety:</h5>
	 * <p>
	 * This method is thread-safe and uses thread-local storage. Each test method running in parallel
	 * will have its own converter instance, preventing cross-thread interference.
	 * </p>
	 *
	 * @param converter The bean converter to use for the current thread. Must not be <jk>null</jk>.
	 * @throws IllegalArgumentException If converter is <jk>null</jk>.
	 * @see #clear()
	 * @see org.apache.juneau.junit.bct.annotations.BctConfig#beanConverter()
	 * @see BeanConverter
	 * @see BasicBeanConverter
	 */
	public static void set(BeanConverter converter) {
		assertArgNotNull("converter", converter);
		CONVERTER_OVERRIDE.set(converter);
	}


	/**
	 * Gets the bean converter for the current thread.
	 *
	 * <p>Returns the thread-local converter override if set, otherwise returns the memoized default converter.
	 * This method is used internally by all assertion methods to get the current thread-local converter.</p>
	 *
	 * @return The bean converter to use for the current thread.
	 */
	static BeanConverter getConverter() {
		return opt(BctConfiguration.CONVERTER_OVERRIDE.get()).orElseGet(CONVERTER_SUPPLIER.get());
	}
}
