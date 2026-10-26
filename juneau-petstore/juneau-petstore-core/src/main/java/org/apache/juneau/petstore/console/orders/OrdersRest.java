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
package org.apache.juneau.petstore.console.orders;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.commons.Schema;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.console.datatables.*;
import org.apache.juneau.console.datatables.adapter.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.console.views.*;
import org.apache.juneau.console.widgets.*;

/**
 * The Orders page and its endpoints: the server-mode datatable query over BeanQuery, the {@code approve} and
 * {@code deliver} status transitions, and the cancel dialog form with its {@code DELETE}.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// PLACED to APPROVED; any other status is a 409.</jc>
 * 	<ja>@RestPost</ja>(path=<js>"/{id}/approve"</js>)
 * 	<jk>public</jk> ActionResult approve(RestRequest <jv>req</jv>, <ja>@Path</ja>(<js>"id"</js>) <jk>long</jk> <jv>id</jv>) {
 * 		<jk>return</jk> transition(<jv>req</jv>, <jv>id</jv>, <js>"approve"</js>, OrderStatus.<jsf>PLACED</jsf>, OrderStatus.<jsf>APPROVED</jsf>);
 * 	}
 * </p>
 * <p class='bcode'>
 * 	&lt;@card type="datatables" id="orders"&gt;
 * 	{ dataMode:'server', dataUrl:'/console/orders/query', rowActions:[{id:'approve', endpoint:'/console/orders/{id}/approve', method:'POST'}] }
 * 	&lt;/@card&gt;
 * </p>
 */
