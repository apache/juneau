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
package org.apache.juneau.config.store;

import static org.junit.Assert.*;

import java.io.*;
import java.util.concurrent.*;

import org.apache.juneau.internal.*;
import org.junit.*;

public class FileStoreTest {

	private static final File DIR = new File("./config");

	@After
	public void cleanUp() {
		FileUtils.delete(DIR);
	}

	@Test
	public void testNoFile() throws Exception {
		ConfigFileStore fs = ConfigFileStore.create().directory(DIR).build();
		assertEquals("", fs.read("X.cfg"));
		assertFileNotExists("X.cfg");
	}

	@Test
	public void testDifferentExtension() throws Exception {
		ConfigFileStore fs = ConfigFileStore.create().directory(DIR).build();
		assertEquals("", fs.read("X.ini"));
		assertFileNotExists("X.ini");
	}

	@Test
	public void testSimpleCreate() throws Exception {
		ConfigFileStore fs = ConfigFileStore.create().directory(DIR).build();
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertFileExists("X.cfg");
	}

	@Test
	public void testFailOnMismatch() throws Exception {
		assertFileNotExists("X.cfg");
		ConfigFileStore fs = ConfigFileStore.create().directory(DIR).build();
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

	@Test
	public void testCharset() throws Exception {
		ConfigFileStore fs = ConfigFileStore.create().directory(DIR).charset("UTF-8").build();
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
	}

	@Test
	public void testWatcher() throws Exception {
		ConfigFileStore fs = ConfigFileStore.create().directory(DIR).useWatcher().watcherSensitivity(WatcherSensitivity.HIGH).build();

		final CountDownLatch latch = new CountDownLatch(2);
		fs.register("X.cfg", new ConfigStoreListener() {
			@Override
			public void onChange(String contents) {
				if ("xxx".equals(contents))
					latch.countDown();
			}
		});
		fs.register("Y.cfg", new ConfigStoreListener() {
			@Override
			public void onChange(String contents) {
				if ("yyy".equals(contents))
					latch.countDown();
			}
		});
		IOUtils.write(new File(DIR, "Z.ini"), new StringReader("zzz"));
		IOUtils.write(new File(DIR, "X.cfg"), new StringReader("xxx"));
		IOUtils.write(new File(DIR, "Y.cfg"), new StringReader("yyy"));
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
	}

	@Test
	public void testUpdate() throws Exception {
		ConfigFileStore fs = ConfigFileStore.create().directory(DIR).build();

		final CountDownLatch latch = new CountDownLatch(2);
		fs.register("X.cfg", new ConfigStoreListener() {
			@Override
			public void onChange(String contents) {
				if ("xxx".equals(contents))
					latch.countDown();
			}
		});
		fs.register("Y.cfg", new ConfigStoreListener() {
			@Override
			public void onChange(String contents) {
				if ("yyy".equals(contents))
					latch.countDown();
			}
		});

		fs.update("X.cfg", "xxx");
		fs.update("Y.cfg", "yyy");
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
	}

	private void assertFileExists(String name) {
		assertTrue(new File(DIR, name).exists());
	}

	private void assertFileNotExists(String name) {
		assertTrue(! new File(DIR, name).exists());
	}
}
