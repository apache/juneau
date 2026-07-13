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

import static org.apache.juneau.commons.utils.Shorts.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link RelaxedPropertySource} — relaxed-binding candidate generation and decorator lookup.
 */
class RelaxedPropertySource_Test extends TestBase {

	/** Simple in-memory delegate keyed by exact name. */
	private static PropertySource map(String... kv) {
		var m = new LinkedHashMap<String,String>();
		for (var i = 0; i < kv.length; i += 2)
			m.put(kv[i], kv[i + 1]);
		return name -> m.containsKey(name) ? PropertyLookupResult.present(o(m.get(name))) : PropertyLookupResult.missing();
	}

	private static String resolve(PropertySource s, String name) {
		var r = s.get(name);
		return r.isPresent() ? r.value().orElse(null) : null;
	}

	// =================================================================================
	// A. Candidate generation
	// =================================================================================

	@Test void a01_candidates_edges() {
		// A name with no alphanumerics tokenizes to nothing -> only the verbatim candidate.
		assertEquals(List.of("_"), RelaxedPropertySource.candidates("_"));
		assertEquals(List.of(""), RelaxedPropertySource.candidates(""));
		assertEquals(List.of("/"), RelaxedPropertySource.candidates("/"));
		// digit->upper camel boundary: "a1B" -> tokens [a1, b].
		assertEquals(List.of("a1B", "A1_B", "a1.b"), RelaxedPropertySource.candidates("a1B"));
	}

	@Test void a02_candidates_basic() {
		// "my.prop": verbatim, then UPPER_UNDERSCORE (=MY_PROP); the lower-dotted form equals verbatim so it dedups out.
		assertEquals(List.of("my.prop", "MY_PROP"), RelaxedPropertySource.candidates("my.prop"));
		assertEquals(List.of("myProp", "MY_PROP", "my.prop"), RelaxedPropertySource.candidates("myProp"));
		// "MY_PROP": verbatim, then lower-dotted (=my.prop); the upper-underscore form equals verbatim so it dedups out.
		assertEquals(List.of("MY_PROP", "my.prop"), RelaxedPropertySource.candidates("MY_PROP"));
		assertEquals(List.of("MySection/myKey", "MY_SECTION_MY_KEY", "my.section.my.key"), RelaxedPropertySource.candidates("MySection/myKey"));
		assertEquals(List.of("my-prop", "MY_PROP", "my.prop"), RelaxedPropertySource.candidates("my-prop"));
	}

	// =================================================================================
	// B. Verbatim-first / exact-match preserved
	// =================================================================================

	@Test void b01_verbatimWins() {
		// Both the exact key and a relaxed variant exist; the exact (verbatim) one must win.
		var s = new RelaxedPropertySource(map("my.prop", "exact", "MY_PROP", "relaxed"));
		assertEquals("exact", resolve(s, "my.prop"));
	}

	@Test void b02_alreadyCanonicalUnchanged() {
		var s = new RelaxedPropertySource(map("MY_PROP", "v"));
		assertEquals("v", resolve(s, "MY_PROP"));
	}

	// =================================================================================
	// C. Relaxed matches
	// =================================================================================

	@Test void c01_dottedResolvesEnvStyle() {
		var s = new RelaxedPropertySource(map("MY_PROP", "v"));
		assertEquals("v", resolve(s, "my.prop"));
	}

	@Test void c02_camelResolvesEnvStyle() {
		var s = new RelaxedPropertySource(map("MY_PROP", "v"));
		assertEquals("v", resolve(s, "myProp"));
	}

	@Test void c03_sectionKeyResolvesEnvStyle() {
		var s = new RelaxedPropertySource(map("MY_SECTION_MY_KEY", "v"));
		assertEquals("v", resolve(s, "MySection/myKey"));
	}

	@Test void c04_envStyleResolvesDotted() {
		var s = new RelaxedPropertySource(map("my.prop", "v"));
		assertEquals("v", resolve(s, "MY_PROP"));
	}

	@Test void c05_missReturnsMissing() {
		var s = new RelaxedPropertySource(map("OTHER", "v"));
		assertNull(resolve(s, "my.prop"));
		assertFalse(s.get("my.prop").isPresent());
	}

	@Test void c06_nullNameMissing() {
		var s = new RelaxedPropertySource(map("X", "v"));
		assertFalse(s.get(null).isPresent());
	}

	@Test void c07_presentNullValuePreserved() {
		// A key present with a null value resolves as present-empty, not missing.
		var s = new RelaxedPropertySource(name -> "MY_PROP".equals(name) ? PropertyLookupResult.present(oe()) : PropertyLookupResult.missing());
		var r = s.get("my.prop");
		assertTrue(r.isPresent());
		assertTrue(r.value().isEmpty());
	}
}
