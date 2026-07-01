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
package org.apache.juneau.marshall.xml;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.commons.reflect.*;
import org.apache.juneau.commons.svl.*;
import org.apache.juneau.marshall.*;
import org.junit.jupiter.api.*;

class XmlApplyAnnotation_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// empty() helper
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_empty_null() {
		assertTrue(XmlApplyAnnotation.empty(null));
	}

	@Test void a02_empty_default() {
		assertTrue(XmlApplyAnnotation.empty(XmlApplyAnnotation.DEFAULT));
	}

	@Test void a03_empty_nonDefault() {
		var a = XmlApplyAnnotation.create()
			.on("SomeClass")
			.value(XmlAnnotation.create().prefix("x").build())
			.build();
		assertFalse(XmlApplyAnnotation.empty(a));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Basic builder / create
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_create_and_build() {
		var a = XmlApplyAnnotation.create("MyClass").build();
		assertEquals(1, a.on().length);
		assertEquals("MyClass", a.on()[0]);
	}

	@Test void b02_propertyStoreEquivalency() {
		var a1 = XmlApplyAnnotation.create()
			.on("u")
			.value(XmlAnnotation.create().prefix("foo").build())
			.build();
		var a2 = XmlApplyAnnotation.create()
			.on("u")
			.value(XmlAnnotation.create().prefix("foo").build())
			.build();
		var bc1 = MarshallingContext.create().annotations(a1).build();
		var bc2 = MarshallingContext.create().annotations(a2).build();
		assertSame(bc1, bc2);
	}

	@Test void b05_applier_emptyOn_returnsEarly() {
		var vr = VarResolver.DEFAULT.createSession();
		var applier = new XmlApplyAnnotation.Applier(vr);
		var b = MarshallingContext.DEFAULT.copy();
		var a = XmlApplyAnnotation.DEFAULT;
		assertDoesNotThrow(() -> applier.apply(AnnotationInfo.of(ClassInfo.of(Object.class), a), b));
	}
}
