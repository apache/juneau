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

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.EventCategory.*;
import org.junit.jupiter.api.*;

/**
 * {@link EventCategory} builder and color-token contract.
 */
class EventCategory_Test extends TestBase {

	@Test void a01_builder() {
		var c = EventCategory.create().id("team").label("Team").color(CategoryColor.AMBER).description("Team events");
		assertBean(c, "id,label,color,description", "team,Team,AMBER,Team events");
		assertEquals(CategoryColor.AMBER, c.effectiveColor());
	}

	@Test void a02_effectiveColor_defaultsToNeutral() {
		assertEquals(CategoryColor.NEUTRAL, EventCategory.create().effectiveColor());
	}

	@Test void a03_colorTokens_areLowercaseNames() {
		assertList(Arrays.stream(CategoryColor.values()).map(CategoryColor::token).toList(),
			"blue", "green", "amber", "red", "neutral");
	}
}
