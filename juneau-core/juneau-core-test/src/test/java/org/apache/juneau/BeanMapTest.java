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
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@SuppressWarnings({"unchecked","rawtypes","serial","javadoc"})
public class BeanMapTest {

	JsonSerializer serializer = JsonSerializer.DEFAULT_LAX;

	BeanContext bc = BeanContext.create()
			.beanDictionary(MyBeanDictionaryMap.class)
			.pojoSwaps(CalendarSwap.ISO8601DTZ.class)
			.build();
	BeanSession session = bc.createSession();

	public static class MyBeanDictionaryMap extends BeanDictionaryMap {
		public MyBeanDictionaryMap() {
			addClass("StringArray", String[].class);
			addClass("String2dArray", String[][].class);
			addClass("IntArray", int[].class);
			addClass("Int2dArray", int[][].class);
			addClass("S", S.class);
			addClass("R1", R1.class);
			addClass("R2", R2.class);
			addClass("LinkedList", LinkedList.class);
			addClass("TreeMap", TreeMap.class);
			addCollectionClass("LinkedListOfInts", LinkedList.class, Integer.class);
			addCollectionClass("LinkedListOfR1", LinkedList.class, R1.class);
			addCollectionClass("LinkedListOfCalendar", LinkedList.class, Calendar.class);
		}
	}

	//====================================================================================================
	// Primitive field properties
	//====================================================================================================
	@Test
	public void testPrimitiveFieldProperties() {
		A t = new A();
		Map m = session.toBeanMap(t);

		// Make sure setting primitive values to null causes them to get default values.
		m.put("i1", null);
		m.put("s1", null);
		m.put("l1", null);
		m.put("d1", null);
		m.put("f1", null);
		m.put("b1", null);
		assertEquals(new Integer(0), m.get("i1"));
		assertEquals(new Short((short)0), m.get("s1"));
		assertEquals(new Long(0l), m.get("l1"));
		assertEquals(new Double(0d), m.get("d1"));
		assertEquals(new Float(0f), m.get("f1"));
		assertEquals(new Boolean(false), m.get("b1"));

		// Make sure setting non-primitive values to null causes them to set to null.
		m.put("i2", null);
		m.put("s2", null);
		m.put("l2", null);
		m.put("d2", null);
		m.put("f2", null);
		m.put("b2", null);
		assertNull(m.get("i2"));
		assertNull(m.get("s2"));
		assertNull(m.get("l2"));
		assertNull(m.get("d2"));
		assertNull(m.get("f2"));
		assertNull(m.get("b2"));

		// Make sure setting them all to an integer is kosher.
		m.put("i1", 1);
		m.put("s1", 1);
		m.put("l1", 1);
		m.put("d1", 1);
		m.put("f1", 1);
		m.put("i2", 1);
		m.put("s2", 1);
		m.put("l2", 1);
		m.put("d2", 1);
		m.put("f2", 1);
		assertEquals(new Integer(1), m.get("i1"));
		assertEquals(new Integer(1), m.get("i2"));
		assertEquals(new Short((short)1), m.get("s1"));
		assertEquals(new Short((short)1), m.get("s2"));
		assertEquals(new Long(1), m.get("l1"));
		assertEquals(new Long(1), m.get("l2"));
		assertEquals(new Double(1), m.get("d1"));
		assertEquals(new Double(1), m.get("d2"));
		assertEquals(new Float(1), m.get("f1"));
		assertEquals(new Float(1), m.get("f2"));

		m.put("b1", true);
		m.put("b2", new Boolean(true));
		assertEquals(new Boolean(true), m.get("b1"));
		assertEquals(new Boolean(true), m.get("b2"));
	}

	public static class A {
		public int i1;
		public Integer i2;
		public short s1;
		public Short s2;
		public long l1;
		public Long l2;
		public double d1;
		public Double d2;
		public float f1;
		public Float f2;
		public boolean b1;
		public Boolean b2;
	}

	//====================================================================================================
	// Primitive method properties
	//====================================================================================================
	@Test
	public void testPrimitiveMethodProperties() {
		B t = new B();
		Map m = session.toBeanMap(t);

		// Make sure setting primitive values to null causes them to get default values.
		m.put("i1", null);
		m.put("s1", null);
		m.put("l1", null);
		m.put("d1", null);
		m.put("f1", null);
		m.put("b1", null);
		assertEquals(new Integer(0), m.get("i1"));
		assertEquals(new Short((short)0), m.get("s1"));
		assertEquals(new Long(0l), m.get("l1"));
		assertEquals(new Double(0d), m.get("d1"));
		assertEquals(new Float(0f), m.get("f1"));
		assertEquals(new Boolean(false), m.get("b1"));

		// Make sure setting non-primitive values to null causes them to set to null.
		m.put("i2", null);
		m.put("s2", null);
		m.put("l2", null);
		m.put("d2", null);
		m.put("f2", null);
		m.put("b2", null);
		assertNull(m.get("i2"));
		assertNull(m.get("s2"));
		assertNull(m.get("l2"));
		assertNull(m.get("d2"));
		assertNull(m.get("f2"));
		assertNull(m.get("b2"));

		// Make sure setting them all to an integer is kosher.
		m.put("i1", 1);
		m.put("s1", 1);
		m.put("l1", 1);
		m.put("d1", 1);
		m.put("f1", 1);
		m.put("i2", 1);
		m.put("s2", 1);
		m.put("l2", 1);
		m.put("d2", 1);
		m.put("f2", 1);
		assertEquals(new Integer(1), m.get("i1"));
		assertEquals(new Integer(1), m.get("i2"));
		assertEquals(new Short((short)1), m.get("s1"));
		assertEquals(new Short((short)1), m.get("s2"));
		assertEquals(new Long(1), m.get("l1"));
		assertEquals(new Long(1), m.get("l2"));
		assertEquals(new Double(1), m.get("d1"));
		assertEquals(new Double(1), m.get("d2"));
		assertEquals(new Float(1), m.get("f1"));
		assertEquals(new Float(1), m.get("f2"));

		m.put("b1", true);
		m.put("b2", true);
		assertEquals(new Boolean(true), m.get("b1"));
		assertEquals(new Boolean(true), m.get("b2"));
	}

	public static class B {
		private int i1;
		private Integer i2;
		private short s1;
		private Short s2;
		private long l1;
		private Long l2;
		private double d1;
		private Double d2;
		private float f1;
		private Float f2;
		private boolean b1;
		private Boolean b2;

		public int getI1() {return i1;}
		public void setI1(int i1) {this.i1 = i1;}
		public Integer getI2() {return i2;}
		public void setI2(Integer i2) {this.i2 = i2;}

		public short getS1() {return s1;}
		public void setS1(short s1) {this.s1 = s1;}
		public Short getS2() {return s2;}
		public void setS2(Short s2) {this.s2 = s2;}

		public long getL1() {return l1;}
		public void setL1(long l1) {this.l1 = l1;}
		public Long getL2() {return l2;}
		public void setL2(Long l2) {this.l2 = l2;}

		public double getD1() {return d1;}
		public void setD1(double d1) {this.d1 = d1;}
		public Double getD2() {return d2;}
		public void setD2(Double d2) {this.d2 = d2;}

		public float getF1() {return f1;}
		public void setF1(float f1) {this.f1 = f1;}
		public Float getF2() {return f2;}
		public void setF2(Float f2) {this.f2 = f2;}

