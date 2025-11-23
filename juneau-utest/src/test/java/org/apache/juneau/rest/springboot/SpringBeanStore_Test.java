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
package org.apache.juneau.rest.springboot;

import static org.apache.juneau.common.utils.Utils.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Tests for SpringBeanStore fluent setter overrides.
 */
class SpringBeanStore_Test extends TestBase {

	@Test
	void a01_fluentChaining_clear() {
		// Test that clear() returns SpringBeanStore (not BeanStore)
		var store = new SpringBeanStore(opte(), opte(), null);

		SpringBeanStore result = store.clear();

		assertSame(store, result);
		assertInstanceOf(SpringBeanStore.class, result);
	}

	@Test
	void a02_fluentChaining_removeBean_byClass() {
		// Test that removeBean(Class) returns SpringBeanStore (not BeanStore)
		var store = new SpringBeanStore(opte(), opte(), null);

		SpringBeanStore result = store.removeBean(String.class);

		assertSame(store, result);
		assertInstanceOf(SpringBeanStore.class, result);
	}

	@Test
	void a03_fluentChaining_removeBean_byClassAndName() {
		// Test that removeBean(Class, String) returns SpringBeanStore (not BeanStore)
		var store = new SpringBeanStore(opte(), opte(), null);

		SpringBeanStore result = store.removeBean(String.class, "testBean");

		assertSame(store, result);
		assertInstanceOf(SpringBeanStore.class, result);
	}

	@Test
	void a04_fluentChaining_complex() {
		// Test chaining multiple fluent calls
		var store = new SpringBeanStore(opte(), opte(), null);

		SpringBeanStore result = store.removeBean(String.class).removeBean(Integer.class, "myInt").clear();

		assertSame(store, result);
		assertInstanceOf(SpringBeanStore.class, result);
	}
}