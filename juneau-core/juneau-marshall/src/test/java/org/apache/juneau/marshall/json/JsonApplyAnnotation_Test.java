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
package org.apache.juneau.marshall.json;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class JsonApplyAnnotation_Test extends TestBase {

	@Test void a01_empty_null() {
		assertTrue(JsonApplyAnnotation.empty(null));
	}

	@Test void a02_empty_default() {
		assertTrue(JsonApplyAnnotation.empty(JsonApplyAnnotation.DEFAULT));
	}

	@Test void a03_empty_nonDefault() {
		var a = JsonApplyAnnotation.create(String.class).build();
		assertFalse(JsonApplyAnnotation.empty(a));
	}

	@Test void a04_create_and_build() {
		var a = JsonApplyAnnotation.create("MyClass").build();
		assertEquals(1, a.on().length);
		assertEquals("MyClass", a.on()[0]);
	}

	@Test void b05_applier_emptyOn_returnsEarly() {
		var vr = VarResolver.DEFAULT.createSession();
		var applier = new JsonApplyAnnotation.Applier(vr);
		var b = MarshallingContext.DEFAULT.copy();
		var a = JsonApplyAnnotation.DEFAULT;
		assertDoesNotThrow(() -> applier.apply(AnnotationInfo.of(ClassInfo.of(Object.class), a), b));
	}
}
