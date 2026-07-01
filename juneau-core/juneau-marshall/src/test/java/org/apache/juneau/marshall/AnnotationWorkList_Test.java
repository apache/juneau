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
package org.apache.juneau.marshall;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;

class AnnotationWorkList_Test {

	// equals: this == o (identity)
	@Test void a01_equals_same_instance() {
		var list = AnnotationWorkList.create();
		assertEquals(list, list);
	}

	// equals: o instanceof AnnotationWorkList && super.equals(other)
	@Test void a02_equals_two_empty_lists() {
		var l1 = AnnotationWorkList.create();
		var l2 = AnnotationWorkList.create();
		assertEquals(l1, l2);
	}

	// equals: o not an AnnotationWorkList
	@Test void a03_equals_non_list() {
		var list = AnnotationWorkList.create();
		assertNotEquals("not a list", list);
	}

	// equals: o is null
	@Test void a04_equals_null() {
		var list = AnnotationWorkList.create();
		assertNotEquals(null, list);
	}

	// hashCode smoke test
	@Test void a05_hashCode() {
		var l1 = AnnotationWorkList.create();
		var l2 = AnnotationWorkList.create();
		assertEquals(l1.hashCode(), l2.hashCode());
	}
}
