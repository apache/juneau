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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/** {@link RibbonItem} as a {@link Handled} element: action items, model blocks and required ids. */
class RibbonItemModel_Test extends TestBase {

	private static TableSpec table(RibbonItem... items) {
		return TableSpec.create("t1").dataUrl("/d").columns(Column.create("id")).ribbon(items);
	}

	@Test void a01_actionFactoryRequiresId() {
		assertThrows(IllegalArgumentException.class, () -> RibbonItem.action(" "));
		assertThrows(IllegalArgumentException.class, () -> RibbonItem.action((String)null));
	}

	@Test void a02_actionShape() {
		var m = RibbonItem.action("reload").icon("refresh-cw").action("click", null, "juneau.refresh").toMap();
		assertEquals("action", m.get("type"));
		assertEquals("reload", m.get("id"));
		assertEquals("refresh-cw", m.get("icon"));
		assertTrue(m.get("model").toString().contains("juneau.refresh"), m.toString());
	}

	@Test void a03_stockDefaults() {
		assertEquals("refresh", RibbonItem.refresh().toMap().get("id"));
		assertEquals("collapseAll", RibbonItem.collapseAll().toMap().get("id"));
		assertEquals("pausePolling", RibbonItem.pausePolling().toMap().get("id"));
		assertEquals("resetDefaults", RibbonItem.resetDefaults().toMap().get("id"));
		assertEquals("export", RibbonItem.export("copy").toMap().get("id"));
		assertEquals("add", RibbonItem.dialog("add").toMap().get("id"));
		assertEquals("opt", RibbonItem.option("opt").column("c").value("v").toMap().get("id"));
	}

	@Test void a04_secondStockOfSameKindNeedsExplicitId() {
		var e = assertThrows(IllegalArgumentException.class, () -> table(RibbonItem.refresh(), RibbonItem.refresh()));
		assertTrue(e.getMessage().contains("t1"), e.getMessage());
		table(RibbonItem.refresh(), RibbonItem.refresh().id("refresh2"));
	}

	@Test void a05_publishRequiresExplicitId() {
		assertThrows(IllegalArgumentException.class, () -> table(RibbonItem.publish("T", "app.x", null)));
		table(RibbonItem.publish("T", "app.x", null).id("p"));
	}

	@Test void a06_dividerNeedsNoId() {
		table(RibbonItem.divider(), RibbonItem.divider(), RibbonItem.refresh());
		assertFalse(RibbonItem.divider().toMap().containsKey("id"));
	}

	@Test void a07_duplicateIdsThrow() {
		assertThrows(IllegalArgumentException.class, () -> table(RibbonItem.action("a"), RibbonItem.action("a")));
	}

	@Test void a08_modelKeyAllowedOnEveryType() {
		for (var r : List.of(RibbonItem.refresh(), RibbonItem.collapseAll(), RibbonItem.export("copy"), RibbonItem.divider(),
				RibbonItem.dialog("d"), RibbonItem.action("a"), RibbonItem.publish("T", "app.x", null).id("p")))
			assertDoesNotThrow(() -> r.on("activated", "a.b").toMap(), r.toMap().toString());
	}

	@Test void a09_onlyActivatedEvent() {
		assertThrows(IllegalArgumentException.class, () -> RibbonItem.refresh().on("loaded", "a.b"));
	}

	@Test void a10_inlineSurfacesThroughTable() {
		assertTrue(table(RibbonItem.action("a").on("activated", HandlerRef.inline("return 1"))).hasInlineHandlers());
	}
}
