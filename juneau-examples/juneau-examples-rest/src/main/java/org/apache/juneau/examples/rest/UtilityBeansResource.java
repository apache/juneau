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
package org.apache.juneau.examples.rest;

import java.util.*;

import org.apache.juneau.annotation.*;
import org.apache.juneau.examples.parser.*;
import org.apache.juneau.examples.serializer.*;
import org.apache.juneau.html.annotation.*;
import org.apache.juneau.http.annotation.*;
import org.apache.juneau.http.response.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.beans.*;
import org.apache.juneau.rest.config.*;
import org.apache.juneau.rest.servlet.*;

import java.awt.image.*;
import java.net.*;

/**
 * Sample resource that allows images to be uploaded and retrieved.
 *
 * <ul class='seealso'>
 * 	<li class='link'>{@doc jrs.Marshalling REST Marshalling}
 * 	<li class='jc'>{@link ImageSerializer}
 * 	<li class='jc'>{@link ImageParser}
 * 	<li class='extlink'>{@source}
 * </ul>
 */
@Rest(
	path="/utilitybeans",
	title="Utility beans examples",
	description="Examples of utility bean usage."
)
@HtmlDocConfig(
	navlinks="options: ?method=OPTIONS"
)
public class UtilityBeansResource extends BasicRestServlet implements BasicUniversalConfig {

	private static final long serialVersionUID = 1L;

	@RestGet("/")
	public ResourceDescriptions getDescriptions() {
		return ResourceDescriptions
			.create()
			.append("BeanDescription", "Example of BeanDescription bean");
	}


	@RestGet("/BeanDescription")
	public BeanDescription aBeanDescription() {
		return BeanDescription.of(AddressBook.class);
	}

	public static class AddressBook extends LinkedList<Person> {

		public AddressBook init() {
			add(
				new Person("Bill Clinton", 65,
					new Address("55W. 125th Street", "New York", "NY", 10027, true)
				)
			);
			return this;
		}
	}

	@Bean(p="street,city,state,zip,isCurrent")
	public static class Address {
		public String street;
		public String city;
		public String state;
		public int zip;
		public boolean isCurrent;

		public Address() {}

		public Address(String street, String city, String state, int zip, boolean isCurrent) {
			this.street = street;
			this.city = city;
			this.state = state;
			this.zip = zip;
			this.isCurrent = isCurrent;
		}
		@Override /* Object */
		public String toString() {
			return "Address(street="+street+",city="+city+",state="+state+",zip="+zip+",isCurrent="+isCurrent+")";
		}
	}

	@Bean(typeName="Person",p="name,age,addresses")
	public static class Person {
		public String name;
		public int age;
		public Address[] addresses;

		public Person() {}

		public Person(String name, int age, Address...addresses) {
			this.name = name;
			this.age = age;
			this.addresses = addresses;
		}

		@Override /* Object */
		public String toString() {
			return "Person(name="+name+",age="+age+")";
		}
	}



}