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
		FileStore fs = FileStore.create().directory(DIR).build();
		assertEquals(null, fs.read("X"));
		assertFileNotExists("X.cfg");
	}

	@Test
	public void testDifferentExtension() throws Exception {
		FileStore fs = FileStore.create().directory(DIR).ext("ini").build();
		assertEquals(null, fs.read("X"));
		assertFileNotExists("X.ini");
	}

	@Test
	public void testSimpleCreate() throws Exception {
		FileStore fs = FileStore.create().directory(DIR).build();
		assertTrue(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertFileExists("X.cfg");
	}

	@Test
	public void testFailOnMismatch() throws Exception {
		FileStore fs = FileStore.create().directory(DIR).build();
		assertFalse(fs.write("X", "xxx", "foo"));
		assertEquals(null, fs.read("X"));
		assertFileNotExists("X.cfg");
		assertTrue(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertFalse(fs.write("X", "xxx", "foo"));
		assertEquals("foo", fs.read("X"));
		assertTrue(fs.write("X", "foo", "bar"));
		assertEquals("bar", fs.read("X"));
	}
	
	@Test
	public void testCharset() throws Exception {
		FileStore fs = FileStore.create().directory(DIR).charset("UTF-8").build();
		assertTrue(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
	}		
	
	@Test
	public void testWatcher() throws Exception {
		FileStore fs = FileStore.create().directory(DIR).useWatcher().watcherSensitivity(WatcherSensitivity.HIGH).build();

		final CountDownLatch latch = new CountDownLatch(2);
		final boolean[] error = {false};
		fs.register(new StoreListener() {
			@Override
			public void onChange(String name, String contents) {
				if ("X".equals(name) && "xxx".equals(contents))
					latch.countDown();
				else if ("Y".equals(name) && "yyy".equals(contents))
					latch.countDown();
				else
					error[0] = true;
			}
		});
		IOUtils.write(new File(DIR, "Z.ini"), new StringReader("zzz"));
		IOUtils.write(new File(DIR, "X.cfg"), new StringReader("xxx"));
		IOUtils.write(new File(DIR, "Y.cfg"), new StringReader("yyy"));
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
		assertFalse(error[0]);
	}
	
	@Test
	public void testUpdate() throws Exception {
		FileStore fs = FileStore.create().directory(DIR).build();

		final CountDownLatch latch = new CountDownLatch(2);
		final boolean[] error = {false};
		fs.register(new StoreListener() {
			@Override
			public void onChange(String name, String contents) {
				if ("X".equals(name) && "xxx".equals(contents))
					latch.countDown();
				else if ("Y".equals(name) && "yyy".equals(contents))
					latch.countDown();
				else
					error[0] = true;
			}
		});
		
		fs.update("X", "xxx");
		fs.update("Y", "yyy");
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
		assertFalse(error[0]);
	}	
	
	
	private void assertFileExists(String name) {
		assertTrue(new File(DIR, name).exists());
	}
	
	private void assertFileNotExists(String name) {
		assertTrue(! new File(DIR, name).exists());
	}
	
}
