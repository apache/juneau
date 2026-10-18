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
package org.apache.juneau.petstore.console.dev;

import static java.nio.charset.StandardCharsets.*;

import java.io.*;
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;

/**
 * The Custom cards page, which shows how an adopter writes a card type, and the feeds its cards read.
 *
 * <p>
 * The page has a JSON-only card type ({@code kpi}), a Java-validated one ({@code inventory-gauge}, see
 * {@link InventoryGaugeCardType}) and two charts: the stock bands and the zoomable prices.
 *
 * <p>
 * Both JS handlers live in {@code petstore-cards.js}, served beside this class, which also shows the card API
 * ({@code JuneauConsole.refreshCard}, {@code JuneauConsole.cardApi}) and the {@code juneau:card-mounted} event.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjs'>
 * 	(window.JuneauConsoleCards = window.JuneauConsoleCards || []).push([<js>'kpi'</js>, {
 * 		render(card, el, ctx) { ... }
 * 	}]);
 * </p>
 * <p class='bcode'>
 * 	GET /console/dev/cards                       the page
 * 	GET /console/dev/cards/stats/available       {label, value} for the kpi card
 * 	GET /console/dev/cards/inventory?species=    {species, available, pending, sold} for the gauge
 * 	GET /console/dev/cards/inventory/all         one gauge row per species, for the stock bands chart
 * 	GET /console/dev/cards/prices                {id, price} per pet, for the prices chart
 * 	GET /console/dev/cards/petstore-cards.js     the page script
 * </p>
 */
@Rest(path="/cards", title="Custom cards")
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
	"resource" // The response writer is owned by the servlet container; Eclipse JDT @Owning warning is by design.
})
public class CustomCardsRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * The Custom cards page.
	 *
	 * @param req The request; {@code ?builder} renders the builder twin.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		return builder(req) ? twin(req, DevPageBuilders.cards()) : FreemarkerView.of("cards.ftlh");
	}

	/** @return The kpi card's number: how many pets are available. */
	@RestGet(path="/stats/available")
	public JsonMap available() {
		var n = store().getPets().stream().filter(p -> p.getStatus() == PetStatus.AVAILABLE).count();
		return new JsonMap().append("label", "Pets available").append("value", n);
	}

	/**
	 * Counts one species' pets by status, for the gauge.
	 *
	 * @param species The species name, case-insensitive.
	 * @return {@code {species, available, pending, sold}}.
	 * @throws BadRequest If the species is missing or unknown.
	 */
	@RestGet(path="/inventory")
	public JsonMap inventory(@Query("species") String species) {
		if (species == null)
			throw new BadRequest("Missing 'species'");
		Species s;
		try {
			s = Species.valueOf(species.toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e, "Unknown species '%s'", species);
		}
		var counts = new EnumMap<PetStatus,Long>(PetStatus.class);
		for (var st : PetStatus.values())
			counts.put(st, 0L);
		store().getPets().stream().filter(p -> p.getSpecies() == s).forEach(p -> counts.merge(p.getStatus(), 1L, Long::sum));
		return new JsonMap().append("species", s.name())
			.append("available", counts.get(PetStatus.AVAILABLE))
			.append("pending", counts.get(PetStatus.PENDING))
			.append("sold", counts.get(PetStatus.SOLD));
	}

	/** @return One {@link #inventory(String)} row per species, in enum order, for the stock bands chart. */
	@RestGet(path="/inventory/all")
	public JsonList inventoryAll() {
		var rows = new JsonList();
		Arrays.stream(Species.values()).forEach(s -> rows.add(inventory(s.name())));
		return rows;
	}

	/** @return One row {@code {id, price}} per pet, by id, for the zoomable prices chart. */
	@RestGet(path="/prices")
	public JsonList prices() {
		var rows = new JsonList();
		store().getPets().stream().sorted(Comparator.comparingLong(Pet::getId))
			.forEach(p -> rows.add(new JsonMap().append("id", p.getId()).append("price", p.getPrice())));
		return rows;
	}

	/**
	 * Serves the page-local script.
	 *
	 * @param res The response.
	 * @throws IOException If the bundled script can't be read.
	 */
	@RestGet(path="/petstore-cards.js")
	public void script(RestResponse res) throws IOException {
		try (var in = CustomCardsRest.class.getResourceAsStream("petstore-cards.js")) {
			if (in == null)
				throw new IllegalStateException("Missing classpath resource 'petstore-cards.js' beside " + CustomCardsRest.class.getName());
			res.setContentType("text/javascript;charset=utf-8");
			res.getWriter().write(new String(in.readAllBytes(), UTF_8));
		}
	}
}
