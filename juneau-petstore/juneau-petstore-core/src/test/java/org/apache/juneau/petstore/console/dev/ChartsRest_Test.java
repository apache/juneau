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
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * The chart-core twin page: a line chart with markers, a threshold and a click topic, and a ribbon cmd to it.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class ChartsRest_Test extends TestBase {

	@Test void a01_chartPage() throws Exception {
		var html = page(client(), "/console/dev/charts");
		assertPage(html).isValid().hasActiveNav("dev").hasCardOrder("trend", "days").hasCard("trend", "chart")
			.publishes("trend", "app.day-picked").hasNoWiringErrors()
			.hasCardKey("trend", "/decorators/0/use", "markers")
			.hasCardKey("trend", "/decorators/1/use", "thresholds")
			.hasCardKey("trend", "/on/click", "app.day-picked")
			.hasCardKey("days", "/table/ribbon/0/topic", "cmd:trend");
		// The builder-twin render emits the seeded toolkit and card-required pack assets (charts pulls in echarts).
		assertContains("echarts/6.1.0/dist/echarts.min.js", html);
		assertContains("juneau-charts.js", html);
	}

	@Test void a02_builderOnly() throws Exception {
		// Builder-only: the page is not in PetstorePages_ContractTest.PAGES and has no template twin.
		var rsp = client().get("/console/dev/charts").accept("text/html").run().assertStatus(200);
		assertEquals("builder", rsp.getHeader("X-Petstore-Page").orElse(null));
	}

	@Test void a03_dailyRows() throws Exception {
		var json = client().get("/console/dev/charts/daily").accept("application/json").run().assertStatus(200).getContent().asString();
		assertContains("\"day\":\"Mon\"", json);
		assertContains("\"placed\":", json);
	}

	@Test void a04_echartsWebJarIsServed() throws Exception {
		// The page's mount serves the ECharts WebJar that the charts pack reaches (PetstoreConsolePage requires the pack at init, E-70).
		client().get("/console/dev/charts/webjars/echarts/6.1.0/dist/echarts.min.js").run().assertStatus(200)
			.assertHeader("Content-Type").asString().isContains("javascript");
	}
}
