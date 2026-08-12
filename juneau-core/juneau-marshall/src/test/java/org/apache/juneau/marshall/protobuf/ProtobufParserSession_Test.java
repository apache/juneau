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
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.swap.spi.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-targeted tests for {@link ProtobufParserSession}, focused on branches not otherwise exercised by the
 * golden-byte, round-trip, and swap-dispatch test suites in this package:  {@code TAGGED_REPEATED}/map-value
 * element-is-a-map dispatch, object-swap-to-map dispatch, the bean-setter-exception propagation path, the
 * {@code decodeEnumOrdinal} guard conditions, and the unknown-field-number skip inside a map entry.
 */
@SuppressWarnings({
	"resource" // ProtobufWriter wraps a caller-owned OutputStream; its inherited close() is a no-op.
})
class ProtobufParserSession_Test extends TestBase {

	private static byte[] bytes(java.util.function.Consumer<ProtobufWriter> fn) {
		var out = new ByteArrayOutputStream();
		fn.accept(new ProtobufWriter(out));
		return out.toByteArray();
	}

	//------------------------------------------------------------------------------------------------------------------
	// TAGGED_REPEATED with Map elements -- readMessage's "elType.isBean() || elType.isMap()" (L152), isMap() arm.
	//------------------------------------------------------------------------------------------------------------------

	public static class MapList {
		public List<Map<String,Integer>> items;
		public MapList() {}
		public MapList(List<Map<String,Integer>> items) { this.items = items; }
	}

