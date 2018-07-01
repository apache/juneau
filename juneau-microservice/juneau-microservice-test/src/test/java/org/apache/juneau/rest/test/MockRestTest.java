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

import static org.apache.juneau.http.HttpMethodName.*;
import static org.junit.Assert.*;

import org.apache.juneau.http.annotation.*;
import org.apache.juneau.json.*;
import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.client.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;
import org.junit.runners.*;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class MockRestTest {
	
	//=================================================================================================================
	// Basic tests
	//=================================================================================================================
	
	@RestResource 
	public static class A {
		@RestMethod(name=PUT, path="/a01")
		public String a01(@Body String body) {
			return body;
		}

		@RestMethod(name=PUT, path="/a02", serializers=JsonSerializer.class, parsers=JsonParser.class)
		public String a02(@Body String body) {
			return body;
		}
	}

	@Test
	public void a01() throws Exception {
		MockRest a = MockRest.create(A.class);
		RestClient rc = RestClient.create().mockHttpConnection(a).build();
		assertEquals("OK", rc.doPut("/a01", "OK").getResponseAsString());
	}

	@Test
	public void a02() throws Exception {
		MockRest a = MockRest.create(A.class);
		RestClient rc = RestClient.create().json().mockHttpConnection(a).build();
		assertEquals("OK", rc.doPut("/a02", "OK").getResponse(String.class));
	}
}

