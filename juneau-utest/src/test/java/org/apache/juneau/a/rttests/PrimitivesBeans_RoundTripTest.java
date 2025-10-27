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
package org.apache.juneau.a.rttests;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class PrimitivesBeans_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// testPrimitivesBean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_primitivesBean(RoundTrip_Tester t) throws Exception {
		var x = new PrimitivesBean().init();
		x = t.roundTrip(x, PrimitivesBean.class);

		// primitives
		assertEquals(true, x.pBoolean);
		assertEquals(1, x.pByte);
		assertEquals('a', x.pChar);
		assertEquals(2, x.pShort);
		assertEquals(3, x.pInt);
		assertEquals(4L, x.pLong);
		assertEquals(5f, x.pFloat, 0.1f);
		assertEquals(6d, x.pDouble, 0.1f);

		// uninitialized primitives
		assertEquals(false, x.puBoolean);
		assertEquals(0, x.puByte);
		assertEquals((char)0, x.puChar);
		assertEquals(0, x.puShort);
		assertEquals(0, x.puInt);
		assertEquals(0L, x.puLong);
		assertEquals(0f, x.puFloat, 0.1f);
		assertEquals(0d, x.puDouble, 0.1f);

		// primitive arrays
		assertEquals(false, x.paBoolean[1][0]);
		assertEquals(2, x.paByte[1][0]);
		assertEquals('b', x.paChar[1][0]);
		assertEquals(2, x.paShort[1][0]);
		assertEquals(2, x.paInt[1][0]);
		assertEquals(2L, x.paLong[1][0]);
		assertEquals(2f, x.paFloat[1][0], 0.1f);
		assertEquals(2d, x.paDouble[1][0], 0.1f);
		assertNull(x.paBoolean[2]);
		assertNull(x.paByte[2]);
		assertNull(x.paChar[2]);
		assertNull(x.paShort[2]);
		assertNull(x.paInt[2]);
		assertNull(x.paLong[2]);
		assertNull(x.paFloat[2]);
		assertNull(x.paDouble[2]);

		// uninitialized primitive arrays
		assertNull(x.pauBoolean);
		assertNull(x.pauByte);
		assertNull(x.pauChar);
		assertNull(x.pauShort);
		assertNull(x.pauInt);
		assertNull(x.pauLong);
		assertNull(x.pauFloat);
		assertNull(x.pauDouble);

		// anonymous list of primitive arrays
		assertEquals(true, x.palBoolean.get(0)[0]);
		assertEquals(1, x.palByte.get(0)[0]);
		assertEquals('a', x.palChar.get(0)[0]);
		assertEquals(1, x.palShort.get(0)[0]);
		assertEquals(1, x.palInt.get(0)[0]);
		assertEquals(1L, x.palLong.get(0)[0]);
		assertEquals(1f, x.palFloat.get(0)[0], 0.1f);
		assertEquals(1d, x.palDouble.get(0)[0], 0.1f);
		assertNull(x.palBoolean.get(1));
		assertNull(x.palByte.get(1));
		assertNull(x.palChar.get(1));
		assertNull(x.palShort.get(1));
		assertNull(x.palInt.get(1));
		assertNull(x.palLong.get(1));
		assertNull(x.palFloat.get(1));
		assertNull(x.palDouble.get(1));

		// regular list of primitive arrays
		assertEquals(true, x.plBoolean.get(0)[0]);
		assertEquals(1, x.plByte.get(0)[0]);
		assertEquals('a', x.plChar.get(0)[0]);
		assertEquals(1, x.plShort.get(0)[0]);
		assertEquals(1, x.plInt.get(0)[0]);
		assertEquals(1L, x.plLong.get(0)[0]);
		assertEquals(1f, x.plFloat.get(0)[0], 0.1f);
		assertEquals(1d, x.plDouble.get(0)[0], 0.1f);
		assertNull(x.plBoolean.get(1));
		assertNull(x.plByte.get(1));
		assertNull(x.plChar.get(1));
		assertNull(x.plShort.get(1));
		assertNull(x.plInt.get(1));
		assertNull(x.plLong.get(1));
		assertNull(x.plFloat.get(1));
		assertNull(x.plDouble.get(1));
	}

	public static class PrimitivesBean {

		// primitives
		public boolean pBoolean;
		public byte pByte;
		public char pChar;
		public short pShort;
		public int pInt;
		public long pLong;
		public float pFloat;
		public double pDouble;

		// uninitialized primitives
		public boolean puBoolean;
		public byte puByte;
		public char puChar;
		public short puShort;
		public int puInt;
		public long puLong;
		public float puFloat;
		public double puDouble;

		// primitive arrays
		public boolean[][] paBoolean;
		public byte[][] paByte;
		public char[][] paChar;
		public short[][] paShort;
		public int[][] paInt;
		public long[][] paLong;
		public float[][] paFloat;
		public double[][] paDouble;

		// uninitialized primitive arrays
		public boolean[][] pauBoolean;
		public byte[][] pauByte;
		public char[][] pauChar;
		public short[][] pauShort;
		public int[][] pauInt;
		public long[][] pauLong;
		public float[][] pauFloat;
		public double[][] pauDouble;

		// Regular lists of primitives
		public List<boolean[]> plBoolean;
		public List<byte[]> plByte;
		public List<char[]> plChar;
		public List<short[]> plShort;
		public List<int[]> plInt;
		public List<long[]> plLong;
		public List<float[]> plFloat;
		public List<double[]> plDouble;

		// Anonymous list of primitives
		public List<boolean[]> palBoolean;
		public List<byte[]> palByte;
		public List<char[]> palChar;
		public List<short[]> palShort;
		public List<int[]> palInt;
		public List<long[]> palLong;
		public List<float[]> palFloat;
		public List<double[]> palDouble;

		public PrimitivesBean init() {
			// primitives
			pBoolean = true;
			pByte = 1;
			pChar = 'a';
			pShort = 2;
			pInt = 3;
			pLong = 4L;
			pFloat = 5f;
			pDouble = 6d;

			// primitive arrays
			paBoolean = new boolean[][]{{true},{false},null};
			paByte = new byte[][]{{1},{2},null};
			paChar = new char[][]{{'a'},{'b'},null};
			paShort = new short[][]{{1},{2},null};
			paInt = new int[][]{{1},{2},null};
			paLong = new long[][]{{1},{2},null};
			paFloat = new float[][]{{1},{2},null};
			paDouble = new double[][]{{1},{2},null};

			// Regular lists of primitives
			plBoolean = alist(new boolean[]{true}, null);
			plByte = alist(new byte[]{1}, null);
			plChar = alist(new char[]{'a'}, null);
			plShort = alist(new short[]{1}, null);
			plInt = alist(new int[]{1}, null);
			plLong = alist(new long[]{1}, null);
			plFloat = alist(new float[]{1}, null);
			plDouble = alist(new double[]{1}, null);

			// Anonymous list of primitives
			palBoolean = new ArrayList<>();
			palBoolean.add(new boolean[]{true});
			palBoolean.add(null);
			palByte = new ArrayList<>();
			palByte.add(new byte[]{1});
			palByte.add(null);
			palChar = new ArrayList<>();
			palChar.add(new char[]{'a'});
			palChar.add(null);
			palShort = new ArrayList<>();
			palShort.add(new short[]{1});
			palShort.add(null);
			palInt = new ArrayList<>();
			palInt.add(new int[]{1});
			palInt.add(null);
			palLong = new ArrayList<>();
			palLong.add(new long[]{1});
			palLong.add(null);
			palFloat = new ArrayList<>();
			palFloat.add(new float[]{1});
			palFloat.add(null);
			palDouble = new ArrayList<>();
			palDouble.add(new double[]{1});
			palDouble.add(null);
			return this;
		}
	}

	//====================================================================================================
	// List of PrimitivesBean
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a02_primitivesBeanList(RoundTrip_Tester t) throws Exception {
		var x = alist(new PrimitivesBean().init(), null, new PrimitivesBean().init());
		if (t.getParser() == null)
			return;
		x = t.roundTrip(x, List.class, PrimitivesBean.class);

		var t2 = x.get(2);

		// primitives
		assertEquals(true, t2.pBoolean);
		assertEquals(1, t2.pByte);
		assertEquals('a', t2.pChar);
		assertEquals(2, t2.pShort);
		assertEquals(3, t2.pInt);
		assertEquals(4L, t2.pLong);
		assertEquals(5f, t2.pFloat, 0.1f);
		assertEquals(6d, t2.pDouble, 0.1f);

		// uninitialized primitives
		assertEquals(false, t2.puBoolean);
		assertEquals(0, t2.puByte);
		assertEquals((char)0, t2.puChar);
		assertEquals(0, t2.puShort);
		assertEquals(0, t2.puInt);
		assertEquals(0L, t2.puLong);
		assertEquals(0f, t2.puFloat, 0.1f);
		assertEquals(0d, t2.puDouble, 0.1f);

		// primitive arrays
		assertEquals(false, t2.paBoolean[1][0]);
		assertEquals(2, t2.paByte[1][0]);
		assertEquals('b', t2.paChar[1][0]);
		assertEquals(2, t2.paShort[1][0]);
		assertEquals(2, t2.paInt[1][0]);
		assertEquals(2L, t2.paLong[1][0]);
		assertEquals(2f, t2.paFloat[1][0], 0.1f);
		assertEquals(2d, t2.paDouble[1][0], 0.1f);
		assertNull(t2.paBoolean[2]);
		assertNull(t2.paByte[2]);
		assertNull(t2.paChar[2]);
		assertNull(t2.paShort[2]);
		assertNull(t2.paInt[2]);
		assertNull(t2.paLong[2]);
		assertNull(t2.paFloat[2]);
		assertNull(t2.paDouble[2]);

		// uninitialized primitive arrays
		assertNull(t2.pauBoolean);
		assertNull(t2.pauByte);
		assertNull(t2.pauChar);
		assertNull(t2.pauShort);
		assertNull(t2.pauInt);
		assertNull(t2.pauLong);
		assertNull(t2.pauFloat);
		assertNull(t2.pauDouble);

		// anonymous list of primitive arrays
		assertEquals(true, t2.palBoolean.get(0)[0]);
		assertEquals(1, t2.palByte.get(0)[0]);
		assertEquals('a', t2.palChar.get(0)[0]);
		assertEquals(1, t2.palShort.get(0)[0]);
		assertEquals(1, t2.palInt.get(0)[0]);
		assertEquals(1L, t2.palLong.get(0)[0]);
		assertEquals(1f, t2.palFloat.get(0)[0], 0.1f);
		assertEquals(1d, t2.palDouble.get(0)[0], 0.1f);
		assertNull(t2.palBoolean.get(1));
		assertNull(t2.palByte.get(1));
		assertNull(t2.palChar.get(1));
		assertNull(t2.palShort.get(1));
		assertNull(t2.palInt.get(1));
		assertNull(t2.palLong.get(1));
		assertNull(t2.palFloat.get(1));
		assertNull(t2.palDouble.get(1));

		// regular list of primitive arrays
		assertEquals(true, t2.plBoolean.get(0)[0]);
		assertEquals(1, t2.plByte.get(0)[0]);
		assertEquals('a', t2.plChar.get(0)[0]);
		assertEquals(1, t2.plShort.get(0)[0]);
		assertEquals(1, t2.plInt.get(0)[0]);
		assertEquals(1L, t2.plLong.get(0)[0]);
		assertEquals(1f, t2.plFloat.get(0)[0], 0.1f);
		assertEquals(1d, t2.plDouble.get(0)[0], 0.1f);
		assertNull(t2.plBoolean.get(1));
		assertNull(t2.plByte.get(1));
		assertNull(t2.plChar.get(1));
		assertNull(t2.plShort.get(1));
		assertNull(t2.plInt.get(1));
		assertNull(t2.plLong.get(1));
		assertNull(t2.plFloat.get(1));
		assertNull(t2.plDouble.get(1));

		assertNull(x.get(1));
	}
}