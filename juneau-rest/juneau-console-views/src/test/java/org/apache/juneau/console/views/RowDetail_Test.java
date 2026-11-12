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

import java.io.*;
import static java.nio.charset.StandardCharsets.*;
import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.marshaller.*;
import org.junit.jupiter.api.*;

/** {@link RowDetail}: per-row detail panel endpoint, title, icon and region. */
@SuppressWarnings("unchecked") // Casts parsed maps in assertions.
class RowDetail_Test extends TestBase {

	private static final String UNSAFE = " is not a safe detail endpoint (same-origin path with an {id} placeholder).";

	@SuppressWarnings({
		"unchecked" // Casts a parsed JSON value to its known Map/List shape.
	})
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

	@Test void a10_toMap_withoutRegion_accepted() {
		var m = RowDetail.create("/rest/work/data/{id}").title("Work {id}").toMap();
		assertEquals(Map.of("endpoint", "/rest/work/data/{id}", "title", "Work {id}"), m);
		assertFalse(m.containsKey("region"));
	}

	@Test void a11_toMap_withoutRegion_endpointOnly() {
		assertEquals(Map.of("endpoint", "/rest/work/data/{id}"), RowDetail.create("/rest/work/data/{id}").toMap());
	}

	@Test void a12_region_null_clearsRegion() {
		var m = RowDetail.create("/rest/work/data/{id}")
			.region(RegionDef.create("work-detail").populate("work-detail")).region(null).toMap();
		assertFalse(m.containsKey("region"));
	}

	private static String readGolden(String name) throws IOException {
		try (var in = RowDetail_Test.class.getResourceAsStream(name)) {
			assertNotNull(in, "missing golden " + name);
			return new String(in.readAllBytes(), UTF_8).strip();
		}
	}

	@Test void a13_v1_create_isByteIdentical() throws Exception {
		var json = Json5.of(RowDetail.create("/x/{id}").title("T #{id}").icon("link").toMap());
		assertEquals(readGolden("row-detail-v1.golden.json"), json);
		assertFalse(json.contains("contractVersion"));
		assertFalse(json.contains("tabs"));
	}

	@Test void b01_inline_shape() {
		var m = RowDetail.inline().title("Change {id}").section("Change", "id", "state").toMap();
		assertEquals("inline", m.get("mode"));
		assertNull(m.get("endpoint"));
		assertEquals("fields", ((List<Map<String,Object>>)m.get("tabs")).get(0).get("kind"));
	}

	@Test void b02_region_onTabModel_throws() {
		var sut1 = RowDetail.inline();
		assertThrows(IllegalStateException.class, () -> sut1.region(null));
	}

	@Test void b03_duplicateTabId_throws() {
		var d = RowDetail.inline().section("A", "x").section("A", "y");
		assertThrows(IllegalArgumentException.class, d::toMap);
	}

	@Test void b04_noTabsNoSections_generatesFlatDetails() {
		var tabs = (List<?>) RowDetail.inline().title("T").toMap().get("tabs");
		assertEquals(1, tabs.size());
		assertEquals("details", ((Map<?,?>)tabs.get(0)).get("id"));
	}

	@Test void b05_columnsDefault2_andTabOverrides() {
		var m = RowDetail.inline().columns(3).section("A", "x").toMap();
		assertEquals(3, m.get("columns"));
		var sut2 = RowDetail.inline();
		assertThrows(IllegalArgumentException.class, () -> sut2.columns(5));
		assertFalse(RowDetail.inline().section("A", "x").toMap().containsKey("columns"));
	}

	@Test void b06_labelPrecedence_explicitBeatsColumn() {
		assertEquals("E", RowDetail.resolveLabel("birthDate", "E", "C", "B"));
		assertEquals("C", RowDetail.resolveLabel("birthDate", null, "C", "B"));
		assertEquals("B", RowDetail.resolveLabel("birthDate", null, null, "B"));
		assertEquals("Birth date", RowDetail.resolveLabel("birthDate", null, null, null));
	}

	@Test void b07_fromBean_modeAndKey() {
		var d = RowDetail.fromBean(String.class, id -> id);
		assertEquals("bean", d.section("A", "length").toMap().get("mode"));
		assertEquals("string", d.key());
		assertEquals("pet", RowDetail.fromBean(String.class, id -> id).key("pet").key());
	}

	@Test void b07b_basePath_prefixesBeanEndpoint() {
		var d = RowDetail.fromBean(String.class, id -> id).key("pet").basePath("/console/pets").section("A", "length");
		assertEquals("/console/pets/row-detail/pet/{id}", d.toMap().get("endpoint"));
	}

	@Test void b07c_basePath_rejectsUnsafe() {
		var sut1 = RowDetail.fromBean(String.class, id -> id);
		var sut2 = RowDetail.fromBean(String.class, id -> id);
		assertThrows(IllegalArgumentException.class, () -> sut1.basePath("https://evil.example"));
		assertThrows(IllegalArgumentException.class, () -> sut2.basePath("/a/{id}"));
	}

