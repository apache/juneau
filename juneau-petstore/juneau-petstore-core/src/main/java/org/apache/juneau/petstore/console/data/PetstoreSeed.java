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
package org.apache.juneau.petstore.console.data;

import static org.apache.juneau.commons.utils.Shorts.*;

import java.time.*;
import java.time.format.*;
import java.util.*;

import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;

/**
 * Deterministic demo data for the petstore console: about 500 pets, 2,000 orders, 50 users and 300 audit rows,
 * generated from a fixed {@link Random} seed and a fixed {@link Clock} so tests and screenshots are repeatable.
 *
 * <p>
 * The classic rows a {@code new PetStore()} loads (pets 1-9, orders 101-103, three users) stay first with their ids;
 * generated rows follow.  Both runners seed this way on startup.
 *
 * <h5 class='section'>Example:</h5>
 * <p class='bjava'>
 * 	<jc>// Default seed and clock (what the runners use).</jc>
 * 	PetStore <jv>store</jv> = PetstoreSeed.<jsm>create</jsm>().populate(<jk>new</jk> PetStore());
 *
 * 	<jc>// A different but still repeatable data set.</jc>
 * 	PetStore <jv>other</jv> = PetstoreSeed.<jsm>create</jsm>()
 * 		.randomSeed(42L)
 * 		.clock(Clock.<jsm>fixed</jsm>(Instant.<jsm>parse</jsm>(<js>"2027-01-01T00:00:00Z"</js>), ZoneOffset.<jsf>UTC</jsf>))
 * 		.populate(<jk>new</jk> PetStore());
 * </p>
 */
public final class PetstoreSeed {

	/** Default random seed. */
	public static final long DEFAULT_SEED = 20261004L;

	/** Default clock: fixed at 2026-10-01T12:00:00Z, UTC. */
	public static final Clock DEFAULT_CLOCK = Clock.fixed(Instant.parse("2026-10-01T12:00:00Z"), ZoneOffset.UTC);

	/** Total pets after {@link #populate(PetStore)}. */
	public static final int PETS = 500;
	/** Total orders after {@link #populate(PetStore)}. */
	public static final int ORDERS = 2000;
	/** Total users after {@link #populate(PetStore)}. */
	public static final int USERS = 50;
	/** Audit rows written by {@link #populate(PetStore)}. */
	public static final int AUDIT = 300;

	private static final String[] NAMES = {
		"Biscuit", "Pepper", "Mango", "Nori", "Juniper", "Clover", "Pickle", "Waffles", "Sprout", "Maple",
		"Pebble", "Olive", "Tofu", "Ziggy", "Hazel", "Basil", "Comet", "Dot", "Fig", "Rusty"
	};
	private static final String[] FIRST = { "Ana", "Ben", "Cleo", "Dev", "Eli", "Fay", "Gus", "Hana", "Ivo", "Jo" };
	private static final String[] LAST = { "Abbott", "Brooks", "Chen", "Diaz", "Evans", "Fischer", "Gray", "Hale" };
	private static final String USER_FMT = "user%02d";
	private static final String[] ENTITIES = { "Pet", "Order", "User" };
	private static final String[] ACTIONS = { "CREATE", "UPDATE", "DELETE" };

	private long seed = DEFAULT_SEED;
	private Clock clock = DEFAULT_CLOCK;

	private PetstoreSeed() {}

	/** @return A new seed builder with the default seed and clock. */
	public static PetstoreSeed create() {
		return new PetstoreSeed();
	}

	/**
	 * @param value The {@link Random} seed.
	 * @return This object.
	 */
	public PetstoreSeed randomSeed(long value) {
		seed = value;
		return this;
	}

	/**
	 * @param value The clock that dates orders and audit rows.  Must not be <jk>null</jk>.
	 * @return This object.
	 */
	public PetstoreSeed clock(Clock value) {
		clock = Objects.requireNonNull(value, "value");
		return this;
	}

	/**
	 * Adds the generated rows to a store that holds only the classic rows.
	 *
	 * @param store A freshly constructed store.
	 * @return The same store, for chaining.
	 * @throws IllegalArgumentException If the store already holds more than the classic rows.
	 */
	@SuppressWarnings({
		"java:S2245" // Demo seed data only; a fixed-seed Random is required for repeatability and is not security-sensitive.
	})
	public PetStore populate(PetStore store) {
		var existing = store.getPets().size();
		if (existing > 9)
			throw iaex("PetstoreSeed.populate expects a store holding only the classic rows; found '%s' pets", existing);
		var r = new Random(seed);
		var now = clock.instant();
		var species = Species.values();
		var orderStatus = OrderStatus.values();

		var pets = new ArrayList<Pet>();
		for (long id = 10; id <= PETS; id++) {
			var sp = species[(int)(id % species.length)];
			var roll = r.nextInt(10);
			var status = petStatus(roll);
			pets.add(new Pet().setId(id).setSpecies(sp).setStatus(status)
				.setName(NAMES[r.nextInt(NAMES.length)] + " " + id)
				.setPrice(5 + r.nextInt(9_500) / 100f)
				.setTags(List.of(sp.name().toLowerCase(Locale.ROOT))));
		}

		var users = new ArrayList<User>();
		for (var n = 4; n <= USERS; n++) {
			var username = f(USER_FMT, n);
			users.add(new User().setUsername(username)
				.setFirstName(FIRST[r.nextInt(FIRST.length)]).setLastName(LAST[r.nextInt(LAST.length)])
				.setEmail(username + "@example.org").setPassword("demo").setPhone(f("555-01%02d", n))
				.setUserStatus(r.nextInt(10) == 0 ? UserStatus.INACTIVE : UserStatus.ACTIVE));
		}

		var day = DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC);
		var orders = new ArrayList<Order>();
		for (long id = 104; id <= 2100; id++)
			orders.add(new Order().setId(id).setPetId(1L + r.nextInt(PETS))
				.setShipDate(day.format(now.minus(Duration.ofDays(r.nextInt(90)))))
				.setStatus(orderStatus[r.nextInt(orderStatus.length)]));

		var stamps = new ArrayList<Instant>();
		for (var i = 0; i < AUDIT; i++)
			stamps.add(now.minus(Duration.ofMinutes(1L + r.nextInt(90 * 24 * 60))));
		stamps.sort(Comparator.naturalOrder());
		var audit = new ArrayList<AuditEntry>();
		for (var i = 0; i < AUDIT; i++) {
			var kind = r.nextInt(3);
			var entity = ENTITIES[kind];
			var entityId = switch (kind) {
				case 0 -> String.valueOf(1 + r.nextInt(PETS));
				case 1 -> String.valueOf(101 + r.nextInt(ORDERS));
				default -> f(USER_FMT, 4 + r.nextInt(USERS - 3));
			};
			var actor = r.nextInt(4) == 0 ? PetStore.ACTOR_API : "console:" + f(USER_FMT, 4 + r.nextInt(USERS - 3));
			audit.add(new AuditEntry().setId(i + 1L).setAt(stamps.get(i)).setActor(actor)
				.setEntity(entity).setEntityId(entityId).setAction(ACTIONS[r.nextInt(ACTIONS.length)]));
		}

		store.load(pets, orders, users, audit);
		return store;
	}

	private static PetStatus petStatus(int roll) {
		if (roll < 6)
			return PetStatus.AVAILABLE;
		return roll < 8 ? PetStatus.PENDING : PetStatus.SOLD;
	}
}
