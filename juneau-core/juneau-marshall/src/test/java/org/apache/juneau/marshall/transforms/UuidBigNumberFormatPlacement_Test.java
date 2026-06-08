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
package org.apache.juneau.marshall.transforms;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;
import java.util.*;

import org.apache.juneau.marshall.*;
import org.apache.juneau.marshall.json5.*;
import org.junit.jupiter.api.*;

/**
 * Precedence chain tests for {@link UuidFormat} and {@link BigNumberFormat}:
 * {@code @MarshalledProp} &gt; {@code @Marshalled} &gt; {@code MarshallingContext} &gt; default.
 */
class UuidBigNumberFormatPlacement_Test {

	private static final UUID U = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
	private static final BigInteger HUGE = new BigInteger("123456789012345678901234567890");

	//------------------------------------------------------------------------------------------------------------------
	// UuidFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class A01 { public UUID u = U; }

	@Test void a01_uuid_defaultIsStandard() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{u:'550e8400-e29b-41d4-a716-446655440000'}", s.serialize(new A01()));
	}

	@Test void a02_uuid_contextOverridesDefault() throws Exception {
		var s = Json5Serializer.create().uuidFormat(UuidFormat.NO_DASHES).build();
		assertEquals("{u:'550e8400e29b41d4a716446655440000'}", s.serialize(new A01()));
	}

	@Marshalled(uuidFormat = UuidFormat.URN)
	public static class A03 { public UUID u = U; }

	@Test void a03_uuid_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().uuidFormat(UuidFormat.NO_DASHES).build();
		assertEquals("{u:'urn:uuid:550e8400-e29b-41d4-a716-446655440000'}", s.serialize(new A03()));
	}

	@Marshalled(uuidFormat = UuidFormat.URN)
	public static class A04 {
		@MarshalledProp(uuidFormat = UuidFormat.NO_DASHES)
		public UUID u = U;
	}

	@Test void a04_uuid_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{u:'550e8400e29b41d4a716446655440000'}", s.serialize(new A04()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// BigNumberFormat
	//------------------------------------------------------------------------------------------------------------------

	public static class B01 { public BigInteger n = HUGE; }

	@Test void b01_bigNumber_defaultIsNumber() throws Exception {
		var s = Json5Serializer.create().build();
		// Bare numeric token (today's wire).
		assertEquals("{n:" + HUGE + "}", s.serialize(new B01()));
	}

	@Test void b02_bigNumber_contextString() throws Exception {
		var s = Json5Serializer.create().bigNumberFormat(BigNumberFormat.STRING).build();
		assertEquals("{n:'" + HUGE + "'}", s.serialize(new B01()));
	}

	@Marshalled(bigNumberFormat = BigNumberFormat.STRING)
	public static class B03 { public BigInteger n = HUGE; }

	@Test void b03_bigNumber_classOverridesContext() throws Exception {
		var s = Json5Serializer.create().bigNumberFormat(BigNumberFormat.NUMBER).build();
		assertEquals("{n:'" + HUGE + "'}", s.serialize(new B03()));
	}

	@Marshalled(bigNumberFormat = BigNumberFormat.NUMBER)
	public static class B04 {
		@MarshalledProp(bigNumberFormat = BigNumberFormat.STRING)
		public BigInteger n = HUGE;
	}

	@Test void b04_bigNumber_propertyOverridesClass() throws Exception {
		var s = Json5Serializer.create().build();
		assertEquals("{n:'" + HUGE + "'}", s.serialize(new B04()));
	}

	public static class B05 { public BigInteger n = BigInteger.valueOf(42L); }

	@Test void b05_bigNumber_auto_safeStaysNumber() throws Exception {
		var s = Json5Serializer.create().bigNumberFormat(BigNumberFormat.AUTO).build();
		assertEquals("{n:42}", s.serialize(new B05()));
	}

	public static class B06 { public BigInteger n = HUGE; }

	@Test void b06_bigNumber_auto_unsafeBecomesString() throws Exception {
		var s = Json5Serializer.create().bigNumberFormat(BigNumberFormat.AUTO).build();
		assertEquals("{n:'" + HUGE + "'}", s.serialize(new B06()));
	}

	public static class B07 { public BigDecimal n = new BigDecimal("3.14"); }

	@Test void b07_bigDecimal_auto_fractionalAlwaysString() throws Exception {
		var s = Json5Serializer.create().bigNumberFormat(BigNumberFormat.AUTO).build();
		assertEquals("{n:'3.14'}", s.serialize(new B07()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// @MarshalledConfig integration
	//------------------------------------------------------------------------------------------------------------------

	@MarshalledConfig(
		uuidFormat = UuidFormat.NO_DASHES,
		bigNumberFormat = BigNumberFormat.STRING
	)
	static class C01Config {}

	public static class C01Bean {
		public UUID u = U;
		public BigInteger n = HUGE;
	}

	@Test void c01_marshalledConfig_applies() throws Exception {
		var s = Json5Serializer.create().applyAnnotations(C01Config.class).build();
		var json = s.serialize(new C01Bean());
		assertTrue(json.contains("u:'550e8400e29b41d4a716446655440000'"), "uuid no-dashes: " + json);
		assertTrue(json.contains("n:'" + HUGE + "'"), "bigNumber string: " + json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// NOT_SET sentinel falls through to next-higher precedence
	//------------------------------------------------------------------------------------------------------------------

	@Marshalled(uuidFormat = UuidFormat.NOT_SET, bigNumberFormat = BigNumberFormat.NOT_SET)
	public static class D01 {
		public UUID u = U;
		public BigInteger n = HUGE;
	}

	@Test void d01_notSet_fallsThroughToContext() throws Exception {
		var s = Json5Serializer.create()
			.uuidFormat(UuidFormat.NO_DASHES)
			.bigNumberFormat(BigNumberFormat.STRING)
			.build();
		var json = s.serialize(new D01());
		assertTrue(json.contains("u:'550e8400e29b41d4a716446655440000'"), "uuid: " + json);
		assertTrue(json.contains("n:'" + HUGE + "'"), "bigNumber: " + json);
	}
}
