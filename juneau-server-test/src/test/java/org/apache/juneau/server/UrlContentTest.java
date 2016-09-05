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
package org.apache.juneau.server;

import static org.junit.Assert.*;

import org.apache.juneau.client.*;
import org.junit.*;

public class UrlContentTest {

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
		assertEquals("class=org.apache.juneau.server.UrlContentResource$TestEnum, value=X1", r);
	}

	//====================================================================================================
	// Test URL &Content parameter containing a Bean
	//====================================================================================================
	@Test
	public void testBean() throws Exception {
		String r;
		r = client.doGet(URL + "/testBean?content=%7Bf1:1,f2:'foobar'%7D&Content-Type=text/json").getResponseAsString();
		assertEquals("class=org.apache.juneau.server.UrlContentResource$TestBean, value={f1:1,f2:'foobar'}", r);
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