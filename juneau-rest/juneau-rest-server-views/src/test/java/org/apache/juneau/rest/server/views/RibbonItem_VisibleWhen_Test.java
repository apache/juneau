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
package org.apache.juneau.rest.server.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.junit.jupiter.api.*;

/**
 * {@code RibbonItem.visibleWhen} serialization tests.
 *
 * @since 10.0.0
 */
class RibbonItem_VisibleWhen_Test {

	@Test void a01_absent_omitsKey() {
		assertFalse(RibbonItem.refresh().toMap().containsKey("visibleWhen"));
	}

	@Test void a02_serializesEachRule() {
		var m = RibbonItem.refresh()
			.visibleWhen(VisibilityRule.when("status").eq("active"), VisibilityRule.when("owner").present())
			.toMap();
		@SuppressWarnings("unchecked")
		var rules = (List<Map<String,Object>>)m.get("visibleWhen");
		assertEquals(2, rules.size());
		assertEquals(Map.of("field", "status", "op", "eq", "value", "active"), rules.get(0));
		assertEquals(Map.of("field", "owner", "op", "present"), rules.get(1));
	}

	@Test void a03_emptyCall_clearsRules() {
		var item = RibbonItem.refresh().visibleWhen(VisibilityRule.when("a").present()).visibleWhen();
		assertFalse(item.toMap().containsKey("visibleWhen"));
	}

	@Test void a04_nullRules_areRejected() {
		assertThrows(IllegalArgumentException.class, () -> RibbonItem.refresh().visibleWhen((VisibilityRule[])null));
		assertThrows(IllegalArgumentException.class, () -> RibbonItem.refresh().visibleWhen((VisibilityRule)null));
	}

	@Test void a05_optionGroupMember_cannotCarryVisibleWhen() {
		var member = RibbonItem.option("m").column("c").value("v").visibleWhen(VisibilityRule.when("a").present());
		var group = RibbonItem.optionGroup("g", member);
		assertThrows(IllegalArgumentException.class, group::toMap);
	}
}
