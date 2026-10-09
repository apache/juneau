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

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** {@link RowDetail}: per-row detail panel endpoint, title, icon and region. */
class RowDetail_Test extends TestBase {

	private static final String UNSAFE = " is not a safe detail endpoint (same-origin path with an {id} placeholder).";

	@SuppressWarnings("unchecked")
	private static Map<String,Object> region(Map<String,Object> m) {
		return (Map<String,Object>)m.get("region");
	}

	@Test void a01_create_rejectsUnsafeEndpoint() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RowDetail.create("https://evil.example/x/{id}"));
		assertEquals("RowDetail endpoint 'https://evil.example/x/{id}'" + UNSAFE, ex.getMessage());
	}

	@Test void a02_create_rejectsEndpointWithNoPlaceholder() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RowDetail.create("/rest/work/data"));
		assertEquals("RowDetail endpoint '/rest/work/data'" + UNSAFE, ex.getMessage());
	}

	@Test void a03_create_rejectsNullEndpoint() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RowDetail.create(null));
		assertEquals("RowDetail endpoint 'null'" + UNSAFE, ex.getMessage());
	}

	@Test void a03b_create_rejectsPlaceholderOtherThanId() {
		var ex = assertThrows(IllegalArgumentException.class, () -> RowDetail.create("/x/{foo}"));
		assertEquals("RowDetail endpoint '/x/{foo}'" + UNSAFE, ex.getMessage());
	}

	@Test void a04_toMap_defaultsRegionTypeAndDataUrl() {
		var m = RowDetail.create("/rest/work/data/{id}")
			.title("{workId}").icon("search")
			.region(RegionDef.create("work-detail").populate("work-detail").allowPopulators("work-detail"))
			.toMap();
		assertEquals("/rest/work/data/{id}", m.get("endpoint"));
		assertEquals("endpoint", m.keySet().iterator().next());
		assertEquals("{workId}", m.get("title"));
		assertEquals("search", m.get("icon"));
		var region = region(m);
		assertEquals(RegionDef.TYPE_ROW_DETAIL, region.get("type"));
		assertEquals("/rest/work/data/{id}", region.get("dataUrl"));
		assertFalse(region.containsKey("contractVersion")); // never nested inside a card fragment
	}

	@Test void a05_toMap_regionDataUrl_authorExplicitWins() {
		var m = RowDetail.create("/rest/work/data/{id}")
			.region(RegionDef.create("work-detail").dataUrl("/rest/work/detail-view/{id}").populate("work-detail"))
			.toMap();
		assertEquals("/rest/work/detail-view/{id}", region(m).get("dataUrl"));
	}

	@Test void a06_toMap_regionType_authorExplicitWins() {
		var m = RowDetail.create("/rest/work/data/{id}")
			.region(RegionDef.create("work-detail").type(RegionDef.TYPE_CARD_BODY).populate("work-detail"))
			.toMap();
		assertEquals(RegionDef.TYPE_CARD_BODY, region(m).get("type"));
	}

	@Test void a07_toMap_doesNotMutateCallersRegionDef() {
		var region = RegionDef.create("work-detail").populate("work-detail");
		RowDetail.create("/rest/work/data/{id}").region(region).toMap();
		assertNull(region.type); // toMap() never writes back onto the caller's instance
		assertNull(region.dataUrl);
	}

	@Test void a08_response_wrapsFieldsWithContractVersion() {
		var m = RowDetail.response(Map.of("workId", "W-1"));
		assertEquals(RowDetail.CONTRACT_VERSION, m.get("contractVersion"));
		assertEquals(Map.of("workId", "W-1"), m.get("fields"));
	}

	@Test void a09_toMap_omitsUnsetTitleAndIcon() {
		var m = RowDetail.create("/rest/work/data/{id}")
			.region(RegionDef.create("work-detail").populate("work-detail"))
			.toMap();
		assertFalse(m.containsKey("title"));
		assertFalse(m.containsKey("icon"));
	}

	@Test void a10_toMap_withoutRegion_rejected() {
		var d = RowDetail.create("/rest/work/data/{id}");
		var ex = assertThrows(IllegalArgumentException.class, d::toMap);
		assertEquals("RowDetail '/rest/work/data/{id}' requires a region.", ex.getMessage());
	}
}
