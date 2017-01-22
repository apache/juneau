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

import static javax.servlet.http.HttpServletResponse.*;
import static org.apache.juneau.examples.addressbook.AddressBook.*;
import static org.apache.juneau.html.HtmlDocSerializerContext.*;
import static org.apache.juneau.jena.RdfCommonContext.*;
import static org.apache.juneau.jena.RdfSerializerContext.*;
import static org.apache.juneau.rest.RestServletContext.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.dto.*;
import org.apache.juneau.dto.cognos.*;
import org.apache.juneau.dto.swagger.*;
import org.apache.juneau.encoders.*;
import org.apache.juneau.examples.addressbook.*;
import org.apache.juneau.examples.rest.*;
import org.apache.juneau.microservice.*;
import org.apache.juneau.rest.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.converters.*;
import org.apache.juneau.transform.*;
import org.apache.juneau.utils.*;

/**
 * Proof-of-concept resource that shows off the capabilities of working with POJO resources.
 * Consists of an in-memory address book repository.
 */
@RestResource(
	path="/addressBook",
	messages="nls/AddressBookResource",
	properties={
		@Property(name=REST_allowMethodParam, value="*"),
		@Property(name=HTML_uriAnchorText, value=TO_STRING),
		@Property(name=SERIALIZER_quoteChar, value="'"),
		@Property(name=RDF_rdfxml_tab, value="5"),
		@Property(name=RDF_addRootProperty, value="true"),
		@Property(name=HTMLDOC_links, value="{up:'$R{requestParentURI}',options:'$R{servletURI}?method=OPTIONS',source:'$R{servletParentURI}/source?classes=(org.apache.juneau.examples.rest.addressbook.AddressBookResource,org.apache.juneau.examples.addressbook.Address,org.apache.juneau.examples.addressbook.AddressBook,org.apache.juneau.examples.addressbook.CreateAddress,org.apache.juneau.examples.addressbook.CreatePerson,org.apache.juneau.examples.addressbook.IAddressBook,org.apache.juneau.examples.addressbook.Person)'}"),
		// Resolve all relative URIs so that they're relative to this servlet!
		@Property(name=SERIALIZER_relativeUriBase, value="$R{servletURI}"),
	},
	stylesheet="styles/devops.css",
	encoders=GzipEncoder.class,
	contact="{name:'John Smith',email:'john@smith.com'}",
	license="{name:'Apache 2.0',url:'http://www.apache.org/licenses/LICENSE-2.0.html'}",
	version="2.0",
	termsOfService="You're on your own.",
	tags="[{name:'Java',description:'Java utility',externalDocs:{description:'Home page',url:'http://juneau.apache.org'}}]",
	externalDocs="{description:'Home page',url:'http://juneau.apache.org'}"
)
public class AddressBookResource extends ResourceJena {
	private static final long serialVersionUID = 1L;

	// The in-memory address book
	private AddressBook addressBook;

