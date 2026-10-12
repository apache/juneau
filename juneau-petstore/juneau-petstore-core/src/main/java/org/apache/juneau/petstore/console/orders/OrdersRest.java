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

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;

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
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
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
	 * @param req The DataTables request.
	 * @return One page of orders, or a 200 {@code error} envelope for a malformed search expression.
	 */
	@RestPost(path="/query")
	public DataTablesResults<Order> query(@Content DataTablesRequest req) {
		try (var s = store().queryOrders()) {
			return DataTablesQuery.run(req, s);
		} catch (BeanQuerySyntaxException e) {
			return DataTablesResults.error(req.getDraw(), e.getMessage());
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
			if (text == null || text.isBlank())
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
