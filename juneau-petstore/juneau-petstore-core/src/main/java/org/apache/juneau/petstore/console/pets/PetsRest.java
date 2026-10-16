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
package org.apache.juneau.petstore.console.pets;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;

import org.apache.juneau.commons.Schema;
import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.marshall.parser.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.console.pages.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.view.freemarker.*;
import org.apache.juneau.rest.server.views.*;
import org.apache.juneau.rest.server.widgets.*;

/**
 * The pets JSON endpoints (server-mode datatable query over BeanQuery, create/replace/delete, sell, stage, and the
 * per-pet feeds), the Pets and Sold pages, the add/edit dialog form, and the P4 pet pages: Summary (inline edit),
 * Photo, Orders (with a row detail, and one order selected at nav depth 4) and History, whose nav nodes the page
 * appends under {@code pets} as {@code p<petId>} and {@code o<orderId>}.  The Pending changes page is the
 * {@link ChangesRest} child.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// The BQ#6 endpoint behind the table.</jc>
 * 	<ja>@RestPost</ja>(path=<js>"/query"</js>)
 * 	<jk>public</jk> DataTablesResults&lt;Pet&gt; query(<ja>@Content</ja> DataTablesRequest <jv>req</jv>) {
 * 		<jk>try</jk> (<jk>var</jk> <jv>s</jv> = store().queryPets()) {
 * 			<jk>return</jk> DataTablesQuery.<jsm>run</jsm>(<jv>req</jv>, <jv>s</jv>);
 * 		}
 * 	}
 * </p>
 * <p class='bcode'>
 * 	&lt;@card type="datatables" id="pets"&gt;
 * 	{ dataMode:'server', dataUrl:'/console/pets/query', columns:[{key:'name', label:'Name'}, ...] }
 * 	&lt;/@card&gt;
 * </p>
 * <p class='bjava'>
 * 	<jc>// The same table in the builder twin.</jc>
 * 	TableSpec.<jsm>create</jsm>(<js>"pets"</js>).dataMode(DataMode.<jsf>SERVER</jsf>).dataUrl(<js>"/console/pets/query"</js>)
 * 		.columns(Column.<jsm>create</jsm>(<js>"name"</js>).label(<js>"Name"</js>));
 * </p>
 */
