/***************************************************************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 ***************************************************************************************************************************/
package org.apache.juneau.a.rttests;

import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"unchecked","serial","javadoc"})
public class RoundTripPrimitivesBeansTest extends RoundTripTest {

	public RoundTripPrimitivesBeansTest(String label, Serializer s, Parser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// testPrimitivesBean
	//====================================================================================================
	@Test
	public void testPrimitivesBean() throws Exception {
		PrimitivesBean t = new PrimitivesBean().init();
		t = roundTrip(t, PrimitivesBean.class);

		// primitives
		assertEquals(true, t.pBoolean);
		assertEquals(1, t.pByte);
		assertEquals('a', t.pChar);
		assertEquals(2, t.pShort);
		assertEquals(3, t.pInt);
		assertEquals(4l, t.pLong);
		assertEquals(5f, t.pFloat, 0.1f);
		assertEquals(6d, t.pDouble, 0.1f);

		// uninitialized primitives
		assertEquals(false, t.puBoolean);
		assertEquals(0, t.puByte);
		assertEquals((char)0, t.puChar);
		assertEquals(0, t.puShort);
		assertEquals(0, t.puInt);
		assertEquals(0l, t.puLong);
		assertEquals(0f, t.puFloat, 0.1f);
		assertEquals(0d, t.puDouble, 0.1f);

		// primitive arrays
		assertEquals(false, t.paBoolean[1][0]);
		assertEquals(2, t.paByte[1][0]);
		assertEquals('b', t.paChar[1][0]);
		assertEquals(2, t.paShort[1][0]);
		assertEquals(2, t.paInt[1][0]);
		assertEquals(2l, t.paLong[1][0]);
		assertEquals(2f, t.paFloat[1][0], 0.1f);
		assertEquals(2d, t.paDouble[1][0], 0.1f);
		assertNull(t.paBoolean[2]);
		assertNull(t.paByte[2]);
		assertNull(t.paChar[2]);
		assertNull(t.paShort[2]);
		assertNull(t.paInt[2]);
		assertNull(t.paLong[2]);
		assertNull(t.paFloat[2]);
		assertNull(t.paDouble[2]);

		// uninitialized primitive arrays
		assertNull(t.pauBoolean);
		assertNull(t.pauByte);
		assertNull(t.pauChar);
		assertNull(t.pauShort);
		assertNull(t.pauInt);
		assertNull(t.pauLong);
		assertNull(t.pauFloat);
		assertNull(t.pauDouble);

		// anonymous list of primitive arrays
		assertEquals(true, t.palBoolean.get(0)[0]);
		assertEquals(1, t.palByte.get(0)[0]);
		assertEquals('a', t.palChar.get(0)[0]);
		assertEquals(1, t.palShort.get(0)[0]);
		assertEquals(1, t.palInt.get(0)[0]);
		assertEquals(1l, t.palLong.get(0)[0]);
		assertEquals(1f, t.palFloat.get(0)[0], 0.1f);
		assertEquals(1d, t.palDouble.get(0)[0], 0.1f);
		assertNull(t.palBoolean.get(1));
		assertNull(t.palByte.get(1));
		assertNull(t.palChar.get(1));
		assertNull(t.palShort.get(1));
		assertNull(t.palInt.get(1));
		assertNull(t.palLong.get(1));
		assertNull(t.palFloat.get(1));
		assertNull(t.palDouble.get(1));

		// regular list of primitive arrays
		assertEquals(true, t.plBoolean.get(0)[0]);
		assertEquals(1, t.plByte.get(0)[0]);
		assertEquals('a', t.plChar.get(0)[0]);
		assertEquals(1, t.plShort.get(0)[0]);
		assertEquals(1, t.plInt.get(0)[0]);
		assertEquals(1l, t.plLong.get(0)[0]);
		assertEquals(1f, t.plFloat.get(0)[0], 0.1f);
		assertEquals(1d, t.plDouble.get(0)[0], 0.1f);
		assertNull(t.plBoolean.get(1));
		assertNull(t.plByte.get(1));
		assertNull(t.plChar.get(1));
		assertNull(t.plShort.get(1));
		assertNull(t.plInt.get(1));
		assertNull(t.plLong.get(1));
		assertNull(t.plFloat.get(1));
		assertNull(t.plDouble.get(1));
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
			pLong = 4l;
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
			plBoolean = new ArrayList<boolean[]>() {{
				add(new boolean[]{true}); add(null);
			}};
			plByte = new ArrayList<byte[]>() {{
				add(new byte[]{1}); add(null);
			}};
			plChar = new ArrayList<char[]>() {{
				add(new char[]{'a'}); add(null);
			}};
			plShort = new ArrayList<short[]>() {{
				add(new short[]{1}); add(null);
			}};
			plInt = new ArrayList<int[]>() {{
				add(new int[]{1}); add(null);
			}};
			plLong = new ArrayList<long[]>() {{
				add(new long[]{1}); add(null);
			}};
			plFloat = new ArrayList<float[]>() {{
				add(new float[]{1}); add(null);
			}};
			plDouble = new ArrayList<double[]>() {{
				add(new double[]{1}); add(null);
			}};

			// Anonymous list of primitives
			palBoolean = new ArrayList<boolean[]>();
			palBoolean.add(new boolean[]{true});
			palBoolean.add(null);
			palByte = new ArrayList<byte[]>();
			palByte.add(new byte[]{1});
			palByte.add(null);
			palChar = new ArrayList<char[]>();
			palChar.add(new char[]{'a'});
			palChar.add(null);
			palShort = new ArrayList<short[]>();
			palShort.add(new short[]{1});
			palShort.add(null);
			palInt = new ArrayList<int[]>();
			palInt.add(new int[]{1});
			palInt.add(null);
			palLong = new ArrayList<long[]>();
			palLong.add(new long[]{1});
			palLong.add(null);
			palFloat = new ArrayList<float[]>();
			palFloat.add(new float[]{1});
			palFloat.add(null);
			palDouble = new ArrayList<double[]>();
			palDouble.add(new double[]{1});
			palDouble.add(null);
			return this;
		}
	}

