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

import java.util.concurrent.*;

import org.junit.*;

public class MemoryStoreTest {

	@Test
	public void testNoFile() throws Exception {
		ConfigMemoryStore fs = ConfigMemoryStore.create().build();
		assertEquals("", fs.read("X.cfg"));
	}

	@Test
	public void testSimpleCreate() throws Exception {
		ConfigMemoryStore fs = ConfigMemoryStore.create().build();
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
	}

	@Test
	public void testFailOnMismatch() throws Exception {
		ConfigMemoryStore fs = ConfigMemoryStore.create().build();
		assertNotNull(fs.write("X.cfg", "xxx", "foo"));
		assertEquals("", fs.read("X.cfg"));
		assertNull(fs.write("X.cfg", null, "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNotNull(fs.write("X.cfg", "xxx", "foo"));
		assertEquals("foo", fs.read("X.cfg"));
		assertNull(fs.write("X.cfg", "foo", "bar"));
		assertEquals("bar", fs.read("X.cfg"));
	}

	@Test
	public void testUpdate() throws Exception {
		ConfigMemoryStore fs = ConfigMemoryStore.create().build();

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
}
