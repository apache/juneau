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
package org.apache.juneau.rest.server.widgets;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link CardField} factory contract: the two-arg form defaults {@code value} to the empty string, the three-arg
 * form round-trips and normalizes a <jk>null</jk> value.
 */
class CardField_Test extends TestBase {

	@Test void a01_of_twoArg_defaultsValueToEmpty() {
		var f = CardField.of("k", "Label");
		assertEquals("k", f.data);
		assertEquals("Label", f.label);
		assertEquals("", f.value);
	}

	@Test void a02_of_threeArg_roundTrips() {
		var f = CardField.of("k", "Label", "42");
		assertEquals("k", f.data);
		assertEquals("Label", f.label);
		assertEquals("42", f.value);
	}

	@Test void a03_of_nullValue_normalizedToEmpty() {
		var f = CardField.of("k", "Label", null);
		assertEquals("", f.value);
	}

	@Test void a04_newInstance_valueDefaultsToEmpty() {
		assertEquals("", new CardField().value);
	}
}
