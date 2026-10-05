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
import static org.apache.juneau.test.bct.BctAssertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.*;
import java.util.*;

import org.apache.juneau.*;
import org.junit.jupiter.api.*;

class PendingChanges_Test extends TestBase {

	private static PetStore store() {
		return new PetStore(Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC));
	}

	//------------------------------------------------------------------------------------------------------------------
	// a — staging
	//------------------------------------------------------------------------------------------------------------------

	@Test void a01_stage_recordsOldAndNewWithoutMutatingPet() {
		var s = store();
		var c = s.pendingChanges().stage(1, "name", "Sir Frisky", "alice");
		assertBean(c, "id,petId,field,oldValue,newValue,state,author,summary",
			"1,1,name,Mr. Frisky,Sir Frisky,PENDING,alice,Mr. Frisky: name 'Mr. Frisky' -> 'Sir Frisky'");
		assertBean(s.getPet(1), "name", "Mr. Frisky");
	}

	@Test void a02_stage_price() {
		var c = store().pendingChanges().stage(1, "price", "42.5", "alice");
		assertBean(c, "field,oldValue,newValue", "price,39.99,42.5");
	}

	@Test void a03_stage_unknownPet_404() {
		var pc = store().pendingChanges();
		var e = assertThrows(PetstoreNotFoundException.class, () -> pc.stage(99_999, "name", "x", "a"));
		assertString("Unknown pet '99999'", e.getMessage());
	}

	@Test void a04_stage_badField_400() {
		var pc = store().pendingChanges();
		var e = assertThrows(IllegalArgumentException.class, () -> pc.stage(1, "id", "5", "a"));
		assertString("Field 'id' cannot be staged; use one of: name, price, species, status, tags", e.getMessage());
	}

	@Test void a05_stage_badPrice_400() {
		var pc = store().pendingChanges();
		var e = assertThrows(IllegalArgumentException.class, () -> pc.stage(1, "price", "-1", "a"));
		assertString("Price '-1' must be a non-negative number", e.getMessage());
	}

	@Test void a06_pendingListsOnlyPending() {
		var s = store();
		var pc = s.pendingChanges();
		pc.stage(1, "name", "A", "alice");
		var c2 = pc.stage(2, "name", "B", "bob");
		pc.apply(c2.getId(), "console:bob");
		assertBeans(pc.pending(), "petId", "1");
		assertSize(2, pc.list());
	}

	private static final String FIELD_MSG = "Field '%s' cannot be staged; use one of: name, price, species, status, tags";

	private static String badArg(String field, String value) {
		var pc = store().pendingChanges();
		return assertThrows(IllegalArgumentException.class, () -> pc.stage(1, field, value, "a")).getMessage();
	}

	@Test void a07_stage_species() {
		var c = store().pendingChanges().stage(1, "species", "DOG", "alice");
		assertBean(c, "field,oldValue,newValue", "species,CAT,DOG");
	}

	@Test void a08_stage_status() {
		var c = store().pendingChanges().stage(1, "status", "SOLD", "alice");
		assertBean(c, "field,oldValue,newValue", "status,AVAILABLE,SOLD");
	}

	@Test void a09_stage_tags() {
		var c = store().pendingChanges().stage(1, "tags", "a,b", "alice");
		assertBean(c, "field,oldValue", "tags,friendly");
		assertString("a,b", c.getNewValue());
	}

	@Test void a10_apply_species() {
		var s = store();
		s.pendingChanges().apply(s.pendingChanges().stage(1, "species", "DOG", "alice").getId(), "a");
		assertBean(s.getPet(1), "species", "DOG");
	}

	@Test void a11_apply_status() {
		var s = store();
		s.pendingChanges().apply(s.pendingChanges().stage(1, "status", "SOLD", "alice").getId(), "a");
		assertBean(s.getPet(1), "status", "SOLD");
	}

	@Test void a12_apply_tags_trimmed() {
		var s = store();
		s.pendingChanges().apply(s.pendingChanges().stage(1, "tags", " a , b ", "alice").getId(), "a");
		assertList(s.getPet(1).getTags(), "a", "b");
	}

	@Test void a13_apply_blankTags_clears() {
		var s = store();
		var c = s.pendingChanges().stage(1, "tags", "  ", "alice");
		assertBean(c, "oldValue", "friendly");
		s.pendingChanges().apply(c.getId(), "a");
		assertEmpty(s.getPet(1).getTags());
	}

	@Test void a14_apply_price() {
		var s = store();
		s.pendingChanges().apply(s.pendingChanges().stage(1, "price", "42.5", "alice").getId(), "a");
		assertBean(s.getPet(1), "price", "42.5");
	}

	@Test void a15_stage_photo_rejected() {
		assertString(FIELD_MSG.formatted("photo"), badArg("photo", "/x"));
	}

	@Test void a16_stage_nullField_rejected() {
		assertString(FIELD_MSG.formatted((String)null), badArg(null, "x"));
	}

	@Test void a17_stage_badSpecies() {
		assertString("Species 'LION' must be one of: BIRD, CAT, DOG, FISH, MOUSE, RABBIT, SNAKE", badArg("species", "LION"));
	}

	@Test void a18_stage_badStatus() {
		assertString("Status 'GONE' must be one of: AVAILABLE, PENDING, SOLD", badArg("status", "GONE"));
	}

	@Test void a19_stage_lowercaseEnum_rejected() {
		assertString("Species 'dog' must be one of: BIRD, CAT, DOG, FISH, MOUSE, RABBIT, SNAKE", badArg("species", "dog"));
		assertString("Status 'sold' must be one of: AVAILABLE, PENDING, SOLD", badArg("status", "sold"));
	}

	@Test void a20_stage_nonFinitePrice_rejected() {
		assertString("Price 'NaN' must be a non-negative number", badArg("price", "NaN"));
		assertString("Price 'Infinity' must be a non-negative number", badArg("price", "Infinity"));
		assertString("Price 'abc' must be a non-negative number", badArg("price", "abc"));
	}

	@Test void a21_stage_blankName_rejected() {
		assertString("Name must not be blank", badArg("name", "  "));
		assertString("Name must not be blank", badArg("name", null));
	}

	@Test void a22_stage_tagsWithEmptyEntry_rejected() {
		assertString("Tags 'a,,b' must not contain empty entries", badArg("tags", "a,,b"));
	}

	@Test void a23_stage_nullTags_rejected() {
		assertString("Tags must not be null", badArg("tags", null));
	}

	//------------------------------------------------------------------------------------------------------------------
	// b — apply / retry
	//------------------------------------------------------------------------------------------------------------------

	@Test void b01_apply_updatesPetAndAudits() {
		var s = store();
		var c = s.pendingChanges().stage(1, "name", "Sir Frisky", "alice");
		var r = s.pendingChanges().apply(c.getId(), "console:alice");
		assertBean(r, "state", "APPLIED");
		assertBean(s.getPet(1), "name", "Sir Frisky");
		assertBeans(s.getAudit(), "actor,entity,entityId,action", "console:alice,Pet,1,UPDATE");
	}

	@Test void b02_apply_notPending_409() {
		var s = store();
		var c = s.pendingChanges().stage(1, "name", "X", "alice");
		s.pendingChanges().apply(c.getId(), "a");
		var pc = s.pendingChanges();
		var cid = c.getId();
		var e = assertThrows(PetstoreConflictException.class, () -> pc.apply(cid, "a"));
		assertString("Change '1' is not pending", e.getMessage());
	}

	@Test void b03_apply_unknownChange_404() {
		var pc = store().pendingChanges();
		var e = assertThrows(PetstoreNotFoundException.class, () -> pc.apply(77, "a"));
		assertString("Unknown change '77'", e.getMessage());
	}

	@Test void b04_apply_petGone_marksFailed() {
		var s = store();
		var c = s.pendingChanges().stage(2, "name", "X", "alice");
		s.deletePet(2);
		var r = s.pendingChanges().apply(c.getId(), "a");
		assertBean(r, "state,message", "FAILED,Unknown pet '2'");
	}

	@Test void b05_retry_afterFailure() {
		var s = store();
		var c = s.pendingChanges().stage(2, "name", "Back", "alice");
		var removed = s.getPet(2);
		s.deletePet(2);
		s.pendingChanges().apply(c.getId(), "a");
		s.load(List.of(removed), List.of(), List.of(), List.of());
		var r = s.pendingChanges().retry(c.getId(), "a");
		assertBean(r, "state,message", "APPLIED,<null>");
		assertBean(s.getPet(2), "name", "Back");
	}

	@Test void b06_retry_notFailed_409() {
		var s = store();
		var c = s.pendingChanges().stage(1, "name", "X", "alice");
		var pc = s.pendingChanges();
		var cid = c.getId();
		var e = assertThrows(PetstoreConflictException.class, () -> pc.retry(cid, "a"));
		assertString("Change '1' has not failed", e.getMessage());
	}

	//------------------------------------------------------------------------------------------------------------------
	// c — aggregate discard
	//------------------------------------------------------------------------------------------------------------------

	@Test void c01_discard_mixedOutcome() {
		var s = store();
		var pc = s.pendingChanges();
		var a = pc.stage(1, "name", "A", "alice");
		var b = pc.stage(3, "name", "B", "alice");
		pc.apply(b.getId(), "x");
		var out = pc.discard(List.of(String.valueOf(a.getId()), String.valueOf(b.getId()), "999", "junk"), "console:alice");
		assertList(out.succeeded(), "1");
		assertList(out.notFound(), "999", "junk");
		assertBeans(out.failed(), "id,message", "2,Change '2' is not pending");
		assertEmpty(pc.pending());
		assertBeans(s.getAudit().stream().filter(x -> eq(x.getEntity(), "Change")).toList(), "entityId,action", "1,DISCARD");
	}

	@Test void c02_discard_removesFromList() {
		var pc = store().pendingChanges();
		var a = pc.stage(1, "name", "A", "alice");
		pc.discard(List.of(String.valueOf(a.getId())), "x");
		assertEmpty(pc.list());
	}
}
