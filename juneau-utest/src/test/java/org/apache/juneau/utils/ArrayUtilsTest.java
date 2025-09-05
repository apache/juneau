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
import static org.apache.juneau.internal.ArrayUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class ArrayUtilsTest extends SimpleTestBase {

	//====================================================================================================
	// append(T[], T...)
	//====================================================================================================
	@Test void a01_appendArrayToArray() {
		String[] s = {};

		s = append(s, "a", "b");
		assertList(s, "a", "b");

		s = append(s, "c");
		assertList(s, "a", "b", "c");

		s = append(s);
		assertList(s, "a", "b", "c");

		var o = append((Object[])null);
		assertEmpty(o);

		s = append((String[])null, "a", "b");
		assertList(s, "a", "b");
	}

	//====================================================================================================
	// asSet(T[])
	//====================================================================================================
	@Test void a02_asSet() {
		assertThrows(IllegalArgumentException.class, ()->asSet((String[])null));

		var s = a("a");
		var i = asSet(s).iterator();
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