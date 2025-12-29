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

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.commons.utils.FileUtils.*;
import static org.apache.juneau.commons.utils.IoUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ConfigFileStoreTest extends TestBase {

	private static final File DIR = new File("./target/config");

	@AfterEach void cleanUp() {
		deleteFile(DIR);
	}

	@Test void a01_noFile() throws Exception {
		var fs = FileStore.create().directory(DIR).build();
		assertEquals("", fs.read("X.cfg"));
		assertFileNotExists("X.cfg");
	}

	@Test void a02_differentExtension() throws Exception {
		var fs = FileStore.create().directory(DIR).build();
		assertEquals("", fs.read("X.xxx"));
		assertFileNotExists("X.xxx");
	}

	@Test void a03_simpleCreateAndDelete() throws Exception {
		var fs = FileStore.create().directory(DIR).build();
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertFileExists("X.cfg");
		fs.write("X.cfg", "foo", null);
		assertFileNotExists("X.cfg");
	}

	@Test void a04_simpleCreateAndDeleteWithNoExtension() throws Exception {
		var fs = FileStore.create().directory(DIR).build();
		assertNull(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertFileExists("X.cfg");
		fs.write("X", "foo", null);
		assertFileNotExists("X.cfg");
	}

	@Test void a05_simpleCreateAndDeleteWithNonStandardExtension() throws Exception {
		var fs = FileStore.create().directory(DIR).extensions("xxx").build();
		assertNull(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertFileExists("X.xxx");
		fs.write("X", "foo", null);
		assertFileNotExists("X.xxx");
	}

	@Test void a06_simpleCreateAndDeleteWithMultipleSpecialExtension() throws Exception {
		var fs = FileStore.create().directory(DIR).extensions("foo1,foo2").build();
		assertNull(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertFileExists("X.foo1");
		fs.write("X", "foo", null);
		assertFileNotExists("X.foo1");
	}

	@Test void a07_failOnMismatch() throws Exception {
		assertFileNotExists("X.cfg");
		var fs = FileStore.create().directory(DIR).build();
		assertNotNull(fs.write("X.cfg", "xxx", "foo"));
		assertFileNotExists("X.cfg");
		assertEquals("", fs.read("X.cfg"));
		assertFileNotExists("X.cfg");
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNotNull(fs.write("X.cfg", "xxx", "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNull(fs.write("X.cfg", "foo", "bar"));
		assertEquals("bar", fs.read("X.cfg"));
	}

	@Test void a08_failOnMismatchNoExtension() throws Exception {
		assertFileNotExists("X.cfg");
		var fs = FileStore.create().directory(DIR).build();
		assertNotNull(fs.write("X", "xxx", "foo"));
		assertFileNotExists("X.cfg");
		assertEquals("", fs.read("X"));
		assertEquals("", fs.read("X.cfg"));
		assertFileNotExists("X.cfg");
		assertNull(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNotNull(fs.write("X", "xxx", "foo"));
		assertEquals("foo", fs.read("X"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNull(fs.write("X", "foo", "bar"));
		assertEquals("bar", fs.read("X"));
		assertEquals("bar", fs.read("X.cfg"));
	}

	@Test void a09_charset() throws Exception {
		var fs = FileStore.create().directory(DIR).charset(UTF8).build();
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertEquals("foo", fs.read("X"));
	}

	@Test void a10_charsetNoExtension() throws Exception {
		var fs = FileStore.create().directory(DIR).charset(UTF8).build();
		assertNull(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertEquals("foo", fs.read("X.cfg"));
	}

	@Test void a11_watcher_LONGRUNNING() throws Exception {
		var fs = FileStore.create().directory(DIR).enableWatcher().watcherSensitivity(WatcherSensitivity.HIGH).build();

		final var latch = new CountDownLatch(4);
		fs.register("X.cfg", contents -> {
			if ("xxx".equals(contents))
				latch.countDown();
		});
		fs.register("X", contents -> {
			if ("xxx".equals(contents))
				latch.countDown();
		});
		fs.register("Y.cfg", contents -> {
			if ("yyy".equals(contents))
				latch.countDown();
		});
		fs.register("Y", contents -> {
			if ("yyy".equals(contents))
				latch.countDown();
		});
		pipe(reader("zzz"), new File(DIR, "Z.ini"));
		pipe(reader("xxx"), new File(DIR, "X.cfg"));
		assertDoesNotThrow(()->pipe(reader("yyy"), new File(DIR, "Y.cfg")));
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
	}

	@Test void a12_update() throws Exception {
		var fs = FileStore.create().directory(DIR).build();

		final var latch = new CountDownLatch(4);
		fs.register("X.cfg", contents -> {
			if ("xxx".equals(contents))
				latch.countDown();
		});
		fs.register("X", contents -> {
			if ("xxx".equals(contents))
				latch.countDown();
		});
		fs.register("Y.cfg", contents -> {
			if ("yyy".equals(contents))
				latch.countDown();
		});
		fs.register("Y", contents -> {
			if ("yyy".equals(contents))
				latch.countDown();
		});

		fs.update("X.cfg", "xxx");
		assertDoesNotThrow(()->fs.update("Y.cfg", "yyy"));
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
	}

	@Test void a13_exists() throws IOException {
		var cs = FileStore.DEFAULT;
		assertTrue(cs.exists("test.cfg"));
		assertTrue(cs.exists("test"));
		assertFalse(cs.exists("test2.cfg"));

		assertFalse(cs.exists("foo.cfg"));
		cs.write("foo.cfg", null, "foo");
		assertTrue(cs.exists("foo.cfg"));
		assertTrue(cs.exists("foo"));
		cs.write("foo.cfg", "foo", null);
		assertFalse(cs.exists("foo.cfg"));
		assertFalse(cs.exists("foo"));

		pipe(reader("xxx"), new File("Foox.cfg"));
		assertTrue(cs.exists("Foox.cfg"));
		assertTrue(cs.exists("Foox"));
		new File("Foox.cfg").delete();
		assertFalse(cs.exists("Foox.cfg"));
		assertFalse(cs.exists("Foox"));
	}

	private static void assertFileExists(String name) {
		assertTrue(new File(DIR, name).exists());
	}

	private static void assertFileNotExists(String name) {
		assertTrue(! new File(DIR, name).exists());
	}
}