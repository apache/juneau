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
import org.apache.juneau.json.*;
import org.junit.*;

public class BeanContextPropertiesTest {

	boolean debug = false;

	//====================================================================================================
	// Validate that filters defined on class filter to underlying bean context.
	//====================================================================================================
	@Test
	public void testClassTransforms() throws Exception {
		RestClient client = new TestRestClient(JsonSerializer.class, JsonParser.class);
		String r;
		r = client.doGet("/testBeanContext/testClassTransforms/2001-07-04T15:30:45Z?d2=2001-07-05T15:30:45Z").setHeader("X-D3", "2001-07-06T15:30:45Z").getResponseAsString();
		assertEquals("d1=2001-07-04T15:30:45Z,d2=2001-07-05T15:30:45Z,d3=2001-07-06T15:30:45Z", r);

		client.closeQuietly();
	}
}