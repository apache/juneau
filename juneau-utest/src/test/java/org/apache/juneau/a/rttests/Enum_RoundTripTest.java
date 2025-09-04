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

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.common.internal.*;
import org.apache.juneau.json.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
class Enum_RoundTripTest extends RoundTripTest_Base {

	//====================================================================================================
	// Enum object
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a01_enumA(RoundTripTester t) throws Exception {
		var x = AEnum.FOO;
		assertJson("'FOO'", x);
		x = t.roundTrip(x, AEnum.class);
		assertEquals(AEnum.FOO, x);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a02_enumB(RoundTripTester t) throws Exception {
		var s = JsonSerializer.create().json5().build();
		var x = BEnum.FOO;
		assertEquals("'xfoo'", s.serialize(x));
		x = t.roundTrip(x, BEnum.class);
		assertEquals(BEnum.FOO, x);
	}

	//====================================================================================================
	// Enum[] object
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a03_enumArrayA(RoundTripTester t) throws Exception {
		var x = new AEnum[]{AEnum.FOO,AEnum.BAR,null};
		assertJson("['FOO','BAR',null]", x);
		x = t.roundTrip(x, AEnum[].class);
		assertEquals(AEnum.FOO, x[0]);
		assertEquals(AEnum.BAR, x[1]);
		assertNull(x[2]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a04_enumArrayB(RoundTripTester t) throws Exception {
		var x = new BEnum[]{BEnum.FOO,BEnum.BAR,null};
		assertJson("['xfoo','xbar',null]", x);
		x = t.roundTrip(x, BEnum[].class);
		assertEquals(BEnum.FOO, x[0]);
		assertEquals(BEnum.BAR, x[1]);
		assertNull(x[2]);
	}

	//====================================================================================================
	// Enum[][] object
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a05_enum2dArrayA(RoundTripTester t) throws Exception {
		var x = new AEnum[][]{{AEnum.FOO,AEnum.BAR,null},null};
		assertJson("[['FOO','BAR',null],null]", x);
		x = t.roundTrip(x, AEnum[][].class);
		assertEquals(AEnum.FOO, x[0][0]);
		assertEquals(AEnum.BAR, x[0][1]);
		assertNull(x[0][2]);
		assertNull(x[1]);
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a06_enum2dArrayB(RoundTripTester t) throws Exception {
		var x = new BEnum[][]{{BEnum.FOO,BEnum.BAR,null},null};
		assertJson("[['xfoo','xbar',null],null]", x);
		x = t.roundTrip(x, BEnum[][].class);
		assertEquals(BEnum.FOO, x[0][0]);
		assertEquals(BEnum.BAR, x[0][1]);
		assertNull(x[0][2]);
		assertNull(x[1]);
	}

	//====================================================================================================
	// Bean with Enum fields
	//====================================================================================================

	@ParameterizedTest
	@MethodSource("testers")
	void a07_beansWithEnumA(RoundTripTester t) throws Exception {
		var x1 = new A().init();
		var x2 = t.roundTrip(x1, A.class);
		assertEquals(json(x1), json(x2));
		assertBean(x2, "f3{0,1},f4{0{0,1},1}", "{FOO,null},{{FOO,null},null}");
	}

	@ParameterizedTest
	@MethodSource("testers")
	void a08_beansWithEnumB(RoundTripTester t) throws Exception {
		var x1 = new B().init();
		var x2 = t.roundTrip(x1, B.class);
		assertEquals(json(x1), json(x2));
		assertBean(x2, "f3{0,1},f4{0{0,1},1}", "{FOO,null},{{FOO,null},null}");
	}


	/** Normal Enum */
	public enum AEnum {
		FOO,BAR,BAZ
	}

	/** Enum with custom serialized values */
	public enum BEnum {
		FOO("xfoo"), BAR("xbar"), BAZ("xbaz");

		private String val;

		BEnum(String val) {
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
		public AEnum getF2() { return f2; }
		public void setF2(AEnum v) { f2 = v; }

		public AEnum[] f3;
		public AEnum[][] f4;

		// Should not have 'uniqueSet' attribute.
		public List<AEnum> f5 = new LinkedList<>();

		private List<AEnum> f6 = new LinkedList<>();
		public List<AEnum> getF6() { return f6; }
		public void setF6(List<AEnum> v) { f6 = v; }

		// Should have 'uniqueSet' attribute.
		public Set<AEnum> f7 = new HashSet<>();

		private Set<AEnum> f8 = new HashSet<>();
		public Set<AEnum> getF8() { return f8; }
		public void setF8(Set<AEnum> v) { f8 = v; }

		public Map<AEnum,AEnum> f9 = new LinkedHashMap<>();

		public A init() {
			f1 = AEnum.FOO;
			f2 = AEnum.BAR;
			f3 = new AEnum[]{AEnum.FOO,null};
			f4 = new AEnum[][]{{AEnum.FOO,null},null};
			f5 = alist(AEnum.FOO);
			f6 = alist(AEnum.FOO);
			f7 = Utils.set(AEnum.FOO);
			f8 = Utils.set(AEnum.FOO);

			return this;
		}
	}

	public static class B {

		// Should have 'enum' attribute.
		public BEnum f1;

		private BEnum f2;
		public BEnum getF2() { return f2; }
		public void setF2(BEnum v) { f2 = v; }

		public BEnum[] f3;
		public BEnum[][] f4;

		// Should not have 'uniqueSet' attribute.
		public List<BEnum> f5 = new LinkedList<>();

		private List<BEnum> f6 = new LinkedList<>();
		public List<BEnum> getF6() { return f6; }
		public void setF6(List<BEnum> v) { f6 = v; }

		// Should have 'uniqueSet' attribute.
		public Set<BEnum> f7 = new HashSet<>();

		private Set<BEnum> f8 = new HashSet<>();
		public Set<BEnum> getF8() { return f8; }
		public void setF8(Set<BEnum> v) { f8 = v; }

		public Map<BEnum,BEnum> f9 = new LinkedHashMap<>();

		public B init() {
			f1 = BEnum.FOO;
			f2 = BEnum.BAR;
			f3 = new BEnum[]{BEnum.FOO,null};
			f4 = new BEnum[][]{{BEnum.FOO,null},null};
			f5 = alist(BEnum.FOO);
			f6 = alist(BEnum.FOO);
			f7 = Utils.set(BEnum.FOO);
			f8 = Utils.set(BEnum.FOO);

			return this;
		}
	}
}