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
package org.apache.juneau.examples.guides.springbootrestapi;

// region store
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * An in-memory pet store.  Spring creates one and injects it into {@link PetResource}.
 */
public class PetStore {

	private final Map<Integer,Pet> pets = new ConcurrentSkipListMap<>();
	private final AtomicInteger nextId = new AtomicInteger(1);

	/** Creates the store with one pet in it. */
	public PetStore() {
		add("Fido");
	}

	/**
	 * Adds a pet.
	 *
	 * @param name The pet's name.
	 * @return The new pet, with its id.
	 */
	public Pet add(String name) {
		var pet = new Pet(nextId.getAndIncrement(), name);
		pets.put(pet.id, pet);
		return pet;
	}

	/**
	 * Looks up a pet.
	 *
	 * @param id The id.
	 * @return The pet, or empty if there is none with that id.
	 */
	public Optional<Pet> get(int id) {
		return Optional.ofNullable(pets.get(id));
	}

	/**
	 * Returns every pet, in id order.
	 *
	 * @return The pets.
	 */
	public List<Pet> list() {
		return new ArrayList<>(pets.values());
	}
}
// endregion store
