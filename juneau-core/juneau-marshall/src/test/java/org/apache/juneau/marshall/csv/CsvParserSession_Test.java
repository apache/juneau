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
package org.apache.juneau.marshall.csv;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage tests for {@link CsvParserSession} targeting branches not exercised
 * by {@link CsvParser_Test} or {@link Csv_Test}.
 */
@SuppressWarnings({
	"unchecked", // parser produces unparameterized generics
	"rawtypes"   // raw List/Map needed for several parser-result casts
})
class CsvParserSession_Test extends TestBase {

	//====================================================================================================
	// a - Builder configuration variants (nullValue null fallback, byteArrayFormat)
	//====================================================================================================

	@Test void a01_nullValueExplicitlyNull_fallsBackToDefault() throws Exception {
		// Builder.nullValue(null) -> session falls back to default "<NULL>" marker.
		var p = CsvParser.create().nullValue(null).build();
		var csv = "b,c\n<NULL>,1\nx,2\n";
		var r = (List<A>) p.parse(csv, List.class, A.class);
		assertEquals(2, r.size());
		assertNull(r.get(0).b);
		assertEquals(1, r.get(0).c);
		assertEquals("x", r.get(1).b);
	}

	@Test void a02_byteArrayFormat_base64_explicit() throws Exception {
		// Default byteArrayFormat is BASE64; ensure parse works when explicitly configured.
		var p = CsvParser.create().byteArrayFormat(CsvByteArrayCellFormat.BASE64).build();
		var bytes = "Hi".getBytes();
		var b64 = Base64.getEncoder().encodeToString(bytes);
		var csv = "name,data\nrow1," + b64 + "\n";
		var r = (List<I>) p.parse(csv, List.class, I.class);
		assertArrayEquals(bytes, r.get(0).data);
	}

	@Test void a03_byteArrayFormat_semicolon_emptyValueYieldsEmptyArray() throws Exception {
		// Empty byte[] cell -> parseCsvCellValue short-circuits (val.isEmpty()) and
		// convertToType("", byte[]) yields a zero-length array.
		var p = CsvParser.create().byteArrayFormat(CsvByteArrayCellFormat.SEMICOLON_DELIMITED).build();
		var csv = "name,data\nrow1,\n";
		var r = (List<I>) p.parse(csv, List.class, I.class);
		assertEquals(1, r.size());
		assertNotNull(r.get(0).data);
		assertEquals(0, r.get(0).data.length);
	}

	//====================================================================================================
	// b - Optional<T> target types
	//====================================================================================================

	@Test void b01_parseIntoOptionalString() throws Exception {
		var csv = "value\nhello\n";
		var r = (Optional<String>) CsvParser.DEFAULT.parse(csv, Optional.class, String.class);
		assertTrue(r.isPresent());
		assertEquals("hello", r.get());
	}

	@Test void b02_parseIntoOptionalInteger() throws Exception {
		var csv = "value\n42\n";
		var r = (Optional<Integer>) CsvParser.DEFAULT.parse(csv, Optional.class, Integer.class);
		assertTrue(r.isPresent());
		assertEquals(42, r.get());
	}

	//====================================================================================================
	// c - Empty / boundary inputs for single-target shapes
	//====================================================================================================

	@Test void c01_singleBean_emptyInput_returnsNull() throws Exception {
		assertNull(CsvParser.DEFAULT.parse("", A.class));
	}

	@Test void c02_singleBean_headerOnly_returnsNullObject() throws Exception {
		// Header row exists but no data row -> parseRowIntoBean is skipped, returns null.
		assertNull(CsvParser.DEFAULT.parse("b,c\n", A.class));
	}

	@Test void c03_singleMap_headerOnly_returnsNullObject() throws Exception {
		assertNull(CsvParser.DEFAULT.parse("k1,k2\n", Map.class));
	}

	@Test void c04_singleString_headerOnly_returnsNull() throws Exception {
		// Simple-type target with header only and no data row.
		assertNull(CsvParser.DEFAULT.parse("value\n", String.class));
	}

	@Test void c05_singleString_headerWithoutValueColumn_usesFirstColumn() throws Exception {
		// When no "value" column is present, fall back to column index 0.
		var r = CsvParser.DEFAULT.parse("foo\nbar\n", String.class);
		assertEquals("bar", r);
	}

	@Test void c06_objectTarget_headerOnly_returnsNull() throws Exception {
		// Object-target branch: header but no rows -> results empty -> null.
		assertNull(CsvParser.DEFAULT.parse("a,b\n", Object.class));
	}

	//====================================================================================================
	// d - Concrete Collection / Map types (canCreateNewInstance)
	//====================================================================================================