	@Override /* Servlet */
	public void init() {

		try {
			// Create the address book
			addressBook = new AddressBook(java.net.URI.create(""));

			// Add some people to our address book by default
			addressBook.createPerson(
				new CreatePerson(
					"Barack Obama",
					toCalendar("Aug 4, 1961"),
					new CreateAddress("1600 Pennsylvania Ave", "Washington", "DC", 20500, true),
					new CreateAddress("5046 S Greenwood Ave", "Chicago", "IL", 60615, false)
				)
			);
			addressBook.createPerson(
				new CreatePerson(
					"George Walker Bush",
					toCalendar("Jul 6, 1946"),
					new CreateAddress("43 Prairie Chapel Rd", "Crawford", "TX", 76638, true),
					new CreateAddress("1600 Pennsylvania Ave", "Washington", "DC", 20500, false)
				)
			);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * [GET /]
	 * Get root page.
	 */
	@RestMethod(name="GET", path="/",
		converters=Queryable.class
	)
	public Link[] getRoot() throws Exception {
		return new Link[] {
			new Link("people", "people"),
			new Link("addresses", "addresses")
		};
	}

	/**
	 * [GET /people/*]
	 * Get all people in the address book.
	 * Traversable filtering enabled to allow nodes in returned POJO tree to be addressed.
	 * Introspectable filtering enabled to allow public methods on the returned object to be invoked.
	 */
	@RestMethod(name="GET", path="/people/*",
		converters={Traversable.class,Queryable.class,Introspectable.class}
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
	@RestMethod(name="GET", path="/people/{id}/*",
		converters={Traversable.class,Queryable.class,Introspectable.class}
	)
	public Person getPerson(@Path int id) throws Exception {
		return findPerson(id);
	}

	/**
	 * [GET /addresses/*]
	 * Get all addresses in the address book.
	 */
	@RestMethod(name="GET", path="/addresses/*",
		converters={Traversable.class,Queryable.class}
	)
	public List<Address> getAllAddresses() throws Exception {
		return addressBook.getAddresses();
	}

	/**
	 * [GET /addresses/{id}/*]
	 * Get a single address by ID.
	 */
	@RestMethod(name="GET", path="/addresses/{id}/*",
		converters={Traversable.class,Queryable.class}
	)
	public Address getAddress(@Path int id) throws Exception {
		return findAddress(id);
	}

	/**
	 * [POST /people]
	 * Create a new Person bean.
	 */
	@RestMethod(name="POST", path="/people",
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
	@RestMethod(name="POST", path="/people/{id}/addresses",
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
	@RestMethod(name="DELETE", path="/people/{id}",
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
	@RestMethod(name="DELETE", path="/addresses/{id}",
		guards=AdminGuard.class
	)
	public String deleteAddress(@Path int addressId) throws Exception {
		Person p = addressBook.findPersonWithAddress(addressId);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Person not found");
		Address a = findAddress(addressId);
		p.addresses.remove(a);
		return "DELETE successful";
	}

	/**
	 * [PUT /people/{id}/*]
	 * Change property on Person bean.
	 */
	@RestMethod(name="PUT", path="/people/{id}/*",
		guards=AdminGuard.class
	)
	public String updatePerson(RestRequest req, @Path int id) throws Exception {
		try {
			Person p = findPerson(id);
			String pathRemainder = req.getPathRemainder();
			PojoRest r = new PojoRest(p);
			ClassMeta<?> cm = r.getClassMeta(pathRemainder);
			Object in = req.getBody(cm);
			r.put(pathRemainder, in);
			return "PUT successful";
		} catch (Exception e) {
			throw new RestException(SC_BAD_REQUEST, "PUT unsuccessful").initCause(e);
		}
	}

	/**
	 * [PUT /addresses/{id}/*]
	 * Change property on Address bean.
	 */
	@RestMethod(name="PUT", path="/addresses/{id}/*",
		guards=AdminGuard.class
	)
	public String updateAddress(RestRequest req, @Path int id) throws Exception {
		try {
			Address a = findAddress(id);
			String pathInfo = req.getPathInfo();
			PojoRest r = new PojoRest(a);
			ClassMeta<?> cm = r.getClassMeta(pathInfo);
			Object in = req.getBody(cm);
			r.put(pathInfo, in);
			return "PUT successful";
		} catch (Exception e) {
			throw new RestException(SC_BAD_REQUEST, "PUT unsuccessful").initCause(e);
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
	@RestMethod(name="GET", path="/cognos")
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

		return new DataSet(items, addressBook, this.getBeanContext().createSession());
	}

	/**
	 * [OPTIONS /*]
	 * View resource options
	 */
	@Override /* RestServletJenaDefault */
	@RestMethod(name="OPTIONS", path="/*")
	public Swagger getOptions(RestRequest req) {
		return req.getSwagger();
	}

	/** Convenience method - Find a person by ID */
	private Person findPerson(int id) throws RestException {
		Person p = addressBook.findPerson(id);
		if (p == null)
			throw new RestException(SC_NOT_FOUND, "Person not found");
		return p;
	}

	/** Convenience method - Find an address by ID */
	private Address findAddress(int id) throws RestException {
		Address a = addressBook.findAddress(id);
		if (a == null)
			throw new RestException(SC_NOT_FOUND, "Address not found");
		return a;
	}
}

