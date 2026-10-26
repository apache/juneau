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
package org.apache.juneau.console;

import static java.nio.charset.StandardCharsets.*;
import static org.apache.juneau.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.*;
import java.util.*;
import java.util.function.*;

import org.apache.juneau.*;
import org.apache.juneau.marshall.collections.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

class PageContractSchema_Test extends TestBase {

	private static String fixture(String name) throws IOException {
		try (var in = PageContractSchema_Test.class.getResourceAsStream("/contracts/" + name)) {
			assertNotNull(in, () -> "missing fixture " + name);
			return new String(in.readAllBytes(), UTF_8);
		}
	}

	// These fixtures are schema-only: they are not wiring-checked (e.g. valid-terminal.json has input:true with no wiring).
	@ParameterizedTest
	@ValueSource(strings = {"valid-example.json", "valid-minimal.json", "valid-deep.json",
		"valid-terminal-view.json", "valid-terminal-view-nopoll.json", "valid-terminal.json",
		"valid-datatables-table.json", "valid-datatables-table-full.json", "valid-datatables-rows.json",
		"valid-datatables-src.json", "valid-badge.json", "valid-badge-refreshes-allow-missing.json",
		"valid-chart-src.json", "valid-chart-data.json", "valid-job-log.json",
		"valid-html-src-sanitize.json", "valid-chart-bare.json", "valid-chart-option.json", "valid-chart-helper.json", "valid-chart-action-visiblewhen.json",
		"valid-job-log-bare.json", "valid-rowaction-inline.json", "valid-data-sources.json",
		"valid-job-log-envelope-action.json", "valid-job-log-hidden.json", "valid-job-log-output.json", "valid-datatables-job-log-region.json",
		"valid-datatables-bare.json", "valid-dashboard.json", "valid-quickstats-kinds.json", "valid-urlstate-day-param.json", "valid-urlstate-config.json", "valid-urlstate-config-ids-max-one.json"})
	void a01_validCorpusPasses(String name) throws Exception {
		var errors = PageContractSchema.get().validate(fixture(name));
		assertTrue(errors.isEmpty(), () -> name + ": " + errors);
	}

	/** The card-level oneOf message cannot name the field, so prove the param is the cause: only its value differs from a valid page. */
	@ParameterizedTest
	@ValueSource(strings = {"blank", "badchars", "whitespace", "toolong"})
	void a01b_dayParamIsTheOnlyDifference(String kind) throws Exception {
		var bad = fixture("invalid-urlstate-day-param-" + kind + ".json");
		var fixed = bad.replaceAll("\"param\":\"[^\"]*\"", "\"param\":\"on\"");
		assertNotEquals(bad, fixed);
		assertFalse(PageContractSchema.get().validate(bad).isEmpty());
		assertEquals(List.of(), PageContractSchema.get().validate(fixed));
	}

