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

import java.net.*;

import org.apache.juneau.*;
import org.apache.juneau.client.*;
import org.apache.juneau.html.*;
import org.apache.juneau.json.*;
import org.apache.juneau.server.labels.*;
import org.apache.juneau.xml.*;
import org.junit.*;

public class RootResourcesTest {

	private static String path = URI.create(Constants.getSampleUrl()).getPath();              // /jazz/juneau/sample
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
		assertEquals("org.apache.juneau.server.labels.ChildResourceDescriptions<org.apache.juneau.server.labels.ResourceDescription>", m.getString("description"));
		assertEquals("org.apache.juneau.server.labels.ResourceDescription", m.getObjectMap("items").getString("description"));

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
