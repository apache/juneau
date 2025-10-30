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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.http.*;
import org.apache.http.message.*;
import org.junit.jupiter.api.*;

/**
 * Tests for {@link MediaRange} fluent setter overrides.
 */
class MediaRange_Test extends TestBase {

	@Test void a01_basic() {
		// Test basic MediaRange creation
		HeaderElement element = BasicHeaderValueParser.parseHeaderElement("text/html;charset=UTF-8;q=0.9", null);

		MediaRange x = new MediaRange(element);

		assertEquals("text", x.getType());
		assertEquals("html", x.getSubType());
		assertEquals(0.9f, x.getQValue(), 0.01f);
	}

	@Test void a02_forEachParameter_fluentChaining() {
		// Test that forEachParameter returns MediaRange for fluent chaining
		HeaderElement element = BasicHeaderValueParser.parseHeaderElement("text/html;charset=UTF-8;level=1", null);

		MediaRange x = new MediaRange(element);

		// Test fluent chaining
		List<String> names = list();
		MediaRange result = x.forEachParameter(p -> names.add(p.getName()));

		// Verify it returns MediaRange (not MediaType)
		assertSame(x, result);
		assertInstanceOf(MediaRange.class, result);

		// Verify parameters were processed
		assertTrue(names.contains("charset"));
		assertTrue(names.contains("level"));
	}

	@Test void a03_forEachParameter_withConsumer() {
		// Test that forEachParameter properly iterates parameters
		HeaderElement element = BasicHeaderValueParser.parseHeaderElement("application/json;charset=UTF-8;version=2", null);

		MediaRange x = new MediaRange(element);

		// Collect all parameter values
		var params = new HashMap<String, String>();
		x.forEachParameter(p -> params.put(p.getName(), p.getValue()));

		assertEquals("UTF-8", params.get("charset"));
		assertEquals("2", params.get("version"));
	}

	@Test void a04_forEachParameter_emptyParameters() {
		// Test with no parameters
		HeaderElement element = BasicHeaderValueParser.parseHeaderElement("text/plain", null);

		MediaRange x = new MediaRange(element);

		// Should not throw exception with empty parameters
		int[] count = {0};
		MediaRange result = x.forEachParameter(p -> count[0]++);

		assertSame(x, result);
		assertEquals(0, count[0]);
	}

	@Test void a05_fluentChaining_combined() {
		// Test chaining forEachParameter multiple times
		HeaderElement element = BasicHeaderValueParser.parseHeaderElement("text/html;charset=UTF-8;level=1", null);

		MediaRange x = new MediaRange(element);

		// Chain multiple calls
		List<String> names1 = list();
		List<String> names2 = list();

		x.forEachParameter(p -> names1.add(p.getName()))
		 .forEachParameter(p -> names2.add(p.getName()));

		// Both should have captured the same parameters
		assertEquals(names1.size(), names2.size());
		assertTrue(names1.contains("charset"));
		assertTrue(names2.contains("charset"));
	}

	@Test void a06_forEachParameter_withExtensions() {
		// Test that both forEachParameter and forEachExtension APIs work
		HeaderElement element = BasicHeaderValueParser.parseHeaderElement("text/html;q=0.9", null);

		MediaRange x = new MediaRange(element);

		// Test that forEachParameter can be called (returns MediaRange)
		MediaRange result = x.forEachParameter(p -> {});
		assertSame(x, result);
		assertInstanceOf(MediaRange.class, result);

		// Test that forEachExtension also works
		result = x.forEachExtension(e -> {});
		assertSame(x, result);
		assertInstanceOf(MediaRange.class, result);
	}
}