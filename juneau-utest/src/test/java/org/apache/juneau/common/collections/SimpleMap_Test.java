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
package org.apache.juneau.common.collections;

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.apache.juneau.junit.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class SimpleMap_Test extends TestBase {

	@Test void doTest() {
		var keys = a("a","b");
		Object[] vals = {"A","B"};
		var m = new SimpleMap<>(keys, vals);
		assertEquals(2, m.size());
		assertEquals("A", m.get("a"));
		assertEquals("B", m.get("b"));
		assertBean(m, "a,b", "A,B");
		assertList(m.keySet(), "a", "b");
		m.put("a", "1");
		assertBean(m, "a,b", "1,B");
		m.entrySet().iterator().next().setValue("2");
		assertBean(m, "a,b", "2,B");
		assertThrows(IllegalArgumentException.class, ()->m.put("c", "1"));

		assertNull(m.get("c"));

		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(null, vals));
		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(keys, null));
		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(keys, new Object[0]));

		keys[0] = null;
		assertThrows(IllegalArgumentException.class, ()->new SimpleMap<>(keys, vals));
	}
}