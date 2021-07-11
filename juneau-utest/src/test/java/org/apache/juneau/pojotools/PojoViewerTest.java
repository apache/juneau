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
package org.apache.juneau.pojotools;

import static org.apache.juneau.assertions.Assertions.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.junit.*;

/**
 * Tests the PojoPaginator class.
 */
@FixMethodOrder(NAME_ASCENDING)
public class PojoViewerTest {

	PojoViewer p = new PojoViewer();
	BeanSession bs = BeanContext.DEFAULT_SESSION;

	//-----------------------------------------------------------------------------------------------------------------
	// Null input
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void nullInput() {
		assertNull(p.run(bs, null, null));
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
	public void simpleBean() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = A.create("x1","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{f1:'x1'}");
	}

	@Test
	public void simpleBean_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = A.create("x1","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{f2:'x2',f1:'x1'}");
	}

	@Test
	public void simpleBean_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = A.create("x1","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{f1:'x1'}");
	}

	@Test
	public void simpleBean_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = A.create("x1","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{}");
	}

	@Test
	public void simpleBean_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = A.create("x1","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{f1:'x1'}");
	}

	@Test
	public void simpleBean_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = A.create("x1","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple BeanMap
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void simpleBeanMap() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = bs.toBeanMap(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("{f1:'x1'}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Simple map
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void simpleMap() {
		ViewArgs sa = new ViewArgs("f1");
		Object in = AMap.of("f1","x1","f2","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{f1:'x1'}");
	}

	@Test
	public void simpleMap_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = AMap.of("f1","x1","f2","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{f2:'x2',f1:'x1'}");
	}

	@Test
	public void simpleMap_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = AMap.of("f1","x1","f2","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{}");
	}

	@Test
	public void simpleMap_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = AMap.of("f1","x1","f2","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{f1:'x1'}");
	}

	@Test
	public void simpleMap_emptyView() {
		ViewArgs sa = new ViewArgs();
		Object in = AMap.of("f1","x1","f2","x2");
		assertObject(p.run(bs, in, sa)).asJson().is("{}");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean array
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanArray() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = new A[]{A.create("x1","x2")};
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanArray_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(p.run(bs, in, sa)).asJson().is("[{f2:'x2',f1:'x1'}]");
	}

	@Test
	public void beanArray_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanArray_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = new A[]{A.create("x1","x2")};
		assertObject(p.run(bs, in, sa)).asJson().is("[{}]");
	}

	@Test
	public void beanArray_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = new A[]{A.create("x1","x2")};
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanArray_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = new A[]{A.create("x1","x2")};
		assertObject(p.run(bs, in, sa)).asJson().is("[{}]");
	}

	@Test
	public void beanArray_withNull() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = new A[]{A.create("x1","x2"),null};
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'},null]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanList_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = AList.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f2:'x2',f1:'x1'}]");
	}

	@Test
	public void beanList_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = AList.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanList_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = AList.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{}]");
	}

	@Test
	public void beanList_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = AList.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanList_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = AList.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{}]");
	}

	@Test
	public void beanList_withNull() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.of(A.create("x1","x2"),null);
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'},null]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Bean set
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanSet() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = ASet.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanSet_reverseColumns() {
		ViewArgs sa = new ViewArgs("f2","f1");
		Object in = ASet.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f2:'x2',f1:'x1'}]");
	}

	@Test
	public void beanSet_dupColumns() {
		ViewArgs sa = new ViewArgs("f1","f1");
		Object in = ASet.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanSet_nonExistentColumns() {
		ViewArgs sa = new ViewArgs("fx");
		Object in = ASet.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{}]");
	}

	@Test
	public void beanSet_nullColumn() {
		ViewArgs sa = new ViewArgs("f1",null);
		Object in = ASet.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	@Test
	public void beanSet_emptyArgs() {
		ViewArgs sa = new ViewArgs();
		Object in = ASet.of(A.create("x1","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{}]");
	}

	@Test
	public void beanSet_withNull() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = ASet.of(A.create("x1","x2"),null);
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'},null]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void otherObject() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = "foobar";
		assertObject(p.run(bs, in, sa)).asJson().is("'foobar'");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Map list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void mapList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.of(AMap.of("f1","x1","f2","x2"));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// BeanMap list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void beanMapList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.of(bs.toBeanMap(A.create("x1","x2")));
		assertObject(p.run(bs, in, sa)).asJson().is("[{f1:'x1'}]");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Other object list
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void otherObjectList() {
		ViewArgs sa = new ViewArgs("f1");;
		Object in = AList.of("foobar");
		assertObject(p.run(bs, in, sa)).asJson().is("['foobar']");
	}

}
