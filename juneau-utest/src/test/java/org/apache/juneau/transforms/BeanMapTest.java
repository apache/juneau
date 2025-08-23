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
package org.apache.juneau.transforms;

import static org.junit.Assert.*;
import org.apache.juneau.*;
import org.apache.juneau.collections.*;
import org.apache.juneau.swap.*;
import org.apache.juneau.swaps.*;
import org.junit.jupiter.api.*;

class BeanMapTest extends SimpleTestBase {

	//====================================================================================================
	// testFilteredEntry
	//====================================================================================================
	@Test void testFilteredEntry() {
		BeanSession session = BeanContext.create().swaps(ByteArraySwap.Base64.class).build().getSession();
		BeanMap<A> m = session.toBeanMap(new A());

		assertEquals("AQID", m.get("f1"));
		m.put("f1", "BAUG");
		assertEquals("BAUG", m.get("f1"));
		assertEquals(4, m.getBean().f1[0]);

		assertNull(m.get("f3"));
	}

	public static class A {
		public byte[] f1 = {1,2,3};
		public byte[] f3 = null;
	}

	//====================================================================================================
	// testFilteredEntryWithMultipleMatchingFilters
	// When bean properties can have multiple filters applied to them, pick the first match.
	//====================================================================================================
	@Test void testFilteredEntryWithMultipleMatchingFilters() {
		BeanSession session = BeanContext.create().swaps(B2Swap.class, B1Swap.class).build().getSession();
		BeanMap<B> bm = session.toBeanMap(B.create());
		JsonMap m = (JsonMap)bm.get("b1");
		assertEquals("b2", m.getString("type"));

		session = BeanContext.create().swaps(B1Swap.class, B2Swap.class).build().getSession();
		bm = session.toBeanMap(B.create());
		m = (JsonMap)bm.get("b1");
		assertEquals("b1", m.getString("type"));
	}


	public static class B {
		public B1 b1;

		static B create() {
			var b = new B();
			var b2 = new B2();
			b2.f1 = "f1";
			b2.f2 = "f2";
			b.b1 = b2;
			return b;
		}
	}

	public static class B1 {
		public String f1;
	}

	public static class B2 extends B1 {
		public String f2;
	}

	public static class B1Swap extends MapSwap<B1> {
		@Override /* ObjectSwap */
		public JsonMap swap(BeanSession session, B1 b1) {
			return JsonMap.of("type", "b1", "f1", b1.f1);
		}
	}

	public static class B2Swap extends MapSwap<B2> {
		@Override /* ObjectSwap */
		public JsonMap swap(BeanSession session, B2 b2) {
			return JsonMap.of("type", "b2", "f1", b2.f1);
		}
	}
}