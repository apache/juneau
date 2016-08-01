/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.samples.addressbook;

import java.net.URI;
import java.util.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Person POJO
 */
@Xml(prefix="per",name="person")
@Rdf(prefix="per")
public class Person {

	private static int nextPersonId = 1;

	// Bean properties
	@BeanProperty(beanUri=true) public URI uri;
	public URI addressBookUri;
	public int id;
	public String name;
	@BeanProperty(filter=CalendarFilter.Medium.class) public Calendar birthDate;
	public LinkedList<Address> addresses = new LinkedList<Address>();

	/** Bean constructor - Needed for instantiating on server side */
	public Person() {}

	/** Normal constructor - Needed for instantiating on client side */
	public Person(URI addressBookUri, CreatePerson cp) throws Exception {
		this.id = nextPersonId++;
		this.addressBookUri = addressBookUri;
		if (addressBookUri != null)
			this.uri = addressBookUri.resolve("people/" + id);
		this.name = cp.name;
		this.birthDate = cp.birthDate;
		for (CreateAddress ca : cp.addresses)
			this.addresses.add(new Address(addressBookUri, uri, ca));
	}

	/** Extra read-only bean property */
	public int getAge() {
		return new GregorianCalendar().get(Calendar.YEAR) - birthDate.get(Calendar.YEAR);
	}

	/** Convenience method - Add an address for this person */
	public Address createAddress(CreateAddress ca) throws Exception {
		Address a = new Address(addressBookUri, uri, ca);
		addresses.add(a);
		return a;
	}

	/** Extra method (for method invocation example) */
	public String sayHello(String toPerson, int age) {
		return name + " says hello to " + toPerson + " who is " + age + " years old";
	}
}

