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
package org.apache.juneau.server.samples;

import static org.junit.Assert.*;

import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.apache.juneau.samples.addressbook.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.urlencoding.*;
import org.apache.juneau.xml.*;
import org.junit.*;

public class SampleRemoteableServicesResourceTest {

	static RestClient[] clients;

	@BeforeClass
	public static void beforeClass() throws Exception {
		clients = new RestClient[] {
			new SamplesRestClient(JsonSerializer.class, JsonParser.class),
			new SamplesRestClient(XmlSerializer.class, XmlParser.class),
//	TODO - broken?		new TestRestClient(HtmlSerializer.class, HtmlParser.class).setAccept("text/html+stripped"),
			new SamplesRestClient(UonSerializer.class, UonParser.class),
		};
		for (RestClient c : clients) {
			c.addPojoSwaps(CalendarSwap.Medium.class);
			c.setRemoteableServletUri("/remoteable");
			c.setProperty(XmlSerializerContext.XML_autoDetectNamespaces, true);
		}
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
				"{id:x,name:'Test Person',birthDate:'Aug 1, 1999',addresses:[{id:x,street:'Test street',city:'Test city',state:'Test state',zip:12345,isCurrent:true}],age:x}",
				JsonSerializer.DEFAULT_LAX.toString(p).replaceAll("id:\\d+", "id:x").replaceAll("age:\\d+", "age:x"));
		}
	}

}
