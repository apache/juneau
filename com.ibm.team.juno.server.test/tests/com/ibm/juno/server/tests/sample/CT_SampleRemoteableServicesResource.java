/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests.sample;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.filters.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.urlencoding.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.samples.addressbook.*;

public class CT_SampleRemoteableServicesResource {

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
			c.addFilters(CalendarFilter.Medium.class);
			c.setRemoteableServletUri("/remoteable");
			c.setProperty(XmlSerializerProperties.XML_autoDetectNamespaces, true);
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
