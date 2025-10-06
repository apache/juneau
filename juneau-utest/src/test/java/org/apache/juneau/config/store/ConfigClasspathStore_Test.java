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

import static org.apache.juneau.TestUtils.*;
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
}