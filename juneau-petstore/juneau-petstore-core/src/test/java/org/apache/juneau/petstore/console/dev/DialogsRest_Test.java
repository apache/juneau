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
import static org.apache.juneau.console.test.PageContractAssert.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The dialogs demo page: a ribbon that opens a wizard and a folder-tab dialog, and the two dialog payloads.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class DialogsRest_Test extends TestBase {

	@Test void a01_page() throws Exception {
		var html = page(client(), "/console/dev/dialogs");
		assertPage(html).isValid().hasActiveNav("dev").hasCardOrder("dialogs").hasCard("dialogs", "datatables").hasNoWiringErrors()
			.hasCardKey("dialogs", "/table/ribbon/0/form", "/console/dev/dialogs/wizard-form")
			.hasCardKey("dialogs", "/table/ribbon/1/form", "/console/dev/dialogs/folder-form");
	}

	@Test void a02_wizardIsThreeSteps() throws Exception {
		var json = client().get("/console/dev/dialogs/wizard-form").accept("application/json").run().assertStatus(200).getContent().asString();
		assertTrue(json.contains("\"steps\"") && json.contains("\"species\"") && json.contains("\"review\""), json);
		assertFalse(json.contains("\"nav\""), json);
	}

	@Test void a03_folderIsNavFolderWithThreeSections() throws Exception {
		var json = client().get("/console/dev/dialogs/folder-form").accept("application/json").run().assertStatus(200).getContent().asString();
		assertTrue(json.contains("\"nav\":\"folder\""), json);
		assertTrue(json.contains("\"basics\"") && json.contains("\"options\"") && json.contains("\"danger\""), json);
	}

	@Test void a04_submitStoresNothingAndSucceeds() throws Exception {
		client().post("/console/dev/dialogs/submit", "{\"fields\":{}}").contentType("application/json").run().assertStatus(200);
	}
}
