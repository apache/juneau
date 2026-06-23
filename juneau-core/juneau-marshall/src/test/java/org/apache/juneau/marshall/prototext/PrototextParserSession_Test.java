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
package org.apache.juneau.marshall.prototext;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;

/**
 * Tests targeting low-coverage paths in {@link PrototextParserSession}.
 */
class PrototextParserSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// doParse scalar paths
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_parseNull_returnsNullOrDefault() throws Exception {
		// Empty input → isEmpty(root) → newInstance path
		var r = PrototextParser.DEFAULT.parse("", SimpleBean.class);
		assertNotNull(r);
	}

	@Test void a02_parse_valueSingleField() throws Exception {
		// Single "_value" key in root map → convertValue path
		var r = PrototextParser.DEFAULT.parse("_value: 42", Long.class);
		assertEquals(42L, r);
	}

	@Test void a03_parse_byteArraySingleValue() throws Exception {
		// Single "_value" raw string for byte[] target → swap path
		var proto = PrototextSerializer.DEFAULT.serialize(new byte[]{1, 2, 3});
		var result = PrototextParser.DEFAULT.parse(proto, byte[].class);
		assertNotNull(result);
		assertArrayEquals(new byte[]{1, 2, 3}, result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseMessage: semicolon/comma separator, angle brace, list message items
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_parseSemicolonSeparator() throws Exception {
		// Semicolon separator between fields → t2.type() == SEMICOLON branch
		var r = PrototextParser.DEFAULT.parse("name: \"Alice\"; age: 30", JsonMap.class);
		assertEquals("Alice", r.get("name"));
		assertEquals(30L, r.get("age"));
	}

	@Test void b02_parseCommaSeparator() throws Exception {
		// Comma separator → t2.type() == COMMA branch
		var r = PrototextParser.DEFAULT.parse("a: 1, b: 2", JsonMap.class);
		assertEquals(1L, r.get("a"));
		assertEquals(2L, r.get("b"));
	}

	@Test void b03_parseAngleBrace() throws Exception {
		// Angle brace syntax for nested messages → LANGLE/RANGLE path
		var r = PrototextParser.DEFAULT.parse("nested < x: \"hello\" >", JsonMap.class);
		assertNotNull(r.get("nested"));
	}

	@Test void b04_parseListWithMessageItems() throws Exception {
		// List with nested message items → parseScalarOrMessageInList LBRACE branch
		var r = PrototextParser.DEFAULT.parse("items: [{name: \"a\"}, {name: \"b\"}]", JsonMap.class);
		var items = (List<?>) r.get("items");
		assertNotNull(items);
		assertEquals(2, items.size());
	}

	@Test void b05_parseFieldNameAsInteger() throws Exception {
		// Numeric field name (integer key) → readFieldName DEC_INT path
		var r = PrototextParser.DEFAULT.parse("0: \"zero\" 1: \"one\"", JsonMap.class);
		assertNotNull(r);
	}

	@Test void b06_parseRepeatedFieldToList() throws Exception {
		// Same field name repeated → merges into List (result.containsKey(fieldName))
		var r = PrototextParser.DEFAULT.parse("tag: \"a\"\ntag: \"b\"\ntag: \"c\"", JsonMap.class);
		var v = r.get("tag");
		assertInstanceOf(List.class, v);
		assertEquals(3, ((List<?>) v).size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// parseScalarValue: special IDENT values
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_parseIdentTrue() throws Exception {
		// "true" ident → Boolean.TRUE
		var r = PrototextParser.DEFAULT.parse("flag: true", JsonMap.class);
		assertEquals(Boolean.TRUE, r.get("flag"));
	}

	@Test void c02_parseIdentFalse() throws Exception {
		// "false" ident → Boolean.FALSE
		var r = PrototextParser.DEFAULT.parse("flag: false", JsonMap.class);
		assertEquals(Boolean.FALSE, r.get("flag"));
	}

	@Test void c03_parseIdentT() throws Exception {
		// "t" → Boolean.TRUE (short form)
		var r = PrototextParser.DEFAULT.parse("flag: t", JsonMap.class);
		assertEquals(Boolean.TRUE, r.get("flag"));
	}

	@Test void c04_parseIdentF() throws Exception {
		// "f" → Boolean.FALSE (short form)
		var r = PrototextParser.DEFAULT.parse("flag: f", JsonMap.class);
		assertEquals(Boolean.FALSE, r.get("flag"));
	}

	@Test void c05_parseIdentInf() throws Exception {
		// "inf" → Double.POSITIVE_INFINITY
		var r = PrototextParser.DEFAULT.parse("val: inf", JsonMap.class);
		assertEquals(Double.POSITIVE_INFINITY, r.get("val"));
	}

	@Test void c06_parseIdentNegInf() throws Exception {
		// "-inf" → Double.NEGATIVE_INFINITY
		var r = PrototextParser.DEFAULT.parse("val: -inf", JsonMap.class);
		assertEquals(Double.NEGATIVE_INFINITY, r.get("val"));
	}

	@Test void c07_parseIdentNaN() throws Exception {
		// "nan" → Double.NaN
		var r = PrototextParser.DEFAULT.parse("val: nan", JsonMap.class);
		assertTrue(Double.isNaN((Double) r.get("val")));
	}

	@Test void c08_parseIdentUnknownString() throws Exception {
		// Unknown ident → yield as string
		var r = PrototextParser.DEFAULT.parse("status: ACTIVE", JsonMap.class);
		assertEquals("ACTIVE", r.get("status"));
	}

	@Test void c09_parseHexInteger() throws Exception {
		// HEX_INT field tag
		var r = PrototextParser.DEFAULT.parse("value: 0xFF", JsonMap.class);
		assertNotNull(r.get("value"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// convertMapToType: typed Map keys and bean type resolution
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_parseToBean() throws Exception {
		// convertMapToType → bean path
		var r = PrototextParser.DEFAULT.parse("name: \"Alice\"\nage: 30", SimpleBean.class);
		assertEquals("Alice", r.name);
		assertEquals(30, r.age);
	}

	@Test void d02_parseToIgnoreUnknown() throws Exception {
		// populateBeanMap: pMeta == null && ignoreUnknownBeanProperties → continue
		var p = PrototextParser.create().ignoreUnknownBeanProperties().build();
		var r = p.parse("name: \"Alice\"\nage: 30\nunknown: \"x\"", SimpleBean.class);
		assertEquals("Alice", r.name);
	}

	@Test void d03_parseToMap_stringKey() throws Exception {
		// convertMapToType → isMap() → default string key path → return map
		@SuppressWarnings("unchecked")
		var r = (Map<String, String>) PrototextParser.DEFAULT.parse("a: \"1\" b: \"2\"", Map.class, String.class, String.class);
		assertNotNull(r);
	}

	@Test void d04_parseNestedBeanAsBeanProperty() throws Exception {
		// convertValue: val instanceof Map → targetType.isBean() → populateBeanMap
		var r = PrototextParser.DEFAULT.parse("address {\n  city: \"Boston\"\n  zip: \"02101\"\n}", BeanWithAddressProp.class);
		assertNotNull(r.address);
		assertEquals("Boston", r.address.city);
	}

	@Test void d05_parseListAsBeanProperty() throws Exception {
		// convertValue: val instanceof List → isCollectionOrArray → result list
		var r = PrototextParser.DEFAULT.parse("tags: [\"a\", \"b\", \"c\"]", BeanWithTagsProp.class);
		assertNotNull(r.tags);
		assertEquals(3, r.tags.size());
	}

	@Test void d06_parseListAsArrayProperty() throws Exception {
		// convertValue: val instanceof List → isArray → toArray
		var r = PrototextParser.DEFAULT.parse("values: [1, 2, 3]", BeanWithIntArrayProp.class);
		assertNotNull(r.values);
		assertEquals(3, r.values.length);
	}

	@Test void d07_parseMultipleStringConcatenation() throws Exception {
		// parseScalarValue: adjacent string tokens are concatenated
		var r = PrototextParser.DEFAULT.parse("text: \"hello\" \" world\"", JsonMap.class);
		assertEquals("hello world", r.get("text"));
	}

	@Test void d08_parseMapInsideMapAsBeanProperty() throws Exception {
		// convertValue: nested map → targetType.isMap() → toJsonMap
		var r = PrototextParser.DEFAULT.parse("metadata { key: \"val\" }", BeanWithMapProp.class);
		assertNotNull(r.metadata);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Test fixture beans
	//------------------------------------------------------------------------------------------------------------------

	public static class SimpleBean {
		public String name;
		public int age;
	}

	public static class Address {
		public String city;
		public String zip;
	}

	public static class BeanWithAddressProp {
		public Address address;
	}

	public static class BeanWithTagsProp {
		public List<String> tags;
	}

	public static class BeanWithIntArrayProp {
		public int[] values;
	}

	public static class BeanWithMapProp {
		public Map<String, Object> metadata;
	}
}
