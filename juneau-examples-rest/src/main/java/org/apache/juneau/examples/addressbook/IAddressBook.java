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

import java.util.*;

import org.apache.juneau.remoteable.*;

/**
 * Interface used to help illustrate proxy interfaces.
 * See {@link SampleRemoteableServlet}.
 */
@Remoteable
public interface IAddressBook {

	/** Initialize this address book with preset entries */
	void init() throws Exception;

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
