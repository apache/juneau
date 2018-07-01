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

import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.utils.*;
import org.junit.*;

public class ObjectMapTest {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		String in;

		in = "{A:'asdf'}";
		checkStep(1, in, new ObjectMap(in).getString("A"), "asdf");

		in = "{A:{B:'asdf'}}";
		checkStep(2, in, getDeepString(new ObjectMap(in), "A/B"), "asdf");
		checkStep(3, in, new ObjectMap(in).getString("A"), "{B:'asdf'}");

		in = "{A:{B:'asdf'+\"asdf\"}}";
		checkStep(4, in, getDeepString(new ObjectMap(in), "A/B"), "asdfasdf");
		checkStep(5, in, new ObjectMap(in).getString("A"), "{B:'asdfasdf'}");

		in = "{A:{B:'asdf' + \n\t \"asdf\"}}";
		checkStep(6, in, getDeepString(new ObjectMap(in), "A/B"), "asdfasdf");
		checkStep(7, in, new ObjectMap(in).getString("A"), "{B:'asdfasdf'}");

		in = "{A:{B:'asdf\"asdf', C:\"asdf'asdf\", D : \"asdf\\\"asdf\", E: 'asdf\\\'asdf', F:\"asdf\\\'asdf\", G:'asdf\\\"asdf'}}";
		checkStep(8, in, getDeepString(new ObjectMap(in), "A/B"), "asdf\"asdf");
		checkStep(9, in, getDeepString(new ObjectMap(in), "A/C"), "asdf'asdf");
		checkStep(10, in, getDeepString(new ObjectMap(in), "A/D"), "asdf\"asdf");
		checkStep(11, in, getDeepString(new ObjectMap(in), "A/E"), "asdf'asdf");
		checkStep(12, in, getDeepString(new ObjectMap(in), "A/F"), "asdf'asdf");
		checkStep(13, in, getDeepString(new ObjectMap(in), "A/G"), "asdf\"asdf");

		in = "{A:123, B: 123}";
		checkStep(16, in, new Integer(new ObjectMap(in).getInt("A")).toString(), "123");
		checkStep(17, in, new Integer(new ObjectMap(in).getInt("B")).toString(), "123");

		in = "{A:true, B: true, C:false, D: false}";
		checkStep(18, in, new Boolean(new ObjectMap(in).getBoolean("A")).toString(), "true");
		checkStep(19, in, new Boolean(new ObjectMap(in).getBoolean("B")).toString(), "true");
		checkStep(20, in, new Boolean(new ObjectMap(in).getBoolean("C")).toString(), "false");
		checkStep(21, in, new Boolean(new ObjectMap(in).getBoolean("D")).toString(), "false");

		in = "{'AAA':{\"BBB\":\"CCC\",'DDD':false}}";
		checkStep(31, in, getDeepString(new ObjectMap(in), "AAA/BBB"), "CCC");
		checkStep(32, in, getDeepBoolean(new ObjectMap(in), "AAA/DDD").toString(), "false");

		in = " \n\n\t {  'AAA' : { \"BBB\" : \"CCC\" , 'DDD' : false } } \n\t";
		checkStep(33, in, getDeepString(new ObjectMap(in), "AAA/BBB"), "CCC");
		checkStep(34, in, getDeepBoolean(new ObjectMap(in), "AAA/DDD").toString(), "false");

