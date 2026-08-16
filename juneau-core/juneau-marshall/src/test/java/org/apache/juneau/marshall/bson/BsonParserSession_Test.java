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
package org.apache.juneau.marshall.bson;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.Builder;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.parser.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link BsonParserSession} targeting branches not already exercised by
 * {@link BsonRoundTrip_Test} / {@link BsonEdgeCases_Test} / {@link BsonConformanceFixes_Test} / {@link BsonIntArray_Test}:
 *  - {@code trimKey}'s {@code isTrimStrings()} branch for map/bean-property keys (as opposed to string values,
 *    already covered by {@link BsonTrimStrings_Test}).
 *  - {@code readDocument}'s {@code BuilderSwap} dispatch arm.
 *  - Non-{@link CharSequence}/non-{@link Object} map key-type coercion, and result conversion into a declared
 *    concrete {@link Map} subtype (e.g. {@link TreeMap}).
 *  - {@code readTypedValue}'s ObjectId (0x07) and unknown-type/skip (default) arms, via hand-built documents.
 */
class BsonParserSession_Test extends TestBase {

	// ================================================================
	// Helpers (byte-level construction mirrors BsonConformanceFixes_Test / BsonInputStream_Test)
	// ================================================================

	private static byte[] le4(int v) {
		return new byte[]{(byte) v, (byte) (v >> 8), (byte) (v >> 16), (byte) (v >> 24)};
	}

	private static byte[] cat(byte[]... arrs) {
		var total = 0;
		for (var a : arrs)
			total += a.length;
		var out = new byte[total];
		var pos = 0;
		for (var a : arrs) {
			System.arraycopy(a, 0, out, pos, a.length);
			pos += a.length;
		}
		return out;
	}

	private static byte[] cstring(String s) {
		var b = s.getBytes(StandardCharsets.UTF_8);
		return cat(b, new byte[]{0x00});
	}

	/** Builds a single-element BSON document {@code {name: <value>}} of the given element type. */
	private static byte[] doc(int type, String name, byte[] value) {
		var body = cat(new byte[]{(byte) type}, cstring(name), value, new byte[]{0x00});
		return cat(le4(body.length + 4), body);
	}

