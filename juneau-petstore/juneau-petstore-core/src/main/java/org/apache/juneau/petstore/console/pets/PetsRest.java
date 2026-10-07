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

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.collections.*;
import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.console.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.datatables.*;
import org.apache.juneau.rest.server.datatables.adapter.*;
import org.apache.juneau.rest.server.view.*;
import org.apache.juneau.rest.server.views.*;

/**
 * The pets JSON endpoints (server-mode datatable query over BeanQuery, create/replace/delete, sell, stage, and the
 * per-pet feeds).  The P2 Pets, P3 Sold and P10 Pending changes pages are still "Coming soon" placeholders
 * ({@link ConsoleStubs}).
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
 * 	{ dataMode:'server', dataUrl:'/console/pets/query', columns:[{data:'name', title:'Name'}, ...] }
 * 	&lt;/@card&gt;
 * </p>
 * <p class='bjava'>
 * 	<jc>// The same table in the C3 builder twin.</jc>
 * 	TableSpec.<jsm>create</jsm>(<js>"pets"</js>).dataMode(DataMode.<jsf>SERVER</jsf>).dataUrl(<js>"/console/pets/query"</js>)
 * 		.columns(Column.<jsm>create</jsm>(<js>"name"</js>).label(<js>"Name"</js>));
 * </p>
 */
@Rest(path="/pets", title="Pets")
public class PetsRest extends PetstoreConsolePage {

	private static final long serialVersionUID = 1L;

	/** @return The Pets placeholder (see {@link ConsoleStubs}). */
	@RestGet(path="/")
	public View page() { return ConsoleStubs.view("pets"); }

	/** @return The Sold placeholder (see {@link ConsoleStubs}). */
	@RestGet(path="/sold")
	public View sold() { return ConsoleStubs.view("pets/sold"); }

	/** @return The Pending changes placeholder (see {@link ConsoleStubs}). */
	@RestGet(path="/changes")
	public View changes() { return ConsoleStubs.view("pets/changes"); }

	/**
	 * Server-mode DataTables query over all pets.
	 *
	 * @param req The DataTables request.
	 * @return One page of pets, or a 200 {@code error} envelope for a malformed search expression.
	 */
	@RestPost(path="/query")
	public DataTablesResults<Pet> query(@Content DataTablesRequest req) {
		try (var s = store().queryPets()) {
			return DataTablesQuery.run(req, s);
		} catch (BeanQuerySyntaxException e) {
			// DataTablesQuery maps a bad expression to HTTP 400 by default; the table wants a 200 error envelope it can show.
			return DataTablesResults.error(req.getDraw(), e.getMessage());
		}
	}

	/**
	 * Creates a pet.  Status defaults to {@code AVAILABLE}.
	 *
	 * @param req The request (write gate, audit actor).
	 * @param pet The new pet.
	 * @return The result toast, carrying the stored pet.
	 * @throws BadRequest On a blank name or negative price.
	 */
	@RestPost(path="/")
	public ActionResult create(RestRequest req, @Content Pet pet) {
		var actor = ConsoleWrites.actor(req);
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
	 * @param pet The new state.
	 * @return The result toast.
	 * @throws NotFound If the pet does not exist.
	 */
	@RestPut(path="/{id}")
	public ActionResult update(RestRequest req, @Path("id") long id, @Content Pet pet) {
		var actor = ConsoleWrites.actor(req);
		var old = require(id);
		validate(pet);
		pet.setId(id);
		if (pet.getStatus() == null)  // Let's use defaultX() methods here like we use on DAOs in IRS.
			pet.setStatus(old.getStatus());
		if (pet.getSpecies() == null)
			pet.setSpecies(old.getSpecies());
		if (pet.getTags() == null)
			pet.setTags(old.getTags());
		if (pet.getPhoto() == null)
			pet.setPhoto(old.getPhoto());
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
	 * Marks a pet sold.  The P2 bulk "Mark sold (N)" action calls this per row.
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
	 * Stages an inline edit (R11) for review on P10.  The pet itself is not changed until the change is applied.
	 *
	 * <p>
	 * {@link PendingChanges#stage} owns which fields are stageable and its messages.  The photo is deliberately not
	 * stageable: the P4 Photo tab edits it directly through the API's own {@code PUT /petstore/pets/{id}/photo}.
	 *
	 * @param req The request.
	 * @param id The pet id.
	 * @param body {@code {field, value}}.
	 * @return {@code success} carrying the new {@link PendingChange}.
	 * @throws BadRequest If the field is not stageable or the value is invalid.
	 * @throws NotFound If the pet does not exist.
	 */
	@RestPost(path="/{id}/stage")
	public ActionResult stage(RestRequest req, @Path("id") long id, @Content JsonMap body) {
		var actor = ConsoleWrites.actor(req);
		try {
			var c = store().pendingChanges().stage(id, body.getString("field"), body.getString("value"), ConsoleWrites.author(actor));
			return ActionResult.success(c).message(f("Staged '%s'", c.getSummary()));
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		} catch (IllegalArgumentException e) {
			throw new BadRequest(e.getMessage());
		}
	}

	/**
	 * @param id The pet id.
	 * @return This pet's orders (the R12 related list), by id.
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

	private static void validate(Pet p) {
		if (p.getName() == null || p.getName().isBlank())
			throw new BadRequest("Name must not be blank");
		if (Float.isNaN(p.getPrice()) || Float.isInfinite(p.getPrice()) || p.getPrice() < 0)
			throw new BadRequest("Price '%s' must be a non-negative number", p.getPrice());
	}
}
