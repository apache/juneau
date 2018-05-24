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
package org.apache.juneau.examples.rest.petstore;

import static org.apache.juneau.dto.html5.HtmlBuilder.*;
import static org.apache.juneau.dto.swagger.ui.SwaggerUI.*;
import static org.apache.juneau.http.HttpMethodName.*;
import static org.apache.juneau.rest.annotation.HookEvent.*;
import static org.apache.juneau.rest.helper.Ok.*;

import java.util.*;
import java.util.Map;

import org.apache.juneau.dto.html5.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.annotation.Body;
import org.apache.juneau.rest.annotation.Header;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.utils.*;
import org.apache.juneau.rest.converters.*;

/**
 * Sample resource that shows how to generate ATOM feeds.
 */
@RestResource(
	path="/petstore",
	title="Petstore application",
	description= {
		"This is a sample server Petstore server based on the Petstore sample at Swagger.io.",
		"You can find out more about Swagger at <a class='link' href='http://swagger.io'>http://swagger.io</a>.",
	},
	htmldoc=@HtmlDoc(
		widgets={
			ContentTypeMenuItem.class,
			ThemeMenuItem.class,
		},
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{ThemeMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/petstore/$R{servletClassSimple}.java"
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
		}
	),
	properties= {
		// Resolve recursive references when showing schema info in the swagger.
		@Property(name=SWAGGERUI_resolveRefsMaxDepth, value="99")
	},
	swagger=@ResourceSwagger("$F{PetStoreResource.json}"),
	staticFiles={"htdocs:htdocs"}
)
public class PetStoreResource extends BasicRestServletJena {
	private static final long serialVersionUID = 1L;
	
	private PetStore store;
	
	@RestHook(INIT) 
	public void initDatabase(RestContextBuilder builder) throws Exception {
		store = new PetStore().init();
	}

