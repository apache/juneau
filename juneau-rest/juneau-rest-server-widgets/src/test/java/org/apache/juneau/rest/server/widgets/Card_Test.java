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
 * {@link Card} bean contract and fail-closed {@link Card#validate()} branches.
 */
class Card_Test extends TestBase {

	private static CardFieldList body() {
		return CardFieldList.create().fields(CardField.of("k", "L", "v"));
	}

	@Test void a01_builder_roundTrip() {
		var c = Card.create("c1", "Title").body(body());
		assertEquals("c1", c.id);
		assertEquals("Title", c.title);
		assertNotNull(c.body);
		c.validate();
	}

	@Test void a02_blankId_rejected() {
		var c = Card.create("  ", "Title").body(body());
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void a03_blankTitle_rejected() {
		var c = Card.create("c1", "  ").body(body());
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void a04_nullBody_rejected() {
		var c = Card.create("c1", "Title");
		assertThrows(IllegalArgumentException.class, c::validate);
	}

	@Test void a05_validate_fansOutToBody() {
		var c = Card.create("c1", "Title").body(CardFieldList.create());   // no fields
		assertThrows(IllegalArgumentException.class, c::validate);
	}
}
