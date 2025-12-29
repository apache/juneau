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

import static org.apache.juneau.commons.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.juneau.*;
import org.apache.juneau.commons.function.*;
import org.junit.jupiter.api.*;

class Settings_Test extends TestBase {

	private static final String TEST_PROP = "juneau.test.property";
	private static final String TEST_PROP_2 = "juneau.test.property2";

	@BeforeEach
	void setUp() {
		// Clean up before each test
		Settings.get().clearLocal();
		Settings.get().clearGlobal();
		// Remove test system properties if they exist
		System.clearProperty(TEST_PROP);
		System.clearProperty(TEST_PROP_2);
	}

	@AfterEach
	void tearDown() {
		// Clean up after each test
		Settings.get().clearLocal();
		Settings.get().clearGlobal();
		System.clearProperty(TEST_PROP);
		System.clearProperty(TEST_PROP_2);
	}

	//====================================================================================================
	// get() - Basic functionality
	//====================================================================================================
	@Test
	void a01_get_fromSystemProperty() {
		System.setProperty(TEST_PROP, "system-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("system-value", result.get());
	}

	@Test
	void a02_get_notFound() {
		var result = Settings.get().get("nonexistent.property");
		assertFalse(result.isPresent());
	}

	@Test
	void a03_get_fromGlobalOverride() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void a04_get_fromLocalOverride() {
		Settings.get().setLocal(TEST_PROP, "local-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void a05_get_lookupOrder_localOverridesGlobal() {
		System.setProperty(TEST_PROP, "system-value");
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Settings.get().setLocal(TEST_PROP, "local-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void a06_get_lookupOrder_globalOverridesSystem() {
		System.setProperty(TEST_PROP, "system-value");
		Settings.get().setGlobal(TEST_PROP, "global-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void a07_get_nullValue() {
		Settings.get().setLocal(TEST_PROP, null);
		var result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getInteger()
	//====================================================================================================
	@Test
	void b01_getInteger_valid() {
		System.setProperty(TEST_PROP, "123");
		var result = Settings.get().get(TEST_PROP).asInteger();
		assertTrue(result.isPresent());
		assertEquals(123, result.get());
	}

	@Test
	void b02_getInteger_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		var result = Settings.get().get(TEST_PROP).asInteger();
		assertFalse(result.isPresent());
	}

	@Test
	void b03_getInteger_notFound() {
		var result = Settings.get().get("nonexistent.property").asInteger();
		assertFalse(result.isPresent());
	}

	@Test
	void b04_getInteger_fromOverride() {
		Settings.get().setLocal(TEST_PROP, "456");
		var result = Settings.get().get(TEST_PROP).asInteger();
		assertTrue(result.isPresent());
		assertEquals(456, result.get());
	}

	//====================================================================================================
	// getLong()
	//====================================================================================================
	@Test
	void c01_getLong_valid() {
		System.setProperty(TEST_PROP, "123456789");
		var result = Settings.get().get(TEST_PROP).asLong();
		assertTrue(result.isPresent());
		assertEquals(123456789L, result.get());
	}

	@Test
	void c02_getLong_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		var result = Settings.get().get(TEST_PROP).asLong();
		assertFalse(result.isPresent());
	}

	@Test
	void c03_getLong_fromOverride() {
		Settings.get().setLocal(TEST_PROP, "987654321");
		var result = Settings.get().get(TEST_PROP).asLong();
		assertTrue(result.isPresent());
		assertEquals(987654321L, result.get());
	}

	//====================================================================================================
	// getBoolean()
	//====================================================================================================
	@Test
	void d01_getBoolean_true() {
		System.setProperty(TEST_PROP, "true");
		var result = Settings.get().get(TEST_PROP).asBoolean();
		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	void d02_getBoolean_false() {
		System.setProperty(TEST_PROP, "false");
		var result = Settings.get().get(TEST_PROP).asBoolean();
		assertTrue(result.isPresent());
		assertFalse(result.get());
	}

	@Test
	void d03_getBoolean_caseInsensitive() {
		System.setProperty(TEST_PROP, "TRUE");
		var result = Settings.get().get(TEST_PROP).asBoolean();
		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	void d04_getBoolean_nonTrueValue() {
		System.setProperty(TEST_PROP, "anything");
		var result = Settings.get().get(TEST_PROP).asBoolean();
		assertTrue(result.isPresent());
		assertFalse(result.get());
	}

	@Test
	void d05_getBoolean_notFound() {
		var result = Settings.get().get("nonexistent.property").asBoolean();
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getDouble()
	//====================================================================================================
	@Test
	void e01_getDouble_valid() {
		System.setProperty(TEST_PROP, "123.456");
		var result = Settings.get().get(TEST_PROP).asDouble();
		assertTrue(result.isPresent());
		assertEquals(123.456, result.get(), 0.0001);
	}

	@Test
	void e02_getDouble_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		var result = Settings.get().get(TEST_PROP).asDouble();
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getFloat()
	//====================================================================================================
	@Test
	void f01_getFloat_valid() {
		System.setProperty(TEST_PROP, "123.456");
		var result = Settings.get().get(TEST_PROP).asFloat();
		assertTrue(result.isPresent());
		assertEquals(123.456f, result.get(), 0.0001f);
	}

	@Test
	void f02_getFloat_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		var result = Settings.get().get(TEST_PROP).asFloat();
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getFile()
	//====================================================================================================
	@Test
	void g01_getFile_valid() {
		System.setProperty(TEST_PROP, "/tmp/test.txt");
		var result = Settings.get().get(TEST_PROP).asFile();
		assertTrue(result.isPresent());
		assertEquals(new File("/tmp/test.txt"), result.get());
	}

	@Test
	void g02_getFile_notFound() {
		var result = Settings.get().get("nonexistent.property").asFile();
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getPath()
	//====================================================================================================
	@Test
	void h01_getPath_valid() {
		System.setProperty(TEST_PROP, "/tmp/test.txt");
		var result = Settings.get().get(TEST_PROP).asPath();
		assertTrue(result.isPresent());
		assertEquals(Paths.get("/tmp/test.txt"), result.get());
	}

	@Test
	void h02_getPath_invalid() {
		// Paths.get() can throw exceptions for invalid paths on some systems
		// This test verifies that invalid paths return empty
		System.setProperty(TEST_PROP, "\0invalid");
		var result = Settings.get().get(TEST_PROP).asPath();
		// May or may not be empty depending on OS, but should not throw
		assertNotNull(result);
	}

	//====================================================================================================
	// getURI()
	//====================================================================================================
	@Test
	void i01_getURI_valid() {
		System.setProperty(TEST_PROP, "http://example.com/test");
		var result = Settings.get().get(TEST_PROP).asURI();
		assertTrue(result.isPresent());
		assertEquals(URI.create("http://example.com/test"), result.get());
	}

	@Test
	void i02_getURI_invalid() {
		System.setProperty(TEST_PROP, "not a valid uri");
		var result = Settings.get().get(TEST_PROP).asURI();
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getCharset()
	//====================================================================================================
	@Test
	void j01_getCharset_valid() {
		System.setProperty(TEST_PROP, "UTF-8");
		var result = Settings.get().get(TEST_PROP).asCharset();
		assertTrue(result.isPresent());
		assertEquals(Charset.forName("UTF-8"), result.get());
	}

	@Test
	void j02_getCharset_invalid() {
		System.setProperty(TEST_PROP, "INVALID-CHARSET");
		var result = Settings.get().get(TEST_PROP).asCharset();
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// setGlobal() / unsetGlobal() / clearGlobal()
	//====================================================================================================
	@Test
	void k01_setGlobal() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void k02_unsetGlobal() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Settings.get().unsetGlobal(TEST_PROP);
		var result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void k03_clearGlobal() {
		Settings.get().setGlobal(TEST_PROP, "value1");
		Settings.get().setGlobal(TEST_PROP_2, "value2");
		Settings.get().clearGlobal();
		assertFalse(Settings.get().get(TEST_PROP).isPresent());
		assertFalse(Settings.get().get(TEST_PROP_2).isPresent());
	}

	@Test
	void k04_setGlobal_nullValue() {
		Settings.get().setGlobal(TEST_PROP, null);
		var result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// setLocal() / unsetLocal() / clearLocal()
	//====================================================================================================
	@Test
	void l01_setLocal() {
		Settings.get().setLocal(TEST_PROP, "local-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void l02_unsetLocal() {
		Settings.get().setLocal(TEST_PROP, "local-value");
		Settings.get().unsetLocal(TEST_PROP);
		var result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void l07_unsetLocal_whenNoLocalSource() {
		// unsetLocal should not throw when there's no local source (threadOverrides.get() returns null)
		Settings.get().unsetLocal(TEST_PROP);
		// Should not throw an exception
		var result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void l03_clearLocal() {
		Settings.get().setLocal(TEST_PROP, "value1");
		Settings.get().setLocal(TEST_PROP_2, "value2");
		Settings.get().clearLocal();
		assertFalse(Settings.get().get(TEST_PROP).isPresent());
		assertFalse(Settings.get().get(TEST_PROP_2).isPresent());
	}

	@Test
	void l04_setLocal_nullValue() {
		Settings.get().setLocal(TEST_PROP, null);
		var result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void l05_setLocal_overridesGlobal() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Settings.get().setLocal(TEST_PROP, "local-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void l06_setLocal_overridesSystemProperty() {
		System.setProperty(TEST_PROP, "system-value");
		Settings.get().setLocal(TEST_PROP, "local-value");
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	//====================================================================================================
	// Thread isolation
	//====================================================================================================
	@Test
	void m01_localOverride_threadIsolation() throws InterruptedException {
		Settings.get().setLocal(TEST_PROP, "thread1-value");

		var thread2 = new Thread(() -> {
			Settings.get().setLocal(TEST_PROP, "thread2-value");
			var result = Settings.get().get(TEST_PROP);
			assertTrue(result.isPresent());
			assertEquals("thread2-value", result.get());
		});

		thread2.start();
		thread2.join();

		// Original thread should still have its value
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("thread1-value", result.get());
	}

	@Test
	void m02_globalOverride_sharedAcrossThreads() throws InterruptedException {
		Settings.get().setGlobal(TEST_PROP, "global-value");

		var thread2 = new Thread(() -> {
			var result = Settings.get().get(TEST_PROP);
			assertTrue(result.isPresent());
			assertEquals("global-value", result.get());
		});

		thread2.start();
		thread2.join();

		// Original thread should also see the global value
		var result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	//====================================================================================================
	// Singleton pattern
	//====================================================================================================
	@Test
	void n01_get_returnsSameInstance() {
		var instance1 = Settings.get();
		var instance2 = Settings.get();
		assertSame(instance1, instance2);
	}

	//====================================================================================================
	// Sources
	//====================================================================================================
	@Test
	void o01_addSource() {
		var source = new MapStore();
		source.set(TEST_PROP, "source-value");
		var settings = Settings.create()
			.addSource(source)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source-value", result.get());
	}

	@Test
	void o02_addSource_reverseOrder() {
		var source1 = new MapStore();
		source1.set(TEST_PROP, "source1-value");

		var source2 = new MapStore();
		source2.set(TEST_PROP, "source2-value");

		var settings = Settings.create()
			.addSource(source1)
			.addSource(source2)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		// Last added source should be checked first
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source2-value", result.get());
	}

	@Test
	void o03_addSource_afterGlobalOverride() {
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		settings.setGlobal(TEST_PROP, "global-value");

		var source = new MapStore();
		source.set(TEST_PROP, "source-value");
		// Note: Can't add sources after building, so we create a new instance
		var settingsWithSource = Settings.create()
			.addSource(source)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();
		settingsWithSource.setGlobal(TEST_PROP, "global-value");

		// Global override should take precedence
		var result = settingsWithSource.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void o04_addSource_beforeSystemProperty() {
		System.setProperty(TEST_PROP, "system-value");

		var source = new MapStore();
		source.set(TEST_PROP, "source-value");
		// Sources are checked in reverse order, so add system sources first, then custom source
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(source)
			.build();

		// Source should take precedence over system property (checked first)
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source-value", result.get());
	}

	@Test
	void o05_addSource_fallbackToSystemProperty() {
		var source = new MapStore();
		// Source doesn't have the property

		System.setProperty(TEST_PROP, "system-value");
		var settings = Settings.create()
			.addSource(source)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		// Should fall back to system property
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("system-value", result.get());
	}

	@Test
	void o06_setSources() {
		var source1 = new MapStore();
		source1.set(TEST_PROP, "source1-value");

		var source2 = new MapStore();
		source2.set(TEST_PROP, "source2-value");

		var settings = Settings.create()
			.setSources(source1, source2, Settings.SYSTEM_PROPERTY_SOURCE, Settings.SYSTEM_ENV_SOURCE)
			.build();

		// Last source in array should be checked first
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source2-value", result.get());
	}

	@Test
	void o07_setSources_clearsExisting() {
		var source1 = new MapStore();
		source1.set(TEST_PROP, "source1-value");

		var source2 = new MapStore();
		source2.set(TEST_PROP, "source2-value");

		var settings = Settings.create()
			.addSource(source1)
			.setSources(source2, Settings.SYSTEM_PROPERTY_SOURCE, Settings.SYSTEM_ENV_SOURCE)
			.build();

		// Only source2 should exist now
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source2-value", result.get());
	}

	@Test
	void o08_addSource_nullValue() {
		var source = new MapStore();
		source.set(TEST_PROP, null);
		System.setProperty(TEST_PROP, "system-value");

		// Sources are checked in reverse order, so add system sources first, then custom source
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(source)
			.build();

		// Note that setting a null value on the source (Optional.empty()) overrides the system property.
		// When a source returns Optional.empty(), it means the property exists but has a null value.
		var result = settings.get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void o09_addSource_nullSource() {
		assertThrows(IllegalArgumentException.class, () -> {
			Settings.create().addSource(null);
		});
	}

	@Test
	void o10_setSources_nullSource() {
		var source1 = new MapStore();
		assertThrows(IllegalArgumentException.class, () -> {
			Settings.create().setSources(source1, null);
		});
	}

	@Test
	void o11_source_springPropertiesExample() {
		// Simulate Spring properties
		var springSource = new MapStore();
		springSource.set("spring.datasource.url", "jdbc:postgresql://localhost/db");
		springSource.set("spring.datasource.username", "admin");

		var settings = Settings.create()
			.addSource(springSource)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		var url = settings.get("spring.datasource.url");
		assertTrue(url.isPresent());
		assertEquals("jdbc:postgresql://localhost/db", url.get());

		var username = settings.get("spring.datasource.username");
		assertTrue(username.isPresent());
		assertEquals("admin", username.get());
	}

	//====================================================================================================
	// addSource(FunctionalSource) - Functional interface usage
	//====================================================================================================
	@Test
	void p01_addSource_functionalSource() {
		// Test the addSource(FunctionalSource) overload
		var source = (FunctionalSource) name -> opt(System.getProperty(name));
		System.setProperty(TEST_PROP, "system-value");
		var settings = Settings.create()
			.addSource(source)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("system-value", result.get());
	}

	@Test
	void p02_addSource_functionalSource_factoryMethod() {
		// Test addSource with FunctionalSource.of()
		var source = FunctionalSource.of(System::getProperty);
		System.setProperty(TEST_PROP, "system-value");
		var settings = Settings.create()
			.addSource(source)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("system-value", result.get());
	}

	//====================================================================================================
	// FunctionalStore
	//====================================================================================================
	@Test
	void q01_writeableFunctionalSource_basic() {
		// Create a writable functional source using system properties
		var source = FunctionalStore.of(
			System::getProperty,
			(k, v) -> System.setProperty(k, v),
			k -> System.clearProperty(k),
			() -> { /* No-op clear for system properties */ }
		);

		// Test set and get
		source.set(TEST_PROP, "test-value");
		var result = source.get(TEST_PROP);
		assertNotNull(result);
		assertTrue(result.isPresent());
		assertEquals("test-value", result.get());

		// Test unset
		source.unset(TEST_PROP);
		result = source.get(TEST_PROP);
		assertNull(result); // Should return null when property doesn't exist

		// Clean up
		System.clearProperty(TEST_PROP);
	}

	@Test
	void q02_writeableFunctionalSource_withSettings() {
		// Create a writable functional source and add it to Settings
		var source = FunctionalStore.of(
			System::getProperty,
			(k, v) -> System.setProperty(k, v),
			k -> System.clearProperty(k),
			() -> { /* No-op clear for system properties */ }
		);

		var settings = Settings.create()
			.addSource(source)
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		// Set a value through the source
		source.set(TEST_PROP, "source-value");

		// Get it through Settings
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source-value", result.get());

		// Clean up
		source.unset(TEST_PROP);
		System.clearProperty(TEST_PROP);
	}

	@Test
	void q03_writeableFunctionalSource_clear() {
		// Test clear() functionality with a custom clearer
		var map = new java.util.HashMap<String, String>();
		var source = FunctionalStore.of(
			map::get,
			map::put,
			map::remove,
			map::clear
		);

		source.set(TEST_PROP, "test-value");
		source.set(TEST_PROP_2, "test-value-2");

		// Verify values are set
		var result1 = source.get(TEST_PROP);
		assertNotNull(result1);
		assertTrue(result1.isPresent());
		assertEquals("test-value", result1.get());

		var result2 = source.get(TEST_PROP_2);
		assertNotNull(result2);
		assertTrue(result2.isPresent());
		assertEquals("test-value-2", result2.get());

		// Clear all values
		source.clear();

		// Verify values are cleared
		result1 = source.get(TEST_PROP);
		assertNull(result1);

		result2 = source.get(TEST_PROP_2);
		assertNull(result2);
	}

	//====================================================================================================
	// Builder.localStore() - Coverage for lines 205-206
	//====================================================================================================
	@Test
	void r01_localStore() {
		// Test that localStore() method can be called on the builder
		var settings = Settings.create()
			.localStore(OptionalSupplier.of(() -> new MapStore()))
			.build();
		// Verify it works by setting a local value
		settings.setLocal(TEST_PROP, "test-value");
		var result = settings.get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("test-value", result.get());
	}

	//====================================================================================================
	// Global store disabled (null supplier) - Coverage for lines 476, 493, 571
	//====================================================================================================
	@Test
	void s01_setGlobal_whenGlobalStoreDisabled() {
		// Create Settings with null global store (disabled)
		var settings = Settings.create()
			.globalStore(OptionalSupplier.empty())
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		// setGlobal() should throw IllegalStateException
		assertThrows(IllegalStateException.class, () -> {
			settings.setGlobal(TEST_PROP, "value");
		});
	}

	@Test
	void s02_unsetGlobal_whenGlobalStoreDisabled() {
		// Create Settings with null global store (disabled)
		var settings = Settings.create()
			.globalStore(OptionalSupplier.empty())
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		// unsetGlobal() should throw IllegalStateException
		assertThrows(IllegalStateException.class, () -> {
			settings.unsetGlobal(TEST_PROP);
		});
	}

	@Test
	void s03_clearGlobal_whenGlobalStoreDisabled() {
		// Create Settings with null global store (disabled)
		var settings = Settings.create()
			.globalStore(OptionalSupplier.empty())
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		// clearGlobal() should throw IllegalStateException
		assertThrows(IllegalStateException.class, () -> {
			settings.clearGlobal();
		});
	}

	//====================================================================================================
	// MapStore.unset() - Coverage for line 134 (when map is null)
	//====================================================================================================
	@Test
	void t01_mapStore_unset_whenMapNotInitialized() {
		// Create a fresh MapStore that has never had set() called on it
		// This means the internal map is still null (lazy initialization)
		var store = new MapStore();

		// Calling unset() when map is null should not throw and should do nothing
		// This covers the branch where m == null in line 134
		store.unset(TEST_PROP);

		// Verify the map is still null (not initialized)
		// get() should return null since the map doesn't exist
		var result = store.get(TEST_PROP);
		assertNull(result);
	}

	//====================================================================================================
	// get(String, T) - Type conversion with default value
	//====================================================================================================
	@Test
	void u01_get_withDefaultString_found() {
		System.setProperty(TEST_PROP, "found-value");
		var result = Settings.get().get(TEST_PROP, "default-value");
		assertEquals("found-value", result);
	}

	@Test
	void u02_get_withDefaultString_notFound() {
		var result = Settings.get().get("nonexistent.property", "default-value");
		assertEquals("default-value", result);
	}

	@Test
	void u03_get_withDefaultBoolean_found() {
		System.setProperty(TEST_PROP, "true");
		var result = Settings.get().get(TEST_PROP, false);
		assertTrue(result);
	}

	@Test
	void u04_get_withDefaultBoolean_notFound() {
		var result = Settings.get().get("nonexistent.property", true);
		assertTrue(result);
	}

	@Test
	void u05_get_withDefaultBoolean_falseValue() {
		System.setProperty(TEST_PROP, "false");
		var result = Settings.get().get(TEST_PROP, true);
		assertFalse(result);
	}

	@Test
	void u07_get_withDefaultCharset_notFound() {
		// Use Charset.forName to get a Charset instance (not a concrete implementation)
		var defaultCharset = Charset.forName("UTF-8");
		var result = Settings.get().get("nonexistent.property", defaultCharset);
		assertEquals(defaultCharset, result);
	}

	@Test
	void u08_get_withDefaultEnum_found() {
		enum TestEnum { VALUE1, VALUE2, VALUE3 }
		System.setProperty(TEST_PROP, "VALUE2");
		var result = Settings.get().get(TEST_PROP, TestEnum.VALUE1);
		assertEquals(TestEnum.VALUE2, result);
	}

	@Test
	void u09_get_withDefaultEnum_notFound() {
		enum TestEnum { VALUE1, VALUE2, VALUE3 }
		var result = Settings.get().get("nonexistent.property", TestEnum.VALUE1);
		assertEquals(TestEnum.VALUE1, result);
	}

	@Test
	void u10_get_withDefaultString_nullDefault() {
		// Null defaults are not allowed
		System.setProperty(TEST_PROP, "found-value");
		assertThrows(IllegalArgumentException.class, () -> {
			Settings.get().get(TEST_PROP, (String)null);
		});
	}

	@Test
	void u11_get_withDefaultString_nullProperty() {
		Settings.get().setLocal(TEST_PROP, null);
		var result = Settings.get().get(TEST_PROP, "default-value");
		// When property is set to null, get() returns Optional.empty(), so get(String, T) returns default
		assertEquals("default-value", result);
	}

	//====================================================================================================
	// addTypeFunction() - Custom type conversion
	//====================================================================================================
	@Test
	void v01_addTypeFunction_customType() {
		// Register a custom type converter for Integer
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.addTypeFunction(Integer.class, Integer::valueOf)
			.build();

		System.setProperty(TEST_PROP, "123");
		var result = settings.get(TEST_PROP, 0);
		assertEquals(123, result.intValue());
	}

	@Test
	void v02_addTypeFunction_customType_notFound() {
		// Register a custom type converter for Integer
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.addTypeFunction(Integer.class, Integer::valueOf)
			.build();

		var result = settings.get("nonexistent.property", 999);
		assertEquals(999, result.intValue());
	}

	@Test
	void v03_addTypeFunction_overridesDefault() {
		// Register a custom converter for Boolean that inverts the value
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.addTypeFunction(Boolean.class, s -> !Boolean.valueOf(s))
			.build();

		System.setProperty(TEST_PROP, "true");
		var result = settings.get(TEST_PROP, false);
		// Should be inverted (true -> false)
		assertFalse(result);
	}

	@Test
	void v04_addTypeFunction_multipleTypes() {
		// Register multiple custom type converters
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.addTypeFunction(Integer.class, Integer::valueOf)
			.addTypeFunction(Long.class, Long::valueOf)
			.build();

		System.setProperty(TEST_PROP, "123");
		var intResult = settings.get(TEST_PROP, 0);
		assertEquals(123, intResult.intValue());

		System.setProperty(TEST_PROP_2, "456");
		var longResult = settings.get(TEST_PROP_2, 0L);
		assertEquals(456L, longResult.longValue());
	}

	@Test
	void v05_addTypeFunction_customClass() {
		// Create a custom class with a fromString method
		// Using a static nested class to avoid local class issues
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.addTypeFunction(String.class, s -> "custom-" + s)
			.build();

		System.setProperty(TEST_PROP, "value");
		// String is already supported, but we override it with a custom function
		var result = settings.get(TEST_PROP, "default");
		assertEquals("custom-value", result);
	}

	@Test
	void v06_addTypeFunction_nullType() {
		assertThrows(IllegalArgumentException.class, () -> {
			Settings.create().addTypeFunction(null, Integer::valueOf);
		});
	}

	@Test
	void v07_addTypeFunction_nullFunction() {
		assertThrows(IllegalArgumentException.class, () -> {
			Settings.create().addTypeFunction(Integer.class, null);
		});
	}

	@Test
	void v08_addTypeFunction_unsupportedType() {
		// Try to use a type that doesn't have a static method or constructor
		// Custom class without static method or String constructor
		class UnsupportedType {
			@SuppressWarnings("unused")
			private final int value;
			UnsupportedType(int value) { this.value = value; }
		}

		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		System.setProperty(TEST_PROP, "123");
		assertThrows(RuntimeException.class, () -> {
			settings.get(TEST_PROP, new UnsupportedType(0)); // No static method or String constructor
		});
	}

	@Test
	void v09_addTypeFunction_usesReflectionWhenCustomNotRegistered() {
		// Types with static methods or String constructors work via reflection
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();

		// Boolean has Boolean.valueOf(String) static method, so it should work
		System.setProperty(TEST_PROP, "true");
		var result = settings.get(TEST_PROP, false);
		assertTrue(result);

		// Integer has Integer.valueOf(String) static method, so it should work
		System.setProperty(TEST_PROP_2, "123");
		var intResult = settings.get(TEST_PROP_2, 0);
		assertEquals(123, intResult.intValue());
	}

	//====================================================================================================
	// StringSetting.filter()
	//====================================================================================================
	@Test
	void r01_stringSetting_filter_returnsStringSetting() {
		System.setProperty(TEST_PROP, "hello");
		var setting = Settings.get().get(TEST_PROP);
		var filtered = setting.filter(s -> s.length() > 3);
		assertEquals("hello", filtered.get());
	}

	@Test
	void r02_stringSetting_filter_noMatch() {
		System.setProperty(TEST_PROP, "hi");
		var setting = Settings.get().get(TEST_PROP);
		var filtered = setting.filter(s -> s.length() > 3);
		assertNull(filtered.get());
	}

	@Test
	void r03_stringSetting_filter_empty() {
		var setting = Settings.get().get("nonexistent.property");
		var filtered = setting.filter(s -> s.length() > 3);
		assertNull(filtered.get());
	}

	@Test
	void r04_stringSetting_filter_cached() {
		var callCount = new AtomicInteger();
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();
		System.setProperty(TEST_PROP, "hello");
		var setting = settings.get(TEST_PROP);
		var filtered = setting.filter(s -> {
			callCount.incrementAndGet();
			return s.length() > 3;
		});

		// First call
		assertEquals("hello", filtered.get());
		assertEquals(1, callCount.get());

		// Second call - should use cached value
		assertEquals("hello", filtered.get());
		assertEquals(1, callCount.get());
	}

	@Test
	void r05_stringSetting_filter_independent() {
		System.setProperty(TEST_PROP, "hello");
		var setting = Settings.get().get(TEST_PROP);
		var filtered = setting.filter(s -> s.length() > 3);

		// Reset original
		setting.reset();
		// Filtered should still have cached value
		assertEquals("hello", filtered.get());
	}

	//====================================================================================================
	// StringSetting.mapString()
	//====================================================================================================
	@Test
	void s01_stringSetting_mapString_returnsStringSetting() {
		System.setProperty(TEST_PROP, "hello");
		var setting = Settings.get().get(TEST_PROP);
		var mapped = setting.mapString(String::toUpperCase);
		assertEquals("HELLO", mapped.get());
	}

	@Test
	void s02_stringSetting_mapString_empty() {
		var setting = Settings.get().get("nonexistent.property");
		var mapped = setting.mapString(String::toUpperCase);
		assertNull(mapped.get());
	}

	@Test
	void s03_stringSetting_mapString_cached() {
		var callCount = new AtomicInteger();
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();
		System.setProperty(TEST_PROP, "hello");
		var setting = settings.get(TEST_PROP);
		var mapped = setting.mapString(s -> {
			callCount.incrementAndGet();
			return s.toUpperCase();
		});

		// First call
		assertEquals("HELLO", mapped.get());
		assertEquals(1, callCount.get());

		// Second call - should use cached value
		assertEquals("HELLO", mapped.get());
		assertEquals(1, callCount.get());
	}

	@Test
	void s04_stringSetting_mapString_independent() {
		System.setProperty(TEST_PROP, "hello");
		var setting = Settings.get().get(TEST_PROP);
		var mapped = setting.mapString(String::toUpperCase);

		// Reset original
		setting.reset();
		// Mapped should still have cached value
		assertEquals("HELLO", mapped.get());
	}

	@Test
	void s05_stringSetting_mapString_returnsNull() {
		System.setProperty(TEST_PROP, "hello");
		var setting = Settings.get().get(TEST_PROP);
		var mapped = setting.mapString(s -> null);
		assertNull(mapped.get());
	}

	//====================================================================================================
	// Setting.asOptional()
	//====================================================================================================
	@Test
	void t01_setting_asOptional_present() {
		System.setProperty(TEST_PROP, "value");
		var setting = Settings.get().get(TEST_PROP);
		var optional = setting.asOptional();
		assertTrue(optional.isPresent());
		assertEquals("value", optional.get());
	}

	@Test
	void t02_setting_asOptional_empty() {
		var setting = Settings.get().get("nonexistent.property");
		var optional = setting.asOptional();
		assertFalse(optional.isPresent());
	}

	@Test
	void t03_setting_asOptional_snapshot() {
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();
		System.setProperty(TEST_PROP, "value1");
		var setting = settings.get(TEST_PROP);
		var optional1 = setting.asOptional();
		assertTrue(optional1.isPresent());
		assertEquals("value1", optional1.get());

		// Change the value
		System.setProperty(TEST_PROP, "value2");
		setting.reset(); // Force recomputation

		// Original Optional should still have old value (snapshot)
		assertTrue(optional1.isPresent());
		assertEquals("value1", optional1.get());

		// New Optional should have new value
		var optional2 = setting.asOptional();
		assertTrue(optional2.isPresent());
		assertEquals("value2", optional2.get());
	}

	@Test
	void t04_setting_asOptional_resetDoesNotAffect() {
		var settings = Settings.create()
			.addSource(Settings.SYSTEM_PROPERTY_SOURCE)
			.addSource(Settings.SYSTEM_ENV_SOURCE)
			.build();
		System.setProperty(TEST_PROP, "value1");
		var setting = settings.get(TEST_PROP);
		var optional = setting.asOptional();
		assertTrue(optional.isPresent());
		assertEquals("value1", optional.get());

		// Reset the setting
		setting.reset();

		// Optional should still have the original value (snapshot)
		assertTrue(optional.isPresent());
		assertEquals("value1", optional.get());

		// Getting a new Optional after reset should get the current value
		var optional2 = setting.asOptional();
		assertTrue(optional2.isPresent());
		assertEquals("value1", optional2.get()); // Still value1 since system property hasn't changed
	}
}