	@RestMethod(
		name="GET", 
		path="/",
		summary="Navigation page"
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
	
	@RestMethod(
		name="GET",
		path="/pet",
		summary="All pets in the store",
		swagger=@MethodSwagger(
			tags="pet",
			parameters={
				Queryable.SWAGGER_PARAMS
			}
		),
		bpx="Pet: tags",
		htmldoc=@HtmlDoc(
			widgets={
				QueryMenuItem.class,
				AddPetMenuItem.class
			},
			navlinks={
				"INHERIT",                // Inherit links from class.
				"[2]:$W{QueryMenuItem}",  // Insert QUERY link in position 2.
				"[3]:$W{AddPetMenuItem}"  // Insert ADD link in position 3.
			}
		),
		converters={Queryable.class}
	)
	public Collection<Pet> getPets() throws NotAcceptable {
		return store.getPets();
	}
	
	@RestMethod(
		name="GET", 
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
	public Pet getPet(
			@Path(name="petId", description="ID of pet to return", example="123") long petId
		) throws IdNotFound, NotAcceptable {
		
		return store.getPet(petId);
	}
	
	@RestMethod(
		name="POST", 
		path="/pet",
		summary="Add a new pet to the store",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth:['write:pets','read:pets'] } ]"
			}
		)
	)
	public Ok addPet(
			@Body(description="Pet object that needs to be added to the store") PetCreate pet
		) throws IdConflict, NotAcceptable, UnsupportedMediaType {
		
		JsonSerializer.DEFAULT_LAX_READABLE.println(pet);
		store.create(pet);
		return OK;
	}
	
	@RestMethod(
		name="PUT", 
		path="/pet/{petId}",
		summary="Update an existing pet",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth: ['write:pets','read:pets'] } ]"
			}
		)
	)
	public Ok updatePet(
			@Body(description="Pet object that needs to be added to the store") PetUpdate pet
		) throws IdNotFound, NotAcceptable, UnsupportedMediaType {
		
		store.update(pet);
		return OK;
	}

	@RestMethod(
		name="GET", 
		path="/pet/{petId}/edit",
		summary="Pet edit page",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth:['write:pets','read:pets'] } ]"
			}
		)
	)
	public Div editPetPage(
			@Path(name="petId", description="ID of pet to return", example="123") long petId
		) throws IdConflict, NotAcceptable, UnsupportedMediaType {
		
		Pet pet = getPet(petId);
		
		return div(
			form().id("form").action("servlet:/pet/" + petId).method(POST).children(
				table(
					tr(
						th("Id:"),
						td(input().name("id").type("text").value(petId).readonly(true)),
						td(new Tooltip("(?)", "The name of the pet.", br(), "e.g. 'Fluffy'")) 
					),
					tr(
						th("Name:"),
						td(input().name("name").type("text").value(pet.getName())),
						td(new Tooltip("(?)", "The name of the pet.", br(), "e.g. 'Fluffy'")) 
					),
					tr(
						th("Species:"),
						td(
							select().name("species").children(
								option("cat"), option("dog"), option("bird"), option("fish"), option("mouse"), option("rabbit"), option("snake")
							).choose(pet.getSpecies())
						),
						td(new Tooltip("(?)", "The kind of animal.")) 
					),
					tr(
						th("Price:"),
						td(input().name("price").type("number").placeholder("1.0").step("0.01").min(1).max(100).value(pet.getPrice())),
						td(new Tooltip("(?)", "The price to charge for this pet.")) 
					),
					tr(
						th("Tags:"),
						td(input().name("tags").type("text").value(Tag.asString(pet.getTags()))),
						td(new Tooltip("(?)", "Arbitrary textual tags (comma-delimited).", br(), "e.g. 'fluffy,friendly'")) 
					),
					tr(
						th("Status:"),
						td(
							select().name("status").children(
								option("AVAILABLE"), option("PENDING"), option("SOLD")
							).choose(pet.getStatus())
						),
						td(new Tooltip("(?)", "The current status of the animal.")) 
					),
					tr(
						td().colspan(2).style("text-align:right").children(
							button("reset", "Reset"),
							button("button","Cancel").onclick("window.location.href='/'"),
							button("submit", "Submit")
						)
					)
				).style("white-space:nowrap")
			)
		);
	}

	@RestMethod(
		name="GET", 
		path="/pet/findByStatus",
		summary="Finds Pets by status",
		description="Multiple status values can be provided with comma separated strings.",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[{ petstore_auth:[ 'write:pets','read:pets' ] } ]"
			}
		)
	)
	public Collection<Pet> findPetsByStatus(
			@Query(
				name="status", 
				description="Status values that need to be considered for filter", 
				required="true", 
				example="['AVAILABLE','PENDING']"
			) 
			PetStatus[] status
		) throws NotAcceptable {
		
		return store.getPetsByStatus(status);
	}
	
	@RestMethod(
		name="GET", 
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
	public Collection<Pet> findPetsByTags(
			@Query(
				name="tags", 
				description="Tags to filter by", 
				required="true", 
				example="['tag1','tag2']"
			) 
			String[] tags
		) throws InvalidTag, NotAcceptable {
		
		return store.getPetsByTags(tags);
	}

	@RestMethod(
		name="DELETE", 
		path="/pet/{petId}",
		summary="Deletes a pet",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth:[ 'write:pets','read:pets' ] } ]"
			}
		)
	)
	public Ok deletePet(
			@Header(name="api_key", example="foobar") String apiKey, 
			@Path(name="petId", description="Pet id to delete", example="123") long petId
		) throws IdNotFound, NotAcceptable {
		
		store.removePet(petId);
		return OK;
	}

	@RestMethod(
		name="POST", 
		path="/pet/{petId}/uploadImage",
		summary="Uploads an image",
		swagger=@MethodSwagger(
			tags="pet",
			value={
				"security:[ { petstore_auth:[ 'write:pets','read:pets' ] } ]"
			}
		)
	)
	public Ok uploadImage(
			@Path(name="petId", description="ID of pet to update", example="123") long petId, 
			@FormData(name="additionalMetadata", description="Additional data to pass to server", example="Foobar") String additionalMetadata, 
			@FormData(name="file", description="file to upload", required="true", type="file") byte[] file
		) throws NotAcceptable, UnsupportedMediaType {
		
		return OK;
	}

	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------
	// Orders
	//-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------

	@RestMethod(
		name="GET", 
		path="/store",
		summary="Store navigation page",
		swagger=@MethodSwagger(
			tags="store"
		)
	) 
	public ResourceDescriptions getTopStorePage() {
		return new ResourceDescriptions()
			.append("store/order", "Petstore orders")
			.append("store/inventory", "Petstore inventory")
		;
	}

	@RestMethod(
		name="GET",
		path="/store/order",
		summary="Petstore orders",
		swagger=@MethodSwagger(
			tags="store"
		),
		htmldoc=@HtmlDoc(
			widgets={
				QueryMenuItem.class,
				AddOrderMenuItem.class
			},
			navlinks={
				"INHERIT",                // Inherit links from class.
				"[2]:$W{QueryMenuItem}",  // Insert QUERY link in position 2.
				"[3]:$W{AddOrderMenuItem}"  // Insert ADD link in position 3.
			}
		)
	) 
	public Collection<Order> getOrders() throws NotAcceptable {
		return store.getOrders();
	}

	@RestMethod(
		name="GET", 
		path="/store/order/{orderId}",
		summary="Find purchase order by ID",
		description="Returns a purchase order by ID.",
		swagger=@MethodSwagger(
			tags="store"
		)
	)
	public Order getOrder(
			@Path(name="orderId", description="ID of order to fetch", maximum="1000", minimum="101", example="123") long orderId
		) throws InvalidId, IdNotFound, NotAcceptable {
		
		if (orderId < 101 || orderId > 1000)
			throw new InvalidId();
		return store.getOrder(orderId);
	}
	
	@RestMethod(
		name="POST", 
		path="/store/order",
		summary="Place an order for a pet",
		swagger=@MethodSwagger(
			tags="store"
		),
		pojoSwaps={
			DateSwap.ISO8601D.class
		}
	)
	public Order placeOrder(
			@FormData(name="petId", description="Pet ID") long petId,
			@FormData(name="shipDate", description="Ship date") Date shipDate
		) throws IdConflict, NotAcceptable, UnsupportedMediaType {
		
		CreateOrder co = new CreateOrder(petId, shipDate);
		return store.create(co);
	}

	@RestMethod(
		name="DELETE", 
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
	public Ok deletePurchaseOrder(
			@Path(name="orderId", description="ID of the order that needs to be deleted", minimum="1", example="5") long orderId
		) throws InvalidId, IdNotFound, NotAcceptable {
		
		if (orderId < 0)
			throw new InvalidId();
		store.removeOrder(orderId);
		return OK;
	}

	@RestMethod(
		name="GET", 
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

	@RestMethod(
		name="GET",
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

	@RestMethod(
		name="GET", 
		path="/user/{username}",
		summary="Get user by user name",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public User getUser(
			@Path(name="username", description="The name that needs to be fetched. Use user1 for testing.") String username
		) throws InvalidUsername, IdNotFound, NotAcceptable {
		
		return store.getUser(username);
	}
	
	@RestMethod(
		name="POST", 
		path="/user",
		summary="Create user",
		description="This can only be done by the logged in user.",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok createUser(
			@Body(description="Created user object") User user
		) throws InvalidUsername, IdConflict, NotAcceptable, UnsupportedMediaType {
		
		store.add(user);
		return OK;
	}

	@RestMethod(
		name="POST", 
		path="/user/createWithArray",
		summary="Creates list of users with given input array",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok createUsers(
			@Body(description="List of user objects") User[] users
		) throws InvalidUsername, IdConflict, NotAcceptable, UnsupportedMediaType {
		
		for (User user : users)
			store.add(user);
		return OK;
	}

	@RestMethod(
		name="PUT", 
		path="/user/{username}",
		summary="Update user",
		description="This can only be done by the logged in user.",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok updateUser(
			@Path(name="username", description="Name that need to be updated") String username, 
			@Body(description="Updated user object") User user
		) throws InvalidUsername, IdNotFound, NotAcceptable, UnsupportedMediaType {
		
		store.update(user);
		return OK;
	}

	@RestMethod(
		name="DELETE", 
		path="/user/{username}",
		summary="Delete user",
		description="This can only be done by the logged in user.",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok deleteUser(
			@Path(name="username", description="The name that needs to be deleted") String username
		) throws InvalidUsername, IdNotFound, NotAcceptable {
		
		store.removeUser(username);
		return OK;
	}
	
	@RestMethod(
		name="GET", 
		path="/user/login",
		summary="Logs user into the system",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok login(
			@Query(name="username", description="The username for login", required="true", example="myuser") String username, 
			@Query(name="password", description="The password for login in clear text", required="true", example="abc123") String password, 
			@ResponseHeader(name="X-Rate-Limit", type="integer", format="int32", description="Calls per hour allowed by the user.", example="123") Value<Integer> rateLimit,
			ExpiresAfter expiresAfter,
			RestRequest req, 
			RestResponse res
		) throws InvalidLogin, NotAcceptable {
		
		if (! store.isValid(username, password))
			throw new InvalidLogin();
		
		Date d = new Date(System.currentTimeMillis() + 30 * 60 * 1000);
		req.getSession().setAttribute("login-expires", d);
		rateLimit.set(1000);
		expiresAfter.set(DateUtils.formatDate(d));
		return OK;
	}
	
	@ResponseHeader(name="X-Expires-After", type="string", format="date-time", description="Date in UTC when token expires", example="20120-10-21")
	public static class ExpiresAfter extends Value<String> {}

	@RestMethod(
		name="GET", 
		path="/user/logout",
		summary="Logs out current logged in user session",
		swagger=@MethodSwagger(
			tags="user"
		)
	)
	public Ok logout(RestRequest req) throws NotAcceptable {
		req.getSession().removeAttribute("login-expires");
		return OK;
	}
}