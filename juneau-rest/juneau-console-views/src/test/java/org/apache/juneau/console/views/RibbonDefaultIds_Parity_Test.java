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
package org.apache.juneau.console.views;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** The stock ribbon default ids: {@link RibbonItem#defaultIds()} equals the map {@code juneau-ribbon.js} applies. */
class RibbonDefaultIds_Parity_Test extends TestBase {

	@Test void a01_javaAndJsDefaultIdMapsMatch() {
		var report = NodeHarness.report("ribbon-default-ids.cjs", ViewsMixin.RIBBON_JS_RESOURCE);
		assumeTrue(report != null, "node (or the harness) is not available");
		assertEquals(RibbonItem.defaultIds(), report.get("ids"));
	}

	@Test void a02_everyStockFactoryDefaultsToItsMapEntry() {
		assertEquals("refresh", RibbonItem.refresh().toMap().get("id"));
		assertEquals("collapseAll", RibbonItem.collapseAll().toMap().get("id"));
		assertEquals("pausePolling", RibbonItem.pausePolling().toMap().get("id"));
		assertEquals("resetDefaults", RibbonItem.resetDefaults().toMap().get("id"));
		assertEquals("export", RibbonItem.export("csv").toMap().get("id"));
		assertEquals("search", RibbonItem.search().toMap().get("id"));
		assertEquals("link", RibbonItem.linkButton().toMap().get("id"));
		assertEquals("columns", RibbonItem.columnSettings().toMap().get("id"));
	}
}