	/** The builder refuses these options (TableSpec.urlState(String, Map)); the wire contract must refuse them too, and only the bad fragment differs from a valid page. */
	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"ids-max-zero|\"max\":0|\"max\":1",
		"ids-max-negative|\"max\":-1|\"max\":1",
		"ids-max-fraction|\"max\":1.5|\"max\":2",
		"ids-max-string|\"max\":\"50\"|\"max\":50",
		"ids-param-badchars|p&x=1|pid",
		"window-param-badchars|a b|ab",
		"window-key|\"middle\"|\"start\"",
		"custom|\"bogus\"|\"ids\""})
	void a01c_urlStateConfigOptionsAreRefused(String kind, String badFragment, String goodFragment) throws Exception {
		var bad = fixture("invalid-urlstate-config-" + kind + ".json");
		var fixed = bad.replace(badFragment, goodFragment);
		assertNotEquals(bad, fixed, kind);
		assertFalse(PageContractSchema.get().validate(bad).isEmpty(), kind);
		assertEquals(List.of(), PageContractSchema.get().validate(fixed), kind);
	}

	@ParameterizedTest
	@CsvSource(delimiter = '|', value = {
		"invalid-version.json|$.contractVersion: must equal 1",
		"invalid-urlstate-day-param-blank.json|matches 0 of oneOf",
		"invalid-urlstate-day-param-badchars.json|matches 0 of oneOf",
		"invalid-urlstate-day-param-whitespace.json|matches 0 of oneOf",
		"invalid-urlstate-day-param-toolong.json|matches 0 of oneOf",
		"invalid-dashboard-grid.json|$.dashboard.grid: must be one of",
		"invalid-dashboard-span.json|must be one of",
		"invalid-dashboard-section-kind.json|oneOf",
		"invalid-dashboard-persist-ns.json|does not match",
		"invalid-badge-unknown-key.json|bogus: not allowed",
		"invalid-badge-refreshms.json|refreshMs",
		"invalid-legacy-version.json|missing required 'contractVersion'",
		"invalid-legacy-version.json|$.version: not allowed",
		"invalid-both-versions.json|$.version: not allowed",
		"invalid-extra-top.json|$.x: not allowed",
		"invalid-missing-cards.json|missing required 'cards'",
		"invalid-navlayout.json|$.navLayout: must be one of",
		"invalid-id-pattern.json|does not match",
		"invalid-nav-leaf.json|matches none of anyOf",
		"invalid-nav-empty-children.json|matches none of anyOf",
		"invalid-slot-name.json|{Brand}",
		"invalid-header-slot-enum.json|must be one of",
		"invalid-footer-both.json|forbidden shape (not)",
		"invalid-initials.json|longer than 3",
		"invalid-html-neither.json|oneOf",
		"invalid-html-extra.json|oneOf",
		"invalid-html-sanitize-type.json|oneOf",
		"invalid-html-sanitize-template.json|oneOf",
		"invalid-datatables-sanitize.json|oneOf",
		"invalid-datatables-notable.json|oneOf",
		"invalid-terminal-view-src.json|oneOf",
		"invalid-terminal-view-extra.json|oneOf",
		"invalid-terminal-view-nourl.json|oneOf",
		"invalid-terminal-src.json|oneOf",
		"invalid-terminal-nourl.json|oneOf",
		"invalid-terminal-extra.json|oneOf",
		"invalid-terminal-options.json|oneOf",
		"invalid-chart-nokind.json|oneOf",
		"invalid-chart-both.json|oneOf",
		"invalid-chart-source.json|oneOf",
		"invalid-job-log-extra.json|oneOf",
		"invalid-job-log-noendpoint.json|oneOf",
		"invalid-job-log-envelope-noaction.json|oneOf",
		"invalid-job-log-source-on-page.json|oneOf",
		"invalid-job-log-output-both-targets.json|oneOf",
		"invalid-job-log-source-dotted.json|oneOf",
		"invalid-datatables-job-log-region-flat.json|oneOf",
		"invalid-datatables-job-log-region-nokeyurl.json|oneOf",
		"invalid-datatables-job-log-region-urlparam.json|oneOf",
		"invalid-chart-kind.json|oneOf",
		"invalid-job-log-bare.json|oneOf",
		"invalid-chart-bare.json|oneOf",
		"invalid-datatables-bare.json|oneOf",
		"invalid-card-type.json|does not match",
		"invalid-r1.json|R-1: duplicate nav id 'a'",
		"invalid-r2.json|R-2: duplicate card id 'c'",
		"invalid-r3.json|failed at 'b'",
		"invalid-datatables-both.json|oneOf",
		"invalid-datatables-rows-and-dataurl.json|oneOf",
		"invalid-datatables-slotmeta.json|oneOf",
		"invalid-tablecolumn-no-key.json|oneOf",
		"invalid-tablecolumn-extra.json|oneOf",
		"invalid-selection-norowid.json|oneOf",
		"invalid-bulk-noactions.json|oneOf",
		"invalid-rowaction-noendpoint.json|oneOf",
		"invalid-rowaction-badpresent.json|oneOf",
		"invalid-chart-action-visiblewhen-object.json|oneOf",
		"invalid-chart-action-enabledwhen-object.json|oneOf",
		"invalid-quickstats-unknown-item-key.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-tone.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-meter-no-max.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-tile-with-segments.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-shape-both.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-segment-no-label.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-segment-unknown-key.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-meter-string-value.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-empty-items.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-unknown-strip-key.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-meter-no-label.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-meter-blank-label.json|$.cards[0]: matches 0 of oneOf",
		"invalid-quickstats-segmented-no-label.json|$.cards[0]: matches 0 of oneOf",
	})
	void a02_invalidCorpusFails(String name, String expected) throws Exception {
		var errors = PageContractSchema.get().validate(fixture(name));
		assertTrue(errors.stream().anyMatch(e -> e.contains(expected)), () -> name + " expected '" + expected + "' in " + errors);
	}

	@Test void a03_r4_danglingTemplate() throws Exception {
		var c = fixture("valid-example.json");
		assertEquals(List.of(), PageContractSchema.get().validate(c, Set.of("header.banner", "jc-seg-1")));
		var errors = PageContractSchema.get().validate(c, Set.of("header.banner"));
		assertEquals(List.of("R-4: template 'jc-seg-1' is referenced but not present"), errors);
	}

	@Test void a04_unparseable() {
		var errors = PageContractSchema.get().validate("{nope");
		assertList(errors, (Predicate<String>) e -> e.startsWith("$: unparseable JSON"));
	}

	@Test void a05_c2_schemaIdBumpedTo1_1() throws Exception {
		var id = JsonMap.ofString(PageContractSchema.get().schemaJson()).getString("$id");
		assertTrue(id.endsWith("1.1/juneau-page.schema.json") || id.endsWith("1.1"), () -> "expected $id to carry '1.1'; got '" + id + "'");
	}
}
