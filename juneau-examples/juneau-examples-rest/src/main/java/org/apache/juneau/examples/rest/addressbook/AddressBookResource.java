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
package org.apache.juneau.examples.rest.addressbook;

import static org.apache.juneau.html.HtmlDocSerializer.*;
import static org.apache.juneau.jena.RdfCommon.*;
import static org.apache.juneau.jena.RdfSerializer.*;
import static org.apache.juneau.http.HttpMethodName.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.LinkString;
import org.apache.juneau.dto.cognos.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.examples.addressbook.*;
import org.apache.juneau.examples.rest.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.rest.exception.*;
import org.apache.juneau.rest.helper.*;
import org.apache.juneau.rest.widget.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Proof-of-concept resource that shows off the capabilities of working with POJO resources.
 * <p>
 * Consists of an in-memory address book repository wrapped in a REST API.
 */
@RestResource(
	path="/addressBook",
	messages="nls/AddressBookResource",

	htmldoc=@HtmlDoc(
		
		// Widgets for $W variables.
		widgets={
			PoweredByJuneau.class,
			ContentTypeMenuItem.class,
			QueryMenuItem.class,
			ThemeMenuItem.class
		},

		// Links on the HTML rendition page.
		// "request:/..." URIs are relative to the request URI.
		// "servlet:/..." URIs are relative to the servlet URI.
		// "$C{...}" variables are pulled from the config file.
		navlinks={
			"up: request:/..",
			"options: servlet:/?method=OPTIONS",
			"$W{ContentTypeMenuItem}",
			"$W{ThemeMenuItem}",
			"source: $C{Source/gitHub}/org/apache/juneau/examples/rest/addressbook/$R{servletClassSimple}.java"
		},
		
		// Arbitrary HTML message on the left side of the page.
		aside={
			"<div style='max-width:400px;min-width:200px'>",
			"	<p>Proof-of-concept resource that shows off the capabilities of working with POJO resources.</p>",
			"	<p>Provides examples of: </p>",
			"	<ul>",
			"		<li>XML and RDF namespaces",
			"		<li>Swagger documentation",
			"		<li>Widgets",
			"	</ul>",
			"</div>"
		},
		
		// Juneau icon added to footer.
		footer="$W{PoweredByJuneau}"
	),

	// Allow INIT as a method parameter.
	allowedMethodParams="*",
	
	// Properties that get applied to all serializers and parsers.
	properties={

		// Use single quotes.
		@Property(name=WSERIALIZER_quoteChar, value="'"),

		// Enable XML namespaces.
		@Property(name=XML_enableNamespaces, value="true"),

		// Add namespace URIs to root node.
		@Property(name=XML_addNamespaceUrisToRoot, value="true"),
		
		// Make RDF/XML readable.
		@Property(name=RDF_rdfxml_tab, value="5"),

		// Make RDF parsable by adding a root node.
		@Property(name=RDF_addRootProperty, value="true"),

		// Make URIs absolute so that we can easily reference them on the client side.
		@Property(name=SERIALIZER_uriResolution, value="ABSOLUTE"),

		// Make the anchor text on URLs be just the path relative to the servlet.
		@Property(name=HTML_uriAnchorText, value="SERVLET_RELATIVE")
	},

	// Support GZIP encoding on Accept-Encoding header.
	encoders=GzipEncoder.class,

	// Swagger info.
	swagger=@ResourceSwagger(
		contact="name:'Juneau Developer',email:'dev@juneau.apache.org'",
		license="name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'",
		version="2.0",
		termsOfService="You are on your own.",
		externalDocs="description:'Apache Juneau',url:'http://juneau.apache.org'"
	)
)
public class AddressBookResource extends BasicRestServletJena {
	private static final long serialVersionUID = 1L;

	// The in-memory address book
	private AddressBook addressBook;

	@Override /* Servlet */
	public void init() {

		try {
			// Create the address book
			addressBook = new AddressBook(java.net.URI.create("servlet:/"));

			// Initialize it with some contents.
			addressBook.init();

		} catch (Exception e) {
			// Gets converted to 500
			throw new RuntimeException(e);
		}
	}

	/**
	 * [GET /]
	 * Get root page.
	 */
	@RestMethod(name=GET, path="/")
	public LinkString[] getRoot() throws Exception {
		return new LinkString[] {
			new LinkString("people", "people"),
			new LinkString("addresses", "addresses")
		};
	}

	/**
	 * [GET /people/*]
	 * Get all people in the address book.
	 * Traversable filtering enabled to allow nodes in returned POJO tree to be addressed.
	 * Introspectable filtering enabled to allow public methods on the returned object to be invoked.
	 */
	@RestMethod(name=GET, path="/people/*",
		converters={Traversable.class,Queryable.class,Introspectable.class},
		htmldoc=@HtmlDoc(
			navlinks={
				"INHERIT",  // Inherit links from class.
				"[2]:$W{QueryMenuItem}"  // Insert QUERY link in position 2.
			}
		)
	)
	public AddressBook getAllPeople() throws Exception {
		return addressBook;
	}

	/**
	 * [GET /people/{id}/*]
	 * Get a single person by ID.
	 * Traversable filtering enabled to allow nodes in returned POJO tree to be addressed.
	 * Introspectable filtering enabled to allow public methods on the returned object to be invoked.
	 */
	@RestMethod(name=GET, path="/people/{id}/*",
		converters={Traversable.class,Introspectable.class}
	)
	public Person getPerson(@Path int id) throws Exception {
		return findPerson(id);
	}

