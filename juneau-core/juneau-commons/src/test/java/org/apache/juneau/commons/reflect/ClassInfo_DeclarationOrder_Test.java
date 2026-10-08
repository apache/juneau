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
package org.apache.juneau.commons.reflect;

import static org.apache.juneau.commons.TestAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.commons.*;
import org.junit.jupiter.api.*;

/**
 * Tests {@link ClassInfo#getDeclaredFieldsInDeclarationOrder()}.
 */
class ClassInfo_DeclarationOrder_Test extends TestBase {

	@SuppressWarnings("unused")
	public static class A {
		public String zeta;
		public int alpha;
		public static String STATIC_X;
		private boolean mid;
	}

	public static class B {}

	@Test void a01_declarationOrder() {
		assertList(ClassInfo.of(A.class).getDeclaredFieldsInDeclarationOrder().stream().map(FieldInfo::getName).toList(),
			"zeta", "alpha", "STATIC_X", "mid");
	}

	@Test void a02_sortedAccessorUnchanged() {
		assertList(ClassInfo.of(A.class).getDeclaredFields().stream().map(FieldInfo::getName).toList(),
			"STATIC_X", "alpha", "mid", "zeta");
	}

	@Test void a03_noFields() {
		assertList(ClassInfo.of(B.class).getDeclaredFieldsInDeclarationOrder());
	}

	@Test void a04_sameFieldInfoInstances() {
		var ci = ClassInfo.of(A.class);
		var unsorted = ci.getDeclaredFieldsInDeclarationOrder();
		var sorted = ci.getDeclaredFields();
		assertTrue(sorted.containsAll(unsorted) && unsorted.containsAll(sorted));
	}
}
