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
package org.apache.juneau.petstore.console.pets;

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.mock.classic.*;
import org.junit.jupiter.api.*;

/**
 * P4: one pet at nav depth 4 (Summary with inline edit, Photo, Orders with a row detail and a selected order, History).
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetDetail_Test extends TestBase {

	private static String html(MockRestClient c, String path, int status) throws Exception {
		return c.get(path).accept("text/html").run().assertStatus(status).getContent().asString();
	}

	private static JsonMap json(MockRestClient c, String path) throws Exception {
		return new JsonMap(c.get(path).accept("application/json").run().assertStatus(200).getContent().asString());
	}

	private static JsonList jsonList(MockRestClient c, String path) throws Exception {
		return new JsonList(c.get(path).accept("application/json").run().assertStatus(200).getContent().asString());
	}

	private static long create(MockRestClient c, String name) throws Exception {
		return new JsonMap(c.post("/console/pets", Json.of(Map.of("name", name, "price", 3))).contentType("application/json").accept("application/json")
			.run().assertStatus(200).getContent().asString()).getMap("row").getLong("id");
	}

	/** @return The first seeded order: {@code [petId, orderId]}. */
	private static long[] seededOrder(MockRestClient c) throws Exception {
		var o = jsonList(c, "/petstore/orders").getMap(0);
		return new long[] { o.getLong("petId"), o.getLong("id") };
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - nav depth and tabs
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_bareIdRedirectsToSummary() throws Exception {
		rawClient().get("/console/pets/1").run().assertStatus(303).assertHeader("Location").isContains("/console/pets/1/summary");
	}

	@Test void a02_summaryAtDepthThree() throws Exception {
		assertPage(page(client(), "/console/pets/1/summary")).isValid()
			.hasActiveNav("pets", "p1", "summary")
			.hasNavChildren("pets/p1", "summary", "photo", "orders", "history")
			.hasNavHref("pets/p1/orders", "/console/pets/1/orders")
			.hasTitle("Mr. Frisky")
			.hasCard("pet", "datatables")
			.hasCardKey("pet", "/table/dataUrl", "/console/pets/1/row");
	}

	@Test void a03_orderAtDepthFour() throws Exception {
		var c = client();
		var o = seededOrder(c);
		var pet = "p" + o[0];
		var order = "o" + o[1];
		var html = page(c, "/console/pets/" + o[0] + "/orders/" + o[1]);
		assertPage(html).isValid()
			.hasActiveNav("pets", pet, "orders", order)
			.hasNavPath("pets", pet, "orders", order)
			.hasCardOrder("pet-orders", "order")
			.hasCard("order", "html");
	}

	@Test void a04_ordersTabWithNothingSelected() throws Exception {
		assertPage(page(client(), "/console/pets/1/orders")).isValid()
			.hasActiveNav("pets", "p1", "orders")
			.hasNavChildren("pets/p1/orders")
			.hasCardOrder("pet-orders");
	}

	@Test void a05_unknownTab404() throws Exception {
		client().get("/console/pets/1/nope").run().assertStatus(404).assertContent().isContains("Unknown tab 'nope'");
	}

	@Test void a06_literalSegmentsBeatId() throws Exception {
		var c = client();
		assertPage(page(c, "/console/pets/sold")).hasCard("sold", "datatables");
		assertPage(page(c, "/console/pets/changes")).hasCard("changes", "datatables");
		assertPage(page(c, "/console/pets")).hasCard("pets", "datatables");
	}

	@Test void a07_otherPagesHaveNoViewerFacts() throws Exception {
		assertNotContains(() -> "only P4 pages seed the viewer facts", "\"viewer\"", page(client(), "/console/pets"));
	}

	@Test void a08_hostileNameIsEscaped() throws Exception {
		var c = client();
		var name = "</script><img src=x onerror=1>\"'";
		var id = create(c, name);
		var order = new JsonMap(c.post("/petstore/orders", "{\"petId\":" + id + ",\"status\":\"PLACED\"}").contentType("application/json")
			.accept("application/json").run().assertStatus(200).getContent().asString()).getLong("id");
		// The builder twin hand-builds the photo and order cards' markup, so it is checked too.
		for (var query : List.of("", "?builder"))
			for (var tab : List.of("summary", "photo", "orders/" + order)) {
				var path = "/console/pets/" + id + "/" + tab + query;
				var html = page(c, path);
				assertPage(html).isValid().hasTitle(name);
				assertNotContains(() -> "raw markup from the pet name on " + path, "<img src=x", html);
			}
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - not found
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_unknownPetRendersNotFoundCard() throws Exception {
		var html = html(client(), "/console/pets/99999/summary", 404);
		assertPage(html).isValid().hasActiveNav("pets").hasCard("not-found", "html");
		assertContains("Unknown pet &#39;99999&#39;", html);
	}

	@Test void b02_unknownOrder404() throws Exception {
		client().get("/console/pets/1/orders/99999").run().assertStatus(404).assertContent().isContains("Unknown order '99999'");
	}

	@Test void b03_orderOfAnotherPet404() throws Exception {
		var c = client();
		var o = seededOrder(c);
		var other = create(c, "Orderless");
		c.get("/console/pets/" + other + "/orders/" + o[1]).run().assertStatus(404).assertContent().isContains("Unknown order '" + o[1] + "'");
		c.get("/console/pets/" + other + "/orders/" + o[1] + "/detail").run().assertStatus(404);
	}

	@Test void b04_unknownPetBeatsUnknownOrder() throws Exception {
		var html = html(client(), "/console/pets/99999/orders/1", 404);
		assertPage(html).isValid().hasCard("not-found", "html");
		assertContains("Unknown pet &#39;99999&#39;", html);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// c - R11 inline edit (stage)
	//-----------------------------------------------------------------------------------------------------------------

	@Test void c01_inlineEditEnvelopeStages() throws Exception {
		// The 'edit' renderer commits {fields:{<column>:value}} through the row action's endpoint.
		var c = client();
		var id = create(c, "Inline");
		var r = new JsonMap(c.post("/console/pets/" + id + "/stage", "{\"action\":\"reprice\",\"targetId\":\"" + id + "\",\"fields\":{\"price\":\"44.5\"}}")
			.contentType("application/json").accept("application/json").run().assertStatus(200).getContent().asString());
		assertBean(r, "outcome,row{petId,field,newValue,state,author}", "success,{" + id + ",price,44.5,PENDING,anonymous}");
		assertBean(json(c, "/petstore/pets/" + id), "name,price", "Inline,3.0");
	}

	@Test void c02_inlineEditEnvelopeTakesExactlyOneField() throws Exception {
		var c = client();
		var id = create(c, "Twofield");
		c.post("/console/pets/" + id + "/stage", "{\"fields\":{\"name\":\"a\",\"price\":\"1\"}}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Exactly one field can be staged at a time; got '2'");
		c.post("/console/pets/" + id + "/stage", "{\"fields\":{}}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Exactly one field can be staged at a time; got '0'");
	}

	@Test void c03_inlineEditEnvelopeValidates() throws Exception {
		var c = client();
		var id = create(c, "Validated");
		c.post("/console/pets/" + id + "/stage", "{\"fields\":{\"price\":\"-1\"}}").contentType("application/json").run()
			.assertStatus(400).assertContent().isContains("Price '-1' must be a non-negative number");
		c.post("/console/pets/99999/stage", "{\"fields\":{\"name\":\"x\"}}").contentType("application/json").run()
			.assertStatus(404).assertContent().isContains("Unknown pet '99999'");
	}

	@Test void c04_summaryEditColumnsUseTheStageActions() throws Exception {
		var t = assertPage(page(client(), "/console/pets/1/summary")).card("pet").getMap("table");
		assertBean(t, "dataMode,rowActions{#{id,endpoint,method}}", "client,{[{rename,/console/pets/{id}/stage,POST},{reprice,/console/pets/{id}/stage,POST},{sell,/console/pets/{id}/sell,POST}]}");
		// present=inline: the pencil edit cell commits them and the row menu omits them (and carry no form).
		// A null form is omitted from the wire, which BCT reports as a missing property rather than null.
		assertFalse(t.getList("rowActions").getMap(0).containsKey("form"));
		assertBean(t.getList("rowActions").getMap(0), "present", "inline");
		// A null form is omitted from the wire, which BCT reports as a missing property rather than null.
		assertFalse(t.getList("rowActions").getMap(1).containsKey("form"));
		assertBean(t.getList("rowActions").getMap(1), "present", "inline");
		var name = t.getList("columns").getMap(1);
		assertBean(name, "key,render{id,meta{action,column}}", "name,{edit,{rename,name}}");
	}

	@Test void c05_stageRejectsAMissingField() throws Exception {
		// A row-menu click with no dialog sends only the action envelope.
		var c = client();
		var id = create(c, "Fieldless");
		for (var body : List.of("{\"action\":\"rename\",\"targetId\":\"" + id + "\"}", "{\"field\":\"\",\"value\":\"x\"}", "{\"fields\":{\" \":\"x\"}}"))
			c.post("/console/pets/" + id + "/stage", body).contentType("application/json").run()
				.assertStatus(400).assertContent().isContains("Missing field to stage");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// d - R4 visibility, R12 related list, history
	//-----------------------------------------------------------------------------------------------------------------

	@Test void d01_photoUploadOmittedForNonAdmin() throws Exception {
		var html = page(client(), "/console/pets/1/photo");
		assertPage(html).isValid().hasActiveNav("pets", "p1", "photo").hasCardOrder("photo");
		assertContains("\"viewer\":{\"roles\":[]}", html);
	}

	@Test void d02_photoUploadShownForAdmin() throws Exception {
		var html = client().get("/console/pets/1/photo").accept("text/html").header("Authorization", "Bearer petstore-admin").run()
			.assertStatus(200).getContent().asString();
		assertPage(html).isValid().hasCardOrder("photo", "photo-upload").hasCard("photo-upload", "html")
			.templateContains("photo", "/petstore/pets/1/photo")
			.templateContains("photo-upload", "data-petstore-upload=\"/petstore/pets/1/photo\"");
		assertContains("\"viewer\":{\"roles\":[\"admin\"]}", html);
	}

	@Test void d03_summaryRowIsThisPet() throws Exception {
		var c = client();
		var list = jsonList(c, "/console/pets/1/row");
		assertSize(1, list);
		assertBean(list.getMap(0), "id,name", "1,Mr. Frisky");
		c.get("/console/pets/99999/row").run().assertStatus(404);
	}

	@Test void d04_orderDetailEnvelope() throws Exception {
		var c = client();
		var o = seededOrder(c);
		assertBean(json(c, "/console/pets/" + o[0] + "/orders/" + o[1] + "/detail"), "contractVersion,fields{id,petId}", "1,{" + o[1] + "," + o[0] + "}");
	}

	@Test void d05_ordersTabDeclaresTheRowDetail() throws Exception {
		assertPage(page(client(), "/console/pets/1/orders"))
			.hasCardKey("pet-orders", "/table/dataUrl", "/console/pets/1/orders/rows")
			.hasCardKey("pet-orders", "/table/detail/endpoint", "/console/pets/1/orders/{id}/detail");
	}

	@Test void d06_historyTab() throws Exception {
		var c = client();
		var id = create(c, "Chronicled");
		c.put("/console/pets/" + id, "{\"name\":\"Chronicled\",\"price\":40}").contentType("application/json").run().assertStatus(200);
		assertPage(page(c, "/console/pets/" + id + "/history")).isValid().hasActiveNav("pets", "p" + id, "history")
			.hasCardKey("history", "/table/dataUrl", "/console/pets/" + id + "/history/rows");
		var list = jsonList(c, "/console/pets/" + id + "/history/rows");
		assertSize(2, list);
		for (var row : list.elements(JsonMap.class))
			assertBean(row, "entity,entityId", "Pet," + id);
	}
}
