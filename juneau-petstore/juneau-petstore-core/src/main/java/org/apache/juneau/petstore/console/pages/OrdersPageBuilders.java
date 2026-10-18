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

import org.apache.juneau.rest.server.view.freemarker.console.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.views.RowAction.*;
import org.apache.juneau.rest.server.views.TableSpec.*;

/**
 * Builder twin of the Orders page (P5, {@code orders.ftlh}).
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jk>return</jk> <jsm>twin</jsm>(<jv>req</jv>, OrdersPageBuilders.<jsm>orders</jsm>());
 * </p>
 */
@SuppressWarnings({
	"java:S1192" // Page-definition ids and labels repeat as inline literals by design.
})
public final class OrdersPageBuilders {

	private OrdersPageBuilders() {}

	/**
	 * P5 Orders.
	 *
	 * @return The page spec.
	 */
	public static PageSpec orders() {
		return PageSpec.create().tab("orders").toolkit("views")
			.table(TableSpec.create("orders").rowType("Order").dataMode(DataMode.SERVER).dataUrl("/console/orders/query").urlState("day", "window")
				.columnConfig(Map.of("tabs", List.of("columns", "sort")))
				.defaultOrder("id", Sort.ASC)
				.columns(
					Column.create("id").label("Id"),
					Column.create("petId").label("Pet").href("/console/pets/{petId}/summary"),
					Column.create("shipDate").label("Ship date").render("date"),
					Column.create("status").label("Status").render("pill"))
				.ribbon(
					RibbonItem.optionGroup("status",
						RibbonItem.option("placed").title("Placed").column("status").value("$eq(PLACED)"),
						RibbonItem.option("approved").title("Approved").column("status").value("$eq(APPROVED)"),
						RibbonItem.option("delivered").title("Delivered").column("status").value("$eq(DELIVERED)"))
						.title("Order status").deselectable(true),
					RibbonItem.dialog("new-order").title("+ New order").symbol("plus")
						.form("/console/orders/new-form").endpoint("/console/orders").method(Method.POST).onSuccess(OnSuccess.REDRAW),
					RibbonItem.export("copy", "csv").optional("excel", "pdf"),
					RibbonItem.refresh())
				.rowActions(
					RowAction.create("approve").label("Approve").endpoint("/console/orders/{id}/approve").method(Method.POST).onSuccess(OnSuccess.REDRAW)
						.visibleWhen(VisibilityRule.when("status").eq("PLACED")),
					RowAction.create("deliver").label("Deliver").endpoint("/console/orders/{id}/deliver").method(Method.POST).onSuccess(OnSuccess.REDRAW)
						.visibleWhen(VisibilityRule.when("status").eq("APPROVED")),
					RowAction.create("cancel").label("Cancel").form("/console/orders/{id}/cancel-form").present(Present.DIALOG)
						.endpoint("/console/orders/{id}").method(Method.DELETE).onSuccess(OnSuccess.REDRAW),
					RowAction.create("delete").label("Delete").endpoint("/console/orders/{id}").method(Method.DELETE)
						.confirm("Delete order '{id}'?").confirmLabel("Delete").tone(Tone.DANGER).confirmRenderer("typed").onSuccess(OnSuccess.REDRAW)),
				"Orders");
	}
}
