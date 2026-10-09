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

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

class RibbonItem_Bus_Test extends TestBase {

	@Test void a01_publish() {
		var i = RibbonItem.publish("Focus east", "app.region-picked", Map.of("region", "east"));
		assertEquals("{type:'publish',title:'Focus east',topic:'app.region-picked',payload:{region:'east'}}", Json5.DEFAULT.write(i.toMap()));
		assertEquals("{type:'publish',title:'Go live',topic:'app.go-live'}",
			Json5.DEFAULT.write(RibbonItem.publish("Go live", "app.go-live", null).toMap()));
	}

	@Test void a02_target_onRefresh() {
		assertEquals("{type:'refresh',title:'Refresh tasks',target:'tasks'}",
			Json5.DEFAULT.write(RibbonItem.refresh().title("Refresh tasks").target("tasks").toMap()));
	}

	@Test void a03_target_onEveryTargetableType() {
		for (var i : List.of(RibbonItem.collapseAll(), RibbonItem.pausePolling(),
				RibbonItem.option("mine").param("owner").value("me"),
				RibbonItem.optionGroup("phase", RibbonItem.option("all").column("phase").value("$eq(x)"))))
			assertEquals("tasks", i.target("tasks").toMap().getString("target"));
	}

	@Test void b01_target_notAccepted() {
		var e = assertThrows(IllegalArgumentException.class, () -> RibbonItem.dialog("add").target("tasks").toMap());
		assertEquals("RibbonItem dialog 'add' does not accept 'target'.", e.getMessage());
	}

	@Test void b02_target_onGroupMember_notAccepted() {
		var g = RibbonItem.optionGroup("phase", RibbonItem.option("all").column("phase").value("$eq(x)").target("tasks"));
		var e = assertThrows(IllegalArgumentException.class, g::toMap);
		assertEquals("RibbonItem optionGroup 'phase' member 'all' does not accept 'target'.", e.getMessage());
	}

	@Test void b03_target_badId() {
		var e = assertThrows(IllegalArgumentException.class, () -> RibbonItem.refresh().target("1x"));
		assertEquals("RibbonItem target '1x' must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$.", e.getMessage());
	}

	@Test void b04_publish_needsTitleAndTopic() {
		assertEquals("RibbonItem publish requires a title.",
			assertThrows(IllegalArgumentException.class, () -> RibbonItem.publish("", "app.x", null)).getMessage());
		assertEquals("RibbonItem publish 'T' requires a topic.",
			assertThrows(IllegalArgumentException.class, () -> RibbonItem.publish("T", " ", null)).getMessage());
	}

	@Test void b05_publish_rejectsTarget() {
		var e = assertThrows(IllegalArgumentException.class, () -> RibbonItem.publish("T", "app.x", null).target("tasks").toMap());
		assertTrue(e.getMessage().endsWith("does not accept 'target'."), e.getMessage());
	}
}
