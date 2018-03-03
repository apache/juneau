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
package org.apache.juneau.examples.addressbook;

import java.net.URI;
import java.text.*;
import java.util.*;

import org.apache.juneau.annotation.*;

/**
 * Address book bean
 */
@Bean(typeName="addressBook")
public class AddressBook extends LinkedList<Person> implements IAddressBook {
	private static final long serialVersionUID = 1L;

	// The URL of this resource
	private URI uri;

	/** Bean constructor - Needed for instantiating on server side */
	public AddressBook() {}

	/** Bean constructor - Needed for instantiating on client side */
	public AddressBook(URI uri) throws Exception {
		this.uri = uri;
	}

	@Override /* IAddressBook */
	public void init() {
		clear();
		try {
			createPerson(
				new CreatePerson(
					"Barack Obama",
					toCalendar("Aug 4, 1961"),
					new CreateAddress("1600 Pennsylvania Ave", "Washington", "DC", 20500, true),
					new CreateAddress("5046 S Greenwood Ave", "Chicago", "IL", 60615, false)
				)
			);
			createPerson(
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

	@Override /* IAddressBook */
	public List<Person> getPeople() {
		return this;
	}

	@Override /* IAddressBook */
	public Person createPerson(CreatePerson cp) throws Exception {
		Person p = new Person(uri, cp);
		add(p);
		return p;
	}

	@Override /* IAddressBook */
	public Person findPerson(int id) {
		for (Person p : this)
			if (p.id == id)
				return p;
		return null;
	}

	@Override /* IAddressBook */
	public Address findAddress(int id) {
		for (Person p : this)
			for (Address a : p.addresses)
				if (a.id == id)
					return a;
		return null;
	}

	@Override /* IAddressBook */
	public Person findPersonWithAddress(int id) {
		for (Person p : this)
			for (Address a : p.addresses)
				if (a.id == id)
					return p;
		return null;
	}

	@Override /* IAddressBook */
	public List<Address> getAddresses() {
		Set<Address> s = new LinkedHashSet<>();
		for (Person p : this)
			for (Address a : p.addresses)
				s.add(a);
		return new ArrayList<>(s);
	}

	@Override /* IAddressBook */
	public Person removePerson(int id) {
		Person p = findPerson(id);
		if (p != null)
			this.remove(p);
		return p;
	}

	/** Utility method */
	public static Calendar toCalendar(String birthDate) throws Exception {
		Calendar c = new GregorianCalendar();
		c.setTime(DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).parse(birthDate));
		return c;
	}
}