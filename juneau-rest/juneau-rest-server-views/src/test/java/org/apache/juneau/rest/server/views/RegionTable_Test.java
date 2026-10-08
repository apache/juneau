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
package org.apache.juneau.rest.server.views;

import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/**
 * {@link RegionTable#of(RegionDef)} attribute stamping: identity, and the declared-descriptor attribute that appears
 * only when the region carries {@code params}.
 */
class RegionTable_Test extends TestBase {

	@Test void a01_noParams_noDeclaredAttribute() {
		var html = RegionTable.of(RegionDef.create("side")).toString();
		assertContains(() -> html, "data-juneau-region='side'", html);
		assertFalse(html.contains(RegionTable.REGION_DECLARED_ATTR), html);
	}

	@Test void a02_emptyParams_noDeclaredAttribute() {
		var html = RegionTable.of(RegionDef.create("side").params(new LinkedHashMap<String,Object>())).toString();
		assertFalse(html.contains(RegionTable.REGION_DECLARED_ATTR), html);
	}

	@Test void a03_params_stampDeclaredDescriptor() {
		var r = RegionDef.create("side").params("run", "7");
		var html = RegionTable.of(r).toString();
		assertContains(() -> html, RegionTable.REGION_DECLARED_ATTR + "='", html);
		assertContains(() -> html, "contractVersion", html);
	}

	@Test void a04_declaredDescriptor_isAttributeEscaped() {
		var r = RegionDef.create("side").params("q", "it's <b>&\"x\"");
		var html = RegionTable.of(r).toString();
		var start = html.indexOf(RegionTable.REGION_DECLARED_ATTR);
		assertTrue(start >= 0, html);
		var attr = html.substring(start);
		assertFalse(attr.contains("<b>"), attr);
	}
}
