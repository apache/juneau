/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.samples.addressbook;

import java.util.*;

import com.ibm.juno.server.samples.*;

/**
 * Interface used to help illustrate proxy interfaces.
 * See {@link SampleRemoteableServlet}.
 */
public interface IAddressBook {

	/** Return all people in the address book */
	List<Person> getPeople();

	/** Return all addresses in the address book */
	List<Address> getAddresses();

	/** Create a person in this address book */
	Person createPerson(CreatePerson cp) throws Exception;

	/** Find a person by id */
	Person findPerson(int id);

	/** Find an address by id */
	Address findAddress(int id);

	/** Find a person by address id */
	Person findPersonWithAddress(int id);

	/** Remove a person by id */
	Person removePerson(int id);
}
