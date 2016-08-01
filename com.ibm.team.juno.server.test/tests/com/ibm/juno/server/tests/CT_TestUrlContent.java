/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;

public class CT_TestUrlContent {

	private static String URL = "/testUrlContent";
	private static RestClient client;

	@BeforeClass
	public static void beforeClass() {
		client = new TestRestClient().setHeader("Accept", "text/plain");
	}

	@AfterClass
	public static void afterClass() {
		client.closeQuietly();
	}

	//====================================================================================================
	// Test URL &Content parameter containing a String
	//====================================================================================================
	@Test
	public void testString() throws Exception {
		String r;
		r = client.doGet(URL + "/testString?content=\'xxx\'&Content-Type=text/json").getResponseAsString();
		assertEquals("class=java.lang.String, value=xxx", r);
	}

	//====================================================================================================
	// Test URL &Content parameter containing an Enum
	//====================================================================================================
	@Test
	public void testEnum() throws Exception {
		String r;
		r = client.doGet(URL + "/testEnum?content='X1'&Content-Type=text/json").getResponseAsString();
		assertEquals("class=com.ibm.juno.server.test.TestUrlContent$TestEnum, value=X1", r);
	}

	//====================================================================================================
	// Test URL &Content parameter containing a Bean
	//====================================================================================================
	@Test
	public void testBean() throws Exception {
		String r;
		r = client.doGet(URL + "/testBean?content=%7Bf1:1,f2:'foobar'%7D&Content-Type=text/json").getResponseAsString();
		assertEquals("class=com.ibm.juno.server.test.TestUrlContent$TestBean, value={f1:1,f2:'foobar'}", r);
	}

	//====================================================================================================
	// Test URL &Content parameter containing an int
	//====================================================================================================
	@Test
	public void testInt() throws Exception {
		String r;
		r = client.doGet(URL + "/testInt?content=123&Content-Type=text/json").getResponseAsString();
		assertEquals("class=java.lang.Integer, value=123", r);
	}
}