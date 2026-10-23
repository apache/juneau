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
import org.apache.juneau.commons.beanquery.*;
import org.junit.jupiter.api.*;

/** {@link Column}'s display properties: render, href, className, defaultVisible, toCatalogMap. */
class Column_DisplayProperties_Test extends TestBase {

	@Test void a01_toCatalogMap_keyAndLabelFallback() {
		var m = Column.create("status").toCatalogMap();
		assertEquals("status", m.get("key"));
		assertEquals("status", m.get("label")); // label falls back to name when unset
		assertFalse(m.containsKey("render"));
		assertFalse(m.containsKey("href"));
		assertFalse(m.containsKey("className"));
		assertFalse(m.containsKey("defaultVisible"));
	}

	@Test void a02_toCatalogMap_explicitLabelWins() {
		var m = Column.create("status").label("Status").toCatalogMap();
		assertEquals("Status", m.get("label"));
	}

	@Test void a03_render_stringSugar() {
		var m = Column.create("status").render("tag:status").toCatalogMap();
		assertEquals("tag", ((Render)m.get("render")).id);
		assertEquals("status", ((Render)m.get("render")).meta.get("field"));
	}

	@Test void a04_render_object() {
		var m = Column.create("age").render(Render.of("age-duration")).toCatalogMap();
		assertEquals("age-duration", ((Render)m.get("render")).id);
	}

	@Test void a05_href_withoutRender_impliesLinked() {
		var m = Column.create("incidentKey").href("{incidentUrl}").toCatalogMap();
		assertEquals("linked", ((Render)m.get("render")).id);
		assertEquals("{incidentUrl}", m.get("href"));
	}

	@Test void a05b_href_withoutRender_doesNotMutateColumn() {
		var c = Column.create("k").href("{u}");
		c.toCatalogMap();
		c.render("tag:status");
		assertEquals("tag", ((Render)c.toCatalogMap().get("render")).id);
	}

	@Test void a06_href_withRender_emitted() {
		var m = Column.create("incidentKey").render("linked").href("{incidentUrl}").toCatalogMap();
		assertEquals("{incidentUrl}", m.get("href"));
	}

	@Test void a07_className_emitted() {
		var m = Column.create("pod").className("ssc-id-cell").toCatalogMap();
		assertEquals("ssc-id-cell", m.get("className"));
	}

	@Test void a08_defaultVisible_falseEmitted() {
		var m = Column.create("rootCause").defaultVisible(false).toCatalogMap();
		assertEquals(false, m.get("defaultVisible"));
	}

	@Test void a09_defaultVisible_trueOmitted() {
		var m = Column.create("rootCause").defaultVisible(true).toCatalogMap();
		assertFalse(m.containsKey("defaultVisible")); // true is the implicit default; only false is worth a byte on the wire
	}

	@Test void a10_toCatalogMap_search_inheritedOperators_emitsTypeOnly() {
		var m = Column.create("status").searchType(SearchType.ENUM).toCatalogMap();
		assertEquals("enum", m.get("searchType"));
		assertFalse(m.containsKey("searchOperators"));
		assertFalse(m.containsKey("customOperators"));
	}

	@Test void a10b_toCatalogMap_search_ownOperators_emitsNames() {
		var std = SearchOperatorSet.standard();
		var m = Column.create("status").searchType(SearchType.ID)
			.operators(SearchOperatorSet.of(std.get("$eq"), std.get("$in"))).toCatalogMap();
		assertEquals(List.of("$eq", "$in"), m.get("searchOperators"));
		assertFalse(m.containsKey("customOperators"));
	}

	@Test void a10c_toCatalogMap_search_customOperator_emitsNameAndHelp() {
		var mine = SearchOperator.create("$mine", "Assigned to me").types(SearchType.ID).build();
		var m = Column.create("owner").searchType(SearchType.ID)
			.operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"), mine)).toCatalogMap();
		assertEquals(List.of("$eq", "$mine"), m.get("searchOperators"));
		assertEquals(List.of(Map.of("name", "$mine", "help", "Assigned to me")), m.get("customOperators"));
	}

	@Test void a10m_toCatalogMapWithTableDefault_inheritedColumn_emitsTableSet() {
		var table = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"));
		var m = Column.create("owner").searchType(SearchType.ID).toCatalogMap(table);
		assertEquals(List.of("$eq"), m.get("searchOperators"));
		assertFalse(m.containsKey("customOperators"));
	}

	@Test void a10n_toCatalogMapWithTableDefault_ownSetWins() {
		var table = SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"));
		var own = SearchOperatorSet.of(SearchOperatorSet.standard().get("$ne"));
		var m = Column.create("owner").searchType(SearchType.ID).operators(own).toCatalogMap(table);
		assertEquals(List.of("$ne"), m.get("searchOperators"));
	}

	@Test void a10o_toCatalogMapWithTableDefault_noSearchType_emitsNothing() {
		var m = Column.create("note").toCatalogMap(SearchOperatorSet.standard());
		assertFalse(m.containsKey("searchType"));
		assertFalse(m.containsKey("searchOperators"));
	}

	@Test void a10d_toCatalogMap_noSearchType_noSearchKeys() {
		var m = Column.create("note").operators(SearchOperatorSet.standard()).toCatalogMap();
		assertFalse(m.containsKey("searchType"));
		assertFalse(m.containsKey("searchOperators"));
	}

	@Test void a10e_toCatalogMap_resolvesThroughSearchMeta() {
		// The names/custom shape is what Column.searchMeta(name, type, allowList, customs) takes back.
		var mine = SearchOperator.create("$mine", "Assigned to me").types(SearchType.ID).build();
		var c = Column.create("owner").searchType(SearchType.ID).operators(SearchOperatorSet.of(SearchOperatorSet.standard().get("$eq"), mine));
		var m = c.toCatalogMap();
		@SuppressWarnings({
			"unchecked" // Casts a parsed JSON value to its known Map/List shape.
		})
		var round = Column.searchMeta("owner", (String)m.get("searchType"), (List<String>)m.get("searchOperators"), (List<Map<String,String>>)m.get("customOperators"));
		assertEquals(c.searchMeta(), round);
	}

	@Test void a10f_render_null_clearsRender_hrefThenImpliesLinked() {
		var c = Column.create("k").render("tag:status").render((Render)null).href("{u}");
		assertEquals("linked", ((Render)c.toCatalogMap().get("render")).id);
	}

	@Test void a10g_render_blank_rejected() {
		var target = Column.create("k");
		assertThrows(IllegalArgumentException.class, () -> target.render(""));
	}

	@Test void a10h_href_null_clearsPreviousHref() {
		var m = Column.create("k").render("linked").href("{u}").href(null).toCatalogMap();
		assertFalse(m.containsKey("href"));
	}

	@Test void a11_viewDef_nullColumn_message() {
		var target2 = ViewDef.create("pets");
		var ex = assertThrows(IllegalArgumentException.class, () -> target2.column(null));
		assertEquals("ViewDef 'pets' column must not be null.", ex.getMessage());
	}
}
