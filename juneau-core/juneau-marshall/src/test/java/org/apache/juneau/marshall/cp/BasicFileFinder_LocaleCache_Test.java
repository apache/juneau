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
package org.apache.juneau.marshall.cp;

import static org.apache.juneau.commons.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests that the per-locale name cache of {@link BasicFileFinder} stays bounded while lookups keep resolving.
 */
@SuppressWarnings({
	"resource" // Streams are short-lived test fixtures.
})
class BasicFileFinder_LocaleCache_Test {

	@Test void a01_manyLocales_stillResolve_cacheBounded() throws Exception {
		var x = (BasicFileFinder)FileFinder.create().dir(".").caching(100_000).build();
		for (var i = 0; i < 200; i++) {
			var locale = new Locale.Builder().setLanguage("ab" + (char)('a' + i / 26) + (char)('a' + i % 26)).build();
			var is = x.getStream("files/test1/e.txt", locale).orElse(null);
			assertNotNull(is);
			assertTrue(toUtf8(is).contains("[home:/files/test1/e.txt]"));
		}
		assertTrue(x.localeCacheSize() <= 32, "size=" + x.localeCacheSize());
	}

	private static Locale locale(int i) {
		return new Locale.Builder().setLanguage("ab" + (char)('a' + i / 26) + (char)('a' + i % 26)).build();
	}

	@Test void a02_configuredCap_limitsCache() throws Exception {
		var x = (BasicFileFinder)FileFinder.create().dir(".").caching(100_000).maxCachedLocales(5).build();
		for (var i = 0; i < 50; i++) {
			var is = x.getStream("files/test1/e.txt", locale(i)).orElse(null);
			assertNotNull(is);
		}
		assertEquals(5, x.localeCacheSize());
	}

	@Test void a03_zeroCap_cachesNoLocales_stillResolves() throws Exception {
		var x = (BasicFileFinder)FileFinder.create().dir(".").maxCachedLocales(0).build();
		assertTrue(x.getStream("files/test1/e.txt", locale(1)).isPresent());
		assertEquals(0, x.localeCacheSize());
	}

	@Test void a04_negativeCap_rejected() {
		var b = FileFinder.create();
		assertThrows(IllegalArgumentException.class, () -> b.maxCachedLocales(-1));
	}
}
