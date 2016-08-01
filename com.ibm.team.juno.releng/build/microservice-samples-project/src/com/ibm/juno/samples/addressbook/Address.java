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

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * Address bean
 */
@Xml(prefix="addr",name="address")
@Rdf(prefix="addr")
public class Address {

	private static int nextAddressId = 1;

	// Bean properties
	@BeanProperty(beanUri=true) public URI uri;
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