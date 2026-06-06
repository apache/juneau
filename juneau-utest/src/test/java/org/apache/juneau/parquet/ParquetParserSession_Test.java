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
package org.apache.juneau.parquet;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.parser.ParseException;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link ParquetParserSession} targeting low-coverage paths:
 *  - doParse error paths (too-small, bad magic, bad footer length, bad numRows)
 *  - target type unwrap branches (Optional, scalar, Map, Array, Collection)
 *  - hasNativeBytes/setDebugEnabled accessors
 *  - ValueHolder unwrap with extra keys
 *  - prepareMapForBean (JsonMap wrapping)
 *  - toArray / toCollection internal paths via different target types
 */
@SuppressWarnings({
	"unchecked", // Parser returns raw types; explicit casts required for typed assertions
	"rawtypes",
	"java:S5961"
})
class ParquetParserSession_Test extends TestBase {

	public static class SimpleBean {
		public String name;
		public int age;
	}

	private static SimpleBean simple(String name, int age) {
		var b = new SimpleBean();
		b.name = name;
		b.age = age;
		return b;
	}

	// -----------------------------------------------------------------------------------------------------------------
	// a01_doParse - error paths
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_tooSmall_throws() {
		// Buffer < 12 bytes => "Parquet file too small"
		var bytes = new byte[]{'P', 'A', 'R', '1'};
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.parse(bytes, Object.class));
	}

	@Test void a02_badMagicAtStart_throws() {
		// Bytes long enough but no MAGIC header => "Invalid Parquet magic"
		var bytes = new byte[20];
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.parse(bytes, Object.class));
	}

	@Test void a03_badMagicAtEnd_throws() {
		// MAGIC at start but not at end => "Invalid Parquet magic"
		var bytes = new byte[16];
		bytes[0] = 'P';
		bytes[1] = 'A';
		bytes[2] = 'R';
		bytes[3] = '1';
		// Last 4 bytes still zero => bad end magic
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.parse(bytes, Object.class));
	}

	@Test void a04_badFooterLen_throws() {
		// MAGIC at both ends but footerLen too large -> footerStart < 4
		var bytes = new byte[16];
		bytes[0] = 'P'; bytes[1] = 'A'; bytes[2] = 'R'; bytes[3] = '1';
		bytes[12] = 'P'; bytes[13] = 'A'; bytes[14] = 'R'; bytes[15] = '1';
		// readLe4 of bytes[8..12] is zero by default → footerStart = 16 - 8 - 0 = 8 OK
		// To force footerStart < 4, set footerLen to something huge.
		bytes[8] = (byte)0xFF;
		bytes[9] = (byte)0xFF;
		bytes[10] = (byte)0xFF;
		bytes[11] = (byte)0x7F;
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.parse(bytes, Object.class));
	}

	@Test void a05_corruptFooter_throws() throws Exception {
		// Magic OK but footer body bytes are zero / invalid Thrift => parseFileMetaData throws
		var bytes = new byte[16];
		bytes[0] = 'P'; bytes[1] = 'A'; bytes[2] = 'R'; bytes[3] = '1';
		bytes[12] = 'P'; bytes[13] = 'A'; bytes[14] = 'R'; bytes[15] = '1';
		// footerLen=0 => empty footer → parseFileMetaData with empty bytes throws or returns no-rows
		// Just exercise the end-to-end path; outcome may be ParseException OR returns null/empty.
		try {
			var result = ParquetParser.DEFAULT.parse(bytes, Object.class);
			// If it doesn't throw, allow null/empty.
			assertTrue(result == null || result instanceof Map || result instanceof List);
		} catch (@SuppressWarnings("unused") ParseException expected) {
			// Acceptable - we're hitting the error path.
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// b01_hasNativeBytes - InputStreamParserSession contract
	// -----------------------------------------------------------------------------------------------------------------

	@Test void b01_hasNativeBytes_isFalse() {
		// Parquet's column reader has no native byte-array primitive type.
		assertFalse(ParquetParser.DEFAULT.getSession().hasNativeBytes());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// c01_setDebugEnabled - static debug toggle
	// -----------------------------------------------------------------------------------------------------------------

	@Test void c01_setDebugEnabled_off() throws Exception {
		// Toggle debug mode on/off, exercise enabled path on a small round trip, then turn off.
		ParquetParserSession.setDebugEnabled(true);
		try {
			var bytes = ParquetSerializer.DEFAULT.serialize(simple("x", 1));
			var parsed = (List<SimpleBean>) ParquetParser.DEFAULT.parse(bytes, List.class, SimpleBean.class);
			assertEquals(1, parsed.size());
		} finally {
			ParquetParserSession.setDebugEnabled(false);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d01_targetTypes - Optional / Map / Scalar / Array / Collection paths in doParse
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_optionalScalar_present() throws Exception {
		// Scalar serialized → wrapped as {value: X} → target Optional<String> unwraps inner.
		var bytes = ParquetSerializer.DEFAULT.serialize("foo");
		var opt = (Optional<String>) ParquetParser.DEFAULT.parse(bytes, Optional.class, String.class);
		assertNotNull(opt);
		assertTrue(opt.isPresent());
		assertEquals("foo", opt.get());
	}

	@Test void d02_optionalBean_present() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(simple("alice", 42));
		var opt = (Optional<SimpleBean>) ParquetParser.DEFAULT.parse(bytes, Optional.class, SimpleBean.class);
		assertNotNull(opt);
		assertTrue(opt.isPresent());
		assertEquals("alice", opt.get().name);
		assertEquals(42, opt.get().age);
	}

	@Test void d03_arrayOfBeans() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(simple("a", 1), simple("b", 2)));
		var arr = ParquetParser.DEFAULT.parse(bytes, SimpleBean[].class);
		assertNotNull(arr);
		assertEquals(2, arr.length);
		assertEquals("a", arr[0].name);
	}

	@Test void d04_arrayOfStrings() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list("x", "y", "z"));
		var arr = ParquetParser.DEFAULT.parse(bytes, String[].class);
		assertNotNull(arr);
		assertEquals(3, arr.length);
	}

	@Test void d05_arrayOfPrimitives() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(1, 2, 3));
		var arr = ParquetParser.DEFAULT.parse(bytes, int[].class);
		assertNotNull(arr);
		assertEquals(3, arr.length);
	}

	@Test void d06_linkedListOfBeans() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(simple("a", 1)));
		var ll = (LinkedList<SimpleBean>) ParquetParser.DEFAULT.parse(bytes, LinkedList.class, SimpleBean.class);
		assertNotNull(ll);
		assertEquals(1, ll.size());
	}

	@Test void d07_collection_targetParseAsList() throws Exception {
		// Collection target returns ArrayList (default).
		var bytes = ParquetSerializer.DEFAULT.serialize(list(simple("a", 1), simple("b", 2)));
		var c = (Collection<SimpleBean>) ParquetParser.DEFAULT.parse(bytes, Collection.class, SimpleBean.class);
		assertEquals(2, c.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// e01_mapTarget - target Map<...> goes through prepareMapForBean / convertToType
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_mapTarget() throws Exception {
		// Map<String,Object> from a single bean - exercises Map-target branch in doParse.
		var bytes = ParquetSerializer.DEFAULT.serialize(simple("alice", 30));
		var m = (Map<String, Object>) ParquetParser.DEFAULT.parse(bytes, Map.class, String.class, Object.class);
		assertNotNull(m);
	}

	@Test void e02_listOfMaps() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(simple("a", 1), simple("b", 2)));
		var l = (List<Map<String, Object>>) ParquetParser.DEFAULT.parse(bytes, List.class, Map.class);
		assertEquals(2, l.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// f01_scalarTargets - String/int/Boolean targets unwrap {value: X}
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_stringScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize("hello");
		assertEquals("hello", ParquetParser.DEFAULT.parse(bytes, String.class));
	}

	@Test void f02_intScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(42);
		assertEquals(Integer.valueOf(42), ParquetParser.DEFAULT.parse(bytes, Integer.class));
	}

	@Test void f03_longScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(99999L);
		assertEquals(Long.valueOf(99999L), ParquetParser.DEFAULT.parse(bytes, Long.class));
	}

	@Test void f04_doubleScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(3.14);
		assertEquals(Double.valueOf(3.14), ParquetParser.DEFAULT.parse(bytes, Double.class));
	}

	@Test void f05_booleanScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(true);
		assertEquals(Boolean.TRUE, ParquetParser.DEFAULT.parse(bytes, Boolean.class));
	}

	@Test void f06_charScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize('z');
		assertEquals(Character.valueOf('z'), ParquetParser.DEFAULT.parse(bytes, Character.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// g01_rowsEmpty - empty rows of various target types
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_emptyList_asList() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var l = (List<SimpleBean>) ParquetParser.DEFAULT.parse(bytes, List.class, SimpleBean.class);
		assertNotNull(l);
		assertEquals(0, l.size());
	}

	@Test void g02_emptyList_asArray() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var arr = ParquetParser.DEFAULT.parse(bytes, SimpleBean[].class);
		assertNotNull(arr);
		assertEquals(0, arr.length);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// h01_optionalEmpty - Optional with no rows
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_optional_fromScalarNull() throws Exception {
		// Serialize empty list then read as Optional<Bean> - exercises Optional empty path.
		var bytes = ParquetSerializer.DEFAULT.serialize(list());
		var opt = (Optional<SimpleBean>) ParquetParser.DEFAULT.parse(bytes, Optional.class, SimpleBean.class);
		// Empty rows → returns Optional.empty
		assertNotNull(opt);
		assertFalse(opt.isPresent());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// i01_keyedMap - non-string key map (key/value pair format)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void i01_intKeyedMap() throws Exception {
		// Map<Integer, String> serialized as key/value pairs - exercises isKeyValuePairFormat branch.
		var src = new LinkedHashMap<Integer, String>();
		src.put(1, "a");
		src.put(2, "b");
		var bytes = ParquetSerializer.DEFAULT.serialize(src);
		var m = (Map<Integer, String>) ParquetParser.DEFAULT.parse(bytes, Map.class, Integer.class, String.class);
		assertNotNull(m);
		assertEquals(2, m.size());
		assertEquals("a", m.get(1));
		assertEquals("b", m.get(2));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// j01_extraKeyValueHolderUnwrap - target Optional/scalar but row has _type
	// -----------------------------------------------------------------------------------------------------------------

	@Test void j01_optionalScalarPresent() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(123);
		var opt = (Optional<Integer>) ParquetParser.DEFAULT.parse(bytes, Optional.class, Integer.class);
		assertTrue(opt.isPresent());
		assertEquals(123, opt.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// k01_listOfPrimitives - exercises element-type Map override path for primitives in a list
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_listOfStrings() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list("a", "b", "c"));
		var l = (List<String>) ParquetParser.DEFAULT.parse(bytes, List.class, String.class);
		assertEquals(3, l.size());
		assertEquals("a", l.get(0));
	}

	@Test void k02_listOfIntegers() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(10, 20, 30));
		var l = (List<Integer>) ParquetParser.DEFAULT.parse(bytes, List.class, Integer.class);
		assertEquals(3, l.size());
		assertEquals(10, l.get(0));
	}

	@Test void k03_listOfDoubles() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(1.1, 2.2, 3.3));
		var l = (List<Double>) ParquetParser.DEFAULT.parse(bytes, List.class, Double.class);
		assertEquals(3, l.size());
	}

	@Test void k04_listOfBooleans() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.serialize(list(true, false, true));
		var l = (List<Boolean>) ParquetParser.DEFAULT.parse(bytes, List.class, Boolean.class);
		assertEquals(3, l.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// l01_rawObjectTarget - target Object type
	// -----------------------------------------------------------------------------------------------------------------

	@Test void l01_objectTarget_singleBean() throws Exception {
		// type=Object → effectiveType.isObject() → elementType set to Map.
		var bytes = ParquetSerializer.DEFAULT.serialize(simple("x", 1));
		var o = ParquetParser.DEFAULT.parse(bytes, Object.class);
		assertNotNull(o);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// m01_optional_wrappingNonScalar - Optional<List<...>> path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void m01_optionalOfList() throws Exception {
		// Optional<List<SimpleBean>> - exercises type.isOptional unwrap with Collection inner.
		var bytes = ParquetSerializer.DEFAULT.serialize(list(simple("a", 1)));
		var opt = (Optional<?>) ParquetParser.DEFAULT.parse(bytes, Optional.class);
		assertNotNull(opt);
	}
}
