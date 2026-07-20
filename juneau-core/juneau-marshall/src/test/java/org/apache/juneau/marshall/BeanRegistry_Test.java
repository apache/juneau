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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Validates {@link BeanRegistry}.
 */
class BeanRegistry_Test extends TestBase {

	@Marshalled(typeName="A")
	public static class A {
		public String f;
	}

	// Regression: getTypeName(ClassMeta) NPE'd on a null argument for a non-empty registry (it dereferenced
	// c.inner()), even though its sibling getTypeName(Class) guarded null and both share the "or null" @return.
	@Test void getTypeName_nullClassMeta() {
		var bc = MarshallingContext.create().beanDictionary(A.class).build();
		var reg = bc.getBeanRegistry();

		assertEquals("A", reg.getTypeName(bc.getClassMeta(A.class)));
		assertNull(reg.getTypeName((ClassMeta<?>)null));
		assertNull(reg.getTypeName((Class<?>)null));
	}
}
