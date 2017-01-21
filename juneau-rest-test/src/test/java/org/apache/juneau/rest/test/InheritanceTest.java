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
package org.apache.juneau.rest.test;

import static org.junit.Assert.*;

import org.apache.juneau.json.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class InheritanceTest {

	private static RestClient client;

	@BeforeClass
	public static void beforeClass() {
		client = new TestRestClient();
	}

	@AfterClass
	public static void afterClass() {
		client.closeQuietly();
	}

	//====================================================================================================
	// Test serializer inheritance.
	//====================================================================================================
	@Test
	public void testSerializers() throws Exception {
		String r;
		String url = "/testInheritanceSerializers";
		r = client.doGet(url + "/test1").getResponseAsString();
		assertEquals("['text/s3','text/s4','text/s1','text/s2']", r);

		r = client.doGet(url + "/test2").getResponseAsString();
		assertEquals("['text/s5']", r);

		r = client.doGet(url + "/test3").getResponseAsString();
		assertEquals("['text/s5','text/s3','text/s4','text/s1','text/s2']", r);
	}

	//====================================================================================================
	// Test parser inheritance.
	//====================================================================================================
	@Test
	public void testParsers() throws Exception {
		String r;
		String url = "/testInheritanceParsers";
		r = client.doGet(url + "/test1").getResponseAsString();
		assertEquals("['text/p3','text/p4','text/p1','text/p2']", r);

		r = client.doGet(url + "/test2").getResponseAsString();
		assertEquals("['text/p5']", r);

		r = client.doGet(url + "/test3").getResponseAsString();
		assertEquals("['text/p5','text/p3','text/p4','text/p1','text/p2']", r);
	}

	//====================================================================================================
	// Test encoder inheritance.
	//====================================================================================================
	@Test
	public void testEncoders() throws Exception {
		String url = "/testInheritanceEncoders";
		String r = client.doGet(url + "/test").getResponseAsString();
		assertEquals("['e3','e4','e1','e2','identity']", r);
	}

	//====================================================================================================
	// Test filter inheritance.
	//====================================================================================================
	@Test
	public void testTransforms() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.class, JsonParser.class).setAccept("text/json+simple");
		String r;
		String url = "/testInheritanceTransforms";

		r = client.doGet(url + "/test1").getResponseAsString();
		assertEquals("['F1','F2','Foo3']", r);

		r = client.doGet(url + "/test2").getResponseAsString();
		assertEquals("['F1','F2','F3']", r);

		r = client.doGet(url + "/test3").getResponseAsString();
		assertEquals("['F1','F2','F3']", r);

		r = client.doGet(url + "/test4").getResponseAsString();
		assertEquals("['Foo1','Foo2','F3']", r);

		r = client.doGet(url + "/test5").getResponseAsString();
		assertEquals("['F1','F2','F3']", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test properties inheritance.
	//====================================================================================================
	@Test
	public void testProperties() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.class, JsonParser.class).setAccept("text/json+simple");
		String r;
		String url = "/testInheritanceProperties";

		r = client.doGet(url + "/test1").getResponseAsString();
		assertEquals("{p1:'v1',p2:'v2a',p3:'v3',p4:'v4'}", r);

		r = client.doGet(url + "/test2?override").getResponseAsString();
		assertEquals("{p1:'x',p2:'x',p3:'x',p4:'x',p5:'x'}", r);

		r = client.doGet(url + "/test2").getResponseAsString();
		assertEquals("{p1:'v1',p2:'v2a',p3:'v3',p4:'v4a',p5:'v5'}", r);

		client.closeQuietly();
	}
}
