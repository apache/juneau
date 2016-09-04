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

import org.apache.juneau.internal.*;
import org.junit.*;

@SuppressWarnings("javadoc")
public class SimpleMapTest {

	@Test
	public void doTest() throws Exception {
		String[] keys = {"a","b"};
		Object[] vals = {"A","B"};
		SimpleMap m = new SimpleMap(keys, vals);
		assertEquals(2, m.size());
		assertEquals("A", m.get("a"));
		assertEquals("B", m.get("b"));
		assertObjectEquals("{a:'A',b:'B'}", m);
		assertObjectEquals("['a','b']", m.keySet());
		m.put("a", "1");
		assertObjectEquals("{a:'1',b:'B'}", m);
		m.entrySet().iterator().next().setValue("2");
		assertObjectEquals("{a:'2',b:'B'}", m);
		try { m.put("c", "1"); fail(); } catch (IllegalArgumentException e) {}

		assertNull(m.get("c"));

		try { m = new SimpleMap(null, vals); fail(); } catch (IllegalArgumentException e) {}
		try { m = new SimpleMap(keys, null); fail(); } catch (IllegalArgumentException e) {}
		try { m = new SimpleMap(keys, new Object[0]); fail(); } catch (IllegalArgumentException e) {}

		keys[0] = null;
		try { m = new SimpleMap(keys, vals); fail(); } catch (IllegalArgumentException e) {}
	}
}
