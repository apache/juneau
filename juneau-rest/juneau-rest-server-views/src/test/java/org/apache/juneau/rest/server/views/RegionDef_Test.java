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
 * {@link RegionDef#validate()} matrix: id presence, populator-name allowlisting, and blank
 * {@link RegionDef#allowedPopulators} entries.
 */
@SuppressWarnings({
	"java:S5778" // assertThrows lambda may invoke helpers that also throw; splitting would obscure the LNN case.
})
class RegionDef_Test extends TestBase {

	@Test void a01_create_andFluentChain() {
		var r = RegionDef.create("sidebar").populate("myWidget").allowPopulators("myWidget");
		assertBean(r, "id,populate,allowedPopulators", "sidebar,myWidget,[myWidget]");
	}

	@Test void a02_contractVersion_isOne() {
		assertEquals("1", RegionDef.CONTRACT_VERSION);
	}

	@Test void b01_nullPopulate_accepted() {
		RegionDef.create("sidebar").validate();
	}

	@Test void b02_defaultPopulate_accepted() {
		RegionDef.create("sidebar").populate("default").validate();
	}

	@Test void b03_unallowlistedPopulate_rejected() {
		var r = RegionDef.create("sidebar").populate("myWidget");
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("myWidget"), e::getMessage);
	}

	@Test void b04_optedInPopulate_accepted() {
		RegionDef.create("sidebar").populate("myWidget").allowPopulators("myWidget").validate();
	}

	@Test void c01_blankAllowedPopulatorsEntry_rejected() {
		var r = RegionDef.create("sidebar").allowPopulators("myWidget", "  ");
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("allowPopulators"), e::getMessage);
	}

	@Test void d01_nullId_rejected() {
		var r = RegionDef.create(null);
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("id"), e::getMessage);
	}

	@Test void d02_blankId_rejected() {
		var r = RegionDef.create("  ");
		assertThrows(IllegalArgumentException.class, r::validate);
	}

	// -----------------------------------------------------------------------------------------------------------
	// dataUrl: same-origin validation (RowDetailDef.isSafeDetailEndpoint reuse).
	// -----------------------------------------------------------------------------------------------------------

	@Test void e01_dataUrl_sameOriginPath_accepted() {
		RegionDef.create("r").dataUrl("/rest/gacks/42/diagnose").validate();
	}

	@Test void e02_dataUrl_null_accepted() {
		RegionDef.create("r").validate();
	}

	@Test void e03_dataUrl_absoluteUrl_rejected() {
		var r = RegionDef.create("r").dataUrl("https://evil.example/x");
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("dataUrl"), e::getMessage);
	}

	@Test void e04_dataUrl_protocolRelative_rejected() {
		var r = RegionDef.create("r").dataUrl("//evil.example/x");
		assertThrows(IllegalArgumentException.class, r::validate);
	}

	@Test void e05_dataUrl_dotDotSegment_rejected() {
		var r = RegionDef.create("r").dataUrl("/rest/../secret");
		assertThrows(IllegalArgumentException.class, r::validate);
	}

	// -----------------------------------------------------------------------------------------------------------
	// refreshMs: clamped, never honored below the floor.
	// -----------------------------------------------------------------------------------------------------------

	@Test void f01_refreshMs_aboveFloor_honoredExactly() {
		var r = RegionDef.create("r").refreshMs(60_000);
		assertEquals(60_000L, r.refreshMs);
	}

	@Test void f02_refreshMs_belowFloor_clampedUp() {
		var r = RegionDef.create("r").refreshMs(1);
		assertEquals(RegionDef.MIN_REFRESH_MS, r.refreshMs);
	}

	@Test void f03_refreshMs_nonPositive_rejected() {
		assertThrows(IllegalArgumentException.class, () -> RegionDef.create("r").refreshMs(0));
	}

	// -----------------------------------------------------------------------------------------------------------
	// params: nested-map rejection and dataUrl-collision rejection.
	// -----------------------------------------------------------------------------------------------------------

	@Test void g01_params_nestedMap_rejected() {
		var r = RegionDef.create("r").params(Map.of("scope", Map.of("nested", 1)));
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("scope"), e::getMessage);
	}

	@Test void g02_params_collidesWithDataUrlQuery_rejected() {
		var r = RegionDef.create("r").dataUrl("/rest/x?scope=all").params(Map.of("scope", "recent"));
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("scope"), e::getMessage);
	}

	@Test void g03_params_noCollision_accepted() {
		RegionDef.create("r").dataUrl("/rest/x?scope=all").params(Map.of("tag", "a")).validate();
	}

	// -----------------------------------------------------------------------------------------------------------
	// The field catalog: card/tab only (§6.2.2 property 4 / test 16d, both directions).
	// -----------------------------------------------------------------------------------------------------------

	@Test void h01_fields_onRowDetail_rejected() {
		var r = RegionDef.create("r").type(RegionDef.TYPE_ROW_DETAIL).fields(RegionDef.Field.of("severity"));
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("row-detail"), e::getMessage);
	}

	@Test void h02_fields_onCardBody_accepted() {
		RegionDef.create("r").type(RegionDef.TYPE_CARD_BODY).renderer("field-grid")
			.fields(RegionDef.Field.of("severity").label("Severity")).validate();
	}

	@Test void h03_fields_onTabBody_accepted() {
		RegionDef.create("r").type(RegionDef.TYPE_TAB_BODY).renderer("field-grid")
			.fields(RegionDef.Field.of("severity")).validate();
	}

	@Test void h04_fieldGrid_noCatalog_onCardBody_rejected() {
		var r = RegionDef.create("r").type(RegionDef.TYPE_CARD_BODY).renderer("field-grid");
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("field-grid"), e::getMessage);
	}

	@Test void h05_fieldGrid_noCatalog_onTabBody_rejected() {
		var r = RegionDef.create("r").type(RegionDef.TYPE_TAB_BODY).renderer("field-grid");
		assertThrows(IllegalArgumentException.class, r::validate);
	}

	@Test void h06_fieldGrid_underCustomPopulator_noCatalogAllowed() {
		// A custom populator owns its own paint; the field-grid-requires-a-catalog rule only fires under the
		// reserved default, which is the only populator that actually reads ctx.declared.fields to paint one.
		RegionDef.create("r").type(RegionDef.TYPE_CARD_BODY).populate("myWidget").allowPopulators("myWidget")
			.renderer("field-grid").validate();
	}

	@Test void h07_duplicateFieldDataKey_rejected() {
		var r = RegionDef.create("r").type(RegionDef.TYPE_CARD_BODY)
			.fields(RegionDef.Field.of("severity"), RegionDef.Field.of("severity"));
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("severity"), e::getMessage);
	}

	@Test void h08_blankTitleFieldsEntry_rejected() {
		var r = RegionDef.create("r").titleFields("severity", "  ");
		assertThrows(IllegalArgumentException.class, r::validate);
	}

	@Test void h09_unrecognizedType_rejected() {
		var r = RegionDef.create("r").type("bogus");
		var e = assertThrows(IllegalArgumentException.class, r::validate);
		assertTrue(e.getMessage().contains("bogus"), e::getMessage);
	}

	// -----------------------------------------------------------------------------------------------------------
	// lazy: per-region-type default (fork F9), explicit value always wins.
	// -----------------------------------------------------------------------------------------------------------

	@Test void i01_lazy_default_rowDetail_isFalse() {
		assertFalse(RegionDef.create("r").type(RegionDef.TYPE_ROW_DETAIL).effectiveLazy());
	}

	@Test void i02_lazy_default_cardBody_isFalse() {
		assertFalse(RegionDef.create("r").type(RegionDef.TYPE_CARD_BODY).effectiveLazy());
	}

	@Test void i03_lazy_default_tabBody_isTrue() {
		assertTrue(RegionDef.create("r").type(RegionDef.TYPE_TAB_BODY).effectiveLazy());
	}

	@Test void i04_lazy_default_nullType_isFalse() {
		assertFalse(RegionDef.create("r").effectiveLazy());
	}

	@Test void i05_lazy_explicitTrue_winsOverCardDefault() {
		assertTrue(RegionDef.create("r").type(RegionDef.TYPE_CARD_BODY).lazy(true).effectiveLazy());
	}

	@Test void i06_lazy_explicitFalse_winsOverTabDefault() {
		assertFalse(RegionDef.create("r").type(RegionDef.TYPE_TAB_BODY).lazy(false).effectiveLazy());
	}

	// -----------------------------------------------------------------------------------------------------------
	// toContractMap(): the sidecar shape design §8.2/§6.2.2 specify.
	// -----------------------------------------------------------------------------------------------------------

	@Test void j01_toContractMap_minimal_shape() {
		var m = RegionDef.create("sidebar").toContractMap();
		// Minimal descriptor: toContractMap() only ever adds contractVersion/id/lazy unconditionally, so this
		// exact-contents assertMap also subsumes the four containsKey absence checks it replaces.
		assertMap(m, "contractVersion=1", "id=sidebar", "lazy=false");
	}

	@Test void j02_toContractMap_fullDescriptor_roundTripsThroughJson() throws Exception {
		var r = RegionDef.create("posture").type(RegionDef.TYPE_CARD_BODY).populate("default")
			.dataUrl("/rest/gacks/42/diagnose").params(Map.of("scope", "recent")).renderer("field-grid")
			.titleFields("severity").refreshMs(15_000)
			.fields(
				RegionDef.Field.of("severity").label("Severity").render("pill")
					.renderMeta(Map.of("tone", "warning")),
				RegionDef.Field.of("host").label("Host").href("servlet:/hosts/{host}"),
				RegionDef.Field.of("summary").label("Summary").format(FieldFormat.MARKDOWN)
					.span(FieldSpan.FULL),
				RegionDef.Field.of("actions").label("").actions("ack", "resolve"));
		r.validate();

		@SuppressWarnings({
			"unchecked" // The (Map<String,Object>) cast of the JSON round-trip of the contract map is safe because toContractMap() is a string-keyed map.
		})
		var parsed = (Map<String,Object>) Json.to(Json.of(r.toContractMap()), Map.class);
		// fields[]: 0=severity (pill render + renderMeta), 1=host (href), 2=summary (markdown format, full span),
		// 3=actions (ack/resolve).
		assertBean(parsed,
			"contractVersion,id,type,populate,dataUrl,params,renderer,lazy,refreshMs,titleFields,"
				+ "fields{length,0{data,render,renderMeta},1{href},2{format,span},3{actions}}",
			"1,posture,card-body,default,/rest/gacks/42/diagnose,{scope=recent},field-grid,false,15000,[severity],"
				+ "{4,{severity,pill,{tone=warning}},{servlet:/hosts/{host}},{markdown,full},{[ack,resolve]}}");
	}

	@Test void j03_toContractMap_rowDetail_noFieldsMember() {
		var r = RegionDef.create("diag").type(RegionDef.TYPE_ROW_DETAIL).dataUrl("/rest/gacks/42/diagnose");
		r.validate();
		assertFalse(r.toContractMap().containsKey("fields"), "row-detail must carry no fields member at all");
	}
}
