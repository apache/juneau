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

import static org.apache.juneau.assertions.ObjectAssertion.*;
import static org.apache.juneau.assertions.ThrowableAssertion.*;
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import org.apache.juneau.internal.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class SimpleMapTest {

	@Test
	public void doTest() throws Exception {
		String[] keys = {"a","b"};
		Object[] vals = {"A","B"};
		SimpleMap<String,Object> m = new SimpleMap<>(keys, vals);
		assertEquals(2, m.size());
		assertEquals("A", m.get("a"));
		assertEquals("B", m.get("b"));
		assertObject(m).json().is("{a:'A',b:'B'}");
		assertObject(m.keySet()).json().is("['a','b']");
		m.put("a", "1");
		assertObject(m).json().is("{a:'1',b:'B'}");
		m.entrySet().iterator().next().setValue("2");
		assertObject(m).json().is("{a:'2',b:'B'}");
		assertThrown(()->{m.put("c", "1");}).isType(IllegalArgumentException.class);

		assertNull(m.get("c"));

		assertThrown(()->{new SimpleMap<>(null, vals);}).isType(IllegalArgumentException.class);
		assertThrown(()->{new SimpleMap<>(keys, null);}).isType(IllegalArgumentException.class);
		assertThrown(()->{new SimpleMap<>(keys, new Object[0]);}).isType(IllegalArgumentException.class);

		keys[0] = null;
		assertThrown(()->{new SimpleMap<>(keys, vals);}).isType(IllegalArgumentException.class);
	}
}
