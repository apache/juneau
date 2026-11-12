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

import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/** {@link RowAction#link(String,String)}: the header link action and its validation. */
class RowAction_Link_Test extends TestBase {

	@Test void a01_link_emitsHref_noEndpointMethodForm() {
		var a = RowAction.link("open", "/console/pets/{id}/summary").label("Open").icon("link");
		assertEquals("/console/pets/{id}/summary", a.href);
		assertNull(a.endpoint);
		assertNull(a.method);
		assertNull(a.form);
	}

	@Test void a02_unsafeHref_rejected() {
		for (var bad : List.of("https://evil.example/x", "//evil.example/x", "javascript:alert(1)", "x/y"))
			assertThrows(IllegalArgumentException.class, () -> RowAction.link("o", bad), bad);
	}

	@Test void a03_hrefWithEndpointFormConfirm_rejected() {
		var withEndpoint = RowDetail.inline().action(RowAction.link("o","/a").label("O").endpoint("/b"));
		var withConfirm = RowDetail.inline().action(RowAction.link("o","/a").label("O").confirm("sure"));
		assertThrows(IllegalArgumentException.class, withEndpoint::toMap);
		assertThrows(IllegalArgumentException.class, withConfirm::toMap);
	}

	@Test void a04_iconOnlyAction_needsLabel() {
		var iconOnly = RowDetail.inline().action(RowAction.link("o","/a").icon("link"));
		assertThrows(IllegalArgumentException.class, iconOnly::toMap);
	}

	@Test void a05_rowMenu_ignoresLinkActions() {
		var t = TableSpec.create("t").dataUrl("/rows").columns(Column.create("id"))
			.rowActions(RowAction.link("o", "/a/{id}").label("Open"));
		t.validate();
		assertFalse(t.toCardBody().containsKey("rowActions"));
	}

	@Test void a06_rowMenu_keepsSubmitActions() {
		var t = TableSpec.create("t").dataUrl("/rows").columns(Column.create("id"))
			.rowActions(RowAction.link("o", "/a/{id}").label("Open"),
				RowAction.create("d").label("Del").endpoint("/a/{id}").method(RowAction.Method.DELETE));
		var ra = (List<?>)t.toCardBody().get("rowActions");
		assertEquals(1, ra.size());
	}
}
