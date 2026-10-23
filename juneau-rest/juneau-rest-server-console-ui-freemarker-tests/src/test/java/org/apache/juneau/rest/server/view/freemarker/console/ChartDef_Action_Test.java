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

import static org.apache.juneau.test.bct.BctAssertions.assertSize;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.views.*;
import org.junit.jupiter.api.*;

class ChartDef_Action_Test extends TestBase {

	private static Map<?,?> map(ChartDef d) {
		return Json5.to(Json.of(d.toCardSpec().toMap()), Map.class);
	}

	private static String fail(Runnable r) {
		return assertThrows(IllegalArgumentException.class, r::run).getMessage();
	}

	private static ChartDef chart() {
		return ChartDef.bar("orders").src("/stats").x("day").series("n", "N");
	}

	private static RowAction requeue() {
		return RowAction.create("requeue").label("Requeue").endpoint("/api/jobs/requeue").method(RowAction.Method.POST);
	}

	private static Map<?,?> body(ChartDef d) { return (Map<?,?>)map(d); }

	@Test void a01_onClickAction_serializesRowActionJson_andAutoDeclaresClickTopic() {
		var m = body(chart().onClickAction(requeue()));
		var action = (Map<?,?>)((Map<?,?>)m.get("onClick")).get("action");
		assertEquals("requeue", action.get("id"));
		assertEquals("/api/jobs/requeue", action.get("endpoint"));
		assertEquals("POST", action.get("method"));
		assertEquals("orders.click", ((Map<?,?>)m.get("on")).get("click"));
		assertTrue(((List<?>)m.get("publishes")).toString().contains("orders.click"));
	}

	@Test void a01b_onClickAction_idNotATopicNamespace_throws() {
		assertEquals("ChartDef 'Orders_1' onClickAction cannot derive a click topic from the id; declare one with on(\"click\", topic).",
			fail(() -> ChartDef.bar("Orders_1").src("/s").x("d").series("n", "N").onClickAction(requeue())));
	}

	@Test void a02_onClickAction_keepsExistingClickTopic() {
		var m = body(chart().on("click", "pet.selected").onClickAction(requeue()));
		assertEquals("pet.selected", ((Map<?,?>)m.get("on")).get("click"));
	}

	@Test void a03_onTopicAction_undeclaredTopic_throws() {
		assertEquals("ChartDef 'orders' onTopicAction topic 'pet.selected' must be one this card publishes through on(...).",
			fail(() -> chart().onTopicAction("pet.selected", requeue())));
	}

	@Test void a04_onTopicAction_declaredTopic_writesTopicActions() {
		var m = body(chart().on("markclick", "pet.selected").onTopicAction("pet.selected", requeue()));
		var ta = (List<?>)m.get("topicActions");
		assertSize(1, ta);
		assertEquals("pet.selected", ((Map<?,?>)ta.get(0)).get("topic"));
		assertEquals("requeue", ((Map<?,?>)((Map<?,?>)ta.get(0)).get("action")).get("id"));
	}

	@Test void a05_hrefPlusAction_throws_eitherOrder() {
		var msg = "ChartDef 'orders' onClick takes either href or action, not both.";
		assertEquals(msg, fail(() -> chart().onClickHref("/x/{id}").onClickAction(requeue())));
		assertEquals(msg, fail(() -> chart().onClickAction(requeue()).onClickHref("/x/{id}")));
	}

	@Test void a06_servletEndpoint_throws() {
		assertEquals("ChartDef 'orders' onClick action endpoint must be a same-origin path; got 'servlet:/x'.",
			fail(() -> chart().onClickAction(requeue().endpoint("servlet:/x"))));
	}

	@Test void a07_servletForm_throws() {
		assertEquals("ChartDef 'orders' onClick action form must be a same-origin path; got 'servlet:/f'.",
			fail(() -> chart().onClickAction(requeue().present(RowAction.Present.DIALOG).form("servlet:/f"))));
	}

	@Test void a08_dotDotEndpoint_throws() {
		assertEquals("ChartDef 'orders' onClick action endpoint must be a same-origin path; got '/a/../b'.",
			fail(() -> chart().onClickAction(requeue().endpoint("/a/../b"))));
	}

	@Test void a09_nonSameOriginForm_throws() {
		assertEquals("ChartDef 'orders' onClick action form must be a same-origin path; got 'https://evil.example/f'.",
			fail(() -> chart().onClickAction(requeue().present(RowAction.Present.DIALOG).form("https://evil.example/f"))));
	}

	@Test void a10_presentInline_throws() {
		assertEquals("ChartDef 'orders' onClick action present must be 'dialog' or absent; got 'inline'.",
			fail(() -> chart().onClickAction(requeue().present(RowAction.Present.INLINE))));
	}

	@Test void a11_presentPage_throws() {
		assertEquals("ChartDef 'orders' onClick action present must be 'dialog' or absent; got 'page'.",
			fail(() -> chart().onClickAction(requeue().present(RowAction.Present.PAGE))));
	}

