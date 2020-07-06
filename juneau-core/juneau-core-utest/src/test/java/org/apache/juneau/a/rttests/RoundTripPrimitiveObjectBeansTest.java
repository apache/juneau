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

import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.jena.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.apache.juneau.testutils.pojos.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripPrimitiveObjectBeansTest extends RoundTripTest {

	public RoundTripPrimitiveObjectBeansTest(String label, SerializerBuilder s, ParserBuilder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// testPrimitiveObjectsBean
	//====================================================================================================
	@Test
	public void testPrimitiveObjectsBean() throws Exception {
		PrimitiveObjectsBean t = PrimitiveObjectsBean.get();
		t = roundTrip(t, PrimitiveObjectsBean.class);
		t = roundTrip(t, PrimitiveObjectsBean.class);

		// primitives
		assertEquals(Boolean.valueOf(true), t.poBoolean);
		assertEquals(Byte.valueOf((byte)1), t.poByte);
		assertEquals(Character.valueOf('a'), t.poChar);
		assertEquals(Short.valueOf("2"), t.poShort);
		assertEquals(Integer.valueOf(3), t.poInt);
		assertEquals(Long.valueOf(4), t.poLong);
		assertEquals(Float.valueOf(5), t.poFloat);
		assertEquals(Double.valueOf(6), t.poDouble);
		assertEquals(Integer.valueOf(7), t.poNumber);
		assertEquals(8, t.poBigInteger.intValue());
		assertTrue(t.poBigDecimal.floatValue() == 9f);

		// uninitialized primitives
		assertNull(t.pouBoolean);
		assertNull(t.pouByte);
		assertNull(t.pouChar);
		assertNull(t.pouShort);
		assertNull(t.pouInt);
		assertNull(t.pouLong);
		assertNull(t.pouFloat);
		assertNull(t.pouDouble);
		assertNull(t.pouNumber);
		assertNull(t.pouBigInteger);
		assertNull(t.pouBigDecimal);

		// primitive arrays
		assertEquals(Boolean.valueOf(false), t.poaBoolean[1][0]);
		assertEquals(Byte.valueOf((byte)2), t.poaByte[1][0]);
		assertEquals(Character.valueOf('b'), t.poaChar[1][0]);
		assertEquals(Short.valueOf("2"), t.poaShort[1][0]);
		assertEquals(Integer.valueOf(2), t.poaInt[1][0]);
		assertEquals(Long.valueOf(2), t.poaLong[1][0]);
		assertEquals(Float.valueOf(2), t.poaFloat[1][0]);
		assertEquals(Double.valueOf(2), t.poaDouble[1][0]);
		assertEquals(Integer.valueOf(2), t.poaNumber[1][0]);
		assertEquals(2, t.poaBigInteger[1][0].intValue());
		assertEquals(2, t.poaBigDecimal[1][0].intValue());
		assertNull(t.poaBoolean[2]);
		assertNull(t.poaByte[2]);
		assertNull(t.poaChar[2]);
		assertNull(t.poaShort[2]);
		assertNull(t.poaInt[2]);
		assertNull(t.poaLong[2]);
		assertNull(t.poaFloat[2]);
		assertNull(t.poaDouble[2]);
		assertNull(t.poaNumber[2]);
		assertNull(t.poaBigInteger[2]);
		assertNull(t.poaBigDecimal[2]);

		// uninitialized primitive arrays
		assertNull(t.poauBoolean);
		assertNull(t.poauByte);
		assertNull(t.poauChar);
		assertNull(t.poauShort);
		assertNull(t.poauInt);
		assertNull(t.poauLong);
		assertNull(t.poauFloat);
		assertNull(t.poauDouble);
		assertNull(t.poauNumber);
		assertNull(t.poauBigInteger);
		assertNull(t.poauBigDecimal);

		// anonymous list of object primitive arrays
		assertEquals(Boolean.valueOf(true), t.poalBoolean.get(0)[0]);
		assertEquals(Byte.valueOf((byte)1), t.poalByte.get(0)[0]);
		assertEquals(Character.valueOf('a'), t.poalChar.get(0)[0]);
		assertEquals(Short.valueOf((short)1), t.poalShort.get(0)[0]);
		assertEquals(Integer.valueOf(1), t.poalInt.get(0)[0]);
		assertEquals(Long.valueOf(1l), t.poalLong.get(0)[0]);
		assertEquals(Float.valueOf(1f), t.poalFloat.get(0)[0]);
		assertEquals(Double.valueOf(1d), t.poalDouble.get(0)[0]);
		assertEquals(1, t.poalBigInteger.get(0)[0].intValue());
		assertEquals(1, t.poalBigDecimal.get(0)[0].intValue());
		assertNull(t.poalBoolean.get(1));
		assertNull(t.poalByte.get(1));
		assertNull(t.poalChar.get(1));
		assertNull(t.poalShort.get(1));
		assertNull(t.poalInt.get(1));
		assertNull(t.poalLong.get(1));
		assertNull(t.poalFloat.get(1));
		assertNull(t.poalDouble.get(1));
		assertNull(t.poalNumber.get(1));
		assertNull(t.poalBigInteger.get(1));
		assertNull(t.poalBigDecimal.get(1));

		// regular list of object primitive arrays
		assertEquals(Boolean.valueOf(true), t.polBoolean.get(0)[0]);
		assertEquals(Byte.valueOf((byte)1), t.polByte.get(0)[0]);
		assertEquals(Character.valueOf('a'), t.polChar.get(0)[0]);
		assertEquals(Short.valueOf((short)1), t.polShort.get(0)[0]);
		assertEquals(Integer.valueOf(1), t.polInt.get(0)[0]);
		assertEquals(Long.valueOf(1l), t.polLong.get(0)[0]);
		assertEquals(Float.valueOf(1f), t.polFloat.get(0)[0]);
		assertEquals(Double.valueOf(1d), t.polDouble.get(0)[0]);
		assertEquals(1, t.polBigInteger.get(0)[0].intValue());
		assertEquals(1, t.polBigDecimal.get(0)[0].intValue());
		assertNull(t.polBoolean.get(1));
		assertNull(t.polByte.get(1));
		assertNull(t.polChar.get(1));
		assertNull(t.polShort.get(1));
		assertNull(t.polInt.get(1));
		assertNull(t.polLong.get(1));
		assertNull(t.polFloat.get(1));
		assertNull(t.polDouble.get(1));
		assertNull(t.polNumber.get(1));
		assertNull(t.polBigInteger.get(1));
		assertNull(t.polBigDecimal.get(1));
	}

	//====================================================================================================
	// testPrimitiveAtomicObjectsBean
	//====================================================================================================
	@Test
	public void testPrimitiveAtomicObjectsBean() throws Exception {

		// Jena does not support parsing into AtomicIntegers and AtomicLongs.
		if (getSerializer() instanceof RdfSerializer)
			return;

		PrimitiveAtomicObjectsBean t = PrimitiveAtomicObjectsBean.get();
		t = roundTrip(t, PrimitiveAtomicObjectsBean.class);
		t = roundTrip(t, PrimitiveAtomicObjectsBean.class);

		// primitives
		assertEquals(1, t.poAtomicInteger.intValue());
		assertEquals(2, t.poAtomicLong.intValue());

		// uninitialized primitives
		assertNull(t.pouAtomicInteger);
		assertNull(t.pouAtomicLong);

		// primitive arrays
		assertEquals(2, t.poaAtomicInteger[1][0].intValue());
		assertEquals(2, t.poaAtomicLong[1][0].intValue());
		assertNull(t.poaAtomicInteger[2]);
		assertNull(t.poaAtomicLong[2]);

		// uninitialized primitive arrays
		assertNull(t.poauAtomicInteger);
		assertNull(t.poauAtomicLong);

		// anonymous list of object primitive arrays
		assertEquals(1, t.poalAtomicInteger.get(0)[0].intValue());
		assertEquals(1, t.poalAtomicLong.get(0)[0].intValue());
		assertNull(t.poalAtomicInteger.get(1));
		assertNull(t.poalAtomicLong.get(1));

		// regular list of object primitive arrays
		assertEquals(1, t.polAtomicInteger.get(0)[0].intValue());
		assertEquals(1, t.polAtomicLong.get(0)[0].intValue());
		assertNull(t.polAtomicInteger.get(1));
		assertNull(t.polAtomicLong.get(1));
	}

}
