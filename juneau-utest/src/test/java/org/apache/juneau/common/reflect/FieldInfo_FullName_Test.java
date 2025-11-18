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
package org.apache.juneau.common.reflect;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

/**
 * Tests for {@link FieldInfo#getFullName()} method.
 */
public class FieldInfo_FullName_Test {

	public static class TestClass {
		public String field1;
		public int field2;
	}

	@Test
	public void testGetFullName() {
		var ci = ClassInfo.of(TestClass.class);
		var field1 = ci.getPublicField(x -> x.getName().equals("field1")).get();
		var field2 = ci.getPublicField(x -> x.getName().equals("field2")).get();

		// Verify full names are correct
		String fullName1 = field1.getFullName();
		String fullName2 = field2.getFullName();

		assertTrue(fullName1.endsWith("FieldInfo_FullName_Test$TestClass.field1"));
		assertTrue(fullName2.endsWith("FieldInfo_FullName_Test$TestClass.field2"));
		
		// Verify package is included
		assertTrue(fullName1.startsWith("org.apache.juneau.common.reflect."));
		assertTrue(fullName2.startsWith("org.apache.juneau.common.reflect."));
	}

	@Test
	public void testGetFullNameMemoization() {
		var ci = ClassInfo.of(TestClass.class);
		var field1 = ci.getPublicField(x -> x.getName().equals("field1")).get();

		// Calling getFullName() multiple times should return the same String instance (memoized)
		String name1 = field1.getFullName();
		String name2 = field1.getFullName();
		assertSame(name1, name2);
	}

	public static class InnerClass {
		public String innerField;
	}

	@Test
	public void testGetFullNameWithInnerClass() {
		var ci = ClassInfo.of(InnerClass.class);
		var field = ci.getPublicField(x -> x.getName().equals("innerField")).get();

		String fullName = field.getFullName();
		
		// Verify $ separator is used for inner class
		assertTrue(fullName.contains("FieldInfo_FullName_Test$InnerClass"));
		assertTrue(fullName.endsWith(".innerField"));
	}
}