	@Test void a12_onSuccessNavigate_throws() {
		assertEquals("ChartDef 'orders' onClick action onSuccess must not be 'navigate'; a chart has nowhere to navigate.",
			fail(() -> chart().onClickAction(requeue().onSuccess(RowAction.OnSuccess.NAVIGATE))));
	}

	@Test void a13_presentDialog_withAndWithoutForm_passesAndRoundTrips() {
		var plain = (Map<?,?>)((Map<?,?>)body(chart().onClickAction(requeue().present(RowAction.Present.DIALOG))).get("onClick")).get("action");
		assertEquals("dialog", plain.get("present"));
		assertNull(plain.get("form"));
		var withForm = (Map<?,?>)((Map<?,?>)body(chart().onClickAction(requeue().present(RowAction.Present.DIALOG).form("/forms/requeue"))).get("onClick")).get("action");
		assertEquals("/forms/requeue", withForm.get("form"));
	}

	@Test void a14_confirmString_roundTripsAsString_noConfirm_roundTripsFalse() {
		var s = (Map<?,?>)((Map<?,?>)body(chart().onClickAction(requeue().confirm("Sure?"))).get("onClick")).get("action");
		assertEquals("Sure?", s.get("confirm"));
		var n = (Map<?,?>)((Map<?,?>)body(chart().onClickAction(requeue().noConfirm())).get("onClick")).get("action");
		assertEquals(Boolean.FALSE, n.get("confirm"));
	}

	private static RowAction dlg(String id) {
		return RowAction.create(id).label(id).endpoint("/api/" + id).method(RowAction.Method.POST).present(RowAction.Present.DIALOG);
	}

	@Test void a15_actions_roundTrip_andAccumulateAcrossCalls() {
		var m = body(chart().actions(dlg("a")).actions(dlg("b"), dlg("c")));
		var l = (List<?>)m.get("actions");
		assertSize(3, l);
		assertEquals("a", ((Map<?,?>)l.get(0)).get("id"));
		assertEquals("c", ((Map<?,?>)l.get(2)).get("id"));
	}

	@Test void a16_actions_nonDialogEntry_throws() {
		assertEquals("ChartDef 'orders' actions[1] 'requeue' must be present=dialog; a dialog button can only open a dialog action.",
			fail(() -> chart().actions(dlg("a"), requeue())));
	}

	@Test void a17_duplicateId_catalogVsCatalog_vsOnClickAction_vsOnTopicAction_throws() {
		var dup = "ChartDef 'orders' action id 'requeue' is declared more than once.";
		assertEquals(dup, fail(() -> chart().actions(dlg("requeue"), dlg("requeue"))));
		assertEquals(dup, fail(() -> chart().onClickAction(requeue()).actions(dlg("requeue"))));
		assertEquals(dup, fail(() -> chart().actions(dlg("requeue")).onClickAction(requeue())));
		assertEquals(dup, fail(() -> chart().on("m", "pet.sel").onTopicAction("pet.sel", requeue()).actions(dlg("requeue"))));
		assertEquals(dup, fail(() -> chart().on("m", "pet.sel").onClickAction(requeue()).onTopicAction("pet.sel", requeue())));
	}

	@Test void a18_actions_nullEntry_throws() {
		assertEquals("ChartDef 'orders' actions entries must not be null.", fail(() -> chart().actions((RowAction)null)));
		assertEquals("ChartDef 'orders' actions entries must not be null.", fail(() -> chart().actions(dlg("a"), null)));
		assertEquals("ChartDef 'orders' onClickAction requires an action.", fail(() -> chart().onClickAction(null)));
	}

	@Test void a19_everyPerActionRule_appliesToCatalogEntries() {
		assertTrue(fail(() -> chart().actions(dlg("a").endpoint("servlet:/x"))).contains("actions[0] endpoint must be a same-origin path"));
		assertTrue(fail(() -> chart().actions(dlg("a").form("https://evil.example/f"))).contains("actions[0] form must be a same-origin path"));
		assertTrue(fail(() -> chart().actions(dlg("a").onSuccess(RowAction.OnSuccess.NAVIGATE))).contains("actions[0] onSuccess must not be 'navigate'"));
		assertTrue(fail(() -> chart().actions(dlg("a").endpoint(null))).contains("actions[0] requires a non-blank endpoint"));
	}

	@Test void a20_set_actions_and_set_topicActions_throw() {
		assertEquals("ChartDef 'orders' key 'actions' has a dedicated method.", fail(() -> chart().set("actions", List.of())));
		assertEquals("ChartDef 'orders' key 'topicActions' has a dedicated method.", fail(() -> chart().set("topicActions", List.of())));
	}

	@Test void a21_methodMissing_throws() {
		assertEquals("ChartDef 'orders' onClick action method must be POST, PUT, PATCH or DELETE; got 'null'.",
			fail(() -> chart().onClickAction(RowAction.create("x").endpoint("/api/x"))));
	}
}
