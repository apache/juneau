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
package org.apache.juneau.transforms;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.json5.*;
import org.junit.jupiter.api.*;

/**
 * Precedence chain tests for {@link BinaryFormat} and {@link EnumFormat}:
 * {@code @MarshalledProp} &gt; {@code @Marshalled} &gt; {@code MarshallingContext} &gt; default.
 */
class BinaryEnumFormatPlacement_Test {

	private static final byte[] BYTES = "Hello".getBytes();

	//------------------------------------------------------------------------------------------------------------------
	// BinaryFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 { public byte[] b = BYTES; }

	@Test void a01_binary_defaultIsNotSet() throws Exception {
		// NOT_SET → no swap fires → byte[] falls back to the serializer's native array representation.
		var s = Json5Serializer.create().build();
		assertEquals("{b:[72,101,108,108,111]}", s.serialize(new A01()));
	}

	@Test void a02_binary_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().binaryFormat(BinaryFormat.BASE64).build();
		assertEquals("{b:'SGVsbG8='}", s.serialize(new A01()));
	}

	@Marshalled(binaryFormat = BinaryFormat.BASE64)
	public static class A03 { public byte[] b = BYTES; }

	@Test void a03_binary_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().binaryFormat(BinaryFormat.HEX).build();
		assertEquals("{b:'SGVsbG8='}", s.serialize(new A03()));
	}

	@Marshalled(binaryFormat = BinaryFormat.HEX)
	public static class A04 {
		@MarshalledProp(binaryFormat = BinaryFormat.SPACED_HEX)
		public byte[] b = BYTES;
	}

	@Test void a04_binary_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{b:'48 65 6C 6C 6F'}", s.serialize(new A04()));
	}

	public static class A05 { public byte[] b = BYTES; }

	@Test void a05_binary_base64Url() throws Exception {
		var s = Json5Serializer.create().binaryFormat(BinaryFormat.BASE64_URL).build();
		assertEquals("{b:'SGVsbG8'}", s.serialize(new A05()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// EnumFormat
	//------------------------------------------------------------------------------------------------------------------

	public enum E {
		ALPHA_BETA, GAMMA;

		@Override
		public String toString() {
			return name().toLowerCase().replace('_', ' ');
		}
	}

	public static class B01 { public E e = E.ALPHA_BETA; }

	@Test void b01_enum_defaultIsToString() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{e:'alpha beta'}", s.serialize(new B01()));
	}

	@Test void b02_enum_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().enumFormat(EnumFormat.NAME).build();
		assertEquals("{e:'ALPHA_BETA'}", s.serialize(new B01()));
	}

	@Marshalled(enumFormat = EnumFormat.NAME)
	public static class B03 { public E e = E.ALPHA_BETA; }

	@Test void b03_enum_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().enumFormat(EnumFormat.LOWER_HYPHEN).build();
		assertEquals("{e:'ALPHA_BETA'}", s.serialize(new B03()));
	}

	@Marshalled(enumFormat = EnumFormat.NAME)
	public static class B04 {
		@MarshalledProp(enumFormat = EnumFormat.LOWER_HYPHEN)
		public E e = E.ALPHA_BETA;
	}

	@Test void b04_enum_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{e:'alpha-beta'}", s.serialize(new B04()));
	}

	public static class B05 { public E e = E.ALPHA_BETA; }

	@Test void b05_enum_ordinal() throws Exception {
		var s = Json5Serializer.create().enumFormat(EnumFormat.ORDINAL).build();
		assertEquals("{e:0}", s.serialize(new B05()));
	}

	@Marshalled(enumFormat = EnumFormat.ORDINAL)
	public static class B06 { public E e = E.GAMMA; }

	@Test void b06_enum_ordinalAtClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{e:1}", s.serialize(new B06()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @MarshalledConfig integration
	//------------------------------------------------------------------------------------------------------------------

	@MarshalledConfig(
		binaryFormat = BinaryFormat.HEX,
		enumFormat = EnumFormat.NAME
	)
	static class C01Config {}

	public static class C01Bean {
		public byte[] b = BYTES;
		public E e = E.ALPHA_BETA;
	}

	@Test void c01_marshalledConfig_applies() throws Exception {
		var s = Json5Serializer.create().applyAnnotations(C01Config.class).build();
		var json = (String) s.serialize(new C01Bean());
		assertTrue(json.contains("b:'48656C6C6F'"), "binary hex: " + json);
		assertTrue(json.contains("e:'ALPHA_BETA'"), "enum name: " + json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// NOT_SET sentinel falls through to next-higher precedence
	//------------------------------------------------------------------------------------------------------------------

	@Marshalled(binaryFormat = BinaryFormat.NOT_SET, enumFormat = EnumFormat.NOT_SET)
	public static class D01 {
		public byte[] b = BYTES;
		public E e = E.ALPHA_BETA;
	}

	@Test void d01_notSet_fallsThroughToContext() throws Exception {
		var s = Json5Serializer.create()
			.binaryFormat(BinaryFormat.HEX)
			.enumFormat(EnumFormat.NAME)
			.build();
		var json = (String) s.serialize(new D01());
		assertTrue(json.contains("b:'48656C6C6F'"), "binary hex: " + json);
		assertTrue(json.contains("e:'ALPHA_BETA'"), "enum name: " + json);
	}
}
