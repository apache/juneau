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

import java.util.*;

import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.labels.*;
import org.apache.juneau.rest.widget.*;

/**
 * Sample resource that shows how to generate ATOM feeds.
 */
@RestResource(
	path="/petstore2",
	title="Swagger Petstore",
	description=
		"This is a sample server Petstore server."
		+ "<br>You can find out more about Swagger at <a class='link' href='http://swagger.io'>http://swagger.io</a> or on <a class='link' href='http://swagger.io/irc'>irc.freenode.net#swagger</a>."
		+ "<br>For this sample, you can use the api key `special-key` to test the authorization filters.",
	htmldoc=@HtmlDoc(
		widgets={
			ContentTypeMenuItem.class,
			StyleMenuItem.class
		},
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{StyleMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/petstore/$R{servletClassSimple}.java"
		}
	),
	swagger="$F{PetStoreResource.json}"
)
public class PetStoreResource extends BasicRestServletJena {
	private static final long serialVersionUID = 1L;
	
	@RestMethod(
		name="GET", 
		path="/",
		summary="Top-level page"
	) 
	public ResourceDescription[] getTopPage() {
		return new ResourceDescription[] {
			new ResourceDescription("pet", "All pets in the store"), 
			new ResourceDescription("store", "Petstore orders"), 
			new ResourceDescription("user", "Petstore users")
		};
	}
	
	@RestMethod(
		name="GET",
		path="/pet",
		summary="All pets in the store",
		swagger={
			"tags:['pet']"
		}
	)
	public Collection<Pet> getPets() {
		return null;
	}
	
	@RestMethod(
		name="GET",
		path="/store",
		summary="Petstore orders",
		swagger={
			"tags:['store']"
		}
	)
	public Collection<Order> getOrders() {
		return null;
	}

	@RestMethod(
		name="GET",
		path="/user",
		summary="Petstore users",
		swagger={
			"tags:['user']"
		}
	)
	public Collection<Order> getUsers() {
		return null;
	}

	@RestMethod(
		name="POST", 
		path="/pet",
		summary="Add a new pet to the store",
		swagger={
			"tags:['pet'],",
			"parameters:[",
				"{ in:'body', description:'Pet object that needs to be added to the store', required:true }",
			"],",
			"responses:{",
				"405: { description:'Invalid input' }",
			"},",
			"security:[",
				"{ petstore_auth:['write:pets','read:pets'] }",
			"]"
		}
	)
	public void addPet(@Body Pet pet) {
	}
	
	@RestMethod(
		name="PUT", 
		path="/pet/{petId}",
		summary="Update an existing pet",
		swagger={
			"tags:['pet'],",
			"parameters:[",
//				"{ in:'body', description:'Pet object that needs to be added to the store', required:true, schema:{ $ref:'#/definitions/Pet'} }",
				"{ in:'body', description:'Pet object that needs to be added to the store', required:true }",
			"],",
			"responses:{",
				"400:{ description:'Invalid ID supplied' },",
				"404:{ description:'Pet not found' },",
				"405:{ description:'Validation exception' }",
			"},",
			"security:[ { petstore_auth: ['write:pets','read:pets'] } ]",
		}
	)
	public void updatePet(@Body Pet pet) {}

	@RestMethod(
		name="GET", 
		path="/pet/findByStatus",
		summary="Finds Pets by status",
		description="Multiple status values can be provided with comma separated strings.",
		swagger={
			"tags:['pet'],",
			"parameters: [",
				"{",
					"name:'status', in:'query', description:'Status values that need to be considered for filter', required:true, type:'array',",
					"items:{ type:'string', enum:[ 'available','pending','sold' ], default:'available' },",
					"collectionFormat:'multi'",
				"}",
			"],",
			"responses: {",
				"200:{ description:'successful operation', schema:{ type:'array', items:{ $ref:'#/definitions/Pet' } } },",
				"400:{ description':'Invalid status value' }",
			"},",
			"security:[",
				"{ petstore_auth:[ 'write:pets','read:pets' ] }",
			"]"
		}
	)
	public List<Pet> findByStatus(@Query("status") PetStatus[] status) {
		return null;
	}
	
	@RestMethod(
		name="GET", 
		path="/pet/findByTags",
		summary="Finds Pets by tags",
		description="Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.",
		swagger={
			"tags:['pet'],",
			"parameters:[",
				"{ name:'tags', in:'query', description:'Tags to filter by', required:true, type:'array', items:{ type:'string' }, collectionFormat:'multi' }",
			"],",
			"responses:{",
				"200:{ description:'successful operation', schema:{ type:'array', items:{ $ref:'#/definitions/Pet' } } },",
				"400:{ description:'Invalid tag value' }",
			"},",
			"security:[ { petstore_auth:[ 'write:pets','read:pets' ] } ],",
			"deprecated:true"
		}
	)
	public List<Pet> findByTags(@Query("tags") String[] tags) {
		return null;
	}

