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

import static org.apache.juneau.internal.StringUtils.*;
import static org.apache.juneau.rest.test.TestUtils.*;
import static org.junit.Assert.*;

import java.util.*;

import org.apache.juneau.*;
import org.apache.juneau.config.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class ConfigTest extends RestTestcase {

	private static String URL = "/testConfig";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void test() throws Exception {
		RestClient c = TestMicroservice.client().accept("text/json+simple").build();
		
		Map<String,Map<String,Object>> m = c.doGet(URL).getResponse(Map.class, String.class, ObjectMap.class);

		Config cf = Config.create().memStore().build().load(m);

		assertObjectEquals("{int1:'1',int2:'[1,2,3]',int3:'1',int4:'1',int5:'-1',boolean1:'true',boolean2:'[true,true]',testManifestEntry:'$MF{Test-Entry}'}", cf.getSectionAsMap("Test"));

		assertEquals("'1'", c.doGet(URL + "/Test%2Fint1/" + getName(String.class)).getResponseAsString());
		assertEquals("'[1,2,3]'", c.doGet(URL + "/Test%2Fint2/" + getName(String.class)).getResponseAsString());
		assertEquals("['1','2','3']", c.doGet(URL + "/Test%2Fint2/" + getName(String[].class)).getResponseAsString());
		assertEquals("[1,2,3]", c.doGet(URL + "/Test%2Fint2/" + getName(int[].class)).getResponseAsString());
		assertEquals("[1,2,3]", c.doGet(URL + "/Test%2Fint2/" + getName(Integer[].class)).getResponseAsString());
		assertEquals("1", c.doGet(URL + "/Test%2Fint3/" + getName(Integer.class)).getResponseAsString());
		assertEquals("1", c.doGet(URL + "/Test%2Fint4/" + getName(Integer.class)).getResponseAsString());
		assertEquals("-1", c.doGet(URL + "/Test%2Fint5/" + getName(Integer.class)).getResponseAsString());
		assertEquals("true", c.doGet(URL + "/Test%2Fboolean1/" + getName(Boolean.class)).getResponseAsString());
		assertEquals("[true,true]", c.doGet(URL + "/Test%2Fboolean2/" + getName(Boolean[].class)).getResponseAsString());
		assertEquals("'test-value'", c.doGet(URL + "/Test%2FtestManifestEntry/" + getName(String.class)).getResponseAsString());

		cf.close();
		c.closeQuietly();
	}

	private String getName(Class<?> c) {
		return urlEncode(c.getName());
	}
}
