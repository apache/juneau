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
import static org.junit.Assert.*;
import static org.junit.runners.MethodSorters.*;

import java.util.*;

import org.apache.commons.lang3.*;
import org.junit.*;

@FixMethodOrder(NAME_ASCENDING)
public class CollectionUtilsTest {

	private String[] strings(String s) {
		return StringUtils.split(s, ',');
	}

	@Test
	public void testSortedCaseInsensitiveSet() {
		Set<String> s = newSortedCaseInsensitiveSet("foo,Bar,BAZ");
		for (String ss : strings("foo,Foo,FOO,bar,Bar,BAR,baz,Baz,BAZ"))
			assertTrue(s.contains(ss));
		for (String ss : strings("qux"))
			assertFalse(s.contains(ss));
	}

	@Test
	public void testSortedCaseInsensitiveSet_empty() {
		Set<String> s = newSortedCaseInsensitiveSet("");
		assertFalse(s.contains("foo"));
		assertFalse(s.contains(""));
		assertFalse(s.contains(null));
	}

	@Test
	public void testSortedCaseInsensitiveSet_null() {
		String ss = null;
		Set<String> s = newSortedCaseInsensitiveSet(ss);
		assertFalse(s.contains("foo"));
		assertFalse(s.contains(""));
		assertFalse(s.contains(null));
	}

	@Test
	public void testSortedCaseInsensitiveSet_containsNull() {
		Set<String> s = newSortedCaseInsensitiveSet(null, "foo");
		assertTrue(s.contains("foo"));
		assertFalse(s.contains(""));
		assertFalse(s.contains(null));
	}
}
