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
package org.apache.juneau.petstore.rest;

import java.awt.image.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.marshall.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;

/**
 * Petstore CRUD REST resource.
 *
 * <p>
 * Deployment-agnostic.  Mounted by both {@code juneau-petstore-jetty} and {@code juneau-petstore-springboot} via
 * the {@code @Rest(children=...)} of their root router.
 *
 * <p>
 * Endpoints:
 * <ul>
 * 	<li>{@code GET    /pets}                — list pets
 * 	<li>{@code GET    /pets/{id}}           — get pet
 * 	<li>{@code POST   /pets}                — create pet
 * 	<li>{@code PUT    /pets/{id}}           — update pet
 * 	<li>{@code DELETE /pets/{id}}           — delete pet
 * 	<li>{@code GET    /pets/{id}/photo}     — get pet photo
 * 	<li>{@code PUT    /pets/{id}/photo}     — upload pet photo
 * 	<li>{@code GET    /orders}              — list orders
 * 	<li>{@code GET    /orders/{id}}         — get order
 * 	<li>{@code POST   /orders}              — create order
 * 	<li>{@code PUT    /orders/{id}}         — update order
 * 	<li>{@code DELETE /orders/{id}}         — delete order
 * 	<li>{@code GET    /users}               — list users
 * 	<li>{@code GET    /users/{username}}    — get user
 * 	<li>{@code POST   /users}               — create user
 * 	<li>{@code PUT    /users/{username}}    — update user
 * 	<li>{@code DELETE /users/{username}}    — delete user
 * </ul>
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@Rest(
	path="/petstore",
	title="Petstore",
	description="Apache Juneau petstore sample resource."
)
@SuppressWarnings({
	"java:S110" // Inheritance depth acceptable for example/demo code
})
public class PetStoreResource extends BasicRestServlet {

	private static final long serialVersionUID = 1L;

	/** Backing store.  Singleton servlet → singleton store, shared across requests. */
	private final transient PetStore store = new PetStore();

	/** In-memory photo bytes, keyed by pet ID.  Not seeded from JSON — populated only via {@link #putPetPhoto}. */
	private final transient Map<Long,BufferedImage> photos = new ConcurrentHashMap<>();

	//------------------------------------------------------------------------------------------------------------------
	// Pets
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Lists all pets.
	 *
	 * @return All pets.
	 */
	@RestGet("/pets")
	public Collection<Pet> getPets() {
		return store.getPets();
	}

	/**
	 * Retrieves a pet by ID.
	 *
	 * @param id The pet ID.
	 * @return The pet.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestGet("/pets/{id}")
	public Pet getPet(@Path("id") long id) {
		var pet = store.getPet(id);
		if (pet == null)
			throw new NotFound("Pet not found: id={0}", id);
		return pet;
	}

	/**
	 * Creates a new pet.
	 *
	 * @param pet The pet to create.
	 * @return The created pet (with assigned ID).
	 */
	@RestPost("/pets")
	public Pet createPet(@Content Pet pet) {
		return store.createPet(pet);
	}

	/**
	 * Updates an existing pet.
	 *
	 * @param id The pet ID.
	 * @param pet The replacement pet body.
	 * @return The updated pet.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestPut("/pets/{id}")
	public Pet updatePet(@Path("id") long id, @Content Pet pet) {
		pet.setId(id);
		try {
			return store.updatePet(pet);
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		}
	}

	/**
	 * Deletes a pet by ID.
	 *
	 * @param id The pet ID.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestDelete("/pets/{id}")
	public void deletePet(@Path("id") long id) {
		try {
			store.deletePet(id);
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		}
	}

	/**
	 * Retrieves the uploaded photo for a pet.
	 *
	 * @param id The pet ID.
	 * @return The photo image.
	 * @throws NotFound If no pet with the given ID exists, or no photo has been uploaded for it.
	 */
	@RestGet(path="/pets/{id}/photo", serializers=PetPhotoSerializer.class)
	public BufferedImage getPetPhoto(@Path("id") long id) {
		if (store.getPet(id) == null)
			throw new NotFound("Pet not found: id={0}", id);
		var image = photos.get(id);
		if (image == null)
			throw new NotFound("No photo uploaded for pet: id={0}", id);
		return image;
	}

	/**
	 * Uploads a photo for a pet.
	 *
	 * <p>
	 * On success, also updates the pet's {@link Pet#getPhoto() photo} field to point back at this endpoint.
	 *
	 * @param id The pet ID.
	 * @param image The photo image.
	 * @return OK.
	 * @throws NotFound If no pet with the given ID exists.
	 */
	@RestPut(path="/pets/{id}/photo", parsers=PetPhotoParser.class)
	public Ok putPetPhoto(@Path("id") long id, @Content BufferedImage image) {
		var pet = store.getPet(id);
		if (pet == null)
			throw new NotFound("Pet not found: id={0}", id);
		photos.put(id, image);
		pet.setPhoto("/petstore/pets/" + id + "/photo");
		return Ok.INSTANCE;
	}

	//------------------------------------------------------------------------------------------------------------------
	// Orders
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Lists all orders.
	 *
	 * @return All orders.
	 */
	@RestGet("/orders")
	public Collection<Order> getOrders() {
		return store.getOrders();
	}

	/**
	 * Retrieves an order by ID.
	 *
	 * @param id The order ID.
	 * @return The order.
	 * @throws NotFound If no order with the given ID exists.
	 */
	@RestGet("/orders/{id}")
	public Order getOrder(@Path("id") long id) {
		var order = store.getOrder(id);
		if (order == null)
			throw new NotFound("Order not found: id={0}", id);
		return order;
	}

	/**
	 * Creates a new order.
	 *
	 * @param order The order to create.
	 * @return The created order (with assigned ID).
	 */
	@RestPost("/orders")
	public Order createOrder(@Content Order order) {
		return store.createOrder(order);
	}

	/**
	 * Updates an existing order.
	 *
	 * @param id The order ID.
	 * @param order The replacement order body.
	 * @return The updated order.
	 * @throws NotFound If no order with the given ID exists.
	 */
	@RestPut("/orders/{id}")
	public Order updateOrder(@Path("id") long id, @Content Order order) {
		order.setId(id);
		try {
			return store.updateOrder(order);
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		}
	}

	/**
	 * Deletes an order by ID.
	 *
	 * @param id The order ID.
	 * @throws NotFound If no order with the given ID exists.
	 */
	@RestDelete("/orders/{id}")
	public void deleteOrder(@Path("id") long id) {
		try {
			store.deleteOrder(id);
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		}
	}

	//------------------------------------------------------------------------------------------------------------------
	// Users
	//------------------------------------------------------------------------------------------------------------------

	/**
	 * Lists all users.
	 *
	 * @return All users.
	 */
	@RestGet("/users")
	public Collection<User> getUsers() {
		return store.getUsers();
	}

	/**
	 * Retrieves a user by username.
	 *
	 * @param username The username.
	 * @return The user.
	 * @throws NotFound If no user with the given username exists.
	 */
	@RestGet("/users/{username}")
	public User getUser(@Path("username") String username) {
		var user = store.getUser(username);
		if (user == null)
			throw new NotFound("User not found: username={0}", username);
		return user;
	}

	/**
	 * Creates a new user.
	 *
	 * @param user The user to create.  Must carry a non-null username not already in use.
	 * @return The created user.
	 */
	@RestPost("/users")
	public User createUser(@Content User user) {
		return store.createUser(user);
	}

	/**
	 * Updates an existing user.
	 *
	 * @param username The username.
	 * @param user The replacement user body.
	 * @return The updated user.
	 * @throws NotFound If no user with the given username exists.
	 */
	@RestPut("/users/{username}")
	public User updateUser(@Path("username") String username, @Content User user) {
		user.setUsername(username);
		try {
			return store.updateUser(user);
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		}
	}

	/**
	 * Deletes a user by username.
	 *
	 * @param username The username.
	 * @throws NotFound If no user with the given username exists.
	 */
	@RestDelete("/users/{username}")
	public void deleteUser(@Path("username") String username) {
		try {
			store.deleteUser(username);
		} catch (PetstoreNotFoundException e) {
			throw new NotFound(e.getMessage());
		}
	}
}
