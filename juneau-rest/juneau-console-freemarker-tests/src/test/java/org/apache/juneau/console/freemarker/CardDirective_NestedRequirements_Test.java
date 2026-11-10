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
package org.apache.juneau.console.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.junit.jupiter.api.*;

/**
 * The toolkit packs a container card records for the cards nested inside it.
 */
class CardDirective_NestedRequirements_Test extends TestBase {

	private static final CardDirectiveModel MODEL = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()), CardTypeRegistry.standard());

	private static List<String> packs(String id, String type, Map<String,Object> body) throws Exception {
		var cap = new PageCapture();
		MODEL.recordRequirements(cap, type, id, List.of(), body);
		return List.copyOf(cap.requiredPacks());
	}

	@Test void a01_switcherHoldingAChart_recordsTheChartPackBeforeTheSwitcherPack() throws Exception {
		var body = CardTypeRegistry.standard().toCard(CardSource.create("switcher", "sw").body(
			"{views:[{id:'a',label:'A',card:{id:'c1',type:'chart',src:'/x',kind:'line',x:{field:'d'},series:['n']}}]}").build());
		var p = packs("sw", "switcher", body);
		assertTrue(p.contains("charts"), p.toString());
		assertTrue(p.contains("switcher"), p.toString());
		assertTrue(p.indexOf("charts") < p.indexOf("switcher"), p.toString());
	}

	@Test void a02_rawUnliftedNestedMaps_stillRecordEveryNestedPack() throws Exception {
		// Maps as an app would hand them over: no lifting, a nested table still carrying its dataUrl at the top.
		var chart = map("id", "c1", "type", "chart", "src", "/x", "kind", "line");
		var table = map("id", "d1", "type", "datatables", "dataMode", "client", "dataUrl", "/x");
		var views = List.of(
			map("id", "a", "label", "A", "card", chart),
			map("id", "t", "label", "T", "sections", List.of(map("id", "s1", "label", "S1", "card", table))));
		var p = packs("sw", "switcher", map("views", views));
		assertTrue(p.contains("charts"), p.toString());
		assertTrue(p.contains("datatables-glue"), p.toString());
		assertTrue(p.indexOf("charts") < p.indexOf("switcher"), p.toString());
		assertTrue(p.indexOf("datatables-glue") < p.indexOf("switcher"), p.toString());
	}

	private static Map<String,Object> map(Object...kv) {
		var m = new LinkedHashMap<String,Object>();
		for (var i = 0; i < kv.length; i += 2)
			m.put((String)kv[i], kv[i + 1]);
		return m;
	}
}
