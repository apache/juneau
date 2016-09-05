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
package org.apache.juneau.samples.addressbook;

import java.net.URI;
import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Person POJO
 */
@Xml(prefix="per")
@Rdf(prefix="per")
@Bean(name="person")
public class Person {

	private static int nextPersonId = 1;

	// Bean properties
	@Rdf(beanUri=true) public URI uri;
	public URI addressBookUri;
	public int id;
	public String name;
	@BeanProperty(swap=CalendarSwap.Medium.class) public Calendar birthDate;
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

