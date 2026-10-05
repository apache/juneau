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

import java.io.*;
import java.nio.charset.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.commons.beanquery.*;
import org.apache.juneau.marshall.marshaller.*;
import org.apache.juneau.petstore.dto.*;

/**
 * In-memory {@link Pet}/{@link Order}/{@link User} store seeded from classpath JSON.
 *
 * <p>
 * Backed by {@link ConcurrentHashMap}s; pet and order IDs auto-assign via {@link AtomicLong} starting from
 * the highest seeded ID plus one (or {@code 1} if no seeds existed).  Seed files live on the classpath at
 * {@code petstore/init/{Pets,Orders,Users}.json} and are loaded lazily on first access.
 *
 * <p>
 * This is a sample/demo store — there is no persistence, no transactions, and no replication.  Restart wipes
 * state back to the seeded baseline (and clears the audit trail).
 *
 * <p>
 * Every successful create, update or delete is recorded in the audit trail; see {@link #getAudit()}.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@SuppressWarnings({
	"resource" // Returns an owned session; the caller closes it.
})
public class PetStore {

	private static final String ACTION_CREATE = "CREATE";
	private static final String ACTION_UPDATE = "UPDATE";
	private static final String ACTION_DELETE = "DELETE";
	private static final String ENTITY_ORDER = "Order";
	private static final String SEED_PETS = "petstore/init/Pets.json";
	private static final String SEED_ORDERS = "petstore/init/Orders.json";
	private static final String SEED_USERS = "petstore/init/Users.json";

	/** Actor recorded for mutations made through the {@code /petstore} API. */
	public static final String ACTOR_API = "api";

	/** Query context over pets; {@code tags} and {@code photo} are not searchable. */
	public static final InMemoryBeanQueryContext<Pet> PET_QUERY = InMemoryBeanQueryContext.create(Pet.class).exclude("tags", "photo").build();

	/** Query context over orders. */
	public static final InMemoryBeanQueryContext<Order> ORDER_QUERY = InMemoryBeanQueryContext.create(Order.class).build();

	/** Query context over users; {@code password} is never searchable or returned as a column. */
	public static final InMemoryBeanQueryContext<User> USER_QUERY = InMemoryBeanQueryContext.create(User.class).exclude("password").build();

	/** Query context over the audit trail. */
	public static final InMemoryBeanQueryContext<AuditEntry> AUDIT_QUERY = InMemoryBeanQueryContext.create(AuditEntry.class).build();

	/** Query context over staged changes. */
	public static final InMemoryBeanQueryContext<PendingChange> CHANGE_QUERY = InMemoryBeanQueryContext.create(PendingChange.class).build();

	private final Map<Long,Pet> pets = new ConcurrentHashMap<>();
	private final Map<Long,Order> orders = new ConcurrentHashMap<>();
	private final Map<String,User> users = new ConcurrentHashMap<>();

	private final AtomicLong nextPetId = new AtomicLong();
	private final AtomicLong nextOrderId = new AtomicLong();

	private final Clock clock;
	private final List<AuditEntry> audit = new CopyOnWriteArrayList<>();
	private final AtomicLong nextAuditId = new AtomicLong();
	private final PendingChanges pendingChanges = new PendingChanges(this);

	/**
	 * Constructor using the system UTC clock.
	 *
	 * <p>
	 * Eagerly loads the bundled classpath seed data.  Seeding writes no audit.
	 */
	public PetStore() {
		this(Clock.systemUTC());
	}

	/**
	 * Constructor.
	 *
	 * @param clock The clock used to timestamp audit entries.  Must not be <jk>null</jk>.
	 */
	public PetStore(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock");
		var seededPets = loadList(SEED_PETS, Pet.class);
		var maxPetId = 0L;
		for (var p : seededPets) {
			if (p.getId() == 0L)
				p.setId(++maxPetId);
			else
				maxPetId = Math.max(maxPetId, p.getId());
			pets.put(p.getId(), p);
		}
		nextPetId.set(maxPetId);

		var seededOrders = loadList(SEED_ORDERS, Order.class);
		var maxOrderId = 0L;
		for (var o : seededOrders) {
			if (o.getId() == 0L)
				o.setId(++maxOrderId);
			else
				maxOrderId = Math.max(maxOrderId, o.getId());
			orders.put(o.getId(), o);
		}
		nextOrderId.set(maxOrderId);

		var seededUsers = loadList(SEED_USERS, User.class);
		for (var u : seededUsers)
			users.put(u.getUsername(), u);
	}

	/** @return The clock this store timestamps with. */
	public Clock clock() { return clock; }

	private static <T> List<T> loadList(String resourcePath, Class<T> elementType) {
		var loader = PetStore.class.getClassLoader();
		try (var in = loader.getResourceAsStream(resourcePath)) {
			if (in == null)
				return l();
			try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return Json5.DEFAULT.read(reader, List.class, elementType);
			}
		} catch (Exception e) {
			throw brex(e, "Failed to load petstore seed resource '%s'", resourcePath);
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Pets
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all pets.
	 *
	 * @return All pets, in arbitrary order.  Never <jk>null</jk>.
	 */
	public Collection<Pet> getPets() {
		// Return an immutable snapshot so callers can't mutate the backing store through the returned collection.
		return List.copyOf(pets.values());
	}

	/**
	 * Returns a pet by ID.
	 *
	 * @param id The pet ID.
	 * @return The pet, or <jk>null</jk> if not found.
	 */
	public Pet getPet(long id) {
		return pets.get(id);
	}

	/**
	 * Creates a new pet.
	 *
	 * <p>
	 * Assigns the next available ID, ignoring any caller-supplied ID.
	 *
	 * @param pet The pet to create.  Must not be <jk>null</jk>.
	 * @return The created pet (same instance, with assigned ID).
	 */
	public Pet createPet(Pet pet) {
		return createPet(pet, ACTOR_API);
	}

	/**
	 * Creates a pet and records a {@code CREATE} audit entry.
	 *
	 * @param pet The pet.  Its ID is overwritten.  Must not be <jk>null</jk>.
	 * @param actor Who is creating it, e.g. {@code "console:alice"}.
	 * @return The stored pet.
	 */
	public Pet createPet(Pet pet, String actor) {
		if (pet == null)
			throw iaex("Pet must not be null");
		var id = nextPetId.incrementAndGet();
		pet.setId(id);
		pets.put(id, pet);
		recordAudit(actor, "Pet", String.valueOf(id), ACTION_CREATE, pet.getName());
		return pet;
	}

	/**
	 * Updates an existing pet.
	 *
	 * @param pet The pet to update.  Must not be <jk>null</jk>; must carry an ID matching an existing pet.
	 * @return The updated pet.
	 * @throws PetstoreNotFoundException If no pet with the given ID exists.
	 */
	public Pet updatePet(Pet pet) {
		return updatePet(pet, ACTOR_API);
	}

	/**
	 * Replaces a pet and records an {@code UPDATE} audit entry.
	 *
	 * @param pet The pet.  Must not be <jk>null</jk>.
	 * @param actor Who is updating it.
	 * @return The stored pet.
	 * @throws PetstoreNotFoundException If no pet with the given ID exists.
	 */
	public Pet updatePet(Pet pet, String actor) {
		if (pet == null)
			throw iaex("Pet must not be null");
		if (! pets.containsKey(pet.getId()))
			throw new PetstoreNotFoundException("Pet not found: id=" + pet.getId());
		pets.put(pet.getId(), pet);
		recordAudit(actor, "Pet", String.valueOf(pet.getId()), ACTION_UPDATE, pet.getName());
		return pet;
	}

	/**
	 * Deletes a pet by ID.
	 *
	 * @param id The pet ID.
	 * @throws PetstoreNotFoundException If no pet with the given ID exists.
	 */
	public void deletePet(long id) {
		deletePet(id, ACTOR_API);
	}

	/**
	 * Deletes a pet and records a {@code DELETE} audit entry.
	 *
	 * @param id The pet ID.
	 * @param actor Who is deleting it.
	 * @throws PetstoreNotFoundException If no pet with the given ID exists.
	 */
	public void deletePet(long id, String actor) {
		var removed = pets.remove(id);
		if (removed == null)
			throw new PetstoreNotFoundException("Pet not found: id=" + id);
		recordAudit(actor, "Pet", String.valueOf(id), ACTION_DELETE, removed.getName());
	}

	//------------------------------------------------------------------------------------------------------------------
	// Orders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all orders.
	 *
	 * @return All orders, in arbitrary order.  Never <jk>null</jk>.
	 */
	public Collection<Order> getOrders() {
		return List.copyOf(orders.values());
	}

	/**
	 * Returns an order by ID.
	 *
	 * @param id The order ID.
	 * @return The order, or <jk>null</jk> if not found.
	 */
	public Order getOrder(long id) {
		return orders.get(id);
	}

	/**
	 * Creates a new order.
	 *
	 * <p>
	 * Assigns the next available ID, ignoring any caller-supplied ID.
	 *
	 * @param order The order to create.  Must not be <jk>null</jk>.
	 * @return The created order (same instance, with assigned ID).
	 */
	public Order createOrder(Order order) {
		return createOrder(order, ACTOR_API);
	}

	/**
	 * Creates an order and records a {@code CREATE} audit entry.
	 *
	 * @param order The order.  Its ID is overwritten.  Must not be <jk>null</jk>.
	 * @param actor Who is creating it.
	 * @return The stored order.
	 */
	public Order createOrder(Order order, String actor) {
		if (order == null)
			throw iaex("Order must not be null");
		var id = nextOrderId.incrementAndGet();
		order.setId(id);
		orders.put(id, order);
		recordAudit(actor, ENTITY_ORDER, String.valueOf(id), ACTION_CREATE, String.valueOf(order.getStatus()));
		return order;
	}

	/**
	 * Updates an existing order.
	 *
	 * @param order The order to update.  Must not be <jk>null</jk>; must carry an ID matching an existing order.
	 * @return The updated order.
	 * @throws PetstoreNotFoundException If no order with the given ID exists.
	 */
	public Order updateOrder(Order order) {
		return updateOrder(order, ACTOR_API);
	}

	/**
	 * Replaces an order and records an {@code UPDATE} audit entry.
	 *
	 * @param order The order.  Must not be <jk>null</jk>.
	 * @param actor Who is updating it.
	 * @return The stored order.
	 * @throws PetstoreNotFoundException If no order with the given ID exists.
	 */
	public Order updateOrder(Order order, String actor) {
		if (order == null)
			throw iaex("Order must not be null");
		if (! orders.containsKey(order.getId()))
			throw new PetstoreNotFoundException("Order not found: id=" + order.getId());
		orders.put(order.getId(), order);
		recordAudit(actor, ENTITY_ORDER, String.valueOf(order.getId()), ACTION_UPDATE, String.valueOf(order.getStatus()));
		return order;
	}

	/**
	 * Deletes an order by ID.
	 *
	 * @param id The order ID.
	 * @throws PetstoreNotFoundException If no order with the given ID exists.
	 */
	public void deleteOrder(long id) {
		deleteOrder(id, ACTOR_API);
	}

	/**
	 * Deletes an order and records a {@code DELETE} audit entry.
	 *
	 * @param id The order ID.
	 * @param actor Who is deleting it.
	 * @throws PetstoreNotFoundException If no order with the given ID exists.
	 */
	public void deleteOrder(long id, String actor) {
		var removed = orders.remove(id);
		if (removed == null)
			throw new PetstoreNotFoundException("Order not found: id=" + id);
		recordAudit(actor, ENTITY_ORDER, String.valueOf(id), ACTION_DELETE, String.valueOf(removed.getStatus()));
	}

	//------------------------------------------------------------------------------------------------------------------
	// Users
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns all users.
	 *
	 * @return All users, in arbitrary order.  Never <jk>null</jk>.
	 */
	public Collection<User> getUsers() {
		return List.copyOf(users.values());
	}

	/**
	 * Returns a user by username.
	 *
	 * @param username The username (primary key).
	 * @return The user, or <jk>null</jk> if not found.
	 */
	public User getUser(String username) {
		return users.get(username);
	}

	/**
	 * Creates a new user.
	 *
	 * @param user The user to create.  Must not be <jk>null</jk>; must carry a non-null username not already in use.
	 * @return The created user.
	 * @throws IllegalArgumentException If the username is already in use.
	 */
	public User createUser(User user) {
		return createUser(user, ACTOR_API);
	}

	/**
	 * Creates a user and records a {@code CREATE} audit entry.
	 *
	 * @param user The user.  Must carry a non-null username not already in use.
	 * @param actor Who is creating it.
	 * @return The stored user.
	 * @throws IllegalArgumentException If the username is already in use.
	 */
	public User createUser(User user, String actor) {
		if (user == null)
			throw iaex("User must not be null");
		if (user.getUsername() == null)
			throw iaex("User username must not be null");
		if (users.putIfAbsent(user.getUsername(), user) != null)
			throw iaex("User already exists: username='%s'", user.getUsername());
		recordAudit(actor, "User", user.getUsername(), ACTION_CREATE, user.getUsername());
		return user;
	}

	/**
	 * Updates an existing user.
	 *
	 * @param user The user to update.  Must not be <jk>null</jk>; must carry a username matching an existing user.
	 * @return The updated user.
	 * @throws PetstoreNotFoundException If no user with the given username exists.
	 */
	public User updateUser(User user) {
		return updateUser(user, ACTOR_API);
	}

	/**
	 * Replaces a user and records an {@code UPDATE} audit entry.
	 *
	 * @param user The user.  Must carry a username matching an existing user.
	 * @param actor Who is updating it.
	 * @return The stored user.
	 * @throws PetstoreNotFoundException If no user with the given username exists.
	 */
	public User updateUser(User user, String actor) {
		if (user == null)
			throw iaex("User must not be null");
		if (user.getUsername() == null)
			throw iaex("User username must not be null");
		if (! users.containsKey(user.getUsername()))
			throw new PetstoreNotFoundException("User not found: username=" + user.getUsername());
		users.put(user.getUsername(), user);
		recordAudit(actor, "User", user.getUsername(), ACTION_UPDATE, user.getUsername());
		return user;
	}

	/**
	 * Deletes a user by username.
	 *
	 * @param username The username.
	 * @throws PetstoreNotFoundException If no user with the given username exists.
	 */
	public void deleteUser(String username) {
		deleteUser(username, ACTOR_API);
	}

	/**
	 * Deletes a user and records a {@code DELETE} audit entry.
	 *
	 * @param username The username.
	 * @param actor Who is deleting it.
	 * @throws PetstoreNotFoundException If no user with the given username exists.
	 */
	public void deleteUser(String username, String actor) {
		if (users.remove(username) == null)
			throw new PetstoreNotFoundException("User not found: username=" + username);
		recordAudit(actor, "User", username, ACTION_DELETE, username);
	}

	//------------------------------------------------------------------------------------------------------------------
	// Audit
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the audit trail.
	 *
	 * @return An immutable snapshot, oldest first.  Never <jk>null</jk>.
	 */
	public List<AuditEntry> getAudit() {
		return List.copyOf(audit);
	}

	/**
	 * Appends an audit entry stamped with this store's clock.
	 *
	 * <p>
	 * Public so console operations that are not plain CRUD (P6 restock, P10 apply/discard) are audited too.
	 *
	 * @param actor Who did it.
	 * @param entity The entity kind.
	 * @param entityId The entity id.
	 * @param action The action.
	 * @param detail A short detail, or <jk>null</jk>.
	 * @return The new entry.
	 */
	public AuditEntry recordAudit(String actor, String entity, String entityId, String action, String detail) {
		// Id assignment and append are one step so the trail stays in id order under concurrent writes.
		synchronized (audit) {
			var e = new AuditEntry().setId(nextAuditId.incrementAndGet()).setAt(clock.instant())
				.setActor(actor).setEntity(entity).setEntityId(entityId).setAction(action).setDetail(detail);
			audit.add(e);
			return e;
		}
	}

	/**
	 * Returns the staging area for console inline edits (P4 -> P10).
	 *
	 * @return The pending-changes staging area.  Never <jk>null</jk>.
	 */
	public PendingChanges pendingChanges() {
		return pendingChanges;
	}

	/**
	 * Bulk-loads rows with their ids preserved, writing no audit.  Used by {@code PetstoreSeed}.
	 *
	 * <p>
	 * Id counters advance past the highest loaded id, so later creates never collide.
	 *
	 * @param newPets Pets to add or replace.
	 * @param newOrders Orders to add or replace.
	 * @param newUsers Users to add or replace.
	 * @param newAudit Audit rows to append, in order.
	 */
	public void load(Collection<Pet> newPets, Collection<Order> newOrders, Collection<User> newUsers, Collection<AuditEntry> newAudit) {
		for (var p : newPets) {
			pets.put(p.getId(), p);
			nextPetId.accumulateAndGet(p.getId(), Math::max);
		}
		for (var o : newOrders) {
			orders.put(o.getId(), o);
			nextOrderId.accumulateAndGet(o.getId(), Math::max);
		}
		for (var u : newUsers)
			users.put(u.getUsername(), u);
		synchronized (audit) {
			for (var a : newAudit) {
				audit.add(a);
				nextAuditId.accumulateAndGet(a.getId(), Math::max);
			}
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// BeanQuery views (D-P5)
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Opens a query session over a snapshot of the pets.
	 *
	 * <h5 class='section'>Example:</h5>
	 * <p class='bjava'>
	 * 	<jk>try</jk> (<jk>var</jk> <jv>s</jv> = <jv>store</jv>.queryPets()) {
	 * 		<jk>return</jk> DataTablesQuery.<jsm>run</jsm>(<jv>req</jv>, <jv>s</jv>);
	 * 	}
	 * </p>
	 *
	 * @return A new session; close it.
	 */
	public InMemoryBeanQuerySession<Pet> queryPets() { return PET_QUERY.getSession(getPets()); }

	/** @return A new session over a snapshot of the orders; close it. */
	public InMemoryBeanQuerySession<Order> queryOrders() { return ORDER_QUERY.getSession(getOrders()); }

	/** @return A new session over a snapshot of the users; close it. */
	public InMemoryBeanQuerySession<User> queryUsers() { return USER_QUERY.getSession(getUsers()); }

	/** @return A new session over a snapshot of the audit trail; close it. */
	public InMemoryBeanQuerySession<AuditEntry> queryAudit() { return AUDIT_QUERY.getSession(getAudit()); }

	/** @return A new session over a snapshot of the staged changes; close it. */
	public InMemoryBeanQuerySession<PendingChange> queryPendingChanges() { return CHANGE_QUERY.getSession(pendingChanges.list()); }
}
