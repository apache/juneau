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

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.test.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.dto.*;
import org.junit.jupiter.api.*;

/**
 * The Custom cards page: a JSON-only {@code kpi} card, a Java-validated {@code inventory-gauge} card, the page script
 * that registers both, and the two data endpoints they read.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class CustomCardsRest_Test extends TestBase {

	private static long count(Species species, PetStatus status) {
		return store().getPets().stream().filter(p -> (species == null || p.getSpecies() == species) && p.getStatus() == status).count();
	}

	private static JsonMap json(String path) throws Exception {
		return JsonMap.ofString(client().get(path).accept("application/json").run().assertStatus(200).getContent().asString());
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - page
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_cardsInOrder() throws Exception {
		assertPage(page(client(), "/console/dev/cards"))
			.isValid()
			.hasActiveNav("dev", "cards")
			.hasCardOrder("available", "gauge", "toolbar", "how");
	}

	@Test void a02_kpiCardPassesItsBodyThrough() throws Exception {
		assertPage(page(client(), "/console/dev/cards"))
			.hasCard("available", "kpi")
			.hasCardKey("available", "/label", "Pets available")
			.hasCardKey("available", "/dataUrl", "/console/dev/cards/stats/available");
	}

	@Test void a03_gaugeCardCarriesTheShapedFragment() throws Exception {
		assertPage(page(client(), "/console/dev/cards"))
			.hasCard("gauge", "inventory-gauge")
			.hasCardKey("gauge", "/gauge/species", "DOG")
			.hasCardKey("gauge", "/gauge/low", 20)
			.hasCardKey("gauge", "/gauge/high", 120)
			.hasCardKey("gauge", "/gauge/dataUrl", "/console/dev/cards/inventory?species=DOG");
	}

	@Test void a04_pageLoadsItsScript() throws Exception {
		assertContains("/console/dev/cards/petstore-cards.js", page(client(), "/console/dev/cards"));
	}

	@Test void a05_scriptIsServedAndQueuesBothTypes() throws Exception {
		client().get("/console/dev/cards/petstore-cards.js").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("javascript")
			.assertContent().isContains("window.JuneauConsoleCards", "'kpi'", "'inventory-gauge'", "juneau:card-mounted");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - data endpoints
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_availableCount() throws Exception {
		assertBean(json("/console/dev/cards/stats/available"), "label,value", "Pets available," + count(null, PetStatus.AVAILABLE));
	}

	@Test void b02_inventoryBySpecies() throws Exception {
		assertBean(json("/console/dev/cards/inventory?species=DOG"), "species,available,pending,sold",
			"DOG," + count(Species.DOG, PetStatus.AVAILABLE) + "," + count(Species.DOG, PetStatus.PENDING) + "," + count(Species.DOG, PetStatus.SOLD));
	}

	@Test void b03_speciesIsCaseInsensitive() throws Exception {
		assertBean(json("/console/dev/cards/inventory?species=dog"), "species", "DOG");
	}

	@Test void b04_unknownSpeciesIs400() throws Exception {
		client().get("/console/dev/cards/inventory?species=DRAGON").accept("application/json").run()
			.assertStatus(400).assertContent().isContains("Unknown species 'DRAGON'");
	}

	@Test void b05_missingSpeciesIs400() throws Exception {
		client().get("/console/dev/cards/inventory").accept("application/json").run()
			.assertStatus(400).assertContent().isContains("Missing 'species'");
	}
}
