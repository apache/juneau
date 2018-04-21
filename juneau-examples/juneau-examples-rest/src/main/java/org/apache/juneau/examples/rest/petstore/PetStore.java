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

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import org.apache.juneau.*;
import org.apache.juneau.json.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Pet store database application.
 */
public class PetStore {

	// Our "databases".
	IdMap<Long,Pet> petDb = IdMap.createLongMap(Pet.class);
	IdMap<Long,Species> speciesDb = IdMap.createLongMap(Species.class);
	IdMap<Long,Order> orderDb = IdMap.createLongMap(Order.class);
	IdMap<Long,Tag> tagDb = IdMap.createLongMap(Tag.class);
	ConcurrentHashMap<String,User> userDb = new ConcurrentHashMap<>();

	public PetStore init() throws Exception {
		
		// Load our databases from local JSON files.
		
		JsonParser parser = JsonParser.create().build();
		
		// Note that these must be loaded in the specified order to prevent IdNotFound exceptions.
		for (Species s : parser.parse(getStream("Species.json"), Species[].class)) 
			add(s);
		for (Tag t : parser.parse(getStream("Tags.json"), Tag[].class)) 
			add(t);
		
		parser = parser.builder().pojoSwaps(new CategorySwap(), new TagSwap()).build();
		for (Pet p : parser.parse(getStream("Pets.json"), Pet[].class)) 
			add(p);
		
		parser = parser.builder().pojoSwaps(new PetSwap()).build();
		for (Order o : parser.parse(getStream("Orders.json"), Order[].class)) 
			add(o);

		for (User u : parser.parse(getStream("Users.json"), User[].class)) 
			add(u);
		
		return this;
	}
	
	private InputStream getStream(String fileName) {
		return getClass().getResourceAsStream(fileName);
	}
	
	public Pet getPet(long id) throws IdNotFound {
		Pet value = petDb.get(id);
		if (value == null)
			throw new IdNotFound(id, Pet.class);
		return value;
	}
	
	public Species getSpecies(long id) throws IdNotFound {
		Species value = speciesDb.get(id);
		if (value == null)
			throw new IdNotFound(id, Pet.class);
		return value;
	}
	
	public Species getSpecies(String name) throws IdNotFound {
		for (Species value : speciesDb.values())
			if (value.getName().equals(name))
				return value;
		throw new InvalidSpecies();
	}

	public Order getOrder(long id) throws IdNotFound {
		Order value = orderDb.get(id);
		if (value == null)
			throw new IdNotFound(id, Pet.class);
		return value;
	}
	
	public Tag getTag(long id) throws IdNotFound {
		Tag value =  tagDb.get(id);
		if (value == null)
			throw new IdNotFound(id, Pet.class);
		return value;
	}
	
	public Tag getTag(String name) throws InvalidTag  {
		for (Tag value : tagDb.values())
			if (value.getName().equals(name))
				return value;
		throw new InvalidTag();
	}

	public User getUser(String username) throws InvalidUsername, IdNotFound  {
		assertValidUsername(username);
		for (User user : userDb.values())
			if (user.getUsername().equals(username))
				return user;
		throw new IdNotFound(username, User.class);
	}

	public boolean isValid(String username, String password) {
		for (User user : userDb.values())
			if (user.getUsername().equals(username))
				return user.getPassword().equals(password);
		return false;
	}

	public Collection<Pet> getPets() {
		return petDb.values();
	}

	public Collection<Species> getCategories() {
		return speciesDb.values();
	}

	public Collection<Order> getOrders() {
		return orderDb.values();
	}

	public Collection<Tag> getTags() {
		return tagDb.values();
	}

	public Collection<User> getUsers() {
		return userDb.values();
	}

	public Pet add(Pet value) throws IdConflict {
		if (value.getId() == 0)
			value.id(petDb.nextId());
		else
			petDb.lbId(value.getId());
		Pet old = petDb.putIfAbsent(value.getId(), value);
		if (old != null)
			throw new IdConflict(value.getId(), Pet.class);
		return value;
	}

	public Species add(Species value) throws IdConflict {
		if (value.getId() == 0)
			value.id(speciesDb.nextId());
		Species old = speciesDb.putIfAbsent(value.getId(), value);
		if (old != null)
			throw new IdConflict(value.getId(), Species.class);
		return value;
	}
	
	public Order add(Order value) throws IdConflict {
		if (value.getId() == 0)
			value.id(orderDb.nextId());
		Order old = orderDb.putIfAbsent(value.getId(), value);
		if (old != null)
			throw new IdConflict(value.getId(), Order.class);
		return value;
	}
	
	public Tag add(Tag value) throws IdConflict {
		if (value.getId() == 0)
			value.id(tagDb.nextId());
		Tag old = tagDb.putIfAbsent(value.getId(), value);
		if (old != null)
			throw new IdConflict(value.getId(), Tag.class);
		return value;
	}
	
	public User add(User value) throws IdConflict, InvalidUsername {
		assertValidUsername(value.getUsername());
		User old = userDb.putIfAbsent(value.getUsername(), value);
		if (old != null)
			throw new IdConflict(value.getUsername(), User.class);
		return value;
	}
	