	@RestMethod(
		name="GET", 
		path="/pet/{petId}",
		summary="Find pet by ID",
		description="Returns a single pet.",
		swagger={
			"tags:[ 'pet' ],",
			"parameters:[",
				"{ name:'petId', in:'path', description:'ID of pet to return', required:true, type:'integer', format:'int64' }",
			"],",
			"responses:{",
				"200:{ description:'successful operation', schema:{ $ref:'#/definitions/Pet' } },",
				"400:{ description:'Invalid ID supplied' },",
				"404:{ description:'Pet not found' }",
			"},",
			"security:[ { api_key:[] } ]"
		}
	)
	public Pet getPet(@Path long petId) {
		return null;
	}
	
	@RestMethod(
		name="POST", 
		path="/pet/{petId}",
		summary="Updates a pet in the store with form data",
		swagger={
			"tags:[ 'pet' ],",
			"parameters:[",
				"{ name:'petId', in:'path', description:'ID of pet that needs to be updated', required:true, type:'integer', format:'int64' },",
				"{ name:'name', in:'formData', description:'Updated name of the pet', required:false, type:'string'},",
				"{ name:'status', in:'formData', description:'Updated status of the pet', required:false, type:'string' }",
			"],",
			"responses:{",
				"405:{ description:'Invalid input' }",
			"},",
			"security:[ { petstore_auth:[ 'write:pets', 'read:pets' ] } ]"
		}
	)
	public void updatePetForm(@Path long petId, @FormData("name") String name, @FormData("status") String status) {}

	@RestMethod(
		name="DELETE", 
		path="/pet/{petId}",
		summary="Deletes a pet",
		swagger={
			"tags:[ 'pet' ],",
			"parameters:[",
				"{ name:'api_key', in:'header', required:false, type:'string' },",
				"{ name:'petId', in:'path', description:'Pet id to delete', required:true, type:'integer', format:'int64' }",
			"],",
			"responses:{",
				"400:{ description:'Invalid ID supplied' },",
				"404:{ description:'Pet not found' }",
			"},",
			"security:[ { petstore_auth:[ 'write:pets','read:pets' ] } ]"
		}
	)
	public void deletePet(@Header("api_key") String apiKey, @Path long petId) {}

	@RestMethod(
		name="POST", 
		path="/pet/{petId}/uploadImage",
		summary="Uploads an image",
		swagger={
			"tags:[ 'pet' ],",
			"parameters:[",
				"{ name:'petId', in:'path', description:'ID of pet to update', required:true, type:'integer', format:'int64' },",
				"{ name:'additionalMetadata', in:'formData', description:'Additional data to pass to server', required:false, type:'string' },",
				"{ name:'file', in:'formData', description:'file to upload', required:false, type:'file' }",
			"],",
			"responses:{",
				"200:{ description:'successful operation', schema:{ $ref:'#/definitions/ApiResponse' } }",
			"},",
			"security:[ { petstore_auth:[ 'write:pets','read:pets' ] } ]"
		}
	)
	public void uploadImage(@Path long petId, @FormData("additionalMetadata") String additionalMetadata, @FormData("file") byte[] file) {}

	@RestMethod(
		name="GET", 
		path="/store/inventory",
		summary="Returns pet inventories by status",
		description="Returns a map of status codes to quantities",
		swagger={
			"tags:[ 'store' ],",
			"responses:{",
				"200:{ description:'successful operation', schema:{ type:'object', additionalProperties:{ type:'integer', format:'int32' } } }",
			"},",
			"security:[ { api_key:[] } ]"
		}
	)
	public void getStoreInventory() {}

	@RestMethod(
		name="POST", 
		path="/store/order",
		summary="Place an order for a pet",
		swagger={
			"tags:[ 'store' ],",
			"parameters:[",
				"{ in:'body', name:'body', description:'order placed for purchasing the pet', required:true, schema:{ $ref:'#/definitions/Order' } }",
			"],",
			"responses:{",
				"200:{ description:'successful operation', schema:{ $ref:'#/definitions/Order' } },",
				"400:{ description:'Invalid Order' }",
			"}"
		}
	)
	public Order placeOrder(@Body Order order) {
		return order;
	}

	@RestMethod(
		name="GET", 
		path="/store/order/{orderId}",
		summary="Find purchase order by ID",
		description="For valid response try integer IDs with value >= 1 and <= 10. Other values will generated exceptions",
		swagger={
			"tags:[ 'store' ],",
			"parameters:[",
				"{ name:'orderId', in:'path', description:'ID of pet that needs to be fetched', required:true, type:'integer', maximum:10.0, minimum:1.0, format:'int64' }",
			"],",
			"responses:{",
				"200:{ description:'successful operation', schema:{ $ref:'#/definitions/Order' } },",
				"400:{ description:'Invalid ID supplied' },",
				"404:{ description:'Order not found' }",
			"}"
		}
	)
	public Order findPurchaseOrder(@Path String orderId) {
		return null;
	}

