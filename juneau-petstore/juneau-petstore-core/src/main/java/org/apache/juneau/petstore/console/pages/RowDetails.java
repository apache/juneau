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

import java.util.function.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.console.views.TableSpec.*;
import org.apache.juneau.petstore.console.users.UsersRest.*;
import org.apache.juneau.petstore.dto.*;

/**
 * The expandable row details of the console tables, shared by the page builders (which only need the declaration) and
 * the resources that serve them (which pass a real lookup).
 *
 * <p>
 * A bean-backed detail lists its fields explicitly, so the resource that serves it never depends on having been
 * attached to a table.  Each field list equals the columns of the table it opens from, hidden ones included.
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 * 	<jc>// A page builder attaches the declaration; the resource serves it with a store lookup.</jc>
 * 	TableSpec.<jsm>create</jsm>(<js>"orders"</js>).detail(RowDetails.<jsm>order</jsm>());
 * </p>
 */
public final class RowDetails {

	private RowDetails() {}

	private static <T> Function<String,T> none() {
		return id -> null;
	}

	/** @return The Pets table detail, for building a page. */
	public static RowDetail pet() {
		return pet(none());
	}

	/**
	 * The Pets table detail: Details, this pet's orders and its history.
	 *
	 * @param lookup Finds a pet by row id, or returns <jk>null</jk>.
	 * @return A new detail.
	 */
	public static RowDetail pet(Function<String,Pet> lookup) {
		return RowDetail.fromBean(Pet.class, lookup).basePath("/console/pets").title("{name}").columns(3)
			.section("Details", "id", "name", "species", "status", "price", "tags")
			.label("price", "Price (USD)")
			.tab(DetailTab.table("Orders", ordersView("pets-orders", "/console/pets/orders/rows")).id("orders").scopeParam("petId").countFromRows())
			.tab(DetailTab.table("History", historyView()).id("history").scopeParam("petId").countFromRows());
	}

	/** @return The Sold table detail, for building a page. */
	public static RowDetail soldPet() {
		return soldPet(none());
	}

	/**
	 * The Sold table detail: the columns that table shows, and this pet's orders.
	 *
	 * @param lookup Finds a pet by row id, or returns <jk>null</jk>.
	 * @return A new detail.
	 */
	public static RowDetail soldPet(Function<String,Pet> lookup) {
		return RowDetail.fromBean(Pet.class, lookup).key("soldPet").basePath("/console/pets").title("{name}").columns(3)
			.section("Details", "id", "name", "species", "price")
			.label("price", "Price (USD)")
			.tab(DetailTab.table("Orders", ordersView("sold-orders", "/console/pets/orders/rows")).id("orders").scopeParam("petId").countFromRows());
	}

	/** @return The Orders and Store tables' detail, for building a page. */
	public static RowDetail order() {
		return order(none());
	}

	/**
	 * The Orders and Store recent-orders detail: Details and the ordered pet.
	 *
	 * @param lookup Finds an order by row id, or returns <jk>null</jk>.
	 * @return A new detail.
	 */
	public static RowDetail order(Function<String,Order> lookup) {
		return RowDetail.fromBean(Order.class, lookup).basePath("/console/orders").title("Order {id}").columns(2)
			.section("Details", "id", "petId", "shipDate", "status")
			.label("petId", "Pet")
			.tab(DetailTab.table("Pet", TableSpec.create("order-pet").rowType("Pet").dataMode(DataMode.CLIENT).dataUrl("/console/orders/pet/rows")
				.columns(
					Column.create("id").label("Id"),
					Column.create("name").label("Name").href("/console/pets/{id}/summary"),
					Column.create("species").label("Species"),
					Column.create("status").label("Status"),
					Column.create("price").label("Price (USD)").render(Render.of("decimal").meta("places", 2))))
				.id("pet").scopeParam("orderId").countFromRows());
	}

	/** @return The Users table detail, for building a page. */
	public static RowDetail user() {
		return user(none());
	}

	/**
	 * The Users table detail: Details and the orders this user placed.  It serves {@code UserView}, so a password,
	 * email or phone never leaves the server.
	 *
	 * @param lookup Finds a user by row id (the username), or returns <jk>null</jk>.
	 * @return A new detail.
	 */
	public static RowDetail user(Function<String,UserView> lookup) {
		return RowDetail.fromBean(UserView.class, lookup).key("user").basePath("/console/users").title("{username}").columns(2)
			.section("Details", "username", "firstName", "lastName", "userStatus")
			.label("userStatus", "Status")
			.tab(DetailTab.table("Orders", ordersView("user-orders", "/console/users/orders/rows")).id("orders").scopeParam("username").countFromRows());
	}

	/** @return The Pending changes table detail. */
	public static RowDetail change() {
		return RowDetail.inline().title("Change {id}").section("Details", "id", "summary", "author", "state", "message");
	}

	/** @return The Audit table detail. */
	public static RowDetail audit() {
		return RowDetail.inline().title("{action} {entity} {entityId}")
			.section("Details", "id", "at", "actor", "action", "entity", "entityId", "detail");
	}

	/** @return The pet History table detail. */
	public static RowDetail history() {
		return RowDetail.inline().title("{action}").section("Details", "id", "at", "actor", "action", "detail");
	}

	private static TableSpec ordersView(String id, String dataUrl) {
		return TableSpec.create(id).rowType("Order").dataMode(DataMode.CLIENT).dataUrl(dataUrl)
			.columns(
				Column.create("id").label("Order").href("/console/pets/{petId}/orders/{id}").searchType(SearchType.NUMERIC),
				Column.create("shipDate").label("Ship date").render("date").searchType(SearchType.TEXT),
				Column.create("status").label("Status").render(PillRenders.orderStatus()).searchType(SearchType.ENUM));
	}

	private static TableSpec historyView() {
		return TableSpec.create("pets-history").rowType("AuditEntry").dataMode(DataMode.CLIENT).dataUrl("/console/pets/history/rows")
			.columns(
				Column.create("at").label("When").render("timestamp").searchType(SearchType.TEXT),
				Column.create("actor").label("Actor").searchType(SearchType.TEXT),
				Column.create("action").label("Action").render(PillRenders.auditAction()).searchType(SearchType.ENUM),
				Column.create("detail").label("Detail").searchType(SearchType.TEXT));
	}
}
