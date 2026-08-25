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

/**
 * Golden-fixture contract test for the typed {@code ActionResult} wire format (design doc §6.1/§6.2; the write-path
 * half of the row-action contract).
 *
 * <p>
 * This pins the third independently-versioned wire contract of the toolkit (alongside {@code VIEW_META} and
 * {@code PAGE_META}): its own {@link ActionResult#CONTRACT_VERSION}, its frozen top-level key order, the reserved
 * async outcome set, and the omit-when-unset rule for every optional field.  Serialization uses the same canonical
 * compact JSON marshaller ({@link Json#of(Object)}) the runtime reads.
 */
class ActionResult_Contract_Test extends TestBase {

	//------------------------------------------------------------------------------------------------------------------
	// The frozen ActionResult contract
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_success_serializesToFrozenShape() {
		var row = new LinkedHashMap<String,Object>();
		row.put("id", "INC-1");
		row.put("status", "acknowledged");
		var json = Json.of(ActionResult.success(row));

		var expected = Json.to("{\"contractVersion\":\"1\",\"outcome\":\"success\",\"row\":{\"id\":\"INC-1\",\"status\":\"acknowledged\"}}", Map.class);
		var actual = Json.to(json, Map.class);
		assertEquals(expected, actual, json);
	}

	@Test void a02_contractVersion_isItsOwnValue_notAliasedToViewDef() {
		// The whole point of MED-7: this is a SEPARATE contract from VIEW_META, so its version must not track ViewDef's.
		assertEquals("1", ActionResult.CONTRACT_VERSION);
		assertNotEquals(ViewDef.CONTRACT_VERSION, ActionResult.CONTRACT_VERSION,
			"ActionResult.CONTRACT_VERSION must be its own value, never aliased to ViewDef.CONTRACT_VERSION");
	}

	@Test void a03_topLevelKeyOrder_success() {
		Map<?,?> actual = Json.to(Json.of(ActionResult.success(Map.of("id", "x"))), Map.class);
		assertEquals(List.of("contractVersion", "outcome", "row"), new ArrayList<>(actual.keySet()));
	}

	@Test void a04_topLevelKeyOrder_fullResult_matchesBeanTypeOrder() {
		// contractVersion,outcome,replay,refusalCode,message,row (the @BeanType-pinned order).
		var r = ActionResult.success(Map.of("id", "x")).replay(true).message("done");
		Map<?,?> actual = Json.to(Json.of(r), Map.class);
		assertEquals(List.of("contractVersion", "outcome", "replay", "message", "row"), new ArrayList<>(actual.keySet()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Outcome discriminator + reserved async terminal states (MED-8)
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_syncOutcomeWireTokens() {
		assertEquals("success", ActionResult.Outcome.SUCCESS.wire());
		assertEquals("failure", ActionResult.Outcome.FAILURE.wire());
		assertEquals("refusal", ActionResult.Outcome.REFUSAL.wire());
		assertEquals("unknown", ActionResult.Outcome.UNKNOWN.wire());
	}

	@Test void b02_reservedAsyncOutcomes_areFrozenNow() {
		// Reserved from day one so the async-job feature cannot force a second result-contract bump.
		assertEquals("cancelled", ActionResult.Outcome.CANCELLED.wire());
		assertEquals("cancelled-after-effect", ActionResult.Outcome.CANCELLED_AFTER_EFFECT.wire());
		assertEquals(
			List.of("SUCCESS", "FAILURE", "REFUSAL", "UNKNOWN", "CANCELLED", "CANCELLED_AFTER_EFFECT"),
			Arrays.stream(ActionResult.Outcome.values()).map(Enum::name).toList());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Refusals are an opaque, namespaced code (HIGH-2)
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_refusal_carriesOpaqueCode_noRowNoReplay() {
		var json = Json.of(ActionResult.refusal("write-guard:not-armed"));
		var actual = Json.to(json, Map.class);
		assertEquals(Json.to("{\"contractVersion\":\"1\",\"outcome\":\"refusal\",\"refusalCode\":\"write-guard:not-armed\"}", Map.class), actual, json);
	}

	@Test void c02_refusal_rendersAnyNamespace() {
		// The runtime renders ANY code visibly; the type accepts framework codes and consumer app:/write-guard: codes alike.
		assertTrue(Json.of(ActionResult.refusal("CSRF_TOKEN_MISSING")).contains("\"refusalCode\":\"CSRF_TOKEN_MISSING\""));
		assertTrue(Json.of(ActionResult.refusal("app:custom-thing")).contains("\"refusalCode\":\"app:custom-thing\""));
	}

	@Test void c03_refusal_blankCodeThrows() {
		var e = assertThrows(IllegalArgumentException.class, () -> ActionResult.refusal("  "));
		assertTrue(e.getMessage().contains("blank"), e::getMessage);
		assertThrows(IllegalArgumentException.class, () -> ActionResult.refusal(null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Omit-when-unset for every optional field
	//------------------------------------------------------------------------------------------------------------------

	@Test void d01_failure_omitsRowReplayRefusalMessage() {
		var json = Json.of(ActionResult.failure());
		assertEquals(Json.to("{\"contractVersion\":\"1\",\"outcome\":\"failure\"}", Map.class), Json.to(json, Map.class), json);
		for (var k : List.of("row", "replay", "refusalCode", "message"))
			assertFalse(json.contains("\"" + k + "\""), () -> "unset field leaked: " + k + "\n" + json);
	}

	@Test void d02_unknown_isHonestTerminalState() {
		var json = Json.of(ActionResult.unknown());
		assertEquals(Json.to("{\"contractVersion\":\"1\",\"outcome\":\"unknown\"}", Map.class), Json.to(json, Map.class), json);
	}

	@Test void d03_replayFalseOrNull_isOmitted() {
		assertFalse(Json.of(ActionResult.success(Map.of("id", "x")).replay(false)).contains("\"replay\""));
		assertFalse(Json.of(ActionResult.success(Map.of("id", "x")).replay(null)).contains("\"replay\""));
	}

	@Test void d04_replayTrue_isEmitted() {
		assertTrue(Json.of(ActionResult.success(Map.of("id", "x")).replay(true)).contains("\"replay\":true"));
	}

	@Test void d05_successWithNullRow_omitsRow() {
		// A redraw/navigate success carries no row.
		var json = Json.of(ActionResult.success(null));
		assertEquals(Json.to("{\"contractVersion\":\"1\",\"outcome\":\"success\"}", Map.class), Json.to(json, Map.class), json);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Fluent setters
	//------------------------------------------------------------------------------------------------------------------

	@Test void e01_fluentSetters() {
		var r = ActionResult.failure().message("boom").refusalCode("app:x").row(Map.of("id", "1"));
		assertEquals("boom", r.message);
		assertEquals("app:x", r.refusalCode);
		assertEquals(Map.of("id", "1"), r.row);
	}
}
