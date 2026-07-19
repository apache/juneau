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
package org.apache.juneau.marshall.html;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class HtmlSchemaDocSerializer_Test extends TestBase {

	@Test void a01_writeInteger() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write(42);
		assertTrue(result.contains("integer"), "Expected 'integer' in: " + result);
	}

	@Test void a02_writeString() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write("hello");
		assertTrue(result.contains("string"), "Expected 'string' in: " + result);
	}

	@Test void a03_writeBean() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write(new SimpleBean());
		assertTrue(result.contains("object"), "Expected 'object' in: " + result);
		assertTrue(result.contains("f1"), "Expected field 'f1' in: " + result);
	}

	@Test void a04_writeList() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write(List.of("a", "b"));
		assertNotNull(result);
		assertTrue(result.contains("array") || result.contains("string"), "Expected schema output: " + result);
	}

	@Test void a05_writeMap() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write(Map.of("k", "v"));
		assertNotNull(result);
		assertTrue(result.contains("object") || result.contains("string"), "Expected schema output: " + result);
	}

	@Test void a06_writeBoolean() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write(true);
		assertNotNull(result);
		assertTrue(result.contains("boolean") || result.contains("true"), "Expected schema output: " + result);
	}

	@Test void a07_writeArray() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write(new String[] {"a", "b"});
		assertNotNull(result);
		assertTrue(result.contains("array") || result.contains("string"), "Expected schema output: " + result);
	}

	@Test void a08_writeNull() throws Exception {
		var s = HtmlSchemaDocSerializer.create().build();
		var result = s.write(null);
		assertNotNull(result);
	}

	public static class SimpleBean {
		public String f1;
	}

	//====================================================================================================
	// HtmlStrippedDocSerializerSession branch coverage
	//====================================================================================================

	@Test void b01_strippedDocSerializer_nullInput() throws Exception {
		// null → o == null branch → "No Results" output
		var html = HtmlStrippedDocSerializer.DEFAULT.write(null);
		assertNotNull(html);
		assertTrue(html.contains("No Results"), "Expected 'No Results' for null: " + html);
	}

	@Test void b02_strippedDocSerializer_emptyCollection() throws Exception {
		// Empty Collection → isEmpty() branch → "No Results" output
		var html = HtmlStrippedDocSerializer.DEFAULT.write(List.of());
		assertNotNull(html);
		assertTrue(html.contains("No Results"), "Expected 'No Results' for empty list: " + html);
	}

	@Test void b03_strippedDocSerializer_emptyArray() throws Exception {
		// Empty array → Array.getLength == 0 branch → "No Results" output
		var html = HtmlStrippedDocSerializer.DEFAULT.write(new String[0]);
		assertNotNull(html);
		assertTrue(html.contains("No Results"), "Expected 'No Results' for empty array: " + html);
	}

	@Test void b04_strippedDocSerializer_nonEmptyCollection() throws Exception {
		// Non-empty Collection → falls through to super.doWrite
		var html = HtmlStrippedDocSerializer.DEFAULT.write(List.of("a", "b"));
		assertNotNull(html);
		assertFalse(html.contains("No Results"), "Did not expect 'No Results' for non-empty list: " + html);
	}

	@Test void b05_strippedDocSerializer_nonEmptyArray() throws Exception {
		// Non-empty array → isArray check false branch + Array.getLength > 0 → falls through
		var html = HtmlStrippedDocSerializer.DEFAULT.write(new String[]{"a", "b"});
		assertNotNull(html);
		assertFalse(html.contains("No Results"), "Did not expect 'No Results' for non-empty array: " + html);
	}

	@Test void b06_strippedDocSerializer_nonCollectionNonArray() throws Exception {
		// Non-null, non-Collection, non-array → all conditions false → falls through to super
		var html = HtmlStrippedDocSerializer.DEFAULT.write("hello");
		assertNotNull(html);
		assertTrue(html.contains("hello"), "Expected 'hello' in: " + html);
	}

	@Test void b07_strippedDocSerializer_nonEmptyBeanList() throws Exception {
		// Non-empty list of beans → non-empty collection branch
		var html = HtmlStrippedDocSerializer.DEFAULT.write(List.of(new SimpleBean()));
		assertNotNull(html);
		assertFalse(html.contains("No Results"), "Did not expect 'No Results': " + html);
	}
}
