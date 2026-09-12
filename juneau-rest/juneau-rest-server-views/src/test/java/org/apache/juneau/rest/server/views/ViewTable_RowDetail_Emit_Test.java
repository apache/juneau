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
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.rest.server.widgets.*;
import org.apache.juneau.rest.server.views.ViewDef.DataMode;
import org.junit.jupiter.api.*;

/**
 * {@link ViewTable} emits one {@code <template data-juneau-row-detail>} with chrome plus exactly one region
 * container — no section frames, no field slots.
 */
class ViewTable_RowDetail_Emit_Test extends TestBase {

	private static ViewDef view() {
		return ViewDef.create("alerts")
			.dataMode(DataMode.CLIENT)
			.dataUrl("/data/alerts")
			.columns(Column.of("id").title("Id"))
			.rowActions(
				RowAction.create("ack").label("Acknowledge").endpoint("/data/alerts/{id}/ack").method(RowAction.Method.POST),
				RowAction.create("esc").label("Escalate").endpoint("/data/alerts/{id}/esc").method(RowAction.Method.POST))
			.details(RowDetailDef.create()
				.endpoint("/data/alerts/{id}")
				.title("Alert {severity}")
				.region(RegionDef.create("detail").allowPopulators("alerts-detail").populate("alerts-detail")
					.titleFields("severity")))
			.build();
	}

	@Test void a01_emitsOneTemplate_withStampedUrlAndContract() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-row-detail"), html);
		assertTrue(html.contains("data-juneau-detail-contract=\"1\""), html);
		assertTrue(html.contains("data-juneau-detail-url=\"/data/alerts/{id}\""), html);
		assertEquals(html.indexOf("data-juneau-row-detail"), html.lastIndexOf("data-juneau-row-detail"));
	}

	@Test void a02_oneRegionContainer_namedPopulator_noSectionFrames() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-region=\"detail\""), html);
		assertTrue(html.contains("data-juneau-region-populate=\"alerts-detail\""), html);
		assertFalse(html.contains("data-juneau-detail-section"), html);
		assertFalse(html.contains("data-juneau-field=\""), html);
	}

	@Test void a03_headerChrome_andTitleFieldsAllowlist() {
		var html = Html.of(ViewTable.of(view()));
		assertTrue(html.contains("data-juneau-detail-header"), html);
		assertTrue(html.contains("data-juneau-detail-title"), html);
		assertTrue(html.contains("data-juneau-title-fields=\"severity\""), html);
		assertFalse(html.contains("data-juneau-action="), html);
		assertFalse(html.contains("data-juneau-safe="), html);
	}

	@Test void a04_noHeaderActionBar() {
		var html = Html.of(ViewTable.of(view()));
		assertFalse(html.contains("juneau-view-detail-actions"), html);
		assertFalse(html.contains("data-juneau-action="), html);
	}

	@Test void a05_templateDoesNotPourFieldValues() {
		var html = Html.of(ViewTable.of(view()));
		assertFalse(html.contains("CRITICAL"), html);
		assertFalse(html.contains("sev-value"), html);
	}
}
