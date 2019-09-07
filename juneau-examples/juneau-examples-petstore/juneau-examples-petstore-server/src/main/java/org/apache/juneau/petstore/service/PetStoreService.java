// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.petstore.service;

import static java.text.MessageFormat.*;

import java.io.*;
import java.util.*;

import javax.persistence.*;

import org.apache.juneau.json.*;
import org.apache.juneau.parser.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.pojotools.*;
import org.apache.juneau.pojotools.SearchArgs;

/**
 * Pet store database application.
 * <p>
 * Uses JPA persistence to store and retrieve PetStore DTOs.
 * JPA beans are defined in <c>META-INF/persistence.xml</c>.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
public class PetStoreService extends AbstractPersistenceService {

	//-----------------------------------------------------------------------------------------------------------------
	// Initialization methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Initialize the petstore database using JPA.
	 *
	 * @param w Console output.
	 * @return This object (for method chaining).
	 * @throws ParseException Malformed input encountered.
	 * @throws IOException File could not be read from file system.
	 */
	public PetStoreService initDirect(PrintWriter w) throws ParseException, IOException {

		EntityManager em = getEntityManager();
		EntityTransaction et = em.getTransaction();
		JsonParser parser = JsonParser.create().build();

		et.begin();

		for (Pet x : em.createQuery("select X from PetstorePet X", Pet.class).getResultList()) {
			em.remove(x);
			w.println(format("Deleted pet:  id={0}", x.getId()));
		}
		for (Order x : em.createQuery("select X from PetstoreOrder X", Order.class).getResultList()) {
			em.remove(x);
			w.println(format("Deleted order:  id={0}", x.getId()));
		}
		for (User x : em.createQuery("select X from PetstoreUser X", User.class).getResultList()) {
			em.remove(x);
			w.println(format("Deleted user:  username={0}", x.getUsername()));
		}

		et.commit();
		et.begin();

		for (Pet x : parser.parse(getStream("init/Pets.json"), Pet[].class)) {
			x = em.merge(x);
			w.println(format("Created pet:  id={0}, name={1}", x.getId(), x.getName()));
		}
		for (Order x : parser.parse(getStream("init/Orders.json"), Order[].class)) {
			x = em.merge(x);
			w.println(format("Created order:  id={0}", x.getId()));
		}
		for (User x: parser.parse(getStream("init/Users.json"), User[].class)) {
			x = em.merge(x);
			w.println(format("Created user:  username={0}", x.getUsername()));
		}

		et.commit();

		return this;
	}


	//-----------------------------------------------------------------------------------------------------------------
	// Service methods.
	//-----------------------------------------------------------------------------------------------------------------

	/**
	 * Returns the pet with the specified ID.
	 *
	 * @param id The pet ID.
	 * @return The pet with the specified ID.  Never <jk>null</jk>.
	 * @throws IdNotFound If pet was not found.
	 */
	public Pet getPet(long id) throws IdNotFound {
		return find(Pet.class, id);
	}

	/**
	 * Returns the order with the specified ID.
	 *
	 * @param id The order ID.
	 * @return The order with the specified ID.  Never <jk>null</jk>.
	 * @throws IdNotFound If order was not found.
	 */
	public Order getOrder(long id) throws IdNotFound {
		return find(Order.class, id);
	}

	/**
	 * Returns the user with the specified username.
	 *
	 * @param username The username.
	 * @return The user with the specified username.  Never <jk>null</jk>.
	 * @throws InvalidUsername Username was not valid.
	 * @throws IdNotFound If order was not found.
	 */
	public User getUser(String username) throws InvalidUsername, IdNotFound  {
		assertValidUsername(username);
		return find(User.class, username);
	}

	/**
	 * Returns all pets in the database.
	 *
	 * @return All pets in the database.
	 */
	public List<Pet> getPets() {
		return query("select X from PetstorePet X", Pet.class, (SearchArgs)null, (PageArgs)null);
	}

	/**
	 * Returns all orders in the database.
	 *
	 * @return All orders in the database.
	 */
	public List<Order> getOrders() {
		return query("select X from PetstoreOrder X", Order.class, (SearchArgs)null, (PageArgs)null);
	}

	/**
	 * Returns all users in the database.
	 *
	 * @return All users in the database.
	 */
	public List<User> getUsers() {
		return query("select X from PetstoreUser X", User.class, (SearchArgs)null, (PageArgs)null);
	}

	/**
	 * Creates a new pet in the database.
	 *
	 * @param c The pet input data.
	 * @return a new {@link Pet} object.
	 */
	public Pet create(CreatePet c) {
		return merge(new Pet().status(PetStatus.AVAILABLE).apply(c));
	}

	/**
	 * Creates a new order in the database.
	 *
	 * @param c The order input data.
	 * @return a new {@link Order} object.
	 */
	public Order create(CreateOrder c) {
		return merge(new Order().status(OrderStatus.PLACED).apply(c));
	}

	/**
	 * Creates a new user in the database.
	 *
	 * @param c The user input data.
	 * @return a new {@link User} object.
	 */
	public User create(User c) {
		return merge(new User().apply(c));
	}

	/**
	 * Updates a pet in the database.
	 *
	 * @param u The update information.
	 * @return The updated {@link Pet} object.
	 * @throws IdNotFound Pet was not found.
	 */
	public Pet update(UpdatePet u) throws IdNotFound {
		EntityManager em = getEntityManager();
		return merge(em, find(em, Pet.class, u.getId()).apply(u));
	}

	/**
	 * Updates an order in the database.
	 *
	 * @param o The update information.
	 * @return The updated {@link Order} object.
	 * @throws IdNotFound Order was not found.
	 */
	public Order update(Order o) throws IdNotFound {
		EntityManager em = getEntityManager();
		return merge(em, find(em, Order.class, o.getId()).apply(o));
	}

	/**
	 * Updates a user in the database.
	 *
	 * @param u The update information.
	 * @return The updated {@link User} object.
	 * @throws IdNotFound User was not found.
	 * @throws InvalidUsername The username was not valid.
	 */
	public User update(User u) throws IdNotFound, InvalidUsername {
		assertValidUsername(u.getUsername());
		EntityManager em = getEntityManager();
		return merge(em, find(em, User.class, u.getUsername()).apply(u));
	}

	/**
	 * Removes a pet from the database.
	 *
	 * @param id The pet ID.
	 * @throws IdNotFound Pet was not found.
	 */
	public void removePet(long id) throws IdNotFound {
		EntityManager em = getEntityManager();
		remove(em, find(em, Pet.class, id));
	}

	/**
	 * Removes an order from the database.
	 *
	 * @param id The order ID.
	 * @throws IdNotFound Order was not found.
	 */
	public void removeOrder(long id) throws IdNotFound {
		EntityManager em = getEntityManager();
		remove(em, find(em, Order.class, id));
	}

	/**
	 * Removes a user from the database.
	 *
	 * @param username The username.
	 * @throws IdNotFound User was not found.
	 */
	public void removeUser(String username) throws IdNotFound {
		EntityManager em = getEntityManager();
		remove(em, find(em, User.class, username));
	}

	/**
	 * Returns all pets with the specified statuses.
	 *
	 * @param status Pet statuses.
	 * @return Pets with the specified statuses.
	 */
	public Collection<Pet> getPetsByStatus(PetStatus[] status) {
		return getEntityManager()
			.createQuery("select X from PetstorePet X where X.status in :status", Pet.class)
			.setParameter("status", status)
			.getResultList();
	}

	/**
	 * Returns all pets with the specified tags.
	 *
	 * @param tags Pet tags.
	 * @return Pets with the specified tags.
	 * @throws InvalidTag Tag name was invalid.
	 */
	public Collection<Pet> getPetsByTags(String[] tags) throws InvalidTag {
		return getEntityManager()
			.createQuery("select X from PetstorePet X where X.tags in :tags", Pet.class)
			.setParameter("tags", tags)
			.getResultList();
	}

	/**
	 * Returns a summary of pet statuses and counts.
	 *
	 * @return A summary of pet statuses and counts.
	 */
	public Map<PetStatus,Integer> getInventory() {
		Map<PetStatus,Integer> m = new LinkedHashMap<>();
		for (Pet p : getPets()) {
			PetStatus ps = p.getStatus();
			if (! m.containsKey(ps))
				m.put(ps, 1);
			else
				m.put(ps, m.get(ps) + 1);
		}
		return m;
	}

	/**
	 * Returns <jk>true</jk> if the specified username and password is valid.
	 *
	 * @param username The username.
	 * @param password The password.
	 * @return <jk>true</jk> if the specified username and password is valid.
	 */
	public boolean isValid(String username, String password) {
		return getUser(username).getPassword().equals(password);
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Helper methods
	//-----------------------------------------------------------------------------------------------------------------

	private void assertValidUsername(String username) throws InvalidUsername {
		if (username == null || ! username.matches("[\\w\\d]{3,8}"))
			throw new InvalidUsername();
	}

	private InputStream getStream(String fileName) {
		return getClass().getResourceAsStream(fileName);
	}
}