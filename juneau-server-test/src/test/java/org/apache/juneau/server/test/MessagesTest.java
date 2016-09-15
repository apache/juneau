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
package org.apache.juneau.server.test;

import static org.apache.juneau.server.test.TestUtils.*;

import java.util.*;

import org.apache.juneau.client.*;
import org.apache.juneau.json.*;
import org.junit.*;

/**
 * Validates that resource bundles can be defined on both parent and child classes.
 */
public class MessagesTest {

	//====================================================================================================
	// Return contents of resource bundle.
	//====================================================================================================
	@SuppressWarnings("rawtypes")
	@Test
	public void test() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.class,JsonParser.class);

		// Parent resource should just pick up values from its bundle.
		TreeMap r = client.doGet("/testMessages/test").getResponse(TreeMap.class);
		assertObjectEquals("{key1:'value1a',key2:'value2a'}", r);

		// Child resource should pick up values from both parent and child,
		// ordered child before parent.
		r = client.doGet("/testMessages2/test").getResponse(TreeMap.class);
		assertObjectEquals("{key1:'value1a',key2:'value2b',key3:'value3b'}", r);

		client.closeQuietly();
	}
}