		in = "/*x*/{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(100, in, new ObjectMap(in).getString("A"), "B");
		in = "{/*x*/A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(101, in, new ObjectMap(in).getString("A"), "B");
		in = "{A/*x*/:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(102, in, new ObjectMap(in).getString("A"), "B");
		in = "{A:/*x*/'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(103, in, new ObjectMap(in).getString("A"), "B");
		in = "{A:'/*x*/B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(104, in, new ObjectMap(in).getString("A"), "/*x*/B");
		in = "{A:'B/*x*/','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(105, in, new ObjectMap(in).getString("A"), "B/*x*/");
		in = "{A:'B'/*x*/,'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(106, in, new ObjectMap(in).getString("A"), "B");
		in = "{A:'B',/*x*/'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(107, in, new ObjectMap(in).getString("C"), "1");
		in = "{A:'B','C':/*x*/1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(108, in, new ObjectMap(in).getString("C"), "1");
		in = "{A:'B','C':1/*x*/,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(109, in, new ObjectMap(in).getString("C"), "1");
		in = "{A:'B','C':1,/*x*/\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(110, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"/*x*/E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(111, in, new ObjectMap(in).getObjectList("/*x*/E").getString(0), "1");
		in = "{A:'B','C':1,\"E/*x*/\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(112, in, new ObjectMap(in).getObjectList("E/*x*/").getString(0), "1");
		in = "{A:'B','C':1,\"E\"/*x*/:[1,2,3],G:['g1','g2','g3']}";
		checkStep(113, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":/*x*/[1,2,3],G:['g1','g2','g3']}";
		checkStep(114, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[/*x*/1,2,3],G:['g1','g2','g3']}";
		checkStep(115, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1/*x*/,2,3],G:['g1','g2','g3']}";
		checkStep(116, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1,/*x*/2,3],G:['g1','g2','g3']}";
		checkStep(117, in, new ObjectMap(in).getObjectList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2/*x*/,3],G:['g1','g2','g3']}";
		checkStep(118, in, new ObjectMap(in).getObjectList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2,/*x*/3],G:['g1','g2','g3']}";
		checkStep(119, in, new ObjectMap(in).getObjectList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3]/*x*/,G:['g1','g2','g3']}";
		checkStep(120, in, new ObjectMap(in).getObjectList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3],/*x*/G:['g1','g2','g3']}";
		checkStep(121, in, new ObjectMap(in).getObjectList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:[/*x*/'g1','g2','g3']}";
		checkStep(122, in, new ObjectMap(in).getObjectList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['/*x*/g1','g2','g3']}";
		checkStep(123, in, new ObjectMap(in).getObjectList("G").getString(0), "/*x*/g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1'/*x*/,'g2','g3']}";
		checkStep(124, in, new ObjectMap(in).getObjectList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1',/*x*/'g2','g3']}";
		checkStep(125, in, new ObjectMap(in).getObjectList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2'/*x*/,'g3']}";
		checkStep(126, in, new ObjectMap(in).getObjectList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2',/*x*/'g3']}";
		checkStep(127, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3'/*x*/]}";
		checkStep(128, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']/*x*/}";
		checkStep(129, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}/*x*/";
		checkStep(130, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");

		in = "/*\tx\t*///\tx\t\n\t/*\tx\t*/{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(201, in, new ObjectMap(in).getString("A"), "B");
		in = "{/*\tx\t*///\tx\t\n\t/*\tx\t*/A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(202, in, new ObjectMap(in).getString("A"), "B");
		in = "{A/*\tx\t*///\tx\t\n\t/*\tx\t*/:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(203, in, new ObjectMap(in).getString("A"), "B");
		in = "{A:/*\tx\t*///\tx\t\n\t/*\tx\t*/'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(204, in, new ObjectMap(in).getString("A"), "B");
		in = "{A:'/*\tx\t*///\tx\t\n\t/*\tx\t*/B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(205, in, new ObjectMap(in).getString("A"), "/*\tx\t*///\tx\t\n\t/*\tx\t*/B");
		in = "{A:'B/*\tx\t*///\tx\t\n\t/*\tx\t*/','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(206, in, new ObjectMap(in).getString("A"), "B/*\tx\t*///\tx\t\n\t/*\tx\t*/");
		in = "{A:'B'/*\tx\t*///\tx\t\n\t/*\tx\t*/,'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(207, in, new ObjectMap(in).getString("A"), "B");
		in = "{A:'B',/*\tx\t*///\tx\t\n\t/*\tx\t*/'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(208, in, new ObjectMap(in).getString("C"), "1");
		in = "{A:'B','C':/*\tx\t*///\tx\t\n\t/*\tx\t*/1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(209, in, new ObjectMap(in).getString("C"), "1");
		in = "{A:'B','C':1/*\tx\t*///\tx\t\n\t/*\tx\t*/,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(210, in, new ObjectMap(in).getString("C"), "1");
		in = "{A:'B','C':1,/*\tx\t*///\tx\t\n\t/*\tx\t*/\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(211, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"/*\tx\t*///\tx\t\n\t/*\tx\t*/E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(212, in, new ObjectMap(in).getObjectList("/*\tx\t*///\tx\t\n\t/*\tx\t*/E").getString(0), "1");
		in = "{A:'B','C':1,\"E/*\tx\t*///\tx\t\n\t/*\tx\t*/\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(213, in, new ObjectMap(in).getObjectList("E/*\tx\t*///\tx\t\n\t/*\tx\t*/").getString(0), "1");
		in = "{A:'B','C':1,\"E\"/*\tx\t*///\tx\t\n\t/*\tx\t*/:[1,2,3],G:['g1','g2','g3']}";
		checkStep(214, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":/*\tx\t*///\tx\t\n\t/*\tx\t*/[1,2,3],G:['g1','g2','g3']}";
		checkStep(215, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[/*\tx\t*///\tx\t\n\t/*\tx\t*/1,2,3],G:['g1','g2','g3']}";
		checkStep(216, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1/*\tx\t*///\tx\t\n\t/*\tx\t*/,2,3],G:['g1','g2','g3']}";
		checkStep(217, in, new ObjectMap(in).getObjectList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1,/*\tx\t*///\tx\t\n\t/*\tx\t*/2,3],G:['g1','g2','g3']}";
		checkStep(218, in, new ObjectMap(in).getObjectList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2/*\tx\t*///\tx\t\n\t/*\tx\t*/,3],G:['g1','g2','g3']}";
		checkStep(219, in, new ObjectMap(in).getObjectList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2,/*\tx\t*///\tx\t\n\t/*\tx\t*/3],G:['g1','g2','g3']}";
		checkStep(220, in, new ObjectMap(in).getObjectList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3]/*\tx\t*///\tx\t\n\t/*\tx\t*/,G:['g1','g2','g3']}";
		checkStep(221, in, new ObjectMap(in).getObjectList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3],/*\tx\t*///\tx\t\n\t/*\tx\t*/G:['g1','g2','g3']}";
		checkStep(222, in, new ObjectMap(in).getObjectList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:[/*\tx\t*///\tx\t\n\t/*\tx\t*/'g1','g2','g3']}";
		checkStep(223, in, new ObjectMap(in).getObjectList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['/*\tx\t*///\tx\t\n\t/*\tx\t*/g1','g2','g3']}";
		checkStep(224, in, new ObjectMap(in).getObjectList("G").getString(0), "/*\tx\t*///\tx\t\n\t/*\tx\t*/g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1'/*\tx\t*///\tx\t\n\t/*\tx\t*/,'g2','g3']}";
		checkStep(225, in, new ObjectMap(in).getObjectList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1',/*\tx\t*///\tx\t\n\t/*\tx\t*/'g2','g3']}";
		checkStep(226, in, new ObjectMap(in).getObjectList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2'/*\tx\t*///\tx\t\n\t/*\tx\t*/,'g3']}";
		checkStep(227, in, new ObjectMap(in).getObjectList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2',/*\tx\t*///\tx\t\n\t/*\tx\t*/'g3']}";
		checkStep(228, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3'/*\tx\t*///\tx\t\n\t/*\tx\t*/]}";
		checkStep(229, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']/*\tx\t*///\tx\t\n\t/*\tx\t*/}";
		checkStep(230, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}/*\tx\t*///\tx\t\n\t/*\tx\t*/";
		checkStep(231, in, new ObjectMap(in).getObjectList("G").getString(2), "g3");

		in = "{  /*  x  */  //  x  \n  /*  x  */  A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(240, in, new ObjectMap(in).getString("A"), "B");

		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(301, in, new ObjectMap(in).getString("A", "default"), "B");
		in = "{/*A:'B',*/'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(302, in, new ObjectMap(in).getString("A", "default"), "default");
		in = "{A:'B',/*'C':1,*/\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(303, in, new ObjectMap(in).getString("C", "default"), "default");
		in = "{A:'B','C':1,/*\"E\":[1,2,3],*/G:['g1','g2','g3']}";
		checkStep(304, in, new ObjectMap(in).getString("E", "default"), "default");
		in = "{A:'B','C':1,\"E\":[/*1,*/2,3],G:['g1','g2','g3']}";
		checkStep(305, in, new ObjectMap(in).getObjectList("E").getString(0), "2");
		in = "{A:'B','C':1,\"E\":[1,/*2,*/3],G:['g1','g2','g3']}";
		checkStep(306, in, new ObjectMap(in).getObjectList("E").getString(1), "3");
		in = "{A:'B','C':1,\"E\":[1,2/*,3*/],G:['g1','g2','g3']}";
		checkStep(307, in, new ObjectMap(in).getObjectList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:[/*'g1',*/'g2','g3']}";
		checkStep(308, in, new ObjectMap(in).getObjectList("G").getString(0), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1'/*,'g2'*/,'g3']}";
		checkStep(309, in, new ObjectMap(in).getObjectList("G").getString(1), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2'/*,'g3'*/]}";
		checkStep(310, in, new ObjectMap(in).getObjectList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(310, in, new ObjectMap(in).getObjectList("G").getString(1), "g2");

		// Check keys that contain array indexes
		in = "{A:{B:[{C:'c0'},{C:'c1'},{C:'c2'}]}}";
		checkStep(401, in, getDeepString(new ObjectMap(in), "A/B/0/C"), "c0");
		checkStep(402, in, getDeepString(new ObjectMap(in), "A/B/1/C"), "c1");
		checkStep(403, in, getDeepString(new ObjectMap(in), "A/B/2/C"), "c2");

		// Check extended unicode characters.
		in = "{'𤭢𤭢':'𤭢𤭢'}";
		checkStep(1, in, new ObjectMap(in).getString("𤭢𤭢"), "𤭢𤭢");
	}

	private String getDeepString(ObjectMap m, String url) {
		PojoRest r = new PojoRest(m);
		return (String)r.get(url);
	}

	private Boolean getDeepBoolean(ObjectMap m, String url) {
		PojoRest r = new PojoRest(m);
		return (Boolean)r.get(url);
	}

	private void checkStep(int step, String input, String output, String expectedValue) {
		if (!output.equals(expectedValue)) {
			String msg = "Step #" + step + " failed: [" + input + "]->[" + output + "]...Expected value=[" + expectedValue + "]";
			fail(msg);
		}
	}

	//====================================================================================================
	// testComparison
	//====================================================================================================
	@Test
	public void testComparison() throws Exception {
		ObjectMap m1 = new ObjectMap("{ firstName:'John', lastName:'Smith', age:123, isDeceased:false }");
		ObjectMap m2 = new ObjectMap("{ age:123, isDeceased:false, lastName:'Smith', firstName:'John' }");

		assertTrue(m1.equals(m2));
	}

	//====================================================================================================
	// testParent
	//====================================================================================================
	@Test
	public void testParent() throws Exception {
		ObjectMap m1 = new ObjectMap("{a:1}");
		ObjectMap m2 = new ObjectMap("{b:2}").setInner(m1);

		assertEquals(new Integer(1), m2.getInt("a"));
	}

	//====================================================================================================
	// testUpdatability
	//====================================================================================================
	@Test
	public void testUpdatability() throws Exception {
		ObjectMap m = new ObjectMap("{a:[{b:'c'}]}");
		ObjectList l = m.getObjectList("a");
		ObjectMap m2 = l.getObjectMap(0);
		m2.put("b", "x");
		assertObjectEquals("{a:[{b:'x'}]}", m);

		m = new ObjectMap("{a:[{b:'c'}]}");
		for (ObjectMap m3 : m.getObjectList("a").elements(ObjectMap.class))
			m3.put("b", "y");

		assertObjectEquals("{a:[{b:'y'}]}", m);
	}

	//====================================================================================================
	// testAtMethods
	//====================================================================================================
	@Test
	public void testAtMethods() throws Exception {
		ObjectMap m = new ObjectMap("{a:[{b:'c'}]}");
		String r;

		r = m.getAt("a/0/b", String.class);
		assertEquals("c", r);

		m.putAt("a/0/b", "d");
		r = m.getAt("a/0/b", String.class);
		assertEquals("d", r);

		m.postAt("a", "e");
		r = m.getAt("a/1", String.class);
		assertEquals("e", r);

		m.deleteAt("a/1");
		assertEquals("{a:[{b:'d'}]}", m.toString());
	}

	//====================================================================================================
	// ObjectMap(Reader)
	//====================================================================================================
	@Test
	public void testFromReader() throws Exception {
		assertObjectEquals("{foo:'bar'}", new ObjectMap(new StringReader("{foo:'bar'}")));
	}

	//====================================================================================================
	// testGetMap
	//====================================================================================================
	@Test
	public void testGetMap() throws Exception {
		ObjectMap m = new ObjectMap("{a:{1:'true',2:'false'}}");
		Map<Integer,Boolean> m2 = m.getMap("a", Integer.class, Boolean.class, null);
		assertObjectEquals("{'1':true,'2':false}", m2);
		assertEquals(Integer.class, m2.keySet().iterator().next().getClass());
		assertEquals(Boolean.class, m2.values().iterator().next().getClass());

		m2 = m.getMap("b", Integer.class, Boolean.class, null);
		assertNull(m2);

		m2 = m.get("a", Map.class, Integer.class, Boolean.class);
		assertObjectEquals("{'1':true,'2':false}", m2);
		assertEquals(Integer.class, m2.keySet().iterator().next().getClass());
		assertEquals(Boolean.class, m2.values().iterator().next().getClass());

		m2 = m.get("b", Map.class, Integer.class, Boolean.class);
		assertNull(m2);
	}

	//====================================================================================================
	// testGetList
	//====================================================================================================
	@Test
	public void testGetList() throws Exception {
		ObjectMap m = new ObjectMap("{a:['123','456']}");
		List<Integer> l2 = m.getList("a", Integer.class, null);
		assertObjectEquals("[123,456]", l2);
		assertEquals(Integer.class, l2.iterator().next().getClass());

		l2 = m.getList("b", Integer.class, null);
		assertNull(l2);

		l2 = m.get("a", List.class, Integer.class);
		assertObjectEquals("[123,456]", l2);
		assertEquals(Integer.class, l2.iterator().next().getClass());

		l2 = m.get("b", List.class, Integer.class);
		assertNull(l2);
	}
}