	public Pet create(PetCreate pc) {
		Pet p = new Pet();
		p.name(pc.getName());
		p.price(pc.getPrice());
		p.species(getSpecies(pc.getSpecies()));
		p.tags(getTags(pc.getTags()));
		p.status(PetStatus.AVAILABLE);
		return add(p);
	}

	public Pet update(PetUpdate pu) throws IdNotFound {
		Pet p = petDb.get(pu.getId());
		if (p == null)
			throw new IdNotFound(pu.getId(), Pet.class);
		p.name(pu.getName());
		p.price(pu.getPrice());
		p.species(getSpecies(pu.getSpecies()));
		p.tags(getTags(pu.getTags()));
		p.status(pu.getStatus());
		return p;
	}

	public Pet update(Pet pet) {
		petDb.put(pet.getId(), pet);
		return pet;
	}

	public Order create(CreateOrder c) {
		Order o = new Order();
		o.petId(c.getPetId());
		o.shipDate(c.getShipDate());
		o.status(OrderStatus.PLACED);
		return add(o);
	}

	private Tag[] getTags(String[] tags) {
		if (tags == null)
			return null;
		Tag[] l = new Tag[tags.length];
		for (int i = 0; i < tags.length; i++)
			l[i]= getOrCreateTag(tags[i]);
		return l;
	}

	private Tag getOrCreateTag(String name) {
		for (Tag t : tagDb.values())
			if (t.getName().equals(name))
				return t;
		return add(new Tag().name(name));
	}

	public Order update(Order value) throws IdNotFound {
		Order old = orderDb.replace(value.getId(), value);
		if (old == null)
			throw new IdNotFound(value.getId(), Order.class);
		return value;
	}
	
	public Tag update(Tag value) throws IdNotFound, InvalidTag {
		assertValidTag(value.getName());
		Tag old = tagDb.replace(value.getId(), value);
		if (old == null)
			throw new IdNotFound(value.getId(), Tag.class);
		return value;
	}
	
	public User update(User value) throws IdNotFound, InvalidUsername {
		assertValidUsername(value.getUsername());
		User old = userDb.replace(value.getUsername(), value);
		if (old == null)
			throw new IdNotFound(value.getUsername(), User.class);
		return value;
	}

	public void removePet(long id) throws IdNotFound {
		petDb.remove(getPet(id).getId());
	}

	public void removeCategory(long id) throws IdNotFound {
		speciesDb.remove(getSpecies(id).getId());
	}
	
	public void removeOrder(long id) throws IdNotFound {
		orderDb.remove(getOrder(id).getId());
	}
	
	public void removeTag(long id) throws IdNotFound {
		tagDb.remove(getTag(id).getId());
	}
	
	public void removeUser(String username) throws IdNotFound {
		userDb.remove(getUser(username).getUsername());
	}

	private void assertValidUsername(String username) throws InvalidUsername {
		if (username == null || ! username.matches("[\\w\\d]{8,}"))
			throw new InvalidUsername();
	}

	private void assertValidTag(String tag) throws InvalidTag {
		if (tag == null || ! tag.matches("[\\w\\d]{1,8}"))
			throw new InvalidTag();
	}

	public Collection<Pet> getPetsByStatus(PetStatus[] status) {
		List<Pet> list = new ArrayList<>();
		for (Pet p : petDb.values()) 
			if (p.hasStatus(status))
				list.add(p);
		return list;
	}

	public Collection<Pet> getPetsByTags(String[] tags) throws InvalidTag {
		for (String tag : tags)
			assertValidTag(tag);
		List<Pet> list = new ArrayList<>();
		for (Pet p : petDb.values()) 
			if (p.hasTag(tags))
				list.add(p);
		return list;
	}

	public Map<PetStatus,Integer> getInventory() {
		Map<PetStatus,Integer> m = new LinkedHashMap<>();
		for (Pet p : petDb.values()) {
			PetStatus ps = p.getStatus();
			if (! m.containsKey(ps))
				m.put(ps, 1);
			else
				m.put(ps, m.get(ps) + 1);
		}
		return m;
	}
	
	//-----------------------------------------------------------------------------------------------------------------
	// Helper beans
	//-----------------------------------------------------------------------------------------------------------------
	
	public class CategorySwap extends PojoSwap<Species,String> {
		@Override
		public String swap(BeanSession bs, Species o) throws Exception {
			return o.getName();
		}
		@Override
		public Species unswap(BeanSession bs, String o, ClassMeta<?> hint) throws Exception {
			return getSpecies(o);
		}
	}
	
	public class TagSwap extends PojoSwap<Tag,String> {
		@Override
		public String swap(BeanSession bs, Tag o) throws Exception {
			return o.getName();
		}
		@Override
		public Tag unswap(BeanSession bs, String o, ClassMeta<?> hint) throws Exception {
			return getTag(o);
		}
	}

	public class PetSwap extends PojoSwap<Pet,Long> {
		@Override
		public Long swap(BeanSession bs, Pet o) throws Exception {
			return o.getId();
		}
		@Override
		public Pet unswap(BeanSession bs, Long o, ClassMeta<?> hint) throws Exception {
			return petDb.get(o);
		}
	}
}
