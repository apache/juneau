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

import org.apache.juneau.rest.client.*;
import org.junit.*;

public class UrlContentTest extends RestTestcase {

	private static String URL = "/testUrlContent";
	private RestClient client = TestMicroservice.DEFAULT_CLIENT_PLAINTEXT;


	//====================================================================================================
	// Test URL &Body parameter containing a String
	//====================================================================================================
	@Test
	public void testString() throws Exception {
		String r;
		r = client.doGet(URL + "/testString?body=\'xxx\'&Content-Type=text/json").getResponseAsString();
		assertEquals("class=java.lang.String, value=xxx", r);
	}

	//====================================================================================================
	// Test URL &Body parameter containing an Enum
	//====================================================================================================
	@Test
	public void testEnum() throws Exception {
		String r;
		r = client.doGet(URL + "/testEnum?body='X1'&Content-Type=text/json").getResponseAsString();
		assertEquals("class=org.apache.juneau.rest.test.UrlContentResource$TestEnum, value=X1", r);
	}

	//====================================================================================================
	// Test URL &Body parameter containing a Bean
	//====================================================================================================
	@Test
	public void testBean() throws Exception {
		String r;
		r = client.doGet(URL + "/testBean?body=%7Bf1:1,f2:'foobar'%7D&Content-Type=text/json").getResponseAsString();
		assertEquals("class=org.apache.juneau.rest.test.UrlContentResource$TestBean, value={f1:1,f2:'foobar'}", r);
	}

	//====================================================================================================
	// Test URL &Body parameter containing an int
	//====================================================================================================
	@Test
	public void testInt() throws Exception {
		String r;
		r = client.doGet(URL + "/testInt?body=123&Content-Type=text/json").getResponseAsString();
		assertEquals("class=java.lang.Integer, value=123", r);
	}
}