	//====================================================================================================
	// List of PrimitivesBean
	//====================================================================================================
	@Test
	public void testPrimitivesBeanList() throws Exception {
		List<PrimitivesBean> t = new ArrayList<PrimitivesBean>() {{
			add(new PrimitivesBean().init());
			add(null);
			add(new PrimitivesBean().init());
		}};
		if (p == null)
			return;
		t = roundTrip(t, p.getBeanContext().getCollectionClassMeta(List.class, PrimitivesBean.class));

		PrimitivesBean t2 = t.get(2);

		// primitives
		assertEquals(true, t2.pBoolean);
		assertEquals(1, t2.pByte);
		assertEquals('a', t2.pChar);
		assertEquals(2, t2.pShort);
		assertEquals(3, t2.pInt);
		assertEquals(4l, t2.pLong);
		assertEquals(5f, t2.pFloat, 0.1f);
		assertEquals(6d, t2.pDouble, 0.1f);

		// uninitialized primitives
		assertEquals(false, t2.puBoolean);
		assertEquals(0, t2.puByte);
		assertEquals((char)0, t2.puChar);
		assertEquals(0, t2.puShort);
		assertEquals(0, t2.puInt);
		assertEquals(0l, t2.puLong);
		assertEquals(0f, t2.puFloat, 0.1f);
		assertEquals(0d, t2.puDouble, 0.1f);

		// primitive arrays
		assertEquals(false, t2.paBoolean[1][0]);
		assertEquals(2, t2.paByte[1][0]);
		assertEquals('b', t2.paChar[1][0]);
		assertEquals(2, t2.paShort[1][0]);
		assertEquals(2, t2.paInt[1][0]);
		assertEquals(2l, t2.paLong[1][0]);
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
		assertEquals(1l, t2.palLong.get(0)[0]);
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
		assertEquals(1l, t2.plLong.get(0)[0]);
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

		assertNull(t.get(1));
	}
}
