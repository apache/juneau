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
package org.apache.juneau.rest.server.view.freemarker.console;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

/**
 * {@code CardSpec.visibleWhen} serialization tests.
 *
 * @since 10.0.0
 */
class CardSpec_VisibleWhen_Test {

	@Test void a01_absent_omitsKey() {
		assertFalse(CardSpec.html("c1").toMap().containsKey("visibleWhen"));
	}

	@Test void a02_serializesEachRule() {
		var m = CardSpec.html("c1").visibleWhen(VisibilityRule.when("role").in("admin", "owner")).toMap();
		@SuppressWarnings("unchecked")
		var rules = (List<Map<String,Object>>)m.get("visibleWhen");
		assertEquals(1, rules.size());
		assertEquals("role", rules.get(0).get("field"));
		assertEquals("in", rules.get(0).get("op"));
		assertEquals(List.of("admin", "owner"), rules.get(0).get("value"));
	}
}
