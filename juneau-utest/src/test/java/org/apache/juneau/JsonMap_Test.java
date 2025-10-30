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
package org.apache.juneau;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.collections.*;
import org.apache.juneau.objecttools.*;
import org.junit.jupiter.api.*;

class JsonMap_Test extends TestBase {

	//====================================================================================================
	// testBasic
	//====================================================================================================
	@Test void a01_basic() throws Exception {
		var in = "{A:'asdf'}";

		checkStep(1, in, JsonMap.ofJson(in).getString("A"), "asdf");

		in = "{A:{B:'asdf'}}";
		checkStep(2, in, getDeepString(JsonMap.ofJson(in), "A/B"), "asdf");
		checkStep(3, in, JsonMap.ofJson(in).getString("A"), "{B:'asdf'}");

		in = "{A:{B:'asdf'+\"asdf\"}}";
		checkStep(4, in, getDeepString(JsonMap.ofJson(in), "A/B"), "asdfasdf");
		checkStep(5, in, JsonMap.ofJson(in).getString("A"), "{B:'asdfasdf'}");

		in = "{A:{B:'asdf' + \n\t \"asdf\"}}";
		checkStep(6, in, getDeepString(JsonMap.ofJson(in), "A/B"), "asdfasdf");
		checkStep(7, in, JsonMap.ofJson(in).getString("A"), "{B:'asdfasdf'}");

		in = "{A:{B:'asdf\"asdf', C:\"asdf'asdf\", D : \"asdf\\\"asdf\", E: 'asdf\\\'asdf', F:\"asdf\\\'asdf\", G:'asdf\\\"asdf'}}";
		checkStep(8, in, getDeepString(JsonMap.ofJson(in), "A/B"), "asdf\"asdf");
		checkStep(9, in, getDeepString(JsonMap.ofJson(in), "A/C"), "asdf'asdf");
		checkStep(10, in, getDeepString(JsonMap.ofJson(in), "A/D"), "asdf\"asdf");
		checkStep(11, in, getDeepString(JsonMap.ofJson(in), "A/E"), "asdf'asdf");
		checkStep(12, in, getDeepString(JsonMap.ofJson(in), "A/F"), "asdf'asdf");
		checkStep(13, in, getDeepString(JsonMap.ofJson(in), "A/G"), "asdf\"asdf");

		in = "{A:123, B: 123}";
		checkStep(16, in, JsonMap.ofJson(in).getInt("A").toString(), "123");
		checkStep(17, in, JsonMap.ofJson(in).getInt("B").toString(), "123");

		in = "{A:true, B: true, C:false, D: false}";
		checkStep(18, in, JsonMap.ofJson(in).getBoolean("A").toString(), "true");
		checkStep(19, in, JsonMap.ofJson(in).getBoolean("B").toString(), "true");
		checkStep(20, in, JsonMap.ofJson(in).getBoolean("C").toString(), "false");
		checkStep(21, in, JsonMap.ofJson(in).getBoolean("D").toString(), "false");

		in = "{'AAA':{\"BBB\":\"CCC\",'DDD':false}}";
		checkStep(31, in, getDeepString(JsonMap.ofJson(in), "AAA/BBB"), "CCC");
		checkStep(32, in, getDeepBoolean(JsonMap.ofJson(in), "AAA/DDD").toString(), "false");

		in = " \n\n\t {  'AAA' : { \"BBB\" : \"CCC\" , 'DDD' : false } } \n\t";
		checkStep(33, in, getDeepString(JsonMap.ofJson(in), "AAA/BBB"), "CCC");
		checkStep(34, in, getDeepBoolean(JsonMap.ofJson(in), "AAA/DDD").toString(), "false");

		in = "/*x*/{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(100, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{/*x*/A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(101, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A/*x*/:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(102, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A:/*x*/'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(103, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A:'/*x*/B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(104, in, JsonMap.ofJson(in).getString("A"), "/*x*/B");
		in = "{A:'B/*x*/','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(105, in, JsonMap.ofJson(in).getString("A"), "B/*x*/");
		in = "{A:'B'/*x*/,'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(106, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A:'B',/*x*/'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(107, in, JsonMap.ofJson(in).getString("C"), "1");
		in = "{A:'B','C':/*x*/1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(108, in, JsonMap.ofJson(in).getString("C"), "1");
		in = "{A:'B','C':1/*x*/,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(109, in, JsonMap.ofJson(in).getString("C"), "1");
		in = "{A:'B','C':1,/*x*/\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(110, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"/*x*/E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(111, in, JsonMap.ofJson(in).getList("/*x*/E").getString(0), "1");
		in = "{A:'B','C':1,\"E/*x*/\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(112, in, JsonMap.ofJson(in).getList("E/*x*/").getString(0), "1");
		in = "{A:'B','C':1,\"E\"/*x*/:[1,2,3],G:['g1','g2','g3']}";
		checkStep(113, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":/*x*/[1,2,3],G:['g1','g2','g3']}";
		checkStep(114, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[/*x*/1,2,3],G:['g1','g2','g3']}";
		checkStep(115, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1/*x*/,2,3],G:['g1','g2','g3']}";
		checkStep(116, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1,/*x*/2,3],G:['g1','g2','g3']}";
		checkStep(117, in, JsonMap.ofJson(in).getList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2/*x*/,3],G:['g1','g2','g3']}";
		checkStep(118, in, JsonMap.ofJson(in).getList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2,/*x*/3],G:['g1','g2','g3']}";
		checkStep(119, in, JsonMap.ofJson(in).getList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3]/*x*/,G:['g1','g2','g3']}";
		checkStep(120, in, JsonMap.ofJson(in).getList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3],/*x*/G:['g1','g2','g3']}";
		checkStep(121, in, JsonMap.ofJson(in).getList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:[/*x*/'g1','g2','g3']}";
		checkStep(122, in, JsonMap.ofJson(in).getList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['/*x*/g1','g2','g3']}";
		checkStep(123, in, JsonMap.ofJson(in).getList("G").getString(0), "/*x*/g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1'/*x*/,'g2','g3']}";
		checkStep(124, in, JsonMap.ofJson(in).getList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1',/*x*/'g2','g3']}";
		checkStep(125, in, JsonMap.ofJson(in).getList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2'/*x*/,'g3']}";
		checkStep(126, in, JsonMap.ofJson(in).getList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2',/*x*/'g3']}";
		checkStep(127, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3'/*x*/]}";
		checkStep(128, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']/*x*/}";
		checkStep(129, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}/*x*/";
		checkStep(130, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");

		in = "/*\tx\t*///\tx\t\n\t/*\tx\t*/{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(201, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{/*\tx\t*///\tx\t\n\t/*\tx\t*/A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(202, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A/*\tx\t*///\tx\t\n\t/*\tx\t*/:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(203, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A:/*\tx\t*///\tx\t\n\t/*\tx\t*/'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(204, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A:'/*\tx\t*///\tx\t\n\t/*\tx\t*/B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(205, in, JsonMap.ofJson(in).getString("A"), "/*\tx\t*///\tx\t\n\t/*\tx\t*/B");
		in = "{A:'B/*\tx\t*///\tx\t\n\t/*\tx\t*/','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(206, in, JsonMap.ofJson(in).getString("A"), "B/*\tx\t*///\tx\t\n\t/*\tx\t*/");
		in = "{A:'B'/*\tx\t*///\tx\t\n\t/*\tx\t*/,'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(207, in, JsonMap.ofJson(in).getString("A"), "B");
		in = "{A:'B',/*\tx\t*///\tx\t\n\t/*\tx\t*/'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(208, in, JsonMap.ofJson(in).getString("C"), "1");
		in = "{A:'B','C':/*\tx\t*///\tx\t\n\t/*\tx\t*/1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(209, in, JsonMap.ofJson(in).getString("C"), "1");
		in = "{A:'B','C':1/*\tx\t*///\tx\t\n\t/*\tx\t*/,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(210, in, JsonMap.ofJson(in).getString("C"), "1");
		in = "{A:'B','C':1,/*\tx\t*///\tx\t\n\t/*\tx\t*/\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(211, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"/*\tx\t*///\tx\t\n\t/*\tx\t*/E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(212, in, JsonMap.ofJson(in).getList("/*\tx\t*///\tx\t\n\t/*\tx\t*/E").getString(0), "1");
		in = "{A:'B','C':1,\"E/*\tx\t*///\tx\t\n\t/*\tx\t*/\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(213, in, JsonMap.ofJson(in).getList("E/*\tx\t*///\tx\t\n\t/*\tx\t*/").getString(0), "1");
		in = "{A:'B','C':1,\"E\"/*\tx\t*///\tx\t\n\t/*\tx\t*/:[1,2,3],G:['g1','g2','g3']}";
		checkStep(214, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":/*\tx\t*///\tx\t\n\t/*\tx\t*/[1,2,3],G:['g1','g2','g3']}";
		checkStep(215, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[/*\tx\t*///\tx\t\n\t/*\tx\t*/1,2,3],G:['g1','g2','g3']}";
		checkStep(216, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1/*\tx\t*///\tx\t\n\t/*\tx\t*/,2,3],G:['g1','g2','g3']}";
		checkStep(217, in, JsonMap.ofJson(in).getList("E").getString(0), "1");
		in = "{A:'B','C':1,\"E\":[1,/*\tx\t*///\tx\t\n\t/*\tx\t*/2,3],G:['g1','g2','g3']}";
		checkStep(218, in, JsonMap.ofJson(in).getList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2/*\tx\t*///\tx\t\n\t/*\tx\t*/,3],G:['g1','g2','g3']}";
		checkStep(219, in, JsonMap.ofJson(in).getList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2,/*\tx\t*///\tx\t\n\t/*\tx\t*/3],G:['g1','g2','g3']}";
		checkStep(220, in, JsonMap.ofJson(in).getList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3]/*\tx\t*///\tx\t\n\t/*\tx\t*/,G:['g1','g2','g3']}";
		checkStep(221, in, JsonMap.ofJson(in).getList("E").getString(2), "3");
		in = "{A:'B','C':1,\"E\":[1,2,3],/*\tx\t*///\tx\t\n\t/*\tx\t*/G:['g1','g2','g3']}";
		checkStep(222, in, JsonMap.ofJson(in).getList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:[/*\tx\t*///\tx\t\n\t/*\tx\t*/'g1','g2','g3']}";
		checkStep(223, in, JsonMap.ofJson(in).getList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['/*\tx\t*///\tx\t\n\t/*\tx\t*/g1','g2','g3']}";
		checkStep(224, in, JsonMap.ofJson(in).getList("G").getString(0), "/*\tx\t*///\tx\t\n\t/*\tx\t*/g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1'/*\tx\t*///\tx\t\n\t/*\tx\t*/,'g2','g3']}";
		checkStep(225, in, JsonMap.ofJson(in).getList("G").getString(0), "g1");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1',/*\tx\t*///\tx\t\n\t/*\tx\t*/'g2','g3']}";
		checkStep(226, in, JsonMap.ofJson(in).getList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2'/*\tx\t*///\tx\t\n\t/*\tx\t*/,'g3']}";
		checkStep(227, in, JsonMap.ofJson(in).getList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2',/*\tx\t*///\tx\t\n\t/*\tx\t*/'g3']}";
		checkStep(228, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3'/*\tx\t*///\tx\t\n\t/*\tx\t*/]}";
		checkStep(229, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']/*\tx\t*///\tx\t\n\t/*\tx\t*/}";
		checkStep(230, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}/*\tx\t*///\tx\t\n\t/*\tx\t*/";
		checkStep(231, in, JsonMap.ofJson(in).getList("G").getString(2), "g3");

		in = "{  /*  x  */  //  x  \n  /*  x  */  A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(240, in, JsonMap.ofJson(in).getString("A"), "B");

		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(301, in, JsonMap.ofJson(in).getString("A", "default"), "B");
		in = "{/*A:'B',*/'C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(302, in, JsonMap.ofJson(in).getString("A", "default"), "default");
		in = "{A:'B',/*'C':1,*/\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(303, in, JsonMap.ofJson(in).getString("C", "default"), "default");
		in = "{A:'B','C':1,/*\"E\":[1,2,3],*/G:['g1','g2','g3']}";
		checkStep(304, in, JsonMap.ofJson(in).getString("E", "default"), "default");
		in = "{A:'B','C':1,\"E\":[/*1,*/2,3],G:['g1','g2','g3']}";
		checkStep(305, in, JsonMap.ofJson(in).getList("E").getString(0), "2");
		in = "{A:'B','C':1,\"E\":[1,/*2,*/3],G:['g1','g2','g3']}";
		checkStep(306, in, JsonMap.ofJson(in).getList("E").getString(1), "3");
		in = "{A:'B','C':1,\"E\":[1,2/*,3*/],G:['g1','g2','g3']}";
		checkStep(307, in, JsonMap.ofJson(in).getList("E").getString(1), "2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:[/*'g1',*/'g2','g3']}";
		checkStep(308, in, JsonMap.ofJson(in).getList("G").getString(0), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1'/*,'g2'*/,'g3']}";
		checkStep(309, in, JsonMap.ofJson(in).getList("G").getString(1), "g3");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2'/*,'g3'*/]}";
		checkStep(310, in, JsonMap.ofJson(in).getList("G").getString(1), "g2");
		in = "{A:'B','C':1,\"E\":[1,2,3],G:['g1','g2','g3']}";
		checkStep(310, in, JsonMap.ofJson(in).getList("G").getString(1), "g2");

		// Check keys that contain array indexes
		in = "{A:{B:[{C:'c0'},{C:'c1'},{C:'c2'}]}}";
		checkStep(401, in, getDeepString(JsonMap.ofJson(in), "A/B/0/C"), "c0");
		checkStep(402, in, getDeepString(JsonMap.ofJson(in), "A/B/1/C"), "c1");
		checkStep(403, in, getDeepString(JsonMap.ofJson(in), "A/B/2/C"), "c2");

		// Check extended unicode characters.
		in = "{'𤭢𤭢':'𤭢𤭢'}";
		checkStep(1, in, JsonMap.ofJson(in).getString("𤭢𤭢"), "𤭢𤭢");
	}

	private static String getDeepString(JsonMap m, String url) {
		var r = ObjectRest.create(m);
		return (String)r.get(url);
	}

	private static Boolean getDeepBoolean(JsonMap m, String url) {
		var r = ObjectRest.create(m);
		return (Boolean)r.get(url);
	}

	private static void checkStep(int step, String input, String output, String expectedValue) {
		if (!output.equals(expectedValue)) {
			var msg = "Step #" + step + " failed: [" + input + "]->[" + output + "]...Expected value=[" + expectedValue + "]";
			fail(msg);
		}
	}

	//====================================================================================================
	// testComparison
	//====================================================================================================
	@Test void a02_comparison() throws Exception {
		var m1 = JsonMap.ofJson("{ firstName:'John', lastName:'Smith', age:123, isDeceased:false }");
		var m2 = JsonMap.ofJson("{ age:123, isDeceased:false, lastName:'Smith', firstName:'John' }");

		assertEquals(m1, m2);
	}

	//====================================================================================================
	// testParent
	//====================================================================================================
	@Test void a03_parent() throws Exception {
		var m1 = JsonMap.ofJson("{a:1}");
		var m2 = JsonMap.ofJson("{b:2}").inner(m1);

		assertEquals(Integer.valueOf(1), m2.getInt("a"));
	}

	//====================================================================================================
	// testUpdatability
	//====================================================================================================
	@Test void a04_updatability() throws Exception {
		var m = JsonMap.ofJson("{a:[{b:'c'}]}");
		var l = m.getList("a");
		var m2 = l.getMap(0);
		m2.put("b", "x");
		assertBean(m, "a", "[{b=x}]");

		m = JsonMap.ofJson("{a:[{b:'c'}]}");
		for (var m3 : m.getList("a").elements(JsonMap.class))
			m3.put("b", "y");

		assertBean(m, "a", "[{b=y}]");
	}

	//====================================================================================================
	// testAtMethods
	//====================================================================================================
	@Test void a05_atMethods() throws Exception {
		var m = JsonMap.ofJson("{a:[{b:'c'}]}");
		var r = m.getAt("a/0/b", String.class);

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
	// JsonMap(Reader)
	//====================================================================================================
	@Test void a06_fromReader() throws Exception {
		assertBean(JsonMap.ofJson(reader("{foo:'bar'}")), "foo", "bar");
	}

	//====================================================================================================
	// testGetMap
	//====================================================================================================
	@Test void a07_getMap() throws Exception {
		var m = JsonMap.ofJson("{a:{1:'true',2:'false'}}");
		var m2 = m.getMap("a", Integer.class, Boolean.class, null);
		assertJson("{'1':true,'2':false}", m2);
		assertEquals(Integer.class, m2.keySet().iterator().next().getClass());
		assertEquals(Boolean.class, m2.values().iterator().next().getClass());

		m2 = m.getMap("b", Integer.class, Boolean.class, null);
		assertNull(m2);

		m2 = m.get("a", Map.class, Integer.class, Boolean.class);
		assertJson("{'1':true,'2':false}", m2);
		assertEquals(Integer.class, m2.keySet().iterator().next().getClass());
		assertEquals(Boolean.class, m2.values().iterator().next().getClass());

		m2 = m.get("b", Map.class, Integer.class, Boolean.class);
		assertNull(m2);
	}

	//====================================================================================================
	// testGetList
	//====================================================================================================
	@Test void a08_getList() throws Exception {
		var m = JsonMap.ofJson("{a:['123','456']}");
		var l2 = m.getList("a", Integer.class, null);
		assertList(l2, "123", "456");
		assertEquals(Integer.class, l2.iterator().next().getClass());

		l2 = m.getList("b", Integer.class, null);
		assertNull(l2);

		l2 = m.get("a", List.class, Integer.class);
		assertList(l2, "123", "456");
		assertEquals(Integer.class, l2.iterator().next().getClass());

		l2 = m.get("b", List.class, Integer.class);
		assertNull(l2);
	}
}