	@RestMethod(
		name="DELETE", 
		path="/store/order/{orderId}",
		summary="Delete purchase order by ID",
		description="For valid response try integer IDs with positive integer value. Negative or non-integer values will generate API errors.",
		swagger={
			"tags:[ 'store' ],",
			"parameters:[",
				"{ name:'orderId', in:'path', description:'ID of the order that needs to be deleted', required:true, type:'integer', minimum:1.0, format:'int64' }",
			"],",
			"responses:{",
				"400:{ description:'Invalid ID supplied' },",
				"404:{ description:'Order not found' }",
			"}"
		}
	)
	public void deletePurchaseOrder(@Path String orderId) {}

	@RestMethod(
		name="POST", 
		path="/user",
		summary="Create user",
		description="This can only be done by the logged in user.",
		swagger={
			"tags:[ 'user' ],",
			"parameters:[",
				"{ in:'body', name:'body', description:'Created user object', required:true, schema:{ $ref:'#/definitions/User' } }",
			"],",
			"responses:{",
				"default:{ description:'successful operation' }",
			"}"
		}
	)
	public void createUser(@Body User user) {}

	@RestMethod(
		name="POST", 
		path="/user/createWithArray",
		summary="Creates list of users with given input array",
		swagger={
			"tags:[ 'user' ],",
			"parameters:[",
				"{ in:'body', name:'body', description:'List of user object', required:true, schema:{ type:'array', items:{ $ref:'#/definitions/User' } } }",
			"],",
			"responses:{",
				"default:{ description:'successful operation' }",
			"}"
		}
	)
	public void createUsersWithArrayInput(@Body User[] users) {}

	@RestMethod(
		name="POST", 
		path="/user/createWithList",
		summary="Creates list of users with given input array",
		description="This can only be done by the logged in user.",
		swagger={
			"tags:[ 'user' ],",
			"operationId:'createUsersWithListInput',",
			"parameters:[",
				"{ in:'body', name:'body', description:'List of user object', required:true, schema:{ type:'array', items:{ $ref:'#/definitions/User' } } }",
			"],",
			"responses:{",
				"default:{ description:'successful operation' }",
			"}"
		}
	)
	public void createUsersWithListInput(@Body List<User> users) {}

	@RestMethod(
		name="GET", 
		path="/user/login",
		summary="Logs user into the system",
		swagger={
			"tags:[ 'user' ],",
			"parameters:[",
				"{ name:'username', in:'query', description:'The user name for login', required:true, type:'string' },",
				"{ name:'password', in:'query', description:'The password for login in clear text', required:true, type:'string' }",
			"],",
			"responses:{",
				"200:{",
					"description:'successful operation', schema:{ type:'string' },",
					"headers:{",
						"X-Rate-Limit:{ type:'integer', format:'int32', description:'calls per hour allowed by the user' },",
						"X-Expires-After:{ type:'string', format:'date-time', description:'date in UTC when token expires' }",
					"}",
				"},",
				"400:{ description:'Invalid username/password supplied' }",
			"}"
		}
	)
	public void login(@Query("username") String username, @Query("password") String password) {}

	@RestMethod(
		name="GET", 
		path="/user/logout",
		summary="Logs out current logged in user session",
		swagger={
			"tags:[ 'user' ],",
			"responses:{",
				"default:{ description:'successful operation' }",
			"}"
		}
	)
	public void logout() {}

	@RestMethod(
		name="GET", 
		path="/user/{username}",
		summary="Get user by user name",
		swagger={
			"tags:[ 'user' ],",
			"parameters:[",
				"{ name:'username', in:'path', description:'The name that needs to be fetched. Use user1 for testing. ', required:true, type:'string' }",
			"],",
			"responses:{",
				"200:{ description:'successful operation', schema:{ $ref:'#/definitions/User' } },",
				"400:{ description:'Invalid username supplied' },",
				"404:{ description:'User not found' }",
			"}"
		}
	)
	public User getUser(@Path String username) {
		return null;
	}

	@RestMethod(
		name="PUT", 
		path="/user/{username}",
		summary="Update user",
		description="This can only be done by the logged in user.",
		swagger={
			"tags:[ 'user' ],",
			"parameters:[",
				"{ name:'username', in:'path', description:'name that need to be updated', required:true, type:'string' },",
				"{ in:'body', name:'body', description:'Updated user object', required:true, schema:{ $ref:'#/definitions/User' } }",
			"],",
			"responses:{",
				"400:{ description:'Invalid user supplied' },",
				"404:{ description:'User not found' }",
			"}"
		}
	)
	public void updateUser(@Path String username, @Body User user) {}

	@RestMethod(
		name="DELETE", 
		path="/user/{username}",
		summary="Delete user",
		description="This can only be done by the logged in user.",
		swagger={
			"tags:[ 'user' ],",
			"parameters:[",
				"{ name:'username', in:'path', description:'The name that needs to be deleted', required:true, type:'string' }",
			"],",
			"responses:{",
				"400:{ description:'Invalid username supplied' },",
				"404:{ description:'User not found' }",
			"}"
		}
	)
	public void deleteUser(@Path String username) {}
}