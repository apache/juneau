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
package org.apache.juneau.petstore.console.pages;

import static org.apache.juneau.commons.utils.StringUtils.*;

import java.util.*;

import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.RowAction.*;
import org.apache.juneau.rest.server.views.TableSpec.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Builder twins of the Pets pages: P2 Pets ({@code pets.ftlh}), its {@code ?mixed} variant, P3 Sold
 * ({@code pets-sold.ftlh}) and P4 one pet ({@code pet.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, PetsPageBuilders.<jsm>pets</jsm>());
 * </p>
 */
public final class PetsPageBuilders {

	/**
	 * The edit-pet dialog action, shared by the Pets table row menu and the Store page treemap so the two cannot drift.
	 *
	 * @param onSuccess What the page does after a saved edit; a table merges the row, a chart (which has no row) redraws.
	 * @return The action.
	 */
	static RowAction editPetAction(OnSuccess onSuccess) {
		return RowAction.create("edit").label("Edit").form("/console/pets/form?id={id}").present(Present.DIALOG)
			.endpoint("/console/pets/{id}").method(Method.PUT).onSuccess(onSuccess);
	}

	private PetsPageBuilders() {}

	/**
	 * P2 Pets.
	 *
	 * @return The page spec.
	 */
	public static PageSpec pets() {
		return PageSpec.create().tab("pets").toolkit("views", "datatables-buttons").table(petsTable(), "Pets");
	}

	/**
	 * P2 Pets, half FTL: {@code pets-mixed.ftlh} lays out the page and places this spec's table with
	 * {@code <@card ref="pets"/>}.
	 *
	 * @return The page spec.
	 */
	public static PageSpec petsMixed() {
		return PageSpec.create().template("pets-mixed.ftlh").table(petsTable(), "Pets");
	}

	/**
	 * The P2 table, shared by {@link #pets()} and {@link #petsMixed()} so the two cannot drift.
	 *
	 * @return A new table spec.
	 */
	static TableSpec petsTable() {
		var selection = SelectionDef.create("id").selectAll(true).scope(SelectionDef.Scope.PERSISTENT).labelField("name")
			.selectableWhen(RowActionEnabledRule.of("status", Op.NE, "SOLD", "Already sold."));
		return TableSpec.create("pets").rowType("Pet").dataMode(DataMode.SERVER).dataUrl("/console/pets/query")
			.columnConfig(true)
			.urlState("ids")
			.defaultOrder("id", Sort.ASC)
			.pollIntervalMs(30_000)
			.pausePollingWhileEditing(true)
			.columns(
				Column.create("id").label("Id"),
				Column.create("name").label("Name").href("/console/pets/{id}/summary"),
				Column.create("species").label("Species"),
				Column.create("status").label("Status").render("pill"),
				Column.create("price").label("Price (USD)").render(Render.of("decimal").meta("places", 2)),
				Column.create("tags").label("Tags").defaultVisible(false))
			.rowClass("status", Op.EQ, "SOLD", "is-muted")
			.ribbon(
				RibbonItem.optionGroup("status",
					RibbonItem.option("available").title("Available").column("status").value("$eq(AVAILABLE)"),
					RibbonItem.option("pending").title("Pending").column("status").value("$eq(PENDING)"))
					.title("Pet status").deselectable(true).defaultOption("available"),
				RibbonItem.optionGroup("species",
					RibbonItem.option("mammals").title("Mammals").column("species").value("$in(DOG,CAT,MOUSE,RABBIT)"),
					RibbonItem.option("others").title("Others").column("species").value("$in(BIRD,FISH,SNAKE)"))
					.title("Species").deselectable(true).defaultOption("mammals"),
				RibbonItem.dialog("add-pet").title("+ Add pet").symbol("plus")
					.form("/console/pets/form").endpoint("/console/pets").method(Method.POST).onSuccess(OnSuccess.REDRAW),
				RibbonItem.export("copy", "csv").optional("excel", "pdf"),
				RibbonItem.refresh())
			.rowActions(
				editPetAction(OnSuccess.MERGE_ROW),
				RowAction.create("delete").label("Delete").endpoint("/console/pets/{id}").method(Method.DELETE)
					.confirm("Delete '{name}'?").confirmLabel("Delete").tone(Tone.DANGER).onSuccess(OnSuccess.REDRAW))
			.selection(selection)
			.bulk(BulkMutateDef.create(WritePermit.forCapability("pets.sell"), selection).actions(
				RowAction.create("sell").label("Mark sold").endpoint("/console/pets/{id}/sell").method(Method.POST).bulkMode(BulkMode.PER_ROW)
					.confirmTitle("Mark pets sold").confirm("Mark the selected pets sold?").onSuccess(OnSuccess.REDRAW)));
	}

	/**
	 * P3 Sold.
	 *
	 * @return The page spec.
	 */
	public static PageSpec sold() {
		return PageSpec.create().toolkit("views")
			.table(TableSpec.create("sold").rowType("Pet").dataMode(DataMode.CLIENT).dataUrl("/console/pets/sold/rows")
				.columns(
					Column.create("id").label("Id"),
					Column.create("name").label("Name"),
					Column.create("species").label("Species"),
					Column.create("price").label("Price (USD)").render(Render.of("decimal").meta("places", 2)))
				.ribbon(
					RibbonItem.optionGroup("species",
						RibbonItem.option("dogs").title("Dogs").column("species").value("$eq(DOG)"),
						RibbonItem.option("cats").title("Cats").column("species").value("$eq(CAT)"))
						.title("Species"),
					RibbonItem.refresh()), "Sold pets");
	}

	/**
	 * P4 one pet, on one tab.
	 *
	 * @param pet The pet.
	 * @param tab The tab: {@code summary}, {@code photo}, {@code orders} or {@code history}.
	 * @param activeNav The active nav path, {@code pets/p<id>/<tab>} or {@code pets/p<id>/orders/o<orderId>}.
	 * @param order The selected order on the orders tab, or <jk>null</jk>.
	 * @param roles The viewer's roles.
	 * @param apiCsrf The API's CSRF token for the photo upload form, or <jk>null</jk> to render the form without one.
	 * @return The page spec.
	 */
	public static PageSpec pet(Pet pet, String tab, String activeNav, Order order, List<String> roles, String apiCsrf) {
		var id = pet.getId();
		var href = "/console/pets/" + id;
		var p = PageSpec.create().tab(activeNav).toolkit("views").title(pet.getName())
			.facts(Map.of("viewer", Map.of("roles", roles)))
			.navUnder("pets", n -> {
				var node = n.add("p" + id, pet.getName(), href + "/summary");
				node.add("summary", "Summary", href + "/summary");
				node.add("photo", "Photo", href + "/photo");
				var orders = node.add("orders", "Orders", href + "/orders");
				if (order != null)
					orders.add("o" + order.getId(), "Order " + order.getId(), href + "/orders/" + order.getId());
				node.add("history", "History", href + "/history");
			});
		var name = escapeHtml(pet.getName());
		switch (tab) {
			case "summary" -> p.table(TableSpec.create("pet").rowType("Pet").dataMode(DataMode.CLIENT).dataUrl(href + "/row")
				.columns(
					Column.create("id").label("Id"),
					Column.create("name").label("Name").render(Render.of("edit").meta("action", "rename").meta("column", "name").meta("input", "text")),
					Column.create("species").label("Species"),
					Column.create("status").label("Status").render("pill"),
					Column.create("price").label("Price (USD)").render(Render.of("edit").meta("action", "reprice").meta("column", "price").meta("input", "number")))
				.rowActions(
					stage("rename", "Rename"),
					stage("reprice", "Reprice"),
					RowAction.create("sell").label("Mark sold").endpoint("/console/pets/{id}/sell").method(Method.POST).onSuccess(OnSuccess.REDRAW)
						.confirm("Mark '{name}' sold?").visibleWhen(VisibilityRule.when("status").ne("SOLD")))
				.ribbon(RibbonItem.refresh()), pet.getName());
			case "photo" -> {
				p.html("photo", "Photo", """
					<figure class="jc-figure">
					<object data="/petstore/pets/%1$s/photo" type="image/png" aria-label="Photo of %2$s"><p>No photo uploaded for %2$s yet.</p></object>
					<figcaption>Served by <code>PetPhotoSerializer</code> from <code>GET /petstore/pets/%1$s/photo</code>.</figcaption>
					</figure>""".formatted(id, name));
				if (roles.contains("admin")) {
					var csrf = apiCsrf == null ? "" : " data-petstore-csrf=\"%s\"".formatted(escapeHtml(apiCsrf));
					p.html("photo-upload", "Upload photo", """
						<form class="jc-form" data-petstore-upload="/petstore/pets/%s/photo"%s>
						<input type="file" name="photo" accept="image/png,image/jpeg" aria-label="Photo file">
						<button type="submit" class="jc-button">Upload</button>
						</form>""".formatted(id, csrf));
				}
			}
			case "orders" -> {
				p.table(TableSpec.create("pet-orders").rowType("Order").dataMode(DataMode.CLIENT).dataUrl(href + "/orders/rows")
					.columns(
						Column.create("id").label("Order").href(href + "/orders/{id}"),
						Column.create("shipDate").label("Ship date"),
						Column.create("status").label("Status").render("pill"))
					.detail(RowDetail.create(href + "/orders/{id}/detail").title("Order {id}")), "Orders");
				if (order != null)
					p.html("order", "Order " + order.getId(),
						"<dl class=\"jc-details\"><dt>Status</dt><dd>%s</dd><dt>Ship date</dt><dd>%s</dd><dt>Pet</dt><dd>%s</dd></dl>"
							.formatted(order.getStatus(), escapeHtml(order.getShipDate() == null ? "" : order.getShipDate()), name));
			}
			default -> p.table(TableSpec.create("history").rowType("AuditEntry").dataMode(DataMode.CLIENT).dataUrl(href + "/history/rows")
				.defaultOrder("at", Sort.DESC)
				.columns(
					Column.create("at").label("When").render("timestamp"),
					Column.create("actor").label("Actor"),
					Column.create("action").label("Action"),
					Column.create("detail").label("Detail")), "History");
		}
		return p;
	}

	// The pencil edit cell commits it; present=inline keeps it out of the row menu.
	private static RowAction stage(String id, String label) {
		return RowAction.create(id).label(label).endpoint("/console/pets/{id}/stage").method(Method.POST).noConfirm()
			.present(Present.INLINE)
			.visibleWhen(VisibilityRule.when("status").ne("SOLD"));
	}
}
