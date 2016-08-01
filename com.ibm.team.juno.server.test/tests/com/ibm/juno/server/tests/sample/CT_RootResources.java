/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 *
 * Note to U.S. Government Users Restricted Rights:  Use,
 * duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/
package com.ibm.juno.server.tests.sample;

import static org.junit.Assert.*;

import org.junit.*;

import com.ibm.juno.client.*;
import com.ibm.juno.core.*;
import com.ibm.juno.core.html.*;
import com.ibm.juno.core.json.*;
import com.ibm.juno.core.xml.*;
import com.ibm.juno.server.labels.*;
import com.ibm.juno.server.tests.*;

public class CT_RootResources {

	private static String path = Constants.getJunoSamplesUri().getPath();              // /jazz/juno/sample
	private static boolean debug = false;

	private static RestClient jsonClient;

	@BeforeClass
	public static void beforeClass() {
		jsonClient = new SamplesRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
	}

	@AfterClass
	public static void afterClass() {
		jsonClient.closeQuietly();
	}

	//====================================================================================================
	// text/json
	//====================================================================================================
	@Test
	public void testJson() throws Exception {
		RestClient client = new SamplesRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		RestCall r = client.doGet("");
		ResourceDescription[] x = r.getResponse(ResourceDescription[].class);
		assertEquals("helloWorld", x[0].getName().getName());
		assertEquals(path + "/helloWorld", x[0].getName().getHref());
		assertEquals("Hello World sample resource", x[0].getDescription());

		r = jsonClient.doOptions("");
		ObjectMap x2 = r.getResponse(ObjectMap.class);
		String s = x2.getString("description");
		if (debug) System.err.println(s);
		assertTrue(s, s.startsWith("This is an example"));

		client.closeQuietly();
	}

	//====================================================================================================
	// text/xml
	//====================================================================================================
	@Test
	public void testXml() throws Exception {
		RestClient client = new SamplesRestClient().setParser(XmlParser.DEFAULT);
		RestCall r = client.doGet("");
		ResourceDescription[] x = r.getResponse(ResourceDescription[].class);
		assertEquals("helloWorld", x[0].getName().getName());
		assertEquals(path + "/helloWorld", x[0].getName().getHref());
		assertEquals("Hello World sample resource", x[0].getDescription());

		r = jsonClient.doOptions("");
		ObjectMap x2 = r.getResponse(ObjectMap.class);
		String s = x2.getString("description");
		if (debug) System.err.println(s);
		assertTrue(s, s.startsWith("This is an example"));

		client.closeQuietly();
	}

	//====================================================================================================
	// text/html+stripped
	//====================================================================================================
	@Test
	public void testHtmlStripped() throws Exception {
		RestClient client = new SamplesRestClient().setParser(HtmlParser.DEFAULT).setAccept("text/html+stripped");
		RestCall r = client.doGet("");
		ResourceDescription[] x = r.getResponse(ResourceDescription[].class);
		assertEquals("helloWorld", x[0].getName().getName());
		assertTrue(x[0].getName().getHref().endsWith("/helloWorld"));
		assertEquals("Hello World sample resource", x[0].getDescription());

		r = jsonClient.doOptions("").setHeader("Accept", "text/json");
		ObjectMap x2 = r.getResponse(ObjectMap.class);
		String s = x2.getString("description");
		if (debug) System.err.println(s);
		assertTrue(s, s.startsWith("This is an example"));

		client.closeQuietly();
	}

	//====================================================================================================
	// /htdoces/styles.css
	//====================================================================================================
	@Test
	public void testStyleSheet() throws Exception {
		RestClient client = new SamplesRestClient().setAccept("text/css");
		RestCall r = client.doGet("/style.css");
		String css = r.getResponseAsString();
		if (debug) System.err.println(css);
		assertTrue(css, css.indexOf("table {") != -1);

		client.closeQuietly();
	}

	//====================================================================================================
	// application/json+schema
	//====================================================================================================
	@Test
	public void testJsonSchema() throws Exception {
		RestClient client = new SamplesRestClient().setParser(JsonParser.DEFAULT).setAccept("text/json+schema");
		RestCall r = client.doGet("");
		ObjectMap m = r.getResponse(ObjectMap.class);
		if (debug) System.err.println(m);
		assertEquals("com.ibm.juno.server.labels.ChildResourceDescriptions<com.ibm.juno.server.labels.ResourceDescription>", m.getString("description"));
		assertEquals("com.ibm.juno.server.labels.ResourceDescription", m.getObjectMap("items").getString("description"));

		client.closeQuietly();
	}

	//====================================================================================================
	// OPTIONS page
	//====================================================================================================
	@Test
	public void testOptionsPage() throws Exception {
		RestCall r = jsonClient.doOptions("");
		ResourceOptions o = r.getResponse(ResourceOptions.class);
		if (debug) System.err.println(o);
		assertEquals("This is an example of a router resource that is used to access other resources.", o.getDescription());
	}
}
