// ***************************************************************************************************************************
// * Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file *
// * distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file        *
// * to you under the Apache License, Version 2.0 (the 'License"); you may not use this file except in compliance            *
// * with the License.  You may obtain a copy of the License at                                                              *
// *                                                                                                                         *
// *  http://www.apache.org/licenses/LICENSE-2.0                                                                             *
// *                                                                                                                         *
// * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an  *
// * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the        *
// * specific language governing permissions and limitations under the License.                                              *
// ***************************************************************************************************************************
package org.apache.juneau.petstore.rest;

import static org.apache.juneau.dto.swagger.ui.SwaggerUI.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.http.response.Ok.*;

import java.util.*;
import java.util.Map;

import javax.inject.*;

import org.apache.juneau.jsonschema.annotation.*;
import org.apache.juneau.petstore.*;
import org.apache.juneau.petstore.dto.*;
import org.apache.juneau.petstore.service.*;
import org.apache.juneau.*;
import org.apache.juneau.annotation.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.http.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.rest.converters.*;

/**
 * Sample Petstore application.
 *
 * <ul class='seealso'>
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@RestResource(
	path="/petstore",
	title="Petstore application",
	description={
		"This is a sample server Petstore server based on the Petstore sample at Swagger.io.",
		"You can find out more about Swagger at http://swagger.io.",
	},
	properties= {
		// Resolve recursive references when showing schema info in the swagger.
		@Property(name=SWAGGERUI_resolveRefsMaxDepth, value="99")
	},
	swagger=@ResourceSwagger(
		version="1.0.0",
		title="Swagger Petstore",
		termsOfService="You are on your own.",
		contact=@Contact(
			name="Juneau Development Team",
			email="dev@juneau.apache.org",
			url="http://juneau.apache.org"
		),
		license=@License(
			name="Apache 2.0",
			url="http://www.apache.org/licenses/LICENSE-2.0.html"
		),
		externalDocs=@ExternalDocs(
			description="Find out more about Juneau",
			url="http://juneau.apache.org"
		),
		tags={
			@Tag(
				name="pet",
				description="Everything about your Pets",
				externalDocs=@ExternalDocs(
					description="Find out more",
					url="http://juneau.apache.org"
				)
			),
			@Tag(
				name="store",
				description="Access to Petstore orders"
			),
			@Tag(
				name="user",
				description="Operations about user",
				externalDocs=@ExternalDocs(
					description="Find out more about our store",
					url="http://juneau.apache.org"
				)
			)
		}
	),
	staticFiles={"htdocs:/htdocs"}  // Expose static files in htdocs subpackage.
)
@HtmlDocConfig(
	widgets={
		ContentTypeMenuItem.class,
		ThemeMenuItem.class,
	},
	navlinks={
		"up: request:/..",
		"options: servlet:/?method=OPTIONS",
		"$W{ContentTypeMenuItem}",
		"$W{ThemeMenuItem}",
		"source: $C{Source/gitHub}/org/apache/juneau/petstore/rest/$R{servletClassSimple}.java"
	},
	head={
		"<link rel='icon' href='$U{servlet:/htdocs/cat.png}'/>"  // Add a cat icon to the page.
	},
	header={
		"<h1>$R{resourceTitle}</h1>",
		"<h2>$R{methodSummary}</h2>",
		"$C{PetStore/headerImage}"
	},
	aside={
		"<div style='max-width:400px' class='text'>",
		"	<p>This page shows a standard nested REST resource.</p>",
		"	<p>It shows how different properties can be rendered on the same bean in different views.</p>",
		"	<p>It also shows examples of HtmlRender classes and @BeanProperty(format) annotations.</p>",
		"	<p>It also shows how the Queryable converter and query widget can be used to create searchable interfaces.</p>",
		"</div>"
	},
	stylesheet="servlet:/htdocs/themes/dark.css"  // Use dark theme by default.
)
public class PetStoreResource extends BasicRest implements PetStore {

	@Inject
	private PetStoreService store;

	/**
	 * Navigation page
	 *
	 * @return Navigation page contents.
	 */
	@RestMethod(
		name=GET,
		path="/",
		summary="Navigation page"
	)
	@HtmlDocConfig(
		style={
			"INHERIT",  // Flag for inheriting resource-level CSS.
			"body { ",
				"background-image: url('petstore/htdocs/background.jpg'); ",
				"background-color: black; ",
				"background-size: cover; ",
				"background-attachment: fixed; ",
			"}"
		}
	)
	public ResourceDescriptions getTopPage() {
		return new ResourceDescriptions()
			.append("pet", "All pets in the store")
			.append("store", "Orders and inventory")
			.append("user", "Petstore users")
		;
	}

	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Pets
	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override /* PetStore */
	@RestMethod(
		name=GET,
		path="/pet",
		summary="All pets in the store",
		swagger=@MethodSwagger(
			tags="pet",
			parameters={
				Queryable.SWAGGER_PARAMS
			}
		),
		converters={Queryable.class}
	)
	@BeanConfig(
		bpx="Pet: tags,photo"
	)
	public Collection<Pet> getPets() throws NotAcceptable {
		return store.getPets();
	}

	@Override /* PetStore */
	@RestMethod(
		name=GET,
		path="/pet/{petId}",
		summary="Find pet by ID",
		description="Returns a single pet",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { api_key:[] } ]"
			}
		)
	)
	public Pet getPet(long petId) throws IdNotFound, NotAcceptable {
		return store.getPet(petId);
	}

	@Override /* PetStore */
	@RestMethod(
		name=POST,
		path="/pet",
		summary="Add a new pet to the store",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth:['write:pets','read:pets'] } ]"
			}
		)
	)
	public long createPet(CreatePet pet) throws IdConflict, NotAcceptable, UnsupportedMediaType {
		return store.create(pet).getId();
	}

	@Override /* PetStore */
	@RestMethod(
		name=PUT,
		path="/pet/{petId}",
		summary="Update an existing pet",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth: ['write:pets','read:pets'] } ]"
			}
		)
	)
	public Ok updatePet(UpdatePet pet) throws IdNotFound, NotAcceptable, UnsupportedMediaType {
		store.update(pet);
		return OK;
	}

	@Override /* PetStore */
	@RestMethod(
		name=GET,
		path="/pet/findByStatus",
		summary="Finds Pets by status",
		description="Multiple status values can be provided with comma separated strings.",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[{petstore_auth:['write:pets','read:pets']}]"
			}
		)
	)
	public Collection<Pet> findPetsByStatus(PetStatus[] status) throws NotAcceptable {
		return store.getPetsByStatus(status);
	}

	@Override /* PetStore */
	@RestMethod(
		name=GET,
		path="/pet/findByTags",
		summary="Finds Pets by tags",
		description="Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth:[ 'write:pets','read:pets' ] } ]"
			}
		)
	)
	@Deprecated
	public Collection<Pet> findPetsByTags(String[] tags) throws InvalidTag, NotAcceptable {
		return store.getPetsByTags(tags);
	}

	@Override /* PetStore */
	@RestMethod(
		name=DELETE,
		path="/pet/{petId}",
		summary="Deletes a pet",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth:[ 'write:pets','read:pets' ] } ]"
			}
		)
	)
	public Ok deletePet(String apiKey, long petId) throws IdNotFound, NotAcceptable {
		store.removePet(petId);
		return OK;
	}

	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Orders
	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	/**
	 * Store navigation page.
	 *
	 * @return Store navigation page contents.
	 */
	@RestMethod(
		summary="Store navigation page",
		swagger=@MethodSwagger(
			tags="store"
		)
	)
	public ResourceDescriptions getStore() {
		return new ResourceDescriptions()
			.append("store/order", "Petstore orders")
			.append("store/inventory", "Petstore inventory")
		;
	}

	@Override
	@RestMethod(
		name=GET,
		path="/store/order",
		summary="Petstore orders",
		swagger=@MethodSwagger(
			tags="store"
		)
	)
	@HtmlDocConfig(
		widgets={
			QueryMenuItem.class
		},
		navlinks={
			"INHERIT",                // Inherit links from class.
			"[2]:$W{QueryMenuItem}",  // Insert QUERY link in position 2.
			"[3]:$W{AddOrderMenuItem}"  // Insert ADD link in position 3.
		}
	)
	public Collection<Order> getOrders() throws NotAcceptable {
		return store.getOrders();
	}

	@Override
	@RestMethod(
		name=GET,
		path="/store/order/{orderId}",
		summary="Find purchase order by ID",
		description="Returns a purchase order by ID.",
		swagger=@MethodSwagger(
			tags="store"
		)
	)
	public Order getOrder(long orderId) throws InvalidId, IdNotFound, NotAcceptable {
		if (orderId < 1 || orderId > 1000)
			throw new InvalidId();
		return store.getOrder(orderId);
	}

	@Override
	@RestMethod(
		name=POST,
		path="/store/order",
		summary="Place an order for a pet",
		swagger=@MethodSwagger(
			tags="store"
		),
		pojoSwaps={
			TemporalDateSwap.IsoLocalDate.class
		}
	)
	public long placeOrder(long petId, String username) throws IdConflict, NotAcceptable, UnsupportedMediaType {
		CreateOrder co = new CreateOrder(petId, username);
		return store.create(co).getId();
	}

	@Override
	@RestMethod(
		name=DELETE,
		path="/store/order/{orderId}",
		summary="Delete purchase order by ID",
		description= {
			"For valid response try integer IDs with positive integer value.",
			"Negative or non-integer values will generate API errors."
		},
		swagger=@MethodSwagger(
			tags="store"
		)
	)
	public Ok deleteOrder(long orderId) throws InvalidId, IdNotFound, NotAcceptable {
		if (orderId < 0)
			throw new InvalidId();
		store.removeOrder(orderId);
		return OK;
	}

	@Override
	@RestMethod(
		name=GET,
		path="/store/inventory",
		summary="Returns pet inventories by status",
		description="Returns a map of status codes to quantities",
		swagger=@MethodSwagger(
			tags="store",
			responses={
				"200:{ 'x-example':{AVAILABLE:123} }",
			},
			value={
				"security:[ { api_key:[] } ]"
			}
		)
	)
	public Map<PetStatus,Integer> getStoreInventory() throws NotAcceptable {
		return store.getInventory();
	}

	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Users
	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	@Override
	@RestMethod(
		name=GET,
		path="/user",
		summary="Petstore users",
		bpx="User: email,password,phone",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Collection<User> getUsers() throws NotAcceptable {
		return store.getUsers();
	}

	@Override
	@RestMethod(
		name=GET,
		path="/user/{username}",
		summary="Get user by user name",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public User getUser(String username) throws InvalidUsername, IdNotFound, NotAcceptable {
		return store.getUser(username);
	}

	@Override
	@RestMethod(
		name=POST,
		path="/user",
		summary="Create user",
		description="This can only be done by the logged in user.",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok createUser(User user) throws InvalidUsername, IdConflict, NotAcceptable, UnsupportedMediaType {
		store.create(user);
		return OK;
	}

	@Override
	@RestMethod(
		name=POST,
		path="/user/createWithArray",
		summary="Creates list of users with given input array",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok createUsers(User[] users) throws InvalidUsername, IdConflict, NotAcceptable, UnsupportedMediaType {
		for (User user : users)
			store.create(user);
		return OK;
	}

	@Override
	@RestMethod(
		name=PUT,
		path="/user/{username}",
		summary="Update user",
		description="This can only be done by the logged in user.",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok updateUser(String username, User user) throws InvalidUsername, IdNotFound, NotAcceptable, UnsupportedMediaType {
		store.update(user);
		return OK;
	}

	@Override
	@RestMethod(
		name=DELETE,
		path="/user/{username}",
		summary="Delete user",
		description="This can only be done by the logged in user.",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok deleteUser(String username) throws InvalidUsername, IdNotFound, NotAcceptable {
		store.removeUser(username);
		return OK;
	}

	@Override
	@RestMethod(
		name=GET,
		path="/user/login",
		summary="Logs user into the system",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok login(
			String username,
			String password,
			Value<Integer> rateLimit,
			Value<ExpiresAfter> expiresAfter
		) throws InvalidLogin, NotAcceptable {

		RestRequest req = getRequest();

		if (! store.isValid(username, password))
			throw new InvalidLogin();

		Date d = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
		req.getSession().setAttribute("login-expires", d);
		rateLimit.set(1000);
		expiresAfter.set(new ExpiresAfter(d));
		return OK;
	}

	@Override
	@RestMethod(
		name=GET,
		path="/user/logout",
		summary="Logs out current logged in user session",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok logout() throws NotAcceptable {
		getRequest().getSession().removeAttribute("login-expires");
		return OK;
	}
}