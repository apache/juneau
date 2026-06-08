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
package org.apache.juneau.marshall.urlencoding;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-targeted tests for {@link UrlEncodingParserSession}.
 *
 * <p>Targets gaps in:
 * <ul>
 *  <li>Builder.expandedParams(boolean) and Builder.property(...) overrides
 *  <li>parseAnything paths: collection/array/args, Optional, "not a bean", _value unwrap
 *  <li>parseIntoBeanMap state-machine: empty value, unknown property, expanded-params
 *  <li>parseIntoMap2 state-machine: array indices, repeated keys, null/empty values
 *  <li>doParseIntoMap with leading '?' and without
 * </ul>
 */
@SuppressWarnings({
	"rawtypes",
	"java:S5961",
	"unused",      // Unused parameters/variables kept for consistent method signatures across test utilities.
	"java:S125",   // Commented-out code is retained as historical reference / future re-enable candidate.
	"java:S5778",  // Lambda intentionally calls multiple throwing methods to test compound failure scenarios.
	"java:S5976"   // Separate test methods preferred over parameterized for clarity and independent failure reporting.
})
class UrlEncodingParserSession_Test extends TestBase {

	private static final UrlEncodingParser P = UrlEncodingParser.DEFAULT;

	//------------------------------------------------------------------------------------------------------------------
	// a01-a04: Builder property overrides
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_builderProperty_shortKey_true() {
		var session = UrlEncodingParser.DEFAULT.createSession()
			.property("expandedParams", true)
			.build();
		assertTrue(session.isExpandedParams());
	}

	@Test void a02_builderProperty_shortKey_false() {
		var p = UrlEncodingParser.create().expandedParams().build();
		var session = p.createSession()
			.property("expandedParams", false)
			.build();
		assertFalse(session.isExpandedParams());
	}

	@Test void a03_builderProperty_longKey() {
		var session = UrlEncodingParser.DEFAULT.createSession()
			.property("UrlEncodingParserSession.expandedParams", "true")
			.build();
		assertTrue(session.isExpandedParams());
	}

	@Test void a04_builderProperty_unknownKey_passesThrough() {
		// Falls through to super.property() -> default branch.
		var session = UrlEncodingParser.DEFAULT.createSession()
			.property("someOtherKey", "value")
			.build();
		assertNotNull(session);
	}

	@Test void a05_builderProperty_nullKey() {
		// Triggers the (key == null) early-return branch which delegates to super.property().
		// super.property() throws IllegalArgumentException on null key; we still execute the early-return code.
		assertThrows(IllegalArgumentException.class, () -> UrlEncodingParser.DEFAULT.createSession()
			.property(null, "value")
			.build());
	}

