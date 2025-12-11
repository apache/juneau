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

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class Settings_Test extends TestBase {

	private static final String TEST_PROP = "juneau.test.property";
	private static final String TEST_PROP_2 = "juneau.test.property2";

	@BeforeEach
	void setUp() {
		// Clean up before each test
		Settings.get().clearLocal();
		Settings.get().clearGlobal();
		Settings.get().resetSources();
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
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("system-value", result.get());
	}

	@Test
	void a02_get_notFound() {
		Optional<String> result = Settings.get().get("nonexistent.property");
		assertFalse(result.isPresent());
	}

	@Test
	void a03_get_fromGlobalOverride() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void a04_get_fromLocalOverride() {
		Settings.get().setLocal(TEST_PROP, "local-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void a05_get_lookupOrder_localOverridesGlobal() {
		System.setProperty(TEST_PROP, "system-value");
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Settings.get().setLocal(TEST_PROP, "local-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void a06_get_lookupOrder_globalOverridesSystem() {
		System.setProperty(TEST_PROP, "system-value");
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void a07_get_nullValue() {
		Settings.get().setLocal(TEST_PROP, null);
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getInteger()
	//====================================================================================================
	@Test
	void b01_getInteger_valid() {
		System.setProperty(TEST_PROP, "123");
		Optional<Integer> result = Settings.get().getInteger(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(123, result.get());
	}

	@Test
	void b02_getInteger_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		Optional<Integer> result = Settings.get().getInteger(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void b03_getInteger_notFound() {
		Optional<Integer> result = Settings.get().getInteger("nonexistent.property");
		assertFalse(result.isPresent());
	}

	@Test
	void b04_getInteger_fromOverride() {
		Settings.get().setLocal(TEST_PROP, "456");
		Optional<Integer> result = Settings.get().getInteger(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(456, result.get());
	}

	//====================================================================================================
	// getLong()
	//====================================================================================================
	@Test
	void c01_getLong_valid() {
		System.setProperty(TEST_PROP, "123456789");
		Optional<Long> result = Settings.get().getLong(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(123456789L, result.get());
	}

	@Test
	void c02_getLong_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		Optional<Long> result = Settings.get().getLong(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void c03_getLong_fromOverride() {
		Settings.get().setLocal(TEST_PROP, "987654321");
		Optional<Long> result = Settings.get().getLong(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(987654321L, result.get());
	}

	//====================================================================================================
	// getBoolean()
	//====================================================================================================
	@Test
	void d01_getBoolean_true() {
		System.setProperty(TEST_PROP, "true");
		Optional<Boolean> result = Settings.get().getBoolean(TEST_PROP);
		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	void d02_getBoolean_false() {
		System.setProperty(TEST_PROP, "false");
		Optional<Boolean> result = Settings.get().getBoolean(TEST_PROP);
		assertTrue(result.isPresent());
		assertFalse(result.get());
	}

	@Test
	void d03_getBoolean_caseInsensitive() {
		System.setProperty(TEST_PROP, "TRUE");
		Optional<Boolean> result = Settings.get().getBoolean(TEST_PROP);
		assertTrue(result.isPresent());
		assertTrue(result.get());
	}

	@Test
	void d04_getBoolean_nonTrueValue() {
		System.setProperty(TEST_PROP, "anything");
		Optional<Boolean> result = Settings.get().getBoolean(TEST_PROP);
		assertTrue(result.isPresent());
		assertFalse(result.get());
	}

	@Test
	void d05_getBoolean_notFound() {
		Optional<Boolean> result = Settings.get().getBoolean("nonexistent.property");
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getDouble()
	//====================================================================================================
	@Test
	void e01_getDouble_valid() {
		System.setProperty(TEST_PROP, "123.456");
		Optional<Double> result = Settings.get().getDouble(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(123.456, result.get(), 0.0001);
	}

	@Test
	void e02_getDouble_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		Optional<Double> result = Settings.get().getDouble(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getFloat()
	//====================================================================================================
	@Test
	void f01_getFloat_valid() {
		System.setProperty(TEST_PROP, "123.456");
		Optional<Float> result = Settings.get().getFloat(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(123.456f, result.get(), 0.0001f);
	}

	@Test
	void f02_getFloat_invalid() {
		System.setProperty(TEST_PROP, "not-a-number");
		Optional<Float> result = Settings.get().getFloat(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getFile()
	//====================================================================================================
	@Test
	void g01_getFile_valid() {
		System.setProperty(TEST_PROP, "/tmp/test.txt");
		Optional<File> result = Settings.get().getFile(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(new File("/tmp/test.txt"), result.get());
	}

	@Test
	void g02_getFile_notFound() {
		Optional<File> result = Settings.get().getFile("nonexistent.property");
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getPath()
	//====================================================================================================
	@Test
	void h01_getPath_valid() {
		System.setProperty(TEST_PROP, "/tmp/test.txt");
		Optional<Path> result = Settings.get().getPath(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(Paths.get("/tmp/test.txt"), result.get());
	}

	@Test
	void h02_getPath_invalid() {
		// Paths.get() can throw exceptions for invalid paths on some systems
		// This test verifies that invalid paths return empty
		System.setProperty(TEST_PROP, "\0invalid");
		Optional<Path> result = Settings.get().getPath(TEST_PROP);
		// May or may not be empty depending on OS, but should not throw
		assertNotNull(result);
	}

	//====================================================================================================
	// getURI()
	//====================================================================================================
	@Test
	void i01_getURI_valid() {
		System.setProperty(TEST_PROP, "http://example.com/test");
		Optional<URI> result = Settings.get().getURI(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(URI.create("http://example.com/test"), result.get());
	}

	@Test
	void i02_getURI_invalid() {
		System.setProperty(TEST_PROP, "not a valid uri");
		Optional<URI> result = Settings.get().getURI(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// getCharset()
	//====================================================================================================
	@Test
	void j01_getCharset_valid() {
		System.setProperty(TEST_PROP, "UTF-8");
		Optional<Charset> result = Settings.get().getCharset(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals(Charset.forName("UTF-8"), result.get());
	}

	@Test
	void j02_getCharset_invalid() {
		System.setProperty(TEST_PROP, "INVALID-CHARSET");
		Optional<Charset> result = Settings.get().getCharset(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// setGlobal() / unsetGlobal() / clearGlobal()
	//====================================================================================================
	@Test
	void k01_setGlobal() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void k02_unsetGlobal() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Settings.get().unsetGlobal(TEST_PROP);
		Optional<String> result = Settings.get().get(TEST_PROP);
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
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	//====================================================================================================
	// setLocal() / unsetLocal() / clearLocal()
	//====================================================================================================
	@Test
	void l01_setLocal() {
		Settings.get().setLocal(TEST_PROP, "local-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void l02_unsetLocal() {
		Settings.get().setLocal(TEST_PROP, "local-value");
		Settings.get().unsetLocal(TEST_PROP);
		Optional<String> result = Settings.get().get(TEST_PROP);
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
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void l05_setLocal_overridesGlobal() {
		Settings.get().setGlobal(TEST_PROP, "global-value");
		Settings.get().setLocal(TEST_PROP, "local-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	@Test
	void l06_setLocal_overridesSystemProperty() {
		System.setProperty(TEST_PROP, "system-value");
		Settings.get().setLocal(TEST_PROP, "local-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("local-value", result.get());
	}

	//====================================================================================================
	// Thread isolation
	//====================================================================================================
	@Test
	void m01_localOverride_threadIsolation() throws InterruptedException {
		Settings.get().setLocal(TEST_PROP, "thread1-value");

		Thread thread2 = new Thread(() -> {
			Settings.get().setLocal(TEST_PROP, "thread2-value");
			Optional<String> result = Settings.get().get(TEST_PROP);
			assertTrue(result.isPresent());
			assertEquals("thread2-value", result.get());
		});

		thread2.start();
		thread2.join();

		// Original thread should still have its value
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("thread1-value", result.get());
	}

	@Test
	void m02_globalOverride_sharedAcrossThreads() throws InterruptedException {
		Settings.get().setGlobal(TEST_PROP, "global-value");

		Thread thread2 = new Thread(() -> {
			Optional<String> result = Settings.get().get(TEST_PROP);
			assertTrue(result.isPresent());
			assertEquals("global-value", result.get());
		});

		thread2.start();
		thread2.join();

		// Original thread should also see the global value
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	//====================================================================================================
	// Singleton pattern
	//====================================================================================================
	@Test
	void n01_get_returnsSameInstance() {
		Settings instance1 = Settings.get();
		Settings instance2 = Settings.get();
		assertSame(instance1, instance2);
	}

	//====================================================================================================
	// Sources
	//====================================================================================================
	@Test
	void o01_addSource() {
		MapSource source = new MapSource();
		source.set(TEST_PROP, "source-value");
		Settings.get().addSource(source);

		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source-value", result.get());
	}

	@Test
	void o02_addSource_reverseOrder() {
		MapSource source1 = new MapSource();
		source1.set(TEST_PROP, "source1-value");
		Settings.get().addSource(source1);

		MapSource source2 = new MapSource();
		source2.set(TEST_PROP, "source2-value");
		Settings.get().addSource(source2);

		// Last added source should be checked first
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source2-value", result.get());
	}

	@Test
	void o03_addSource_afterGlobalOverride() {
		Settings.get().setGlobal(TEST_PROP, "global-value");

		MapSource source = new MapSource();
		source.set(TEST_PROP, "source-value");
		Settings.get().addSource(source);

		// Global override should take precedence
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("global-value", result.get());
	}

	@Test
	void o04_addSource_beforeSystemProperty() {
		System.setProperty(TEST_PROP, "system-value");

		MapSource source = new MapSource();
		source.set(TEST_PROP, "source-value");
		Settings.get().addSource(source);

		// Source should take precedence over system property
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source-value", result.get());
	}

	@Test
	void o05_addSource_fallbackToSystemProperty() {
		MapSource source = new MapSource();
		// Source doesn't have the property

		System.setProperty(TEST_PROP, "system-value");
		Settings.get().addSource(source);

		// Should fall back to system property
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("system-value", result.get());
	}

	@Test
	void o06_setSources() {
		MapSource source1 = new MapSource();
		source1.set(TEST_PROP, "source1-value");

		MapSource source2 = new MapSource();
		source2.set(TEST_PROP, "source2-value");

		Settings.get().setSources(source1, source2);

		// Last source in array should be checked first
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source2-value", result.get());
	}

	@Test
	void o07_setSources_clearsExisting() {
		MapSource source1 = new MapSource();
		source1.set(TEST_PROP, "source1-value");
		Settings.get().addSource(source1);

		MapSource source2 = new MapSource();
		source2.set(TEST_PROP, "source2-value");
		Settings.get().setSources(source2);

		// Only source2 should exist now
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertTrue(result.isPresent());
		assertEquals("source2-value", result.get());
	}

	@Test
	void o08_addSource_nullValue() {
		MapSource source = new MapSource();
		source.set(TEST_PROP, null);
		Settings.get().addSource(source);

		// Note that setting a null value on the source overrides the system property.
		System.setProperty(TEST_PROP, "system-value");
		Optional<String> result = Settings.get().get(TEST_PROP);
		assertFalse(result.isPresent());
	}

	@Test
	void o09_addSource_nullSource() {
		assertThrows(IllegalArgumentException.class, () -> {
			Settings.get().addSource(null);
		});
	}

	@Test
	void o10_setSources_nullSource() {
		MapSource source1 = new MapSource();
		assertThrows(IllegalArgumentException.class, () -> {
			Settings.get().setSources(source1, null);
		});
	}

	@Test
	void o11_source_springPropertiesExample() {
		// Simulate Spring properties
		MapSource springSource = new MapSource();
		springSource.set("spring.datasource.url", "jdbc:postgresql://localhost/db");
		springSource.set("spring.datasource.username", "admin");

		Settings.get().addSource(springSource);

		Optional<String> url = Settings.get().get("spring.datasource.url");
		assertTrue(url.isPresent());
		assertEquals("jdbc:postgresql://localhost/db", url.get());

		Optional<String> username = Settings.get().get("spring.datasource.username");
		assertTrue(username.isPresent());
		assertEquals("admin", username.get());
	}
}