		public boolean getB1() {return b1;}
		public void setB1(boolean b1) {this.b1 = b1;}
		public Boolean getB2() {return b2;}
		public void setB2(Boolean b2) {this.b2 = b2;}
	}

	//====================================================================================================
	// testCollectionFieldProperties
	//====================================================================================================
	@Test
	public void testCollectionFieldProperties() throws Exception {
		C t = new C();
		Map m = session.toBeanMap(t);

		// Non-initialized list fields.
		m.put("l1", new ObjectList("[1,2,3]"));
		m.put("al1", new ObjectList("[1,2,3]"));
		m.put("ll1", new ObjectList("[1,2,3]"));
		m.put("c1", new ObjectList("[1,2,3]"));
		m.put("jl1", new ObjectList("[1,2,3]"));

		// al1 should be initialized with an ArrayList, since it's not a superclass of ObjectList.
		assertEquals(ArrayList.class.getName(), m.get("al1").getClass().getName());

		// The rest are proper superclasses of ObjectList.
		assertEquals(ObjectList.class.getName(), m.get("l1").getClass().getName());
		assertEquals(LinkedList.class.getName(), m.get("ll1").getClass().getName());
		assertEquals(ObjectList.class.getName(), m.get("c1").getClass().getName());
		assertEquals(ObjectList.class.getName(), m.get("jl1").getClass().getName());

		// Non-initialized map fields.
		m.put("m1", new ObjectMap("{foo:'bar'}"));
		m.put("hm1", new ObjectMap("{foo:'bar'}"));
		m.put("jm1", new ObjectMap("{foo:'bar'}"));
		m.put("tm1", new ObjectMap("{foo:'bar'}"));

		// tm1 should be initialized with TreeMap, since it's not a superclass of ObjectMap.
		assertEquals(TreeMap.class.getName(), m.get("tm1").getClass().getName());

		// The rest are propert superclasses of ObjectMap
		assertEquals(ObjectMap.class.getName(), m.get("m1").getClass().getName());
		assertEquals(HashMap.class.getName(), m.get("hm1").getClass().getName());
		assertEquals(ObjectMap.class.getName(), m.get("jm1").getClass().getName());

		// Initialized fields should reuse existing field value.
		m.put("l2", new ObjectList("[1,2,3]"));
		m.put("al2", new ObjectList("[1,2,3]"));
		m.put("ll2", new ObjectList("[1,2,3]"));
		m.put("c2", new ObjectList("[1,2,3]"));
		m.put("m2", new ObjectMap("{foo:'bar'}"));
		m.put("hm2", new ObjectMap("{foo:'bar'}"));
		m.put("tm2", new ObjectMap("{foo:'bar'}"));
		m.put("jm2", new ObjectMap("{foo:'bar'}"));
		m.put("jl2", new ObjectList("[1,2,3]"));

		assertEquals(ArrayList.class.getName(), m.get("l2").getClass().getName());
		assertEquals(ArrayList.class.getName(), m.get("al2").getClass().getName());
		assertEquals(LinkedList.class.getName(), m.get("ll2").getClass().getName());
		assertEquals(ArrayList.class.getName(), m.get("c2").getClass().getName());
		assertEquals(HashMap.class.getName(), m.get("m2").getClass().getName());
		assertEquals(HashMap.class.getName(), m.get("hm2").getClass().getName());
		assertEquals(TreeMap.class.getName(), m.get("tm2").getClass().getName());
		assertEquals(ObjectMap.class.getName(), m.get("jm2").getClass().getName());
		assertEquals(ObjectList.class.getName(), m.get("jl2").getClass().getName());
	}

	public static class C {
		public List l1;
		public ArrayList al1;
		public LinkedList ll1;
		public Collection c1;
		public Map m1;
		public HashMap hm1;
		public ObjectMap jm1;
		public TreeMap tm1;
		public ObjectList jl1;
		public List l2 = new ArrayList();
		public ArrayList al2 = new ArrayList();
		public LinkedList ll2 = new LinkedList();
		public Collection c2 = new ArrayList();
		public Map m2 = new HashMap();
		public HashMap hm2 = new HashMap();
		public TreeMap tm2 = new TreeMap();
		public ObjectMap jm2 = new ObjectMap();
		public ObjectList jl2 = new ObjectList();
	}

	//====================================================================================================
	// testCollectionMethodProperties
	//====================================================================================================
	@Test
	public void testCollectionMethodProperties() throws Exception {
		D t = new D();
		Map m = session.toBeanMap(t);

		// Non-initialized list fields.
		m.put("l1", new ObjectList("[1,2,3]"));
		m.put("al1", new ObjectList("[1,2,3]"));
		m.put("ll1", new ObjectList("[1,2,3]"));
		m.put("c1", new ObjectList("[1,2,3]"));
		m.put("jl1", new ObjectList("[1,2,3]"));

		// al1 should be initialized with an ArrayList, since it's not a superclass of ObjectList.
		assertEquals(ArrayList.class.getName(), m.get("al1").getClass().getName());

		// The rest are proper superclasses of ObjectList.
		assertEquals(ObjectList.class.getName(), m.get("l1").getClass().getName());
		assertEquals(LinkedList.class.getName(), m.get("ll1").getClass().getName());
		assertEquals(ObjectList.class.getName(), m.get("c1").getClass().getName());
		assertEquals(ObjectList.class.getName(), m.get("jl1").getClass().getName());

		// Non-initialized map fields.
		m.put("m1", new ObjectMap("{foo:'bar'}"));
		m.put("hm1", new ObjectMap("{foo:'bar'}"));
		m.put("jm1", new ObjectMap("{foo:'bar'}"));
		m.put("tm1", new ObjectMap("{foo:'bar'}"));

		// tm1 should be initialized with TreeMap, since it's not a superclass of ObjectMap.
		assertEquals(TreeMap.class.getName(), m.get("tm1").getClass().getName());

		// The rest are propert superclasses of ObjectMap
		assertEquals(ObjectMap.class.getName(), m.get("m1").getClass().getName());
		assertEquals(HashMap.class.getName(), m.get("hm1").getClass().getName());
		assertEquals(ObjectMap.class.getName(), m.get("jm1").getClass().getName());

		// Initialized fields should reuse existing field value.
		m.put("l2", new ObjectList("[1,2,3]"));
		m.put("al2", new ObjectList("[1,2,3]"));
		m.put("ll2", new ObjectList("[1,2,3]"));
		m.put("c2", new ObjectList("[1,2,3]"));
		m.put("m2", new ObjectMap("{foo:'bar'}"));
		m.put("hm2", new ObjectMap("{foo:'bar'}"));
		m.put("tm2", new ObjectMap("{foo:'bar'}"));
		m.put("jm2", new ObjectMap("{foo:'bar'}"));
		m.put("jl2", new ObjectList("[1,2,3]"));

		assertEquals(ArrayList.class.getName(), m.get("l2").getClass().getName());
		assertEquals(ArrayList.class.getName(), m.get("al2").getClass().getName());
		assertEquals(LinkedList.class.getName(), m.get("ll2").getClass().getName());
		assertEquals(ArrayList.class.getName(), m.get("c2").getClass().getName());
		assertEquals(HashMap.class.getName(), m.get("m2").getClass().getName());
		assertEquals(HashMap.class.getName(), m.get("hm2").getClass().getName());
		assertEquals(TreeMap.class.getName(), m.get("tm2").getClass().getName());
		assertEquals(ObjectMap.class.getName(), m.get("jm2").getClass().getName());
		assertEquals(ObjectList.class.getName(), m.get("jl2").getClass().getName());
	}

