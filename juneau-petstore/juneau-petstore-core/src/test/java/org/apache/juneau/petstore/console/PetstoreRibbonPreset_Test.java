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

import static org.apache.juneau.petstore.console.PetstoreConsoleFixture.*;
import static org.apache.juneau.bct.BctAssertions.*;

import org.apache.juneau.*;
import org.apache.juneau.console.test.*;
import org.junit.jupiter.api.*;

/**
 * The petstore pages on the standard ribbon preset, and the export libraries its Excel and PDF buttons need.
 */
@SuppressWarnings({
	"resource" // MockRestClient is a no-op close.
})
class PetstoreRibbonPreset_Test extends TestBase {

	//-----------------------------------------------------------------------------------------------------------------
	// a - the datatables-export toolkit pack
	//-----------------------------------------------------------------------------------------------------------------

	@Test void a01_datatablesExportPack_registered() throws Exception {
		var html = page(client(), "/console/pets?builder");
		var urls = AssetResolutionAssert.assetUrls(html);
		var jszip = indexOf(urls, "jszip.min.js");
		var pdfmake = indexOf(urls, "pdfmake.min.js");
		var fonts = indexOf(urls, "vfs_fonts.js");
		var buttons = indexOf(urls, "buttons.html5.min.js");
		org.junit.jupiter.api.Assertions.assertTrue(buttons >= 0 && buttons < jszip, "export libraries load after datatables-buttons: " + urls);
		org.junit.jupiter.api.Assertions.assertTrue(jszip < pdfmake && pdfmake < fonts, "order jszip, pdfmake, vfs_fonts: " + urls);
		for (var u : new String[] {urls.get(jszip), urls.get(pdfmake), urls.get(fonts)})
			client().get(u).run().assertStatus(200).assertHeader("Content-Type").asString().isContains("javascript");
	}

	@Test void a02_exportAssetOutsideTheWhitelist_404() throws Exception {
		client().get("/console/export/no-such.js").run().assertStatus(404);
		client().get("/console/export/..%2Fpetstore-ui.html").run().assertStatus(404);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// b - the pages build their ribbons on the standard preset
	//-----------------------------------------------------------------------------------------------------------------

	private static void assertRibbon(String path, String card, String...ids) throws Exception {
		var a = org.apache.juneau.console.test.PageContractAssert.assertPage(page(client(), path + "?builder"));
		for (var i = 0; i < ids.length; i++)
			a.hasCardKey(card, "/table/ribbon/" + i + "/id", ids[i]);
		a.hasCardKey(card, "/table/ribbon/" + ids.length, null);
		a.hasCardKey(card, "/table/columnConfig", true);
	}

	@Test void b01_pets() throws Exception {
		assertRibbon("/console/pets", "pets", "search", "add-pet", "status", "species", "export", "link", "collapse", "columns", "reset", "refresh");
	}

	@Test void b02_sold() throws Exception {
		assertRibbon("/console/pets/sold", "sold", "search", "species", "export", "link", "collapse", "columns", "reset", "refresh");
	}

	@Test void b03_orders() throws Exception {
		var a = org.apache.juneau.console.test.PageContractAssert.assertPage(page(client(), "/console/orders?builder"));
		a.hasCardKey("orders", "/table/ribbon/0/id", "search").hasCardKey("orders", "/table/ribbon/1/id", "new-order")
			.hasCardKey("orders", "/table/ribbon/2/id", "status").hasCardKey("orders", "/table/ribbon/3/id", "export")
			.hasCardKey("orders", "/table/ribbon/8/id", "refresh");
	}

	@Test void b04_users() throws Exception {
		assertRibbon("/console/users", "users", "search", "add-user", "userStatus", "export", "link", "collapse", "columns", "reset", "refresh");
	}

	@Test void b05_changes() throws Exception {
		assertRibbon("/console/pets/changes", "changes", "search", "pending-only", "export", "link", "collapse", "columns", "reset", "refresh");
	}

	@Test void b06_store() throws Exception {
		assertRibbon("/console/store", "stats", "search", "new-order", "export", "link", "collapse", "columns", "reset", "refresh");
	}

	@Test void b07_ops() throws Exception {
		assertRibbon("/console/ops/jobs", "jobs", "search", "restock", "export", "link", "collapse", "columns", "reset", "refresh");
		assertRibbon("/console/ops/jobs", "species", "search", "export", "link", "collapse", "columns", "reset", "refresh");
		assertRibbon("/console/ops/jobs", "groom-runs", "search", "groom", "export", "link", "collapse", "columns", "reset", "groom-refresh");
		assertRibbon("/console/ops/audit", "audit", "search", "export", "link", "collapse", "columns", "reset", "refresh");
	}

	@Test void b08_toolkits() throws Exception {
		for (var suffix : new String[] {"", "?builder"}) {
			for (var path : new String[] {"/console/pets", "/console/orders", "/console/users", "/console/store"})
				assertContains("jszip.min.js", page(client(), path + suffix));
			for (var path : new String[] {"/console/pets/sold", "/console/pets/changes", "/console/ops/jobs", "/console/ops/audit"}) {
				var html = page(client(), path + suffix);
				assertContains("buttons.html5.min.js", html);
				assertNotContains(() -> path + " must not load the export libraries", "jszip.min.js", html);
			}
		}
	}

	@Test void b09_filterSymbols() throws Exception {
		for (var path : new String[] {"/console/pets", "/console/pets/sold", "/console/orders", "/console/users"}) {
			var html = page(client(), path + "?builder");
			var syms = new java.util.ArrayList<String>();
			var m = java.util.regex.Pattern.compile("\\\"id\\\":\\\"(?:[a-z-]+)\\\",\\\"title\\\":\\\"[^\\\"]+\\\",\\\"symbol\\\":\\\"([a-z_]+)\\\"").matcher(html);
			while (m.find())
				syms.add(m.group(1));
			org.junit.jupiter.api.Assertions.assertFalse(syms.isEmpty(), path + " has filter symbols");
			org.junit.jupiter.api.Assertions.assertEquals(syms.size(), new java.util.HashSet<>(syms).size(), path + " symbols: " + syms);
		}
	}

	private static int indexOf(java.util.List<String> urls, String part) {
		for (var i = 0; i < urls.size(); i++)
			if (urls.get(i).contains(part))
				return i;
		return -1;
	}
}
