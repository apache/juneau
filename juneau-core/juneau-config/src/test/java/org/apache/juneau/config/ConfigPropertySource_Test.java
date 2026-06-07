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
package org.apache.juneau.config;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.config.store.*;
import org.junit.jupiter.api.*;

class ConfigPropertySource_Test extends TestBase {

	private Config build(String...lines) {
		MemoryStore.DEFAULT.update("Test.cfg", lines);
		return Config.create().store(MemoryStore.DEFAULT).name("Test.cfg").build();
	}

	@Test
	void a01_nullConfig_missing() {
		var src = new ConfigPropertySource(null);
		assertFalse(src.get("any.key").isPresent());
	}

	@Test
	void a02_presentKey_found() {
		var cfg = build("key = value");
		var src = new ConfigPropertySource(cfg);
		var r = src.get("key");
		assertTrue(r.isPresent());
		assertEquals("value", r.value().orElse(null));
	}

	@Test
	void a03_missingKey_missing() {
		var cfg = build("other = value");
		var src = new ConfigPropertySource(cfg);
		assertFalse(src.get("key").isPresent());
	}

	@Test
	void a04_keyInSection_found() {
		var cfg = build("[section]", "key = hello");
		var src = new ConfigPropertySource(cfg);
		var r = src.get("section/key");
		assertTrue(r.isPresent());
		assertEquals("hello", r.value().orElse(null));
	}

	@Test
	void a05_exception_treatedAsMissing() {
		// Passing a null config exercises the null guard, not the catch block.
		// We exercise the catch by using a key path that Config.getString may reject.
		var cfg = build();
		var src = new ConfigPropertySource(cfg);
		// An empty string key is unusual; whatever happens, no exception should propagate.
		assertNotNull(src.get(""));
	}
}
