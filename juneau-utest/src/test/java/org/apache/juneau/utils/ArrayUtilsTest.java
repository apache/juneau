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
package org.apache.juneau.utils;

import static org.apache.juneau.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.common.utils.*;
import org.junit.jupiter.api.*;

class ArrayUtilsTest extends TestBase {

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test void a01_appendArrayToArray() {
		String[] s = {};

		s = CollectionUtils.append(s, "a", "b");
		assertList(s, "a", "b");

		s = CollectionUtils.append(s, "c");
		assertList(s, "a", "b", "c");

		s = CollectionUtils.append(s);
		assertList(s, "a", "b", "c");

		var o = CollectionUtils.append((Object[])null);
		assertEmpty(o);

		s = CollectionUtils.append((String[])null, "a", "b");
		assertList(s, "a", "b");
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test void a02_asSet() {
		assertThrows(IllegalArgumentException.class, ()->CollectionUtils.asSet((String[])null));

		var s = a("a");
		var i = CollectionUtils.asSet(s).iterator();
		assertEquals("a", i.next());

		assertThrows(UnsupportedOperationException.class, i::remove);
		assertThrows(NoSuchElementException.class, i::next);
	}

	//====================================================================================================
	// combine(T[]...)
	//====================================================================================================
	@Test void a03_combine() {
		var s1 = a("a");
		var s2 = a("b");

		assertList(CollectionUtils.combine(s1, s2), "a", "b");
		assertList(CollectionUtils.combine(s1), "a");
		assertList(CollectionUtils.combine(s2), "b");
		assertList(CollectionUtils.combine(s1,null), "a");
		assertList(CollectionUtils.combine(null,s2), "b");
		assertNull(CollectionUtils.combine(null,null));
		assertNull(CollectionUtils.combine());
	}
}