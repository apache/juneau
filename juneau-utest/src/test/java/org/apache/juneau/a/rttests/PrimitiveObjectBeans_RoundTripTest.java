// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.a.rttests;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.testutils.pojos.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class PrimitiveObjectBeans_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// testPrimitiveObjectsBean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_primitiveObjectsBean(RoundTripTester t) throws Exception {
		var x = PrimitiveObjectsBean.get();
		x = t.roundTrip(x, PrimitiveObjectsBean.class);
		x = t.roundTrip(x, PrimitiveObjectsBean.class);

		// primitives
		assertEquals(Boolean.valueOf(true), x.poBoolean);
		assertEquals(Byte.valueOf((byte)1), x.poByte);
		assertEquals(Character.valueOf('a'), x.poChar);
		assertEquals(Short.valueOf("2"), x.poShort);
		assertEquals(Integer.valueOf(3), x.poInt);
		assertEquals(Long.valueOf(4), x.poLong);
		assertEquals(Float.valueOf(5), x.poFloat);
		assertEquals(Double.valueOf(6), x.poDouble);
		assertEquals(Integer.valueOf(7), x.poNumber);
		assertEquals(8, x.poBigInteger.intValue());
		assertEquals(9f, x.poBigDecimal.floatValue(), 0.1f);

		// uninitialized primitives
		assertNull(x.pouBoolean);
		assertNull(x.pouByte);
		assertNull(x.pouChar);
		assertNull(x.pouShort);
		assertNull(x.pouInt);
		assertNull(x.pouLong);
		assertNull(x.pouFloat);
		assertNull(x.pouDouble);
		assertNull(x.pouNumber);
		assertNull(x.pouBigInteger);
		assertNull(x.pouBigDecimal);

		// primitive arrays
		assertEquals(Boolean.valueOf(false), x.poaBoolean[1][0]);
		assertEquals(Byte.valueOf((byte)2), x.poaByte[1][0]);
		assertEquals(Character.valueOf('b'), x.poaChar[1][0]);
		assertEquals(Short.valueOf("2"), x.poaShort[1][0]);
		assertEquals(Integer.valueOf(2), x.poaInt[1][0]);
		assertEquals(Long.valueOf(2), x.poaLong[1][0]);
		assertEquals(Float.valueOf(2), x.poaFloat[1][0]);
		assertEquals(Double.valueOf(2), x.poaDouble[1][0]);
		assertEquals(Integer.valueOf(2), x.poaNumber[1][0]);
		assertEquals(2, x.poaBigInteger[1][0].intValue());
		assertEquals(2, x.poaBigDecimal[1][0].intValue());
		assertNull(x.poaBoolean[2]);
		assertNull(x.poaByte[2]);
		assertNull(x.poaChar[2]);
		assertNull(x.poaShort[2]);
		assertNull(x.poaInt[2]);
		assertNull(x.poaLong[2]);
		assertNull(x.poaFloat[2]);
		assertNull(x.poaDouble[2]);
		assertNull(x.poaNumber[2]);
		assertNull(x.poaBigInteger[2]);
		assertNull(x.poaBigDecimal[2]);

		// uninitialized primitive arrays
		assertNull(x.poauBoolean);
		assertNull(x.poauByte);
		assertNull(x.poauChar);
		assertNull(x.poauShort);
		assertNull(x.poauInt);
		assertNull(x.poauLong);
		assertNull(x.poauFloat);
		assertNull(x.poauDouble);
		assertNull(x.poauNumber);
		assertNull(x.poauBigInteger);
		assertNull(x.poauBigDecimal);

		// anonymous list of object primitive arrays
		assertEquals(Boolean.valueOf(true), x.poalBoolean.get(0)[0]);
		assertEquals(Byte.valueOf((byte)1), x.poalByte.get(0)[0]);
		assertEquals(Character.valueOf('a'), x.poalChar.get(0)[0]);
		assertEquals(Short.valueOf((short)1), x.poalShort.get(0)[0]);
		assertEquals(Integer.valueOf(1), x.poalInt.get(0)[0]);
		assertEquals(Long.valueOf(1L), x.poalLong.get(0)[0]);
		assertEquals(Float.valueOf(1f), x.poalFloat.get(0)[0]);
		assertEquals(Double.valueOf(1d), x.poalDouble.get(0)[0]);
		assertEquals(1, x.poalBigInteger.get(0)[0].intValue());
		assertEquals(1, x.poalBigDecimal.get(0)[0].intValue());
		assertNull(x.poalBoolean.get(1));
		assertNull(x.poalByte.get(1));
		assertNull(x.poalChar.get(1));
		assertNull(x.poalShort.get(1));
		assertNull(x.poalInt.get(1));
		assertNull(x.poalLong.get(1));
		assertNull(x.poalFloat.get(1));
		assertNull(x.poalDouble.get(1));
		assertNull(x.poalNumber.get(1));
		assertNull(x.poalBigInteger.get(1));
		assertNull(x.poalBigDecimal.get(1));

		// regular list of object primitive arrays
		assertEquals(Boolean.valueOf(true), x.polBoolean.get(0)[0]);
		assertEquals(Byte.valueOf((byte)1), x.polByte.get(0)[0]);
		assertEquals(Character.valueOf('a'), x.polChar.get(0)[0]);
		assertEquals(Short.valueOf((short)1), x.polShort.get(0)[0]);
		assertEquals(Integer.valueOf(1), x.polInt.get(0)[0]);
		assertEquals(Long.valueOf(1L), x.polLong.get(0)[0]);
		assertEquals(Float.valueOf(1f), x.polFloat.get(0)[0]);
		assertEquals(Double.valueOf(1d), x.polDouble.get(0)[0]);
		assertEquals(1, x.polBigInteger.get(0)[0].intValue());
		assertEquals(1, x.polBigDecimal.get(0)[0].intValue());
		assertNull(x.polBoolean.get(1));
		assertNull(x.polByte.get(1));
		assertNull(x.polChar.get(1));
		assertNull(x.polShort.get(1));
		assertNull(x.polInt.get(1));
		assertNull(x.polLong.get(1));
		assertNull(x.polFloat.get(1));
		assertNull(x.polDouble.get(1));
		assertNull(x.polNumber.get(1));
		assertNull(x.polBigInteger.get(1));
		assertNull(x.polBigDecimal.get(1));
	}

	//====================================================================================================
	// testPrimitiveAtomicObjectsBean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_primitiveAtomicObjectsBean(RoundTripTester t) throws Exception {

		var x = PrimitiveAtomicObjectsBean.get();
		x = t.roundTrip(x, PrimitiveAtomicObjectsBean.class);
		x = t.roundTrip(x, PrimitiveAtomicObjectsBean.class);

		// primitives
		assertEquals(1, x.poAtomicInteger.intValue());
		assertEquals(2, x.poAtomicLong.intValue());

		// uninitialized primitives
		assertNull(x.pouAtomicInteger);
		assertNull(x.pouAtomicLong);

		// primitive arrays
		assertEquals(2, x.poaAtomicInteger[1][0].intValue());
		assertEquals(2, x.poaAtomicLong[1][0].intValue());
		assertNull(x.poaAtomicInteger[2]);
		assertNull(x.poaAtomicLong[2]);

		// uninitialized primitive arrays
		assertNull(x.poauAtomicInteger);
		assertNull(x.poauAtomicLong);

		// anonymous list of object primitive arrays
		assertEquals(1, x.poalAtomicInteger.get(0)[0].intValue());
		assertEquals(1, x.poalAtomicLong.get(0)[0].intValue());
		assertNull(x.poalAtomicInteger.get(1));
		assertNull(x.poalAtomicLong.get(1));

		// regular list of object primitive arrays
		assertEquals(1, x.polAtomicInteger.get(0)[0].intValue());
		assertEquals(1, x.polAtomicLong.get(0)[0].intValue());
		assertNull(x.polAtomicInteger.get(1));
		assertNull(x.polAtomicLong.get(1));
	}
}