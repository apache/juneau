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
package org.apache.juneau.marshall.jcs;

import static org.junit.jupiter.api.Assertions.*;

import java.math.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.serializer.*;
import org.junit.jupiter.api.*;

/**
 * Coverage-focused tests for {@link JcsSerializerSession} targeting branches not already exercised by
 * {@link JcsNumbers_Test}, {@link JcsSorting_Test}, {@link JcsStrings_Test}, {@link JcsEdgeCases_Test}, and
 * {@link JcsCanonical_Test}:
 *  - {@code toEcmaNumber}'s {@code Float} NaN/Infinite rejection arms (only the {@code Double} arms were covered).
 *  - {@code jcsCompare}'s both-null arm.
 *  - {@code formatBigDecimal}/{@code formatBigInteger}'s out-of-range rejection arms.
 *  - {@code getJsonWriter}'s reuse arm (output already a {@link JcsWriter}).
 *  - {@code writeBeanMap}'s {@code isKeepNullProperties()} branch combinations.
 */
class JcsSerializerSession_Coverage_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// a. toEcmaNumber: Float NaN/Infinite rejection (mirrors the Double arms already covered elsewhere).
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_floatNaNRejected() {
		assertThrows(SerializeException.class, () -> JcsSerializerSession.toEcmaNumber(Float.NaN));
	}

	@Test void a02_floatPositiveInfinityRejected() {
		assertThrows(SerializeException.class, () -> JcsSerializerSession.toEcmaNumber(Float.POSITIVE_INFINITY));
	}

	@Test void a03_floatNegativeInfinityRejected() {
		assertThrows(SerializeException.class, () -> JcsSerializerSession.toEcmaNumber(Float.NEGATIVE_INFINITY));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b. jcsCompare: both-null arm (a==null path only tested with a non-null b elsewhere).
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_bothNullKeysCompareEqual() {
		assertEquals(0, JcsSerializerSession.jcsCompare(null, null));
	}

	@Test void b02_nullFirstNonNullSecond() {
		assertEquals(-1, JcsSerializerSession.jcsCompare(null, "a"));
	}

	@Test void b03_nonNullFirstNullSecond() {
		assertEquals(1, JcsSerializerSession.jcsCompare("a", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// c. formatDoubleScientific: lowercase 'e' exponent marker in Double.toString() output.
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_scientificMantissaEndsWithDotZero() throws Exception {
		// 2.0E25 is outside [1e-6, 1e21) so it uses scientific notation; Double.toString() yields "2.0E25",
		// whose mantissa "2.0" hits the endsWith(".0") truncation arm (not the "1.0"/"1" special case),
		// producing "2e+25".
		assertEquals("2e+25", JcsSerializer.DEFAULT.write(2.0E25));
	}

	//------------------------------------------------------------------------------------------------------------------
	// d. formatBigDecimal / formatBigInteger: out-of-range rejection arms.
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_bigDecimalExceedsDoubleRange_throws() {
		// A BigDecimal whose magnitude exceeds Double.MAX_VALUE converts to +/-Infinity via doubleValue(),
		// hitting formatBigDecimal's isInfinite() rejection arm.
		var bd = BigDecimal.valueOf(10).pow(400);
		var ex = assertThrows(SerializeException.class, () -> JcsSerializerSession.toEcmaNumber(bd));
		assertTrue(ex.getMessage().contains("BigDecimal"));
	}

	@Test void d02_bigIntegerExceedsLongRange_throws() {
		var bi = BigInteger.TEN.pow(30);
		var ex = assertThrows(SerializeException.class, () -> JcsSerializerSession.toEcmaNumber(bi));
		assertTrue(ex.getMessage().contains("BigInteger"));
	}

	@Test void d03_bigDecimalInRange_roundTrips() throws Exception {
		assertEquals("3.5", JcsSerializer.DEFAULT.write(BigDecimal.valueOf(3.5)));
	}

	@Test void d04_bigIntegerInRange_roundTrips() throws Exception {
		assertEquals("123", JcsSerializer.DEFAULT.write(BigInteger.valueOf(123)));
	}

	//------------------------------------------------------------------------------------------------------------------
	// e. getJsonWriter: reuse arm (output already a JcsWriter).
	//------------------------------------------------------------------------------------------------------------------

	public static class E01_Bean {
		public int x = 5;
	}

	@Test void e01_getJsonWriter_alreadyJcsWriter() throws Exception {
		var sw = new java.io.StringWriter();
		try (var w = new JcsWriter(sw, false, -1, false, '"', false, false, null)) {
			JcsSerializer.DEFAULT.write(new E01_Bean(), w);
			assertEquals("{\"x\":5}", sw.toString());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// f. writeBeanMap: isKeepNullProperties() branch combinations for the checkNull predicate.
	//------------------------------------------------------------------------------------------------------------------

	public static class F01_Bean {
		public String a = "x";
		public String b;
	}

	@Test void f01_keepNullPropertiesFalse_nullOmitted() throws Exception {
		assertEquals("{\"a\":\"x\"}", JcsSerializer.DEFAULT.write(new F01_Bean()));
	}

	@Test void f02_keepNullPropertiesTrue_nullKept() throws Exception {
		var s = JcsSerializer.create().keepNullProperties().build();
		assertEquals("{\"a\":\"x\",\"b\":null}", s.write(new F01_Bean()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// g. writeBeanMap: bean-getter-exception arm (mirrors ProtobufSerializerSession_Test's c01).
	//------------------------------------------------------------------------------------------------------------------

	public static class G01_ThrowBean {
		public String a = "ok";
		public String getBad() { throw new RuntimeException("boom"); }
	}

	@Test void g01_beanGetterExceptionDefaultThrows() {
		assertThrows(SerializeException.class, () -> JcsSerializer.DEFAULT.write(new G01_ThrowBean()));
	}
}
