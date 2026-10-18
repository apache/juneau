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

import java.util.*;

import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.RowAction.*;
import org.apache.juneau.rest.server.views.TableSpec.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Builder twin of the Store page (P1, {@code store.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, StorePageBuilders.<jsm>store</jsm>(QuickStats.<jsm>of</jsm>(store())));
 * </p>
 */
@SuppressWarnings({
	"java:S1192" // Page-definition ids and labels repeat as inline literals by design.
})
public final class StorePageBuilders {

	private StorePageBuilders() {}

	/**
	 * The Store page's quick-stat tiles, shared by both render paths.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>var</jk> <jv>qs</jv> = QuickStats.<jsm>of</jsm>(<jv>store</jv>);
	 * </p>
	 *
	 * @param available Available pets.
	 * @param pending Pending pets.
	 * @param sold Sold pets.
	 * @param ordersPlaced Placed orders.
	 * @param ordersApproved Approved orders.
	 * @param ordersDelivered Delivered orders.
	 * @param petTotal All pets, the inventory meter's maximum.
	 */
	public record QuickStats(long available, long pending, long sold, long ordersPlaced, long ordersApproved, long ordersDelivered, long petTotal) {

		/**
		 * Counts the tiles from the store.
		 *
		 * @param store The store.
		 * @return The tiles.
		 */
		public static QuickStats of(PetStore store) {
			var pets = store.getPets();
			var orders = store.getOrders();
			return new QuickStats(count(pets, PetStatus.AVAILABLE), count(pets, PetStatus.PENDING), count(pets, PetStatus.SOLD),
				count(orders, OrderStatus.PLACED), count(orders, OrderStatus.APPROVED), count(orders, OrderStatus.DELIVERED), pets.size());
		}

		private static long count(Collection<Pet> pets, PetStatus status) {
			return pets.stream().filter(x -> x.getStatus() == status).count();
		}

		private static long count(Collection<Order> orders, OrderStatus status) {
			return orders.stream().filter(x -> x.getStatus() == status).count();
		}
	}

	/** The store strip: three status tiles, an order breakdown, and an inventory meter (a tile when there are no pets). */
	private static QuickStat[] storeQuickStats(QuickStats qs) {
		var inventory = qs.petTotal() > 0
			? QuickStat.meter("inventory", "Inventory", qs.available(), qs.petTotal()).text(qs.available() + " of " + qs.petTotal())
			: QuickStat.of("inventory", "Inventory", "0 of 0");
		return new QuickStat[] {
			QuickStat.of("available", "Available", qs.available()),
			QuickStat.of("pending", "Pending", qs.pending()),
			QuickStat.of("sold", "Sold", qs.sold()),
			QuickStat.segmented("orders", "Orders").segment("Placed", qs.ordersPlaced())
				.segment("Approved", qs.ordersApproved(), StatusTone.INFO).segment("Delivered", qs.ordersDelivered(), StatusTone.SUCCESS),
			inventory
		};
	}

	/**
	 * P1 Store.
	 *
	 * @param qs The quick-stat tiles.
	 * @return The page spec.
	 */
	public static PageSpec store(QuickStats qs) {
		return PageSpec.create().tab("store").toolkit("views").init("/console/dev/cards/petstore-cards.js")
			.html("welcome", "Welcome to the petstore", """
				<div class="jc-prose"><p>Browse pets, place orders and review what changed. Every page here is built from the same
				Juneau console pieces your own app can use.</p>
				<p data-slot="welcome-note">Signed in as a guest.</p></div>""")
			.table(TableSpec.create("stats").rowType("Order").dataMode(DataMode.CLIENT).dataUrl("/console/store/recent-orders")
				.columns(
					Column.create("id").label("Order").href("/console/pets/{petId}/orders/{id}"),
					Column.create("petId").label("Pet").href("/console/pets/{petId}/summary"),
					Column.create("shipDate").label("Ship date").render("date"),
					Column.create("status").label("Status").render("pill"))
				.quickStats("store", storeQuickStats(qs)), "Recent orders")
			.card(CardSpec.of("kpi", "revenue").title("Revenue")
				.body("label", "Revenue today").body("dataUrl", "/console/store/kpi/revenue"))
			.card(ChartDef.line("orders-trend").title("Orders, last 30 days").src("/console/store/stats/daily")
				.x("day", null, "Day").series("placed", "Placed").series("delivered", "Delivered", "ok")
				.onClickHref("/console/orders?state=day({day})").toCardSpec())
			.card(ChartDef.donut("by-species").title("Pets by species").src("/console/store/stats/species")
				.label("species").value("count").center("total").toCardSpec())
			.card(ChartDef.bar("by-status").title("Pets by status").src("/console/store/stats/status")
				.x("status").series("count", "Pets").toCardSpec())
			.card(ChartDef.treemap("pets-map").title("Pets by species and status").src("/console/store/stats/pets")
				.groupBy("species", "status").label("name")
				.colorBy("status", ChartDef.legendItem("AVAILABLE", "Available", "ok"),
					ChartDef.legendItem("PENDING", "Pending", "warning"),
					ChartDef.legendItem("SOLD", "Sold", null))
				.tileFields("id", "name", "species", "status")
				.onClickAction(PetsPageBuilders.editPetAction(OnSuccess.REDRAW)).toCardSpec())
			.htmlSrc("hours", "Store hours", "/console/store/fragments/hours");
	}
}