	@Test void d01_concreteCollectionType_linkedList() throws Exception {
		// Sets sType.canCreateNewInstance(outer) == true so the parser uses the
		// supplied collection class rather than ArrayList.
		var csv = "value\nfoo\nbar\n";
		var r = (LinkedList<String>) CsvParser.DEFAULT.parse(csv, LinkedList.class, String.class);
		assertInstanceOf(LinkedList.class, r);
		assertEquals(List.of("foo", "bar"), r);
	}

	@Test void d02_singleMap_concreteLinkedHashMap() throws Exception {
		// canCreateNewInstance(outer) on map type -> uses provided LinkedHashMap impl.
		var csv = "k1,k2\nv1,v2\n";
		var r = CsvParser.DEFAULT.parse(csv, LinkedHashMap.class);
		assertInstanceOf(LinkedHashMap.class, r);
		assertEquals("v1", r.get("k1"));
		assertEquals("v2", r.get("k2"));
	}

	//====================================================================================================
	// e - Polymorphic / type-discriminator branches in parseRow / parseRowIntoBean
	//====================================================================================================

	@Test void e01_polymorphicWithDictionary_resolvesViaTypeColumn() throws Exception {
		// Shape is an interface with bean dictionary; header carries _type column.
		// parseRow takes the BeanRegistry/_type branch, parses to map then casts to Circle.
		var csv = "name,radius,_type\nc1,9,Circle\n";
		var r = (List<Shape>) CsvParser.DEFAULT.parse(csv, List.class, Shape.class);
		assertEquals(1, r.size());
		assertInstanceOf(Circle.class, r.get(0));
		assertEquals("c1", r.get(0).getName());
		assertEquals(9, ((Circle) r.get(0)).radius);
	}

	@Test void e02_singleBeanWithEmptyTypeColumn() throws Exception {
		// Single-bean target (parseAnything's isBean branch) with _type column whose
		// value is whitespace -> parseRowIntoBean's "nn(typeVal) && !typeVal.trim().isEmpty()"
		// guard is false; declared bean type is used as-is.
		var csv = "b,c,_type\nfoo,7,   \n";
		var r = CsvParser.DEFAULT.parse(csv, A.class);
		assertEquals("foo", r.b);
		assertEquals(7, r.c);
	}

	@Test void e03_singleBeanWithTypeColumnShortRow() throws Exception {
		// Single-bean target with _type column at end and a short data row ->
		// typeColIdx >= row.size() short-circuit branch is taken in parseRowIntoBean.
		var csv = "b,c,_type\nfoo,11\n";
		var r = CsvParser.DEFAULT.parse(csv, A.class);
		assertEquals("foo", r.b);
		assertEquals(11, r.c);
	}

	@Test void e04_singleBeanWithTypeColumnResolvingToNonBean() throws Exception {
		// Single-bean target with _type column whose value resolves to a non-bean
		// (e.g. "java.lang.String") -> "resolved != null && resolved.isBean()" is false,
		// declared bean type is kept.
		var csv = "b,c,_type\nfoo,12,String\n";
		var r = CsvParser.DEFAULT.parse(csv, A.class);
		assertEquals("foo", r.b);
		assertEquals(12, r.c);
	}

	//====================================================================================================
	// f - Unknown property + bean property fallthrough
	//====================================================================================================

	@Test void f01_unknownProperty_throwsByDefault() throws Exception {
		// Header has an extra column not on the bean -> onUnknownProperty branch
		// triggers ParseException because ignoreUnknownBeanProperties is false by default.
		var csv = "b,c,extra\nfoo,7,ignored\n";
		assertThrows(ParseException.class, () -> CsvParser.DEFAULT.parse(csv, List.class, A.class));
	}

	@Test void f01b_unknownProperty_ignoredWhenConfigured() throws Exception {
		// Same input but with ignoreUnknownBeanProperties() -> unknown column accepted silently.
		var p = CsvParser.create().ignoreUnknownBeanProperties().build();
		var csv = "b,c,extra\nfoo,7,ignored\n";
		var r = (List<A>) p.parse(csv, List.class, A.class);
		assertEquals(1, r.size());
		assertEquals("foo", r.get(0).b);
		assertEquals(7, r.get(0).c);
	}

	@Test void f02_beanRow_shorterThanHeaders_missingPropsNullDefault() throws Exception {
		// Row shorter than headers -> i < row.size() is false on trailing columns;
		// parseCellValue invoked with null val.
		var csv = "b,c\nonly_b\n";
		var r = (List<B>) CsvParser.DEFAULT.parse(csv, List.class, B.class);
		assertEquals(1, r.size());
		assertEquals("only_b", r.get(0).b);
		assertNull(r.get(0).c);
	}

	//====================================================================================================
	// g - parseRow Object element-type branch (List<Object>)
	//====================================================================================================

