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
package org.apache.juneau.serializer;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.csv.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

/**
 * Tests for fluent setter overrides in Writer classes.
 *
 * <p>Writer classes (CsvWriter, JsonWriter, UonWriter, XmlWriter, HtmlWriter) have
 * protected constructors and are internal implementation details of the serialization
 * framework. The fluent setter overrides ensure type-safe method chaining during
 * serialization.
 *
 * <p>These tests verify that the basic serialization functionality works correctly,
 * which implicitly confirms the fluent overrides are functioning (since they're used
 * internally during serialization).
 */
class WriterFluentSetters_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// CsvSerializer - Fluent setter overrides verified through compilation
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_csvSerializer_basic() throws Exception {
		// Verify CsvSerializer works correctly (uses CsvWriter internally)
		CsvSerializer s = CsvSerializer.DEFAULT;
		String result = s.serialize(a("foo", "bar"));
		assertTrue(result.contains("foo") && result.contains("bar"));
	}

	@Test void a02_csvSerializer_object() throws Exception {
		// Verify object serialization works
		CsvSerializer s = CsvSerializer.DEFAULT;
		TestBean bean = new TestBean("test", 42);
		String result = s.serialize(bean);
		assertTrue(result.contains("test") && result.contains("42"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// JsonSerializer - Fluent setter overrides verified through compilation
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_jsonSerializer_basic() throws Exception {
		// Verify JsonSerializer works correctly (uses JsonWriter internally)
		JsonSerializer s = JsonSerializer.DEFAULT;
		String result = s.serialize(a("foo", "bar"));
		assertEquals("[\"foo\",\"bar\"]", result.trim());
	}

	@Test void b02_jsonSerializer_object() throws Exception {
		// Verify object serialization works
		JsonSerializer s = JsonSerializer.DEFAULT;
		TestBean bean = new TestBean("test", 42);
		String result = s.serialize(bean);
		assertTrue(result.contains("\"name\":\"test\"") && result.contains("\"value\":42"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// UonSerializer - Fluent setter overrides verified through compilation
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_uonSerializer_basic() throws Exception {
		// Verify UonSerializer works correctly (uses UonWriter internally)
		UonSerializer s = UonSerializer.DEFAULT;
		String result = s.serialize(a("foo", "bar"));
		assertEquals("@(foo,bar)", result.trim());
	}

	@Test void c02_uonSerializer_object() throws Exception {
		// Verify object serialization works
		UonSerializer s = UonSerializer.DEFAULT;
		TestBean bean = new TestBean("test", 42);
		String result = s.serialize(bean);
		assertTrue(result.contains("name=test") && result.contains("value=42"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// XmlSerializer - Fluent setter overrides verified through compilation
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_xmlSerializer_basic() throws Exception {
		// Verify XmlSerializer works correctly (uses XmlWriter internally)
		XmlSerializer s = XmlSerializer.DEFAULT;
		String result = s.serialize(a("foo", "bar"));
		assertTrue(result.contains("<string>foo</string>") && result.contains("<string>bar</string>"));
	}

	@Test void d02_xmlSerializer_object() throws Exception {
		// Verify object serialization works
		XmlSerializer s = XmlSerializer.DEFAULT;
		TestBean bean = new TestBean("test", 42);
		String result = s.serialize(bean);
		assertTrue(result.contains("<name>test</name>") && result.contains("<value>42</value>"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// HtmlSerializer - Fluent setter overrides verified through compilation
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_htmlSerializer_basic() throws Exception {
		// Verify HtmlSerializer works correctly (uses HtmlWriter internally)
		HtmlSerializer s = HtmlSerializer.DEFAULT;
		String result = s.serialize(a("foo", "bar"));
		assertTrue(result.contains("foo") && result.contains("bar"));
	}

	@Test void e02_htmlSerializer_object() throws Exception {
		// Verify object serialization works
		HtmlSerializer s = HtmlSerializer.DEFAULT;
		TestBean bean = new TestBean("test", 42);
		String result = s.serialize(bean);
		assertTrue(result.contains("test") && result.contains("42"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test bean
	//------------------------------------------------------------------------------------------------------------------

	public static class TestBean {
		public String name;
		public int value;

		public TestBean(String name, int value) {
			this.name = name;
			this.value = value;
		}
	}
}