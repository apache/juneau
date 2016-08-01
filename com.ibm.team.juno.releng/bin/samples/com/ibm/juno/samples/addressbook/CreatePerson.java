/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.samples.addressbook;

import java.util.*;

import com.ibm.juno.core.annotation.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.jena.annotation.*;
import com.ibm.juno.core.xml.annotation.*;

/**
 * POJO for creating a new person
 */
@Xml(prefix="per",name="person")
@Rdf(prefix="per")
public class CreatePerson {

	// Bean properties
	public String name;
	@BeanProperty(filter=CalendarFilter.Medium.class) public Calendar birthDate;
	public LinkedList<CreateAddress> addresses = new LinkedList<CreateAddress>();

	/** Bean constructor - Needed for instantiating on server side */
	public CreatePerson() {}

	/** Normal constructor - Needed for instantiating on client side */
	public CreatePerson(String name, Calendar birthDate, CreateAddress...addresses) {
		this.name = name;
		this.birthDate = birthDate;
		this.addresses.addAll(Arrays.asList(addresses));
	}
}

