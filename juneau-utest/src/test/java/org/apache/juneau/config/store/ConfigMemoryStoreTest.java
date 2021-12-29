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
import static org.junit.runners.MethodSorters.*;

import java.util.concurrent.*;

import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ConfigMemoryStoreTest {

	@Test
	public void testNoFile() throws Exception {
		MemoryStore fs = MemoryStore.create().build();
		assertEquals("", fs.read("X"));
	}

	@Test
	public void testSimpleCreate() throws Exception {
		MemoryStore fs = MemoryStore.create().build();
		assertNull(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
	}

	@Test
	public void testFailOnMismatch() throws Exception {
		MemoryStore fs = MemoryStore.create().build();
		assertNotNull(fs.write("X", "xxx", "foo"));
		assertEquals("", fs.read("X"));
		assertNull(fs.write("X", null, "foo"));
		assertEquals("foo", fs.read("X"));
		assertNotNull(fs.write("X", "xxx", "foo"));
		assertEquals("foo", fs.read("X"));
		assertNull(fs.write("X", "foo", "bar"));
		assertEquals("bar", fs.read("X"));
	}

	@Test
	public void testUpdate() throws Exception {
		MemoryStore fs = MemoryStore.create().build();

		final CountDownLatch latch = new CountDownLatch(2);
		fs.register("X", new ConfigStoreListener() {
			@Override
			public void onChange(String contents) {
				if ("xxx".equals(contents))
					latch.countDown();
			}
		});
		fs.register("Y", new ConfigStoreListener() {
			@Override
			public void onChange(String contents) {
				if ("yyy".equals(contents))
					latch.countDown();
			}
		});

		fs.update("X", "xxx");
		fs.update("Y", "yyy");
		if (! latch.await(10, TimeUnit.SECONDS))
			throw new Exception("CountDownLatch never reached zero.");
	}

	@Test
	public void testExists() {
		MemoryStore.DEFAULT.write("foo", null, "foo");

		assertTrue(MemoryStore.DEFAULT.exists("foo"));
		assertFalse(MemoryStore.DEFAULT.exists("foo2"));

		MemoryStore.DEFAULT.write("foo", "foo", null);

		assertFalse(MemoryStore.DEFAULT.exists("foo"));
		assertFalse(MemoryStore.DEFAULT.exists("foo2"));
	}
}
