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

import static org.apache.juneau.commons.utils.ThrowableUtils.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import org.apache.juneau.marshall.json5.*;
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
 * state back to the seeded baseline.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstoreOverview">juneau-petstore</a>
 * </ul>
 */
public class PetStore {

	private static final String SEED_PETS = "petstore/init/Pets.json";
	private static final String SEED_ORDERS = "petstore/init/Orders.json";
	private static final String SEED_USERS = "petstore/init/Users.json";

	private final Map<Long,Pet> pets = new ConcurrentHashMap<>();
	private final Map<Long,Order> orders = new ConcurrentHashMap<>();
	private final Map<String,User> users = new ConcurrentHashMap<>();

	private final AtomicLong nextPetId = new AtomicLong();
	private final AtomicLong nextOrderId = new AtomicLong();

	/**
	 * Constructor.
	 *
	 * <p>
	 * Eagerly loads the bundled classpath seed data.
	 */
	public PetStore() {
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

	private static <T> List<T> loadList(String resourcePath, Class<T> elementType) {
		var loader = PetStore.class.getClassLoader();
		try (var in = loader.getResourceAsStream(resourcePath)) {
			if (in == null)
				return new ArrayList<>();
			try (var reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return Json5Parser.DEFAULT.parse(reader, List.class, elementType);
			}
		} catch (Exception e) {
			throw bex(e, "Failed to load petstore seed resource ''{0}''", resourcePath);
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
		return pets.values();
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
		if (pet == null)
			throw illegalArg("Pet must not be null");
		var id = nextPetId.incrementAndGet();
		pet.setId(id);
		pets.put(id, pet);
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
		if (pet == null)
			throw illegalArg("Pet must not be null");
		if (! pets.containsKey(pet.getId()))
			throw new PetstoreNotFoundException("Pet not found: id=" + pet.getId());
		pets.put(pet.getId(), pet);
		return pet;
	}

	/**
	 * Deletes a pet by ID.
	 *
	 * @param id The pet ID.
	 * @throws PetstoreNotFoundException If no pet with the given ID exists.
	 */
	public void deletePet(long id) {
		if (pets.remove(id) == null)
			throw new PetstoreNotFoundException("Pet not found: id=" + id);
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
		return orders.values();
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
		if (order == null)
			throw illegalArg("Order must not be null");
		var id = nextOrderId.incrementAndGet();
		order.setId(id);
		orders.put(id, order);
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
		if (order == null)
			throw illegalArg("Order must not be null");
		if (! orders.containsKey(order.getId()))
			throw new PetstoreNotFoundException("Order not found: id=" + order.getId());
		orders.put(order.getId(), order);
		return order;
	}

	/**
	 * Deletes an order by ID.
	 *
	 * @param id The order ID.
	 * @throws PetstoreNotFoundException If no order with the given ID exists.
	 */
	public void deleteOrder(long id) {
		if (orders.remove(id) == null)
			throw new PetstoreNotFoundException("Order not found: id=" + id);
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
		return users.values();
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
		if (user == null)
			throw illegalArg("User must not be null");
		if (user.getUsername() == null)
			throw illegalArg("User username must not be null");
		if (users.putIfAbsent(user.getUsername(), user) != null)
			throw illegalArg("User already exists: username=''{0}''", user.getUsername());
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
		if (user == null)
			throw illegalArg("User must not be null");
		if (! users.containsKey(user.getUsername()))
			throw new PetstoreNotFoundException("User not found: username=" + user.getUsername());
		users.put(user.getUsername(), user);
		return user;
	}

	/**
	 * Deletes a user by username.
	 *
	 * @param username The username.
	 * @throws PetstoreNotFoundException If no user with the given username exists.
	 */
	public void deleteUser(String username) {
		if (users.remove(username) == null)
			throw new PetstoreNotFoundException("User not found: username=" + username);
	}
}