	/**
	 * [GET /addresses/*]
	 * Get all addresses in the address book.
	 */
	@RestMethod(name=GET, path="/addresses/*",
		converters={Traversable.class,Queryable.class},
		htmldoc=@HtmlDoc(
			navlinks={
				"INHERIT",  // Inherit links from class.
				"[2]:$W{QueryMenuItem}"  // Insert QUERY link in position 2.
			}
		)
	)
	public List<Address> getAllAddresses() throws Exception {
		return addressBook.getAddresses();
	}

	/**
	 * [GET /addresses/{id}/*]
	 * Get a single address by ID.
	 */
	@RestMethod(name=GET, path="/addresses/{id}/*",
		converters={Traversable.class}
	)
	public Address getAddress(@Path int id) throws Exception {
		return findAddress(id);
	}

	/**
	 * [POST /people]
	 * Create a new Person bean.
	 */
	@RestMethod(name=POST, path="/people",
		guards=AdminGuard.class
	)
	public Redirect createPerson(@Body CreatePerson cp) throws Exception {
		Person p = addressBook.createPerson(cp);
		return new Redirect("people/{0}", p.id);
	}

	/**
	 * [POST /people/{id}/addresses]
	 * Create a new Address bean.
	 */
	@RestMethod(name=POST, path="/people/{id}/addresses",
		guards=AdminGuard.class
	)
	public Redirect createAddress(@Path int id, @Body CreateAddress ca) throws Exception {
		Person p = findPerson(id);
		Address a = p.createAddress(ca);
		return new Redirect("addresses/{0}", a.id);
	}

	/**
	 * [DELETE /people/{id}]
	 * Delete a Person bean.
	 */
	@RestMethod(name=DELETE, path="/people/{id}",
		guards=AdminGuard.class
	)
	public String deletePerson(@Path int id) throws Exception {
		addressBook.removePerson(id);
		return "DELETE successful";
	}

	/**
	 * [DELETE /addresses/{id}]
	 * Delete an Address bean.
	 */
	@RestMethod(name=DELETE, path="/addresses/{id}",
		guards=AdminGuard.class
	)
	public String deleteAddress(@Path int addressId) throws NotFound {
		Person p = addressBook.findPersonWithAddress(addressId);
		if (p == null)
			throw new NotFound("Person not found");
		Address a = findAddress(addressId);
		p.addresses.remove(a);
		return "DELETE successful";
	}

	/**
	 * [PUT /people/{id}/*]
	 * Change property on Person bean.
	 */
	@RestMethod(name=PUT, path="/people/{id}/*",
		guards=AdminGuard.class
	)
	public String updatePerson(RequestBody body, @Path int id, @PathRemainder String remainder) throws BadRequest {
		try {
			Person p = findPerson(id);
			PojoRest r = new PojoRest(p);
			ClassMeta<?> cm = r.getClassMeta(remainder);
			Object in = body.asType(cm);
			r.put(remainder, in);
			return "PUT successful";
		} catch (Exception e) {
			throw new BadRequest(e, "PUT unsuccessful");
		}
	}

	/**
	 * [PUT /addresses/{id}/*]
	 * Change property on Address bean.
	 */
	@RestMethod(name=PUT, path="/addresses/{id}/*",
		guards=AdminGuard.class
	)
	public String updateAddress(RestRequest req, @Path int id, @PathRemainder String remainder) throws BadRequest {
		try {
			Address a = findAddress(id);
			PojoRest r = new PojoRest(a);
			ClassMeta<?> cm = r.getClassMeta(remainder);
			Object in = req.getBody().asType(cm);
			r.put(remainder, in);
			return "PUT successful";
		} catch (Exception e) {
			throw new BadRequest(e, "PUT unsuccessful");
		}
	}

	/**
	 * [INIT /]
	 * Reinitialize this resource.
	 */
	@RestMethod(name="INIT", path="/",
		guards=AdminGuard.class
	)
	public String doInit() throws Exception {
		init();
		return "OK";
	}

	/**
	 * [GET /cognos]
	 * Get data in Cognos/XML format
	 */
	@RestMethod(name=GET, path="/cognos")
	public DataSet getCognosData() throws Exception {

		// The Cognos metadata
		Column[] items = {
			new Column("name", "xs:String", 255),
			new Column("age", "xs:int"),
			new Column("numAddresses", "xs:int")
				.addPojoSwap(
					new PojoSwap<Person,Integer>() {
						@Override /* PojoSwap */
						public Integer swap(BeanSession session, Person p) {
							return p.addresses.size();
						}
					}
				)
		};

		return new DataSet(items, addressBook, this.getContext().getBeanContext().createSession());
	}

	/**
	 * [PROXY /*]
	 * Return a proxy interface to IAddressBook.
	 */
	@RestMethod(name=PROXY, path="/proxy/*")
	public IAddressBook getProxy() {
		return addressBook;
	}

	/** Convenience method - Find a person by ID */
	private Person findPerson(int id) throws NotFound {
		Person p = addressBook.findPerson(id);
		if (p == null)
			throw new NotFound("Person not found");
		return p;
	}

	/** Convenience method - Find an address by ID */
	private Address findAddress(int id) throws NotFound {
		Address a = addressBook.findAddress(id);
		if (a == null)
			throw new NotFound("Address not found");
		return a;
	}
}

