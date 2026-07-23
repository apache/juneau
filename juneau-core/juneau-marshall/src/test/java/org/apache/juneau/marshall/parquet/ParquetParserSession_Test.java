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
package org.apache.juneau.marshall.parquet;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link ParquetParserSession} targeting low-coverage paths:
 *  - doRead error paths (too-small, bad magic, bad footer length, bad numRows)
 *  - target type unwrap branches (Optional, scalar, Map, Array, Collection)
 *  - hasNativeBytes/setDebugEnabled accessors
 *  - ValueHolder unwrap with extra keys
 *  - prepareMapForBean (JsonMap wrapping)
 *  - toArray / toCollection internal paths via different target types
 */
@SuppressWarnings({
	"unchecked",   // Parser returns raw types; explicit casts required for typed assertions
	"java:S5961",  // High assertion count is acceptable in comprehensive data-driven test methods.
	"java:S125"    // Commented-out code is retained as historical reference / future re-enable candidate.
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
	// a01_doRead - error paths
	// -----------------------------------------------------------------------------------------------------------------

	@Test void a01_tooSmall_throws() {
		// Buffer < 12 bytes => "Parquet file too small"
		var bytes = new byte[]{'P', 'A', 'R', '1'};
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.read(bytes, Object.class));
	}

	@Test void a02_badMagicAtStart_throws() {
		// Bytes long enough but no MAGIC header => "Invalid Parquet magic"
		var bytes = new byte[20];
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.read(bytes, Object.class));
	}

	@Test void a03_badMagicAtEnd_throws() {
		// MAGIC at start but not at end => "Invalid Parquet magic"
		var bytes = new byte[16];
		bytes[0] = 'P';
		bytes[1] = 'A';
		bytes[2] = 'R';
		bytes[3] = '1';
		// Last 4 bytes still zero => bad end magic
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.read(bytes, Object.class));
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
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.read(bytes, Object.class));
	}

	@Test void a05_corruptFooter_throws() throws Exception {
		// Magic OK but footer body bytes are zero / invalid Thrift => readFileMetaData throws
		var bytes = new byte[16];
		bytes[0] = 'P'; bytes[1] = 'A'; bytes[2] = 'R'; bytes[3] = '1';
		bytes[12] = 'P'; bytes[13] = 'A'; bytes[14] = 'R'; bytes[15] = '1';
		// footerLen=0 => empty footer → readFileMetaData with empty bytes throws or returns no-rows
		// Just exercise the end-to-end path; outcome may be ParseException OR returns null/empty.
		try {
			var result = ParquetParser.DEFAULT.read(bytes, Object.class);
			// If it doesn't throw, allow null/empty.
			assertTrue(result == null || result instanceof Map || result instanceof List);
		} catch (@SuppressWarnings("unused") ParseException expected) {
			// Acceptable - we're hitting the error path.
		}
	}

	private static byte[] buildParquetWithNumRows(long numRows) throws IOException {
		var footer = ThriftCompactEncoder.encodeToBytes(enc -> {
			enc.writeStructBegin();
			enc.writeFieldBegin(ThriftCompactEncoder.I64, 3); // field 3 = num_rows
			enc.writeI64(numRows);
			enc.writeStructEnd();
		});
		var out = new ByteArrayOutputStream();
		out.write(new byte[]{'P', 'A', 'R', '1'});
		out.write(footer);
		int flen = footer.length;
		out.write(flen & 0xFF);
		out.write((flen >> 8) & 0xFF);
		out.write((flen >> 16) & 0xFF);
		out.write((flen >> 24) & 0xFF);
		out.write(new byte[]{'P', 'A', 'R', '1'});
		return out.toByteArray();
	}

	@Test void a06_negativeNumRows_throws() throws Exception {
		var bytes = buildParquetWithNumRows(-1L);
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.read(bytes, Object.class));
	}

	@Test void a07_tooLargeNumRows_throws() throws Exception {
		var bytes = buildParquetWithNumRows(Long.MAX_VALUE);
		assertThrows(ParseException.class, () -> ParquetParser.DEFAULT.read(bytes, Object.class));
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
			var bytes = ParquetSerializer.DEFAULT.write(simple("x", 1));
			var parsed = (List<SimpleBean>) ParquetParser.DEFAULT.read(bytes, List.class, SimpleBean.class);
			assertEquals(1, parsed.size());
		} finally {
			ParquetParserSession.setDebugEnabled(false);
		}
	}

	// -----------------------------------------------------------------------------------------------------------------
	// d01_targetTypes - Optional / Map / Scalar / Array / Collection paths in doRead
	// -----------------------------------------------------------------------------------------------------------------

	@Test void d01_optionalScalar_present() throws Exception {
		// Scalar serialized → wrapped as {value: X} → target Optional<String> unwraps inner.
		var bytes = ParquetSerializer.DEFAULT.write("foo");
		var opt = (Optional<String>) ParquetParser.DEFAULT.read(bytes, Optional.class, String.class);
		assertNotNull(opt);
		assertTrue(opt.isPresent());
		assertEquals("foo", opt.get());
	}

	@Test void d02_optionalBean_present() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(simple("alice", 42));
		var opt = (Optional<SimpleBean>) ParquetParser.DEFAULT.read(bytes, Optional.class, SimpleBean.class);
		assertNotNull(opt);
		assertTrue(opt.isPresent());
		assertEquals("alice", opt.get().name);
		assertEquals(42, opt.get().age);
	}

	@Test void d03_arrayOfBeans() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(simple("a", 1), simple("b", 2)));
		var arr = ParquetParser.DEFAULT.read(bytes, SimpleBean[].class);
		assertNotNull(arr);
		assertEquals(2, arr.length);
		assertEquals("a", arr[0].name);
	}

	@Test void d04_arrayOfStrings() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list("x", "y", "z"));
		var arr = ParquetParser.DEFAULT.read(bytes, String[].class);
		assertNotNull(arr);
		assertEquals(3, arr.length);
	}

	@Test void d05_arrayOfPrimitives() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(1, 2, 3));
		var arr = ParquetParser.DEFAULT.read(bytes, int[].class);
		assertNotNull(arr);
		assertEquals(3, arr.length);
	}

	@Test void d06_linkedListOfBeans() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(simple("a", 1)));
		var ll = (LinkedList<SimpleBean>) ParquetParser.DEFAULT.read(bytes, LinkedList.class, SimpleBean.class);
		assertNotNull(ll);
		assertEquals(1, ll.size());
	}

	@Test void d07_collection_targetParseAsList() throws Exception {
		// Collection target returns ArrayList (default).
		var bytes = ParquetSerializer.DEFAULT.write(list(simple("a", 1), simple("b", 2)));
		var c = (Collection<SimpleBean>) ParquetParser.DEFAULT.read(bytes, Collection.class, SimpleBean.class);
		assertEquals(2, c.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// e01_mapTarget - target Map<...> goes through prepareMapForBean / convertToType
	// -----------------------------------------------------------------------------------------------------------------

	@Test void e01_mapTarget() throws Exception {
		// Map<String,Object> from a single bean - exercises Map-target branch in doRead.
		var bytes = ParquetSerializer.DEFAULT.write(simple("alice", 30));
		var m = (Map<String,Object>) ParquetParser.DEFAULT.read(bytes, Map.class, String.class, Object.class);
		assertNotNull(m);
	}

	@Test void e02_listOfMaps() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(simple("a", 1), simple("b", 2)));
		var l = (List<Map<String,Object>>) ParquetParser.DEFAULT.read(bytes, List.class, Map.class);
		assertEquals(2, l.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// f01_scalarTargets - String/int/Boolean targets unwrap {value: X}
	// -----------------------------------------------------------------------------------------------------------------

	@Test void f01_stringScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write("hello");
		assertEquals("hello", ParquetParser.DEFAULT.read(bytes, String.class));
	}

	@Test void f02_intScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(42);
		assertEquals(Integer.valueOf(42), ParquetParser.DEFAULT.read(bytes, Integer.class));
	}

	@Test void f03_longScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(99999L);
		assertEquals(Long.valueOf(99999L), ParquetParser.DEFAULT.read(bytes, Long.class));
	}

	@Test void f04_doubleScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(3.14);
		assertEquals(Double.valueOf(3.14), ParquetParser.DEFAULT.read(bytes, Double.class));
	}

	@Test void f05_booleanScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(true);
		assertEquals(Boolean.TRUE, ParquetParser.DEFAULT.read(bytes, Boolean.class));
	}

	@Test void f06_charScalar() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write('z');
		assertEquals(Character.valueOf('z'), ParquetParser.DEFAULT.read(bytes, Character.class));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// g01_rowsEmpty - empty rows of various target types
	// -----------------------------------------------------------------------------------------------------------------

	@Test void g01_emptyList_asList() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list());
		var l = (List<SimpleBean>) ParquetParser.DEFAULT.read(bytes, List.class, SimpleBean.class);
		assertNotNull(l);
		assertEquals(0, l.size());
	}

	@Test void g02_emptyList_asArray() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list());
		var arr = ParquetParser.DEFAULT.read(bytes, SimpleBean[].class);
		assertNotNull(arr);
		assertEquals(0, arr.length);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// h01_optionalEmpty - Optional with no rows
	// -----------------------------------------------------------------------------------------------------------------

	@Test void h01_optional_fromScalarNull() throws Exception {
		// Serialize empty list then read as Optional<Bean> - exercises Optional empty path.
		var bytes = ParquetSerializer.DEFAULT.write(list());
		var opt = (Optional<SimpleBean>) ParquetParser.DEFAULT.read(bytes, Optional.class, SimpleBean.class);
		// Empty rows → returns Optional.empty
		assertNotNull(opt);
		assertFalse(opt.isPresent());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// i01_keyedMap - non-string key map (key/value pair format)
	// -----------------------------------------------------------------------------------------------------------------

	@Test void i01_intKeyedMap() throws Exception {
		// Map<Integer,String> serialized as key/value pairs - exercises isKeyValuePairFormat branch.
		var src = new LinkedHashMap<Integer,String>();
		src.put(1, "a");
		src.put(2, "b");
		var bytes = ParquetSerializer.DEFAULT.write(src);
		var m = (Map<Integer,String>) ParquetParser.DEFAULT.read(bytes, Map.class, Integer.class, String.class);
		assertNotNull(m);
		assertEquals(2, m.size());
		assertEquals("a", m.get(1));
		assertEquals("b", m.get(2));
	}

	// -----------------------------------------------------------------------------------------------------------------
	// j01_extraKeyValueHolderUnwrap - target Optional/scalar but row has _type
	// -----------------------------------------------------------------------------------------------------------------

	@Test void j01_optionalScalarPresent() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(123);
		var opt = (Optional<Integer>) ParquetParser.DEFAULT.read(bytes, Optional.class, Integer.class);
		assertTrue(opt.isPresent());
		assertEquals(123, opt.get());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// k01_listOfPrimitives - exercises element-type Map override path for primitives in a list
	// -----------------------------------------------------------------------------------------------------------------

	@Test void k01_listOfStrings() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list("a", "b", "c"));
		var l = (List<String>) ParquetParser.DEFAULT.read(bytes, List.class, String.class);
		assertEquals(3, l.size());
		assertEquals("a", l.get(0));
	}

	@Test void k02_listOfIntegers() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(10, 20, 30));
		var l = (List<Integer>) ParquetParser.DEFAULT.read(bytes, List.class, Integer.class);
		assertEquals(3, l.size());
		assertEquals(10, l.get(0));
	}

	@Test void k03_listOfDoubles() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(1.1, 2.2, 3.3));
		var l = (List<Double>) ParquetParser.DEFAULT.read(bytes, List.class, Double.class);
		assertEquals(3, l.size());
	}

	@Test void k04_listOfBooleans() throws Exception {
		var bytes = ParquetSerializer.DEFAULT.write(list(true, false, true));
		var l = (List<Boolean>) ParquetParser.DEFAULT.read(bytes, List.class, Boolean.class);
		assertEquals(3, l.size());
	}

	// -----------------------------------------------------------------------------------------------------------------
	// l01_rawObjectTarget - target Object type
	// -----------------------------------------------------------------------------------------------------------------

	@Test void l01_objectTarget_singleBean() throws Exception {
		// type=Object → effectiveType.isObject() → elementType set to Map.
		var bytes = ParquetSerializer.DEFAULT.write(simple("x", 1));
		var o = ParquetParser.DEFAULT.read(bytes, Object.class);
		assertNotNull(o);
	}

	// -----------------------------------------------------------------------------------------------------------------
	// m01_optional_wrappingNonScalar - Optional<List<...>> path
	// -----------------------------------------------------------------------------------------------------------------

	@Test void m01_optionalOfList() throws Exception {
		// Optional<List<SimpleBean>> - exercises type.isOptional unwrap with Collection inner.
		var bytes = ParquetSerializer.DEFAULT.write(list(simple("a", 1)));
		var opt = ParquetParser.DEFAULT.read(bytes, Optional.class);
		assertNotNull(opt);
	}
}
