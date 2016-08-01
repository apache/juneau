/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.core.test.a.rttests;

import static com.ibm.juno.core.test.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.junit.*;

import com.ibm.juno.core.json.*;
import com.ibm.juno.core.parser.*;
import com.ibm.juno.core.serializer.*;

/**
 * Tests designed to serialize and parse objects to make sure we end up
 * with the same objects for all serializers and parsers.
 */
@SuppressWarnings({"hiding","serial"})
public class CT_RoundTripEnum extends RoundTripTest {

	public CT_RoundTripEnum(String label, WriterSerializer s, ReaderParser p, int flags) throws Exception {
		super(label, s, p, flags);
	}

	//====================================================================================================
	// Enum object
	//====================================================================================================
	@Test
	public void testEnumA() throws Exception {
		AEnum t = AEnum.FOO;
		assertObjectEquals("'FOO'", t);
		t = roundTrip(t, AEnum.class);
		assertEquals(AEnum.FOO, t);
	}

	@Test
	public void testEnumB() throws Exception {
		WriterSerializer s = new JsonSerializer.Simple().addFilters(getFilters());
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
		assertObjectEquals("['FOO','BAR',null]", t);
		t = roundTrip(t, AEnum[].class);
		assertEquals(AEnum.FOO, t[0]);
		assertEquals(AEnum.BAR, t[1]);
		assertNull(t[2]);
	}

	@Test
	public void testEnumArrayB() throws Exception {
		BEnum[] t = {BEnum.FOO,BEnum.BAR,null};
		assertObjectEquals("['xfoo','xbar',null]", t);
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
		assertObjectEquals("[['FOO','BAR',null],null]", t);
		t = roundTrip(t, AEnum[][].class);
		assertEquals(AEnum.FOO, t[0][0]);
		assertEquals(AEnum.BAR, t[0][1]);
		assertNull(t[0][2]);
		assertNull(t[1]);
	}

	@Test
	public void testEnum2dArrayB() throws Exception {
		BEnum[][] t = {{BEnum.FOO,BEnum.BAR,null},null};
		assertObjectEquals("[['xfoo','xbar',null],null]", t);
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
		assertEqualObjects(t1, t2);
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
		assertEqualObjects(t1, t2);
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
		public List<AEnum> f5 = new LinkedList<AEnum>();

		private List<AEnum> f6 = new LinkedList<AEnum>();
		public List<AEnum> getF6() {return f6;}
		public void setF6(List<AEnum> f6) {this.f6 = f6;}

		// Should have 'uniqueSet' attribute.
		public Set<AEnum> f7 = new HashSet<AEnum>();

		private Set<AEnum> f8 = new HashSet<AEnum>();
		public Set<AEnum> getF8() {return f8;}
		public void setF8(Set<AEnum> f8) {this.f8 = f8;}

		public Map<AEnum,AEnum> f9 = new LinkedHashMap<AEnum,AEnum>();

		public A init() {
			f1 = AEnum.FOO;
			f2 = AEnum.BAR;
			f3 = new AEnum[]{AEnum.FOO,null};
			f4 = new AEnum[][]{{AEnum.FOO,null},null};
			f5 = new ArrayList<AEnum>(){{add(AEnum.FOO);}};
			f6 = new ArrayList<AEnum>(){{add(AEnum.FOO);}};
			f7 = new HashSet<AEnum>(){{add(AEnum.FOO);}};
			f8 = new HashSet<AEnum>(){{add(AEnum.FOO);}};

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
		public List<BEnum> f5 = new LinkedList<BEnum>();

		private List<BEnum> f6 = new LinkedList<BEnum>();
		public List<BEnum> getF6() {return f6;}
		public void setF6(List<BEnum> f6) {this.f6 = f6;}

		// Should have 'uniqueSet' attribute.
		public Set<BEnum> f7 = new HashSet<BEnum>();

		private Set<BEnum> f8 = new HashSet<BEnum>();
		public Set<BEnum> getF8() {return f8;}
		public void setF8(Set<BEnum> f8) {this.f8 = f8;}

		public Map<BEnum,BEnum> f9 = new LinkedHashMap<BEnum,BEnum>();

		public B init() {
			f1 = BEnum.FOO;
			f2 = BEnum.BAR;
			f3 = new BEnum[]{BEnum.FOO,null};
			f4 = new BEnum[][]{{BEnum.FOO,null},null};
			f5 = new ArrayList<BEnum>(){{add(BEnum.FOO);}};
			f6 = new ArrayList<BEnum>(){{add(BEnum.FOO);}};
			f7 = new HashSet<BEnum>(){{add(BEnum.FOO);}};
			f8 = new HashSet<BEnum>(){{add(BEnum.FOO);}};

			return this;
		}
	}
}
