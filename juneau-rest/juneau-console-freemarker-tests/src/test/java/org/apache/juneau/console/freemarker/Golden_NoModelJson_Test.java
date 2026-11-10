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
package org.apache.juneau.console.freemarker;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.console.*;
import org.apache.juneau.console.views.*;
import org.junit.jupiter.api.*;

/**
 * Guards byte-identical contract output for builders that do not use the model API: no {@code model} key,
 * {@code modelVersion}, {@code scripts} or {@code allowInlineHandlers} may appear.
 */
class Golden_NoModelJson_Test extends TestBase {

	private static TableSpec table() {
		return TableSpec.create("t1").dataUrl("/api/t").columns(Column.create("id")).detail(RowDetail.create("/d/{id}").title("T {id}"));
	}

	@Test void a01_card() {
		var m = CardSpec.html("c1").title("X").src("/x").publishes().subscribes(Subscription.to("selection:t1").as("params")).toMap();
		assertEquals("{id=c1, type=html, title=X, src=/x, subscribes=[{\"topic\":\"selection:t1\",\"as\":\"params\"}]}", m.toString());
	}

	@Test void a02_table() {
		assertEquals("{\"dataUrl\":\"/api/t\",\"columns\":[{\"key\":\"id\",\"label\":\"id\"}],\"detail\":{\"endpoint\":\"/d/{id}\",\"title\":\"T {id}\"}}", table().toCardBody().toString());
	}

	@Test void a03_detail() {
		assertEquals("{\"endpoint\":\"/d/{id}\",\"title\":\"T\",\"icon\":\"i\"}", RowDetail.create("/d/{id}").title("T").icon("i").toMap().toString());
	}

	@Test void a04_pageContract() throws Exception {
		var cap = new PageCapture();
		cap.title("T");
		cap.addCard(CardSpec.html("c1").title("X").src("/x"), null);
		assertEquals("{\"contractVersion\":\"1\",\"title\":\"T\",\"nav\":[],\"activeNav\":[],\"cards\":[{\"id\":\"c1\",\"type\":\"html\",\"title\":\"X\",\"src\":\"/x\"}]}", cap.toContractJson());
	}

	@Test void a05_modelKeyInsideAPayloadIsNotAModelBlock() throws Exception {
		var cap = new PageCapture();
		cap.title("T");
		var payload = Map.of("model", "legacy", "type", "handler", "handler", "h");
		cap.addCard(CardSpec.of("datatables", "t1").body("table", Map.of("dataUrl", "/api/t",
			"ribbon", List.of(Map.of("type", "publish", "id", "p", "topic", "x", "payload", payload)))), null);
		var json = cap.toContractJson();
		assertFalse(json.contains("modelVersion"), json);
	}
}
