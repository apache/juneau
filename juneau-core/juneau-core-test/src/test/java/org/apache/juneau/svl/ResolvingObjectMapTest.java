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
package org.apache.juneau.svl;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.utils.*;
import org.junit.*;

public class ResolvingObjectMapTest {
	
	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		VarResolver vr = new VarResolverBuilder().defaultVars().vars(XVar.class).build();
		ObjectMap m = new ResolvingObjectMap(vr.createSession());

		m.put("foo", "$X{a}");
		assertEquals(m.get("foo"), "1");

		m.put("foo", new String[]{"$X{a}"});
		assertObjectEquals("['1']", m.get("foo"));

		m.put("foo", new AList<String>().append("$X{a}"));
		assertObjectEquals("['1']", m.get("foo"));

		m.put("foo", new AMap<String,String>().append("k1","$X{a}"));
		assertObjectEquals("{k1:'1'}", m.get("foo"));
	}
	
	public static class XVar extends MapVar {
		public XVar() {
			super("X", new ObjectMap().append("a", 1).append("b", 2).append("c", 3));
		}
	}

	//====================================================================================================
	// testNulls
	//====================================================================================================
	@Test
	public void testNulls() throws Exception {
		VarResolver vr = new VarResolverBuilder().defaultVars().vars(XVar.class).build();
		ObjectMap m = new ResolvingObjectMap(vr.createSession());

		m.put("foo", null);
		assertNull(m.get("foo"));

		m.put("foo", new String[]{null});
		assertObjectEquals("[null]", m.get("foo"));

		m.put("foo", new AList<String>().append(null));
		assertObjectEquals("[null]", m.get("foo"));

		m.put("foo", new AMap<String,String>().append("k1",null));
		assertObjectEquals("{k1:null}", m.get("foo"));
	}

	//====================================================================================================
	// testNonStrings
	//====================================================================================================
	@Test
	public void testNonStrings() throws Exception {
		VarResolver vr = new VarResolverBuilder().defaultVars().vars(XVar.class).build();
		ObjectMap m = new ResolvingObjectMap(vr.createSession());

		m.put("foo", FooEnum.ONE);
		assertObjectEquals("'ONE'", m.get("foo"));

		m.put("foo", new Object[]{FooEnum.ONE});
		assertObjectEquals("['ONE']", m.get("foo"));

		m.put("foo", new AList<FooEnum>().append(FooEnum.ONE));
		assertObjectEquals("['ONE']", m.get("foo"));

		m.put("foo", new AMap<FooEnum,FooEnum>().append(FooEnum.ONE,FooEnum.ONE));
		assertObjectEquals("{ONE:'ONE'}", m.get("foo"));
	}
	
	public static enum FooEnum {
		ONE
	}
	
	//====================================================================================================
	// testInner - Test inner maps
	//====================================================================================================
	@Test
	public void testInner() throws Exception {
		VarResolver vr = new VarResolverBuilder().defaultVars().vars(XVar.class).build();
		ObjectMap m = new ResolvingObjectMap(vr.createSession());
		ObjectMap m2 = new ObjectMap();
		ObjectMap m3 = new ObjectMap();
		m.setInner(m2);
		m2.setInner(m3);

		m3.put("foo", "$X{a}");
		assertEquals(m.get("foo"), "1");

		m3.put("foo", new String[]{"$X{a}"});
		assertObjectEquals("['1']", m.get("foo"));

		m3.put("foo", new AList<String>().append("$X{a}"));
		assertObjectEquals("['1']", m.get("foo"));

		m3.put("foo", new AMap<String,String>().append("k1","$X{a}"));
		assertObjectEquals("{k1:'1'}", m.get("foo"));
	}
}