@Rest(path="/pets", title="Pets", children={ChangesRest.class})
@SuppressWarnings({
	"java:S110" // Inheritance depth comes from the BasicRestServlet hierarchy, not this page.
})
public class PetsRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** The P4 tabs, in nav order. */
	private static final List<String> TABS = List.of("summary", "photo", "orders", "history");

	/**
	 * The Pets page.
	 *
	 * @param req The request; {@code ?mixed} renders the half-FTL variant, {@code ?builder} the builder twin.
	 * @return The page.
	 */
	@RestGet(path="/")
	public View page(RestRequest req) {
		var mixed = req.containsQueryParam("mixed");
		if (! builder(req))
			return mixed ? PetsPageBuilders.petsMixed().view(req) : FreemarkerView.of("pets.ftlh");
		return twin(req, mixed ? PetsPageBuilders.petsMixed() : PetsPageBuilders.pets());
	}

	/**
	 * The Sold pets page.
	 *
	 * @param req The request; {@code ?builder} renders the builder twin.
	 * @return The page.
	 */
	@RestGet(path="/sold")
	public View sold(RestRequest req) {
		return builder(req) ? twin(req, PetsPageBuilders.sold()) : FreemarkerView.of("pets-sold.ftlh");
	}

	/** @return The client-mode rows for the Sold page: every sold pet, by id. */
	@RestGet(path="/sold/rows")
	public List<Pet> soldRows() {
		return store().getPets().stream().filter(x -> x.getStatus() == PetStatus.SOLD).sorted(Comparator.comparingLong(Pet::getId)).toList();
	}

	/**
	 * The add/edit dialog form.
	 *
	 * @param id The pet to edit, or <jk>null</jk> to add.
	 * @return The modal.
	 * @throws NotFound If {@code id} names no pet.
	 */
	@RestGet(path="/form")
	public ModalDef form(@Query("id") Long id) {
		var p = id == null ? new Pet().setStatus(PetStatus.AVAILABLE) : require(id);
		var form = FormDef.create()
			.field(FormDef.Input.of("name", "Name", "text").required().value(p.getName()));
		var species = FormDef.Input.of("species", "Species", "select").required();
		for (var s : Species.values())
			species.option(s.name(), s.name().charAt(0) + s.name().substring(1).toLowerCase());
		if (p.getSpecies() != null)
			species.value(p.getSpecies().name());
		form.field(species);
		form.field(FormDef.Input.of("price", "Price (USD)", "text").required().value(id == null ? "" : String.valueOf(p.getPrice())));
		return ModalDef.create(id == null ? "Add pet" : f("Edit '%s'", p.getName())).form(form);
	}

	/**
	 * Server-mode DataTables query over all pets.
	 *
	 * <p>
	 * The optional {@code ids} query parameter (comma-separated pet ids, as the {@code ids} URL-state directive sends it)
	 * narrows the pets to those ids, before the request's own column filters apply, so the two combine.  Tokens that are
	 * not numbers are dropped, and a blank value or one with no usable token is ignored.
	 *
	 * @param req The DataTables request.
	 * @param ids The ids to keep, or {@code null}.
	 * @return One page of pets, or a 200 {@code error} envelope for a malformed search expression.
	 */
	@RestPost(path="/query")
	public DataTablesResults<Pet> query(@Content DataTablesRequest req, @Query("ids") @Schema(aev=true) String ids) {
		var wanted = parseIds(ids);
		var pets = store().getPets();
		if (wanted != null)
			pets = pets.stream().filter(p -> wanted.contains(p.getId())).toList();
		try (var s = PetStore.PET_QUERY.getSession(pets)) {
			return DataTablesQuery.run(req, s);
		} catch (BeanQuerySyntaxException e) {
			// DataTablesQuery maps a bad expression to HTTP 400 by default; the table wants a 200 error envelope it can show.
			return DataTablesResults.error(req.getDraw(), e.getMessage());
		}
	}

	/** The usable ids in a comma-separated value, or {@code null} if there are none (no filter). */
	private static Set<Long> parseIds(String value) {
		if (value == null)
			return null;
		var out = new HashSet<Long>();
		for (var token : value.split(",")) {
			try {
				out.add(Long.parseLong(token.trim()));
			} catch (@SuppressWarnings("unused") NumberFormatException e) {
				// Not a pet id; dropped.
			}
		}
		return out.isEmpty() ? null : out;
	}

	/**
	 * Creates a pet.  Status defaults to {@code AVAILABLE}.
	 *
	 * @param req The request (write gate, audit actor).
	 * @param body The new pet, either as the plain pet or as the dialog envelope {@code {fields:{...}}}.
	 * @return The result toast, carrying the stored pet.
	 * @throws BadRequest On a blank name or negative price.
	 */
	@RestPost(path="/")
	public ActionResult create(RestRequest req, @Content JsonMap body) {
		var actor = ConsoleWrites.actor(req);
		var pet = toPet(body);
		validate(pet);
		if (pet.getStatus() == null)
			pet.setStatus(PetStatus.AVAILABLE);
		var p = store().createPet(pet, actor);
		return ActionResult.success(p).message(f("Added '%s'", p.getName()));
	}

	/**
	 * Replaces a pet.  Name and price are required; a species, tags or photo left out of the body keep their
	 * current values, as does the status.
	 *
	 * @param req The request.
	 * @param id The pet id.
	 * @param body The new state, either as the plain pet or as the dialog envelope {@code {fields:{...}}}.
	 * @return The result toast.
	 * @throws NotFound If the pet does not exist.
	 */
	@RestPut(path="/{id}")
	public ActionResult update(RestRequest req, @Path("id") long id, @Content JsonMap body) {
		// The edit form offers name, species and price but no status, so for a saved edit (for example from the
		// store treemap) species is the grouping field that can change; status moves only through the sell action.
		var actor = ConsoleWrites.actor(req);
		var old = require(id);
		var pet = toPet(body);
		validate(pet);
		pet.setId(id);
		pet.defaultStatus(old.getStatus()).defaultSpecies(old.getSpecies()).defaultTags(old.getTags()).defaultPhoto(old.getPhoto());
		return ActionResult.success(store().updatePet(pet, actor)).message(f("Saved '%s'", pet.getName()));
	}

	/**
	 * Deletes a pet.
	 *
	 * @param req The request.
	 * @param id The pet id.
	 * @return The result toast.
	 * @throws NotFound If the pet does not exist.
	 */
	@RestDelete(path="/{id}")
	public ActionResult delete(RestRequest req, @Path("id") long id) {
		var actor = ConsoleWrites.actor(req);
		var p = require(id);
		store().deletePet(id, actor);
		return ActionResult.success(null).message(f("Deleted '%s'", p.getName()));
	}

	/**
	 * Marks a pet sold.  The Pets page bulk "Mark sold (N)" action calls this per row.
	 *
	 * @param req The request.
	 * @param id The pet id.
	 * @return The result toast, carrying the updated pet.
	 * @throws NotFound If the pet does not exist.
	 * @throws Conflict If it is already sold.
	 */
	@RestPost(path="/{id}/sell")
	public ActionResult sell(RestRequest req, @Path("id") long id) {
		var actor = ConsoleWrites.actor(req);
		var p = require(id);
		if (p.getStatus() == PetStatus.SOLD)
			throw new Conflict("Pet '%s' is already '%s'", id, p.getStatus());
		// Update a copy: the live pet must not change unless the store accepts the write.
		var sold = new Pet().setId(p.getId()).setSpecies(p.getSpecies()).setName(p.getName()).setPrice(p.getPrice())
			.setTags(p.getTags()).setStatus(PetStatus.SOLD).setPhoto(p.getPhoto());
		return ActionResult.success(store().updatePet(sold, actor)).message(f("Sold '%s'", p.getName()));
	}

	/**
	 * Stages an inline edit for review on the Pending changes page.  The pet itself is not changed until the change is applied.
	 *
	 * <p>
	 * {@link PendingChanges#stage} owns which fields are stageable and its messages.  The photo is deliberately not
	 * stageable: the pet detail Photo tab edits it directly through the API's own {@code PUT /petstore/pets/{id}/photo}.
	 *
	 * @param req The request.
	 * @param id The pet id.
	 * @param body {@code {field, value}}, or the inline-edit envelope {@code {fields:{<field>:value}}} with exactly one field.
	 * @return {@code success} carrying the new {@link PendingChange}.
	 * @throws BadRequest If the field is missing or not stageable, or the value is invalid.
	 * @throws NotFound If the pet does not exist.
	 */
	@RestPost(path="/{id}/stage")
	public ActionResult stage(RestRequest req, @Path("id") long id, @Content JsonMap body) {
		var actor = ConsoleWrites.actor(req);
		String field;
		String value;
		// The 'edit' column renderer commits {fields:{<column>:value}} (plus its dialog envelope keys) through the row action.
		var fields = body.getMap("fields");
		if (fields != null) {
			if (fields.size() != 1)
				throw new BadRequest("Exactly one field can be staged at a time; got '%s'", fields.size());
			field = fields.keySet().iterator().next();
			value = fields.getString(field);
		} else {
			field = body.getString("field");
			value = body.getString("value");
		}
		if (field == null || field.isBlank())
			throw new BadRequest("Missing field to stage");
		try {
			var c = store().pendingChanges().stage(id, field, value, ConsoleWrites.author(actor));
			return ActionResult.success(c).message(f("Staged '%s'", c.getSummary()));
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e.getMessage());
		}
	}

	/**
	 * @param id The pet id.
	 * @return This pet's orders (the related list), by id.
	 * @throws NotFound If the pet does not exist.
	 */
	@RestGet(path="/{id}/orders/rows")
	public List<Order> orderRows(@Path("id") long id) {
		require(id);
		return store().getOrders().stream().filter(x -> x.getPetId() == id).sorted(Comparator.comparingLong(Order::getId)).toList();
	}

	/**
	 * @param id The pet id.
	 * @return The audit rows for this pet, oldest first.
	 * @throws NotFound If the pet does not exist.
	 */
	@RestGet(path="/{id}/history/rows")
	public List<AuditEntry> historyRows(@Path("id") long id) {
		require(id);
		var t = String.valueOf(id);
		return store().getAudit().stream().filter(x -> "Pet".equals(x.getEntity()) && t.equals(x.getEntityId())).toList();
	}

	/**
	 * The bare pet URL; lands on the first tab.
	 *
	 * @param req The request.
	 * @param id The pet id.
	 * @return A 303 to {@code /console/pets/{id}/summary}.
	 */
	@RestGet(path="/{id}")
	public SeeOther detail(RestRequest req, @Path("id") long id) {
		return new SeeOther().setLocation(req.getUriResolver().resolve("servlet:/" + id + "/summary"));
	}

	/**
	 * P4, one tab.
	 *
	 * @param req The request (viewer roles).
	 * @param res The response (404 status for the not-found card).
	 * @param id The pet id.
	 * @param tab One of {@link #TABS}.
	 * @return The tab page, or the not-found card with a 404 status.
	 * @throws NotFound If the tab is unknown.
	 */
	@RestGet(path="/{id}/{tab}")
	public View detailTab(RestRequest req, RestResponse res, @Path("id") long id, @Path("tab") String tab) {
		if (! TABS.contains(tab))
			throw new NotFound("Unknown tab '%s'", tab);
		return petPage(req, res, id, tab, null);
	}

	/**
	 * P4 at nav depth 4: the Orders tab with one of this pet's orders selected.
	 *
	 * @param req The request.
	 * @param res The response.
	 * @param id The pet id.
	 * @param orderId The order id.
	 * @return The Orders tab with the order card, or the not-found card with a 404 status if the pet does not exist.
	 * @throws NotFound If the order does not exist or is not this pet's.
	 */
	@RestGet(path="/{id}/orders/{orderId}")
	public View detailOrder(RestRequest req, RestResponse res, @Path("id") long id, @Path("orderId") long orderId) {
		// An unknown pet gets the not-found card, not the order 404.
		return petPage(req, res, id, "orders", store().getPet(id) == null ? null : requireOrder(id, orderId));
	}

	/**
	 * @param id The pet id.
	 * @return The Summary card's single row: this pet, as a one-element list (the card is client-mode).
	 * @throws NotFound If the pet does not exist.
	 */
	@RestGet(path="/{id}/row")
	public List<Pet> row(@Path("id") long id) {
		return List.of(require(id));
	}

	/**
	 * The Orders tab's row-detail GET.
	 *
	 * @param id The pet id.
	 * @param orderId The order id.
	 * @return {@code {contractVersion, fields}} (see {@link RowDetail#response(Object)}).
	 * @throws NotFound If the order does not exist or is not this pet's.
	 */
	@RestGet(path="/{id}/orders/{orderId}/detail")
	public JsonMap orderDetail(@Path("id") long id, @Path("orderId") long orderId) {
		return RowDetail.response(requireOrder(id, orderId));
	}

	private View petPage(RestRequest req, RestResponse res, long id, String tab, Order order) {
		var p = store().getPet(id);
		if (p == null) {
			// A View, not a thrown NotFound, so the not-found card renders inside the chrome.
			res.setStatus(404);
			return FreemarkerView.of("pet-not-found.ftlh").attr("message", f("Unknown pet '%s'", id));
		}
		// Node ids must start with a letter, so the pet node is p<id> and the order node o<id>.
		var nav = order == null ? f("pets/p%s/%s", id, tab) : f("pets/p%s/orders/o%s", id, order.getId());
		// The API's CSRF token, published by the console boundary on allowed requests; absent means the upload form renders without one.
		var servletReq = req.getHttpServletRequest();
		var apiCsrf = servletReq.getAttribute(PetstoreApiBoundary.TOKEN_ATTRIBUTE) instanceof String s ? s : null;
		if (builder(req))
			return twin(req, PetsPageBuilders.pet(p, tab, nav, order, ConsoleWrites.roles(req), apiCsrf));
		var v = FreemarkerView.of("pet.ftlh").attr("pet", p).attr("tab", tab).attr("activeNav", nav)
			.attr("pageTitle", p.getName()).attr("roles", ConsoleWrites.roles(req));
		if (apiCsrf != null)
			v = v.attr("apiCsrf", apiCsrf);
		return order == null ? v : v.attr("order", order);
	}

	private Order requireOrder(long id, long orderId) {
		var o = store().getOrder(orderId);
		if (o == null || o.getPetId() != id)
			throw new NotFound("Unknown order '%s'", orderId);
		return o;
	}

	/**
	 * @param id The pet id.
	 * @return The live pet.
	 * @throws NotFound If it does not exist.
	 */
	protected Pet require(long id) {
		var p = store().getPet(id);
		if (p == null)
			throw new NotFound("Unknown pet '%s'", id);
		return p;
	}

	/** Unwraps the dialog envelope ({@code fields}) if present; otherwise the body is the pet itself. */
	private static Pet toPet(JsonMap body) {
		var fields = body.getMap("fields");
		var src = fields != null ? fields : body;
		try {
			return Json.to(Json.of(src), Pet.class);
		} catch (ParseException e) {
			throw new BadRequest("Invalid pet: '%s'", e.getMessage());
		}
	}

	private static void validate(Pet p) {
		if (p.getName() == null || p.getName().isBlank())
			throw new BadRequest("Name must not be blank");
		if (Float.isNaN(p.getPrice()) || Float.isInfinite(p.getPrice()) || p.getPrice() < 0)
			throw new BadRequest("Price '%s' must be a non-negative number", p.getPrice());
	}
}
