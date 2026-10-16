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
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The data-sources demo page: a day-chunked CSV source on a script-published range, drawn by a line chart.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class DataRest_Test extends TestBase {

	@Test void a01_dataPage() throws Exception {
		var html = page(client(), "/console/dev/data");
		assertPage(html).isValid().hasActiveNav("dev").hasCardOrder("ordersChart").hasCard("ordersChart", "chart")
			.hasNoWiringErrors();
		assertContains("\"dataSources\":[{\"id\":\"orders\",\"kind\":\"source\"", html);
		assertContains("\"chunk\":{\"unit\":\"day\",\"start\":\"start\",\"end\":\"end\"}", html);
		assertContains("\"cache\":\"indexedDb\"", html);
		assertContains("/console/dev/data/data.js", html);
	}

	@Test void a02_builderOnly() throws Exception {
		var rsp = client().get("/console/dev/data").accept("text/html").run().assertStatus(200);
		assertEquals("builder", rsp.getHeader("X-Petstore-Page").orElse(null));
	}

	@Test void a03_hourlyRowsInTheWindow() throws Exception {
		var rsp = client().get("/console/dev/data/orders?start=2026-10-01T00:00:00Z&end=2026-10-01T03:00:00Z")
			.run().assertStatus(200);
		rsp.assertHeader("Content-Type").asString().isContains("text/csv");
		var csv = rsp.getContent().asString();
		assertString("t,placed\n2026-10-01T00:00:00Z,7\n2026-10-01T01:00:00Z,4\n2026-10-01T02:00:00Z,1\n", csv);
	}

	@Test void a04_badWindowIs400() throws Exception {
		client().get("/console/dev/data/orders?start=yesterday&end=2026-10-01T03:00:00Z").run().assertStatus(400);
		client().get("/console/dev/data/orders?start=2026-10-01T00:00:00Z&end=2026-12-01T00:00:00Z").run()
			.assertStatus(400);
		client().get("/console/dev/data/orders?start=2026-10-01T00:00:00Z").run().assertStatus(400);
		client().get("/console/dev/data/orders?start=2026-10-01T03:00:00Z&end=2026-10-01T00:00:00Z").run()
			.assertStatus(400);
	}

	@Test void a06_unalignedWindowEmitsEveryHourStartingInIt() throws Exception {
		var csv = client().get("/console/dev/data/orders?start=2026-10-01T00:30:00Z&end=2026-10-01T02:30:00Z")
			.run().assertStatus(200).getContent().asString();
		assertString("t,placed\n2026-10-01T00:00:00Z,7\n2026-10-01T01:00:00Z,4\n2026-10-01T02:00:00Z,1\n", csv);
	}

	@Test void a05_pageScript() throws Exception {
		client().get("/console/dev/data/data.js").run().assertStatus(200).assertHeader("Content-Type").asString()
			.isContains("javascript");
	}
}
