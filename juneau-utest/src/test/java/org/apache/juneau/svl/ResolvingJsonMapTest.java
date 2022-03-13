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

import static org.apache.juneau.assertions.Assertions.*;
import static org.apache.juneau.internal.CollectionUtils.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.collections.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class ResolvingJsonMapTest {

	//====================================================================================================
	// test - Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		VarResolver vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		JsonMap m = new ResolvingJsonMap(vr.createSession());

		m.put("foo", "$X{a}");
		assertEquals(m.get("foo"), "1");

		m.put("foo", new String[]{"$X{a}"});
		assertObject(m.get("foo")).asJson().is("['1']");

		m.put("foo", list("$X{a}"));
		assertObject(m.get("foo")).asJson().is("['1']");

		m.put("foo", map("k1","$X{a}"));
		assertObject(m.get("foo")).asJson().is("{k1:'1'}");
	}

	public static class XVar extends MapVar {
		public XVar() {
			super("X", JsonMap.of("a", 1, "b", 2, "c", 3));
		}
	}

	//====================================================================================================
	// testNulls
	//====================================================================================================
	@Test
	public void testNulls() throws Exception {
		VarResolver vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		JsonMap m = new ResolvingJsonMap(vr.createSession());

		m.put("foo", null);
		assertNull(m.get("foo"));

		m.put("foo", new String[]{null});
		assertObject(m.get("foo")).asJson().is("[null]");

		m.put("foo", list((String)null));
		assertObject(m.get("foo")).asJson().is("[null]");

		m.put("foo", map("k1",null));
		assertObject(m.get("foo")).asJson().is("{k1:null}");
	}

	//====================================================================================================
	// testNonStrings
	//====================================================================================================
	@Test
	public void testNonStrings() throws Exception {
		VarResolver vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		JsonMap m = new ResolvingJsonMap(vr.createSession());

		m.put("foo", FooEnum.ONE);
		assertObject(m.get("foo")).asJson().is("'ONE'");

		m.put("foo", new Object[]{FooEnum.ONE});
		assertObject(m.get("foo")).asJson().is("['ONE']");

		m.put("foo", list(FooEnum.ONE));
		assertObject(m.get("foo")).asJson().is("['ONE']");

		m.put("foo", map(FooEnum.ONE,FooEnum.ONE));
		assertObject(m.get("foo")).asJson().is("{ONE:'ONE'}");
	}

	public static enum FooEnum {
		ONE
	}

	//====================================================================================================
	// testInner - Test inner maps
	//====================================================================================================
	@Test
	public void testInner() throws Exception {
		VarResolver vr = VarResolver.create().defaultVars().vars(XVar.class).build();
		JsonMap m = new ResolvingJsonMap(vr.createSession());
		JsonMap m2 = new JsonMap();
		JsonMap m3 = new JsonMap();
		m.inner(m2);
		m2.inner(m3);

		m3.put("foo", "$X{a}");
		assertEquals(m.get("foo"), "1");

		m3.put("foo", new String[]{"$X{a}"});
		assertObject(m.get("foo")).asJson().is("['1']");

		m3.put("foo", list("$X{a}"));
		assertObject(m.get("foo")).asJson().is("['1']");

		m3.put("foo", map("k1","$X{a}"));
		assertObject(m.get("foo")).asJson().is("{k1:'1'}");
	}
}
