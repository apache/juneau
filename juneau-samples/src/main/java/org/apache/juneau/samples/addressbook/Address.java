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

import org.apache.juneau.annotation.*;
import org.apache.juneau.jena.annotation.*;
import org.apache.juneau.xml.annotation.*;

/**
 * Address bean
 */
@Xml(prefix="addr")
@Rdf(prefix="addr")
@Bean(typeName="address")
public class Address {

	private static int nextAddressId = 1;

	// Bean properties
	@Rdf(beanUri=true) public URI uri;
	public URI personUri;
	public int id;
	@Xml(prefix="mail") @Rdf(prefix="mail") public String street, city, state;
	@Xml(prefix="mail") @Rdf(prefix="mail") public int zip;
	public boolean isCurrent;

	/** Bean constructor - Needed for instantiating on client side */
	public Address() {}

	/** Normal constructor - Needed for instantiating on server side */
	public Address(URI addressBookUri, URI personUri, CreateAddress ca) throws Exception {
		this.id = nextAddressId++;
		if (addressBookUri != null)
		this.uri = addressBookUri.resolve("addresses/" + id);
		this.personUri = personUri;
		this.street = ca.street;
		this.city = ca.city;
		this.state = ca.state;
		this.zip = ca.zip;
		this.isCurrent = ca.isCurrent;
	}
}