@Rest(path="/orders", title="Orders")
@SuppressWarnings({
	"java:S110", // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
	"java:S1192" // Route paths and field names repeat as inline literals by design.
})
public class OrdersRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/**
	 * The Orders page.
	 *
	 * @param req The request; {@code ?builder} renders the builder twin.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		return builder(req) ? twin(req, OrdersPageBuilders.orders()) : FreemarkerView.of("orders.ftlh");
	}

	/**
	 * Server-mode DataTables query over all orders.
	 *
	 * <p>
	 * The optional {@code day} query parameter ({@code yyyy-mm-dd}) narrows the orders to those whose ship date falls in
	 * that day's window, before the request's own column filters apply, so the two combine.  A missing, blank or
	 * malformed value is ignored.
	 *
	 * <p>
	 * The optional {@code start} and {@code end} query parameters (the {@code window} URL-state directive's defaults;
	 * each a {@code yyyy-mm-dd} date or an ISO date-time, whose calendar date is used) keep the orders whose ship date
	 * falls between them, both days included, and either may be given alone.  A bound that is blank or malformed is
	 * ignored.  An order with no readable ship date is outside any window that has a usable bound.  The window and
	 * {@code day} combine with each other and with the request's own column filters.
	 *
	 * @param req The DataTables request.
	 * @param day The day to keep, or {@code null}.
	 * @param start The first ship day to keep, or {@code null}.
	 * @param end The last ship day to keep, or {@code null}.
	 * @return One page of orders, or a 200 {@code error} envelope for a malformed search expression.
	 */
	@RestPost(path="/query")
	public DataTablesResults<Order> query(@Content DataTablesRequest req, @Query("day") @Schema(aev=true) String day,
			@Query("start") @Schema(aev=true) String start, @Query("end") @Schema(aev=true) String end) {
		var wanted = parseDay(day);
		var from = shipDay(start);
		var to = shipDay(end);
		var orders = store().getOrders();
		if (wanted != null)
			orders = orders.stream().filter(o -> wanted.equals(shipDay(o.getShipDate()))).toList();
		if (from != null || to != null)
			orders = orders.stream().filter(o -> inWindow(shipDay(o.getShipDate()), from, to)).toList();
		try (var s = PetStore.ORDER_QUERY.getSession(orders)) {
			return DataTablesQuery.run(req, s);
		} catch (BeanQuerySyntaxException e) {
			return DataTablesResults.error(req.getDraw(), e.getMessage());
		}
	}

	/** Whether a ship day is inside the window; either bound may be {@code null} (open), and no ship day is never inside. */
	private static boolean inWindow(LocalDate shipped, LocalDate from, LocalDate to) {
		return shipped != null && (from == null || ! shipped.isBefore(from)) && (to == null || ! shipped.isAfter(to));
	}

	private static LocalDate parseDay(String value) {
		if (value == null)
			return null;
		try {
			return LocalDate.parse(value.trim());
		} catch (@SuppressWarnings("unused") DateTimeException e) {
			return null;
		}
	}

	/** The calendar date of an order's ship date, which is an ISO date or an ISO date-time; {@code null} if it is neither. */
	private static LocalDate shipDay(String value) {
		var day = parseDay(value);
		if (day != null || value == null)
			return day;
		try {
			return DateTimeFormatter.ISO_DATE_TIME.parse(value.trim(), LocalDate::from);
		} catch (@SuppressWarnings("unused") DateTimeException e) {
			return null;
		}
	}

	/**
	 * The new-order wizard, step one: pick a pet that is for sale and an optional ship date.
	 *
	 * <p>
	 * The {@code review} button opens the stacked second step and carries these inputs to it as drafts.  That step's
	 * Confirm posts to {@code /console/orders}, as declared on the child action.
	 *
	 * @return The modal, on the compact chrome.
	 */
	@RestGet(path="/new-form")
	public ModalDef newForm() {
		var pet = FormDef.Input.of("petId", "Pet", "select").required();
		store().getPets().stream().filter(p -> p.getStatus() == PetStatus.AVAILABLE)
			.sorted(Comparator.comparingLong(Pet::getId))
			.forEach(p -> pet.option(String.valueOf(p.getId()), p.getName()));
		var form = FormDef.create()
			.field(pet)
			.field(FormDef.Input.of("shipDate", "Ship date (yyyy-mm-dd)", "text"))
			.field(FormDef.Input.of("review", "Review order", "action").actionId("review"));
		return ModalDef.create("New order: choose a pet").chrome("compact")
			.form(form)
			.childAction(ModalDef.ChildAction.of("review", "Review order").form("/console/orders/review-form").carryDrafts(true)
				// The stacked step's Confirm submits with the child's own endpoint and method; without them it sends nothing.
				.endpoint("/console/orders").method("POST").onSuccess("redraw"));
	}

	/**
	 * The new-order wizard, step two: the order about to be placed, read back from the first step's drafts.
	 *
	 * @param drafts The first step's inputs as JSON, carried by the dialog as {@code juneauDrafts}.
	 * @return The modal, on the compact chrome, whose form re-posts the validated values.
	 * @throws BadRequest If the drafts are missing, not JSON, or carry a bad ship date.
	 * @throws NotFound If the pet does not exist.
	 * @throws Conflict If the pet is not available.
	 */
	@RestGet(path="/review-form")
	public ModalDef reviewForm(@Query("juneauDrafts") String drafts) {
		if (ib(drafts))
			throw new BadRequest("Missing order draft");
		JsonMap m;
		try {
			m = new JsonMap(drafts);
		} catch (Exception e) {
			throw new BadRequest("Invalid order draft: '%s'", e.getMessage());
		}
		var pet = pet(m.getString("petId"));
		var shipDate = shipDate(m.getString("shipDate"));
		return ModalDef.create("Review order").chrome("compact")
			.field("Pet", pet.getName())
			.field("Ship date", shipDate == null ? "(none)" : shipDate)
			.form(FormDef.create()
				.field(FormDef.Input.of("petId", "Pet", "text").value(String.valueOf(pet.getId())))
				.field(FormDef.Input.of("shipDate", "Ship date", "text").value(shipDate == null ? "" : shipDate)));
	}

	/**
	 * Places a new order.
	 *
	 * @param req The request (audit actor).
	 * @param body The order, as the plain {@code {petId, shipDate}} or the dialog envelope {@code {fields:{...}}}.
	 * @return The result toast, carrying the stored order.
	 * @throws BadRequest On a missing body, non-object {@code fields}, missing pet or a bad ship date.
	 * @throws NotFound If the pet does not exist.
	 * @throws Conflict If the pet is not available.
	 */
	@RestPost(path="/")
	public ActionResult create(RestRequest req, @Content JsonMap body) {
		var actor = ConsoleWrites.actor(req);
		if (body == null)
			throw new BadRequest("Missing order body");
		JsonMap fields;
		try {
			fields = body.getMap("fields");
		} catch (Exception e) {
			throw new BadRequest("Invalid order fields: '%s'", e.getMessage());
		}
		if (fields == null && body.get("fields") != null)
			throw new BadRequest("Invalid order fields: '%s'", body.get("fields"));
		var f = fields != null ? fields : body;
		var pet = pet(f.getString("petId"));
		var order = new Order().setPetId(pet.getId()).setShipDate(shipDate(f.getString("shipDate"))).setStatus(OrderStatus.PLACED);
		var stored = store().createOrder(order, actor);
		return ActionResult.success(stored).message(f("Placed order '%s' for '%s'", stored.getId(), pet.getName()));
	}

	private Pet pet(String id) {
		if (ib(id))
			throw new BadRequest("Missing pet");
		Pet p = null;
		try {
			p = store().getPet(Long.parseLong(id.trim()));
		} catch (NumberFormatException e) {
			throw new BadRequest("Invalid pet '%s'", id);
		}
		if (p == null)
			throw new NotFound("Unknown pet '%s'", id);
		if (p.getStatus() != PetStatus.AVAILABLE)
			throw new Conflict("Pet '%s' is '%s'; only an available pet can be ordered", id, p.getStatus());
		return p;
	}

	private static String shipDate(String value) {
		if (ib(value))
			return null;
		try {
			return LocalDate.parse(value.trim()).toString();
		} catch (DateTimeException e) {
			throw new BadRequest("Invalid ship date '%s'", value);
		}
	}

	/**
	 * Approves a placed order.
	 *
	 * @param req The request.
	 * @param id The order id.
	 * @return The result toast, carrying the order.
	 * @throws NotFound If the order does not exist.
	 * @throws Conflict If it is not {@code PLACED}.
	 */
	@RestPost(path="/{id}/approve")
	public ActionResult approve(RestRequest req, @Path("id") long id) {
		return transition(req, id, "approve", OrderStatus.PLACED, OrderStatus.APPROVED);
	}

	/**
	 * Delivers an approved order.
	 *
	 * @param req The request.
	 * @param id The order id.
	 * @return The result toast, carrying the order.
	 * @throws NotFound If the order does not exist.
	 * @throws Conflict If it is not {@code APPROVED}.
	 */
	@RestPost(path="/{id}/deliver")
	public ActionResult deliver(RestRequest req, @Path("id") long id) {
		return transition(req, id, "deliver", OrderStatus.APPROVED, OrderStatus.DELIVERED);
	}

	/**
	 * The cancel dialog form: a reason and whether to notify the customer.
	 *
	 * @param id The order id.
	 * @return The modal.
	 * @throws NotFound If the order does not exist.
	 */
	@RestGet(path="/{id}/cancel-form")
	public ModalDef cancelForm(@Path("id") long id) {
		require(id);
		return ModalDef.create(f("Cancel order '%s'", id))
			.form(FormDef.create()
				.field(FormDef.Input.of("reason", "Reason", "text"))
				.field(FormDef.Input.of("notify", "Notify the customer", "checkbox")));
	}

	/**
	 * Deletes (cancels) an order.
	 *
	 * @param req The request.
	 * @param id The order id.
	 * @return The result toast.
	 * @throws BadRequest If the optional body is not a JSON object.
	 * @throws NotFound If the order does not exist.
	 */
	@RestDelete(path="/{id}")
	public ActionResult cancel(RestRequest req, @Path("id") long id) {
		var actor = ConsoleWrites.actor(req);
		require(id);
		var reason = reason(req);
		store().deleteOrder(id, actor, reason);
		return ActionResult.success(null).message(f("Cancelled order '%s'", id));
	}

	/** The optional body: the cancel dialog envelope {@code {fields:{reason, notify}}}, or {@code {reason}} from a plain API call. */
	private static String reason(RestRequest req) {
		try {
			var text = req.getContent().asString();
			if (ib(text))
				return null;
			var body = new JsonMap(text);
			var fields = body.getMap("fields");
			return (fields != null ? fields : body).getString("reason");
		} catch (Exception e) {
			throw new BadRequest("Invalid cancel body: '%s'", e.getMessage());
		}
	}

	private ActionResult transition(RestRequest req, long id, String verb, OrderStatus from, OrderStatus to) {
		var actor = ConsoleWrites.actor(req);
		var o = require(id);
		if (o.getStatus() != from)
			throw new Conflict("Order '%s' is '%s'; cannot '%s'", id, o.getStatus(), verb);
		// Update a copy: the live order must not change unless the store accepts the write.
		var next = new Order().setId(o.getId()).setPetId(o.getPetId()).setShipDate(o.getShipDate()).setStatus(to);
		return ActionResult.success(store().updateOrder(next, actor)).message(f("Order '%s' %s", id, to));
	}

	private Order require(long id) {
		var o = store().getOrder(id);
		if (o == null)
			throw new NotFound("Unknown order '%s'", id);
		return o;
	}
}
