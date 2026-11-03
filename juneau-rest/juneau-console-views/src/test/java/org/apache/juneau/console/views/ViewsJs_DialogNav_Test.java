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
import static org.junit.jupiter.api.Assumptions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Node behavioral checks for the dialog frame's inner navigation: {@code data-nav}, the wizard step rail and
 * per-step validation (driven by {@code dialog-nav.cjs}).
 */
class ViewsJs_DialogNav_Test extends TestBase {

	private static Map<?,?> report() {
		var r = NodeHarness.report("dialog-nav.cjs", ViewsMixin.RENDERS_JS_RESOURCE, ViewsMixin.VIEWS_JS_RESOURCE);
		assumeTrue(r != null, "node (or dialog-nav.cjs) not available - behavioral layer skipped");
		return r;
	}

	private static Map<?,?> map(String key) {
		return (Map<?,?>)report().get(key);
	}

	@Test void a01_dataNav_pillByDefault() {
		assertEquals("pill", map("plain").get("nav"));
		assertEquals(false, map("plain").get("rail"));
	}

	@Test void a02_dataNav_pillAndFolderFromModalNav() {
		assertEquals("pill", map("pill").get("nav"));
		assertEquals(true, map("pill").get("strip"));
		assertEquals("folder", map("folder").get("nav"));
		assertEquals(true, map("folder").get("strip"));
	}

	@Test void a03_dataNav_unknownFallsBackToPillSilently() {
		assertEquals("pill", map("unknown").get("nav"));
		assertEquals(List.of(), report().get("unknownLogged"));
	}

	@Test void a04_dataNav_stepsMakeAWizard() {
		var w = map("wizardWinsOverNav");
		assertEquals("wizard", w.get("nav"));
		assertEquals(true, w.get("rail"));
	}

	@Test void b01_rail_numberedMarksAndActiveStep() {
		var r = map("rail");
		assertEquals("OL", r.get("tag"));
		assertEquals("Steps", r.get("ariaLabel"));
		assertEquals(true, r.get("railAfterHeader"));
		assertEquals(List.of(
			Map.of("state", "active", "current", "step", "mark", "1", "label", "Pick"),
			Map.of("state", "todo", "mark", "2", "label", "two"),
			Map.of("state", "todo", "mark", "3", "label", "Done")
		), stripNulls((List<?>)r.get("at0")));
	}

	@Test void b02_rail_completedStepShowsCheckMark() {
		var r = map("rail");
		assertEquals(List.of(
			Map.of("state", "done", "mark", "✓", "label", "Pick"),
			Map.of("state", "active", "current", "step", "mark", "2", "label", "two"),
			Map.of("state", "todo", "mark", "3", "label", "Done")
		), stripNulls((List<?>)r.get("at1")));
	}

	@Test void b03_wizard_onePanePerStepOnlyCurrentShown() {
		var r = map("rail");
		assertEquals(3, ((Number)r.get("paneCount")).intValue());
		assertEquals(List.of("two"), r.get("visiblePanes"));
	}

	@Test void b04_wizard_footerCancelSpacerBackConfirm() {
		assertEquals(List.of("juneau-view-dialog-cancel", "juneau-view-dialog-actions-spacer", "juneau-view-dialog-back", "juneau-view-dialog-confirm"),
			map("rail").get("footer"));
	}

	@Test void b05_wizard_consumesOneDepthSlot() {
		assertEquals(1, ((Number)map("rail").get("depth")).intValue());
	}

	@Test void c01_next_validatesOnlyTheCurrentStep() {
		var v = map("validation");
		assertEquals("one", v.get("blockedStep"));
		assertEquals(true, v.get("focusedA"));
		assertEquals("true", v.get("aInvalid"));
		assertEquals(true, v.get("bUntouched"));
		assertEquals("two", v.get("advancedStep"));
		assertEquals(true, v.get("bUntouchedAfterNext"));
	}

	@Test void c02_finish_validatesEveryStepAndRevealsTheFirstInvalid() {
		var v = map("validation");
		assertEquals("one", v.get("finishRevealedStep"));
		assertEquals(true, v.get("finishFocusedA"));
		assertEquals(true, v.get("paneOneShown"));
		assertEquals(true, v.get("paneTwoHidden"));
		assertEquals(1, ((Number)v.get("stillOpen")).intValue());
		assertEquals("submitted", v.get("closed"));
	}

	private static List<Map<?,?>> stripNulls(List<?> l) {
		var out = new ArrayList<Map<?,?>>();
		for (var o : l) {
			var m = new LinkedHashMap<Object,Object>((Map<?,?>)o);
			m.values().removeIf(Objects::isNull);
			out.add(m);
		}
		return out;
	}
}
