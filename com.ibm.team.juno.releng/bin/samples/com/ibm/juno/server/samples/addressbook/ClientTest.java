/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 *  The source code for this program is not published or otherwise
 *  divested of its trade secrets, irrespective of what has been
 *  deposited with the U.S. Copyright Office.
 *******************************************************************************/
package com.ibm.juno.server.samples.addressbook;

import java.text.*;
import java.util.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.samples.addressbook.*;

/**
 * Sample client code for interacting with AddressBookResource
 */
public class ClientTest {

	public static void main(String[] args) {

		try {
			System.out.println("Running client test...");

			// Create a client to handle XML requests and responses.
			RestClient client = new RestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
			RestClient xmlClient = new RestClient(XmlSerializer.DEFAULT, XmlParser.DEFAULT);

			String root = "http://localhost:10000/addressBook";

			// Get the current contents of the address book
			AddressBook ab = client.doGet(root + "/people").getResponse(AddressBook.class);
			System.out.println("Number of entries = " + ab.getPeople().size());

			// Same, but use XML as the protocol both ways
			ab = xmlClient.doGet(root + "/people").getResponse(AddressBook.class);
			System.out.println("Number of entries = " + ab.getPeople().size());


			// Delete the existing entries
			for (Person p : ab.getPeople()) {
				String r = client.doDelete(p.uri).getResponse(String.class);
				System.out.println("Deleted person " + p.name + ", response = " + r);
			}

			// Make sure they're gone
			ab = client.doGet(root + "/people").getResponse(AddressBook.class);
			System.out.println("Number of entries = " + ab.getPeople().size());

			// Add 1st person again
			CreatePerson cp = new CreatePerson(
				"Barack Obama",
				toCalendar("Aug 4, 1961"),
				new CreateAddress("1600 Pennsylvania Ave", "Washington", "DC", 20500, true),
				new CreateAddress("5046 S Greenwood Ave", "Chicago", "IL", 60615, false)
			);
			Person p = client.doPost(root + "/people", cp).getResponse(Person.class);
			System.out.println("Created person " + p.name + ", uri = " + p.uri);

			// Add 2nd person again, but add addresses separately
			cp = new CreatePerson(
				"George Walker Bush",
				toCalendar("Jul 6, 1946")
			);
			p = client.doPost(root + "/people", cp).getResponse(Person.class);
			System.out.println("Created person " + p.name + ", uri = " + p.uri);

			// Add addresses to 2nd person
			CreateAddress ca = new CreateAddress("43 Prairie Chapel Rd", "Crawford", "TX", 76638, true);
			Address a = client.doPost(p.uri + "/addresses", ca).getResponse(Address.class);
			System.out.println("Created address " + a.uri);

			ca = new CreateAddress("1600 Pennsylvania Ave", "Washington", "DC", 20500, false);
			a = client.doPost(p.uri + "/addresses", ca).getResponse(Address.class);
			System.out.println("Created address " + a.uri);

			// Find 1st person, and change name
			Person[] pp = client.doGet(root + "/people?q=(name='Barack+Obama')").getResponse(Person[].class);
			String r = client.doPut(pp[0].uri + "/name", "Barack Hussein Obama").getResponse(String.class);
			System.out.println("Changed name, response = " + r);
			p = client.doGet(pp[0].uri).getResponse(Person.class);
			System.out.println("New name = " + p.name);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// Utility method
	public static Calendar toCalendar(String birthDate) throws Exception {
		Calendar c = new GregorianCalendar();
		c.setTime(DateFormat.getDateInstance(DateFormat.MEDIUM).parse(birthDate));
		return c;
	}
}