	public static class D {
		private List l1;
		public List getL1() {return l1;}
		public void setL1(List l1) {this.l1 = l1;}

		private ArrayList al1;
		public ArrayList getAl1() {return al1;}
		public void setAl1(ArrayList al1) {this.al1 = al1;}

		private LinkedList ll1;
		public LinkedList getLl1() {return ll1;}
		public void setLl1(LinkedList ll1) {this.ll1 = ll1;}

		private Collection c1;
		public Collection getC1() {return c1;}
		public void setC1(Collection c1) {this.c1 = c1;}

		private Map m1;
		public Map getM1() {return m1;}
		public void setM1(Map m1) {this.m1 = m1;}

		private HashMap hm1;
		public HashMap getHm1() {return hm1;}
		public void setHm1(HashMap hm1) {this.hm1 = hm1;}

		private ObjectMap jm1;
		public ObjectMap getJm1() {return jm1;}
		public void setJm1(ObjectMap jm1) {this.jm1 = jm1;}

		private TreeMap tm1;
		public TreeMap getTm1() {return tm1;}
		public void setTm1(TreeMap tm1) {this.tm1 = tm1;}

		private ObjectList jl1;
		public ObjectList getJl1() {return jl1;}
		public void setJl1(ObjectList jl1) {this.jl1 = jl1;}

		private List l2 = new ArrayList();
		public List getL2() {return l2;}
		public void setL2(List l2) {this.l2 = l2;}

		private ArrayList al2 = new ArrayList();
		public ArrayList getAl2() {return al2;}
		public void setAl2(ArrayList al2) {this.al2 = al2;}

		private LinkedList ll2 = new LinkedList();
		public LinkedList getLl2() {return ll2;}
		public void setLl2(LinkedList ll2) {this.ll2 = ll2;}

		private Collection c2 = new ArrayList();
		public Collection getC2() {return c2;}
		public void setC2(Collection c2) {this.c2 = c2;}

		private Map m2 = new HashMap();
		public Map getM2() {return m2;}
		public void setM2(Map m2) {this.m2 = m2;}

		private HashMap hm2 = new HashMap();
		public HashMap getHm2() {return hm2;}
		public void setHm2(HashMap hm2) {this.hm2 = hm2;}

		private TreeMap tm2 = new TreeMap();
		public TreeMap getTm2() {return tm2;}
		public void setTm2(TreeMap tm2) {this.tm2 = tm2;}

		private ObjectMap jm2 = new ObjectMap();
		public ObjectMap getJm2() {return jm2;}
		public void setJm2(ObjectMap jm2) {this.jm2 = jm2;}

		private ObjectList jl2 = new ObjectList();
		public ObjectList getJl2() {return jl2;}
		public void setJl2(ObjectList jl2) {this.jl2 = jl2;}
	}

	//====================================================================================================
	// testArrayProperties
	//====================================================================================================
	@Test
	public void testArrayProperties() throws Exception {
		D1 t = new D1();
		Map m = session.toBeanMap(t);
		m.put("b", new ObjectMap("{s:'foo'}"));
		assertNotNull(t.b);
		assertEquals("foo", t.b.s);

		Map m2 = new TreeMap();
		m2.put("s", "bar");
		m.put("b", m2);
		assertNotNull(t.b);
		assertEquals("bar", t.b.s);

		m.put("b", new D2());
		assertEquals("default", t.b.s);

		JsonParser p = JsonParser.create().beanDictionary(D2.class).build();
		m.put("lb1", new ObjectList("[{_type:'D2',s:'foobar'}]", p));
		assertEquals(ObjectList.class.getName(), t.lb1.getClass().getName());
		assertEquals(D2.class.getName(), t.lb1.get(0).getClass().getName());
		assertEquals("foobar", (t.lb1.get(0)).s);

		m.put("lb2", new ObjectList("[{_type:'D2',s:'foobar'}]", p));
		assertEquals(ArrayList.class.getName(), t.lb2.getClass().getName());
		assertEquals(D2.class.getName(), t.lb2.get(0).getClass().getName());
		assertEquals("foobar", (t.lb2.get(0)).s);

		m.put("ab1", new ObjectList("[{_type:'D2',s:'foobar'}]", p));
		assertEquals("[L"+D2.class.getName()+";", t.ab1.getClass().getName());
		assertEquals(D2.class.getName(), t.ab1[0].getClass().getName());
		assertEquals("foobar", t.ab1[0].s);

		m.put("ab2", new ObjectList("[{_type:'D2',s:'foobar'}]", p));
		assertEquals("[L"+D2.class.getName()+";", t.ab2.getClass().getName());
		assertEquals(D2.class.getName(), t.ab2[0].getClass().getName());
		assertEquals("foobar", t.ab2[0].s);
	}

	public static class D1 {
		public D2 b;
		public List<D2> lb1;
		public List<D2> lb2 = new ArrayList<D2>();
		public D2[] ab1;
		public D2[] ab2 = new D2[0];
	}

	@Bean(typeName="D2")
	public static class D2 {
		public String s = "default";
	}

	//====================================================================================================
	// testArrayPropertiesInObjectList
	//====================================================================================================
	@Test
	public void testArrayPropertiesInObjectList() throws Exception {
		E t = new E();
		Map m = session.toBeanMap(t);
		m.put("s", new ObjectList("['foo']"));
		m.put("s2", new ObjectList("[['foo']]"));
		m.put("i", new ObjectList("[1,2,3]"));
		m.put("i2", new ObjectList("[[1,2,3],[4,5,6]]"));
		assertEquals("{s:['foo'],s2:[['foo']],i:[1,2,3],i2:[[1,2,3],[4,5,6]]}", JsonSerializer.DEFAULT_LAX.serialize(t));
		m.put("i", new ObjectList("[null,null,null]"));
		m.put("i2", new ObjectList("[[null,null,null],[null,null,null]]"));
		assertEquals("{s:['foo'],s2:[['foo']],i:[0,0,0],i2:[[0,0,0],[0,0,0]]}", JsonSerializer.DEFAULT_LAX.serialize(t));
	}

	public static class E {
		public String[] s;
		public String[][] s2;
		public int[] i;
		public int[][] i2;
	}

	//====================================================================================================
	// BeanMap.invokeMethod()
	//====================================================================================================
	@Test
	public void testInvokeMethod() throws Exception {
		F t5 = new F();
		ReaderParser p = JsonParser.DEFAULT;
		BeanMap m = session.toBeanMap(t5);
		new PojoIntrospector(t5, p).invokeMethod("doSetAProperty(java.lang.String)", "['baz']");
		assertEquals("baz", m.get("prop"));
	}

	public static class F {
		public String prop;

		public boolean doSetAProperty(String prop) {
			this.prop = prop;
			return true;
		}
	}

