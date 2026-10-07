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
package org.apache.juneau.petstore.service;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

import org.apache.juneau.petstore.dto.*;

/**
 * The staging area for pet-detail inline edits: edits are staged here instead of mutating the pet, then applied
 * (perRow, with retry on failure) or discarded (aggregate) from the Pending changes page.
 *
 * <p>
 * Obtained from {@link PetStore#pendingChanges()}.  All methods are synchronized; the store is a demo, not a
 * transactional system.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	PendingChanges <jv>pc</jv> = <jv>store</jv>.pendingChanges();
 * 	PendingChange <jv>c</jv> = <jv>pc</jv>.stage(1, <js>"price"</js>, <js>"42.50"</js>, <js>"alice"</js>);
 * 	<jv>pc</jv>.stage(1, <js>"status"</js>, <js>"SOLD"</js>, <js>"alice"</js>);
 * 	<jv>pc</jv>.apply(<jv>c</jv>.getId(), <js>"console:alice"</js>);          <jc>// pet 1 now costs 42.50</jc>
 * 	PendingChanges.DiscardOutcome <jv>out</jv> = <jv>pc</jv>.discard(List.<jsm>of</jsm>(<js>"7"</js>, <js>"8"</js>), <js>"console:alice"</js>);
 * </p>
 */
public class PendingChanges {

	/**
	 * Result of an aggregate discard, mapped 1:1 onto the console's {@code BulkResult} by {@code ChangesRest}.
	 *
	 * @param succeeded Discarded change ids.
	 * @param notFound Ids that name no change (including unparseable ids).
	 * @param failed Ids that exist but could not be discarded, with the reason.
	 */
	public record DiscardOutcome(List<String> succeeded, List<String> notFound, List<Failure> failed) {}

	/**
	 * One failed id in a {@link DiscardOutcome}.
	 *
	 * @param id The change id.
	 * @param message Why it failed.
	 */
	public record Failure(String id, String message) {}

	private static final String PRICE_MSG = "Price '%s' must be a non-negative number";

	/** Stageable field names, in the order reported by the "cannot be staged" message. */
	private static final List<String> FIELDS = List.of("name", "price", "species", "status", "tags");

	/** Per-field old-value rendering, validation and write. */
	private enum StageField {
		NAME("name", Pet::getName, PendingChanges::validateName, Pet::setName),
		PRICE("price", p -> String.valueOf(p.getPrice()), PendingChanges::validatePrice, (p, v) -> p.setPrice(Float.parseFloat(v))),
		SPECIES("species", p -> p.getSpecies() == null ? "" : p.getSpecies().name(), PendingChanges::validateSpecies, (p, v) -> p.setSpecies(Species.valueOf(v))),
		STATUS("status", p -> p.getStatus() == null ? "" : p.getStatus().name(), PendingChanges::validateStatus, (p, v) -> p.setStatus(PetStatus.valueOf(v))),
		TAGS("tags", p -> p.getTags() == null ? "" : String.join(",", p.getTags()), PendingChanges::parseTags, (p, v) -> p.setTags(parseTags(v)));

		final String fieldName;
		final Function<Pet,String> oldValue;
		final Consumer<String> validator;
		final BiConsumer<Pet,String> writer;

		StageField(String fieldName, Function<Pet,String> oldValue, Consumer<String> validator, BiConsumer<Pet,String> writer) {
			this.fieldName = fieldName;
			this.oldValue = oldValue;
			this.validator = validator;
			this.writer = writer;
		}

		static StageField of(String fieldName) {
			for (var f : values())
				if (f.fieldName.equals(fieldName))
					return f;
			return null;
		}
	}

	private final PetStore store;
	private final Map<Long,PendingChange> changes = new LinkedHashMap<>();
	private final AtomicLong nextId = new AtomicLong();

	PendingChanges(PetStore store) {
		this.store = store;
	}

	/**
	 * Stages an edit without touching the pet.
	 *
	 * @param petId The pet id.
	 * @param field One of {@code "name"}, {@code "price"}, {@code "species"}, {@code "status"} or {@code "tags"}
	 * (tags is a comma-separated list; blank clears).
	 * @param newValue The new value (string form).
	 * @param author Who staged it.
	 * @return The new pending change.
	 * @throws PetstoreNotFoundException If the pet does not exist.
	 * @throws IllegalArgumentException If the field is not stageable or the value is invalid.
	 */
	public synchronized PendingChange stage(long petId, String field, String newValue, String author) {
		var pet = store.getPet(petId);
		if (pet == null)
			throw new PetstoreNotFoundException(f("Unknown pet '%s'", petId));
		var sf = StageField.of(field);
		if (sf == null)
			throw iaex("Field '%s' cannot be staged; use one of: %s", field, String.join(", ", FIELDS));
		sf.validator.accept(newValue);
		var old = sf.oldValue.apply(pet);
		var c = new PendingChange().setId(nextId.incrementAndGet()).setPetId(petId).setField(field)
			.setOldValue(old).setNewValue(newValue).setAuthor(author)
			.setSummary(f("%s: %s '%s' -> '%s'", pet.getName(), field, old, newValue));
		changes.put(c.getId(), c);
		return c;
	}

