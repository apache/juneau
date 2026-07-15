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
package org.apache.juneau.petstore.client;

import java.util.*;

import org.apache.juneau.http.*;
import org.apache.juneau.http.remote.*;
import org.apache.juneau.petstore.dto.*;

/**
 * Typed client view of the petstore pet CRUD surface, consumed via {@code org.apache.juneau.rest.client.RestClient#remote(Class)}.
 *
 * <p>
 * Mirrors the pet endpoints of {@link org.apache.juneau.petstore.rest.PetStoreResource} (mounted at
 * {@code /petstore} in both the Jetty and Spring Boot deployments). Demonstrates Juneau's REST client consuming
 * a REST resource written with Juneau's REST server — the client interface and the server resource live in
 * different modules but share nothing but the wire protocol and the {@link Pet} DTO.
 *
 * <h5 class='section'>See Also:</h5><ul>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/RestProxies">REST Proxy Basics</a>
 * 	<li class='link'><a class="doclink" href="https://juneau.apache.org/docs/topics/JuneauPetstore">juneau-petstore</a>
 * </ul>
 */
@Remote(path="/petstore")
public interface PetStoreClient {

	/**
	 * Lists all pets.
	 *
	 * @return All pets.
	 */
	@RemoteGet("/pets")
	List<Pet> getPets();

	/**
	 * Retrieves a pet by ID.
	 *
	 * @param id The pet ID.
	 * @return The pet.
	 */
	@RemoteGet("/pets/{id}")
	Pet getPet(@Path("id") long id);

	/**
	 * Creates a new pet.
	 *
	 * @param pet The pet to create.
	 * @return The created pet (with assigned ID).
	 */
	@RemotePost("/pets")
	Pet addPet(@Content Pet pet);
}
