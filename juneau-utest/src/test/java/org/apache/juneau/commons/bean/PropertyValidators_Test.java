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
package org.apache.juneau.commons.bean;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.*;
import org.apache.juneau.*;

/**
 * Tests {@link PropertyValidators} — the SPI discovery helper for {@link PropertyValidatorFactory}.
 */
class PropertyValidators_Test extends TestBase {

	@Test void a01_factory_isDiscovered() {
		// juneau-bean-jsonschema is on the classpath in juneau-utest, so a factory must be resolved.
		assertNotNull(PropertyValidators.factory());
	}

	@Test void a02_setFactory_overridesAndRestores() {
		var original = PropertyValidators.factory();
		try {
			PropertyValidators.setFactory(null);
			assertNull(PropertyValidators.factory());

			PropertyValidatorFactory stub = (map, type) -> value -> { /* noop */ };
			PropertyValidators.setFactory(stub);
			assertSame(stub, PropertyValidators.factory());
		} finally {
			PropertyValidators.setFactory(original);
		}
		assertSame(original, PropertyValidators.factory());
	}
}
