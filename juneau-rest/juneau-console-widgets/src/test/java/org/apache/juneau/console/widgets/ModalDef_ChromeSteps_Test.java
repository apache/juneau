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
package org.apache.juneau.console.widgets;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Pins the wire shape and validation of {@link ModalDef#chrome}, {@link ModalDef#steps} and {@link ModalDef#nav}.
 */
class ModalDef_ChromeSteps_Test extends TestBase {

	@Test void a01_unset_omittedFromWire() {
		Map<?,?> actual = Json.to(Json.of(ModalDef.create("t")), Map.class);
		assertFalse(actual.containsKey("chrome"));
		assertFalse(actual.containsKey("steps"));
	}

	@Test void a02_chrome_serializesAsString() {
		Map<?,?> actual = Json.to(Json.of(ModalDef.create("t").chrome("compact")), Map.class);
		assertEquals("compact", actual.get("chrome"));
	}

	@Test void a03_steps_serializeInOrderWithFields() {
		var modal = ModalDef.create("Wizard")
			.step(ModalDef.Step.of("pick").title("Pick").confirmLabel("Next").field("Pet", "Rex"))
			.step(ModalDef.Step.of("review").title("Review").field("Total", "5"));
		var expected = Json.to("""
			{"contractVersion":"3","title":"Wizard",
			 "steps":[{"id":"pick","title":"Pick","confirmLabel":"Next","fields":[{"label":"Pet","value":"Rex"}]},
			          {"id":"review","title":"Review","fields":[{"label":"Total","value":"5"}]}]}
			""", Map.class);
		assertEquals(expected, Json.to(Json.of(modal), Map.class));
	}

	@Test void a04_keyOrder_chromeAndStepsLast() {
		var modal = ModalDef.create("t").chrome("c").step(ModalDef.Step.of("s")).keepOpenOnSubmit(true);
		Map<?,?> actual = Json.to(Json.of(modal), Map.class);
		assertEquals(List.of("contractVersion", "title", "keepOpenOnSubmit", "chrome", "steps"), new ArrayList<>(actual.keySet()));
	}

	@Test void a05_keyOrder_navAfterSteps() {
		var modal = ModalDef.create("t").chrome("c").step(ModalDef.Step.of("s")).nav(ModalDef.NAV_FOLDER);
		Map<?,?> actual = Json.to(Json.of(modal), Map.class);
		assertEquals(List.of("contractVersion", "title", "chrome", "steps", "nav"), new ArrayList<>(actual.keySet()));
	}

	@Test void a06_nav_unsetByDefault_omittedFromWire() {
		var modal = ModalDef.create("t");
		assertNull(modal.nav);
		assertFalse(Json.to(Json.of(modal), Map.class).containsKey("nav"));
	}

	@Test void a07_nav_eachValueRoundTrips() {
		for (var v : List.of(ModalDef.NAV_PILL, ModalDef.NAV_FOLDER, "bogus")) {
			var modal = Json.to(Json.of(ModalDef.create("t").nav(v)), ModalDef.class);
			assertEquals(v, modal.nav);
		}
		assertEquals("pill", ModalDef.NAV_PILL);
		assertEquals("folder", ModalDef.NAV_FOLDER);
	}

	@Test void a08_nav_contractVersionUnchanged() {
		Map<?,?> actual = Json.to(Json.of(ModalDef.create("t").nav(ModalDef.NAV_FOLDER)), Map.class);
		assertEquals("3", actual.get("contractVersion"));
		assertEquals("3", ModalDef.CONTRACT_VERSION);
	}

	@Test void b01_validate_blankChromeRejected() {
		var m = ModalDef.create("t").chrome(" ");
		assertThrows(IllegalArgumentException.class, m::validate);
	}

	@Test void b02_validate_blankStepIdRejected() {
		assertThrows(IllegalArgumentException.class, () -> ModalDef.Step.of(" "));
		var m = ModalDef.create("t");
		m.steps = new ArrayList<>(List.of(new ModalDef.Step()));
		assertThrows(IllegalArgumentException.class, m::validate);
	}

	@Test void b03_validate_duplicateStepIdRejected() {
		var m = ModalDef.create("t").step(ModalDef.Step.of("a")).step(ModalDef.Step.of("a"));
		assertThrows(IllegalArgumentException.class, m::validate);
	}

	@Test void b04_validate_stepFieldKindChecked() {
		var step = ModalDef.Step.of("a").field("L", "v");
		step.fields.get(0).kind = "bogus";
		var m = ModalDef.create("t").step(step);
		assertThrows(IllegalArgumentException.class, m::validate);
	}

	@Test void b05_validate_nullStepRejected() {
		var m = ModalDef.create("t");
		assertThrows(IllegalArgumentException.class, () -> m.step(null));
	}

	@Test void b06_validate_wellFormedPasses() {
		assertDoesNotThrow(() -> ModalDef.create("t").chrome("compact").step(ModalDef.Step.of("a").field("L", "v")).validate());
	}

	@Test void b07_validate_unknownNavRejected() {
		for (var bad : List.of("bogus", "", " ", "Folder", "wizard")) {
			var m = ModalDef.create("t").nav(bad);
			var e = assertThrows(IllegalArgumentException.class, m::validate, () -> "nav '" + bad + "'");
			assertTrue(e.getMessage().contains("nav"), e::getMessage);
		}
	}

	@Test void b08_validate_knownOrUnsetNavPasses() {
		assertDoesNotThrow(() -> ModalDef.create("t").validate());
		assertDoesNotThrow(() -> ModalDef.create("t").nav(ModalDef.NAV_PILL).validate());
		assertDoesNotThrow(() -> ModalDef.create("t").nav(ModalDef.NAV_FOLDER).validate());
		assertDoesNotThrow(() -> ModalDef.create("t").nav(null).validate());
	}
}
