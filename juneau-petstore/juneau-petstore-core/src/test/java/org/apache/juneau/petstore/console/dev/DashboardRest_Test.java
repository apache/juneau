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
 * The petstore dashboard page, its CSV source variants and its request counter.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class DashboardRest_Test extends TestBase {

	private static final String BASE = "/console/dev/dashboard";

	private static String csv(String variant, String query) throws Exception {
		return client().get(BASE + "/series/" + variant + query).run().assertStatus(200)
			.assertHeader("Content-Type").isContains("text/csv").getContent().asString();
	}

	//-----------------------------------------------------------------------------------------------------------------
	// a - page
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_dashboardContract() throws Exception {
		var html = page(client(), BASE);
		var page = assertPage(html).isValid().hasActiveNav("dev").hasNoWiringErrors()
			.hasCard("daily", "chart").hasCard("by-reason", "chart").hasCard("by-weekday", "chart").hasCard("share", "chart")
			.publishes("daily", "zoom:daily");
		var d = page.contract().getMap("dashboard");
		assertBean(d, "id,title,grid,status{length,0}", "overview,Order issues,2,{1,data:orders}");
		assertBean(d, "panels{length,0{card,span},1{card,span},2{card,span}}", "{3,{daily,2},{by-reason,1},{by-weekday,1}}");
		assertBean(d, "rail{side,width,sections{length,0{kind,id,custom,defaultPreset},1{kind,id},2{kind,id,source,dim},3{kind,card},4{kind,id,name}}}",
			"{left,280,{5,{range,range,true,30d},{search,q},{choice,reasons,orders,reason},{card,share},{custom,late,petstore.late}}}");
		assertBean(d, "persist{ns,version,shareLink,topics{length,0,1,2,3}}",
			"{petstore.dashboard,1,true,{4,filter:range,filter:q,filter:reasons,zoom:daily}}");
		// The source entry; the page script and the dashboard runtime both load.
		assertBean(page.contract().getList("dataSources").getMap(0), "id,kind,url,subscribes{length}", "orders,source,/console/dev/dashboard/series/ok,{4}");
		assertContainsAll(html, "/console/dev/dashboard/dashboard.js", "juneau-dashboard.js", BASE + "/series/ok");
	}

	@Test void a02_builderOnly() throws Exception {
		var rsp = client().get(BASE).accept("text/html").run().assertStatus(200);
		assertEquals("builder", rsp.getHeader("X-Petstore-Page").orElse(null));
	}

	@Test void a03_variantAndVersionParams() throws Exception {
		assertContains(BASE + "/series/slow", page(client(), BASE + "?variant=slow"));
		assertContains(BASE + "/series/ok", page(client(), BASE + "?variant=bogus"));   // unknown is ok
		assertBean(assertPage(page(client(), BASE + "?variant=right")).contract().getMap("dashboard").getMap("rail"), "side", "right");
		assertBean(assertPage(page(client(), BASE + "?v=3")).contract().getMap("dashboard"), "persist{version}", "{3}");
		assertBean(assertPage(page(client(), BASE + "?v=0")).contract().getMap("dashboard"), "persist{version}", "{1}");
	}

	@Test void a04_scriptIsServed() throws Exception {
		client().get(BASE + "/dashboard.js").run()
			.assertStatus(200)
			.assertHeader("Content-Type").isContains("javascript")
			.assertContent().isContains("petstore.late", "JuneauConsoleDashboard");
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the source endpoints
	//-----------------------------------------------------------------------------------------------------------------

	@Test void b01_oneRowPerUtcDay() throws Exception {
		assertString("day,reason,orders\n"
			+ "2026-10-01T00:00:00Z,wrong-item,15\n"
			+ "2026-10-02T00:00:00Z,late,22\n"
			+ "2026-10-03T00:00:00Z,lost,29\n",
			csv("ok", "?start=2026-10-01T00:00:00Z&end=2026-10-04T00:00:00Z"));
	}

	@Test void b02_filters() throws Exception {
		var week = "?start=2026-10-01T00:00:00Z&end=2026-10-08T00:00:00Z";
		assertString("day,reason,orders\n"
			+ "2026-10-02T00:00:00Z,late,22\n2026-10-03T00:00:00Z,lost,29\n2026-10-06T00:00:00Z,late,27\n2026-10-07T00:00:00Z,lost,11\n",
			csv("ok", week + "&reasons=late&reasons=lost"));
		assertString("day,reason,orders\n2026-10-03T00:00:00Z,lost,29\n2026-10-07T00:00:00Z,lost,11\n", csv("ok", week + "&q=OS"));
		assertString("day,reason,orders\n2026-10-02T00:00:00Z,late,22\n2026-10-06T00:00:00Z,late,27\n", csv("ok", week + "&lateOnly=true"));
		// The search box sends an empty q, which means no filter (a typed String parameter would have answered 400).
		assertString(csv("ok", week), csv("ok", week + "&q=&lateOnly=false"));
		assertString("day,reason,orders\n", csv("ok", week + "&reasons=late&q=lost"));
	}

	@Test void b03_truncatedEndsWithTheSentinel() throws Exception {
		var lines = csv("truncated", "?start=2026-09-01T00:00:00Z&end=2026-10-11T00:00:00Z").split("\n");
		assertEquals(22, lines.length);   // the header, 20 rows, the sentinel
		assertString("2026-09-20T00:00:00Z,late,30", lines[20]);
		assertString("#truncated,20", lines[21]);
	}

	@Test void b04_slowStillReturnsEveryRow() throws Exception {
		assertEquals(4, csv("slow", "?start=2026-10-01T00:00:00Z&end=2026-10-04T00:00:00Z").split("\n").length);
	}

	@Test void b05_errorIs500() throws Exception {
		client().get(BASE + "/series/error").run().assertStatus(500);
	}

	@Test void b06_everySeriesRequestIsCounted() throws Exception {
		var n0 = DashboardRest.SERIES_REQUESTS.get();
		csv("ok", "");
		client().get(BASE + "/series/error").run();
		assertEquals(n0 + 2, DashboardRest.SERIES_REQUESTS.get());
		assertContains("\"series\":" + (n0 + 2), client().get(BASE + "/requests").accept("application/json").run()
			.assertStatus(200).getContent().asString());
	}

	@Test void b07_fixedWindowTotals() throws Exception {
		var reasons = client().get(BASE + "/reasons").accept("application/json").run().assertStatus(200).getContent().asString();
		assertContainsAll(reasons, "\"reason\":\"late\",\"orders\":129", "\"reason\":\"lost\",\"orders\":143",
			"\"reason\":\"damaged\",\"orders\":146", "\"reason\":\"wrong-item\",\"orders\":149");
		var days = client().get(BASE + "/weekdays").accept("application/json").run().assertStatus(200).getContent().asString();
		assertContainsAll(days, "\"weekday\":\"Mon\",\"orders\":73", "\"weekday\":\"Sun\",\"orders\":91");
	}
}
