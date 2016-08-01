/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.samples.addressbook;

import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * POJO for creating a new address
 */
@Xml(prefix="addr",name="address")
@Rdf(prefix="addr")
public class CreateAddress {

	// Bean properties
	@Xml(prefix="mail") @Rdf(prefix="mail") public String street, city, state;
	@Xml(prefix="mail") @Rdf(prefix="mail") public int zip;
	public boolean isCurrent;

	/** Bean constructor - Needed for instantiating on server side */
	public CreateAddress() {}

	/** Normal constructor - Needed for instantiating on client side */
	public CreateAddress(String street, String city, String state, int zip, boolean isCurrent) {
		this.street = street;
		this.city = city;
		this.state = state;
		this.zip = zip;
		this.isCurrent = isCurrent;
	}
}
