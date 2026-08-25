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

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@link ActionBar} / {@link ActionRef} / {@link SafeAction} bean contract.  The module-isolation rule these beans
 * depend on is asserted in {@link Widgets_ModuleBoundary_Test}.
 */
class ActionBar_Test extends TestBase {

	@Test void a01_contractVersion_isOne() {
		assertEquals("1", ActionBar.CONTRACT_VERSION);
	}

	@Test void a02_items_roundTrip() {
		var bar = ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE);
		assertSize(2, bar.items);
		assertEquals("ack", ((ActionRef) bar.items.get(0)).id);
		assertEquals(SafeAction.COLLAPSE, bar.items.get(1));
		bar.validate();
	}

	@Test void a03_blankActionRef_rejectedAtFactory() {
		assertThrows(IllegalArgumentException.class, () -> ActionRef.of(null));
		assertThrows(IllegalArgumentException.class, () -> ActionRef.of("  "));
	}

	@Test void a04_validate_rejectsBlankActionRefId() {
		var bar = ActionBar.create();
		var blank = new ActionRef();
		blank.id = "  ";
		bar.items = java.util.List.of(blank);
		var e = assertThrows(IllegalArgumentException.class, bar::validate);
		assertTrue(e.getMessage().contains("ActionRef"), e::getMessage);
	}

	@Test void a05_safeAction_collapseWireAndLabel() {
		assertEquals("collapse", SafeAction.COLLAPSE.wire());
		assertEquals("Collapse", SafeAction.COLLAPSE.label());
	}
}
