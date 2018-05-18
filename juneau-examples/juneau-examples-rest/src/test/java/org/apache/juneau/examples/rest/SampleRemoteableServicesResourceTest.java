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

import static org.apache.juneau.xml.XmlSerializer.*;
import static org.junit.Assert.*;

import java.text.*;

import org.apache.juneau.examples.addressbook.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.transforms.*;
import org.junit.*;

public class SampleRemoteableServicesResourceTest extends RestTestcase {

	static RestClient[] clients;

	private static String path = SamplesMicroservice.getURI().getPath() + "/addressBook/proxy";

	@BeforeClass
	public static void beforeClass() throws Exception {
		clients = new RestClient[] {
			SamplesMicroservice.client()
				.json()
				.pojoSwaps(CalendarSwap.DateMedium.class)
				.build(),
			SamplesMicroservice.client()
				.xml()
				.pojoSwaps(CalendarSwap.DateMedium.class)
				.set(XML_autoDetectNamespaces, true)
				.build(),
			SamplesMicroservice.client()
				.uon()
				.pojoSwaps(CalendarSwap.DateMedium.class)
				.build(),
		};
	}

	@AfterClass
	public static void afterClass() {
		for (RestClient c : clients) {
			c.closeQuietly();
		}
	}

	//====================================================================================================
	// Get AddressBookResource as JSON
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		for (RestClient client : clients) {
			IAddressBook ab = client.getRemoteableProxy(IAddressBook.class, path);
			Person p = ab.createPerson(
				new CreatePerson("Test Person",
					AddressBook.toCalendar("Aug 1, 1999"),
					new CreateAddress("Test street", "Test city", "Test state", 12345, true))
			);
			assertEquals("Test Person", p.name);
			assertEquals("Aug 1, 1999", DateFormat.getDateInstance(DateFormat.MEDIUM).format(p.birthDate.getTime()));
			assertEquals("Test street", p.addresses.get(0).street);
			assertEquals("Test city", p.addresses.get(0).city);
			assertEquals("Test state", p.addresses.get(0).state);
			assertEquals(12345, p.addresses.get(0).zip);
			assertEquals(true, p.addresses.get(0).isCurrent);
		}
	}
}
