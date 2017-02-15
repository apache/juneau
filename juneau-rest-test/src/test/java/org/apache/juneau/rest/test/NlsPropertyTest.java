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

import org.apache.juneau.plaintext.*;
import org.apache.juneau.rest.client.*;
import org.junit.*;

public class NlsPropertyTest extends RestTestcase {

	private static String URL = "/testNlsProperty";

	//====================================================================================================
	// Test getting an NLS property defined on a class.
	//====================================================================================================
	@Test
	public void testInheritedFromClass() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String r = client.doGet(URL + "/testInheritedFromClass").getResponseAsString();
		assertEquals("value1", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test getting an NLS property defined on a method.
	//====================================================================================================
	@Test
	public void testInheritedFromMethod() throws Exception {
		RestClient client = new TestRestClient(PlainTextSerializer.class, PlainTextParser.class);
		String r = client.doGet(URL + "/testInheritedFromMethod").getResponseAsString();
		assertEquals("value2", r);

		client.closeQuietly();
	}
}
