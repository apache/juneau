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

import java.util.*;

import org.apache.juneau.utils.*;

/**
 * Pet store database application.
 */
public class PetStore {

	// Our databases.
	private IdMap<Long,Pet> petDb = IdMap.createLongMap(Pet.class);
	private IdMap<Long,Category> categoryDb = IdMap.createLongMap(Category.class);
	private IdMap<Long,Order> orderDb = IdMap.createLongMap(Order.class);
	private IdMap<Long,Tag> tagDb = IdMap.createLongMap(Tag.class);
	private IdMap<Long,User> userDb = IdMap.createLongMap(User.class);

	
	public Pet getPet(long id) throws IdNotFound {
		Pet value = petDb.get(id);
		if (value == null)
			throw new IdNotFound(id, Pet.class);
		return value;
	}
	
	public Category getCategory(long id) throws IdNotFound {
		Category value = categoryDb.get(id);
		if (value == null)
			throw new IdNotFound(id, Pet.class);
		return value;
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

	public User getUser(long id) throws IdNotFound {
		User value =  userDb.get(id);
		if (value == null)
			throw new IdNotFound(id, Pet.class);
		return value;
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

	public Collection<Category> getCategories() {
		return categoryDb.values();
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
		Pet old = petDb.putIfAbsent(value.getId(), value);
		if (old != null)
			throw new IdConflict(value.getId(), Pet.class);
		return value;
	}

	public Category add(Category value) throws IdConflict {
		if (value.getId() == 0)
			value.id(categoryDb.nextId());
		Category old = categoryDb.putIfAbsent(value.getId(), value);
		if (old != null)
			throw new IdConflict(value.getId(), Category.class);
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
		if (value.getId() == 0)
			value.id(userDb.nextId());
		User old = userDb.putIfAbsent(value.getId(), value);
		if (old != null)
			throw new IdConflict(value.getId(), User.class);
		return value;
	}
	
	public Pet update(Pet value) throws IdNotFound {
		Pet old = petDb.replace(value.getId(), value);
		if (old == null)
			throw new IdNotFound(value.getId(), Pet.class);
		return value;
	}

	public Category update(Category value) throws IdNotFound {
		Category old = categoryDb.replace(value.getId(), value);
		if (old == null)
			throw new IdNotFound(value.getId(), Category.class);
		return value;
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
		User old = userDb.replace(value.getId(), value);
		if (old == null)
			throw new IdNotFound(value.getId(), User.class);
		return value;
	}

	public void removePet(long id) throws IdNotFound {
		petDb.remove(getPet(id).getId());
	}

	public void removeCategory(long id) throws IdNotFound {
		categoryDb.remove(getCategory(id).getId());
	}
	
	public void removeOrder(long id) throws IdNotFound {
		orderDb.remove(getOrder(id).getId());
	}
	
	public void removeTag(long id) throws IdNotFound {
		tagDb.remove(getTag(id).getId());
	}
	
	public void removeUser(long id) throws IdNotFound {
		userDb.remove(getUser(id).getId());
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
		// TODO
		Map<PetStatus,Integer> m = new LinkedHashMap<>();
		return m;
	}
}
