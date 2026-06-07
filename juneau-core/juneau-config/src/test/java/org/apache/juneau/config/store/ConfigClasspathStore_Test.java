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
package org.apache.juneau.config.store;

import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ConfigClasspathStore_Test extends TestBase {

	@Test void a01_noFile() throws Exception {
		var fs = ClasspathStore.create().build();
		assertEquals("", fs.read("X.cfg"));
	}

	@Test void a02_realFiles() throws Exception {
		var fs = ClasspathStore.create().build();
		assertContains("bar1", fs.read("foo1.cfg"));
		assertContains("bar2", fs.read("sub/foo2.cfg"));
		assertEquals("", fs.read("sub/bad.cfg"));
		assertEquals("", fs.read("bad/bad.cfg"));
	}

	@Test void a03_overwriteRealFiles() throws Exception {
		var fs = ClasspathStore.create().build();
		assertContains("bar1", fs.read("foo1.cfg"));
		fs.write("foo1.cfg", fs.read("foo1.cfg"), "xxx");
		assertEquals("xxx", fs.read("foo1.cfg"));
	}

	@Test void a04_simpleCreate() throws Exception {
		var fs = ClasspathStore.create().build();
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
	}

	@Test void a05_failOnMismatch() throws Exception {
		var fs = ClasspathStore.create().build();
		assertNotNull(fs.write("X.cfg", "xxx", "foo"));
		assertEquals("", fs.read("X.cfg"));
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNotNull(fs.write("X.cfg", "xxx", "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNull(fs.write("X.cfg", "foo", "bar"));
		assertEquals("bar", fs.read("X.cfg"));
	}

	@Test void a06_update() throws Exception {
		var fs = ClasspathStore.create().build();

		final var latch = new CountDownLatch(2);
		fs.register("X.cfg", contents -> {
			if ("xxx".equals(contents))
				latch.countDown();
		});
		fs.register("Y.cfg", contents -> {
			if ("yyy".equals(contents))
				latch.countDown();
		});

		fs.update("X.cfg", "xxx");
		assertDoesNotThrow(()->fs.update("Y.cfg", "yyy"));
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
	}

	@Test void a07_exists() throws Exception {
		ClasspathStore.DEFAULT.write("foo.cfg", null, "foo");

		assertTrue(ClasspathStore.DEFAULT.exists("foo.cfg"));
		assertFalse(ClasspathStore.DEFAULT.exists("foo2.cfg"));

		ClasspathStore.DEFAULT.write("foo.cfg", "foo", null);

		assertFalse(ClasspathStore.DEFAULT.exists("foo.cfg"));
		assertFalse(ClasspathStore.DEFAULT.exists("foo2.cfg"));
	}

	//====================================================================================================
	// Builder fluent override methods
	//====================================================================================================

	@Test void b01_builder_debug() {
		var fs = ClasspathStore.create().debug().build();
		assertNotNull(fs);
	}

	@Test void b02_builder_debugBoolean() {
		var fs = ClasspathStore.create().debug(true).build();
		assertNotNull(fs);
	}

	@Test void b03_builder_copy() {
		var b = ClasspathStore.create().debug(true);
		var b2 = b.copy();
		assertNotNull(b2.build());
	}

	@Test void b04_builder_type() {
		var fs = ClasspathStore.create().type(ClasspathStore.class).build();
		assertNotNull(fs);
	}

	@Test void b05_builder_applyAnnotationsFromClass() {
		var fs = ClasspathStore.create().applyAnnotations(String.class).build();
		assertNotNull(fs);
	}

	@Test void b06_builder_applyAnnotationsFromObject() {
		var fs = ClasspathStore.create().applyAnnotations("foo").build();
		assertNotNull(fs);
	}

	@Test void b07_context_copy() {
		var fs = ClasspathStore.create().build();
		var copy = fs.copy().build();
		assertNotNull(copy);
	}

	@Test void b08_builder_copyFromInstance() {
		var fs = ClasspathStore.DEFAULT;
		var b = fs.copy();
		assertNotNull(b.build());
	}

	@Test void b09_builder_annotations() throws Exception {
		var fs = ClasspathStore.create().annotations().build();
		assertNotNull(fs);
	}

	@Test void b10_builder_cache() throws Exception {
		var fs = ClasspathStore.create().cache(null).build();
		assertNotNull(fs);
	}

	@Test void b11_builder_impl() throws Exception {
		var impl = ClasspathStore.DEFAULT;
		var fs = ClasspathStore.create().impl(impl).build();
		assertNotNull(fs);
	}

	@Test void b12_close() throws Exception {
		var fs = ClasspathStore.create().build();
		assertDoesNotThrow(fs::close);
	}

	@Test void b13_write_sameContents_noOp() throws Exception {
		var fs = ClasspathStore.create().build();
		assertNull(fs.write("same.cfg", "same", "same"));
	}

	@Test void b14_unregister_noListener() throws Exception {
		var fs = ClasspathStore.create().build();
		assertDoesNotThrow(() -> fs.unregister("nonexistent.cfg", s -> {}));
	}
}