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

import static org.apache.juneau.internal.CollectionUtils.*;
import static org.apache.juneau.testutils.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.*;

public class CollectionUtilsTest {

	//====================================================================================================
	// reverse(LinkedHashMap)
	//====================================================================================================
	@Test
	public void testReverse() throws Exception {
		assertNull(reverse((Map<?,?>)null));

		assertObjectEquals("{b:2,a:1}", reverse(new ObjectMap("{a:1,b:2}")));
		assertObjectEquals("{}", reverse(new ObjectMap("{}")));
	}

	@Test
	public void testCollapse() throws Exception {
		assertObjectEquals("[]", collapse());
		assertObjectEquals("[1,2,3]", collapse(1,2,3));
		assertObjectEquals("[1,2,3,4,5,6,7,8,9,10,11,12]", collapse(1, 2, null, new int[]{3,4}, new int[][]{null,{},{5,6}}, null, new ObjectList("[null,7,8,[9,10,[11,12,null]]]")));
	}

	@Test
	public void testAssertTypes() throws Exception {
		assertTypes(new AList<>().appendAll(1,2,3), Integer.class);
		assertTypes(new AList<>().appendAll(1,2,3), Object.class);
		assertTypes(new AList<>().appendAll(String.class), String.class);
		assertTypes(new AList<>().appendAll(String.class), CharSequence.class);

		try {
			assertTypes(new AList<>().appendAll(1,2,3), Long.class);
		} catch (Exception e) {
			assertEquals("Invalid list entry '1' (Integer).  Not one of the following types: ['java.lang.Long']", e.getMessage());
		}

		try {
			assertTypes(new AList<>().appendAll(1,2,3), String.class);
		} catch (Exception e) {
			assertEquals("Invalid list entry '1' (Integer).  Not one of the following types: ['java.lang.String']", e.getMessage());
		}

		try {
			assertTypes(new AList<>().appendAll(String.class), StringBuilder.class);
		} catch (Exception e) {
			assertEquals("Invalid list entry ''java.lang.String'' (Class).  Not one of the following types: ['java.lang.StringBuilder']", e.getMessage());
		}

		try {
			assertTypes(new AList<>().appendAll(CharSequence.class), String.class);
		} catch (Exception e) {
			assertEquals("Invalid list entry ''java.lang.CharSequence'' (Class).  Not one of the following types: ['java.lang.String']", e.getMessage());
		}
	}
}
