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
import org.apache.juneau.rest.server.terminal.*;
import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.RowAction.*;
import org.apache.juneau.rest.server.views.TableSpec.*;

/**
 * Builder twins of the Operations pages: P6 Jobs ({@code jobs.ftlh}) and P7 Audit ({@code audit.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, OpsPageBuilders.<jsm>audit</jsm>());
 * </p>
 */
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
		return PageSpec.create().tab("ops/jobs").toolkit("views")
			.html("restock-help", "Restocking", """
				<div class="jc-prose"><p>Restock adds new available pets to the store as a background job; progress shows here until it finishes.
				Submitting twice from the same dialog runs one job.  Give the card below a wait to see a cancel before anything changes, or a longer hold to see one after.</p></div>""")
			.card(CardSpec.of("job-log", "restock").title("Restock inventory")
				.body("submit", Map.of("endpoint", "/console/ops/jobs/restock", "submitLabel", "Restock", "fields", List.of(
					Map.of("name", "DOG", "label", "Dogs (0-50)", "type", "number", "default", 1),
					Map.of("name", "slowMs", "label", "Wait before restocking (ms, 0-10000)", "type", "number", "default", 0),
					Map.of("name", "holdMs", "label", "Hold after restocking (ms, 0-10000)", "type", "number", "default", 1000))))
				.body("urlParam", "job"))
			.table(TableSpec.create("jobs").dataMode(DataMode.CLIENT).dataUrl("/console/ops/jobs/rows")
				.pollIntervalMs(2000)
				.columns(
					Column.create("jobId").label("Job"),
					Column.create("submitted").label("Submitted").render("timestamp"),
					Column.create("state").label("State").render("pill"),
					Column.create("result.message").label("Result"))
				.ribbon(
					RibbonItem.dialog("restock").title("Restock").endpoint("/console/ops/jobs/restock").method(Method.POST).form("/console/ops/jobs/restock-form"),
					RibbonItem.refresh().id("refresh")))
			.table(TableSpec.create("species").dataMode(DataMode.CLIENT).dataUrl("/console/ops/jobs/species")
				.pollIntervalMs(2000)
				.columns(
					Column.create("species").label("Species"),
					Column.create("available").label("Available"))
				.detail(RowDetail.create("/console/ops/jobs/species/{id}").region(JobLogRegion.create("restock-species")
					.submit(JobLogRegion.Submit.create("/console/ops/jobs/species/{id}/restock", "restock-species", "/console/ops/jobs/species/{id}/key")
						.submitLabel("Restock")
						.field(JobLogRegion.Field.of("count").label("How many (1-50)").type("number").defaultValue(1).required(true))
						.field(JobLogRegion.Field.of("species").type("hidden").source("row.species")))
					.detail(JobLogRegion.Detail.of("fieldGrid", RegionDef.Field.of("available").label("Available")))
					.output(JobLogRegion.Output.field("available").from("row.available"))
					.toRegion())))
			.card(CardSpec.of("terminal", "groom-output").title("Groom run").body("terminal", groomOutput.toMap()))
			.card(CardSpec.of("terminal", "groom-file").title("Groom transcript (file)").body("terminal", groomFile.toMap()))
			.table(TableSpec.create("groom-runs").dataMode(DataMode.CLIENT).dataUrl("/console/ops/jobs/groom-rows")
				.columns(
					Column.create("pet").label("Pet"),
					Column.create("submitted").label("Submitted").render("timestamp"),
					Column.create("state").label("State").render("pill"),
					Column.create("lines").label("Lines"))
				.detail(RowDetail.create("/console/ops/jobs/groom-runs/{id}")
					.region(RegionDef.create("regroom").type(RegionDef.TYPE_ROW_DETAIL).populate("petstore-regroom").allowPopulators("petstore-regroom")))
				.ribbon(
					RibbonItem.dialog("groom").title("Groom").endpoint("/console/ops/jobs/groom").method(Method.POST).form("/console/ops/jobs/groom-form"),
					RibbonItem.refresh().id("groom-refresh")));
	}

	/**
	 * P7 Audit.
	 *
	 * @return The page spec.
	 */
	public static PageSpec audit() {
		return PageSpec.create().tab("ops/audit").toolkit("views")
			.table(TableSpec.create("audit").dataMode(DataMode.SERVER).dataUrl("/console/ops/audit/query")
				.operators(SearchOperatorSet.standard())
				.defaultOrder("at", Sort.DESC)
				.columns(
					Column.create("at").label("When").render("timestamp").searchType(SearchType.TIMESTAMP),
					Column.create("actor").label("Actor").searchType(SearchType.TEXT),
					Column.create("action").label("Action").searchType(SearchType.TEXT),
					Column.create("entity").label("Entity").searchType(SearchType.TEXT),
					Column.create("entityId").label("Id").searchType(SearchType.TEXT),
					Column.create("detail").label("Detail").searchType(SearchType.TEXT))
				.ribbon(RibbonItem.export("copy", "csv"), RibbonItem.refresh().id("refresh")));
	}
}
