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
package org.apache.juneau.common.utils;

import static org.apache.juneau.TestUtils.*;
import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ArrayUtilsTest extends TestBase {

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test void a01_appendArrayToArray() {
		String[] s = {};

		s = addAll(s, "a", "b");
		assertList(s, "a", "b");

		s = addAll(s, "c");
		assertList(s, "a", "b", "c");

		s = addAll(s);
		assertList(s, "a", "b", "c");

		var o = addAll((Object[])null);
		assertEmpty(o);

		s = addAll((String[])null, "a", "b");
		assertList(s, "a", "b");
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test void a02_asSet() {
		assertThrows(IllegalArgumentException.class, ()->toSet((String[])null));

		var s = a("a");
		var i = toSet(s).iterator();
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

		assertList(combine(s1, s2), "a", "b");
		assertList(combine(s1), "a");
		assertList(combine(s2), "b");
		assertList(combine(s1,null), "a");
		assertList(combine(null,s2), "b");
		assertNull(combine(null,null));
		assertNull(combine());
	}
}