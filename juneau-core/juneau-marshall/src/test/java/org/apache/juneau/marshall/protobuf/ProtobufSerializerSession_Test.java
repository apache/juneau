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
package org.apache.juneau.marshall.protobuf;

import static org.apache.juneau.commons.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.serializer.*;
import org.apache.juneau.marshall.swap.spi.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link ProtobufSerializerSession} targeting branches not already exercised by
 * {@link ProtobufCoverage_Test}, {@link ProtobufEdgeCases_Test}, and {@link ProtobufContainers_Test}:
 *  - {@code getProtobufWriter}'s reuse arm (output already a {@link ProtobufWriter}).
 *  - {@code doWrite}'s root-type guard for an already-wrapped {@link BeanMap} (as opposed to a raw {@link Map}).
 *  - {@code writeBean}'s bean-getter-exception arm.
 *  - {@code writeSingle}'s swap-to-null (omit field) and swap-to-Object-then-rewrap arms.
 *  - {@code writePackedField}'s null-element skip.
 *  - {@code writeMapField}'s null-key/null-value skip.
 *  - {@code encodeScalarValue}'s {@code ENUM_STRING} arm (default enum encoding is {@code ENUM_INT}).
 */
@SuppressWarnings({
	"resource" // ProtobufWriter wraps a caller-owned OutputStream; its inherited close() is a no-op.
})
class ProtobufSerializerSession_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a. getProtobufWriter reuse arm.
	//------------------------------------------------------------------------------------------------------------------

	public static class A01_Bean {
		public int x = 5;
	}

	@Test void a01_getProtobufWriter_alreadyProtobufWriter() throws Exception {
		var baos = new ByteArrayOutputStream();
		var pw = new ProtobufWriter(baos);
		ProtobufSerializer.DEFAULT.write(new A01_Bean(), pw);
		assertTrue(baos.size() > 0);
		var p = ProtobufParser.DEFAULT.read(baos.toByteArray(), A01_Bean.class);
		assertEquals(5, p.x);
	}

	//------------------------------------------------------------------------------------------------------------------
	// b. doWrite's root-type guard: an already-wrapped BeanMap root is not rejected (unlike a raw Map).
	//------------------------------------------------------------------------------------------------------------------

	public static class B01_Bean {
		public String name = "x";
	}

	@Test void b00_rawMapRootRejected() {
		var m = new LinkedHashMap<String,Object>();
		m.put("a", 1);
		var ex = assertThrows(SerializeException.class, () -> ProtobufSerializer.DEFAULT.write(m));
		assertTrue(ex.getMessage().contains("raw Map"));
	}

	@Test void b01_beanMapRootNotRejected() throws Exception {
		// A BeanMap root satisfies cm.isMap(), but cm.isBeanMap() is also true, so the raw-Map rejection
		// guard in doWrite (cm.isMap() && !cm.isBean() && !cm.isBeanMap()) does not fire for it -- unlike
		// a raw Map root, this completes without throwing (the resulting bytes reflect the BeanMap's own
		// class metadata, which carries no protobuf field table, hence the empty output).
		var session = ProtobufSerializer.DEFAULT.createSession().build();
		var bm = session.toBeanMap(new B01_Bean());
		var bytes = ProtobufSerializer.DEFAULT.write(bm);
		assertNotNull(bytes);
	}

	//------------------------------------------------------------------------------------------------------------------
	// c. writeBean: bean-getter-exception arm.
	//------------------------------------------------------------------------------------------------------------------

	public static class C01_ThrowBean {
		public String a = "ok";
		public String getBad() { throw new RuntimeException("boom"); }
	}

	@Test void c01_beanGetterExceptionDefaultThrows() {
		assertThrows(SerializeException.class, () -> ProtobufSerializer.DEFAULT.write(new C01_ThrowBean()));
	}

	@Test void c02_beanGetterExceptionIgnored() throws Exception {
		var s = ProtobufSerializer.create().ignoreInvocationExceptionsOnGetters().build();
		var bytes = s.write(new C01_ThrowBean());
		var p = ProtobufParser.DEFAULT.read(bytes, C01_ThrowBean.class);
		assertEquals("ok", p.a);
	}

	//------------------------------------------------------------------------------------------------------------------
	// d. writeSingle: swap-to-null (omit field, presence model) and swap-to-Object-then-rewrap arms.
	//
	// Both arms live in writeSingle's OWN swap resolution (rType.getSwap(this), evaluated on the runtime type of
	// the value passed in), which only fires for values that are NOT already pre-swapped by the bean property
	// layer.  Per this class's writeSingle javadoc, bean *property* values whose declared type has a registered
	// swap are pre-swapped upstream (MarshalledPropertyPostProcessor), so a swap on a top-level property never
	// reaches this method's own swap check -- confirmed empirically: with the swap on a direct bean property,
	// writeSingle sees the *already-swapped* value (e.g. a String) with a swap-less runtime type.  Raw List/Map
	// *elements*, however, are not pre-swapped, so putting the swapped type inside a List reaches this method
	// with the swap still to be resolved.
	//------------------------------------------------------------------------------------------------------------------

	public static class D01_NullSwapTarget {
		public int x = 1;
	}

	public static class D01_NullSwap extends ObjectSwap<D01_NullSwapTarget,String> {
		@Override public String swap(MarshallingSession session, D01_NullSwapTarget o) { return null; }
	}

	public static class D01_Bean {
		public String name = "n";
		public List<D01_NullSwapTarget> inner;
	}

	@Test void d01_swapToNullOmitsField() throws Exception {
		var bean = new D01_Bean();
		bean.inner = new ArrayList<>(List.of(new D01_NullSwapTarget()));
		var s = ProtobufSerializer.create().swaps(D01_NullSwap.class).build();
		var bytes = s.write(bean);
		var p = ProtobufParser.create().swaps(D01_NullSwap.class).build().read(bytes, D01_Bean.class);
		assertEquals("n", p.name);
		assertTrue(p.inner == null || p.inner.isEmpty());
	}

	public static class D02_ObjSwapTarget {
		public int x = 1;
	}

	public static class D02_ObjSwap extends ObjectSwap<D02_ObjSwapTarget,Object> {
		@Override public Object swap(MarshallingSession session, D02_ObjSwapTarget o) { return "swapped-" + o.x; }
	}

	public static class D02_Bean {
		public String name = "n";
		public List<D02_ObjSwapTarget> inner = new ArrayList<>(List.of(new D02_ObjSwapTarget()));
	}

	@Test void d02_swapToObjectTypeRewrapsToRuntimeType() throws Exception {
		// The swap's declared target type is Object (isObject() true), forcing a re-resolve via
		// getClassMetaForObject(v) to the swap's actual runtime return type (String here), which then
		// serializes as a STRING scalar rather than a nested message.
		var s = ProtobufSerializer.create().swaps(D02_ObjSwap.class).build();
		var bytes = s.write(new D02_Bean());
		assertNotNull(bytes);
		assertTrue(bytes.length > 0);
	}

	//------------------------------------------------------------------------------------------------------------------
	// e. writePackedField: null-element skip (mirrors a10_nullInsideCollectionSkipped for TAGGED_REPEATED).
	//------------------------------------------------------------------------------------------------------------------

	public static class E01_IntList {
		public List<Integer> nums;
	}

	@Test void e01_nullInsidePackedListSkipped() throws Exception {
		var a = new E01_IntList();
		a.nums = new ArrayList<>(Arrays.asList(1, null, 2));
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var p = ProtobufParser.DEFAULT.read(bytes, E01_IntList.class);
		assertEquals(list(1, 2), p.nums);
	}

	public static class E02_IntArray {
		public int[] nums;
	}

	@Test void e02_primitiveArrayPackedField_toElementListArrayBranch() throws Exception {
		// toElementList's value.getClass().isArray() branch (as opposed to the Collection branch exercised by
		// e01) is reached only for an actual array-typed repeated property.
		var a = new E02_IntArray();
		a.nums = new int[]{1, 2, 3};
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var p = ProtobufParser.DEFAULT.read(bytes, E02_IntArray.class);
		assertArrayEquals(new int[]{1, 2, 3}, p.nums);
	}

	//------------------------------------------------------------------------------------------------------------------
	// f. writeMapField: null-key and null-value skip.
	//------------------------------------------------------------------------------------------------------------------

	public static class F01_Map {
		public Map<String,Integer> m;
	}

	@Test void f01_nullKeyInMapSkipped() throws Exception {
		var a = new F01_Map();
		a.m = new LinkedHashMap<>();
		a.m.put(null, 1);
		a.m.put("b", 2);
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var p = ProtobufParser.DEFAULT.read(bytes, F01_Map.class);
		assertEquals(1, p.m.size());
		assertEquals(Integer.valueOf(2), p.m.get("b"));
	}

	public static class F03_NestedMap {
		public Map<String,Map<String,Integer>> m;
	}

	@Test void f03_mapValueIsItselfAMap_hitsWriteSingleIsMapBranch() throws Exception {
		// writeMapField's writeSingle(bw, 2, valSt, v, valueType) call for a map *value* that is itself a Map
		// reaches writeSingle's isMap() disjunct (as opposed to isBean()), routing into writeMessageField, which
		// now recognizes a non-bean Map target and delegates to writeMapMessage instead of unconditionally calling
		// toBeanMap(value) (mirrors the analogous read-side fix in ProtobufParserSession_Test's a01/a02/a03) -- so
		// a plain Map value round-trips correctly through the default serializer/parser.
		var a = new F03_NestedMap();
		a.m = new LinkedHashMap<>();
		a.m.put("outer", new LinkedHashMap<>(Map.of("inner", 5)));
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var p = ProtobufParser.DEFAULT.read(bytes, F03_NestedMap.class);
		assertEquals(Integer.valueOf(5), p.m.get("outer").get("inner"));
	}

	@Test void f02_nullValueInMapSkipped() throws Exception {
		var a = new F01_Map();
		a.m = new LinkedHashMap<>();
		a.m.put("a", null);
		a.m.put("b", 2);
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var p = ProtobufParser.DEFAULT.read(bytes, F01_Map.class);
		assertEquals(1, p.m.size());
		assertEquals(Integer.valueOf(2), p.m.get("b"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g. encodeScalarValue: ENUM_STRING arm (default enum encoding is ENUM_INT; ENUM_STRING needs an override).
	//------------------------------------------------------------------------------------------------------------------

	public enum G01_E { ALPHA, BETA }

	public static class G01_Bean {
		@Protobuf(type=ProtobufScalarType.ENUM_STRING)
		public G01_E e;
	}

	@Test void g01_enumStringScalarRoundTrip() throws Exception {
		var a = new G01_Bean();
		a.e = G01_E.BETA;
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var p = ProtobufParser.DEFAULT.read(bytes, G01_Bean.class);
		assertEquals(G01_E.BETA, p.e);
	}

	//------------------------------------------------------------------------------------------------------------------
	// h. toBoolean: non-Boolean fallback (Boolean.parseBoolean(value.toString())).
	//------------------------------------------------------------------------------------------------------------------

	public static class H01_Bean {
		@Protobuf(type=ProtobufScalarType.BOOL)
		public String flag;
	}

	@Test void h01_boolScalarFromNonBooleanValue_parsesStringFallback() throws Exception {
		// @Protobuf(type=BOOL) forces a String-typed property through encodeScalarValue's BOOL arm, so
		// toBoolean(value) sees a String (not a Boolean instance) and falls back to Boolean.parseBoolean.
		var a = new H01_Bean();
		a.flag = "true";
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var p = ProtobufParser.DEFAULT.read(bytes, H01_Bean.class);
		assertEquals("true", p.flag);
	}
}
