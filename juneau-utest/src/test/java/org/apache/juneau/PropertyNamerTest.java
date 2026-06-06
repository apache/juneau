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
package org.apache.juneau;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.apache.juneau.commons.bean.*;

class PropertyNamerTest extends TestBase {

	//====================================================================================================
	// test dashed-lower-case
	//====================================================================================================
	@Test void a01_dLC() {
		var n = new PropertyNamerDLC();

		assertEquals("abc", n.getPropertyName("ABC"));
		assertEquals("abc", n.getPropertyName("abc"));
		assertEquals("foo-bar-baz", n.getPropertyName("FooBarBaz"));
		assertEquals("foo-bar-baz", n.getPropertyName("FooBarBAZ"));
		assertEquals("foo-bar-baz", n.getPropertyName("fooBarBAZ"));
		assertEquals("", n.getPropertyName(""));
		assertNull(n.getPropertyName(null));
		assertEquals("a", n.getPropertyName("A"));
		assertEquals("a", n.getPropertyName("A"));
	}

	//====================================================================================================
	// test underscore-lower-case
	//====================================================================================================
	@Test void a02_uLC() {
		var n = new PropertyNamerULC();

		assertEquals("abc", n.getPropertyName("ABC"));
		assertEquals("abc", n.getPropertyName("abc"));
		assertEquals("foo_bar_baz", n.getPropertyName("FooBarBaz"));
		assertEquals("foo_bar_baz", n.getPropertyName("FooBarBAZ"));
		assertEquals("foo_bar_baz", n.getPropertyName("fooBarBAZ"));
		assertEquals("", n.getPropertyName(""));
		assertNull(n.getPropertyName(null));
		assertEquals("a", n.getPropertyName("A"));
		assertEquals("a", n.getPropertyName("A"));
	}

	//====================================================================================================
	// test dashed-upper-case-start
	//====================================================================================================
	@Test void a03_dUCS() {
		var n = new PropertyNamerDUCS();

		// Examples from class javadoc.
		assertEquals("Foo-Bar", n.getPropertyName("fooBar"));
		assertEquals("Foo-Bar-Url", n.getPropertyName("fooBarURL"));
		assertEquals("Foo-Bar-Url", n.getPropertyName("FooBarURL"));

		// Single-character / single-word inputs.
		assertEquals("A", n.getPropertyName("A"));
		assertEquals("A", n.getPropertyName("a"));
		assertEquals("Abc", n.getPropertyName("abc"));
		assertEquals("Abc", n.getPropertyName("Abc"));

		// All-caps acronym is normalized to a single capitalized word.
		assertEquals("Abc", n.getPropertyName("ABC"));

		// Multi-word camelCase / PascalCase.
		assertEquals("Foo-Bar-Baz", n.getPropertyName("FooBarBaz"));
		assertEquals("Foo-Bar-Baz", n.getPropertyName("fooBarBaz"));
		assertEquals("Foo-Bar-Baz", n.getPropertyName("FooBarBAZ"));
		assertEquals("Foo-Bar-Baz", n.getPropertyName("fooBarBAZ"));

		// Empty / null are passed through unchanged.
		assertEquals("", n.getPropertyName(""));
		assertNull(n.getPropertyName(null));
	}

	//====================================================================================================
	// test reusable INSTANCE constants
	//====================================================================================================
	@Test void a04_instances() {
		assertNotNull(PropertyNamerDLC.INSTANCE);
		assertNotNull(PropertyNamerULC.INSTANCE);
		assertNotNull(PropertyNamerDUCS.INSTANCE);
		assertEquals("foo-bar", PropertyNamerDLC.INSTANCE.getPropertyName("fooBar"));
		assertEquals("foo_bar", PropertyNamerULC.INSTANCE.getPropertyName("fooBar"));
		assertEquals("Foo-Bar", PropertyNamerDUCS.INSTANCE.getPropertyName("fooBar"));
	}
}