	@Test void g01_parseListOfObject_yieldsMapsPerRow() throws Exception {
		// Element type Object -> parseRow takes the "isObject()" early branch
		// returning a generic map per row.
		var csv = "a,b\n1,2\n3,4\n";
		var r = (List<Map>) CsvParser.DEFAULT.parse(csv, List.class, Object.class);
		assertEquals(2, r.size());
		assertEquals("1", r.get(0).get("a"));
		assertEquals("2", r.get(0).get("b"));
		assertEquals("3", r.get(1).get("a"));
		assertEquals("4", r.get(1).get("b"));
	}

	@Test void g02_parseListOfObject_shortRow_padsWithNull() throws Exception {
		// In parseRow Object branch, i < row.size() false case -> map gets null for missing cell.
		var csv = "a,b\n1\n";
		var r = (List<Map>) CsvParser.DEFAULT.parse(csv, List.class, Object.class);
		assertEquals(1, r.size());
		assertEquals("1", r.get(0).get("a"));
		assertNull(r.get(0).get("b"));
	}

	//====================================================================================================
	// h - Simple-type element list with empty data row
	//====================================================================================================

	@Test void h01_simpleTypeList_emptyDataCell_yieldsNullElement() throws Exception {
		// Multi-column header with one empty value column; parseRow simple-type path
		// reads row.get(0), which is an empty cell; parseCellValue("",String) -> null.
		var csv = "value,other\n,x\n";
		var r = (List<String>) CsvParser.DEFAULT.parse(csv, List.class, String.class);
		assertEquals(1, r.size());
		assertNull(r.get(0));
	}

	//====================================================================================================
	// i - Quoted-field edge cases at parser-session level
	//====================================================================================================

	@Test void i01_quotedComma_inBeanField() throws Exception {
		var csv = "b,c\n\"a,b,c\",5\n";
		var r = (List<A>) CsvParser.DEFAULT.parse(csv, List.class, A.class);
		assertEquals("a,b,c", r.get(0).b);
		assertEquals(5, r.get(0).c);
	}

	@Test void i02_quotedNewline_inBeanField() throws Exception {
		var csv = "b,c\n\"line1\nline2\",1\n";
		var r = (List<A>) CsvParser.DEFAULT.parse(csv, List.class, A.class);
		assertEquals("line1\nline2", r.get(0).b);
	}

	@Test void i03_escapedQuotes_inBeanField() throws Exception {
		var csv = "b,c\n\"a\"\"b\",1\n";
		var r = (List<A>) CsvParser.DEFAULT.parse(csv, List.class, A.class);
		assertEquals("a\"b", r.get(0).b);
	}

	//====================================================================================================
	// j - Primitive-array branches in parseCsvCellValue
	//====================================================================================================

	@Test void j01_primitiveArrays_longFloatShortBooleanChar() throws Exception {
		var csv = "name,longs,floats,shorts,bools,chars\n"
			+ "row1,[1;2;3],[1.5;2.5],[10;20],[true;false],[65;66;67]\n";
		var r = (List<P>) CsvParser.DEFAULT.parse(csv, List.class, P.class);
		assertEquals(1, r.size());
		assertArrayEquals(new long[]{1L, 2L, 3L}, r.get(0).longs);
		assertArrayEquals(new float[]{1.5f, 2.5f}, r.get(0).floats);
		assertArrayEquals(new short[]{10, 20}, r.get(0).shorts);
		assertArrayEquals(new boolean[]{true, false}, r.get(0).bools);
		assertArrayEquals(new char[]{'A', 'B', 'C'}, r.get(0).chars);
	}

	@Test void j02_primitiveArrays_emptyVariants() throws Exception {
		// "[]" -> createEmptyPrimitiveArray; exercises long/float/short/bool/char empty branches.
		var csv = "name,longs,floats,shorts,bools,chars\n"
			+ "row1,[],[],[],[],[]\n";
		var r = (List<P>) CsvParser.DEFAULT.parse(csv, List.class, P.class);
		assertEquals(0, r.get(0).longs.length);
		assertEquals(0, r.get(0).floats.length);
		assertEquals(0, r.get(0).shorts.length);
		assertEquals(0, r.get(0).bools.length);
		assertEquals(0, r.get(0).chars.length);
	}

	@Test void j03_primitiveArray_missingBracketsTreatedAsString() throws Exception {
		// Array cell without [..] markers -> parseCsvCellValue returns null and
		// convertToType(string,int[]) is attempted; throws ParseException.
		var csv = "name,longs,floats,shorts,bools,chars\n"
			+ "row1,1;2;3,[],[],[],[]\n";
		assertThrows(ParseException.class, () -> CsvParser.DEFAULT.parse(csv, List.class, P.class));
	}

