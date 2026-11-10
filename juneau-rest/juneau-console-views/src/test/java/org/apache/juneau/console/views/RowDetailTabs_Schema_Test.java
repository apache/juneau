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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/** Detail tabs of every kind, and a kind-less tab, validate against the page contract schema. */
class RowDetailTabs_Schema_Test extends TestBase {

	private static String page(RowDetail detail) {
		var card = TableSpec.create("t").dataUrl("/rest/t/data").columns(Column.create("id"), Column.create("name")).detail(detail).toCardBody();
		var json = Json.of(card);
		return "{\"contractVersion\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":[{\"id\":\"t\",\"type\":\"datatables\",\"table\":" + json + "}]}";
	}

	private static void assertValid(String json) {
		var errors = PageContractSchema.get().validate(json);
		assertTrue(errors.isEmpty(), () -> errors + " in " + json);
	}

	@Test void a01_javaBuiltTabs_emitKindAndValidate() {
		var nested = TableSpec.create("n").dataUrl("/rest/n/data").columns(Column.create("id"));
		var detail = RowDetail.inline()
			.tab(DetailTab.fields("Summary", "name"))
			.tab(DetailTab.table("Items", nested))
			.tab(DetailTab.chart("Trend", RegionDef.chart("trend", "/rest/trend/{id}").kind("line").x("name")))
			.tab(DetailTab.client("Custom", "custom-renderer"));
		var json = page(detail);
		for (var k : new String[]{"fields", "table", "chart", "client"})
			assertTrue(json.contains("\"kind\":\"" + k + "\""), () -> k + " missing in " + json);
		assertValid(json);
	}

	@Test void a02_kindlessTab_validates() {
		var json = "{\"contractVersion\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":[{\"id\":\"t\",\"type\":\"datatables\",\"table\":"
			+ "{\"dataUrl\":\"/rest/t/data\",\"columns\":[{\"key\":\"id\",\"label\":\"Id\"}],\"detail\":{\"mode\":\"inline\",\"tabs\":[{\"id\":\"x\",\"title\":\"X\"}]}}}]}";
		assertValid(json);
	}
}
