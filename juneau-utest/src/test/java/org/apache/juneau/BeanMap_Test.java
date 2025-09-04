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
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.objecttools.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.jupiter.api.*;

@SuppressWarnings({"rawtypes","serial"})
class BeanMap_Test extends SimpleTestBase {

	JsonSerializer serializer = Json5Serializer.DEFAULT.copy().addBeanTypes().addRootType().build();

	BeanContext bc = BeanContext.create()
			.beanDictionary(MyBeanDictionaryMap.class)
			.build();
	BeanSession session = bc.getSession();

	public static class MyBeanDictionaryMap extends BeanDictionaryMap {
		public MyBeanDictionaryMap() {
			append("StringArray", String[].class);
			append("String2dArray", String[][].class);
			append("IntArray", int[].class);
			append("Int2dArray", int[][].class);
			append("S", S.class);
			append("R1", R1.class);
			append("R2", R2.class);
			append("LinkedList", LinkedList.class);
			append("TreeMap", TreeMap.class);
			append("LinkedListOfInts", LinkedList.class, Integer.class);
			append("LinkedListOfR1", LinkedList.class, R1.class);
			append("LinkedListOfCalendar", LinkedList.class, Calendar.class);
		}
	}

	//====================================================================================================
	// Primitive field properties
	//====================================================================================================
	@Test void a01_primitiveFieldProperties() {
		var t = new A();
		var m = bc.toBeanMap(t);

		// Make sure setting primitive values to null causes them to get default values.
		m.put("i1", null);
		m.put("s1", null);
		m.put("l1", null);
		m.put("d1", null);
		m.put("f1", null);
		m.put("b1", null);
		assertBean(m, "i1,s1,l1,d1,f1,b1", "0,0,0,0.0,0.0,false");

		// Make sure setting non-primitive values to null causes them to set to null.
		m.put("i2", null);
		m.put("s2", null);
		m.put("l2", null);
		m.put("d2", null);
		m.put("f2", null);
		m.put("b2", null);
		assertBean(m, "i2,s2,l2,d2,f2,b2", "null,null,null,null,null,null");

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
		assertBean(m, "i1,i2,s1,s2,l1,l2,d1,d2,f1,f2", "1,1,1,1,1,1,1.0,1.0,1.0,1.0");

		m.put("b1", true);
		m.put("b2", Boolean.valueOf(true));
		assertBean(m, "b1,b2", "true,true");
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
	@Test void a02_primitiveMethodProperties() {
		var t = new B();
		var m = bc.toBeanMap(t);

		// Make sure setting primitive values to null causes them to get default values.
		m.put("i1", null);
		m.put("s1", null);
		m.put("l1", null);
		m.put("d1", null);
		m.put("f1", null);
		m.put("b1", null);
		assertBean(m, "i1,s1,l1,d1,f1,b1", "0,0,0,0.0,0.0,false");

		// Make sure setting non-primitive values to null causes them to set to null.
		m.put("i2", null);
		m.put("s2", null);
		m.put("l2", null);
		m.put("d2", null);
		m.put("f2", null);
		m.put("b2", null);
		assertBean(m, "i2,s2,l2,d2,f2,b2", "null,null,null,null,null,null");

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
		assertBean(m, "i1,i2,s1,s2,l1,l2,d1,d2,f1,f2", "1,1,1,1,1,1,1.0,1.0,1.0,1.0");

		m.put("b1", true);
		m.put("b2", true);
		assertBean(m, "b1,b2", "true,true");
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
		public void setI1(int v) {i1 = v;}
		public Integer getI2() {return i2;}
		public void setI2(Integer v) {i2 = v;}

		public short getS1() {return s1;}
		public void setS1(short v) {s1 = v;}
		public Short getS2() {return s2;}
		public void setS2(Short v) {s2 = v;}

		public long getL1() {return l1;}
		public void setL1(long v) {l1 = v;}
		public Long getL2() {return l2;}
		public void setL2(Long v) {l2 = v;}

		public double getD1() {return d1;}
		public void setD1(double v) {d1 = v;}
		public Double getD2() {return d2;}
		public void setD2(Double v) {d2 = v;}

		public float getF1() {return f1;}
		public void setF1(float v) {f1 = v;}
		public Float getF2() {return f2;}
		public void setF2(Float v) {f2 = v;}

		public boolean getB1() {return b1;}
		public void setB1(boolean v) {b1 = v;}
		public Boolean getB2() {return b2;}
		public void setB2(Boolean v) {b2 = v;}
	}

	//====================================================================================================
	// testCollectionFieldProperties
	//====================================================================================================
	@Test void a03_collectionFieldProperties() {
		var t = new C();
		var m = bc.toBeanMap(t);

		// Non-initialized list fields.
		m.put("l1", JsonList.ofJson("[1,2,3]"));
		m.put("al1", JsonList.ofJson("[1,2,3]"));
		m.put("ll1", JsonList.ofJson("[1,2,3]"));
		m.put("c1", JsonList.ofJson("[1,2,3]"));
		m.put("jl1", JsonList.ofJson("[1,2,3]"));

		// al1 should be initialized with an ArrayList, since it's not a superclass of JsonList.
		// The rest are proper superclasses of JsonList.
		assertMap(
			m,
			"al1{class{simpleName}},l1{class{simpleName}},ll1{class{simpleName}},c1{class{simpleName}},jl1{class{simpleName}}",
			"{{ArrayList}},{{JsonList}},{{LinkedList}},{{JsonList}},{{JsonList}}"
		);

		// Non-initialized map fields.
		m.put("m1", JsonMap.ofJson("{foo:'bar'}"));
		m.put("hm1", JsonMap.ofJson("{foo:'bar'}"));
		m.put("jm1", JsonMap.ofJson("{foo:'bar'}"));
		m.put("tm1", JsonMap.ofJson("{foo:'bar'}"));

		// tm1 should be initialized with TreeMap, since it's not a superclass of JsonMap.
		// The rest are proper superclasses of JsonMap
		assertBean(
			m,
			"tm1{class{simpleName}},m1{class{simpleName}},hm1{class{simpleName}},jm1{class{simpleName}}",
			"{{TreeMap}},{{JsonMap}},{{HashMap}},{{JsonMap}}"
		);

		// Initialized fields should reuse existing field value.
		m.put("l2", JsonList.ofJson("[1,2,3]"));
		m.put("al2", JsonList.ofJson("[1,2,3]"));
		m.put("ll2", JsonList.ofJson("[1,2,3]"));
		m.put("c2", JsonList.ofJson("[1,2,3]"));
		m.put("m2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("hm2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("tm2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("jm2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("jl2", JsonList.ofJson("[1,2,3]"));

		assertBean(
			m,
			"l2{class{simpleName}},al2{class{simpleName}},ll2{class{simpleName}},c2{class{simpleName}},m2{class{simpleName}},hm2{class{simpleName}},tm2{class{simpleName}},jm2{class{simpleName}},jl2{class{simpleName}}",
			"{{ArrayList}},{{ArrayList}},{{LinkedList}},{{ArrayList}},{{HashMap}},{{HashMap}},{{TreeMap}},{{JsonMap}},{{JsonList}}"
		);
	}

	public static class C {
		public List l1;
		public ArrayList al1;
		public LinkedList ll1;
		public Collection c1;
		public Map m1;
		public HashMap hm1;
		public JsonMap jm1;
		public TreeMap tm1;
		public JsonList jl1;
		public List l2 = new ArrayList();
		public ArrayList al2 = new ArrayList();
		public LinkedList ll2 = new LinkedList();
		public Collection c2 = new ArrayList();
		public Map m2 = new HashMap();
		public HashMap hm2 = new HashMap();
		public TreeMap tm2 = new TreeMap();
		public JsonMap jm2 = new JsonMap();
		public JsonList jl2 = new JsonList();
	}

	//====================================================================================================
	// testCollectionMethodProperties
	//====================================================================================================
	@Test void a04_collectionMethodProperties() {
		var t = new D();
		var m = bc.toBeanMap(t);

		// Non-initialized list fields.
		m.put("l1", JsonList.ofJson("[1,2,3]"));
		m.put("al1", JsonList.ofJson("[1,2,3]"));
		m.put("ll1", JsonList.ofJson("[1,2,3]"));
		m.put("c1", JsonList.ofJson("[1,2,3]"));
		m.put("jl1", JsonList.ofJson("[1,2,3]"));

		// al1 should be initialized with an ArrayList, since it's not a superclass of JsonList.
		// The rest are proper superclasses of JsonList.
		assertBean(
			m,
			"al1{class{simpleName}},l1{class{simpleName}},ll1{class{simpleName}},c1{class{simpleName}},jl1{class{simpleName}}",
			"{{ArrayList}},{{JsonList}},{{JsonList}},{{JsonList}},{{JsonList}}"
		);

		// Non-initialized map fields.
		m.put("m1", JsonMap.ofJson("{foo:'bar'}"));
		m.put("hm1", JsonMap.ofJson("{foo:'bar'}"));
		m.put("jm1", JsonMap.ofJson("{foo:'bar'}"));
		m.put("tm1", JsonMap.ofJson("{foo:'bar'}"));

		// tm1 should be initialized with TreeMap, since it's not a superclass of JsonMap.
		// The rest are proper superclasses of JsonMap
		assertBean(
			m,
			"tm1{class{simpleName}},m1{class{simpleName}},hm1{class{simpleName}},jm1{class{simpleName}}",
			"{{TreeMap}},{{JsonMap}},{{JsonMap}},{{JsonMap}}"
		);

		// Initialized fields should reuse existing field value.
		m.put("l2", JsonList.ofJson("[1,2,3]"));
		m.put("al2", JsonList.ofJson("[1,2,3]"));
		m.put("ll2", JsonList.ofJson("[1,2,3]"));
		m.put("c2", JsonList.ofJson("[1,2,3]"));
		m.put("m2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("hm2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("tm2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("jm2", JsonMap.ofJson("{foo:'bar'}"));
		m.put("jl2", JsonList.ofJson("[1,2,3]"));

		assertBean(
			m,
			"l2{class{simpleName}},al2{class{simpleName}},ll2{class{simpleName}},c2{class{simpleName}},m2{class{simpleName}},hm2{class{simpleName}},tm2{class{simpleName}},jm2{class{simpleName}},jl2{class{simpleName}}",
			"{{JsonList}},{{ArrayList}},{{JsonList}},{{JsonList}},{{JsonMap}},{{JsonMap}},{{TreeMap}},{{JsonMap}},{{JsonList}}"
		);
	}

	public static class D {
		private List l1;
		public List getL1() {return l1;}
		public void setL1(List v) {l1 = v;}

		private ArrayList al1;
		public ArrayList getAl1() {return al1;}
		public void setAl1(ArrayList v) {al1 = v;}

		private LinkedList ll1;
		public LinkedList getLl1() {return ll1;}
		public void setLl1(LinkedList v) {ll1 = v;}

		private Collection c1;
		public Collection getC1() {return c1;}
		public void setC1(Collection v) {c1 = v;}

		private Map m1;
		public Map getM1() {return m1;}
		public void setM1(Map v) {m1 = v;}

		private HashMap hm1;
		public HashMap getHm1() {return hm1;}
		public void setHm1(HashMap v) {hm1 = v;}

		private JsonMap jm1;
		public JsonMap getJm1() {return jm1;}
		public void setJm1(JsonMap v) {jm1 = v;}

		private TreeMap tm1;
		public TreeMap getTm1() {return tm1;}
		public void setTm1(TreeMap v) {tm1 = v;}

		private JsonList jl1;
		public JsonList getJl1() {return jl1;}
		public void setJl1(JsonList v) {jl1 = v;}

		private List l2 = new ArrayList();
		public List getL2() {return l2;}
		public void setL2(List v) {l2 = v;}

		private ArrayList al2 = new ArrayList();
		public ArrayList getAl2() {return al2;}
		public void setAl2(ArrayList v) {al2 = v;}

		private LinkedList ll2 = new LinkedList();
		public LinkedList getLl2() {return ll2;}
		public void setLl2(LinkedList v) {ll2 = v;}

		private Collection c2 = new ArrayList();
		public Collection getC2() {return c2;}
		public void setC2(Collection v) {c2 = v;}

		private Map m2 = new HashMap();
		public Map getM2() {return m2;}
		public void setM2(Map v) {m2 = v;}

		private HashMap hm2 = new HashMap();
		public HashMap getHm2() {return hm2;}
		public void setHm2(HashMap v) {hm2 = v;}

		private TreeMap tm2 = new TreeMap();
		public TreeMap getTm2() {return tm2;}
		public void setTm2(TreeMap v) {tm2 = v;}

		private JsonMap jm2 = new JsonMap();
		public JsonMap getJm2() {return jm2;}
		public void setJm2(JsonMap v) {jm2 = v;}

		private JsonList jl2 = new JsonList();
		public JsonList getJl2() {return jl2;}
		public void setJl2(JsonList v) {jl2 = v;}
	}

	//====================================================================================================
	// testArrayProperties
	//====================================================================================================
	@Test void a05_arrayProperties() {
		var t = new D1();
		var m = bc.toBeanMap(t);
		m.put("b", JsonMap.ofJson("{s:'foo'}"));
		assertNotNull(t.b);
		assertEquals("foo", t.b.s);

		var m2 = new TreeMap<String,Object>();
		m2.put("s", "bar");
		m.put("b", m2);
		assertNotNull(t.b);
		assertEquals("bar", t.b.s);

		m.put("b", new D2());
		assertEquals("default", t.b.s);

		var p = JsonParser.create().beanDictionary(D2.class).build();
		m.put("lb1", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "lb1{class{simpleName}},lb1{0{class{simpleName}}},lb1{0{s}}", "{{JsonList}},{{{D2}}},{{foobar}}");

		m.put("lb2", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "lb2{class{simpleName}},lb2{0{class{simpleName}}},lb2{0{s}}", "{{ArrayList}},{{{D2}}},{{foobar}}");

		m.put("ab1", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "ab1{class{simpleName}},ab1{0{class{simpleName},s}}", "{{D2[]}},{{{D2},foobar}}");

		m.put("ab2", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "ab2{class{simpleName}},ab2{0{class{simpleName},s}}", "{{D2[]}},{{{D2},foobar}}");
	}

	public static class D1 {
		public D2 b;
		public List<D2> lb1;
		public List<D2> lb2 = new ArrayList<>();
		public D2[] ab1;
		public D2[] ab2 = {};
	}

	@Bean(typeName="D2")
	public static class D2 {
		public String s = "default";
	}

	@Test void a06_arrayProperties_usingConfig() {
		var t = new D1c();
		var m = bc.toBeanMap(t);
		m.put("b", JsonMap.ofJson("{s:'foo'}"));
		assertNotNull(t.b);
		assertBean(t.b, "s", "foo");

		var m2 = new TreeMap<String,Object>();
		m2.put("s", "bar");
		m.put("b", m2);
		assertNotNull(t.b);
		assertBean(t.b, "s", "bar");

		m.put("b", new D2c());
		assertBean(t.b, "s", "default");

		var p = JsonParser.create().beanDictionary(D2c.class).applyAnnotations(D1cConfig.class).build();
		m.put("lb1", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "lb1{class{simpleName}},lb1{0{class{simpleName}}},lb1{0{s}}", "{{JsonList}},{{{D2c}}},{{foobar}}");

		m.put("lb2", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "lb2{class{simpleName}},lb2{0{class{simpleName}}},lb2{0{s}}", "{{ArrayList}},{{{D2c}}},{{foobar}}");

		m.put("ab1", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "ab1{class{simpleName}},ab1{0{class{simpleName},s}}", "{{D2c[]}},{{{D2c},foobar}}");

		m.put("ab2", JsonList.ofText("[{_type:'D2',s:'foobar'}]", p));
		assertBean(t, "ab2{class{simpleName}},ab2{0{class{simpleName},s}}", "{{D2c[]}},{{{D2c},foobar}}");
	}

	@Bean(on="Dummy1", typeName="dummy")
	@Bean(on="D2c", typeName="D2")
	@Bean(on="Dummy2", typeName="dummy")
	private static class D1cConfig {}

	public static class D1c {
		public D2c b;
		public List<D2c> lb1;
		public List<D2c> lb2 = new ArrayList<>();
		public D2c[] ab1;
		public D2c[] ab2 = {};
	}

	public static class D2c {
		public String s = "default";
	}

	//====================================================================================================
	// testArrayPropertiesInJsonList
	//====================================================================================================
	@Test void a07_arrayPropertiesInJsonList() {
		var t = new E();
		var m = bc.toBeanMap(t);
		m.put("s", JsonList.ofJson("['foo']"));
		m.put("s2", JsonList.ofJson("[['foo']]"));
		m.put("i", JsonList.ofJson("[1,2,3]"));
		m.put("i2", JsonList.ofJson("[[1,2,3],[4,5,6]]"));
		assertEquals("{s:['foo'],s2:[['foo']],i:[1,2,3],i2:[[1,2,3],[4,5,6]]}", Json5Serializer.DEFAULT.serialize(t));
		m.put("i", JsonList.ofJson("[null,null,null]"));
		m.put("i2", JsonList.ofJson("[[null,null,null],[null,null,null]]"));
		assertEquals("{s:['foo'],s2:[['foo']],i:[0,0,0],i2:[[0,0,0],[0,0,0]]}", Json5Serializer.DEFAULT.serialize(t));
	}

	@Bean(p="s,s2,i,i2")
	public static class E {
		public String[] s;
		public String[][] s2;
		public int[] i;
		public int[][] i2;
	}

	//====================================================================================================
	// BeanMap.invokeMethod()
	//====================================================================================================
	@Test void a08_invokeMethod() throws Exception {
		var t5 = new F();
		var p = JsonParser.DEFAULT;
		var m = bc.toBeanMap(t5);
		ObjectIntrospector.create(t5, p).invokeMethod("doSetAProperty(java.lang.String)", "['baz']");
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
	// @Beanp tests
	//====================================================================================================
	@Test void a09_beanPropertyAnnotation() {
		var t6 = new G1();
		var m = bc.toBeanMap(t6);

		m.put("l2", "[{a:'a',i:1}]");
		m.put("l3", "[{a:'a',i:1}]");
		m.put("l4", "[{a:'a',i:1}]");
		m.put("m2", "[{a:'a',i:1}]");
		m.put("m3", "[{a:'a',i:1}]");
		m.put("m4", "[{a:'a',i:1}]");

		assertMapped(m, (map,prop) -> map.get(prop).getClass().getSimpleName(),
			"l2,l3,l4,m2,m3,m4",
			"LinkedList,JsonList,LinkedList,LinkedList,JsonList,LinkedList");

		assertMapped(m, (map,prop) -> ((List)map.get(prop)).get(0).getClass().getSimpleName(),
			"l2,l3,l4,m2,m3,m4",
			"G,G,G,G,G,G");

		m.put("m5", "[{a:'a',i:1}]");
		assertMapped(m, (map,prop) -> map.get(prop).getClass().getSimpleName(),
			"m5", "LinkedList");
		assertMapped(m, (map,prop) -> ((List)map.get(prop)).get(0).getClass().getSimpleName(),
			"m5", "G");
	}

	public static class G {
		public String a;
		public int i;
	}

	public static class G1 {

		public List<G> l1;

		public List<G> l2 = new LinkedList<>();

		@Beanp(type=List.class,params={G.class})
		public List<G> l3;

		@Beanp(type=LinkedList.class,params={G.class})
		public List<G> l4;

		private List<G> m1;
		public List<G> getM1() { return m1; }
		public void setM1(List<G> v) { m1 = v; }

		private List<G> m2 = new LinkedList<>();
		public List<G> getM2() { return m2; }
		public void setM2(List<G> v) { m2 = v; }

		private List<G> m3;
		@Beanp(type=List.class,params={G.class})
		public List<G> getM3() { return m3; }
		public void setM3(List<G> v) { m3 = v; }

		private List<G> m4;
		@Beanp(type=LinkedList.class,params={G.class})
		public List<G> getM4() { return m4; }
		public void setM4(List<G> v) { m4 = v; }

		@Beanp(type=LinkedList.class,params={G.class})
		private List<G> m5;
		public List<G> getM5() { return m5; }
		public void setM5(List<G> v) { m5 = v; }
	}

	//====================================================================================================
	// Enum tests
	//====================================================================================================
	@Test void a10_enum() {

		// Initialize existing bean.
		var t7 = new H();
		var m = bc.toBeanMap(t7);
		m.put("enum1", "ONE");
		m.put("enum2", "TWO");
		assertEquals("{_type:'H',enum1:'ONE',enum2:'TWO'}", serializer.serialize(t7));
		assertBean(t7, "enum1,enum2", "ONE,TWO");

		// Use BeanContext to create bean instance.
		m = BeanContext.DEFAULT.newBeanMap(H.class).load("{enum1:'TWO',enum2:'THREE'}");
		assertEquals("{_type:'H',enum1:'TWO',enum2:'THREE'}", serializer.serialize(m.getBean()));
		t7 = m.getBean();
		assertBean(t7, "enum1,enum2", "TWO,THREE");

		// Create instance directly from JSON.
		var p = JsonParser.create().beanDictionary(H.class).build();
		t7 = (H)p.parse("{_type:'H',enum1:'THREE',enum2:'ONE'}", Object.class);
		assertEquals("{_type:'H',enum1:'THREE',enum2:'ONE'}", serializer.serialize(t7));
		assertBean(t7, "enum1,enum2", "THREE,ONE");
	}

	public enum HEnum {
		ONE, TWO, THREE
	}

	@Bean(typeName="H")
	public static class H {

		public HEnum enum1;

		private HEnum enum2;
		public HEnum getEnum2() { return enum2; }
		public void setEnum2(HEnum v) { enum2 = v; }
	}

	//====================================================================================================
	// Automatic detection of generic types
	//====================================================================================================
	@Test void a11_automaticDetectionOfGenericTypes() {
		var bm = BeanContext.DEFAULT.newBeanMap(I.class);
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
		public List<Integer> getP2() { return null; }
		public List<? extends Integer> p3;
		public Map<String,Integer> p4;
		public Map<String,Integer> getP5() { return null; }
		public Map<String,? extends Integer> p6;
	}

	//====================================================================================================
	// Overriding detection of generic types.
	//====================================================================================================
	@Test void a12_overridingDetectionOfGenericTypes() {
		var bm = BeanContext.DEFAULT.newBeanMap(J.class);
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
		@Beanp(params={Float.class}) public List<String> p1;
		@Beanp(params={Float.class}) public List<Integer> getP2() { return null; }
		@Beanp(params={Float.class}) public List<? extends Integer> p3;
		@Beanp(params={Object.class, Float.class}) public Map<String,Integer> p4;
		@Beanp(params={Object.class, Float.class}) public Map<String,Integer> getP5() { return null; }
		@Beanp(params={String.class, Float.class}) public Map<String,? extends Integer> p6;
	}

	//====================================================================================================
	// Overriding detection of generic types.
	//====================================================================================================
	@Test void a13_overridingDetectionOfGenericTypes2() {
		var bm = bc.newBeanMap(K.class);
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
		@Beanp(params=Float.class) public List<String> p1;
		@Beanp(params=Float.class) public List<Integer> getP2() { return null; }
		@Beanp(params=Float.class) public List<? extends Integer> p3;
		@Beanp(params={String.class,Float.class}) public Map<String,Integer> p4;
		@Beanp(params={String.class,Float.class}) public Map<String,Integer> getP5() { return null; }
		@Beanp(params={String.class,Float.class}) public Map<String,? extends Integer> p6;
	}

	//====================================================================================================
	// List<E> subclass properties
	//====================================================================================================
	@Test void a14_genericListSubclass() {
		var bm = bc.newBeanMap(L.class);
		bm.put("list", "[{name:'1',value:'1'},{name:'2',value:'2'}]");
		var b = bm.getBean();
		assertBean(b.list.get(0), "name", "1");
	}

	public static class L {
		public L1 list;
	}

	public static class L1 extends LinkedList<L2> {}

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
	@Test void a15_genericFields() {

		var t1 = new M2();
		var bm = bc.toBeanMap(t1);
		assertBean(bm, "x", "1");

		var t2 = new M3();
		var cm = bc.toBeanMap(t2);
		assertBean(cm, "x", "2");

		var t3 = new M4();
		var dm = bc.toBeanMap(t3);
		assertBean(dm, "x", "3");

		var t4 = new M5();
		var em = bc.toBeanMap(t4);
		assertBean(em, "x", "4");
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

	@SuppressWarnings("unchecked")
	public static class M4<T extends Number> extends M1<T> {
		public M4() {
			this.x = (T)Integer.valueOf(3);
		}
	}

	public static class M5 extends M4<Integer> {
		public M5() {
			this.x = Integer.valueOf(4);
		}
	}

	//====================================================================================================
	// Generic methods
	//====================================================================================================
	@Test void a16_genericMethods() {

		var t1 = new N2();
		var bm = bc.toBeanMap(t1);
		assertBean(bm, "x", "1");

		var t2 = new N3();
		var cm = bc.toBeanMap(t2);
		assertBean(cm, "x", "2");

		var t3 = new N4();
		var dm = bc.toBeanMap(t3);
		assertBean(dm, "x", "3");

		var t4 = new N5();
		var em = bc.toBeanMap(t4);
		assertBean(em, "x", "4");
	}

	public static class N1<T> {
		private T x;
		public void setX(T v) { x = v; }
		public T getX() { return x; }
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

	@SuppressWarnings("unchecked")
	public static class N4<T extends Number> extends N1<T> {
		public N4() {
			setX((T)Integer.valueOf(3));
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
	@Test void a17_ignoreUnknownBeanPropertiesSetting() {

		// JSON
		var json = "{baz:789,foo:123,bar:456}";
		var p = (ReaderParser)JsonParser.create().ignoreUnknownBeanProperties().build();
		var t = p.parse(json, O.class);
		assertBean(t, "foo", "123");

		assertThrows(Exception.class, ()->JsonParser.DEFAULT.parse(json, O.class));

		// XML
		var xml = "<object><baz type='number'>789</baz><foo type='number'>123</foo><bar type='number'>456</bar></object>";
		p = XmlParser.create().ignoreUnknownBeanProperties().build();
		t = p.parse(xml, O.class);
		assertBean(t, "foo", "123");

		assertThrows(Exception.class, ()->XmlParser.DEFAULT.parse(json, O.class));

		// HTML
		var html = "<table _type='object'><tr><th><string>key</string></th><th><string>value</string></th></tr><tr><td><string>baz</string></td><td><number>789</number></td></tr><tr><td><string>foo</string></td><td><number>123</number></td></tr><tr><td><string>bar</string></td><td><number>456</number></td></tr></table>";
		p = HtmlParser.create().ignoreUnknownBeanProperties().build();
		t = p.parse(html, O.class);
		assertBean(t, "foo", "123");

		assertThrows(Exception.class, ()->HtmlParser.DEFAULT.parse(json, O.class));

		// UON
		var uon = "(baz=789,foo=123,bar=456)";
		p = UonParser.create().ignoreUnknownBeanProperties().build();
		t = p.parse(uon, O.class);
		assertBean(t, "foo", "123");

		assertThrows(Exception.class, ()->UonParser.DEFAULT.parse(json, O.class));

		// URL-Encoding
		var urlencoding = "baz=789&foo=123&bar=456";
		p = UrlEncodingParser.create().ignoreUnknownBeanProperties().build();
		t = p.parse(urlencoding, O.class);
		assertBean(t, "foo", "123");

		assertThrows(Exception.class, ()->UrlEncodingParser.DEFAULT.parse(json, O.class));
	}

	public static class O {
		public int foo;
	}

	//====================================================================================================
	// testPropertyNameFactoryDashedLC1
	//====================================================================================================
	@Test void a18_propertyNameFactoryDashedLC1() {
		var m = bc.newBeanMap(P1.class).load("{'foo':1,'bar-baz':2,'bing-boo-url':3}");
		assertMap(m, "foo,bar-baz,bing-boo-url", "1,2,3");
		assertBean(m.getBean(), "foo,barBaz,bingBooURL", "1,2,3");
		m.put("foo", 4);
		m.put("bar-baz", 5);
		m.put("bing-boo-url", 6);
		assertBean(m.getBean(), "foo,barBaz,bingBooURL", "4,5,6");
	}

	@Bean(propertyNamer=PropertyNamerDLC.class)
	public static class P1 {
		public int foo, barBaz, bingBooURL;
	}

	@Test void a19_propertyNameFactoryDashedLC1_usingConfig() {
		var m = bc.copy().applyAnnotations(P1cConfig.class).build().newBeanMap(P1c.class).load("{'foo':1,'bar-baz':2,'bing-boo-url':3}");
		assertMap(m, "foo,bar-baz,bing-boo-url", "1,2,3");
		assertBean(m.getBean(), "foo,barBaz,bingBooURL", "1,2,3");
		m.put("foo", 4);
		m.put("bar-baz", 5);
		m.put("bing-boo-url", 6);
		assertBean(m.getBean(), "foo,barBaz,bingBooURL", "4,5,6");
	}

	@Bean(on="Dummy1", propertyNamer=PropertyNamerDLC.class)
	@Bean(on="P1c", propertyNamer=PropertyNamerDLC.class)
	@Bean(on="Dummy2", propertyNamer=PropertyNamerDLC.class)
	private static class P1cConfig {}

	public static class P1c {
		public int foo, barBaz, bingBooURL;
	}


	//====================================================================================================
	// testPropertyNameFactoryDashedLC2
	//====================================================================================================
	@Test void a20_propertyNameFactoryDashedLC2() {
		var bc2 = BeanContext.DEFAULT_SORTED;
		var m = bc2.newBeanMap(P2.class).load("{'foo-bar':1,'baz-bing':2}");
		assertMap(m, "foo-bar,baz-bing", "1,2");
		assertBean(m.getBean(), "fooBar,bazBING", "1,2");
		m.put("foo-bar", 3);
		m.put("baz-bing", 4);
		assertBean(m.getBean(), "fooBar,bazBING", "3,4");
	}

	@Bean(propertyNamer=PropertyNamerDLC.class)
	public static class P2 {
		private int fooBar;
		public int getFooBar() { return fooBar; }
		public void setFooBar(int v) { fooBar = v; }

		private int bazBING;
		public int getBazBING() { return bazBING; }
		public void setBazBING(int v) { bazBING = v; }
	}

	//====================================================================================================
	// testBeanWithFluentStyleSetters
	//====================================================================================================
	@Test void a21_beanWithFluentStyleSetters() {
		var t = new Q2();
		var m = BeanContext.DEFAULT_SORTED.toBeanMap(t);
		m.put("f1", 1);
		m.put("f2", 2);
		m.put("f3", 3);

		assertBean(t, "f1,f2,f3", "1,2,0");
	}

	public static class Q1 {}

	public static class Q2 extends Q1 {
		private int f1, f2, f3;

		public Q1 setF1(int v) { this.f1 = v; return this; }

		public Q2 setF2(int v) { this.f2 = v; return this; }

		// Shouldn't be detected as a setter.
		public String setF3(int v) { this.f3 = v; return null; }

		public int getF1() { return f1; }
		public int getF2() { return f2; }
		public int getF3() { return f3; }
	}

	//====================================================================================================
	// testCastWithNormalBean
	//====================================================================================================
	@Test void a22_castWithNormalBean() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "R2");
		m.put("f1", 1);
		m.put("f2", "2");

		var t = (R2)m.cast(Object.class);
		assertEquals(1, t.f1);

		t = (R2)m.cast(R1.class);
		assertBean(t, "f1,f2", "1,2");

		t = (R2)m.cast(bc.getClassMeta(R1.class));
		assertBean(t, "f1,f2", "1,2");

		// Without _type
		m = new JsonMap(session);
		m.put("f1", 1);
		m.put("f2", "2");

		m = (JsonMap)m.cast(Object.class);
		assertBean(t, "f1,f2", "1,2");

		t = m.cast(R2.class);
		assertBean(t, "f1,f2", "1,2");

		t = m.cast(bc.getClassMeta(R2.class));
		assertBean(t, "f1,f2", "1,2");
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
	@Test void a23_castWithNestedBean() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "S");
		m.put("f1", new JsonMap(session).append("_type", "R1").append("f1", 1));

		var t = (S)m.cast(Object.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(S.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(bc.getClassMeta(S.class));
		assertEquals(1, t.f1.f1);

		// Without _type
		m = new JsonMap(session);
		m.put("f1", new JsonMap(session).append("_type", R1.class.getName()).append("f1", 1));

		m = (JsonMap)m.cast(Object.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(S.class);
		assertEquals(1, t.f1.f1);

		t = m.cast(bc.getClassMeta(S.class));
		assertEquals(1, t.f1.f1);
	}

	public static class S {
		public R1 f1;
	}

	//====================================================================================================
	// testCastToAnotherMapType
	//====================================================================================================
	@Test void a24_castToAnotherMapType() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "TreeMap");
		m.put("1", "ONE");

		var m2 = (Map)m.cast(Object.class);
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
		var e = (Map.Entry)m2.entrySet().iterator().next();
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
		m = new JsonMap();
		m.put("1", "ONE");

		m2 = (JsonMap)m.cast(Object.class);
		assertTrue(m2 instanceof JsonMap);
		assertEquals("ONE", m2.get("1"));

		m2 = m.cast(Map.class);
		assertTrue(m2 instanceof JsonMap);
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

	public enum TEnum {
		ONE, TWO, THREE;
	}

	//====================================================================================================
	// testCastToLinkedList
	//====================================================================================================
	@Test void a25_castToLinkedList() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "LinkedList");
		m.put("items", JsonList.of("1","2"));

		var l = (List)m.cast(Object.class);
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
		m = new JsonMap();
		m.put("items", JsonList.of("1","2"));

		l = m.cast(List.class);
		assertTrue(l instanceof JsonList);
		assertEquals("1", l.get(0));

		l = m.cast(LinkedList.class);
		assertTrue(l instanceof LinkedList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof JsonList);
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
	@Test void a26_toLinkedListInteger() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "LinkedListOfInts");
		m.put("items", JsonList.of("1","2"));

		var l = (List)m.cast(Object.class);
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
		m = new JsonMap();
		m.put("items", JsonList.of("1", "2"));

		l = m.cast(List.class);
		assertTrue(l instanceof JsonList);
		assertEquals("1", l.get(0));

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof JsonList);
		assertEquals("1", l.get(0));

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertEquals("1", l.get(0));

		l = (List)m.cast(bc.getClassMeta(List.class, Integer.class));
		assertTrue(l instanceof JsonList);
		assertTrue(l.get(0) instanceof Integer);
		assertEquals(1, l.get(0));
	}

	//====================================================================================================
	// testCastToLinkedListBean - cast() to LinkedList<R1>
	//====================================================================================================
	@Test void a27_castToLinkedListBean() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "LinkedListOfR1");
		m.put("items", new JsonList(session).append("{f1:1}"));

		var l = (List)m.cast(Object.class);
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

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = (List)m.cast(bc.getClassMeta(List.class, HashMap.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof HashMap);
		assertEquals(1, ((Map)l.get(0)).get("f1"));

		// Without _type
		m = new JsonMap(session);
		m.put("items", new JsonList(session).append("{f1:1}"));

		l = m.cast(List.class);
		assertTrue(l instanceof JsonList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof JsonList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("{f1:1}", l.get(0));

		l = (List)m.cast(bc.getClassMeta(List.class, R1.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof R1);
		assertEquals(1, ((R1)l.get(0)).f1);

		l = (List)m.cast(bc.getClassMeta(List.class, HashMap.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof HashMap);
		assertEquals(1, ((Map)l.get(0)).get("f1"));

		l = (List)m.cast(bc.getClassMeta(List.class, Map.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof JsonMap);
		assertEquals(1, ((Map)l.get(0)).get("f1"));
	}

	//====================================================================================================
	// testCastToLinkedListUsingSwap - cast() to LinkedList<Calendar> using CalendarSwap
	//====================================================================================================
	@Test void a28_castToLinkedListUsingSwap() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "LinkedListOfCalendar");
		m.put("items", JsonList.ofJsonOrCdl("2001-07-04T15:30:45Z"));

		var l = (List)m.cast(Object.class);
		assertTrue(l instanceof LinkedList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = m.cast(List.class);
		assertTrue(l instanceof LinkedList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		m.cast(HashSet.class);

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));

		l = (List)m.cast(bc.getClassMeta(List.class, String.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof String);
		assertEquals("2001-07-04T15:30:45Z", l.get(0));

		// Without _type
		m = new JsonMap().session(bc.getSession());
		m.put("items", JsonList.ofJsonOrCdl("2001-07-04T15:30:45Z"));

		l = m.cast(List.class);
		assertTrue(l instanceof LinkedList);

		l = m.cast(ArrayList.class);
		assertTrue(l instanceof ArrayList);

		m.cast(HashSet.class);

		l = m.cast(bc.getClassMeta(List.class));
		assertTrue(l instanceof LinkedList);

		l = m.cast(bc.getClassMeta(ArrayList.class));
		assertTrue(l instanceof ArrayList);

		l = (List)m.cast(bc.getClassMeta(List.class, Calendar.class));
		assertTrue(l instanceof LinkedList);
		assertTrue(l.get(0) instanceof Calendar);
		assertEquals(2001, ((Calendar)l.get(0)).get(Calendar.YEAR));
	}

	//====================================================================================================
	// testCastToStringArray - cast() to String[]
	//====================================================================================================
	@Test void a29_castToStringArray() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "StringArray");
		m.put("items", JsonList.of("1","2"));

		var l = (String[])m.cast(Object.class);
		assertEquals("1", l[0]);

		l = m.cast(String[].class);
		assertEquals("1", l[0]);

		var l2 = m.cast(StringBuffer[].class);
		assertEquals("1", l2[0].toString());

		int[] l3 = m.cast(int[].class);
		assertEquals(1, l3[0]);

		l = m.cast(bc.getClassMeta(String[].class));
		assertEquals("1", l[0]);

		l2 = m.cast(bc.getClassMeta(StringBuffer[].class));
		assertEquals("1", l2[0].toString());

		l3 = m.cast(bc.getClassMeta(int[].class));
		assertEquals(1, l3[0]);

		// Without _type
		m = new JsonMap();
		m.put("items", JsonList.of("1","2"));

		l = m.cast(String[].class);
		assertEquals("1", l[0]);

		l = m.cast(bc.getClassMeta(String[].class));
		assertEquals("1", l[0]);

		l2 = m.cast(bc.getClassMeta(StringBuffer[].class));
		assertEquals("1", l2[0].toString());
	}

	//====================================================================================================
	// testCastToIntArray - cast() to int[]
	//====================================================================================================
	@Test void a30_castToIntArray() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "IntArray");
		m.put("items", JsonList.of("1","2"));

		var l = (int[])m.cast(Object.class);
		assertEquals(1, l[0]);

		l = m.cast(int[].class);
		assertEquals(1, l[0]);

		l = m.cast(bc.getClassMeta(int[].class));
		assertEquals(1, l[0]);

		var l2 = m.cast(long[].class);
		assertEquals(1, l2[0]);

		l2 = m.cast(bc.getClassMeta(long[].class));
		assertEquals(1, l2[0]);

		// Without _type
		m = new JsonMap();
		m.put("items", JsonList.of("1","2"));

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
	@Test void a31_castToString2dArray() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "String2dArray");
		m.put("items", JsonList.of(JsonList.ofJsonOrCdl("1"),JsonList.ofJsonOrCdl("2")));

		var l = (String[][])m.cast(Object.class);
		assertEquals("1", l[0][0]);
		assertEquals("2", l[1][0]);

		l = m.cast(String[][].class);
		assertEquals("1", l[0][0]);

		l = m.cast(bc.getClassMeta(String[][].class));
		assertEquals("2", l[1][0]);

		// Without _type
		m = new JsonMap();
		m.put("items", JsonList.of(JsonList.ofJsonOrCdl("1"),JsonList.ofJsonOrCdl("2")));

		l = m.cast(String[][].class);
		assertEquals("1", l[0][0]);

		l = m.cast(bc.getClassMeta(String[][].class));
		assertEquals("2", l[1][0]);
	}

	//====================================================================================================
	// testCastToInt2dArray - cast() to int[][]
	//====================================================================================================
	@Test void a32_castToInt2dArray() {

		// With _type
		var m = new JsonMap(session);
		m.put("_type", "Int2dArray");
		m.put("items", JsonList.of(JsonList.ofJsonOrCdl("1"),JsonList.ofJsonOrCdl("2")));

		var l = (int[][])m.cast(Object.class);
		assertEquals(1, l[0][0]);
		assertEquals(2, l[1][0]);

		l = m.cast(int[][].class);
		assertEquals(1, l[0][0]);

		l = m.cast(bc.getClassMeta(int[][].class));
		assertEquals(2, l[1][0]);

		// Without _type
		m = new JsonMap();
		m.put("items", JsonList.of(JsonList.ofJsonOrCdl("1"),JsonList.ofJsonOrCdl("2")));

		l = m.cast(int[][].class);
		assertEquals(1, l[0][0]);

		l = m.cast(bc.getClassMeta(int[][].class));
		assertEquals(2, l[1][0]);
	}

	//====================================================================================================
	// testHiddenProperties
	//====================================================================================================
	@Test void a33_hiddenProperties() {
		var s = Json5Serializer.DEFAULT;
		var bm = s.getBeanContext().getBeanMeta(U.class);
		assertNotNull(bm.getPropertyMeta("a"));
		assertNotNull(bm.getPropertyMeta("b"));
		assertNull(bm.getPropertyMeta("c"));
		assertNull(bm.getPropertyMeta("d"));

		var t = new U();
		t.a = "a";
		t.b = "b";
		var r = s.serialize(t);
		assertEquals("{a:'a',b:'b'}", r);

		// Make sure setters are used if present.
		t = JsonParser.DEFAULT.parse(r, U.class);
		assertEquals("b(setter)", t.b);
	}

	public static class U {
		public String a;
		public String getA() { return a; }
		public void setA(String v) { a = v; }

		public String b;
		@BeanIgnore public String getB() { return b; }
		public void setB(String v) { this.b = v+"(setter)"; }

		@BeanIgnore public String c;

		@BeanIgnore public String getD() { return null; }
		@BeanIgnore public void setD(String v) {}  // NOSONAR
	}

	@Test void a34_hiddenProperties_usingConfig() {
		var s = Json5Serializer.DEFAULT.copy().applyAnnotations(UcConfig.class).build();
		var bm = s.getBeanContext().getBeanMeta(U.class);
		assertNotNull(bm.getPropertyMeta("a"));
		assertNotNull(bm.getPropertyMeta("b"));
		assertNull(bm.getPropertyMeta("c"));
		assertNull(bm.getPropertyMeta("d"));

		var t = new Uc();
		t.a = "a";
		t.b = "b";
		var r = s.serialize(t);
		assertEquals("{a:'a',b:'b'}", r);

		// Make sure setters are used if present.
		t = JsonParser.DEFAULT.copy().applyAnnotations(Uc.class).build().parse(r, Uc.class);
		assertEquals("b(setter)", t.b);
	}

	@BeanIgnore(on="Dummy1")
	@BeanIgnore(on="Uc.getB,Uc.c,Uc.getD,Uc.setD")
	@BeanIgnore(on="Dummy2")
	private static class UcConfig {}

	public static class Uc {
		public String a;
		public String getA() { return a; }
		public void setA(String v) { a = v; }

		public String b;
		@BeanIgnore public String getB() { return b; }
		public void setB(String b) { this.b = b+"(setter)"; }

		@BeanIgnore public String c;

		@BeanIgnore public String getD() { return null; }
		@BeanIgnore public void setD(String v) {}  // NOSONAR
	}

	//====================================================================================================
	// testBeanPropertyOrder
	//====================================================================================================
	@Test void a35_beanPropertyOrder() {
		assertJson(new V2(), "{a1:'1',a2:'2',a3:'3',a4:'4'}");
		assertJson(new V3(), "{a3:'3',a4:'4',a5:'5',a6:'6'}");
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

	@Test void a36_beanPropertyOrder_usingConfig() {
		var ws = Json5Serializer.create().applyAnnotations(VcConfig.class).build();
		assertEquals("{a1:'1',a2:'2',a3:'3',a4:'4'}", ws.toString(new V2c()));
		assertEquals("{a3:'3',a4:'4',a5:'5',a6:'6'}", ws.toString(new V3c()));
	}

	@Bean(on="Dummy1", stopClass=Vc.class)
	@Bean(on="V3c", stopClass=Vc.class)
	@Bean(on="Dummy2", stopClass=Vc.class)
	private static class VcConfig {}

	public static class Vc {
		public String a1="1", a2="2";
	}

	public static class V2c extends Vc {
		public String a3="3", a4="4";
	}

	public static class V3c extends V2c {
		public String a5="5", a6="6";
	}

	//====================================================================================================
	// testBeanMethodOrder
	//====================================================================================================
	@Test void a37_beanMethodOrder() {
		assertJson(new W2(), "{a1:'1',a2:'2',a3:'3',a4:'4'}");
		assertJson(new W3(), "{a3:'3',a4:'4',a5:'5',a6:'6'}");
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
	@Test void a38_overriddenPropertyTypes() {
		var s = Json5Serializer.DEFAULT;
		var p = JsonParser.DEFAULT;

		var t1 = X1.create();
		var r = s.serialize(t1);
		assertEquals("{f1:'1',f2:'2'}", r);
		t1 = p.parse(r, X1.class);
		assertEquals("1", t1.f1);
		assertEquals("2", t1.getF2());

		var t2 = X2.create();
		r = s.serialize(t2);
		assertEquals("{f1:1,f2:2}", r);
		t2 = p.parse(r, X2.class);
		assertEquals(1, t2.f1.intValue());
		assertEquals(2, t2.getF2().intValue());
	}

	public static class X1 {
		public Object f1;

		static X1 create() {
			var x = new X1();
			x.f1 = "1";
			x.f2 = "2";
			return x;
		}

		private Object f2;
		public Object getF2() { return f2; }
		public void setF2(Object v) { f2 = v; }
	}

	public static class X2 extends X1 {
		public Integer f1;

		static X2 create() {
			var x = new X2();
			x.f1 = 1;
			x.f2 = 2;
			return x;
		}

		private Integer f2;
		@Override /* X1 */ public Integer getF2() { return f2; }
		public void setF2(Integer v) { f2 = v; }
	}

	@Test void a39_settingCollectionPropertyMultipleTimes() {
		var m = BeanContext.DEFAULT.newBeanMap(Y.class);
		m.put("f1", JsonList.ofJsonOrCdl("a"));
		m.put("f1",  JsonList.ofJsonOrCdl("b"));
		assertEquals("{f1=[b]}", m.toString());
	}

	public static class Y {
		public List<String> f1 = new LinkedList<>();
	}

	//====================================================================================================
	// testCollectionSetters_preferSetter
	//====================================================================================================
	@Test void a40_collectionSetters_preferSetter() {
		var aa = new AA();
		var bm = BeanContext.DEFAULT.toBeanMap(aa);

		bm.put("a", alist("x"));
		assertJson(aa.a, "['x']");
	}

	public static class AA {
		private List<String> a = new ArrayList<>();
		public List<String> getA() { return Collections.emptyList(); }
		public void setA(List<String> v) { a = v; }
	}
}