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

import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * The server half of the inventory-gauge custom card: body validation and fragment shaping.
 */
class InventoryGaugeCardType_Test extends TestBase {

	private static final InventoryGaugeCardType H = new InventoryGaugeCardType();

	private static CardSource gauge(String body) {
		return CardSource.create(InventoryGaugeCardType.TYPE, "gauge").body(body).build();
	}

	private static String rejected(String body) {
		var source = gauge(body);
		return assertThrows(IllegalArgumentException.class, () -> H.toFragment(source)).getMessage();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - shape
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_type() {
		assertString("inventory-gauge", H.type());
	}

	@Test void a02_fragment() {
		assertBean(H.toFragment(gauge("{species:'DOG',low:20,high:120}")), "gauge{species,low,high,dataUrl}",
			"{DOG,20,120,/console/dev/cards/inventory?species=DOG}");
	}

	@Test void a03_speciesIsCaseInsensitive() {
		assertBean(H.toFragment(gauge("{species:'cat',low:1,high:2}")), "gauge{species,dataUrl}", "{CAT,/console/dev/cards/inventory?species=CAT}");
	}

	@Test void a04_lowAndHighDefault() {
		assertBean(H.toFragment(gauge("{species:'FISH'}")), "gauge{low,high}", "{0,100}");
	}

	@Test void a05_fragmentIsOnlyTheGaugeKey() {
		assertList(H.toFragment(gauge("{species:'DOG',low:20,high:120}")).keySet(), "gauge");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - validation
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_missingSpecies() {
		assertString("<@card id='gauge'> type='inventory-gauge' requires 'species'.", rejected("{low:1,high:2}"));
	}

	@Test void b02_unknownSpecies() {
		assertString("<@card id='gauge'> unknown species 'DRAGON'; choose one of: 'BIRD, CAT, DOG, FISH, MOUSE, RABBIT, SNAKE'.",
			rejected("{species:'DRAGON',low:1,high:2}"));
	}

	@Test void b03_lowMustBeBelowHigh() {
		assertString("<@card id='gauge'> low '50' must be below high '50'.", rejected("{species:'DOG',low:50,high:50}"));
	}

	@Test void b04_negativeLow() {
		assertString("<@card id='gauge'> low '-1' must not be negative.", rejected("{species:'DOG',low:-1,high:5}"));
	}

	@Test void b05_markupBodyIsRejected() {
		assertContains("type='inventory-gauge' requires a JSON5 object body", rejected("<p>DOG</p>"));
	}

	@Test void b06_nonIntegerLow() {
		assertString("<@card id='gauge'> low 'abc' must be a whole number.", rejected("{species:'DOG',low:'abc',high:5}"));
	}

	@Test void b07_nonIntegerHigh() {
		assertString("<@card id='gauge'> high '1.5' must be a whole number.", rejected("{species:'DOG',low:1,high:1.5}"));
	}
}
