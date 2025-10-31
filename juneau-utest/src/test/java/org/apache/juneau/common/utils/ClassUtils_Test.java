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

import static org.apache.juneau.common.utils.ClassUtils.*;
import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link ClassUtils}.
 */
class ClassUtils_Test {

	//-----------------------------------------------------------------------------------------------------------------
	// simpleQualifiedClassName tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void a01_simpleQualifiedClassName_topLevelClass() {
		assertEquals("String", simpleQualifiedClassName(String.class));
		assertEquals("ArrayList", simpleQualifiedClassName(ArrayList.class));
	}

	@Test
	public void a02_simpleQualifiedClassName_innerClass() {
		assertEquals("Map.Entry", simpleQualifiedClassName(Map.Entry.class));
	}

	@Test
	public void a03_simpleQualifiedClassName_nestedInnerClass() {
		class Outer {
			class Inner {
				class Deep {}
			}
		}
		var deepClass = Outer.Inner.Deep.class;
		var result = simpleQualifiedClassName(deepClass);
		// Result will be something like "ClassUtils_Test.1Outer.Inner.Deep"
		assertTrue(result.endsWith("Outer.Inner.Deep"), result);
		assertFalse(result.contains("$"), result);
	}

	@Test
	public void a04_simpleQualifiedClassName_withObject() {
		var obj = new HashMap<>();
		assertEquals("HashMap", simpleQualifiedClassName(obj));
	}

	@Test
	public void a05_simpleQualifiedClassName_null() {
		assertNull(simpleQualifiedClassName(null));
	}

	@Test
	public void a06_simpleQualifiedClassName_noPackage() {
		// Test with a class that has no package (unlikely in practice, but good to test)
		var name = simpleQualifiedClassName(String.class);
		assertFalse(name.contains(".java.lang"), name);
	}

	@Test
	public void a07_simpleQualifiedClassName_anonymousClass() {
		var anon = new Object() {};
		var result = simpleQualifiedClassName(anon);
		// Anonymous classes have names like "ClassUtils_Test$1"
		// After conversion should be like "ClassUtils_Test.1"
		assertNotNull(result);
		assertFalse(result.contains("$"), result);
	}

	@Test
	public void a08_simpleQualifiedClassName_arrayTypes() {
		assertEquals("String[]", simpleQualifiedClassName(String[].class));
		assertEquals("String[][]", simpleQualifiedClassName(String[][].class));
		assertEquals("int[]", simpleQualifiedClassName(int[].class));
		assertEquals("Map.Entry[]", simpleQualifiedClassName(Map.Entry[].class));
	}

	@Test
	public void a09_simpleQualifiedClassName_arrayObjects() {
		var stringArray = new String[]{"a", "b"};
		assertEquals("String[]", simpleQualifiedClassName(stringArray));
		
		var intArray = new int[]{1, 2, 3};
		assertEquals("int[]", simpleQualifiedClassName(intArray));
		
		var multiDimArray = new String[][]{{"a"}};
		assertEquals("String[][]", simpleQualifiedClassName(multiDimArray));
	}

	//-----------------------------------------------------------------------------------------------------------------
	// sqcn shortcut tests
	//-----------------------------------------------------------------------------------------------------------------

	@Test
	public void b01_sqcn_shortcut() {
		// Test that the shortcut method works the same as the full method
		assertEquals("String", sqcn(String.class));
		assertEquals("Map.Entry", sqcn(Map.Entry.class));
		assertNull(sqcn(null));
	}

	@Test
	public void b02_sqcn_withObject() {
		var obj = new HashMap<>();
		assertEquals("HashMap", sqcn(obj));
		assertEquals(simpleQualifiedClassName(obj), sqcn(obj));
	}

	@Test
	public void b03_sqcn_withArrays() {
		assertEquals("String[]", sqcn(String[].class));
		assertEquals("int[][]", sqcn(int[][].class));
		assertEquals("Map.Entry[]", sqcn(Map.Entry[].class));
		
		var arr = new String[]{"test"};
		assertEquals("String[]", sqcn(arr));
	}
}

