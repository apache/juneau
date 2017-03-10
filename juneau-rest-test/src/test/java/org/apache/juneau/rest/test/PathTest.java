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

public class PathTest extends RestTestcase {

	private static String URL = "/testPath";

	//====================================================================================================
	// Basic tests
	//====================================================================================================
	@Test
	public void testBasic() throws Exception {
		RestClient client = TestMicroservice.DEFAULT_CLIENT;
		String r = null;

		r = client.doGet(URL).getResponse(String.class);
		assertEquals("/testPath", r);

		r = client.doGet(URL + "/testPath2").getResponse(String.class);
		assertEquals("/testPath/testPath2", r);

		r = client.doGet(URL + "/testPath2/testPath3").getResponse(String.class);
		assertEquals("/testPath/testPath2/testPath3", r);
	}
}
