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

import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.RowAction.*;
import org.apache.juneau.rest.server.views.TableSpec.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * Builder twin of the Pending changes page (P10, {@code changes.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, ChangesPageBuilders.<jsm>changes</jsm>());
 * </p>
 */
@SuppressWarnings({
	"java:S1192" // Page-definition ids and labels repeat as inline literals by design.
})
public final class ChangesPageBuilders {

	private ChangesPageBuilders() {}

	/**
	 * P10 Pending changes.
	 *
	 * @return The page spec.
	 */
	public static PageSpec changes() {
		var selection = SelectionDef.create("id").selectAll(true).scope(SelectionDef.Scope.PERSISTENT).labelField("summary")
			.selectableWhen(RowActionEnabledRule.of("state", Op.EQ, "PENDING", "Only pending changes can be discarded or applied."));
		return PageSpec.create().tab("pets/changes").toolkit("views")
			.table(TableSpec.create("changes").rowType("PendingChange").dataMode(DataMode.SERVER).dataUrl("/console/pets/changes/query")
				.defaultOrder("id", Sort.DESC)
				.columns(
					Column.create("id").label("Change"),
					Column.create("summary").label("Change summary"),
					Column.create("author").label("Author"),
					Column.create("state").label("State").render("pill"),
					Column.create("message").label("Message").defaultVisible(false))
				.ribbon(
					RibbonItem.option("pending-only").title("Pending only").column("state").value("$eq(PENDING)").defaultOn(),
					RibbonItem.refresh())
				.rowActions(
					RowAction.create("retry").label("Retry").endpoint("/console/pets/changes/{id}/retry").method(Method.POST).onSuccess(OnSuccess.REDRAW)
						.visibleWhen(VisibilityRule.when("state").eq("FAILED")))
				.selection(selection)
				.bulk(BulkMutateDef.create(WritePermit.forCapability("pets.changes"), selection).actions(
					RowAction.create("discard").label("Discard").endpoint("/console/pets/changes/discard").method(Method.POST).bulkMode(BulkMode.AGGREGATE)
						.confirmTitle("Discard changes").confirm("Discard the selected changes?").tone(Tone.DANGER).confirmRenderer("typed"),
					RowAction.create("apply").label("Apply").endpoint("/console/pets/changes/{id}/apply").method(Method.POST).bulkMode(BulkMode.PER_ROW))),
				"Pending changes");
	}
}
