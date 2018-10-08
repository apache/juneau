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
import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;

/**
 * Pet store database application.
 */
public class PetStoreService {

	private final EntityManagerFactory entityManagerFactory;

	public PetStoreService() {
		entityManagerFactory = Persistence.createEntityManagerFactory("test");
	}

	public PetStoreService initDirect(PrintWriter w) throws Exception {

		EntityManager em = getEntityManager();
		EntityTransaction et = em.getTransaction();
		JsonParser parser = JsonParser.create().build();

		et.begin();

		for (Pet x : em.createQuery("select X from Pet X", Pet.class).getResultList()) {
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

	public PetStoreService initViaRest(PrintWriter w) throws Exception {
		JsonParser parser = JsonParser.create().ignoreUnknownBeanProperties().build();

		try (RestClient rc = RestClient.create().json().rootUrl("http://localhost:10000").build()) {
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
		return query("select X from Pet X", Pet.class);
	}

	public List<Order> getOrders() {
		return query("select X from PetstoreOrder X", Order.class);
	}

	public List<User> getUsers() {
		return query("select X from PetstoreUser X", User.class);
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
		return merge(getPet(u.getId()).apply(u));
	}

	public Order update(Order o) throws IdNotFound {
		return merge(getOrder(o.getId()).apply(o));
	}

	public User update(User u) throws IdNotFound, InvalidUsername {
		assertValidUsername(u.getUsername());
		return merge(getUser(u.getUsername()).apply(u));
	}

	public void removePet(long id) throws IdNotFound {
		remove(getPet(id));
	}

	public void removeOrder(long id) throws IdNotFound {
		remove(getOrder(id));
	}

	public void removeUser(String username) throws IdNotFound {
		remove(getUser(username));
	}

	public Collection<Pet> getPetsByStatus(PetStatus[] status) {
		return getEntityManager()
			.createQuery("select X from Pet X where X.status in :status", Pet.class)
			.setParameter("status", status)
			.getResultList();
	}

	public Collection<Pet> getPetsByTags(String[] tags) throws InvalidTag {
		return getEntityManager()
			.createQuery("select X from Pet X where X.tags in :tags", Pet.class)
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

	private EntityManager getEntityManager() {
		return entityManagerFactory.createEntityManager();
	}

	private <T> T merge(T t) {
		EntityManager em = getEntityManager();
		try {
			EntityTransaction et = em.getTransaction();
			et.begin();
			t = em.merge(t);
			et.commit();
			return t;
		} finally {
			em.close();
		}
	}

	private <T> void remove(T t) {
		EntityManager em = getEntityManager();
		try {
			EntityTransaction et = em.getTransaction();
			et.begin();
			em.remove(t);
			et.commit();
		} finally {
			em.close();
		}
	}

	private <T> List<T> query(String query, Class<T> t) {
		return getEntityManager().createQuery(query, t).getResultList();
	}

	private <T> T find(Class<T> t, Object id) throws IdNotFound {
		T o = getEntityManager().find(t, id);
		if (o == null)
			throw new IdNotFound(id, t);
		return o;
	}

	private InputStream getStream(String fileName) {
		return getClass().getResourceAsStream(fileName);
	}
}