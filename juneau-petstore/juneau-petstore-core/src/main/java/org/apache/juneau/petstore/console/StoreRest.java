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
package org.apache.juneau.petstore.console;

import java.time.*;
import java.util.*;
import java.util.stream.*;

import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.console.pages.StorePageBuilders.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * The Store page, the petstore dashboard, and the feeds its charts, tiles and fragments read.
 *
 * <p>
 * The page is {@code store.ftlh}, with a builder twin at {@code ?builder}.  The feeds are read-only JSON over the
 * live store, and the revenue tile counts days by the store's clock in UTC.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<ja>@RestGet</ja>(path=<js>"/"</js>)
 * 	<jk>public</jk> View page() { <jk>return</jk> FreemarkerView.<jsm>of</jsm>(<js>"store.ftlh"</js>); }
 * </p>
 * <p class='bcode'>
 * 	&lt;@page tab="store" toolkit="views"&gt;
 * 	  &lt;@card id="welcome" title="Welcome to the petstore"&gt;&lt;div class="jc-prose"&gt;...&lt;/div&gt;&lt;/@card&gt;
 * 	  &lt;@card type="chart" id="by-status" title="Pets by status" src="/console/store/stats/status"&gt;
 * 	  { kind: 'bar', x: { field: 'status' }, series: [ { field: 'count', label: 'Pets' } ] }
 * 	  &lt;/@card&gt;
 * 	&lt;/@page&gt;
 * </p>
 * <p class='bcode'>
 * 	GET /console/store                   the page
 * 	GET /console/store/recent-orders     the ten newest orders
 * 	GET /console/store/stats/daily       {day, placed, delivered} for each recent day
 * 	GET /console/store/kpi/revenue       {label, value, unit} for the revenue tile
 * 	GET /console/store/stats/species     {species, count} for each species
 * 	GET /console/store/stats/status      {status, count} for each status
 * 	GET /console/store/stats/pets        {id, name, species, status} for each pet
 * 	GET /console/store/fragments/hours   the opening hours, as markup only
 * </p>
 * <p class='bcode'>
 * 	&lt;@badge id="pending" src="/console/pets/changes/pending" href="/console/pets/changes" refreshMs=60000/&gt;
 * </p>
 */
@Rest(path="/store", title="Store")
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class StoreRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * Renders the Store page, with the quick-stat tile values computed from the store.
	 *
	 * @param req The request; {@code ?builder} renders the builder twin.
	 * @return The page view.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		var qs = QuickStats.of(store());
		if (builder(req))
			return twin(req, StorePageBuilders.store(qs));
		return FreemarkerView.of("store.ftlh")
			.attr("qsAvailable", qs.available())
			.attr("qsPending", qs.pending())
			.attr("qsSold", qs.sold())
			.attr("qsOrdersPlaced", qs.ordersPlaced())
			.attr("qsOrdersApproved", qs.ordersApproved())
			.attr("qsOrdersDelivered", qs.ordersDelivered())
			.attr("qsPetTotal", qs.petTotal());
	}

	/** @return The 10 most recent orders, by ship date then id, newest first. */
	@RestGet(path="/recent-orders")
	public List<Order> recentOrders() {
		return store().getOrders().stream()
			.sorted(Comparator.comparing(Order::getShipDate, Comparator.nullsFirst(Comparator.<String>naturalOrder())).thenComparingLong(Order::getId).reversed())
			.limit(10).toList();
	}

	/** @return 30 rows {@code {day, placed, delivered}} ending today, oldest first; an order counts on its ship date. */
	@RestGet(path="/stats/daily")
	public JsonList daily() {
		var today = LocalDate.ofInstant(store().clock().instant(), ZoneOffset.UTC);
		var placed = new HashMap<String,Integer>();
		var delivered = new HashMap<String,Integer>();
		for (var o : store().getOrders()) {
			if (o.getStatus() == OrderStatus.DELIVERED)
				delivered.merge(o.getShipDate(), 1, Integer::sum);
			else
				placed.merge(o.getShipDate(), 1, Integer::sum);
		}
		var rows = new JsonList();
		for (var i = 29; i >= 0; i--) {
			var day = today.minusDays(i).toString();
			rows.add(JsonMap.of("day", day, "placed", placed.getOrDefault(day, 0), "delivered", delivered.getOrDefault(day, 0)));
		}
		return rows;
	}

	/**
	 * The revenue tile: the summed pet prices of the orders delivered today, by the store clock in UTC.
	 *
	 * @return {@code {label, value, unit}}, with the value rounded to cents.
	 */
	@RestGet(path="/kpi/revenue")
	public JsonMap revenue() {
		var today = LocalDate.ofInstant(store().clock().instant(), ZoneOffset.UTC).toString();
		var total = 0d;
		for (var o : store().getOrders()) {
			if (o.getStatus() == OrderStatus.DELIVERED && today.equals(o.getShipDate())) {
				var p = store().getPet(o.getPetId());
				if (p != null)
					total += p.getPrice();
			}
		}
		return JsonMap.of("label", "Revenue today", "value", Math.round(total * 100) / 100d, "unit", "USD");
	}

	/** @return One row {@code {species, count}} per species, in enum order, zero counts included. */
	@RestGet(path="/stats/species")
	public JsonList species() {
		var counts = store().getPets().stream().collect(Collectors.groupingBy(Pet::getSpecies, Collectors.counting()));
		var rows = new JsonList();
		for (var s : Species.values())
			rows.add(JsonMap.of("species", s.name(), "count", counts.getOrDefault(s, 0L)));
		return rows;
	}

	/** @return One row {@code {status, count}} per pet status, in enum order. */
	@RestGet(path="/stats/status")
	public JsonList status() {
		var counts = store().getPets().stream().collect(Collectors.groupingBy(Pet::getStatus, Collectors.counting()));
		var rows = new JsonList();
		for (var s : PetStatus.values())
			rows.add(JsonMap.of("status", s.name(), "count", counts.getOrDefault(s, 0L)));
		return rows;
	}

	/** @return One row {@code {id, name, species, status}} per pet, by id; the treemap groups them and opens the edit dialog from a tile. */
	@RestGet(path="/stats/pets")
	public JsonList pets() {
		var rows = new JsonList();
		store().getPets().stream().sorted(Comparator.comparingLong(Pet::getId)).forEach(p ->
			rows.add(JsonMap.of("id", p.getId(), "name", p.getName(), "species", p.getSpecies() == null ? null : p.getSpecies().name(), "status", p.getStatus() == null ? null : p.getStatus().name())));
		return rows;
	}

	/** @return The store hours as a chrome-free fragment. */
	@RestGet(path="/fragments/hours")
	public View hours() {
		return FreemarkerView.of("fragments/store-hours.ftlh");
	}
}