	@Test
	void a01_taggedRepeatedMapElement_hitsIsMapBranch() throws Exception {
		// readMessage's elType.isMap() dispatch (L152) now routes a non-bean Map target to readMapMessage, which
		// decodes the sub-message's key(1)/value(2) tag pairs into a real Map instead of NPEing on a null BeanMap.
		// Mirrored on write by writeMessageField/writeMapMessage (see ProtobufSerializerSession_Test#f03), so this
		// now round-trips through the default serializer/parser rather than needing hand-built bytes.
		var a = new MapList(list(map("a", 1)));
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var b = ProtobufParser.DEFAULT.read(bytes, MapList.class);
		assertEquals(1, b.items.size());
		assertEquals(Integer.valueOf(1), b.items.get(0).get("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// decodeMapComponent's "cm.isBean() || cm.isMap()" (L314), isMap() arm -- a map value that is itself a map.
	//------------------------------------------------------------------------------------------------------------------

	public static class NestedMap {
		public Map<String,Map<String,Integer>> m;
		public NestedMap() {}
		public NestedMap(Map<String,Map<String,Integer>> m) { this.m = m; }
	}

	@Test
	void a02_mapValueIsMap_hitsIsMapBranch() throws Exception {
		// Same underlying fix as a01 (readMessage now routes a non-bean Map target to readMapMessage instead of
		// NPEing on a null BeanMap), reached this time via decodeMapComponent's cm.isMap() branch (L314) for a
		// map whose *value* type is itself a map. Mirrored on write by writeMapField's writeSingle -> writeMessageField
		// -> writeMapMessage chain, so this now round-trips through the default serializer/parser.
		var a = new NestedMap(map("outer", map("inner", 5)));
		var bytes = ProtobufSerializer.DEFAULT.write(a);
		var b = ProtobufParser.DEFAULT.read(bytes, NestedMap.class);
		assertEquals(Integer.valueOf(5), b.m.get("outer").get("inner"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// decodeSwapped's "sType.isBean() || sType.isMap()" (L223), isMap() arm -- an ObjectSwap whose swap-class is a Map.
	//------------------------------------------------------------------------------------------------------------------

	public static class Color {
		public String name;
		public Color() {}
		public Color(String name) { this.name = name; }
	}

	/** POJO -> Map swap (distinct from the POJO->scalar and POJO->bean swaps already covered by ProtobufSwap_Test). */
	public static class ColorMapSwap extends ObjectSwap<Color,Map<String,Object>> {
		@Override
		public Map<String,Object> swap(MarshallingSession session, Color o) { return o == null ? null : map("name", o.name); }
		@Override
		public Color unswap(MarshallingSession session, Map<String,Object> o, ClassMeta<?> hint, String attrName) {
			return o == null ? null : new Color((String)o.get("name"));
		}
	}

	public static class ScalarSwapBean {
		public Color color;
		public ScalarSwapBean() {}
		public ScalarSwapBean(Color color) { this.color = color; }
	}

	@Test
	void a03_swapToMapType_decodeSwappedHitsIsMapBranch() throws Exception {
		// Same underlying fix as a01/a02, reached this time via decodeSwapped's sType.isMap() branch (L223) for
		// an ObjectSwap whose swap-class is a Map. Mirrored on write by writeSingle's own swap dispatch into
		// writeMessageField/writeMapMessage, so this now round-trips through a swap-registered serializer/parser
		// pair rather than needing hand-built bytes.
		var ser = ProtobufSerializer.create().swaps(ColorMapSwap.class).build();
		var par = ProtobufParser.create().swaps(ColorMapSwap.class).build();
		var a = new ScalarSwapBean(new Color("red"));
		var bytes = ser.write(a);
		var b = par.read(bytes, ScalarSwapBean.class);
		assertEquals("red", b.color.name);
	}

	//------------------------------------------------------------------------------------------------------------------
	// readMapEntry's "fn == 1 / else if fn == 2 / else skipField" (L300-305) -- an unknown field number inside a map
	// entry sub-message must be skipped rather than assigned to the key or value.
	//------------------------------------------------------------------------------------------------------------------

	public static class StrMap {
		public Map<String,Integer> m;
		public StrMap() {}
	}

	@Test
	void a04_unknownFieldNumberInMapEntry_isSkipped() throws Exception {
		// Hand-built map entry: field 1 (key)="a", field 3 (unknown)=varint 999, field 2 (value)=varint 1.
		var entry = bytes(w -> {
			w.writeTag(1, WireType.LEN); w.writeString("a");
			w.writeTag(3, WireType.VARINT); w.writeVarint(999);
			w.writeTag(2, WireType.VARINT); w.writeVarint(1);
		});
		var msg = bytes(w -> { w.writeTag(1, WireType.LEN); w.writeLenDelimited(entry); });
		var b = ProtobufParser.DEFAULT.read(msg, StrMap.class);
		assertEquals(Integer.valueOf(1), b.m.get("a"));
	}

	//------------------------------------------------------------------------------------------------------------------
	// decodeEnumOrdinal's "constants != null && ordinal >= 0 && ordinal < constants.length" (L272/274).
	//------------------------------------------------------------------------------------------------------------------

	public static class NonEnumOrdinal {
		@Protobuf(type=ProtobufScalarType.ENUM_INT)
		public int val;
		public NonEnumOrdinal() {}
	}

	@Test
	void a05_enumIntOnNonEnumType_constantsNullFallsBackToRawOrdinal() throws Exception {
		// L272: cm.inner().getEnumConstants() is null for a non-enum type -- falls back to the raw ordinal (L274).
		var msg = bytes(w -> { w.writeTag(1, WireType.VARINT); w.writeVarint(5); });
		var b = ProtobufParser.DEFAULT.read(msg, NonEnumOrdinal.class);
		assertEquals(5, b.val);
	}

	public enum E { A, B, C }

	public static class EnumBean {
		public E e;
		public EnumBean() {}
	}

	@Test
	void a06_enumOrdinalNegative_fallsBackToRawOrdinal() throws Exception {
		// A varint whose low 32 bits are all set decodes to ordinal -1 after the (int) narrowing cast in
		// decodeScalar's ENUM_INT arm, hitting the "ordinal >= 0" false branch (L272) -- falls back to the raw
		// ordinal (L274) rather than indexing into the enum's constants array. The raw Integer -1 is then
		// rejected by the bean-property setter's own int-to-enum conversion, which is a separate, expected
		// failure downstream of the branch under test here.
		var msg = bytes(w -> { w.writeTag(1, WireType.VARINT); w.writeVarint(0xFFFFFFFFL); });
		var ex = assertThrows(org.apache.juneau.marshall.parser.ParseException.class, () -> ProtobufParser.DEFAULT.read(msg, EnumBean.class));
		assertTrue(ex.getCause() instanceof InvalidDataConversionException);
	}

	@Test
	void a07_enumOrdinalOutOfRange_fallsBackToRawOrdinal() throws Exception {
		// Ordinal 99 exceeds E's 3 constants, hitting the "ordinal < constants.length" false branch (L272) --
		// falls back to the raw ordinal (L274) rather than indexing out of bounds. As in a06, the raw Integer is
		// then rejected by the setter's int-to-enum conversion, which is expected and outside this branch's scope.
		var msg = bytes(w -> { w.writeTag(1, WireType.VARINT); w.writeVarint(99); });
		var ex = assertThrows(org.apache.juneau.marshall.parser.ParseException.class, () -> ProtobufParser.DEFAULT.read(msg, EnumBean.class));
		assertTrue(ex.getCause() instanceof InvalidDataConversionException);
	}

	//------------------------------------------------------------------------------------------------------------------
	// setProperty's BeanRuntimeException catch/rethrow (L233-237) -- a getter-only property (no setter, no field)
	// throws BeanRuntimeException when the parser tries to assign a wire value to it.
	//------------------------------------------------------------------------------------------------------------------

	public static class GetterOnlyBean {
		public int i;
		public String getName() { return "fixed"; }
		public GetterOnlyBean() {}
		public GetterOnlyBean(int i) { this.i = i; }
	}

	@Test
	void a08_beanRuntimeExceptionFromSetterPropagates() throws Exception {
		// Missing setters are silently ignored by default (MarshallingContext.Builder#disableIgnoreMissingSetters()
		// defaults to false), so disableIgnoreMissingSetters() is needed to force BeanPropertyMeta.set() to actually
		// throw for the getter-only "name" property, exercising setProperty's catch/rethrow (L233-237).
		var ser = ProtobufSerializer.create().build();
		var par = ProtobufParser.create().disableIgnoreMissingSetters().build();
		var bytes = ser.write(new GetterOnlyBean(5));
		var ex = assertThrows(org.apache.juneau.marshall.parser.ParseException.class, () -> par.read(bytes, GetterOnlyBean.class));
		assertTrue(ex.getCause() instanceof BeanRuntimeException);
	}
}
