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

public class PropertiesTest extends RestTestcase {

	private static String URL = "/testProperties";

	//====================================================================================================
	// Properties defined on method.
	//====================================================================================================
	@Test
	public void testPropertiesDefinedOnMethod() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r = client.doGet(URL + "/testPropertiesDefinedOnMethod").getResponseAsString();
		assertTrue(r.matches("A1=a1,A2=c,B1=b1,B2=c,C=c,R1a=.*/testProperties/testPropertiesDefinedOnMethod,R1b=.*/testProperties,R2=bar,R3=baz,R4=a1,R5=c,R6=c"));
	}

	//====================================================================================================
	// Make sure attributes/parameters/headers are available through ctx.getProperties().
	//====================================================================================================
	@Test
	public void testProperties() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r = client.doGet(URL + "/testProperties/a1?P=p1").header("H", "h1").getResponseAsString();
		assertEquals("A=a1,P=p1,H=h1", r);
	}
}
