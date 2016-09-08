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

import static org.apache.juneau.server.samples.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.client.*;
import org.apache.juneau.html.*;
import org.apache.juneau.internal.*;
import org.apache.juneau.json.*;
import org.apache.juneau.samples.addressbook.*;
import org.apache.juneau.transforms.*;
import org.apache.juneau.xml.*;
import org.junit.*;

@SuppressWarnings({"serial"})
public class AddressBookResourceTest {

	private static boolean debug = false;

	static RestClient[] clients;

	@BeforeClass
	public static void beforeClass() throws Exception {
		clients = new RestClient[] {
			new SamplesRestClient(JsonSerializer.class, JsonParser.class),
			new SamplesRestClient(XmlSerializer.class, XmlParser.class),
			new SamplesRestClient(HtmlSerializer.class, HtmlParser.class).setAccept("text/html+stripped"),
			new SamplesRestClient(XmlSerializer.class,  HtmlParser.class).setAccept("text/html+stripped")
		};
		for (RestClient c : clients) {
			c.getSerializer().addPojoSwaps(CalendarSwap.Medium.class);
			c.getParser().addPojoSwaps(CalendarSwap.Medium.class);
			c.getSerializer().setProperty(XmlSerializerContext.XML_autoDetectNamespaces, true);
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
		String in = IOUtils.read(getClass().getResourceAsStream("/org/apache/juneau/server/test/AddressBookResource_test0Test.json"));
		JsonParser p = new JsonParser().addPojoSwaps(CalendarSwap.Medium.class);
		Person person = p.parse(in, Person.class);
		if (debug) System.err.println(person);
	}

	// A list of People objects.
	public static class PersonList extends LinkedList<Person> {}

	//====================================================================================================
	// PojoRest tests
	//====================================================================================================
	@Test
	public void testPojoRest() throws Exception {
		for (RestClient client : clients) {
			int rc;
			Person p;
			List<Person> people;

			// Reinitialize the resource
			rc = client.doGet("/addressBook?method=init").run();
			assertEquals(200, rc);

			// Simple GETs
			people = client.doGet("/addressBook/people").getResponse(PersonList.class);
			assertEquals("Barack Obama", people.get(0).name);
			assertEquals(76638, people.get(1).addresses.get(0).zip);

			// PUT a simple String field
			p = people.get(0);
			rc = client.doPut(p.uri+"/name", "foo").run();
			assertEquals(200, rc);
			String name = client.doGet(p.uri+"/name").getResponse(String.class);
			assertEquals("foo", name);
			p = client.doGet(p.uri).getResponse(Person.class);
			assertEquals("foo", p.name);

			// POST an address as JSON
			CreateAddress ca = new CreateAddress("a1","b1","c1",1,false);
			Address a = client.doPost(p.uri + "/addresses", new ObjectMap(BeanContext.DEFAULT.forBean(ca))).getResponse(Address.class);
			assertEquals("a1", a.street);
			a = client.doGet(a.uri).getResponse(Address.class);
			assertEquals("a1", a.street);
			assertEquals(1, a.zip);
			assertFalse(a.isCurrent);

			// POST an address as a bean
			ca = new CreateAddress("a2","b2","c2",2,true);
			a = client.doPost(p.uri + "/addresses", ca).getResponse(Address.class);
			assertEquals("a2", a.street);
			a = client.doGet(a.uri).getResponse(Address.class);
			assertEquals("a2", a.street);
			assertEquals(2, a.zip);
			assertTrue(a.isCurrent);

			// POST a person
			CreatePerson billClinton = new CreatePerson("Bill Clinton", AddressBook.toCalendar("Aug 19, 1946"),
				new CreateAddress("a3","b3","c3",3,false)
			);
			rc = client.doPost("/addressBook/people", billClinton).run();
			assertEquals(200, rc);
			people = client.doGet("/addressBook/people").getResponse(PersonList.class);
			p = people.get(2);
			assertEquals(3, people.size());
			assertEquals("Bill Clinton", p.name);

			// DELETE an address
			rc = client.doDelete(p.addresses.get(0).uri).run();
			assertEquals(200, rc);
			people = client.doGet("/addressBook/people").getResponse(PersonList.class);
			p = people.get(2);
			assertEquals(0, p.addresses.size());

			// DELETE a person
			rc = client.doDelete(p.uri).run();
			assertEquals(200, rc);
			people = client.doGet("/addressBook/people").getResponse(PersonList.class);
			assertEquals(2, people.size());

			// Reinitialize the resource
			rc = client.doGet("/addressBook?method=init").run();
			assertEquals(200, rc);
		}
	}

	//====================================================================================================
	// PojoQuery tests
	//====================================================================================================
	@Test
	public void testPojoQuery() throws Exception {

		for (RestClient client : clients) {
			RestCall r;
			List<Person> people;

			// Reinitialize the resource
			int rc = client.doGet("/addressBook?method=init").run();
			assertEquals(200, rc);

			r = client.doGet("/addressBook/people?q=(name=B*)");
			people = r.getResponse(PersonList.class);
			assertEquals(1, people.size());
			assertEquals("Barack Obama", people.get(0).name);

			r = client.doGet("/addressBook/people?q=(name='Barack+Obama')");
			people = r.getResponse(PersonList.class);
			assertEquals(1, people.size());
			assertEquals("Barack Obama", people.get(0).name);

			r = client.doGet("/addressBook/people?q=(name='Barack%20Obama')");
			people = r.getResponse(PersonList.class);
			assertEquals(1, people.size());
			assertEquals("Barack Obama", people.get(0).name);

			r = client.doGet("/addressBook/people?v=(name,birthDate)");
			people = r.getResponse(PersonList.class);
			assertEquals("Barack Obama", people.get(0).name);
			assertTrue(people.get(0).getAge() > 10);
			assertEquals(0, people.get(0).addresses.size());

			r = client.doGet("/addressBook/people?v=(addresses,birthDate)");
			people = r.getResponse(PersonList.class);
			assertNull(people.get(0).name);
			assertTrue(people.get(0).getAge() > 10);
			assertEquals(2, people.get(0).addresses.size());

			r = client.doGet("/addressBook/people?s=($o(age=d))");
			people = r.getResponse(PersonList.class);
			assertTrue(people.get(0).getAge() > 10);
			r = client.doGet("/addressBook/people?s=(age)");
			people = r.getResponse(PersonList.class);
			assertTrue(people.get(0).getAge() > 10);
			r = client.doGet("/addressBook/people?s=($o(age=a))");
			people = r.getResponse(PersonList.class);
			assertTrue(people.get(0).getAge() > 10);

			r = client.doGet("/addressBook/people?p=1&l=1");
			people = r.getResponse(PersonList.class);
			assertEquals(1, people.size());
			assertTrue(people.get(0).getAge() > 10);
		}
	}

	//====================================================================================================
	// PojoIntrospector tests
	//====================================================================================================
	@Test
	public void testPojoIntrospector() throws Exception {

		for (RestClient client : clients) {

			List<Person> people;

			// Reinitialize the resource
			int rc = client.doGet("/addressBook?method=init").run();
			assertEquals(200, rc);

			// Simple GETs
			people = client.doGet("/addressBook/people").getResponse(PersonList.class);
			Person p = people.get(0);
			int length = client.doGet(p.uri+"/name?invokeMethod=length").getResponse(Integer.class);
			assertEquals(12, length);

			String[] tokens = client.doGet(p.uri+"/name?invokeMethod=split(java.lang.String,int)&invokeArgs=['a',3]").getResponse(String[].class);
			assertObjectEquals("['B','r','ck Obama']", tokens);
		}
	}
}