	/** @return All changes, staging order.  Immutable snapshot. */
	public synchronized List<PendingChange> list() {
		return List.copyOf(changes.values());
	}

	/** @return Only {@link PendingChange.State#PENDING} changes, staging order.  Immutable snapshot. */
	public synchronized List<PendingChange> pending() {
		return changes.values().stream().filter(x -> x.getState() == PendingChange.State.PENDING).toList();
	}

	/**
	 * Returns one change.
	 *
	 * @param id The change id.
	 * @return The change.
	 * @throws PetstoreNotFoundException If it does not exist.
	 */
	public synchronized PendingChange get(long id) {
		var c = changes.get(id);
		if (c == null)
			throw new PetstoreNotFoundException(f("Unknown change '%s'", id));
		return c;
	}

	/**
	 * Writes a pending change to its pet.  If the pet is gone, the change becomes {@code FAILED} instead.
	 *
	 * @param id The change id.
	 * @param actor The audit actor.
	 * @return The change, now {@code APPLIED} or {@code FAILED}.
	 * @throws PetstoreNotFoundException If the change does not exist.
	 * @throws PetstoreConflictException If the change is not pending.
	 */
	public synchronized PendingChange apply(long id, String actor) {
		var c = get(id);
		if (c.getState() != PendingChange.State.PENDING)
			throw new PetstoreConflictException("Change '%s' is not pending", id);
		return write(c, actor);
	}

	/**
	 * Re-applies a failed change.
	 *
	 * @param id The change id.
	 * @param actor The audit actor.
	 * @return The change, now {@code APPLIED} or still {@code FAILED}.
	 * @throws PetstoreNotFoundException If the change does not exist.
	 * @throws PetstoreConflictException If the change has not failed.
	 */
	public synchronized PendingChange retry(long id, String actor) {
		var c = get(id);
		if (c.getState() != PendingChange.State.FAILED)
			throw new PetstoreConflictException("Change '%s' has not failed", id);
		return write(c, actor);
	}

	/**
	 * Discards pending changes in one aggregate call.
	 *
	 * @param ids The change ids (string form, as sent by the bulk action).
	 * @param actor The audit actor.
	 * @return The per-id outcome.
	 */
	public synchronized DiscardOutcome discard(Collection<String> ids, String actor) {
		var ok = new ArrayList<String>();
		var missing = new ArrayList<String>();
		var failed = new ArrayList<Failure>();
		for (var sid : ids) {
			Long id = parse(sid);
			var c = id == null ? null : changes.get(id);
			if (c == null)
				missing.add(sid);
			else if (c.getState() != PendingChange.State.PENDING)
				failed.add(new Failure(sid, f("Change '%s' is not pending", sid)));
			else {
				changes.remove(id);
				store.recordAudit(actor, "Change", sid, "DISCARD", c.getSummary());
				ok.add(sid);
			}
		}
		return new DiscardOutcome(List.copyOf(ok), List.copyOf(missing), List.copyOf(failed));
	}

	private PendingChange write(PendingChange c, String actor) {
		var pet = store.getPet(c.getPetId());
		if (pet == null)
			return c.setState(PendingChange.State.FAILED).setMessage(f("Unknown pet '%s'", c.getPetId()));
		StageField.of(c.getField()).writer.accept(pet, c.getNewValue());
		store.updatePet(pet, actor);
		return c.setState(PendingChange.State.APPLIED).setMessage(null);
	}

	private static void validateName(String value) {
		if (value == null || value.isBlank())
			throw iaex("Name must not be blank");
	}

	private static void validatePrice(String value) {
		if (value == null)
			throw iaex(PRICE_MSG, value);
		float p;
		try {
			p = Float.parseFloat(value);
		} catch (NumberFormatException e) {
			throw iaex(PRICE_MSG, value);
		}
		if (p < 0 || Float.isNaN(p) || Float.isInfinite(p))
			throw iaex(PRICE_MSG, value);
	}

	private static void validateSpecies(String value) {
		for (var x : Species.values())
			if (x.name().equals(value))
				return;
		throw iaex("Species '%s' must be one of: %s", value, enumNames(Species.values()));
	}

	private static void validateStatus(String value) {
		for (var x : PetStatus.values())
			if (x.name().equals(value))
				return;
		throw iaex("Status '%s' must be one of: %s", value, enumNames(PetStatus.values()));
	}

	private static String enumNames(Enum<?>[] values) {
		return String.join(", ", Arrays.stream(values).map(Enum::name).toList());
	}

	/** Parses a comma-separated tag list; blank means empty; validates as it parses. */
	private static List<String> parseTags(String value) {
		if (value == null)
			throw iaex("Tags must not be null");
		if (value.isBlank())
			return new ArrayList<>();
		var out = new ArrayList<String>();
		for (var t : value.split(",", -1)) {
			var x = t.trim();
			if (x.isEmpty())
				throw iaex("Tags '%s' must not contain empty entries", value);
			out.add(x);
		}
		return out;
	}

	private static Long parse(String s) {
		try {
			return Long.valueOf(s);
		} catch (NumberFormatException e) {
			return null;
		}
	}
}
