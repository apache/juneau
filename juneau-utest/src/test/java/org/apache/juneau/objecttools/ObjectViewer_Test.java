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
package org.apache.juneau.objecttools;

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.junit.*;

/**
 * Tests the PojoPaginator class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class ObjectViewer_Test {

	ObjectViewer ov = new ObjectViewer();
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_nullInput() {
		assertNull(ov.run(bs, null, null));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple bean
	//-----------------------------------------------------------------------------------------------------------------

	public static class A {
		public String f1,f2;

		public static A create(String f1, String f2) {
			A a = new A();
			a.f1 = f1;
			a.f2 = f2;
			return a;
		}
	}

	@Test
	public void b01_simpleBean() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = A.create("x1","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{f1:'x1'}");
		assertObject(ov.runSingle(in, "f1")).asJson().is("{f1:'x1'}");
	}

	@Test
	public void b02_simpleBean_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2,f1");
		Object in = A.create("x1","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{f2:'x2',f1:'x1'}");
		assertObject(ov.runSingle(in, "f2,f1")).asJson().is("{f2:'x2',f1:'x1'}");
	}

	@Test
	public void b03_simpleBean_dupColumns() {
		ViewArgs sa = new ViewArgs("f1,f1");
		Object in = A.create("x1","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{f1:'x1'}");
		assertObject(ov.runSingle(in, "f1,f1")).asJson().is("{f1:'x1'}");
	}

	@Test
	public void b04_simpleBean_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = A.create("x1","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{}");
		assertObject(ov.runSingle(in, "fx")).asJson().is("{}");
	}

	@Test
	public void b05_simpleBean_emptyArgs() {
		ViewArgs sa = new ViewArgs("");
		Object in = A.create("x1","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{}");
		assertObject(ov.runSingle(in, "")).asJson().is("{}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple BeanMap
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b06_simpleBeanMap() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = bs.toBeanMap(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("{f1:'x1'}");
		assertObject(ov.runSingle(in, "f1")).asJson().is("{f1:'x1'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple map
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b07_simpleMap() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = map("f1","x1","f2","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{f1:'x1'}");
		assertObject(ov.runSingle(in, "f1")).asJson().is("{f1:'x1'}");
	}

	@Test
	public void b08_simpleMap_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2,f1");
		Object in = map("f1","x1","f2","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{f2:'x2',f1:'x1'}");
		assertObject(ov.runSingle(in, "f2,f1")).asJson().is("{f2:'x2',f1:'x1'}");
	}

	@Test
	public void b09_simpleMap_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = map("f1","x1","f2","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{}");
		assertObject(ov.runSingle(in, "fx")).asJson().is("{}");
	}

	@Test
	public void b10_simpleMap_emptyView() {
		ViewArgs sa = new ViewArgs("");
		Object in = map("f1","x1","f2","x2");
		assertObject(ov.run(bs, in, sa)).asJson().is("{}");
		assertObject(ov.runSingle(in, "")).asJson().is("{}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean array
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void c01_beanArray() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void c02_beanArray_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2,f1");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f2:'x2',f1:'x1'}]");
		assertObject(ov.run(in, "f2,f1")).asJson().is("[{f2:'x2',f1:'x1'}]");
	}

	@Test
	public void c03_beanArray_dupColumns() {
		ViewArgs sa = new ViewArgs("f1,f1");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1,f1")).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void c04_beanArray_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(ov.run(bs, in, sa)).asJson().is("[{}]");
		assertObject(ov.run(in, "fx")).asJson().is("[{}]");
	}

	@Test
	public void c05_beanArray_emptyArgs() {
		ViewArgs sa = new ViewArgs("");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(ov.run(bs, in, sa)).asJson().is("[{}]");
		assertObject(ov.run(in, "")).asJson().is("[{}]");
	}

	@Test
	public void c06_beanArray_withNull() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = new A[]{A.create("x1","x2"),null};
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'},null]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'},null]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void d01_beanList() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = list(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void d02_beanList_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2,f1");
		Object in = list(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f2:'x2',f1:'x1'}]");
		assertObject(ov.run(in, "f2,f1")).asJson().is("[{f2:'x2',f1:'x1'}]");
	}

	@Test
	public void d03_beanList_dupColumns() {
		ViewArgs sa = new ViewArgs("f1,f1");
		Object in = list(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1,f1")).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void d04_beanList_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = list(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{}]");
		assertObject(ov.run(in, "fx")).asJson().is("[{}]");
	}

	@Test
	public void d05_beanList_emptyArgs() {
		ViewArgs sa = new ViewArgs("");
		Object in = list(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{}]");
		assertObject(ov.run(in, "")).asJson().is("[{}]");
	}

	@Test
	public void d06_beanList_withNull() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = list(A.create("x1","x2"),null);
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'},null]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'},null]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean set
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void e01_beanSet() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = set(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void e02_beanSet_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2,f1");
		Object in = set(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f2:'x2',f1:'x1'}]");
		assertObject(ov.run(in, "f2,f1")).asJson().is("[{f2:'x2',f1:'x1'}]");
	}

	@Test
	public void e03_beanSet_dupColumns() {
		ViewArgs sa = new ViewArgs("f1,f1");
		Object in = set(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1,f1")).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void e04_beanSet_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = set(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{}]");
		assertObject(ov.run(in, "fx")).asJson().is("[{}]");
	}

	@Test
	public void e05_beanSet_emptyArgs() {
		ViewArgs sa = new ViewArgs("");
		Object in = set(A.create("x1","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{}]");
		assertObject(ov.run(in, "")).asJson().is("[{}]");
	}

	@Test
	public void e06_beanSet_withNull() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = set(A.create("x1","x2"),null);
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'},null]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'},null]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void f01_otherObject() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = "foobar";
		assertObject(ov.run(bs, in, sa)).asJson().is("'foobar'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Map list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void g01_mapList() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = list(map("f1","x1","f2","x2"));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanMap list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void h01_beanMapList() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = list(bs.toBeanMap(A.create("x1","x2")));
		assertObject(ov.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
		assertObject(ov.run(in, "f1")).asJson().is("[{f1:'x1'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void i01_otherObjectList() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = list("foobar");
		assertObject(ov.run(bs, in, sa)).asJson().is("['foobar']");
		assertObject(ov.run(in, "f1")).asJson().is("['foobar']");
	}
}