	//====================================================================================================
	// @BeanProperty tests
	//====================================================================================================
	@Test
	public void testBeanPropertyAnnotation() throws Exception {
		G1 t6 = new G1();
		BeanMap m = session.toBeanMap(t6);

		try {
			m.put("l1", "[{a:'a',i:1}]");
			throw new Exception("Expected exception on unsettable field.");
		} catch (Exception e) {
			// Good.
		}

		m.put("l2", "[{a:'a',i:1}]");
		assertEquals("java.util.LinkedList", m.get("l2").getClass().getName());
		assertEquals("org.apache.juneau.BeanMapTest$G", ((List)m.get("l2")).get(0).getClass().getName());

		m.put("l3", "[{a:'a',i:1}]");
		assertEquals("org.apache.juneau.ObjectList", m.get("l3").getClass().getName());
		assertEquals("org.apache.juneau.BeanMapTest$G", ((List)m.get("l3")).get(0).getClass().getName());

		m.put("l4", "[{a:'a',i:1}]");
		assertEquals("java.util.LinkedList", m.get("l4").getClass().getName());
		assertEquals("org.apache.juneau.BeanMapTest$G", ((List)m.get("l4")).get(0).getClass().getName());

		try {
			m.put("m1", "[{a:'a',i:1}]");
			throw new Exception("Expected exception on unsettable field.");
		} catch (Exception e) {
			// Good.
		}

		m.put("m2", "[{a:'a',i:1}]");
		assertEquals("java.util.LinkedList", m.get("m2").getClass().getName());
		assertEquals("org.apache.juneau.BeanMapTest$G", ((List)m.get("m2")).get(0).getClass().getName());

		m.put("m3", "[{a:'a',i:1}]");
		assertEquals("org.apache.juneau.ObjectList", m.get("m3").getClass().getName());
		assertEquals("org.apache.juneau.BeanMapTest$G", ((List)m.get("m3")).get(0).getClass().getName());

		m.put("m4", "[{a:'a',i:1}]");
		assertEquals("java.util.LinkedList", m.get("m4").getClass().getName());
		assertEquals("org.apache.juneau.BeanMapTest$G", ((List)m.get("m4")).get(0).getClass().getName());
	}

	public static class G {
		public String a;
		public int i;
	}

	public static class G1 {

		public List<G> l1;

		public List<G> l2 = new LinkedList<G>();

		@BeanProperty(type=List.class,params={G.class})
		public List<G> l3;

		@BeanProperty(type=LinkedList.class,params={G.class})
		public List<G> l4;

		private List<G> m1;
		public List<G> getM1() { return m1; }
		public void setM1(List<G> m1) { this.m1 = m1; }

		private List<G> m2 = new LinkedList<G>();
		public List<G> getM2() { return m2; }
		public void setM2(List<G> m2) { this.m2 = m2; }

		private List<G> m3;
		@BeanProperty(type=List.class,params={G.class})
		public List<G> getM3() { return m3; }
		public void setM3(List<G> m3) { this.m3 = m3; }

		private List<G> m4;
		@BeanProperty(type=LinkedList.class,params={G.class})
		public List<G> getM4() { return m4; }
		public void setM4(List<G> m4) { this.m4 = m4; }
	}

	//====================================================================================================
	// Enum tests
	//====================================================================================================
	@Test
	public void testEnum() throws Exception {

		// Initialize existing bean.
		H t7 = new H();
		BeanMap m = session.toBeanMap(t7);
		m.put("enum1", "ONE");
		m.put("enum2", "TWO");
		assertEquals("{_type:'H',enum1:'ONE',enum2:'TWO'}", serializer.serialize(t7));
		assertEquals(HEnum.ONE, t7.enum1);
		assertEquals(HEnum.TWO, t7.getEnum2());

		// Use BeanContext to create bean instance.
		m = BeanContext.DEFAULT.createSession().newBeanMap(H.class).load("{enum1:'TWO',enum2:'THREE'}");
		assertEquals("{_type:'H',enum1:'TWO',enum2:'THREE'}", serializer.serialize(m.getBean()));
		t7 = (H)m.getBean();
		assertEquals(HEnum.TWO, t7.enum1);
		assertEquals(HEnum.THREE, t7.getEnum2());

		// Create instance directly from JSON.
		JsonParser p = JsonParser.create().beanDictionary(H.class).build();
		t7 = (H)p.parse("{_type:'H',enum1:'THREE',enum2:'ONE'}", Object.class);
		assertEquals("{_type:'H',enum1:'THREE',enum2:'ONE'}", serializer.serialize(t7));
		assertEquals(HEnum.THREE, t7.enum1);
		assertEquals(HEnum.ONE, t7.getEnum2());
	}

	public static enum HEnum {
		ONE, TWO, THREE
	}

	@Bean(typeName="H")
	public static class H {

		public HEnum enum1;

		private HEnum enum2;

		public HEnum getEnum2() {
			return enum2;
		}

		public void setEnum2(HEnum enum2) {
			this.enum2 = enum2;
		}
	}

