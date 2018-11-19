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
package org.apache.juneau.examples.rest.petstore;

import static java.text.MessageFormat.*;

import java.io.*;
import java.util.*;

import javax.persistence.*;

import org.apache.juneau.examples.rest.petstore.dto.*;
import org.apache.juneau.examples.rest.petstore.rest.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.utils.*;

/**
 * Pet store database application.
 * <p>
 * Uses JPA persistence to store and retrieve PetStore DTOs.
 * JPA beans are defined in <code>META-INF/persistence.xml</code>.
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
	 * @throws Exception
	 */
	public PetStoreService initDirect(PrintWriter w) throws Exception {

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

	/**
	 * Initialize the petstore database by using a remote resource interface against our REST.
	 *
	 * @param w Console output.
	 * @return This object (for method chaining).
	 * @throws Exception
	 */
	public PetStoreService initViaRest(PrintWriter w) throws Exception {
		JsonParser parser = JsonParser.create().ignoreUnknownBeanProperties().build();

		String port = System.getProperty("juneau.serverPort", "8000");

		try (RestClient rc = RestClient.create().json().rootUrl("http://localhost:" + port).build()) {
			PetStore ps = rc.getRemoteResource(PetStore.class);

			for (Pet x : ps.getPets()) {
				ps.deletePet("apiKey", x.getId());
				w.println(format("Deleted pet:  id={0}", x.getId()));
			}
			for (Order x : ps.getOrders()) {
				ps.deleteOrder(x.getId());
				w.println(format("Deleted order:  id={0}", x.getId()));
			}
			for (User x : ps.getUsers()) {
				ps.deleteUser(x.getUsername());
				w.println(format("Deleted user:  username={0}", x.getUsername()));
			}
			for (CreatePet x : parser.parse(getStream("init/Pets.json"), CreatePet[].class)) {
				long id = ps.postPet(x);
				w.println(format("Created pet:  id={0}, name={1}", id, x.getName()));
			}
			for (Order x : parser.parse(getStream("init/Orders.json"), Order[].class)) {
				long id = ps.placeOrder(x.getPetId(), x.getUsername());
				w.println(format("Created order:  id={0}", id));
			}
			for (User x: parser.parse(getStream("init/Users.json"), User[].class)) {
				ps.postUser(x);
				w.println(format("Created user:  username={0}", x.getUsername()));
			}
		}

		return this;
	}

	//-----------------------------------------------------------------------------------------------------------------
	// Service methods.
	//-----------------------------------------------------------------------------------------------------------------

	public Pet getPet(long id) throws IdNotFound {
		return find(Pet.class, id);
	}

	public Order getOrder(long id) throws IdNotFound {
		return find(Order.class, id);
	}

	public User getUser(String username) throws InvalidUsername, IdNotFound  {
		assertValidUsername(username);
		return find(User.class, username);
	}

	public List<Pet> getPets() {
		return query("select X from PetstorePet X", Pet.class, (SearchArgs)null);
	}

	public List<Order> getOrders() {
		return query("select X from PetstoreOrder X", Order.class, (SearchArgs)null);
	}

	public List<User> getUsers() {
		return query("select X from PetstoreUser X", User.class, (SearchArgs)null);
	}

	public Pet create(CreatePet c) {
		return merge(new Pet().status(PetStatus.AVAILABLE).apply(c));
	}

	public Order create(CreateOrder c) {
		return merge(new Order().status(OrderStatus.PLACED).apply(c));
	}

	public User create(User c) {
		return merge(new User().apply(c));
	}

	public Pet update(UpdatePet u) throws IdNotFound {
		EntityManager em = getEntityManager();
		return merge(em, find(em, Pet.class, u.getId()).apply(u));
	}

	public Order update(Order o) throws IdNotFound {
		EntityManager em = getEntityManager();
		return merge(em, find(em, Order.class, o.getId()).apply(o));
	}

	public User update(User u) throws IdNotFound, InvalidUsername {
		assertValidUsername(u.getUsername());
		EntityManager em = getEntityManager();
		return merge(em, find(em, User.class, u.getUsername()).apply(u));
	}

	public void removePet(long id) throws IdNotFound {
		EntityManager em = getEntityManager();
		remove(em, find(em, Pet.class, id));
	}

	public void removeOrder(long id) throws IdNotFound {
		EntityManager em = getEntityManager();
		remove(em, find(em, Order.class, id));
	}

	public void removeUser(String username) throws IdNotFound {
		EntityManager em = getEntityManager();
		remove(em, find(em, User.class, username));
	}

	public Collection<Pet> getPetsByStatus(PetStatus[] status) {
		return getEntityManager()
			.createQuery("select X from PetstorePet X where X.status in :status", Pet.class)
			.setParameter("status", status)
			.getResultList();
	}

	public Collection<Pet> getPetsByTags(String[] tags) throws InvalidTag {
		return getEntityManager()
			.createQuery("select X from PetstorePet X where X.tags in :tags", Pet.class)
			.setParameter("tags", tags)
			.getResultList();
	}

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