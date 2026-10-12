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

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.TableSpec.*;

/**
 * Builder twin of the Store page (P1, {@code store.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, StorePageBuilders.<jsm>store</jsm>(QuickStats.<jsm>of</jsm>(store())));
 * </p>
 */
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
	 * @param orders Placed / approved / delivered order counts.
	 * @param inventory Available pets of all pets.
	 */
	public record QuickStats(long available, long pending, long sold, String orders, String inventory) {

		/**
		 * Counts the tiles from the store.
		 *
		 * @param store The store.
		 * @return The tiles.
		 */
		public static QuickStats of(PetStore store) {
			var pets = store.getPets();
			var orders = store.getOrders();
			var available = count(pets, PetStatus.AVAILABLE);
			return new QuickStats(available, count(pets, PetStatus.PENDING), count(pets, PetStatus.SOLD),
				f("%s / %s / %s", count(orders, OrderStatus.PLACED), count(orders, OrderStatus.APPROVED), count(orders, OrderStatus.DELIVERED)),
				f("%s of %s", available, pets.size()));
		}

		private static long count(Collection<Pet> pets, PetStatus status) {
			return pets.stream().filter(x -> x.getStatus() == status).count();
		}

		private static long count(Collection<Order> orders, OrderStatus status) {
			return orders.stream().filter(x -> x.getStatus() == status).count();
		}
	}

	/**
	 * P1 Store.
	 *
	 * @param qs The quick-stat tiles.
	 * @return The page spec.
	 */
	public static PageSpec store(QuickStats qs) {
		return PageSpec.create().tab("store").toolkit("views")
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
				.quickStats("store",
					QuickStat.of("available", "Available", qs.available()),
					QuickStat.of("pending", "Pending", qs.pending()),
					QuickStat.of("sold", "Sold", qs.sold()),
					QuickStat.of("orders", "Orders", qs.orders()),
					QuickStat.of("inventory", "Inventory", qs.inventory())), "Recent orders");
			// Blocked: ChartDef.  The three chart cards (orders-trend, by-species, by-status) go here, in this order.
			// Blocked: PageSpec cannot build an html card with src=, so the "hours" card (src=/console/store/fragments/hours)
			// has no twin yet.
	}
}
