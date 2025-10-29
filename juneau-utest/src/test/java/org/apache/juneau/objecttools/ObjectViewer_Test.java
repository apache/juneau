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
package org.apache.juneau.objecttools;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests the PojoPaginator class.
 */
class ObjectViewer_Test extends TestBase {

	ObjectViewer ov = new ObjectViewer();
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_nullInput() {
		assertNull(ov.run(bs, null, null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple bean
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		public String f1,f2;

		public static A create(String f1, String f2) {
			var a = new A();
			a.f1 = f1;
			a.f2 = f2;
			return a;
		}
	}

	@Test void b01_simpleBean() {
		var sa = new ViewArgs("f1");
		var in = A.create("x1","x2");
		assertJson("{f1:'x1'}", ov.run(bs, in, sa));
		assertJson("{f1:'x1'}", ov.runSingle(in, "f1"));
	}

	@Test void b02_simpleBean_reverseColumns() {
		var sa = new ViewArgs("f2,f1");
		var in = A.create("x1","x2");
		assertJson("{f2:'x2',f1:'x1'}", ov.run(bs, in, sa));
		assertJson("{f2:'x2',f1:'x1'}", ov.runSingle(in, "f2,f1"));
	}

	@Test void b03_simpleBean_dupColumns() {
		var sa = new ViewArgs("f1,f1");
		var in = A.create("x1","x2");
		assertJson("{f1:'x1'}", ov.run(bs, in, sa));
		assertJson("{f1:'x1'}", ov.runSingle(in, "f1,f1"));
	}

	@Test void b04_simpleBean_nonExistentColumns() {
		var sa = new ViewArgs("fx");
		var in = A.create("x1","x2");
		assertJson("{}", ov.run(bs, in, sa));
		assertJson("{}", ov.runSingle(in, "fx"));
	}

	@Test void b05_simpleBean_emptyArgs() {
		var sa = new ViewArgs("");
		var in = A.create("x1","x2");
		assertJson("{}", ov.run(bs, in, sa));
		assertJson("{}", ov.runSingle(in, ""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple BeanMap
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b06_simpleBeanMap() {
		var sa = new ViewArgs("f1");
		var in = bs.toBeanMap(A.create("x1","x2"));
		assertJson("{f1:'x1'}", ov.run(bs, in, sa));
		assertJson("{f1:'x1'}", ov.runSingle(in, "f1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple map
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b07_simpleMap() {
		var sa = new ViewArgs("f1");
		var in = m("f1","x1","f2","x2");
		assertJson("{f1:'x1'}", ov.run(bs, in, sa));
		assertJson("{f1:'x1'}", ov.runSingle(in, "f1"));
	}

	@Test void b08_simpleMap_reverseColumns() {
		var sa = new ViewArgs("f2,f1");
		var in = m("f1","x1","f2","x2");
		assertJson("{f2:'x2',f1:'x1'}", ov.run(bs, in, sa));
		assertJson("{f2:'x2',f1:'x1'}", ov.runSingle(in, "f2,f1"));
	}

	@Test void b09_simpleMap_nonExistentColumns() {
		var sa = new ViewArgs("fx");
		var in = m("f1","x1","f2","x2");
		assertJson("{}", ov.run(bs, in, sa));
		assertJson("{}", ov.runSingle(in, "fx"));
	}

	@Test void b10_simpleMap_emptyView() {
		var sa = new ViewArgs("");
		var in = m("f1","x1","f2","x2");
		assertJson("{}", ov.run(bs, in, sa));
		assertJson("{}", ov.runSingle(in, ""));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean array
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_beanArray() {
		var sa = new ViewArgs("f1");
		var in = a(A.create("x1","x2"));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1"));
	}

	@Test void c02_beanArray_reverseColumns() {
		var sa = new ViewArgs("f2,f1");
		var in = a(A.create("x1","x2"));
		assertJson("[{f2:'x2',f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f2:'x2',f1:'x1'}]", ov.run(in, "f2,f1"));
	}

	@Test void c03_beanArray_dupColumns() {
		var sa = new ViewArgs("f1,f1");
		var in = a(A.create("x1","x2"));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1,f1"));
	}

	@Test void c04_beanArray_nonExistentColumns() {
		var sa = new ViewArgs("fx");
		var in = a(A.create("x1","x2"));
		assertJson("[{}]", ov.run(bs, in, sa));
		assertJson("[{}]", ov.run(in, "fx"));
	}

	@Test void c05_beanArray_emptyArgs() {
		var sa = new ViewArgs("");
		var in = a(A.create("x1","x2"));
		assertJson("[{}]", ov.run(bs, in, sa));
		assertJson("[{}]", ov.run(in, ""));
	}

	@Test void c06_beanArray_withNull() {
		var sa = new ViewArgs("f1");
		var in = a(A.create("x1","x2"),null);
		assertJson("[{f1:'x1'},null]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'},null]", ov.run(in, "f1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean list
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_beanList() {
		var sa = new ViewArgs("f1");
		var in = l(A.create("x1","x2"));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1"));
	}

	@Test void d02_beanList_reverseColumns() {
		var sa = new ViewArgs("f2,f1");
		var in = l(A.create("x1","x2"));
		assertJson("[{f2:'x2',f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f2:'x2',f1:'x1'}]", ov.run(in, "f2,f1"));
	}

	@Test void d03_beanList_dupColumns() {
		var sa = new ViewArgs("f1,f1");
		var in = l(A.create("x1","x2"));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1,f1"));
	}

	@Test void d04_beanList_nonExistentColumns() {
		var sa = new ViewArgs("fx");
		var in = l(A.create("x1","x2"));
		assertJson("[{}]", ov.run(bs, in, sa));
		assertJson("[{}]", ov.run(in, "fx"));
	}

	@Test void d05_beanList_emptyArgs() {
		var sa = new ViewArgs("");
		var in = l(A.create("x1","x2"));
		assertJson("[{}]", ov.run(bs, in, sa));
		assertJson("[{}]", ov.run(in, ""));
	}

	@Test void d06_beanList_withNull() {
		var sa = new ViewArgs("f1");
		var in = l(A.create("x1","x2"),null);
		assertJson("[{f1:'x1'},null]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'},null]", ov.run(in, "f1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean set
	//-----------------------------------------------------------------------------------------------------------------

	@Test void e01_beanSet() {
		var sa = new ViewArgs("f1");
		var in = set(A.create("x1","x2"));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1"));
	}

	@Test void e02_beanSet_reverseColumns() {
		var sa = new ViewArgs("f2,f1");
		var in = set(A.create("x1","x2"));
		assertJson("[{f2:'x2',f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f2:'x2',f1:'x1'}]", ov.run(in, "f2,f1"));
	}

	@Test void e03_beanSet_dupColumns() {
		var sa = new ViewArgs("f1,f1");
		var in = set(A.create("x1","x2"));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1,f1"));
	}

	@Test void e04_beanSet_nonExistentColumns() {
		var sa = new ViewArgs("fx");
		var in = set(A.create("x1","x2"));
		assertJson("[{}]", ov.run(bs, in, sa));
		assertJson("[{}]", ov.run(in, "fx"));
	}

	@Test void e05_beanSet_emptyArgs() {
		var sa = new ViewArgs("");
		var in = set(A.create("x1","x2"));
		assertJson("[{}]", ov.run(bs, in, sa));
		assertJson("[{}]", ov.run(in, ""));
	}

	@Test void e06_beanSet_withNull() {
		var sa = new ViewArgs("f1");
		var in = set(A.create("x1","x2"),null);
		assertJson("[{f1:'x1'},null]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'},null]", ov.run(in, "f1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object
	//-----------------------------------------------------------------------------------------------------------------

	@Test void f01_otherObject() {
		var sa = new ViewArgs("f1");
		var in = "foobar";
		assertEquals("foobar", ov.run(bs, in, sa));	}

	//-----------------------------------------------------------------------------------------------------------------
	// Map list
	//-----------------------------------------------------------------------------------------------------------------

	@Test void g01_mapList() {
		var sa = new ViewArgs("f1");
		var in = l(map("f1","x1","f2","x2"));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanMap list
	//-----------------------------------------------------------------------------------------------------------------

	@Test void h01_beanMapList() {
		var sa = new ViewArgs("f1");
		var in = l(bs.toBeanMap(A.create("x1","x2")));
		assertJson("[{f1:'x1'}]", ov.run(bs, in, sa));
		assertJson("[{f1:'x1'}]", ov.run(in, "f1"));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object list
	//-----------------------------------------------------------------------------------------------------------------

	@Test void i01_otherObjectList() {
		var sa = new ViewArgs("f1");
		var in = l("foobar");
		assertJson("['foobar']", ov.run(bs, in, sa));
		assertJson("['foobar']", ov.run(in, "f1"));
	}
}