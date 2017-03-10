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

import static org.apache.juneau.xml.XmlSerializerContext.*;
import static org.junit.Assert.*;

import org.apache.juneau.examples.addressbook.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.uon.*;
import org.apache.juneau.xml.*;
import org.junit.*;

public class SampleRemoteableServicesResourceTest extends RestTestcase {

	static RestClient[] clients;

	@BeforeClass
	public static void beforeClass() throws Exception {
		clients = new RestClient[] {
			SamplesMicroservice.client()
				.pojoSwaps(CalendarSwap.DateMedium.class)
				.remoteableServletUri("/remoteable")
				.build(),
			SamplesMicroservice.client(XmlSerializer.class, XmlParser.class)
				.pojoSwaps(CalendarSwap.DateMedium.class)
				.remoteableServletUri("/remoteable")
				.property(XML_autoDetectNamespaces, true)
				.build(),
			SamplesMicroservice.client(UonSerializer.class, UonParser.class)
				.pojoSwaps(CalendarSwap.DateMedium.class)
				.remoteableServletUri("/remoteable")
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
			IAddressBook ab = client.getRemoteableProxy(IAddressBook.class);
			Person p = ab.createPerson(
				new CreatePerson("Test Person",
					AddressBook.toCalendar("Aug 1, 1999"),
					new CreateAddress("Test street", "Test city", "Test state", 12345, true))
			);
			assertEquals(
				"{_type:'person',id:x,name:'Test Person',birthDate:'Aug 1, 1999',addresses:[{id:x,street:'Test street',city:'Test city',state:'Test state',zip:12345,isCurrent:true}],age:x}",
				JsonSerializer.DEFAULT_LAX.toString(p).replaceAll("id:\\d+", "id:x").replaceAll("age:\\d+", "age:x"));
		}
	}

}