	//------------------------------------------------------------------------------------------------------------------
	// a0x - trimKey: isTrimStrings() branch for map keys and bean-property names (readElementName() results).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_mapKeyTrimmed_whenTrimStringsEnabled() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().trimStrings().build();
		var m = new LinkedHashMap<String,Object>();
		m.put(" padded ", 1);
		var bytes = s.write(m);
		@SuppressWarnings("unchecked")
		var result = (Map<String,Object>) p.read(bytes, Map.class, String.class, Object.class);
		assertTrue(result.containsKey("padded"), result.toString());
	}

	@Test void a02_mapKeyNotTrimmed_byDefault() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var m = new LinkedHashMap<String,Object>();
		m.put(" padded ", 1);
		var bytes = s.write(m);
		@SuppressWarnings("unchecked")
		var result = (Map<String,Object>) p.read(bytes, Map.class, String.class, Object.class);
		assertTrue(result.containsKey(" padded "), result.toString());
	}

	public static class A03_Bean {
		public String name;
	}

	@Test void a03_beanPropertyNameTrimmed_whenTrimStringsEnabled() throws Exception {
		// Bean property names come through readElementName() too, but bean field names never carry
		// whitespace in practice; trimKey's true-branch on this path is exercised via getPropertyMeta lookup
		// succeeding regardless (trim is a no-op here). Guard that a normal bean still round-trips under
		// trimStrings() so the trimKey call site on the bean path stays covered end-to-end.
		var s = BsonSerializer.create().build();
		var p = BsonParser.create().trimStrings().build();
		var b = new A03_Bean();
		b.name = " Alice ";
		var bytes = s.write(b);
		var result = p.read(bytes, A03_Bean.class);
		assertEquals("Alice", result.name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b0x - readDocument: BuilderSwap dispatch (nn(builder) branch), mirroring the same @Builder-annotated pattern
	// used for YamlParserSession_Test.
	//------------------------------------------------------------------------------------------------------------------

	@Builder(B01_BeanBuilder.class)
	public static class B01_Bean {
		public int x;
		public B01_Bean(B01_BeanBuilder b) { if (b != null) x = b.x; }
	}

	public static class B01_BeanBuilder {
		public int x;
		public B01_Bean build() { return new B01_Bean(this); }
	}

	@Test void b01_builderSwapRoundTrip() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("x", 42);
		var bytes = s.write(m);
		var result = p.read(bytes, B01_Bean.class);
		assertNotNull(result);
		assertEquals(42, result.x);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c0x - non-CharSequence/non-Object map key-type coercion (Map<TestEnum,String>), and conversion of the parsed
	// raw map into a declared concrete Map subtype (TreeMap) that the generic-map path doesn't natively produce.
	//------------------------------------------------------------------------------------------------------------------

	enum C_Color { RED, GREEN, BLUE }

	public static class C01_Bean {
		public Map<C_Color,String> byColor;
	}

	@Test void c01_nonStringKeyTypeCoercion() throws Exception {
		var s = BsonSerializer.create().build();
		var p = BsonParser.create().build();
		var b = new C01_Bean();
		b.byColor = new LinkedHashMap<>();
		b.byColor.put(C_Color.RED, "stop");
		var bytes = s.write(b);
		var result = p.read(bytes, C01_Bean.class);
		assertNotNull(result.byColor);
		assertEquals("stop", result.byColor.get(C_Color.RED));
	}

	@Test void c02_rawMapConvertedToDeclaredTreeMapType() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("b", 2);
		m.put("a", 1);
		var bytes = s.write(m);
		var result = (TreeMap<?,?>) p.read(bytes, TreeMap.class, String.class, Object.class);
		assertInstanceOf(TreeMap.class, result);
		assertEquals(List.of("a", "b"), List.copyOf(result.keySet()));
	}

	@Test void c04_beanMapTargetHasNullKeyTypeSkipsCoercion() throws Exception {
		// BeanMap.class itself is classified isMap()==true but isBeanMap()==true too, and ClassMeta's
		// findKeyValueTypes() only resolves a non-null key/value type for the "MAP but not BEANMAP" case --
		// so getKeyType() returns null here, and coerceKeys' nn(keyType) guard is false (as opposed to c01's
		// non-String enum key, where it's true).
		var s = BsonSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("x", 1);
		var bytes = s.write(m);
		@SuppressWarnings("unchecked")
		Object result = BsonParser.DEFAULT.read(bytes, (Class<Object>)(Class<?>)org.apache.juneau.commons.bean.BeanMap.class);
		assertInstanceOf(Map.class, result);
		assertEquals(1, ((Map<?,?>)result).get("x"));
	}

	@Test void c03_rawMapConvertedToDeclaredSortedMapInterface() throws Exception {
		// sType.canCreateNewInstance() is false for the bare SortedMap interface, so the map is first
		// built via newGenericMap(sType) and then must be converted to the resolved implementation
		// (TreeMap) afterward -- exercises the true arm of readDocument's post-map-loop conversion check.
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("b", 2);
		m.put("a", 1);
		var bytes = s.write(m);
		var result = (SortedMap<?,?>) p.read(bytes, SortedMap.class, String.class, Object.class);
		assertInstanceOf(TreeMap.class, result);
		assertEquals(List.of("a", "b"), List.copyOf(result.keySet()));
	}

	@Marshalled(typeName = "C05_RawBean")
	public static class C05_RawBean {
		public String name;
	}

	@Test void c05_mapPathCastResolvesToNonMapBean_skipsConversion() throws Exception {
		// The map-building loop's cast(map2, pMeta, eType) call resolves the "_type" discriminator to a
		// registered bean type (rather than leaving the MarshalledMap as-is) -- "raw" ends up as a bean
		// instance, not a Map, so "raw instanceof Map raw2" is false and the whole conversion condition
		// short-circuits false even though eType.isMap() is true. This is the only reachable way to make
		// "raw" not a Map at this point, since the map-building loop otherwise always produces one.
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().beanDictionary(C05_RawBean.class).build();
		var m = new LinkedHashMap<String,Object>();
		m.put("_type", "C05_RawBean");
		m.put("name", "n");
		var bytes = s.write(m);
		Object result = p.read(bytes, Map.class, String.class, Object.class);
		assertInstanceOf(C05_RawBean.class, result);
		assertEquals("n", ((C05_RawBean)result).name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// cc0x - readDocument bean-path arms: unknown property (default throws / ignored) and setter exception.
	//------------------------------------------------------------------------------------------------------------------

	public static class Cc01_Bean {
		public String x;
	}

	@Test void cc01_unknownPropertyDefaultThrows() throws Exception {
		var bytes = BsonSerializer.DEFAULT.write(new LinkedHashMap<>(Map.of("x", "hello", "y", "world")));
		assertThrows(ParseException.class, () -> BsonParser.DEFAULT.read(bytes, Cc01_Bean.class));
	}

	@Test void cc02_unknownPropertyIgnored() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		m.put("x", "hello");
		m.put("y", "world");
		var bytes = BsonSerializer.DEFAULT.write(m);
		var p = BsonParser.create().ignoreUnknownBeanProperties().build();
		var bean = p.read(bytes, Cc01_Bean.class);
		assertEquals("hello", bean.x);
	}

	public static class Cc03_BadSetterBean {
		public String f1;
		public void setF1(@SuppressWarnings("unused") String v) {
			throw new RuntimeException("bad setter");
		}
	}

	@Test void cc03_beanSetterException() throws Exception {
		var bytes = BsonSerializer.DEFAULT.write(Map.of("f1", "v"));
		assertThrows(Exception.class, () -> BsonParser.DEFAULT.read(bytes, Cc03_BadSetterBean.class));
	}

	public static class Cc04_OuterBean {
		public Cc03_BadSetterBean inner;
	}

	@Test void cc04_beanSetterExceptionNestedUsesPropertyMeta() throws Exception {
		// Unlike cc03 (root-level, pMeta==null), here the bad-setter bean is a nested property of an outer
		// bean, so pMeta (the outer's BeanPropertyMeta for "inner") is non-null when the recursive readDocument
		// call's setter throws -- exercises the nn(pMeta) guard's true arm (as opposed to cc03's false arm).
		var m = new LinkedHashMap<String,Object>();
		m.put("inner", Map.of("f1", "v"));
		var bytes = BsonSerializer.create().keepNullProperties().build().write(m);
		assertThrows(Exception.class, () -> BsonParser.DEFAULT.read(bytes, Cc04_OuterBean.class));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d0x - readTypedValue: ObjectId (0x07) dispatch and unknown-type/skip (default) arm, via hand-built documents
	// (no serializer support exists for either, so bytes are constructed directly).
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_objectIdParsedAsHexString() throws Exception {
		var oid = new byte[]{0x50, 0x7F, 0x1F, 0x77, (byte) 0xBC, (byte) 0xF8, 0x6C, (byte) 0xD7,
			(byte) 0x99, 0x43, (byte) 0x90, 0x11};
		var bytes = doc(0x07, "id", oid);
		@SuppressWarnings("unchecked")
		var result = (Map<String,Object>) BsonParser.DEFAULT.read(bytes, Map.class, String.class, Object.class);
		assertEquals("507f1f77bcf86cd799439011", result.get("id"));
	}

	@Test void d02_unknownTypeCode_skippedAsNull() throws Exception {
		// Type 0x06 (Undefined, deprecated) has zero-length payload; readTypedValue's default arm must skip it
		// (via skipValue) rather than fail, yielding a null value for that key.
		var bytes = doc(0x06, "u", new byte[0]);
		@SuppressWarnings("unchecked")
		var result = (Map<String,Object>) BsonParser.DEFAULT.read(bytes, Map.class, String.class, Object.class);
		assertTrue(result.containsKey("u"), result.toString());
		assertNull(result.get("u"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e0x - readArray: Optional-wrapped array/collection root (parallels BsonEdgeCases_Test.a06's scalar Optional).
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_optionalWrappedListRoundTrip() throws Exception {
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		var bytes = s.write(Optional.of(List.of(1, 2, 3)));
		@SuppressWarnings("unchecked")
		var result = (Optional<List<Integer>>)(Optional<?>) p.read(bytes, Optional.class, List.class);
		assertNotNull(result);
		assertTrue(result.isPresent());
		assertEquals(List.of(1, 2, 3), result.get());
	}

	@Test void e02_optionalWrappedMapRoundTrip_readDocumentDisjunct1() throws Exception {
		// eType.isOptional() is the FIRST disjunct of readDocument's line-231 unwrap condition -- short-circuits
		// true before sType.isMap()/isBean()/isObject() (disjunct 2) or the null-check (disjunct 3) are ever
		// evaluated. f01/f02/f03/f04/f06 above only exercise disjuncts 2 and 3 (and the all-false else); this
		// is the only case in the suite that exercises disjunct 1 itself.
		var s = BsonSerializer.create().keepNullProperties().build();
		var p = BsonParser.create().build();
		Map<String,Object> m = new LinkedHashMap<>();
		m.put("a", "hi");
		m.put("b", "bye");
		var bytes = s.write(Optional.of(m));
		@SuppressWarnings("unchecked")
		var result = (Optional<Map<String,Object>>)(Optional<?>) p.read(bytes, Optional.class, Map.class);
		assertNotNull(result);
		assertTrue(result.isPresent());
		assertEquals("hi", result.get().get("a"));
		assertEquals("bye", result.get().get("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// f0x - readDocument's root {"value":x} unwrap logic: scalar root, Object-typed root (both null and non-null
	// wrapped values), and the plain-map-cast else arm (no wrap, or wrap ambiguous with real data).
	//------------------------------------------------------------------------------------------------------------------

	@Test void f01_scalarRootUnwrapped() throws Exception {
		// sType (Integer) is neither Map, Bean, nor Object -- disjunct 2 of the unwrap condition is true.
		var bytes = BsonSerializer.DEFAULT.write(42);
		var result = BsonParser.DEFAULT.read(bytes, Integer.class);
		assertEquals(42, result);
	}

	@Test void f02_objectRootWithNonNullScalarFallsThroughToCastArm() throws Exception {
		// sType.isObject() is true and the wrapped value is non-null, so neither disjunct 2 nor 3 fires --
		// falls through to the else (cast) arm even though the root is single-key {"value":x}. Unlike the
		// unwrap arm (f01/f03), cast(map, pMeta, eType) for an Object.class target returns the map itself
		// rather than unwrapping "value" -- a quirk of the Object.class root case that f01 (Integer.class)
		// and e01 (Optional.class) avoid via their own disjuncts.
		var bytes = BsonSerializer.DEFAULT.write("hello");
		var result = BsonParser.DEFAULT.read(bytes, Object.class);
		assertEquals(Map.of("value", "hello"), result);
	}

	@Test void f03_objectRootWithNullValueUnwrapped() throws Exception {
		// Disjunct 3 (wrapped == null && sType.isObject()) fires: the type code 0x0A (BSON null) round-trips
		// through the {"value":x} wrapper as a literal null for an Object.class target.
		var bytes = doc(0x0A, "value", new byte[0]);
		var result = BsonParser.DEFAULT.read(bytes, Object.class);
		assertNull(result);
	}

	@Test void f03b_mapRootWithNullValueFallsThroughToCastArm() throws Exception {
		// Same null-wrapped "value" as f03, but the target is Map.class (not Object.class) -- disjunct 2's
		// "!sType.isMap()" sub-check is false (short-circuits disjunct 2 to false without evaluating isBean()/
		// isObject()), then disjunct 3's "wrapped == null" is true but its "sType.isObject()" sub-check is
		// false (a Map is not classified as isObject()) -- so disjunct 3 also evaluates false, exercising the
		// "wrapped==null but sType is NOT object-shaped" combination that f03's Object.class case can't reach
		// (there, sType.isObject() is always true whenever wrapped==null is even checked).
		var bytes = doc(0x0A, "value", new byte[0]);
		@SuppressWarnings("unchecked")
		var result = (Map<String,Object>) BsonParser.DEFAULT.read(bytes, Map.class, String.class, Object.class);
		assertNull(result.get("value"));
		assertTrue(result.containsKey("value"));
	}

	@Test void f04_rawMapWithLiteralValueKeyAmbiguousWithWrapConvention() throws Exception {
		// The parsed document happens to have exactly one key literally named "value" (a real map entry, not the
		// {"value":x} root-wrap convention), and the target is a concrete Map subtype (TreeMap) that the raw
		// LinkedHashMap result doesn't already satisfy -- exercises the else-if's isInstance/convertToMemberType
		// ternary (sType.isMap() is true so disjunct 2 doesn't fire; wrapped is non-null so disjunct 3 doesn't fire).
		var s = BsonSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("value", "x");
		var bytes = s.write(m);
		var result = (TreeMap<?,?>) BsonParser.DEFAULT.read(bytes, TreeMap.class, String.class, Object.class);
		assertInstanceOf(TreeMap.class, result);
		assertEquals("x", result.get("value"));
	}

	@Test void f06_singleValueKeyMapAlreadySatisfiesDeclaredType() throws Exception {
		// Same ambiguous single-"value"-key wrap as f04, but the target (raw Map.class) is already satisfied
		// by the parsed LinkedHashMap -- exercises the inner if-branch's ternary false arm (plain castResult,
		// no convertToMemberType), as opposed to f04's true arm (TreeMap needs conversion).
		var s = BsonSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("value", "x");
		var bytes = s.write(m);
		@SuppressWarnings("unchecked")
		var result = (Map<String,Object>) BsonParser.DEFAULT.read(bytes, Map.class, String.class, Object.class);
		assertEquals("x", result.get("value"));
	}

	@Test void f07_multiKeyRawMapConvertedToDeclaredTreeMapType() throws Exception {
		// Same multi-key (no ambiguous wrap) shape as f05, but the target (TreeMap.class) is NOT already
		// satisfied by the parsed LinkedHashMap -- exercises the outer else's ternary true arm
		// (convertToMemberType), as opposed to f05's false arm (plain castResult).
		var s = BsonSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("b", 2);
		m.put("a", 1);
		var bytes = s.write(m);
		var result = (TreeMap<?,?>) BsonParser.DEFAULT.read(bytes, TreeMap.class, String.class, Object.class);
		assertInstanceOf(TreeMap.class, result);
		assertEquals(List.of("a", "b"), List.copyOf(result.keySet()));
	}

	@Test void f05_multiKeyRawMapCastElseArm() throws Exception {
		// map.size() != 1, so the size==1/&&containsKey("value") guard is false outright, routing straight to
		// the outer else's cast/convert ternary -- and since Map.class (raw) is already satisfied by the parsed
		// LinkedHashMap, eType.inner().isInstance(castResult) is true, so the ternary's false arm (plain castResult)
		// is exercised here (as opposed to f04's true arm).
		var s = BsonSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("a", 1);
		m.put("b", 2);
		var bytes = s.write(m);
		@SuppressWarnings("unchecked")
		var result = (Map<String,Object>) BsonParser.DEFAULT.read(bytes, Map.class, String.class, Object.class);
		assertEquals(1, result.get("a"));
		assertEquals(2, result.get("b"));
	}

	@Test void f08_multiKeyRawMapIntoObjectRootLeftUnconvertedAsMap() throws Exception {
		// Same multi-key document as f05/f07, but the target is Object.class (not a Map subtype) -- exercises
		// the outer else's ternary's "eType.isMap()" sub-condition false arm (short-circuits before even
		// checking castResult instanceof Map / isInstance), landing on the plain-castResult false arm. f05/f07
		// (and dd3 above) only ever exercise this ternary with a Map-typed eType, leaving the "not a map at
		// all" combination uncovered.
		var s = BsonSerializer.create().keepNullProperties().build();
		var m = new LinkedHashMap<String,Object>();
		m.put("a", 1);
		m.put("b", 2);
		var bytes = s.write(m);
		var result = BsonParser.DEFAULT.read(bytes, Object.class);
		assertInstanceOf(Map.class, result);
		assertEquals(1, ((Map<?,?>) result).get("a"));
		assertEquals(2, ((Map<?,?>) result).get("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// dd0x - readDocument fallback path (217+) reached with eType.isMap()==true: only possible when a @Swap on a
	// Map-subtype declared type resolves sType to something non-Map (here, a bean), forcing line 162's
	// "sType.isMap()" check false despite eType itself being a Map.
	//------------------------------------------------------------------------------------------------------------------

	public static class Dd1_SwapTarget {
		public String value;
	}

	public static class Dd1_Swap extends org.apache.juneau.marshall.swap.spi.ObjectSwap<Dd1_MapType,Dd1_SwapTarget> {
		@Override
		public Dd1_SwapTarget swap(MarshallingSession session, Dd1_MapType o) {
			var t = new Dd1_SwapTarget();
			t.value = String.valueOf(o.get("k"));
			return t;
		}
		@Override
		public Dd1_MapType unswap(MarshallingSession session, Dd1_SwapTarget f, ClassMeta<?> hint) {
			var m = new Dd1_MapType();
			m.put("k", f.value);
			return m;
		}
	}

	@Swap(Dd1_Swap.class)
	public static class Dd1_MapType extends LinkedHashMap<String,Object> {
		private static final long serialVersionUID = 1L;
	}

	@Test void dd1_mapTypeWithBeanSwap_fallbackPathCastResultConvertedToDeclaredMapType() throws Exception {
		// eType (Dd1_MapType) isMap()==true, but its @Swap resolves sType to Dd1_SwapTarget (a creatable
		// bean, not a Map) -> readDocument's line 184 "sType.canCreateNewBean()" check is true, so this
		// actually still takes the ordinary bean-instantiation path (184-216), unswapping back to
		// Dd1_MapType at the very end (line 257) -- a useful contrast with dd2 below, where the swap
		// target is NOT bean/map-creatable and genuinely reaches the 217+ fallback path instead.
		var src = new Dd1_MapType();
		src.put("k", "hi");
		var bytes = BsonSerializer.DEFAULT.write(src);
		var result = BsonParser.DEFAULT.read(bytes, Dd1_MapType.class);
		assertNotNull(result);
		assertEquals("hi", result.get("k"));
	}

	public static class Dd2_Swap extends org.apache.juneau.marshall.swap.spi.ObjectSwap<Dd2_MapType,Object> {
		@Override
		public Object swap(MarshallingSession session, Dd2_MapType o) {
			return String.valueOf(o.get("k"));
		}
		@Override
		public Dd2_MapType unswap(MarshallingSession session, Object f, ClassMeta<?> hint) {
			// readDocument's fallback path (BsonParserSession L238/239) and its trailing unswap guard (L257)
			// both fire for a loose Object-typed swap like this one (isSwappedObject() can't distinguish
			// "still swapped" from "already unswapped" when the swap's declared swapped-type is Object), so
			// unswap() is invoked twice on the same read. Guard for idempotency rather than corrupting the
			// already-unswapped result on the second call.
			if (f instanceof Dd2_MapType already)
				return already;
			var m = new Dd2_MapType();
			m.put("k", f instanceof Map<?,?> fm ? fm.get("value") : f);
			return m;
		}
	}

	@Swap(Dd2_Swap.class)
	public static class Dd2_MapType extends LinkedHashMap<String,Object> {
		private static final long serialVersionUID = 1L;
	}

	@Test void dd2_mapTypeWithObjectSwap_fallbackPathCastResultConvertedToDeclaredMapType() throws Exception {
		// eType (Dd2_MapType) isMap()==true, and its @Swap resolves sType to plain Object (isObject()==true,
		// not map/bean/creatable-bean) -> BOTH the line 162 "sType.isMap()" check AND the line 184
		// "sType.canCreateNewBean()" check are false, so readDocument genuinely reaches the 217+ fallback
		// path despite eType being a Map. The swapped scalar value round-trips through BSON's {"value":x}
		// root-wrap convention, producing the single-key {"value":x} shape that reaches the 231-else (236)
		// cast arm; castResult (a plain parsed map, not yet Dd2_MapType) is then converted to the declared
		// Dd2_MapType via the "eType.isMap() && ... && !isInstance()" ternary's true arm (238/239), and
		// finally unswapped back (line 257, since the loose Object-typed swap always reports "swapped").
		var src = new Dd2_MapType();
		src.put("k", "hi");
		var bytes = BsonSerializer.DEFAULT.write(src);
		var result = BsonParser.DEFAULT.read(bytes, Dd2_MapType.class);
		assertNotNull(result);
		assertEquals("hi", result.get("k"));
	}

	public interface Dd3_View {
		String getA();
		String getB();
	}

	public static class Dd3_ViewImpl implements Dd3_View {
		private String a, b;
		public Dd3_ViewImpl() {}
		public Dd3_ViewImpl(String a, String b) { this.a = a; this.b = b; }
		@Override public String getA() { return a; }
		@Override public String getB() { return b; }
		public void setA(String a) { this.a = a; }
		public void setB(String b) { this.b = b; }
	}

	public static class Dd3_Swap extends org.apache.juneau.marshall.swap.spi.ObjectSwap<Dd3_MapType,Dd3_View> {
		@Override
		public Dd3_View swap(MarshallingSession session, Dd3_MapType o) {
			return new Dd3_ViewImpl(String.valueOf(o.get("a")), String.valueOf(o.get("b")));
		}
		@Override
		public Dd3_MapType unswap(MarshallingSession session, Dd3_View f, ClassMeta<?> hint) {
			var m = new Dd3_MapType();
			m.put("a", f.getA());
			m.put("b", f.getB());
			return m;
		}
	}

	@Swap(Dd3_Swap.class)
	public static class Dd3_MapType extends LinkedHashMap<String,Object> {
		private static final long serialVersionUID = 1L;
	}

	@Test void dd4_mapTypeWithNonCreatableInterfaceSwap_singleValueKeyDocumentHitsDisjunct2IsBeanSubcheck() throws Exception {
		// Hand-crafted (not round-tripped through BsonSerializer, since the writer's own sType.isBean() check
		// would never actually produce a single-"value"-key document for a bean-shaped sType -- see dd3's
		// comment) single-key {"value":"hi"} document read as Dd3_MapType. sType (post-swap) is the Dd3_View
		// interface: isMap()==false (disjunct-2's "!sType.isMap()" sub-check is true) but isBean()==true
		// (disjunct-2's "!sType.isBean()" sub-check is false), so disjunct 2 evaluates to false via its SECOND
		// sub-condition rather than its first (as in dd2) or third (never reached elsewhere) -- the one
		// sub-condition-false combination the rest of the suite doesn't exercise. wrapped ("hi") is non-null,
		// so disjunct 3 is also false, landing in the same else (236-239) / castResult-conversion path as dd3.
		// Dd3_View is neither Map- nor Number-shaped, so findConversion's ObjectSwap fallback (MarshallingContext's
		// copyMapEntries) applies here too: the swap doesn't bridge, but castResult is already Map-shaped, so its
		// entries (just the raw "value" wrapper key, since this document was never a genuine multi-key shape) are
		// copied verbatim into a new Dd3_MapType instead of being lost.
		var strVal = cstring("hi");
		var bytes = doc(0x02, "value", cat(le4(strVal.length), strVal));
		var result = BsonParser.DEFAULT.read(bytes, Dd3_MapType.class);
		assertNotNull(result);
		assertEquals(1, result.size());
		assertEquals("hi", result.get("value"));
	}

	@Test void dd3_mapTypeWithNonCreatableInterfaceSwap_fallbackPathMultiKeyDocumentHitsMapConversionBranch() throws Exception {
		// eType (Dd3_MapType) isMap()==true, and its @Swap resolves sType to the Dd3_View interface --
		// interfaces are beans (readable getters) but never canCreateNewBean(), so on WRITE the serializer's
		// own sType.isBean() check takes the direct writeBeanMap path (multi-key document, no "value" wrap),
		// while on READ neither the isMap() (162) nor the canCreateNewBean() (184) guard is satisfied, so
		// readDocument reaches the 217+ fallback with a genuinely multi-key document -- landing in line 241's
		// "else" arm (not the single-"value"-key arm at 229-240) and exercising the 243/244
		// "eType.isMap() && castResult instanceof Map && !isInstance()" true arm.
		//
		// FIXED (previously flagged as a bug in MarshallingContext.findConversion): convertToMemberType(null,
		// castResult, eType) at line 244 used to silently return null here instead of copying the parsed map's
		// entries into a new Dd3_MapType. findConversion's ObjectSwap-conversion branch matches purely on eType
		// having *a* registered swap and unconditionally wins the dispatch, but its body only knows how to bridge
		// a Map/Number swap-class input to the unswap() call -- it had no path for "swap-class is some other type
		// (e.g. a bean or interface) and the input is already the correct Map shape, just needs a plain
		// copy-construct". MarshallingContext#copyMapEntries now provides that fallback, so the parsed entries
		// survive into a real Dd3_MapType instead of the whole value being dropped as null.
		var src = new Dd3_MapType();
		src.put("a", "hi");
		src.put("b", "bye");
		var bytes = BsonSerializer.DEFAULT.write(src);
		var result = BsonParser.DEFAULT.read(bytes, Dd3_MapType.class);
		assertNotNull(result);
		assertEquals("hi", result.get("a"));
		assertEquals("bye", result.get("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g0x - readDocument: bean-with-Map-swap (eType stays a bean while sType becomes the swap's Map target), and
	// the type-property value resolving to an explicit null (readTypedValue returning null for the "_type" key).
	//------------------------------------------------------------------------------------------------------------------

	public static class G01_Target {
		public Map<String,Object> m;
	}

	public static class G01_Swap extends org.apache.juneau.marshall.swap.spi.ObjectSwap<G01_Bean,Map<String,Object>> {
		@Override
		public Map<String,Object> swap(MarshallingSession session, G01_Bean o) {
			return Map.of("v", o.v);
		}
		@Override
		public G01_Bean unswap(MarshallingSession session, Map<String,Object> f, ClassMeta<?> hint) {
			var b = new G01_Bean();
			b.v = (String)f.get("v");
			return b;
		}
	}

	@Swap(G01_Swap.class)
	public static class G01_Bean {
		public String v;
	}

	public static class G00_Impl {
		public String v;
	}

	public static class G00_Swap extends org.apache.juneau.marshall.swap.spi.ObjectSwap<G00_Bean,G00_Impl> {
		@Override
		public G00_Impl swap(MarshallingSession session, G00_Bean o) {
			var i = new G00_Impl();
			i.v = o.v;
			return i;
		}
		@Override
		public G00_Bean unswap(MarshallingSession session, G00_Impl f, ClassMeta<?> hint) {
			var b = new G00_Bean();
			b.v = f.v;
			return b;
		}
	}

	@Swap(G00_Swap.class)
	public static class G00_Bean {
		public String v;
	}

	@Test void g00_beanToBeanSwapUnswapsResultAtEnd() throws Exception {
		// The swap's target (G00_Impl) is itself a creatable bean (not Map/String), so readDocument routes
		// through the ordinary bean-instantiation path (184-216) using sType=G00_Impl, producing a G00_Impl
		// instance as `result` that's still "in swapped form" relative to the declared eType (G00_Bean) --
		// exercises the final nn(swap)&&nn(result)&&isSwappedObject(result) unswap arm (line 257's true side),
		// which the Map-swap (g01) and Object-swap (d02 in ProtobufSerializerSession-style tests elsewhere)
		// scenarios don't reach because their own conversion paths already normalize the result first.
		var b = new G00_Bean();
		b.v = "hi";
		var bytes = BsonSerializer.DEFAULT.write(b);
		var result = BsonParser.DEFAULT.read(bytes, G00_Bean.class);
		assertNotNull(result);
		assertEquals("hi", result.v);
	}

	@Test void g01_beanWithMapSwapRoundTrip() throws Exception {
		// eType (G01_Bean) is not itself a Map, but the swap resolves sType to a Map -- exercises the
		// eType.isMap()==false arm of readDocument's post-map-loop conversion check (raw result kept as-is,
		// then unswapped back to G01_Bean at the end via swap.isSwappedObject/unswap).
		var b = new G01_Bean();
		b.v = "hi";
		var bytes = BsonSerializer.DEFAULT.write(b);
		var result = BsonParser.DEFAULT.read(bytes, G01_Bean.class);
		assertNotNull(result);
		assertEquals("hi", result.v);
	}

	public static class G02_Bean {
		public String name;
	}

	@Test void g02_typePropertyWithNullValueSkipsApply() throws Exception {
		// The "_type" key is present but its own BSON value is an explicit null (0x0A, zero-length payload) --
		// readTypedValue returns null for it, so applyTypeProperty is never invoked (the nn(value) guard's
		// false arm). A second "name" element (a normal string) verifies parsing continues correctly afterward.
		var nameValue = cat(le4("n".getBytes(StandardCharsets.UTF_8).length + 1), cstring("n"));
		var body = cat(
			new byte[]{0x0A}, cstring("_type"),
			new byte[]{0x02}, cstring("name"), nameValue,
			new byte[]{0x00});
		var bytes = cat(le4(body.length + 4), body);
		var result = BsonParser.DEFAULT.read(bytes, G02_Bean.class);
		assertNotNull(result);
		assertEquals("n", result.name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h0x - applyTypeProperty: unknown type name (cm resolves to null) and a resolved type that eType cannot
	// be assigned from (both fall through to returning the original beanMap unchanged).
	//------------------------------------------------------------------------------------------------------------------

	public static class H01_Bean {
		public String name;
	}

	@Test void h01_unknownTypeNameLeavesBeanMapUnchanged() throws Exception {
		var m = new LinkedHashMap<String,Object>();
		m.put("_type", "com.example.NoSuchBeanTypeXyz");
		m.put("name", "n");
		var bytes = BsonSerializer.create().keepNullProperties().build().write(m);
		var result = BsonParser.DEFAULT.read(bytes, H01_Bean.class);
		assertEquals("n", result.name);
	}

	@Marshalled(typeName="H02_Other")
	public static class H02_Other {
		public int x;
	}

	public static class H03_Base {
		public String name;
	}

	@Marshalled(typeName="H03_Sub")
	public static class H03_Sub extends H03_Base {
		public int extra;
	}

	@Test void h03_resolvedAssignableTypeSwapsBeanInstance() throws Exception {
		// "_type" resolves to a registered subtype (H03_Sub) that IS assignable from the target (H03_Base) --
		// the true arm of applyTypeProperty's guard, swapping in a fresh H03_Sub instance via newBean/toBeanMap.
		var m = new LinkedHashMap<String,Object>();
		m.put("_type", "H03_Sub");
		m.put("name", "n");
		m.put("extra", 5);
		var bytes = BsonSerializer.create().keepNullProperties().build().write(m);
		var p = BsonParser.create().beanDictionary(H03_Sub.class).build();
		var result = p.read(bytes, H03_Base.class);
		assertInstanceOf(H03_Sub.class, result);
		assertEquals("n", result.name);
		assertEquals(5, ((H03_Sub)result).extra);
	}

	@Test void h02_resolvedTypeNotAssignableLeavesBeanMapUnchanged() throws Exception {
		// "_type" resolves to a real registered bean type (H02_Other), but the target class (H01_Bean) is not
		// assignable from it -- eType.isAssignableFrom(cm) is false, so applyTypeProperty falls through to
		// returning the original beanMap rather than swapping in a new bean instance.
		var m = new LinkedHashMap<String,Object>();
		m.put("_type", "H02_Other");
		m.put("name", "n");
		var bytes = BsonSerializer.create().keepNullProperties().build().write(m);
		var p = BsonParser.create().beanDictionary(H02_Other.class).build();
		var result = p.read(bytes, H01_Bean.class);
		assertEquals("n", result.name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// i0x - readDocument's non-string/non-Object key type (raw Map, not just bean property) with a nullKeyString
	// override that collides with a legitimate root-wrap key name -- exercises the key.equals(nullKeyString) true
	// arm at the fallback (root scalar) path, which is otherwise unreachable since "value" != the default sentinel.
	//------------------------------------------------------------------------------------------------------------------

	@Test void i01_nullKeyStringMatchingRootWrapKeyNameYieldsNullResult() throws Exception {
		// Configuring nullKeyString to literally "value" collides with the fallback (root-scalar) loop's own
		// wrap-key name -- the parsed root key becomes null instead of "value", so the subsequent
		// map.containsKey(BSON_VALUE_KEY) check (which only ever looks for the literal String "value") misses,
		// routing to the outer else/cast arm instead of the normal unwrap arm. Exercises key.equals(nullKeyString)
		// == true in the fallback loop, which is otherwise unreachable (BSON_VALUE_KEY never equals the default
		// nullKeyString sentinel).
		var p = BsonParser.create().nullKeyString("value").build();
		var bytes = doc(0x10, "value", le4(7));
		var result = p.read(bytes, Object.class);
		assertNotNull(result);
	}

	//------------------------------------------------------------------------------------------------------------------
	// j0x - readArray: sType.inner().isArray() true (nested primitive array element, e.g. int[][]) forces the
	// Collection-gathering arm even though sType itself isn't classified isArray()/isArgs() at this call.
	//------------------------------------------------------------------------------------------------------------------

	@Test void j01_nestedPrimitiveArrayUsesCollectionGatheringArm() throws Exception {
		var a = new int[][]{{1, 2}, {3, 4}};
		var bytes = BsonSerializer.DEFAULT.write(a);
		var result = BsonParser.DEFAULT.read(bytes, int[][].class);
		assertEquals(2, result.length);
		assertArrayEquals(new int[]{1, 2}, result[0]);
		assertArrayEquals(new int[]{3, 4}, result[1]);
	}

	@Test void j03_concreteCollectionTypeIsUsedDirectly() throws Exception {
		// sType (LinkedList.class) is a concrete, creatable Collection type -> canCreateNewInstance(outer)
		// is true AND the resulting instance IS a Collection -- exercises both ternaries' true arms
		// (as opposed to j02's Object.class target, which hits both false arms).
		var bytes = BsonSerializer.DEFAULT.write(List.of(1, 2, 3));
		var result = BsonParser.DEFAULT.read(bytes, LinkedList.class, Integer.class);
		assertInstanceOf(LinkedList.class, result);
		assertEquals(List.of(1, 2, 3), result);
	}

	@Test void j04_nonCreatableCollectionInterfaceFallsBackToGenericList() throws Exception {
		// sType (the bare Collection interface) is not directly instantiable -> canCreateNewInstance(outer)
		// is false -- exercises the "instance" ternary's false arm (as opposed to j03's true arm).
		var bytes = BsonSerializer.DEFAULT.write(List.of(1, 2, 3));
		Collection<?> result = BsonParser.DEFAULT.read(bytes, Collection.class, Integer.class);
		assertEquals(List.of(1, 2, 3), List.copyOf(result));
	}

	@Test void j02_arrayIntoObjectTargetUsesGenericListFallback() throws Exception {
		// sType (Object.class) is neither array/args, canCreateNewInstance() is true (Object has a no-arg
		// ctor), but the resulting instance isn't a Collection -- exercises the instanceof-Collection ternary's
		// false arm (newGenericList() fallback) inside readArray, which still runs during the outer document's
		// "value"-field parse loop even though the root-level wrap/unwrap logic (Object.class isn't unwrapped
		// per f02's quirk) then surfaces it wrapped in a map rather than as a bare List.
		var bytes = BsonSerializer.DEFAULT.write(List.of(1, 2, 3));
		var result = BsonParser.DEFAULT.read(bytes, Object.class);
		assertEquals(Map.of("value", List.of(1, 2, 3)), result);
	}
}
