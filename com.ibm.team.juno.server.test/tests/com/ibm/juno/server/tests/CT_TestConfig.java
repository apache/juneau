/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static com.ibm.juno.server.tests.TestUtils.*;
import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.ini.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.server.*;

public class CT_TestConfig {

	private static String URL = "/testConfig";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		RestClient c = new TestRestClient(JsonSerializer.class, JsonParser.class).setAccept("text/json+simple");

		ConfigFile cf = c.doGet(URL).getResponse(ConfigFileImpl.class);

		assertObjectEquals("{int1:'1',int2:'1,2,3',int3:'$C{Test/int1, -1}',int4:'$C{Test/int3, -1}',int5:'$C{XXX, -1}',boolean1:'true',boolean2:'true,true',path:'$E{PATH}',mainClass:'$MF{Main-Class}',importPackage:'$MF{Import-Package}'}", cf.get("Test"));

		assertEquals("'1'", c.doGet(URL + "/Test%2Fint1/" + getName(String.class)).getResponseAsString());
		assertEquals("['1']", c.doGet(URL + "/Test%2Fint1/" + getName(String[].class)).getResponseAsString());
		assertEquals("'1,2,3'", c.doGet(URL + "/Test%2Fint2/" + getName(String.class)).getResponseAsString());
		assertEquals("['1','2','3']", c.doGet(URL + "/Test%2Fint2/" + getName(String[].class)).getResponseAsString());
		assertEquals("[1,2,3]", c.doGet(URL + "/Test%2Fint2/" + getName(int[].class)).getResponseAsString());
		assertEquals("[1,2,3]", c.doGet(URL + "/Test%2Fint2/" + getName(Integer[].class)).getResponseAsString());
		assertEquals("[1]", c.doGet(URL + "/Test%2Fint3/" + getName(int[].class)).getResponseAsString());
		assertEquals("[1]", c.doGet(URL + "/Test%2Fint4/" + getName(int[].class)).getResponseAsString());
		assertEquals("[-1]", c.doGet(URL + "/Test%2Fint5/" + getName(int[].class)).getResponseAsString());
		assertEquals("true", c.doGet(URL + "/Test%2Fboolean1/" + getName(Boolean.class)).getResponseAsString());
		assertEquals("[true,true]", c.doGet(URL + "/Test%2Fboolean2/" + getName(Boolean[].class)).getResponseAsString());
		assertTrue(c.doGet(URL + "/Test%2Fpath/" + getName(String.class)).getResponseAsString().length() > 10);
		assertEquals("'com.ibm.juno.microservice.RestMicroservice'", c.doGet(URL + "/Test%2FmainClass/" + getName(String.class)).getResponseAsString());

		c.closeQuietly();
	}

	private String getName(Class<?> c) {
		return RestUtils.encode(c.getName());
	}
}
