/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.samples.addressbook;

import java.net.*;
import java.text.*;
import java.util.*;

import com.ibm.juno.core.xml.annotation.Xml;

/**
 *  Address book bean
 */
@Xml(name="addressBook")
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
		Set<Address> s = new LinkedHashSet<Address>();
		for (Person p : this)
			for (Address a : p.addresses)
				s.add(a);
		return new ArrayList<Address>(s);
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


