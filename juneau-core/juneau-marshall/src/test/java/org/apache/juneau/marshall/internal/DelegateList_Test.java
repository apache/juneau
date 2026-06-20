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
package org.apache.juneau.marshall.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class DelegateList_Test extends TestBase {

	private static final MarshallingContext BC = MarshallingContext.create().build();

	@Test void a01_nonArray_preservesClassMeta() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		var dl = new DelegateList<>(cm);
		assertSame(cm, dl.getBeanInfo());
		assertFalse(dl.getBeanInfo().isArray());
	}

	@Test @SuppressWarnings({"unchecked","rawtypes"}) void a02_array_convertsToListClassMeta() {
		var cm = (ClassMeta) BC.getClassMeta(String[].class);
		assertTrue(cm.isArray());
		var dl = new DelegateList(cm);
		assertFalse(dl.getBeanInfo().isArray());
	}

	@Test void a03_equals_self() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		var dl = new DelegateList<>(cm);
		assertEquals(dl, dl);
	}

	@Test void a04_equals_null() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		var dl = new DelegateList<>(cm);
		assertNotEquals(dl, null);
	}

	@Test void a05_equals_nonList() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		var dl = new DelegateList<>(cm);
		assertNotEquals(dl, "not a list");
	}

	@Test void a06_equals_emptyList() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		var dl = new DelegateList<>(cm);
		assertEquals(new ArrayList<>(), dl);
	}

	@Test void a07_equals_listWithSameContent() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		List<Object> dl1 = new DelegateList<>(cm);
		dl1.add("a");
		List<Object> dl2 = new ArrayList<>(List.of("a"));
		assertEquals(dl2, dl1);
	}

	@Test void a08_equals_listWithDifferentContent() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		var dl = new DelegateList<>(cm);
		dl.add("a");
		assertNotEquals(dl, new ArrayList<>());
	}

	@Test void a09_hashCode() {
		var cm = BC.<List<String>>getClassMeta(List.class, String.class);
		var dl = new DelegateList<>(cm);
		assertEquals(new ArrayList<>().hashCode(), dl.hashCode());
	}
}
