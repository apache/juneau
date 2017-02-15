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

public class TransformsTest extends RestTestcase {

	private static String URL = "/testTransforms";

	//====================================================================================================
	// test1 - Test class transform overrides parent class transform
	// Should return "A2-1".
	//====================================================================================================
	@Test
	public void testClassTransformOverridesParentClassTransform() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;
		String url = URL + "/testClassTransformOverridesParentClassTransform";

		r = client.doGet(url).getResponse(String.class);
		assertEquals("A2-0", r);

		r = client.doPut(url, "A2-1").getResponse(String.class);
		assertEquals("A2-1", r);

		r = client.doPut(url + "/A2-2", "").getResponse(String.class);
		assertEquals("A2-2", r);

		client.closeQuietly();
	}

	//====================================================================================================
	// Test method transform overrides class transform
	// Should return "A3-1".
	//====================================================================================================
	@Test
	public void testMethodTransformOverridesClassTransform() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.DEFAULT, JsonParser.DEFAULT);
		String r;
		String url = URL + "/testMethodTransformOverridesClassTransform";

		r = client.doGet(url).getResponse(String.class);
		assertEquals("A3-0", r);

		r = client.doPut(url, "A3-1").getResponse(String.class);
		assertEquals("A3-1", r);

		r = client.doPut(url + "/A3-2", "").getResponse(String.class);
		assertEquals("A3-2", r);

		client.closeQuietly();
	}
}