	@Test void b08_cacheNone_emitted_defaultOmitted() {
		assertEquals("none", RowDetail.inline().cache(DetailTab.Cache.NONE).section("A", "x").toMap().get("cache"));
		assertFalse(RowDetail.inline().section("A", "x").toMap().containsKey("cache"));
	}

	@Test void b09_copy_isIndependent() {
		var a = RowDetail.inline().section("A", "x");
		var b = a.copy().section("B", "y");
		assertEquals(1, ((List<?>)a.toMap().get("tabs")).size());
		assertEquals(2, ((List<?>)b.toMap().get("tabs")).size());
	}

	@Test void b10_labelsAndFieldsNarrowing() {
		var m = RowDetail.inline().label("a", "Alpha").fields("a", "b").toMap();
		assertEquals("{a=Alpha}", m.get("labels").toString());
		assertEquals("[a, b]", ((Map<?,?>)((List<?>)m.get("tabs")).get(0)).get("fields").toString());
	}

	@Test void c01_actionTypes() {
		var d = RowDetail.inline()
			.action(RowAction.link("open","/p/{id}").label("Open"))
			.action(RowAction.create("del").label("Delete").endpoint("/p/{id}").method(RowAction.Method.DELETE).confirm("Sure?"))
			.toMap();
		var a = (List<Map<String,Object>>) d.get("actions");
		assertEquals("link", a.get(0).get("type"));
		assertEquals("row-action", a.get(1).get("type"));
		assertEquals("/p/{id}", a.get(0).get("href"));
	}

	@Test void c02_handlerAction_typeHandler() {
		var d = RowDetail.inline().action(RowAction.link("open","/p/{id}").label("Open"))
			.action("refresh", "refresh", "pets.refresh").toMap();
		var a = (List<Map<String,Object>>) d.get("actions");
		assertEquals("handler", a.get(1).get("type"));
		assertEquals("link", a.get(0).get("type"));
	}

	@SuppressWarnings("unchecked")
	private static List<String> fieldsOf(Map<String,Object> table) {
		var d = (Map<String,Object>) table.get("detail");
		var tabs = (List<Map<String,Object>>) d.get("tabs");
		return (List<String>) tabs.get(0).get("fields");
	}

	@Test void d01_sharedRowDetail_independentOutputPerTable() {
		var shared = RowDetail.inline().title("{id}");
		var a = TableSpec.create("a").dataUrl("/a").columns(Column.create("id"), Column.create("x")).detail(shared).toCardBody();
		var b = TableSpec.create("b").dataUrl("/b").columns(Column.create("id"), Column.create("y")).detail(shared).toCardBody();
		assertEquals(List.of("id", "x"), fieldsOf(a));
		assertEquals(List.of("id", "y"), fieldsOf(b));
	}

	@Test void d02_detailWithoutRowIdField_failsAtBuild() {
		var t = TableSpec.create("t").dataUrl("/t").columns(Column.create("label")).detail(RowDetail.inline());
		assertThrows(IllegalStateException.class, t::toCardBody);
	}

	@Test void d02b_explicitRowIdField_resolvesWithoutSelection() {
		var t = TableSpec.create("t").dataUrl("/t").columns(Column.create("label")).rowIdField("label").detail(RowDetail.inline()).toCardBody();
		assertEquals("label", t.get("rowIdField"));
		assertFalse(t.containsKey("selection"));
		assertEquals(List.of("label"), fieldsOf(t));
	}

	@Test void d02c_rowIdFieldMatchingSelection_ok() {
		var t = TableSpec.create("t").dataUrl("/t").columns(Column.create("label")).rowIdField("label")
			.selection(SelectionDef.create("label")).detail(RowDetail.inline()).toCardBody();
		assertEquals("label", t.get("rowIdField"));
	}

	@Test void d02d_rowIdFieldDiffersFromSelection_failsValidation() {
		var t = TableSpec.create("t").dataUrl("/t").columns(Column.create("label"), Column.create("x")).rowIdField("label")
			.selection(SelectionDef.create("x"));
		assertThrows(IllegalStateException.class, t::validate);
	}

	@Test void d02e_selectionRowIdField_usedBeforeFallbacks() {
		var t = TableSpec.create("t").dataUrl("/t").columns(Column.create("label")).selection(SelectionDef.create("label")).detail(RowDetail.inline()).toCardBody();
		assertFalse(t.containsKey("rowIdField"));
		assertEquals(List.of("label"), fieldsOf(t));
	}

	@Test void d03_defaultFieldSet_includesHiddenByDefaultColumns() {
		var t = TableSpec.create("t").dataUrl("/t")
			.columns(Column.create("id"), Column.create("secretish").defaultVisible(false)).detail(RowDetail.inline()).toCardBody();
		assertEquals(List.of("id", "secretish"), fieldsOf(t));
	}

	@Test void d04_columnLabelsFeedDetailLabels() {
		var t = TableSpec.create("t").dataUrl("/t")
			.columns(Column.create("id").label("Identifier")).detail(RowDetail.inline().label("id", "Explicit")).toCardBody();
		@SuppressWarnings("unchecked") var d = (Map<String,Object>) t.get("detail");
		@SuppressWarnings("unchecked") var labels = (Map<String,String>) d.get("labels");
		assertEquals("Explicit", labels.get("id"));
	}
}
