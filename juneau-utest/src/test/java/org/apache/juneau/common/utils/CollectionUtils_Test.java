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

import static org.apache.juneau.common.utils.CollectionUtils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class CollectionUtils_Test extends TestBase {

	//====================================================================================================
	// treeSet(Set)
	//====================================================================================================
	@Test
	void a01_treeSet_fromSet() {
		LinkedHashSet<String> input = new LinkedHashSet<>(List.of("c", "a", "b"));
		TreeSet<String> result = toSortedSet(input);
		
		assertNotNull(result);
		assertEquals(List.of("a", "b", "c"), new ArrayList<>(result));
		
		// Null input
		assertNull(toSortedSet((Set<String>)null));
	}

	@Test
	void a02_treeSet_fromSet_numbers() {
		LinkedHashSet<Integer> input = new LinkedHashSet<>(List.of(3, 1, 2));
		TreeSet<Integer> result = toSortedSet(input);
		
		assertNotNull(result);
		assertEquals(List.of(1, 2, 3), new ArrayList<>(result));
	}

	//====================================================================================================
	// treeSet(T...)
	//====================================================================================================
	@Test
	void a03_treeSet_varargs() {
		TreeSet<String> result = sortedSet("c", "a", "b");
		
		assertNotNull(result);
		assertEquals(List.of("a", "b", "c"), new ArrayList<>(result));
	}

	@Test
	void a04_treeSet_varargs_empty() {
		TreeSet<String> result = sortedSet();
		
		assertNotNull(result);
		assertTrue(result.isEmpty());
	}

	@Test
	void a05_treeSet_varargs_single() {
		TreeSet<String> result = sortedSet("a");
		
		assertNotNull(result);
		assertEquals(List.of("a"), new ArrayList<>(result));
	}

	@Test
	void a06_treeSet_varargs_numbers() {
		TreeSet<Integer> result = sortedSet(3, 1, 2, 5, 4);
		
		assertNotNull(result);
		assertEquals(List.of(1, 2, 3, 4, 5), new ArrayList<>(result));
	}
}

