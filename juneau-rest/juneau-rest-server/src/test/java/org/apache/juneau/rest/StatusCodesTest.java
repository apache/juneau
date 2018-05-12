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
package org.apache.juneau.rest;

import static org.apache.juneau.http.HttpMethodName.*;

import java.io.*;

import org.apache.juneau.rest.annotation.*;
import org.apache.juneau.rest.mock.*;
import org.junit.*;

/**
 * Validates that the correct status codes are returned on REST requests.
 */
@SuppressWarnings("javadoc")
public class StatusCodesTest {

	//=================================================================================================================
	// OK
	//=================================================================================================================
	
	@RestResource
	public static class A {
		@RestMethod(name=PUT)
		public Reader a01(@Body String b) {
			return new StringReader(b);
		}
	}
	
	private static MockRest a = MockRest.create(A.class);
	
	@Test
	public void a01a_OK() throws Exception {
		a.request("PUT", "/").body("foo").execute().assertStatus(200);
	}
	
	// TODO - Test all the status codes
}
