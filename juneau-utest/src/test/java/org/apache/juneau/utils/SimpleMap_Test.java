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
package org.apache.juneau.utils;

import static org.apache.juneau.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.*;
import org.apache.juneau.internal.*;
import org.junit.jupiter.api.*;

class SimpleMap_Test extends SimpleTestBase {

	@Test void doTest() {
		var keys = a("a","b");
		Object[] vals = {"A","B"};
		var m = new SimpleMap<>(keys, vals);
		assertEquals(2, m.size());
		assertEquals("A", m.get("a"));
		assertEquals("B", m.get("b"));
		assertJson(m, "{a:'A',b:'B'}");
		assertJson(m.keySet(), "['a','b']");
		m.put("a", "1");
		assertJson(m, "{a:'1',b:'B'}");
		m.entrySet().iterator().next().setValue("2");
		assertJson(m, "{a:'2',b:'B'}");
		assertThrows(IllegalArgumentException.class, ()->m.put("c", "1"));

		assertNull(m.get("c"));

		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(null, vals));
		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(keys, null));
		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(keys, new Object[0]));

		keys[0] = null;
		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(keys, vals));
	}
}
