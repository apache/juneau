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

// region resource
import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.server.*;
import org.apache.juneau.rest.server.servlet.*;
import org.springframework.beans.factory.annotation.*;

/**
 * The pet API, mounted at {@code /pets} under {@link PetRootServlet}.
 */
@Rest(path = "/pets", title = "Pets")
public class PetResource extends BasicRestResource {

	private final PetStore store;

	/**
	 * Called by Spring.
	 *
	 * @param store The pet store bean.
	 */
	@Autowired
	public PetResource(PetStore store) {
		this.store = store;
	}

	/**
	 * Lists the pets.
	 *
	 * @return Every pet.
	 */
	@RestGet("/")
	public List<Pet> listPets() {
		return store.list();
	}

	/**
	 * Returns one pet.
	 *
	 * @param id The pet id from the URL.
	 * @return The pet.
	 * @throws NotFound If there is no pet with that id.
	 */
	@RestGet("/{id}")
	public Pet getPet(@Path("id") int id) throws NotFound {
		return store.get(id).orElseThrow(() -> new NotFound("No pet with id " + id));
	}

	/**
	 * Adds a pet.
	 *
	 * @param pet The pet to add; only {@code name} is used.
	 * @return The new pet, with its id.
	 */
	@RestPost("/")
	public Pet addPet(@Content Pet pet) {
		return store.add(pet.name);
	}
}
// endregion resource
