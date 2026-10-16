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

import static org.junit.jupiter.api.Assertions.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.console.*;
import org.junit.jupiter.api.*;

class PageSpec_Data_Test extends TestBase {

	private static final CardDirectiveModel MODEL = new CardDirectiveModel(
		CardRequirements.create().build(new ToolkitPackRegistry()), CardTypeRegistry.standard());

	private static PageSpec page() {
		return PageSpec.create()
			.data(DataSourceDef.create("orders").url("/rest/orders"))
			.data(Derived.from("orders").transform("topN", JsonMap.of("by", "count", "n", 5)).as("top"));
	}

	@Test void a01_applyTo_emitsDataSourcesAndRequiresThePack() throws Exception {
		var cap = new PageCapture();
		page().applyTo(cap, MODEL);
		var c = JsonMap.ofString(cap.toContractJson());
		assertEquals("[{id:'orders',kind:'source',url:'/rest/orders',parser:'csv',"
			+ "builder:'columnar',subscribes:[],concurrency:1,"
			+ "debounceMs:250,cache:'memory',mode:'raw'},"
			+ "{id:'top',kind:'derived',from:'orders',transforms:[{use:'topN',args:{by:'count',n:5}}],subscribes:[]}]",
			Json5.DEFAULT.write(c.getList("dataSources")));
		assertNull(c.get("topics"), "page.data emits no topics entry; a source publishes its own data: topic");
		assertTrue(cap.requiredPacks().contains(ToolkitPackRegistry.PACK_DATA));
	}

	@Test void a02_noDataSources_noKey_noPack() throws Exception {
		var cap = new PageCapture();
		PageSpec.create().applyTo(cap, MODEL);
		assertNull(JsonMap.ofString(cap.toContractJson()).get("dataSources"));
		assertFalse(cap.requiredPacks().contains(ToolkitPackRegistry.PACK_DATA));
	}

	@Test void a03_duplicateId_isE76() {
		var spec = PageSpec.create().data(DataSourceDef.create("orders").url("/a"));
		var e = assertThrows(IllegalArgumentException.class, () -> spec.data(DataSourceDef.create("orders").url("/b")));
		assertEquals("E-76: data source 'orders': duplicate id", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> spec.data(Derived.from("x").as("orders")));
		assertEquals("E-76: data source 'orders': duplicate id", e.getMessage());
	}

	@Test void a04_badDeclaration_failsAtTheCallSite() {
		var e = assertThrows(IllegalArgumentException.class, () -> PageSpec.create()
			.data(DataSourceDef.create("orders")));
		assertEquals("E-76: data source 'orders': url is required", e.getMessage());
	}

	private static PageSpec withConsumer(boolean declared) {
		var p = PageSpec.create();
		if (declared)
			p.data(DataSourceDef.create("orders").url("/rest/orders"));
		return p.html("c", "<p>c</p>", c -> c.subscribes(Subscription.to(Topics.data("orders")).as("refresh")));
	}

	@Test void a05_earlyWiring_runsWithDataSources() {
		// A declared source is its data: topic's one publisher; without it the read is E-71, not E-41 (R-E1).
		var reg = CardTypeRegistry.standard();
		var e = assertThrows(IllegalArgumentException.class, () -> withConsumer(false).checkWiringEarly(reg, null));
		assertEquals("topic 'data:orders' needs exactly one source publisher; found 0", e.getMessage());
		assertDoesNotThrow(() -> withConsumer(true).checkWiringEarly(reg, null));
		assertDoesNotThrow(() -> page().checkWiringEarly(reg, null));
	}

	@Test void a06_nullDefinition_isRejected() {
		var spec = PageSpec.create();
		var e = assertThrows(IllegalArgumentException.class, () -> spec.data((DataSourceDef)null));
		assertEquals("PageSpec.data requires a non-null definition.", e.getMessage());
		e = assertThrows(IllegalArgumentException.class, () -> spec.data((Derived)null));
		assertEquals("PageSpec.data requires a non-null definition.", e.getMessage());
	}

	@Test void a07_captureRejectsDuplicateDataSourceId() {
		var cap = new PageCapture().addDataSource(DataSourceDef.create("orders").url("/a").toMap());
		var dup = DataSourceDef.create("orders").url("/b").toMap();
		var e = assertThrows(IllegalArgumentException.class, () -> cap.addDataSource(dup));
		assertEquals("E-76: data source 'orders': duplicate id", e.getMessage());
	}

	@Test void a08_declarationOrderIsKept() throws Exception {
		var spec = PageSpec.create()
			.data(DataSourceDef.create("zeta").url("/z"))
			.data(DataSourceDef.create("alpha").url("/a"));
		var cap = new PageCapture();
		spec.applyTo(cap, MODEL);
		var list = JsonMap.ofString(cap.toContractJson()).getList("dataSources");
		assertEquals("zeta", ((JsonMap)list.get(0)).getString("id"));
		assertEquals("alpha", ((JsonMap)list.get(1)).getString("id"));
	}

	@Test void a09_badId_failsAtConstruction() {
		var e = assertThrows(IllegalArgumentException.class, () -> DataSourceDef.create("1st"));
		assertEquals("E-76: data source '1st': id must match ^[A-Za-z][A-Za-z0-9_-]{0,63}$", e.getMessage());
	}

	@Test void a10_eachCaptureGetsItsOwnCopy() throws Exception {
		var spec = page();
		var c1 = new PageCapture();
		spec.applyTo(c1, MODEL);
		var first = JsonMap.ofString(c1.toContractJson());
		var c2 = new PageCapture();
		spec.applyTo(c2, MODEL);
		assertEquals(first.getList("dataSources").toString(), JsonMap.ofString(c2.toContractJson())
			.getList("dataSources").toString());
	}
}
