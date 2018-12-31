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
package org.apache.juneau.examples.rest.petstore.rest;

import static org.apache.juneau.http.HttpMethodName.*;

import java.util.*;

import org.apache.juneau.jsonschema.annotation.Items;
import org.apache.juneau.*;
import org.apache.juneau.examples.rest.petstore.*;
import org.apache.juneau.examples.rest.petstore.dto.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.client.remote.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.response.*;

/**
 * Defines the interface for both the server-side and client-side pet store application.
 *
 * <h5 class='section'>See Also:</h5>
 * <ul class='doctree'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@RemoteResource(path="/petstore")
public interface PetStore {

	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Pets
	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	@RemoteMethod(method=GET, path="/pet")
	public Collection<Pet> getPets() throws NotAcceptable;

	@RemoteMethod(path="/pet/{petId}") /* method inferred from method name */
	public Pet getPet(
		@Path(
			name="petId",
			description="ID of pet to return",
			example="123"
		)
		long petId
	) throws IdNotFound, NotAcceptable;

	@RemoteMethod /* method and path inferred from method name */
	public long postPet(
		@Body(
			description="Pet object to add to the store"
		) CreatePet pet
	) throws IdConflict, NotAcceptable, UnsupportedMediaType;

	@RemoteMethod(method=PUT, path="/pet/{petId}")
	public Ok updatePet(
		@Body(
			description="Pet object that needs to be added to the store"
		) UpdatePet pet
	) throws IdNotFound, NotAcceptable, UnsupportedMediaType;

	@RemoteMethod(method=GET, path="/pet/findByStatus")
	public Collection<Pet> findPetsByStatus(
		@Query(
			name="status",
			description="Status values that need to be considered for filter.",
			required=true,
			type="array",
			collectionFormat="csv",
			items=@Items(
				type="string",
				_enum="AVAILABLE,PENDING,SOLD",
				_default="AVAILABLE"
			),
			example="AVALIABLE,PENDING"
		)
		PetStatus[] status
	) throws NotAcceptable;

	@RemoteMethod(method=GET, path="/pet/findByTags")
	@Deprecated
	public Collection<Pet> findPetsByTags(
		@Query(
			name="tags",
			description="Tags to filter by",
			required=true,
			example="['tag1','tag2']"
		)
		String[] tags
	) throws InvalidTag, NotAcceptable;

	@RemoteMethod(method=DELETE, path="/pet/{petId}")
	public Ok deletePet(
		@Header(
			name="api_key",
			description="Security API key",
			required=true,
			example="foobar"
		)
		String apiKey,
		@Path(
			name="petId",
			description="Pet id to delete",
			example="123"
		)
		long petId
	) throws IdNotFound, NotAcceptable;

	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Orders
	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	@RemoteMethod(method=GET, path="/store/order")
	public Collection<Order> getOrders() throws NotAcceptable;

	@RemoteMethod(method=GET, path="/store/order/{orderId}")
	public Order getOrder(
		@Path(
			name="orderId",
			description="ID of order to fetch",
			maximum="1000",
			minimum="1",
			example="123"
		)
		long orderId
	) throws InvalidId, IdNotFound, NotAcceptable;

	@RemoteMethod(method=POST, path="/store/order")
	public long placeOrder(
		@FormData(
			name="petId",
			description="Pet ID"
		)
		long petId,
		@FormData(
			name="username",
			description="The username of the user creating the order"
		)
		String username
	) throws IdConflict, NotAcceptable, UnsupportedMediaType;

	@RemoteMethod(method=DELETE, path="/store/order/{orderId}")
	public Ok deleteOrder(
		@Path(
			name="orderId",
			description="ID of the order that needs to be deleted",
			minimum="1",
			example="5"
		)
		long orderId
	) throws InvalidId, IdNotFound, NotAcceptable;

	@RemoteMethod(method=GET, path="/store/inventory")
	public Map<PetStatus,Integer> getStoreInventory() throws NotAcceptable;

	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Users
	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	@RemoteMethod(method=GET, path="/user")
	public Collection<User> getUsers() throws NotAcceptable;

	@RemoteMethod(method=GET, path="/user/{username}")
	public User getUser(
		@Path(
			name="username",
			description="The name that needs to be fetched. Use user1 for testing."
		)
		String username
	) throws InvalidUsername, IdNotFound, NotAcceptable;

	@RemoteMethod
	public Ok postUser(
		@Body(
			description="Created user object"
		)
		User user
	) throws InvalidUsername, IdConflict, NotAcceptable, UnsupportedMediaType;

	@RemoteMethod(method=POST, path="/user/createWithArray")
	public Ok createUsers(
		@Body(
			description="List of user objects"
		)
		User[] users
	) throws InvalidUsername, IdConflict, NotAcceptable, UnsupportedMediaType;

	@RemoteMethod(method=PUT, path="/user/{username}")
	public Ok updateUser(
		@Path(
			name="username",
			description="Name that need to be updated"
		)
		String username,
		@Body(
			description="Updated user object"
		)
		User user
	) throws InvalidUsername, IdNotFound, NotAcceptable, UnsupportedMediaType;

	@RemoteMethod(method=DELETE, path="/user/{username}")
	public Ok deleteUser(
		@Path(
			name="username",
			description="The name that needs to be deleted"
		)
		String username
	) throws InvalidUsername, IdNotFound, NotAcceptable;

	@RemoteMethod(method=GET, path="/user/login")
	public Ok login(
		@Query(
			name="username",
			description="The username for login",
			required=true,
			example="myuser"
		)
		String username,
		@Query(
			name="password",
			description="The password for login in clear text",
			required=true,
			example="abc123"
		)
		String password,
		@ResponseHeader(
			name="X-Rate-Limit",
			type="integer",
			format="int32",
			description="Calls per hour allowed by the user.",
			example="123"
		)
		Value<Integer> rateLimit,
		Value<ExpiresAfter> expiresAfter,
		RestRequest req,
		RestResponse res
	) throws InvalidLogin, NotAcceptable;

	@RemoteMethod(method=GET, path="/user/logout")
	public Ok logout() throws NotAcceptable;
}
