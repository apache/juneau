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

import static org.apache.juneau.rest.server.console.test.PageContractAssert.*;
import static org.apache.juneau.rest.server.view.freemarker.console.C1Fixtures.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * {@code <@card>} dispatches every non-built-in type through the mixin's
 * {@link org.apache.juneau.rest.server.console.CardTypeRegistry}: a type registered with
 * {@code Builder.cardType} runs its handler, and an unregistered well-formed type falls back to the generic
 * passthrough.
 */
class CardDirective_OpenTypes_Test extends TestBase {

	@Test void a01_registeredOpenType_runsItsHandler() {
		var body = renderKpi("card-open-type");
		assertPage(body).isValid();
		var card = CardDirective_Test.card(body, "count");
		assertEquals("kpi", card.getString("type"));
		assertEquals(42, card.get("value"));
		assertEquals("Open issues", card.getString("label"));
	}

	@Test void a02_unregisteredType_usesGenericPassthrough_markupBecomesTemplate() {
		var body = render("card-open-type-markup");
		var card = CardDirective_Test.card(body, "w");
		assertEquals("widget", card.getString("type"));
		assertEquals("w", card.getString("template"));
		assertPage(body).templateContains("w", "<p>unregistered type, generic passthrough</p>");
	}

	@Test void a03_requiresIsStillRecordedForRegistryTypes() {
		// recordRequirements runs after registry dispatch with the effective type.
		assertTrue(render("card-open-type-requires").contains("juneau-datatables.js"));
		assertFalse(render("card-open-type-markup").contains("juneau-datatables.js"));
	}

	@Test void a04_badTypeName_failsWithE21Wording() {
		assertError("card-open-type-badname", "<@card id='g'> type='Gauge_1' must match ^[a-z][a-z0-9-]{0,31}$.");
	}

	@Test void a05_openTypeWithoutId_fails() {
		assertError("card-open-type-noid", "<@card type=\"widget\"> requires id=.");
	}

	@Test void a06_kpiNotRegisteredOnDefaultHost() {
		// Same template, default host: kpi is unregistered there, so the generic passthrough handles it.
		var card = CardDirective_Test.card(render("card-open-type"), "count");
		assertEquals(42, card.get("value"));
	}
}