	//------------------------------------------------------------------------------------------------------------------
	// b01-b: Collections / arrays / numeric-indexed maps (parseAnything sType.isCollection/isArray)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_arrayInput() throws Exception {
		var v = P.parse("?0=a&1=b&2=c", String[].class);
		assertArrayEquals(new String[]{"a","b","c"}, v);
	}

	@Test void b02_listInput() throws Exception {
		var v = P.parse("?0=a&1=b&2=c", List.class, String.class);
		assertEquals(List.of("a","b","c"), v);
	}

	@Test void b03_intArrayInput() throws Exception {
		var v = P.parse("?0=1&1=2&2=3", int[].class);
		assertArrayEquals(new int[]{1,2,3}, v);
	}

	@Test void b04_outOfOrderIndices_sortedByTreeMap() throws Exception {
		// TreeMap<Integer,Object> sorts the indices regardless of arrival order.
		var v = P.parse("?2=c&0=a&1=b", String[].class);
		assertArrayEquals(new String[]{"a","b","c"}, v);
	}

	@Test void b05_argsInput() throws Exception {
		var v = P.parseArgs("?0=foo&1=42", new java.lang.reflect.Type[]{ String.class, Integer.class });
		assertEquals("foo", v[0]);
		assertEquals(Integer.valueOf(42), v[1]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c01-c: parseAnything _value unwrap routing
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_valueUnwrap_string() throws Exception {
		// Routes through "if (m.containsKey(CONST_value)) o = m.get(CONST_value);" branch in object-type path.
		assertEquals("hello", P.parse("_value=hello", Object.class));
	}

	@Test void c02_valueUnwrap_intoNonBeanType() throws Exception {
		// Goes through unwrapValueAs path (sType is not Map/Collection/Bean).
		assertEquals(Integer.valueOf(42), P.parse("_value=42", Integer.class));
	}

	@Test void c03_valueUnwrap_intoLong() throws Exception {
		assertEquals(Long.valueOf(123), P.parse("_value=123", Long.class));
	}

	@Test void c04_valueUnwrap_intoBoolean() throws Exception {
		assertEquals(Boolean.TRUE, P.parse("_value=true", Boolean.class));
	}

	@Test void c05_optional_present() throws Exception {
		// Optional path: parseAnything recurses with element type.
		Optional<?> v = P.parse("_value=foo", Optional.class, String.class);
		assertTrue(v.isPresent());
		assertEquals("foo", v.get());
	}

	@Test void c06_optional_empty_input() throws Exception {
		// Empty value parses to empty string (which Optional wraps).
		Optional<?> v = P.parse("_value=", Optional.class, String.class);
		assertTrue(v.isPresent());
		assertEquals("", v.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// d01-d: parseIntoBeanMap state-machine paths
	//------------------------------------------------------------------------------------------------------------------

	public static class Bean1 {
		public String f1;
		public int f2;
	}

	@Test void d01_emptyInput() throws Exception {
		// r.peekSkipWs() == -1 -> early return with empty bean instance.
		var b = P.parse("", Bean1.class);
		assertNotNull(b);
		assertNull(b.f1);
	}

	@Test void d02_attrOnly_noEquals_nullValue() throws Exception {
		// state==S2, c==-1 -> m.put(currAttr, null); return m.
		var m = P.parse("?f1", JsonMap.class);
		assertTrue(m.containsKey("f1"));
		assertNull(m.get("f1"));
	}

	@Test void d03_attrOnly_separator_nullValue_thenAnother() throws Exception {
		// state==S2, c=='<sep>' -> null + state=S1.
		var b = P.parse("?f1&f2=5", Bean1.class);
		assertNull(b.f1);
		assertEquals(5, b.f2);
	}

	@Test void d04_emptyValue_createsEmptyInstance() throws Exception {
		// state==S3, c=='<sep>' or -1 with creatable type. Empty array path.
		var b = P.parse("?f1=&f2=5", Bean1.class);
		assertEquals("", b.f1);
		assertEquals(5, b.f2);
	}

	@Test void d05_emptyValue_arrayProperty() throws Exception {
		// In bean map, empty value for an array-typed property creates an empty instance.
		var b = P.parse("?f1=", BeanArr.class);
		assertNotNull(b.f1);
		assertEquals(0, b.f1.length);
	}

	public static class BeanArr {
		public String[] f1;
	}

	@Test void d06_unknownProperty_emptyValue() throws Exception {
		// Unknown property with empty value path.
		var p = UrlEncodingParser.create().ignoreUnknownBeanProperties().build();
		var b = p.parse("?unknown=&f1=hi", Bean1.class);
		assertEquals("hi", b.f1);
	}

	@Test void d07_unknownProperty_withValue() throws Exception {
		// Unknown property with value; onUnknownProperty path.
		var p = UrlEncodingParser.create().ignoreUnknownBeanProperties().build();
		var b = p.parse("?unknown=stuff&f1=hi", Bean1.class);
		assertEquals("hi", b.f1);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e: Expanded params path through parseIntoBeanMap (shouldUseExpandedParams)
	//------------------------------------------------------------------------------------------------------------------

	public static class BeanList {
		public List<String> f1;
	}

	@Test void e01_expandedParams_repeatedKey_addsToList() throws Exception {
		var p = UrlEncodingParser.create().expandedParams().build();
		var b = p.parse("?f1=a&f1=b&f1=c", BeanList.class);
		assertEquals(List.of("a","b","c"), b.f1);
	}

	@Test void e02_expandedParams_singleKey_listType() throws Exception {
		var p = UrlEncodingParser.create().expandedParams().build();
		var b = p.parse("?f1=onlyOne", BeanList.class);
		assertEquals(List.of("onlyOne"), b.f1);
	}

	@Test void e03_expandedParams_arrayBean() throws Exception {
		var p = UrlEncodingParser.create().expandedParams().build();
		var b = p.parse("?f1=x&f1=y", BeanArr.class);
		assertArrayEquals(new String[]{"x","y"}, b.f1);
	}

	@Test void e04_expandedParams_emptyValue_arrayBean() throws Exception {
		var p = UrlEncodingParser.create().expandedParams().build();
		var b = p.parse("?f1=", BeanArr.class);
		assertNotNull(b.f1);
		// Empty sentinel results in empty array (round-trip from serializer).
		assertEquals(0, b.f1.length);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f: parseIntoMap2 paths via parseIntoMap public API
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_doParseIntoMap_withQuestionMark() throws Exception {
		// doParseIntoMap: r.peekSkipWs() == '?' -> read.
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?a=1&b=2", m, String.class, String.class);
		assertEquals("1", m.get("a"));
		assertEquals("2", m.get("b"));
	}

	@Test void f02_doParseIntoMap_withoutQuestionMark() throws Exception {
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("a=1&b=2", m, String.class, String.class);
		assertEquals("1", m.get("a"));
		assertEquals("2", m.get("b"));
	}

	@Test void f03_doParseIntoMap_emptyInput() throws Exception {
		// Triggers c==-1 early-return in parseIntoMap2.
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("", m, String.class, String.class);
		assertTrue(m.isEmpty());
	}

	@Test void f04_repeatedKey_objectValue_promotesToList() throws Exception {
		// parseIntoMap2: when valueType.isObject() and key seen twice, promote to JsonList.
		var m = (Map) P.parse("?a=1&a=2&a=3", Map.class);
		Object v = m.get("a");
		assertTrue(v instanceof List, "Expected list, got: " + v.getClass());
		assertEquals(3, ((List)v).size());
	}

	@Test void f05_repeatedKey_stringValue_overwrites() throws Exception {
		// valueType.isString() so no promotion; last wins.
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?a=1&a=2", m, String.class, String.class);
		assertEquals("2", m.get("a"));
	}

	@Test void f06_emptyValue_S3_eof() throws Exception {
		// parseIntoMap2: state==S3, c==-1 -> empty conversion, m.put.
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?a=", m, String.class, String.class);
		assertEquals("", m.get("a"));
	}

	@Test void f07_emptyValue_S3_separator() throws Exception {
		// parseIntoMap2: state==S3, c=='<sep>' -> empty conversion, state=S1.
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?a=&b=v", m, String.class, String.class);
		assertEquals("", m.get("a"));
		assertEquals("v", m.get("b"));
	}

	@Test void f08_attrNoEquals_S2_eof() throws Exception {
		// parseIntoMap2: state==S2, c==-1 with no '='.
		Map<String,Object> m = new LinkedHashMap<>();
		P.parseIntoMap("?a", m, String.class, Object.class);
		assertTrue(m.containsKey("a"));
		assertNull(m.get("a"));
	}

	@Test void f09_attrNoEquals_S2_separator() throws Exception {
		// parseIntoMap2: state==S2, c=='<sep>' with no '='.
		Map<String,Object> m = new LinkedHashMap<>();
		P.parseIntoMap("?a&b=v", m, String.class, Object.class);
		assertTrue(m.containsKey("a"));
		assertNull(m.get("a"));
		assertEquals("v", m.get("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g: Encoded values
	//------------------------------------------------------------------------------------------------------------------

	@Test void g01_percentEncodedUtf8_2byte() throws Exception {
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?k=%C2%A2", m, String.class, String.class);
		assertEquals("¢", m.get("k"));
	}

	@Test void g02_percentEncodedUtf8_3byte() throws Exception {
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?k=%E2%82%AC", m, String.class, String.class);
		assertEquals("€", m.get("k"));
	}

	@Test void g03_plusAsSpace() throws Exception {
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?k=hello+world", m, String.class, String.class);
		// '+' is decoded as space by the URL-encoded parser.
		assertEquals("hello world", m.get("k"));
	}

	@Test void g04_specialCharsEncoded() throws Exception {
		Map<String,String> m = new LinkedHashMap<>();
		P.parseIntoMap("?k=%26%3D%3F", m, String.class, String.class);
		assertEquals("&=?", m.get("k"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// h: Map-into-Map with custom map type
	//------------------------------------------------------------------------------------------------------------------

	@Test void h01_customMapType() throws Exception {
		Map<String,String> m = P.parse("?a=1&b=2", TreeMap.class, String.class, String.class);
		assertEquals("1", m.get("a"));
		assertEquals("2", m.get("b"));
	}

	@Test void h02_nestedMapValue() throws Exception {
		var m = (Map) P.parse("?a=(x=1,y=2)", Map.class);
		assertTrue(m.get("a") instanceof Map);
		assertEquals("1", ((Map)m.get("a")).get("x").toString());
	}

	//------------------------------------------------------------------------------------------------------------------
	// i: Malformed input
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_malformed_nonBeanClass() {
		// Class without no-arg ctor and without _type/_value: triggers "Malformed" or "not-a-bean" throw path.
		assertThrows(ParseException.class, () -> P.parse("?x=1", java.lang.Thread.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// j: Non-default outer (parseAnything outer != null branch)
	//------------------------------------------------------------------------------------------------------------------

	public static class OuterBean {
		public InnerBean inner;
	}

	public static class InnerBean {
		public String val;
	}

	@Test void j01_nestedBean() throws Exception {
		// Nested bean parse path through parseIntoBeanMap.
		var b = P.parse("?inner=(val=hello)", OuterBean.class);
		assertNotNull(b.inner);
		assertEquals("hello", b.inner.val);
	}

	//------------------------------------------------------------------------------------------------------------------
	// k: Leading '?' branch in parseAnything (line 166-170)
	//------------------------------------------------------------------------------------------------------------------

	@Test void k01_leadingQuestionMark_intoBean() throws Exception {
		var b = P.parse("?f1=foo&f2=99", Bean1.class);
		assertEquals("foo", b.f1);
		assertEquals(99, b.f2);
	}

	@Test void k02_noLeadingQuestionMark_intoBean() throws Exception {
		var b = P.parse("f1=foo&f2=99", Bean1.class);
		assertEquals("foo", b.f1);
		assertEquals(99, b.f2);
	}

	//------------------------------------------------------------------------------------------------------------------
	// l: shouldUseExpandedParams getter
	//------------------------------------------------------------------------------------------------------------------

	@Test void l01_isExpandedParams_default_false() {
		var s = UrlEncodingParser.DEFAULT.createSession().build();
		assertFalse(s.isExpandedParams());
	}

	@Test void l02_isExpandedParams_set_true() {
		var s = UrlEncodingParser.create().expandedParams().build()
			.createSession().build();
		assertTrue(s.isExpandedParams());
	}

	//------------------------------------------------------------------------------------------------------------------
	// m: Bean-type _type marker path (parseAnything object branch)
	//------------------------------------------------------------------------------------------------------------------

	@org.apache.juneau.marshall.Marshalled(typeName="b1")
	public static class TypedBean {
		public String x;
	}

	@Test void m01_typeMarker() throws Exception {
		// parseAnything: m.containsKey(getBeanTypePropertyName(eType)) -> cast(m, null, eType).
		// Use beanDictionary-aware parser.
		var p = UrlEncodingParser.create().beanDictionary(TypedBean.class).build();
		Object v = p.parse("?_type=b1&x=hello", Object.class);
		assertTrue(v instanceof TypedBean, "got: " + v.getClass().getName());
		assertEquals("hello", ((TypedBean)v).x);
	}

	//------------------------------------------------------------------------------------------------------------------
	// n: Args parsing - covers parseIntoMap2 type.isArgs() branches (lines 416-419)
	//------------------------------------------------------------------------------------------------------------------

	@Test void n01_args_threeTypes() throws Exception {
		Object[] v = P.parseArgs("?0=foo&1=42&2=true",
			new java.lang.reflect.Type[]{String.class, Integer.class, Boolean.class});
		assertEquals("foo", v[0]);
		assertEquals(Integer.valueOf(42), v[1]);
		assertEquals(Boolean.TRUE, v[2]);
	}

	@Test void n02_args_emptyValueInArgs() throws Exception {
		// state==S3, c==-1 with isArgs path -> argIndex++ for each entry.
		Object[] v = P.parseArgs("?0=&1=x",
			new java.lang.reflect.Type[]{String.class, String.class});
		assertEquals("", v[0]);
		assertEquals("x", v[1]);
	}

	//------------------------------------------------------------------------------------------------------------------
	// o: Null-attr (%00 attribute) -> parseAttrName returns null -> parseIntoBeanMap returns null
	//------------------------------------------------------------------------------------------------------------------

	@Test void o01_nullAttrName() throws Exception {
		// %00 as an attribute name causes parseAttrName to return null and parseIntoBeanMap to return null.
		// The wrapping parseAnything() treats null as a null bean -> builder/canCreateNewBean branch returns null.
		var p = UrlEncodingParser.create().ignoreUnknownBeanProperties().build();
		Bean1 b = p.parse("?%00=value", Bean1.class);
		// %00 attr name path; bean still constructed.
		assertNotNull(b);
	}

	//------------------------------------------------------------------------------------------------------------------
	// p: Custom Map subtypes
	//------------------------------------------------------------------------------------------------------------------

	@Test void p01_customMapSubtype() throws Exception {
		// parseAnything sType.isMap() with canCreateNewInstance true -> sType.newInstance().
		Map<String,String> m = P.parse("?a=1", LinkedHashMap.class, String.class, String.class);
		assertEquals("1", m.get("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// q: Object key value path -> JsonList promotion (parseIntoMap2 with object value type, repeated keys)
	//------------------------------------------------------------------------------------------------------------------

	@Test void q01_repeatedKey_promotesAlreadyList() throws Exception {
		// Repeat 4 times to exercise the "v2 instanceof MarshalledList -> add" branch.
		var m = (Map) P.parse("?a=1&a=2&a=3&a=4", Map.class);
		Object v = m.get("a");
		assertTrue(v instanceof List);
		assertEquals(4, ((List)v).size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// r: Collection with no-arg ctor (canCreateNewInstance true -> newInstance branch in line 195)
	//------------------------------------------------------------------------------------------------------------------

	@Test void r01_collectionWithCtor() throws Exception {
		// LinkedList has a no-arg ctor; parseAnything line 195 ternary uses sType.newInstance().
		List<String> v = P.parse("?0=a&1=b", LinkedList.class, String.class);
		assertEquals(2, v.size());
	}

	//------------------------------------------------------------------------------------------------------------------
	// s: ParserPipe-driven explicit doParseIntoMap with leading '?'
	//------------------------------------------------------------------------------------------------------------------

	@Test void s01_typedMap_strings() throws Exception {
		Map<String,String> m = new TreeMap<>();
		P.parseIntoMap("?b=2&a=1&c=3", m, String.class, String.class);
		assertEquals(3, m.size());
		assertEquals("1", m.get("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// t: Optional wrapper around primitive
	//------------------------------------------------------------------------------------------------------------------

	@Test void t01_optionalInteger() throws Exception {
		Optional<?> v = P.parse("_value=42", Optional.class, Integer.class);
		assertTrue(v.isPresent());
		assertEquals(Integer.valueOf(42), v.get());
	}

	//------------------------------------------------------------------------------------------------------------------
	// u: Bean property setter exception. Triggers BeanRuntimeException catch (lines 326-338).
	//------------------------------------------------------------------------------------------------------------------

	public static class BadSetterBean {
		public String f1;
		public void setF1(String v) {
			throw new RuntimeException("bad setter");
		}
	}

	@Test void u01_setterExceptionInExpandedParams() {
		// Setter throws -> BeanRuntimeException -> caught and rethrown.
		var p = UrlEncodingParser.create().expandedParams().build();
		assertThrows(Exception.class, () -> p.parse("?f1=a", BadSetterBean.class));
	}

	@Test void u02_setterExceptionInRegular() {
		assertThrows(Exception.class, () -> P.parse("?f1=a", BadSetterBean.class));
	}
}
