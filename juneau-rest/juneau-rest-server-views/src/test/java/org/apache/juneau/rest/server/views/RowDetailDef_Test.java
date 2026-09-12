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

import org.apache.juneau.*;
import org.apache.juneau.rest.server.widgets.*;
import org.junit.jupiter.api.*;

/**
 * {@link RowDetailDef#validate(java.util.List)} after the sections path retired: region is required, endpoint
 * safety still holds, header {@code enabledWhen} is rejected, and there is no replacement catalog cross-check.
 */
class RowDetailDef_Test extends TestBase {

	private static RegionDef region() {
		return RegionDef.create("d").allowPopulators("p").populate("p");
	}

	private static RowAction ack() {
		return RowAction.create("ack").endpoint("/x").method(RowAction.Method.POST);
	}

	@Test void a01_valid_minimal() {
		RowDetailDef.create().endpoint("/data/{id}").region(region()).validate(null);
	}

	@Test void a02_missingRegion_rejected() {
		var d = RowDetailDef.create().endpoint("/data/{id}");
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("region"), e::getMessage);
	}

	@Test void a03_nullRegion_rejectedAtSetter() {
		assertThrows(IllegalArgumentException.class, () -> RowDetailDef.create().region(null));
	}

	@Test void a04_unknownHeaderActionRef_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.region(region())
			.headerActions(ActionBar.create().items(ActionRef.of("ack")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("ack"), e::getMessage);
	}

	@Test void a05_knownHeaderActionRef_accepted() {
		RowDetailDef.create()
			.endpoint("/data/{id}")
			.region(region())
			.headerActions(ActionBar.create().items(ActionRef.of("ack"), SafeAction.COLLAPSE))
			.validate(java.util.List.of(ack()));
	}

	@Test void a06_missingIdPlaceholder_rejected() {
		var d = RowDetailDef.create().endpoint("/data/x").region(region());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("{id}"), e::getMessage);
	}

	@Test void a07_blankEndpoint_rejected() {
		var d = RowDetailDef.create().endpoint("  ").region(region());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a08_absoluteUrl_rejected() {
		var d = RowDetailDef.create().endpoint("https://evil/{id}").region(region());
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(null));
		assertTrue(e.getMessage().contains("same-origin") || e.getMessage().contains("https"), e::getMessage);
	}

	@Test void a09_dotDotSegment_rejected() {
		var d = RowDetailDef.create().endpoint("/data/../x/{id}").region(region());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a10_servletScheme_rejected() {
		var d = RowDetailDef.create().endpoint("servlet:/data/{id}").region(region());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a11_protocolRelative_rejected() {
		var d = RowDetailDef.create().endpoint("//evil/{id}").region(region());
		assertThrows(IllegalArgumentException.class, () -> d.validate(null));
	}

	@Test void a12_headerEnabledWhen_rejected() {
		var d = RowDetailDef.create()
			.endpoint("/data/{id}")
			.region(region())
			.headerActions(ActionBar.create().items(
				ActionRef.of("ack").enabledWhen("status", Op.EQ, "open", "Not open.")));
		var e = assertThrows(IllegalArgumentException.class, () -> d.validate(java.util.List.of(ack())));
		assertTrue(e.getMessage().contains("enabledWhen"), e::getMessage);
		assertTrue(e.getMessage().contains("region"), e::getMessage);
	}

	@Test void a13_noAllowCustomRenderers() {
		for (var m : RowDetailDef.class.getMethods())
			assertNotEquals("allowCustomRenderers", m.getName(),
				"allowedCustomRenderers retired with the DetailField path");
	}

	@Test void a14_contractVersion_leftStanding() {
		assertEquals("1", RowDetailDef.CONTRACT_VERSION);
	}

	@Test void a15_isSafeDetailEndpoint() {
		assertTrue(RowDetailDef.isSafeDetailEndpoint("/data/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("https://x/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("//x/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("javascript:{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint("/a/../b/{id}"));
		assertFalse(RowDetailDef.isSafeDetailEndpoint(null));
	}

	@Test void a16_regionStampsType() {
		var r = RegionDef.create("d").allowPopulators("p").populate("p");
		RowDetailDef.create().endpoint("/data/{id}").region(r);
		assertEquals(RegionDef.TYPE_ROW_DETAIL, r.type);
	}

	@Test void a17_isRegionBody() {
		assertFalse(RowDetailDef.create().isRegionBody());
		assertTrue(RowDetailDef.create().region(region()).isRegionBody());
	}
}
