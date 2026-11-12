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
package org.apache.juneau.petstore.console.pages;

import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.terminal.region.*;
import org.apache.juneau.console.freemarker.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.console.views.RowAction.*;
import org.apache.juneau.console.views.TableSpec.*;

/**
 * Builder twins of the Operations pages: P6 Jobs ({@code jobs.ftlh}) and P7 Audit ({@code audit.ftlh}).
 *
 * <h2 class='section'>Example:</h2>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, OpsPageBuilders.<jsm>audit</jsm>());
 * </p>
 */
@SuppressWarnings({
	"java:S1192" // Page-definition ids and labels repeat as inline literals by design.
})
public final class OpsPageBuilders {

	private OpsPageBuilders() {}

	/**
	 * P6 Jobs.
	 *
	 * @param groomOutput The latest groom run's terminal.
	 * @param groomFile The latest groom run's transcript file.
	 * @return The page spec.
	 */
	public static PageSpec jobs(TerminalDef groomOutput, TerminalDef groomFile) {
		return PageSpec.create().tab("ops/jobs").toolkit("views", "datatables-buttons")
			.card(SwitcherDef.create("jobs-sw").style(SwitcherDef.STYLE_SLIDER).initialView("restock")
				.view("restock", "Restock", v -> v.cards(
					CardSpec.html("restock-help").title("Restocking").src("/console/ops/jobs/fragments/restock-help"),
					restockJobLog()))
				.view("jobs", "Jobs", v -> v.section(SwitcherCards.table(jobsTable())))
				.view("species", "Species", v -> v.section(SwitcherCards.table(speciesTable())))
				.view("groom", "Groom", v -> v.cards(
					CardSpec.of("terminal", "groom-output").title("Groom run").body("terminal", groomOutput.toMap()),
					CardSpec.of("terminal", "groom-file").title("Groom transcript (file)").body("terminal", groomFile.toMap()),
					SwitcherCards.table(groomRunsTable())))
				.toCardSpec());
	}

	private static CardSpec restockJobLog() {
		return CardSpec.of("job-log", "restock").title("Restock inventory")
				.body("submit", Map.of("endpoint", "/console/ops/jobs/restock", "submitLabel", "Restock", "fields", List.of(
					Map.of("name", "DOG", "label", "Dogs (0-50)", "type", "number", "default", 1),
					Map.of("name", "slowMs", "label", "Wait before restocking (ms, 0-10000)", "type", "number", "default", 0),
					Map.of("name", "holdMs", "label", "Hold after restocking (ms, 0-10000)", "type", "number", "default", 1000))))
				.body("urlParam", "job");
	}

	private static TableSpec jobsTable() {
		return TableSpec.create("jobs").dataMode(DataMode.CLIENT).dataUrl("/console/ops/jobs/rows")
				.pollIntervalMs(2000)
				.columnConfig(true)
				.columns(
					Column.create("jobId").label("Job"),
					Column.create("submitted").label("Submitted").render("timestamp"),
					Column.create("state").label("State").render(PillRenders.jobState()),
					Column.create("result.message").label("Result"))
				.ribbon(Ribbons.standard()
					.create(RibbonItem.dialog("restock").title("Restock").endpoint("/console/ops/jobs/restock").method(Method.POST).form("/console/ops/jobs/restock-form"))
					.build());
	}

	private static TableSpec speciesTable() {
		return TableSpec.create("species").dataMode(DataMode.CLIENT).dataUrl("/console/ops/jobs/species")
				.pollIntervalMs(2000)
				.columnConfig(true)
				.columns(
					Column.create("species").label("Species").render(PillRenders.species()),
					Column.create("available").label("Available"))
				.detail(RowDetail.create("/console/ops/jobs/species/{id}").region(JobLogRegion.create("restock-species")
					.submit(JobLogRegion.Submit.create("/console/ops/jobs/species/{id}/restock", "restock-species", "/console/ops/jobs/species/{id}/key")
						.submitLabel("Restock")
						.field(JobLogRegion.Field.of("count").label("How many (1-50)").type("number").defaultValue(1).required(true))
						.field(JobLogRegion.Field.of("species").type("hidden").source("row.species")))
					.detail(JobLogRegion.Detail.of("fieldGrid", RegionDef.Field.of("available").label("Available")))
					.output(JobLogRegion.Output.field("available").from("row.available"))
					.toRegion()))
				.ribbon(Ribbons.standard().build());
	}

	private static TableSpec groomRunsTable() {
		return TableSpec.create("groom-runs").dataMode(DataMode.CLIENT).dataUrl("/console/ops/jobs/groom-rows")
				.columns(
					Column.create("pet").label("Pet"),
					Column.create("submitted").label("Submitted").render("timestamp"),
					Column.create("state").label("State").render(PillRenders.jobState()),
					Column.create("lines").label("Lines"))
				.detail(RowDetail.create("/console/ops/jobs/groom-runs/{id}")
					.region(RegionDef.create("regroom").type(RegionDef.TYPE_ROW_DETAIL).populate("petstore-regroom").allowPopulators("petstore-regroom")))
				.columnConfig(true)
				.ribbon(Ribbons.standard()
					.create(RibbonItem.dialog("groom").title("Groom").endpoint("/console/ops/jobs/groom").method(Method.POST).form("/console/ops/jobs/groom-form"))
					.refreshId("groom-refresh")
					.build());
	}

	/**
	 * P7 Audit.
	 *
	 * @return The page spec.
	 */
	public static PageSpec audit() {
		return PageSpec.create().tab("ops/audit").toolkit("views", "datatables-buttons")
			.table(TableSpec.create("audit").dataMode(DataMode.SERVER).dataUrl("/console/ops/audit/query")
				.operators(SearchOperatorSet.standard())
				.defaultOrder("at", Sort.DESC)
				.detail(RowDetails.audit())
				.columns(
					Column.create("id").label("Id").defaultVisible(false),
					Column.create("at").label("When").render("timestamp").searchType(SearchType.TIMESTAMP),
					Column.create("actor").label("Actor").searchType(SearchType.TEXT),
					Column.create("action").label("Action").render(PillRenders.auditAction()).searchType(SearchType.TEXT),
					Column.create("entity").label("Entity").render(PillRenders.entity()).searchType(SearchType.TEXT),
					Column.create("entityId").label("Id").searchType(SearchType.TEXT),
					Column.create("detail").label("Detail").searchType(SearchType.TEXT))
				.columnConfig(true)
				.ribbon(Ribbons.standard().build()));
	}
}