	@Test void j04_byteArrayElement_emptyArrayLiteral() throws Exception {
		// Element type byte[] with non-empty value (semicolon) returns byte[].
		var p = CsvParser.create().byteArrayFormat(CsvByteArrayCellFormat.SEMICOLON_DELIMITED).build();
		var csv = "name,data\nr,7;8;9\n";
		var r = (List<I>) p.parse(csv, List.class, I.class);
		assertArrayEquals(new byte[]{7, 8, 9}, r.get(0).data);
	}

	//====================================================================================================
	// k - parseCellValue type-conversion failure path
	//====================================================================================================

	@Test void k01_nonNumericIntoInt_throwsParseException() throws Exception {
		// Triggers InvalidDataConversionException -> rewrapped as ParseException.
		var csv = "b,c\nfoo,not-a-number\n";
		assertThrows(ParseException.class, () -> CsvParser.DEFAULT.parse(csv, List.class, A.class));
	}

	@Test void k02_emptyStringIntoInteger_returnsNull() throws Exception {
		// Empty CharSequence cell -> parseCellValue returns null via the
		// "val.isEmpty() && eType.isCharSequence()" branch when target is String.
		var csv = "b,c\n,5\n";
		var r = (List<B>) CsvParser.DEFAULT.parse(csv, List.class, B.class);
		assertEquals(1, r.size());
		assertNull(r.get(0).b);
		assertEquals(5, r.get(0).c);
	}

	//====================================================================================================
	// m - trimStrings + allowNestedStructures branches
	//====================================================================================================

	@Test void m01_trimStrings_appliesAtCellLevel() throws Exception {
		// Builder.trimStrings() -> isTrimStrings() returns true; cell value "  hi  " is trimmed.
		var p = CsvParser.create().trimStrings().build();
		var csv = "b,c\n  hi  ,  3  \n";
		var r = (List<A>) p.parse(csv, List.class, A.class);
		assertEquals(1, r.size());
		assertEquals("hi", r.get(0).b);
		assertEquals(3, r.get(0).c);
	}

	@Test void m02_allowNestedStructures_plainCellTreatedAsString() throws Exception {
		// allowNestedStructures=true but cell does not start with { or [ ->
		// fallback to convertToType (no nested-parse attempt).
		var p = CsvParser.create().allowNestedStructures(true).build();
		var csv = "b,c\nplain,42\n";
		var r = (List<A>) p.parse(csv, List.class, A.class);
		assertEquals("plain", r.get(0).b);
		assertEquals(42, r.get(0).c);
	}

	@Test void m03_allowNestedStructures_withTrimStringsCombined() throws Exception {
		// allowNestedStructures=true + trimStrings=true: cell with leading whitespace
		// before [ should still be detected as a nested structure after trim.
		var p = CsvParser.create().allowNestedStructures(true).trimStrings().build();
		var csv = "name,tags\nrow1,  [a;b;c]  \n";
		var r = (List<J>) p.parse(csv, List.class, J.class);
		assertEquals(1, r.size());
		assertEquals(List.of("a", "b", "c"), r.get(0).tags);
	}

	//====================================================================================================
	// n - Map element type with explicit value-type
	//====================================================================================================

	@Test void n01_mapCollection_typedValues() throws Exception {
		// Map<String,Integer> element type drives keyType / valueType branches in parseRowIntoMap.
		var csv = "a,b\n1,2\n3,4\n";
		var r = (List<Map<String,Integer>>) CsvParser.DEFAULT.parse(csv,
				List.class, Map.class, String.class, Integer.class);
		assertEquals(2, r.size());
		assertEquals(Integer.valueOf(1), r.get(0).get("a"));
		assertEquals(Integer.valueOf(2), r.get(0).get("b"));
		assertEquals(Integer.valueOf(3), r.get(1).get("a"));
		assertEquals(Integer.valueOf(4), r.get(1).get("b"));
	}

	//====================================================================================================
	// l - Reused beans
	//====================================================================================================

	public static class A {
		public String b;
		public int c;
	}

	public static class B {
		public String b;
		public Integer c;
	}

	public static class I {
		public String name;
		public byte[] data;
	}

	public static class P {
		public String name;
		public long[] longs;
		public float[] floats;
		public short[] shorts;
		public boolean[] bools;
		public char[] chars;
	}

	public static class J {
		public String name;
		public List<String> tags;
	}

	@Marshalled(dictionary = {Circle.class, Rectangle.class})
	public interface Shape {
		String getName();
	}

	@Marshalled(typeName = "Circle")
	public static class Circle implements Shape {
		public String name;
		public int radius;
		public Circle() {}
		@Override public String getName() { return name; }
	}

	@Marshalled(typeName = "Rectangle")
	public static class Rectangle implements Shape {
		public String name;
		public int width;
		public int height;
		public Rectangle() {}
		@Override public String getName() { return name; }
	}
}
