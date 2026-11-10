// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.console.views;

import static java.nio.charset.StandardCharsets.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * Java-to-JS parity for tabbed row details: the Java serialisation of a detail covering every tab kind is pinned to a
 * golden file, and the same file is loaded by {@code juneau-views.js} in a Node sandbox.
 */
class RowDetailTabs_Parity_Test extends TestBase {

	private static final String GOLDEN = "row-detail-all-kinds.golden.json";

	private static Map<String,Object> detail() {
		var nested = TableSpec.create("orders").dataUrl("/rest/orders/rows").columns(Column.create("id"), Column.create("status"));
		var d = RowDetail.inline()
			.title("Pet {id}")
			.icon("paw")
			.action(RowAction.link("open", "/pets/{id}").label("Open"))
			.tab(DetailTab.fields("Summary", "name", "status").id("summary"))
			.tab(DetailTab.table("Orders", nested).id("orders").countFromRows())
			.tab(DetailTab.chart("Trend", RegionDef.chart("trend", "/rest/trend/{id}").kind("line").x("name")).id("trend"))
			.tab(DetailTab.client("Custom", "pet-labels").id("custom"));
		return d.toMap("pets", List.of(Column.create("id"), Column.create("name")), "id");
	}

	private static String golden() throws IOException {
		try (var in = RowDetailTabs_Parity_Test.class.getResourceAsStream(GOLDEN)) {
			assertNotNull(in, "missing golden " + GOLDEN);
			return new String(in.readAllBytes(), UTF_8).strip();
		}
	}

	@Test void a01_javaSerialisation_matchesGolden() throws Exception {
		var json = Json.of(detail());
		Files.createDirectories(Path.of("target"));
		Files.writeString(Path.of("target", "row-detail-all-kinds.actual.json"), json + "\n", UTF_8);
		assertEquals(golden(), json);
	}

	@Test void a02_jsConsumesTheGolden() {
		var r = RegionsHarness.reportWith("row-detail-parity.cjs", ViewsMixin.HELPERS_JS_RESOURCE,
			"/org/apache/juneau/console/views/" + GOLDEN);
		assumeTrue(r != null, "node not available or row-detail-parity.cjs not found - behavioral layer skipped");
		assertEquals("summary|orders|trend|custom", r.get("tabIds"));
		assertEquals("Summary|Orders|Trend|Custom", r.get("tabLabels"));
		assertEquals("open", r.get("headerLinkId"));
		assertEquals("/pets/s-7", r.get("headerLinkHref"));
		assertEquals(true, r.get("noFetch"));
	}

	@Test void a03_tabKinds_matchJavaEnum() {
		var r = RegionsHarness.reportWith("row-detail-parity.cjs", ViewsMixin.HELPERS_JS_RESOURCE,
			"/org/apache/juneau/console/views/" + GOLDEN);
		assumeTrue(r != null, "node not available or row-detail-parity.cjs not found - behavioral layer skipped");
		var java = new ArrayList<String>();
		for (var k : DetailTab.Kind.values())
			java.add(k.wire());
		assertEquals(java, r.get("kinds"));
	}
}
