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

import static org.apache.juneau.server.TestUtils.*;
import static org.junit.Assert.*;

import org.apache.juneau.client.*;
import org.apache.juneau.ini.*;
import org.apache.juneau.json.*;
import org.junit.*;

public class ConfigTest {

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
		assertEquals("'org.apache.juneau.microservice.RestMicroservice'", c.doGet(URL + "/Test%2FmainClass/" + getName(String.class)).getResponseAsString());

		c.closeQuietly();
	}

	private String getName(Class<?> c) {
		return RestUtils.encode(c.getName());
	}
}