	//====================================================================================================
	// Automatic detection of generic types
	//====================================================================================================
	@Test
	public void testAutomaticDetectionOfGenericTypes() throws Exception {
		BeanMap bm = BeanContext.DEFAULT.createSession().newBeanMap(I.class);
		assertEquals(String.class, bm.getProperty("p1").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(Integer.class, bm.getProperty("p2").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(Object.class, bm.getProperty("p3").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(String.class, bm.getProperty("p4").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Integer.class, bm.getProperty("p4").getMeta().getClassMeta().getValueType().getInnerClass());
		assertEquals(String.class, bm.getProperty("p5").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Integer.class, bm.getProperty("p5").getMeta().getClassMeta().getValueType().getInnerClass());
		assertEquals(Object.class, bm.getProperty("p6").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Object.class, bm.getProperty("p6").getMeta().getClassMeta().getValueType().getInnerClass());
	}

	public static class I {

		public List<String> p1;

		public List<Integer> getP2() {
			return null;
		}

		public List<? extends Integer> p3;

		public Map<String,Integer> p4;

		public Map<String,Integer> getP5() {
			return null;
		}

		public Map<String,? extends Integer> p6;
	}

	//====================================================================================================
	// Overriding detection of generic types.
	//====================================================================================================
	@Test
	public void testOverridingDetectionOfGenericTypes() throws Exception {
		BeanMap bm = BeanContext.DEFAULT.createSession().newBeanMap(J.class);
		assertEquals(Float.class, bm.getProperty("p1").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p2").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p3").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(Object.class, bm.getProperty("p4").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p4").getMeta().getClassMeta().getValueType().getInnerClass());
		assertEquals(Object.class, bm.getProperty("p5").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p5").getMeta().getClassMeta().getValueType().getInnerClass());
		assertEquals(String.class, bm.getProperty("p6").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p6").getMeta().getClassMeta().getValueType().getInnerClass());
	}

	public static class J {

		@BeanProperty(params={Float.class})
		public List<String> p1;

		@BeanProperty(params={Float.class})
		public List<Integer> getP2() {
			return null;
		}

		@BeanProperty(params={Float.class})
		public List<? extends Integer> p3;

		@BeanProperty(params={Object.class, Float.class})
		public Map<String,Integer> p4;

		@BeanProperty(params={Object.class, Float.class})
		public Map<String,Integer> getP5() {
			return null;
		}

		@BeanProperty(params={String.class, Float.class})
		public Map<String,? extends Integer> p6;
	}

	//====================================================================================================
	// Overriding detection of generic types.
	//====================================================================================================
	@Test
	public void testOverridingDetectionOfGenericTypes2() throws Exception {
		BeanMap bm = session.newBeanMap(K.class);
		assertEquals(Float.class, bm.getProperty("p1").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p2").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p3").getMeta().getClassMeta().getElementType().getInnerClass());
		assertEquals(String.class, bm.getProperty("p4").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p4").getMeta().getClassMeta().getValueType().getInnerClass());
		assertEquals(String.class, bm.getProperty("p5").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p5").getMeta().getClassMeta().getValueType().getInnerClass());
		assertEquals(String.class, bm.getProperty("p6").getMeta().getClassMeta().getKeyType().getInnerClass());
		assertEquals(Float.class, bm.getProperty("p6").getMeta().getClassMeta().getValueType().getInnerClass());
	}

	public static class K {

		@BeanProperty(params=Float.class)
		public List<String> p1;

		@BeanProperty(params=Float.class)
		public List<Integer> getP2() {
			return null;
		}

		@BeanProperty(params=Float.class)
		public List<? extends Integer> p3;

		@BeanProperty(params={String.class,Float.class})
		public Map<String,Integer> p4;

		@BeanProperty(params={String.class,Float.class})
		public Map<String,Integer> getP5() {
			return null;
		}

		@BeanProperty(params={String.class,Float.class})
		public Map<String,? extends Integer> p6;
	}

	//====================================================================================================
	// List<E> subclass properties
	//====================================================================================================
	@Test
	public void testGenericListSubclass() throws Exception {
		BeanMap<L> bm = session.newBeanMap(L.class);
		bm.put("list", "[{name:'1',value:'1'},{name:'2',value:'2'}]");
		L b = bm.getBean();
		assertEquals("1", b.list.get(0).name);
	}

	public static class L {
		public L1 list;
	}

	public static class L1 extends LinkedList<L2> {
	}

	public static class L2 {
		public String name, value;
		public L2(){}
		public L2(String name, String value) {
			this.name = name;
			this.value = value;
		}
	}

	//====================================================================================================
	// Generic fields.
	//====================================================================================================
	@Test
	public void testGenericFields() throws Exception {

		M2 t1 = new M2();
		BeanMap<M2> bm = session.toBeanMap(t1);
		assertEquals(1, bm.get("x"));

		M3 t2 = new M3();
		BeanMap<M3> cm = session.toBeanMap(t2);
		assertEquals(2, cm.get("x"));

		M4 t3 = new M4();
		BeanMap<M4> dm = session.toBeanMap(t3);
		assertEquals(3, dm.get("x"));

		M5 t4 = new M5();
		BeanMap<M5> em = session.toBeanMap(t4);
		assertEquals(4, em.get("x"));
	}

	public static class M1<T> {
		public T x;
	}

	public static class M2 extends M1<Integer> {
		public M2() {
			this.x = 1;
		}
	}

	public static class M3 extends M2 {
		public M3() {
			this.x = 2;
		}
	}

	public static class M4<T extends Number> extends M1<T> {
		public M4() {
			this.x = (T)new Integer(3);
		}
	}

	public static class M5 extends M4<Integer> {
		public M5() {
			this.x = new Integer(4);
		}
	}

	//====================================================================================================
	// Generic methods
	//====================================================================================================
	@Test
	public void testGenericMethods() throws Exception {

		N2 t1 = new N2();
		BeanMap<N2> bm = session.toBeanMap(t1);
		assertEquals(1, bm.get("x"));

		N3 t2 = new N3();
		BeanMap<N3> cm = session.toBeanMap(t2);
		assertEquals(2, cm.get("x"));

		N4 t3 = new N4();
		BeanMap<N4> dm = session.toBeanMap(t3);
		assertEquals(3, dm.get("x"));

		N5 t4 = new N5();
		BeanMap<N5> em = session.toBeanMap(t4);
		assertEquals(4, em.get("x"));
	}

	public static class N1<T> {
		private T x;
		public void setX(T x) {
			this.x = x;
		}
		public T getX() {
			return x;
		}
	}

	public static class N2 extends N1<Integer> {
		public N2() {
			setX(1);
		}
	}

	public static class N3 extends N2 {
		public N3() {
			setX(2);
		}
	}

	public static class N4<T extends Number> extends N1<T> {
		public N4() {
			setX((T)new Integer(3));
		}
	}

	public static class N5 extends N4<Integer> {
		public N5() {
			setX(4);
		}
	}

	//====================================================================================================
	// Test ignoreUnknownBeanProperties setting
	//====================================================================================================
	@Test
	public void testIgnoreUnknownBeanPropertiesSetting() throws Exception {
		ReaderParser p = null;
		O t;

		// JSON
		String json = "{baz:789,foo:123,bar:456}";
		p = JsonParser.create().ignoreUnknownBeanProperties(true).build();
		t = p.parse(json, O.class);
		assertEquals(123, t.foo);

		try {
			p = JsonParser.DEFAULT;
			t = p.parse(json, O.class);
			fail("Expected exception never occurred");
		} catch (Exception e) {
			// Good.
		}

		// XML
		String xml = "<object><baz type='number'>789</baz><foo type='number'>123</foo><bar type='number'>456</bar></object>";
		p = XmlParser.create().ignoreUnknownBeanProperties(true).build();
		t = p.parse(xml, O.class);
		assertEquals(123, t.foo);

		try {
			p = XmlParser.DEFAULT;
			t = p.parse(json, O.class);
			fail("Expected exception never occurred");
		} catch (Exception e) {
			// Good.
		}

		// HTML
		String html = "<table _type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>baz</string></td><td><number>789</number></td></tr><tr><td><string>foo</string></td><td><number>123</number></td></tr><tr><td><string>bar</string></td><td><number>456</number></td></tr></table>";
		p = HtmlParser.create().ignoreUnknownBeanProperties(true).build();
		t = p.parse(html, O.class);
		assertEquals(123, t.foo);

		try {
			p = HtmlParser.DEFAULT;
			t = p.parse(json, O.class);
			fail("Expected exception never occurred");
		} catch (Exception e) {
			// Good.
		}

		// UON
		String uon = "(baz=789,foo=123,bar=456)";
		p = UonParser.create().ignoreUnknownBeanProperties(true).build();
		t = p.parse(uon, O.class);
		assertEquals(123, t.foo);

		try {
			p = UonParser.DEFAULT;
			t = p.parse(json, O.class);
			fail("Expected exception never occurred");
		} catch (Exception e) {
			// Good.
		}

		// URL-Encoding
		String urlencoding = "baz=789&foo=123&bar=456";
		p = UrlEncodingParser.create().ignoreUnknownBeanProperties(true).build();
		t = p.parse(urlencoding, O.class);
		assertEquals(123, t.foo);

		try {
			p = UrlEncodingParser.DEFAULT;
			t = p.parse(json, O.class);
			fail("Expected exception never occurred");
		} catch (Exception e) {
			// Good.
		}

	}

	public static class O {
		public int foo;
	}

	//====================================================================================================
	// testPropertyNameFactoryDashedLC1
	//====================================================================================================
	@Test
	public void testPropertyNameFactoryDashedLC1() throws Exception {
		BeanMap<P1> m = session.newBeanMap(P1.class).load("{'foo':1,'bar-baz':2,'bing-boo-url':3}");
		assertEquals(1, m.get("foo"));
		assertEquals(2, m.get("bar-baz"));
		assertEquals(3, m.get("bing-boo-url"));
		P1 b = m.getBean();
		assertEquals(1, b.foo);
		assertEquals(2, b.barBaz);
		assertEquals(3, b.bingBooURL);
		m.put("foo", 4);
		m.put("bar-baz", 5);
		m.put("bing-boo-url", 6);
		assertEquals(4, b.foo);
		assertEquals(5, b.barBaz);
		assertEquals(6, b.bingBooURL);
	}

	@Bean(propertyNamer=PropertyNamerDLC.class)
	public static class P1 {
		public int foo, barBaz, bingBooURL;
	}

	//====================================================================================================
	// testPropertyNameFactoryDashedLC2
	//====================================================================================================
	@Test
	public void testPropertyNameFactoryDashedLC2() throws Exception {
		BeanSession session = BeanContext.DEFAULT_SORTED.createSession();
		BeanMap<P2> m = session.newBeanMap(P2.class).load("{'foo-bar':1,'baz-bing':2}");
		assertEquals(1, m.get("foo-bar"));
		assertEquals(2, m.get("baz-bing"));
		P2 b = m.getBean();
		assertEquals(1, b.getFooBar());
		assertEquals(2, b.getBazBING());
		m.put("foo-bar", 3);
		m.put("baz-bing", 4);
		assertEquals(3, b.getFooBar());
		assertEquals(4, b.getBazBING());
	}

	@Bean(propertyNamer=PropertyNamerDLC.class)
	public static class P2 {
		private int fooBar, bazBING;
		public int getFooBar() {
			return fooBar;
		}
		public void setFooBar(int fooBar) {
			this.fooBar = fooBar;
		}
		public int getBazBING() {
			return bazBING;
		}
		public void setBazBING(int bazBING) {
			this.bazBING = bazBING;
		}
	}

	//====================================================================================================
	// testBeanWithFluentStyleSetters
	//====================================================================================================
	@Test
	public void testBeanWithFluentStyleSetters() throws Exception {
		Q2 t = new Q2();
		BeanMap m = BeanContext.DEFAULT_SORTED.createSession().toBeanMap(t);
		m.put("f1", 1);
		m.put("f2", 2);
		m.put("f3", 3);

		assertSortedObjectEquals("{f1:1,f2:2,f3:0}", m);
	}

	public static class Q1 {}

	public static class Q2 extends Q1 {
		private int f1, f2, f3;

		public Q1 setF1(int f1) {
			this.f1 = f1;
			return this;
		}

		public Q2 setF2(int f2) {
			this.f2 = f2;
			return this;
		}

		// Shouldn't be detected as a setter.
		public String setF3(int f3) {
			this.f3 = f3;
			return null;
		}

		public int getF1() { return f1; }
		public int getF2() { return f2; }
		public int getF3() { return f3; }
	}

	//====================================================================================================
	// testCastWithNormalBean
	//====================================================================================================
	@Test
	public void testCastWithNormalBean() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "R2");
		m.put("f1", 1);
		m.put("f2", "2");

		R2 t = (R2)m.cast(Object.class);
		assertEquals(1, t.f1);

		t = (R2)m.cast(R1.class);
		assertEquals(1, t.f1);
		assertEquals(2, t.f2);

		t = (R2)m.cast(session.getClassMeta(R1.class));
		assertEquals(1, t.f1);
		assertEquals(2, t.f2);

		// Without _type
		m = new ObjectMap(session);
		m.put("f1", 1);
		m.put("f2", "2");

		m = (ObjectMap)m.cast(Object.class);
		assertEquals(1, t.f1);
		assertEquals(2, t.f2);

		t = m.cast(R2.class);
		assertEquals(1, t.f1);
		assertEquals(2, t.f2);

		t = m.cast(session.getClassMeta(R2.class));
		assertEquals(1, t.f1);
		assertEquals(2, t.f2);
	}

	// Bean with no properties
	public static class R1 {
		public int f1;
	}

	public static class R2 extends R1 {
		public int f2;
	}

	//====================================================================================================
	// testCastWithNestedBean
	//====================================================================================================
	@Test
	public void testCastWithNestedBean() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "S");
		m.put("f1", new ObjectMap(session).append("_type", "R1").append("f1", 1));

		S t = (S)m.cast(Object.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(S.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(session.getClassMeta(S.class));
		assertEquals(1, t.f1.f1);

		// Without _type
		m = new ObjectMap(session);
		m.put("f1", new ObjectMap(session).append("_type", R1.class.getName()).append("f1", 1));

		m = (ObjectMap)m.cast(Object.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(S.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(session.getClassMeta(S.class));
		assertEquals(1, t.f1.f1);
	}

	public static class S {
		public R1 f1;
	}

	//====================================================================================================
	// testCastToAnotherMapType
	//====================================================================================================
	@Test
	public void testCastToAnotherMapType() throws Exception {
		Map m2;

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "TreeMap");
		m.put("1", "ONE");

		m2 = (Map)m.cast(Object.class);
		assertTrue(m2 instanceof TreeMap);
		assertEquals("ONE", m2.get("1"));

		m2 = m.cast(Map.class);
		assertTrue(m2 instanceof TreeMap);
		assertEquals("ONE", m2.get("1"));

		m2 = m.cast(bc.getClassMeta(TreeMap.class));
		assertTrue(m2 instanceof TreeMap);
		assertEquals("ONE", m2.get("1"));

		m2 = (Map)m.cast(bc.getClassMeta(TreeMap.class, Integer.class, TEnum.class));
		assertTrue(m2 instanceof TreeMap);
		Map.Entry e = (Map.Entry)m2.entrySet().iterator().next();
		assertTrue(e.getKey() instanceof Integer);
		assertTrue(e.getValue() instanceof TEnum);
		assertEquals(TEnum.ONE, m2.get(1));

		m2 = m.cast(bc.getClassMeta(TreeMap.class));
		assertTrue(m2 instanceof TreeMap);
		e = (Map.Entry)m2.entrySet().iterator().next();
		assertTrue(e.getKey() instanceof String);
		assertTrue(e.getValue() instanceof String);
		assertEquals("ONE", m2.get("1"));

		m2 = (Map)m.cast(bc.getClassMeta(HashMap.class, Integer.class, TEnum.class));
		assertTrue(m2 instanceof HashMap);
		e = (Map.Entry)m2.entrySet().iterator().next();
		assertTrue(e.getKey() instanceof Integer);
		assertTrue(e.getValue() instanceof TEnum);
		assertEquals(TEnum.ONE, m2.get(1));

		// Without _type
		m = new ObjectMap();
		m.put("1", "ONE");

		m2 = (ObjectMap)m.cast(Object.class);
		assertTrue(m2 instanceof ObjectMap);
		assertEquals("ONE", m2.get("1"));

		m2 = m.cast(Map.class);
		assertTrue(m2 instanceof ObjectMap);
		assertEquals("ONE", m2.get("1"));

		m2 = m.cast(bc.getClassMeta(TreeMap.class));
		assertTrue(m2 instanceof TreeMap);
		assertEquals("ONE", m2.get("1"));

		m2 = (Map)m.cast(bc.getClassMeta(TreeMap.class, Integer.class, TEnum.class));
		assertTrue(m2 instanceof TreeMap);
		e = (Map.Entry)m2.entrySet().iterator().next();
		assertTrue(e.getKey() instanceof Integer);
		assertTrue(e.getValue() instanceof TEnum);
		assertEquals(TEnum.ONE, m2.get(1));

		m2 = m.cast(bc.getClassMeta(TreeMap.class));
		assertTrue(m2 instanceof TreeMap);
		e = (Map.Entry)m2.entrySet().iterator().next();
		assertTrue(e.getKey() instanceof String);
		assertTrue(e.getValue() instanceof String);
		assertEquals("ONE", m2.get("1"));
	}

	public static enum TEnum {
		ONE, TWO, THREE;
	}

	//====================================================================================================
	// testCastToLinkedList
	//====================================================================================================
	@Test
	public void testCastToLinkedList() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "LinkedList");
		m.put("items", new ObjectList().append("1").append("2"));

		List l = (List)m.cast(Object.class);
		assertTrue(l instanceof LinkedList);
		assertEquals("1", l.get(0));

		l = m.cast(List.class);
		assertTrue(l instanceof LinkedList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertEquals("1", l.get(0));

		// Without _type
		m = new ObjectMap();
		m.put("items", new ObjectList().append("1").append("2"));

		l = m.cast(List.class);
		assertTrue(l instanceof ObjectList);
		assertEquals("1", l.get(0));

		l = m.cast(LinkedList.class);
		assertTrue(l instanceof LinkedList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof ObjectList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertEquals("1", l.get(0));

		l = (List)m.cast(bc.getClassMeta(List.class, Integer.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof Integer);
		assertEquals(1, l.get(0));
	}

	//====================================================================================================
	// testToLinkedListInteger - cast() to LinkedList<Integer>
	//====================================================================================================
	@Test
	public void testToLinkedListInteger() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "LinkedListOfInts");
		m.put("items", new ObjectList().append("1").append("2"));

		List l = (List)m.cast(Object.class);
		assertTrue(l instanceof LinkedList);
		assertEquals(1, l.get(0));

		l = m.cast(List.class);
		assertTrue(l instanceof LinkedList);
		assertEquals(1, l.get(0));

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertEquals(1, l.get(0));

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);
		assertEquals(1, l.get(0));

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertEquals(1, l.get(0));

		l = (List)m.cast(bc.getClassMeta(List.class, String.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("1", l.get(0));

		// Without _type
		m = new ObjectMap();
		m.put("items", new ObjectList().append("1").append("2"));

		l = m.cast(List.class);
		assertTrue(l instanceof ObjectList);
		assertEquals("1", l.get(0));

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof ObjectList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertEquals("1", l.get(0));

		l = (List)m.cast(bc.getClassMeta(List.class, Integer.class));
		assertTrue(l instanceof ObjectList);
		assertTrue(l.get(0) instanceof Integer);
		assertEquals(1, l.get(0));
	}

	//====================================================================================================
	// testCastToLinkedListBean - cast() to LinkedList<R1>
	//====================================================================================================
	@Test
	public void testCastToLinkedListBean() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "LinkedListOfR1");
		m.put("items", new ObjectList(session).append("{f1:1}"));

		List l = (List)m.cast(Object.class);
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = m.cast(List.class);
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = m.cast(session.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = m.cast(session.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = (List)m.cast(session.getClassMeta(List.class, HashMap.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof HashMap);
		assertEquals(1, ((Map)l.get(0)).get("f1"));

		// Without _type
		m = new ObjectMap(session);
		m.put("items", new ObjectList(session).append("{f1:1}"));

		l = m.cast(List.class);
		assertTrue(l instanceof ObjectList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = m.cast(session.getClassMeta(List.class));
		assertTrue(l instanceof ObjectList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = m.cast(session.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = (List)m.cast(session.getClassMeta(List.class, R1.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = (List)m.cast(session.getClassMeta(List.class, HashMap.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof HashMap);
		assertEquals(1, ((Map)l.get(0)).get("f1"));

		l = (List)m.cast(session.getClassMeta(List.class, Map.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof ObjectMap);
		assertEquals(1, ((Map)l.get(0)).get("f1"));
	}

	//====================================================================================================
	// testCastToLinkedListUsingSwap - cast() to LinkedList<Calendar> using CalendarSwap
	//====================================================================================================
	@Test
	public void testCastToLinkedListUsingSwap() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "LinkedListOfCalendar");
		m.put("items", new ObjectList().append("2001-07-04T15:30:45Z"));

		List l = (List)m.cast(Object.class);
		assertTrue(l instanceof LinkedList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = m.cast(List.class);
		assertTrue(l instanceof LinkedList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		m.cast(HashSet.class);

		l = m.cast(session.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = m.cast(session.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = (List)m.cast(session.getClassMeta(List.class, String.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("2001-07-04T15:30:45Z", l.get(0));

		// Without _type
		m = new ObjectMap().setBeanSession(session);
		m.put("items", new ObjectList().append("2001-07-04T15:30:45Z"));

		l = m.cast(List.class);
		assertTrue(l instanceof LinkedList);

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);

		m.cast(HashSet.class);

		l = m.cast(session.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);

		l = m.cast(session.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);

		l = (List)m.cast(session.getClassMeta(List.class, Calendar.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof Calendar);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));
	}

	//====================================================================================================
	// testCastToStringArray - cast() to String[]
	//====================================================================================================
	@Test
	public void testCastToStringArray() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "StringArray");
		m.put("items", new ObjectList().append("1").append("2"));

		String[] l = (String[])m.cast(Object.class);
		assertEquals("1", l[0]);

		l = m.cast(String[].class);
		assertEquals("1", l[0]);

		StringBuffer[] l2 = m.cast(StringBuffer[].class);
		assertEquals("1", l2[0].toString());

		int[] l3 = m.cast(int[].class);
		assertEquals(1, l3[0]);

		l = m.cast(bc.getClassMeta(String[].class));
		assertEquals("1", l[0]);

		l2 = m.cast(bc.getClassMeta(StringBuffer[].class));
		assertEquals("1", l2[0].toString());

		l3 = m.cast(bc.getClassMeta(int[].class));
		assertEquals("1", l2[0].toString());

		// Without _type
		m = new ObjectMap();
		m.put("items", new ObjectList().append("1").append("2"));

		l = m.cast(String[].class);
		assertEquals("1", l[0]);

		l = m.cast(bc.getClassMeta(String[].class));
		assertEquals("1", l[0]);

		l2 = m.cast(bc.getClassMeta(StringBuffer[].class));
		assertEquals("1", l[0].toString());
	}

	//====================================================================================================
	// testCastToIntArray - cast() to int[]
	//====================================================================================================
	@Test
	public void testCastToIntArray() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "IntArray");
		m.put("items", new ObjectList().append("1").append("2"));

		int[] l = (int[])m.cast(Object.class);
		assertEquals(1, l[0]);

		l = m.cast(int[].class);
		assertEquals(1, l[0]);

		l = m.cast(bc.getClassMeta(int[].class));
		assertEquals(1, l[0]);

		long[] l2;

		l2 = m.cast(long[].class);
		assertEquals(1, l2[0]);

		l2 = m.cast(bc.getClassMeta(long[].class));
		assertEquals(1, l2[0]);

		// Without _type
		m = new ObjectMap();
		m.put("items", new ObjectList().append("1").append("2"));

		l = m.cast(int[].class);
		assertEquals(1, l[0]);

		l = m.cast(bc.getClassMeta(int[].class));
		assertEquals(1, l[0]);

		l2 = m.cast(long[].class);
		assertEquals(1, l2[0]);

		l2 = m.cast(bc.getClassMeta(long[].class));
		assertEquals(1, l2[0]);
	}

	//====================================================================================================
	// testCastToString2dArray - cast() to String[][]
	//====================================================================================================
	@Test
	public void testCastToString2dArray() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "String2dArray");
		m.put("items", new ObjectList().append(new ObjectList().append("1")).append(new ObjectList().append("2")));

		String[][] l = (String[][])m.cast(Object.class);
		assertEquals("1", l[0][0]);
		assertEquals("2", l[1][0]);

		l = m.cast(String[][].class);
		assertEquals("1", l[0][0]);

		l = m.cast(bc.getClassMeta(String[][].class));
		assertEquals("2", l[1][0]);

		// Without _type
		m = new ObjectMap();
		m.put("items", new ObjectList().append(new ObjectList().append("1")).append(new ObjectList().append("2")));

		l = m.cast(String[][].class);
		assertEquals("1", l[0][0]);

		l = m.cast(bc.getClassMeta(String[][].class));
		assertEquals("2", l[1][0]);
	}

	//====================================================================================================
	// testCastToInt2dArray - cast() to int[][]
	//====================================================================================================
	@Test
	public void testCastToInt2dArray() throws Exception {

		// With _type
		ObjectMap m = new ObjectMap(session);
		m.put("_type", "Int2dArray");
		m.put("items", new ObjectList().append(new ObjectList().append("1")).append(new ObjectList().append("2")));

		int[][] l = (int[][])m.cast(Object.class);
		assertEquals(1, l[0][0]);
		assertEquals(2, l[1][0]);

		l = m.cast(int[][].class);
		assertEquals(1, l[0][0]);

		l = m.cast(bc.getClassMeta(int[][].class));
		assertEquals(2, l[1][0]);

		// Without _type
		m = new ObjectMap();
		m.put("items", new ObjectList().append(new ObjectList().append("1")).append(new ObjectList().append("2")));

		l = m.cast(int[][].class);
		assertEquals(1, l[0][0]);

		l = m.cast(bc.getClassMeta(int[][].class));
		assertEquals(2, l[1][0]);
	}

	//====================================================================================================
	// testHiddenProperties
	//====================================================================================================
	@Test
	public void testHiddenProperties() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX;
		BeanMeta bm = s.getBeanMeta(U.class);
		assertNotNull(bm.getPropertyMeta("a"));
		assertNotNull(bm.getPropertyMeta("b"));
		assertNull(bm.getPropertyMeta("c"));
		assertNull(bm.getPropertyMeta("d"));

		U t = new U();
		t.a = "a";
		t.b = "b";
		String r = s.serialize(t);
		assertEquals("{a:'a',b:'b'}", r);

		// Make sure setters are used if present.
		t = JsonParser.DEFAULT.parse(r, U.class);
		assertEquals("b(setter)", t.b);
	}

	public static class U {
		public String a, b;

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}

		@BeanIgnore
		public String getB() {
			return b;
		}

		public void setB(String b) {
			this.b = b+"(setter)";
		}

		@BeanIgnore
		public String c;

		@BeanIgnore
		public String getD() {
			return null;
		}

		@BeanIgnore
		public void setD(String d) {
		}
	}

	//====================================================================================================
	// testBeanPropertyOrder
	//====================================================================================================
	@Test
	public void testBeanPropertyOrder() throws Exception {
		assertObjectEquals("{a1:'1',a2:'2',a3:'3',a4:'4'}", new V2());
		assertObjectEquals("{a3:'3',a4:'4',a5:'5',a6:'6'}", new V3());
	}

	public static class V {
		public String a1="1", a2="2";
	}

	public static class V2 extends V {
		public String a3="3", a4="4";
	}

	@Bean(stopClass=V.class)
	public static class V3 extends V2 {
		public String a5="5", a6="6";
	}

	//====================================================================================================
	// testBeanMethodOrder
	//====================================================================================================
	@Test
	public void testBeanMethodOrder() throws Exception {
		assertSortedObjectEquals("{a1:'1',a2:'2',a3:'3',a4:'4'}", new W2());
		assertSortedObjectEquals("{a3:'3',a4:'4',a5:'5',a6:'6'}", new W3());
	}

	public static class W {
		public String getA1() {return "1";}
		public String getA2() {return "2";}
	}

	public static class W2 extends W {
		public String getA3() {return "3";}
		public String getA4() {return "4";}
	}

	@Bean(stopClass=W.class)
	public static class W3 extends W2 {
		public String getA5() {return "5";}
		public String getA6() {return "6";}
	}

	//====================================================================================================
	// testResourceDescription
	//====================================================================================================
	@Test
	public void testOverriddenPropertyTypes() throws Exception {
		JsonSerializer s = JsonSerializer.DEFAULT_LAX;
		JsonParser p = JsonParser.DEFAULT;
		String r;

		X1 t1 = X1.create();
		r = s.serialize(t1);
		assertEquals("{f1:'1',f2:'2'}", r);
		t1 = p.parse(r, X1.class);
		assertEquals("1", t1.f1);
		assertEquals("2", t1.getF2());

		X2 t2 = X2.create();
		r = s.serialize(t2);
		assertEquals("{f1:1,f2:2}", r);
		t2 = p.parse(r, X2.class);
		assertEquals(1, t2.f1.intValue());
		assertEquals(2, t2.getF2().intValue());
	}

	public static class X1 {
		public Object f1;
		private Object f2;

		static X1 create() {
			X1 x = new X1();
			x.f1 = "1";
			x.f2 = "2";
			return x;
		}

		public Object getF2() {
			return f2;
		}

		public void setF2(Object f2) {
			this.f2 = f2;
		}
	}

	public static class X2 extends X1 {
		public Integer f1;
		private Integer f2;

		static X2 create() {
			X2 x = new X2();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}

		@Override /* X1 */
		public Integer getF2() {
			return f2;
		}

		public void setF2(Integer f2) {
			this.f2 = f2;
		}
	}

	@Test
	public void testSettingCollectionPropertyMultipleTimes() throws Exception {

		BeanMap m = BeanContext.DEFAULT.createSession().newBeanMap(Y.class);
		m.put("f1", new ObjectList().append("a"));
		m.put("f1",  new ObjectList().append("b"));
		assertEquals("{f1=[b]}", m.toString());
	}

	public static class Y {
		public List<String> f1 = new LinkedList<String>();
	}

	//====================================================================================================
	// entrySet(false).
	//====================================================================================================
	@Test
	public void testIgnoreNulls() {
		Z z = new Z();
		BeanMap<Z> bm = BeanContext.DEFAULT.createSession().toBeanMap(z);

		Iterator i = bm.getValues(true).iterator();
		assertFalse(i.hasNext());

		z.b = "";
		i = bm.getValues(true).iterator();
		assertTrue(i.hasNext());
		i.next();
		assertFalse(i.hasNext());

		i = bm.getValues(false).iterator();
		assertTrue(i.hasNext());
		i.next();
		assertTrue(i.hasNext());
		i.next();
		assertTrue(i.hasNext());
		i.next();
		assertFalse(i.hasNext());
	}

	public static class Z {
		public String a, b, c;
	}
}