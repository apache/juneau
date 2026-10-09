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
import java.util.function.*;
import java.util.stream.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

/**
 * Pins {@link RibbonItem#toMap()} to the shared ribbon corpus ({@code ribbon-corpus.json}), the same contract
 * {@code juneau-ribbon.js} is tested against: for every corpus case, the builder-made list must equal the case's
 * {@code ribbon}.  A case with no builder here fails, so a new corpus case cannot go unchecked.
 */
class RibbonItem_Parity_Test extends TestBase {

	private static RibbonItem dropped() {
		return RibbonItem.option("dropped-only").column("status").value("$eq(DROPPED)");
	}

	private static RibbonItem pending() {
		return RibbonItem.option("pending").column("phase").value("$in(Waiting,\"Partially reviewed\")");
	}

	private static RibbonItem done() {
		return RibbonItem.option("done").column("phase").value("$in(\"Ready to push\",Completed)");
	}

	private static RibbonItem all() {
		return RibbonItem.option("all").title("All");
	}

	private static RibbonItem phaseGroup() {
		return RibbonItem.optionGroup("phase", pending(), done());
	}

	private static final Map<String,Supplier<List<RibbonItem>>> BUILDERS = new LinkedHashMap<>();

	static {
		BUILDERS.put("single-option-on", () -> List.of(dropped().persist(true)));
		BUILDERS.put("single-option-off", () -> List.of(dropped()));
		BUILDERS.put("option-group-member", () -> List.of(phaseGroup()));
		BUILDERS.put("option-group-none", () -> List.of(RibbonItem.optionGroup("phase", all(), pending())));
		BUILDERS.put("two-options-different-columns", () -> List.of(dropped(),
			RibbonItem.option("stdout").column("stream").value("$eq(out)")));
		BUILDERS.put("two-options-same-column", () -> List.of(
			RibbonItem.option("early").column("phase").value("$in(Waiting,\"Ready to push\",\"Committed, not pushed\")"),
			RibbonItem.option("not-waiting").column("phase").value("$ne(Waiting)")));
		BUILDERS.put("two-options-same-param", () -> List.of(
			RibbonItem.option("s-dropped").param("search").value("status=$eq(DROPPED)"),
			RibbonItem.option("s-out").param("search").value("stream=$eq(out)")));
		BUILDERS.put("combine-user-and-ribbon", () -> List.of(dropped()));
		BUILDERS.put("user-and-ribbon-same-column", () -> List.of(dropped()));
		BUILDERS.put("user-only", () -> List.of(dropped()));
		BUILDERS.put("column-hidden-dtindex", () -> List.of(dropped()));
		BUILDERS.put("quoted-comma-value", () -> List.of(
			RibbonItem.option("committed").column("phase").value("$eq(\"Committed, not pushed\")")));
		BUILDERS.put("two-options-same-opt-param", () -> List.of(
			RibbonItem.option("counts").param("opt").value("counts=true"),
			RibbonItem.option("compact").param("opt").value("density=compact")));
		BUILDERS.put("param-option", () -> List.of(RibbonItem.option("mine").param("owner").value("me")));
		BUILDERS.put("jrm-dropped-only", () -> List.of(
			dropped().title("Dropped only").group("filters").persist(true).symbol("filter_alt"),
			RibbonItem.export("copy", "csv").optional("excel", "pdf"),
			RibbonItem.refresh()));
		BUILDERS.put("foundry-daemon-stream", () -> List.of(RibbonItem.optionGroup("stream",
			all(),
			RibbonItem.option("out").title("stdout").column("stream").value("$eq(out)"),
			RibbonItem.option("err").title("stderr").column("stream").value("$eq(err)"))));
		BUILDERS.put("foundry-review-phase", () -> List.of(RibbonItem.optionGroup("reviewStateBucket",
			all(),
			RibbonItem.option("waiting").title("Waiting").column("phase").value("$in(Waiting,\"Partially reviewed\")"),
			RibbonItem.option("reviewed").title("Reviewed").column("phase")
				.value("$in(\"Ready to push\",\"Committed, not pushed\",Completed)"))));
		BUILDERS.put("foundry-work-flavor", () -> List.of(RibbonItem.optionGroup("workNotReadyFlavor",
			all(),
			RibbonItem.option("awaiting-you").title("Awaiting you").column("phase").value("$eq(\"Awaiting you\")"),
			RibbonItem.option("armed").title("Armed \u00b7 awaiting daemon").column("phase")
				.value("$eq(\"Armed \u00b7 awaiting daemon\")"))));
		BUILDERS.put("foundry-review-detail-isnew", () -> List.of(RibbonItem.optionGroup("sonarIssueFilter",
			RibbonItem.option("new").title("New only").column("isNew").value("$eq(yes)"),
			all())));
		BUILDERS.put("option-default-applied", () -> List.of(dropped().persist(true).defaultOn()));
		BUILDERS.put("group-default-applied", () -> List.of(phaseGroup().persist(true).defaultOption("pending")));
		BUILDERS.put("default-overridden-by-persisted", () -> List.of(
			dropped().persist(true).defaultOn(),
			phaseGroup().persist(true).defaultOption("pending")));
		BUILDERS.put("bus-target-and-publish", () -> List.of(
			RibbonItem.refresh().title("Refresh tasks").target("tasks"),
			RibbonItem.publish("Focus east", "app.region-picked", Map.of("region", "east"))));
	}

	private static List<Map<String,Object>> normalized(List<Map<String,Object>> ribbon) {
		// The corpus case still opens with the retired columnSearchToggle entry; the builder has no such item.
		return ribbon.stream().filter(e -> ! "columnSearchToggle".equals(e.get("type"))).collect(Collectors.toList());
	}

	@Test void a01_everyCorpusCaseMatches() throws Exception {
		var cases = RibbonCorpus_Test.corpus().cases;
		assertFalse(cases.isEmpty());
		for (var c : cases) {
			var builder = BUILDERS.get(c.name);
			assertNotNull(builder, "No builder for corpus case '" + c.name + "'");
			var actual = builder.get().stream().map(i -> (Map<String,Object>)i.toMap()).collect(Collectors.toList());
			assertEquals(normalized(c.ribbon), actual, c.name);
		}
	}

	@Test void a02_noStaleBuilders() throws Exception {
		var names = RibbonCorpus_Test.corpus().cases.stream().map(c -> c.name).collect(Collectors.toSet());
		assertEquals(names, BUILDERS.keySet());
	}
}
