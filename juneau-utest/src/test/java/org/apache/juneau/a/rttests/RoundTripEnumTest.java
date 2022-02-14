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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.serializer.*;
import org.junit.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@FixMethodOrder(NAME_ASCENDING)
public class RoundTripEnumTest extends RoundTripTest {

	public RoundTripEnumTest(String label, Serializer.Builder s, Parser.Builder p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// Enum object
	//====================================================================================================
	@Test
	public void testEnumA() throws Exception {
		AEnum t = AEnum.FOO;
		assertObject(t).asJson().is("'FOO'");
		t = roundTrip(t, AEnum.class);
		assertEquals(AEnum.FOO, t);
	}

	@Test
	public void testEnumB() throws Exception {
		WriterSerializer s = JsonSerializer.create().ssq().swaps(getPojoSwaps()).build();
		BEnum t = BEnum.FOO;
		assertEquals("'xfoo'", s.serialize(t));
		t = roundTrip(t, BEnum.class);
		assertEquals(BEnum.FOO, t);
	}

	//====================================================================================================
	// Enum[] object
	//====================================================================================================
	@Test
	public void testEnumArrayA() throws Exception {
		AEnum[] t = {AEnum.FOO,AEnum.BAR,null};
		assertObject(t).asJson().is("['FOO','BAR',null]");
		t = roundTrip(t, AEnum[].class);
		assertEquals(AEnum.FOO, t[0]);
		assertEquals(AEnum.BAR, t[1]);
		assertNull(t[2]);
	}

	@Test
	public void testEnumArrayB() throws Exception {
		BEnum[] t = {BEnum.FOO,BEnum.BAR,null};
		assertObject(t).asJson().is("['xfoo','xbar',null]");
		t = roundTrip(t, BEnum[].class);
		assertEquals(BEnum.FOO, t[0]);
		assertEquals(BEnum.BAR, t[1]);
		assertNull(t[2]);
	}

	//====================================================================================================
	// Enum[][] object
	//====================================================================================================
	@Test
	public void testEnum2dArrayA() throws Exception {
		AEnum[][] t = {{AEnum.FOO,AEnum.BAR,null},null};
		assertObject(t).asJson().is("[['FOO','BAR',null],null]");
		t = roundTrip(t, AEnum[][].class);
		assertEquals(AEnum.FOO, t[0][0]);
		assertEquals(AEnum.BAR, t[0][1]);
		assertNull(t[0][2]);
		assertNull(t[1]);
	}

	@Test
	public void testEnum2dArrayB() throws Exception {
		BEnum[][] t = {{BEnum.FOO,BEnum.BAR,null},null};
		assertObject(t).asJson().is("[['xfoo','xbar',null],null]");
		t = roundTrip(t, BEnum[][].class);
		assertEquals(BEnum.FOO, t[0][0]);
		assertEquals(BEnum.BAR, t[0][1]);
		assertNull(t[0][2]);
		assertNull(t[1]);
	}

	//====================================================================================================
	// Bean with Enum fields
	//====================================================================================================
	@Test
	public void testBeansWithEnumA() throws Exception {
		A t1 = new A().init(), t2;
		t2 = roundTrip(t1, A.class);
		assertObject(t1).isSameJsonAs(t2);
		assertEquals(AEnum.FOO, t2.f3[0]);
		assertNull(t2.f3[1]);
		assertEquals(AEnum.FOO, t2.f4[0][0]);
		assertNull(t2.f4[0][1]);
		assertNull(t2.f4[1]);
	}

	@Test
	public void testBeansWithEnumB() throws Exception {
		B t1 = new B().init(), t2;
		t2 = roundTrip(t1, B.class);
		assertObject(t1).isSameJsonAs(t2);
		assertEquals(BEnum.FOO, t2.f3[0]);
		assertNull(t2.f3[1]);
		assertEquals(BEnum.FOO, t2.f4[0][0]);
		assertNull(t2.f4[0][1]);
		assertNull(t2.f4[1]);
	}


	/** Normal Enum */
	public enum AEnum {
		FOO,BAR,BAZ
	}

	/** Enum with custom serialized values */
	public enum BEnum {
		FOO("xfoo"), BAR("xbar"), BAZ("xbaz");

		private String val;

		private BEnum(String val) {
			this.val = val;
		}

		@Override /* Object */
		public String toString() {
			return val;
		}

		public static BEnum fromString(String val) {
			if (val.equals("xfoo"))
				return FOO;
			if (val.equals("xbar"))
				return BAR;
			if (val.equals("xbaz"))
				return BAZ;
			return null;
		}
	}

	public static class A {

		// Should have 'enum' attribute.
		public AEnum f1;

		private AEnum f2;
		public AEnum getF2() {return f2;}
		public void setF2(AEnum f2) {this.f2 = f2;}

		public AEnum[] f3;
		public AEnum[][] f4;

		// Should not have 'uniqueSet' attribute.
		public List<AEnum> f5 = new LinkedList<>();

		private List<AEnum> f6 = new LinkedList<>();
		public List<AEnum> getF6() {return f6;}
		public void setF6(List<AEnum> f6) {this.f6 = f6;}

		// Should have 'uniqueSet' attribute.
		public Set<AEnum> f7 = new HashSet<>();

		private Set<AEnum> f8 = new HashSet<>();
		public Set<AEnum> getF8() {return f8;}
		public void setF8(Set<AEnum> f8) {this.f8 = f8;}

		public Map<AEnum,AEnum> f9 = new LinkedHashMap<>();

		public A init() {
			f1 = AEnum.FOO;
			f2 = AEnum.BAR;
			f3 = new AEnum[]{AEnum.FOO,null};
			f4 = new AEnum[][]{{AEnum.FOO,null},null};
			f5 = list(AEnum.FOO);
			f6 = list(AEnum.FOO);
			f7 = set(AEnum.FOO);
			f8 = set(AEnum.FOO);

			return this;
		}
	}

	public static class B {

		// Should have 'enum' attribute.
		public BEnum f1;

		private BEnum f2;
		public BEnum getF2() {return f2;}
		public void setF2(BEnum f2) {this.f2 = f2;}

		public BEnum[] f3;
		public BEnum[][] f4;

		// Should not have 'uniqueSet' attribute.
		public List<BEnum> f5 = new LinkedList<>();

		private List<BEnum> f6 = new LinkedList<>();
		public List<BEnum> getF6() {return f6;}
		public void setF6(List<BEnum> f6) {this.f6 = f6;}

		// Should have 'uniqueSet' attribute.
		public Set<BEnum> f7 = new HashSet<>();

		private Set<BEnum> f8 = new HashSet<>();
		public Set<BEnum> getF8() {return f8;}
		public void setF8(Set<BEnum> f8) {this.f8 = f8;}

		public Map<BEnum,BEnum> f9 = new LinkedHashMap<>();

		public B init() {
			f1 = BEnum.FOO;
			f2 = BEnum.BAR;
			f3 = new BEnum[]{BEnum.FOO,null};
			f4 = new BEnum[][]{{BEnum.FOO,null},null};
			f5 = list(BEnum.FOO);
			f6 = list(BEnum.FOO);
			f7 = set(BEnum.FOO);
			f8 = set(BEnum.FOO);

			return this;
		}
	}
}
